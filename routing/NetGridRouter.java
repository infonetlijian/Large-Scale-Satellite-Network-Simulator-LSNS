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

import routing.NetGridRouter.GridNeighbors.GridCell;
import movement.MovementModel;
import movement.SatelliteMovement;
import util.Tuple;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.NetworkInterface;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import core.SimError;

public class NetGridRouter extends ActiveRouter{
	
	/** write the routing path into the message header or not -setting id ({@value})*/
	public static final String MSG_PATHLABEL = "msgPathLabel"; 
	/** Group name in the group -setting id ({@value})*/
	public static final String GROUPNAME_S = "Group";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";
	/** grid update mode -setting id ({@value})*/
	public static final String GRIDUPDATEOPTION_S = "gridUpdateOption";
	/** write the routing path into the message header */
	public static final String MSG_ROUTERPATH = "routerPath"; 

	/** light speed，approximate 3*10^8m/s */
	private static final double SPEEDOFLIGHT = 299792458;
	
    /** indicate the transmission radius of each satellite */
    private static double transmitRange;
    /** label indicates that routing path can contain in the message or not */
    private static boolean msgPathLabel;
    /** label indicates that the static routing parameters are set or not */
    private static boolean initLabel = false;
    /** label indicates the grid topology should be updated online or offline */
    private static String gridUpdateOption;
    /** to make the random choice */
    private static Random random;	
    
	/**根据基于网格的最短路径搜索结果，存储翻译过后的到达目的节点的最短路径，供选择链路时直接使用**/
    /** the netgrid router table comes from routing algorithm */
	private HashMap<DTNHost, List<Tuple<List<Integer>, Boolean>>> multiPathFromNetgridTable = new HashMap<DTNHost, List<Tuple<List<Integer>, Boolean>>>();	
	/** maintain the earliest arrival time to other nodes */
    private HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
    /** the router table comes from routing algorithm */
    private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();
    /** label indicates that routing algorithm has been executed or not at this time */
    private boolean routerTableUpdateLabel;
    /** the netgrid object which is used in the routing algorithm */
	private GridNeighbors GN;

    /** to indicates the final hop */
	private boolean finalHopLabel = false;
	/** to indicates the final connection */
	private Connection finalHopConnection = null;

	/** used as priori information for Walker constellation */
	public List<DTNHost> neighborPlaneHosts = new ArrayList<DTNHost>();//相同轨道平面里的两个邻居节点
	public List<DTNHost> neighborHostsInSamePlane = new ArrayList<DTNHost>();//相邻轨道平面内的两个邻居节点
	
	/** Queue mode for sending messages */
	protected int sendQueueMode;
	
	public static final String SEND_QUEUE_MODE_S = "sendQueue";
	
	public NetGridRouter(Settings s){
		super(s);
	}

	protected NetGridRouter(NetGridRouter r) {
		super(r);
		this.GN = new GridNeighbors(this.getHost());
	}

