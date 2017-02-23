/* 
 * Copyright 2013 Aalto University Comnet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;
import core.*;

/**
 * Floating Content message router.
 * Replicates a message inside the replication range (r) to every node met.
 * Replicates a message inside the buffer zone (r--a) to every node as per replication algorithm.
 * Deletes messages outside the anchor zone as per deletion policy.
 * Deletes messages inside the buffer zone for encounter-based deletion policy per deletion algorithm.
 * Drop oldest messages to make room for new ones.
 * Prioritizes message for replication during an encounter as per replication policy.
 */

/**
 * Replication policy options
 * fifo: do nothing special; replicate in the order in which they are found in the buffer
 * rnd: shuffle randomly
 * saf: smallest anchor zone (area) first
 * svf[2]: smallest cylinder (area x volume) first; svf2 uses area, svf just radius
 * stf[2]: smallest total (area x volume x ttl) first; stf2 uses area, stf just radius
 */

public class FloatingContentRouter extends ActiveRouter {
	public static final int REPL_FIFO  = 0;
	public static final int REPL_RND   = 1;
	public static final int REPL_SAF   = 2;
	public static final int REPL_SVF   = 3;
	public static final int REPL_STF   = 4;
	public static final int REPL_SVF2  = 5;
	public static final int REPL_STF2  = 6;

	public static final int REPL_ALG_NONE   = 0;
	public static final int REPL_ALG_LINEAR = 1;
	public static final int REPL_ALG_EXP    = 2;
	public static final int REPL_ALG_COSINE = 3;
	public static final int REPL_ALG_FIXED  = 4;

	public static final int DEL_ALG_NONE    = 0;
	public static final int DEL_ALG_LINEAR  = 1;
	public static final int DEL_ALG_EXP     = 2;
	public static final int DEL_ALG_COSINE  = 3;
	public static final int DEL_ALG_FIXED   = 4;

	public static final int LOC_SRC_NONE    = 0;
	public static final int LOC_SRC_GPS     = 1;
	public static final int LOC_SRC_HOTSPOT = 2;

	public static final int DEL_ENCOUNTER   = 0;
	public static final int DEL_IMMEDIATE   = 1;

        private static Random locRng = null;

	private int seed = 1;
	private int deletion_policy = DEL_ENCOUNTER;
	private int replication_policy = REPL_FIFO;
	private int replication_algorithm = REPL_ALG_NONE;
	private int deletion_algorithm = DEL_ALG_NONE;
	private double replication_fixed = 0.0;
	private double deletion_fixed = 0.0;

	private boolean location_error = false;
	private double location_error_min = 0;
	private double location_error_max = 0;
	private double location_update_interval = 0;
	private int location_source = LOC_SRC_GPS;
        private double ratio = 1.0;
        private Coord last_known_location = null;

	public static final String FC_SEED        = "seed";
	public static final String FC_NS          = "FloatingContentRouter";
	public static final String FC_DELETION    = "deletionPolicy";
	public static final String FC_REPLICATION = "replicationPolicy";
	public static final String FC_REPL_ALG    = "replicationAlgorithm";
	public static final String FC_DEL_ALG     = "deletionAlgorithm";

	public static final String FC_LOCATION_ERROR_RANGE = "locationError";
	public static final String FC_LOCATION_UPDATE_INTERVAL = "locationUpdate";
	public static final String FC_LOCATION_SOURCE = "locationSource";
	public static final String FC_LOCATION_RATIO  = "locationRatio";

	/** Message property keys */ 

	public static final String FC_SRCLOC = "srcloc";
	public static final String FC_ANCHOR = "anchor";
	public static final String FC_A = "a";
	public static final String FC_R = "r";
	public static final String FC_TYPE = "type";
	public static final String FC_TTL = "ttl";
	public static final String FC_TTL_VAL = "ttlval";

	protected Random rng;
	protected Random replRng;
	protected Random delRng;

	private List<Connection>   new_conns = new ArrayList<Connection> ();
	
