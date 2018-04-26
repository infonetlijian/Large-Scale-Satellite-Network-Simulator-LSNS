/*
 * copyright 2017 ustc, Infonet
 */
package Cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimClock;
import routing.MessageRouter;

public class CacheRouter extends MessageRouter {

	/** bitMap用于对chunkID进行映射    */
	private ArrayList<Integer> bitMap = new ArrayList<Integer>();
	/**定义一个临时的队列，用于对中继节点得到chunk传输消息存储 */
	protected Queue<Message> tempQueue = new LinkedList<Message>();
	/** 需要定义多维的链表形式，来对数据进行存储 */
	protected HashMap<String,HashMap<String,Message>> MessageHashMap = new HashMap<String,HashMap<String,Message>>();
	/** 用于判断文件是否得到确认，从而决定是否需要重传  */
	private HashMap<String, ArrayList<Object>> judgeForRetransfer = new HashMap<String, ArrayList<Object>>();	
	/** 用于判断重传时间，这里设定为100s */
	protected double time_out = 10;
	/** 用于判断重传次数，初始为0，设定最多重传3次*/
	protected int reTransTimes = 3;
	/**　用于ack包的time_wait时间 */
	protected double time_wait = 20;
	/** 用于应答包的等待时间 time_free */
	protected double time_free = 3.5*time_out;
	/** 用于判断包的类型 */
	public final static String SelectLabel = "PacketType";
	/** 新建一个文件buffer */
	public static final String F_SIZE_S = "filebuffersize";
	/** 文件缓存大小*/
	private int filebuffersize;
	/** Host where this router belongs to */
	private DTNHost host;
	/** 绑定一个消息路由 */
	private MessageRouter router;
	
	/** 几种包的标识ID 请求包*/
	public static final String Request_Msg = "Request_";
	/** 几种包的标识ID 应答包*/
	public static final String Response_Msg = "Response_";
	/** 几种包的标识ID 控制包*/
	public static final String Control_Msg = "Control_";
	/** 几种包的标识ID 控制包的确认包*/
	public static final String Ack2Ctrl_Msg = "Ack2Ctrl_";
	/** 几种包的标识ID 请求包的确认包*/
	public static final String Ack2Request_Msg = "Ack2Request_";
	/** 响应消息前缀 */
	public static final String RESPONSE_PREFIX = "R_";
	/** 判断是否需要随机丢包！*/
	public String DropMessage;

	
	/**
	 * Initializes the router; i.e. sets the host this router is in and
	 * message listeners that need to be informed about message related
	 * events etc.
	 * @param host The host this router is in
	 * @param mListeners The message listeners
	 */
	public void init(DTNHost host, List<MessageListener> mListeners) {
		Settings s = new Settings("userSetting"); 
		DropMessage = s.getSetting("RandomDropMessage"); //从配置文件读取一次是否需要随机丢包
		
		this.host = host;
		this.router = host.getRouter();
	}
	
	public CacheRouter(CacheRouter r) {
		super(r);
		this.filebuffersize = r.filebuffersize;  // 仿写文件缓存大小
	}
	
	public CacheRouter(Settings s) {
		super(s);
		this.filebuffersize = Integer.MAX_VALUE; 	// 对文件缓存大小 仿写
		if (s.contains(F_SIZE_S)) {
			this.filebuffersize = s.getInt(F_SIZE_S);
		}
	}
	
	@Override
	public CacheRouter replicate() {
		return new CacheRouter(this);
	}
	
	/**
	 * Called when a connection's state changes. If energy modeling is enabled,
	 * and a new connection is created to this node, reduces the energy for the
	 * device discovery (scan response) amount
	 * @param @con The connection whose state changed
	 */
	@Override
	public void changedConnection(Connection con) {	
		
	}
	
	/** -------------------------- 对代码的修改  --------------------------- */
	public DTNHost getHost(){
		return this.host;
	}
	
	/** 添加到对应的文件缓冲区内  addToFileBuffer() */       
	protected void addToFileBuffer(Message m, boolean newMessage) {
		if ( m.getResponseSize() ==0){	//从message中取出file类
			File ee = m.getFile();				
			this.getHost().getFileBuffer().put(m.getFilename(), ee); // 放到消息缓冲区FileBuffer中
		}
	}	
	
	/** 添加chunk到对应的chunkBuffer中  	*/
	protected void addToChunkBuffer(Message m){
		if(m.getProperty(SelectLabel)== (Object)1){
			// 判断是否存在对应分片的Hash表，若存在则直接放入，否则新建
			if(this.getHost().getChunkBuffer().containsKey(m.getFilename())){
				this.getHost().getChunkBuffer().get(m.getFilename()).put(m.getChunkID(),m.getFile());
			}	else{
				HashMap<String,File> NewHashMap = new HashMap<String,File>();
				NewHashMap.put(m.getChunkID(),m.getFile());
				this.getHost().getChunkBuffer().put(m.getFilename(), NewHashMap);
			}
		}
	}
	
	/** 对bitMap中所有元素置零操作   */
	public void setZeroForBitMap(){
		this.bitMap.clear();
		for(int i=0;i<10;i++)
			bitMap.add(0);
	}
    /** 得到存放文件的缓存大小filebuffersize */
	public int getFileBufferSize(){
		return this.filebuffersize;
	}
	
	/** 得到当前路由的重传buffer。*/
	public HashMap<String,ArrayList<Object>> getJudgeForRetransfer(){
		return this.judgeForRetransfer;
	}
	
