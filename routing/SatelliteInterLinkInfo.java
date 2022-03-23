package routing;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import movement.SatelliteMovement;
import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimError;
import util.Tuple;

public class SatelliteInterLinkInfo {
    /**
     * Group name in the group -setting id ({@value})
     */
    public static final String GROUPNAME_S = "Group";
    /**
     * Interface name in the group -setting id ({@value})
     */
    public static final String INTERFACENAME_S = "Interface";
    /**
     * Decides the message transmitted through radio link or laser link
     * according to this message size threshold， -setting id ({@value})
     */
    public static final String MSG_SIZE_THRESHOLD_S = "MessageThreshold";
    /** indicates the type of link*/
    public static final String LASER_LINK = "LaserInterface";
    /** indicates the type of link*/
	public static final String RADIO_LINK = "RadioInterface";
	
	/* bind the host and this info*/
	private DTNHost host;
	/* bind the SatelliteMovement and this info*/
	private SatelliteMovement sMovement;

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
    
    /** store LEO cluster information */
    private LEOclusterInfo LEOci;
    /** store MEO cluster information */
    private MEOclusterInfo MEOci;
    /** store GEO cluster information */
    private GEOclusterInfo GEOci;
    
    /** initialization label*/
    private boolean initLable = false;
    /** the message size threshold, decides the message transmitted 
     *  through radio link or laser link -setting id ({@value}*/
    private static int msgThreshold;
	/** maximum connection betweent this node and node in the neighbor plane*/
	public int nrofAllowConnectedHostInNeighborPlane = 2;//设定邻居轨道平面最大允许的连接数

    public SatelliteInterLinkInfo(DTNHost host, String satelliteType){
    	this.host = host;
    	this.sMovement = (SatelliteMovement)this.host.getMovementModel();
    	
    	Settings setting = new Settings(INTERFACENAME_S);
    	msgThreshold = setting.getInt(MSG_SIZE_THRESHOLD_S);
    	//LEO  	
        Settings s = new Settings(GROUPNAME_S);
        LEO_TOTAL_SATELLITES = s.getInt("nrofLEO");//总节点数
        LEO_TOTAL_PLANE = s.getInt("nrofLEOPlanes");//总轨道平面数
        LEO_NROF_S_EACHPLANE = LEO_TOTAL_SATELLITES/LEO_TOTAL_PLANE;//每个轨道平面上的节点数
        //MEO
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
        clusterInfoInit();
        //if it is static clustering, then start initialization
        //and decide MEO manage hosts for LEO nodes
    }
    
