/*
 * Copyright 2017 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package routing;

        import java.util.*;

        import core.*;
import movement.MovementModel;
import movement.SatelliteMovement;
import util.Tuple;
import static core.SimClock.getTime;
import static java.lang.Math.abs;


public class OptimizedClusteringRouter extends ActiveRouter {
    /**
     * Label indicates that the message can wait for next hop coming or not -setting id ({@value})
     */
    public static final String MSG_WAITLABEL = "waitLabel";
    /**
     * Label indicates that routing path can contain in the message or not -setting id ({@value})
     */
    public static final String MSG_PATHLABEL = "msgPathLabel";
    /**
     * Router path -setting id ({@value})
     */
    public static final String MSG_ROUTERPATH = "routerPath";
    /**
     * Group name in the group -setting id ({@value})
     */
    public static final String GROUPNAME_S = "Group";
    /**
     * Interface name in the group -setting id ({@value})
     */
    public static final String INTERFACENAME_S = "Interface";
    /**
     * Transmit range -setting id ({@value})
     */
    public static final String TRANSMIT_RANGE_S = "transmitRange";
    /**
     * Cluster check interval -setting id ({@value})
     */
    public static final String CLUSTERCHECKINTERVAL_S = "clusterCheckInterval";
    /**
     * Check interval between MEO nodes -setting id ({@value})
     */
    public static final String MEOCHECKINTERVAL_S = "MEOCheckInterval";
    /**
     * The size of confirm message -setting id ({@value})
     */
    public static final String COMFIRMMESSAGESIZE_S = "comfirmMessageSize";
    /**
     * The TTL of confirm message -setting id ({@value})
     */
    public static final String COMFIRMTTL_S = "comfirmTtl";

    /** light speed，approximate 3*10^8m/s */
    private static final double LIGHTSPEED = 299792458;

    /** indicate the transmission radius of each satellite -setting id ({@value} */
    private static double transmitRange;
    /** label indicates that routing path can contain in the message or not -setting id ({@value} */
    private static boolean msgPathLabel;
    /** indicates the TTL of confirm message -setting id ({@value} */
    private static int confirmTtl;

    /** label indicates that the static routing parameters are set or not */
    private static boolean initLabel = false;
    /** to make the random choice */
    private static Random random;
    /** total number of LEO satellites*/
    private static int LEO_TOTAL_SATELLITES;//总节点数
    /** total number of LEO plane*/
    private static int LEO_TOTAL_PLANE;//总轨道平面数
    /** number of hosts in each LEO plane*/
    private static int LEO_NROF_S_EACHPLANE;//每个平面上的卫星数
    
    /** total number of MEO satellites*/
    private static int MEO_TOTAL_SATELLITES;//总节点数
    /** total number of MEO plane*/
    private static int MEO_TOTAL_PLANE;//总轨道平面数
    /** number of hosts in each MEO plane*/
    private static int MEO_NROF_S_EACHPLANE;//每个平面上的卫星数

    /** label indicates that if LEO_MEOClustering is initialized*/
    private static boolean LEO_MEOClusteringInitLable;

    /** indicates that router initialization has been executed or not*/
    private static boolean routerInitLabel = false;
    /** label indicates that routing algorithm has been executed or not at this time */
    private boolean routerTableUpdateLabel;
    /** maintain the earliest arrival time to other nodes */
    private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /** the router table comes from routing algorithm */
    private HashMap<DTNHost, List<Tuple<Integer, Boolean>>>
            routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();

    /** store LEO cluster information */
    private LEOclusterInfo LEOci;
    /** store MEO cluster information */
    private MEOclusterInfo MEOci;


    public OptimizedClusteringRouter(Settings s) {
        super(s);
    }

    protected OptimizedClusteringRouter(OptimizedClusteringRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new OptimizedClusteringRouter(this);
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);

        if (!initLabel){ 
        	//LEO
            Settings sat = new Settings("userSetting");
            LEO_TOTAL_SATELLITES = sat.getInt("nrofLEO");//总节点数
            LEO_TOTAL_PLANE = sat.getInt("nrofLEOPlanes");//总轨道平面数
            LEO_NROF_S_EACHPLANE = LEO_TOTAL_SATELLITES/LEO_TOTAL_PLANE;//每个轨道平面上的节点数
            //MEO
            Settings s = new Settings("Group");
            MEO_TOTAL_SATELLITES = s.getInt("nrofMEO");//总节点数
            MEO_TOTAL_PLANE = s.getInt("nrofMEOPlane");//总轨道平面数
            MEO_NROF_S_EACHPLANE = MEO_TOTAL_SATELLITES/MEO_TOTAL_PLANE;//每个轨道平面上的节点数

            random = new Random();
            Settings setting = new Settings(INTERFACENAME_S);
            transmitRange = setting.getInt(TRANSMIT_RANGE_S);
            setting.setNameSpace(GROUPNAME_S);
            msgPathLabel = setting.getBoolean(MSG_PATHLABEL);
            confirmTtl = setting.getInt(COMFIRMTTL_S);

            routerInitLabel = false;
            initLabel = true;

        }
    }
    /**
     * 在NetworkInterface类中执行链路中断函数disconnect()后，对应节点的router调用此函数
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
//		System.out.println("message: "+con);
//		if (!con.isUp()){
//			if(con.isTransferring()){
//				if (con.getOtherNode(this.getHost()).getRouter().isIncomingMessage(con.getMessage().getId()))
//					con.getOtherNode(this.getHost()).getRouter().removeFromIncomingBuffer(con.getMessage().getId(), this.getHost());
//				super.addToMessages(con.getMessage(), false);//对于因为链路中断而丢失的消息，重新放回发送方的队列中，并且删除对方节点的incoming信息
//				System.out.println("message: "+con.getMessage());
//			}
//		}
    }

    @Override
    public void update() {
        super.update();

        //for initialization, create cluster info
        if (!routerInitLabel)
            routerInit();

        //根据先验信息对LEO进行分组，并确定各个LEO的固定管理MEO节点，从而无需信令交互进行动态分簇
//        if (!LEO_MEOClusteringInitLable)
//            initLEO_MEOClusteringRelationship();

//        //检测用
//        for (DTNHost h : this.getHosts()){
//        	if (h.getSatelliteType().contains("LEO"))
//        		System.out.println(h+"  LEO  "+((OptimizedClusteringRouter)h.getRouter()).LEOci.getManageHosts());
//           	if (h.getSatelliteType().contains("MEO"))
//        		System.out.println(h+"  MEO  "+((OptimizedClusteringRouter)h.getRouter()).MEOci.getClusterList());
//        }
        
        //update dynamic clustering information
        if (!clusteringUpdate()){
            //TODO deal with isolate LEO node
            return; // for isolate LEO node, it does noting
        }
        if (isTransferring()) { // judge the link is occupied or not
            return; // can't start a new transfer
        }
        //helloProtocol();//执行hello包的维护工作
        if (!canStartTransfer())
            return;

        /**Set router update label to make sure that routing algorithm only execute once at a time**/
        routerTableUpdateLabel = false;

        /** sort the messages to transmit */
        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
        List<Message> messages = sortByQueueMode(messageList);

        // try to send the message in the message buffer
        for (Message msg : messages) {
//            //Confirm message's TTL only has 1 minutes, will be drop by itself
//            if (msg.getId().contains("Confirm") || msg.getId().contains("ClusterInfo"))
//                continue;
            if (findPathToSend(msg) == true)
                return;
        }

    }

    /** transform the message Collection to List
     * @param messages
     * @return
     */
    public List<Message> CollectionToList(Collection<Message> messages){
        List<Message> forMsg = new ArrayList<Message>();
        for (Message msg : messages) {	//尝试发送队列里的消息
            forMsg.add(msg);
        }
        return forMsg;
    }



    /**
     * Creates a new Confirm message to the router.
     * The TTL of confirm message setting is different from normal message.
     * @param m The message to create
     * @return True if the creation succeeded, false if not (e.g.
     * the message was too big for the buffer)
     */
    public boolean createNewMessage(Message m, int Ttl) {
        m.setTtl(Ttl);
        addToMessages(m, true);
        return true;
    }
    /**
     * Execute router initialization process of LEO and MEO
     */
    public void routerInit(){
        for (DTNHost host : this.getHosts()){
        	((OptimizedClusteringRouter)host.getRouter()).clusterInfoInit(host);
        }
        
//        /**添加MEO允许连接的节点列表，从而减少MEO不必要的连接，优化性能**/
//        if (((OptimizedClusteringRouter)this.getHost().getRouter()).getMEO_ClusterList() != null){
//    		List<DTNHost> list = new ArrayList<DTNHost>
//    		(((OptimizedClusteringRouter)this.getHost().getRouter()).getMEO_ClusterList());
//    		for (NetworkInterface i : this.getHost().getInterfaces()){
//    			i.allowToConnectNodesInMEOPlane.addAll(list);
//    		}
//        }
//        /**添加MEO允许连接的节点列表，从而减少MEO不必要的连接，优化性能**/
		
        routerInitLabel = true;
    }
    /**
     * Initialize cluster information
     */
    public void clusterInfoInit(DTNHost host){
        switch (host.getSatelliteType()){
        case "LEO":{
            LEOci = new LEOclusterInfo(host);
            //TODO
            break;
        }
        case "MEO":{
            MEOci = new MEOclusterInfo(host);
            break;
        }
    }
    }
    /**
     * update clustering information
     */
    public boolean clusteringUpdate(){

        //do different thing according to this node's satellite type
        switch (getSatelliteType()){
            case "LEO":{
                if (LEOci.getManageHosts().isEmpty()){
                    //don't have MEO and LEO connections, then it do noting
                    if (this.getHost().getConnections().isEmpty())
                        return false;
                    else{
                    	LEOci.updateManageHosts();
                        return true;
                    }
                }
                else
                    return true;
            }
            case "MEO":{
                MEOci.updateClusterMember();
                return true;
            }
        }
        throw new SimError("Satellite Type Error!");
    }
    /**
     * periodically send hello packet to neighbor satellite nodes to check their status
     */
    public void helloProtocol(){
        // TODO helloProtocol
    }
    /**
     * Update router table, find a routing path and try to send the message
     *
     * @param msg
     * @return
     */
    public boolean findPathToSend(Message msg) {
        if (msgPathLabel == true) {//如果允许在消息中写入路径消息
            if (msg.getProperty(MSG_ROUTERPATH) == null) {//通过包头是否已写入路径信息来判断是否需要单独计算路由(同时也包含了预测的可能)
                Tuple<Message, Connection> t =
                        findPathFromRouterTabel(msg);
                return sendMsg(t);
            } else {//如果是中继节点，就检查消息所带的路径信息
                Tuple<Message, Connection> t =
                        findPathFromMessage(msg);
                assert t != null : "读取路径信息失败！";
                return sendMsg(t);
            }
        } else {
            //don't write the routing path into the header
            //routing path will be calculated in each hop
            Tuple<Message, Connection> t =
                    findPathFromRouterTabel(msg);
            return sendMsg(t);
        }
    }

    /**
     * Try to read the path information stored in the header.
     * If the operation fails, the routing table should be re-calculated.
     * @param msg
     * @return
     */
    public Tuple<Message, Connection> findPathFromMessage(Message msg) {
        List<Tuple<Integer, Boolean>> routerPath = null;
        if (msg.getProperty(MSG_ROUTERPATH) instanceof List){
            routerPath = (List<Tuple<Integer, Boolean>>) msg.getProperty(MSG_ROUTERPATH);
        }
        int thisAddress = this.getHost().getAddress();
        if (msg.getTo().getAddress() == thisAddress){
            throw new SimError("Message: " + msg +
                    " already arrive the destination! " + this.getHost());
        }
        if (routerPath == null)
            return null;

        //try to find the next hop from routing path in the message header
        int nextHopAddress = -1;
        boolean waitLable = false;
        for (int i = 0; i < routerPath.size(); i++) {
            if (routerPath.get(i).getKey() == thisAddress) {
            	if (routerPath.size() == i + 1){
            		msg.removeProperty(MSG_ROUTERPATH);
            		return null;
            	}
                nextHopAddress = routerPath.get(i + 1).getKey();//找到下一跳节点地址
                waitLable = routerPath.get(i + 1).getValue();//找到下一跳是否需要等待的标志位
                break;
            }
        }

        if (nextHopAddress > -1) {
            Connection nextCon = findConnection(nextHopAddress);
            //the routing path in the message header could be invaild
            if (nextCon == null) {
                if (!waitLable) {
                    msg.removeProperty(MSG_ROUTERPATH);
                    //try to re-routing
                    Tuple<Message, Connection> t =
                            findPathFromRouterTabel(msg);
                    return t;
                }
            } else {
                Tuple<Message, Connection> t = new
                        Tuple<Message, Connection>(msg, nextCon);
                return t;
            }
        }
        msg.removeProperty(MSG_ROUTERPATH);
        return null;
    }

    /**
     * Try to update router table and find the routing path from router table.
     * If 'msgPathLabel' is true, then the routing path should be written into the header.
     * @param message
     * @return
     */
    public Tuple<Message, Connection> findPathFromRouterTabel(Message message) {
        //update router table by using specific routing algorithm
        if (updateRouterTable(message) == false) {
            return null;
        }
        //get the routing path from router table
        List<Tuple<Integer, Boolean>> routerPath =
                this.routerTable.get(message.getTo());

        //write the routing path into the header
        //or not according to the 'msgPathLabel'
        if (msgPathLabel == true) {
            message.updateProperty(MSG_ROUTERPATH, routerPath);
        }

        Connection firstHop = findConnection(routerPath.get(0).getKey());
        if (firstHop != null) {
            Tuple<Message, Connection> t =
                    new Tuple<Message, Connection>(message, firstHop);
            return t;
        } else {
            if (routerPath.get(0).getValue()) {
                return null;
            } else {
                //TODO
//                throw new SimError("No such connection: " + routerPath.get(0) +
//                       " at routerTable " + this);
                this.routerTable.remove(message.getTo());
                return null;
            }
        }
    }

    /**
     * Find the DTNHost according to its address
     *
     * @param address
     * @return
     */
    public DTNHost findHostByAddress(int address) {
        for (DTNHost host : getHosts()) {
            if (host.getAddress() == address)
                return host;
        }
        return null;
    }

    /**
     * Find the connection according to DTNHost's address
     * @param address
     * @return
     */
    public Connection findConnectionByAddress(int address) {
        for (Connection con : this.getHost().getConnections()) {
            if (con.getOtherNode(this.getHost()).getAddress() == address)
                return con;
        }
        return null;
    }

    /**
     * Update the router table
     *
     * @param msg
     * @return
     */
    public boolean updateRouterTable(Message msg) {
        switch (getSatelliteType()){
            case "LEO":{
                LEOshortestPathSearch(msg);
                break;
            }
            case "MEO":{
                MEOroutingPathSearch(msg);
                break;
            }
        }

        if (this.routerTable.containsKey(msg.getTo())) {
            System.out.println("find the path!  " +this.routerTable.get(msg.getTo())+"   "+ getSatelliteType() +" to "+ msg.getTo().getSatelliteType()+"  " + msg);
            return true;
        } else {
            return false;
        }
    }
    /**
     * 优化方法，直接读取需要计算节点的Connection列表，从而减少计算开销，优化仿真效率
     * @param allHosts
     */
    public HashMap<DTNHost, List<DTNHost>> optimizedTopologyCalculation(List<DTNHost> allHosts){
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        //Calculate links between each two satellite nodes
        for (DTNHost h : allHosts) {
        		for (Connection con : h.getConnections()){
        			DTNHost otherNode = con.getOtherNode(h);
                    if (topologyInfo.get(h) == null)
                        topologyInfo.put(h, new ArrayList<DTNHost>());
                    List<DTNHost> neighborList = topologyInfo.get(h);
                    if (neighborList == null) {
                        neighborList = new ArrayList<DTNHost>();
                        neighborList.add(otherNode);
                    } else {
                        neighborList.add(otherNode);
                    }
        		}               
        }
        return topologyInfo;
    }
    /**
     * Return current network topology in forms of temporal graph
     */
    public HashMap<DTNHost, List<DTNHost>> localTopologyCalculation(List<DTNHost> allHosts) {
        HashMap<DTNHost, Coord> locationRecord = new HashMap<DTNHost, Coord>();
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        double radius = transmitRange;//Represent communication Radius
        //Get satellite movement model which store orbit-info of all satellites in the network
        SatelliteMovement movementModel = ((SatelliteMovement) this.getHost().getMovementModel());

        //Calculate the current coordinate of all satellite nodes in the network
        for (DTNHost h : allHosts) {
            //locationRecord.put(h, movementModel.getCoordinate(h, SimClock.getTime()));
            locationRecord.put(h, h.getLocation());
        }

        //Calculate links between each two satellite nodes
        for (DTNHost h : allHosts) {
            for (DTNHost otherNode : allHosts) {
                if (otherNode == h)
                    continue;
                Coord otherNodeLocation = locationRecord.get(otherNode);
                if (locationRecord.get(h).distance(otherNodeLocation) <= radius) {
                    if (topologyInfo.get(h) == null)
                        topologyInfo.put(h, new ArrayList<DTNHost>());
                    List<DTNHost> neighborList = topologyInfo.get(h);
                    if (neighborList == null) {
                        neighborList = new ArrayList<DTNHost>();
                        neighborList.add(otherNode);
                    } else {
                        neighborList.add(otherNode);
                    }
                }
            }
        }
        return topologyInfo;
    }
    /**
     * 改造的最短路径算法，用于特殊场景，需要指定出发源节点，并给定网络拓扑
     * @param msg
     */
    public void shortestPathSearch(Message msg, DTNHost source, HashMap<DTNHost, List<DTNHost>> topologyInfo) {
        if (topologyInfo.isEmpty())
            return;

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**全网的传输速率假定为一样的**/
        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**表示路由开始的时间**/

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(source);//初始时只有源节点所
        searchedSet.add(source);//初始时只有源节点

        int iteratorTimes = 0;
        int size = topologyInfo.keySet().size();
        boolean updateLabel = true;
        boolean predictLable = false;

        arrivalTime.put(source, SimClock.getTime());//初始化到达时间

        /**优先级队列，做排序用**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        //List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
        //List<Double> correspondingTimeinQueue = new ArrayList<Double>();
        /**优先级队列，做排序用**/

        while (true) {//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;

            for (DTNHost c : sourceSet) {
                if (!topologyInfo.keySet().contains(c)) // limit the search area in the local hosts list
                    continue;
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**判断是否已经是搜索过的源网格集合中的网格**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;

                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
                    if (sourceSet.contains(eachNeighborNetgrid))//确保不回头
                        continue;

                    double time = arrivalTime.get(c) + msg.getSize() / transmitSpeed;
                    /**添加路径信息**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    path.add(thisHop);//注意顺序
                    /**添加路径信息**/
                    /**维护最小传输时间的队列**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//如果时间相等，做随机化选择

                                /**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                                int index = -1;
                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                    if (t.getKey() == eachNeighborNetgrid) {
                                        index = PriorityQueue.indexOf(t);
                                    }
                                }
                                /**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                                if (index > -1) {
                                    PriorityQueue.remove(index);
                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                    arrivalTime.put(eachNeighborNetgrid, time);
                                    routerTable.put(eachNeighborNetgrid, path);
                                }
                            }
                        }
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**对队列进行排序**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//将新的最短网格加入
                    break;
                }
            }
        }
        routerTableUpdateLabel = true;
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void MEOtoLEOshortestPathSearch(Message msg, HashMap<DTNHost, List<DTNHost>> topologyInfo) {
        if (topologyInfo.isEmpty())
            return;

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**全网的传输速率假定为一样的**/
        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**表示路由开始的时间**/

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//初始时只有源节点所
        searchedSet.add(this.getHost());//初始时只有源节点

        for (Connection con : this.getHost().getConnections()) {//添加链路可探测到的一跳邻居，并更新路由表
        	/**不是MEO的一跳节点全部忽略**/
            if (!con.getOtherNode(this.getHost()).getSatelliteType().contains("MEO"))
                continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//初始时只有本节点和链路邻居
            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
            path.add(hop);//注意顺序
            arrivalTime.put(neiHost, time);
            routerTable.put(neiHost, path);
        }
        /**添加链路可探测到的一跳邻居网格，并更新路由表**/

        int iteratorTimes = 0;
        int size = topologyInfo.keySet().size();
        boolean updateLabel = true;
        boolean predictLable = false;

        arrivalTime.put(this.getHost(), SimClock.getTime());//初始化到达时间

        /**优先级队列，做排序用**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        //List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
        //List<Double> correspondingTimeinQueue = new ArrayList<Double>();
        /**优先级队列，做排序用**/

        while (true) {//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;

            for (DTNHost c : sourceSet) {
                if (!topologyInfo.keySet().contains(c)) // limit the search area in the local hosts list
                    continue;
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**判断是否已经是搜索过的源网格集合中的网格**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;

                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
                    if (sourceSet.contains(eachNeighborNetgrid))//确保不回头
                        continue;

                    double time = arrivalTime.get(c) + msg.getSize() / transmitSpeed;
                    /**添加路径信息**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    path.add(thisHop);//注意顺序
                    /**添加路径信息**/
                    /**维护最小传输时间的队列**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//如果时间相等，做随机化选择

                                /**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                                int index = -1;
                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                    if (t.getKey() == eachNeighborNetgrid) {
                                        index = PriorityQueue.indexOf(t);
                                    }
                                }
                                /**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                                if (index > -1) {
                                    PriorityQueue.remove(index);
                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                    arrivalTime.put(eachNeighborNetgrid, time);
                                    routerTable.put(eachNeighborNetgrid, path);
                                }
                            }
                        }
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**对队列进行排序**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//将新的最短网格加入
                    break;
                }
            }
        }
        routerTableUpdateLabel = true;
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void shortestPathSearch(Message msg, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty())
            return;
        //update the current topology information
        HashMap<DTNHost, List<DTNHost>> topologyInfo = localTopologyCalculation(localHostsList);

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**全网的传输速率假定为一样的**/
        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**表示路由开始的时间**/

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//初始时只有源节点所
        searchedSet.add(this.getHost());//初始时只有源节点

        for (Connection con : this.getHost().getConnections()) {//添加链路可探测到的一跳邻居，并更新路由表
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//初始时只有本节点和链路邻居
            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
            path.add(hop);//注意顺序
            arrivalTime.put(neiHost, time);
            routerTable.put(neiHost, path);
        }
        /**添加链路可探测到的一跳邻居网格，并更新路由表**/

        int iteratorTimes = 0;
        int size = localHostsList.size();
        boolean updateLabel = true;
        boolean predictLable = false;

        arrivalTime.put(this.getHost(), SimClock.getTime());//初始化到达时间

        /**优先级队列，做排序用**/
        List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
        //List<GridCell> GridCellListinPriorityQueue = new ArrayList<GridCell>();
        //List<Double> correspondingTimeinQueue = new ArrayList<Double>();
        /**优先级队列，做排序用**/

        while (true) {//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
            if (iteratorTimes >= size)//|| updateLabel == false)
                break;
            updateLabel = false;

            for (DTNHost c : sourceSet) {
                if (!localHostsList.contains(c)) // limit the search area in the local hosts list
                    continue;
                List<DTNHost> neiList = topologyInfo.get(c);//get neighbor nodes from topology info

                /**判断是否已经是搜索过的源网格集合中的网格**/
                if (searchedSet.contains(c) || neiList == null)
                    continue;

                searchedSet.add(c);
                for (DTNHost eachNeighborNetgrid : neiList) {//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
                    if (sourceSet.contains(eachNeighborNetgrid))//确保不回头
                        continue;

                    double time = arrivalTime.get(c) + msg.getSize() / transmitSpeed;
                    /**添加路径信息**/
                    List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
                    if (this.routerTable.containsKey(c))
                        path.addAll(this.routerTable.get(c));
                    Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborNetgrid.getAddress(), predictLable);
                    path.add(thisHop);//注意顺序
                    /**添加路径信息**/
                    /**维护最小传输时间的队列**/
                    if (arrivalTime.containsKey(eachNeighborNetgrid)) {
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                        if (time <= arrivalTime.get(eachNeighborNetgrid)) {
                            if (random.nextBoolean() == true && time - arrivalTime.get(eachNeighborNetgrid) < 0.1) {//如果时间相等，做随机化选择

                                /**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                                int index = -1;
                                for (Tuple<DTNHost, Double> t : PriorityQueue) {
                                    if (t.getKey() == eachNeighborNetgrid) {
                                        index = PriorityQueue.indexOf(t);
                                    }
                                }
                                /**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
                                if (index > -1) {
                                    PriorityQueue.remove(index);
                                    PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                                    arrivalTime.put(eachNeighborNetgrid, time);
                                    routerTable.put(eachNeighborNetgrid, path);
                                }
                            }
                        }
                        /**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
                    } else {
                        PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborNetgrid, time));
                        arrivalTime.put(eachNeighborNetgrid, time);
                        routerTable.put(eachNeighborNetgrid, path);
                    }
                    /**对队列进行排序**/
                    sort(PriorityQueue);
                    updateLabel = true;
                }
            }
            iteratorTimes++;
            for (int i = 0; i < PriorityQueue.size(); i++) {
                if (!sourceSet.contains(PriorityQueue.get(i).getKey())) {
                    sourceSet.add(PriorityQueue.get(i).getKey());//将新的最短网格加入
                    break;
                }
            }
        }
        routerTableUpdateLabel = true;
    }

    /**
     * 根据先验信息对LEO进行分组，并确定各个LEO的固定管理MEO节点，从而无需信令交互进行动态分簇
     */
//    public void initLEO_MEOClusteringRelationship(){
//    	System.out.println("init clustering ralationship!");
//        List<DTNHost> LEOList = new ArrayList<DTNHost>();
//        List<DTNHost> MEOList = new ArrayList<DTNHost>();
//        //找出所有LEO节点
//        for (DTNHost h : this.getHosts()){
//            if (h.getSatelliteType().contains("LEO"))
//                LEOList.add(h);
//        }
//        //找出所有MEO节点
//        for (DTNHost h : this.getHosts()){
//            if (h.getSatelliteType().contains("MEO"))
//                MEOList.add(h);
//        }
//        
//        //对于每一个MEO节点进行初始化分簇节点的确定
//        for (DTNHost mh : MEOList){
//            List<Tuple<DTNHost, Double>> ConnectedLEO = new ArrayList<Tuple<DTNHost, Double>>();
//            //先找出所有通信范围内的LEO节点
//            for (DTNHost lh : LEOList){
//                double distance = getDistance(mh, lh);
//                if (distance <= this.transmitRange)
//                    ConnectedLEO.add(new Tuple<DTNHost, Double>(lh, distance));
//            }
//            //排序，找出最近的节点
//            List<Tuple<DTNHost, Double>> sortLEO = sort(ConnectedLEO);
//            //从排序后的节点中记录最近的n个临近LEO轨道平面，作为受此MEO管理的分簇,这里n=4
//            int upBound = 4;
//            List<Integer> nearnestPlane = new ArrayList<Integer>();
//            for (Tuple<DTNHost, Double> t : sortLEO) {
//                boolean label = true;
//                int num = this.getLEOOrbitPlane(t.getKey());//获取每个卫星所属的轨道平面编号
//                             
//                //如果一个LEO轨道平面被分配太多MEO管理节点，则需要跳过，从而保证每个LEO轨道平面都被分配到管理节点
//                if(((OptimizedClusteringRouter)t.getKey().getRouter()).LEOci.getManageHosts().size() 
//                		>= (this.MEO_TOTAL_PLANE*upBound)/this.LEO_TOTAL_PLANE)
//                	continue;
//                
//                if (nearnestPlane.isEmpty()) {
//                    nearnestPlane.add(num);
//                    continue;
//                }
//                for (int i = 0; i < nearnestPlane.size(); i++) {//和已经存储的每个变量进行比较，一样就跳出
//                    if (nearnestPlane.get(i) == num) {
//                        label = false;
//                        break;
//                    }
//                }
//                if (label)
//                    nearnestPlane.add(num);
//                //找足了upBound个最近轨道平面，就跳出
//                if (nearnestPlane.size() > upBound) {
//                    ((OptimizedClusteringRouter)mh.getRouter()).MEOci.initClusterList(nearnestPlane);
//                    break;
//                }
//            }
//        }
//        //完成后置初始化位，以免再次初始化
//        LEO_MEOClusteringInitLable = true;
//    }

    /**
     * 获取每个卫星所属的轨道平面编号
     * @param host
     */
    public int getLEOOrbitPlane(DTNHost host){
        return host.getAddress()/LEO_NROF_S_EACHPLANE + 1;
    }
    /**
     * judge the shortest direction to forward message in the same orbit plane
     * @param to
     */
    public void chooseOneNeighborHostToSendInSamePlane(Message msg, DTNHost to){
        if (LEOci.getNeighborHostsInSamePlane().size() != 2)
            throw new SimError("LEOci.getNeighborHostsInSamePlane() error!");
        DTNHost a = LEOci.getNeighborHostsInSamePlane().get(0);
        DTNHost b = LEOci.getNeighborHostsInSamePlane().get(1);
        DTNHost nextHop = null;
        if (abs(to.getAddress() - a.getAddress()) > abs(to.getAddress() - b.getAddress()))
            nextHop = b;
        else
            nextHop = a;

        List<Tuple<Integer, Boolean>> path =
                new ArrayList<Tuple<Integer, Boolean>>();
        path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
        routerTable.put(to, path);
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void LEOshortestPathSearch(Message msg) {
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
                //目的节点是否在自身所属轨道平面上
            	System.out.println(this.getHost()+" 同一轨道平面内节点 : "+LEOci.getAllHostsInSamePlane());
                if (LEOci.getAllHostsInSamePlane().contains(to)){
                	System.out.println("同一轨道平面!");
                    chooseOneNeighborHostToSendInSamePlane(msg, to);
                }
                else{
                    //检查目的节点是否在邻居轨道平面上
                    List<DTNHost> hostsInNeighborOrbitPlane = LEOci.ifHostsInNeighborOrbitPlane(to);
                    if (hostsInNeighborOrbitPlane != null){//不为空，则说明在邻居轨道上，且返回的是邻居轨道的所有节点
                    	//先尝试通过邻居轨道通信节点转发
                    	if(!msgFromLEOForwardToNeighborPlane(to))
                    		msgFromLEOForwardedByMEO(msg, to);
                    }
                    //否则，直接通过MEO管理节点转发
                    else{
                    	msgFromLEOForwardedByMEO(msg, to);
                    }
                }
                break;
            }
            case "MEO":{
                Connection desConnection = null;
                List<Connection> MEOConnectionList = new ArrayList<Connection>();
                for (Connection con : this.getConnections()){
                    if (con.getOtherNode(this.getHost()).equals(to))
                        desConnection = con;
                    if (con.getOtherNode(this.getHost()).getSatelliteType().contains("MEO"))
                        MEOConnectionList.add(con);
                }
                if (MEOConnectionList.isEmpty())
                	break;
                //目的作为MEO节点，先检查是否在通信范围之内可以直接转发
                if (desConnection != null){
                    DTNHost nextHop = desConnection.getOtherNode(this.getHost());
                    List<Tuple<Integer, Boolean>> path =
                            new ArrayList<Tuple<Integer, Boolean>>();
                    path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
                    routerTable.put(to, path);
                }
                //否则，通过其它MEO节点进行转发
                else{
                    desConnection = MEOConnectionList.get(random.nextInt(MEOConnectionList.size()));
                    DTNHost nextHop = desConnection.getOtherNode(this.getHost());
                    List<Tuple<Integer, Boolean>> path =
                            new ArrayList<Tuple<Integer, Boolean>>();
                    path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
                    routerTable.put(to, path);
                }
            }
            break;
        }
    }
    /**
     * LEO信息发往邻居轨道平面
     * @param to
     */
    public boolean msgFromLEOForwardToNeighborPlane(DTNHost to){
    	
    	int destinationSerialNumberOfPlane = to.getAddress()/LEO_NROF_S_EACHPLANE + 1;
    	List<DTNHost> allCommunicationNodes = new ArrayList<DTNHost>();
    	//找出所有此平面上的可以支持跨平面通信的卫星
    	for (DTNHost h : this.CommunicationNodesList.keySet()){
    		if (this.CommunicationNodesList.get(h) + 1 == destinationSerialNumberOfPlane)
    			allCommunicationNodes.add(h);
    	}
    	System.out.println("在邻居轨道! 邻居轨道上可通信节点： "+allCommunicationNodes+" connections: "+this.getConnections());
    	for (DTNHost h : allCommunicationNodes){
    		Connection con = this.findConnection(h.getAddress());
    		if (con != null){
                List<Tuple<Integer, Boolean>> path =
                        new ArrayList<Tuple<Integer, Boolean>>();
                path.add(new Tuple<Integer, Boolean>(h.getAddress(), false));
                routerTable.put(to, path);
                return true;
    		}
    	}   
    	//没有与通信卫星相遇
    	return false;
    }
    /**
     * 执行从LEO信息交由MEO转发
     * @param to
     */
    public void msgFromLEOForwardedByMEO(Message msg, DTNHost to){
    	if (LEOci.getManageHosts().isEmpty())
    		return;
    	if (((OptimizedClusteringRouter)to.getRouter()).LEOci.getManageHosts().isEmpty()){
    		System.out.println(" manage hosts empty!"+to);
    		return;
    	}
    	
    	/**采用最短路径搜索算法的变种来找最优路径**/
        //获取LEO通过MEO网络到达目的LEO的拓扑
        //getLEOtoLEOThroughMEOTopology(this.getHost(), to);
        //改造的最短路径算法，用于特殊场景，需要指定出发源节点，并给定网络拓扑
        shortestPathSearch(msg, this.getHost(), getLEOtoLEOThroughMEOTopology(this.getHost(), to));
    	/**采用最短路径搜索算法的变种来找最优路径**/
    	
    	if (this.routerTable.containsKey(to)){
    		System.out.println("搜索到通过MEO转发的最短路径！ to" + to);
    		return;
    	}
    	
    	System.out.println("转交给MEO节点进行转发   to" + to);
        int nrofManageHosts = LEOci.getManageHosts().size();
        DTNHost nextHop = LEOci.getManageHosts().get(random.nextInt(nrofManageHosts));//随机选取一个MEO管理节点帮助转发

        if (nextHop != null){
            List<Tuple<Integer, Boolean>> path =
                    new ArrayList<Tuple<Integer, Boolean>>();
            path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
            routerTable.put(to, path);
        }
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void MEOroutingPathSearch(Message msg) {
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
                //是否处于本节点管理簇当中
            	System.out.println(this.getHost()+" cluster list: "+MEOci.clusterList);
                if (MEOci.clusterList.contains(to)){
                    //是的话，看是否直接相连，否则就等待
                	Connection con = this.findConnection(to.getAddress());
                	if (con != null){
                        List<Tuple<Integer, Boolean>> path =
                                new ArrayList<Tuple<Integer, Boolean>>();
                        path.add(new Tuple<Integer, Boolean>(to.getAddress(), false));
                        routerTable.put(to, path);
                        return;//找到路径，返回
                	}
                }
                //否则找到管理此节点的MEO节点，交于它进行转发
            	/**采用最短路径搜索算法的变种来找最优路径**/          
                HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoLEOTopology(this.getHost(), to);//optimizedTopologyCalculation(MEOci.MEOList);//localTopologyCalculation(MEOci.MEOList);          
                List<DTNHost> manageHosts = ((OptimizedClusteringRouter)to.getRouter()).LEOci.getManageHosts();              
                //目的节点没有管理节点可达,则直接跳出
                if (manageHosts.isEmpty()){
                	System.out.println(to+" has no manage hosts! ");
                	return;
                }
                //调用搜索算法
            	MEOtoLEOshortestPathSearch(msg, topologyInfo);
            	if (this.routerTable.containsKey(to))
            		System.out.println("MEO找到了最短路径");
            	/**采用最短路径搜索算法的变种来找最优路径**/
//                //TODO 不是找的最近的MEO，可以改进
//                else{
//                    // check other cluster information managed by other MEO
//                    DTNHost nextHop = MEOci.findHostInOtherClusterList(to);
//                    if (nextHop != null){
//                        List<Tuple<Integer, Boolean>> path =
//                                new ArrayList<Tuple<Integer, Boolean>>();
//                        path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
//                        routerTable.put(to, path);
//                    }
//                }
                break;
            }
            case "MEO":{
//                //check the neighbors first
//                for (Connection con : this.getConnections()){
//                    DTNHost neighborNode = con.getOtherNode(this.getHost());
//                    if (to == neighborNode){
//                        List<Tuple<Integer, Boolean>> path =
//                                new ArrayList<Tuple<Integer, Boolean>>();
//                        path.add(new Tuple<Integer, Boolean>(neighborNode.getAddress(), false));
//                        routerTable.put(to, path);
//                    }
//                }
//                //if not, then check all other MEO nodes
                //改造的最短路径算法，用于特殊场景，需要指定出发源节点，并给定网络拓扑
                shortestPathSearch(msg, this.getHost(), getMEOtoMEOTopology(this.getHost()));
                //shortestPathSearch(msg, MEOci.getMEOList());
                break;
            }
        }
    }
    /**
     * 每一个MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getLEOtoLEOThroughMEOTopology(DTNHost startLEO, DTNHost endLEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	List<DTNHost> manageHosts = ((OptimizedClusteringRouter)endLEO.getRouter()).LEOci.updateManageHosts();
    	
    	topologyInfo = getMEOtoLEOTopology(manageHosts.get(0), endLEO);
    	
    	for (DTNHost MEO : manageHosts){
    		List<DTNHost> list = topologyInfo.get(MEO);  	
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.add(startLEO);
            } else {
            	list.add(startLEO);
            }
    	}
    	return topologyInfo;
    }

    /**
     * 每一个MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoMEOTopology(DTNHost sMEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	for (DTNHost MEO : ((OptimizedClusteringRouter)sMEO.getRouter()).MEOci.getMEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(((OptimizedClusteringRouter)
    				MEO.getRouter()).MEOci.allowConnectMEOHostsInSamePlane);
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(((OptimizedClusteringRouter)
    				MEO.getRouter()).MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	return topologyInfo;
    }
    /**
     * 每一个MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始MEO点
     * @param endMEO   目的LEO节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoLEOTopology(DTNHost sMEO, DTNHost endLEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	topologyInfo = getMEOtoMEOTopology(sMEO);
    	
    	//拓扑中添加MEO到目的LEO的链路
    	for (DTNHost MEO : ((OptimizedClusteringRouter)endLEO.getRouter()).LEOci.updateManageHosts()){
    		List<DTNHost> list = topologyInfo.get(MEO);  	
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.add(endLEO);
            } else {
            	list.add(endLEO);
            }
    	}	
    	return topologyInfo;
    }
    
    /**
     * Bubble sort algorithm
     *
     * @param distanceList
     * @return
     */
    public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList) {
        for (int j = 0; j < distanceList.size(); j++) {
            for (int i = 0; i < distanceList.size() - j - 1; i++) {
                if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()) {//从小到大，大的值放在队列右侧
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
     * Find the DTNHost according to its address
     * @param path
     * @return
     */
    public List<DTNHost> getHostListFromPath(List<Integer> path) {
        List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
        for (int i = 0; i < path.size(); i++) {
            hostsOfPath.add(this.getHostFromAddress(path.get(i)));//根据节点地址找到DTNHost
        }
        return hostsOfPath;
    }

    /**
     * Find the DTNHost according to its address
     *
     * @param address
     * @return
     */
    public DTNHost getHostFromAddress(int address) {
        for (DTNHost host : getHosts()) {
            if (host.getAddress() == address)
                return host;
        }
        return null;
    }

    /**
     * Calculate the distance between two nodes.
     *
     * @param a
     * @param b
     * @return
     */
    public double getDistance(DTNHost a, DTNHost b) {
        double ax = a.getLocation().getX();
        double ay = a.getLocation().getY();
        double az = a.getLocation().getZ();
        double bx = a.getLocation().getX();
        double by = a.getLocation().getY();
        double bz = a.getLocation().getZ();

        double distance = (ax - bx) * (ax - bx) + (ay - by) * (ay - by) + (az - bz) * (az - bz);
        distance = Math.sqrt(distance);

        return distance;
    }

    /**
     * Find the specific connection according to neighbor node's address
     *
     * @param address
     * @return
     */
    public Connection findConnection(int address) {
        List<Connection> connections = this.getHost().getConnections();
        for (Connection c : connections) {
            if (c.getOtherNode(this.getHost()).getAddress() == address) {
                return c;
            }
        }
        return null;
    }

    /**
     * Try to send the message through a specific connection
     *
     * @param t
     * @return
     */
    public Message tryMessageToConnection(Tuple<Message, Connection> t) {
        if (t == null)
            throw new SimError("No such tuple: " +
                    " at " + this);
        Message m = t.getKey();
        Connection con = t.getValue();
        int retVal = startTransfer(m, con);
        if (retVal == RCV_OK) {  //accepted a message, don't try others
            return m;
        } else if (retVal > 0) { //系统定义，只有TRY_LATER_BUSY大于0，即为1
            return null;          // should try later -> don't bother trying others
        }
        return null;
    }

    /**
     * Judge the next hop is busy or not.
     *
     * @param t
     * @return
     */
    public boolean nextHopIsBusyOrNot(Tuple<Message, Connection> t) {

        Connection con = t.getValue();
        if (con == null)
            return false;
        /**检查所经过路径的情况，如果下一跳的链路已经被占用，则需要等待**/
        if (con.isTransferring() || ((OptimizedClusteringRouter)
                con.getOtherNode(this.getHost()).getRouter()).isTransferring()) {
            return true;//说明目的节点正忙
        }
        return false;
        /**至于检查所有的链路占用情况，看本节点是否在对外发送的情况，在update函数中已经检查过了，在此无需重复检查**/
    }

    /**
     * Try to send the message through a specific connection.
     *
     * @param t
     * @return
     */
    public boolean sendMsg(Tuple<Message, Connection> t) {
        if (t == null || t.getValue() == null) {
            assert false : "error!";
            return false;
        } else {
            if (tryMessageToConnection(t) != null)//列表第一个元素从0指针开始！！！
                return true;//只要成功传一次，就跳出循环
            else
                return false;
        }
    }

    /**
     * Returns true if this router is transferring something at the moment or
     * some transfer has not been finalized.
     *
     * @return true if this router is transferring something
     */
    @Override
    public boolean isTransferring() {
        //判断该节点能否进行传输消息，存在以下情况一种以上的，直接返回，不更新,即现在信道已被占用：
        //情形1：本节点正在向外传输
        if (this.sendingConnections.size() > 0) {//protected ArrayList<Connection> sendingConnections;
            return true; // sending something
        }

        List<Connection> connections = getConnections();
        //情型2：没有邻居节点
        if (connections.size() == 0) {
            return false; // not connected
        }
        //情型3：有邻居节点，但自身与周围节点正在传输
        //模拟了无线广播链路，即邻居节点之间同时只能有一对节点传输数据!
        for (int i = 0, n = connections.size(); i < n; i++) {
            Connection con = connections.get(i);
            //isReadyForTransfer返回false则表示有信道在被占用，因此对于广播信道而言不能传输
            if (!con.isReadyForTransfer()) {
                return true;    // a connection isn't ready for new transfer
            }
        }
        return false;
    }

    /**
     * 此重写函数保证在传输完成之后，源节点的信息从messages缓存中删除
     */
    @Override
    protected void transferDone(Connection con) {
        String msgId = con.getMessage().getId();
        removeFromMessages(msgId);
    }

    /**
     * get all satellite nodes info in the movement model
     *
     * @return all satellite nodes in the network
     */
    public List<DTNHost> getHosts() {
        return new ArrayList<DTNHost>(((SatelliteMovement) this.getHost().getMovementModel()).getHosts());
    }
    /**
     * get satellite movement model
     * @return
     */
    public MovementModel getMovementModel(){
        return this.getHost().getMovementModel();
    }
    /**
     * @return satellite type in multi-layer satellite networks: LEO, MEO or GEO
     */
    public String getSatelliteType(){
        return this.getHost().getSatelliteType();
    }
    /**
     * 
     * @return all MEO nodes
     */
    public List<DTNHost> getMEO_ClusterList(){
    	if (MEOci != null)
    		return MEOci.getClusterList();
    	return null;
    }
    /**
     * Stores the cluster information in the LEO node
     */
    private class LEOclusterInfo{
    	/** bind node and this cluster info*/
    	public DTNHost thisNode;
        /** start address number of the first host in the plane*/
        public int startNumber;//此轨道平面内的节点，起始编号
        /** end address number of the first host in the plane*/
        public int endNumber;//此轨道平面内的节点，结尾编号
        /** all hosts in LEO plane*/
        private List<DTNHost> LEOList = new ArrayList<DTNHost>();
        /** all hosts in the same orbit plane*/
        public List<DTNHost> allHostsInSamePlane = new ArrayList<DTNHost>();
        /** neighbor hosts in the same orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectMEOHostsInSamePlane = new ArrayList<DTNHost>();
        /** neighbor hosts in two neighbor orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectMEOHostsInNeighborPlane = new ArrayList<DTNHost>();
        /** neighbor hosts in the neighbor orbit plane*/
        public List<DTNHost> neighborPlaneHosts = new ArrayList<DTNHost>();//相邻轨道平面内的两个邻居节点
        /** hosts list in the same orbit plane, and they can be forwarded directly without MEO */
        public List<DTNHost> neighborHostsInSamePlane = new ArrayList<DTNHost>();//相同轨道平面里的两个邻居节点
        /** all manage hosts which contains in the transmission range of MEO */
        private List<DTNHost> manageHosts = new ArrayList<DTNHost>();

        public LEOclusterInfo(DTNHost h){
        	thisNode = h;
            initInterSatelliteNeighbors();//初始化记录节点在同一个轨道内的所有节点，以及轨道内相邻的邻居用于直接转发
            //找到所有LEO节点
            findAllLEONodes();
            //找到所有邻居轨道平面的节点
            findAllSatellitesInLEONeighborPlane();
            //同平面内的邻居节点
            findAllowConnectMEOHostsInLEOSamePlane(thisNode.getAddress()/LEO_NROF_S_EACHPLANE + 1, LEO_NROF_S_EACHPLANE);
        }
        /**
         * 初始化设定本节点的同轨的邻居节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllowConnectMEOHostsInLEOSamePlane(int nrofPlane, int nrofSatelliteInOnePlane){ 
            int	startNumber = LEO_NROF_S_EACHPLANE * (nrofPlane - 1);//此轨道平面内的节点，起始编号
            int endNumber = LEO_NROF_S_EACHPLANE * nrofPlane - 1;//此轨道平面内的节点，结尾编号
            if (!(thisNode.getAddress() >= startNumber && thisNode.getAddress()<= endNumber)){
            	throw new SimError("LEO address calculation error");
            }
            int a = thisNode.getAddress() - 1;
            int b = thisNode.getAddress() + 1;
            
            if (a < startNumber)
            	a = endNumber;
            if (b > endNumber)
            	b = startNumber;
            allowConnectMEOHostsInSamePlane.add(findHostByAddress(a));
            allowConnectMEOHostsInSamePlane.add(findHostByAddress(b));
        }
        /**
         * 初始化设定本节点的两个邻居轨道平面所有节点于allowConnectMEOHostsInNeighborPlane中
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInLEONeighborPlane(){ 

            int thisHostAddress = getHost().getAddress();

            int serialNumberOfPlane = thisHostAddress/LEO_NROF_S_EACHPLANE + 1;
            int a = serialNumberOfPlane - 1;
            int b = serialNumberOfPlane + 1;
            if (a < 1)
            	a = LEO_TOTAL_PLANE;
            if(b > LEO_TOTAL_PLANE)
            	b = 1;
            //左邻居MEO轨道平面
            int startNumber1 = LEO_NROF_S_EACHPLANE * (a - 1);//此轨道平面内的节点，起始编号
            int endNumber1 = LEO_NROF_S_EACHPLANE * a - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber1 && host.getAddress() <= endNumber1){
                	allowConnectMEOHostsInNeighborPlane.add(host);
                }
            }
            //右邻居MEO轨道平面
            int startNumber2 = LEO_NROF_S_EACHPLANE * (b - 1);//此轨道平面内的节点，起始编号
            int endNumber2 = LEO_NROF_S_EACHPLANE * b - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber2 && host.getAddress() <= endNumber2){
                	allowConnectMEOHostsInNeighborPlane.add(host);
                }
            }
        }
        /**
         * 同一平面内的邻居两个节点
         * @return
         */
        public List<DTNHost> getAllowConnectMEOHostsInLEOSamePlane(){
        	return allowConnectMEOHostsInSamePlane;
        }
        /**
         * neighbor hosts in two neighbor orbit plane
         * @return
         */
        public List<DTNHost> getAllowConnectMEOHostsInNeighborPlane(){
        	return allowConnectMEOHostsInNeighborPlane;
        }
        /**
         * 动态找到MEO的当前邻居轨道的最近两个节点用户邻居通信
         * @return
         */
        public List<DTNHost> updateAllowConnectLEOHostsInNeighborPlane(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	
        	List<Tuple<DTNHost, Double>> listFromDistance = new ArrayList<Tuple<DTNHost, Double>>();
        	for (DTNHost h : getAllowConnectMEOHostsInNeighborPlane()){
        		listFromDistance.add(new Tuple<DTNHost, Double>(h, getDistance(thisNode, h)));
        	}
        	sort(listFromDistance);
        	for (Tuple<DTNHost, Double> t : listFromDistance){
        		if (list.isEmpty())
        			list.add(t.getKey());
        		else{
        			//不是同一个轨道平面的
        			if (!((OptimizedClusteringRouter)list.get(0).getRouter())
        					.LEOci.getAllowConnectMEOHostsInLEOSamePlane().contains(t.getKey())){
        					list.add(t.getKey());	
        					}
        		}
        		if (list.size() >= 2)
        			break;
        	}
        	return list;
        }
        /**
         * 找到所有LEO节点
         * @return
         */
        public List<DTNHost> findAllLEONodes(){
        	LEOList.clear();
        	for (DTNHost h : getHosts()){
        		if (h.getSatelliteType().contains("LEO"))
        			LEOList.add(h);
        	}
        	return LEOList;       		
        }
        /**
         * 判断目的节点是否在邻居平面上
         * @param to
         * @return
         */
        public List<DTNHost> ifHostsInNeighborOrbitPlane(DTNHost to){
            List<DTNHost> hostsInNeighborOrbitPlane = null;

            int NROF_S_EACHPLANE = LEO_TOTAL_SATELLITES/LEO_TOTAL_PLANE;//每个轨道平面上的节点数
            int thisHostAddress = getHost().getAddress();

            int serialNumberOfPlane = thisHostAddress/NROF_S_EACHPLANE + 1;
            int destinationSerialNumberOfPlane = to.getAddress()/NROF_S_EACHPLANE + 1;
            System.out.println("src: "+serialNumberOfPlane+" des  "+destinationSerialNumberOfPlane);
            if (abs(serialNumberOfPlane - destinationSerialNumberOfPlane) <= 1 ||
                    abs(serialNumberOfPlane - destinationSerialNumberOfPlane) >= LEO_TOTAL_PLANE){
                int startNumber = NROF_S_EACHPLANE * (destinationSerialNumberOfPlane - 1);//此轨道平面内的节点，起始编号
                int endNumber = NROF_S_EACHPLANE * destinationSerialNumberOfPlane - 1;//此轨道平面内的节点，结尾编号
                
                hostsInNeighborOrbitPlane = new ArrayList<DTNHost>();
                //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
                for (DTNHost host : getHosts()){
                    if (host.getAddress() >= startNumber && host.getAddress() <= endNumber){
                        hostsInNeighborOrbitPlane.add(host);
                    }
                }
            }
            //否则就返回空
            return hostsInNeighborOrbitPlane;
        }
        /**
         * @return hosts list contains all LEO nodes in the same plane
         */
        public List<DTNHost> getAllHostsInSamePlane(){
            return allHostsInSamePlane;
        }
        /**
         * @return hosts list contains all LEO nodes in the same plane
         */
        public List<DTNHost> getNeighborHostsInSamePlane(){
            return neighborHostsInSamePlane;
        }
        /**
         * 获取当前通信范围内的MEO节点
         * @return
         */
        public List<DTNHost> getConnectedMEOHosts(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	for (Connection con : thisNode.getConnections()){
        		DTNHost h = con.getOtherNode(thisNode);
        		if (!list.contains(h) && h.getSatelliteType().contains("MEO"))
        			list.add(h);
        	}
        	return list;
        }
        /**
         * update manage hosts according to connection
         */
        public List<DTNHost> updateManageHosts(){
        	manageHosts.clear();
        	manageHosts.addAll(getConnectedMEOHosts());
        	return manageHosts;
        }
        /**
         * add a MEO manage host into list
         * @param h
         */
        public void addManageHost(DTNHost h){
            manageHosts.add(h);
        }
        /**
         * @return all MEO manage hosts list
         */
        public List<DTNHost> getManageHosts(){
            return manageHosts;
        }
        /**
         * 处理initInterSatelliteNeighbors()函数中的边界值问题
         * @param n
         * @param upperBound
         * @param lowerBound
         * @return
         */
        public int processBoundOfNumber(int n , int lowerBound, int upperBound){
            if (n < lowerBound){
                return n + upperBound + 1 + lowerBound;
            }
            if (n > upperBound){
                return n - upperBound - 1 + lowerBound;
            }
            return n;
        }
        /**
         * 处理在同一个平面内的节点编号，处在边界时的问题
         * @param n
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         * @return
         */
        public int processBound(int n ,int nrofPlane, int nrofSatelliteInOnePlane){
            if ((Integer)startNumber == null && (Integer)endNumber == null) {
                startNumber = nrofSatelliteInOnePlane * (nrofPlane - 1);//此轨道平面内的节点，起始编号
                endNumber = nrofSatelliteInOnePlane * nrofPlane - 1;//此轨道平面内的节点，结尾编号
            }
            if (n < startNumber)
                return endNumber;
            if (n > endNumber)
                return startNumber;
            //int nrofPlane = n/nrofSatelliteInOnePlane + 1;
            return n;
        }
        /**
         * 初始化设定本节点的同轨所有节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInSamePlane(int nrofPlane, int nrofSatelliteInOnePlane){
            if (startNumber == 0 && endNumber == 0) {  
                startNumber = LEO_NROF_S_EACHPLANE * (nrofPlane - 1);//此轨道平面内的节点，起始编号
                endNumber = LEO_NROF_S_EACHPLANE * nrofPlane - 1;//此轨道平面内的节点，结尾编号
            }
            System.out.println("init  "+thisNode+"  "+startNumber+"  "+endNumber);
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
                    allHostsInSamePlane.add(host);//同一个轨道内的相邻节点
                }
            }
        }
        /**
         * 初始化找到同一轨道的所有节点，并且设定本节点的同轨邻居节点
         */
        public void initInterSatelliteNeighbors(){

            int thisHostAddress = getHost().getAddress();

            //同轨道平面内所有节点
            findAllSatellitesInSamePlane(thisHostAddress/LEO_NROF_S_EACHPLANE + 1, LEO_NROF_S_EACHPLANE);

            int upperBound = getHosts().size() - 1;
            int a = processBound(thisHostAddress + 1, thisHostAddress/LEO_NROF_S_EACHPLANE + 1, LEO_NROF_S_EACHPLANE);
            int b = processBound(thisHostAddress - 1, thisHostAddress/LEO_NROF_S_EACHPLANE + 1, LEO_NROF_S_EACHPLANE);

            for (DTNHost host : getHosts()){
                if (host.getAddress() == a || host.getAddress() == b){
                    neighborHostsInSamePlane.remove(host);
                    neighborHostsInSamePlane.add(host);//同一个轨道内的相邻节点
                }
            }
        }
        /**
         * return cluster info
         */
        public String toString(){
            return " Manage Hosts: "+manageHosts.toString()+
                    " Hosts in the cluster: ";
        }
    }
    
    /**
     * Stores the cluster information in the MEO node
     */
    private class MEOclusterInfo{
        private DTNHost thisNode;
        /** hosts list in the transmission range of MEO*/
        private List<DTNHost> hostsInTransmissionRange;
        /** confirmed hosts list in the cluster */
        private List<DTNHost> clusterList;
        /** all MEO hosts list */
        private List<DTNHost> MEOList = new ArrayList<DTNHost>();
        /** record LEO nodes in other cluster through MEO confirm messges */
        private HashMap<DTNHost, List<DTNHost>> otherClusterList;
        /** record latest cluster information update time */
        private HashMap<DTNHost, Double> clusterUpdateTime;
        /** neighbor hosts in the same orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectMEOHostsInSamePlane = new ArrayList<DTNHost>();
        /** neighbor hosts in two neighbor orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectMEOHostsInNeighborPlane = new ArrayList<DTNHost>();
        /** all hosts in the same orbit plane*/
        public List<DTNHost> allHostsInSamePlane = new ArrayList<DTNHost>();
        
        /* plane number of this MEO node*/
        private int nrofMEOPlane;
        /* start host number of this MEO plane*/
        private int startNumberInSameMEOPlane;
        /* end host number of this MEO plane*/
        private int endNumberInSameMEOPlane;
        
        public MEOclusterInfo(DTNHost thisNode){
            this.thisNode = thisNode;
            hostsInTransmissionRange = new ArrayList<DTNHost>();
            clusterList = new ArrayList<DTNHost>();
            
            findAllMEONodes();
            otherClusterList = new HashMap<DTNHost, List<DTNHost>>();
            clusterUpdateTime = new HashMap<DTNHost, Double>();
            
            //设置本MEO轨道平面内的开始/结束节点编号，以及MEO平面编号
            setPlaneNumber();
            //同轨道平面内邻居的两个节点
            findAllowConnectMEOHostsInSamePlane();
            //同轨道平面内所有节点
            findAllSatellitesInSamePlane();
            //初始化设定本节点的两个邻居轨道平面所有节点
            findAllSatellitesInNeighborPlane();
        }
        /**
         * 计算本MEO节点所属的轨道参数
         */
        public void setPlaneNumber(){
        	this.nrofMEOPlane = (thisNode.getAddress() - LEO_TOTAL_SATELLITES)/MEO_NROF_S_EACHPLANE + 1;
        	this.startNumberInSameMEOPlane = LEO_TOTAL_SATELLITES + MEO_NROF_S_EACHPLANE * (nrofMEOPlane - 1);//此轨道平面内的节点，起始编号
            this.endNumberInSameMEOPlane = LEO_TOTAL_SATELLITES + MEO_NROF_S_EACHPLANE * nrofMEOPlane - 1;//此轨道平面内的节点，结尾编号
            //System.out.println(thisNode.getAddress()+"  "+LEO_TOTAL_SATELLITES+"  "+MEO_NROF_S_EACHPLANE+"  "+nrofMEOPlane+"  "+startNumberInSameMEOPlane+"  "+endNumberInSameMEOPlane);
        }
        /**
         * 初始化找到所有MEO属性节点
         */
        public List<DTNHost> findAllMEONodes(){
        	MEOList.clear();
        	for (DTNHost h : getHosts()){
        		if (h.getSatelliteType().contains("MEO"))
        			MEOList.add(h);
        	} 
        	return MEOList;
        }
        /**
         * 初始化设定本节点的同轨的邻居节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllowConnectMEOHostsInSamePlane(){ 
            int	startNumber = this.startNumberInSameMEOPlane;//此轨道平面内的节点，起始编号
            int endNumber = this.endNumberInSameMEOPlane;//此轨道平面内的节点，结尾编号
            if (!(thisNode.getAddress() >= startNumber && thisNode.getAddress()<= endNumber)){
            	System.out.println(thisNode+" nrofPlane "+nrofMEOPlane+"  "+this.startNumberInSameMEOPlane+"  "+this.endNumberInSameMEOPlane);
            	throw new SimError("MEO address calculation error");
            }
            int a = thisNode.getAddress() - 1;
            int b = thisNode.getAddress() + 1;
            
            if (a < startNumber)
            	a = endNumber;
            if (b > endNumber)
            	b = startNumber;
            allowConnectMEOHostsInSamePlane.add(findHostByAddress(a));
            allowConnectMEOHostsInSamePlane.add(findHostByAddress(b));
        }
        /**
         * 初始化设定本节点的同轨所有节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInSamePlane(){ 
        	int startNumber = this.startNumberInSameMEOPlane;//此轨道平面内的节点，起始编号
            int endNumber = this.endNumberInSameMEOPlane;//此轨道平面内的节点，结尾编号

            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
                    allHostsInSamePlane.add(host);//同一个轨道内的相邻节点
                }
            }
            //System.out.println(thisNode+"  allowConnectMEOHostsInSamePlane  "+allowConnectMEOHostsInSamePlane);
        }
        /**
         * 初始化设定本节点的两个邻居轨道平面所有节点于allowConnectMEOHostsInNeighborPlane中
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInNeighborPlane(){ 
            int serialNumberOfPlane = this.nrofMEOPlane;
            int a = serialNumberOfPlane - 1;
            int b = serialNumberOfPlane + 1;
            
            if (a < 1)
            	a = MEO_TOTAL_PLANE;
            if(b > MEO_TOTAL_PLANE)
            	b = 1;
            //左邻居MEO轨道平面
            int startNumber1 = LEO_TOTAL_SATELLITES + MEO_NROF_S_EACHPLANE * (a - 1);//此轨道平面内的节点，起始编号
            int endNumber1 = LEO_TOTAL_SATELLITES + MEO_NROF_S_EACHPLANE * a - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber1 && host.getAddress() <= endNumber1){
                	allowConnectMEOHostsInNeighborPlane.add(host);
                }
            }
            //右邻居MEO轨道平面
            int startNumber2 = LEO_TOTAL_SATELLITES + MEO_NROF_S_EACHPLANE * (b - 1);//此轨道平面内的节点，起始编号
            int endNumber2 = LEO_TOTAL_SATELLITES + MEO_NROF_S_EACHPLANE * b - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber2 && host.getAddress() <= endNumber2){
                	allowConnectMEOHostsInNeighborPlane.add(host);
                }
            }
            //System.out.println(thisNode+"  allowConnectMEOHostsInNeighborPlane  "+allowConnectMEOHostsInNeighborPlane);
        }

        /**
         * 动态找到MEO的当前邻居轨道的最近两个节点用户邻居通信
         * @return
         */
        public List<DTNHost> updateAllowConnectMEOHostsInNeighborPlane(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	
        	List<Tuple<DTNHost, Double>> listFromDistance = new ArrayList<Tuple<DTNHost, Double>>();
        	for (DTNHost h : getAllowConnectMEOHostsInNeighborPlane()){
        		listFromDistance.add(new Tuple<DTNHost, Double>(h, getDistance(thisNode, h)));
        	}
        	sort(listFromDistance);
        	for (Tuple<DTNHost, Double> t : listFromDistance){
        		if (list.isEmpty())
        			list.add(t.getKey());
        		else{
        			//不是同一个轨道平面的
        			if (!((OptimizedClusteringRouter)list.get(0).getRouter())
        					.MEOci.getAllowConnectMEOHostsInSamePlane().contains(t.getKey())){
        					list.add(t.getKey());	
        					}
        		}
        		if (list.size() >= 2)
        			break;
        	}
        	return list;
        }
        /**
         * 同一轨道内的所有节点
         * @return
         */
        public List<DTNHost> getAllowConnectMEOHostsInSamePlane(){
        	return allowConnectMEOHostsInSamePlane;
        }
        /**
         * 邻居轨道内的所有节点
         * @return
         */
        public List<DTNHost> getAllowConnectMEOHostsInNeighborPlane(){
        	return allowConnectMEOHostsInNeighborPlane;
        }
        /**
         * update hosts list that in transmission range of this MEO
         */
        public void clearHostsInTransmissionRange(){
            hostsInTransmissionRange.clear();
        }
        /**
         * @return all LEO hosts in transmission range
         */
        public List<DTNHost> getHostsInTransmissionRange(){
            return hostsInTransmissionRange;
        }
        /**
         * initialize LEO cluster list managed by this MEO node
         */
//        public void initClusterList(List<Integer> nearnestPlane){
//            for (int n = 0; n < nearnestPlane.size(); n++){
//                for (DTNHost host : getHosts()){
//                    int startNumber = LEO_NROF_S_EACHPLANE * (nearnestPlane.get(n) - 1);//此轨道平面内的节点，起始编号
//                    int endNumber = LEO_NROF_S_EACHPLANE * nearnestPlane.get(n) - 1;//此轨道平面内的节点，结尾编号
//
//                    //找出当前平面内的所有节点，加入此MEO所管理的分簇中
//                    if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
//                        if (!host.getSatelliteType().contains("LEO"))
//                            throw new SimError("Clustering Calculation error!");
//                        //同时对相应的LEO添加本节点作为管理节点
//                        ((OptimizedClusteringRouter)host.getRouter()).LEOci.addManageHost(thisNode);
//                        clusterList.add(host);//添加分簇内的节点
//                    }
//                }
//            }
//        }
        /**
         * update cluster member according to connection
         */
        public void updateClusterMember(){
        	clusterList.clear();
        	clusterList.addAll(getConnectedLEOHosts());
        	
        }
        /**
         * 获取当前通信范围内的LEO节点
         * @return
         */
        public List<DTNHost> getConnectedLEOHosts(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	for (Connection con : thisNode.getConnections()){
        		DTNHost h = con.getOtherNode(thisNode);
        		if (!list.contains(h) && h.getSatelliteType().contains("LEO"))
        			list.add(h);
        	}
        	return list;
        }
        /**
         * @return cluster list
         */
        public List<DTNHost> getClusterList(){
            return clusterList;
        }
        /**
         * delete other unaccessible MEO node
         * @param h
         */
        public void removeMEONode(DTNHost h){
            MEOList.remove(h);
        }
        /**
         * @return other MEO nodes list in the network
         */
        public List<DTNHost> getMEOList(){
            return MEOList;
        }
        /**
         * find specific node in other cluster list
         * @param to
         * @return
         */
        public DTNHost findHostInOtherClusterList(DTNHost to){
            for (DTNHost MEO : MEOList){
                if (MEO == thisNode)
                    continue;
                for (DTNHost LEO : ((OptimizedClusteringRouter)MEO.getRouter()).MEOci.getClusterList()){
                    if (LEO == to)
                        return MEO;
                }
            }
            return null;
        }
        /**
         * @return other MEO cluster information
         */
        public HashMap<DTNHost, List<DTNHost>> getOtherClusterList(){
            return otherClusterList;
        }
        /**
         * @return the latest other cluster information update time
         */
        public HashMap<DTNHost, Double> getClusterUpdateTime(){
            return clusterUpdateTime;
        }
        public String toString(){
            return " Other MEO Hosts: " + MEOList.toString() +
                    " Hosts in the cluster: " + clusterList.toString() +
                    " other cluster: " + otherClusterList.toString() +
                    "  clusterUpdateTime:  " + clusterUpdateTime;
        }
    }
}
