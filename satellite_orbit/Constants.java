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

/** Astrodynamical Constants
 * @author 
 * @version 1.0
 */
public class Constants {
    /** GM of the Earth in km^3/s^2 from JGM-3.
     */
    public static final double mu = 398600.4415;
    /** Earth's J2 Value from JGM-3.
     */
    public static final double j2 = 0.00108263;
    /** Acceleration due to gravity near earth in m/s^2.
     */
    public static final double g0 = -9.780;
    /** Speed of Light in m/s from IAU 1976.  Same as c but used for Matlab interface.
     */
    public static final double clight = 299792458.0;
    /** Speed of Light in m/s from IAU 1976.
     */
    public static final double c = 299792458.0;
    /** PI.
     */
    public static final double pi = Math.acos(-1.0);
    /** Conversion factor for degrees to radians.
     */
    public static final double deg2rad = pi/180.0;
    /** Conversion factor for arcsec to radians.
     */
    public static final double arcsec2rad = pi/648000.0;

    /** Conversion factor for radians to degrees.
     */
    public static final double rad2deg = 180.0/pi;

    /** Earth atmosphere model parameter.
     */
    public static final double h_0 = 920000.0;              // atmosphere model parameter
    /** Earth atmosphere model parameter.
     */
    public final static double rho_0 = 4.36E-14;            // atmosphere model parameter
    /** Earth atmosphere model parameter.
     */
    public final static double gamma_0 = 5.381E-06;         // atmosphere model parameter
    /** Earth's rotation rate in rad/s.
     */
    //public final static double omega_e = 7.2921157746E-05;  // earth rotation rate
    public final static double omega_e = 7.292115E-05;  // IERS 1996 conventions
    //public final static double omega_e = 7.2921158553E-05;  // Vallado
    
    /** Mean Earth Radius in km from WGS-84.
     */
    public static final double re = 6378.137;              // radius of earth in kilometers
    /** Equatorial radius of earth in m from WGS-84
     */
    //public final static double R_Earth = 6378.137e3;      // Radius Earth [m]; WGS-84
    public final static double R_Earth = 6378.1363e3;      // Radius Earth [m]; STK JGM3

    /** Equatorial radius of earth in m from WGS-84
     */
    public final static double rEarth_WGS84 = 6378.137e3;      // Radius Earth [m]; WGS-84

    /** Equatorial radius of earth in m from STK JGM3
     */
    public final static double rEarth_STKJGM3 = 6378.1363e3;      // Radius Earth [m]; STK JGM3

    /** Mean radius of the Sun in m
     */
    //public final static double R_Sun = 6.960e8;			   // [m] scienceworld.wolfram.com
    public final static double R_Sun = 6.9599e8;			   // [m] STK
    
    /** Mean radius of the Sun in m from wolfram
     */
    public final static double rSun_ScienceWorld = 6.960e8;    // [m] scienceworld.wolfram.com
    
    /** Mean radius of the Sun in m
     */
    public final static double rSun_STK = 6.9599e8;			   // [m] STK
    
    /** Flattening factor of earth
     */
    //public final static double f_Earth = 1.0/298.257223563; // Flattening; WGS-84
    //public final static double f_Earth = 0.003353; // STK HPOP
    public final static double f_Earth = 0.00335281; // STK HPOP - 2

    /** Flattening factor of earth from WGS-84
     */
    public final static double fEarth_WGS84 = 1.0/298.257223563; // Flattening; WGS-84

    /** Flattening factor of earth from STK HPOP-2
     */
    public final static double fEarth_STKHPOP2 = 0.00335281; // STK HPOP - 2

    /** Solar radiation pressure at 1 AU in N/m^2
     */
    public final static double P_Sol       = 4.560E-6;          // [N/m^2] (~1367 W/m^2); IERS 96

    /** Astronomical Unit [m]; IAY 1976
     */
    public final static double AU        = 149597870000.0;		// [m]

    /** Earth gravity constant in m^3/s^2 from JGM3
     */
    public final static double GM_Earth    = 398600.4415e+9;    // [m^3/s^2]; JGM3
    
    /** Earth gravity constant in m^3/s^2 from WGS84
     */
    public final static double GM_WGS84    = 398600.5e+9;    // [m^3/s^2]; WGS-84

    /** Earth's rotation rate in rad/s.
     */
    public final static double WE_WGS84 = 7.2921151467E-05;  // earth rotation rate

    /** Earth's rotation rate in rad/s from IERS 1996
     */
    public final static double wEarth_IERS96 = 7.292115E-05;  // IERS 1996 conventions
    
    /** Earth's rotation rate in rad/s from Vallado
     */
    public final static double wEarth_Vallado = 7.2921158553E-05;  // Vallado
    
    /** Sun gravity constant in m^3/s^2
     */
    //public final static double GM_Sun      = 1.32712438e+20;    // [m^3/s^2]; IAU 1976
    public final static double GM_Sun      = 1.3271220e+20;    // [m^3/s^2]; STK
    
    /** Sun gravity constant in m^3/s^2 from IAU 1976
     */
    public final static double muSun_IAU76 = 1.32712438e+20;    // [m^3/s^2]; IAU 1976
    
    /** Sun gravity constant in m^3/s^2 from STK
     */
    public final static double muSun_STK      = 1.3271220e+20;    // [m^3/s^2]; STK
    
    /** Moon gravity constant in m^3/s^2
     */
    //public final static double GM_Moon     = GM_Earth/81.300587;// [m^3/s^2]; DE200
    public final static double GM_Moon     = 4.90279490e+12;    // [m^3/s^2]; STK
    //public final static double GM_Moon     = GM_Earth*0.012300034;    // [m^3/s^2]; JPL ssd

    /** Moon gravity constant in m^3/s^2 from DE200
     */
    public final static double muMoon_DE200   = GM_Earth/81.300587;// [m^3/s^2]; DE200

    /** Moon gravity constant in m^3/s^2 from STK
     */
    public final static double muMoon_STK     = 4.90279490e+12;    // [m^3/s^2]; STK

    /** Moon gravity constant in m^3/s^2 from JPL SSD
     */
    public final static double muMoon_JPLSSD  = GM_Earth*0.012300034;    // [m^3/s^2]; JPL ssd

    /** Obliquity of the ecliptic, J2000 in degrees
     */
    public final static double eps = 23.43929111;


    
    // Constants in various distance and time units
    
    public final static double GM_Earth_km_day    =    2.97553635177984E15;// [km^3/day^2]; JGM3
    public final static double GM_Sun_km_s        =    1.3271220e+11;    // [km^3/s^2]; STK
    public final static double GM_Sun_km_day      =    9.90691264512E20;    // [km^3/day^2]; STK
    
    
   
    
    /** Default constructor.
     */
    public Constants() {
    }
}