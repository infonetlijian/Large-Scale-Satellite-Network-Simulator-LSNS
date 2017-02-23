package core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import util.Tuple;
import satellite_orbit.SatelliteOrbit;

public class Neighbors {
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "Interface";
	/** transmit range -setting id ({@value})*/
	public static final String TRANSMIT_RANGE_S = "transmitRange";
	/** scenario name -setting id ({@value})*/
	public static final String SCENARIONAME_S = "Scenario";
	/** simulation end time -setting id ({@value})*/
	public static final String SIMULATION_END_TIME = "endTime";
	
	private static final double INTERVAL = 1;
	private static final double MIN_PREDICT_TIME = 100;
	private double PREDICT_TIME = 600;
	
	private DTNHost host;
	private double simEndTime;
	private double msgTtl;
	private double	transmitRange;//设置的可通行距离阈值
	private HashMap<DTNHost, double[]> neighborsLiveTime= new HashMap<DTNHost, double[]>();
	private HashMap<DTNHost, double[]> potentialNeighborsStartTime= new HashMap<DTNHost, double[]>();
	
	private List<DTNHost> neighbors = new ArrayList<DTNHost>();//邻居节点列表 
	private List<DTNHost> hosts = new ArrayList<DTNHost>();//全局卫星节点列表
	private List<NetworkInterface> potentialNeighbors = new ArrayList<NetworkInterface>();
	
	HashMap<DTNHost, List<Double>> leaveTime = new HashMap<DTNHost, List<Double>>();
	HashMap<DTNHost, List<Double>> startTime = new HashMap<DTNHost, List<Double>>();
	private double updateInterval = 1;
	
	public List<DTNHost> getNeighbors(DTNHost host, double time){
		int num = (int)((time-SimClock.getTime())/updateInterval);
		time = SimClock.getTime()+num*updateInterval;
		
		List<DTNHost> neiHost = new ArrayList<DTNHost>();//邻居列表
		
		HashMap<DTNHost, Coord> loc = new HashMap<DTNHost, Coord>();
		loc.clear();
		Coord location = new Coord(0,0); 	// where is the host
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
		}
		
		Coord myLocation = loc.get(host);
		for (DTNHost h : hosts){//再分别及计算
			if (h == host)
				continue;
			if (JudgeNeighbors(myLocation, loc.get(h)) == true){
				//System.out.println(host+"  locate  "+myLocation+"  "+loc.get(host));
				neiHost.add(h);
			}
		}
		//System.out.println(host+" neighbor: "+neiHost+" time: "+time);
		return neiHost;
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
		
		List<DTNHost> futureList = new ArrayList<DTNHost>();//(邻居网格内的节点集合)
		
