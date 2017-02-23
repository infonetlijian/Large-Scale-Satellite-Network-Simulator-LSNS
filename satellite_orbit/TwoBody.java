/* JAT: Java Astrodynamics Toolkit
 *
 * Copyright (c) 2002, 2003 National Aeronautics and Space Administration. All rights reserved.
 *
 * This file is part of JAT. JAT is free software; you can
 * redistribute it and/or modify it under the terms of the
 * NASA Open Source Agreement
 * 
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * NASA Open Source Agreement for more details.
 *
 * You should have received a copy of the NASA Open Source Agreement
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package satellite_orbit;

import satellite_orbit.Derivatives;
import satellite_orbit.Printable;
import satellite_orbit.Matrix;
import satellite_orbit.VectorN;

import java.io.IOException;

/**
 * <P>
 * The TwoBody Class provides the fundamental operations of the restricted
 * Two Body problem. Most of this material is found in Vallado.
 *
 * @author 
 * @version 1.0
 */
public class TwoBody implements Derivatives
{

	protected double a; // sma in km
	protected double e; // eccentricity
	protected double i; // inclination in radians
	protected double raan; // right ascension of ascending node in radians
	protected double w; // argument of perigee in radians
	public double ta; // true anomaly in radians
	protected double mu = 398600.4415; // default: earth GM in km^3/s^2 (value from JGM-3
	protected double steps = 500.;
	protected int numbers=0;

	/** VectorN containing the position and velocity.
	 */
	public VectorN rv; // position and velocity vector

	/** Default constructor.
	 */
	public TwoBody()
	{
		a = 7778.14;
		e = 0.000128;
		i = 0.0;
		raan = 0.0;
		w = 0.0;
		ta = 0.0;
		double[] temp = new double[6];
		temp = this.randv();
		this.rv = new VectorN(temp);
	}

	/** Construct a TwoBody orbit from 6 orbit elements. Angles are input in degrees.
	 * @param k Kepler elements
	 */
	public TwoBody(KeplerElements k)
	{
		//Constants c = new Constants();
		this.a = k.a;
		this.e = k.e;
		this.i = k.i;
		this.raan = k.raan;
		this.w = k.w;
		this.ta = k.ta;
		rv = new VectorN(randv());
	}

	/** Construct a TwoBody orbit from 6 orbit elements and set mu.
	 *  Angles are input in degrees.
	 * @param k Kepler elements
	 * @param mu 
	 */
	public TwoBody(double mu)
	{
		//Constants c = new Constants();
		this.mu = mu;
		rv = new VectorN(randv());
	}

	/** Construct a TwoBody orbit from 6 orbit elements and set mu.
	 *  Angles are input in degrees.
	 * @param k Kepler elements
	 * @param mu 
	 */
	public TwoBody(double mu, KeplerElements k)
	{
		//Constants c = new Constants();
		this.a = k.a;
		this.e = k.e;
		this.i = k.i;
		this.raan = k.raan;
		this.w = k.w;
		this.ta = k.ta;
		this.mu = mu;
		rv = new VectorN(randv());
	}

	/** Construct a TwoBody orbit from 6 orbit elements. Angles are input in degrees.
	 * @param x1 Semi-major axis in km.
	 * @param x2 Eccentricity.
	 * @param x3 Inclination in degrees.
	 * @param x4 RAAN in degrees.
	 * @param x5 Argument of perigee in degrees.
	 * @param x6 True anomaly in degrees.
	 */
	public TwoBody(double x1, double x2, double x3, double x4, double x5, double x6)
	{
		double[] temp = new double[6];
		this.a = x1;
		this.e = x2;
		this.i = x3 * Constants.deg2rad;
		this.raan = x4 * Constants.deg2rad;
		this.w = x5 * Constants.deg2rad;
		this.ta = x6 * Constants.deg2rad;
		temp = this.randv();
		this.rv = new VectorN(temp);
	}

