package interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import core.CBRConnection;
import core.Connection;
import core.DTNHost;
import core.Neighbors;
import core.NetworkInterface;
import core.Settings;
import core.SimClock;

/**
 * A simple Network Interface that provides a constant bit-rate service, where
 * one transmission can be on at a time.
 */
public class SimpleSatelliteInterface  extends NetworkInterface {
	
	//新增
	/** router mode in the sim -setting id ({@value})*/
	public static final String USERSETTINGNAME_S = "userSetting";
	/** router mode in the sim -setting id ({@value})*/
	public static final String ROUTERMODENAME_S = "routerMode";
	public static final String DIJSKTRA_S = "dijsktra";
	public static final String SIMPLECONNECTIVITY_S = "simpleConnectivity";

	/**
	 * Reads the interface settings from the Settings file
	 */
	public SimpleSatelliteInterface(Settings s)	{
		super(s);
	}
		
	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SimpleSatelliteInterface(SimpleSatelliteInterface ni) {
		super(ni);
	}

	public NetworkInterface replicate()	{
		return new SimpleSatelliteInterface(this);
	}

	/**
	 * Tries to connect this host to another host. The other host must be
	 * active and within range of this host for the connection to succeed. 
	 * @param anotherInterface The interface to connect to
	 */
	public void connect(NetworkInterface anotherInterface) {
		if (isScanning()  
				&& anotherInterface.getHost().isRadioActive() 
				&& isWithinRange(anotherInterface) 
				&& !isConnected(anotherInterface)
				&& (this != anotherInterface)) {
			// new contact within range
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed();//连接两端的连接速率由较小的一个决定
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);//会访问连接双方的host节点，把这个新生成的连接con加入连接列表中
		}
	}

	/*新增函数*/
	public ConnectivityOptimizer predictionUpdate(){
		if (optimizer == null) {
			return null; /* nothing to do */
		}
		optimizer.updateLocation(this);
		return optimizer;
		
	}
	/*新增函数*/
	/**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range and creates new ones).
	 */
	public void update() {
		
		if (optimizer == null) {
			return; /* nothing to do */
		}
		
		// First break the old ones
		optimizer.updateLocation(this);
		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {//更新节点位置后，检查之前维护的连接是否会因为太远而断掉
				disconnect(con,anotherInterface);
				connections.remove(i);
				
				//neighbors.removeNeighbor(con.getOtherNode(this.getHost()));//在断掉连接的同时移除在邻居列表里的邻居节点，新增！！！
			}
			else {
				i++;
			}
		}
		Settings s = new Settings(USERSETTINGNAME_S);
		int mode = s.getInt(ROUTERMODENAME_S);//从配置文件中读取路由模式
		switch(mode){
		case 1:
			// Then find new possible connections
			Collection<NetworkInterface> interfaces =//无需调用optimizer.getNearInterfaces(this)来获取邻居节点了，现在连接的建立全部放在world。java当中进行
				optimizer.getNearInterfaces(this);
			for (NetworkInterface i : interfaces) {
				connect(i);
				//neighbors.addNeighbor(i.getHost());
			}
			break;
		case 2 :
			break;
		/*case 3://分簇模式
			Collection<NetworkInterface> interfaces_ =//无需调用optimizer.getNearInterfaces(this)来获取邻居节点了，现在连接的建立全部放在world。java当中进行
				optimizer.getNearInterfaces(this, clusterHosts, hostsOfGEO);
			for (NetworkInterface i : interfaces_) {
				connect(i);
			}
			break;*/
		}

		//System.out.println(this.getHost()+"  interface  "+SimClock.getTime()+" this time  "+this.connections);
		//this.getHost().getNeighbors().updateNeighbors(this.getHost(), this.connections);//更新邻居节点数据库
		
	}

	
	/** 
	 * Creates a connection to another host. This method does not do any checks
	 * on whether the other node is in range or active 
	 * @param anotherInterface The interface to create the connection to
	 */
	public void createConnection(NetworkInterface anotherInterface) {
		if (!isConnected(anotherInterface) && (this != anotherInterface)) {    			
			// connection speed is the lower one of the two speeds 
			int conSpeed = anotherInterface.getTransmitSpeed();
			if (conSpeed > this.transmitSpeed) {
				conSpeed = this.transmitSpeed; 
			}

			Connection con = new CBRConnection(this.host, this, 
					anotherInterface.getHost(), anotherInterface, conSpeed);
			connect(con,anotherInterface);
		}
	}

	/**
	 * Returns a string representation of the object.
	 * @return a string representation of the object.
	 */
	public String toString() {
		return "SatelliteLaserInterface " + super.toString();
	}

}
