package core;

import input.EventQueue;
import input.EventQueueHandler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import Cache.CacheRouter;
import Cache.File;
import movement.MapBasedMovement;
import movement.MovementModel;
import movement.SatelliteMovement;
import movement.map.SimMap;
import routing.MessageRouter;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File SimScenario.java
 * Package Name:core
 * Description: A simulation scenario used for getting and storing the
 * settings of a simulation run.
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class SimScenario implements Serializable {
	
	/** a way to get a hold of this... */	
	private static SimScenario myinstance=null;

	/** namespace of scenario settings ({@value})*/
	public static final String SCENARIO_NS = "Scenario";
	/** number of host groups -setting id ({@value})*/
	public static final String NROF_GROUPS_S = "nrofHostGroups";
	/** number of interface types -setting id ({@value})*/
	public static final String NROF_INTTYPES_S = "nrofInterfaceTypes";
	/** scenario name -setting id ({@value})*/
	public static final String NAME_S = "name";
	/** end time -setting id ({@value})*/
	public static final String END_TIME_S = "endTime";
	/** update interval -setting id ({@value})*/
	public static final String UP_INT_S = "updateInterval";
	/** simulate connections -setting id ({@value})*/
	public static final String SIM_CON_S = "simulateConnections";

	/** namespace for interface type settings ({@value}) */
	public static final String INTTYPE_NS = "Interface";
	/** interface type -setting id ({@value}) */
	public static final String INTTYPE_S = "type";
	/** interface name -setting id ({@value}) */
	public static final String INTNAME_S = "name";

	/** namespace for application type settings ({@value}) */
	public static final String APPTYPE_NS = "Application";
	/** application type -setting id ({@value}) */
	public static final String APPTYPE_S = "type";
	/** setting name for the number of applications */
	public static final String APPCOUNT_S = "nrofApplications";
	
	/** namespace for host group settings ({@value})*/
	public static final String GROUP_NS = "Group";
	/** group id -setting id ({@value})*/
	public static final String GROUP_ID_S = "groupID";
	/** number of hosts in the group -setting id ({@value})*/
	public static final String NROF_HOSTS_S = "nrofHosts";
	/** movement model class -setting id ({@value})*/
	public static final String MOVEMENT_MODEL_S = "movementModel";
	/** router class -setting id ({@value})*/
	public static final String ROUTER_S = "router";
	/** number of interfaces in the group -setting id ({@value})*/
	public static final String NROF_INTERF_S = "nrofInterfaces";
	/** interface name in the group -setting id ({@value})*/
	public static final String INTERFACENAME_S = "interface";
	/** application name in the group -setting id ({@value})*/
	public static final String GAPPNAME_S = "application";

	/** package where to look for movement models */
	private static final String MM_PACKAGE = "movement.";
	/** package where to look for router classes */
	private static final String ROUTING_PACKAGE = "routing.";

	/** package where to look for interface classes */
	private static final String INTTYPE_PACKAGE = "interfaces.";
	
	/** package where to look for application classes */
	private static final String APP_PACKAGE = "applications.";
	
	/** The world instance */
	private World world;
	/** List of hosts in this simulation */
	protected List<DTNHost> hosts;
	/** Name of the simulation */
	private String name;
	/** number of host groups */
	int nrofGroups;
	/** Largest host's radio range */
	private double maxHostRange;
	/** Simulation end time */
	private double endTime;
	/** Update interval of sim time */
	private double updateInterval;
	/** External events queue */
	private EventQueueHandler eqHandler;
	/** Should connections between hosts be simulated */
	private boolean simulateConnections;
	/** Map used for host movement (if any) */
	private SimMap simMap;

	/** Global connection event listeners */
	private List<ConnectionListener> connectionListeners;
	/** Global message event listeners */
	private List<MessageListener> messageListeners;
	/** Global movement event listeners */
	private List<MovementListener> movementListeners;
	/** Global update event listeners */
	private List<UpdateListener> updateListeners;
	/** Global application event listeners */
	private List<ApplicationListener> appListeners;

	/** new 3-dimensional coordinate */
	private int worldSizeX;
	private int worldSizeY;
	private int worldSizeZ;//新增参数，三维坐标

	/**------------------------------   对Message添加的变量       --------------------------------*/
	/** user setting in the sim Cache */
	public static final String EnableCache_s = "EnableCache";
	/** 缓存空间  */
	private HashMap<String,File> FileBuffer;
	/** 文件列表（test）*/
	private HashMap<String,Integer> FileOfHosts;
    /** Record the orbit parameters info for satellite movement model **/
    private HashMap<DTNHost, double[]> orbitInfo;
    


	static {
		DTNSim.registerForReset(SimScenario.class.getCanonicalName());
		reset();
	}
	
	public static void reset() {
		myinstance = null;
	}

	/**
	 * Creates a scenario based on Settings object.
	 */
	public SimScenario() {
		Settings s = new Settings(SCENARIO_NS);
		nrofGroups = s.getInt(NROF_GROUPS_S);
		
		this.name = s.valueFillString(s.getSetting(NAME_S));
		this.endTime = s.getDouble(END_TIME_S);
		this.updateInterval = s.getDouble(UP_INT_S);
		this.simulateConnections = s.getBoolean(SIM_CON_S);

		s.ensurePositiveValue(nrofGroups, NROF_GROUPS_S);
		s.ensurePositiveValue(endTime, END_TIME_S);
		s.ensurePositiveValue(updateInterval, UP_INT_S);

		this.simMap = null;
		this.maxHostRange = 1;

		this.connectionListeners = new ArrayList<ConnectionListener>();
		this.messageListeners = new ArrayList<MessageListener>();
		this.movementListeners = new ArrayList<MovementListener>();
		this.updateListeners = new ArrayList<UpdateListener>();
		this.appListeners = new ArrayList<ApplicationListener>();
		this.eqHandler = new EventQueueHandler();

		/** new 3-dimensional coordinate */
		/* TODO: check size from movement models */
		s.setNameSpace(MovementModel.MOVEMENT_MODEL_NS);
		int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE, 3);//从2维修改为3维
		this.worldSizeX = worldSize[0];
		this.worldSizeY = worldSize[1];
		this.worldSizeZ = worldSize[2];
		
		createHosts();
		
		this.world = new World(hosts, worldSizeX, worldSizeY, updateInterval, 
				updateListeners, simulateConnections, 
				eqHandler.getEventQueues());
	}
	
	/**
	 * Returns the SimScenario instance and creates one if it doesn't exist yet
	 */
	public static SimScenario getInstance() {
		reset();//清空原先初始化的仿真环境
		if (myinstance == null) {
			myinstance = new SimScenario();
		}
		return myinstance;
	}



	/**
	 * Returns the name of the simulation run
	 * @return the name of the simulation run
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Returns true if connections should be simulated
	 * @return true if connections should be simulated (false if not)
	 */
	public boolean simulateConnections() {
		return this.simulateConnections;
	}

	/**
	 * Returns the width of the world
	 * @return the width of the world
	 */
	public int getWorldSizeX() {
		return this.worldSizeX;
	}

	/**
	 * Returns the height of the world
	 * @return the height of the world
	 */
	public int getWorldSizeY() {
		return worldSizeY;
	}

	/**
	 * Returns simulation's end time
	 * @return simulation's end time
	 */
	public double getEndTime() {
		return endTime;
	}

	/**
	 * Returns update interval (simulated seconds) of the simulation
	 * @return update interval (simulated seconds) of the simulation
	 */
	public double getUpdateInterval() {
		return updateInterval;
	}

	/**
	 * Returns how long range the hosts' radios have
	 * @return Range in meters
	 */
	public double getMaxHostRange() {
		return maxHostRange;
	}

	/**
	 * Returns the (external) event queue(s) of this scenario or null if there 
	 * aren't any
	 * @return External event queues in a list or null
	 */
	public List<EventQueue> getExternalEvents() {
		return this.eqHandler.getEventQueues();
	}

	/**
	 * Returns the SimMap this scenario uses, or null if scenario doesn't
	 * use any map
	 * @return SimMap or null if no map is used
	 */
	public SimMap getMap() {
		return this.simMap;
	}

	/**
	 * Adds a new connection listener for all nodes
	 * @param cl The listener
	 */
	public void addConnectionListener(ConnectionListener cl){
		this.connectionListeners.add(cl);
	}

	/**
	 * Adds a new message listener for all nodes
	 * @param ml The listener
	 */
	public void addMessageListener(MessageListener ml){
		this.messageListeners.add(ml);
	}

	/**
	 * Adds a new movement listener for all nodes
	 * @param ml The listener
	 */
	public void addMovementListener(MovementListener ml){
		this.movementListeners.add(ml);
	}

	/**
	 * Adds a new update listener for the world
	 * @param ul The listener
	 */
	public void addUpdateListener(UpdateListener ul) {
		this.updateListeners.add(ul);
	}

	/**
	 * Returns the list of registered update listeners
	 * @return the list of registered update listeners
	 */
	public List<UpdateListener> getUpdateListeners() {
		return this.updateListeners;
	}

	/** 
	 * Adds a new application event listener for all nodes.
	 * @param al The listener
	 */
	public void addApplicationListener(ApplicationListener al) {
		this.appListeners.add(al);
	}
	
	/**
	 * Returns the list of registered application event listeners
	 * @return the list of registered application event listeners
	 */
	public List<ApplicationListener> getApplicationListeners() {
		return this.appListeners;
	}
	
	/**
	 * Returns the list of nodes for this scenario.
	 * @return the list of nodes for this scenario.
	 */
	public List<DTNHost> getHosts() {
		return this.hosts;
	}
	
	/**
	 * Returns the World object of this scenario
	 * @return the World object
	 */
	public World getWorld() {
		return this.world;
	}
	
	/**
	 * 设置仿真更新时间
	 */
	public void setUpdateInterval(double interval){
		this.updateInterval = interval;
	}
	
	/**
	 * 设置仿真结束时间
	 */
	public void setEndTime(double EndTime){
		this.endTime = EndTime;
	}
   /**
     * Creates hosts for the scenario
     */
    protected void createHosts() {
        this.hosts = new ArrayList<DTNHost>();
		List<DTNHost> satelliteHosts = new ArrayList<DTNHost>();//record satellite nodes as DTNHost

        for (int i = 1; i <= nrofGroups; i++) {
        	System.out.println(GROUP_NS + i);
            List<NetworkInterface> mmNetInterfaces =
                    new ArrayList<NetworkInterface>();
            Settings s = new Settings(GROUP_NS + i);
            s.setSecondaryNamespace(GROUP_NS);
            String gid = s.getSetting(GROUP_ID_S);
            int nrofHosts = s.getInt(NROF_HOSTS_S);
            int nrofInterfaces = s.getInt(NROF_INTERF_S);
            int appCount;

            // creates prototypes of MessageRouter and MovementModel
            MovementModel mmProto =
                    (MovementModel) s.createIntializedObject(MM_PACKAGE +
                            s.getSetting(MOVEMENT_MODEL_S));
            MessageRouter mRouterProto =
                    (MessageRouter) s.createIntializedObject(ROUTING_PACKAGE +
                            s.getSetting(ROUTER_S));
            // checks that these values are positive (throws Error if not)
            ensurePositiveValue(nrofHosts, NROF_HOSTS_S);
            ensurePositiveValue(nrofInterfaces, NROF_INTERF_S);

            // setup interfaces
            for (int j = 1; j <= nrofInterfaces; j++) {
                String Intname = s.getSetting(INTERFACENAME_S + j);
                Settings t = new Settings(Intname);
                NetworkInterface mmInterface =
                        (NetworkInterface) t.createIntializedObject(INTTYPE_PACKAGE +
                                t.getSetting(INTTYPE_S));
                
                mmInterface.setClisteners(connectionListeners);
                mmNetInterfaces.add(mmInterface);
            }
            
            // setup applications
            if (s.contains(APPCOUNT_S)) {
                appCount = s.getInt(APPCOUNT_S);
            } else {
                appCount = 0;
            }
            for (int j = 1; j <= appCount; j++) {
                String appname = null;
                Application protoApp = null;
                try {
                    // Get name of the application for this group
                    appname = s.getSetting(GAPPNAME_S + j);
                    // Get settings for the given application
                    Settings t = new Settings(appname);
                    // Load an instance of the application
                    protoApp = (Application) t.createIntializedObject(
                            APP_PACKAGE + t.getSetting(APPTYPE_S));
                    // Set application listeners
                    protoApp.setAppListeners(this.appListeners);
                    // Set the proto application in proto router
                    //mRouterProto.setApplication(protoApp);
                    mRouterProto.addApplication(protoApp);
                } catch (SettingsError se) {
                    // Failed to create an application for this group
                    System.err.println("Failed to setup an application: " + se);
                    System.err.println("Caught at " + se.getStackTrace()[0]);
                    System.exit(-1);
                }
            }

            if (mmProto instanceof MapBasedMovement) {
                this.simMap = ((MapBasedMovement) mmProto).getMap();
            }

            // creates hosts of ith group
            for (int j = 0; j < nrofHosts; j++) {
                ModuleCommunicationBus comBus = new ModuleCommunicationBus();

                // prototypes are given to new DTNHost which replicates
                // new instances of movement model and message router
                DTNHost host = new DTNHost(this.messageListeners,
                        this.movementListeners, gid, mmNetInterfaces, comBus,
                        mmProto, mRouterProto);
                hosts.add(host);

                //For satellite nodes, in order to deploy the satellite network according to
				//constellation, an initialization for satellite orbit should be done
                mmProto = host.getMovementModel();// warning: mmProto will be replicated in the DTNHost
				if (mmProto instanceof SatelliteMovement) {
					satelliteHosts.add(host);
					setSatelliteOrbitInfo(host, mmProto, s, j);
				}
            }
            //内部设置了判断条件，只会在满足SatelliteMovement模块时执行
            setOrbitInfo(s, satelliteHosts);
            setCommunicationNodesProperty(s, hosts);

        }

		/* initialization of satellite link info */
		for (DTNHost h : hosts) {
			if (h.getMovementModel() instanceof SatelliteMovement)//卫星节点
				((SatelliteMovement)h.getMovementModel()).initSatelliteInfo();
			else {
				h.changeHostsList(hosts);//对于不是卫星的节点，也将全局节点列表写入
			}
		}
		//set static clustering for LEO nodes
		if (!((SatelliteMovement)hosts.get(0).getMovementModel()).getDynamicClustering()){
			//System.out.println("dynamic false!");
			((SatelliteMovement)hosts.get(0).getMovementModel()).getSatelliteLinkInfo().initStaticClustering();
		}

        // Set the multiThread label according to user's setting
        DTNHost.setMultiThread();
        // decide whether to use cache function
        this.CacheEnalbe();
        
    }
    /**
     * set some nodes in the same orbit plane as communication nodes
     */
	public void setCommunicationNodesProperty(Settings s, List<DTNHost> hosts){
		//只对卫星节点进行设置，其它组的节点不管
		if (!s.getSetting(MOVEMENT_MODEL_S).contains("SatelliteMovement"))
			return;

		//设置通信卫星节点
		int nrofCommunicationNodesInEachPlane = s.getInt("nrofCommunicationNodesInEachPlane");
		//轨道平面信息
		int nrofLEO = ((SatelliteMovement)hosts.get(0).getMovementModel()).getTotalNrofLEOSatellites();   
		int nrofPlanes = ((SatelliteMovement)hosts.get(0).getMovementModel()).getTotalNrofLEOPlanes();
    	int nrofLEOInOnePlane = nrofLEO/nrofPlanes;
    	
    	HashMap<DTNHost, Integer> CommunicationNodesList = new HashMap<DTNHost, Integer>();
    	//轨道平面内均匀设置通信节点
    	int interval = nrofLEOInOnePlane/nrofCommunicationNodesInEachPlane;
    	
    	//对每一个轨道平面
    	for (int i = 0; i < nrofPlanes; i++){
    		for (int number = 0 + i*nrofLEOInOnePlane; number < (i+1)*nrofLEOInOnePlane; number+= interval){
    			hosts.get(number).getRouter().CommunicationSatellitesLabel = true;
    			CommunicationNodesList.put(hosts.get(number), i);
    		}
    	}
    	for (DTNHost h : hosts){
    		h.getRouter().CommunicationNodesList = new HashMap<DTNHost, Integer>(CommunicationNodesList); 
    	}		
	}

    /**
     * Makes sure that a value is positive
     *
     * @param value       Value to check
     * @param settingName Name of the setting (for error's message)
     * @throws SettingsError if the value was not positive
     */
    private void ensurePositiveValue(double value, String settingName) {
        if (value < 0) {
            throw new SettingsError("Negative value (" + value +
                    ") not accepted for setting " + settingName);
        }
    }

    /**
     * Set the orbit information into the movement model
     * @param mmProto
     * @param s
     * @param satelliteNum
     */
    public void setSatelliteOrbitInfo(DTNHost host, MovementModel mmProto, Settings s, int satelliteNum) {
		//TODO
		int nrofLEO = s.getInt("nrofLEO");
        int nrofLEOPlanes = s.getInt("nrofLEOPlanes");	//total number of orbit planes in the constellation
        
        //multi-layer satellite networks
        if(s.getBoolean("EnableGEO") == true){
        	int nrofGEO = s.getInt("nrofGEO");
			int nrofGEOPlanes = s.getInt("nrofGEOPlane");
			int nrofMEO = s.getInt("nrofMEO");
            int nrofMEOPlanes = s.getInt("nrofMEOPlane");

			int nrofSatellites = nrofGEO + nrofLEO + nrofMEO; //total number of satellite nodes

        	// generate LEO nodes first, then generate MEO nodes
            if (satelliteNum < nrofLEO){
                //Set LEO orbit parameters
                CalculateOrbitInfo(host, mmProto, s, satelliteNum, nrofLEO, nrofLEOPlanes, "LEO");
                //Set other orbit information
                ((SatelliteMovement) mmProto).setOrbit(nrofLEO, nrofLEOPlanes, 
                		satelliteNum/nrofLEOPlanes, satelliteNum - (nrofLEO/nrofLEOPlanes)*(nrofLEOPlanes - 1));
                // set the satellite parameters
				
            } 
            else if(satelliteNum < (nrofLEO + nrofMEO)){
            	int MEONum = satelliteNum - nrofLEO + 1;
                //Set MEO orbit parameters
                CalculateOrbitInfo(host, mmProto, s, MEONum, nrofMEO, nrofMEOPlanes, "MEO");
             	//Set other orbit information
                ((SatelliteMovement) mmProto).setOrbit(nrofLEO, nrofLEOPlanes, 
                		MEONum/nrofMEOPlanes, MEONum - (nrofMEO/nrofMEOPlanes)*(nrofMEOPlanes - 1));
            }
            else {
            	int GEONum = satelliteNum - nrofLEO - nrofMEO + 1;
                //Set GEO orbit parameters
                CalculateOrbitInfo(host, mmProto, s, GEONum, nrofGEO, nrofGEOPlanes, "GEO");
             	//Set other orbit information
                ((SatelliteMovement) mmProto).setOrbit(nrofLEO, nrofLEOPlanes, 
                		GEONum/nrofGEOPlanes, GEONum - (nrofMEO/nrofGEOPlanes)*(nrofGEOPlanes - 1));
            }
        }
        else if (s.getBoolean("EnableMEO") == true){
        	
            int nrofMEO = s.getInt("nrofMEO");
            int nrofMEOPlanes = s.getInt("nrofMEOPlane");

			int nrofSatellites = nrofLEO + nrofMEO; //total number of satellite nodes

            // generate LEO nodes first, then generate MEO nodes
            if (satelliteNum < nrofLEO){
                //Set LEO orbit parameters
                CalculateOrbitInfo(host, mmProto, s, satelliteNum, nrofLEO, nrofLEOPlanes, "LEO");
                //Set other orbit information
                ((SatelliteMovement) mmProto).setOrbit(nrofLEO, nrofLEOPlanes, 
                		satelliteNum/nrofLEOPlanes, satelliteNum - (nrofLEO/nrofLEOPlanes)*(nrofLEOPlanes - 1));
                // set the satellite parameters
				
            }
            else{
            	int MEONum = satelliteNum - nrofLEO + 1;
                //Set MEO orbit parameters
                CalculateOrbitInfo(host, mmProto, s, MEONum, nrofMEO, nrofMEOPlanes, "MEO");
              //Set other orbit information
                ((SatelliteMovement) mmProto).setOrbit(nrofLEO, nrofLEOPlanes, 
                		MEONum/nrofMEOPlanes, MEONum - (nrofMEO/nrofMEOPlanes)*(nrofMEOPlanes - 1));
            }
        }
        // only LEO satellite networks
        else{
        	CalculateOrbitInfo(host, mmProto, s, satelliteNum, nrofLEO, nrofLEOPlanes, "LEO");
        	//Set other orbit information
            ((SatelliteMovement) mmProto).setOrbit(nrofLEO, nrofLEOPlanes,
            		satelliteNum/nrofLEOPlanes, satelliteNum-(nrofLEO/nrofLEOPlanes)*(satelliteNum - 1));
        }
    }
    /**
     * Calculate the orbit parameter according to their satellite type
     * @param host
     * @param mmProto
     * @param s
     * @param satelliteNum
     * @param nrofHosts
     * @param nrofPlanes
     * @param type
     */
    public void CalculateOrbitInfo(DTNHost host, MovementModel mmProto, 
    		Settings s, int satelliteNum, int nrofHosts, int nrofPlanes, String type){
       switch(type){
       case "LEO":{
       	if (s.getSetting(MOVEMENT_MODEL_S).contains("SatelliteMovement")) {
            double[] orbitParameters = generateOrbitParameters(s, "LEO", satelliteNum, nrofHosts, nrofPlanes);
            ((SatelliteMovement) mmProto).setOrbitParameters(orbitParameters);
            
            /** test for the orbit parameters*/
            host.SetSatelliteParametersTest(orbitParameters);
            /** test for the orbit parameters*/
            
            if (orbitInfo == null)
                orbitInfo = new HashMap<DTNHost, double[]>();
            orbitInfo.put(host, orbitParameters);//record orbit parameters info
        }
       	break;
       }
       case "MEO":{
          	if (s.getSetting(MOVEMENT_MODEL_S).contains("SatelliteMovement")) {
                double[] orbitParameters = generateOrbitParameters(s, "MEO", satelliteNum, nrofHosts, nrofPlanes);
                ((SatelliteMovement) mmProto).setOrbitParameters(orbitParameters);

                /** test for the orbit parameters*/
                host.SetSatelliteParametersTest(orbitParameters);
                /** test for the orbit parameters*/
                
                if (orbitInfo == null)
                    orbitInfo = new HashMap<DTNHost, double[]>();
                orbitInfo.put(host, orbitParameters);//record orbit parameters info
            }
          	break;
       	}
       // 针对三层卫星网络，GEO卫星参数计算！
       case "GEO":{
         	if (s.getSetting(MOVEMENT_MODEL_S).contains("SatelliteMovement")) {
                double[] orbitParameters = generateOrbitParameters(s, "GEO", satelliteNum, nrofHosts, nrofPlanes);
                ((SatelliteMovement) mmProto).setOrbitParameters(orbitParameters);

                /** test for the orbit parameters*/
                host.SetSatelliteParametersTest(orbitParameters);
                /** test for the orbit parameters*/
                
                if (orbitInfo == null)
                    orbitInfo = new HashMap<DTNHost, double[]>();
                orbitInfo.put(host, orbitParameters);//record orbit parameters info
            }
          	break;
       }
       
       
       }

    }
    /**
     * Set orbit-info library of each satellite node in the movement model
     *
     * @param s
     */
    public void setOrbitInfo(Settings s, List<DTNHost> satelliteHosts) {
        for (DTNHost host : satelliteHosts) {
            if (s.getSetting(MOVEMENT_MODEL_S).contains("SatelliteMovement")) {
                MovementModel mmProto = host.getMovementModel();
                ((SatelliteMovement) mmProto).setOrbitInfo(orbitInfo, hosts);
            }
        }
    }

    public double[] generateOrbitParameters(Settings s, String type, 
    							int m, int NROF_SATELLITES, int NROF_PLANE){
    	//assigned constellation type, e.g., Walker star or Walker delta
    	String constellationType = s.getSetting("Constellation");

    	//generates different type constellation, for LEO or MEO
    	switch(type){
	    	case "LEO":{
	    		switch(constellationType){
		    		case "WalkerStar":{ // 极轨道
		    			return initialLEOWalkerStarParameters(s, m, NROF_SATELLITES, NROF_PLANE);
		    		}
		    		case "WalkerDelta":{ // 倾斜轨道
		    			return initialLEOWalkerDeltaParameters(s, m, NROF_SATELLITES, NROF_PLANE);
		    		}
	    		}

	    			
	    	}
	    	case "MEO":{
	    		switch(constellationType){
		    		case "WalkerStar":{	
		    			return initialMEOWalkerStarParameters(s, m, NROF_SATELLITES, NROF_PLANE);
		    		}
		    		case "WalkerDelta":{
		    			return initialMEOWalkerDeltaParameters(s, m, NROF_SATELLITES, NROF_PLANE);
		    		}
	    		}
	    		
	    	}
	    	case "GEO":{
	    		switch(constellationType){
	    		case "WalkerStar":{	
	    			return initialGEOWalkerStarParameters(s, m, NROF_SATELLITES, NROF_PLANE);
	    		}
	    		case "WalkerDelta":{
	    			return initialGEOWalkerDeltaParameters(s, m, NROF_SATELLITES, NROF_PLANE);
	    		}
    		}
	    	}
    	}
    	throw new SimError("Satellite Orbit Parameters Generation fails !");
    }
    /**
     * Generate orbit parameters of satellite in the constellation
     *
     * @param m: the number of satellite
     * @param NROF_SATELLITES
     * @param NROF_PLANE
     * @return
     */
    public double[] initialLEOWalkerDeltaParameters(Settings s, int m, int NROF_SATELLITES, int NROF_PLANE) {
        double[] parameters = new double[7];

        double radius;							//半径
        double eccentricity;					//离心率
        double orbitPlaneAngle;					//轨道面倾角
        if (s.contains("LEO_OrbitPlaneAngle") == false)
            orbitPlaneAngle = 60;
        else
            orbitPlaneAngle = s.getDouble("LEO_OrbitPlaneAngle");

        if (s.contains("LEO_Eccentricity") == false)
            eccentricity = 0;
        else
            eccentricity = s.getDouble("LEO_Eccentricity");

        if (s.contains("LEO_Radius") == false)
        /**地球半径为6371km**/
            radius = 6371 + 780;							//单位是km
        else
            radius = 6371 + s.getDouble("LEO_Radius");
        
        //int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//总节点数
        //int NROF_PLANE = 3;//轨道平面数
        int NROF_S_EACHPLANE = NROF_SATELLITES / NROF_PLANE;//每个轨道平面上的节点数
        
        parameters[0] = radius;
        parameters[1] = eccentricity;		// 0.1偏心率，影响较大,e=c/a
        parameters[2] = orbitPlaneAngle;	// 轨道倾角
//        parameters[3] = (360 / NROF_S_EACHPLANE) * (Math.floor(m / NROF_S_EACHPLANE) + 1);//m从0开始
//        parameters[4] = 0;
        
		parameters[3]= (360/NROF_PLANE)*(m/NROF_S_EACHPLANE);			// (升交点赤经)
		System.out.println(m+"  "+NROF_S_EACHPLANE);
		parameters[4]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) 
						+ (360/NROF_SATELLITES)*(m/NROF_S_EACHPLANE); 	// (近地点幅角)
		
        
