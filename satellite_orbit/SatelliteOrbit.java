/* JAT: Java Astrodynamics Toolkit
 * 
  Copyright 2012 Tobias Berthold

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package satellite_orbit;

import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;

import satellite_orbit.Printable;
import satellite_orbit.TwoBody;
//import jat.coreNOSA.cm.cm;


public class SatelliteOrbit implements Printable {

	public double a = 8000.; // sma in km
	double e = 0.1; // eccentricity
	double i = 15; // inclination in degrees
	double raan = 0.0; // right ascension of ascending node in degrees
	double w = 0.0; // argument of perigee in degrees
	double ta = 0.0; // true anomaly in degrees
//	double max;

	int steps = 200;
    double[][] satellitecoordinate = new double[1][3];
	double[][] initcoordinate = new double[1][3];
	
	public SatelliteOrbit() {
		//default Constructor
	}
	public SatelliteOrbit(double x1, double x2, double x3, double x4, double x5, double x6){
		this.a = x1;
		this.e = x2;
		this.i = x3;
		this.raan = x4;
		this.w = x5;
		this.ta = x6;
		this.steps=200;
		this.satellitecoordinate = new double[1][3];
		this.initcoordinate = new double[1][3];
	}
	
	public SatelliteOrbit(double[] t) {
		if(t.length<6||t.length>6) {
			new SatelliteOrbit();
		}
		else if(t.length==6){
			this.a = t[0];
			this.e = t[1];
			this.i = t[2];
			this.raan = t[3];
			this.w = t[4];
			this.ta = t[5];
			this.steps=200;
			this.satellitecoordinate = new double[1][3];
			this.initcoordinate = new double[1][3];
		}
	}
	
	public double[][] getInitLocation() {
		TwoBody tb = new TwoBody(a,e,i,raan,w,ta);
		double period = tb.period();
		double tf = period;
		tb.setSteps(steps);
		tb.propagate(tf);
		
		initcoordinate = new double[1][3];
		initcoordinate[0][0] = tb.rv.x[0];
		initcoordinate[0][1] = tb.rv.x[1];
		initcoordinate[0][2] = tb.rv.x[2];
		
		return this.initcoordinate;
	}
	
	public double[][] getSatelliteCoordinate(double t) {
		TwoBody tb = new TwoBody(a,e,i,raan,w,ta);
		// find out the period of the orbit
		double period = tb.period();
		// set the final time = one orbit period
		double tf = period;
		tb.setSteps(steps);
		// propagate the orbit
		if(t>tf) {
			tb.propagate(t%tf);
		}
		else if(t<=tf) {
			tb.propagate(t);
		}
		
		satellitecoordinate = new double[1][3];
		satellitecoordinate[0][0] = tb.rv.x[0];
		satellitecoordinate[0][1] = tb.rv.x[1];
		satellitecoordinate[0][2] = tb.rv.x[2];
      //  System.out.println("coordinate "+ satellitecoordinate[0][0]);
        return this.satellitecoordinate;
	}
	/**
	 * 返回轨道的周期
	 * @return
	 */
	public double getPeriod(){
		TwoBody tb = new TwoBody(a,e,i,raan,w,ta);
		double period = tb.period();
		return period;
	}

/*	public void make_plot() {
		// create your PlotPanel (you can use it as a JPanel) with a legend at
		// SOUTH
		plot = new Plot3DPanel("SOUTH");
		// add grid plot to the PlotPanel
		add_scene();
	}*/

	public void print(double t, double[] y) {

		// add data point to the plot
		// also print to the screen for warm fuzzy feeling
		//System.out.println("coord ");
	}
	/*
	 * public static void main(String[] args) {
	 * 
	 * 
	 * // put the PlotPanel in a JFrame like a JPanel JFrame frame = new
	 * JFrame("a plot panel"); frame.setSize(600, 600);
	 * frame.setContentPane(plot); frame.setVisible(true);
	 * 
	 * }
	 */
}