	/** 将刚创建的消息放入到判断是否需要重传buffer中 */
	public void putJudgeForRetransfer(Message m){		
		switch((int) m.getProperty(SelectLabel)){
		case 0:{
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_out);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
//		case 1:{  // 应答包可以不放入表中
//			ArrayList<Object> arraylist = new ArrayList<Object>();
//			arraylist.add(0, m);
//			arraylist.add(1, this.time_free);
//			arraylist.add(2, -1);
//			this.judgeForRetransfer.put("Chunk"+m.getInitMsgId(), arraylist);
//			return;
//		}
		case 2:{
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_out);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
		case 3:{
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_wait);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
		case 4:{	//	三次重传的机会
			ArrayList<Object> arraylist = new ArrayList<Object>();
			arraylist.add(0, m);
			arraylist.add(1, this.time_wait);
			arraylist.add(2, this.reTransTimes);
			this.judgeForRetransfer.put(m.getId(), arraylist);
			return;
		}
		}
	}
	
	/** 更新待确认消息buffer中的消息 */
	public void updateReTransfer(){
		
		/**	这里需要对待确认消息缓存中的消息待确认时间更新；并判断是否到期，到期重传。*/
//		System.out.println("刷新数据量大小："+this.judgeForRetransfer.values().size());
		
		for( ArrayList<Object> reTrans : this.judgeForRetransfer.values()){	
			System.out.println("待刷新表项大小："+this.judgeForRetransfer.size());
			reTrans.set(1, (double)reTrans.get(1)-10*(0.01));
			Message n = (Message)reTrans.get(0);
			String s = n.getId();
			
			if((double)reTrans.get(1)<=0){	//判断生存时间是否到期？
				Message m = (Message)reTrans.get(0);
				switch((int) m.getProperty(SelectLabel)){
					case 0:{
						if(this.getHost().getFileBuffer().containsKey(m.getFilename())==false){	// 如果缓存已有，就不再重发请求消息，没有才发
							if((int)reTrans.get(2)>0){	//判断重传次数是否用完
								Message reqMessage = new Message(m.getFrom(),m.getTo(),m.getId(), m.getResponseSize());
								reqMessage.setInitMsgId(m.getInitMsgId());
								reqMessage.updateProperty(SelectLabel, 0);						//	标识为控制包
								reqMessage.setFilename(m.getFilename());
								reqMessage.setZeroForBitMap();
								reqMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//	更新消息生成时间
								
								this.judgeForRetransfer.get(m.getInitMsgId()).set(1, this.time_out);
								int i = (int) this.judgeForRetransfer.get(m.getInitMsgId()).get(2);
				                this.judgeForRetransfer.get(m.getInitMsgId()).set(2, i-1); 				//	重传次数减少一次
				                this.createNewMessage(reqMessage);
							}
							else{
								this.judgeForRetransfer.remove(m.getInitMsgId());
							}
						}
						else{
							this.judgeForRetransfer.remove(m.getInitMsgId());
						}
						return;
					}
					
					/** 对于应答包的time_free到期，首先判断内存中是否有对应的应答包？ 有的话删了，然后删除待确认消息缓存中的此消息。*/
					case 1:{ 		
						if(this.MessageHashMap.containsKey(n.getFilename())){
							this.MessageHashMap.remove(n.getFilename());
						}
						if(this.getHost().getChunkBuffer().containsKey(n.getFilename())){
							this.getHost().getChunkBuffer().remove(n.getFilename());
						}
						this.judgeForRetransfer.remove("Chunk"+n.getInitMsgId());
					}
					
					case 2:{	 
						
						if((int)reTrans.get(2)>0){	//判断重传次数是否用完，重传控制包
							Message ctrMessage = new Message(m.getFrom(),m.getTo(),
									 Control_Msg + m.getInitMsgId(), m.getResponseSize());
							
							ctrMessage.setInitMsgId(m.getInitMsgId());
							ctrMessage.updateProperty(SelectLabel, 2);	//	标识为控制包
							ctrMessage.setFilename(m.getFilename());
							ctrMessage.setZeroForBitMap();
							ctrMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//	更新消息生成时间
							
							this.judgeForRetransfer.get(Control_Msg+m.getInitMsgId()).set(1, this.time_out);//	刷新重传时间
			                int j = (int) this.judgeForRetransfer.get(Control_Msg + m.getInitMsgId()).get(2);
			                this.judgeForRetransfer.get( Control_Msg + m.getInitMsgId()).set(2, j-1); 		//	重传次数减少一次
			                this.createNewMessage(ctrMessage);
			                System.out.println("重发控制包消息！！！！");
			        		System.out.println("IB成功接收文件："+"  "+this.getHost()+"   "+ctrMessage.getProperty(SelectLabel)+ "  "
			        				+ctrMessage.getFilename()+" "+ctrMessage.getChunkID()+"  "
			        				+ctrMessage.getId()+" "+ctrMessage.getFrom()+"  "+ctrMessage.getTo()
			        				+"  "+"初始消息名称："+"  "+ctrMessage.getInitMsgId()
			        				+" "+"消息创建时间："+"  "+ ctrMessage.getCreationTime()
			        				+"  "+"消息接收时间："+"  "+ ctrMessage.getReceiveTime());
						}
						else{
							this.judgeForRetransfer.remove(Control_Msg + m.getInitMsgId());
						}
						return;
					}
					
					case 3:{		// 对控制包的确认消息
						this.judgeForRetransfer.remove(s);
						return;
					}
					case 4:{		// 对请求包的确认消息：
						this.judgeForRetransfer.remove(s);
						return;
					}
				
				}
			}
		}	
	}

	
	/**
	 * Creates a new message to the router.
	 * @param m The message to create
	 * @return True if the creation succeeded, false if not (e.g.
	 * the message was too big for the buffer)
	 */
	@Override
	public boolean createNewMessage(Message m) {
		if( DropMessage.indexOf("true") >= 0){	// 随机丢包模式，百分之一的概率丢包
			// 针对不同类型的包丢弃！！！！！！
			Random random = new Random();
			int r = random.nextInt(1000);
			if(r!=0){
				m.setTtl(this.msgTtl);
				this.router.addToMessages(m, true);		
				return true;
			} else{
				System.out.println("++++++++++++++++++++创建失败的消息是：" + "  "
						+ this.getHost() + "   " + "消息类型是：" + " "
						+ m.getProperty(SelectLabel) + "  " + m.getFilename() + " "
						+ m.getChunkID() + "  " + m.getId() + " " + m.getFrom()
						+ "  " + m.getTo());
				return false;
			}
		} else{
			m.setTtl(this.msgTtl);
			this.router.addToMessages(m, true);		
			return true;
		}
	}