	/** Construct a TwoBody orbit from 6 orbit elements. Angles are input in degrees.
	 * @param mu GM in km^3/s^2.
	 * @param x1 Semi-major axis in km.
	 * @param x2 Eccentricity.
	 * @param x3 Inclination in degrees.
	 * @param x4 RAAN in degrees.
	 * @param x5 Argument of perigee in degrees.
	 * @param x6 True anomaly in degrees.
	 */
	public TwoBody(double mu, double x1, double x2, double x3, double x4, double x5, double x6)
	{
		//Constants c = new Constants();
		double[] temp = new double[6];
		this.mu = mu;
		this.a = x1;
		this.e = x2;
		this.i = x3 * Constants.deg2rad;
		this.raan = x4 * Constants.deg2rad;
		this.w = x5 * Constants.deg2rad;
		this.ta = x6 * Constants.deg2rad;
		temp = this.randv();
		this.rv = new VectorN(temp);
	}

	/** Construct a TwoBody orbit from a position and velocity vector.
	 * @param r Position vector.
	 * @param v Velocity vector.
	 */

	public TwoBody(VectorN r, VectorN v)
	{
		rv2Elements(r, v);
	}

	/** Construct a TwoBody orbit from a position and velocity vector.
	 * @param mu GM in km^3/s^2
	 * @param r Position vector.
	 * @param v Velocity vector.
	 */
	public TwoBody(double mu, VectorN r, VectorN v)
	{
		this.mu = mu;
		rv2Elements(r, v);
	}

	/** Construct a TwoBody orbit from a double array containing position and velocity.
	 * @param x double[] containing position and velocity.
	 */
	public TwoBody(double[] x)
	{
		if (x.length < 6)
		{
			System.out.println("can't create a TwoBody with array < 6 elements");
		}

		VectorN r = new VectorN(x[0], x[1], x[2]);
		VectorN v = new VectorN(x[3], x[4], x[5]);
		rv2Elements(r, v);
	}

	/** Construct a TwoBody orbit from a double array containing position and velocity.
	 * @param mu GM in km^3/s^2.
	 * @param x double[] containing position and velocity.
	 */
	public TwoBody(double mu, double[] x)
	{
		if (x.length < 6)
		{
			System.out.println("can't create a TwoBody with array < 6 elements");
		}
		this.mu = mu;
		VectorN r = new VectorN(x[0], x[1], x[2]);
		VectorN v = new VectorN(x[3], x[4], x[5]);
		rv2Elements(r, v);
	}

	
	// Setters and Getters
	/**
	 * @param ta true anomaly in degrees
	 */
	public void setTa(double ta)
	{
		this.ta = ta* Constants.deg2rad;
		rv = new VectorN(randv());	//update cartesian elements
	}
	
	public void setSteps(double steps)
	{
		this.steps = steps;
	}
		
	
	/** Copy a TwoBody orbit into a new TwoBody orbit.
	 * @param path PrintWriter.
	 * @return Newly created copy of the TwoBody orbit.
	 */
	public TwoBody copy()
	{
		double gm = this.mu;
		VectorN r = this.getR();
		VectorN v = this.getV();
		TwoBody out = new TwoBody(gm, r, v);
		return out;
	}