	private double gpsLastReading = 0;
	private double gpsUpdateInterval = 0;
	private double gpsErrorMin = 0;
	private double gpsErrorMax = 0;
	private Coord  gpsLocation = new Coord (-1,-1);
	//this is for the RMS error calculation
	protected Random rdAngle;
    protected Random rdDistance;

	/**
	 * Constructor. Creates a new message router based on the settings in
	 * the given Settings object.
	 * @param s The settings object
	 */
	public FloatingContentRouter(Settings s) {
		super(s);
		Settings fcSettings = new Settings(FC_NS);

		seed = fcSettings.getInt(FC_SEED);

		if (FloatingContentRouter.locRng == null) {
		    FloatingContentRouter.locRng = new Random (seed);
		}

		if (fcSettings.contains (FC_DELETION)) {
			deletion_policy = DEL_ENCOUNTER;
			if (fcSettings.getSetting(FC_DELETION).equals ("immediate"))
				deletion_policy = DEL_IMMEDIATE;
		}
		replication_policy = REPL_FIFO;
		if (fcSettings.getSetting(FC_REPLICATION).equals ("rnd"))
			replication_policy = REPL_RND;
		else if (fcSettings.getSetting(FC_REPLICATION).equals ("saf"))
			replication_policy = REPL_SAF;
		else if (fcSettings.getSetting(FC_REPLICATION).equals ("svf"))
			replication_policy = REPL_SVF;
		else if (fcSettings.getSetting(FC_REPLICATION).equals ("stf"))
			replication_policy = REPL_STF;
		else if (fcSettings.getSetting(FC_REPLICATION).equals ("svf2"))
			replication_policy = REPL_SVF2;
		else if (fcSettings.getSetting(FC_REPLICATION).equals ("stf2"))
			replication_policy = REPL_STF2;

		replication_algorithm = REPL_ALG_NONE;
		if (fcSettings.getSetting(FC_REPL_ALG).equals ("exp"))
			replication_algorithm = REPL_ALG_EXP;
		else if (fcSettings.getSetting(FC_REPL_ALG).equals ("cosine"))
			replication_algorithm = REPL_ALG_COSINE;
		else if (fcSettings.getSetting(FC_REPL_ALG).equals ("linear"))
			replication_algorithm = REPL_ALG_LINEAR;
		else if (fcSettings.getSetting(FC_REPL_ALG).equals ("none"))
			replication_algorithm = REPL_ALG_NONE;
		else {
			replication_algorithm = REPL_ALG_FIXED;
			replication_fixed = fcSettings.getDouble (FC_REPL_ALG);
		}

		deletion_algorithm = DEL_ALG_NONE;
		if (fcSettings.getSetting(FC_DEL_ALG).equals ("exp"))
			deletion_algorithm = DEL_ALG_EXP;
		else if (fcSettings.getSetting(FC_DEL_ALG).equals ("cosine"))
			deletion_algorithm = DEL_ALG_COSINE;
		else if (fcSettings.getSetting(FC_DEL_ALG).equals ("linear"))
			deletion_algorithm = DEL_ALG_LINEAR;
		else if (fcSettings.getSetting(FC_DEL_ALG).equals ("none"))
			deletion_algorithm = DEL_ALG_NONE;
		else {
			deletion_algorithm = REPL_ALG_FIXED;
			deletion_fixed = fcSettings.getDouble (FC_DEL_ALG);
		}

		if (fcSettings.contains (FC_LOCATION_RATIO))
		    ratio = fcSettings.getDouble (FC_LOCATION_RATIO);
		else
		    ratio = 1.0;

		if (fcSettings.contains (FC_LOCATION_SOURCE)) {
			if (fcSettings.getSetting (FC_LOCATION_SOURCE).equals ("gps")) {
				location_source = LOC_SRC_GPS;
				location_error = false;
				if (fcSettings.contains (FC_LOCATION_UPDATE_INTERVAL)) {
				    if (!fcSettings.getSetting (FC_LOCATION_UPDATE_INTERVAL).equals ("none") &&
					!fcSettings.getSetting (FC_LOCATION_UPDATE_INTERVAL).equals ("0")) {
					location_update_interval = fcSettings.getDouble (FC_LOCATION_UPDATE_INTERVAL);
					location_error = true;
				    }
				}
				if (fcSettings.contains (FC_LOCATION_ERROR_RANGE)) {
				    if (!fcSettings.getSetting (FC_LOCATION_ERROR_RANGE).equals ("none")) {
					double error_range [] = fcSettings.getCsvDoubles(FC_LOCATION_ERROR_RANGE, 2);
					if (error_range [1] > 0) {
					    location_error_min = error_range [0];
					    location_error_max = error_range [1];
					    if (location_error_min > location_error_max)
						location_error_min = location_error_max;
					    location_error = true;
					}
				    }
				}
			} else if (fcSettings.getSetting (FC_LOCATION_SOURCE).equals ("hotspot")) {
				location_source = LOC_SRC_HOTSPOT;
			} else if (fcSettings.getSetting (FC_LOCATION_SOURCE).equals ("none")) {
				location_source = LOC_SRC_NONE;
				location_error  = false;
			}
		} else {
			location_source = LOC_SRC_GPS;
			location_update_interval = 0;
			location_error = false;
		}
	}