//        if ((Math.floor(m / NROF_S_EACHPLANE) + 1) % 2 == 1)
//            parameters[5] = (360 / NROF_S_EACHPLANE) * (m - Math.floor(m / NROF_S_EACHPLANE) * NROF_S_EACHPLANE);
//        else
//            parameters[5] = (360 / NROF_S_EACHPLANE) * (m - Math.floor(m / NROF_S_EACHPLANE) * NROF_S_EACHPLANE + 0.5);
		parameters[5]= 0.0;

        System.out.println("LEOWalkerStarParameters:"+m + "  " + parameters[0] + 
        		"  " + parameters[1]+ "    "+parameters[2]+"   "
        					+parameters[3] + "  " + parameters[4] + "  " + parameters[5]);
        //nrofPlane = m/NROF_S_EACHPLANE + 1;//卫星所属轨道平面编号
        //nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//卫星在轨道平面内的编号
        
        parameters[6] = 1;					// '1' indicates LEO satellite
        
        return parameters;
    }
    /**
     * Generate orbit parameters of MEO satellite in the constellation
     * @param m
     * @return
     */
	public double[] initialMEOWalkerStarParameters(Settings s, int m, int NROF_MEOSATELLITES, int nrofMEOPlane){
		double[] parameters = new double[7];

		double MEOradius;//MEO轨道半径
		double eccentricity;
		double orbitPlaneAngle;//轨道面倾角
		
		if (s.contains("MEO_OrbitPlaneAngle") == false)
			orbitPlaneAngle = 86.4;
		else
			orbitPlaneAngle = s.getDouble("MEO_OrbitPlaneAngle");
		
//		System.out.println("test the MEO angle: "+orbitPlaneAngle);
		
		if (s.contains("MEO_Eccentricity") == false)
			eccentricity = 0;
		else
			eccentricity = s.getDouble("MEO_Eccentricity");
		
		if (s.contains("MEO_Radius") == false)
			/**地球半径为6371km**/
			MEOradius = 6371 + 2000;//单位是km;
		else
			MEOradius = 6371 + s.getDouble("MEO_Radius");
		/**MEO轨道平面数**/
		if (s.contains("nrofMEOPlane") == false)
			nrofMEOPlane = 3;
		else
			nrofMEOPlane = s.getInt("nrofMEOPlane");
		/**MEO节点个数**/
		if (s.contains("nrofMEO") == false)
			NROF_MEOSATELLITES = 3;
		else
			NROF_MEOSATELLITES = s.getInt("nrofMEO");
		//int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//总节点数
		//int NROF_PLANE = 3;//轨道平面数
		
		int NROF_S_EACHPLANE = NROF_MEOSATELLITES/nrofMEOPlane;//每个轨道平面上的节点数
		
		parameters[0]= MEOradius;
		parameters[1]= eccentricity;//0.1偏心率，影响较大,e=c/a
		parameters[2]= orbitPlaneAngle;
		parameters[3]= (360 / NROF_S_EACHPLANE) * (Math.floor(m / NROF_S_EACHPLANE) + 1);//m从0开始
		parameters[4]= 0.0;
        if ((Math.floor(m / NROF_S_EACHPLANE) + 1) % 2 == 1)
            parameters[5] = (360 / NROF_S_EACHPLANE) * (m - Math.floor(m / NROF_S_EACHPLANE) * NROF_S_EACHPLANE);
        else
            parameters[5] = (360 / NROF_S_EACHPLANE) * (m - Math.floor(m / NROF_S_EACHPLANE) * NROF_S_EACHPLANE + 0.5);
		
        System.out.println("MEOWalkerStarParameters"+m + "  " + parameters[0] + 
        		"  " + parameters[1]+ "    "+parameters[2]+"   "
        					+parameters[3] + "  " + parameters[4] + "  " + parameters[5]);
		//nrofPlane = m/NROF_S_EACHPLANE + 1;//卫星所属轨道平面编号
		//nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//卫星在轨道平面内的编号
		
        parameters[6] = 2;// '2' indicates LEO satellite
        
		return parameters;
	}
	
    /**
     * Generate orbit parameters of MEO satellite in the constellation
     * @param m
     * @return
     */
	public double[] initialGEOWalkerStarParameters(Settings s, int m, int NROF_GEOSATELLITES, int nrofGEOPlane){
		double[] parameters = new double[7];

		double GEOradius;//GEO轨道半径
		double eccentricity;
		double orbitPlaneAngle;//轨道面倾角
		
		if (s.contains("GEO_OrbitPlaneAngle") == false)
			orbitPlaneAngle = 86.4;
		else
			orbitPlaneAngle = s.getDouble("GEO_OrbitPlaneAngle");
		
		System.out.println("test the GEO angle: "+orbitPlaneAngle);
		
		if (s.contains("GEO_Eccentricity") == false)
			eccentricity = 0;
		else
			eccentricity = s.getDouble("GEO_Eccentricity");
		
		if (s.contains("GEO_Radius") == false)
			/**地球半径为6371km**/
			GEOradius = 6371 + 2000;//单位是km;
		else
			GEOradius = 6371 + s.getDouble("GEO_Radius");
		/**GEO轨道平面数**/
		if (s.contains("nrofGEOPlane") == false)
			nrofGEOPlane = 3;
		else
			nrofGEOPlane = s.getInt("nrofGEOPlane");
		/**GEO节点个数**/
		if (s.contains("nrofGEO") == false)
			NROF_GEOSATELLITES = 3;
		else
			NROF_GEOSATELLITES = s.getInt("nrofGEO");
		//int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//总节点数
		//int NROF_PLANE = 3;//轨道平面数
		
		int NROF_S_EACHPLANE = NROF_GEOSATELLITES/nrofGEOPlane;//每个轨道平面上的节点数
		
		parameters[0]= GEOradius;
		parameters[1]= eccentricity;//0.1偏心率，影响较大,e=c/a
		parameters[2]= orbitPlaneAngle;
		parameters[3]= (360 / NROF_S_EACHPLANE) * (Math.floor(m / NROF_S_EACHPLANE) + 1);//m从0开始
		parameters[4]= 0.0;
        if ((Math.floor(m / NROF_S_EACHPLANE) + 1) % 2 == 1)
            parameters[5] = (360 / NROF_S_EACHPLANE) * (m - Math.floor(m / NROF_S_EACHPLANE) * NROF_S_EACHPLANE);
        else
            parameters[5] = (360 / NROF_S_EACHPLANE) * (m - Math.floor(m / NROF_S_EACHPLANE) * NROF_S_EACHPLANE + 0.5);
		
        System.out.println("GEOWalkerStarParameters"+m + "  " + parameters[0] + 
        		"  " + parameters[1]+ "    "+parameters[2]+"   "
        					+parameters[3] + "  " + parameters[4] + "  " + parameters[5]);
		//nrofPlane = m/NROF_S_EACHPLANE + 1;//卫星所属轨道平面编号
		//nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//卫星在轨道平面内的编号
		
        parameters[6] = 3;// '3' indicates GEO satellite
        
		return parameters;
	}
	
	
	public double[] initialLEOWalkerStarParameters(Settings s, int m, int NROF_SATELLITES, int NROF_PLANE){
        double[] parameters = new double[7];

        double radius;//半径
        double eccentricity;//离心率
        double orbitPlaneAngle;//轨道面倾角
        if (s.contains("LEO_OrbitPlaneAngle") == false)
            orbitPlaneAngle = 86.4;
        else
            orbitPlaneAngle = s.getDouble("LEO_OrbitPlaneAngle");

        if (s.contains("LEO_Eccentricity") == false)
            eccentricity = 0;
        else
            eccentricity = s.getDouble("LEO_Eccentricity");

        if (s.contains("LEO_Radius") == false)
        /**地球半径为6371km**/
            radius = 6371 + 780;//单位是km
        else
            radius = 6371 + s.getDouble("LEO_Radius");
        //int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//总节点数
        //int NROF_PLANE = 3;//轨道平面数
        int NROF_S_EACHPLANE = NROF_SATELLITES / NROF_PLANE;//每个轨道平面上的节点数
		
		Random random = new Random();
		parameters[0]= radius;
		parameters[1]= eccentricity;//0.1偏心率，影响较大,e=c/a
		parameters[2]= orbitPlaneAngle;//轨道倾角
		parameters[3]= (360/NROF_S_EACHPLANE)*(m/NROF_S_EACHPLANE);
		
		parameters[4]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) + (360/NROF_SATELLITES)*(m/NROF_S_EACHPLANE);
		parameters[5]= 0.0;
		
