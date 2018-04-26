/*
 * Copyright 2017 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package routing;

import java.util.*;

import util.Tuple;
import routing.SatelliteInterLinkInfo.GEOclusterInfo;
import routing.SatelliteInterLinkInfo.LEOclusterInfo;
import routing.SatelliteInterLinkInfo.MEOclusterInfo;
import core.*;
import movement.MovementModel;
import movement.SatelliteMovement;
import static core.SimClock.getTime;
import static java.lang.Math.abs;


public class DynamicMultiLayerSatelliteRouter extends ActiveRouter {
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
    /**
     * Decides the message transmitted through radio link or laser link
     * according to this message size threshold， -setting id ({@value})
     */
    public static final String MSG_SIZE_THRESHOLD_S = "MessageThreshold";
    /** indicates the type of link*/
    public static final String LASER_LINK = "LaserInterface";
    /** indicates the type of link*/
	public static final String RADIO_LINK = "RadioInterface";
    /** light speed，approximate 3*10^8m/s */
    private static final double LIGHTSPEED = 299792458;

    /** indicate the transmission radius of each satellite -setting id ({@value} */
    private static double transmitRange;
    /** label indicates that routing path can contain in the message or not -setting id ({@value} */
    private static boolean msgPathLabel;
    /** indicates the TTL of confirm message -setting id ({@value} */
    private static int confirmTtl;
    /** the message size threshold, decides the message transmitted 
     *  through radio link or laser link -setting id ({@value}*/
    private static int msgThreshold;
    /** the optimized label, if it turns on, all routing will use the 
     * shortest path search, which is optimized but slow -setting id ({@value}*/
    private static boolean OptimizedRouting;
    
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
    
    /** total number of GEO satellites*/
    private static int GEO_TOTAL_SATELLITES;//总节点数
    /** total number of GEO plane*/
    private static int GEO_TOTAL_PLANE;//总轨道平面数
    /** number of hosts in each GEO plane*/
    private static int GEO_NROF_S_EACHPLANE;//每个平面上的卫星数

    /** label indicates that if LEO_MEOClustering is initialized*/
    private static boolean LEO_MEOClusteringInitLable;

    /** label indicates that routing algorithm has been executed or not at this time */
    private boolean routerTableUpdateLabel;
    /** maintain the earliest arrival time to other nodes */
    private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /** the router table comes from routing algorithm */
    private HashMap<DTNHost, List<Tuple<Integer, Boolean>>>
            routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();
	/** number of different interface*/
    public int nrofRadioInterface;
	public int nrofSendingLaserInterface;
	public int nrofSendingRadioInterface;
	

    public DynamicMultiLayerSatelliteRouter(Settings s) {
        super(s);
    }

    protected DynamicMultiLayerSatelliteRouter(DynamicMultiLayerSatelliteRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new DynamicMultiLayerSatelliteRouter(this);
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        
        Settings s1 = new Settings("Interface1");
        nrofRadioInterface = s1.getInt("nrofRadioInterface");
        
        if (!initLabel){ 
        	//LEO
            Settings sat = new Settings("Group");
            LEO_TOTAL_SATELLITES = sat.getInt("nrofLEO");//总节点数
            LEO_TOTAL_PLANE = sat.getInt("nrofLEOPlanes");//总轨道平面数
            LEO_NROF_S_EACHPLANE = LEO_TOTAL_SATELLITES/LEO_TOTAL_PLANE;//每个轨道平面上的节点数
            //MEO
            Settings s = new Settings("Group");
            if (s.getBoolean("EnableMEO")){
                MEO_TOTAL_SATELLITES = s.getInt("nrofMEO");//总节点数
                MEO_TOTAL_PLANE = s.getInt("nrofMEOPlane");//总轨道平面数
                MEO_NROF_S_EACHPLANE = MEO_TOTAL_SATELLITES/MEO_TOTAL_PLANE;//每个轨道平面上的节点数
            }
            //GEO
            if (s.getBoolean("EnableGEO")){
                GEO_TOTAL_SATELLITES = s.getInt("nrofGEO");//总节点数
                GEO_TOTAL_PLANE = s.getInt("nrofGEOPlane");//总轨道平面数
                GEO_NROF_S_EACHPLANE = GEO_TOTAL_SATELLITES/GEO_TOTAL_PLANE;//每个轨道平面上的节点数
            }
                        
            random = new Random();
            s.setNameSpace(INTERFACENAME_S);
            transmitRange = s.getInt(TRANSMIT_RANGE_S);
            msgThreshold = s.getInt(MSG_SIZE_THRESHOLD_S);
            
            s.setNameSpace(GROUPNAME_S);
            msgPathLabel = s.getBoolean(MSG_PATHLABEL);
            confirmTtl = s.getInt(COMFIRMTTL_S);
                    
            s.setNameSpace("DynamicMultiLayerSatelliteRouter");
            OptimizedRouting = s.getBoolean("Optimized");
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
       
        //根据先验信息对LEO进行分组，并确定各个LEO的固定管理MEO节点，从而无需信令交互进行动态分簇
//        if (!LEO_MEOClusteringInitLable)
//            initLEO_MEOClusteringRelationship();
        
        //update dynamic clustering information
        if (!clusteringUpdate()){
            //TODO deal with isolate LEO node
            return; // for isolate LEO node, it does noting
        }

//        if (isTransferring()) { // judge the link is occupied or not
//            return; // can't start a new transfer
//        }
        
        //helloProtocol();//执行hello包的维护工作
        if (!canStartTransfer())
            return;

        /** Set router update label to make sure that routing algorithm only execute once at a time */
        routerTableUpdateLabel = false;

        /** sort the messages to transmit */
        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
        List<Message> messages = sortByQueueMode(messageList);

        // try to send the message in the message buffer
        for (Message msg : messages) {
        	if (checkMsgShouldGetRoutingOrNot(msg) == false) 
        		continue;
//            //Confirm message's TTL only has 1 minutes, will be drop by itself
//            if (msg.getId().contains("Confirm") || msg.getId().contains("ClusterInfo"))
//                continue;
            if (findPathToSend(msg) == true)
                return;
        }

    }
    
    /**
     * Try to send the message through a specific connection.
     *
     * @param t
     * @return
     */
    public boolean sendMsg(Tuple<Message, Connection> t) {    	
        if (t == null || t.getValue() == null) {
            //throw new SimError("send msg error!");
            return false;
        } else {
        	// 判断网络接口个数，满足同时传输链路数目要求！
        	nrofSendingLaserInterface = 0;	nrofSendingRadioInterface = 0; 
        	for(Connection con : this.sendingConnections ){       		
        		if(con.getLinkType().equals("RadioInterface")){
        			nrofSendingRadioInterface++;
        		}else if(con.getLinkType().equals("LaserInterface")){
        			nrofSendingLaserInterface++;
        		}
        	}
        	if(t.getValue().getLinkType().equals("RadioInterface")){
        		if(nrofSendingRadioInterface>=nrofRadioInterface){
            		return false;
        		}
        	}else if(t.getValue().getLinkType().equals("LaserInterface")){
        		if(nrofSendingLaserInterface>=1){
            		return false;
        		}
        	}
        	
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
     * check if msg is being sending or not
     * @param msg
     * @return
     */
    public boolean checkMsgShouldGetRoutingOrNot(Message msg){
    	for (Connection con : this.sendingConnections){
    		if (msg.getId().contains(con.getMessage().getId())){
    			//throw new SimError("error" );
    			return false;//this msg shouldn't be sended again
    		}
    	}
    	return true;
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
     * update clustering information
     */
    public boolean clusteringUpdate(){
    	return this.getSatelliteLinkInfo().clusteringUpdate();
    }
    /**
     * periodically send hello packet to neighbor satellite nodes to check their status
     */
    public void helloProtocol(){
        // TODO helloProtocol
    }
    
    /**
     * Update router table, find a routing path and try to send the message
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
            Connection nextCon = findConnection(nextHopAddress, msg);
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
        
        Connection firstHop = findConnection(routerPath.get(0).getKey(), message);
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
            case "GEO":{
            	//TODO
            	GEOroutingPathSearch(msg);
            	break;
            }
        }

        if (this.routerTable.containsKey(msg.getTo())) {
//            System.out.println("find the path!  " +
//            		this.routerTable.get(msg.getTo())+"   "+ getSatelliteType() 
//            				+" to "+ msg.getTo().getSatelliteType()+"  " + msg);
            return true;
        } else {
            return false;
        }
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void LEOshortestPathSearch(Message msg) {
    	LEOclusterInfo LEOci = this.getSatelliteLinkInfo().getLEOci();
    	
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                //目的节点是否在自身所属轨道平面上
                if (LEOci.getAllHostsInSamePlane().contains(to)){ 
//                	System.out.println(this.getHost() +"  "+ to );
                    findPathInSameLEOPlane(this.getHost(), to);
                }
                else{
                	//对于发往不是同一轨道平面上的消息，统一先送往最近的通信节点
                	//1、作为通信节点
                	if (this.getHost().getRouter().CommunicationSatellitesLabel){
                        //检查目的节点是否在邻居轨道平面上
                        List<DTNHost> hostsInNeighborOrbitPlane = LEOci.ifHostsInNeighborOrbitPlane(to);
                        if (hostsInNeighborOrbitPlane != null){//不为空，则说明在邻居轨道上，且返回的是邻居轨道的所有节点
                        	//先尝试通过邻居轨道通信节点转发
                        	if(msgFromLEOForwardToNeighborPlane(msg, to))
                        		return;
                        }
                        //否则，直接通过MEO管理节点转发
                        msgFromCommunicationLEOForwardedByMEO(msg, to);
                    	
                	}
                	//2、作为LEO遥感节点，直接先把数据传到通信节点上
                	else{
                    	DTNHost communicationLEO = findNearestCommunicationLEONodes(this.getHost());
                    	List<Tuple<Integer, Boolean>> path = findPathInSameLEOPlane(this.getHost(), communicationLEO);
                    	
                        if (!path.isEmpty()){
//                        	System.out.println("先交给通信LEO节点进行转发   to" + to);
                            routerTable.put(to, path);
                        }
                	}
                }
                break;
            }
            case "MEO":{
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                
            	if (this.getHost().getRouter().CommunicationSatellitesLabel){
            		//如果自身是通信节点，则先发送到与自己相连接的MEO节点上
            		//TODO
                   	List<DTNHost> searchArea = new ArrayList<DTNHost>();                  	
                   	searchArea.addAll(findMEOHosts());
                   	searchArea.add(this.getHost());
                   	//this.getMEOtoMEOTopology();
                	optimzedShortestPathSearch(msg, searchArea);
//                	if (this.routerTable.get(to) != null)
//                		System.out.println(this.getHost()+" 找到了LEO to MEO 的路径"+msg);
            	}
            	//作为LEO遥感节点，直接先把数据传到通信节点上
            	else{
                	DTNHost communicationLEO = findNearestCommunicationLEONodes(this.getHost());
                	List<Tuple<Integer, Boolean>> path = findPathInSameLEOPlane(this.getHost(), communicationLEO);
                	
                    if (!path.isEmpty()){
//                    	System.out.println("先交给通信LEO节点进行转发   to" + to);
                        routerTable.put(to, path);
                    }
            	}                                          
                break;
            }
            case "GEO":{
            	//TODO
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                if (to.getRouter().CommunicationSatellitesLabel){
                    //TODO 构建LEO和GEO的拓扑
                    HashMap<DTNHost, List<DTNHost>> topologyInfo = 
                    		getGEOtoLEOTopology(msg, to, this.getHost());//optimizedTopologyCalculation(MEOci.MEOList);//localTopologyCalculation(MEOci.MEOList);          
                    //调用搜索算法
                    DTNHost nearestCLEO = findNearestCommunicationLEONodes(this.getHost());
                    List<DTNHost> localHostsList = new ArrayList<DTNHost>();
                    localHostsList.addAll(findGEOHosts());
                    localHostsList.addAll(findMEOHosts());
                    localHostsList.add(nearestCLEO);
                	this.shortestPathSearch(msg, topologyInfo, localHostsList);
//                	if (this.routerTable.containsKey(to))
//                		System.out.println("LEO to GEO" + this.getHost() + "找到了最短路径");
                	
                }
                //否则先交给通信节点
                else{
                	DTNHost communicationLEO = findNearestCommunicationLEONodes(this.getHost());
                	List<Tuple<Integer, Boolean>> path = findPathInSameLEOPlane(this.getHost(), communicationLEO);
                	
                    if (!path.isEmpty()){
//                    	System.out.println("先交给通信LEO节点进行转发   to" + to);
                        routerTable.put(to, path);
                    }
                }
            	break;
            }
        }
    }
    /**
     * Core routing logic for MEO satellite
     * @param msg
     */
    public void MEOroutingPathSearch(Message msg) {
    	MEOclusterInfo MEOci = this.getSatelliteLinkInfo().getMEOci();   	
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
            	  if (OptimizedRouting){
                  	optimzedShortestPathSearch(msg, this.getHosts());
                  	return;
            	  }
                //目的节点是否是管理节点
            	if (to.getRouter().CommunicationSatellitesLabel){
            		//通过MEO节点直接传给LEO
            		 HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoCommunicationLEOTopology(msg, to);//构建拓扑
            		 //限定搜索的节点范围
            		 List<DTNHost> localHostsList = new ArrayList<DTNHost>(findMEOHosts());
            		 localHostsList.add(to);
            		 shortestPathSearch(msg, topologyInfo, localHostsList);
            	}
            	else{
            		DTNHost nearestCLEO = findNearestCommunicationLEONodes(to);
	            	//先发给目的LEO最近的通信LEO节点上，再由它进行处理
	           		HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoCommunicationLEOTopology(msg, nearestCLEO);//构建拓扑
	           		//限定搜索的节点范围
	           		List<DTNHost> localHostsList = new ArrayList<DTNHost>(findMEOHosts());
	           		localHostsList.add(nearestCLEO);
	           		shortestPathSearch(msg, topologyInfo, localHostsList);
	           		
	            	if (this.routerTable.containsKey(nearestCLEO)){
//	            		System.out.println("搜索到通过MEO转发的最短路径！ to" + to);
	            		this.routerTable.put(to, this.routerTable.get(nearestCLEO));//添加去目的节点的路径
	            		return;
	            	}  
            	}
                break;
            }
            case "MEO":{
            	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
            	allMEOandGEO.addAll(findMEOHosts());
            	allMEOandGEO.addAll(findGEOHosts());
            	
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
                //改造的最短路径算法，用于特殊场景，需要指定出发源节点，并给定网络拓扑
                HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoMEOTopology();//构建拓扑
                //TODO 拓扑中应该添加GEO节点
                //shortestPathSearch(msg, topologyInfo, allMEOandGEO);    
                shortestPathSearch(msg, topologyInfo, findMEOHosts()); 
                break;
            }
            case "GEO":{
               	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
            	allMEOandGEO.addAll(findMEOHosts());
            	allMEOandGEO.addAll(findGEOHosts());
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
              //改造的最短路径算法，用于特殊场景，需要指定出发源节点，并给定网络拓扑
                HashMap<DTNHost, List<DTNHost>> topologyInfo = getMEOtoMEOTopology();//构建拓扑
                //TODO 构建MEO和GEO的拓扑
                throw new SimError("MEO to GEO");
                //shortestPathSearch(msg, topologyInfo, allMEOandGEO);  
            }
        }
    }
    /**
     * Core routing logic for GEO satellite
     * @param msg
     */
    public void GEOroutingPathSearch(Message msg) {
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
            case "LEO":{
                if (OptimizedRouting){
                	optimzedShortestPathSearch(msg, this.getHosts());
                	return;
                }
                
                //找到管理此节点的MEO节点，交于它进行转发
            	/**采用最短路径搜索算法的变种来找最优路径**/          
                HashMap<DTNHost, List<DTNHost>> topologyInfo = 
                		getGEOtoLEOTopology(msg, this.getHost(), to);//optimizedTopologyCalculation(MEOci.MEOList);//localTopologyCalculation(MEOci.MEOList);          
                //调用搜索算法
                DTNHost nearestCLEO = findNearestCommunicationLEONodes(to);
                List<DTNHost> localHostsList = new ArrayList<DTNHost>();
                localHostsList.addAll(findGEOHosts());
                localHostsList.addAll(findMEOHosts());
                localHostsList.add(nearestCLEO);
            	this.shortestPathSearch(msg, topologyInfo, localHostsList);
//            	if (this.routerTable.containsKey(to))
//            		System.out.println("GEO" + this.getHost() + "找到了最短路径");
            	
            	if (!to.getRouter().CommunicationSatellitesLabel){
	            	if (this.routerTable.containsKey(nearestCLEO)){
//	            		System.out.println("搜索到通过MEO转发的最短路径！ to" + to);
	            		this.routerTable.put(to, this.routerTable.get(nearestCLEO));//添加去目的节点的路径
	            		return;
	            	}  
            	}
            	/**采用最短路径搜索算法的变种来找最优路径**/
            	
                break;
            }
            case "MEO":{
                if (OptimizedRouting){
                   	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
                	allMEOandGEO.addAll(findMEOHosts());
                	allMEOandGEO.addAll(findGEOHosts());
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
                //TODO
                //shortestPathSearch(msg, this.getHost(), getGEOtoMEOTopology(this.getHost(), to));
                //shortestPathSearch(msg, MEOci.getMEOList());
                break;
            }
            case "GEO":{ 
                if (OptimizedRouting){
                   	List<DTNHost> allMEOandGEO = new ArrayList<DTNHost>();
                	allMEOandGEO.addAll(findMEOHosts());
                	allMEOandGEO.addAll(findGEOHosts());
                	optimzedShortestPathSearch(msg, allMEOandGEO);
                	return;
                }
                //TODO
            	//shortestPathSearch(msg, this.getHost(), getGEOtoGEOTopology(to));
            	break;
            }
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
     * Return current network topology in forms of current topology graph
     */
    public HashMap<DTNHost, List<DTNHost>> localTopologyGeneration(Message msg, List<DTNHost> allHosts) {
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        //get all 
        //TODO better
        for (DTNHost h : allHosts) {
        	for (Connection con : h.getConnections()){
        		if (!isRightConnection(msg, con))
        			continue;
        		DTNHost otherNode = con.getOtherNode(h);
                if (topologyInfo.get(h) == null)
                    topologyInfo.put(h, new ArrayList<DTNHost>());
                List<DTNHost> neighborList = topologyInfo.get(h);
                neighborList.add(otherNode);
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
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void shortestPathSearch(Message msg, HashMap<DTNHost, List<DTNHost>> topologyInfo, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty() || topologyInfo.isEmpty())
            return;

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**全网的传输速率假定为一样的**/
        double transmitSpeed;
        if(msg.getSize() < msgThreshold ){
        	transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        } else{
        	transmitSpeed = this.getHost().getInterface(2).getTransmitSpeed();
        }
//        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**表示路由开始的时间**/

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//初始时只有源节点所
        searchedSet.add(this.getHost());//初始时只有源节点

        for (Connection con : this.getHost().getConnections()) {//添加链路可探测到的一跳邻居，并更新路由表
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            if (!isRightConnection(msg, con))//判断是否是正确的链路，配备多接口后需要进行检查
            	continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//初始时只有本节点和链路邻居
//            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            Double time = getTime() + msg.getSize() / transmitSpeed;
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
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void optimzedShortestPathSearch(Message msg, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty())
            return;
        //update the current topology information
        HashMap<DTNHost, List<DTNHost>> topologyInfo = localTopologyGeneration(msg, localHostsList);

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();

        /**全网的传输速率假定为一样的**/
        double transmitSpeed;
        if(msg.getSize() < msgThreshold ){
        	transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        } else{
        	transmitSpeed = this.getHost().getInterface(2).getTransmitSpeed();
        }
//        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        /**表示路由开始的时间**/ 

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//初始时只有源节点所
        searchedSet.add(this.getHost());//初始时只有源节点

        for (Connection con : this.getHost().getConnections()) {//添加链路可探测到的一跳邻居，并更新路由表
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            if (!isRightConnection(msg, con))//判断是否是正确的链路，配备多接口后需要进行检查
            	continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//初始时只有本节点和链路邻居
//            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            Double time = getTime() + msg.getSize() / transmitSpeed;
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
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void shortestPathSearch(Message msg, DTNHost to, List<DTNHost> localHostsList) {
        if (localHostsList.isEmpty())
            return;
        //update the current topology information
        HashMap<DTNHost, List<DTNHost>> topologyInfo = localTopologyGeneration(msg, localHostsList);

        if (routerTableUpdateLabel == true)
            return;
        this.routerTable.clear();
        this.arrivalTime.clear();
        
        /**全网的传输速率假定为一样的**/
//        double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        double transmitSpeed;
        if(msg.getSize() < msgThreshold ){
        	transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
        } else{
        	transmitSpeed = this.getHost().getInterface(2).getTransmitSpeed();
        }
        /**表示路由开始的时间**/

        /**添加链路可探测到的一跳邻居网格，并更新路由表**/
        List<DTNHost> searchedSet = new ArrayList<DTNHost>();
        List<DTNHost> sourceSet = new ArrayList<DTNHost>();
        sourceSet.add(this.getHost());//初始时只有源节点所
        searchedSet.add(this.getHost());//初始时只有源节点

        for (Connection con : this.getHost().getConnections()) {//添加链路可探测到的一跳邻居，并更新路由表
            if (!localHostsList.contains(con.getOtherNode(this.getHost())))
                continue;
            if (!isRightConnection(msg, con))//判断是否是正确的链路，配备多接口后需要进行检查
            	continue;
            DTNHost neiHost = con.getOtherNode(this.getHost());
            sourceSet.add(neiHost);//初始时只有本节点和链路邻居
//            Double time = getTime() + msg.getSize() / this.getHost().getInterface(1).getTransmitSpeed();
            Double time = getTime() + msg.getSize() / transmitSpeed;
            
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
    public DTNHost chooseOneNeighborHostToSendInSameLEOPlane(DTNHost src, DTNHost to){
    	DynamicMultiLayerSatelliteRouter srcRouter = (DynamicMultiLayerSatelliteRouter)src.getRouter();
    	
    	LEOclusterInfo LEOci = srcRouter.getSatelliteLinkInfo().getLEOci();
    	
        if (LEOci.getNeighborHostsInSamePlane().size() != 2)
            throw new SimError("LEOci.getNeighborHostsInSamePlane() error!");
        DTNHost a = LEOci.getNeighborHostsInSamePlane().get(0);
        DTNHost b = LEOci.getNeighborHostsInSamePlane().get(1);
        DTNHost nextHop = null;
        if (abs(to.getAddress() - a.getAddress()) > abs(to.getAddress() - b.getAddress()))
            nextHop = b;
        else
            nextHop = a;
        return nextHop;
    }
    /**
     * find the path from this node to another LEO node in the same plane
     * @param to
     */
    public List<Tuple<Integer, Boolean>> findPathInSameLEOPlane(DTNHost srcLEO, DTNHost to){
        List<Tuple<Integer, Boolean>> path =
                new ArrayList<Tuple<Integer, Boolean>>();//记录最终路径
        
        DynamicMultiLayerSatelliteRouter srcRouter = (DynamicMultiLayerSatelliteRouter)srcLEO.getRouter();
        
        //如果路径位打开了，就直接找到整个路径，然后写入
        DTNHost nextHop = srcLEO;
    	for (int i = 0; i < srcRouter.getSatelliteLinkInfo().
    			getLEOci().getAllHostsInSamePlane().size() ; i++){//防止无法跳出的错误
    		nextHop = chooseOneNeighborHostToSendInSameLEOPlane(nextHop, to);
    		path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
    		//已经到达了目的节点，则路径搜索结束
    		if (nextHop.getAddress() == to.getAddress()){
    			srcRouter.routerTable.put(to, path); 
    			break;
    		}
    	}
//    	System.out.println(this.getHost()+"  同一个平面内的路径： "+path);
        return path;
    }
 
    /**
     * LEO信息发往邻居轨道平面
     * @param to
     */
    public boolean msgFromLEOForwardToNeighborPlane(Message msg, DTNHost to){
    	
    	int destinationSerialNumberOfPlane = to.getAddress()/LEO_NROF_S_EACHPLANE + 1;
//    	System.out.println("forward to neighbor plane   "+destinationSerialNumberOfPlane);
    	List<DTNHost> allCommunicationNodes = new ArrayList<DTNHost>();
    	//找出所有目的节点轨道平面上的可以支持跨平面通信的卫星
    	for (DTNHost h : this.CommunicationNodesList.keySet()){//这里的CommunicationNodesList里记录的轨道平面编号是从0开始的
    		if (this.CommunicationNodesList.get(h) + 1 == destinationSerialNumberOfPlane)
    			allCommunicationNodes.add(h);
    	}
//    	System.out.println("all communication nodes: "+allCommunicationNodes);
//    	System.out.println("在邻居轨道! 邻居轨道上可通信节点： "+allCommunicationNodes+" connections: "+this.getConnections());
    	for (DTNHost h : allCommunicationNodes){
    		Connection con = this.findConnection(h.getAddress(), msg);
    		if (con != null){
                List<Tuple<Integer, Boolean>> path =
                        new ArrayList<Tuple<Integer, Boolean>>();
                path.add(new Tuple<Integer, Boolean>(h.getAddress(), false));
                routerTable.put(to, path);
//                System.out.println(this.getHost()+"  同一个平面内的路径： "+path);
                return true;
    		}
    	}   
    	//没有与通信卫星相遇
    	return false;
    }
    /**
     * find the nearest communication LEO nodes in the same orbit plane
     * @param LEO
     * @return
     */
    public DTNHost findNearestCommunicationLEONodes(DTNHost LEO){
    	if (LEO.getRouter().CommunicationSatellitesLabel)
    		return LEO;
    	int min = Integer.MAX_VALUE;
    	DTNHost minHost = null;
    	//取出本轨道平面内所有的通信节点
    	for (DTNHost cLEO : ((SatelliteMovement)LEO.getMovementModel()).
    			getSatelliteLinkInfo().getLEOci().getAllCommunicationNodes()){
    		int distance = Math.abs(cLEO.getAddress() - LEO.getAddress());
    		if (distance < min){
    			min = distance;
    			minHost = cLEO;
    		}
    		else{
    			if (distance == min && random.nextBoolean()){
	    			min = distance;
	    			minHost = cLEO;
    			}
    		}
    	}
    	return minHost;
    }
    /**
     * 执行从LEO信息交由MEO转发
     * @param to
     */
    public void msgFromCommunicationLEOForwardedByMEO(Message msg, DTNHost to){
    	LEOclusterInfo LEOci = this.getSatelliteLinkInfo().getLEOci();
    	
    	if (this.getHost().getRouter().CommunicationSatellitesLabel &&
    			LEOci.updateManageHosts(msg).isEmpty()){
//            System.out.println(this.getHost()+" 通信节点LEO 孤立！没有MEO连接！  "+msg);
    		return;
    	}
    	   	
    	if (((SatelliteMovement)to.getMovementModel()).getSatelliteLinkInfo().getLEOci() == null){
//    		System.out.println(msg+" not initiliation LEOci! "+to);
    		throw new SimError("not initiliation LEOci!");
    		//return;
    	}
    	
    	/**采用最短路径搜索算法的变种来找最优路径**/
        //获取LEO通过MEO网络到达目的LEO的拓扑
        //改造的最短路径算法，用于特殊场景，需要指定出发源节点，并给定网络拓扑
    	
        //shortestPathSearch(msg, this.getHost(), getLEOtoLEOThroughMEOTopology(msg, this.getHost(), to));
        
    	//找到所有MEO节点
        List<DTNHost> hostsList = findMEOHosts();
        DTNHost nearestCLEOtoDestination = findNearestCommunicationLEONodes(to);
        hostsList.add(nearestCLEOtoDestination);
        hostsList.add(this.getHost());
        /*一定是从通信节点发送到MEO进行转发*/
        shortestPathSearch(msg, nearestCLEOtoDestination, hostsList);//搜索得到去王离目的节点最近的通信节点的路径
    	/**采用最短路径搜索算法的变种来找最优路径**/
        
        List<Tuple<Integer, Boolean>> lastPath = findPathInSameLEOPlane(nearestCLEOtoDestination, to);
    	
    	if (to.getRouter().CommunicationSatellitesLabel == false 
    			&& this.routerTable.containsKey(nearestCLEOtoDestination)){
//    		System.out.println(msg+ " 搜索到通过MEO转发的最短路径！ to" + to);
    		List<Tuple<Integer, Boolean>> path = this.routerTable.get(nearestCLEOtoDestination);
    		path.addAll(lastPath);
    		this.routerTable.put(to, path);//添加去目的节点的路径
    		return;
    	}   	    	
    }
    
    /**
     * 先得到起始LEO到最近通信LEO的路径，再得到通信LEO到MEO以及MEO层的拓扑，最后得到目的LEO到其最近通信LEO的拓扑，组合在一起
     * 每一个MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getLEOtoLEOThroughMEOTopology(Message msg, DTNHost startLEO, DTNHost endLEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
//    	//如果是动态分簇路由会执行更新操作，如果是静态分簇路由则直接返回事先规定的MEO管理节点
//    	List<DTNHost> manageHosts = ((SatelliteMovement)endLEO.getMovementModel()).
//    			getSatelliteLinkInfo().getLEOci().updateManageHosts(msg);
//    	if (manageHosts.isEmpty())
//    		return topologyInfo;//返回为空
    	   	
    	topologyInfo = getMEOtoLEOTopology(msg, endLEO);
    	/**找到startLEO的最近通信节点，用于与MEO进行通信,并添加相应的拓扑**/ 	
    	topologyInfo.putAll(getLEOtoNearestCommunicationLEOTopology(startLEO));
    	
//    	for (DTNHost MEO : manageHosts){
//    		List<DTNHost> list = topologyInfo.get(MEO);  	
//            if (list == null) {
//            	list = new ArrayList<DTNHost>();
//                list.add(startLEO);
//            } else {
//            	list.add(startLEO);
//            }
//    	}
    	return topologyInfo;
    }   
    /**
     * 每一个GEO和MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getGEOtoGEOTopology(DTNHost endGEO){
    	GEOclusterInfo GEOci = ((SatelliteMovement)endGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();   	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	for (DTNHost GEO : GEOci.getGEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(GEOci.getAllowConnectGEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(GEOci.updateAllowConnectGEOHostsInNeighborPlane());  
    		topologyInfo.put(GEO, neighborNodes);
    	}

    	return topologyInfo;
    }
    /**
     * 每一个GEO和MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoGEOTopology(DTNHost sMEO, DTNHost endGEO){
    	GEOclusterInfo endGEOci = ((SatelliteMovement)endGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();
    	MEOclusterInfo sMEOci = ((SatelliteMovement)sMEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	//GEO层的节点拓扑
    	for (DTNHost GEO : endGEOci.getGEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(endGEOci.getAllowConnectGEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(endGEOci.updateAllowConnectGEOHostsInNeighborPlane());  
    		topologyInfo.put(GEO, neighborNodes);
    	}
    	//MEO层的节点拓扑
    	for (DTNHost MEO : sMEOci.getMEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(sMEOci.getAllowConnectMEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(sMEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	//拓扑中添加GEO到目的MEO的链路
    	for (DTNHost MEO : sMEOci.getMEOList()){
    		List<DTNHost> list = topologyInfo.get(MEO);  
        	MEOclusterInfo MI = ((SatelliteMovement)MEO.
        			getMovementModel()).getSatelliteLinkInfo().getMEOci();
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.addAll(MI.getConnectedGEOHosts());
            } else {
            	list.addAll(MI.getConnectedGEOHosts());
            }
    	}
    	return topologyInfo;
    }
    /**
     * 每一个GEO和MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getGEOtoMEOTopology(DTNHost sGEO, DTNHost endMEO){
    	GEOclusterInfo GEOci = ((SatelliteMovement)sGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();
    	MEOclusterInfo MEOci = ((SatelliteMovement)endMEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	//GEO层的节点拓扑
    	for (DTNHost GEO : GEOci.getGEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(GEOci.getAllowConnectGEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(GEOci.updateAllowConnectGEOHostsInNeighborPlane());  
    		topologyInfo.put(GEO, neighborNodes);
    	}
    	//MEO层的节点拓扑
    	for (DTNHost MEO : MEOci.getMEOList()){
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(MEOci.getAllowConnectMEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	//拓扑中添加GEO到目的MEO的链路
    	for (DTNHost GEO : GEOci.getGEOList()){
    		List<DTNHost> list = topologyInfo.get(GEO);  
        	GEOclusterInfo GI = ((SatelliteMovement)GEO.
        			getMovementModel()).getSatelliteLinkInfo().getGEOci();
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.addAll(GI.getConnectedMEOHosts());
            } else {
            	list.addAll(GI.getConnectedMEOHosts());
            }
    	}
    	return topologyInfo;
    }
    /**
     * 每一个GEO/MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始MEO点
     * @param endMEO   目的LEO节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getGEOtoLEOTopology(Message msg, DTNHost sGEO, DTNHost endLEO){
    	DTNHost nearestCLEO = this.findNearestCommunicationLEONodes(endLEO);
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	LEOclusterInfo nearestCLEOci = ((SatelliteMovement)nearestCLEO.getMovementModel()).getSatelliteLinkInfo().getLEOci();
    	GEOclusterInfo sGEOci = ((SatelliteMovement)sGEO.getMovementModel()).getSatelliteLinkInfo().getGEOci();

    	topologyInfo = getMEOtoMEOTopology(this.findMEOHosts());//首先添加MEO层的所有链路
    	//拓扑中添加本GEO到所有相连的MEO的链路
    	for (DTNHost MEO : sGEOci.updateGEOClusterMember()){
    		List<DTNHost> list = topologyInfo.get(MEO);  	
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.add(sGEO);
            } else {
            	list.add(sGEO);
            }
    	}
    	
    	topologyInfo.putAll(getGEOtoGEOTopology(sGEO));//添加GEO层的拓扑
    	
    	//拓扑中添加MEO到目的LEO的链路
    	for (DTNHost MEO : nearestCLEOci.updateManageHosts(msg)){
    		List<DTNHost> list = topologyInfo.get(MEO);  	
            if (list == null) {
            	list = new ArrayList<DTNHost>();
                list.add(endLEO);
            } else {
            	list.add(endLEO);
            }
    	}	
    	//添加LEO到MEO的链路
    	topologyInfo.put(nearestCLEO, nearestCLEOci.updateManageHosts(msg));
    	return topologyInfo;
    }
    /**
     * 每一个MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始点
     * @param endMEO   目的节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoMEOTopology(List<DTNHost> MEOHosts){    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	for (DTNHost MEO : MEOHosts){
    		MEOclusterInfo MEOci = ((SatelliteMovement)MEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(MEOci.getAllowConnectMEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
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
    public HashMap<DTNHost, List<DTNHost>> getMEOtoMEOTopology(){
    	if (MEO_TOTAL_SATELLITES <= 0)
    		return null;
    	//先找到一个MEO节点
    	DTNHost sMEO = null;
    	for (DTNHost h : this.getHosts()){
    		if (h.getSatelliteType().contains("MEO"))
    			sMEO = h;
    	}
    	MEOclusterInfo sMEOci = ((SatelliteMovement)sMEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    	
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	//对每一个MEO节点，添加其相邻链路，从而构成MEO层的拓扑
    	for (DTNHost MEO : sMEOci.getMEOList()){
    		MEOclusterInfo MEOci = ((SatelliteMovement)MEO.getMovementModel()).getSatelliteLinkInfo().getMEOci();
    		List<DTNHost> neighborNodes = new ArrayList<DTNHost>();
    		//同一轨道内的相邻两个节点
    		neighborNodes.addAll(MEOci.getAllowConnectMEOHostsInSamePlane());
    		//邻居轨道的两个最近节点
    		neighborNodes.addAll(MEOci.updateAllowConnectMEOHostsInNeighborPlane());  
    		topologyInfo.put(MEO, neighborNodes);
    	}
    	return topologyInfo;
    }
    /**
     * 获取本LEO节点到最近的通信LEO节点的拓扑
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getLEOtoNearestCommunicationLEOTopology(DTNHost src){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	DTNHost startCommunicationLEO = findNearestCommunicationLEONodes(src);  
    	//如果自己不是通信节点，先添加到通信节点的路径
    	if (!(startCommunicationLEO.getAddress() == src.getAddress())){
    		List<Tuple<Integer, Boolean>> pathTocLEO = 
    				findPathInSameLEOPlane(src, startCommunicationLEO);//找到前往同一平面上通信节点的路径
        	
        	int size = pathTocLEO.size();
        	DTNHost previousHop = src;
        	//前行添加拓扑中的链路
        	for (int index = 0; index < size; index++){       		
        		Tuple<Integer, Boolean> t = pathTocLEO.get(index);
        		List<DTNHost> links = new ArrayList<DTNHost>();
        		DTNHost thisHop = findHostByAddress(t.getKey());
        		links.add(thisHop);//添加一跳的链路
        		if (!(index + 1 >= size))
        			links.add(previousHop);//如果是中间节点，需要添加双向连接链路
        		topologyInfo.put(previousHop, links);
        		previousHop = thisHop;
        	}
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
    public HashMap<DTNHost, List<DTNHost>> getMEOtoLEOTopology(Message msg, DTNHost endLEO){
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	LEOclusterInfo endLEOci = ((SatelliteMovement)endLEO.getMovementModel()).getSatelliteLinkInfo().getLEOci();
    	
    	topologyInfo = getMEOtoMEOTopology();
    
    	//考虑到MEO节点只能与通信LEO节点之间进行通信
    	topologyInfo.putAll(getLEOtoNearestCommunicationLEOTopology(endLEO));//找到目的LEO到其最近通信LEO节点之间的拓扑，并进行添加
    	
//    	//拓扑中添加MEO到目的LEO的链路
//    	for (DTNHost MEO : endLEOci.updateManageHosts(msg)){
//    		List<DTNHost> list = topologyInfo.get(MEO);  	
//            if (list == null) {
//            	list = new ArrayList<DTNHost>();
//                list.add(endLEO);
//            } else {
//            	list.add(endLEO);
//            }
//    	}	
    	return topologyInfo;
    }
    /**
     * 每一个MEO节点的邻居返回4个节点，同一轨道内的相邻两个节点，邻居轨道的两个最近节点，
     * 从而计算出整体的拓扑
     * @param startMEO 起始MEO点
     * @param endMEO   目的LEO节点
     * @return
     */
    public HashMap<DTNHost, List<DTNHost>> getMEOtoCommunicationLEOTopology(Message msg, DTNHost endLEO){
    	if (!endLEO.getRouter().CommunicationSatellitesLabel)
    		throw new SimError(" not a communication LEO! ");
    	HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();
    	
    	topologyInfo = getMEOtoMEOTopology();
    
    	//获取目的通信LEO节点的管理MEO节点列表，动态或者静态
		List<DTNHost> manageMEO = ((SatelliteMovement) endLEO
				.getMovementModel()).getSatelliteLinkInfo().getLEOci()
				.updateManageHosts(msg);
    	//添加MEO到目的通信LEO节点的链路
    	topologyInfo.put(endLEO , manageMEO);
    	for (DTNHost MEO : manageMEO){
    		topologyInfo.get(MEO).add(endLEO);
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
    public Connection findConnection(int address, Message msg) {
    	String connectionType = "";
    	if (msg.getSize() > msgThreshold)
    		connectionType = "LaserLink";
    	else
    		connectionType = "RadioLink";
    	
        List<Connection> connections = this.getHost().getConnections();
        
        for (Connection c : connections) {
            if (c.getOtherNode(this.getHost()).getAddress() == address 
            		&& isRightConnection(msg, c)) {    
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





//    /**
//     * 此重写函数保证在传输完成之后，源节点的信息从messages缓存中删除
//     */
//    @Override
//    protected void transferDone(Connection con) {
//        String msgId = con.getMessage().getId();
//        removeFromMessages(msgId);
//    }

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
     * @return SatelliteInterLinkInfo for getting cluster information
     */
    public SatelliteInterLinkInfo getSatelliteLinkInfo(){
    	return ((SatelliteMovement)this.getHost().getMovementModel()).getSatelliteLinkInfo();
    }
    
    /**
     * 
     * @return all MEO nodes
     */
    public List<DTNHost> getMEO_ClusterList(){
    	if (this.getSatelliteLinkInfo().getMEOci() != null)
    		return this.getSatelliteLinkInfo().getMEOci().getClusterList();
    	return null;
    }
    /**
     * find all MEO hosts
     * @return
     */
    public List<DTNHost> findMEOHosts(){
    	List<DTNHost> MEOHosts = new ArrayList<DTNHost>();
    	for (DTNHost h : getHosts()){
    		if (h.getSatelliteType().contains("MEO"))
    			MEOHosts.add(h);
    	}
    	return MEOHosts;
    }
    /**
     * find all GEO hosts
     * @return
     */
    public List<DTNHost> findGEOHosts(){
    	List<DTNHost> GEOHosts = new ArrayList<DTNHost>();
    	for (DTNHost h : getHosts()){
    		if (h.getSatelliteType().contains("GEO"))
    			GEOHosts.add(h);
    	}
    	return GEOHosts;
    }
    /**
     * if the connection type is the matched with this type of message
     * @param msg
     * @param con
     * @return
     */
    public boolean isRightConnection(Message msg, Connection con){
    	if (msg.getSize() > msgThreshold && con.getLinkType().contains(LASER_LINK))
    		return true;
    	if (msg.getSize() <= msgThreshold && con.getLinkType().contains(RADIO_LINK))
    		return true;
    	
    	return false;
    }
}