	/**
	 * Copyconstructor.
	 * @param r The router prototype where setting values are copied from
	 */
	protected FloatingContentRouter(FloatingContentRouter r) {
		super(r);
		this.seed = r.seed;
		this.deletion_policy = r.deletion_policy;
		this.replication_policy = r.replication_policy;
		this.deletion_algorithm = r.deletion_algorithm;
		this.replication_algorithm = r.replication_algorithm;
		this.deletion_fixed = r.deletion_fixed;
		this.replication_fixed = r.replication_fixed;
		this.location_error_min = r.location_error_min;
		this.location_error_max = r.location_error_max;
		this.last_known_location = r.last_known_location;
		this.ratio = r.ratio;
		if (ratio == 1.0 || FloatingContentRouter.locRng.nextDouble () < ratio) {
		    this.location_update_interval = r.location_update_interval;
		    this.location_error = r.location_error;
		    this.location_source = r.location_source;
		} else {
		    this.location_update_interval = 0;
		    this.location_error = false;
		    this.location_source = LOC_SRC_NONE;
		}
	}

	@Override 
	    public boolean createNewMessage(Message msg) {
	        /* we record the absolute position of the node when posting the message,
		 * since the perceived position is already in the anchor point.
		 */
	        msg.addProperty (FC_SRCLOC, getHost().getLocation ());
		super.createNewMessage (msg);
		return true;
	}

	/* Some functions should be carried out only once per encounter.
	 * Record the new encounters here for later use in update()
	 */
	@Override
	public void changedConnection (Connection conn) {
		super.changedConnection (conn);

		if (conn.isUp ()) {
			new_conns.add (conn);
		}
	}