	/** Compute the orbit elements from a position and velocity vector.
	 * @param r Position Vector.
	 * @param v Velocity Vector.
	 */
	private void rv2Elements(VectorN r, VectorN v)
	{
		r.checkVectorDimensions(3);
		v.checkVectorDimensions(3);
		//Constants c = new Constants();
		VectorN evec = new VectorN(3); // e vector
		VectorN k = new VectorN(3); // unit vector in z direction

		this.rv = new VectorN(r, v);

		double rmag = r.mag();
		double vmag = v.mag();
		double energy = vmag * vmag / 2.0 - this.mu / rmag;

		k.x[0] = 0.0;
		k.x[1] = 0.0;
		k.x[2] = 1.0;

		VectorN h = r.crossProduct(v);
		VectorN n = k.crossProduct(h);

		double rdotv = r.dotProduct(v);

		double q1 = (vmag * vmag - this.mu / rmag) / this.mu;
		double q2 = rdotv / this.mu;

		evec.x[0] = q1 * r.x[0] - q2 * v.x[0];
		evec.x[1] = q1 * r.x[1] - q2 * v.x[1];
		evec.x[2] = q1 * r.x[2] - q2 * v.x[2];

		this.e = evec.mag();

		if (e != 1.0)
		{
			this.a = -this.mu / (2.0 * energy);
		} else
		{
			this.a = 1.0E30;
			System.out.println("parabolic orbit");
		}

		this.i = Math.acos(h.x[2] / h.mag()); // inclination

		this.raan = Math.acos(n.x[0] / n.mag()); // raan
		if (n.x[1] < 0.0)
		{
			this.raan = 2.0 * Constants.pi - raan;
		}

		this.w = Math.acos(n.dotProduct(evec) / (n.mag() * e));
		if (evec.x[2] < 0.0)
		{
			this.w = 2.0 * Constants.pi - this.w;
		}

		if (i == 0.0) // equatorial orbit, things blow up
		{
			//		   System.out.println("KeplerElements: equatorial orbit, RAAN no good");
			this.raan = 0.0;
			this.w = Math.acos(evec.x[0] / e);
			if (evec.x[1] < 0.0)
			{
				this.w = 2.0 * Constants.pi - this.w;
			}
		}

		if (i == Constants.pi) // equatorial orbit, things blow up
		{
			//		   System.out.println("KeplerElements: equatorial orbit, RAAN no good");
			this.raan = 0.0;
			this.w = Math.acos(evec.x[0] / e);
			if (evec.x[1] > 0.0)
			{
				this.w = 2.0 * Constants.pi - this.w;
			}
		}

		this.ta = Math.acos(evec.dotProduct(r) / (e * rmag));
		if (rdotv < 0.0)
		{
			this.ta = 2.0 * Constants.pi - this.ta;
		}
	}

	/** Get the semi-major axis.
	 * @return Semi-major axis value.
	 */

	public double semiMajorAxis()
	{
		return this.a;
	}

	/** Get the eccentricity
	 * @return Eccentricity
	 */

	public double eccentricity()
	{
		return this.e;
	}

	/** Get the inclination.
	 * @return Inclination in radians.
	 */

	public double inclination()
	{
		return this.i;
	}

	/** Get the RAAN.
	 * @return RAAN in radians.
	 */

	public double RAAN()
	{
		return this.raan;
	}

	/** Get the argument of perigee.
	 * @return Argument of perigee in radians.
	 */

	public double argPerigee()
	{
		return this.w;
	}

	/** Get true anomaly.
	 * @return True anomaly in radians.
	 */

	public double trueAnomaly()
	{
		return this.ta;
	}

	/** Get eccentric anomaly.
	 * @return Eccentric anomaly in radians.
	 */

	public double eccentricAnomaly()
	{
		double cta = Math.cos(ta);
		double e0 = Math.acos((e + cta) / (1.0 + e * cta));
		return e0;
	}

	/** Get mean anomaly.
	 * @return Mean anomaly in radians.
	 */

	public double meanAnomaly()
	{
		double ea = eccentricAnomaly();
		double m = ea - e * Math.sin(ea);
		return m;
	}

	/** Get mean motion
	 * @return Mean motion in radians/s.
	 */

	public double meanMotion()
	{
		double acubed = a * a * a;
		double n = Math.sqrt(this.mu / acubed);
		return n;
	}

	/** Get the orbital period.
	 * @return Period in s.
	 */

	public double period()
	{
		double n = meanMotion();
		double period = 2.0 * Constants.pi / n;
		return period;
	}

	/** Print the orbit elements.
	 * @param title A title for the element set.
	 */

	public void printElements(String title)
	{
		System.out.println(" Kepler Elset: " + title);
		System.out.println("   a = " + this.a);
		System.out.println("   e = " + this.e);
		System.out.println("   i = " + (this.i * Constants.rad2deg));
		System.out.println("   raan = " + (this.raan * Constants.rad2deg));
		System.out.println("   w = " + (this.w * Constants.rad2deg));
		System.out.println("   ta = " + (this.ta * Constants.rad2deg));
	}

