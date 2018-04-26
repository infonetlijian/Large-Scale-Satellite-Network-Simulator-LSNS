package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessageDeletedReport extends Report implements MessageListener{
	public static String HEADER="# time  Id  DTNHost  true/false";
	/**
	 * Constructor
	 */
	public MessageDeletedReport(){
		init();
	}
	
	@Override
	public void init(){
		super.init();
		write(HEADER);
	}
	
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {}
	public void newMessage(Message m) {}
	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {
		write(format(getSimTime()) + " " + m.getId() + " " + where.getAddress() + 
				" " + dropped);
	}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
	
	@Override 
	public void done(){
		super.done();
	}
}
