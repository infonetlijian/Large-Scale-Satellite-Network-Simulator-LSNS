package core;

import interfaces.SatelliteWithChannelModelInterface;
import interfaces.channelModel;
import routing.MessageRouter;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File VBRConnectionWithChannelModel.java
 * Package Name:core
 * Description: A connection between two DTN nodes. The transmission speed
 * is updated every round according to the random status of channel model
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class VBRConnectionWithChannelModel extends Connection {
    private int msgsize;
    private int msgsent;
    private double currentspeed = 0;
    private channelModel channelModel;
    /**
     * Creates a new connection between nodes and sets the connection
     * state to "up".
     * @param fromNode The node that initiated the connection
     * @param fromInterface The interface that initiated the connection
     * @param toNode The node in the other side of the connection
     * @param toInterface The interface in the other side of the connection
     */
    public VBRConnectionWithChannelModel(DTNHost fromNode, NetworkInterface fromInterface,
                         DTNHost toNode, NetworkInterface toInterface) {
        super(fromNode, fromInterface, toNode, toInterface);
        this.msgsent = 0;

        Settings s = new Settings(DTNSim.INTERFACE);
        //to simulate random status of wireless link
        channelModel = new channelModel(s.getSetting(DTNSim.CHANNEL_MODEL),
                s.getDouble(DTNSim.TRANSMITTING_POWER), s.getDouble(DTNSim.TRANSMITTING_FREQUENCY), s.getDouble(DTNSim.BANDWIDTH));
    }

    /**
     * Sets a message that this connection is currently transferring. If message
     * passing is controlled by external events, this method is not needed
     * (but then e.g. {@link #finalizeTransfer()} and
     * {@link #isMessageTransferred()} will not work either). Only a one message
     * at a time can be transferred using one connection.
     * @param from The host sending the message
     * @param m The message
     * @return The value returned by
     * {@link MessageRouter#receiveMessage(Message, DTNHost)}
     */
    public int startTransfer(DTNHost from, Message m) {
        assert this.msgOnFly == null : "Already transferring " +
                this.msgOnFly + " from " + this.msgFromNode + " to " +
                this.getOtherNode(this.msgFromNode) + ". Can't "+
                "start transfer of " + m + " from " + from;

        this.msgFromNode = from;
        Message newMessage = m.replicate();
        int retVal = getOtherNode(from).receiveMessage(newMessage, from);

        if (retVal == MessageRouter.RCV_OK) {
            this.msgOnFly = newMessage;
            this.msgsize = m.getSize();
            this.msgsent = 0;
        }

        return retVal;
    }

    /**
     * Calculate the current transmission speed from the information
     * given by the interfaces, and calculate the missing data amount.
     *
     */
    public void update() {
        currentspeed = channelModel.updateLinkState(this, fromNode, toNode);
        if (msgsize > 0)
            System.out.println("VBRconnection "+currentspeed+ " size "+msgsize);
        msgsent = msgsent + (int)currentspeed;
    }

    /**
     * returns the current speed of the connection
     */
    public double getSpeed() {
        return this.currentspeed;
    }

    /**
     * Returns the amount of bytes to be transferred before ongoing transfer
     * is ready or 0 if there's no ongoing transfer or it has finished
     * already
     * @return the amount of bytes to be transferred
     */
    public int getRemainingByteCount() {
        int bytesLeft = msgsize - msgsent;
        return (bytesLeft > 0 ? bytesLeft : 0);
    }
    /**
     * Returns true if the current message transfer is done.
     * @return True if the transfer is done, false if not
     */
    public boolean isMessageTransferred() {
        if (msgsent >= msgsize) {
            return true;
        } else {
            return false;
        }
    }

}