//        System.out.println("LEOWalkerDeltaParameters"+m + "  " + parameters[0] + 
//        		"  " + parameters[1]+ "    "+parameters[2]+"   "
//        					+parameters[3] + "  " + parameters[4] + "  " + parameters[5]);

		
		parameters[6] = 1;// '1' indicates LEO satellite
		
		return parameters;
	}
	
	public double[] initialMEOWalkerDeltaParameters(Settings s, int m, int NROF_MEOSATELLITES, int nrofMEOPlane){
		double[] parameters = new double[7];

		double MEOradius;//MEO轨道半径
		double eccentricity;
		double orbitPlaneAngle;//轨道面倾角
		
		if (s.contains("MEO_OrbitPlaneAngle") == false)
			orbitPlaneAngle = 86.4;
		else
			orbitPlaneAngle = s.getDouble("MEO_OrbitPlaneAngle");
		
		if (s.contains("MEO_Eccentricity") == false)
			eccentricity = 0;
		else
			eccentricity = s.getDouble("MEO_Eccentricity");
		
		if (s.contains("MEO_Radius") == false)
			/**地球半径为6371km**/
			MEOradius = 6371 + 2000;//单位是km;
		else
			MEOradius = 6371 + s.getDouble("MEO_Radius");
		/**MEO轨道平面数**/
		if (s.contains("nrofMEOPlane") == false)
			nrofMEOPlane = 3;
		else
			nrofMEOPlane = s.getInt("nrofMEOPlane");
		/**MEO节点个数**/
		if (s.contains("nrofMEO") == false)
			NROF_MEOSATELLITES = 3;
		else
			NROF_MEOSATELLITES = s.getInt("nrofMEO");

		//int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//总节点数
		//int NROF_PLANE = 3;//轨道平面数
		int NROF_S_EACHPLANE = NROF_MEOSATELLITES/nrofMEOPlane;//每个轨道平面上的节点数
		
		Random random = new Random();
		m = m - 1;
		parameters[0]= MEOradius;
		parameters[1]= eccentricity;//0.1偏心率，影响较大,e=c/a
		parameters[2]= orbitPlaneAngle;
		parameters[3]= (360/nrofMEOPlane)*((m-1)/NROF_S_EACHPLANE);
		System.out.println(m+"  "+NROF_S_EACHPLANE);
		parameters[4]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) + (360/NROF_MEOSATELLITES)*(m/NROF_S_EACHPLANE);
		parameters[5]= 0.0;
		
        System.out.println("MEOWalkerDeltaParameters"+m + "  " + parameters[0] + 
        		"  " + parameters[1]+ "    "+parameters[2]+"   "
        					+parameters[3] + "  " + parameters[4] + "  " + parameters[5]);
		//nrofPlane = m/NROF_S_EACHPLANE + 1;//卫星所属轨道平面编号
		//nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//卫星在轨道平面内的编号
		
		parameters[6] = 2;// '2' indicates MEO satellite
		
		return parameters;
	}

	/**
	 * 计算GEO卫星参数
	 * @param s
	 * @param m
	 * @param NROF_GEOSATELLITES
	 * @param nrofGEOPlane
	 * @return
	 */
	public double[] initialGEOWalkerDeltaParameters(Settings s, int m, int NROF_GEOSATELLITES, int nrofGEOPlane){
		double[] parameters = new double[7];

		double GEOradius;//MEO轨道半径
		double eccentricity;
		double orbitPlaneAngle;//轨道面倾角
		
		if (s.contains("GEO_OrbitPlaneAngle") == false)
			orbitPlaneAngle = 86.4;
		else
			orbitPlaneAngle = s.getDouble("GEO_OrbitPlaneAngle");
		
		if (s.contains("GEO_Eccentricity") == false)
			eccentricity = 0;
		else
			eccentricity = s.getDouble("GEO_Eccentricity");
		
		if (s.contains("GEO_Radius") == false)
			/**地球半径为6371km**/
			GEOradius = 6371 + 2000;//单位是km;
		else
			GEOradius = 6371 + s.getDouble("GEO_Radius");
		/** GEO轨道平面数**/
		if (s.contains("nrofGEOPlane") == false)
			nrofGEOPlane = 3;
		else
			nrofGEOPlane = s.getInt("nrofGEOPlane");
		/** GEO节点个数**/
		if (s.contains("nrofGEO") == false)
			NROF_GEOSATELLITES = 3;
		else
			NROF_GEOSATELLITES = s.getInt("nrofGEO");

		//int NROF_SATELLITES = s.getInt(NROF_HOSTS_S);//总节点数
		//int NROF_PLANE = 3;//轨道平面数
		int NROF_S_EACHPLANE = NROF_GEOSATELLITES/nrofGEOPlane;//每个轨道平面上的节点数
		
		Random random = new Random();
		m = m - 1;					// 与卫星序号有关，类比LEO从0开始计算！
		parameters[0]= GEOradius;
		parameters[1]= eccentricity;//0.1偏心率，影响较大,e=c/a
		parameters[2]= orbitPlaneAngle;
		parameters[3]= (360/nrofGEOPlane)*((m-1)/NROF_S_EACHPLANE);
		System.out.println(m+"  "+NROF_S_EACHPLANE);
		parameters[4]= (360/NROF_S_EACHPLANE)*((m-(m/NROF_S_EACHPLANE)*NROF_S_EACHPLANE) - 1) + (360/NROF_GEOSATELLITES)*(m/NROF_S_EACHPLANE);
		parameters[5]= 0.0;
		
        System.out.println("GEOWalkerDeltaParameters"+m + "  " + parameters[0] + 
        		"  " + parameters[1]+ "    "+parameters[2]+"   "+ parameters[3]
        		+ "  " + parameters[4] + "  " + parameters[5]);
		//nrofPlane = m/NROF_S_EACHPLANE + 1;//卫星所属轨道平面编号
		//nrofSatelliteINPlane = m - (nrofPlane - 1) * NROF_S_EACHPLANE;//卫星在轨道平面内的编号
		
		parameters[6] = 3;// '3' indicates GEO satellite
		
		return parameters;
	}
	
	/**
	 * Decide whether to use the cache function
	 * @param
	 */
	public void CacheEnalbe(){
		Settings settings = new Settings(DTNSim.USERSETTINGNAME_S);	 // read the setting parameters
		String cacheEnable = settings.getSetting(EnableCache_s); // decide whether to enable the cache function
		
		if (cacheEnable.indexOf("true") >= 0) {
			Settings ss = new Settings(GROUP_NS + 1);					// 每一个主机组有一个配置对象，具体的可能和命名空间有关
			ss.setSecondaryNamespace(GROUP_NS);
			int num = ss.getInt(NROF_HOSTS_S);
			int nrofFile = ss.getInt(DTNSim.nrofFile_s);						// default设定的文件数目
			
			this.FileOfHosts = new HashMap<String, Integer>(); 			// （添加）一个文件列表

			for (int j = 0; j < num * nrofGroups; j++) {
				CacheRouter cacherouter = new CacheRouter(ss);
				FileBuffer = new HashMap<String, File>(); 				// 定义每个节点的缓存区
				this.hosts.get(j).setFileBuffer(FileBuffer);
				this.hosts.get(j).setRouter(cacherouter); 				// 为每个节点设置缓存路由
			}

			for (int m = 0; m < nrofFile; m++) {
				File newFile = new File(m, num * nrofGroups);					// 这里先得到随机的host
				this.FileOfHosts.put(newFile.getId(), newFile.getFromAddressID());
				for (int i = 0; i < num * nrofGroups; i++) {				// 往对应节点的缓存空间中放入文件
					if (i == newFile.getFromAddressID()) {
						this.hosts.get(i).getFileBuffer().put(newFile.getId(), newFile);
					}
				}
			}

			// 还要对每个节点都放入全局的文件表，测试得到初始化后每个节点存在这张表
			for (int j = 0; j < num * nrofGroups; j++) {
				this.hosts.get(j).setFiles(this.FileOfHosts); // 对每个节点设置总体的这张表
			}
		}
		else assert false : "the setting of EnableCache error!";
	}
	
	/**
	 * 初始化计算每个轨道平面内的所有节点，并放入NetworkInterace中存储下来，便于在建立链路时判断，直接拒绝不允许的Connection建立，减少仿真器开销
	 * @param hosts
	 */