	/** Print the position and velocity vector.
	 * @param title Title for the position and velocity vector.
	 */

	public void printVector(String title)
	{
		this.rv.print("r and v vector: " + title);
	}

	/** Print the orbit elements and position and velocity vectors.
	 * @param title Title for the orbit elements.
	 */

	public void print(String title)
	{
		this.printElements(title);
		this.printVector(title);
	}

	/** Compute the PQW to ECI transformation matrix.
	 * @return Matrix containing the transformation.
	 */

	public Matrix PQW2ECI()
	{
		double cw = Math.cos(w);
		double sw = Math.sin(w);
		double craan = Math.cos(raan);
		double sraan = Math.sin(raan);
		double ci = Math.cos(i);
		double si = Math.sin(i);

		Matrix out = new Matrix(3, 3);
		out.A[0][0] = craan * cw - sraan * sw * ci;
		out.A[0][1] = -craan * sw - sraan * cw * ci;
		out.A[0][2] = sraan * si;
		out.A[1][0] = sraan * cw + craan * sw * ci;
		out.A[1][1] = -sraan * sw + craan * cw * ci;
		out.A[1][2] = -craan * si;
		out.A[2][0] = sw * si;
		out.A[2][1] = cw * si;
		out.A[2][2] = ci;
		return out;
	}

	/** Compute the RSW to ECI transformation matrix.
	 * @return Matrix containing the transformation.
	 */

	public Matrix RSW2ECI()
	{
		VectorN r = this.getR();
		//VectorN v = this.getV();
		VectorN h = this.getH();
		VectorN rhat = r.unitVector();
		VectorN what = h.unitVector();
		VectorN s = what.crossProduct(rhat);
		VectorN shat = s.unitVector();
		Matrix out = new Matrix(3, 3);
		out.setColumn(0, rhat);
		out.setColumn(1, shat);
		out.setColumn(2, what);
		return out;
	}

	/** Compute the ECI position and velocity vectors.
	 * @return ECI position and velocity vector
	 */

	public double[] randv()
	{
		double p = a * (1.0 - e * e);
		double cta = Math.cos(ta);
		double sta = Math.sin(ta);
		double opecta = 1.0 + e * cta;
		double sqmuop = Math.sqrt(this.mu / p);

		VectorN xpqw = new VectorN(6);
		xpqw.x[0] = p * cta / opecta;
		xpqw.x[1] = p * sta / opecta;
		xpqw.x[2] = 0.0;
		xpqw.x[3] = -sqmuop * sta;
		xpqw.x[4] = sqmuop * (e + cta);
		xpqw.x[5] = 0.0;

		Matrix cmat = PQW2ECI();

		VectorN rpqw = new VectorN(xpqw.x[0], xpqw.x[1], xpqw.x[2]);
		VectorN vpqw = new VectorN(xpqw.x[3], xpqw.x[4], xpqw.x[5]);

		VectorN rijk = cmat.times(rpqw);
		VectorN vijk = cmat.times(vpqw);

		double[] out = new double[6];

		for (int i = 0; i < 3; i++)
		{
			out[i] = rijk.x[i];
			out[i + 3] = vijk.x[i];
		}

		return out;
	}

	/** Get the position vector.
	 * @return Position vector.
	 */

	public VectorN getR()
	{
		VectorN out = new VectorN(3);
		out.x[0] = this.rv.x[0];
		out.x[1] = this.rv.x[1];
		out.x[2] = this.rv.x[2];
		return out;
	}

	/** Get the velocity vector.
	 * @return Velocity vector.
	 */

	public VectorN getV()
	{
		VectorN out = new VectorN(3);
		out.x[0] = this.rv.x[3];
		out.x[1] = this.rv.x[4];
		out.x[2] = this.rv.x[5];
		return out;
	}

	/** Get the angular momentum vector.
	 * @return angular momentum vector.
	 */

	public VectorN getH()
	{
		VectorN r = new VectorN(rv.x[0], rv.x[1], rv.x[2]);
		VectorN v = new VectorN(rv.x[3], rv.x[4], rv.x[5]);
		VectorN out = r.crossProduct(v);
		return out;
	}

