/*
 * Copyright 2016 University of Science and Technology of China , Infonet Lab
 * Written by LiJian.
 */
package interfaces;

import core.*;
import movement.SatelliteMovement;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * A simple Network Interface that provides a constant bit-rate service, where
 * one transmission can be on at a time.
 */
public class SatelliteWithChannelModelInterface extends NetworkInterface {

	private SatellitetoGroundChannelModel channelModel;
	private Collection<NetworkInterface> interfaces;

	/** indicates the interface type, i.e., radio or laser*/
	public static final String interfaceType = "RadioInterface";
	/** dynamic clustering by MEO or static clustering by MEO */
	private static boolean dynamicClustering;
	/** allConnected or clustering */
	private static String mode;

	/**
	 * Reads the interface settings from the Settings file
	 */
	public SatelliteWithChannelModelInterface(Settings s)	{
		super(s);
		Settings s1 = new Settings("Interface");
		dynamicClustering = s1.getBoolean("DynamicClustering");
		Settings s2 = new Settings(DTNSim.USERSETTINGNAME_S);
		mode = s2.getSetting(DTNSim.ROUTERMODENAME_S);

		//to simulate random status of wireless link
		channelModel = new SatellitetoGroundChannelModel(
				s1.getDouble(DTNSim.TRANSMITTING_POWER), s1.getDouble(DTNSim.TRANSMITTING_FREQUENCY), s1.getDouble(DTNSim.BANDWIDTH));
	}

	/**
	 * Copy constructor
	 * @param ni the copied network interface object
	 */
	public SatelliteWithChannelModelInterface(SatelliteWithChannelModelInterface ni) {
		super(ni);
		this.mode = ni.mode;
	}

	public NetworkInterface replicate()	{
		return new SatelliteWithChannelModelInterface(this);
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
				&& (this != anotherInterface)
				&& (this.interfaceType == anotherInterface.getInterfaceType())) {
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

	/**
	 * Independent calculation process in each node, which is used
	 * in multi-thread method.
	 */
	public Collection<NetworkInterface> multiThreadUpdate(){
		if (optimizer == null) {
			return null; /* nothing to do */
		}

		// First break the old ones
		optimizer.updateLocation(this);

		this.interfaces =
				optimizer.getNearInterfaces(this);
		return interfaces;
	}

	/**
	 * Updates the state of current connections (i.e. tears down connections
	 * that are out of range and creates new ones).
	 */
	public void update() {
		//update the satellite link info
		List<DTNHost> allowConnectedList = 
				((SatelliteMovement)this.getHost().getMovementModel()).updateSatelliteLinkInfo();
		
		if (!this.getHost().multiThread){
			if (optimizer == null) {
				return; /* nothing to do */
			}
			
			// First break the old ones
			optimizer.updateLocation(this);
		}

		for (int i=0; i<this.connections.size(); ) {
			Connection con = this.connections.get(i);
			NetworkInterface anotherInterface = con.getOtherInterface(this);

			// all connections should be up at this stage
			assert con.isUp() : "Connection " + con + " was down!";

			if (!isWithinRange(anotherInterface)) {//更新节点位置后，检查之前维护的连接是否会因为太远而断掉
				disconnect(con,anotherInterface);
				connections.remove(i);
			}
			else {
					i++;
			}
		}

		switch (mode) { 
		case "AllConnected":{
			if (!this.getHost().multiThread) {
				// Then find new possible connections
				interfaces = optimizer.getNearInterfaces(this);
			}
			for (NetworkInterface i : interfaces) {
				connect(i);
			}
			break;
		}
		case "Cluster":{
			if (!this.getHost().multiThread) {
				// Then find new possible connections
				interfaces = optimizer.getNearInterfaces(this);
			}		
			
			for (NetworkInterface i : interfaces) {	
				/*检查是否处在允许建链的列表当中，否则不允许建立链路*/
				boolean allowConnection = false;
				switch(this.getHost().getSatelliteType()){
				/*如果在范围内的这个节点既不是同一平面内的，又不是通讯节点，就不进行连接，节省开销**/
					case "LEO":{						
						//只用LEO通信节点才允许和MEO层建立链路
						if (allowConnectedList.contains(i.getHost()))
							allowConnection = true;//即进行连接
						break;
					}
					case "MEO":{
						//MEO只允许和LEO通信节点通信和GEO层建立链路
						if (allowConnectedList.contains(i.getHost()))
							allowConnection = true;//即进行连接
						break;
					}
					case "GEO":{
						if (i.getHost().getSatelliteType().contains("MEO")){
							allowConnection = true;
							break;
						}
						if (allowConnectedList.contains(i.getHost()))
							allowConnection = true;//即进行连接
						break;
					}
				}
				
				if (allowConnection){//不被置位，才进行连接
					connect(i);
				}
			}
			break;
		}
		}

	}

	@Override
	public int getTransmitSpeed() {
		return this.transmitSpeed;
	}

	/**
	 * Returns the transmit speed of this physical layer according to
	 * the channel model
	 * @return the transmit speed
	 */
	public int getTransmitSpeed(DTNHost from, DTNHost to){
		if (from == this.getHost())
			return getCurrentChannelStatus().get(to).intValue();
		return getCurrentChannelStatus().get(from).intValue();
	}

	/**
	 * should be updated once by DTNHost router in each update function
	 * @param model
	 * @param distance
	 */
	public void updateLinkState(String model, DTNHost otherNode, double distance){
		this.channelModel.updateLinkState(model, otherNode, distance);
	}

	/**
	 * Return current channel status in terms of Signal-to-Noise Ratio (SNR)
	 * will not change the current channel status
	 * @return the current channel capacity (aka speed, bit/s) in each time slot
	 */
	public HashMap<DTNHost, Double> getCurrentChannelStatus(){
		return this.channelModel.getCurrentChannelStatus();
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
	
	/** return the type of this interface */
	@Override
	public String getInterfaceType(){
		return this.interfaceType;
	}
}
