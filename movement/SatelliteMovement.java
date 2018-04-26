/*
 * Copyright 2016 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import routing.SatelliteInterLinkInfo;
import satellite_orbit.SatelliteOrbit;
import core.Coord;
import core.DTNHost;
import core.Settings;
import core.SimError;

public class SatelliteMovement extends MovementModel {
    /**
     * six orbit parameters
     */
    private double a = 8000.; // sma in km
    private double e = 0.1; // eccentricity
    private double i = 15; // inclination in degrees
    private double raan = 0.0; // right ascension of ascending node in degrees
    private double w = 0.0; // argument of perigee in degrees
    private double ta = 0.0; // true anomaly in degrees
    /**
     * record orbit-info of all satellite nodes in the network
     */
    private HashMap<DTNHost, double[]> orbitInfo;
    /** to create the entity of satellite orbit calculation model */
    private SatelliteOrbit satelliteOrbit;
    /** total LEO satellites */
    private int LEOtotalSatellites;
    /** total LEO orbit planes */
    private int LEOtotalPlane;
    /** number of orbit plane that this satellite belongs */
    private int nrofPlane;
    /** indicate which satellite in its orbit plane */
    private int nrofSatelliteINPlane;
    /** all satellite nodes in the network */
    private List<DTNHost> hosts = new ArrayList<DTNHost>();
    /** indicates satellite type: LEO, MEO or GEO */
    private String satelliteType;
    
	/** Container for generic message properties. Note that all values
	 * stored in the properties should be immutable because only a shallow
	 * copy of the properties is made when replicating messages */
	private Map<String, Object> properties;//设置卫星的自定义属性
	
	private SatelliteInterLinkInfo satelliteLinkInfo;

	/** dynamic clustering by MEO or static clustering by MEO */
	private static boolean dynamicClustering;
	
    public SatelliteMovement(Settings settings) {
        super(settings);
        Settings s0 = new Settings("Interface");
		dynamicClustering = s0.getBoolean("DynamicClustering");
    }

    protected SatelliteMovement(SatelliteMovement rwp) {
        super(rwp);
    }
    /**
     * 在接口建立连接的时候调用，确定每个节点的连接建立
     */
    public List<DTNHost> updateSatelliteLinkInfo(){
    	//同层之间允许建立的链路规则(4条链路)
    	List<DTNHost> allowConnectedListInSameLayer = new ArrayList<DTNHost>();
    	switch(this.getSatelliteType()){
    	case "LEO":{
    		if (this.satelliteLinkInfo.getLEOci() == null)
    			break;
    		allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getLEOci().getAllowConnectLEOHostsInLEOSamePlane());
    		//allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getLEOci().updateAllowConnectLEOHostsInNeighborPlane());
    		//只有通信节点才能和MEO节点进行通信
    		if (this.getHost().getRouter().CommunicationSatellitesLabel){
    			List<DTNHost> MEOLists = new ArrayList<DTNHost>();
    			if (getDynamicClustering())
    				MEOLists.addAll(this.satelliteLinkInfo.findAllMEOHosts());
    			else{
    				//添加静态分簇指定的管理节点，由initStaticClustering()初始化，在SimScenario.java中调用
    				MEOLists.addAll(this.getSatelliteLinkInfo().getLEOci().getManageHosts());
    				//System.out.println(this.getHost()+" mh: "+this.getSatelliteLinkInfo().getLEOci().getManageHosts());
    			}
    			if (!MEOLists.isEmpty())
    				allowConnectedListInSameLayer.addAll(MEOLists);
    		}
    		break;
    	}
    	case "MEO":{
    		if (this.satelliteLinkInfo.getMEOci() == null)
    			break;
    		allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getMEOci().updateAllowConnectMEOHostsInNeighborPlane());
    		allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getMEOci().getAllowConnectMEOHostsInSamePlane());
    		//添加静态分簇指定的LEO簇内节点
