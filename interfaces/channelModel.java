package interfaces;

import core.Connection;
import core.Coord;
import core.DTNHost;
import core.VBRConnectionWithChannelModel;

import java.util.HashMap;
import java.util.Random;

/**
 * Project Name:Large-scale Satellite Networks Simulator (LSNS)
 * File SatellitetoGroundChannelModel.java
 * Package Name:interface
 * Copyright 2018 University of Science and Technology of China , Infonet
 * lijian9@mail.ustc.mail.cn. All Rights Reserved.
 */
public class channelModel {
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
    /** channel model type (e.g. Gaussian, Rayleigh)**/
    private String modelType;
    /** generate random number */
    private static Random channelRandom = new Random();

    /** temporary store the current speed of link**/
    private double currentSpeed;
    ///** general channel update count **/
    //private static int channelUpdateCount = 0;

    public channelModel(String modelType, double transmitPower, double transmitFrequency, double bandwidth){
        this.transmitPower = transmitPower;
        this.transmitFrequency = transmitFrequency;
        this.bandwidth = bandwidth;
        this.modelType = modelType;
    }


    /**
     * should be updated once by DTNHost router in each update function
     * @return the current channel capacity (aka speed, bit/s)
     */
    public double updateLinkState(Connection con, DTNHost from, DTNHost to){
        double distance = calculateDistance(from, to);
        double fadingFactor = channelState(modelType, distance);
        double currentSpeed = channelCapacity(fadingFactor, bandwidth);
        System.out.println(currentSpeed+"  "+con);
        return currentSpeed;
    }

    /**
     * calculate distance between two DTNHosts
     * @param from
     * @param to
     * @return
     */
    public  double calculateDistance(DTNHost from, DTNHost to){
        Coord a = from.getLocation();
        Coord b = to.getLocation();
        return a.distance(b);
    }

    /**
        Generate instant channel state according to different channel model
     */
    public double channelState(String channelModel, double distance){
        double fadingFactor = 1;

        switch (channelModel){
            case "Rayleigh": {
                //reference: https://daim.idi.ntnu.no/masteroppgaver/003/3614/masteroppgave.pdf
                double I = channelRandom.nextGaussian();
                double J = channelRandom.nextGaussian();
                double envelope = Math.sqrt(Math.pow(I, 2) + Math.pow(J, 2));//幅度
                double standardDeviation = 1.0;//标准差
                double PDF = 0;
                if (envelope >= 0)
                    PDF = envelope/Math.pow(standardDeviation, 2)*
                        Math.exp(- Math.pow(envelope, 2)/(2*Math.pow(standardDeviation, 2)));
                //formula : CDF = 1 - Math.exp(- fadingFactor / 2*Math.pow(standardDeviation, 2)) = randomNumber(0 - 1)
                fadingFactor = Math.sqrt(-2*Math.pow(standardDeviation, 2)*Math.log(1 - channelRandom.nextDouble()));
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
        System.out.println(transmitPower_mW);
        double noise_mW = bandwidth*Math.exp(spectralDensityNoisePower/10);
        double SNR = transmitPower_mW*fadingFactor/noise_mW;
        double capacity = bandwidth*Math.log10(1+SNR);//Shannon Equation
        return capacity;
    }
}