	@Override
	public MessageRouter replicate() {
		return new NetGridRouter(this);
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
            gridUpdateOption = setting.getSetting(GRIDUPDATEOPTION_S);//从配置文件中读取设置，是采用在运行过程中不断计算轨道坐标的方式，还是通过提前利用网格表存储各个节点的轨道信息
            initLabel = true;
        }
    }
	/**
	 * 执行路由的初始化操作
	 */
	public void initialzation(){
		GN.setHost(this.getHost());//为了实现GN和Router以及Host之间的绑定，待修改！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
		initInterSatelliteNeighbors();//初始化记录节点在同一个轨道内的相邻节点，以及相邻平面的邻居
		//this.GN.initializeGridLocation();//初始化提前计算网格表
	}	

	@Override
	public void changedConnection(Connection con){
		super.changedConnection(con);

		if (!con.isUp()){
			if(con.isTransferring()){
				if (con.getOtherNode(this.getHost()).getRouter().isIncomingMessage(con.getMessage().getId()))
					con.getOtherNode(this.getHost()).getRouter().removeFromIncomingBuffer(con.getMessage().getId(), this.getHost());
				super.addToMessages(con.getMessage(), false);//对于因为链路中断而丢失的消息，重新放回发送方的队列中，并且删除对方节点的incoming信息
			}
		}
	}

	/**
	 * 路由更新，每次调用路由更新时的主入口
	 */
	@Override
	public void update() {
		super.update();
		
		/**动态更新相邻轨道平面内的邻居节点列表(因为在边缘轨道平面时会有问题)**/
		List<DTNHost> neiList = getNeighbors(this.getHost(), SimClock.getTime());//通过距离来判断的邻居，不会受到链路中断的影响	
		neighborPlaneHosts.clear();//清空相邻轨道平面内的邻居节点列表(在边缘轨道平面时会有问题)
		updateInterSatelliteNeighbors(neiList);//动态更新相邻轨道平面内的邻居节点列表

		List<Connection> connections = this.getConnections();  //取得所有邻居节点
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
				
		if (isTransferring()) {//判断链路是否被占用
			return; // can't start a new transfer
		}
		if (connections.size() > 0){//有邻居时需要进行hello包发送协议
			//helloProtocol();//执行hello包的维护工作
		}
		if (!canStartTransfer())//是否有林杰节点且有信息需要传送
			return;

		this.multiPathFromNetgridTable.clear();
		routerTableUpdateLabel = false;
		if (messages.isEmpty())
			return;
		
		//判断如果为先进先出模式，则进行排序
		Settings s = new Settings("Group");
		if (s.contains(SEND_QUEUE_MODE_S)) {
			this.sendQueueMode = s.getInt(SEND_QUEUE_MODE_S);
			if (sendQueueMode < 1 || sendQueueMode > 2) {
				throw new SettingsError("Invalid value for " + 
						s.getFullPropertyName(SEND_QUEUE_MODE_S));
			}
		}
		else {
			sendQueueMode = Q_MODE_RANDOM;
		}
		// FIFO, sort the messages
		if(sendQueueMode == 2){
	        /** sort the messages to transmit */
	        List<Message> messageList = this.CollectionToList(this.getMessageCollection());
	        List<Message> sortedMessages = sortByQueueMode(messageList);
			for (Message msg : sortedMessages){	//尝试发送队列里的消息	
				if (findPathToSend(msg, connections) == true)
					return;
			}
		} 
		
		else{
			for (Message msg : messages){	//尝试发送队列里的消息	
				if (findPathToSend(msg, connections) == true)
					return;
			}
		}
	}
	
    /** transform the Collection to List
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
	 * Calculates all neighbors of specific host at specific time
	 * @param host
	 * @param time
	 * @return all neighbors of specific host
	 */
	public List<DTNHost> getNeighbors(DTNHost host, double time){
		double updateInterval = (new Settings("Scenario")).getDouble("updateInterval");
		int num = (int)((time-SimClock.getTime())/updateInterval);
		time = SimClock.getTime()+num*updateInterval;
		
		List<DTNHost> neiHost = new ArrayList<DTNHost>();//邻居列表
		
		HashMap<DTNHost, Coord> loc = new HashMap<DTNHost, Coord>();
		loc.clear();
		
		/**原来的代码中，有优化机制，不符合实际，故而删除**/
		/*
		if (!(time == SimClock.getTime())){
			for (DTNHost h : hosts){//更新指定时刻全局节点的坐标
				//location.my_Test(time, 0, h.getParameters());
				//Coord xyz = new Coord(location.getX(), location.getY(), location.getZ());
				Coord xyz = h.getCoordinate(time);
				loc.put(h, xyz);//记录指定时刻全局节点的坐标
			}
		}
		else{
			for (DTNHost h : hosts){//更新指定时刻全局节点的坐标
				loc.put(h, h.getLocation());//记录指定时刻全局节点的坐标
			}
		}*/
		
		/**实时计算全网节点的坐标构成拓扑图**/
		for (DTNHost h : getHosts()){//更新指定时刻全局节点的坐标
			//location.my_Test(time, 0, h.getParameters());
			//Coord xyz = new Coord(location.getX(), location.getY(), location.getZ());
			/**根据轨道模型，实时计算节点当前的位置，运行速度较慢**/
			//Coord xyz = h.getCoordinate(time);
			/**直接获取当前的节点位置，简化计算过程，提高仿真运行速度**/
			Coord xyz = h.getLocation();
			/**直接获取当前的节点位置，简化计算过程**/
			loc.put(h, xyz);//记录指定时刻全局节点的坐标
		}
		
		Coord myLocation = loc.get(host);
		for (DTNHost h : getHosts()){//再分别及计算
			if (h == host)
				continue;
			if (JudgeNeighbors(myLocation, loc.get(h)) == true){
				double distance = myLocation.distance(loc.get(h));
				//System.out.println(host+"  locate  "+myLocation+" to "+h + "  the distance is: " + distance);
				neiHost.add(h);
			}
		}
		//System.out.println(host+" neighbor: "+neiHost+" time: "+time);

		return neiHost;
	}
	/**
	 * 对Coord类坐标进行距离计算
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean JudgeNeighbors(Coord c1,Coord c2){

		double distance = c1.distance(c2);
		if (distance <= this.transmitRange)
			return true;
		else
			return false;
	}	

	/**
	 * 更新路由表，寻找路径并尝试转发消息
	 * @param msg
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public boolean findPathToSend(Message msg, List<Connection> connections){
		if (msgPathLabel == true){//如果允许在消息中写入路径消息
			if (msg.getProperty(MSG_ROUTERPATH) == null){//通过包头是否已写入路径信息来判断是否需要单独计算路由(同时也包含了预测的可能)
				Tuple<Message, Connection> t = 
						findPathFromRouterTabel(msg, connections, msgPathLabel);
				return sendMsg(t);
			}
			else{//如果是中继节点，就检查消息所带的路径信息
				Tuple<Message, Connection> t = 
						findPathFromMessage(msg);
				if (t == null){
					//msg.removeProperty(MSG_ROUTERPATH);
					throw new SimError("读取路径信息失败！");	
				}						
				return sendMsg(t);
			}
		}else{//不会再信息中写入路径信息，每一跳都需要重新计算路径
			Tuple<Message, Connection> t = 
					findPathFromRouterTabel(msg, connections, msgPathLabel);//按待发送消息顺序找路径，并尝试发送
			return sendMsg(t);
		}
	}
	/**
	 * 通过读取信息msg头部里的路径信息，来获取路由路径，如果失效，则需要当前节点重新计算路由
	 * @param msg
	 * @return
	 */
	public Tuple<Message, Connection> findPathFromMessage(Message msg){
		assert msg.getProperty(MSG_ROUTERPATH) != null : 
			"message don't have routerPath";//先查看信息有没有路径信息，如果有就按照已有路径信息发送，没有则查找路由表进行发送
		List<Tuple<Integer, Boolean>> routerPath = (List<Tuple<Integer, Boolean>>)msg.getProperty(MSG_ROUTERPATH);
		
		int thisAddress = this.getHost().getAddress();
		//assert msg.getTo().getAddress() != thisAddress : "本节点已是目的节点，接收处理过程错误";
		int nextHopAddress = -1;
		

		//System.out.println(this.getHost()+"  "+msg+" "+routerPath);
		boolean waitLable = false;
		for (int i = 0; i < routerPath.size(); i++){
			if (routerPath.get(i).getKey() == thisAddress){
				/**查看是否写入的路径有误**/
				if (routerPath.size() == i + 1){
					if (msg.getTo() != this.getHost()){
						this.receiveMessage(msg, msg.getFrom());
						return null;
					}
					else
						return findPathFromRouterTabel(msg, this.getConnections(), msgPathLabel);
				}
				nextHopAddress = routerPath.get(i+1).getKey();//找到下一跳节点地址
				waitLable = routerPath.get(i+1).getValue();//找到下一跳是否需要等待的标志位
				break;//跳出循环
			}
		}
		
	      if (nextHopAddress > -1) {
	    	  Connection nextCon = NetgridMultiPathMatchingProcess(nextHopAddress);//通过同一网格中含有多个节点的网格时，可以采用多路径
	            //the routing path in the message header could be invaild
	            if (nextCon == null) {
	                if (!waitLable) {
	                    //msg.removeProperty(MSG_ROUTERPATH);
	                    //try to re-routing
	                    Tuple<Message, Connection> t =
	                            findPathFromRouterTabel(msg, this.getConnections(), true);
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
	 * 通过同一网格中含有多个节点的网格时，可以采用多路径，通过此函数找到此多路径
	 * @param routerPath
	 * @return
	 */
	public Connection NetgridMultiPathMatchingProcess(int hostAddress){
		DTNHost firstHop = this.findHostByAddress(hostAddress);
		GridCell firstGridCell = this.DTNHostToGridCell.get(firstHop);
		
		if (this.GridCellhasMultiDTNHosts.containsKey(firstGridCell) && this.GridCellhasMultiDTNHosts.get(firstGridCell).size() > 1){
			
			List<DTNHost> multiHostsList = new ArrayList<DTNHost>(this.GridCellhasMultiDTNHosts.get(firstGridCell));
			DTNHost selectedHost;
			Connection con = null;
			for (int i = 0; i < 1;){
				//System.out.println(multiHostsList + "  " + this.GridCellhasMultiDTNHosts.get(firstGridCell));
				if (multiHostsList.size() == 1)
					return findConnection(hostAddress);//取第一跳的节点地址
				if (multiHostsList.isEmpty() || multiHostsList.size() <= 0){
					return con;
				}				
				//注：Random.nextInt(n)方法，返回的值介于[0,n)之间，但不包含n
				selectedHost = multiHostsList.get(Math.abs(this.random.nextInt(multiHostsList.size())));
				
				con = findConnection(selectedHost.getAddress());
				if (con != null)
					return con;
				else
					multiHostsList.remove(selectedHost);
			}
			return con;
		}
		else
			return findConnection(hostAddress);//取第一跳的节点地址
	}
	/**
	 * 通过更新路由表，找到当前信息应当转发的下一跳节点，并且根据预先设置决定此计算得到的路径信息是否需要写入信息msg头部当中
	 * @param message
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public Tuple<Message, Connection> findPathFromRouterTabel(Message message, List<Connection> connections, boolean msgPathLabel){
		
		if (updateRouterTable(message) == false){//在传输之前，先更新路由表
			return null;//若没有返回说明一定找到了对应路径
		}
		List<Tuple<Integer, Boolean>> routerPath = this.routerTable.get(message.getTo());
		
		if (msgPathLabel == true){//如果写入路径信息标志位真，就写入路径消息
			message.updateProperty(MSG_ROUTERPATH, routerPath);
		}
				
		//Connection path = findConnection(routerPath.get(0).getKey());//取第一跳的节点地址
		
		/**确保最后一跳直接送达**/
		if (finalHopLabel == true){
			Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, finalHopConnection);//找到与第一跳节点的连接
			return t;
		}
		
		Connection path = NetgridMultiPathMatchingProcess(routerPath.get(0).getKey());//通过同一网格中含有多个节点的网格时，可以采用多路径
		
		if (path != null){
			Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, path);//找到与第一跳节点的连接
			return t;
		}
		else{			
			
			if (routerPath.get(0).getValue()){
				
				return null;
				//DTNHost nextHop = this.getHostFromAddress(routerPath.get(0).getKey()); 
				//this.busyLabel.put(message.getId(), startTime);//设置一个等待
			}
			else{
//				throw new SimError("No such connection: "+ routerPath.get(0) + 
//						" at routerTable " + this);		
				this.routerTable.remove(message.getTo());	
				return null;

			}
		}
	}

	/**
	 * 由节点地址找到对应的节点DTNHost
	 * @param address
	 * @return
	 */
	public DTNHost findHostByAddress(int address){
		for (DTNHost host : getHosts()){
			if (host.getAddress() == address)
				return host;
		}
		return null;
	}
	/**
	 * 由下一跳节点地址寻找对应的邻居连接
	 * @param address
	 * @return
	 */
	public Connection findConnectionByAddress(int address){
		for (Connection con : this.getHost().getConnections()){
			if (con.getOtherNode(this.getHost()).getAddress() == address)
				return con;
		}
		return null;
	}

	/**
	 * 更新路由表，包括1、更新已有链路的路径；2、进行全局预测
	 * @param m
	 * @return
	 */
	public boolean updateRouterTable(Message msg){
		gridSearch(msg);
		
		if (this.routerTable.containsKey(msg.getTo())){//预测也找不到到达目的节点的路径，则路由失败		
			//System.out.println("寻路成功！！！    "+" Path length:  "+routerTable.get(msg.getTo()).size()+" routertable size: "+routerTable.size()+" Netgrid Path:  "+routerTable.get(msg.getTo()));
			return true;//找到了路径
		}else{
			//System.out.println("寻路失败！！！");
			return false;
		}
	}
	
	/**
	 * 冒泡排序
	 * @param distanceList
	 * @return
	 */
	public List<Tuple<DTNHost, Double>> sort(List<Tuple<DTNHost, Double>> distanceList){
		for (int j = 0; j < distanceList.size(); j++){
			for (int i = 0; i < distanceList.size() - j - 1; i++){
				if (distanceList.get(i).getValue() > distanceList.get(i + 1).getValue()){//从小到大，大的值放在队列右侧
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

	private HashMap<GridCell, DTNHost> GridCellToDTNHosts = new HashMap<GridCell, DTNHost>();//记录有单个节点的网格
	private HashMap<DTNHost, GridCell> DTNHostToGridCell = new HashMap<DTNHost, GridCell>();
	private HashMap<GridCell, List<DTNHost>> GridCellhasMultiDTNHosts = new HashMap<GridCell, List<DTNHost>>();//记录有多个节点的网格
	/**
	 * 更新记录网格和DTNHost节点关系表
	 */
	public void updateRelationshipofGridsAndDTNHosts(){
		DTNHostToGridCell.clear();
		GridCellToDTNHosts.clear();
		
		/**全局节点遍历一次**/
		for (DTNHost h : getHosts()){
			if (h == null)
				throw new SimError("null");
			GridCell Netgrid = GN.getGridCellFromCoordNow(h);
			if (Netgrid == null)
				throw new SimError("null");
			DTNHostToGridCell.put(h, Netgrid);
	
			if (!GridCellToDTNHosts.containsKey(Netgrid)){
				GridCellToDTNHosts.put(Netgrid, h);
			}
			else{
				if (GridCellhasMultiDTNHosts.containsKey(Netgrid)){
					List<DTNHost> hostsList = GridCellhasMultiDTNHosts.get(Netgrid);
					hostsList.add(h);
					GridCellhasMultiDTNHosts.put(Netgrid, hostsList);
				}
				else{
					List<DTNHost> hostsLists = new ArrayList<DTNHost>();
					hostsLists.add(GridCellToDTNHosts.get(Netgrid));
					hostsLists.add(h);
					GridCellhasMultiDTNHosts.put(Netgrid, hostsLists);
				}
			}
		}
		for (GridCell c : GridCellhasMultiDTNHosts.keySet()){//最后从GridCellToDTNHosts列表中剔除有多个节点的网格
			GridCellToDTNHosts.remove(c);
		}	
		//System.out.println(GridCellToDTNHosts.size()+" "+GridCellToDTNHosts + " \n  " + DTNHostToGridCell.size()+"  "
		//		+DTNHostToGridCell + "  \n  " +GridCellhasMultiDTNHosts.size()+" "+GridCellhasMultiDTNHosts);
	}
	
    /**
     * Return current network topology in forms of temporal graph
     */
    public HashMap<DTNHost, List<DTNHost>> globalNetGridCaluculation() {
        HashMap<DTNHost, GridCell> locationRecord = new HashMap<DTNHost, GridCell>();
        HashMap<GridCell, List<DTNHost>> inclusionRelation = new HashMap<GridCell, List<DTNHost>>();
        HashMap<DTNHost, List<DTNHost>> topologyInfo = new HashMap<DTNHost, List<DTNHost>>();

        double radius = transmitRange;//Represent communication Radius
        //Get satellite movement model which store orbit-info of all satellites in the network
        SatelliteMovement movementModel = ((SatelliteMovement) this.getHost().getMovementModel());

        //Calculate the current coordinate of all satellite nodes in the network
        for (DTNHost h : movementModel.getHosts()) {
        	GridCell gc = GN.cellFromCoord(movementModel.getCoordinate(h, SimClock.getTime()));
            locationRecord.put(h, gc);
            
            if (inclusionRelation.containsKey(gc)){
            	inclusionRelation.get(gc).add(h);//add new host belongs to this GridCell
            }
            else{
            	List<DTNHost> hosts = new ArrayList<DTNHost>();
            	hosts.add(h);
            	inclusionRelation.put(gc, hosts);
            }
        }

        //Calculate links between each two satellite nodes
        for (DTNHost h : movementModel.getHosts()) {
            GridCell gc = locationRecord.get(h);
            List<GridCell> neighborList = GN.getNeighborCells(gc.getNumber()[0], gc.getNumber()[1], gc.getNumber()[2]);
            for (GridCell gridCell: neighborList){
            	if (topologyInfo.get(h) == null)
                    topologyInfo.put(h, new ArrayList<DTNHost>());
            	if (inclusionRelation.containsKey(gridCell))
            		topologyInfo.get(h).addAll(inclusionRelation.get(gridCell));
            }
        }
        return topologyInfo;
    }
	/**
	 * 核心路由算法，运用贪心选择性质进行遍历，找出到达目的节点的最短路径
	 * @param msg
	 */
	public void gridSearch(Message msg){		
		this.finalHopLabel = false;
		this.finalHopConnection = null;
		
		if (routerTableUpdateLabel == true)//routerTableUpdateLabel == true则代表此次更新路由表已经更新过了，所以不要重复计算
			return;
		this.routerTable.clear();
		this.arrivalTime.clear();
	
		if (GN.isHostsListEmpty()){
			GN.setHostsList(getHosts());
		}
		
		HashMap<DTNHost, List<DTNHost>> topologyInfo;	
		topologyInfo = globalNetGridCaluculation();
		switch (gridUpdateOption){
		case "onlineOrbitCalculation":
			
			break;
		case "preOrbitCalculation"://通过提前利用网格表存储各个节点的轨道信息，从而运行过程中不再调用轨道计算函数来预测而是通过读表来预测
			/**!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!更新的时间段待修改!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!**/
			//GN.updateNetGridInfo_without_OrbitCalculation(this.RoutingTimeNow);//实际仿真时用，用于读取事先计算好的网格表
			GN.updateNetGridInfo_without_OrbitCalculation_without_gridTable();//加快仿真进度用，直接读取现有的节点坐标值，然后转换成对应网格坐标
			updateRelationshipofGridsAndDTNHosts();//需要放在gridTable更新之后
			//GN.updateGrid_without_OrbitCalculation(this.RoutingTimeNow);//更新网格表
			/**!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!**/
			break;
		}
		/**全网的传输速率假定为一样的**/
		double transmitSpeed = this.getHost().getInterface(1).getTransmitSpeed();
		
		/**添加链路可探测到的一跳邻居网格，并更新路由表**/
		List<DTNHost> searchedSet = new ArrayList<DTNHost>();
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		sourceSet.add(this.getHost());//初始时只有源节点所
		searchedSet.add(this.getHost());//初始时只有源节点
		
		for (Connection con : this.getHost().getConnections()){//添加链路可探测到的一跳邻居，并更新路由表
			DTNHost neiHost = con.getOtherNode(this.getHost());
			sourceSet.add(neiHost);//初始时只有本节点和链路邻居		
			Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
			List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
			Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
			path.add(hop);//注意顺序
			arrivalTime.put(neiHost, time);
			routerTable.put(neiHost, path);
			
			if (msg.getTo() == neiHost){//一跳的邻居节点，直接返回
				finalHopLabel = true;
				finalHopConnection = con;
//				System.out.println(msg+" through "+finalHopConnection+"  to "+msg.getTo());
				//GridCell desNetgrid = GN.getGridCellFromCoordNow(msg.getTo());
				//System.out.println(desNetgrid+"  "+neighborNetgrid+"  "+SimClock.getTime());
				return;
			}
		}
		
		/**添加链路可探测到的一跳邻居网格，并更新路由表**/		
		int iteratorTimes = 0;
		int size = getHosts().size();
		boolean updateLabel = true;
		boolean predictLable = false;

		arrivalTime.put(this.getHost(), SimClock.getTime());//初始化到达时间
		
		/**优先级队列，做排序用**/
		List<Tuple<DTNHost, Double>> PriorityQueue = new ArrayList<Tuple<DTNHost, Double>>();
		/**优先级队列，做排序用**/
		
		while(true){//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
			if (iteratorTimes >= size )//|| updateLabel == false)
				break; 
			updateLabel = false;
			
			for (DTNHost c : sourceSet){															
				//List<DTNHost> neighborHostsList = GN.getNeighborsHostsNow(GN.cellFromCoord(c.getLocation()));//获取源集合中host节点的邻居节点(当前的邻居网格)
				List<DTNHost> neighborHostsList = topologyInfo.get(c);
				/**添加同一轨道内的相邻节点，以及相邻轨道内的最近的相邻节点**/
				neighborHostsList.removeAll(((NetGridRouter)c.getRouter()).neighborHostsInSamePlane);//去重复
				neighborHostsList.addAll(((NetGridRouter)c.getRouter()).neighborHostsInSamePlane);//添加同一轨道内的相邻节点
				if (!((NetGridRouter)c.getRouter()).neighborPlaneHosts.isEmpty()){
					neighborHostsList.removeAll(((NetGridRouter)c.getRouter()).neighborPlaneHosts);//去重复
					neighborHostsList.addAll(((NetGridRouter)c.getRouter()).neighborPlaneHosts);//添加相邻轨道内的最近的相邻节点
				}
				//System.out.println("RoutingHost and time :  "+this.getHost()+this.RoutingTimeNow+"  thisHostGrid:  "+thisHostGrid  +"  SourceNetgird:  "+c+"  contains:  "+GN.getHostsFromNetgridNow(c, this.RoutingTimeNow)+"  NeighborNetgrid:  "+neighborNetgridsList.keySet()+" contains: "+neighborNetgridsList.values()+"  sourceSet:  "+sourceSet);
										
				/**判断是否已经是搜索过的源网格集合中的网格**/
				if (searchedSet.contains(c) || neighborHostsList == null)
					continue;				
				searchedSet.add(c);
				
				for (DTNHost eachNeighborHost : neighborHostsList){//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
					if (sourceSet.contains(eachNeighborHost))//确保不回头
						continue;
				
					double time = arrivalTime.get(c) + msg.getSize()/transmitSpeed;
					
					/**添加路径信息**/
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					if (this.routerTable.containsKey(c))
						path.addAll(this.routerTable.get(c));
					Tuple<Integer, Boolean> thisHop = new Tuple<Integer, Boolean>(eachNeighborHost.getAddress(), predictLable);
					path.add(thisHop);//注意顺序
					/**添加路径信息**/
					
					/**维护最小传输时间的队列**/
					if (arrivalTime.containsKey(eachNeighborHost)){
						/**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
						if (time <= arrivalTime.get(eachNeighborHost)){
							if (random.nextBoolean() == true && 
									time - arrivalTime.get(eachNeighborHost) < 0.1){//如果时间相等，做随机化选择
								
								/**注意，在对队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
								int index = -1;
								for (Tuple<DTNHost, Double> t : PriorityQueue){
									if (t.getKey() == eachNeighborHost){
										index = PriorityQueue.indexOf(t);
									}
								}
								/**注意，在上面对PriorityQueue队列进行迭代的时候，不能够在for循环里面对此队列进行修改操作，否则会报错**/
								if (index > -1){
									PriorityQueue.remove(index);
									PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborHost, time));
									arrivalTime.put(eachNeighborHost, time);
									routerTable.put(eachNeighborHost, path);
								}
							}
						}
						/**检查队列中是否已有通过此网格的路径，如果有，看哪个时间更短**/
					}
					else{						
						PriorityQueue.add(new Tuple<DTNHost, Double>(eachNeighborHost, time));
						arrivalTime.put(eachNeighborHost, time);
						routerTable.put(eachNeighborHost, path);
					}
					/**对队列进行排序**/
					sort(PriorityQueue);					
					updateLabel = true;
				}
			}
			iteratorTimes++;
			for (int i = 0; i < PriorityQueue.size(); i++){
				if (!sourceSet.contains(PriorityQueue.get(i).getKey())){
					sourceSet.add(PriorityQueue.get(i).getKey());//将新的最短网格加入
					break;
				}
			}				
//			if (netgridRouterTable.containsKey(msg.getTo()))//如果中途找到需要的路徑，就直接退出搜索
//				break;
		}
		routerTableUpdateLabel = true;
	}
	
	/**
	 * 通过信息头部内的路径信息(节点地址)找到对应的节点，DTNHost类
	 * @param path
	 * @return
	 */
	public List<DTNHost> getHostListFromPath(List<Integer> path){
		List<DTNHost> hostsOfPath = new ArrayList<DTNHost>();
		for (int i = 0; i < path.size(); i++){
			hostsOfPath.add(this.getHostFromAddress(path.get(i)));//根据节点地址找到DTNHost 
		}
		return hostsOfPath;
	}
	/**
	 * 通过节点地址找到对应的节点，DTNHost类
	 * @param address
	 * @return
	 */
	public DTNHost getHostFromAddress(int address){
		for (DTNHost host : getHosts()){
			if (host.getAddress() == address)
				return host;
		}
		return null;
	}

	/**
	 * 计算通过预测节点到达，所需的传输时间(即传输时间加上等待时间)
	 * @param msgSize
	 * @param startTime
	 * @param host
	 * @param nei
	 * @return
	 */
	public double calculatePredictionDelay(int msgSize, double startTime, DTNHost host, DTNHost nei){
		if (startTime >= SimClock.getTime()){
			double waitTime;
			waitTime = startTime - SimClock.getTime() + msgSize/((nei.getInterface(1).getTransmitSpeed() > 
									host.getInterface(1).getTransmitSpeed()) ? host.getInterface(1).getTransmitSpeed() : 
										nei.getInterface(1).getTransmitSpeed()) + this.transmitRange*1000/SPEEDOFLIGHT;//取二者较小的传输速率;
			return waitTime;
		}
		else{
			assert false :"预测结果失效 ";
			return -1;
		}
	}
	/**
	 * 计算当前节点与一跳邻居的传输延时
	 * @param msgSize
	 * @param host
	 * @return
	 */
	public double calculateNeighborsDelay(int msgSize, DTNHost host){
		double transmitDelay = msgSize/((this.getHost().getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : this.getHost().getInterface(1).getTransmitSpeed()) + getDistance(this.getHost(), host)*1000/SPEEDOFLIGHT;//取二者较小的传输速率
		return transmitDelay;
	}
	
	/**
	 * 计算两个节点之间的距离
	 * @param a
	 * @param b
	 * @return
	 */
	public double getDistance(DTNHost a, DTNHost b){
		double ax = a.getLocation().getX();
		double ay = a.getLocation().getY();
		double az = a.getLocation().getZ();
		double bx = a.getLocation().getX();
		double by = a.getLocation().getY();
		double bz = a.getLocation().getZ();
		
		double distance = (ax - bx)*(ax - bx) + (ay - by)*(ay - by) + (az - bz)*(az - bz);
		distance = Math.sqrt(distance);
		
		return distance;
	}
	/**
	 * Find the DTNHost according to its address
	 * @param address
	 * @return
	 */
	public Connection findConnection(int address){
		List<Connection> connections = this.getHost().getConnections();
		for (Connection c : connections){
			if (c.getOtherNode(this.getHost()).getAddress() == address){
				return c;
			}
		}
		return null;
	}
	/**
	 * 根据这一跳的可选节点地址集合，选择一个最合适的下一跳节点并找到对应的connection进行发送
	 * @param address
	 * @return
	 */
	public Connection findConnectionFromHosts(Message msg, List<Integer> hostsInThisHop){
		if (hostsInThisHop.size() == 1){
			return findConnection(hostsInThisHop.get(0));
		}
		/**有多个可选下一跳节点的时候**/
		else{
			/**确保一跳的传输不会错过**/
			DTNHost destination = msg.getTo();
			for (int i = 0; i < hostsInThisHop.size(); i++){
				Connection connect = findConnection(hostsInThisHop.get(i));
				
				/**路由找到的路径可能出现错误，导致当前路径不可用**/
				if (connect == null) 
					return null;
				/**路由找到的路径可能出现错误，导致当前路径不可用**/
				
				if (connect.getOtherInterface(this.getHost().getInterface(1)).getHost() == destination)
					return connect;
			}
			/**确保一跳的传输不会错过**/
			/****************************************************************!!!!!待修改!!!!!!**************************************************************************/
			int randomInt = this.random.nextInt(hostsInThisHop.size());
			Connection con = findConnection(hostsInThisHop.get(randomInt) - 1);//注意要减一，因为是ArrayList，数组下标
			if (con != null){
				return con;
			}
			/**一旦有一次失败就进行遍历寻找**/
			else{
				for (int i = 0; i < hostsInThisHop.size(); i++){
					con = findConnection(i);
					/**遍历所有可能性，找出一个可达的邻居节点，否则返回null**/
					if (con != null)
						return con;
				}
			}
			
			return null;
			/****************************************************************!!!!!待修改!!!!!!**************************************************************************/
		}
	}
    /**
     * Try to send the message through a specific connection
     *
     * @param t
     * @return
     */
	public Message tryMessageToConnection(Tuple<Message, Connection> t){
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
	public boolean nextHopIsBusyOrNot(Tuple<Message, Connection> t){		
		Connection con = t.getValue();
        if (con == null)
        	return false;
		/**检查所经过路径的情况，如果下一跳的链路已经被占用，则需要等待**/
		if (con.isTransferring() || ((NetGridRouter)con.getOtherNode(this.getHost()).getRouter()).isTransferring()){	
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
		//模拟了无线广播链路，即邻居节点之间同时只能有一对节点传输数据!!!!!!!!!!!!!!!!!!!!!!!!!!!
		//需要修改!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			if (!con.isReadyForTransfer()) {//isReadyForTransfer返回false则表示有信道在被占用，因此对于广播信道而言不能传输
				return true;	// a connection isn't ready for new transfer
			}
		}		
		return false;		
	}
	/**
	 * 此重写函数保证在传输完成之后，源节点的信息从messages缓存中删除
	 */
	@Override
	protected void transferDone(Connection con){
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
		int startNumber = nrofSatelliteInOnePlane * (nrofPlane - 1);//此轨道平面内的节点，起始编号
		int endNumber = nrofSatelliteInOnePlane * nrofPlane - 1;//此轨道平面内的节点，结尾编号
		if (n < startNumber)
			return endNumber;
		if (n > endNumber)
			return startNumber;
		//int nrofPlane = n/nrofSatelliteInOnePlane + 1;
		return n;
	}
	/**
	 * 初始化设定本节点的同轨邻居节点
	 */
	public void initInterSatelliteNeighbors(){
		Settings setting = new Settings("userSetting");
		Settings sat = new Settings("Group");
		int TOTAL_SATELLITES = sat.getInt("nrofHosts");//总节点数
		int TOTAL_PLANE = setting.getInt("nrofPlane");//总轨道平面数
		int NROF_S_EACHPLANE = TOTAL_SATELLITES/TOTAL_PLANE;//每个轨道平面上的节点数
		
		int thisHostAddress = this.getHost().getAddress();
		
		int upperBound = getHosts().size() - 1;
		int a = processBound(thisHostAddress + 1, thisHostAddress/NROF_S_EACHPLANE + 1, NROF_S_EACHPLANE);
		int b = processBound(thisHostAddress - 1, thisHostAddress/NROF_S_EACHPLANE + 1, NROF_S_EACHPLANE);		
		
		for (DTNHost host : getHosts()){
			if (host.getAddress() == a || host.getAddress() == b){
				neighborHostsInSamePlane.remove(host);
				neighborHostsInSamePlane.add(host);//同一个轨道内的相邻节点
			}
		}
	}
	/**
	 * 初始化设定本节点的相邻轨道的邻居节点(因为在边缘轨道平面时的简单对应关系存在一些问题，所以需要动态更新)
	 */
	public void updateInterSatelliteNeighbors(List<DTNHost> conNeighbors){	
		SatelliteMovement movementModel = ((SatelliteMovement)this.getHost().getMovementModel());
		
		int TOTAL_SATELLITES = movementModel.getTotalNrofLEOSatellites();//总节点数		
		int TOTAL_PLANE = movementModel.getTotalNrofLEOPlanes();//总轨道平面数
		int NROF_S_EACHPLANE = TOTAL_SATELLITES/TOTAL_PLANE;//每个轨道平面上的节点数
		
		int thisHostAddress = this.getHost().getAddress();
		
		int upperBound = getHosts().size() - 1;
		int c = processBoundOfNumber(thisHostAddress + NROF_S_EACHPLANE, 0, upperBound);
		int d = processBoundOfNumber(thisHostAddress - NROF_S_EACHPLANE, 0, upperBound);
		

		for (DTNHost host : getHosts()){
			if (host.getAddress() == c){
				if (!conNeighbors.contains(host)){
					double minDistance = Double.MAX_VALUE;
					DTNHost minHost = null;	
					for (DTNHost neighborHost : conNeighbors){//如果不包含就从已有的c所属的轨道平面上的节点，选一个最近的
						int planeOfC = c/NROF_S_EACHPLANE + 1;//两个相邻节点各自所属的轨道平面号
						if (planeOfC == movementModel.getNrofPlane()){
							if (neighborHost.getLocation().distance(this.getHost().getLocation()) < minDistance)
								minHost = neighborHost;
						}
					}
					if (minHost != null){
						neighborPlaneHosts.remove(minHost);//去重复添加
						neighborPlaneHosts.add(minHost);
					}
				}
				else{
					neighborPlaneHosts.remove(host);//去重复添加
					neighborPlaneHosts.add(host);
				}
			}
			
			if (host.getAddress() == d){
				if (!conNeighbors.contains(host)){
					double minDistance = Double.MAX_VALUE;
					DTNHost minHost = null;	
					for (DTNHost neighborHost : conNeighbors){
						int planeOfD = d/NROF_S_EACHPLANE + 1;//两个相邻节点各自所属的轨道平面号
						if (planeOfD == movementModel.getNrofPlane()){
							if (neighborHost.getLocation().distance(this.getHost().getLocation()) < minDistance)
								minHost = neighborHost;
						}
					}
					if (minHost != null){
						neighborPlaneHosts.remove(minHost);//去重复添加
						neighborPlaneHosts.add(minHost);
					}
				}
				else{
					neighborPlaneHosts.remove(host);//去重复添加
					neighborPlaneHosts.add(host);
				}
			}
		}
	}
	
	public class GridNeighbors {
		
		private List<DTNHost> hosts = new ArrayList<DTNHost>();//全局卫星节点列表
		private DTNHost host;
		private double transmitRange;
		private double msgTtl;
		
		private double updateInterval = 1;
		
		private GridCell[][][] cells;//GridCell这个类，创建一个实例代表一个单独的网格，整个world创建了一个三维数组存储这个网格，每个网格内又存储了当前在其中的host的networkinterface
		
		private double cellSize;
		private int rows;
		private int cols;
		private int zs;//新增三维变量
		private  int worldSizeX;
		private  int worldSizeY;
		private  int worldSizeZ;//新增
		
		private int gridLayer;
		
		/**每次routing进行更新时，用于存储指定时间的拓扑状态，网格和节点的映射关系**/
//		private HashMap<Double, HashMap<NetworkInterface, GridCell>> gridmap = new HashMap<Double, HashMap<NetworkInterface, GridCell>>();
//		private HashMap<Double, HashMap<GridCell, List<DTNHost>>> cellmap = new HashMap<Double, HashMap<GridCell, List<DTNHost>>>();
		
		/**当前瞬时时刻的拓扑状态，包含网格和节点的映射关系**/
		HashMap<NetworkInterface, GridCell> interfaceToGridCell = new HashMap<NetworkInterface, GridCell>();
		HashMap<GridCell, List<DTNHost>> gridCellToHosts = new HashMap<GridCell, List<DTNHost>>();
		
		/*用于初始化时，计算各个节点在一个周期内的网格坐标*/
		private HashMap <DTNHost, List<GridCell>> gridLocation = new HashMap<DTNHost, List<GridCell>>();//存放节点所经过的网格
		private HashMap <DTNHost, List<Double>> gridTime = new HashMap<DTNHost, List<Double>>();//存放节点经过这些网格时的时间
		private HashMap <DTNHost, Double> periodMap = new HashMap <DTNHost, Double>();//记录各个节点轨道的周期
		
		public GridNeighbors(DTNHost host){
			this.host = host;
			//System.out.println(this.host);
			Settings se = new Settings("Interface");
			transmitRange = se.getDouble("transmitRange");//从配置文件中读取传输速率
			Settings set = new Settings("Group");
			msgTtl = set.getDouble("msgTtl");
			
			Settings s = new Settings(MovementModel.MOVEMENT_MODEL_NS);
			int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE,3);//参数从2维修改为3维
			worldSizeX = worldSize[0];
			worldSizeY = worldSize[1];
			worldSizeZ = worldSize[1];//新增三维变量，待检查！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
			
			Settings layer = new Settings("Group");
			this.gridLayer = layer.getInt("layer");
			
			switch(this.gridLayer){
			case 1 : 
				cellSize =  (transmitRange*0.2886751345);//Layer=2
				break;
			case 2 : 
				cellSize =  (transmitRange*0.144337567);//Layer=3
				break;
			case 3:
				cellSize =  (transmitRange*0.0721687836);//Layer=4
				break;
			default :
				cellSize =  (transmitRange*0.2886751345);//Layer=2
				break;
			}
			//cellSize = (int) (transmitRange*0.5773502);
			
			CreateGrid(cellSize);
			/*初始化，前提算好卫星轨道信息*/
			
		}
		public void setHost(DTNHost h){
			this.host = h;
		}
		public DTNHost getHost(){
			return this.host;
		}
		/**
		 * 初始化创建固定的网格
		 * @param cellSize
		 */
		public void CreateGrid(double cellSize){
			this.rows = (int)Math.floor(worldSizeY/cellSize) + 1;
			this.cols = (int)Math.floor(worldSizeX/cellSize) + 1;
			this.zs = (int)Math.floor(worldSizeZ/cellSize) + 1;//新增
			System.out.println(cellSize+"  "+this.rows+"  "+this.cols+"  "+this.zs);
			// leave empty cells on both sides to make neighbor search easier 
			this.cells = new GridCell[rows+2][cols+2][zs+2];
			this.cellSize = cellSize;

			for (int i=0; i<rows+2; i++) {
				for (int j=0; j<cols+2; j++) {
					for (int n=0;n<zs+2; n++){//新增三维变量
						this.cells[i][j][n] = new GridCell();
						cells[i][j][n].setNumber(i, j, n);
					}
				}
			}
		}
		/**
		 * 遍歷所有節點，對每個節點遍歷一個週期，記錄其一個週期內遍歷過的網格，并找到對應的進入和離開時間
		 */
		public void initializeGridLocation(){	

			for (DTNHost h : getHosts()){//對每個節點遍歷一個週期，記錄其一個週期內遍歷過的網格，并找到對應的進入和離開時間
				double period = getPeriodofOrbit(h);
				this.periodMap.put(h, period);
				System.out.println(this.host+" now calculate "+h+"  "+period);
				
				List<GridCell> gridList = new ArrayList<GridCell>();
				List<Double> intoTime = new ArrayList<Double>();
				List<Double> outTime = new ArrayList<Double>();
				GridCell startCell = cellFromCoord(h.getCoordinate(0));//记录起始网格
				for (double time = 0; time < period; time += updateInterval){
					Coord c = h.getCoordinate(time);
					GridCell gc = cellFromCoord(c);//根據坐標找到對應的網格
					if (!gridList.contains(gc)){
						if (gridList.isEmpty()){
							startCell = gc;//记录起始网格
							gridList.add(null);//把起始网格第一次放空指针，占个位
							intoTime.add(time);
						}						
						gridList.add(gc);//第一次检测到节点进入此网格（注意，边界检查！！！开始和结束的时候！！！!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!）
						intoTime.add(time);//记录相应的进入时间
						if (gc == startCell){
							gridList.set(0, startCell);
							intoTime.set(0, time);
						}
					}	
					else{
						//设置起始网格的真正进入时间，在一个轨道周期内
						if (gc == startCell){
							gridList.set(0, startCell);
							intoTime.set(0, time);
						}						
					}
				}
				//System.out.println(h+" startCell "+h.getCoordinate(1)+" time: "+h.getCoordinate(0)+ "  "+h.getCoordinate(period)+ "  "+h.getCoordinate(6024)+ "  "+h.getCoordinate(6023));
				//System.out.println(h+" startCell "+startCell+" time: "+intoTime.get(0)+ "  "+intoTime.get(1)+"  "+intoTime.get(intoTime.size()-1)+"  "+gridLocation);
				gridLocation.put(h, gridList);//遍历完一个节点就记录下来
				gridTime.put(h, intoTime);
			}
			System.out.println(gridLocation);
		}
		
		
		/**用于测试拓扑计算的开销，可删**/
		public void computationComplexityOfGridCalculation(double time, int RunningTimes){	
			
			double orbitCost = 0;
			double GridCost = 0;
			double totalCost = 0;
			
			HashMap<DTNHost, Tuple<Coord, GridCell>> relationship = new HashMap<DTNHost, Tuple<Coord, GridCell>>();
			HashMap<GridCell, DTNHost> gridMap = new HashMap<GridCell, DTNHost>();
			HashMap<GridCell, List<GridCell>> edges = new HashMap<GridCell, List<GridCell>>();
			
			
			
			for (int n = 0; n < RunningTimes; n++){
				double t00 = System.nanoTime();//复杂度测试代码,精确到纳秒
				for (DTNHost h : getHosts()){//對每個節點遍歷一個週期，記錄其一個週期內遍歷過的網格，并找到對應的進入和離開時間
						
					Coord c = h.getCoordinate(time);					
					
					GridCell gc = cellFromCoord(c);//根據坐標找到對應的網格
					relationship.put(h, new Tuple<Coord, GridCell>(c, gc));
					
					gridMap.put(gc, h);
					
				}	
				double t01 = System.nanoTime();//复杂度测试代码,精确到纳秒
				System.out.println(n+"  轨道计算开销: "+ (t01-t00));
				orbitCost += (t01-t00);
				
				
				for (DTNHost h : getHosts()){
					GridCell gc = relationship.get(h).getValue();
					List<GridCell> neighbors = new ArrayList<GridCell>();
				//	for (int i = 0; i < 132; i++){
						for (GridCell c : getLayer1(gc.number[0], gc.number[1], gc.number[2])){
							if (gridMap.containsKey(c)){
								neighbors.add(c);
							}
					
						}
						edges.put(gc, neighbors);
					}

				//}
				double t02 = System.nanoTime();//复杂度测试代码,精确到纳秒
				System.out.println(n+"  总共开销: " + (t02-t00) + "  网格计算以及存放开销: "+ (t02-t01));
				GridCost += (t02-t01);
				totalCost += (t02-t00);
			}		
			System.out.println("  平均总共开销: " + totalCost/RunningTimes + "  平均轨道计算开销: "+ orbitCost/RunningTimes + "  平均边计算开销: "+ GridCost/RunningTimes);
			throw new SimError("Pause");		
		}
		/**用于测试拓扑计算的开销，可删**/
		public GridCell[] getLayer1(int row, int col, int z){
			List<GridCell> GC = new ArrayList<GridCell>();
			return new GridCell[] {
					cells[row-1][col-1][z],cells[row-1][col][z],cells[row-1][col+1][z],//1st row
					cells[row][col-1][z],cells[row][col][z],cells[row][col+1][z],//2nd row
					cells[row+1][col-1][z],cells[row+1][col][z],cells[row+1][col+1][z],//3rd row
							
					cells[row-1][col-1][z],cells[row-1][col][z],cells[row-1][col+1][z],//1st row
					cells[row][col-1][z],cells[row][col][z],cells[row][col+1][z],//2nd row
					cells[row+1][col-1][z],cells[row+1][col][z],cells[row+1][col+1][z],//3rd row	
							
					cells[row-1][col-1][z],cells[row-1][col][z],cells[row-1][col+1][z],//1st row
					cells[row][col-1][z],cells[row][col][z],cells[row][col+1][z],//2nd row
					cells[row+1][col-1][z],cells[row+1][col][z],cells[row+1][col+1][z]//3rd row
				};
//			for (int i = -1; i < 2; i += 1){
//				for (int j = -1; j < 2; j += 1){
//					for (int k = -1; k < 2; k += 1){
//						GC.add(cells[row+i][col+j][z+k]);
//					}
//				}
//			}
//			return GC;
		}
		/**用于测试拓扑计算的开销，可删**/

		
		
		/**
		 * 獲取指定衛星節點的運行週期時間
		 * @param h
		 * @return
		 */
		public double getPeriodofOrbit(DTNHost h){
			return ((SatelliteMovement)this.getHost().getMovementModel()).getPeriod();
		}
		
		/**
		 * 找到host节点在当前时间对应所在的网格
		 * @param host
		 * @param time
		 * @return
		 */
		public GridCell getGridCellFromCoordNow(DTNHost host){
			/**注意读表方式获得的网格坐标，和实时三维坐标计算得到的网格坐标之间往往存在误差！**/
			return this.interfaceToGridCell.get(host.getInterface(1));
			//return cellFromCoord(host.getCoordinate(time));
		}
		
		/**
		 * 找到host节点在时间time对应所在的网格
		 * @param host
		 * @param time
		 * @return
		 */
		public GridCell getGridCellFromCoordAtTime(DTNHost host, double time){
			/**注意读表方式获得的网格坐标，和实时三维坐标计算得到的网格坐标之间往往存在误差！**/
			return this.interfaceToGridCell.get(host.getInterface(1));
			//return cellFromCoord(host.getCoordinate(time));
		}
		/**
		 * 获取指定时间点，指定网格的邻居网格
		 * @param source
		 * @param time
		 * @return
		 */
		public HashMap<GridCell, Tuple<GridCell, List<DTNHost>>> getNeighborsNetgridsNow(GridCell source){//获取指定时间的邻居节点(同时包含预测到TTL时间内的邻居)	

			//HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
			//GridCell cell = this.interfaceToGridCell.get(host.getInterface(1));
			int[] number = source.getNumber();//得到本网格的三维坐标
			
			List<GridCell> cellList = getNeighborCells(number[0], number[1], number[2]);//所有邻居的网格（当前时刻）
			/**找出所有的邻居网格以及其包含的节点**/
			HashMap<GridCell, Tuple<GridCell, List<DTNHost>>> neighborNetgridInfo = new HashMap<GridCell, Tuple<GridCell, List<DTNHost>>>();
			//List<Tuple<GridCell, List<DTNHost>>> gridInfoList = new ArrayList<Tuple<GridCell, List<DTNHost>>>();
			/**找出所有的邻居网格以及其包含的节点**/
			//assert cellmap.containsKey(time):" 时间错误 ";
			/**去除本网格**/
			if (cellList.contains(source))
				cellList.remove(source);
			
			for (GridCell c : cellList){
				if (this.gridCellToHosts.containsKey(c)){//如果不包含，这说明此邻居网格为空，里面不含任何节点
					List<DTNHost> hostList = new ArrayList<DTNHost>(this.gridCellToHosts.get(c));//找出这一个邻居网格内对应的所有节点
					Tuple<GridCell, List<DTNHost>> oneNeighborGrid = new Tuple<GridCell, List<DTNHost>>(c, hostList);
					neighborNetgridInfo.put(c, oneNeighborGrid);
				}
			}	
			
			//System.out.println(host+" 邻居列表   "+hostList);
			return neighborNetgridInfo;
		}
		/**
		 * 获取当前仿真时间下，指定网格的邻居网格内所含有的所有邻居节点
		 * @param source
		 * @param time
		 * @return
		 */
		public List<DTNHost> getNeighborsHostsNow(GridCell source){//获取指定时间的邻居节点(同时包含预测到TTL时间内的邻居)	
			//HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
			//GridCell cell = this.interfaceToGridCell.get(host.getInterface(1));
			int[] number = source.getNumber();//得到本网格的三维坐标
			
			List<GridCell> cellList = getNeighborCells(number[0], number[1], number[2]);//所有邻居的网格（当前时刻）
			/**找出所有的邻居网格以及其包含的节点**/

			/**去除本网格**/
			if (cellList.contains(source))
				cellList.remove(source);
			
			List<DTNHost> neighborHosts = new ArrayList<DTNHost>();
			
			for (DTNHost host : hosts){
				GridCell cell = cellFromCoord(host.getLocation());
				if (cellList.contains(cell))
					neighborHosts.add(host);
				
			}
//			for (GridCell c : cellList){
//				if (this.gridCellToHosts.containsKey(c)){//如果不包含，这说明此邻居网格为空，里面不含任何节点
//					neighborHosts.addAll(this.gridCellToHosts.get(c));//找出这一个邻居网格内对应的所有节点
//				}
//			}	
			
			//System.out.println(host+" 邻居列表   "+hostList);
			return neighborHosts;
		}
		
//		public List<DTNHost> getNeighbors(DTNHost host, double time){//获取指定时间的邻居节点(同时包含预测到TTL时间内的邻居)
//			int num = (int)((time-SimClock.getTime())/updateInterval);
//			time = SimClock.getTime()+num*updateInterval;
//			
//			if (time > SimClock.getTime()+msgTtl*60){//检查输入的时间是否超过预测时间
//				//assert false :"超出预测时间";
//				time = SimClock.getTime()+msgTtl*60;
//			}
//			
//			//double t0 = System.currentTimeMillis();
//			//System.out.println(t0);
//			
//			HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
//			GridCell cell = ginterfaces.get(host.getInterface(1));
//			int[] number = cell.getNumber();
//			
//			List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//所有邻居的网格（当前时刻）
//			List<DTNHost> hostList = new ArrayList<DTNHost>();//(邻居网格内的节点集合)
//			assert cellmap.containsKey(time):" 时间错误 ";
//			for (GridCell c : cellList){
//				if (cellmap.get(time).containsKey(c))//如果不包含，这说明此邻居网格为空，里面不含任何节点
//					hostList.addAll(cellmap.get(time).get(c));
//			}	
//			if (hostList.contains(host))//把自身节点去掉
//				hostList.remove(host);
//			
//			//double t1 = System.currentTimeMillis();
//			//System.out.println("search cost"+(t1-t0));
//			//System.out.println(host+" 邻居列表   "+hostList);
//			return hostList;
//		}

		
//		public Tuple<HashMap<DTNHost, List<Double>>, //neiList 为已经计算出的当前邻居节点列表
//			HashMap<DTNHost, List<Double>>> getFutureNeighbors(List<DTNHost> neiList, DTNHost host, double time){
//			int num = (int)((time-SimClock.getTime())/updateInterval);
//			time = SimClock.getTime()+num*updateInterval;	
//			
//			HashMap<DTNHost, List<Double>> leaveTime = new HashMap<DTNHost, List<Double>>();
//			HashMap<DTNHost, List<Double>> startTime = new HashMap<DTNHost, List<Double>>();
//			for (DTNHost neiHost : neiList){
//				List<Double> t= new ArrayList<Double>();
//				t.add(SimClock.getTime());
//				startTime.put(neiHost, t);//添加已存在邻居节点的开始时间
//			}
//			
//			List<DTNHost> futureList = new ArrayList<DTNHost>();//(邻居网格内的未来节点集合)
//			List<NetworkInterface> futureNeiList = new ArrayList<NetworkInterface>();//(预测未来邻居的节点集合)
//			
//			
//			Collection<DTNHost> temporalNeighborsBefore = startTime.keySet();//前一时刻的邻居，通过交叉对比这一时刻的邻居，就知道哪些是新加入的，哪些是新离开的			
//			Collection<DTNHost> temporalNeighborsNow = new ArrayList<DTNHost>();//用于记录当前时刻的邻居
//			for (; time < SimClock.getTime() + msgTtl*60; time += updateInterval){
//				
//				HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//取出time时刻的网格表
//				GridCell cell = ginterfaces.get(host.getInterface(1));//找到此时指定节点所处的网格位置
//				
//				int[] number = cell.getNumber();
//				List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//获取所有邻居的网格（当前时刻）
//				
//				for (GridCell c : cellList){	//遍历在不同时间维度上，指定节点周围网格的邻居
//					if (!cellmap.get(time).containsKey(c))
//						continue;
//					temporalNeighborsNow.addAll(cellmap.get(time).get(c));
//					for (DTNHost ni : cellmap.get(time).get(c)){//检查当前预测时间点，所有的邻居节点
//						if (ni == this.host)//排除自身节点
//							continue;
//						if (!neiList.contains(ni))//如果现有邻居中没有，则一定是未来将到达的邻居					
//							futureList.add(ni); //此为未来将会到达的邻居(当然对于当前已有的邻居，也可能会中途离开，然后再回来)
//										
//						/**如果是未来到达的邻居，直接get会返回空指针，所以要先加startTime和leaveTime判断**/
//						if (startTime.containsKey(ni)){
//							if (leaveTime.isEmpty())
//								break;
//							if (startTime.get(ni).size() == leaveTime.get(ni).size()){//如果不相等则一定是邻居节点离开的情况					
//								List<Double> mutipleTime= leaveTime.get(ni);
//								mutipleTime.add(time);
//								startTime.put(ni, mutipleTime);//将此新的开始时间加入
//							}
//							/*if (leaveTime.containsKey(ni)){//有两种情况，一种在预测时间段内此邻居会离开，另一种情况是此邻居不仅在此时间段内会离开还会回来
//								if (startTime.get(ni).size() == leaveTime.get(ni).size()){//如果不相等则一定是邻居节点离开的情况					
//									List<Double> mutipleTime= leaveTime.get(ni);
//									mutipleTime.add(time);
//									startTime.put(ni, mutipleTime);//将此新的开始时间加入
//								}
//								else{
//									List<Double> mutipleTime= leaveTime.get(ni);
//									mutipleTime.add(time);
//									leaveTime.put(ni, mutipleTime);//将此新的离开时间加入
//								}	
//							}
//							else{
//								List<Double> mutipleTime= new ArrayList<Double>();
//								mutipleTime.add(time);
//								leaveTime.put(ni, mutipleTime);//将此新的离开时间加入
//							}*/
//						}
//						else{
//							//System.out.println(this.host+" 出现预测节点: "+ni+" 时间  "+time);
//							List<Double> mutipleTime= new ArrayList<Double>();
//							mutipleTime.add(time);
//							startTime.put(ni, mutipleTime);//将此新的开始时间加入
//						}
//						/**如果是未来到达的邻居，直接get会返回空指针，所以要先加startTime和leaveTime判断**/
//					}	
//				}
//				
//				for (DTNHost h : temporalNeighborsBefore){//交叉对比这一时刻和上一时刻的邻居节点，从而找出离开的邻居节点
//					if (!temporalNeighborsNow.contains(h)){
//						List<Double> mutipleTime= leaveTime.get(h);
//						mutipleTime.add(time);
//						leaveTime.put(h, mutipleTime);//将此新的离开时间加入
//					}						
//				}
//				temporalNeighborsBefore.clear();
//				temporalNeighborsBefore = temporalNeighborsNow;
//				temporalNeighborsNow.clear();	
//			}
//			
//			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime= //二元组合并开始和结束时间
//					new Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>(startTime, leaveTime); 
//			
//			
//			return predictTime;
//		}


		public List<GridCell> getNeighborCells(int row, int col, int z){
			//HashMap<GridCell, List<DTNHost>> cellToHost = this.gridCellToHosts;//获取time时刻的全局网格表
			List<GridCell> GC = new ArrayList<GridCell>();
			/***********************************************************************/
			switch(this.gridLayer){
			case 1 : //只占有%15.5的体积
			/*两层网格分割*/
				for (int i = -1; i < 2; i += 1){
					for (int j = -1; j < 2; j += 1){
						for (int k = -1; k < 2; k += 1){
							GC.add(cells[row+i][col+j][z+k]);
						}
					}
				}
				break;
			case 2 : {//m=1时，只占有%28.5的体积
			/*三层网格分割*/
				for (int i = -3; i <= 3; i += 1){
					for (int j = -3; j <= 3; j += 1){
						for (int k = -3; k <= 3; k += 1){
							if (boundaryCheck(row+i,col+j,z+k))
								GC.add(cells[row+i][col+j][z+k]);
						}
					}
				}
				int m = 1;//默认m = 1;
				for (int j = -m; j <= m; j += 1){
					for (int k = -m; k <= m; k += 1){
						if (boundaryCheck(row+4,col+j,z+k)){
							GC.add(cells[row+4][col+j][z+k]);
						}
					}
				}
				for (int j = -m; j <= m; j += 1){
					for (int k = -m; k <= m; k += 1){
						if (boundaryCheck(row-4,col+j,z+k))
							GC.add(cells[row-4][col+j][z+k]);
					}
				}
				for (int j = -m; j <= m; j += 1){
					for (int k = -m; k <= m; k += 1){
						if (boundaryCheck(row+j,col+4,z+k))
							GC.add(cells[row+j][col+4][z+k]);
					}
				}
				for (int j = -m; j <= m; j += 1){
					for (int k = -m; k <= m; k += 1){
						if (boundaryCheck(row+j,col-4,z+k))
							GC.add(cells[row+j][col-4][z+k]);
					}
				}
				for (int j = -m; j <= m; j += 1){
					for (int k = -m; k <= m; k += 1){
						if (boundaryCheck(row+j,col+k,z+4))
							GC.add(cells[row+j][col+k][z+4]);
					}
				}
				for (int j = -m; j <= m; j += 1){
					for (int k = -m; k <= m; k += 1){
						if (boundaryCheck(row+j,col+k,z-4))
							GC.add(cells[row+j][col+k][z-4]);
					}
				}	
			}
			break;
			default :/*两层网格分割*/
				for (int i = -1; i < 2; i += 1){
					for (int j = -1; j < 2; j += 1){
						for (int k = -1; k < 2; k += 1){
							GC.add(cells[row+i][col+j][z+k]);	
						}
					}
				}
				break;
			
			case 3:{//n1=4,n2=2时，只占有%36的体积
				/*四层层网格分割*/
				for (int i = -7; i <= 7; i += 1){
					for (int j = -7; j <= 7; j += 1){
						for (int k = -7; k <= 7; k += 1){
							if (boundaryCheck(row+i,col+j,z+k))
								GC.add(cells[row+i][col+j][z+k]);

						}
					}
				}
				int n1 = 2;//默认n1 = 2;
				for (int j = -n1; j <= n1; j += 1){
					for (int k = -n1; k <= n1; k += 1){
						if (boundaryCheck(row+8,col+j,z+k)){
							GC.add(cells[row+8][col+j][z+k]);

						}
					}
				}
				for (int j = -n1; j <= n1; j += 1){
					for (int k = -n1; k <= n1; k += 1){
						if (boundaryCheck(row-8,col+j,z+k))
							GC.add(cells[row-8][col+j][z+k]);

					}
				}
				for (int j = -n1; j <= n1; j += 1){
					for (int k = -n1; k <= n1; k += 1){
						if (boundaryCheck(row+j,col+8,z+k))
							GC.add(cells[row+j][col+8][z+k]);

					}
				}
				for (int j = -n1; j <= n1; j += 1){
					for (int k = -n1; k <= n1; k += 1){
						if (boundaryCheck(row+j,col-8,z+k))
							GC.add(cells[row+j][col-8][z+k]);

					}
				}
				for (int j = -n1; j <= n1; j += 1){
					for (int k = -n1; k <= n1; k += 1){
						if (boundaryCheck(row+j,col+k,z+8))
							GC.add(cells[row+j][col+k][z+8]);

					}
				}
				for (int j = -n1; j <= n1; j += 1){
					for (int k = -n1; k <= n1; k += 1){
						if (boundaryCheck(row+j,col+k,z-8))
							GC.add(cells[row+j][col+k][z-8]);

					}
				}
				//
				int n2 = 1;//默认n2 = 1;
				for (int j = -n2; j <= n2; j += 1){
					for (int k = -n2; k <= n2; k += 1){
						if (boundaryCheck(row+9,col+j,z+k)){
							GC.add(cells[row+9][col+j][z+k]);

						}
					}
				}
				for (int j = -n2; j <= n2; j += 1){
					for (int k = -n2; k <= n2; k += 1){
						if (boundaryCheck(row-9,col+j,z+k))
							GC.add(cells[row-9][col+j][z+k]);

					}
				}
				for (int j = -n2; j <= n2; j += 1){
					for (int k = -n2; k <= n2; k += 1){
						if (boundaryCheck(row+j,col+9,z+k))
							GC.add(cells[row+j][col+9][z+k]);

					}
				}
				for (int j = -n2; j <= n2; j += 1){
					for (int k = -n2; k <= n2; k += 1){
						if (boundaryCheck(row+j,col-9,z+k))
							GC.add(cells[row+j][col-9][z+k]);

					}
				}
				for (int j = -n2; j <= n2; j += 1){
					for (int k = -n2; k <= n2; k += 1){
						if (boundaryCheck(row+j,col+k,z+9))
							GC.add(cells[row+j][col+k][z+9]);

					}
				}
				for (int j = -n2; j <= n2; j += 1){
					for (int k = -n2; k <= n2; k += 1){
						if (boundaryCheck(row+j,col+k,z-9))
							GC.add(cells[row+j][col+k][z-9]);

					}
				}
			}
		}
			//GC.add(cells[row][col][z]);//修改邻居网格的条件！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
			/***********************************************************************/
			return GC;
		}
		
		public boolean boundaryCheck(int i, int j, int k){
			if (i<0 || j<0 || k<0)
				return false;
			if (i > rows+1 || j > cols+1 || k > zs+1){
				return false;
			}
			return true;
		}
		
		public boolean isHostsListEmpty(){
			return this.hosts.isEmpty();
		}
		
		/**
		 * 测试新的网格获取方式下，路由算法的性能
		 * @param simClock
		 */
		public void updateNetGridInfo_without_OrbitCalculation_without_gridTable(){
			//if (gridLocation.isEmpty())//初始化只执行一次
			//	initializeGridLocation();
			
			HashMap<NetworkInterface, GridCell> ginterfaces = new HashMap<NetworkInterface, GridCell>();//每次清空;			
			HashMap<GridCell, List<DTNHost>> cellToHost = new HashMap<GridCell, List<DTNHost>>();
			
			for (DTNHost host : hosts){
				GridCell cell = null;
			
				cell = cellFromCoord(host.getLocation());
				//cell = this.getGridCellFromCoordNow(host);
				if (cell == null)
					throw new SimError(" cell error!");
				
				ginterfaces.put(host.getInterface(1), cell);
				
				List<DTNHost> hostList = new ArrayList<DTNHost>();
				if (cellToHost.containsKey(cell)){
					hostList.addAll(cellToHost.get(cell));	
				}
				hostList.add(host);
				cellToHost.put(cell, hostList);
			}		
			gridCellToHosts.clear();
			interfaceToGridCell.clear();
			
			gridCellToHosts.putAll(cellToHost);
			interfaceToGridCell.putAll(ginterfaces);				
		}
		
		/**
		 * 提前计算了各个轨道在一个周期内的网格历遍情况，生成轨道对应的历经网格表，根据此表就可以计算相互之间未来的关系，而无需再计算轨道
		 */
		public void updateNetGridInfo_without_OrbitCalculation(double simClock){
			if (gridLocation.isEmpty())//初始化只执行一次
				initializeGridLocation();
			
			HashMap<NetworkInterface, GridCell> ginterfaces = new HashMap<NetworkInterface, GridCell>();//每次清空;
			//ginterfaces.clear();//每次清空
			//Coord location = new Coord(0,0); 	// where is the host
			//double simClock = SimClock.getTime();
			//System.out.println("update time:  "+ simClock);
				
			//int[] coordOfNetgrid;
			
			HashMap<GridCell, List<DTNHost>> cellToHost = new HashMap<GridCell, List<DTNHost>>();
			for (DTNHost host : hosts){
				/**记录各个节点所经过的网格**/
				List<GridCell> gridCellList = this.gridLocation.get(host);
				/**记录各个节点所经过网格时对应的进入时间**/
				List<Double> timeList = this.gridTime.get(host);

				if (gridCellList.size() != timeList.size()){
					throw new SimError("轨道预测得到的数据有问题！");	
				}
				/**卫星轨道周期**/
				double period = this.periodMap.get(host);
				double t0 = simClock;
				GridCell cell = null;
				boolean label = false;
					
				if (simClock > period)
					t0 = t0 % period;//大于周期就取余操作
				
				if (timeList.get(0) > timeList.get(timeList.size() - 1)){
					for (int iterator = 1; iterator < timeList.size(); iterator++){
						if (timeList.get(iterator) > t0){
							/**注意，这里iterator - 1是没有错的，因为对于iterator个来说，是下一个网格进入的时间，如果if条件满足，那么此时刻节点应该处在前一个网格位置当中**/
							int[] coordOfNetgrid = gridCellList.get(iterator - 1).getNumber();
							cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
							//cell = gridCellList.get(iterator - 1);
							label = true;
							break;
						}
					}
					/**判断是不是处于轨道周期的末尾时刻，边界位置**/
					if (t0 >= timeList.get(0) & cell == null){
						int[] coordOfNetgrid = gridCellList.get(0).getNumber();
						cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
						label = true;
					}
					
					if (t0 >= timeList.get(timeList.size() - 1) & t0 < timeList.get(0) & cell == null){
						int[] coordOfNetgrid = gridCellList.get(timeList.size() - 1).getNumber();
						cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
						label = true;
					}	
				}
				else{
					for (int iterator = 1; iterator < timeList.size(); iterator++){
						if (timeList.get(iterator) > t0){
							/**注意，这里iterator - 1是没有错的，因为对于iterator个来说，是下一个网格进入的时间，如果if条件满足，那么此时刻节点应该处在前一个网格位置当中**/
							int[] coordOfNetgrid = gridCellList.get(iterator - 1).getNumber();
							cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
							//cell = gridCellList.get(iterator - 1);
							label = true;
							break;
						}
					}
					/**判断是不是处于轨道周期的末尾时刻，边界位置**/
					if (t0 >= timeList.get(timeList.size() - 1) & cell == null){
						int[] coordOfNetgrid = gridCellList.get(timeList.size() - 1).getNumber();
						cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
						//cell = gridCellList.get(0);
						label = true;
					}	
				}
			
//				for (double t : timeList){
//					if (t >= t0){
//						cell = gridCellList.get(iterator);
//						label = true;
//						break;
//					}
//					iterator++;//找到与timeList时间对应的网格所在位置,iterator 代表在这两个list中的指针						
//				}				
				//System.out.println(host+"  "+cell+" time "+SimClock.getTime());

				if (label != true){
					/**如果前面没有找到，就说明此时节点是在一个轨道周期内的，处于最后一个网格和第一个网格的交界处,应该取最后一个网格**/
//					int[] coordOfNetgrid = gridCellList.get(timeList.size() - 1).getNumber();
//					cell = this.cells[coordOfNetgrid[0]][coordOfNetgrid[1]][coordOfNetgrid[2]];
					System.out.println(simClock+"  "+host);
					throw new SimError("grid calculation error");
				}
				
//				/**验证用**/
//				int[] coordOfNetgrid = cell.getNumber();
//				int[] TRUEcoordOfNetgrid = this.getGridCellFromCoordAtTime(host, simClock).getNumber();
//				if (!(TRUEcoordOfNetgrid[0] == coordOfNetgrid[0] & TRUEcoordOfNetgrid[0] == coordOfNetgrid[0] & TRUEcoordOfNetgrid[0] == coordOfNetgrid[0])){
//					System.out.println(simClock+"  "+host+" coordofnetgrid "+TRUEcoordOfNetgrid[0]+" "+ TRUEcoordOfNetgrid[1]+" "+TRUEcoordOfNetgrid[2]+"  "+coordOfNetgrid[0]+" "+coordOfNetgrid[1]+" "+coordOfNetgrid[2]);
//					//cell = this.getGridCellFromCoord(host, simClock);
//					//throw new SimError("grid calculation error");	
//				}					
//				/**验证用**/

				ginterfaces.put(host.getInterface(1), cell);
				
				List<DTNHost> hostList = new ArrayList<DTNHost>();
				if (cellToHost.containsKey(cell)){
					hostList = cellToHost.get(cell);	
				}
				hostList.add(host);
				cellToHost.put(cell, hostList);
			}		
			gridCellToHosts.clear();
			interfaceToGridCell.clear();
			
			gridCellToHosts.putAll(cellToHost);
			interfaceToGridCell.putAll(ginterfaces);
			//ginterfaces = new HashMap<NetworkInterface, GridCell>();//每次清空
			
			
//			cellmap.put(simClock, cellToHost);
//			gridmap.put(simClock, ginterfaces);//预测未来time时间里节点和网格之间的对应关系
			//ginterfaces.clear();//每次清空
			
			//CreateGrid(cellSize);//包含cells的new和ginterfaces的new
				
		}		
	
		/**
		 * 根据坐标c找到c所属的网格
		 * @param c
		 * @return
		 */
		private GridCell cellFromCoord(Coord c) {
			// +1 due empty cells on both sides of the matrix
			int row = (int)Math.ceil(c.getY()/cellSize); 
			int col = (int)Math.ceil(c.getX()/cellSize);
			int z = (int)Math.ceil(c.getZ()/cellSize);
			if (!(row > 0 && row <= rows && col > 0 && col <= cols))
				throw new SimError("Location " + c + " is out of world's bounds");
			//assert row > 0 && row <= rows && col > 0 && col <= cols : "Location " + 
			//c + " is out of world's bounds";
		
			return this.cells[row][col][z];
		}
		
		public void setHostsList(List<DTNHost> hosts){
			this.hosts = hosts;
		}
		
		/**
		 * 新建内部类，用于实现网格划分，存储各个网格的离散坐标
		 */
		public class GridCell {
			// how large array is initially chosen
			private static final int EXPECTED_INTERFACE_COUNT = 18;
			//private ArrayList<NetworkInterface> interfaces;//GridCell就是依靠维护网络接口列表，来记录在此网格内的节点，对于全局网格来说，需要保证同一个网络接口不会同时出现在两个GridCell中
			private int[] number;
			
			private GridCell() {
			//	this.interfaces = new ArrayList<NetworkInterface>(
			//			EXPECTED_INTERFACE_COUNT);
				number = new int[3];
			}
			
			public void setNumber(int row, int col, int z){
				number[0] = row;
				number[1] = col;
				number[2] = z;
			}
			public int[] getNumber(){
				return number;
			}
			
			public String toString() {
				return getClass().getSimpleName() + " with " + 
					"cell number: "+ number[0]+" "+number[1]+" "+number[2];
			}
		}
	}
}