	/** Get the angular momentum magnitude.
	 * @return angular momentum magnitude.
	 */

	public double getHmag()
	{
		VectorN h = this.getH();
		double out = h.mag();
		return out;
	}

	/** Compute the local acceleration due to gravity.
	 * @return g vector.
	 */

	public VectorN local_grav()
	{
		VectorN r = this.getR();
		double rmag = r.mag();
		double muor3 = this.mu / (rmag * rmag * rmag);

		double gx = -1.0 * (muor3 * r.x[0]);
		double gy = -1.0 * (muor3 * r.x[1]);
		double gz = -1.0 * (muor3 * r.x[2]);

		VectorN grav = new VectorN(gx, gy, gz);
		return grav;
	}

	/** Compute the gravity gradient matrix (dg/dr).
	 * @return Gravity gradient matrix.
	 */

	public Matrix gravityGradient()
	{
		VectorN r = this.getR();
		double rmag = r.mag();
		double r2 = rmag * rmag;
		double muor3 = this.mu / (r2 * rmag);
		double jk = 3.0 * muor3 / (r2);
		Matrix gg = new Matrix(3, 3);

		double xx = r.x[0];
		double yy = r.x[1];
		double zz = r.x[2];

		gg.A[0][0] = jk * xx * xx - muor3;
		gg.A[0][1] = jk * xx * yy;
		gg.A[0][2] = jk * xx * zz;

		gg.A[1][0] = gg.A[0][1];
		gg.A[1][1] = jk * yy * yy - muor3;
		gg.A[1][2] = jk * yy * zz;

		gg.A[2][0] = gg.A[0][2];
		gg.A[2][1] = gg.A[1][2];
		gg.A[2][2] = jk * zz * zz - muor3;

		return gg;
	}

	/** Solve Kepler's equation. Computes eccentric anomaly give mean anomaly and eccentricity.
	 * @param mean_anomaly Mean anomaly in radians.
	 * @param ecc eccentricity.
	 * @return Eccentric anomaly in radians.
	 */

	public static double solveKepler(double mean_anomaly, double ecc)
	{
		if (Math.abs(ecc) < 0.000000001)
		{
			return mean_anomaly;
		}
		int maxit = 10000;
		int it = 0;

		double de = 1000.0;

		double ea = mean_anomaly;
		double old_m = mean_anomaly;

		while ((it < maxit) && (Math.abs(de) > 1.0E-10))
		{
			double new_m = ea - ecc * Math.sin(ea);
			de = (old_m - new_m) / (1.0 - ecc * Math.cos(ea));
			ea = ea + de;
			it = it + 1;
		}
		return ea;
	}

	/** Propagate a TwoBody orbit from t0 to tf using Kepler's equation.
	 * @param t0 Initial time in s.
	 * @param tf Final time in s.
	 * @param print_switch print switch (true = print, false = don't print).
	 * @throws IOException If file fails to open.
	 */
	public void propagate(double t0, double tf, Printable pr, boolean print_switch)
	{
		double[] temp = new double[6];


		// Determine step size
		double n = this.meanMotion();
		double period = this.period();
		double dt = period / steps;
		if ((t0 + dt) > tf) // check to see if we're going past tf
		{
			dt = tf - t0;
		}

		// determine initial E and M
		double sqrome2 = Math.sqrt(1.0 - this.e * this.e);
		double cta = Math.cos(this.ta);
		double sta = Math.sin(this.ta);
		double sine0 = (sqrome2 * sta) / (1.0 + this.e * cta);
		double cose0 = (this.e + cta) / (1.0 + this.e * cta);
		double e0 = Math.atan2(sine0, cose0);

		double ma = e0 - this.e * Math.sin(e0);

		// determine sqrt(1+e/1-e)

		//double q = Math.sqrt((1.0 + this.e) / (1.0 - this.e));

		// initialize t

		double t = t0;

		if (print_switch)
		{
			//System.out.println(a+"  "+e+"  "+i+"  "+raan+"  "+w+"  "+ta);
			temp = this.randv();
			pr.print(t, temp);
//			for(int i=0;i<temp.length;++i) {
//				System.out.println("temp "+temp[i]);
//			}
		}
//		System.out.println("t "+ t);
//		System.out.println("tf "+ tf);

		while (t < tf)
		{
			ma = ma + n * dt;
			double ea = solveKepler(ma, this.e);

			double sinE = Math.sin(ea);
			double cosE = Math.cos(ea);
			double den = 1.0 - this.e * cosE;

			double sinv = (sqrome2 * sinE) / den;
			double cosv = (cosE - this.e) / den;

			this.ta = Math.atan2(sinv, cosv);
			if (this.ta < 0.0)
			{
				this.ta = this.ta + 2.0 * Constants.pi;
			}

			t = t + dt;

			temp = this.randv();
			this.rv = new VectorN(temp);

			if (print_switch)
			{
				pr.print(t, temp);
				/*for(int i=0;i<temp.length;++i) {
					System.out.println("temp "+temp[i]);
					numbers++;
				}*/
			}
			if ((t + dt) > tf)
			{
				dt = tf - t;
			}
			if(t==tf)
				;//System.out.println("最终坐标  "+ temp[0]);
			//System.out.println("number "+ numbers);

		}
		//System.out.println("t 时刻坐标  "+ this.rv.x[0]);
		//System.out.println("numbers "+ numbers);
	}

