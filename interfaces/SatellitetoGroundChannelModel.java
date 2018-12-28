package interfaces;

import core.DTNHost;

import java.util.HashMap;
import java.util.Random;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File SatellitetoGroundChannelModel.java
 * Package Name:interface
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class SatellitetoGroundChannelModel {
    /** transmission power at transmitter, unit: dBm */
    private double transmitPower = 24;
    /** allocated subcarrier bandwidth in FDMA */
    private double bandwidth;
    /** allocated time slot for transmission in TDMA */
    private double timeSlot;
    /** center frequency in transmission */
    private double transmitFrequency;
    /** Gaussian white noise in the background, which is related to the bandwidth, unit dBm/Hz */
    private double spectralDensityNoisePower = -174;
    /** current channel capacity, should be updated once by DTNHost router in each update function*/
    private HashMap<DTNHost, Double> currentChannelCapacity = new HashMap<DTNHost, Double>();
    /** generate random number */
    private Random random = new Random();

    public SatellitetoGroundChannelModel(double transmitPower, double transmitFrequency, double bandwidth){
        this.transmitPower = transmitPower;
        this.transmitFrequency = transmitFrequency;
        this.bandwidth = bandwidth;
    }

    /**
     * should be updated once by DTNHost router in each update function
     * @param model
     * @param distance
     * @return the current channel capacity (aka speed, bit/s)
     */
    public void updateLinkState(String model, DTNHost otherNode, double distance){
        double fadingFactor = channelState(model, distance);
        this.currentChannelCapacity.put(otherNode, channelCapacity(fadingFactor, bandwidth));
    }

    /**
     * will not change the current channel status
     * @return the current channel capacity (aka speed, bit/s) in each time slot
     */
    public HashMap<DTNHost, Double> getCurrentChannelStatus(){
        return this.currentChannelCapacity;
    }

    /**
        Generate instant channel state according to different channel model
     */
    public double channelState(String channelModel, double distance){
        double fadingFactor = 1;

        switch (channelModel){
            case "Shannon": {
                //reference: https://daim.idi.ntnu.no/masteroppgaver/003/3614/masteroppgave.pdf
                double I = random.nextGaussian();
                double J = random.nextGaussian();
                double envelope = Math.sqrt(Math.pow(I, 2) + Math.pow(J, 2));//幅度
                double standardDeviation = 1.0;//标准差
                double PDF = 0;
                if (envelope >= 0)
                    PDF = envelope/Math.pow(standardDeviation, 2)*
                        Math.exp(- Math.pow(envelope, 2)/(2*Math.pow(standardDeviation, 2)));
                //formula : CDF = 1 - Math.exp(- fadingFactor / 2*Math.pow(standardDeviation, 2)) = randomNumber(0 - 1)
                fadingFactor = Math.sqrt(-2*Math.pow(standardDeviation, 2)*Math.log(1 - random.nextDouble()));
            }
            case "Rice": {
                //TODO;
                //distance // km
                double lightSpeed = 2.99792458*Math.pow(10,8);// unit: m/s
                //transmitFrequency//Hz
                double lamda = lightSpeed/transmitFrequency;
                double K = Math.pow(4*Math.PI*distance/lamda, 2);// free space path loss
            }
            case "Nakagami": {
                //TODO;
            }
            //default
        }
        return fadingFactor;
    }

    /**
       Calculate channel capacity according to Shannon equation
     */
    public double channelCapacity(double fadingFactor, double bandwidth){
        double transmitPower_mW = Math.exp(this.transmitPower/10);
        double noise_mW = bandwidth*Math.exp(spectralDensityNoisePower/10);
        double SNR = transmitPower_mW*fadingFactor/noise_mW;
        double capacity = bandwidth*Math.log10(1+SNR);//Shannon Equation
        return capacity;
    }
}
