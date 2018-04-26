/*
 * Copyright 2016 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import core.*;
import movement.MovementModel;
import movement.SatelliteMovement;
import util.Tuple;
import static core.SimClock.getTime;


public class ClusteringRouter extends ActiveRouter {
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
    /** the interval of each cluster confirm process -setting id ({@value} */
    private static double clusterCheckInterval;
    /** the interval of each MEO confirm process -setting id ({@value} */
    private static double MEOCheckInterval;
    /** confirm message size -setting id ({@value} */
    private static int confirmMessageSize;
    /** indicates the number of confirm message -setting id ({@value} */
    private static int confirmMessageNum;
    /** indicates the TTL of confirm message -setting id ({@value} */
    private static int confirmTtl;
    
    /** label indicates that the static routing parameters are set or not */
    private static boolean initLabel = false;
    /** to make the random choice */
    private static Random random; 
    
    /** indicates that router initialization has been executed or not*/
    private boolean routerInitLabel;
    /** label indicates that routing algorithm has been executed or not at this time */
    private boolean routerTableUpdateLabel;
    /** maintain the earliest arrival time to other nodes */
    private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /** the router table comes from routing algorithm */
    private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> 
    		routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();
      
    /** store the latest cluster check time */
    private double lastClusterCheckTime;
    /** store the latest cluster check time */
    private double lastMEOCheckTime;
    /** store the latest cluster Info update time */
    private double lastClusterInfoUpdateTime;
    /** store LEO cluster information */
    private LEOclusterInfo LEOci;
    /** store MEO cluster information */
    private MEOclusterInfo MEOci;
    
    private List<Message> confirmMessages;
    //private List<Message> clusterInfoMessages;
    private List<Message> confirmFeedbackMessages;
    private List<Message> MEOConfirmFeedbackMessages;
    
    public ClusteringRouter(Settings s) {
        super(s);
    }

    protected ClusteringRouter(ClusteringRouter r) {
        super(r);
    }

    @Override
    public MessageRouter replicate() {
        return new ClusteringRouter(this);
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
            confirmTtl = setting.getInt(COMFIRMTTL_S);
            
            confirmMessageNum = 0;
            MEOCheckInterval = setting.getDouble(MEOCHECKINTERVAL_S);
            clusterCheckInterval = setting.getDouble(CLUSTERCHECKINTERVAL_S);
            confirmMessageSize = setting.getInt(COMFIRMMESSAGESIZE_S);
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
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred(id, from);

		// 1.LEO satellite nodes receive process
		if (m.getProperty("Confirm") != null &&
               (DTNHost) m.getProperty("Confirm") == this.getHost()){
            Object o = m.getProperty("ManageNode");
		    if (! (o instanceof DTNHost))
		        throw new SimError("Confirm message error!");
		  //TODO change the sequence
		    LEOci.addManageHosts((DTNHost) o);
		    // send feedback
            if (sendConfirmFeedback((DTNHost) o)) {
                // add manage hosts
            }
        }
//        if (this.getMessage("Confirm") != null)
//        	deleteMessage("Confirm", true); // delete confirm message, don't relay

        // 2.MEO satellite nodes receive process
        // TODO
        if (m.getProperty("ConfirmFeedback") != null &&
                m.getProperty("ConfirmFeedback") == this.getHost()){
            DTNHost feedbackFrom = m.getFrom();
            if (MEOci.getHostsInTransmissionRange().contains(feedbackFrom)){
                MEOci.addClusterList(feedbackFrom, SimClock.getTime());
            }
        }
//        if (this.getMessage("ConfirmFeedback") != null)
//        	deleteMessage("ConfirmFeedback", true); // delete confirm feedback message, don't relay

        // 3.LEO satellite nodes second receive process for cluster information        
        if (m.getProperty("ClusterInfo") != null &&
                m.getTo() == this.getHost()){
            Object o = m.getProperty("ClusterInfo");
  		    if (! (o instanceof List))
  		        throw new SimError("ClusterInfo message error!");
  		    
  		    // after MEO confirm message, MEO will broadcast clusterInfo message to update clusterInfo
  		    LEOci.updateClusterList((List<DTNHost>) o);
        }
        
        /** Confirm messages from MEO node to MEO node, receiving process */
        if (m.getProperty("MEOConfirm") != null &&
                m.getProperty("MEOConfirm") == this.getHost()){
        	DTNHost otherMEO = m.getFrom();
        	if (m.getProperty("LEOInCluster") instanceof List){
        		MEOci.updateOtherClusterList(otherMEO, 
        				(List<DTNHost>)m.getProperty("LEOInCluster"), MEOci.getUpdateTime());
        		
        		Object otherClusterInfo = m.getProperty("LEOInOtherCluster");
        		Object updateTime = m.getProperty("OtherClusterInfoUpdateTime");
        		if (otherClusterInfo instanceof HashMap &&
        				updateTime instanceof HashMap){
        			MEOci.updateInfoCompareWithUpdateTime((HashMap<DTNHost, List<DTNHost>>)
        								otherClusterInfo, (HashMap<DTNHost, Double>)updateTime);     
        		}
        	}

        	//TODO change the sequence
        	MEOci.addMEONode(otherMEO);
		    // send feedback
            if (sendMEOConfirmFeedback(otherMEO)) {
                // add manage hosts
                
            }
        }
        
        /** Confirm Feedback messages from MEO node to MEO node, receiving process */
        if (m.getProperty("MEOConfirmFeedback") != null &&
                m.getProperty("MEOConfirmFeedback") == this.getHost()){
        	DTNHost otherMEO = m.getFrom();
        	
        	if (m.getProperty("LEOInCluster") instanceof List){
            	MEOci.updateOtherClusterList(otherMEO, (List<DTNHost>)m.getProperty("LEOInCluster"), MEOci.getUpdateTime());
            	
        		Object otherClusterInfo = m.getProperty("LEOInOtherCluster");
        		Object updateTime = m.getProperty("OtherClusterInfoUpdateTime");
        		if (otherClusterInfo instanceof HashMap &&
        				updateTime instanceof HashMap){
        			MEOci.updateInfoCompareWithUpdateTime((HashMap<DTNHost, List<DTNHost>>)
        								otherClusterInfo, (HashMap<DTNHost, Double>)updateTime);     
        		}
        	}

        	//TODO change the sequence
        	MEOci.addMEONode(otherMEO);
		    // send feedback
            if (sendMEOConfirmFeedback(otherMEO)) {
                // add manage hosts
                
            }
        }
		return m;
	}
    @Override
    public void update() {
        super.update();  
        
        //update clustering information
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
       
        // for LEO nodes, to re-send the confirm Feedback Messages
        if (getSatelliteType().contains("LEO")){
            if (!confirmFeedbackMessages.isEmpty()){
            	for (Message m : confirmFeedbackMessages){
                	if (sendMsg(new Tuple<Message, Connection>
                			(m, this.findConnection(m.getTo().getAddress())))){
                		confirmFeedbackMessages.remove(m);
                		return;
                	}             		
            	}
            }
        }
        /** sort the messages to transmit */
        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
        List<Message> messages = sortByQueueMode(messageList);
        
        // try to send the message in the message buffer
        for (Message msg : messages) {
        	//Confirm message's TTL only has 1 minutes, will be drop by itself
        	if (msg.getId().contains("Confirm") || msg.getId().contains("ClusterInfo"))
        		continue;
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
	 * Creates a new confirm message from MEO node to LEO node
	 * @param from
	 * @param to
	 * @param id
	 * @param size
	 * @param responseSize
	 * @return
	 */
    public Message createConfrimMessage(DTNHost from, DTNHost to, String id, int size, int responseSize){
		Message m = new Message(from, to, id, size);
		m.setResponseSize(responseSize);
		m.updateProperty("Confirm", to);
		m.updateProperty("ManageNode", from);
		((ClusteringRouter)from.getRouter()).createNewMessage(m, confirmTtl);
		confirmMessages.add(m);
		
		return m;
    }
    /**
     * Creates a new confirm feedback message from a LEO node
     * @param from
     * @param to
     * @param id
     * @param size
     * @param responseSize
     * @return
     */
    public Message createConfrimFeedbackMessage(DTNHost from, DTNHost to, String id, int size, int responseSize){
		Message m = new Message(from, to, id, size);
		m.setResponseSize(responseSize);
		m.updateProperty("ConfirmFeedback", to);
		((ClusteringRouter)from.getRouter()).createNewMessage(m, confirmTtl);
		confirmFeedbackMessages.add(m);
		
		return m;
    }
	/**
	 * Creates a new ClusterInfo message from MEO node to LEO node
	 * @param from
	 * @param to
	 * @param id
	 * @param size
	 * @param responseSize
	 * @return
	 */
    public Message createClusterInfoMessage(DTNHost from, DTNHost to, String id, int size, int responseSize){
		Message m = new Message(from, to, id, size);
		m.setResponseSize(responseSize);
		m.updateProperty("ClusterInfo", new ArrayList<DTNHost>(MEOci.getClusterList()));
		((ClusteringRouter)from.getRouter()).createNewMessage(m, confirmTtl);
		confirmMessages.add(m);
		
		return m;
    }
	/**
	 * Creates a new confirm message from MEO node to MEO node
	 * @param from
	 * @param to
	 * @param id
	 * @param size
	 * @param responseSize
	 * @return
	 */
    public Message createMEOConfrimMessage(DTNHost from, DTNHost to, String id, int size, int responseSize){
		Message m = new Message(from, to, id, size);
		m.setResponseSize(responseSize);
		m.updateProperty("MEOConfirm", to);
		if (MEOci.getUpdateTime() > 0 && !MEOci.getClusterList().isEmpty()){
			m.updateProperty("LEOInCluster", MEOci.getClusterList());
			m.updateProperty("UpdateTime", MEOci.getUpdateTime());
		}
		
		if (!MEOci.getOtherClusterList().isEmpty())
			m.updateProperty("LEOInOtherCluster", MEOci.getOtherClusterList());
		
		if (!MEOci.getClusterUpdateTime().isEmpty())
			m.updateProperty("OtherClusterInfoUpdateTime", MEOci.getClusterUpdateTime());		
		((ClusteringRouter)from.getRouter()).createNewMessage(m, confirmTtl);
		confirmMessages.add(m);
		
		return m;
    }
    /**
     * Creates a new MEO confirm feedback message from a MEO node to MEO node
     * @param from
     * @param to
     * @param id
     * @param size
     * @param responseSize
     * @return
     */
    public Message createMEOConfrimFeedbackMessage(DTNHost from, DTNHost to, String id, int size, int responseSize){
		Message m = new Message(from, to, id, size);
		m.setResponseSize(responseSize);
		m.updateProperty("MEOConfirmFeedback", to);
		if (MEOci.getUpdateTime() > 0 && !MEOci.getClusterList().isEmpty()){
			m.updateProperty("LEOInCluster", MEOci.getClusterList());
			m.updateProperty("UpdateTime", MEOci.getUpdateTime());
		}

		if (!MEOci.getOtherClusterList().isEmpty())
			m.updateProperty("LEOInOtherCluster", MEOci.getOtherClusterList());
		if (!MEOci.getClusterUpdateTime().isEmpty())
			m.updateProperty("OtherClusterInfoUpdateTime", MEOci.getClusterUpdateTime());
		((ClusteringRouter)from.getRouter()).createNewMessage(m, confirmTtl);
		MEOConfirmFeedbackMessages.add(m);
		
		return m;
    }
    /**
     * Although The ONE does not have MAC layer, so it does not support broadcast,
     * this method can still be used to simulate the broadcast
     * @param connections
     * @return
     */
    public boolean broadcastConfirmMessage(List<Tuple<Message, Connection>> Collections){
    	boolean tryLabel = false;
    	//TODO deleteMessage方法在startTransfer方法内部有用过
    	//deleteMessage(m.getId(), true);
    	for (Tuple<Message, Connection> t : Collections){
    		if (sendMsg(t)){
    			tryLabel = true;
    		}
    	}

    	//TODO check this clear method
    	if (tryLabel)
    		confirmMessages.clear();
    	return tryLabel;
    }
    /**
     * When MEO satellite node receive confirm message from other MEO node,
     * then MEO send MEO confirm feedback message
     * @param otherMEO
     * @return
     */
    public boolean sendMEOConfirmFeedback(DTNHost otherMEO){
        DTNHost to = otherMEO;
        Message m = createMEOConfrimFeedbackMessage(this.getHost(), to,
                        "MEOConfirmFeedback From " + this.getHost() + " To "+to+" num: "
                        		+(confirmMessageNum++), confirmMessageSize, 0);

        Connection con = findConnection(to.getAddress());
        Tuple<Message, Connection> t =
                new Tuple<Message, Connection>(m, con);
        return sendMsg(t);
    }
    /**
     * When LEO satellite node receive confirm message from manage node,
     * then LEO send confirm feedback message
     * @param manageHost
     * @return
     */
    public boolean sendConfirmFeedback(DTNHost manageHost){
        DTNHost to = manageHost;
        Message m = createConfrimFeedbackMessage(this.getHost(), to,
                        "ConfirmFeedback To "+to+" num: "+(confirmMessageNum++), confirmMessageSize, 0);

        Connection con = findConnection(to.getAddress());
        Tuple<Message, Connection> t =
                new Tuple<Message, Connection>(m, con);
        return sendMsg(t);
    }
    
    /**
     * Execute router initialization process of LEO and MEO
     */
    public void routerInit(){
        switch (getSatelliteType()){
    		case "LEO":{
    			LEOci = new LEOclusterInfo();
                confirmFeedbackMessages = new ArrayList<Message>();
                //TODO
    			break;
    	}
    		case "MEO":{
    			MEOci = new MEOclusterInfo(this.getHost());
                confirmMessages = new ArrayList<Message>();
                MEOConfirmFeedbackMessages = new ArrayList<Message>();
    			// set MEO list
    			for (DTNHost h : ((SatelliteMovement)getMovementModel()).getHosts()){
    				if (h.getSatelliteType().contains("MEO")){
    					MEOci.addMEONode(h);
    				}            			
    			}
    			break;
    		}	
        }  
        routerInitLabel = true;
    }
    /**
     * update clustering information
     */
    public boolean clusteringUpdate(){
    	//for initialization
    	if (!routerInitLabel)
    		routerInit();
    	
    	//do different thing according to this node's satellite type
    	switch (getSatelliteType()){
    		case "LEO":{
    			if (LEOci.getManageHosts().isEmpty()){
    				//don't have MEO and LEO connections, then it do noting
    				if (this.getHost().getConnections().isEmpty())
    					return false;
    				else
    					return true;
    			}
    			else
    				return true;
    		}
    		case "MEO":{
    			confirmClusterMember();
    			return true;
    		}
    	}
    	throw new SimError("Satellite Type Error!");
    }
    /**
     * Confirm all LEO member in the cluster
     */
    public void confirmClusterMember(){	
    	/** 1.send confirm message to LEO nodes periodically */
    	if (SimClock.getTime() > getNextComfirmTime()){ 
    		//i.e., do hostsInTransmissionRange.clear();
            MEOci.clearHostsInTransmissionRange();
            //i.e., do ClusterList.clear();
            MEOci.clearClusterList();
            // TODO
            
            // find all LEO connections, create confirm messages, and try to broadcast them confirm message
            List<Tuple<Message, Connection>> toLEOConnections = findConnectionsAndCreateMessages("ConfirmFromMEOToLEO");
        	// try to broadcastConfirmMessage
        	broadcastConfirmMessage(toLEOConnections);
        	// update latest cluster check time
        	updateClusterCheckTime(SimClock.getTime());
        	
        	return;
    	}
    	
    	/** 2.send cluster-info message to LEO nodes after receving confirm feedback */
    	if (SimClock.getTime() > getNextClusterInfoTime()){ 
            // find all LEO connections, create confirm messages, and try to broadcast them confirm message
            List<Tuple<Message, Connection>> toLEOConnections = findConnectionsAndCreateMessages("ClusterInfoFromMEOToLEO");
        	// try to broadcastConfirmMessage
        	broadcastConfirmMessage(toLEOConnections);
        	
        	return;
    	}
    	
    	/** 3.MEO sends confirm message to MEO nodes periodically */
    	if (SimClock.getTime() > getNextMEOComfirmTime()){ 
    		// find all LEO connections, create confirm messages, and try to broadcast them confirm message
            List<Tuple<Message, Connection>> toMEOConnections = findConnectionsAndCreateMessages("ConfirmFromMEOToMEO");
        	// try to broadcastConfirmMessage
        	broadcastConfirmMessage(toMEOConnections);
        	// update latest cluster check time
        	updateMEOCheckTime(SimClock.getTime()); 
        	
        	return;
    	}
    }
    /**
     * Find all specific type connections and create specific type confirm messages
     * @param satelliteType
     * @return
     */
    public List<Tuple<Message, Connection>> findConnectionsAndCreateMessages(String type){
    	List<Tuple<Message, Connection>> Collection = new ArrayList<Tuple<Message, Connection>>();
    	switch(type){
    	case "ConfirmFromMEOToLEO":{
          	for (Connection con : this.getConnections()){
          		DTNHost to = con.getOtherNode(this.getHost());
          		if (to.getSatelliteType().contains("LEO")){
                      MEOci.addHostsInTransmissionRange(to);
                      
                      Message msg = createConfrimMessage(this.getHost(), to, 
                  			"Confirm To "+to+" num: "+(confirmMessageNum++), confirmMessageSize, 0);
                  	// then broadcast to all LEO satellite nodes in the cluster
                      Collection.add(new Tuple<Message, Connection>(msg, con));
          		}
          	}
          	return Collection;
    	}
    	case "ClusterInfoFromMEOToLEO":{
          	for (Connection con : this.getConnections()){
          		DTNHost to = con.getOtherNode(this.getHost());
          		if (MEOci.getClusterList().contains(to)){
                      
                      Message msg = createClusterInfoMessage(this.getHost(), to, 
                  			"ClusterInfo To "+to+" num: "+(confirmMessageNum++), confirmMessageSize, 0);
                  	// then broadcast to all LEO satellite nodes in the cluster
                      Collection.add(new Tuple<Message, Connection>(msg, con));
          		}
          	}
          	return Collection;
    	}
    	case "ConfirmFromMEOToMEO":{
          	for (Connection con : this.getConnections()){
          		DTNHost to = con.getOtherNode(this.getHost());
          		if (to.getSatelliteType().contains("MEO")){
          			// delete the MEO node when we send the MEO confirm, 
          			// if the feedback message comes back, this MEO node will be added again 
                      MEOci.removeMEONode(to);
                      
                      Message msg = createMEOConfrimMessage(this.getHost(), to, 
                  			"MEOConfirm From "+ this.getHost() +" To "+to+" num: "
                  					+(confirmMessageNum++), confirmMessageSize, 0);
                  	// then broadcast to all MEO satellite nodes
                      Collection.add(new Tuple<Message, Connection>(msg, con));
          		}
          	}
          	return Collection;
    	}
    	}
      	return Collection;
    }
    /**
     * @return next time to send cluster-info message to 
     * LEO satellite nodes after sending confirm message
     */
    public double getNextClusterInfoTime(){
    	return lastClusterCheckTime + (clusterCheckInterval/4);//(confirmTtl*60)/2;
    }
    /**
     * @return next time to send confirm message to MEO satellite nodes
     */
    public double getNextMEOComfirmTime(){
    	if (lastMEOCheckTime <= 0)
    		return random.nextDouble()*MEOCheckInterval;
    	else
    		// plus random interval time 
    		return lastMEOCheckTime + MEOCheckInterval;
    }
    /**
     * update latest cluster check time
     * @param ClusterCheckTime
     */
    public void updateMEOCheckTime(double MEOCheckTime){
    	lastMEOCheckTime = MEOCheckTime;
    }
    /**
     * @return next time to send confirm message to LEO satellite nodes
     */
    public double getNextComfirmTime(){
    	if (lastClusterCheckTime <= 0)
    		return random.nextDouble()*(clusterCheckInterval/2);
    	else
    		return lastClusterCheckTime + clusterCheckInterval;
    }
    /**
     * update latest cluster check time
     * @param ClusterCheckTime
     */
    public void updateClusterCheckTime(double ClusterCheckTime){
    	lastClusterCheckTime = ClusterCheckTime;
    }
//    /**
//     * Send comfirm message to LEO satellite nodes
//     * @param to
//     * @param con
//     * @return
//     */
//    public boolean sendComfirmMessage(DTNHost to, Connection con){
//
//    	
////    	Message confirmMessage = 
////    			new Message(this.getHost(), to,
////                        "Confirm To "+to+" num: "+(confirmMessageNum++), confirmMessageSize);
////    	// add clustering information
////    	confirmMessage.updateProperty("Confirm", to);
////    	confirmMessage.updateProperty("ManageNode", this.getHost());
////    	
////        Tuple<Message, Connection> t =
////                new Tuple<Message, Connection>(confirmMessage, con);
////        return sendMsg(t);
//    }

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
        	System.out.println("find the path!  " + getSatelliteType() +" to "+ msg.getTo().getSatelliteType()+"  " + msg);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return current network topology in forms of temporal graph
     */
    public HashMap<DTNHost, List<DTNHost>> localTopologyCaluculation(List<DTNHost> allHosts) {
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
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination.
     * It will search the routing path in specific local nodes area.
     * @param msg
     */
    public void shortestPathSearch(Message msg, List<DTNHost> localHostsList) {
    	if (localHostsList.isEmpty())
    		return;
    	//update the current topology information
        HashMap<DTNHost, List<DTNHost>> topologyInfo = localTopologyCaluculation(localHostsList);

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
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void LEOshortestPathSearch(Message msg) {
        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
        	case "LEO":{
        		if (LEOci.getClusterList().contains(to)){
        			// search the routing path in the cluster
        			shortestPathSearch(msg, LEOci.getClusterList());
        		}
        		else{
        			int nrofManageHosts = LEOci.getManageHosts().size();
        			DTNHost nextHop = null;
        			
        			//TODO deal with isolate node, i.e., the node without management of MEO
        			if (nrofManageHosts <= 0){
        				for (Connection con : this.getConnections()){
        					DTNHost neighborNode = con.getOtherNode(this.getHost());
        					if (to == neighborNode){
        						nextHop = neighborNode;
        					}
        				}
        			}
        			else{
            			// send the message to MEO manage node directly
            			// if this message's destination belongs to other cluster
        				nextHop = LEOci.getManageHosts().get(random.nextInt(nrofManageHosts));
        			}

        			if (nextHop != null){
            			List<Tuple<Integer, Boolean>> path =
                				new ArrayList<Tuple<Integer, Boolean>>();
                		path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
            			routerTable.put(nextHop, path);
        			}		
        		}
        		break;
        	}
        	case "MEO":{
        		if (LEOci.getManageHosts().contains(to)){
            		List<Tuple<Integer, Boolean>> path =
            				new ArrayList<Tuple<Integer, Boolean>>();
            		path.add(new Tuple<Integer, Boolean>(to.getAddress(), false));
        			routerTable.put(to, path);
        		}
        		else{
        			if (!LEOci.getManageHosts().isEmpty()){
            			// choose a manage host randomly to relay the message
            			DTNHost nextHop = LEOci.getManageHosts().get(random.nextInt(LEOci.getManageHosts().size()));
                		List<Tuple<Integer, Boolean>> path = 
                				new ArrayList<Tuple<Integer, Boolean>>();
                		path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
            			routerTable.put(nextHop, path);
        			}
        		}
        		//TODO
        		break;
        	}
        }
    }
    /**
     * Core routing algorithm, utilizes greed approach to search the shortest path to the destination
     *
     * @param msg
     */
    public void MEOroutingPathSearch(Message msg) {

        this.routerTable.clear();
        this.arrivalTime.clear();

        DTNHost to = msg.getTo();// get destination
        switch (to.getSatelliteType()){
        	case "LEO":{
        		if (MEOci.getClusterList().contains(to)){
            		List<Tuple<Integer, Boolean>> path =
            				new ArrayList<Tuple<Integer, Boolean>>();
            		path.add(new Tuple<Integer, Boolean>(to.getAddress(), false));
        			routerTable.put(to, path);
        		}
        		else{
        			// check other cluster information managed by other MEO
        			DTNHost nextHop = MEOci.findHostInOtherClusterList(to);
        			if (nextHop != null){
                		List<Tuple<Integer, Boolean>> path =
                				new ArrayList<Tuple<Integer, Boolean>>();
                		path.add(new Tuple<Integer, Boolean>(nextHop.getAddress(), false));
            			routerTable.put(nextHop, path);
        			}        				
        			//TODO
        		}
        		break;
        	}
        	case "MEO":{
        		//check the neighbors first
				for (Connection con : this.getConnections()){
					DTNHost neighborNode = con.getOtherNode(this.getHost());
					if (to == neighborNode){
                		List<Tuple<Integer, Boolean>> path =
                				new ArrayList<Tuple<Integer, Boolean>>();
                		path.add(new Tuple<Integer, Boolean>(neighborNode.getAddress(), false));
            			routerTable.put(neighborNode, path);
					}
				}
				//if not, then check all other MEO nodes
        		shortestPathSearch(msg, MEOci.getMEOList());
        		break;
        	}
        }      
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
        if (con.isTransferring() || ((ClusteringRouter) 
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
    	return ((SatelliteMovement)getMovementModel()).getSatelliteType();
    }
    
    /**
     * Stores the cluster information in the LEO node
     */
    private class LEOclusterInfo{
        /** confirmed hosts list in the cluster */
        private List<DTNHost> clusterList;
        /** all manage hosts which contains in the transmission range of MEO */
        private List<DTNHost> manageHosts;

        public LEOclusterInfo(){
        	clusterList = new ArrayList<DTNHost>();
        	manageHosts = new ArrayList<DTNHost>();
        }
        /**
         * update cluster list by receiving Cluster-Info message from MEO node
         * @param hosts
         */
        public void updateClusterList(List<DTNHost> hosts){
        	clusterList.clear();
        	if (hosts != null)       	
        		clusterList.addAll(hosts);
        }
        /**
         * @return cluster list contains all LEO nodes in the cluster
         */
        public List<DTNHost> getClusterList(){
        	return clusterList;
        }
        /**
         * add a MEO manage host into list
         * @param h
         */
        public void addManageHosts(DTNHost h){
        	manageHosts.add(h);
        }
        /**
         * clear and update manage hosts list
         * @param hosts
         */
        public void updateManageHosts(List<DTNHost> hosts){
        	manageHosts.clear();
        	if (hosts != null)  
        		manageHosts.addAll(hosts);
        }
        /**
         * @return all MEO manage hosts list
         */
        public List<DTNHost> getManageHosts(){
        	return manageHosts;
        }    
        /**
         * return cluster info
         */
        public String toString(){
        	return " Manage Hosts: "+manageHosts.toString()+
        			" Hosts in the cluster: "+clusterList.toString();
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
        private List<DTNHost> MEOList;
        /** record LEO nodes in other cluster through MEO confirm messges */
        private HashMap<DTNHost, List<DTNHost>> otherClusterList;
        /** record latest cluster information update time */
        private HashMap<DTNHost, Double> clusterUpdateTime;
        private Double updateTime;
        
        public MEOclusterInfo(DTNHost thisNode){
        	this.thisNode = thisNode;
        	hostsInTransmissionRange = new ArrayList<DTNHost>();
        	clusterList = new ArrayList<DTNHost>();
        	MEOList = new ArrayList<DTNHost>();
        	otherClusterList = new HashMap<DTNHost, List<DTNHost>>();
        	clusterUpdateTime = new HashMap<DTNHost, Double>();
        	updateTime = -1.0;
        }
        /**
         * In confirm process (MEO to LEO), record each LEO host in transmission range
         * @param h
         */
        public void addHostsInTransmissionRange(DTNHost h){
        	hostsInTransmissionRange.add(h);
        }
        /**
         * update hosts list that in transmission range of this MEO
         * @param hosts
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
         * add LEO node into the cluster list, which is managed by MEO node
         * @param h
         */
        public void addClusterList(DTNHost h, double update_Time){
        	clusterList.add(h);
        	updateTime = update_Time;
        }
        /**
         * update cluster list of this MEO node
         * @param hosts
         */
        public void clearClusterList(){
        	clusterList.clear();
        }
        /**
         * @return cluster list
         */
        public List<DTNHost> getClusterList(){
        	return clusterList;
        }
        /**
         * add other accessible MEO node in the network
         * @param h
         */
        public void addMEONode(DTNHost h){
        	MEOList.add(h);
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
        	for (DTNHost MEO : otherClusterList.keySet()){
        		if (MEO == thisNode)
        			continue;
        		for (DTNHost LEO : otherClusterList.get(MEO)){
        			if (LEO == to)
        				return MEO;
        		}
        	}
        	return null;
        }
        /**
         * update information in other cluster, 
         * which is send by other MEO nodes.
         * @param manageHost
         * @param LEOInCluster
         * @param time
         */
        public void updateOtherClusterList(
        		DTNHost manageHost, List<DTNHost> LEOInCluster, double time){
        	otherClusterList.put(manageHost, LEOInCluster);
        	clusterUpdateTime.put(manageHost, time);
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
        /**
         * @return the latest cluster information update time
         */
        public double getUpdateTime(){
        	return updateTime;
        }
        /**
         * update other cluster informations according to their latest update time;
         * if they are newer, then updates; if not, do nothing
         * @param otherClusters
         * @param updateTime
         */
        public void updateInfoCompareWithUpdateTime(
        		HashMap<DTNHost, List<DTNHost>> otherClusters, 
        				HashMap<DTNHost, Double> updateTime){       	
        	if (otherClusterList.keySet().size() != clusterUpdateTime.keySet().size())
        		throw new SimError("size mismatch");       	
        	for (DTNHost MEO : otherClusters.keySet()){
        		if (clusterUpdateTime.containsKey(MEO)){
        			/** only if it contains newer cluster information, then update */
        			if (clusterUpdateTime.get(MEO) < updateTime.get(MEO)){
        				updateOtherClusterList(MEO, otherClusters.get(MEO), updateTime.get(MEO));
        			}       				
        		}
        		/** this node doesn't contain this cluster-info, then copy directly */
        		else{
        			updateOtherClusterList(MEO, otherClusters.get(MEO), updateTime.get(MEO));
        		}
        	}
        }
        
        public String toString(){
        	return " Other MEO Hosts: " + MEOList.toString() +
        			" Hosts in the cluster: " + clusterList.toString() +
        				" other cluster: " + otherClusterList.toString() + 
        				"  clusterUpdateTime:  " + clusterUpdateTime;
        }
    }
}