//	public void setallowToConnectNodesInLEOPlane(List<DTNHost> hosts){
//		//轨道平面信息
//		int nrofLEO = ((SatelliteMovement)hosts.get(0).getMovementModel()).getTotalNrofLEOSatellites();   
//		int nrofPlanes = ((SatelliteMovement)hosts.get(0).getMovementModel()).getTotalNrofLEOPlanes();
//    	int nrofLEOInOnePlane = nrofLEO/nrofPlanes;
//    	
//    	HashMap<Integer, List<DTNHost>> map = new HashMap<Integer, List<DTNHost>>();
//    	/**计算每一个节点的轨道内节点**/
//		for (DTNHost h : hosts){
//			int seriesNumberOfLEOPlane = h.getAddress()/nrofLEOInOnePlane + 1;
//			//判断这个轨道平面是否计算过了
//			if (!map.containsKey(seriesNumberOfLEOPlane)){
//			    int startNumber = nrofLEOInOnePlane * (seriesNumberOfLEOPlane - 1);//此轨道平面内的节点，起始编号
//			    int endNumber = nrofLEOInOnePlane * seriesNumberOfLEOPlane - 1;//此轨道平面内的节点，结尾编号
//			        List<DTNHost> allHostsInSamePlane = new ArrayList<DTNHost>();
//			        for (DTNHost host : getHosts()){
//			            if (host.getAddress() >= startNumber && host.getAddress()<= endNumber){
//			                allHostsInSamePlane.add(host);//同一个轨道内的相邻节点
//			            }
//			        }
//			        map.put(seriesNumberOfLEOPlane, allHostsInSamePlane);
//			}
//			//写入接口类中存储下来
//			for (NetworkInterface i : h.getInterfaces()){
//				//在LEO平面上，两类允许连接的节点列表
//				i.allowToConnectNodesInLEOPlane = new ArrayList<DTNHost>();
//				i.allowToConnectNodesInLEOPlane.addAll(map.get(seriesNumberOfLEOPlane));
//				i.allowToConnectNodesInLEOPlane.addAll(i.getHost().getRouter().CommunicationNodesList.keySet());
//			}	
//		}
//	}
}