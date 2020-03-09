package interfaces;

import core.*;
import util.Tuple;

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
    private double transmitPower = 46;
    /** allocated subcarrier bandwidth in FDMA */
    private double bandwidth;
    /** allocated time slot for transmission in TDMA */
    private double timeSlot;
    /** center frequency in transmission */
    private double transmitFrequency;
    /** Gaussian white noise in the background, which is related to the bandwidth, unit dBm/Hz
     * value comes from article: Seamless Handover in Software-Defined Satellite Networking */
    private double spectralDensityNoisePower = -174;
    /** transmit antenna of diameter, unit: m */
    private double transmitAntennaDiameter = 1;
    /** receiver antenna of diameter, unit: m */
    private double receiverAntennaDiameter = 1;
    /** standard deviation used in each channel model **/
    private double standardDeviation = 1.0;
    /** energy ratio (K) in Rice channel model **/
    private double energyRatio_K = 1;
    /** probability when shadowing happens **/
    private double shadowingProbability = 0;
    /** time duration when shadowing happens*/
    private double shadowingDuration = 1;
    /** record shadowing duration end time */
    private double shadowingEndTime = 0;
    /** save shadowing fading factor */
    private double shadowingFadingFactor = -1;
    /** label at this time duration if it has shadowing*/
    private boolean shadowingLabel = false;
    ///** channel model type (e.g. Gaussian, Rayleigh, Rice...)**/
    //private String modelType;
    /** generate random number */
    private static Random channelRandom = new Random();
    /** speed of light, value comes from google, unit: m/s */
    private static final double speedOfLight = 299792458;

    /** temporary store the current speed of link**/
    private double currentSpeed;

    public channelModel(String modelType, double transmitPower,
                        double transmitFrequency, double bandwidth){
        this.transmitPower = transmitPower;
        this.transmitFrequency = transmitFrequency;
        this.bandwidth = bandwidth;
        //this.modelType = modelType;

        Settings s = new Settings(DTNSim.USERSETTINGNAME_S);
        energyRatio_K = s.getDouble(DTNSim.ENERGYRATIO_RICE);
        shadowingProbability = s.getDouble(DTNSim.SHADOWING_PROB);
        shadowingDuration = s.getDouble(DTNSim.SHADOWING_DURATION);
        transmitAntennaDiameter = s.getDouble(DTNSim.TRANSMIT_ANTENNADIAMETER);
        receiverAntennaDiameter = s.getDouble(DTNSim.RECEIVER_ANTENNADIAMETER);
        spectralDensityNoisePower = s.getDouble(DTNSim.SPECTRIAL_DENSITYNOISE);
    }


    /**
     * should be updated once by DTNHost router in each update function
     * @return the current channel capacity (aka speed, bit/s)
     */
    public Tuple<Double, Double> updateLinkState(DTNHost from, DTNHost to, String channelModel){
        double distance = calculateDistance(from, to);
        if (distance < 550) {//transmitRange
            String type1 = from.toString();
            String type2 = to.toString();
            if (type1.contains(DTNSim.USER) | type2.contains(DTNSim.USER))
                throw new SimError("distance too short  " + from + to);
        }
        double fadingFactor = channelState(channelModel, distance);
        Tuple<Double, Double> currentSpeed = channelCapacity(fadingFactor, bandwidth);
        //System.out.println("Distance:  "+distance+"  channel:  "+fadingFactor+" speed: "+currentSpeed);
        return currentSpeed;//bit/s
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
            case "Shadowing": {
                fadingFactor = ShadowingRandowmVariable(distance, energyRatio_K, shadowingProbability);
                break;
            }
            //default
        }
        return fadingFactor;
    }

    /**
     * generate random variable when shadowing is present
     * @return
     */
    public double ShadowingRandowmVariable(double distance, double energyRatio_K, double shadowingProbability){
        double fadingFactor = 1;
        /** reference: https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=922757
         *  title: Ka-Band Land Mobile Satellite Channel Model Incorporating Weather Effects */

        /** and also seen in :  http://read.pudn.com/downloads62/sourcecode/comm/217464/Rayleigh_Channel/Suzuki_generator%E6%88%90%E7%A1%AE%E5%AE%9A%E5%9E%8BSuzuki%E8%BF%87%E7%A8%8B.m__.htm
         * 文件名： Suzuki_generator成确定型Suzuki过程.m （matlab中的实现）
         % 函数Suzuki_generator(N1,N2,N3,variance,fmax,fc,kc,sigma3,m3,A_Los,f_Los,T_interval,T)
         % 功能：根据需要采用MED、MEA、MCM、MSEM、MEDS和JM中的任何一种方法，来生成既考虑小尺度衰落（莱斯分布），
         %      又考虑大尺度阴影（对数正态分布）的确定型Suzuki过程
         % 输入参数说明：
         % （1）N1,N2,N3,分别表示实确定型高斯过程gauss1_t,gauss2_t,gauss3_t的正弦振荡器数目
         % （2）Variance1,表示确定型高斯过程gauss1_t,gauss2_t,gauss3_t的平均功率
         % （3）fmax,表示最大多普勒频移；
         % （4）fc,为3dB截止频率
         % （5）kc由仿真所需的离散多普勒频移fi,n的选择范围来决定，这里选择kc=2*sqrt(2/ln2);
         % （6）sigma3,表示确定型高斯过程gauss3_t的平均功率的平方根；
         % （7）m3,表示确定型高斯过程gauss3_t的均值；
         % （8）A_Los、f_Los和theta_Los分别表示可视径的幅度，多普勒频移和相移；
         % （9）T_interval,表示抽样间隔；
         % （10）T,表示仿真持续时间。
         % 画出参数说明：
         % Suzuki_t,表示生成的Suzuki过程
         % 程序：
         function Suzuki_t=Suzuki_generator(N1,N2,N3,variance,fmax,fc,kc,sigma3,m3,A_Los,f_Los,T_interval,T)
         %分别采用MEDS和MEA方法生成瑞利过程的同相分量和正交分量参数
         [f1,c1,theta1]=Parameter_Classical('MEDS',N1,variance1,fmax,'rand');
         [f2,c2,theta2]=Parameter_Classical('MEA',N2,Variancel,fmax,'rand');
         c1=c1/sqrt(2);
         c2=c2/sqrt(2);

         %分别采用MEDS生成均值为0、方差为1的对数正态阴影的参数
         [f3,c3,theta3]=Parameter_Gaussian('MEDS',N3,1,fc,kc,'rand')
         g=(2*pi*fd/sqrt(2*log(2)))^2;
         f3(N3)=sqrt(g*N3/(2*pi)^2-sum(f3(1:N3-1).^2));

         N=ceil(T/T_interval);
         t=(0:N-1)*T_interval;
         %生成莱斯过程
         rice_t=Rice_generator(c1,f1,theta1,c2,f2,theta2,A_Los,f_Los,theta_Los,T_interval,T);
         %生成对数正态过程
         lognormal_t=exp(Gauss_generator(c3,f3,theta3,T_interval,T)*sigma3+m3);
         %生成Suzuki过程
         Suzuki_t=rice_t.*lognormal_t;*/
        fadingFactor = RiceRandomVariable(distance, energyRatio_K) * LogNormalDistribution();
//        if (channelRandom.nextDouble() < shadowingProbability && !shadowingLabel){
//            shadowingLabel = true;
//            shadowingEndTime = SimClock.getTime() + shadowingDuration;
//            shadowingFadingFactor = -1;
//        }
//        if (shadowingLabel && shadowingEndTime > SimClock.getTime()){//set a time duration when each shadowing happens
//            if (shadowingFadingFactor > 0){
//                fadingFactor = shadowingFadingFactor;
//            }
//            else{
//                fadingFactor = RiceRandomVariable(distance, energyRatio_K) * LogNormalDistribution();
//                //shadowingFadingFactor = fadingFactor;
//            }
//        }
//        else{
//            shadowingLabel = false;
//            shadowingEndTime = 0;
//            fadingFactor = RiceRandomVariable(distance, energyRatio_K);
//        }
//        //System.out.println("test channel:  "+fadingFactor);
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
        /** reference:https://blog.csdn.net/yuan1164345228/article/details/12451073
        /*  https://www.edaboard.com/showthread.php?136982-generate-Rician-fading-using-Rician-K-factor
        /*  既然K代表了直射信号和瑞利信号的信号比值，因此先把直射信号的信道因子作为1，直接对叠加之后的h做统一的缩放*/
        //distance // km
        //transmitFrequency//Hz
        double lamda = speedOfLight/transmitFrequency;
        //double path_loss = Math.pow(lamda /(4 * distance * Math.PI), 2);// free space path loss, Friis equation
        double path_loss = SatelliteDirectChannelToEarth(distance);
        //double path_loss = Math.pow(lamda/(4 * Math.PI * distance), 2);// free space path loss

        double ratio = path_loss / 1;

        double rayleighFactor = RayleighRandomVariable(distance);
        double h = Math.sqrt(energyRatio_K/(energyRatio_K + 1))*1 +
                Math.sqrt(1/(energyRatio_K + 1))*rayleighFactor;

        fadingFactor = h * ratio;
        //System.out.println("free space loss: "+path_loss+" lamda "+lamda+" fading factor "+fadingFactor+" rayleigh fading: "+rayleighFactor);
        return fadingFactor;
    }

    /**
     * lognormal distribution for shadowing channel
     * @return
     */
    public double LogNormalDistribution(){
//        double factor = channelRandom.nextGaussian();
//        double mean = 0;
//        double var = 1;
//        factor = Math.exp(mean + var * factor);
//
//        double rand = channelRandom.nextDouble();
        double mean = -15.6;//dB
        double factor = dBcoverter(mean, true);
        //System.out.println("shadowing factor:  "+factor);
        return factor;
    }

    /**
     * covert dB value to normal value or converse
     * @param input
     * @return
     */
    public static double dBcoverter(double input, boolean isInputdBvalue){
        if (isInputdBvalue)
            return Math.pow(10, input/10);
        else
            return 10 * Math.log10(input);
    }
    /**
     * General channel of satellite-to-earth links
     * @param distance
     * @return
     */
    public double SatelliteDirectChannelToEarth(double distance){
        //https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=1545873
        //article: Optimum Power and Beam Allocation Based on Traffic Demands and Channel Conditions Over Satellite Downlinks
        double coff = 1;

        double waveLength = speedOfLight/transmitFrequency;
        coff = (waveLength)/(4 * Math.PI * distance);
        //coff = (Math.PI/4)*((transmitAntennaDiameter * receiverAntennaDiameter)/(waveLength * distance));
        coff = transmitAntennaDiameter * receiverAntennaDiameter * Math.pow(coff, 2);
        return coff;

        // reference 2: article- Power Budgets for CubeSat Radios to Support Ground Communications and Inter-Satellite Links
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
        //System.out.println("free space loss: "+path_loss+" lamda "+lamda+" fading factor "+fadingFactor+" rayleigh fading: "+rayleighFactor);
        return fadingFactor;
    }

    /**
     * Calculate channel capacity according to Shannon equation
     */
    public Tuple<Double, Double> channelCapacity(double fadingFactor, double bandwidth){
        double transmitPower_mW = Math.pow(10, this.transmitPower/10) * 0.001;//dBm converter equation = ( 10^(dBm/10) )*0.001
        //double transmitPower_mW = dBcoverter(transmitPower, true);
        double noise_mW = bandwidth * Math.pow(10, spectralDensityNoisePower/10) * 0.001;
        //double noise_mW = bandwidth * dBcoverter(spectralDensityNoisePower, true);

        double SNR = transmitPower_mW * fadingFactor/noise_mW;
        Tuple<Double, Double> capacity = new Tuple<Double, Double>(bandwidth * Math.log(1 + SNR), SNR);//Shannon Equation
        //System.out.println("power test: "+transmitPower_mW + "  "+transmitPower + "  noise:  "+ noise_mW + "  SNR: "+SNR+" SNR_dB:  "+dBcoverter(SNR, false)
        //        +"  capacity: "+capacity+" fadingFactor: "+fadingFactor);
        return capacity;
    }

    /**
     * return random variable
     * @return
     */
    public Random getChannelRandom(){
        return channelRandom;
    }
}