    /**
     * Initialize cluster information
     */
    public void clusterInfoInit(){
	    switch (this.getSatelliteType()){
	        case "LEO":{
	            LEOci = new LEOclusterInfo(host);
	            //TODO
	            break;
	        }
	        case "MEO":{
	            MEOci = new MEOclusterInfo(host);
	            break;
	        }
	        case "GEO":{
	            GEOci = new GEOclusterInfo(host);
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
                    	//LEOci.updateManageHosts();
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
            case "GEO":{
            	return true;
            }
        }
        throw new SimError("Satellite Type Error!");
    }
	/**
	 * Returns the host this router is in
	 * @return The host object
	 */
	protected DTNHost getHost() {
		return this.host;
	}
    /**
     * @return satellite type in multi-layer satellite networks: LEO, MEO or GEO
     */
    public String getSatelliteType(){
        return this.host.getSatelliteType();
    }
    /**
     * get all satellite nodes info in the movement model
     *
     * @return all satellite nodes in the network
     */
    public List<DTNHost> getHosts() {
        return new ArrayList<DTNHost>(((SatelliteMovement) this.getHost().getMovementModel()).getHosts());
    }
	public List<DTNHost> findAllMEOHosts(){
		List<DTNHost> MEOLists = new ArrayList<DTNHost>();
		for (DTNHost h : this.getHosts()){
			if (h.getSatelliteType().contains("MEO")){
				MEOLists.add(h);
			}			
		}
		return MEOLists;
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
     * @return LEO satellite's cluster information
     */
    public LEOclusterInfo getLEOci(){
    	return this.LEOci;
    }
    /**
     * @return MEO satellite's cluster information
     */
    public MEOclusterInfo getMEOci(){
    	return this.MEOci;
    }
    /**
     * @return GEO satellite's cluster information
     */
    public GEOclusterInfo getGEOci(){
    	return this.GEOci;
    }
    /**
     * initialize static LEO cluster list managed by MEO node
     */
    public void initStaticClustering(){
    	//check the initialization label
    	if (this.initLable == true){
    		return;
    	}
    	
    	Settings s = new Settings("Group");
    	int totalMEOPlane = s.getInt("nrofMEOPlane");
    	int totalLEOPlane = s.getInt("nrofLEOPlanes");
    	int staticNrofManageLEOPlane = totalLEOPlane / totalMEOPlane;//平均每个MEO平面需要管理的LEO轨道平面数
    	
//    	System.out.println("hosts size  "+getHosts().size());
    	/*find all MEO orbit plane and their first MEO nodes*/
    	List<DTNHost> allMEOPlane = new ArrayList<DTNHost>();
    	for (int i = LEO_TOTAL_SATELLITES; i < LEO_TOTAL_SATELLITES + 
    			MEO_TOTAL_PLANE * MEO_NROF_S_EACHPLANE; i+= MEO_NROF_S_EACHPLANE){  	
    		DTNHost h = findHostByAddress(i);
    		allMEOPlane.add(h);
    	}
    	/*对每一个MEO平面分配管理节点*/
    	for (DTNHost MEO : allMEOPlane){
        	double MEOAngle = sMovement.getOrbitParameters()[3];//获取第4个参数,升交点赤经
        	//find all MEO hosts in same orbit plane
        	if (!MEO.getSatelliteType().contains("MEO"))
        		System.out.println(MEO+"  error!  "+MEO.getSatelliteType());
        	List<DTNHost> MEOInSamePlane = 
        			((SatelliteMovement)MEO.getMovementModel())
        			.getSatelliteLinkInfo().getMEOci().findAllSatellitesInSamePlane();
        	//记录每个LEO平面的簇头节点代表，以及轨道平面的升交点赤经参数
        	HashMap<DTNHost, Double> LEOAngle = new HashMap<DTNHost, Double>();
            /** total number of LEO satellites*/

        	/*收集所有LEO轨道平面的升交点赤经参数*/
        	for (int i = 0; i < LEO_TOTAL_SATELLITES; i+= LEO_NROF_S_EACHPLANE){
        		DTNHost h = findHostByAddress(i);
        		LEOAngle.put(h, ((SatelliteMovement)h.getMovementModel()).getOrbitParameters()[3]);//获取第4个参数,升交点赤经
        	}
        	
        	/*找出离自己升交点赤经差距最小的LEO轨道平面*/
        	List<Tuple<DTNHost, Double>> differenceOfAngle = new ArrayList<Tuple<DTNHost, Double>>();
        	for (DTNHost h : LEOAngle.keySet()){
        		differenceOfAngle.add(new 
        				Tuple<DTNHost, Double>(h, Math.abs(MEOAngle - LEOAngle.get(h))));		
        	}
        	
        	sort(differenceOfAngle);
        	
        	//找出本MEO节点轨道平面最近的多个LEO轨道平面，并设置管理节点为本MEO轨道平面上的所有节点
        	for (int index = 0, times = 0; index 
        			< staticNrofManageLEOPlane; times++){             		
        		if (times > differenceOfAngle.size())
        			break;
        		DTNHost minLEO = differenceOfAngle.get(times).getKey();
        		//如果此平面已经被安排了MEO管理节点，就跳过
        		if (!((SatelliteMovement)minLEO.getMovementModel()).
        				getSatelliteLinkInfo().getLEOci().getManageHosts().isEmpty()){
        			continue;
        		}
        		//1.set manage list for LEO nodes
            	List<DTNHost> allHostsInSameLEOPlane = 
            			((SatelliteMovement)minLEO.getMovementModel()).getSatelliteLinkInfo().getLEOci().getAllHostsInSamePlane();	
            	for (DTNHost h : allHostsInSameLEOPlane){
            		((SatelliteMovement)h.getMovementModel()).getSatelliteLinkInfo().getLEOci().setManageHosts(MEOInSamePlane);
            	}
            	//2.set cluster list for MEO nodes
            	for (DTNHost MEOs : MEOInSamePlane){
            		((SatelliteMovement)MEOs.getMovementModel())
            			.getSatelliteLinkInfo().getMEOci().addClusterMember(allHostsInSameLEOPlane);
            	}
            	index++;
        	}         	
    	}
    	for (DTNHost h : this.getHosts()){
    		if (h.getSatelliteType().contains("LEO"))
    			System.out.println(h+" manage hosts "+((SatelliteMovement)h.getMovementModel())
            			.getSatelliteLinkInfo().getLEOci().getManageHosts());
    	}
    	this.initLable = true;
//        for (int n = 0; n < nearnestPlane.size(); n++){
//            for (DTNHost host : getHosts()){
//                int startNumber = this.startNumberInSameMEOPlane;//此轨道平面内的节点，起始编号
//                int endNumber = this.endNumberInSameMEOPlane;//此轨道平面内的节点，结尾编号
//
//                //找出当前平面内的所有节点，加入此MEO所管理的分簇中
//                if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
//                    if (!host.getSatelliteType().contains("LEO"))
//                        throw new SimError("Clustering Calculation error!");
//                    //同时对相应的LEO添加本节点作为管理节点
//                    ((OptimizedClusteringRouter)host.getRouter()).LEOci.addManageHost(thisNode);
//                    clusterList.add(host);//添加分簇内的节点
//                }
//            }
//        }
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
    /**
     * Stores the cluster information in the LEO node
     */
    public class LEOclusterInfo{
    	/** bind node and this cluster info*/
    	public DTNHost thisNode;
       
        /** all hosts in LEO plane*/
        private List<DTNHost> LEOList = new ArrayList<DTNHost>();
        /** all hosts in the same orbit plane*/
        public List<DTNHost> allHostsInSamePlane = new ArrayList<DTNHost>();
        /** neighbor hosts in the same orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectLEOHostsInSamePlane = new ArrayList<DTNHost>();
        /** neighbor hosts in two neighbor orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectLEOHostsInNeighborPlane = new ArrayList<DTNHost>();
        /** neighbor hosts in the neighbor orbit plane*/
        public List<DTNHost> neighborPlaneHosts = new ArrayList<DTNHost>();//相邻轨道平面内的两个邻居节点
        /** hosts list in the same orbit plane, and they can be forwarded directly without MEO */
        public List<DTNHost> neighborHostsInSamePlane = new ArrayList<DTNHost>();//相同轨道平面里的两个邻居节点
        /** all manage hosts which contains in the transmission range of MEO */
        private List<DTNHost> manageHosts = new ArrayList<DTNHost>();

        /* plane number of this LEO node*/
        public int nrofLEOPlane;
        /** start address number of the first host in the plane*/
        public int startNumberInSameLEOPlane;//此轨道平面内的节点，起始编号
        /** end address number of the first host in the plane*/
        public int endNumberInSameLEOPlane;//此轨道平面内的节点，结尾编号
        
        public LEOclusterInfo(DTNHost h){
        	thisNode = h;
        	
            //设置本MEO轨道平面内的开始/结束节点编号，以及MEO平面编号
            setPlaneNumber();
        	
            initInterSatelliteNeighbors();//初始化记录节点在同一个轨道内的所有节点，以及轨道内相邻的邻居用于直接转发
            //找到所有LEO节点
            findAllLEONodes();
            //找到所有邻居轨道平面的节点
            findAllSatellitesInLEONeighborPlane();
            //同平面内的邻居节点
            findAllowConnectMEOHostsInLEOSamePlane(thisNode.getAddress()/LEO_NROF_S_EACHPLANE + 1, LEO_NROF_S_EACHPLANE);
        }
        /**
         * 计算本LEO节点所属的轨道参数
         */
        public void setPlaneNumber(){
        	this.nrofLEOPlane = thisNode.getAddress()/LEO_NROF_S_EACHPLANE + 1;
        	this.startNumberInSameLEOPlane = LEO_NROF_S_EACHPLANE * (nrofLEOPlane - 1);//此轨道平面内的节点，起始编号
            this.endNumberInSameLEOPlane = LEO_NROF_S_EACHPLANE * nrofLEOPlane - 1;//此轨道平面内的节点，结尾编号
        }
        /**
         * 初始化设定本节点的同轨的邻居节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllowConnectMEOHostsInLEOSamePlane(int nrofPlane, int nrofSatelliteInOnePlane){ 
            int	startNumber = this.startNumberInSameLEOPlane;//此轨道平面内的节点，起始编号
            int endNumber = this.endNumberInSameLEOPlane;//此轨道平面内的节点，结尾编号
            if (!(thisNode.getAddress() >= startNumber && thisNode.getAddress()<= endNumber)){
            	throw new SimError("LEO address calculation error");
            }
            int a = thisNode.getAddress() - 1;
            int b = thisNode.getAddress() + 1;
            
            if (a < startNumber)
            	a = endNumber;
            if (b > endNumber)
            	b = startNumber;
            allowConnectLEOHostsInSamePlane.add(findHostByAddress(a));
            allowConnectLEOHostsInSamePlane.add(findHostByAddress(b));
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
                	allowConnectLEOHostsInNeighborPlane.add(host);
                }
            }
            //右邻居MEO轨道平面
            int startNumber2 = LEO_NROF_S_EACHPLANE * (b - 1);//此轨道平面内的节点，起始编号
            int endNumber2 = LEO_NROF_S_EACHPLANE * b - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber2 && host.getAddress() <= endNumber2){
                	allowConnectLEOHostsInNeighborPlane.add(host);
                }
            }
        }
        /**
         * 同一平面内的邻居两个节点
         * @return
         */
        public List<DTNHost> getAllowConnectLEOHostsInLEOSamePlane(){
        	return allowConnectLEOHostsInSamePlane;
        }
        /**
         * neighbor hosts in two neighbor orbit plane
         * @return
         */
        public List<DTNHost> getAllowConnectLEOHostsInNeighborPlane(){
        	return allowConnectLEOHostsInNeighborPlane;
        }
        /**
         * 动态找到MEO的当前邻居轨道的最近两个节点用户邻居通信
         * @return
         */
        public List<DTNHost> updateAllowConnectLEOHostsInNeighborPlane(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	
        	if (!thisNode.getRouter().CommunicationSatellitesLabel)
        		return list;//非通信节点，直接返回，不会和邻居轨道的LEO节点建立连接
        	
        	List<Tuple<DTNHost, Double>> listFromDistance = new ArrayList<Tuple<DTNHost, Double>>();
        	for (DTNHost h : getAllowConnectLEOHostsInNeighborPlane()){
        		if (thisNode.getRouter().CommunicationNodesList.containsKey(h)){//确定是一个通信节点
        			listFromDistance.add(new Tuple<DTNHost, Double>(h, getDistance(thisNode, h)));
        		}
        	}
        	sort(listFromDistance);
        	for (Tuple<DTNHost, Double> t : listFromDistance){
        		if (list.isEmpty())
        			list.add(t.getKey());
        		else{
        			//不是同一个轨道平面的
        			if (!((SatelliteMovement)list.get(0).getMovementModel()).
        					getSatelliteLinkInfo().getLEOci().getAllowConnectLEOHostsInLEOSamePlane().contains(t.getKey())){
        					list.add(t.getKey());	
        					}
        		}
        		if (list.size() >= nrofAllowConnectedHostInNeighborPlane)
        			return list;
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
//            System.out.println(thisNode+" src plane: "+serialNumberOfPlane+"  "+to+" des plane: "+destinationSerialNumberOfPlane);
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
         * @return all communication LEO nodes in this LEO orbit plane
         */
        public List<DTNHost> getAllCommunicationNodes(){
        	List<DTNHost> CommunicationNodes = new ArrayList<DTNHost>();
        	for (DTNHost LEO : this.getAllHostsInSamePlane()){
        		if (LEO.getRouter().CommunicationSatellitesLabel){
        			CommunicationNodes.add(LEO);
        		}
        	}
        	return CommunicationNodes;
        }
        /**
         * @return hosts list contains all LEO nodes in the same plane
         */
        public List<DTNHost> getNeighborHostsInSamePlane(){
            return neighborHostsInSamePlane;
        }
        /**
         * Bubble sort algorithm, from small value to large value
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
         * 获取当前通信范围内的MEO节点
         * @return
         */
        public List<DTNHost> getConnectedMEOHosts(Message msg){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	for (Connection con : thisNode.getConnections()){
        		DTNHost h = con.getOtherNode(thisNode);
        		if (!list.contains(h) && h.getSatelliteType().contains("MEO") 
        				&& isRightConnection(msg, con))
        			list.add(h);
        	}
        	return list;
        }
        /**
         * update manage hosts according to connection
         */
        public List<DTNHost> updateManageHosts(Message msg){
        	if (sMovement.getDynamicClustering()){
            	manageHosts.clear();
            	manageHosts.addAll(getConnectedMEOHosts(msg));
            	return manageHosts;
        	}
        	else{
        		List<DTNHost> MEOHosts = new ArrayList<DTNHost>(manageHosts);
        		MEOHosts.retainAll(getConnectedMEOHosts(msg));
        		return MEOHosts;
        	}
        }
        /**
         * add a MEO manage host into list
         * @param h
         */
        public void addManageHost(DTNHost h){
            manageHosts.add(h);
        }
        /**
         * set a MEO manage host into list
         * @param hosts
         */
        public void setManageHosts(List<DTNHost> hosts){
        	if (manageHosts == null)
        		manageHosts = new ArrayList<DTNHost>();
        	manageHosts.clear();
            manageHosts.addAll(hosts);
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
            if (n < startNumberInSameLEOPlane)
                return endNumberInSameLEOPlane;
            if (n > endNumberInSameLEOPlane)
                return startNumberInSameLEOPlane;
            //int nrofPlane = n/nrofSatelliteInOnePlane + 1;
            return n;
        }
        /**
         * 初始化设定本节点的同轨所有节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInSamePlane(int nrofPlane, int nrofSatelliteInOnePlane){
            if (startNumberInSameLEOPlane == 0 && endNumberInSameLEOPlane == 0) {  
            	startNumberInSameLEOPlane = LEO_NROF_S_EACHPLANE * (nrofPlane - 1);//此轨道平面内的节点，起始编号
            	endNumberInSameLEOPlane = LEO_NROF_S_EACHPLANE * nrofPlane - 1;//此轨道平面内的节点，结尾编号
            }

            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumberInSameLEOPlane && host.getAddress()<= endNumberInSameLEOPlane){
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
    public class MEOclusterInfo{
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
            	System.out.println(thisNode.getAddress()+"  "+this.nrofMEOPlane+"  "+
            			this.startNumberInSameMEOPlane+"  "+this.endNumberInSameMEOPlane);
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
        public List<DTNHost> findAllSatellitesInSamePlane(){ 
        	allHostsInSamePlane.clear();
        	int	startNumber = this.startNumberInSameMEOPlane;//此轨道平面内的节点，起始编号
            int endNumber = this.endNumberInSameMEOPlane;//此轨道平面内的节点，结尾编号

            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
                    allHostsInSamePlane.add(host);//同一个轨道内的相邻节点
                }
            }
            return allHostsInSamePlane;
        }
        /**
         * 初始化设定本节点的两个邻居轨道平面所有节点于allowConnectMEOHostsInNeighborPlane中
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInNeighborPlane(){ 

            int thisHostAddress = getHost().getAddress();

            int serialNumberOfPlane = nrofMEOPlane;
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
        			if (!((SatelliteMovement)list.get(0).getMovementModel()).
        					getSatelliteLinkInfo().getMEOci().getAllowConnectMEOHostsInSamePlane().contains(t.getKey())){
        					list.add(t.getKey());	
        					}
        		}
        		if (list.size() >= nrofAllowConnectedHostInNeighborPlane)
        			return list;
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
         * update cluster member according to connection
         */
        public void updateClusterMember(){
        	clusterList.clear();
        	clusterList.addAll(getConnectedLEOHosts());
        	
        }
        /**
         * set cluster member 
         */
        public void addClusterMember(List<DTNHost> LEOHosts){
        	if (clusterList == null){
        		clusterList = new ArrayList<DTNHost>();
        	}
        	clusterList.addAll(LEOHosts);
        	
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
         * 获取当前通信范围内的GEO节点
         * @return
         */
        public List<DTNHost> getConnectedGEOHosts(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	for (Connection con : thisNode.getConnections()){
        		DTNHost h = con.getOtherNode(thisNode);
        		if (!list.contains(h) && h.getSatelliteType().contains("GEO"))
        			list.add(h);
        	}
        	return list;
        }
        /**
         * @return cluster list
         */
        public List<DTNHost> getClusterList(){
            return clusterList;//返回的是MEO层相互连接的MEO节点
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
                for (DTNHost LEO : ((SatelliteMovement)MEO.getMovementModel()).getSatelliteLinkInfo().getMEOci().getClusterList()){
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
    
    /**
     * Stores the cluster information in the MEO node
     */
    public class GEOclusterInfo{
        private DTNHost thisNode;
        /** hosts list in the transmission range of GEO*/
        private List<DTNHost> hostsInTransmissionRange;
        /** confirmed MEO hosts list in the cluster */
        private List<DTNHost> GEOclusterList;
        /** all GEO hosts list */
        private List<DTNHost> GEOList = new ArrayList<DTNHost>();
        /** record MEO nodes in other cluster through GEO confirm messges */
        private HashMap<DTNHost, List<DTNHost>> otherClusterList;
        /** record latest cluster information update time */
        private HashMap<DTNHost, Double> clusterUpdateTime;
        /** neighbor hosts in the same orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectGEOHostsInSamePlane = new ArrayList<DTNHost>();
        /** neighbor hosts in two neighbor orbit plane, and they can be forwarded directly*/
        private List<DTNHost> allowConnectGEOHostsInNeighborPlane = new ArrayList<DTNHost>();
        /** all hosts in the same orbit plane*/
        public List<DTNHost> allHostsInSamePlane = new ArrayList<DTNHost>();
        
        /* plane number of this GEO node*/
        private int nrofGEOPlane;
        /** start address number of the first host in the plane*/
        private int startNumberInSameGEOPlane;//此轨道平面内的节点，起始编号
        /** end address number of the first host in the plane*/
        private int endNumberInSameGEOPlane;//此轨道平面内的节点，结尾编号
        
        public GEOclusterInfo(DTNHost thisNode){
            this.thisNode = thisNode;
            hostsInTransmissionRange = new ArrayList<DTNHost>();
            GEOclusterList = new ArrayList<DTNHost>();
            
            findAllGEONodes();
            otherClusterList = new HashMap<DTNHost, List<DTNHost>>();
            clusterUpdateTime = new HashMap<DTNHost, Double>();
            
            //设置本GEO轨道平面内的开始/结束节点编号，以及GEO平面编号
            setPlaneNumber();
            //同轨道平面内邻居的两个节点
            findAllowConnectGEOHostsInSamePlane(nrofGEOPlane, GEO_NROF_S_EACHPLANE);
            //同轨道平面内所有节点
            findAllSatellitesInSamePlane(thisNode.getAddress()/GEO_NROF_S_EACHPLANE + 1, GEO_NROF_S_EACHPLANE);
            //初始化设定本节点的两个邻居轨道平面所有节点
            findAllSatellitesInNeighborPlane();
        }
        /**
         * 计算本GEO节点所属的轨道参数
         */
        public void setPlaneNumber(){
        	this.nrofGEOPlane = (thisNode.getAddress() - LEO_TOTAL_SATELLITES - MEO_TOTAL_SATELLITES)/GEO_NROF_S_EACHPLANE + 1;//默认下取整
        	this.startNumberInSameGEOPlane = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * (nrofGEOPlane - 1);//此轨道平面内的节点，起始编号
            this.endNumberInSameGEOPlane = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * nrofGEOPlane - 1;//此轨道平面内的节点，结尾编号
            System.out.println(thisNode+" GEO "+nrofGEOPlane+"  "+startNumberInSameGEOPlane+"  "+endNumberInSameGEOPlane);
        }
        /**
         * 初始化找到所有GEO属性节点
         */
        public List<DTNHost> findAllGEONodes(){
        	GEOList.clear();
        	for (DTNHost h : getHosts()){
        		if (h.getSatelliteType().contains("GEO"))
        			GEOList.add(h);
        	} 
        	return GEOList;
        }
        /**
         * 初始化设定本节点的同轨的邻居节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllowConnectGEOHostsInSamePlane(int nrofPlane, int nrofSatelliteInOnePlane){ 
            int	startNumber = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES;//此轨道平面内的节点，起始编号
            int endNumber = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * nrofPlane - 1;//此轨道平面内的节点，结尾编号
            if (!(thisNode.getAddress() >= startNumber && thisNode.getAddress()<= endNumber)){
            	
            	throw new SimError("GEO address calculation error");
            }
            int a = thisNode.getAddress() - 1;
            int b = thisNode.getAddress() + 1;
            
            if (a < startNumber)
            	a = endNumber;
            if (b > endNumber)
            	b = startNumber;
            allowConnectGEOHostsInSamePlane.add(findHostByAddress(a));
            allowConnectGEOHostsInSamePlane.add(findHostByAddress(b));
        }
        /**
         * 初始化设定本节点的同轨所有节点
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInSamePlane(int nrofPlane, int nrofSatelliteInOnePlane){ 
        	int startNumber = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * (nrofPlane - 1);//此轨道平面内的节点，起始编号
            int endNumber = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * nrofPlane - 1;//此轨道平面内的节点，结尾编号
            //TODO 检查下这里还有MEOci当中的函数
            
            
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
                    allHostsInSamePlane.add(host);//同一个轨道内的相邻节点
                }
            }
            //System.out.println(thisNode+"  allowConnectGEOHostsInSamePlane  "+allowConnectGEOHostsInSamePlane);
        }
        /**
         * 初始化设定本节点的两个邻居轨道平面所有节点于allowConnectGEOHostsInNeighborPlane中
         * @param nrofPlane
         * @param nrofSatelliteInOnePlane
         */
        public void findAllSatellitesInNeighborPlane(){ 

            int thisHostAddress = getHost().getAddress();

            int serialNumberOfPlane = thisHostAddress/GEO_NROF_S_EACHPLANE + 1;
            int a = serialNumberOfPlane - 1;
            int b = serialNumberOfPlane + 1;
            if (a < 1)
            	a = GEO_TOTAL_PLANE;
            if(b > GEO_TOTAL_PLANE)
            	b = 1;
            //左邻居GEO轨道平面
            int startNumber1 = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * (a - 1);//此轨道平面内的节点，起始编号
            int endNumber1 = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * a - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber1 && host.getAddress() <= endNumber1){
                	allowConnectGEOHostsInNeighborPlane.add(host);
                }
            }
            //右邻居GEO轨道平面
            int startNumber2 = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * (b - 1);//此轨道平面内的节点，起始编号
            int endNumber2 = LEO_TOTAL_SATELLITES + MEO_TOTAL_SATELLITES + GEO_NROF_S_EACHPLANE * b - 1;//此轨道平面内的节点，结尾编号
            
            //如果目的节点在邻居轨道平面上，就找出这个目的节点所属轨道平面的所有的节点
            for (DTNHost host : getHosts()){
                if (host.getAddress() >= startNumber2 && host.getAddress() <= endNumber2){
                	allowConnectGEOHostsInNeighborPlane.add(host);
                }
            }
        }

        /**
         * 动态找到GEO的当前邻居轨道的最近两个节点用户邻居通信
         * @return
         */
        public List<DTNHost> updateAllowConnectGEOHostsInNeighborPlane(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	
        	List<Tuple<DTNHost, Double>> listFromDistance = new ArrayList<Tuple<DTNHost, Double>>();
        	for (DTNHost h : getAllowConnectGEOHostsInNeighborPlane()){
        		listFromDistance.add(new Tuple<DTNHost, Double>(h, getDistance(thisNode, h)));
        	}
        	sort(listFromDistance);
        	for (Tuple<DTNHost, Double> t : listFromDistance){
        		if (list.isEmpty())
        			list.add(t.getKey());
        		else{
        			//不是同一个轨道平面的
        			if (!((SatelliteMovement)list.get(0).getMovementModel()).
        					getSatelliteLinkInfo().getGEOci().getAllowConnectGEOHostsInSamePlane().contains(t.getKey())){
        					list.add(t.getKey());	
        					}
        		}
        		if (list.size() >= nrofAllowConnectedHostInNeighborPlane)
        			return list;
        	}
        	return list;
        }
        /**
         * 同一轨道内的所有节点
         * @return
         */
        public List<DTNHost> getAllowConnectGEOHostsInSamePlane(){
        	return allowConnectGEOHostsInSamePlane;
        }
        /**
         * 邻居轨道内的所有节点
         * @return
         */
        public List<DTNHost> getAllowConnectGEOHostsInNeighborPlane(){
        	return allowConnectGEOHostsInNeighborPlane;
        }
        /**
         * update hosts list that in transmission range of this GEO
         */
        public void clearHostsInTransmissionRange(){
            hostsInTransmissionRange.clear();
        }
        /**
         * @return all LEO+MEO hosts in transmission range
         */
        public List<DTNHost> getHostsInTransmissionRange(){
            return hostsInTransmissionRange;
        }

        /**
         * update MEO cluster member according to connection
         */
        public List<DTNHost> updateGEOClusterMember(){
        	GEOclusterList.clear();
        	GEOclusterList.addAll(getConnectedMEOHosts());
        	return GEOclusterList;
        }
        /**
         * 获取当前通信范围内的MEO节点
         * @return
         */
        public List<DTNHost> getConnectedMEOHosts(){
        	List<DTNHost> list = new ArrayList<DTNHost>();
        	for (Connection con : thisNode.getConnections()){
        		DTNHost h = con.getOtherNode(thisNode);
        		if ((!list.contains(h)) && h.getSatelliteType().contains("MEO"))
        			list.add(h);
        	}
        	return list;
        }
        /**
         * @return cluster list
         */
        public List<DTNHost> getGEOClusterList(){
            return GEOclusterList;
        }
        /**
         * delete other unaccessible GEO node
         * @param h
         */
        public void removeGEONode(DTNHost h){
            GEOList.remove(h);
        }
        /**
         * @return other GEO nodes list in the network
         */
        public List<DTNHost> getGEOList(){
            return GEOList;
        }

        /**
         * @return other GEO cluster information
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
            return " Other GEO Hosts: " + GEOList.toString() +
                    " Hosts in the cluster: " + GEOclusterList.toString() +
                    " other cluster: " + otherClusterList.toString() +
                    "  clusterUpdateTime:  " + clusterUpdateTime;
        }
    }

}