//        	if (!dynamicClustering){
//        		allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getMEOci().getClusterList());
//        		System.out.println(this.getHost()+" cluster "+this.satelliteLinkInfo.getMEOci().getClusterList());
//        	}
    		break;
    	}
    	case "GEO":{
    		if (this.satelliteLinkInfo.getGEOci() == null)
    			break;
    		allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getGEOci().updateAllowConnectGEOHostsInNeighborPlane());
    		allowConnectedListInSameLayer.addAll(this.satelliteLinkInfo.getGEOci().getAllowConnectGEOHostsInSamePlane());
    		break;
    	}
    	}
    	return allowConnectedListInSameLayer;
    }
    /**
     * enable dynamic clustering or static clustering for MEO
     * @return
     */
    public boolean getDynamicClustering(){
    	return this.dynamicClustering;
    }
    /**
     * set all satellite hosts list
     *
     * @param hosts
     */
    public void setHostsList(List<DTNHost> hosts) {
        this.hosts = hosts;
    }
    
    /**
     * set orbit during the initialization
     *
     * @param LEOtotalSatellites
     * @param LEOtotalPlane
     * @param nrofPlane
     * @param nrofSatelliteInPlane
     */
    public void setOrbit(int LEOtotalSatellites, int LEOtotalPlane, int nrofPlane, int nrofSatelliteInPlane) {
    	if (LEOtotalSatellites <= 0 || LEOtotalPlane <= 0)
    		throw new SimError("Setting Paramater Error");
        this.LEOtotalSatellites = LEOtotalSatellites;// total LEO satellites
        this.LEOtotalPlane = LEOtotalPlane;// total LEO orbit planes   
        this.nrofPlane = nrofPlane;// number of orbit plane that this satellite belongs
        this.nrofSatelliteINPlane = nrofSatelliteInPlane;// indicate which satellite in its orbit plane
    }
    /**
     * @return SatelliteInterLinkInfo for getting the link information
     */
    public SatelliteInterLinkInfo getSatelliteLinkInfo(){
    	return this.satelliteLinkInfo;
    }
    /**
     * @return total LEO orbit plane numbers
     */
    public int getTotalNrofLEOPlanes(){
    	return LEOtotalPlane;// total LEO orbit planes
    }
    /**
     * @return total LEO satellite numbers
     */
    public int getTotalNrofLEOSatellites(){
    	return LEOtotalSatellites;
    }
    /**
     * set six orbit parameters
     *
     * @param parameters
     */
    public void setOrbitParameters(double[] parameters) {   	
        assert parameters.length >= 6 : "orbit parameters initialization error";
        
        this.a = parameters[0]; // sma in km
        this.e = parameters[1]; // eccentricity
        this.i = parameters[2]; // inclination in degrees
        this.raan = parameters[3]; // right ascension of ascending node in degrees
        this.w = parameters[4]; // argument of perigee in degrees
        this.ta = parameters[5]; // true anomaly in degrees

        double[] orbitParameters = new double[6];
        for (int j = 0; j < 6; j++) {
            orbitParameters[j] = parameters[j];
        }
        
        if (parameters.length > 6){
            //set satellite type, i.e., LEO,MEO or GEO
            switch((int)parameters[6]){
            	case 1:{
            		satelliteType = "LEO";
            		break;
            	}
            	case 2:{
            		satelliteType = "MEO";
            		break;
            	}
            	case 3:{
            		satelliteType = "GEO";
            		break;
            	}
            }
        }
        
        this.satelliteOrbit = new SatelliteOrbit(orbitParameters);      
    }
    /**
     * get six orbit parameters
     * @return
     */
    public double[] getOrbitParameters(){
    	double[] op = new double[]{this.a, this.e, this.i, this.raan, this.w, this.ta};
    	return op;		
    }
    /**
     * be careful with the initialization time
     */
    public void initSatelliteInfo(){
    	//此初始化函数，在SimScenario中被调用
    	if (this.satelliteLinkInfo != null)
    		return;
    	this.satelliteLinkInfo = new SatelliteInterLinkInfo(this.getHost(), satelliteType);
    }
    /**
     * get satellite coordinate in specific time
     *
     * @param time
     * @return
     */
    public double[] getSatelliteCoordinate(double time) {
        double[][] coordinate = new double[1][3];
        double[] xyz = new double[3];

        Settings s = new Settings("MovementModel");
        int worldSize[] = s.getCsvInts("worldSize");

        coordinate = satelliteOrbit.getSatelliteCoordinate(time);
        /**ONE中的距离单位为meter，但是JAT中的轨道半径单位为km，因此在得到的坐标中应该*1000进行转换**/
//		xyz[0] = (coordinate[0][0]*1000 + worldSize/2);//坐標軸平移
//		xyz[1] = (coordinate[0][1]*1000 + worldSize/2);
//		xyz[2] = (coordinate[0][2]*1000 + worldSize/2);
        /**ONE中的距离单位为meter，但是JAT中的轨道半径单位为km，因此此做统一缩放，将ONE中的距离单位也视作km，同时坐标平移量保持为world大小的一半**/
        xyz[0] = (coordinate[0][0] + worldSize[0] / 2);// move the coordinate axis
        xyz[1] = (coordinate[0][1] + worldSize[0] / 2);
        xyz[2] = (coordinate[0][2] + worldSize[0] / 2);

        return xyz;
    }

    /**
     * calculate the orbit coordinate according to the orbit parameters
     *
     * @param parameters
     * @param time
     * @return
     */
    public double[] calculateOrbitCoordinate(double[] parameters, double time) {
        double[][] coordinate = new double[1][3];
        double[] xyz = new double[3];
        SatelliteOrbit so = new SatelliteOrbit(parameters);
        coordinate = so.getSatelliteCoordinate(time);
        
        Settings s = new Settings("MovementModel");
        int worldSize[] = s.getCsvInts("worldSize");
        /**ONE中的距离单位为meter，但是JAT中的轨道半径单位为km，因此此做统一缩放，将ONE中的距离单位也视作km，同时坐标平移量保持为world大小的一半**/
        xyz[0] = (coordinate[0][0] + worldSize[0] / 2);// move the coordinate axis
        xyz[1] = (coordinate[0][1] + worldSize[0] / 2);
        xyz[2] = (coordinate[0][2] + worldSize[0] / 2);

        return xyz;
    }

    /**
     * @return number of orbit plane
     */
    public int getNrofPlane() {
        return this.nrofPlane;
    }

    /**
     * @return number of satellite in its orbit plane
     */
    public int getNrofSatelliteINPlane() {
        return this.nrofSatelliteINPlane;
    }

    /**
     * 
     * @return orbit period
     */
    public double getPeriod(){
    	return this.satelliteOrbit.getPeriod();
    }
    /**
     * Returns a possible (random) placement for a host
     *
     * @return Random position on the map
     */
    @Override
    public Coord getInitialLocation() {
        assert rng != null : "MovementModel not initialized!";
        Coord c = randomCoord();

        return c;
    }

    @Override
    public Path getPath() {
        Path p;
        p = new Path(generateSpeed());

        return p;
    }

    @Override
    public SatelliteMovement replicate() {
        return new SatelliteMovement(this);
    }

    protected Coord randomCoord() {
        return new Coord(rng.nextDouble() * getMaxX(),
                rng.nextDouble() * getMaxY());
    }
    /**
     * Initialization entrance, set orbit-info during the initialization
     *
     * @param orbitInfo
     * @param hosts
     */
    public void setOrbitInfo(HashMap<DTNHost, double[]> orbitInfo, List<DTNHost> hosts) {
        this.orbitInfo = orbitInfo;
        this.hosts = hosts;
        //Initialize orbit parameters of this satellite host
        setOrbitParameters(orbitInfo.get(this.getHost()));
        //set all satellite hosts list
        setHostsList(new ArrayList<DTNHost>(orbitInfo.keySet()));
    }

    /**
     * @return all orbit-info of all satellite nodes in the network
     */
    public HashMap<DTNHost, double[]> getOrbitInfo() {
        return this.orbitInfo;
    }

    /**
     * @return all satellite nodes set
     */
    public Set<DTNHost> getHosts() {
        return orbitInfo.keySet();
    }

    /**
     * @param host
     * @param time
     * @return a satellite's location in specific time
     */
    public Coord getCoordinate(DTNHost host, double time) {
        double[] p = orbitInfo.get(host);
        double[] location = calculateOrbitCoordinate(p, time);

        return new Coord(location[0], location[1], location[2]);
    }
    /**
     * @return satellite type
     */
    public String getSatelliteType(){
    	return satelliteType;
    }
    
	/**
	 * Adds a generic property for this satellite. The key can be any string but 
	 * it should be such that no other class accidently uses the same value.
	 * The value can be any object but it's good idea to store only immutable
	 * objects because when message is replicated, only a shallow copy of the
	 * properties is made.  
	 * @param key The key which is used to lookup the value
	 * @param value The value to store
	 * @throws SimError if the message already has a value for the given key
	 */
	public void addProperty(String key, Object value) throws SimError {
		if (this.properties != null && this.properties.containsKey(key)) {
			/* check to prevent accidental name space collisions */
			throw new SimError("SatelliteMovement property " + this + " already contains value " + 
					"for a key " + key);
		}
		
		this.updateProperty(key, value);
	}
	
	/**
	 * Returns an object that was stored to this property using the given
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
}
