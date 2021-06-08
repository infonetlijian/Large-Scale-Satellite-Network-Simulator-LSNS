/*
 * Copyright 2016 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package routing;

//import Cache.File;
import core.*;
import movement.SatelliteMovement;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;
import util.Tuple;

import java.io.*;
import java.util.*;

import static core.SimClock.getTime;

class gnn_test {
    SavedModelBundle tensorflowModelBundle;
    Session tensorflowSession;

    void load(String modelPath) {
        this.tensorflowModelBundle = SavedModelBundle.load(modelPath, "serve");
        this.tensorflowSession = tensorflowModelBundle.session();
    }

    public Tensor predict(Tensor tensorInput) {
        Tensor output = this.tensorflowSession.runner().
                feed("input_A", tensorInput).fetch("pred_dis").run().get(0);

        return output;
    }
}

public class gnn_router extends ActiveRouter {
    /**
     * The TTL of confirm message -setting id ({@value})
     */
    public static final String HELLOTTL_S = "HelloTTL";
    /**
     * The size of confirm message -setting id ({@value})
     */
    public static final String HELLOMESSAGESIZE_S = "HelloMessageSize";
    /**
     * label indicates that the message can wait for next hop coming or not -setting id ({@value})
     */
    public static final String MSG_WAITLABEL = "waitLabel";
    /**
     * label indicates that routing path can contain in the message or not -setting id ({@value})
     */
    public static final String MSG_PATHLABEL = "msgPathLabel";
    /**
     * router path -setting id ({@value})
     */
    public static final String MSG_ROUTERPATH = "routerPath";
    /**
     * Group name in the group -setting id ({@value})
     */
    public static final String GROUPNAME_S = "Group";
    /**
     * interface name in the group -setting id ({@value})
     */
    public static final String INTERFACENAME_S = "Interface";
    /**
     * transmit range -setting id ({@value})
     */
    public static final String TRANSMIT_RANGE_S = "transmitRange";

    public static final String ModelPath = "model_path";

    private static String modelPath_all;

    public static final gnn_test myModel_all = new gnn_test();
    /** light speed，approximate 3*10^8m/s */
    private static final double LIGHTSPEED = 299792458;
    /** the interval of each hello process -setting id ({@value} */
    private double helloInterval;
    /** indicates the TTL of hello message -setting id ({@value} */
    private static int helloTtl;
    /** indicates the number of hello message -setting id ({@value} */
    private static int helloMessageNum;
    /** hello message size -setting id ({@value} */
    private static int helloMessageSize;

    /** store the latest hello check time */
    private double lastHelloCheckTime;
    /** indicate the transmission radius of each satellite */
    private static double transmitRange;
    /** label indicates that routing path can contain in the message or not */
    private static boolean msgPathLabel;
    /** label indicates that the static routing parameters are set or not */
    private static boolean initLabel = false;
    /** label indicates that routing algorithm has been executed or not at this time */
    private boolean routerTableUpdateLabel;
    /** maintain the earliest arrival time to other nodes */
    private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /** the router table comes from routing algorithm */
    private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();
    /** to make the random choice */
    private Random random;

    private List<Message> helloMessages;

    //导入神经网络模型
    SavedModelBundle tensorflowModelBundle;
    Session tensorflowSession;

    void load(String modelPath){
        this.tensorflowModelBundle = SavedModelBundle.load(modelPath, "serve");
        this.tensorflowSession = tensorflowModelBundle.session();
    }

    public Tensor predict(Tensor tensorInput){
        Tensor output = this.tensorflowSession.runner().
                feed("input_A", tensorInput).fetch("pred_dis").run().get(0);

        return output;
    }

	/** 用于判断包的类型 */
	public String SelectLabel;
	/** Queue mode for sending messages */
	protected int sendQueueMode;


    public gnn_router(Settings s) {
        super(s);
    }


    protected gnn_router(gnn_router r) {
        super(r);
    }


    @Override
    public MessageRouter replicate() {
        return new gnn_router(this);
    }

    @Override
    public void init(DTNHost host, List<MessageListener> mListeners) {
        super.init(host, mListeners);
        if (!initLabel){
        	random = new Random();
            Settings setting = new Settings(INTERFACENAME_S);
            transmitRange = setting.getInt(TRANSMIT_RANGE_S);
            setting.setNameSpace(GROUPNAME_S);
            msgPathLabel = setting.getBoolean(MSG_PATHLABEL);
            initLabel = true;
            helloInterval = setting.getDouble("HelloInterval");
            helloTtl = setting.getInt(HELLOTTL_S);
            helloMessageSize = setting.getInt(HELLOMESSAGESIZE_S);

            modelPath_all = setting.getSetting(ModelPath);

            myModel_all.load(modelPath_all);
        }

    }
    
    /**
     * 在Networkinterface类中执行链路中断函数disconnect()后，对应节点的router调用此函数
     */
    @Override
    public void changedConnection(Connection con) {
        super.changedConnection(con);
    }

    @Override
    public void update() {
        super.update();

        List<Connection> connections = this.getConnections();

        if (isTransferring()) { 	// judge the link is occupied or not
            return; 				// can't start a new transfer
        }
        if (connections.size() > 0) {//有邻居时需要进行hello包发送协议
            //helloProtocol();//执行hello包的维护工作
        }
        if (!canStartTransfer())
            return;

        /**Set router update label to make sure that routing algorithm only execute once at a time**/
        routerTableUpdateLabel = false;

        /** sort the messages to transmit */
        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
        List<Message> messages = sortByQueueMode(messageList);
        
        for (Message msg : messages){
        	if(findPathToSend(msg) == true){
        		return;
        	}
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
     * periodically send hello packet to neighbor satellite nodes to check their status
     */
    public void helloProtocol(){
        // TODO helloProtocol
    	List<Tuple<Message, Connection>> Collection = 
    				new ArrayList<Tuple<Message, Connection>>();
    	
    	if (SimClock.getTime() > getNextHelloTime()){ 
    		for (Connection con : this.getConnections()){
    			DTNHost to = con.getOtherNode(this.getHost());
        		Message m = createHelloMessage(this.getHost(), to, "Hello, num: " + (helloMessageNum++), helloMessageSize, 0);
        		Collection.add(new Tuple<Message, Connection>(m, con));     		
    		}
    		//simulate broadcast process
    		broadcastHelloMessage(Collection);
    	}
    }
    /**
     * Although The ONE does not have MAC layer, so it does not support broadcast,
     * this method can still be used to simulate the broadcast
     * @param connections
     * @return
     */
    public boolean broadcastHelloMessage(List<Tuple<Message, Connection>> Collections){
    	boolean tryLabel = false;
    	//TODO deleteMessage方法在startTransfer方法内部有用过
    	//deleteMessage(m.getId(), true);
    	for (Tuple<Message, Connection> t : Collections){
    		if (sendMsg(t)){
    		    System.out.println("helloMessage");
    			tryLabel = true;
    		}
    	}

    	//TODO check this clear method
    	if (tryLabel)
    		helloMessages.clear();
    	return tryLabel;
    }
	@Override
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		// 1.LEO satellite nodes receive process
		if (m.getProperty("Hello") != null &&
               (DTNHost) m.getProperty("Hello") == this.getHost()){
//            Object o = m.getProperty("ManageNode");
//		    if (! (o instanceof DTNHost))
//		        throw new SimError("Confirm message error!");
//		  //TODO change the sequence
//		    LEOci.addManageHosts((DTNHost) o);
//		    // send feedback
//            if (sendConfirmFeedback((DTNHost) o)) {
//                // add manage hosts
//            }
        }
		return m;
	}
	/**
	 * Creates a new confirm message from MEO node to LEO node
	 * @param from
	 * @param to
	 * @param id
	 * @param size
	 * @param responseSize
	 * @return
	 */
    public Message createHelloMessage(DTNHost from, DTNHost to, String id, int size, int responseSize){
		Message m = new Message(from, to, id, size);
		m.setResponseSize(responseSize);
		m.updateProperty("Hello", to);
		//m.updateProperty("HelloInfo", from);
		((gnn_router)from.getRouter()).createNewMessage(m, helloTtl);
		helloMessages.add(m);
		
		return m;
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
     * @return next time to send confirm message to LEO satellite nodes
     */
    public double getNextHelloTime(){
    	if (lastHelloCheckTime <= 0)
    		return random.nextDouble()*(helloInterval/2);
    	else
    		return lastHelloCheckTime + helloInterval;
    }
    /**
     * Update router table, find a routing path and try to send the message
     *
     * @param msg
     * @param connections
     * @param msgPathLabel
     * @return
     */
    public boolean findPathToSend(Message msg) {
    	// if user allow routing path information written into the message header
        if (msgPathLabel == true) {
        	// if message header contains routing path information, this node should be intermediate node
            if (msg.getProperty(MSG_ROUTERPATH) == null) {
                Tuple<Message, Connection> t =
                        findPathFromRouterTabel(msg);
                //System.out.println(this.getHost()+" sending "+t+"  "+SimClock.getTime());
                return sendMsg(t);
            } else {
                Tuple<Message, Connection> t =
                        findPathFromMessage(msg);
                assert t != null : "Reading routing path from message header fail!";
                return sendMsg(t);
            }
        } else {
        	//don't write the routing path into the header
        	//routing path will be calculated in each hop
            Tuple<Message, Connection> t =
                    findPathFromRouterTabel(msg);
//            System.out.println("消息传输元组为："+t);
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
//            throw new SimError("Message: " + msg +
//                    " already arrive the destination! " + this.getHost());  
        	System.out.println("Message: " + msg +
                  " already arrive the destination! " + this.getHost());
        }
        if (routerPath == null)
        	return null;
        
        //try to find the next hop from routing path in the message header
        int nextHopAddress = -1;
        boolean waitLable = false;
        for (int i = 0; i < routerPath.size(); i++) {
            if (routerPath.get(i).getKey() == thisAddress) {
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
        List<Tuple<Integer, Boolean>> routerPath = this.routerTable.get(message.getTo());
        //System.out.println(this.getHost()+" send path "+routerPath+"  "+SimClock.getTime());
        //write the routing path into the header or not according to the 'msgPathLabel'
        if (msgPathLabel == true) {
            message.updateProperty(MSG_ROUTERPATH, routerPath);
        }

        Connection firstHop = findConnection(routerPath.get(0).getKey());
        if (firstHop != null) {
            Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, firstHop);
            return t;
        } else {
            if (routerPath.get(0).getValue()) {

                return null;
            } else {
            	this.routerTable.remove(message.getTo());
            	return null;
//                throw new SimError("No such connection: " + routerPath.get(0) +
//                       " at routerTable " + this);

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

        //统计时间
//        long time_start = System.currentTimeMillis();
        gnnSearch(msg);
//        long time_end = System.currentTimeMillis();
//        int time = (int)(time_end - time_start);
////        System.out.println(time);
//        try{
//            String dir = "reports/time_core.txt";
//            File file = new File(dir);
//            FileOutputStream fos = null;
//            if(!file.exists()){
//                file.createNewFile(); // 创建新文件
//                fos = new FileOutputStream(file);
//            }
//            else{
//                fos = new FileOutputStream(file,true);
//            }
//            OutputStreamWriter out = new OutputStreamWriter(fos,"UTF-8");
//            out.write(time+"\r\n");
//            out.close(); //
//
//        }catch (IOException e){
//            e.printStackTrace();
//        }

        //System.out.println(this.getHost()+"  "+routerTable+"  "+SimClock.getTime());
        if (this.routerTable.containsKey(msg.getTo())) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Return current network topology in forms of temporal graph
     */
    public float[][][] temporalGraphCaluculation() {
        HashMap<DTNHost, Coord> locationRecord = new HashMap<DTNHost, Coord>();

        double radius = transmitRange;//Represent communication Radius
        //Get satellite movement model which store orbit-info of all satellites in the network
        if (! (this.getHost().getMovementModel() instanceof SatelliteMovement))
            System.out.println("?");
        SatelliteMovement movementModel = ((SatelliteMovement) this.getHost().getMovementModel());

        //Calculate the current coordinate of all satellite nodes in the network
        List<DTNHost> allHosts = this.getHosts();
        if (allHosts.size() <= this.getHost().getHostsList().size())
            allHosts = this.getHost().getHostsList();
        float [][][] topologyA = new float[1][allHosts.size()][allHosts.size()];

        for (DTNHost h : allHosts) {
            //locationRecord.put(h, movementModel.getCoordinate(h, SimClock.getTime()));
            locationRecord.put(h, h.getLocation());          
        }
        //System.out.println(this.getHost()+" list: "+locationRecord+"  "+SimClock.getTime());
        //Calculate links between each two satellite nodes
        for (DTNHost h : allHosts) {
            for (DTNHost otherNode : allHosts) {
                if (otherNode == h)
                    continue;
                Coord otherNodeLocation = locationRecord.get(otherNode);
                if (locationRecord.get(h).distance(otherNodeLocation) <= radius) {
                    topologyA[0][h.getAddress()][otherNode.getAddress()] = 1;
                    topologyA[0][otherNode.getAddress()][h.getAddress()] = 1;
                }
            }
        }
        return topologyA;
    }

    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */



    public void gnnSearch(Message msg){

        int node_num = 160;
        float[][][] topologyA = temporalGraphCaluculation();
//        float[][][] topologyfloat = new float[1][node_num][node_num];
//        try{
//
//            File topoName = new File("topo/"+SimClock.getTime()+".txt");
//            if (!topoName.exists()){
//                topoName.createNewFile();
//            }
////            FileWriter out = new FileWriter(topoName);
//            for (int i=0;i<node_num;i++){
//                for (int j =0;j<node_num;j++){
////                    out.write(topologyA[0][i][j]+"\t");
//                    topologyfloat[0][i][j] = topologyA[0][i][j];
//                }
////                out.write("\r\n");
//            }
////            out.close();
//        }catch (IOException e){
//            e.printStackTrace();
//        }


        Tensor input = Tensor.create(topologyA);
        Tensor out_all = myModel_all.predict(input);
        float[][][] resultValues;

        resultValues = (float[][][]) out_all.copyTo(new float[1][node_num][node_num]);
//        float []resultValues_use = resultValues[][msg.getTo().getAddress()];
        // 防止内存泄漏, 主动释放内存
        input.close();
        out_all.close();

        float mindis = 100;

        List<Tuple<Integer, Boolean>> path = new LinkedList<Tuple<Integer, Boolean>>();

        for (Connection con : this.getHost().getConnections()) {
            DTNHost neiHost = con.getOtherNode(this.getHost());
            if(msg.getHops().contains(neiHost))
                continue;

            Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);

            if (resultValues[0][msg.getTo().getAddress()][neiHost.getAddress()]<mindis){
                mindis = resultValues[0][msg.getTo().getAddress()][neiHost.getAddress()];
                if (path.size()!=0)
                    path.remove(0);
                path.add(hop);
            }
            //routerTable.put(neiHost, path);
        }
        if (path.size()==0) {
            for (Connection con : this.getHost().getConnections()) {
                DTNHost neiHost = con.getOtherNode(this.getHost());

                Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);

                if (resultValues[0][msg.getTo().getAddress()][neiHost.getAddress()] < mindis) {
                    mindis = resultValues[0][msg.getTo().getAddress()][neiHost.getAddress()];
                    if (path.size() != 0)
                        path.remove(0);
                    path.add(hop);
                }
            }
        }
        routerTable.put(msg.getTo(), path);

    }

    /**
     * Bubble sort algorithm 
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
     * Find the corresponding DTNHost from host address in 
     * routing path information (contained in message header)
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
     * @param t
     * @return
     */

    public Message tryMessageToConnection(Tuple<Message, Connection> t) {
        if (t == null)
            throw new SimError("No such tuple: " + " at " + this);
        Message m = t.getKey();
        Connection con = t.getValue();
        int retVal = startTransfer(m, con);
        if (retVal == RCV_OK) {  	//accepted a message, don't try others
            return m;
        } else if (retVal > 0) { 	//系统定义，只有TRY_LATER_BUSY大于0，即为1
            return null;          	// should try later -> don't bother trying others
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
        if (con.isTransferring() || ((gnn_router) con.getOtherNode(this.getHost()).getRouter()).isTransferring()) {
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
        if (t == null) {
            assert false : "error!";
            return false;
        } else {
        	// check the next hop is busy or not
            if (nextHopIsBusyOrNot(t) == true)
                return false;
            if (tryMessageToConnection(t) != null)
                return true;
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
        if (msgId != null)
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
}