	@Override
	public void update() {

	        Coord loc, peer_loc;
		List<Connection> connections, conn_list;
		int n;
		Collection<Message> m_set, m_set2;
		List<Message> m_list, m_ordered_list;
		List<String> d_list, d_list2;
		double distance_curr;
		boolean location_valid = (location_source == LOC_SRC_GPS);

		super.update();

		if (rng == null)
			rng = new Random (getHost().getAddress()*1000+seed);
		if (replRng == null)
			replRng = new Random (getHost().getAddress()*5000+seed);
		if (delRng == null)
			delRng = new Random (getHost().getAddress()*10000+seed);

		if (location_valid) {
		    loc = getLocation ();

		    if (deletion_policy == DEL_IMMEDIATE) {
			// this branch of the code causes immediate message deletion when the anchor zone is left;
			// otherwise, the message will only be deleted upon the next encounter

			if (this.getNrofMessages() > 0 && !isTransferring()) {

				m_set2 = this.getMessageCollection();
				d_list2 = new ArrayList<String> ();

				for (Message m : m_set2) {
					distance_curr = loc.distance ((Coord) m.getProperty (FC_ANCHOR));
					if  (distance_curr > (Double) m.getProperty (FC_A)) {
						d_list2.add (m.getId ());
					}
				}

				for (String id : d_list2)
					this.deleteMessage (id, false);
				d_list2.clear();
			}
		    }
		} else {
		    loc = new Coord (-1, -1);
		} /* location_source == LOC_SRC_GPS */

		if (isTransferring() || !canStartTransfer()) {
			return; // transferring, don't try other connections yet
		}

		connections = getHost().getConnections();
		conn_list = new ArrayList<Connection> ();
		m_list = new ArrayList<Message> ();
		m_ordered_list = new ArrayList<Message> ();
		m_set = this.getMessageCollection();
		d_list = new ArrayList<String> ();

		/* If we do not have an own notion of a location, we infer our location as the mean of the nodes
		 * we are connected to.  This can be done in reality if the other nodes broadcast beacons with their
		 * location information.
		 */
		if (location_source == LOC_SRC_NONE) {
		    double x = 0, y = 0;

		    n = 0;
		    for (Connection c : connections) {
			if (((FloatingContentRouter) c.getOtherNode (getHost ()).getRouter ()).getLocationSource () == LOC_SRC_GPS) {
			    peer_loc = ((FloatingContentRouter) (c.getOtherNode (getHost ()).getRouter ())).getLocation ();
			    x += peer_loc.getX ();
			    y += peer_loc.getY ();
			    n++;
			}
		    }
		    if (n > 0) {
			last_known_location = loc = new Coord (x / (double) n, y / (double) n);
			location_valid = true;
		    }
		}
		if (deletion_policy == DEL_ENCOUNTER && location_valid) {
			/* This branch of the code deletes the message upon first encounter of another node outside the anchor zone
			 * We loop through the new connections to perform the evaluation of the probabilistic deletion check
			 * once per new connection.  
			 */
             for (Connection c : new_conns) {
				for (Message m : m_set) {
					distance_curr = loc.distance ((Coord) m.getProperty (FC_ANCHOR));
					if ((deletion_check (distance_curr, (Double) m.getProperty (FC_R), (Double) m.getProperty (FC_A)) == 1)) {
					    if (!d_list.contains (m.getId ()))
							d_list.add (m.getId ());
					}
				}
			}
		}

		for (String id : d_list)
			this.deleteMessage (id, false);
		d_list.clear();

		// organize messages for replication
		for (Message m : m_set) {
		    m_ordered_list.add (m);
		}
		
		switch (replication_policy) {
		case REPL_RND:
			Collections.shuffle (m_ordered_list, rng);
			break;
		case REPL_SAF:
			Collections.sort (m_ordered_list, new Comparator<Message> () {
				public int compare (Message m1, Message m2) {
					double  a1, a2;
					a1 = (Double) m1.getProperty (FC_A);
					a2 = (Double) m2.getProperty (FC_A);

					if (a1 == a2)
						return 0;
					return a1 < a2 ? -1 : 1;
				}
			});
			break;
		case REPL_SVF:
			Collections.sort (m_ordered_list, new Comparator<Message> () {
				public int compare (Message m1, Message m2) {
					double  v1, v2;
					v1 = (Double) m1.getProperty (FC_A) * (double) m1.getSize ();
					v2 = (Double) m2.getProperty (FC_A) * (double) m2.getSize ();

					if (v1 == v2)
						return 0;
					return v1 < v2 ? -1 : 1;
				}
			});
			break;
		case REPL_SVF2:
			Collections.sort (m_ordered_list, new Comparator<Message> () {
				public int compare (Message m1, Message m2) {
					double  v1, v2;
					v1 = (Double) m1.getProperty (FC_A) * (Double) m1.getProperty (FC_A) * (double) m1.getSize ();
					v2 = (Double) m2.getProperty (FC_A) * (Double) m2.getProperty (FC_A) * (double) m2.getSize ();

					if (v1 == v2)
						return 0;
					return v1 < v2 ? -1 : 1;
				}
			});
			break;
		case REPL_STF:
			Collections.sort (m_ordered_list, new Comparator<Message> () {
				public int compare (Message m1, Message m2) {
					double  t1, t2;
					t1 = (Double) m1.getProperty (FC_A);
					t1 *= (Double) m1.getProperty (FC_TTL_VAL);
					t1 *= m1.getSize ();
					t2 = (Double) m2.getProperty (FC_A);
					t2 *= (Double) m2.getProperty (FC_TTL_VAL);
					t2 *= m2.getSize ();

					if (t1 == t2)
						return 0;
					return t1 < t2 ? -1 : 1;
				}
			});
			break;
		case REPL_STF2:
			Collections.sort (m_ordered_list, new Comparator<Message> () {
				public int compare (Message m1, Message m2) {
					double  t1, t2;

					t1 = (Double) m1.getProperty (FC_A);
					t1 *= t1;
					t1 *= (Double) m1.getProperty (FC_TTL_VAL);
					t1 *= m1.getSize ();
					t2 = (Double) m2.getProperty (FC_A);
					t2 *= t2;
					t2 *= (Double) m2.getProperty (FC_TTL_VAL);
					t2 *= m2.getSize ();

					if (t1 == t2)
						return 0;
					return t1 < t2 ? -1 : 1;
				}
			});
		case REPL_FIFO:
		default:
			/* if none of the above is chosen, we imply "fifo" and use the original message order */
		}

		double a, r, h;
		int    replicate;

		for (Message m : m_ordered_list) {
		    /* find the right connection(s) for each message
		     * Messages are replicated to nodes if the target node is within the anchor zone
		     * If the other node doesn't know where it is, we'll pick the position of the local node.
		     * If the local node doesn't know its position either, we check if the peer knows its
		     * previous location and choose this.  If this isn't known either, we stay passive and don't do anything.
		     *
		     * We prefer our most recent position over the peer's because our location was surely
		     * established in this round and may this be more accurate.
		     */
		        for (Connection conn : connections) {
			        DTNHost               peer = null;
				FloatingContentRouter peer_router = null;

				peer = conn.getOtherNode (getHost());
				peer_router = (FloatingContentRouter) peer.getRouter ();
				if (peer_router.getLocationSource () == LOC_SRC_GPS) {
				    /* the remote node knows where it is -> use its location */
				    peer_loc = peer_router.getLocation ();
				    h = peer_loc.distance ((Coord) m.getProperty (FC_ANCHOR));
				} else if (location_valid) {
				    /* we know where we are (or at least approximately -> our our location as a backup */
				    h = loc.distance ((Coord) m.getProperty (FC_ANCHOR));
				} else if ((peer_loc = peer_router.getLastKnownLocation ()) != null) {
				    /* if we don't know either, let's try the most recent peer location */
				    h = peer_loc.distance ((Coord) m.getProperty (FC_ANCHOR));
				} else if (last_known_location != null) { 
				    /* last resort: our most recently known location if not established in this round */
				    h = last_known_location.distance ((Coord) m.getProperty (FC_ANCHOR));
				} else {
				    /* no idea about location on either side -> don't replicate */
				    continue;
				}

				r = (Double) m.getProperty (FC_R);
				a = (Double) m.getProperty (FC_A);
				// Rule 1: if within core radius -> replicate
				// d <= r
				if (h <= r) {
					conn_list.add (conn);
				}  // Rule 2: r < d <= a: if outside core radius but below a -> use the buffer zone replication rule
				else if (a > r && h <= a) {
					double   x;
					/* check first if this connection came up freshly */
					if (new_conns.contains (conn)) {
						switch (replication_algorithm) {
						case REPL_ALG_LINEAR:
							x = replRng.nextDouble ();
							replicate = x < (-(h - r)/(a - r) + 1.0) ? 1 : 0;
							break;
						case REPL_ALG_COSINE:
							replicate = replRng.nextDouble () < 0.5*Math.cos(Math.PI*(h-r)/(a-r))+0.5 ? 1 : 0;
							break;
						case REPL_ALG_EXP:
							replicate = replRng.nextDouble () < Math.exp(-7*(h-r)/(a-r)) ? 1 : 0;
							break;
						case REPL_ALG_NONE:
							replicate = 0;
							break;
						case REPL_ALG_FIXED:
							replicate = replRng.nextDouble () < replication_fixed ? 1 : 0;
							break;
						default:
							replicate = 0;
							break;
						}
						if (replicate == 1)
							conn_list.add (conn);
					}
				}
			}

			if (!conn_list.isEmpty ()) {
				m_list.add (m);
				this.tryMessagesToConnections (m_list, conn_list);
				m_list.clear();
				conn_list.clear();
			}
		}
		m_ordered_list.clear ();
		new_conns.clear ();
	}

