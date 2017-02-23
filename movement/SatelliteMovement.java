package movement;

import core.Coord;
import core.Settings;
import satellite_orbit.SatelliteOrbit;

public class SatelliteMovement extends MovementModel{
	
	public double a = 8000.; // sma in km
	double e = 0.1; // eccentricity
	double i = 15; // inclination in degrees
	double raan = 0.0; // right ascension of ascending node in degrees
	double w = 0.0; // argument of perigee in degrees
	double ta = 0.0; // true anomaly in degrees
	double[] orbitParameters;
	SatelliteOrbit satelliteOrbit;

	public SatelliteMovement(Settings settings) {
		super(settings);
	}
	
	protected SatelliteMovement(SatelliteMovement rwp) {
		super(rwp);
	}
	
	public void setOrbitParameters(double[] parameters){
		assert parameters.length >= 6 : "传入的卫星轨道参数不全";
			
		this.a = parameters[0]; // sma in km
		this.e = parameters[1]; // eccentricity
		this.i = parameters[2]; // inclination in degrees
		this.raan = parameters[3]; // right ascension of ascending node in degrees
		this.w = parameters[4]; // argument of perigee in degrees
		this.ta = parameters[5]; // true anomaly in degrees
		
		this.orbitParameters = new double[6];
		for (int j = 0 ; j < 6 ; j++){
			this.orbitParameters[j] = parameters[j];
		}

		this.satelliteOrbit = new SatelliteOrbit(this.orbitParameters);
	}
	
	public double[] getSatelliteCoordinate(double time){
		double[][] coordinate = new double[1][3];
		double[] xyz = new double[3];
		
		coordinate = satelliteOrbit.getSatelliteCoordinate(time);
		
		xyz[0] = (coordinate[0][0]+40000);//坐標軸平移
		xyz[1] = (coordinate[0][1]+40000);
		xyz[2] = (coordinate[0][2]+40000);
		
		return xyz;
	}

	public double[] calculateOrbitCoordinate(double[] parameters, double time){
		double[][] coordinate = new double[1][3];
		
		SatelliteOrbit so = new SatelliteOrbit(parameters);
		coordinate = so.getSatelliteCoordinate(time);
		
		return coordinate[0];
	}
	/**
	 * Returns a possible (random) placement for a host
	 * @return Random position on the map
	 */
	@Override
	public Coord getInitialLocation() {
		assert rng != null : "MovementModel not initialized!";
		Coord c = randomCoord();

		//this.lastWaypoint = c;
		return c;
	}
	
	@Override
	public Path getPath() {
		Path p;
		p = new Path(generateSpeed());
		//p.addWaypoint(lastWaypoint.clone());
		//Coord c = lastWaypoint;
		
		//for (int i=0; i<PATH_LENGTH; i++) {
		//	c = randomCoord();
		//	p.addWaypoint(c);	
		//}
		
		//this.lastWaypoint = c;
		return p;
	}
	
	@Override
	public SatelliteMovement replicate() {
		return new SatelliteMovement(this);
	}
	
	protected Coord randomCoord() {
		return new Coord(rng.nextDouble() * getMaxX(),
				rng.nextDouble() * getMaxY());
	}
	
}
