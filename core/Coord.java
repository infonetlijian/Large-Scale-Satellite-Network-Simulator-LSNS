/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.util.Random;
import satellite_orbit.SatelliteOrbit;
/**
 * Class to hold 2D coordinates and perform simple arithmetics and
 * transformations
 */
public class Coord implements Cloneable, Comparable<Coord> {
	private double x;
	private double y;
	private double z;
	/**
	 * Constructor.
	 * @param x Initial X-coordinate
	 * @param y Initial Y-coordinate
	 */
	public Coord(double x, double y) {
		setLocation(x,y);
	}
	public Coord(double x, double y, double z) {
		this.x = x;
		this.y = y;	
		this.z = z;
	}
	/**
	 * Sets the location of this coordinate object
	 * @param x The x coordinate to set
	 * @param y The y coordinate to set
	 */
	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;		
	}
	
	/**
	 * Sets this coordinate's location to be equal to other
	 * coordinates location
	 * @param c The other coordinate
	 */
	public void setLocation(Coord c) {
		this.x = c.x;
		this.y = c.y;		
	}
	
	/**
	 * Moves the point by dx and dy
	 * @param dx How much to move the point in X-direction
	 * @param dy How much to move the point in Y-direction
	 */
	public void translate(double dx, double dy) {
		this.x += dx;
		this.y += dy;
	}
	
	/**
	 * Returns the distance to another coordinate
	 * @param other The other coordinate
	 * @return The distance between this and another coordinate
	 */
	public double distance(Coord other) {
		double dx = this.x - other.x;
		double dy = this.y - other.y;
		double dz = this.z - other.z;
		//return Math.sqrt(dx*dx + dy*dy );
		return Math.sqrt(dx*dx + dy*dy +dz*dz);
	}

	/**
	 * Returns the angle (in radians) from this coord to the given coord
	 * @param other The other coordinate
	 * @return The angle from this coord to the other coord
	 */
	public double angle(Coord other) {
		double dx = this.x - other.x;
		double dy = this.y - other.y;
		
		return Math.atan2(dy, dx);
	}

	/**
	 * Returns the x coordinate
	 * @return x coordinate
	 */
	public double getX() {
		return this.x;
	}

	/**
	 * Returns the y coordinate
	 * @return y coordinate
	 */	
	public double getY() {
		return this.y;
	}
	
	public double getZ() {//新增函数
		return this.z;
	}
	
	/**
	 * Returns a text representation of the coordinate (rounded to 2 decimals)
	 * @return a text representation of the coordinate
	 */
	public String toString() {
		return String.format("(%.2f,%.2f,%.2f)",x,y,z);
	}
	
	/**
	 * Returns a clone of this coordinate
	 */
	public Coord clone() {
		Coord clone = null;
		try {
			clone = (Coord) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return clone;
	}
	
	/**
	 * Checks if this coordinate's location is equal to other coordinate's
	 * @param c The other coordinate
	 * @return True if locations are the same
	 */
	public boolean equals(Coord c) {
		if (c == this) {
			return true;
		}
		else {
			return (x == c.x && y == c.y);
		}
	}

	@Override
	public boolean equals(Object o) {
		return equals((Coord) o);
	}

	/**
	 * Returns a hash code for this coordinate
	 * (actually a hash of the String made of the coordinates)
	 */
	public int hashCode() {
		return (x+","+y).hashCode();
	}

	/**
	 * Compares this coordinate to other coordinate. Coordinate whose y
	 * value is smaller comes first and if y values are equal, the one with
	 * smaller x value comes first.
	 * @return -1, 0 or 1 if this node is before, in the same place or
	 * after the other coordinate
	 */
	public int compareTo(Coord other) {
		if (this.y < other.y) {
			return -1;
		}
		else if (this.y > other.y) {
			return 1;
		}
		else if (this.x < other.x) {
			return -1;
		}
		else if (this.x > other.x) {
			return 1;
		}
		else {
			return 0;
		}
	}

	/**
	 * 将Coord类拓展成三维坐标后，通过此函数进行三维坐标设置
	 * @param x x axis
	 * @param y y axis
	 * @param z z axis
	 */
	public void resetLocation(double x,double y,double z){//设置三维坐标
		this.x=x;
		this.y=y;
		this.z=z;
	}
	/**
	 * 将Coord类拓展成三维坐标后，通过此函数进行三维坐标设置
	 * @param x 传入参量为三维坐标的数组形式
	 */
	public void setLocation3D(double[] x){
		this.x=x[0];
		this.y=x[1];
		this.z=x[2];
	}
	/**新增函数**/
	public void my_Test(double time,double time_0,double []t){
				double[][] coordinate = new double[1][3];
				
				SatelliteOrbit saot = new SatelliteOrbit(t);
				
				coordinate = saot.getSatelliteCoordinate(time);
				this.x = (coordinate[0][0]+40000);
				this.y = (coordinate[0][1]+40000);
				this.z = (coordinate[0][2]+40000);				
	}
}