	public void propagate(double tf)
	{
		double[] temp = new double[6];
		double t0=0.0;

		// Determine step size
		double n = this.meanMotion();
		double period = this.period();
		double dt = period / steps;
		if ((t0 + dt) > tf) // check to see if we're going past tf
		{
			dt = tf - t0;
		}

		// determine initial E and M
		double sqrome2 = Math.sqrt(1.0 - this.e * this.e);
		double cta = Math.cos(this.ta);
		double sta = Math.sin(this.ta);
		double sine0 = (sqrome2 * sta) / (1.0 + this.e * cta);
		double cose0 = (this.e + cta) / (1.0 + this.e * cta);
		double e0 = Math.atan2(sine0, cose0);

		double ma = e0 - this.e * Math.sin(e0);

		// determine sqrt(1+e/1-e)

		//double q = Math.sqrt((1.0 + this.e) / (1.0 - this.e));

		// initialize t

		double t = t0;

	/*	if (print_switch)
		{
			temp = this.randv();
			pr.print(t, temp);
			for(int i=0;i<temp.length;++i) {
				System.out.println("temp "+temp[i]);
			}
		}
		System.out.println("t "+ t);
		System.out.println("tf "+ tf);*/

		while (t < tf)
		{
			ma = ma + n * dt;
			double ea = solveKepler(ma, this.e);

			double sinE = Math.sin(ea);
			double cosE = Math.cos(ea);
			double den = 1.0 - this.e * cosE;

			double sinv = (sqrome2 * sinE) / den;
			double cosv = (cosE - this.e) / den;

			this.ta = Math.atan2(sinv, cosv);
			if (this.ta < 0.0)
			{
				this.ta = this.ta + 2.0 * Constants.pi;
			}

			t = t + dt;

			temp = this.randv();
			this.rv = new VectorN(temp);

		/*	if (print_switch)
			{
				pr.print(t, temp);
				for(int i=0;i<temp.length;++i) {
					System.out.println("temp "+temp[i]);
					numbers++;
				}
			} */
			if ((t + dt) > tf)
			{
				dt = tf - t;
			}
			//System.out.println("最终坐标  "+ temp[0]);
			//System.out.println("number "+ numbers);

		}
		//System.out.println("t 时刻坐标  "+ this.rv.x[0]);
		//System.out.println("numbers "+ numbers);
	}
	