	/**
	 * 消息到达目的节点，而且是第一次到达
	 * @param aMessage 为对应到达的消息
	 */
	public void DestinationCache(Message aMessage){
		if (aMessage.getProperty(SelectLabel) == (Object) 0) { // 这是一个请求包
			RequestMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 1) { // 这是一个应答包，携带chunk文件
			ResponseMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 2) { // 这是一个控制包
			ControlMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 3) { // 这是对控制包的确认包
			Ack2CtrlMessage_Destination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 4) { // 这是对请求包的确认包
			Ack2RequestMessage_Destination(aMessage);
		}
		
	}

	
	/**
	 * 处理到达目的节点的请求包0
	 * @param aMessage
	 */
	private void RequestMessage_Destination(Message aMessage) {
		/** 如果这个请求包是由于Ack2Request确认包丢失造成的重发，需要time_wait处理 */
		if(this.judgeForRetransfer.containsKey(Ack2Request_Msg + aMessage.getInitMsgId())){
			
			/** 也即是由于Ack2Request确认包丢失造成的重发控制包，这里直接回复确认包即可*/
			Message m = (Message)this.judgeForRetransfer
						.get(Ack2Request_Msg + aMessage.getInitMsgId()).get(0);

			Message ackMessage = new Message(m.getFrom(), m.getTo(),
						Ack2Request_Msg + m.getId(), m.getResponseSize());
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 4);	//标识为控制包
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//更新消息生成时间
			
			this.judgeForRetransfer.get(Ack2Request_Msg + m.getInitMsgId()).set(1, this.time_wait);	//刷新重传时间
            createNewMessage(ackMessage);

		} else{
			/** 接收到请求消息之后，首先进行确认*/
			createAcknowledgeMessage(aMessage);
			if (this.getHost().getFileBufferForFile(aMessage)!=null) {
				/** 对文件进行分片，创建应答包*/
				createResponseMessage(aMessage);
				/** 应答消息发完之后，应该发送一个控制包*/
				createControlMessage(aMessage);	
	        } else {
				System.out.println("当为目的节点时，出现错误，目的节点中没有对应的文件。");
			}
		}
	}
	
	/**
	 * 处理到达目的节点的应答包1
	 * @param aMessage
	 */
	private void ResponseMessage_Destination(Message aMessage) {
		/** 为应答包加上计时器 Time_free,首先判断应答包的计时器在待确认消息中是否存在？ 存在的话更新，若不存在，则新添一个*/
		if (this.judgeForRetransfer.containsKey("Chunk" + aMessage.getInitMsgId())) {
			this.judgeForRetransfer.get("Chunk" + aMessage.getInitMsgId()).set(
					1, this.time_free);
		} else {
			this.putJudgeForRetransfer(aMessage);
		}
		
		/** 往chunkBuffer中放入文件*/
		if (this.getHost().getFileBufferForFile(aMessage) == null) {
			addToChunkBuffer(aMessage);
		}
		
		/** 当消息是由于丢包重发,将中继节点当做目的节点进行处理*/
		if (this.MessageHashMap.containsKey(aMessage.getFilename())) {
			this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getChunkID(), aMessage);
		}
	}
	
	/**
	 * 处理到达目的节点的控制包2 
	 * @param aMessage
	 */
	private void ControlMessage_Destination(Message aMessage) {
		if(this.getHost().getFileBuffer().containsKey(aMessage.getFilename())==false){// 目的节点中不存在文件
			/** 也即是由于ack确认包丢失造成的重发控制包，这里直接回复确认包即可.区别在于此时的chunkBuffer中已经没有了chunk文件。*/
			boolean a = false;
			for(int i=0;i<10;i++){					
				if(this.getHost().getChunkBuffer().containsKey(aMessage.getFilename())){
					if (this.getHost()
							.getChunkBuffer()
							.get(aMessage.getFilename())
							.containsKey(aMessage.getFilename() + "ChunkID" + i))
						a = true;
				} 
				if(a==true) break;
			}
			if(	a==false ){		
				this.NoChunkInChunkBuffer(aMessage);
			} else{
				// 遍历chunkBuffer看chunk是否收齐，回复确认包
				boolean b = true;						//b=1 默认为收齐
				this.setZeroForBitMap();				//对 bitmap置零操作
				for(int i=0;i<10;i++){
					if (this.getHost()
							.getChunkBuffer()
							.get(aMessage.getFilename())
							.containsKey(aMessage.getFilename() + "ChunkID" + i))
						this.bitMap.set(i, 1);
					else 
						b = false;  
				}
				if(b == true){	//  判断收齐的情况下
					if (this.getHost().getFreeFileBufferSize() < 0) {	//判断内存是否满，若满的话删除内存,根据文件接收时间
						this.getHost().makeRoomForNewFile(0);   		//必要时，删除那些最早接收到且不正在传输的消息
						System.out.print("+++++++++++++++++++++		删除成功	 ++++++++++++++++++++"+"\n");
					}						
					//判断文件分片收齐的情况下，放入文件缓存中
					PutIntoFileBuffer(aMessage);
					System.out.println("-------------------------" 
										+ " 目的节点 " + this.getHost() 
										+ " 放入文件：" + aMessage.getFilename()
										+ "--------------------------");
				}
				/** 回复确认包 */
				createAck2CtrlMessage(aMessage);
			}
		}else{
			System.out.println("目的节点中已存在文件："+aMessage.getFilename());
		}
	}
	
	
	/**
	 * 处理对控制包的确认包3，根据bitmap进行处理
	 * @param aMessage
	 */
	private void Ack2CtrlMessage_Destination(Message aMessage) {
		boolean b = true;		//用于判断是否需要再回复一个控制包？默认为不需要。
		File f = this.getHost().getFileBufferForFile(aMessage);				
				
		for(int i=0; i<10; i++){
			File chunk = f.copyFrom(f);
			if(aMessage.getBitMap().get(i)!=1){		
				for(int j=i*10;j<i*10+10;j++){					
					chunk.getData().add(j-i*10,f.getData().get(j));
				}
				Message res = new Message(this.getHost(), aMessage.getFrom(),
										  Response_Msg + aMessage.getInitMsgId()+i,
										  aMessage.getResponseSize(), chunk);
				res.setInitMsgId(aMessage.getInitMsgId());
				res.setResponseSize(0);				
				res.setFilename(aMessage.getFilename());
				res.setChunkID(aMessage.getFilename()+"ChunkID"+i);	
				res.updateProperty(SelectLabel,1);	
				this.createNewMessage(res);
				b = false;
			}	
		}	
		
		if(b==false){	//当b=false，证明有包丢失，此时需要再发送一个控制包
			Message ctrMessage =new Message(this.getHost(),aMessage.getFrom(),
					Control_Msg + aMessage.getId(), aMessage.getResponseSize());
			
			ctrMessage.setInitMsgId(aMessage.getInitMsgId());
			ctrMessage.updateProperty(SelectLabel, 2);			//标识为控制包
			ctrMessage.setFilename(aMessage.getFilename());
			ctrMessage.setZeroForBitMap();
			ctrMessage.setTime(SimClock.getTime()+11*0.01, SimClock.getTime()+11*0.01);	//更新消息生成时间
            this.createNewMessage(ctrMessage);
            
			if(this.judgeForRetransfer.containsKey(Control_Msg+aMessage.getInitMsgId())== true){
				this.judgeForRetransfer.get(Control_Msg+aMessage.getInitMsgId()).set(1, this.time_out);	//刷新重传时间
				int m = (int) this.judgeForRetransfer.get(Control_Msg+aMessage.getInitMsgId()).get(2);
	            this.judgeForRetransfer.get(Control_Msg + aMessage.getInitMsgId()).set(2, m-1); 	//重传次数减少一次
			} //否则不管，相当于三次重传都失败，或者由于超时过久，导致删除！！！
			
        } else{
			this.judgeForRetransfer.remove(Control_Msg+aMessage.getInitMsgId());
		}
	}
	
	
	
	/**
	 * 处理对请求包的确认包4
	 * @param aMessage
	 */
	private void Ack2RequestMessage_Destination(Message aMessage) {
		this.judgeForRetransfer.remove(aMessage.getInitMsgId());	// 删除保留的用于重传的请求消息
	}
	
	/**
	 * 收到控制包2之后，检查ChunkBuffer发现没有Chunk存在，针对可能情况进行处理。
	 * @param aMessage
	 */
	public void NoChunkInChunkBuffer(Message aMessage){
		/**	证明一个文件都没有，是由于对控制包的确认包丢失造成的*/
		if(this.judgeForRetransfer.containsKey(Ack2Ctrl_Msg + aMessage.getInitMsgId())){
			Message m = (Message) this.judgeForRetransfer.get(
					Ack2Ctrl_Msg + aMessage.getInitMsgId()).get(0);
			
			Message ackMessage = new Message(m.getFrom(), m.getTo(),
					Ack2Ctrl_Msg + m.getId(), m.getResponseSize());	
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 3);	//标识为控制包
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//更新消息生成时间					
			this.judgeForRetransfer.get(Ack2Ctrl_Msg + m.getInitMsgId()).set(1, this.time_wait);	//刷新重传时间
            this.createNewMessage(ackMessage);
		}else{
		/** 证明由于重新选路造成的*/
			Message ack2CtrlMessage = new Message(this.getHost(),
					aMessage.getHops().get(aMessage.getHopCount() - 1),
					Ack2Ctrl_Msg + aMessage.getInitMsgId(),
					aMessage.getResponseSize());
			ack2CtrlMessage.setInitMsgId(aMessage.getInitMsgId());
			ack2CtrlMessage.updateProperty(SelectLabel, 3);															
			ack2CtrlMessage.setFilename(aMessage.getFilename());
			ack2CtrlMessage.setZeroForBitMap();
			ack2CtrlMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//更新消息生成时间
			this.putJudgeForRetransfer(ack2CtrlMessage);	//放入重传表中
            this.createNewMessage(ack2CtrlMessage);
            this.router.removeFromMessages(aMessage.getId());
		}
	}
	
	/**
	 * 往节点文件缓存中放入文件
	 */
	public void PutIntoFileBuffer(Message aMessage){
		File NewFile = 	this.getHost().getChunkBuffer()
							.get(aMessage.getFilename())
							.get(aMessage.getFilename() + "ChunkID" + 0);
		for (int i = 1; i < 10; i++) {
			File temp = this.getHost().getChunkBuffer()
							.get(aMessage.getFilename())
							.get(aMessage.getFilename() + "ChunkID" + i);
			ArrayList<Integer> c = temp.getData();
			NewFile.getData().addAll(i * 10, c);
		}
		this.getHost().getChunkBuffer().remove(aMessage.getFilename());				
		NewFile.setInitFile(NewFile);  		
		NewFile.setTimeRequest(SimClock.getTime());

		this.getHost().getFileBuffer().put(aMessage.getFilename(), NewFile);
	}
	
	/**
	 * create response message identified as 1
	 * @param aMessage
	 */
	public void createResponseMessage(Message aMessage){
    	/** 需要在这里加对文件分片的处理，然后再将对应的生成消息放入待发送序列中*/
		this.getHost().getFileBufferForFile(aMessage).setTimeRequest(SimClock.getTime());  	//缓存中文件更新请求时间					
		File f = this.getHost().getFileBufferForFile(aMessage);
		for(int i=0;i<10;i++){						// each file is divided into 10 chunks
			File chunk = f.copyFrom(f);
			for(int j=i*10;j<i*10+10;j++){
				chunk.getData().add(j-i*10,f.getData().get(j));
			}
	
			Message res = new Message(this.getHost(), aMessage.getFrom(),
					Response_Msg + aMessage.getInitMsgId()+i, aMessage.getResponseSize(),chunk);	
			
			res.setInitMsgId(aMessage.getInitMsgId());
			res.setResponseSize(0);					//这一句应该没用
			res.setFilename(aMessage.getFilename());
			res.setChunkID(aMessage.getFilename()+"ChunkID"+i);	//设置chunkID，在message中设置。
			res.updateProperty(SelectLabel,1);	    			//说明这是一个应答包						
			res.setTime(SimClock.getTime()+0.01*(i+1), SimClock.getTime()+0.01*(i+1));
			this.createNewMessage(res);
		}
	}
	
	/**
	 * create Control message identified as 2
	 * @param aMessage
	 */
	public void createControlMessage(Message aMessage){
		Message ctrMessage =new Message(this.getHost(),aMessage.getFrom(),
				Control_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ctrMessage.setInitMsgId(aMessage.getInitMsgId());
		ctrMessage.updateProperty(SelectLabel, 2);		//标识为控制包
		ctrMessage.setFilename(aMessage.getFilename());
		ctrMessage.setZeroForBitMap();
		ctrMessage.setTime(SimClock.getTime()+11*(0.01), SimClock.getTime()+11*(0.01));	              
		this.createNewMessage(ctrMessage);  
		this.putJudgeForRetransfer(ctrMessage);
	}
	
	/**
	 * 收到控制包2之后，生成对应的控制包的确认包3
	 * @param aMessage
	 */
	public void createAck2CtrlMessage(Message aMessage){
		Message ackMessage =new Message(this.getHost(),aMessage.getFrom(),
				Ack2Ctrl_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ackMessage.setInitMsgId(aMessage.getInitMsgId());
		ackMessage.updateProperty(SelectLabel,3);		//说明这是一个确认包
		ackMessage.getBitMap().clear();             	//先清空bitmap
		ackMessage.getBitMap().addAll(this.bitMap);		//回复bitMap
		ackMessage.setFilename(aMessage.getFilename());	
		this.putJudgeForRetransfer(ackMessage);
		System.out.println("目的节点bitmap确认："+"  "+ackMessage.getBitMap()+" "+"消息的初始ID为："
							+ackMessage.getInitMsgId()+"  "+"当前节点为："+this.getHost());
		this.createNewMessage(ackMessage);
	}
	
	/**
	 * create acknowledge message identified as 4
	 * @param aMessage
	 */
	public void createAcknowledgeMessage(Message aMessage){
		// 确认包4是向上一跳进行确认
		Message ackMessage = new Message(this.getHost(), aMessage.getHops()
				.get(aMessage.getHopCount() - 1), Ack2Request_Msg
				+ aMessage.getInitMsgId(), aMessage.getResponseSize());

		ackMessage.setInitMsgId(aMessage.getInitMsgId());
		ackMessage.updateProperty(SelectLabel, 4);			//标识为对请求的确认包
		ackMessage.setFilename(aMessage.getFilename());
		ackMessage.setTime(SimClock.getTime()+0, SimClock.getTime()+0);		
		this.putJudgeForRetransfer(ackMessage);
		this.createNewMessage(ackMessage); 
		
	}
	
	/**
	 * 消息并未到达目的节点，对消息类型做出判断，做出相应操作
	 * @param aMessage 为对应到达的消息
	 */	
	public void NotDestinationCache(Message aMessage) {
		if (aMessage.getProperty(SelectLabel) == (Object) 0) { // 这是一个请求包
			RequestMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 1) { // 这是一个应答包，携带chunk文件
			ResponseMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 2) { // 这是一个控制包
			ControlMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 3) { // 这是对控制包的确认包
			Ack2CtrlMessage_NotDestination(aMessage);
		} 
		else if (aMessage.getProperty(SelectLabel) == (Object) 4) { // 这是对请求包的确认包
			Ack2RequestMessage_NotDestination(aMessage);
		}
	}
	
	/**
	 * 处理到达非目的节点的请求包0
	 * @param aMessage
	 */
	private void RequestMessage_NotDestination(Message aMessage) { 
		if(this.judgeForRetransfer.containsKey(Ack2Request_Msg + aMessage.getInitMsgId())){
			Message m = (Message) this.judgeForRetransfer.get(
					Ack2Request_Msg + aMessage.getInitMsgId()).get(0);
			Message ackMessage = new Message(m.getFrom(), m.getTo(),
					Ack2Request_Msg + m.getId(), m.getResponseSize());
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 4);		//标识为控制包
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//更新消息生成时间				
			this.judgeForRetransfer.get(Ack2Request_Msg + m.getInitMsgId()).set(1, this.time_wait);	//刷新重传时间
            this.createNewMessage(ackMessage);

		} else{
			createAcknowledgeMessage(aMessage);				//对请求包的确认
			this.router.getMessage(aMessage.getId()).setTime(
					SimClock.getTime() + 0.01, SimClock.getTime() + 0.01); // 对创建时间与接收时间进行重新设定
			
			if (this.getHost().getFileBufferForFile(aMessage)!=null) {
				//如果有则直接进行应答
				this.createResponseMessage(aMessage);
				//应答之后发控制包
	            this.createControlMessage(aMessage);
	            this.router.removeFromMessages(aMessage.getId());	
			} else{
//				System.out.println("……………………………………………………………………………………………………不作处理，直接往下一跳转发！！！");
			}
		}
	}
	
	/**
	 * 处理到达非目的节点的应答包1
	 * @param aMessage
	 */
	private void ResponseMessage_NotDestination(Message aMessage) {
		/**
		 * 为应答包加上计时器 Time_free
		 * 首先判断应答包的计时器在待确认消息中是否存在？ 存在的话更新，若不存在，则新添一个
		 */
		if(this.judgeForRetransfer.containsKey("Chunk"+aMessage.getInitMsgId())){		
			this.judgeForRetransfer.get("Chunk"+aMessage.getInitMsgId()).set(1, this.time_free);
		} else{
			this.putJudgeForRetransfer(aMessage);
		}
		
		if (this.getHost().getFileBufferForFile(aMessage)==null){		
			/**
			 * 在添加到缓存中之前，需要先对缓存做判断，是否满？ 若未满，直接加入缓存；若满，则先对缓存中内容进行删除，再加入缓存
			 * 不用队列对信息进行存储，统一用一个多维的HashMap进行存储。
			 */
			addToChunkBuffer(aMessage);				
			if(MessageHashMap.containsKey(aMessage.getFilename())){
				this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getChunkID(), aMessage);
			}	else{
				HashMap<String,Message> NewHashMap = new HashMap<String,Message>();
				NewHashMap.put(aMessage.getChunkID(), aMessage);
				this.MessageHashMap.put(aMessage.getFilename(), NewHashMap);
			}
			this.router.removeFromMessages(aMessage.getId());	
		}else{	
			/** 
			 * 中继中已经存在了文件，原因是在应答时路径与请求时的路径不一致 
			 * 为了保证逐跳确认，需要在此节点保留这些文件，然后由此中继节点往下一跳应答
			 */
			addToChunkBuffer(aMessage);
			if(MessageHashMap.containsKey(aMessage.getFilename())){
				this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getChunkID(), aMessage);
			}	else{
				HashMap<String,Message> NewHashMap = new HashMap<String,Message>();
				NewHashMap.put(aMessage.getChunkID(), aMessage);
				this.MessageHashMap.put(aMessage.getFilename(), NewHashMap);
			}
			this.router.removeFromMessages(aMessage.getId());
		}
	}
	
	/**
	 * 处理到达非目的节点的控制包2 
	 * @param aMessage
	 */
	private void ControlMessage_NotDestination(Message aMessage) {
		if(this.getHost().getFileBuffer().containsKey(aMessage.getFilename())==false){
			/** 针对控制包重传的情况：1、由于控制包丢失；2、由于控制包重新选路   */
			boolean a = false;
			for(int i=0;i<10;i++){					
				if(this.getHost().getChunkBuffer().containsKey(aMessage.getFilename())){
					if (this.getHost()
							.getChunkBuffer()
							.get(aMessage.getFilename())
							.containsKey(aMessage.getFilename()+"ChunkID"+i))
						a = true;
				}
				if (a == true) break;
			}
			
			if(	a==false ){						
				/**证明一个文件都没有,向上一跳请求文件。根据消息aMessage生成*/
				RetransCtrlMsg_notDestination(aMessage);
			} else {
				if(MessageHashMap.containsKey(aMessage.getFilename())){	// 这一步为了逐跳传输
					this.MessageHashMap.get(aMessage.getFilename()).put(aMessage.getId(), aMessage);
				} else{
					HashMap<String,Message> NewHashMap = new HashMap<String,Message>();
					NewHashMap.put(aMessage.getChunkID(), aMessage);
					this.MessageHashMap.put(aMessage.getId(), NewHashMap);
				}
	        	this.router.removeFromMessages(aMessage.getId());
				
	        	/** 遍历chunkBuffer看chunk是否收齐，b=1 默认为收齐*/
	        	boolean b = true;						
	        	this.setZeroForBitMap();
	        	for(int i=0;i<10;i++){
					if(this.getHost().getChunkBuffer().get(aMessage.getFilename()).containsKey(aMessage.getFilename()+"ChunkID"+i))
	        			this.bitMap.set(i, 1);
	        		else 
	        			b = false;
	        	}
	        	if(b == true){
	            	/** 往里面放之前，需要先判断内存是否满，若满的话需要删除内存*/
	        		if (this.getHost().getFreeFileBufferSize() < 0) {
	        			this.getHost().makeRoomForNewFile(0);    	//必要时，删除那些最早接收到且不正在传输的消息
	        			System.out.print("+++++++++++++++++++++		删除成功	++++++++++++++++++++"+"\n");
	        		}	
	        		this.PutIntoFileBuffer(aMessage);
	        		System.out.println("-------------------------"+" 中继节点 "+this.getHost()+
	        						   " 放入文件："+aMessage.getFilename()+"--------------------");
		        	
					/**
					 * 收到控制包之后，需要做两件事，一件回复上一跳，一件是往目的节点发
					 * 1、判断收齐的情况下，将MessageHashMap  中消息顺序取出，往下一跳发
					 */	 
			        this.createResponseMsg_notDestination(aMessage);
		        	this.createControlMsg_notDestination(aMessage);
		    		this.MessageHashMap.remove(aMessage.getFilename());	
	        	} 
	        	/**
	        	 * 2、回复确认包(收齐或者没有收齐都会向上一跳回复确认包)			
	        	 */
	        	this.createAck2CtrlMsg_notDestination(aMessage);
			}
		}else{
			/** 
			 * 类比于收到应答包，内存中存在此文件，需要逐跳确认， 需要做三件事：
			 * 1、往上一跳发送对控制包的确认包；
			 * 2、往下一跳从自身缓存中取出文件进行应答，而不必管是否收齐；
			 * 3、 删除接收到的文件分片以及控制包
			 */	
//			System.out.println("******************************中继节点中已经存在了文件："+aMessage.getFilename());

			/**  往下一跳从自身缓存中取出文件进行应答，而不必管是否收齐*/
			CreateResponseMsg(aMessage);
			/**  往下一跳发送控制包 */
			CreateControlMsg(aMessage);
			/**  往上一跳发送对控制包的确认包 */
			CreateAck2CtrlMsg(aMessage);
			/**  删除接收到的文件分片以及控制包 */
			DeleteRcvMsg(aMessage);
		}
	}
	/**
	 * 针对控制包重传的情况：1、由于控制包丢失；2、由于控制包重新选路
	 * @param aMessage
	 */
	public void RetransCtrlMsg_notDestination(Message aMessage){
		/**	证明一个文件都没有,向上一跳请求文件。根据消息aMessage生成*/
		if(this.judgeForRetransfer.containsKey(Ack2Ctrl_Msg + aMessage.getInitMsgId())){			
			Message m = (Message) this.judgeForRetransfer.get(
					Ack2Ctrl_Msg + aMessage.getInitMsgId()).get(0);
			Message ackMessage = new Message(m.getFrom(), m.getTo(),
					Ack2Ctrl_Msg + m.getId(), m.getResponseSize());
			
			ackMessage.setInitMsgId(m.getInitMsgId());
			ackMessage.updateProperty(SelectLabel, 3);															
			ackMessage.setFilename(m.getFilename());
			ackMessage.setZeroForBitMap();
			ackMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//更新消息生成时间
			this.judgeForRetransfer.get(Ack2Ctrl_Msg + m.getInitMsgId()).set(1, this.time_wait);
            this.createNewMessage(ackMessage);
		}else{
			/** 证明由于重新选路造成的*/
			Message ack2CtrlMessage = new Message(this.getHost(),aMessage.getHops().get(aMessage.getHopCount()-1),
					Ack2Ctrl_Msg + aMessage.getInitMsgId(),aMessage.getResponseSize());
			ack2CtrlMessage.setInitMsgId(aMessage.getInitMsgId());
			ack2CtrlMessage.updateProperty(SelectLabel, 3);															
			ack2CtrlMessage.setFilename(aMessage.getFilename());
			ack2CtrlMessage.setZeroForBitMap();
			ack2CtrlMessage.setTime(SimClock.getTime()+0.01, SimClock.getTime()+0.01);	//更新消息生成时间
			this.putJudgeForRetransfer(ack2CtrlMessage);	//放入重传表中
            this.createNewMessage(ack2CtrlMessage);
            this.router.removeFromMessages(aMessage.getId());
		}
	}
	
	/**  
	 * 中继节点存在文件的情况
	 * 删除接收到的文件分片以及控制包 
	 */
	public void DeleteRcvMsg(Message aMessage){
		this.getHost().getChunkBuffer().remove(aMessage.getFilename());	//清空ChunkBuffer	
		this.MessageHashMap.remove(aMessage.getFilename());		//清空MessageHashMap中消息
		this.router.removeFromMessages(aMessage.getId());		//需要删除这个控制包
	}
	/**
	 * 针对中继节点中存在文件的情况创建应答包
	 */
	public void CreateResponseMsg(Message aMessage){
		this.getHost().getFileBufferForFile(aMessage).setTimeRequest(SimClock.getTime());  	//缓存中文件更新请求时间
		File f = this.getHost().getFileBufferForFile(aMessage);
		for(int i=0;i<10;i++){							// each file is divided into 10 chunks
			File chunk = f.copyFrom(f);
			for(int j=i*10;j<i*10+10;j++){
				chunk.getData().add(j-i*10,f.getData().get(j));
			}
			Message response = new Message(this.getHost(), aMessage.getTo(),Response_Msg
									  +aMessage.getInitMsgId()+i, aMessage.getResponseSize(),chunk);	
			response.setInitMsgId(aMessage.getInitMsgId());
			response.setResponseSize(0);						//这一句应该没用
			response.setFilename(aMessage.getFilename());
			response.setChunkID(aMessage.getFilename()+"ChunkID"+i);	//设置chunkID，在message中设置。
			response.updateProperty(SelectLabel,1);		//说明这是一个应答包						
			response.setTime(SimClock.getTime()+0.01*(i+1), SimClock.getTime()+0.01*(i+1));
			this.createNewMessage(response);
		}
		System.out.println("中继节点存在文件，收到控制包时候的消息ID为："+aMessage.getId()+"  "+"test!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	}
	
	/**
	 * 针对中继节点中存在文件时创建控制包
	 */
	public void CreateControlMsg(Message aMessage){
		
		Message ctrMessage =new Message(this.getHost(),aMessage.getTo(),
				aMessage.getId(), aMessage.getResponseSize());
		ctrMessage.setInitMsgId(aMessage.getInitMsgId());
		ctrMessage.updateProperty(SelectLabel, 2);												//标识为控制包
		ctrMessage.setFilename(aMessage.getFilename());
		ctrMessage.setZeroForBitMap();
		ctrMessage.setTime(SimClock.getTime()+11*(0.01), SimClock.getTime()+11*(0.01));	              
		this.createNewMessage(ctrMessage);  
		this.putJudgeForRetransfer(ctrMessage);
	}
	
	/**
	 * 针对中继节点中存在文件时创建控制包的确认包
	 */
	public void CreateAck2CtrlMsg(Message aMessage){
    	Message ackMessage = new Message(this.getHost(),aMessage.getHops().get(aMessage.getHopCount()-1),
    			Ack2Ctrl_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ackMessage.setInitMsgId(aMessage.getInitMsgId());
    	ackMessage.updateProperty(SelectLabel,3);	//说明这是一个确认包
		ackMessage.getBitMap().clear();             //先清空bitmap
		/** 为了区分，这里的bitmap是一个局部变量*/
		ArrayList<Integer> BitMap = new ArrayList<Integer>();
		for (int i = 0; i<10; i++) {
			BitMap.add(i, 1);
		}
		ackMessage.getBitMap().addAll(BitMap);				//回复bitMap    	
    	ackMessage.setFilename(aMessage.getFilename());
		this.putJudgeForRetransfer(ackMessage);
    	this.createNewMessage(ackMessage);
		System.out.println("中继中bitmap进行测试：" + "  " + ackMessage.getBitMap()
						    + " " + "消息的初始ID为：" + ackMessage.getInitMsgId() 
						    + " " + "当前节点为：" + this.getHost());
	}
	
	
	/**
	 * 非目的节点生成应答包1
	 * 收到控制包之后，需要做两件事，一件回复上一跳，一件是往目的节点发
	 * 判断收齐的情况下，将 MessageHashMap 中消息顺序取出，往下一跳发的应答包
	 * @param aMessage
	 */
	public void createResponseMsg_notDestination (Message aMessage){
		try{
			HashMap<String,Message> NewHashMap = MessageHashMap.get(aMessage.getFilename());
	    	for(int i=0;i<10;i++){
	    		Message m = NewHashMap.get(aMessage.getFilename()+"ChunkID"+i);    		
	    		// 主要是改变源地址 
	    		DTNHost thisHost = this.getHost();	//	源地址 
	    		DTNHost thisto = m.getTo();			//	当前消息的目的节点
	    		Message newMessage = new Message(thisHost,thisto,m.getId(),m.getSize());
	    		newMessage.copyFrom(m);				//  copy当前消息的内容
				newMessage.setFilename(m.getFilename());	        			
				newMessage.setBitMap(m.getBitMap());
				newMessage.setInitMsgId(m.getInitMsgId());
		        newMessage.setFile(m.getFile());
		        newMessage.setTime(SimClock.getTime()+0.01*(i+1), SimClock.getTime()+0.01*(i+1));
	    		this.createNewMessage(newMessage);
	    	}
    	} catch(NullPointerException e){
    		System.out.println("MessageHashMap中这一项为空！！！");
    	}
	}
	
	/**
	 * 非目的节点生成控制包2
	 * 收到控制包之后，需要做两件事，一件回复上一跳，一件是往目的节点发
	 * 判断收齐的情况下，将 MessageHashMap 中消息顺序取出，往下一跳发的控制包
	 * @param aMessage
	 */
	public void createControlMsg_notDestination(Message aMessage){
		try{
			HashMap<String,Message> NewHashMap = MessageHashMap.get(aMessage.getFilename());
	    	Message m = NewHashMap.remove(aMessage.getId());// 这个是控制包		            		
	    	DTNHost thisHost = this.getHost();				// 源地址 
			DTNHost thisto = m.getTo();						// 当前消息的目的节点
			Message newMessage = new Message(thisHost,thisto,m.getId(),m.getSize());
			newMessage.copyFrom(m);							// copy当前消息的内容
			newMessage.setFilename(m.getFilename());	        			
			newMessage.setBitMap(m.getBitMap());
			newMessage.setInitMsgId(m.getInitMsgId());
			newMessage.setTime(SimClock.getTime()+11*(0.01), SimClock.getTime()+11*(0.01));
		    this.createNewMessage(newMessage);
			this.putJudgeForRetransfer(newMessage);			// 由当前节点发出的控制包，放入当前节点的待确认缓存中
		} catch(NullPointerException e){
    		System.out.println("MessageHashMap中这一项为空！！！");
    	}

	}
	
	/**
	 * 非目的节点生成对控制包的确认包3
	 * @param aMessage
	 */
	public void createAck2CtrlMsg_notDestination(Message aMessage){
    	Message ackMessage = new Message(this.getHost(),aMessage.getHops().get(aMessage.getHopCount()-1),
    			Ack2Ctrl_Msg + aMessage.getInitMsgId(), aMessage.getResponseSize());
		ackMessage.setInitMsgId(aMessage.getInitMsgId());
    	ackMessage.updateProperty(SelectLabel,3);		//说明这是一个确认包
		ackMessage.getBitMap().clear();             	//先清空bitmap
    	ackMessage.getBitMap().addAll(this.bitMap);		//回复bitMap    	
    	ackMessage.setFilename(aMessage.getFilename());
		this.putJudgeForRetransfer(ackMessage);
    	this.createNewMessage(ackMessage);
    	
    	System.out.println("中继中bitmap进行测试："+"  "+ ackMessage.getBitMap()+" "+
				   "消息的初始ID为："+ackMessage.getInitMsgId()+"  "+"当前节点为："+this.getHost());
	}
	
	/**
	 * 处理到达非目的节点的控制包的确认包3 
	 * @param aMessage
	 */
	private void Ack2CtrlMessage_NotDestination(Message aMessage) {
		// TODO Auto-generated method stub
	}
	
	/**
	 * 处理到达非目的节点的请求包的确认包4
	 * @param aMessage
	 */
	private void Ack2RequestMessage_NotDestination(Message aMessage) {
		// TODO Auto-generated method stub
	}
	

}
