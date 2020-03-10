package routing;

import core.*;
import interfaces.SatelliteWithChannelModelInterface;
import interfaces.channelModel;
import movement.MovementModel;
import movement.SatelliteMovement;
import util.Tuple;

import java.util.*;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File RelayModeforInternetAccess.java
 * Package Name:routing
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class RelayRouterforInternetAccess extends ActiveRouter{

    /** Container for generic message properties. Note that all values
     * stored in the properties should be immutable as the same of
     * properties in Message.java */
    private Map<String, Object> properties;

    /** update one access satellite to transmit data from backup group in each transmission slot*/
    public HashMap<DTNHost, DTNHost> allocatedAccessSatelliteForEachUser = new HashMap<DTNHost, DTNHost>();
    /** Key: User Host, Value: Allocated satellites in backup group for each terrestrial user and their weight */
    public HashMap<DTNHost, ArrayList<Tuple<DTNHost, Double>>> allocatedBackupGroupForEachUser =
            new HashMap<DTNHost, ArrayList<Tuple<DTNHost, Double>>>();
    /** Key: Satellite Host, Value: Allocated users accessed through this satellite */
    public HashMap<DTNHost, List<DTNHost>> allocatedAccessUsers = new HashMap<DTNHost, List<DTNHost>>();

    /** Key: User Host, Value: handover count **/
    public static HashMap<DTNHost, Integer> handoverCount = new HashMap<DTNHost, Integer>();
    /** Key: User Host, Value: last user's access node **/
    public HashMap<DTNHost, DTNHost> lastAccessNode = new HashMap<DTNHost, DTNHost>();

    private static String channelModelType;
    private String transmissionMode;
    private String handoverMode;
    private int nrofBackupSatellites = 1;
    private int nrofRobustBackupSatellite = 0;
    private double transmitRange = 0;
    private double minElevationAngle;
    private double earthRadius = 6371;//km;
    private double LEO_radius;
    private int worldSize[];
    private int endTime;
    private double SNR_threshold;
    private boolean fastMode = false;
    private boolean parameterInitializationLabel = false;

    public static double updateInterval = new Settings(DTNSim.USERSETTINGNAME_S).getDouble(DTNSim.ACCESS_SAT_UPDATEINTERVAL);//100ms
    public static double lastAccessUserUpdateTime = 0 - updateInterval + 1;
    private double lastConnectionDurationUpdateTime = 0;
    private double connectionDurationUpdateInterval = 10;
    private int countInSlidingWindow = 0;
    /**record user's current sending message id*/
    private int currentSendingMessageID = 0;

    public static double accumulativeHandoverDelay = 0;
    private static double handoverDelay = -1;
    private List<DTNHost> accessUsers = new ArrayList<DTNHost>();
    /** Key: satellite which can establish connection with the typical user  Value: Connection break time point **/
    private HashMap<DTNHost, Double> connectionDurationRecord = new HashMap<DTNHost, Double>();
    /** Key: terrestrial user -- DTNHost
     * Value: satellite nodes in backup group and their history channel status information */
    public static HashMap<DTNHost, HashMap<DTNHost, Double>> SlidingWindowRecord = new HashMap<DTNHost, HashMap<DTNHost, Double>>();
    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public RelayRouterforInternetAccess(Settings s) {
        super(s);
    }

    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected RelayRouterforInternetAccess(RelayRouterforInternetAccess r) {
        super(r);
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        int recvCheck = super.checkReceiving(m, from);

        if (recvCheck == RCV_OK) {
            /* don't accept a message that has already traversed this node */
            if (m.getHops().contains(getHost())) {
                recvCheck = DENIED_OLD;
            }
        }

        return recvCheck;
    }

    /**
     * read simulation parameters in setting.txt
     */
    public void settingParameterInitialization(){
        Settings userSetting = new Settings(USERSETTINGNAME_S);
        //enable of backup satellites or not
        transmissionMode = userSetting.getSetting(DTNSim.TRANSMISSION_MODE);
        //read the setting of number of satellites in each backup group
        if (transmissionMode.contains(DTNSim.SINR_HANDOVER)){
            //the number of backup satellites still needed for SINR handover, just used for limitation of next satellite selection, so it can be very large
            nrofBackupSatellites = this.findDedicatedHosts(DTNSim.SAT).size()/16;
            nrofRobustBackupSatellite = 0;
            SNR_threshold = userSetting.getDouble(DTNSim.HANDOVER_SNR_THRESHOLD);
        }
        else {
            nrofBackupSatellites = userSetting.getInt(DTNSim.NROF_BACKUPSATELLITES);//upper limits of backup satellites
            nrofRobustBackupSatellite = userSetting.getInt(DTNSim.NROF_ROBUSTBACKUPSATELLITE);
        }
        channelModelType = userSetting.getSetting(DTNSim.CHANNEL_MODEL);

        Settings interfaceSetting = new Settings(DTNSim.INTERFACE);
        transmitRange = interfaceSetting.getDouble(DTNSim.TRANSMIT_RANGE);

        handoverMode = userSetting.getSetting(DTNSim.HANDOVER_CRITERION);
        //read the setting of minimum elevation angle
        if (handoverMode.contains(DTNSim.MIN_ELEVATIONANGLE))
            minElevationAngle = userSetting.getDouble(DTNSim.MIN_ELEVATIONANGLE);
        fastMode = userSetting.getBoolean(DTNSim.FASTMODE);

        Settings groupSetting = new Settings(DTNSim.GROUP);
        LEO_radius = groupSetting.getDouble(DTNSim.LEO_RADIUS);

        Settings movementModelSetting = new Settings("MovementModel");
        updateWorldSize(movementModelSetting.getCsvInts("worldSize"));

        Settings s = new Settings(DTNSim.SCENARIO);
        endTime = s.getInt(DTNSim.ENDTIME);// simulation end time

        this.parameterInitializationLabel = true;
    }
    @Override
    public void update() {
        super.update();

        //read simulation parameters in setting.txt
        if (!parameterInitializationLabel){
            settingParameterInitialization();
        }

        //TODO skip update if this node is far away from the dedicated user
        //if (!distanceJudge(this.getHost(), findDedicatedHosts(DTNSim.USER).get(0)))
        //    return;

        //for (Connection con : this.sendingConnections){
        //    System.out.println(con.toString()+"  "+con.getRemainingByteCount());
        //}

        //allow the same satellite node to send multiple messages through different connections simultaneously
        //if (!canStartTransfer()) {
        //  return;
        //}

        String type = this.getHost().toString();
        //System.out.println("test 000  "+ this.getHost().toString()+"  "+(String)this.getProperty(DTNSim.NODE_TYPE));
        //String type = (String)this.getProperty(DTNSim.NODE_TYPE);//TODO
        if (type == null) {
            this.addProperty(DTNSim.NODE_TYPE, this.getHost().toString());//TODO should be done in SimScenario.java
            type = this.getHost().toString();
        }

        // used by shadowing decision, in VBRConnectionWithChannelModel.java
        if (channelModelType.contains(DTNSim.SHADOWING)){
            updateRelativeLocation();
        }

        /** 1.User only needs to receive messages from satellites **/
        if (type.contains(DTNSim.USER)) {
            return;
        }

        /** 2.Ground Station is in charge of centralized data updating **/
        if (type.contains(DTNSim.GS)) {
            switch (transmissionMode) {
                case DTNSim.ENABLE_BACKUPSATELLITE: {
                    //update access node and backup group
                    if (this.lastAccessUserUpdateTime + this.updateInterval < SimClock.getTime()) {
                        //update backup group for each terrestrial user
                        updateBackupGroup();
                        //update users served by this satellite node in each time interval
                        updateAccessSatellite();

                        updateAccessUsers();
                        //TODO forward signal control message to satellites
                        //tryControlMessagesToSatellites();
                        this.lastAccessUserUpdateTime = SimClock.getTime();

                        //System.out.println("backup group:  " + nrofBackupSatellites + "  " + this.allocatedBackupGroupForEachUser + " time " + SimClock.getTime());
                        //System.out.println("access node: " + this.allocatedAccessSatelliteForEachUser + " time " + SimClock.getTime());
                    }
                    tryMessagesToBackupSatellites();
                    break;
                }
                case DTNSim.PREMIGRATION_HANDOVER: {
                    HashMap<DTNHost, Integer> storedMessages = updatePreMigrationHandover();

                    updateAccessUsers();

                    tryMessagesToMigratedSatellites(storedMessages);
                    break;
                }
                case DTNSim.NORMAL_HANDOVER:{
                    //judgement criterion is in isReachable()
                    if (!updateNormalHandoverProcess())
                        return;

                    updateAccessUsers();
                    //System.out.println("backup group:  "+nrofBackupSatellites+"  "+this.allocatedBackupGroupForEachUser+" time "+SimClock.getTime());
                    //System.out.println("access node: "+this.allocatedAccessSatelliteForEachUser+" time "+SimClock.getTime());

                    tryMessagesToBackupSatellites();
                    break;
                }
                case DTNSim.SINR_HANDOVER:{
                    //update access node and backup group
                    if (this.lastAccessUserUpdateTime + this.updateInterval < SimClock.getTime()) {
                        //update backup group for each terrestrial user
                        updateBackupGroup();

                        //update users served by this satellite node in each time interval
                        updateAccessSatellite();

                        updateAccessUsers();
                        //TODO forward signal control message to satellites
                        //tryControlMessagesToSatellites();
                        this.lastAccessUserUpdateTime = SimClock.getTime();

                        //System.out.println("backup group:  " + nrofBackupSatellites + "  " + this.allocatedBackupGroupForEachUser + " time " + SimClock.getTime());
                        //System.out.println("access node: " + this.allocatedAccessSatelliteForEachUser + " time " + SimClock.getTime());
                    }
                    tryMessagesToAccessSatellite();

                    //if handover happens, delete all messages from old satellites
                    List<DTNHost> accessNodes = new ArrayList<DTNHost>();
                    for (DTNHost user : this.allocatedAccessSatelliteForEachUser.keySet()){
                        accessNodes.add(this.allocatedAccessSatelliteForEachUser.get(user));
                    }
                    for (DTNHost sat : findDedicatedHosts(DTNSim.SAT)){
                        if (!accessNodes.contains(sat)){
                            ((RelayRouterforInternetAccess)sat.getRouter()).clearMessages();
                        }
                    }

                    break;
                }
            }
                /** update user's current sending message id to ensure satellite
                 can send message to user one by one */
                updateUserCurrentSendingMessageID();
                handoverCount();
                handoverDelay();
        }

        //skip the update of useless satellites, accelerate the running speed of simulator
        if (fastMode && !type.contains(DTNSim.GS)){
            boolean reachable = false;
            for (DTNHost user: findDedicatedHosts(DTNSim.USER)){
                if (distanceJudge(user, this.getHost()))
                    reachable = true;
            }
            if (!reachable) {
                return;
            }
        }

        /** 3.For satellite nodes, to forward data messages to terrestrial users **/
        if (type.contains(DTNSim.SAT)) {
            updateSlidingWindow(this.getHost());
            messageQueueManagement(this);

            if (!this.accessUsers.isEmpty()) {
                //try to send messages to users
                //tryMessagesToTerrestrialUsers(this.accessUsers);//TODO this function can not transfer message
                exchangeDeliverableMessages();
            }
            //if (exchangeDeliverableMessages() != null) {
            //    return;
            //}
        }
    }

    /**
     * manage backup satellite's message queue, delete unnecessary messages
     */
    private List<String> messageQueueManagement(RelayRouterforInternetAccess router){
        List<String> noNeedToSend = new ArrayList<String>();

        for (DTNHost user : findDedicatedHosts(DTNSim.USER)){
            for (Message m : router.getMessageCollection()){
                if (user.isMessgaeReceived(m)){
                    noNeedToSend.add(m.getId());
                }
            }
            for (String mID : noNeedToSend) {
                router.removeFromMessages(mID);//user already received the message, then delete it from ground station
            }
        }
        return noNeedToSend;
    }
    /**
     * Exchanges deliverable (to final recipient) messages between this host
     * and all hosts this host is currently connected to. First all messages
     * from this host are checked and then all other hosts are asked for
     * messages to this host. If a transfer is started, the search ends.
     * @return A connection that started a transfer or null if no transfer
     * was started
     */
    @Override
    protected Connection exchangeDeliverableMessages() {
        List<Connection> connections = getConnections();
        if (connections.size() == 0) {
            return null;
        }

        @SuppressWarnings(value = "unchecked")
        Tuple<Message, Connection> t =
                tryMessagesForConnected(sortByQueueMode(getMessagesForConnected()));

        if (t != null) {
            return t.getValue(); // started transfer
        }

        // didn't start transfer to any node -> ask messages from connected
        for (Connection con : connections) {
            //TODO System.out.println("ActiveRouter.java test for connections:" + con.getLinkType());
            if (con.getOtherNode(getHost()).requestDeliverableMessages(con)) {
                return con;
            }
        }

        return null;
    }

    /**
     * resolve meeeage's id, transform string type id to integer type id
     * @param m
     * @return
     */
    public int resolveMessageID(Message m){
        //String id = m.getId();
        String id = m.getId().substring("RadioM".length());
        int ID = Integer.parseInt(id);

        return ID;
    }

    /**
     * update user's current sending message id to ensure satellite
     * can send message to user one by one
     */
    private void updateUserCurrentSendingMessageID(){
        for (DTNHost user: findDedicatedHosts(DTNSim.USER)){
            int maximumID = ((RelayRouterforInternetAccess)user.getRouter()).getCurrentSendingMessageID();
            for (Message m : user.getRouter().getDeliveredMessageCollection()){
                int id = resolveMessageID(m);

                if (id > maximumID)
                    maximumID = id;
            }
            ((RelayRouterforInternetAccess)user.getRouter()).setCurrentSendingMessageID(maximumID);
        }
    }

    /**
     * record user's current sending message id
     * @param maximumID
     */
    public void setCurrentSendingMessageID(int maximumID){
        currentSendingMessageID = maximumID;
    }

    /**
     * get user's current sending message id to ensure satellite
     * can send message to user one by one
     * @return
     */
    public int getCurrentSendingMessageID(){
        return currentSendingMessageID;
    }

    /**
     * Tries to send messages for the connections that are mentioned
     * in the Tuples in the order they are in the list until one of
     * the connections starts transferring or all tuples have been tried.
     * @param tuples The tuples to try
     * @return The tuple whose connection accepted the message or null if
     * none of the connections accepted the message that was meant for them.
     */
    @Override
    protected Tuple<Message, Connection> tryMessagesForConnected(
            List<Tuple<Message, Connection>> tuples) {
        if (tuples.size() == 0) {
            return null;
        }

        for (Tuple<Message, Connection> t : tuples) {
            Message m = t.getKey();

            /*only send message one by one, avoid out of order*/
            int id = resolveMessageID(m);
            DTNHost user = m.getTo();
            int maximumID = ((RelayRouterforInternetAccess)user.getRouter()).getCurrentSendingMessageID();
            if (id != maximumID + 1 && id != 1){
                //M1 should be always transmitted as the first message
                continue;
            }

            Connection con = t.getValue();
            if (startTransfer(m, con) == RCV_OK) {
                return t;
            }
        }
        return null;
    }

    /**
     * Returns a list of message-connections tuples of the messages whose
     * recipient is some host that we're connected to at the moment.
     * @return a list of message-connections tuples
     */
    @Override
    protected List<Tuple<Message, Connection>> getMessagesForConnected() {
        if (getNrofMessages() == 0 || getConnections().size() == 0) {
            /* no messages -> empty list */
            return new ArrayList<Tuple<Message, Connection>>(0);
        }

        List<Tuple<Message, Connection>> forTuples =
                new ArrayList<Tuple<Message, Connection>>();
        for (Message m : getMessageCollection()) {
            //TODO //if the message has been sent to the destination, don't try to send it again
            DTNHost destination = m.getTo();
            if (destination.isMessgaeReceived(m))
                continue;

            for (Connection con : getConnections()) {
                DTNHost to = con.getOtherNode(getHost());
                if (m.getTo() == to) {
                    forTuples.add(new Tuple<Message, Connection>(m,con));
                }
            }
        }

        return forTuples;
    }
    private static int Testcount = 0;
    /**
     * calculate handover delay when handover happens
     */
    public void handoverDelay(){
        for (DTNHost user: findDedicatedHosts(DTNSim.USER)) {
            DTNHost accessNode = this.allocatedAccessSatelliteForEachUser.get(user);
            if (accessNode == null) {
                if (this.handoverDelay >= 0)
                    this.handoverDelay = SimClock.getTime();
                //System.out.println("test access node:  " +Testcount++);
                return;
            }

            RelayRouterforInternetAccess router;
            if (accessNode.getRouter() instanceof RelayRouterforInternetAccess)
                 router = ((RelayRouterforInternetAccess)accessNode.getRouter());
            else
                return;

            int maximumID = ((RelayRouterforInternetAccess)user.getRouter()).getCurrentSendingMessageID();
            String nextMessage = "RadioM" + Integer.toString(maximumID + 1);
            //if (!router.getDedicatedMessages(user).isEmpty() && this.handoverDelay >= 0){
            if (router.getMessage(nextMessage) != null && this.handoverDelay >= 0){
                this.accumulativeHandoverDelay = this.accumulativeHandoverDelay + SimClock.getTime() - this.handoverDelay;
                this.handoverDelay = -1;//clear
            }
            //System.out.println(this.getHost()+"  "+router.messages + "  " + nextMessage + "  " + router.getMessage(nextMessage));
            //System.out.println("handover delay:  "+this.handoverDelay+" accumulative Delay:  "+this.accumulativeHandoverDelay+" time "+SimClock.getTime());
        }
    }
    /**
     * count when handover happens
     */
    public void handoverCount(){
        for (DTNHost h : findDedicatedHosts(DTNSim.USER)){
            if (!this.allocatedAccessSatelliteForEachUser.containsKey(h))
                continue;
            if (this.handoverCount.containsKey(h)){
                DTNHost lastAccessNode = this.lastAccessNode.get(h);
                if (lastAccessNode != this.allocatedAccessSatelliteForEachUser.get(h)){
                    int newCount = this.handoverCount.get(h) + 1;
                    this.handoverCount.remove(h);
                    this.handoverCount.put(h, newCount);
                    this.lastAccessNode.remove(h);
                    this.lastAccessNode.put(h, this.allocatedAccessSatelliteForEachUser.get(h));//update access node

                    /**
                     * once handover happens, the buffer of old satellite will be eliminated
                     *  but only for traditional scheme, for backup-enabled scheme, buffer
                     *  shouldn't be eliminated
                     */
                    if (!transmissionMode.contains(DTNSim.ENABLE_BACKUPSATELLITE) && this.findBackupGroup(h).contains(this.getHost())) {
                        ((RelayRouterforInternetAccess) this.lastAccessNode.get(h).getRouter()).clearMessages();
                    }
                    handoverDelay = SimClock.getTime();
                }
            }
            else{
                this.lastAccessNode.put(h, this.allocatedAccessSatelliteForEachUser.get(h));
                this.handoverCount.put(h, 0);
            }
            //System.out.println("handover count:  "+this.handoverCount+" time "+SimClock.getTime());
        }
    }

    /**
     * clear messages collection
     */
    public void clearMessages(){
        this.messages = new HashMap<String, Message>();
    }
    /**
     * executed in each sliding window
     */
    public void updateAccessUsers(){
        clearAccessUsers();

        for (DTNHost h : findDedicatedHosts(DTNSim.USER)){
            DTNHost sat = this.allocatedAccessSatelliteForEachUser.get(h);
            ((RelayRouterforInternetAccess)sat.getRouter()).addAccessUsers(h);
        }
    }
    /**
     * add element in AccessUsers
     * @param accessUser
     */
    public void addAccessUsers(DTNHost accessUser){
        this.accessUsers.add(accessUser);
    }
    /**
     * clear all elements in AccessUsers
     */
    public void clearAccessUsers(){
        for (DTNHost h : findDedicatedHosts(DTNSim.SAT)) {
            ((RelayRouterforInternetAccess)h.getRouter()).accessUsers.clear();
        }
        //this.accessUsers.clear();
    }
    /**
     * calculate the distance between two DTNHost
     * @param a
     * @param b
     * @return
     */
    public double calculateDistance(DTNHost a, DTNHost b){
        Coord A = a.getLocation();
        Coord B = b.getLocation();
        return A.distance(B);
    }
    /**
     * only for ground station since ground station is in charge of
     * centralized status collecting and updating
     * */
    protected void updateSlidingWindow(DTNHost satellite){
        // warning:
        // updateChannelModelForInterface() must be executed before updateSlidingWindow()
        //String channelModel = s.getSetting(DTNSim.CHANNEL_MODEL);
        //String channelModel = RICE;

        //TODO
        this.countInSlidingWindow++;

        int firstInterface = 1;//not 0
        for (DTNHost h : findDedicatedHosts(DTNSim.USER)){        // find terrestrial user list
            List<DTNHost> satellitesInBackupGroup = findBackupGroup(h);
            HashMap<DTNHost, Double> StatusInBackupGroup = this.SlidingWindowRecord.get(h);
            //TODO channel status update
            Double currentCapacity = ((SatelliteWithChannelModelInterface)satellite.getInterface(firstInterface)).
                    getCurrentChannelStatus(h);
            if (currentCapacity == null) {
                return;
            }

            double history = 0;
            if (StatusInBackupGroup != null) {
                if (StatusInBackupGroup.get(satellite) != null)
                    history = StatusInBackupGroup.get(satellite);
            }
            else
                StatusInBackupGroup = new HashMap<DTNHost, Double>();

            //update history information
            StatusInBackupGroup.put(satellite,(history*(countInSlidingWindow - 1) + currentCapacity)/countInSlidingWindow);
            this.SlidingWindowRecord.put(h, StatusInBackupGroup);
        }
    }

    /**
     * find backup group for dedicated terrestrial user
     * @param user
     * @return
     */
    protected List<DTNHost> findBackupGroup(DTNHost user){
        if (this.allocatedBackupGroupForEachUser.containsKey(user)) {
            List<DTNHost> backupGroup = new ArrayList<DTNHost>();
            for (Tuple<DTNHost, Double> t : allocatedBackupGroupForEachUser.get(user))
                backupGroup.add(t.getKey());
            return backupGroup;
        }
        else
            return null;
    }

    /**
     * find all dedicated property hosts (terrestrial user or satellites)
     * in DTNHost list
     * @return terrestrial user list
     */
    protected List<DTNHost> findDedicatedHosts(String type){
        List<DTNHost> dedicatedHosts = new ArrayList<DTNHost>();
        for (DTNHost h : this.getHost().getHostsList()){

            String getType = h.toString();
            if (getType.contains(type))
                dedicatedHosts.add(h);
        }
        //number check
        if (dedicatedHosts.isEmpty())
            throw new SimError("there is no " + type + " in the setting " +
                    "(RelayRouterforInternetAccess.java)");

        return dedicatedHosts;
    }

    /**
     * Single satellite access scenario, handover process between terrestrial
     * user and satellite, but enable pre-migration of user data
     * @return
     */
    protected HashMap<DTNHost, Integer> updatePreMigrationHandover(){
        HashMap<DTNHost, Integer> storedMessages = new HashMap<DTNHost, Integer>();
        for (DTNHost user : findDedicatedHosts(DTNSim.USER)){
            DTNHost oldAccess = null;
            if (this.allocatedAccessSatelliteForEachUser.containsKey(user)) {
                oldAccess = this.allocatedAccessSatelliteForEachUser.get(user);
            }
            updateNormalHandoverProcess();
            DTNHost newAccess = this.allocatedAccessSatelliteForEachUser.get(user);
            if (oldAccess != null){
                if (oldAccess != newAccess){// start data migration
                    //tryMessagesToBackupSatellites();
                    tryMessageMigration(oldAccess, newAccess);

                    //TODO
                    double handoverTime = SimClock.getTime() + updateInterval/2;
                    Tuple<DTNHost, Double> delayHandoverOrder = new Tuple<DTNHost, Double>(newAccess, handoverTime);
                }
            }

            if (SimClock.getTime() < 1.1)
                continue;
            //find the max stored message id
            int max = Integer.MIN_VALUE;
            for (Message m : oldAccess.getMessageCollection()){
                max = resolveMessageID(m) > max ?
                        resolveMessageID(m) : max;
            }
            storedMessages.put(user, max);
        }


        return storedMessages;
    }
    /**
     * Single satellite access scenario, handover process between terrestrial
     * user and satellite
     * @return
     */
    protected boolean updateNormalHandoverProcess(){
        ArrayList<Tuple<DTNHost, Double>> group = new ArrayList<Tuple<DTNHost, Double>>();

        /** calculate each reachable satellite's weight (connection time or elevation angle) */
        for (DTNHost user: findDedicatedHosts(DTNSim.USER)){
            for (DTNHost satellite: findDedicatedHosts(DTNSim.SAT)){
                Tuple<Boolean, Double> isReachable = isReachable(user, satellite);
                if (isReachable.getKey())
                    group.add(new Tuple<DTNHost, Double>(satellite, isReachable.getValue()));
            }
            group = (ArrayList<Tuple<DTNHost, Double>>) sort(group);
            //System.out.println(" group:  "+group+" time "+SimClock.getTime());

            if (group.size() <= 0)
                return false;

            int firstChoice = 0;
            boolean maximumConnection = false;

            switch (handoverMode){
                case "maximumConnectionDuration": {
                    //sort function: output list from minimum value to maximum value
                    firstChoice = group.size() - 1;
                    maximumConnection = true;
                    break;
                }
                default: {
                    firstChoice = 0;
                }
            }

            if (this.allocatedBackupGroupForEachUser.containsKey(user)) {
                if (maximumConnection){
                    Coord A = user.getLocation();// user node
                    Coord B = this.allocatedBackupGroupForEachUser.get(user).get(0).getKey().getLocation();// last access node
                    double distance = A.distance(B);
                    if (distance <= transmitRange) {
                        continue;// for maximum connection scheme, access node doesn't need to update in each handover window if current satellite is accessible
                    }
                }
                this.allocatedBackupGroupForEachUser.remove(user);// be careful of memory lead problem
                ArrayList<Tuple<DTNHost, Double>> newList = new ArrayList<Tuple<DTNHost, Double>>();
                newList.add(group.get(firstChoice));
                this.allocatedBackupGroupForEachUser.put(user, newList);
            }
            else{
                ArrayList<Tuple<DTNHost, Double>> accessNode = new ArrayList<Tuple<DTNHost, Double>>();
                accessNode.add(group.get(firstChoice));
                this.allocatedBackupGroupForEachUser.put(user, accessNode);
            }
            this.allocatedAccessSatelliteForEachUser.remove(user);
            this.allocatedAccessSatelliteForEachUser.put(user, group.get(firstChoice).getKey());
        }
        return true;
    }

    /**
     * update elevation angle or distance info in connection,
     * used by shadowing decision
     */
    public void updateRelativeLocation(){
        for (DTNHost user : findDedicatedHosts(DTNSim.USER)){
            ArrayList<Tuple<DTNHost, Double>> backupGroupAngle = new ArrayList<Tuple<DTNHost, Double>>();
            ArrayList<Tuple<DTNHost, Double>> backupGroupConnection = new ArrayList<Tuple<DTNHost, Double>>();
            Tuple<DTNHost, Double> currentAccessNode = new Tuple<DTNHost, Double>(user, -1.0);
            //find all reachable satellites
            for (DTNHost satellite: findDedicatedHosts(DTNSim.SAT)){
                Tuple<Boolean, Double> isReachable = isBackupSatReachable(user, satellite, DTNSim.MIN_ELEVATIONANGLE);
                if (isReachable.getKey()) {
                    backupGroupAngle.add(new Tuple<DTNHost, Double>(satellite, isReachable.getValue()));
                }
            }
            backupGroupAngle = (ArrayList<Tuple<DTNHost, Double>>) sort(backupGroupAngle);
            // update elevation angle or distance info in connection, for shadowing decision
            VBRConnectionWithChannelModel.updateRelativeLocation(user, backupGroupAngle);
        }
    }
    /**
     * Ground station takes charge of backup group calculation
     */
    protected void updateBackupGroup(){
        if (nrofRobustBackupSatellite > nrofBackupSatellites){
            throw new SimError(" nrofRobustBackupSatellite exceeds nrofBackupSatellites!");
        }

        //update satellite weight for each user as backup satellite
        for (DTNHost user: findDedicatedHosts(DTNSim.USER)){
            ArrayList<Tuple<DTNHost, Double>> backupGroupAngle = new ArrayList<Tuple<DTNHost, Double>>();
            ArrayList<Tuple<DTNHost, Double>> backupGroupConnection = new ArrayList<Tuple<DTNHost, Double>>();
            Tuple<DTNHost, Double> currentAccessNode = new Tuple<DTNHost, Double>(user, -1.0);
            //at first, find all reachable satellites
            for (DTNHost satellite: findDedicatedHosts(DTNSim.SAT)){
                Tuple<Boolean, Double> isReachable = isBackupSatReachable(user, satellite, DTNSim.MIN_ELEVATIONANGLE);
                if (isReachable.getKey()) {
                    backupGroupAngle.add(new Tuple<DTNHost, Double>(satellite, isReachable.getValue()));
                }
                if (satellite == this.allocatedAccessSatelliteForEachUser.get(user)){
                    currentAccessNode = new Tuple<DTNHost, Double>(satellite, isReachable.getValue());
                }
            }
            backupGroupAngle = (ArrayList<Tuple<DTNHost, Double>>) sort(backupGroupAngle);

            if (this.allocatedBackupGroupForEachUser.get(user) != null){
                this.allocatedBackupGroupForEachUser.remove(user);
                ArrayList<Tuple<DTNHost, Double>> newList = new ArrayList<Tuple<DTNHost, Double>>();
                newList.add(currentAccessNode);
                newList.addAll(remainTopElements(backupGroupAngle,
                        nrofBackupSatellites - nrofRobustBackupSatellite - 1, currentAccessNode.getKey()));// add 1 less for access node
                this.allocatedBackupGroupForEachUser.put(user, newList);//update backup group every time
            }
            else{
                //remove redundant backup satellites in function: remainTopElements()
                //only do this when initialization for each user
                this.allocatedBackupGroupForEachUser.put(user,  new ArrayList<Tuple<DTNHost, Double>>(remainTopElements(backupGroupAngle,
                        nrofBackupSatellites - nrofRobustBackupSatellite, null)));
            }

            if (nrofRobustBackupSatellite > 0) {
                //at first, find all reachable satellites
                for (DTNHost satellite: findDedicatedHosts(DTNSim.SAT)){
                    Tuple<Boolean, Double> isReachable = isBackupSatReachable(user, satellite, DTNSim.MAX_CONNECTIONDURATION);
                    if (isReachable.getKey()) {
                        backupGroupConnection.add(new Tuple<DTNHost, Double>(satellite, isReachable.getValue()));
                    }
                }
                backupGroupConnection = (ArrayList<Tuple<DTNHost, Double>>) sort(backupGroupConnection);
                //remainTopElements(backupGroupConnection, nrofRobustBackupSatellite);

                //possible error check
                if (this.allocatedBackupGroupForEachUser.get(user) == null){
                    throw new SimError("code flow error in RelayRouterforInternetAccess.java!");
                }

                // check first, avoid repetition
                ArrayList<Tuple<DTNHost, Double>> needToAdd = new ArrayList<Tuple<DTNHost, Double>>();
                for (int index = 0; index < backupGroupConnection.size() - 1; index++){
                    DTNHost sat = backupGroupConnection.get(index).getKey();
                    boolean containLabel = false;
                    for (Tuple<DTNHost, Double> t : this.allocatedBackupGroupForEachUser.get(user)){
                        if (t.getKey() == sat)
                            containLabel = true;
                    }
                    if (!containLabel) {
                        needToAdd.add(new Tuple<DTNHost, Double>(sat, backupGroupConnection.get(index).getValue()));
                        if (needToAdd.size() >= nrofRobustBackupSatellite){
                            break;
                        }
                    }
                }
                // add robust backup satellite according connection time criterion
                ArrayList<Tuple<DTNHost, Double>> nextList = new ArrayList<Tuple<DTNHost, Double>>();
                nextList.addAll(this.allocatedBackupGroupForEachUser.get(user));//copy original list
                nextList.addAll(needToAdd);//add new element to the list
                this.allocatedBackupGroupForEachUser.remove(user);//release memory
                this.allocatedBackupGroupForEachUser.put(user, nextList);
            }
        }
    }

    /**
     * remian top elements from a list (must be sort first)
     * @param list
     * @return
     */
    public List remainTopElements(List<Tuple<DTNHost, Double>> list, int nrofElements, DTNHost noNeedToRemain){
        if (nrofElements > list.size())
            throw new SimError("number of remain elements exceeds the number of list length");

        List newList = new ArrayList<Tuple<DTNHost, Double>>();
        for (int i = 0; i < list.size(); i++){
            if (noNeedToRemain == null)
                newList.add(list.get(i));
            else{
                if (list.get(i).getKey() != noNeedToRemain)
                    newList.add(list.get(i));
            }
            if (newList.size() >= nrofElements)
                return newList;
        }
        return newList;
    }
    /**
     * Ground station takes charge of backup group management,
     * and also allocate satellite's buffer resource for each user
     */
    private void bufferManager(){
        //TODO
    }

    /**
     * judge the distance of two nodes exceeds the transmission  range or not
     * @param a
     * @param b
     * @return
     */
    protected boolean distanceJudge(DTNHost a, DTNHost b){
        Coord A = a.getLocation();
        Coord B = b.getLocation();
        double distance = A.distance(B);

        if (distance <= transmitRange)
            return true;
        else
            return false;
    }
    /**
     *
     * @param a should be user on the earth
     * @param b should be satellite outer the earth, which radius should be greater than satellite a
     * @return
     */
    protected Tuple<Boolean, Double> isReachable(DTNHost a, DTNHost b){

        Coord A = a.getLocation();
        Coord B = b.getLocation();
        double distance = A.distance(B);

        double dx = A.getX() - B.getX();
        double dy = A.getY() - B.getY();
        double dz = A.getZ() - B.getZ();
        // the calculation formula can be found in:
        // https://www.tutorialspoint.com/satellite_communication/satellite_communication_look_angles_orbital_perturbations.htm
        // also in: http://tiij.org/issues/issues/3_2/3_2e.html
        // and the method to convert XYZ to latitude and longitude:
        // https://gis.stackexchange.com/questions/120679/equations-to-convert-from-global-cartesian-coordinates-to-geographic-coordinates
        /*
        double userLatitude = Math.asin(A.getZ()/earthRadius)*(180/Math.PI);

        double userLongitude = A.getX() > 0 ?
                Math.atan(A.getY()/A.getX())*(180/Math.PI): (A.getY() > 0 ?
                        Math.atan(A.getY()/A.getX())*(180/Math.PI) + 180: Math.atan(A.getY()/A.getX())*(180/Math.PI) - 180);
        double satelliteLongitude = B.getX() > 0 ?
                Math.atan(B.getY()/B.getX())*(180/Math.PI): (B.getY() > 0 ?
                Math.atan(B.getY()/B.getX())*(180/Math.PI) + 180: Math.atan(B.getY()/B.getX())*(180/Math.PI) - 180);
        double G = satelliteLongitude - userLongitude;
        double L = userLatitude;


        double elevationAngle = Math.atan((Math.cos(G)*Math.cos(L) - 0.15)/
                (Math.sqrt(1 - Math.pow(Math.cos(G), 2)*Math.pow((Math.cos(L)), 2)) ) );
        */
        double elevationAngle = calculateElevationAngle(distance, earthRadius, LEO_radius, A);

        switch (handoverMode){
            case "maximumConnectionDuration": {
                //.out.println("connection duration reachable");
                if (elevationAngle > minElevationAngle && distance <= transmitRange)
                    return isReachable_LongestConnectionDuration(a, b, new Tuple<Boolean, Double>(true, distance));
                else
                    return isReachable_LongestConnectionDuration(a, b, new Tuple<Boolean, Double>(false, distance));
            }
            default: {// minimumElevationAngle
                if (elevationAngle > minElevationAngle && distance <= transmitRange)
                    //if (distance <= transmitRange)
                    //return new Tuple<Boolean, Double>(true, elevationAngle);
                    return new Tuple<Boolean, Double>(true, distance);
                else
                    //return  new Tuple<Boolean, Double>(false, elevationAngle);
                    return new Tuple<Boolean, Double>(false, distance);
            }
        }
    }
    /**
     *
     * @param a should be user on the earth
     * @param b should be satellite outer the earth, which radius should be greater than satellite a
     * @return
     */
    protected Tuple<Boolean, Double> isBackupSatReachable(DTNHost a, DTNHost b, String criterion){
        Coord A = a.getLocation();
        Coord B = b.getLocation();
        double distance = A.distance(B);

        double dx = A.getX() - B.getX();
        double dy = A.getY() - B.getY();
        double dz = A.getZ() - B.getZ();
        // the calculation formula can be found in:
        // https://www.tutorialspoint.com/satellite_communication/satellite_communication_look_angles_orbital_perturbations.htm
        // also in: http://tiij.org/issues/issues/3_2/3_2e.html
        // and the method to convert XYZ to latitude and longitude:
        // https://gis.stackexchange.com/questions/120679/equations-to-convert-from-global-cartesian-coordinates-to-geographic-coordinates
        /*
        double userLatitude = Math.asin(A.getZ()/earthRadius)*(180/Math.PI);

        double userLongitude = A.getX() > 0 ?
                Math.atan(A.getY()/A.getX())*(180/Math.PI): (A.getY() > 0 ?
                        Math.atan(A.getY()/A.getX())*(180/Math.PI) + 180: Math.atan(A.getY()/A.getX())*(180/Math.PI) - 180);
        double satelliteLongitude = B.getX() > 0 ?
                Math.atan(B.getY()/B.getX())*(180/Math.PI): (B.getY() > 0 ?
                Math.atan(B.getY()/B.getX())*(180/Math.PI) + 180: Math.atan(B.getY()/B.getX())*(180/Math.PI) - 180);
        double G = satelliteLongitude - userLongitude;
        double L = userLatitude;


        double elevationAngle = Math.atan((Math.cos(G)*Math.cos(L) - 0.15)/
                (Math.sqrt(1 - Math.pow(Math.cos(G), 2)*Math.pow((Math.cos(L)), 2)) ) );
        */
        double elevationAngle = calculateElevationAngle(distance, earthRadius, LEO_radius, A);

        switch (criterion){
            case "maximumConnectionDuration": {
                if (elevationAngle > minElevationAngle && distance <= transmitRange)
                    return isReachable_LongestConnectionDuration(a, b, new Tuple<Boolean, Double>(true, distance));
                else
                    return isReachable_LongestConnectionDuration(a, b, new Tuple<Boolean, Double>(false, distance));
            }
            default: {// minimumElevationAngle
                if (elevationAngle > minElevationAngle && distance <= transmitRange)
                    //if (distance <= transmitRange)
                    //return new Tuple<Boolean, Double>(true, elevationAngle);
                    return new Tuple<Boolean, Double>(true, distance);
                else
                    //return  new Tuple<Boolean, Double>(false, elevationAngle);
                    return new Tuple<Boolean, Double>(false, distance);
            }
        }
    }
    /**
     * update world size parameter
     * @param worldSize
     */
    public void updateWorldSize(int worldSize[]){
        this.worldSize = worldSize;
    }
    /**
     * calculate elevation angle for dedicated node
     * @param distance
     * @param earthRadius
     * @param LEO_radius
     * @param userLocation
     * @return
     */
    public double calculateElevationAngle(double distance, double earthRadius, double LEO_radius, Coord userLocation){
        Coord centerOfEarth = new Coord(worldSize[0] / 2, worldSize[0] / 2, worldSize[0] / 2);

        double userToEarchDistance = userLocation.distance(centerOfEarth);

        double La = distance;
        double Lb = earthRadius+LEO_radius;
        double Lc = userLocation.distance(centerOfEarth);
        double test = (Math.pow(La, 2) + Math.pow(Lb, 2) - Math.pow(Lc, 2))/(2*(La*Lb));
        double elevationAngle = Math.acos((Math.pow(La, 2) + Math.pow(Lb, 2) - Math.pow(Lc, 2))/(2*(La*Lb)));
        //elevationAngle = Math.acos(((earthRadius+LEO_radius)/2)/distance);
        elevationAngle = Math.toDegrees(elevationAngle);
        //TODO
        //System.out.println(test+"  "+La+"  "+Lb+"  "+Lc+"  "+distance+"  a: "+a+"  "+a.getLocation()+" b: "+b+"  "+b.getLocation()+"  "+elevationAngle+"  "+minElevationAngle);

        return  elevationAngle;
    }
    /**
     * calculate two nodes' connection duration
     * @param a
     * @param b
     * @return
     */
    protected Tuple<Boolean, Double> isReachable_LongestConnectionDuration(DTNHost a, DTNHost b, Tuple<Boolean, Double> tuple){
        double connectionDuration = 0;
        if (tuple.getKey() == false)
            return new Tuple<Boolean, Double>(tuple.getKey(), connectionDuration);

        if (connectionDurationRecord.containsKey(b)){
            /** avoid repeat calculation */
            if (Math.abs(SimClock.getTime() - lastConnectionDurationUpdateTime) < connectionDurationUpdateInterval){
                connectionDuration = connectionDurationRecord.get(b) - SimClock.getIntTime();
            }
            else{
                connectionDuration = connectionDurationCalculation(connectionDuration, a, b);
            }
        }
        else{
            connectionDuration = connectionDurationCalculation(connectionDuration, a, b);
        }
        connectionDurationRecord.remove(b);//release memory
        connectionDurationRecord.put(b, SimClock.getIntTime() + connectionDuration);
        //System.out.println(a+"  "+b+"  maximumConnectionDuration: "+connectionDuration);
       return new Tuple<Boolean, Double>(tuple.getKey(), connectionDuration);
    }
    /**
     * calculate each satellite's connection duration to terrestrial user
     * @param connectionDuration
     * @param a
     * @param b
     * @return
     */
    public double connectionDurationCalculation(double connectionDuration, DTNHost a, DTNHost b){
        Coord A = a.getLocation();
        Coord B = b.getLocation();
        double distance = A.distance(B);

        for (double t = 0; t < endTime; t = t + 1) {
            connectionDuration = endTime;
            MovementModel movement = b.getMovementModel();
            Coord location = new Coord(0, 0);
            if (movement instanceof SatelliteMovement)
                location.setLocation3D(((SatelliteMovement) movement).getSatelliteCoordinate(SimClock.getTime() + t));
            distance = A.distance(location);
            double elevationAngle = calculateElevationAngle(distance, earthRadius, LEO_radius, A);
            if (!(elevationAngle > minElevationAngle && distance <= transmitRange)) {
                connectionDuration = t;
                break;
            }
        }
        lastConnectionDurationUpdateTime = SimClock.getTime();
        return connectionDuration;
    }
    /**
     * update access satellite for each terrestrial user according to history channel information
     */
    protected void updateAccessSatellite(){
        for (DTNHost h : findDedicatedHosts(DTNSim.USER)){
            //List<DTNHost> satellitesInBackupGroup = findBackupGroup(h);
            HashMap<DTNHost, Double> StatusInBackupGroup = this.SlidingWindowRecord.get(h);

            //initialization
            if (StatusInBackupGroup == null) {
                int firstChoice = 0;
                Tuple<DTNHost, Double> initChoice = this.allocatedBackupGroupForEachUser.get(h).get(firstChoice);
                allocatedAccessSatelliteForEachUser.put(h, initChoice.getKey());
                return;
            }

            //only for SINR handover scheme, only handover if SNR under the threshold
            if (transmissionMode.contains(DTNSim.SINR_HANDOVER)){
                DTNHost lastAccessSatellite = this.allocatedAccessSatelliteForEachUser.get(h);
                if (lastAccessSatellite != null){
                    if (StatusInBackupGroup.get(lastAccessSatellite) == null)
                        throw new SimError("SNR record information error");
                    //convert transmission rate to SNR (unit : dB)
                    double SNR = channelModel.dBcoverter(StatusInBackupGroup.get(lastAccessSatellite), false);
                    //System.out.println("SNR  "+SNR);
                    if (SNR > SNR_threshold){
                        return;
                    }
                }
            }

            //only remains the hosts in the backup groups
            List<DTNHost> backupGroups = findBackupGroup(h);
            StatusInBackupGroup.keySet().retainAll(backupGroups);
            //System.out.println(this.getHost()+"   "+StatusInBackupGroup.size()+" Sliding window--：  "+StatusInBackupGroup+" time "+SimClock.getTime());

            //format convert
            List<Tuple<DTNHost, Double>> AL = new ArrayList<Tuple<DTNHost, Double>>();
            for (DTNHost satellite: StatusInBackupGroup.keySet()) {
                AL.add(new Tuple<DTNHost, Double>(satellite, StatusInBackupGroup.get(satellite)));
            }
            //format convert
            //System.out.println(this.getHost()+" test__  "+AL+" time "+SimClock.getTime());
            //find the best choice according to history channel status
            int first = AL.size() - 1;
            Tuple<DTNHost, Double> bestChoice = sort(AL).get(first);//sort():From smallest to largest

            allocatedAccessSatelliteForEachUser.put(h, bestChoice.getKey());
        }

        //reset counter
        this.countInSlidingWindow = 0;
        this.SlidingWindowRecord.clear();
    }

    /**
     * 冒泡排序,从小到大排序，大的值放在队列右侧
     * @param distanceList
     * @return
     */
    public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList){
        if (distanceList.size() <= 1)
            return distanceList;

        for (int j = 0; j < distanceList.size(); j++){
            for (int i = 0; i < distanceList.size() - j - 1; i++){
                if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()){//
                    Tuple<DTNHost, Double> var1 = distanceList.get(i);
                    Tuple<DTNHost, Double> var2 = distanceList.get(i + 1);
                    distanceList.remove(i);
                    distanceList.remove(i);//注意，一旦执行remove之后，整个List的大小就变了，所以原本i+1的位置现在变成了i
                    //注意顺序
                    distanceList.add(i, var2);
                    distanceList.add(i + 1, var1);
                }
            }
        }
        return distanceList;
    }

    /**
     * Sorts/shuffles the given list according to the current sending queue
     * mode. The list can contain either Message or Tuple<Message, Connection>
     * objects. Other objects cause error.
     * @param list The list to sort or shuffle
     * @return The sorted/shuffled list
     */
    @Override
    @SuppressWarnings(value = "unchecked") /* ugly way to make this generic */
    protected List sortByQueueMode(List list) {
        switch (sendQueueMode) {
            case Q_MODE_RANDOM:
                Collections.shuffle(list, new Random(SimClock.getIntTime()));
                break;
            case Q_MODE_FIFO:
                /** warning: */
                /** modification method, FIFO according to message's name (e.g., M1, M2, M3...) */
                Collections.sort(list,
                        new Comparator() {
                            /** Compares two tuples by their messages' name */
                            public int compare(Object o1, Object o2) {
                                double diff;
                                Message m1, m2;

                                if (o1 instanceof Tuple) {
                                    m1 = ((Tuple<Message, Connection>)o1).getKey();
                                    m2 = ((Tuple<Message, Connection>)o2).getKey();
                                }
                                else if (o1 instanceof Message) {
                                    m1 = (Message)o1;
                                    m2 = (Message)o2;
                                }
                                else {
                                    throw new SimError("Invalid type of objects in " +
                                            "the list");
                                }

                                diff = resolveMessageID(m1) - resolveMessageID(m2);

                                if (diff == 0) {
                                    return 0;
                                }
                                return (diff < 0 ? -1 : 1);
                            }
                        });
                break;
            /* add more queue modes here */
            default:
                throw new SimError("Unknown queue mode " + sendQueueMode);
        }
        return list;
    }
    /**
     * Returns the oldest (by receive time) message in the message buffer
     * (that is not being sent if excludeMsgBeingSent is true).
     * @param excludeMsgBeingSent If true, excludes message(s) that are
     * being sent from the oldest message check (i.e. if oldest message is
     * being sent, the second oldest message is returned)
     * @return The oldest message or null if no message could be returned
     * (no messages in buffer or all messages in buffer are being sent and
     * exludeMsgBeingSent is true)
     */
    @Override
    protected Message getNextMessageToRemove(boolean excludeMsgBeingSent) {
        Collection<Message> messages = this.getMessageCollection();
        Message oldest = null;
        for (Message m : messages) {

            if (excludeMsgBeingSent && isSending(m.getId())) {
                continue; // skip the message(s) that router is sending
            }

            if (oldest == null ) {
                oldest = m;
            }
            else if (resolveMessageID(oldest) < resolveMessageID(m)) {
                oldest = m;// remove message according to message sequence (i.e., message ID)
            }
        }

        return oldest;
    }
    /**
     * find message collection to dedicated user
     * @param user
     * @return
     */
    protected List<Message> getDedicatedMessages(DTNHost user){
        List<Message> getMessages = new ArrayList<Message>();
        for (Message m : this.messages.values()){
            if (m.getTo() == user)
                getMessages.add(m);
        }
        return getMessages;
    }

    /**
     * find connections from this node to the dedicated nodes
     * @param toSatellites
     * @return
     */
    protected List<Connection> getDedicatedConnectionsToNodes(List<DTNHost> toSatellites){
        List<Connection> connectionsToNodes = new ArrayList<Connection>();
        for (Connection con : this.getConnections()){
            if (toSatellites.contains(con.getOtherNode(this.getHost())))
                connectionsToNodes.add(con);
        }
        return connectionsToNodes;
    }

    /**
     * migrate user's messages from old access satellite to new access satellite
     * @param oldHost
     * @param newHost
     * @return migration status
     */
    protected int tryMessageMigration(DTNHost oldHost, DTNHost newHost){
        //no need to migration
        if (oldHost.getRouter().getMessageCollection().isEmpty())
            return 1;

        Connection con = findConnection(newHost, oldHost.getConnections());

        ArrayList<Message> remainMesssages = new ArrayList<Message>(oldHost.getRouter().getMessageCollection());
        Message m = tryAllMessages(con, remainMesssages);
        //TODO
        if (m != null)
            return 2;
        else
            return 3;
    }
    /**
     * for ground station to use, try to send messages to backup group
     * of dedicated terrestrial user
     */
    protected void  tryMessagesToBackupSatellites() {
        for (DTNHost user : findDedicatedHosts(DTNSim.USER)){
            List<Message> getMessages = new ArrayList<Message>(getDedicatedMessages(user));//User ID
            if (getMessages.isEmpty())
                continue;

            List<DTNHost> backupGroup = findBackupGroup(user);
            if (backupGroup == null)
                return;

            List<Connection> toBackupGroupConnections =
                    getDedicatedConnectionsToNodes(backupGroup);

            //if the message has been sent to the user, don't try to send it again
            List<Message> noNeedToSend = new ArrayList<Message>();
            for (Message m : getMessages){
                if (user.isMessgaeReceived(m)) {
                    noNeedToSend.add(m);
                    this.removeFromMessages(m.getId());//user already received the message, then delete it from ground station
                }
                if (user.getRouter().isIncomingMessage(m.getId())){
                    noNeedToSend.add(m);
                }
            }
            getMessages.removeAll(noNeedToSend);

            //System.out.println("size  "+getMessages.size()+" size__  "+noNeedToSend.size());
            //TODO
            for (Connection con : toBackupGroupConnections) {
                Message started = tryAllMessages(con, sortByQueueMode(getMessages));
                //if (started != null) {
                //    return con;
                //}
            }
            //System.out.println("test sort:  "+sortByQueueMode(getMessages));

            //tryAllMessagesToAllConnections();
        }
    }
    /**
     * for ground station to use, try to send messages to current access satellite
     * of dedicated terrestrial user
     */
    protected void  tryMessagesToAccessSatellite() {
        for (DTNHost user : findDedicatedHosts(DTNSim.USER)){
            List<Message> getMessages = new ArrayList<Message>(getDedicatedMessages(user));//User ID
            if (getMessages.isEmpty())
                continue;

            List<DTNHost> backupGroup = findBackupGroup(user);
            if (backupGroup == null)
                return;

            List<Connection> toBackupGroupConnections =
                    getDedicatedConnectionsToNodes(backupGroup);

            //if the message has been sent to the user, don't try to send it again
            List<Message> noNeedToSend = new ArrayList<Message>();
            for (Message m : getMessages){
                if (user.isMessgaeReceived(m)) {
                    noNeedToSend.add(m);
                    this.removeFromMessages(m.getId());//user already received the message, then delete it from ground station
                }
                if (user.getRouter().isIncomingMessage(m.getId())){
                    noNeedToSend.add(m);
                }
            }
            getMessages.removeAll(noNeedToSend);

            //find current access node
            DTNHost accessNode = this.allocatedAccessSatelliteForEachUser.get(user);
            for (Connection con : toBackupGroupConnections) {
                if (con.getOtherNode(this.getHost()) == accessNode){
                    //only send messages to the current access node
                    Message started = tryAllMessages(con, sortByQueueMode(getMessages));
                }
            }
        }
    }
    /**
     *  Ground station try to send messages to new migrated access satellites
     */
    protected  void tryMessagesToMigratedSatellites(HashMap<DTNHost, Integer> storedMessages){
        if (storedMessages.isEmpty())
            return;

        for (DTNHost user : findDedicatedHosts(DTNSim.USER)){
            List<Message> getMessages = new ArrayList<Message>(getDedicatedMessages(user));//User ID
            if (getMessages.isEmpty())
                continue;

            List<DTNHost> backupGroup = findBackupGroup(user);
            if (backupGroup == null)
                return;

            List<Connection> toBackupGroupConnections =
                    getDedicatedConnectionsToNodes(backupGroup);

            //if the message has been sent to the user, don't try to send it again
            List<Message> noNeedToSend = new ArrayList<Message>();
            for (Message m : getMessages){
                if (user.isMessgaeReceived(m) || resolveMessageID(m) < storedMessages.get(user)) {
                    noNeedToSend.add(m);
                    this.removeFromMessages(m.getId());//user already received the message, then delete it from ground station
                }
                if (user.getRouter().isIncomingMessage(m.getId())){
                    noNeedToSend.add(m);
                }
            }
            getMessages.removeAll(noNeedToSend);

            //System.out.println("migration size  "+getMessages.size()+" size__  "+noNeedToSend.size());

            for (Connection con : toBackupGroupConnections) {
                Message started = tryAllMessages(con, sortByQueueMode(getMessages));
                //if (started != null) {
                //    return con;
                //}
            }
            //System.out.println("test sort migration:  "+sortByQueueMode(getMessages));

        }
    }

    /**
     * Goes trough the messages until the other node accepts one
     * for receiving (or doesn't accept any). If a transfer is started, the
     * connection is included in the list of sending connections.
     * @param con Connection trough which the messages are sent
     * @param messages A list of messages to try
     * @return The message whose transfer was started or null if no
     * transfer was started.
     */
    @Override
    protected Message tryAllMessages(Connection con, List<Message> messages) {
        DTNHost backupSat = con.getOtherNode(this.getHost());
        for (Message m : messages) {
            /** if relay satellite's buffer is full, don't send message */
            if (backupSat.getRouter().getFreeBufferSize() < m.getSize()) {
                return null;
            }
            int retVal = startTransfer(m, con);
            if (retVal == RCV_OK) {
                return m;	// accepted a message, don't try others
            }
            else if (retVal > 0) {
                return null; // should try later -> don't bother trying others
            }
        }
        return null; // no message was accepted
    }
    /**
     * Tries to forward stored messages that this router is carrying to all
     * connections this node has to terrestrial users. Messages are
     * ordered using the {@link MessageRouter#sortByQueueMode(List)}. See
     * {@link #tryMessagesToConnections(List, List)} for sending details.
     * @return The connections that started a transfer or null if no connection
     * accepted a message.
     */
    protected void tryMessagesToTerrestrialUsers(List<DTNHost> accessUsers){
        List<String> sendingMessagesRecord = new ArrayList<String>();
        //sort messages according to the destination host
        HashMap<DTNHost,ArrayList<Message>> sendingMessages = new HashMap<DTNHost,ArrayList<Message>>();
        List<DTNHost> destinationUsers = new ArrayList<DTNHost>();

        List<Message> messages =
                new ArrayList<Message>(this.getMessageCollection());
        this.sortByQueueMode(messages);

        int maximumSendingNrofMessages = messages.size();
        for (int i = 0;i < maximumSendingNrofMessages;i++) {
            Message m = messages.get(i);
            sendingMessagesRecord.add(m.getId());//record sending message id

            ArrayList<Message> getMessages = sendingMessages.get(m.getDestinationHost());//User ID
            destinationUsers.add(m.getDestinationHost());
            if (getMessages == null)
                getMessages = new ArrayList<Message>();
            getMessages.add(m);
            sendingMessages.put(m.getDestinationHost(),getMessages);
        }


        List<Connection> connections = getConnections();
        if (connections.size() == 0 || this.getNrofMessages() == 0) {
            return;
        }

        for (int i = 0; i < accessUsers.size(); i++){
            DTNHost h = accessUsers.get(i);
            if (!destinationUsers.contains(h))
                continue;

            //find the connection to dedicated destination user (DTNHost h)
            Connection con = findConnection(h, connections);

            //try to send the the dedicated messages to the destination user
            this.tryAllMessages(con,sendingMessages.get(h));
        }
    }

    protected Connection findConnection(DTNHost h, List<Connection> connections){
        if (connections.size() < 1)
                return null;
        for (int i = 0; i < connections.size(); i++){
            Connection con = connections.get(i);
            if (h == con.getOtherNode(this.getHost()))
                return con;
        }
        return null;
    }

    /**
     * Adds a generic property for this DTNHost. The key can be any string but
     * it should be such that no other class accidently uses the same value.
     * @param key The key which is used to lookup the value
     * @param value The value to store
     * @throws SimError if the DTNHost already has a value for the given key
     */
    public void addProperty(String key, Object value) throws SimError {
        if (this.properties != null && this.properties.containsKey(key)) {
            /* check to prevent accidental name space collisions */
            throw new SimError("DTNHost " + this + " already contains value " +
                    "for a key " + key);
        }

        this.updateProperty(key, value);
    }
    /**
     * Returns an object that was stored to this DTNHost using the given
     * key. If such object is not found, null is returned.
     * @param key The key used to lookup the object
     * @return The stored object or null if it isn't found
     */
    public Object getProperty(String key) {
        if (this.properties == null) {
            return null;
        }
        return this.properties.get(key);
    }

    /**
     * Updates a value for an existing property. For storing the value first
     * time, {@link #addProperty(String, Object)} should be used which
     * checks for name space clashes.
     * @param key The key which is used to lookup the value
     * @param value The new value to store
     */
    public void updateProperty(String key, Object value) throws SimError {
        if (this.properties == null) {
			/* lazy creation to prevent performance overhead for classes
			   that don't use the property feature  */
            this.properties = new HashMap<String, Object>();
        }

        this.properties.put(key, value);
    }

    /**
     * Removes a value for an existing property.
     * @param key The key which should be removed from properties
     */
    public void removeProperty(String key){
        if (this.properties == null) {
			/* lazy creation to prevent performance overhead for classes
			   that don't use the property feature  */
            this.properties = new HashMap<String, Object>();
        }
        this.properties.remove(key);

    }

    @Override
    protected void transferDone(Connection con) {
        /* don't leave a copy for the sender */
        boolean sending = false;
        String id = con.getMessage().getId();
        for (Connection c : this.sendingConnections){
            if (id.contains(c.getMessage().getId())) {
                sending = true;
                break;
            }
        }
        if (!sending)
            this.deleteMessage(con.getMessage().getId(), false);
    }

    @Override
    public RelayRouterforInternetAccess replicate() {
        return new RelayRouterforInternetAccess(this);
    }

}
