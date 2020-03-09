package core;

import interfaces.SatelliteWithChannelModelInterface;
import interfaces.channelModel;
import routing.MessageRouter;
import util.Tuple;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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
    private double currentSNR = 0;//dB
    private static String channelModelType;
    private channelModel channelModel;
    private double timeSlot;
    private static double backhaulSpeed;
    /**
     * Key: User, Value: reachable satellites and their relative location (Elevation Angle)
     */
    private static HashMap<DTNHost, ArrayList<Tuple<DTNHost, Double>>> relativeLocation = new HashMap<DTNHost, ArrayList<Tuple<DTNHost, Double>>>();
    /**
     * probability when shadowing happens
     **/
    private static double shadowingProbability = 0;
    /**
     * time duration when shadowing happens
     */
    private static double shadowingDuration = 1;
    /**
     * label at this time duration if it has shadowing
     */
    private static boolean shadowingLabel = false;
    private boolean localShadowingLabel = false;
    /**
     * save the shadowing connection
     */
    private static List<Connection> shadowingList = new ArrayList<Connection>();
    private static List<DTNHost> shadowingHostList = new ArrayList<DTNHost>();
    /**
     * record shadowing duration end time
     */
    private static double shadowingEndTime = 0;
    private double localShadowingEndTime = 0;
    /**
     * record the last random generation time
     */
    private static double lastCheckTime = 0;
    /**
     * shadowing happens in global links or partial links
     */
    private static String shadowingMode;
    /**
     * if shadowing happens in partial links, indicate how many part of them would appear shadowing
     */
    private static int nrofShadowingLink = 0;

    /**
     * an open method to update relative location information for shadowing decision
     *
     * @param user
     * @param angle
     */
    public static void updateRelativeLocation(DTNHost user, ArrayList<Tuple<DTNHost, Double>> angle) {
        relativeLocation.remove(user);//release memory at first
        relativeLocation.put(user, angle);
    }

    /**
     * Creates a new connection between nodes and sets the connection
     * state to "up".
     *
     * @param fromNode      The node that initiated the connection
     * @param fromInterface The interface that initiated the connection
     * @param toNode        The node in the other side of the connection
     * @param toInterface   The interface in the other side of the connection
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
        if (channelModelType.contains(DTNSim.SHADOWING)) {
            shadowingMode = s.getSetting(DTNSim.SHADOWING_MODE);
            if (shadowingMode.contains(DTNSim.SHADOWING_PARTIAL_LINK)) {
                nrofShadowingLink = s.getInt(DTNSim.NROF_SHADOWINGLINK);
            }
        }

        Settings set = new Settings(DTNSim.SCENARIO);
        this.timeSlot = set.getDouble(DTNSim.UPDATEINTERVAL);
    }

    /**
     * Sets a message that this connection is currently transferring. If message
     * passing is controlled by external events, this method is not needed
     * (but then e.g. {@link #finalizeTransfer()} and
     * {@link #isMessageTransferred()} will not work either). Only a one message
     * at a time can be transferred using one connection.
     *
     * @param from The host sending the message
     * @param m    The message
     * @return The value returned by
     * {@link MessageRouter#receiveMessage(Message, DTNHost)}
     */
    public int startTransfer(DTNHost from, Message m) {
        assert this.msgOnFly == null : "Already transferring " +
                this.msgOnFly + " from " + this.msgFromNode + " to " +
                this.getOtherNode(this.msgFromNode) + ". Can't " +
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
     * judge if shadowing should happen or not on this connection
     *
     * @return
     */
    public boolean shadowingJudge(int nrofPossibleShadowingLinks) {
        // ensure it is user--satellite link
        if (!(toNode.toString().contains(DTNSim.USER) || fromNode.toString().contains(DTNSim.USER)))
            return false;

        DTNHost user = toNode.toString().contains(DTNSim.USER) ? toNode : fromNode;
        DTNHost satellite = toNode.toString().contains(DTNSim.SAT) ? toNode : fromNode;

        ArrayList<Tuple<DTNHost, Double>> distances = relativeLocation.get(user);
        // find n number of links
        List<Tuple<DTNHost, Double>> shadowingLinks = new
                ArrayList<Tuple<DTNHost, Double>>(distances.subList(0, nrofPossibleShadowingLinks - 1));

        if (lastCheckTime + timeSlot <= SimClock.getTime()) {
            if (getChannelRandom().nextDouble() < shadowingProbability && !shadowingLabel) {
                for (Tuple<DTNHost, Double> t : shadowingLinks){
                    shadowingHostList.add(t.getKey());
                }
                shadowingEndTime = SimClock.getTime() + shadowingDuration;
                shadowingLabel = true;
                //shadowingList.clear();
                //shadowingList.add(this);
                return true;
            }
            lastCheckTime = SimClock.getTime();
        }
        return false;
    }
    private double totalSpeed = 0;
    private int totalCount = 0;
    private double averageSpeed = 0;
    /**
     * deal with shadowing scenario, for global links or partial links
     */
    public double shadowingUpdate() {
        currentspeed = -1;
        switch (shadowingMode) {
            // for global links can occur shadowing
            case DTNSim.SHADOWING_GLOBAL_LINK: {
                if (localShadowingLabel) {
                    if (localShadowingEndTime > SimClock.getTime()) {
                        Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.SHADOWING);
                        currentspeed = status.getKey();
                        currentSNR = status.getValue();
                        //System.out.println("shadowing: "+currentspeed+"  "+SimClock.getTime()+ " con: " +this+"  ");
                    } else {
                        localShadowingEndTime = 0;
                        localShadowingLabel = false;
                        Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
                        currentspeed = status.getKey();
                        currentSNR = status.getValue();
                    }
                }
                else{
                    if (getChannelRandom().nextDouble() < shadowingProbability && !localShadowingLabel) {
                        localShadowingLabel = true;
                        localShadowingEndTime = SimClock.getTime() + shadowingDuration;
                    }
                }
                break;
            }
            case DTNSim.SHADOWING_TRANSFERRING_LINK:{
                if (localShadowingLabel) {
                    if (localShadowingEndTime > SimClock.getTime()) {
                        Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.SHADOWING);
                        currentspeed = status.getKey();
                        currentSNR = status.getValue();
                        //System.out.println("shadowing: "+currentspeed+"  "+SimClock.getTime()+ " con: " +this+"  ");

                    } else {
                        shadowingList.remove(this);
                        localShadowingEndTime = 0;
                        localShadowingLabel = false;
                        Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
                        currentspeed = status.getKey();
                        currentSNR = status.getValue();
                    }
                }
                else{
                    // for example, if one link already occurs shadowing, the probability of another shadowing happens in other links is P_s * P_s
                    //double shadowingCumulativeProbability = (shadowingList.size() + 1)*shadowingProbability;
                    if (getChannelRandom().nextDouble() < shadowingProbability && !localShadowingLabel && this.isTransferring()) {
                        if (shadowingList.size() < 1){
                            shadowingList.add(this);
                            localShadowingLabel = true;
                            localShadowingEndTime = SimClock.getTime() + shadowingDuration;
                        }
                    }
                }
                break;
            }
            // for only part of links can occur shadowing
            case DTNSim.SHADOWING_PARTIAL_LINK: {
                if (shadowingLabel) {
                    if (shadowingEndTime > SimClock.getTime()) {
                        if (shadowingHostList.contains(toNode) || shadowingHostList.contains(fromNode)) {
                            Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.SHADOWING);
                            currentspeed = status.getKey();
                            currentSNR = status.getValue();
                        }
                    } else {
                        shadowingEndTime = 0;
                        shadowingLabel = false;
                        Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
                        currentspeed = status.getKey();
                        currentSNR = status.getValue();
                    }
                }
                else {
                    // use the information provided by RelayRouterforInternetAccess.java, it updates "relativeLocation"
                    // variable
                    if (shadowingJudge(this.nrofShadowingLink)) {
                        Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.SHADOWING);
                        currentspeed = status.getKey();
                        currentSNR = status.getValue();
                    }
                }
                break;
            }
        }
        if (currentspeed <= 0) {
            Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);
            currentspeed = status.getKey();
            currentSNR = status.getValue();
        }
        return currentspeed;
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
            if (backhaulSpeed <= 0){
                Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);//rice channel model for backhaul link
                currentspeed = status.getKey();
                currentSNR = status.getValue();
            }
            else {
                currentspeed = backhaulSpeed;//constant backhaul link speed
            }
            msgsent = msgsent + (int)(currentspeed * timeSlot);
            return;
        }
        switch (channelModelType){
            case DTNSim.SHADOWING :{
                currentspeed = shadowingUpdate();
                break;
            }
            //other situations
            default:{
                Tuple<Double, Double> status = channelModel.updateLinkState(fromNode, toNode, DTNSim.RICE);//rice channel model for backhaul link
                currentspeed = status.getKey();
                currentSNR = status.getValue();
            }
        }
        //if (msgsize > 0)
            //System.out.println("VBRconnection current speed: "+currentspeed+ " bps, msg size: "+msgsize+" transmission time: "+msgsize/currentspeed);

        msgsent = msgsent + (int)(currentspeed * timeSlot);
//        if (toNode.toString().contains(DTNSim.USER)||fromNode.toString().contains(DTNSim.USER)) {
//            totalSpeed = currentspeed + totalSpeed;
//            totalCount++;
//            averageSpeed = totalSpeed/totalCount;
//            double SNR = interfaces.channelModel.dBcoverter(averageSpeed, false);
//            System.out.println("shadowing: "+currentspeed+"  "+SimClock.getTime()+ " con: " +this+"  speed: "+averageSpeed+" count: "+totalCount + "  SNR: "+SNR);
//        }
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
