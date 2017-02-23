/* 
 * Copyright 2016 University of Science and Technology of China , Infonet
 * Written by LiJian.
 */
package routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import movement.MovementModel;
import movement.SatelliteMovement;
import util.Tuple;
import core.Connection;
import core.Coord;
import core.DTNHost;
import core.Message;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;
import core.SimError;

public class TwoLayerRouterBasedonGridRouter extends ActiveRouter{
	/**自己定义的变量和映射等
	 * 
	 */
	public static final String MSG_WAITLABEL = "waitLabel";
	public static final String MSG_PATHLABEL = "msgPathLabel"; 
	public static final String MSG_ROUTERPATH = "routerPath";  //定义字段名称，假设为MSG_MY_PROPERTY
	/** Group name in the group -setting id ({@value})*/
	public static final String GROUPNAME_S = "Group";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";

	private static final double SPEEDOFLIGHT = 299792458;//光速，近似3*10^8m/s
	private static final double MESSAGESIZE = 1024000;//1MB
	private static final double  HELLOINTERVAL = 30;//hello包发送间隔
	
	int[] predictionLabel = new int[2000];
	double[] transmitDelay = new double[2000];//1000代表总的节点数
	//double[] liveTime = new double[2000];//链路的生存时间，初始化时自动赋值为0
	double[] endTime = new double[2000];//链路的生存时间，初始化时自动赋值为0
	
	private boolean msgPathLabel;//此标识指示是否在信息头部中标识路由路径
	private double	transmitRange;//设置的可通行距离阈值
	private List<DTNHost> hosts;//全局节点列表
	
	HashMap<DTNHost, Double> arrivalTime = new HashMap<DTNHost, Double>();
	private HashMap<DTNHost, List<Tuple<Integer, Boolean>>> routerTable = new HashMap<DTNHost, List<Tuple<Integer, Boolean>>>();//节点的路由表
	private HashMap<String, Double> busyLabel = new HashMap<String, Double>();//指示下一跳节点处于忙的状态，需要等待
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> neighborsList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();//新增全局其它节点邻居链路生存时间信息
	protected HashMap<DTNHost, HashMap<DTNHost, double[]>> predictList = new HashMap<DTNHost, HashMap<DTNHost, double[]>>();
	
	private boolean routerTableUpdateLabel;
	private GridNeighbors GN;
	
