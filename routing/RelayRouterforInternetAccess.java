package routing;

import core.*;
import interfaces.SatelliteWithChannelModelInterface;
import util.Tuple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.DTNSim.NODE_TYPE;

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
    public static HashMap<DTNHost, DTNHost> allocatedAccessSatelliteForEachUser = new HashMap<DTNHost, DTNHost>();
    /** Key: User Host, Value: Allocated satellites in backup group for each terrestrial user and their weight */
    public static HashMap<DTNHost, ArrayList<Tuple<DTNHost, Double>>> allocatedBackupGroupForEachUser =
            new HashMap<DTNHost, ArrayList<Tuple<DTNHost, Double>>>();

    private double lastAccessUserUpdateTime = 0;
    private double updateInterval = 0.1;//100ms
    private int countInSlidingWindow = 0;

    List<DTNHost> accessUsers = new ArrayList<DTNHost>();
    /** Key: terrestrial user -- DTNHost
     * Value: satellite nodes in backup group and their history channel status information */
    HashMap<DTNHost, HashMap<DTNHost, Double>> SlidingWindowRecord = new HashMap<DTNHost, HashMap<DTNHost, Double>>();
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

    @Override
    public void update() {
        super.update();

        //allow the same satellite node to send multiple messages through different connections simultaneously
        if (!canStartTransfer()) {
            return;
        }

        if (exchangeDeliverableMessages() != null) {
            return;
        }
        String channelModel = DTNSim.RAYLEIGH;
        //String channelModel = RICE;
        updateChannelModelForInterface(channelModel);//should be updated once by DTNHost router in each update function

        String type = (String)this.getProperty(DTNSim.NODE_TYPE);


        //1.User only needs to receive messages from satellites
        if (type.contains(DTNSim.USER))
            return;

        //2.Ground Station is in charge of centralized data updating
        if (type.contains(DTNSim.GS)) {
            updateSlidingWindow();

            //update access node and backup group
            if (this.lastAccessUserUpdateTime + this.updateInterval < SimClock.getTime()) {
                //update backup group for each terrestrial user
                updateBackupGroup();
                //update users served by this satellite node in each time interval
                updateAccessSatellite();
                //TODO forward signal control message to satellites
                //tryControlMessagesToSatellites();
            }
            this.lastAccessUserUpdateTime = SimClock.getTime();
        }

        //3.For satellite nodes, to forward data messages to terrestrial users
        if (type.contains(DTNSim.SAT)) {
            if (!this.accessUsers.isEmpty())
                //try to send messages to users
                tryMessagesToTerrestrialUsers(this.accessUsers);
        }
    }

    /**
     * should be updated once by DTNHost router in each update function
     */
    protected void updateChannelModelForInterface(String channelModel){
        List<DTNHost> users = findDedicatedHosts(DTNSim.USER);
        for (DTNHost user : users){
            double distance = calculateDistance(user, this.getHost());
            int first = 1;
            ((SatelliteWithChannelModelInterface)this.getHost().getInterface(first)).
                    updateLinkState(channelModel, user, distance);
        }
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
    protected void updateSlidingWindow(){
        //TODO
        this.countInSlidingWindow++;
        int firstInterface = 1;//not 0
        for (DTNHost h : findDedicatedHosts(DTNSim.USER)){
            List<DTNHost> satellitesInBackupGroup = findBackupGroup(h);
            HashMap<DTNHost, Double> StatusInBackupGroup = this.SlidingWindowRecord.get(h);

            for (DTNHost satellite : satellitesInBackupGroup){
                double currentCapacity = ((SatelliteWithChannelModelInterface)this.getHost().
                        getInterface(firstInterface)).getCurrentChannelStatus().get(h);
                double history = StatusInBackupGroup.get(satellite);

                StatusInBackupGroup.put(satellite,(history*(countInSlidingWindow - 1) + currentCapacity)/countInSlidingWindow);
            }
        }
    }

    /**
     * find
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
            if (((RelayRouterforInternetAccess)h.getRouter()).getProperty(NODE_TYPE) == type)
                dedicatedHosts.add(h);
        }
        return dedicatedHosts;
    }

    protected void updateBackupGroup(){
        //read the setting of number of satellites in each backup group
        Settings s = new Settings(USERSETTINGNAME_S);
        int nrofBackupSatellites = s.getInt(DTNSim.NROF_BACKUPSATELLITES);//upper limits of backup satellites

        List<DTNHost> userHosts = findDedicatedHosts(DTNSim.USER);
        List<DTNHost> satelliteHosts = findDedicatedHosts(DTNSim.SAT);

        //update satellite weight for each user as backup satellite
        for (DTNHost user: userHosts){
            ArrayList<Tuple<DTNHost, Double>> backupGroup = new ArrayList<Tuple<DTNHost, Double>>();
            //at first, find all reachable satellites
            for (DTNHost satellite: satelliteHosts){
                Tuple<Boolean, Double> isReachable = isReachable(user, satellite);
                if (isReachable.getKey())
                    backupGroup.add(new Tuple<DTNHost, Double>(satellite, isReachable.getValue()));
            }
            backupGroup = (ArrayList<Tuple<DTNHost, Double>>) sort(backupGroup);
            if (backupGroup.size() <= nrofBackupSatellites)
                for (int i = 0; i < nrofBackupSatellites - backupGroup.size(); i++)
                    //warning: the length of backupGroup list is changing
                    backupGroup.remove(nrofBackupSatellites);

            //if the satellites in the backup group become invalid
            if (!this.allocatedBackupGroupForEachUser.get(user).isEmpty()){
                //check the status of each satellite in backup group
                List<DTNHost> needToReplaceSat = new ArrayList<DTNHost>();
                for (Tuple<DTNHost, Double> t : this.allocatedBackupGroupForEachUser.get(user)){
                    DTNHost satInBackupGroup = t.getKey();
                    if (!isReachable(user, satInBackupGroup).getKey())
                        needToReplaceSat.add(satInBackupGroup);
                }

                for (int i = 0; i < needToReplaceSat.size(); i++){
                    this.allocatedBackupGroupForEachUser.get(user).remove(needToReplaceSat.get(i));
                    this.allocatedBackupGroupForEachUser.get(user).add(backupGroup.get(i));
                }
                continue;
            }
            //only do this when initialization for each user
            this.allocatedBackupGroupForEachUser.put(user, backupGroup);
        }
    }

    /**
     *
     * @param a should be user on the earth
     * @param b should be satellite outer the earth, which radius should be greater than a
     * @return
     */
    protected Tuple<Boolean, Double> isReachable(DTNHost a, DTNHost b){
        //read the setting of minimum elevation angle
        Settings s1 = new Settings(USERSETTINGNAME_S);
        double minElevationAngle = s1.getDouble(DTNSim.MIN_ELEVATIONANGLE);
        Settings s2= new Settings(DTNSim.INTERFACE);
        double transmitRange = s2.getDouble(DTNSim.TRANSMIT_RANGE);

        double earthRadius = 6371;//km

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

        //TODO
        if (elevationAngle > minElevationAngle && distance <= transmitRange)
            //return new Tuple<Boolean, Double>(true, elevationAngle);
            return new Tuple<Boolean, Double>(true, distance);
        else
            //return  new Tuple<Boolean, Double>(false, elevationAngle);
            return  new Tuple<Boolean, Double>(false, distance);
    }

    /**
     * update access satellite for each terrestrial user according to history channel information
     */
    protected void updateAccessSatellite(){
        for (DTNHost h : findDedicatedHosts(DTNSim.USER)){
            List<DTNHost> satellitesInBackupGroup = findBackupGroup(h);
            HashMap<DTNHost, Double> StatusInBackupGroup = this.SlidingWindowRecord.get(h);
            //format convert
            List<Tuple<DTNHost, Double>> AL = new ArrayList<Tuple<DTNHost, Double>>();
            for (DTNHost satellite: StatusInBackupGroup.keySet()) {
                AL.add(new Tuple<DTNHost, Double>(satellite, StatusInBackupGroup.get(satellite)));
            }
            //format convert

            //find the best choice according to history channel status
            int first = 0;
            Tuple<DTNHost, Double> bestChoice = sort(AL).get(first);

            allocatedAccessSatelliteForEachUser.put(h, bestChoice.getKey());
        }

        //reset counter
        this.countInSlidingWindow = 0;
    }

    /**
     * 冒泡排序,从小到大排序，大的值放在队列右侧
     * @param distanceList
     * @return
     */
    public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList){
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
        try to forward stored messages to terrestrial users
     */
    /**
     * Tries to send all messages that this router is carrying to all
     * connections this node has. Messages are ordered using the
     * {@link MessageRouter#sortByQueueMode(List)}. See
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
        this.deleteMessage(con.getMessage().getId(), false);
    }

    @Override
    public RelayRouterforInternetAccess replicate() {
        return new RelayRouterforInternetAccess(this);
    }

}