	// This check can only be carried out with the encounter-based deletion;
	// otherwise, content in the buffer zone would die out too quickly as
	// every time tick would lead to a new check.
	protected int deletion_check (double h, double r, double a) {
		int     del = 0;

		if (h > a)
			return 1;
		if (a > r && h > r) {
			switch (deletion_algorithm) {
			case DEL_ALG_LINEAR:
				del = delRng.nextDouble () > (-(h - r)/(a - r) + 1.0) ? 1 : 0;
				break;
			case DEL_ALG_COSINE:
				del = delRng.nextDouble () > 0.5*Math.cos(3.14159*(h-r)/(a-r))+0.5 ? 1 : 0;
				break;
			case DEL_ALG_EXP:
				del = delRng.nextDouble () < Math.exp(-7*(1-(h-r)/(a-r))) ? 1 : 0;
				break;
			case DEL_ALG_NONE:
				del = 0;
				break;
			case DEL_ALG_FIXED:
				del = delRng.nextDouble () < deletion_fixed ? 1 : 0;
				break;
			default:
				del = 0;
				break;
			}
		}
		return del;
	}

	@Override
	protected void transferDone(Connection conn) {
		super.transferDone (conn);
		 // At this point, a node may have moved out of the anchor zone.  We leave the message
		 // nevertheless to the regular update processing to ensure that reporting and bookkeeping
		 // don't get confused.
	}