	DTNHost MEOinthisTime;
	Random random = new Random();
	/**
	 * 初始化
	 * @param s
	 */
	public TwoLayerRouterBasedonGridRouter(Settings s){
		super(s);
	}
	/**
	 * 初始化
	 * @param r
	 */
	protected TwoLayerRouterBasedonGridRouter(TwoLayerRouterBasedonGridRouter r) {
		super(r);
		this.GN = new GridNeighbors(this.getHost());//不放在这的原因是，当执行这一步初始化的时候，host和router还没有完成绑定操作
	}
	/**
	 * 复制此router类
	 */
	@Override
	public MessageRouter replicate() {
		//this.GN = new GridNeighbors(this.getHost());
		return new TwoLayerRouterBasedonGridRouter(this);
	}
	/**
	 * 执行路由的初始化操作
	 */
	public void initialzation(){
		GN.setHost(this.getHost());//为了实现GN和Router以及Host之间的绑定，待修改！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
		this.GN.initializeGridLocation();
	}	
	/**
	 * 路由更新，每次调用路由更新时的主入口
	 */
	@Override
	public void update() {
		super.update();
		
		/*测试代码，保证neighbors和connections的一致性*/
		List<DTNHost> conNeighbors = new ArrayList<DTNHost>();
		for (Connection con : this.getConnections()){
			conNeighbors.add(con.getOtherNode(this.getHost()));
		}
		/*for (DTNHost host : this.getHost().getNeighbors().getNeighbors()){
			assert conNeighbors.contains(host) : "connections is not the same as neighbors";
		}
		*/
		//this.getHost().getNeighbors().changeNeighbors(conNeighbors);
		//this.getHost().getNeighbors().updateNeighbors(this.getHost(), this.getConnections());//更新邻居节点数据库
		/*测试代码，保证neighbors和connections的一致性*/
		
		this.hosts = this.getHost().getNeighbors().getHosts();
		List<Connection> connections = this.getConnections();  //取得所有邻居节点
		List<Message> messages = new ArrayList<Message>(this.getMessageCollection());
		
		Settings s = new Settings(GROUPNAME_S);
		this.msgPathLabel = s.getBoolean(MSG_PATHLABEL);//从配置文件中读取传输速率
		
		if (isTransferring()) {//判断链路是否被占用
			return; // can't start a new transfer
		}
		if (connections.size() > 0){//有邻居时需要进行hello包发送协议
			//helloProtocol();//执行hello包的维护工作
		}
		if (!canStartTransfer())//是否有林杰节点且有信息需要传送
			return;
		
		//如果全局链路状态有所改变，就需要重新计算所有路由
		/*boolean linkStateChange = false;
		if (linkStateChange == true){
			this.busyLabel.clear();
			this.routerTable.clear();
		}*/
		
		routerTableUpdateLabel = false;
		if (messages.isEmpty())
			return;
		for (Message msg : messages){//尝试发送队列里的消息	
			if (checkBusyLabelForNextHop(msg))
				continue;
			if (findPathToSend(msg, connections, this.msgPathLabel) == true)
				return;
		}
	}
	/**
	 * 检查此待传消息msg是否需要等待，等待原因可能是1.目的节点正在被占用；2.路由得到的路径是预测路径，下一跳节点需要等待一段时间才能到达
	 * @param msg
	 * @return 是否需要等待
	 */
	public boolean checkBusyLabelForNextHop(Message msg){
		if (this.busyLabel.containsKey(msg.getId())){
			System.out.println(this.getHost()+"  "+SimClock.getTime()+
					"  "+msg+"  is busy until  " + this.busyLabel.get(msg.getId()));
			if (this.busyLabel.get(msg.getId()) < SimClock.getTime()){
				this.busyLabel.remove(msg.getId());
				return false;
			}else
				return true;
		}
		return false;
	}
	/**
	 * 更新路由表，寻找路径并尝试转发消息
	 * @param msg
	 * @param connections
	 * @param msgPathLabel
	 * @return
	 */
	public boolean findPathToSend(Message msg, List<Connection> connections, boolean msgPathLabel){
		if (msgPathLabel == true){//如果允许在消息中写入路径消息
			if (msg.getProperty(MSG_ROUTERPATH) == null){//通过包头是否已写入路径信息来判断是否需要单独计算路由(同时也包含了预测的可能)
				Tuple<Message, Connection> t = 
						findPathFromRouterTabel(msg, connections, msgPathLabel);
				return sendMsg(t);
			}
			else{//如果是中继节点，就检查消息所带的路径信息
				Tuple<Message, Connection> t = 
						findPathFromMessage(msg);
				assert t != null: "读取路径信息失败！";
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
		assert msg.getTo().getAddress() != thisAddress : "本节点已是目的节点，接收处理过程错误";
		int nextHopAddress = -1;
		
		//System.out.println(this.getHost()+"  "+msg+" "+routerPath);
		boolean waitLable = false;
		for (int i = 0; i < routerPath.size(); i++){
			if (routerPath.get(i).getKey() == thisAddress){
				nextHopAddress = routerPath.get(i+1).getKey();//找到下一跳节点地址
				waitLable = routerPath.get(i+1).getValue();//找到下一跳是否需要等待的标志位
				break;//跳出循环
			}
		}
				
		if (nextHopAddress > -1){
			Connection nextCon = findConnection(nextHopAddress);
			if (nextCon == null){//能找到路径信息，但是却没能找到连接
				if (!waitLable){//检查是不是有预测邻居链路
					System.out.println(this.getHost()+"  "+msg+" 指定路径失效");
					msg.removeProperty(this.MSG_ROUTERPATH);//清除原先路径信息!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
					Tuple<Message, Connection> t = 
							findPathFromRouterTabel(msg, this.getConnections(), true);//清除原先路径信息之后再重新寻路
					return t;
				}
			}else{
				Tuple<Message, Connection> t = new 
						Tuple<Message, Connection>(msg, nextCon);
				return t;
			}
		}
		return null;	
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
		
		List<Tuple<Integer, Boolean>> routerPath;
		if (this.getHost().getMEOList().contains(this.getHost()))//如果是MEO节点，则直接找到目的节点的路径
			routerPath = this.routerTable.get(message.getTo());
		else{
			if (this.getHost().getHostsinthisCluster().contains(message.getTo())){//对于簇内的节点，直接找寻最短路径
				routerPath = this.routerTable.get(message.getTo());
			}		
			else{
				routerPath = this.routerTable.get(this.MEOinthisTime);//对非簇内的节点，找寻可达的管理节点MEO，由MEO进行转发
			}
		}

		
		if (msgPathLabel == true){//如果写入路径信息标志位真，就写入路径消息
			message.updateProperty(MSG_ROUTERPATH, routerPath);
		}
					
		Connection path = findConnection(routerPath.get(0).getKey());//取第一跳的节点地址
		if (path != null){
			Tuple<Message, Connection> t = new Tuple<Message, Connection>(message, path);//找到与第一跳节点的连接
			return t;
		}
		else{			
			if (routerPath.get(0).getValue()){
				System.out.println("第一跳预测");
				return null;
				//DTNHost nextHop = this.getHostFromAddress(routerPath.get(0).getKey()); 
				//this.busyLabel.put(message.getId(), startTime);//设置一个等待
			}
			else{
				System.out.println(message+"  "+message.getProperty(MSG_WAITLABEL));
				System.out.println(this.getHost()+"  "+this.getHost().getAddress()+"  "+this.getHost().getConnections());
				System.out.println(routerPath);
				System.out.println(this.routerTable);
				System.out.println(this.getHost().getNeighbors().getNeighbors());
				System.out.println(this.getHost().getNeighbors().getNeighborsLiveTime());
				throw new SimError("No such connection: "+ routerPath.get(0) + 
						" at routerTable " + this);		
			//this.routerTable.remove(message.getTo());	
			}
		}
	}
	/**
	 * 由节点地址找到对应的节点DTNHost
	 * @param address
	 * @return
	 */
	public DTNHost findHostByAddress(int address){
		for (DTNHost host : this.hosts){
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
		if (this.getHost().getMEOList().contains(this.getHost()))//如果这是一个MEO节点
			gridSearch(msg, msg.getTo());
		else{
			if (this.getHost().getHostsinthisCluster().contains(msg.getTo()))
				gridSearch(msg, msg.getTo());
			else
				gridSearch(msg, null);
		}

		/*System.out.println(this.getHost() + "  " +SimClock.getTime()+ "  " + this.routerTable);
		for (int i = 0; i < 10; i++){
			System.out.print(this.transmitDelay[i]+"  ");
		}
		System.out.println("");
		
		for (int i = 0; i < 10; i++){
			System.out.print(this.endTime[i]+"  ");
		}
		System.out.println("");*/
		
		//updatePredictionRouter(msg);//需要进行预测
		if (this.routerTable.containsKey(msg.getTo())){//预测也找不到到达目的节点的路径，则路由失败
			//m.changeRouterPath(this.routerTable.get(m.getTo()));//把计算出来的路径直接写入信息当中
			System.out.println("寻路成功！！！");
			return true;//找到了路径
		}else{
			//System.out.println("寻路失败！！！");
			return false;
		}
		
		//if (!this.getHost().getNeighbors().getNeighbors().isEmpty())//如果本节点不处于孤立状态，则进行邻居节点的路由更新
	}
	/**
	 * 核心路由算法，运用贪心选择性质进行遍历，找出到达目的节点最短路径；同时根据分簇规定，对于不在同一个簇内的目的节点，转而找寻通往MEO节点的路径，让MEO节点代为转发
	 * @param msg
	 * @param destinationHost
	 */
	public void gridSearch(Message msg, DTNHost destinationHost){
		if (routerTableUpdateLabel == true)//routerTableUpdateLabel == true则代表此次更新路由表已经更新过了，所以不要重复计算
			return;
		this.routerTable.clear();
		
		if (GN.isHostsListEmpty()){
			GN.setHostsList(hosts);
		}
		//GridNeighbors GN = this.getHost().getGridNeighbors();
		Settings s = new Settings(GROUPNAME_S);
		String option = s.getSetting("Pre_or_onlineOrbitCalculation");//从配置文件中读取设置，是采用在运行过程中不断计算轨道坐标的方式，还是通过提前利用网格表存储各个节点的轨道信息
		
		HashMap<String, Integer> orbitCalculationWay = new HashMap<String, Integer>();
		orbitCalculationWay.put("preOrbitCalculation", 1);
		orbitCalculationWay.put("onlineOrbitCalculation", 2);
		
		switch (orbitCalculationWay.get(option)){
		case 1://通过提前利用网格表存储各个节点的轨道信息，从而运行过程中不再调用轨道计算函数来预测而是通过读表来预测
			GN.updateGrid_without_OrbitCalculation();//更新网格表
			break;
		case 2:
			GN.updateGrid_with_OrbitCalculation();//更新网格表
			break;
		}
		
		List<DTNHost> sourceSet = new ArrayList<DTNHost>();
		List<DTNHost> availableHosts = new ArrayList<DTNHost>();//创建一个MEO或者是LEO可达的节点集合
		/**1.如果这是一个MEO节点**/
		if (this.getHost().getMEOList().contains(this.getHost())){			
			availableHosts.addAll(this.getHost().getMEOList());
			availableHosts.addAll(this.getHost().getClusterList().get(msg.getTo().getClusterNumber()));//找到目的节点所在的簇的节点列表，并加入可选转发节点列表中
			
			/*添加链路可探测到的一跳邻居，并更新路由表*/
			sourceSet.add(this.getHost());//初始时只有本节点
			for (Connection con : this.getHost().getConnections()){//添加链路可探测到的一跳邻居，并更新路由表
				DTNHost neiHost = con.getOtherNode(this.getHost());
				if (availableHosts.contains(neiHost)){//只添加在可用节点集合availableHosts中的邻居节点
					sourceSet.add(neiHost);//初始时只有本节点和链路邻居		
					Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
					path.add(hop);//注意顺序
					arrivalTime.put(neiHost, time);
					routerTable.put(neiHost, path);
				}
			}
			
			/*添加链路可探测到的一跳邻居，并更新路由表*/
		}
		/**2.如果这是一个LEO节点，则只能找寻在本簇内的直达路径，否则，跨簇则交由MEO节点进行转发**/
		else{	
			availableHosts.addAll(this.getHost().getMEOList());
			availableHosts.addAll(this.getHost().getClusterList().get(this.getHost().getClusterNumber()));//找到本节点所在的簇的节点列表，并加入可选转发节点列表中
			
			/*添加链路可探测到的一跳邻居，并更新路由表*/
			sourceSet.add(this.getHost());//初始时只有本节点
			
			for (Connection con : this.getHost().getConnections()){//添加链路可探测到的一跳邻居，并更新路由表
				DTNHost neiHost = con.getOtherNode(this.getHost());
				if (availableHosts.contains(neiHost)){//只添加在可用节点集合availableHosts中的邻居节点
					sourceSet.add(neiHost);//初始时只有本节点和链路邻居		
					Double time = SimClock.getTime() + msg.getSize()/this.getHost().getInterface(1).getTransmitSpeed();
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), false);
					path.add(hop);//注意顺序
					arrivalTime.put(neiHost, time);
					routerTable.put(neiHost, path);
				}
			}
			/*添加链路可探测到的一跳邻居，并更新路由表*/
			}	
		
		int iteratorTimes = 0;
		int size = availableHosts.size();
		double minTime = 100000;
		DTNHost minHost =null;
		boolean updateLabel = true;
		boolean predictLable = false;
		List<Tuple<Integer, Boolean>> minPath = new ArrayList<Tuple<Integer, Boolean>>();

		
		arrivalTime.put(this.getHost(), SimClock.getTime());//初始化到达时间
		
		while(true){//Dijsktra算法思想，每次历遍全局，找时延最小的加入路由表，保证路由表中永远是时延最小的路径
			if (iteratorTimes >= size || updateLabel == false)
				break; 
			updateLabel = false;
			minTime = 100000;
			HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>> 
						predictionList = new HashMap<DTNHost, Tuple<HashMap<DTNHost, List<Double>>, 
						HashMap<DTNHost, List<Double>>>>();//存储机制，如果之前已经算过就不用再重复计算了
			
			for (DTNHost host : sourceSet){
				Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime;//邻居节点到达时间和离开时间的二元组组合
				HashMap<DTNHost, List<Double>> startTime;
				HashMap<DTNHost, List<Double>> leaveTime;
				
				if (predictionList.containsKey(host)){//存储机制，如果之前已经算过就不用再重复计算了
					predictTime = predictionList.get(host);
				}
				else{
					List<DTNHost> neiList = GN.getNeighbors(host, arrivalTime.get(host));//获取源集合中host节点的邻居节点(包括当前和未来邻居)
					assert neiList==null:"没能成功读取到指定时间的邻居";
					predictTime = GN.getFutureNeighbors(neiList, host, arrivalTime.get(host));
				}
				startTime = predictTime.getKey();
				leaveTime = predictTime.getValue();
				if (startTime.keySet().isEmpty())
					continue;
				for (DTNHost neiHost : startTime.keySet()){//startTime.keySet()包含了所有的邻居节点，包含未来的邻居节点
					
					/**如果不是可选范围内的节点，则跳过**/
					if (!availableHosts.contains(neiHost))
						continue;
					/**如果不是可选范围内的节点，则跳过**/
					
					if (sourceSet.contains(neiHost))
						continue;
					if (arrivalTime.get(host) >= SimClock.getTime() + msgTtl*60)//出发时间就已经超出TTL预测时间的话就直接排除
						continue;
					double waitTime = startTime.get(neiHost).get(0) - arrivalTime.get(host);
					if (waitTime <= 0){
						predictLable = false;
						//System.out.println("waitTime = "+ waitTime);
						waitTime = 0;
					}
					if (waitTime > 0)
						predictLable = true;
					double time = arrivalTime.get(host) + msg.getSize()/host.getInterface(1).getTransmitSpeed() + waitTime;
					List<Tuple<Integer, Boolean>> path = new ArrayList<Tuple<Integer, Boolean>>();
					if (this.routerTable.containsKey(host))
						path.addAll(this.routerTable.get(host));
					Tuple<Integer, Boolean> hop = new Tuple<Integer, Boolean>(neiHost.getAddress(), predictLable);
					path.add(hop);//注意顺序
					/*待修改，应该是leavetime的检查!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
					if (leaveTime.isEmpty()){
						if (time > SimClock.getTime() + msgTtl*60)
							continue;
					}
					else{
						if (time > leaveTime.get(neiHost).get(0))
							continue;
					}
					/*待修改，应该是leavetime的检查!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
					if (time <= minTime){
						if (random.nextBoolean() == true && time - minTime < 1){
							minTime = time;
							minHost = neiHost;
							minPath = path;
							updateLabel = true;	
						}
					}					
				}
			}
			if (updateLabel == true){
				arrivalTime.put(minHost, minTime);
				routerTable.put(minHost, minPath);
			}
			iteratorTimes++;
			sourceSet.add(minHost);//将新的最短节点加入
			if (destinationHost == null)//如果destinationHost为空，代表此信息msg的目的节点跨簇了，则转交给MEO，找MEO的路由
				for (DTNHost MEO : this.getHost().getMEOList()){
					if (routerTable.containsKey(MEO)){//如果中途任何一个可达的MEO，则直接返回
						this.MEOinthisTime = MEO;
						routerTableUpdateLabel = true;
						return;
					}					
				}
			if (routerTable.containsKey(destinationHost))//如果中途找到需要的路徑，就直接退出搜索
				break;
		}
		routerTableUpdateLabel = true;
		
		System.out.println(this.getHost()+" table: "+routerTable+" time : "+SimClock.getTime());	
		
	}
	


	public int transmitFeasible(DTNHost destination){//传输可行性,判断是不是已有到目的节点的路径，同时还要保证此路径的存在时间大于传输所需时间
		if (this.routerTable.containsKey(destination)){
			if (this.transmitDelay[destination.getAddress()] > this.endTime[destination.getAddress()] -SimClock.getTime())
				return 0;
			else
				return 1;//只有此时既找到了通往目的节点的路径，同时路径上的链路存在时间可以满足传输延时
		}
		return 2;
		
	}


	/**
	 * 对信息msg头部进行改写操作，对预测节点的等待标志进行置位
	 * @param fromHost
	 * @param host
	 * @param msg
	 * @param startTime
	 */
	public void addWaitLabelInMessage(DTNHost fromHost, DTNHost host, Message msg, double startTime){
		HashMap<DTNHost, Tuple<DTNHost, Double>> waitList = new HashMap<DTNHost, Tuple<DTNHost, Double>>();
		Tuple<DTNHost, Double> waitLabel = new Tuple<DTNHost, Double>(host, startTime);
		
		if (msg.getProperty(MSG_WAITLABEL) == null){					
			waitList.put(fromHost, waitLabel);//fromHost为需要等待的节点，host为下一跳的预测节点
			msg.addProperty(MSG_WAITLABEL, waitList);
		}else{
			waitList.putAll((HashMap<DTNHost, Tuple<DTNHost, Double>>)msg.getProperty(MSG_WAITLABEL));
			waitList.put(fromHost, waitLabel);
			msg.updateProperty(MSG_WAITLABEL, waitList);
		}
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
		for (DTNHost host : this.hosts){
			if (host.getAddress() == address)
				return host;
		}
		return null;
	}
	/**
	 * 在算路由表时，预测指定路径上的链路存在时间
	 * @param formerLiveTime
	 * @param host
	 * @param path
	 * @return
	 */
	public double calculateExistTime(double formerLiveTime, DTNHost host, List<Integer> path){
		DTNHost formerHost, nextHost;
		double existTime , minTime;

		nextHost = this.getHostFromAddress(path.get(0));
		//System.out.println(host+"  "+host.getNeighbors().getNeighborsLiveTime()+"  "+this.neighborsList.get(host)+"  "+host.getNeighbors().getNeighborsLiveTime().get(nextHost)[1]+"  "+path+" "+nextHost);

		existTime = this.neighborsList.get(host).get(nextHost)[1] - SimClock.getTime();
		minTime = formerLiveTime > existTime ? existTime : formerLiveTime;			
		if (path.size() > 1){//至少长度为2
			for (int i = 1; i < path.size() - 1; i++){
				if (i > path.size() -1)//超过长度，自动返回
					return minTime;
				formerHost = nextHost;
				nextHost = this.getHostFromAddress(path.get(i));
				existTime = this.neighborsList.get(formerHost).get(nextHost)[1] - SimClock.getTime();
				if (existTime < minTime)
					minTime = existTime;
			}
		}				
	
	return minTime;
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
	 * 计算指定链路(两个节点之间)所需的传输时间
	 * @param msgSize
	 * @param nei
	 * @param host
	 * @return
	 */
	public double calculateDelay(int msgSize, DTNHost nei , DTNHost host){
		double transmitDelay = msgSize/((nei.getInterface(1).getTransmitSpeed() > host.getInterface(1).getTransmitSpeed()) ? 
				host.getInterface(1).getTransmitSpeed() : nei.getInterface(1).getTransmitSpeed()) + 
				this.transmitDelay[host.getAddress()] + getDistance(nei, host)*1000/SPEEDOFLIGHT;//取二者较小的传输速率
		return transmitDelay;
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
	 * 根据节点地址找到，与此节点相连的连接
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
		return null;//没有在已有连接中找到通过指定节点的路径
	}
	/**
	 * 发送一个信息到特定的下一跳
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
	 * 用于判断下一跳节点是否处于发送或接受状态
	 * @param t
	 * @return
	 */
	public boolean hostIsBusyOrNot(Tuple<Message, Connection> t){
		List<Connection> connections = t.getValue().getOtherNode(this.getHost()).getConnections();
		for (Connection con : connections){
			if (con.isTransferring()){
				assert this.busyLabel.get(con) == null : "error! ";
				this.busyLabel.put(t.getKey().getId(), con.getRemainingByteCount()/con.getSpeed() + SimClock.getTime());
				System.out.println(this.getHost()+"  "+t.getKey()+"  "+
						t.getValue().getOtherNode(this.getHost())+" "+con+"  "+this.busyLabel.get(t.getKey().getId()));			
				return true;//说明目的节点正忙
			}
		}
		return false;
	}
	/**
	 * 从给定消息和指定链路，尝试发送消息
	 * @param t
	 * @return
	 */
	public boolean sendMsg(Tuple<Message, Connection> t){
		if (t == null){	
			assert false : "error!";//如果确实是需要等待未来的一个节点就等，先传下一个,待修改!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
			return false;
		}
		else{
			if (hostIsBusyOrNot(t) == true)//假设目的节点处于忙的状态
				return false;//发送失败，需要等待
			if (tryMessageToConnection(t) != null)//列表第一个元素从0指针开始！！！	
				return true;//只要成功传一次，就跳出循环
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
		removeFromMessages(msgId);
	}
	
	public class GridNeighbors {
		
		private List<DTNHost> hosts = new ArrayList<DTNHost>();//全局卫星节点列表
		private DTNHost host;
		private double transmitRange;
		private double msgTtl;
		
		private double updateInterval = 1;
		
		private GridCell[][][] cells;//GridCell这个类，创建一个实例代表一个单独的网格，整个world创建了一个三维数组存储这个网格，每个网格内又存储了当前在其中的host的networkinterface
		private HashMap<NetworkInterface, GridCell> ginterfaces;
		private int cellSize;
		private int rows;
		private int cols;
		private int zs;//新增三维变量
		private  int worldSizeX;
		private  int worldSizeY;
		private  int worldSizeZ;//新增
		
		private int gridLayer;
		
		private List<GridCell[][][]> GridList = new ArrayList<GridCell[][][]>();
		private HashMap<Double, HashMap<NetworkInterface, GridCell>> gridmap = new HashMap<Double, HashMap<NetworkInterface, GridCell>>();
		private HashMap<Double, HashMap<GridCell, List<DTNHost>>> cellmap = new HashMap<Double, HashMap<GridCell, List<DTNHost>>>();
		
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
			int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE,2);//参数从2维修改为3维
			worldSizeX = worldSize[0];
			worldSizeY = worldSize[1];
			worldSizeZ = worldSize[1];//新增三维变量，待检查！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！！
			
			Settings layer = new Settings("Group");
			this.gridLayer = layer.getInt("layer");
			
			switch(this.gridLayer){
			case 1 : 
				cellSize = (int) (transmitRange*0.288);//Layer=2
				break;
			case 2 : 
				cellSize = (int) (transmitRange*0.14433);//Layer=3
			//cellSize = (int) (transmitRange*0.0721687);//Layer=4
				break;
			default :
				cellSize = (int) (transmitRange*0.288);//Layer=2
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
		
		public void CreateGrid(int cellSize){
			this.rows = worldSizeY/cellSize + 1;
			this.cols = worldSizeX/cellSize + 1;
			this.zs = worldSizeZ/cellSize + 1;//新增
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
			ginterfaces = new HashMap<NetworkInterface,GridCell>();
		}
		/**
		 * 遍歷所有節點，對每個節點遍歷一個週期，記錄其一個週期內遍歷過的網格，并找到對應的進入和離開時間
		 */
		public void initializeGridLocation(){	
			this.host.getHostsList();
			for (DTNHost h : this.host.getHostsList()){//對每個節點遍歷一個週期，記錄其一個週期內遍歷過的網格，并找到對應的進入和離開時間
				double period = getPeriodofOrbit(h);
				this.periodMap.put(h, period);
				System.out.println(this.host+" now calculate "+h+"  "+period);
				
				List<GridCell> gridList = new ArrayList<GridCell>();
				List<Double> intoTime = new ArrayList<Double>();
				List<Double> outTime = new ArrayList<Double>();
				GridCell startCell;//记录起始网格
				for (double time = 0; time < period; time += updateInterval){
					Coord c = h.getCoordinate(time);
					GridCell gc = cellFromCoord(c);//根據坐標找到對應的網格
					if (!gridList.contains(gc)){
						if (gridList.isEmpty())
							startCell = gc;//记录起始网格
						gridList.add(gc);//第一次检测到节点进入此网格（注意，边界检查！！！开始和结束的时候！！！!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!）
						intoTime.add(time);//记录相应的进入时间
					}	
					else{
						//if (gc. == startCell)
							//intoTime = time;
					}
				}
				gridLocation.put(h, gridList);//遍历完一个节点就记录下来
				gridTime.put(h, intoTime);
			}
			System.out.println(gridLocation);
		}
		/**
		 * 獲取指定衛星節點的運行週期時間
		 * @param h
		 * @return
		 */
		public double getPeriodofOrbit(DTNHost h){
			return h.getPeriod();
		}
			
		public List<DTNHost> getNeighbors(DTNHost host, double time){//获取指定时间的邻居节点(同时包含预测到TTL时间内的邻居)
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;
			
			if (time > SimClock.getTime()+msgTtl*60){//检查输入的时间是否超过预测时间
				//assert false :"超出预测时间";
				time = SimClock.getTime()+msgTtl*60;
			}
				
			HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);
			GridCell cell = ginterfaces.get(host.getInterface(1));
			int[] number = cell.getNumber();
			
			List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//所有邻居的网格（当前时刻）
			List<DTNHost> hostList = new ArrayList<DTNHost>();//(邻居网格内的节点集合)
			assert cellmap.containsKey(time):" 时间错误 ";
			for (GridCell c : cellList){
				if (cellmap.get(time).containsKey(c))//如果不包含，这说明此邻居网格为空，里面不含任何节点
					hostList.addAll(cellmap.get(time).get(c));
			}	
			if (hostList.contains(host))//把自身节点去掉
				hostList.remove(host);
			//System.out.println(host+" 邻居列表   "+hostList);
			return hostList;
		}

		public Tuple<HashMap<DTNHost, List<Double>>, //neiList 为已经计算出的当前邻居节点列表
			HashMap<DTNHost, List<Double>>> getFutureNeighbors(List<DTNHost> neiList, DTNHost host, double time){
			int num = (int)((time-SimClock.getTime())/updateInterval);
			time = SimClock.getTime()+num*updateInterval;	
			
			HashMap<DTNHost, List<Double>> leaveTime = new HashMap<DTNHost, List<Double>>();
			HashMap<DTNHost, List<Double>> startTime = new HashMap<DTNHost, List<Double>>();
			for (DTNHost neiHost : neiList){
				List<Double> t= new ArrayList<Double>();
				t.add(SimClock.getTime());
				startTime.put(neiHost, t);//添加已存在邻居节点的开始时间
			}
			
			List<DTNHost> futureList = new ArrayList<DTNHost>();//(邻居网格内的未来节点集合)
			List<NetworkInterface> futureNeiList = new ArrayList<NetworkInterface>();//(预测未来邻居的节点集合)
			
			
			Collection<DTNHost> temporalNeighborsBefore = startTime.keySet();//前一时刻的邻居，通过交叉对比这一时刻的邻居，就知道哪些是新加入的，哪些是新离开的			
			Collection<DTNHost> temporalNeighborsNow = new ArrayList<DTNHost>();//用于记录当前时刻的邻居
			for (; time < SimClock.getTime() + msgTtl*60; time += updateInterval){
				
				HashMap<NetworkInterface, GridCell> ginterfaces = gridmap.get(time);//取出time时刻的网格表
				GridCell cell = ginterfaces.get(host.getInterface(1));//找到此时指定节点所处的网格位置
				
				int[] number = cell.getNumber();
				List<GridCell> cellList = getNeighborCells(time, number[0], number[1], number[2]);//获取所有邻居的网格（当前时刻）
				
				for (GridCell c : cellList){	//遍历在不同时间维度上，指定节点周围网格的邻居
					if (!cellmap.get(time).containsKey(c))
						continue;
					temporalNeighborsNow.addAll(cellmap.get(time).get(c));
					for (DTNHost ni : cellmap.get(time).get(c)){//检查当前预测时间点，所有的邻居节点
						if (ni == this.host)//排除自身节点
							continue;
						if (!neiList.contains(ni))//如果现有邻居中没有，则一定是未来将到达的邻居					
							futureList.add(ni); //此为未来将会到达的邻居(当然对于当前已有的邻居，也可能会中途离开，然后再回来)
										
						/**如果是未来到达的邻居，直接get会返回空指针，所以要先加startTime和leaveTime判断**/
						if (startTime.containsKey(ni)){
							if (leaveTime.isEmpty())
								break;
							if (startTime.get(ni).size() == leaveTime.get(ni).size()){//如果不相等则一定是邻居节点离开的情况					
								List<Double> mutipleTime= leaveTime.get(ni);
								mutipleTime.add(time);
								startTime.put(ni, mutipleTime);//将此新的开始时间加入
							}
							/*if (leaveTime.containsKey(ni)){//有两种情况，一种在预测时间段内此邻居会离开，另一种情况是此邻居不仅在此时间段内会离开还会回来
								if (startTime.get(ni).size() == leaveTime.get(ni).size()){//如果不相等则一定是邻居节点离开的情况					
									List<Double> mutipleTime= leaveTime.get(ni);
									mutipleTime.add(time);
									startTime.put(ni, mutipleTime);//将此新的开始时间加入
								}
								else{
									List<Double> mutipleTime= leaveTime.get(ni);
									mutipleTime.add(time);
									leaveTime.put(ni, mutipleTime);//将此新的离开时间加入
								}	
							}
							else{
								List<Double> mutipleTime= new ArrayList<Double>();
								mutipleTime.add(time);
								leaveTime.put(ni, mutipleTime);//将此新的离开时间加入
							}*/
						}
						else{
							//System.out.println(this.host+" 出现预测节点: "+ni+" 时间  "+time);
							List<Double> mutipleTime= new ArrayList<Double>();
							mutipleTime.add(time);
							startTime.put(ni, mutipleTime);//将此新的开始时间加入
						}
						/**如果是未来到达的邻居，直接get会返回空指针，所以要先加startTime和leaveTime判断**/
					}	
				}
				
				for (DTNHost h : temporalNeighborsBefore){//交叉对比这一时刻和上一时刻的邻居节点，从而找出离开的邻居节点
					if (!temporalNeighborsNow.contains(h)){
						List<Double> mutipleTime= leaveTime.get(h);
						mutipleTime.add(time);
						leaveTime.put(h, mutipleTime);//将此新的离开时间加入
					}						
				}
				temporalNeighborsBefore.clear();
				temporalNeighborsBefore = temporalNeighborsNow;
				temporalNeighborsNow.clear();	
			}
			
			Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime= //二元组合并开始和结束时间
					new Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>(startTime, leaveTime); 
			
			
			return predictTime;
		}
		
		public List<GridCell> getNeighborCells(double time, int row, int col, int z){
			HashMap<GridCell, List<DTNHost>> cellToHost = cellmap.get(time);//获取time时刻的全局网格表
			List<GridCell> GC = new ArrayList<GridCell>();
			/***********************************************************************/
			switch(this.gridLayer){
			case 1 : 
			/*两层网格分割*/
				for (int i = -1; i < 2; i += 1){
					for (int j = -1; j < 2; j += 1){
						for (int k = -1; k < 2; k += 1){
							GC.add(cells[row+i][col+j][z+k]);
						}
					}
				}
				break;
			case 2 : {
			/*三层网格分割*/
				for (int i = -3; i <= 3; i += 1){
					for (int j = -3; j <= 3; j += 1){
						for (int k = -3; k <= 3; k += 1){
							if (boundaryCheck(row+i,col+j,z+k))
								GC.add(cells[row+i][col+j][z+k]);
	
						}
					}
				}
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
						if (boundaryCheck(row+4,col+j,z+k)){
							GC.add(cells[row+4][col+j][z+k]);
	
						}
					}
				}
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
						if (boundaryCheck(row-4,col+j,z+k))
							GC.add(cells[row-4][col+j][z+k]);
	
					}
				}
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
						if (boundaryCheck(row+j,col+4,z+k))
							GC.add(cells[row+j][col+4][z+k]);
	
					}
				}
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
						if (boundaryCheck(row+j,col-4,z+k))
							GC.add(cells[row+j][col-4][z+k]);
	
					}
				}
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
						if (boundaryCheck(row+j,col+k,z+4))
							GC.add(cells[row+j][col+k][z+4]);
	
					}
				}
				for (int j = -1; j < 2; j += 1){
					for (int k = -1; k < 2; k += 1){
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
			}
			/*四层层网格分割*/
			/*	for (int i = -7; i <= 7; i += 1){
			for (int j = -7; j <= 7; j += 1){
				for (int k = -7; k <= 7; k += 1){
					if (boundaryCheck(row+i,col+j,z+k))
						GC.add(cells[row+i][col+j][z+k]);

				}
			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+8,col+j,z+k)){
					GC.add(cells[row+8][col+j][z+k]);

				}
			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row-8,col+j,z+k))
					GC.add(cells[row-8][col+j][z+k]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col+8,z+k))
					GC.add(cells[row+j][col+8][z+k]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col-8,z+k))
					GC.add(cells[row+j][col-8][z+k]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col+k,z+8))
					GC.add(cells[row+j][col+k][z+8]);

			}
		}
		for (int j = -2; j < 3; j += 1){
			for (int k = -2; k < 3; k += 1){
				if (boundaryCheck(row+j,col+k,z-8))
					GC.add(cells[row+j][col+k][z-8]);

			}
		}
		//
		
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+9,col+j,z+k)){
					GC.add(cells[row+9][col+j][z+k]);

				}
			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row-9,col+j,z+k))
					GC.add(cells[row-9][col+j][z+k]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col+9,z+k))
					GC.add(cells[row+j][col+9][z+k]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col-9,z+k))
					GC.add(cells[row+j][col-9][z+k]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col+k,z+9))
					GC.add(cells[row+j][col+k][z+9]);

			}
		}
		for (int j = -1; j < 2; j += 1){
			for (int k = -1; k < 2; k += 1){
				if (boundaryCheck(row+j,col+k,z-9))
					GC.add(cells[row+j][col+k][z-9]);

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
		 * 提前计算了各个轨道在一个周期内的网格历遍情况，生成轨道对应的历经网格表，根据此表就可以计算相互之间未来的关系，而无需再计算轨道
		 */
		public void updateGrid_without_OrbitCalculation(){
			if (gridLocation.isEmpty())//初始化只执行一次
				initializeGridLocation();
			
			ginterfaces.clear();//每次清空
			//Coord location = new Coord(0,0); 	// where is the host
			double simClock = SimClock.getTime();
			
			for (double time = simClock; time <= simClock + msgTtl*60; time += updateInterval){
				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
				for (DTNHost host : hosts){
					List<GridCell> gridCellList = this.gridLocation.get(host);
					List<Double> timeList = this.gridTime.get(host);
					double period = this.periodMap.get(host);
					double t0 = time;
					GridCell cell = new GridCell();
					boolean label = false;
					int iterator = 0;
					if (time >= period)
						t0 = t0 % period;//大于周期就取余操作
					for (double t : timeList){
						if (t >= t0){
							cell = gridCellList.get(iterator);
							label = true;
							break;
						}
						iterator++;//找到与timeList时间对应的网格所在位置,iterator 代表在这两个list中的指针						
					}				
					//System.out.println(host+" number "+cell.getNumber()[0]+cell.getNumber()[1]+cell.getNumber()[2]);
					//System.out.println(host+" error!!! "+label);
					assert label : "grid calculation error";
					
					this.ginterfaces.put(host.getInterface(1), cell);
					
					List<DTNHost> hostList = new ArrayList<DTNHost>();
					if (cellToHost.containsKey(cell)){
						hostList = cellToHost.get(cell);	
					}
					hostList.add(host);
					cellToHost.put(cell, hostList);
				}		
				cellmap.put(time, cellToHost);
				gridmap.put(time, ginterfaces);//预测未来time时间里节点和网格之间的对应关系
				//ginterfaces.clear();//每次清空
				ginterfaces = new HashMap<NetworkInterface, GridCell>();//每次清空
				//CreateGrid(cellSize);//包含cells的new和ginterfaces的new
			}
		}
		
		/**
		 * GridRouter的更新过程函数
		 */
		public void updateGrid_with_OrbitCalculation(){			
			ginterfaces.clear();//每次清空
			Coord location = new Coord(0,0); 	// where is the host
			double simClock = SimClock.getTime();

			for (double time = simClock; time <= simClock + msgTtl*60; time += updateInterval){
				HashMap<GridCell, List<DTNHost>> cellToHost= new HashMap<GridCell, List<DTNHost>>();
				for (DTNHost host : hosts){
					//location.setLocation3D(((SatelliteMovement)host.getMovementModel()).getSatelliteCoordinate(time));//用satellitemovement替换location。my_test，待测试
					location.my_Test(time, 0, host.getParameters());
					
					GridCell cell = updateLocation(time, location, host);//更新在指定时间节点和网格的归属关系
					List<DTNHost> hostList = new ArrayList<DTNHost>();
					if (cellToHost.containsKey(cell)){
						hostList = cellToHost.get(cell);	
					}
					hostList.add(host);
					cellToHost.put(cell, hostList);
				}		
				cellmap.put(time, cellToHost);
				gridmap.put(time, ginterfaces);//预测未来time时间里节点和网格之间的对应关系
				//ginterfaces.clear();//每次清空
				ginterfaces = new HashMap<NetworkInterface, GridCell>();//每次清空
				//CreateGrid(cellSize);//包含cells的new和ginterfaces的new
			}
			
		}
		
		
		public GridCell updateLocation(double time, Coord location, DTNHost host){
			GridCell cell = cellFromCoord(location);
			//cell.addInterface(host.getInterface(1));
			ginterfaces.put(host.getInterface(1), cell);
			return cell;
		}
		
		

		private GridCell cellFromCoord(Coord c) {
			// +1 due empty cells on both sides of the matrix
			int row = (int)(c.getY()/cellSize) + 1; 
			int col = (int)(c.getX()/cellSize) + 1;
			int z = (int)(c.getZ()/cellSize) + 1;
			assert row > 0 && row <= rows && col > 0 && col <= cols : "Location " + 
			c + " is out of world's bounds";
		
			return this.cells[row][col][z];
		}
		
		public void setHostsList(List<DTNHost> hosts){
			this.hosts = hosts;
		}
		
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
					" cell number: "+ number[0]+" "+number[1]+" "+number[2];
			}
		}
	}
}
