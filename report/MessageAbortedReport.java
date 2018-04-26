package report;
import core.DTNHost;
import core.Message;
import core.MessageListener;

public class MessageAbortedReport extends Report implements MessageListener{
	public static String HEADER="# time  Id  from  to";
	/**
	 * Constructor
	 */
	public MessageAbortedReport(){
		init();
	}
	
	@Override
	public void init(){
		super.init();
		write(HEADER);
	}
	
	public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {}
	public void newMessage(Message m) {}
	
	public void messageDeleted(Message m, DTNHost where, boolean dropped) {}
	public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
		write(format(getSimTime()) + " " + m.getId() + " " + from.getAddress() + 
				" " + to.getAddress());
	}
	public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {}
	
	@Override 
	public void done(){
		super.done();
	}
}
