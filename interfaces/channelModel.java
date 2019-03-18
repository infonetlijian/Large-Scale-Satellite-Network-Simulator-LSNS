package interfaces;

import core.*;

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
    private double transmitPower = 20;
    /** allocated subcarrier bandwidth in FDMA */
    private double bandwidth;
    /** allocated time slot for transmission in TDMA */
    private double timeSlot;
    /** center frequency in transmission */
    private double transmitFrequency;
    /** Gaussian white noise in the background, which is related to the bandwidth, unit dBm/Hz */
    private double spectralDensityNoisePower = -174;
    /** standard deviation used in each channel model **/
    private  double standardDeviation = 1.0;
    /** energy ratio (K) in Rice channel model **/
    private double energyRatio_K = 1;
    /** channel model type (e.g. Gaussian, Rayleigh, Rice...)**/
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

        return currentSpeed/8;//bit ->> byte
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
                fadingFactor = RayleighRandomVariable(distance);
                break;
            }
            case "Rice": {
                fadingFactor = RiceRandomVariable(distance, energyRatio_K);
                break;
            }
            case "Nakagami": {
                //TODO;
                break;
            }
            //default
        }
        return fadingFactor;
    }

    /**
     * generate random variable obeys rice distribution
     * @param distance
     * @return
     */
    public double RiceRandomVariable(double distance, double energyRatio_K){
        if (energyRatio_K < 0)
            throw new SimError("error in channelModel.java, because energy ratio(K) in Rice model < 0 ");

        double fadingFactor = 1;
        //TODO;
        //reference:https://blog.csdn.net/yuan1164345228/article/details/12451073
        //https://www.edaboard.com/showthread.php?136982-generate-Rician-fading-using-Rician-K-factor
        //既然K代表了直射信号和瑞利信号的信号比值，因此先把直射信号的信道因子作为1，直接对叠加之后的h做统一的缩放
        //distance // km
        double lightSpeed = 2.99792458*Math.pow(10,8);// unit: m/s
        //transmitFrequency//Hz
        double lamda = lightSpeed/transmitFrequency;
        double path_loss = Math.pow(Math.PI/(4*distance*lamda), 2);// free space path loss

        double ratio = path_loss / 1;

        double rayleighFactor = RayleighRandomVariable(distance);
        double h = Math.sqrt(energyRatio_K/(energyRatio_K + 1))*1 +
                Math.sqrt(1/(energyRatio_K + 1))*rayleighFactor;

        fadingFactor = h * ratio;
        //System.out.println("free space loss: "+path_loss+" lamda "+lamda+" fading factor "+fadingFactor+" rayleigh fading: "+rayleighFactor);
        return fadingFactor;
    }

    /**
     * generate random variable obeys rayleigh distribution
     * @param distance
     * @return
     */
    public double RayleighRandomVariable(double distance){
        double fadingFactor = 1;
        //method 1:
        //reference: https://daim.idi.ntnu.no/masteroppgaver/003/3614/masteroppgave.pdf
        double I = channelRandom.nextGaussian();
        double J = channelRandom.nextGaussian();
        double envelope = Math.sqrt(Math.pow(I, 2) + Math.pow(J, 2));//幅度
        double standardDeviation = this.standardDeviation;//标准差
        double PDF = 0;
        if (envelope >= 0)
            PDF = envelope/Math.pow(standardDeviation, 2)*
                    Math.exp(- Math.pow(envelope, 2)/(2*Math.pow(standardDeviation, 2)));
        fadingFactor = envelope;

        //method 2:
        //formula : CDF = 1 - Math.exp(- fadingFactor / 2*Math.pow(standardDeviation, 2)) = randomNumber(0 - 1)
        //fadingFactor = Math.sqrt(-2*Math.pow(standardDeviation, 2)*Math.log(1 - channelRandom.nextDouble()));

        return fadingFactor;
    }

    /**
     * Calculate channel capacity according to Shannon equation
     */
    public double channelCapacity(double fadingFactor, double bandwidth){
        double transmitPower_mW = Math.exp(this.transmitPower/10);
        double noise_mW = bandwidth*Math.exp(spectralDensityNoisePower/10);
        double SNR = transmitPower_mW*fadingFactor/noise_mW;
        double capacity = bandwidth*Math.log10(1+SNR);//Shannon Equation
        //System.out.println(transmitPower+"  power:  "+transmitPower_mW+"   "+SNR+"  speed: "+capacity);
        return capacity;
    }

}