		for (; time < SimClock.getTime() + msgTtl*60; time += updateInterval){
			List<DTNHost> neighborList = getNeighbors(host, time);
			
			for (DTNHost ni : neighborList){
				if (ni == this.host)//排除自身节点
					continue;
				if (!neiList.contains(ni))//如果现有邻居中没有，则一定是未来将到达的邻居					
					futureList.add(ni); //此为未来将会到达的邻居(当然对于当前已有的邻居，也可能会中途离开，然后再回来)
				
				/**如果是未来到达的邻居，直接get会返回空指针，所以要先加startTime和leaveTime判断**/
				if (startTime.containsKey(ni)){
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
		Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>> predictTime= //二元组合并开始和结束时间
				new Tuple<HashMap<DTNHost, List<Double>>, HashMap<DTNHost, List<Double>>>(startTime, leaveTime); 
		return predictTime;
	}
	
	/**
	 * 初始化函数
	 * @param host
	 */
	public Neighbors(DTNHost host){
		this.host = host;
		Settings s = new Settings(INTERFACENAME_S);
		transmitRange = s.getDouble(TRANSMIT_RANGE_S);//从配置文件中读取传输速率
		Settings set = new Settings(SCENARIONAME_S);
		simEndTime = set.getDouble(SIMULATION_END_TIME);
		Settings se = new Settings("Group");
		msgTtl = se.getDouble("msgTtl");
		//System.out.println(msgTtl);
		PREDICT_TIME = msgTtl*60;
	}
	public Neighbors(List<DTNHost> hosts){
		this.hosts=hosts;
	}
	/**
	 * 返回全局节点列表
	 * @return
	 */
	public List<DTNHost> getHosts(){
		return this.hosts;
	}
	/**
	 * 修改潜在邻居节点(即未来可能成为邻居的节点)列表，仅由ConnectivityGrid.java中的getNearInterfaces()函数调用
	 * @param potentialNeighbors
	 */
	public void changePotentialNeighbors(List<NetworkInterface> potentialNeighbors){
		this.potentialNeighbors = potentialNeighbors;
	}
	/**
	 * 更改全局节点列表
	 * @param hosts
	 */
	public void changeHostsList(List<DTNHost> hosts){
		this.hosts=hosts;
	}
	/**
	 * 返回当前节点的所有邻居节点列表
	 * @return
	 */
	public List<DTNHost> getNeighbors(){
		return this.neighbors;
	}
	/**
	 * 返回预测邻居节点到达邻居范围的时间
	 * @return
	 */
	public HashMap<DTNHost, double[]> getPotentialNeighborsStartTime(){
		return this.potentialNeighborsStartTime;
	}
	/**
	 * 返回邻居节点的存在时间
	 * @return
	 */
	public HashMap<DTNHost, double[]> getNeighborsLiveTime(){
		return this.neighborsLiveTime;
	}
	/**
	 * 修改邻居节点列表，以及相应的生存时间
	 * @param nei
	 */
	public void changeNeighbors(List<DTNHost> nei){
		this.neighbors = nei;
		this.neighborsLiveTime.clear();
		for (DTNHost host : nei){
			double[] liveTime = new double[2];
			liveTime[0] = SimClock.getTime();
			liveTime[1] = SimClock.getTime();
			this.neighborsLiveTime.put(host, liveTime);//把新到来的邻居节点加入列表中	
		}
	}
	/**
	 * 在SatelliteLaserInterface.java里用于维护离开的邻居节点
	 * @param host
	 */
	public void removeNeighbor(DTNHost host){
		this.neighbors.remove(host);
		this.neighborsLiveTime.remove(host);
	}
	/**
	 * 添加当前节点的邻居节点
	 * @param host
	 */
	public void addNeighbor(DTNHost host){
		if (host != this.host){
			if (this.neighbors.contains(host))
				;
			else
				this.neighbors.add(host);
			if (this.neighborsLiveTime.containsKey(host))
				;
			else{	
				double[] liveTime = new double[2];
				liveTime[0] = SimClock.getTime();
				liveTime[1] = SimClock.getTime();
				this.neighborsLiveTime.put(host, liveTime);//把新到来的邻居节点加入列表中				
				}
		}
	}
	/**
	 * 邻居节点更新的入口，实时更新节点的邻居，并预测其它非邻居节点
	 * @param ni
	 * @param connections
	 */
	public void updateNeighbors(DTNHost host, List<Connection> connections){		
		updateNeighborsEndTime(this.neighborsLiveTime);
		predictAllStartTime();
		predictAllEndTime();
		//for (DTNHost h : this.neighbors)
		//	System.out.println(this.host+"  "+h+"  "+SimClock.getTime()+"  "+this.neighborsLiveTime.get(h)[1]);
		/*Collection<DTNHost> hosts = this.potentialNeighborsStartTime.keySet();
		for (DTNHost h : hosts){
			for (int i = 0; i < 2; i++){
				System.out.print(h+"  "+this.potentialNeighborsStartTime.get(h)[i]+"  ");
			}
			System.out.println("");
		}
	*/
	}
	/**
	 * 计算邻居节点未来离开的时间
	 * @param host
	 * @return
	 */
	public boolean getNeighborsLeaveTime(DTNHost host){
		if (this.potentialNeighborsStartTime.containsKey(host)){
			double[] liveTime = this.potentialNeighborsStartTime.get(host);
			if (liveTime[0] != liveTime[1] && liveTime[1] > SimClock.getTime()){
				changeNeighborsLiveTime(host, liveTime[1]);//如果此节点的到来之前已经预测过就直接把它读出来
			}
		}
		
		double timeNow = SimClock.getTime();
		for (double time = timeNow; time < SimClock.getTime()+msgTtl*60; time += INTERVAL){//粗搜索，搜索步长大
			if (JudgeNeighbors(this.host.getCoordinate(time), host.getCoordinate(time)) == false){//如果仍然在邻居范围内就继续增加时间，暴力搜索
				for (double var = time - INTERVAL; var <= time; var += 0.1){//找到了大概范围之后改为细搜索，按照系统的updateInterval作为搜索步长
					if (JudgeNeighbors(this.host.getCoordinate(var), host.getCoordinate(var)) == false){
						changeNeighborsLiveTime(host, var);
						return true;//跳出循环
					}
				}
			}
		}
		return false;
	}
	/**
	 * 计算邻居节点的离开时间
	 * @param neighborsLiveTime
	 */
	public void updateNeighborsEndTime(HashMap<DTNHost, double[]> neighborsLiveTime){
		double endTime = SimClock.getTime() + PREDICT_TIME;
		if (!this.neighborsLiveTime.isEmpty() && !this.neighbors.isEmpty()){
			for (DTNHost host : this.neighbors){
				double[] Time = neighborsLiveTime.get(host);
				//double timeNow = SimClock.getTime();
				if (Time[0] != Time[1] || Time[1] == this.simEndTime)//因为不需要重复预测，所以满足条件不用则更新预测时间，减少运算量
					continue;
				else{
					if (getNeighborsLeaveTime(host) == false)
						changeNeighborsLiveTime(host, this.simEndTime);//如果搜索了全局时间都没能找到离开时间，表面此节点一直在邻居范围内
					else
						continue;
				}	
				
			}		
		}
	}
	/**
	 * 修改邻居生存时间里，离开的时间
	 * @param time
	 */
	public void changeNeighborsLiveTime(DTNHost host, double time){
		double[] liveTime = new double[2];
		liveTime[0] = SimClock.getTime();
		liveTime[1] = time;
		this.neighborsLiveTime.put(host, liveTime);//更改原先host对应的liveTime值
	}
	/**
	 * 修改当前不是邻居的节点，其到达和结束的时间
	 * @param time
	 */
	public void changePotentialNeighborsTime(DTNHost host, double time1, double time2){
		double[] liveTime = new double[2];
		liveTime[0] = time1;
		liveTime[1] = time2;
		this.potentialNeighborsStartTime.put(host, liveTime);
	}
	/**
	 * 把预测到达的节点集合中，已经成为邻居的节点移除
	 */
	public void removeExistNeighbors(){
		List<DTNHost> existNeighbors = new ArrayList<DTNHost>();
		existNeighbors.clear();
		Collection<DTNHost> potentialNeighborsStartTime = this.potentialNeighborsStartTime.keySet();
		for (DTNHost host : potentialNeighborsStartTime){
			if (this.potentialNeighborsStartTime.get(host)[0] <= SimClock.getTime())//对于到达预测时间且已成为邻居的节点就移除
				if (this.neighbors.contains(host))
					existNeighbors.add(host);
				else
					assert false : this.host+" 到达预测时间但是还没有成为邻居节点 "+ host;
		}
		for (DTNHost host : existNeighbors){
			this.potentialNeighborsStartTime.remove(host);
		}
	}
	/**
	 * 用于预测全部节点成为邻居的时间
	 */
	public void predictAllStartTime(){

		//List<DTNHost> hosts = new ArrayList<DTNHost>(); 
		//hosts = this.hosts;
		//hosts.removeAll(this.neighbors);//去掉邻居节点
		
		removeExistNeighbors();//去掉已成为邻居的预测节点!!!
		
		boolean findLabel = false;
		for (DTNHost host : this.hosts){
			findLabel = false;
			if (this.neighbors.contains(host) || host.getAddress() == this.host.getAddress()){//已经是邻居节点无需预测
				continue;
			}
			else{
				if (this.potentialNeighborsStartTime.containsKey(host) == false){//已经预测过的就不用再算了	
					for (double time = SimClock.getTime(); time < SimClock.getTime()+msgTtl*60; time += INTERVAL){
						if (JudgeNeighbors(this.host.getCoordinate(time) , 
								host.getCoordinate(time)) == true){//判断什么时候才会成为邻居
							for (double var = time - INTERVAL; var < time; var += 0.1){
								if (JudgeNeighbors(this.host.getCoordinate(time) , 
										host.getCoordinate(time)) == true){//判断什么时候才会成为邻居
									if (var > 0){
										findLabel = true;
										changePotentialNeighborsTime(host, time, time);
										//System.out.print(this.host+"   "+host+"  "+SimClock.getTime()+"  allNeighborsStartTime is ");
										//System.out.println(time+"  "+this.neighbors + "  "+this.potentialNeighborsStartTime+"  "+this.neighborsLiveTime);
										break;
									}
								}
							}
							break;
						}
					}
					if (findLabel == true)
						continue;
					else {
						changePotentialNeighborsTime(host, -1, -1);//代表历遍全局也没找到，因此不可能成为邻居节点
						//System.out.println(this.host+"   "+host+"  NeighborsStartTime is -1  "+this.potentialNeighborsStartTime);
					}
				}
			}
		}

	}
	/**
	 * 预测可能成为邻居的节点其离开的时间
	 */
	public void predictAllEndTime(){
		boolean findLabel = false;
		for (DTNHost host : this.potentialNeighborsStartTime.keySet()){
			findLabel = false;
			assert !this.neighbors.contains(host) : "预测节点被包含在了邻居节点中";
			if (this.potentialNeighborsStartTime.get(host)[0] == 
					this.potentialNeighborsStartTime.get(host)[1] && 
					this.potentialNeighborsStartTime.get(host)[0] > 0){//保证对未来可能成为邻居的节点不会重复预测，同时也要排除不可能成为邻居的节点
				for (double time = this.potentialNeighborsStartTime.get(host)[0]; time < SimClock.getTime()+msgTtl*60; time += INTERVAL){
					if (JudgeNeighbors(this.host.getCoordinate(time), 
							host.getCoordinate(time)) == false){//判断什么时候才会离开
						for (double var = time - INTERVAL; var < time; var += 0.1){
							if (JudgeNeighbors(this.host.getCoordinate(time), 
									host.getCoordinate(time)) == false){//判断什么时候才会离开
								if (var > 0){
									findLabel = true;
									changePotentialNeighborsTime(host, 
											this.potentialNeighborsStartTime.get(host)[0], var);
									//System.out.print(this.host+"   "+host+"  allNeighborsEndTime is ");
									//System.out.println("  "+this.potentialNeighborsStartTime.get(host)[0]+"  "+this.potentialNeighborsStartTime.get(host)[1]);
									break;
								}
							}
						}break;
					}
				}
				if (findLabel == true)
					continue;
				else 			
					changePotentialNeighborsTime(host, 
							this.potentialNeighborsStartTime.get(host)[0], simEndTime);//说明在全局时间里其在未来某个时间成为邻居后不会再离开了				
			}
		}
	}
	/**
	 * 预测那些在相邻网格中但还不是邻居节点的卫星
	 * @param potentialNeighbors
	 */
	public void updatePotentialNeighborsStartTime(List<NetworkInterface> potentialNeighbors){	
		double endTime = SimClock.getTime() + PREDICT_TIME;
		//for (DTNHost host : this.neighbors){
		//	potentialNeighbors.remove(host.getInterface(0));//去掉那些已经是邻居节点的
		//}
		Collection<DTNHost> potentialNeighborsStartTime = this.potentialNeighborsStartTime.keySet();//超时的就把它剔除 
		for (DTNHost host : potentialNeighborsStartTime){
			if (this.potentialNeighborsStartTime.get(host)[0] > SimClock.getIntTime())
				this.potentialNeighborsStartTime.remove(host);
		}
		
		for (NetworkInterface ni : potentialNeighbors){
			if (!this.potentialNeighborsStartTime.containsKey(ni.getHost()) && 
					!this.neighborsLiveTime.containsKey(ni.getHost())){//保证每个节点只会预测一次预测，且已经是邻居的节点不用预测
				for (double time = SimClock.getTime(); time < endTime; time += INTERVAL){
					if (JudgeNeighbors(this.host.getCoordinate(time) , 
							ni.getHost().getCoordinate(time))){//判断什么时候才会成为邻居
						for (double var = time - INTERVAL; var < time; var += 0.1){
							if (JudgeNeighbors(this.host.getCoordinate(var) , 
									ni.getHost().getCoordinate(var))){
								double[] liveTime = new double[2];
								liveTime[0] = var;
								liveTime[1] = var;
								this.potentialNeighborsStartTime.put(ni.getHost(), liveTime);
								System.out.print(this.host+"   "+ni.getHost()+"  potentialNeighborsStartTime is ");
								System.out.println(liveTime[0]);
								break;//跳出循环
							}
						}
					}
					break;//跳出第二层循环
				}
			}	
		}	
		//HashMap<DTNHost,double[]> potentialNeighborsTime = new HashMap<DTNHost,double[]>();
		for (DTNHost host : this.neighbors){
			this.potentialNeighborsStartTime.remove(host);//已经成为邻居节点的预测可以删除
			//System.out.println("test!");
		}	
	}	
	/**
	 * 计算在时刻t时，卫星节点的邻居节点并存储在neighbors中
	 * @param parameters
	 * @param t
	 */
	public void CalculateNeighbor(DTNHost host,double t){
		double[][] myCoordinate=new double[1][3];	
		
		myCoordinate=GetCoordinate(host.getParameters(),t);
		int nrofhosts=this.hosts.size();
		
		int index=this.hosts.indexOf(host);
		ChangeMyHost(index);
		
		for(int n=1;n<nrofhosts;n++){//列表中节点的总数是nrofhosts个，末尾那个是本节点，所以不用算
			if(JudgeNeighbors(GetCoordinate(hosts.get(n).getParameters(),t),myCoordinate)){
				neighbors.add(hosts.get(n));
			}
		}
		//System.out.print(hosts.get(index));//系统输出打印是邻居的节点
		//System.out.println(neighbors);
	}
	/**
	 * 把列表中本DTNHost放到动态数组的末尾去，只计算与其它节点之间的距离
	 * @param index
	 */
	public void ChangeMyHost(int index){
		DTNHost host=this.hosts.get(index);
		this.hosts.remove(index);
		this.hosts.add(host);//改变本DTNHost在表中的顺序
	}
	/**
	 * 输入轨道参数和时间，得到对应三维坐标位置，二维数组[1][3]
	 * @param parameters
	 * @param t
	 * @return
	 */
	public double[][] GetCoordinate(double[] parameters,double t){
		SatelliteOrbit saot=new SatelliteOrbit(parameters);
		double[][] myCoordinate;
		myCoordinate=saot.getSatelliteCoordinate(t);
		return myCoordinate;
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
	 * 返回true代表是邻居节点，在通信范围内，返回false则在通信范围之外
	 * @param c1
	 * @param c2
	 * @return
	 */
	public boolean JudgeNeighbors(double[][] c1,double[][] c2){
		double var;
		var=(c1[0][0]-c2[0][0])*(c1[0][0]-c2[0][0])+(c1[0][1]-c2[0][1])*(c1[0][1]-c2[0][1])+(c1[0][2]-c2[0][2])*(c1[0][2]-c2[0][2]);
		var=EnsurePositive(var);
		if (Math.sqrt(var) <= this.transmitRange)
			return true;
		else 
			return false;
	}
	public double EnsurePositive(double var){
		if (var>=0);
		else
			var=-var;
		return var;
	}
}