	public void propagate(double t0, double tf, Printable pr, boolean print_switch, double steps)
	{
		double[] temp = new double[6];

		this.steps=steps;

		// Determine step size
		double n = this.meanMotion();
		double period = this.period();
		double dt = period / steps;
		if ((t0 + dt) > tf) // check to see if we're going past tf
		{
			dt = tf - t0;
		}

		// determine initial E and M
		double sqrome2 = Math.sqrt(1.0 - this.e * this.e);
		double cta = Math.cos(this.ta);
		double sta = Math.sin(this.ta);
		double sine0 = (sqrome2 * sta) / (1.0 + this.e * cta);
		double cose0 = (this.e + cta) / (1.0 + this.e * cta);
		double e0 = Math.atan2(sine0, cose0);

		double ma = e0 - this.e * Math.sin(e0);

		// determine sqrt(1+e/1-e)

		//double q = Math.sqrt((1.0 + this.e) / (1.0 - this.e));

		// initialize t

		double t = t0;

		if (print_switch)
		{
			temp = this.randv();
			pr.print(t, temp);
		}

		while (t < tf)
		{
			ma = ma + n * dt;
			double ea = solveKepler(ma, this.e);

			double sinE = Math.sin(ea);
			double cosE = Math.cos(ea);
			double den = 1.0 - this.e * cosE;

			double sinv = (sqrome2 * sinE) / den;
			double cosv = (cosE - this.e) / den;

			this.ta = Math.atan2(sinv, cosv);
			if (this.ta < 0.0)
			{
				this.ta = this.ta + 2.0 * Constants.pi;
			}

			t = t + dt;

			temp = this.randv();
			this.rv = new VectorN(temp);

			if (print_switch)
			{
				pr.print(t, temp);
			}

			if ((t + dt) > tf)
			{
				dt = tf - t;
			}

		}
	}

	/** Propagate a TwoBody orbit from t0 to tf using Kepler's equation.
	 * @param t0 Initial time in s.
	 * @param tf Final time in s.
	 */
	public void propagate(double t0, double tf)
	{
		double[] temp = new double[6];

		// Determine step size
		double n = this.meanMotion();
		double period = this.period();
		double dt = period / steps;
		if ((t0 + dt) > tf) // check to see if we're going past tf
		{
			dt = tf - t0;
		}

		// determine initial E and M
		double sqrome2 = Math.sqrt(1.0 - this.e * this.e);
		double cta = Math.cos(this.ta);
		double sta = Math.sin(this.ta);
		double sine0 = (sqrome2 * sta) / (1.0 + this.e * cta);
		double cose0 = (this.e + cta) / (1.0 + this.e * cta);
		double e0 = Math.atan2(sine0, cose0);

		double ma = e0 - this.e * Math.sin(e0);

		// determine sqrt(1+e/1-e)

		//double q = Math.sqrt((1.0 + this.e) / (1.0 - this.e));

		// initialize t

		double t = t0;

		while (t < tf)
		{
			ma = ma + n * dt;
			double ea = solveKepler(ma, this.e);

			double sinE = Math.sin(ea);
			double cosE = Math.cos(ea);
			double den = 1.0 - this.e * cosE;

			double sinv = (sqrome2 * sinE) / den;
			double cosv = (cosE - this.e) / den;

			this.ta = Math.atan2(sinv, cosv);
			if (this.ta < 0.0)
			{
				this.ta = this.ta + 2.0 * Constants.pi;
			}

			t = t + dt;

			temp = this.randv();
			this.rv = new VectorN(temp);

			if ((t + dt) > tf)
			{
				dt = tf - t;
			}

		}
	}

	/** Compute the derivatives for numerical integration of two body equations
	 * of motion.
	 * @return double [] containing the derivatives.
	 * @param t Time (not used).
	 * @param y State vector.
	 */

	public double[] derivs(double t, double[] y)
	{
		double out[] = new double[6];
		VectorN r = new VectorN(y[0], y[1], y[2]);
		double rmag = r.mag();
		double rcubed = rmag * rmag * rmag;
		double muorc = -1.0 * this.mu / rcubed;

		out[0] = y[3];
		out[1] = y[4];
		out[2] = y[5];
		out[3] = muorc * y[0];
		out[4] = muorc * y[1];
		out[5] = muorc * y[2];

		return out;
	}

	/** Method for testing the class.
	 * @param args Input arguments (not used).
	 */

	public static void main(String args[])
	{
	}


}


