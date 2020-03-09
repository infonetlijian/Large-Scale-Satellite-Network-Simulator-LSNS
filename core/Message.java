package core;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Cache.File;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File Message.java
 * Package Name:core
 * Description: A message that is created at a node or passed between nodes.
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class Message implements Comparable<Message> {
	/** Time-to-live (TTL) as seconds -setting id ({@value}). Boolean valued.
	 * If set to true, the TTL is interpreted as seconds instead of minutes. 
	 * Default=false. */
	public static final String TTL_SECONDS_S = "Scenario.ttlSeconds";
	private static boolean ttlAsSeconds = false;
	
	/** Value for infinite TTL of message */
	public static final int INFINITE_TTL = -1;
	private DTNHost from;
	private DTNHost to;
	/** Identifier of the message */
	private String id;
	/** Size of the message (bytes) */
	private int size;
	/** List of nodes this message has passed */
	private List<DTNHost> path; 
	/** Next unique identifier to be given */
	private static int nextUniqueId;
	/** Unique ID of this message */
	private int uniqueId;
	/** The time this message was received */
	private double timeReceived;
	/** The time when this message was created */
	private double timeCreated;
	/** Initial TTL of the message */
	private int initTtl;

	/** if a response to this message is required, this is the size of the 
	 * response message (or 0 if no response is requested) */
	private int responseSize;
	/** if this message is a response message, this is set to the request msg*/
	private Message requestMsg;
	
	/** Container for generic message properties. Note that all values
	 * stored in the properties should be immutable because only a shallow
	 * copy of the properties is made when replicating messages */
	private Map<String, Object> properties;
	
	/** Application ID of the application that created the message */
	private String	appID;
	
	//新增属性
	private DTNHost sourceHost;//原地址 
	private DTNHost destinationHost;//目的地址
	
	public void setDestinationHost(DTNHost des){
		this.destinationHost = des;
	}
	public void setSourceHost(DTNHost des){
		this.sourceHost = des;
	}
	public DTNHost getSourceHost(){
		return this.sourceHost;
	}
	public DTNHost getDestinationHost(){
		return this.destinationHost;
	}
	
	/**------------------------------   对Message添加的变量       --------------------------------*/
	
	/** 添加参量，保留消息的初始ID，在处理响应包，控制包时会用到*/
	private String  initMsgID;	
	/** 添加与file相关参量，直接用file类的实例来代替正在传输的文件。*/
	private  String filename;
	/**　文件中携带chunk的ID*/
	private  String chunkID;
	/** 消息中携带的文件 */
	private  File data;	
	/** bitMap用于对chunkID进行映射    */
	private ArrayList<Integer> bitMap = new ArrayList<Integer>();
	/** 用于判断包的类型 */
	public final static String SelectLabel = "PacketType";

	/**------------------------------   对Message添加的变量       --------------------------------*/
	
	public void setTo(DTNHost to) {
		this.to = to;
	}

	public void setSize(int size) {
		this.size = size;
	}
	
	static {
		reset();
		DTNSim.registerForReset(Message.class.getCanonicalName());
	}
	/**
	 * Creates a new Message.
	 * @param from Who the message is (originally) from
	 * @param to Who the message is (originally) to
	 * @param id Message identifier (must be unique for message but
	 * 	will be the same for all replicates of the message)
	 * @param size Size of the message (in bytes)
	 */
//	public Message(DTNHost from, DTNHost to, String id, int size) {
//		this.from = from;
//		this.to = to;//改成上一跳和下一跳的节点地址
//		this.id = id;
//		this.size = size;
//		this.path = new ArrayList<DTNHost>();
//		this.uniqueId = nextUniqueId;
//		
//		this.source = from;//新增属性，初始化语句
//		this.destination = to;
//		
//		this.timeCreated = SimClock.getTime();
//		this.timeReceived = this.timeCreated;
//		this.initTtl = INFINITE_TTL;
//		this.responseSize = 0;
//		this.requestMsg = null;
//		this.properties = null;
//		this.appID = null;
//		
//		Message.nextUniqueId++;
//		addNodeOnPath(from);
//	}
	
	/**
	 * Returns the node this message is originally from
	 * @return the node this message is originally from
	 */
	public DTNHost getFrom() {
		return this.from;
	}

	/**
	 * Returns the node this message is originally to
	 * @return the node this message is originally to
	 */
	public DTNHost getTo() {
		return this.to;
	}

	/**
	 * Returns the ID of the message
	 * @return The message id
	 */
	public String getId() {
		return this.id;
	}
	
	/**
	 * Returns an ID that is unique per message instance 
	 * (different for replicates too)
	 * @return The unique id
	 */
	public int getUniqueId() {
		return this.uniqueId;
	}
	
	/**
	 * Returns the size of the message (in bytes)
	 * @return the size of the message
	 */
	public int getSize() {
		return this.size;
	}

	/**
	 * Adds a new node on the list of nodes this message has passed
	 * @param node The node to add
	 */
	public void addNodeOnPath(DTNHost node) {
		this.path.add(node);
	}
	
	/**
	 * Returns a list of nodes this message has passed so far
	 * @return The list as vector
	 */
	public List<DTNHost> getHops() {
		return this.path;
	}
	
	/**
	 * Returns the amount of hops this message has passed
	 * @return the amount of hops this message has passed
	 */
	public int getHopCount() {
		return this.path.size() -1;
	}
	
	/** 
	 * Returns the time to live (in minutes or seconds, depending on the setting
	 * {@link #TTL_SECONDS_S}) of the message or Integer.MAX_VALUE 
	 * if the TTL is infinite. Returned value can be negative if the TTL has
	 * passed already.
	 * @return The TTL
	 */
	public int getTtl() {
		if (this.initTtl == INFINITE_TTL) {
			return Integer.MAX_VALUE;
		}
		else {
			if (ttlAsSeconds) {
				return (int)(this.initTtl -
						(SimClock.getTime()-this.timeCreated) );				
			} else {
				return (int)( ((this.initTtl * 60) -
						(SimClock.getTime()-this.timeCreated)) /60.0 );
			}
		}
	}
	
	/**
	 * Sets the initial TTL (time-to-live) for this message. The initial
	 * TTL is the TTL when the original message was created. The current TTL
	 * is calculated based on the time of 
	 * @param ttl The time-to-live to set
	 */
	public void setTtl(int ttl) {
		this.initTtl = ttl;
	}
	
	/**
	 * Sets the time when this message was received.
	 * @param time The time to set
	 */
	public void setReceiveTime(double time) {
		this.timeReceived = time;
	}
	
	/**
	 * Returns the time when this message was received
	 * @return The time
	 */
	public double getReceiveTime() {
		return this.timeReceived;
	}
	
	/**
	 * Returns the time when this message was created
	 * @return the time when this message was created
	 */
	public double getCreationTime() {
		return this.timeCreated;
	}
	
	/**
	 * If this message is a response to a request, sets the request message
	 * @param request The request message
	 */
	public void setRequest(Message request) {
		this.requestMsg = request;
	}
	
	/**
	 * Returns the message this message is response to or null if this is not
	 * a response message
	 * @return the message this message is response to
	 */
	public Message getRequest() {
		return this.requestMsg;
	}
	
	/**
	 * Returns true if this message is a response message
	 * @return true if this message is a response message
	 */
	public boolean isResponse() {
		return this.requestMsg != null;
	}
	
	/**
	 * Sets the requested response message's size. If size == 0, no response
	 * is requested (default)
	 * @param size Size of the response message
	 */
	public void setResponseSize(int size) {
		this.responseSize = size;
	}
	
	/**
	 * Returns the size of the requested response message or 0 if no response
	 * is requested.
	 * @return the size of the requested response message
	 */
	public int getResponseSize() {
		return responseSize;
	}
	
	/**
	 * Returns a string representation of the message
	 * @return a string representation of the message
	 */
	public String toString () {
		return id;
	}

	/**
	 * Deep copies message data from other message. If new fields are
	 * introduced to this class, most likely they should be copied here too
	 * (unless done in constructor).
	 * @param m The message where the data is copied
	 */
//	protected void copyFrom(Message m) {
//		this.path = new ArrayList<DTNHost>(m.path);
//		this.timeCreated = m.timeCreated;
//		this.responseSize = m.responseSize;
//		this.requestMsg  = m.requestMsg;
//		this.initTtl = m.initTtl;
//		this.appID = m.appID;
//		
//		if (m.properties != null) {
//			Set<String> keys = m.properties.keySet();
//			for (String key : keys) {
//				updateProperty(key, m.getProperty(key));
//			}
//		}
//	}
	
	/**
	 * Adds a generic property for this message. The key can be any string but 
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
			throw new SimError("Message " + this + " already contains value " + 
					"for a key " + key);
		}
		
		this.updateProperty(key, value);
	}
	
	/**
	 * Returns an object that was stored to this message using the given
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
	/**
	 * Returns a replicate of this message (identical except for the unique id)
	 * @return A replicate of the message
	 */
//	public Message replicate() {
//		Message m = new Message(from, to, id, size);
//		m.copyFrom(this);
//		return m;
//	}
	
	/**
	 * Compares two messages by their ID (alphabetically).
	 * @see String#compareTo(String)
	 */
	public int compareTo(Message m) {
		return toString().compareTo(m.toString());
	}
	
	/**
	 * Resets all static fields to default values
	 */
	public static void reset() {
		nextUniqueId = 0;
		Settings s = new Settings();
		ttlAsSeconds = s.getBoolean(TTL_SECONDS_S, false);
	}

	/**
	 * @return the appID
	 */
	public String getAppID() {
		return appID;
	}

	/**
	 * @param appID the appID to set
	 */
	public void setAppID(String appID) {
		this.appID = appID;
	}
	
	/**------------------------------   对Message添加的函数方法       --------------------------------*/
	
	/** 对bitMap中所有元素置零操作 */
	public void setZeroForBitMap(){
		this.bitMap.clear();
		for(int i=0;i<10;i++)
			bitMap.add(0);
	}
	/** 获取消息初始ID  */
	public String getInitMsgId(){
		return this.initMsgID;
	}
	/** 设置消息初始ID  */
	public void setInitMsgId(String s){
		this.initMsgID = s;
	}
	/** 得到消息携带的bitMap  */
	public ArrayList<Integer> getBitMap(){
		return this.bitMap;
	}
	/** 得到响应函数中正在传输的文件      */
	public File getFile(){
		return this.data;
	}
    /** 对文件名操作      */
	public String getFilename() {
		return filename;
	}
    /** 对文件名设置操作      */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/** 对分片后的内容进行操作  */
	public String getChunkID(){
		return chunkID;
	}
	public void setChunkID(String chunkID){
		this.chunkID=chunkID;		
	}
	/** 对bitmap进行设置   */
	public void setBitMap(ArrayList<Integer> bm){
		this.bitMap = bm;
		
	}
	/** 对消息携带的文件进行设置   */
	public void setFile(File f){
		this.data = f;
	}

	/**　对消息的创建时间与接收时间进行设置*/
	public void setTime(double timeC, double timeR){
		this.timeCreated = timeC;
		this.timeReceived = timeR;
	}
	
	// 这是对原有的函数做了修改，并不完全重写
	
	/**
	 * Creates a new Message.
	 * @param from Who the message is (originally) from
	 * @param to Who the message is (originally) to
	 * @param id Message identifier (must be unique for message but
	 * 	will be the same for all replicates of the message)
	 * @param size Size of the message (in bytes)
	 */
	public Message(DTNHost from, DTNHost to, String id, int size) {			//	消息初始化过程
		this.from = from;
		this.to = to;
		this.id = id;
		this.size = size;
		this.path = new ArrayList<DTNHost>();								
		this.uniqueId = nextUniqueId;  										//	消息id标识
		this.initMsgID = id;
		
		this.timeCreated = SimClock.getTime();
		this.timeReceived = this.timeCreated;
		this.initTtl = INFINITE_TTL;
		this.responseSize = 0;
		this.requestMsg = null;
		this.properties = null;
		this.appID = null;			
		this.setZeroForBitMap();											//	对bitMap初始化
		Message.nextUniqueId++;
		addNodeOnPath(from);
	}
	/**  重写构造函数，当为响应消息有数据存在时，进行调用*/
	public Message(DTNHost from, DTNHost to, String id, int size, File Data) {	//消息初始化过程
		this.from = from;
		this.to = to;
		this.id = id;
		this.size = size;
		this.path = new ArrayList<DTNHost>();								
		this.uniqueId = nextUniqueId;  											//消息id标识
		this.data= new File();
		this.data= this.data.copyFrom(Data);
		this.data.copyData(Data);		
		this.initMsgID = id;
		
		this.timeCreated = SimClock.getTime();
		this.timeReceived = this.timeCreated;
		this.initTtl = INFINITE_TTL;
		this.responseSize = 0;
		this.requestMsg = null;
		this.properties = null;
		this.appID = null;
		Message.nextUniqueId++;
		addNodeOnPath(from);
	}
	/**
	 * Returns a replicate of this message (identical except for the unique id)
	 * @return A replicate of the message
	 */
	public Message replicate() {
		
		if(this.getProperty(SelectLabel) != (Object)1){									// 当消息不为应答消息
			Message m = new Message(from, to, id, size);
			m.copyFrom(this);
			m.filename=this.filename;
			m.bitMap=this.bitMap;
			m.initMsgID = this.initMsgID;			
			return m;
		} else{																			// 当为响应消息时
			Message m = new Message(from, to, id, size, data);			
			m.copyFrom(this);
			m.filename= this.filename;
			m.bitMap=this.bitMap;
			m.initMsgID = this.initMsgID;
			return m;
		}
	}
	/**
	 * Deep copies message data from other message. If new fields are
	 * introduced to this class, most likely they should be copied here too
	 * (unless done in constructor).
	 * @param m The message where the data is copied
	 */
	public void copyFrom(Message m) {
		this.path = new ArrayList<DTNHost>(m.path);
		this.timeCreated = m.timeCreated;
		this.responseSize = m.responseSize;
		this.requestMsg  = m.requestMsg;
		this.appID = m.appID;
		this.initTtl = m.initTtl;						
		this.chunkID = m.chunkID;						//对chunkI进行复制

		if (m.properties != null) {
			Set<String> keys = m.properties.keySet();
			for (String key : keys) {
				updateProperty(key, m.getProperty(key));
			}
		}
	}	
	/**------------------------------   对Message添加的函数方法       --------------------------------*/
	
}