	@Override
	public FloatingContentRouter replicate() {
		return new FloatingContentRouter(this);
	}

	@Override	
	public Message messageTransferred(String id, DTNHost from) {
		Message m = super.messageTransferred (id, from);
		return m;
	}

        public int getLocationSource () {
	    return this.location_source;
        }

        public Coord getLastKnownLocation () {
	    return this.last_known_location;
	}

        public Coord getLocation () {
	    if (!location_error)
		return getHost().getLocation();
	    else
		return getLocationWithError(location_error_min, location_error_max, location_update_interval, seed);
	}
        
    	//In this method, error is introduced
    	public Coord getLocationWithError(double min, double max, double interval, int seed){
    		Coord location = getHost().getLocation();
    	    if (interval >= 0)
    		gpsUpdateInterval = interval;
    	    if (min >= 0)
    		gpsErrorMin = min;
    	    if (max >= 0)
    		gpsErrorMax = max;
    	    
    	    if (gpsLocation.getX () == -1 || SimClock.getTime () >= gpsLastReading + gpsUpdateInterval) {

    		if (max > 0) {
    		    if(this.rdAngle==null)
    			this.rdAngle = new Random(getHost().getAddress()*60000+seed);
    		    if(this.rdDistance==null)
    			this.rdDistance = new Random(getHost().getAddress()*70000+seed);
    		    double angle = this.rdAngle.nextDouble()*360;
    		    double d=this.rdDistance.nextGaussian()*
    		    		(gpsErrorMax - gpsErrorMin)/6+(gpsErrorMax - gpsErrorMin);
    		
    		    gpsLocation.setLocation (location.getX()+Math.cos(angle)*d, 
    					     location.getY()+Math.sin(angle)*d);
    		} else {
    		    gpsLocation.setLocation (location.getX(), location.getY());
    		}
    		gpsLastReading = SimClock.getTime ();
    	    }
    	    return gpsLocation;
    	}

}
