package core;

import interfaces.SatelliteWithChannelModelInterface;
import interfaces.channelModel;
import routing.MessageRouter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private String channelModelType;
    private channelModel channelModel;
    private double timeSlot;
    private static double backhaulSpeed;
    /** probability when shadowing happens **/
    private static double shadowingProbability = 0;
    /** time duration when shadowing happens*/
    private static double shadowingDuration = 1;
    /** label at this time duration if it has shadowing*/
    private static boolean shadowingLabel = false;
    /** save the shadowing connection */
    private static List<Connection> shadowingList = new ArrayList<Connection>();
    /** record shadowing duration end time */
    private static double shadowingEndTime = 0;

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

        Settings s = new Settings(DTNSim.USERSETTINGNAME_S);
        //to simulate random status of wireless link
        channelModel = new channelModel(s.getSetting(DTNSim.CHANNEL_MODEL),
                s.getDouble(DTNSim.TRANSMITTING_POWER), s.getDouble(DTNSim.TRANSMITTING_FREQUENCY), s.getDouble(DTNSim.BANDWIDTH));
        shadowingProbability = s.getDouble(DTNSim.SHADOWING_PROB);
        shadowingDuration = s.getDouble(DTNSim.SHADOWING_DURATION);
        channelModelType = s.getSetting(DTNSim.CHANNEL_MODEL);
        backhaulSpeed = s.getDouble(DTNSim.SPEED_GSLINK);

        Settings set = new Settings(DTNSim.SCENARIO);
        this.timeSlot = set.getDouble(DTNSim.UPDATEINTERVAL);

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
        //Warning:can not set current speed to 0, otherwise the sliding window can not
        //record channel status in "RelayRouterforInternetAccess.java"
        /*if (!isTransferring()) {
            currentspeed = 0;
            return;
        }*/
        if (toNode.toString().contains(DTNSim.GS)||fromNode.toString().contains(DTNSim.GS)) {
            currentspeed = backhaulSpeed;
            msgsent = msgsent + (int)(currentspeed * timeSlot);
            return;
        }
        switch (channelModelType){
            case DTNSim.SHADOWING :{
                currentspeed = -1;
                if (shadowingLabel){
                    if (shadowingEndTime > SimClock.getTime()){
                        if (shadowingList.contains(this)){
                            currentspeed = channelModel.updateLinkState(fromNode, toNode, DTNSim.SHADOWING);
                            System.out.println("shadowing: "+currentspeed+"  "+SimClock.getTime()+ " con: " +this+"  ");
                        }
                        else{
                            currentspeed = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
                        }
                    }
                    else{
                        shadowingEndTime = 0;
                        shadowingLabel = false;
                        currentspeed = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
                    }
                }
                else{
                    if (getChannelRandom().nextDouble() < shadowingProbability && !shadowingLabel && this.isTransferring()){
                        shadowingList.clear();
                        shadowingList.add(this);
                        shadowingEndTime = SimClock.getTime() + shadowingDuration;
                        currentspeed = channelModel.updateLinkState(fromNode, toNode, DTNSim.SHADOWING);
                        System.out.println("shadowing: "+currentspeed+"  "+SimClock.getTime()+ " con: " +this);
                    }
                }
                if (currentspeed <= 0)
                    currentspeed = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
                break;
            }

            default:{
                currentspeed = channelModel.updateLinkState(fromNode, toNode, channelModelType);
            }
        }
        //if (msgsize > 0)
            //System.out.println("VBRconnection current speed: "+currentspeed+ " bps, msg size: "+msgsize+" transmission time: "+msgsize/currentspeed);

        msgsent = msgsent + (int)(currentspeed * timeSlot);
    }

    /**
     * get random variable
     * @return
     */
    public Random getChannelRandom(){
        return channelModel.getChannelRandom();
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
