package routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.MessageListener;
import core.Settings;
import core.SimError;

/**
 * Created by ustc on 2016/6/23.
 */
public class myRouter extends ActiveRouter {
	
	public static final String SelectLabel = "SelectLabel";
    /**
     * Constructor. Creates a new message router based on the settings in
     * the given Settings object.
     * @param s The settings object
     */
    public myRouter(Settings s) {
        super(s);
    }

    /**
     * Copy constructor.
     * @param r The router prototype where setting values are copied from
     */
    protected myRouter(myRouter r) {
        super(r);
    }

    @Override
    protected int checkReceiving(Message m, DTNHost from) {
        int recvCheck = super.checkReceiving(m, from);

        if (recvCheck == RCV_OK) {
			/* don't accept a message that has already traversed this node */
            if (m.getHops().contains(getHost())) {
                recvCheck = DENIED_OLD;
            }
        }
        return recvCheck;
    }

    @Override
    public void update() {
    	
    	
    	super.update();										//  调用ActiveRouter中update()函数
        
        if (isTransferring() || !canStartTransfer()) {
            return;
        }
/*        if (exchangeDeliverableMessages() != null) {
            return;
        }*/
        Connection con = tryAllMessagesToAllConnections();
        con.getOtherNode(this.getHost());
    }
    @Override
	protected Connection tryAllMessagesToAllConnections(){
		List<Connection> connections = getConnections();				//取得所有邻居节点
		if (connections.size() == 0 || this.getNrofMessages() == 0) {	//没有链接，或者没有消息，返回空
			return null;
		}

		List<Message> messages = 
			new ArrayList<Message>(this.getMessageCollection());		//取得缓冲区所有消息
		this.sortByQueueMode(messages);

		return tryMessagesToConnections(messages, connections);
	}
//    @Override
//	protected Connection tryMessagesToConnections(List<Message> messages,
//			List<Connection> connections) {
//    	
//    	for (Message m : messages){
//    		DTNHost toHost = m.getTo();
//    		switch(this.getHost().getAddress()){
//    		case 0://节点A		
//				if (tryMessage(connections.get(0), m) == null)		
//					assert false : "check!";
//    			return connections.get(0);
//    		case 1://节点B    			
//    			if (connections.get(0).getOtherNode(this.getHost()) == toHost){
//    				if (tryMessage(connections.get(0), m) == null)
//    					assert false : "check!";
//    				return connections.get(0);
//    			}
//    			else{
//    				if (tryMessage(connections.get(1), m) == null)
//    					assert false : "check!";
//    				return connections.get(1);
//    			}
//    		case 2://节点C
//				if (tryMessage(connections.get(0), m) == null)
//					assert false : "check!";
//				return connections.get(0);
//    		}
//    	}
//		assert false: "error, check!";
//		return null;
//	}
    
    @Override
	protected Connection tryMessagesToConnections(List<Message> messages,
			List<Connection> connections) {
		for (int i=0, n=connections.size(); i<n; i++) {
			Connection con = connections.get(i);
			Message started = tryAllMessages(con, messages);       // 对一个链接发送所有的消息
			if (started != null) { 
				return con;
			}
		}
		return null;
	}
    
    
    
	protected Message tryMessage(Connection con, Message m) {
				
		int retVal = startTransfer(m, con);  //将当前消息传送到链接上
		if (retVal == RCV_OK) {
			return m;	// accepted a message, don't try others     一个链接只能传一个消息
		}
		else if (retVal > 0) { 
			return null; // should try later -> don't bother trying others     
		}
		return null; // no message was accepted		
	}
	
    @Override
    protected void transferDone(Connection con) {
		/* don't leave a copy for the sender */
        this.deleteMessage(con.getMessage().getId(), false);   //发送完成之后删除源节点的副本
    }

    @Override
    public myRouter replicate() {
        return new myRouter(this);
    }
    
    @Override
    public Message messageTransferred(String id, DTNHost from) {  	
    	Message m = super.messageTransferred(id, from);
        return m;
    }
}
