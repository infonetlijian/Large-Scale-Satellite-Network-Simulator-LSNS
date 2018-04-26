/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Reports information about all created messages. Messages created during
 * the warm up period are ignored.
 * For output syntax, see {@link #HEADER}.
 */
public class CreatedMessagesReport extends Report implements MessageListener {
	/** 用于判断包的类型 */
	public String SelectLabel;
	
	public static String HEADER = "# time  ID  size  fromHost  toHost  TTL  " + 
		"isResponse";

	/**
	 * Constructor.
	 */
	public CreatedMessagesReport() {
		init();
	}
	
	@Override
	public void init() {
		super.init();
		write(HEADER);
	}


	public void newMessage(Message m) {
		if (isWarmup()) {
			return;
		}
		
		int ttl = m.getTtl();
//		write(format(getSimTime()) + " " + m.getId() + " " + 
//				m.getSize() + " " + m.getFrom() + " " + m.getTo() + " " +
//				(ttl != Integer.MAX_VALUE ? ttl : "n/a") +  
//				(m.isResponse() ? " Y " : " N "));
		
		// 希望看到与缓存相关的信息(修改之后信息)

		write(format(getSimTime()) + " " + m.getProperty(SelectLabel)+ "  "
				+m.getFilename()+" "+m.getChunkID()+"  "
				+m.getId()+" "+m.getFrom()+"  "+m.getTo()+"  "+"初始消息名称："+"  "+m.getInitMsgId()
				+" "+ m.getHops()
				+" "+"消息创建时间："+"  "+ m.getCreationTime()+"  "+"消息接收时间："+"  "+ m.getReceiveTime());
	}
	
	// nothing to implement for the rest
	public void messageTransferred(Message m, DTNHost f, DTNHost t,boolean b) {}
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}

	@Override
	public void done() {
		super.done();
	}
}
