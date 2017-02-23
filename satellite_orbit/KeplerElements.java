/* JAT: Java Astrodynamics Toolkit
 *
 * Copyright (c) 2002 National Aeronautics and Space Administration. All rights reserved.
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

/**
 * Simple class to store Kepler elements 
 * @author Tobias Berthold
 * @version 1.0
 */

public class KeplerElements
{
	/** Semimajor axis in km */ 
	public double a; // sma in km
	/** Eccentricity */ 
	public double e; // eccentricity
	/** Inclination in radians */
	public double i; // inclination in radians
	/** Right ascension of the ascending node in radians */
	public double raan; // right ascension of ascending node in radians
	/** Argument of perigee in radians */
	public double w; // argument of perigee in radians
	/** True anomaly in radians */
	public double ta; // true anomaly in radians

	/**
	 * @param a Semimajor axis in km
	 * @param e Eccentricity
	 * @param i Inclination in radians
	 * @param raan Right ascension of ascending node in radians
	 * @param w Argument of perigee in radians
	 * @param ta True anomaly in radians
	 */
	public KeplerElements(double a, double e, double i, double raan, double w, double ta)
	{
		this.a = a;
		this.e = e;
		this.i = i;
		this.raan = raan;
		this.w = w;
		this.ta = ta;
	}
}

/** Orbital elements for the planets from "Explanatory supplement to the Astronomical 
 * Almanac" by Kenneth Seidelmann 
 */
//	public static final Kepler_Elements mars_elements =
//		new Kepler_Elements(227939186, 0.0934006199474, Rad(1.85061), Rad(49.57854), Rad(336.04084), 355.45332);
//	public static final Kepler_Elements earth_moon_elements =
//		new Kepler_Elements(149598023, 0.01671022, Rad(0.00005), Rad(-11.26064), Rad(102.94719), 100.46435);
