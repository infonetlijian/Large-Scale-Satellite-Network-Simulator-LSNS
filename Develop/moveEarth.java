package Develop;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.applet.*;

//java3D
import javax.vecmath.*;
import javax.media.j3d.*;
//new add
import javax.swing.*;

import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.image.*;
import com.sun.j3d.utils.applet.MainFrame;
import com.sun.j3d.utils.behaviors.mouse.*;
import com.sun.j3d.utils.behaviors.vp.*;

import core.DTNHost;





//3d data
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import satellite_orbit.Printable;
import satellite_orbit.TwoBody;
//3d data

public class moveEarth extends Applet implements Runnable{//implements Runnable
	List<DTNHost> hosts;
	
	//NEW ADD
	boolean flag = true;
	int satellite_numbers;
    double[][][] BL;
	Point3f[][] points;
	Shape3D[][] drawpoints;
	//NEW ADD
	
	RotationInterpolator rotator;
	BranchGroup xuanzhuan;
	BranchGroup root;
	//Transform3D tr;
	Transform3D tr = new Transform3D();
	TransformGroup tg = new TransformGroup(tr);//设定为final
	BoundingSphere bounds;
	
	public double[][][] getBL() {
		return this.BL;
	}
	
	/**
	*Create 3D interface
	*/
	public void init(List<DTNHost> hosts) {
		//NEW ADD
		this.flag = true;
		this.satellite_numbers = hosts.size();
		this.BL = new double[satellite_numbers][200][2];
		this.points = new Point3f[satellite_numbers][200];
		this.drawpoints = new Shape3D[satellite_numbers][200];
		//NEW ADD
		
		GraphicsConfiguration gc =
				SimpleUniverse.getPreferredConfiguration();
		Canvas3D cv = new Canvas3D(gc);
		setLayout(new BorderLayout());
		add(cv,BorderLayout.CENTER);
		root = new BranchGroup();
		/**create axis*/
		//Transform3D tr = new Transform3D();
		tr.setScale(0.5);
		tr.setTranslation(new Vector3d(0,0,0));
		//TransformGroup tg = new TransformGroup(tr);
		
		//NEW ADD
		tg.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		tg.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		tg.setCapability(BranchGroup.ALLOW_CHILDREN_READ);
		tg.setCapability(BranchGroup.ALLOW_DETACH);
		//NEW ADD
		
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		root.addChild(tg);
	/*	Axes axes = new Axes();
		tg.addChild(axes);*/
		/**create axis*/
		
		/**create wenli*/
		Appearance ap = createAppearance();
		tg.addChild(new Sphere(0.6f,
		Primitive.GENERATE_TEXTURE_COORDS,50,ap));
		BoundingSphere bounds = new BoundingSphere();
		/**create wenli*/
		
		/**create light*/
		AmbientLight light = new AmbientLight(true,
		new Color3f(Color.blue));
		light.setInfluencingBounds(bounds);
		root.addChild(light);
		PointLight ptlight = new PointLight(new Color3f(Color.white),
		new Point3f(0f,0f,2f),new Point3f(1f,0.3f,0f));
		ptlight.setInfluencingBounds(bounds);
		root.addChild(ptlight);
		/**create light*/
		
		/**add orbit*/
		this.hosts = new ArrayList<DTNHost>(hosts);
		
		for(int order = 0; order < this.hosts.size(); order++) {
			double[] orbitParameters = this.hosts.get(order).getParameters();
			
			drawLine drawline = new drawLine(orbitParameters[0],orbitParameters[1],
					orbitParameters[2],orbitParameters[3],orbitParameters[4],orbitParameters[5],order);
			
		    //NEW CHANGE	
		    //this.BL = drawLine.get2DPoints();
		    for(int m=0;m<200;m++) {
				double[][] bl = drawline.convert3DTo2D(drawline.XYZ[m][0]*1000,
				                     drawline.XYZ[m][1]*1000,drawline.XYZ[m][2]*1000);
				(BL[order])[m][0] = bl[0][0];
				(BL[order])[m][1] = bl[0][1];
			}
			//NEW CHANGE
			
			for(int k=0;k<200;k++) {
				points[order][k] = drawline.getPoint(k);
			}
		    tg.addChild(drawline);
		}
		for(int k=0;k<satellite_numbers;k++) {
			for(int m=0;m<200;m++) {
				drawpoints[k][m] = new drawPoint(points[k][m]);
			}
		}
		/**add orbit*/
		
		/**添加旋转效果**/
		Alpha alpha = new Alpha(-1,6000);//控制旋转
		rotator = 
		new RotationInterpolator(alpha,tg);
		rotator.setSchedulingBounds(bounds);
		xuanzhuan = new BranchGroup();
		xuanzhuan.setCapability(BranchGroup.ALLOW_DETACH);
		xuanzhuan.addChild(rotator);
		tg.addChild(xuanzhuan);
		/**添加旋转效果**/
		
		/**set background*/
		Color3f bgColor = new Color3f(0.0f,0.0f,0.0f);
		Background background = new Background(bgColor);
		background.setApplicationBounds(bounds);
		root.addChild(background);
		root.compile();
		SimpleUniverse su = new SimpleUniverse(cv);
		su.getViewingPlatform().setNominalViewingTransform();
		/**set background*/
		
		/**create and use OrbitBehavior*/
		OrbitBehavior orbit = new OrbitBehavior(cv);
		orbit.setSchedulingBounds(new BoundingSphere());
		su.getViewingPlatform().setViewPlatformBehavior(orbit);
		su.addBranchGraph(root);
		/**create and use OrbitBehavior*/
		
		
	}
	/**create appearence*/
	Appearance createAppearance() {
		Appearance appear = new Appearance();
		URL filename =
				getClass().getClassLoader().getResource("images/earth.jpg");
		TextureLoader loader = new TextureLoader(filename,this);
		Texture texture = loader.getTexture();
		appear.setTexture(texture);
		return appear;
	}
	/**create appearence*/
	
	/**create background*/
	Background createBackground() {
		Background background = new Background();
		BranchGroup bg = new BranchGroup();
		Sphere sphere = new Sphere(1.0f,Sphere.GENERATE_NORMALS |
		Sphere.GENERATE_NORMALS_INWARD |
		Sphere.GENERATE_TEXTURE_COORDS,60);
		Appearance ap = sphere.getAppearance();
		bg.addChild(sphere);
		background.setGeometry(bg);
		
		URL filename =
				getClass().getClassLoader().getResource("images/background.jpg");
		TextureLoader loader = new TextureLoader(filename,this);
		Texture texture = loader.getTexture();
		ap.setTexture(texture);
		return background;	
	}
	/**create background*/
	
	//NEW ADD
	/**NEW ADD,add satellite*/
	public void run() {
		
		tg.removeChild(xuanzhuan);
		//xuanzhuan.removeChild(rotator);
		
		BranchGroup[] rts = new BranchGroup[satellite_numbers];
		for(int m=0;m<satellite_numbers;m++) {
			rts[m] = new BranchGroup();
			rts[m].setCapability(BranchGroup.ALLOW_DETACH);
		}
		int k = 0;
		for(int m=0;m<satellite_numbers;m++) {
			rts[m].addChild(drawpoints[m][k]);	
			tg.addChild(rts[m]);
		}
		while(true) {
			if(flag == true) {
				//xuanzhuan.addChild(rotator);
				tg.addChild(xuanzhuan);
				
				try {
					Thread.sleep(45);
				}
			    catch(InterruptedException ex) {
				    //do nothing
			    }
				for(int m=0;m<satellite_numbers;m++) {
					tg.removeChild(rts[m]);
				    rts[m].removeChild(drawpoints[m][k]);
			    }
				
				k++;
			    if(k>=200) {
					k = 0;
			    }
				
				try {
					Thread.sleep(1);
			    }
			    catch(InterruptedException ex) {
				    //do nothing
			    }
			    for(int m=0;m<satellite_numbers;m++) {
				    rts[m].addChild(drawpoints[m][k]);
				    tg.addChild(rts[m]);
			    }
			    
			    tg.removeChild(xuanzhuan);
				//xuanzhuan.removeChild(rotator);
			    
			}	
			else{
				while(flag == false){			// 界面等待确定配置参数
					tg.removeChild(xuanzhuan);
					//xuanzhuan.removeChild(rotator);
					try {
						 synchronized (this){
							wait(10);
						 }
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
	}
	/**NEW ADD,add satellite*/
	//NEW ADD
	
	//NEW ADD
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	//NEW ADD
	
}

/**
 * Copyright(C),2016-2020,USTC.
 * ClassName:drawPoint
 * draw satellite's orbit
 * 
 * @author XiJianXu
 * @Date 2016-12-05
 * @version 1.0
 */
class drawPoint extends Shape3D {
	/**
	@param point get satellte's initial coordinate
	*/
	public drawPoint(Point3f point) {
		Point3f[] vertexes0 = new Point3f[1];
        Color3f[] pointcolors0 = new Color3f[1];
		vertexes0[0] = point;
        pointcolors0[0] = new Color3f(0.1f,0.2f,0.4f);
		int vCount = 1;
        PointArray points = new PointArray(vCount,PointArray.COORDINATES|IndexedPointArray.COLOR_3);
        points.setCoordinates(0,vertexes0);
        points.setColors(0,pointcolors0);
        PointAttributes pointsattributes = new PointAttributes();
        pointsattributes.setPointSize(15.0f);
        pointsattributes.setPointAntialiasingEnable(true);
        Appearance app = new Appearance();
        app.setPointAttributes(pointsattributes);
        this.setGeometry(points);
        this.setAppearance(app);
	}
}

class drawLine extends Shape3D implements Printable{
	
	/**鍗槦杞ㄩ亾鍙傛暟*/
	public double a; 
	double e;
	double i;
	double raan;
	double w;
	double ta;
	/**鍗槦杞ㄩ亾鍙傛暟*/
	double max;
	int step;
	
	/**鍗槦杞ㄩ亾鍧愭爣*/
	int steps = 200;
	double[][] XYZ = new double[steps][3];
    double[][] points = new double[1][3];
	//NEW ADD
	//static double[][][] BL;
    //NEW ADD
	Point3f[] vertexes = new Point3f[200];
	/**鍗槦杞ㄩ亾鍧愭爣*/
	
	/**
	* @param a semi-major axis			(半长轴)
	* @param e eccentricity				(偏心率)
	* @param i inclination in degrees	(轨道倾角)
	* @param raan right ascension of ascending node in degrees (升交点赤经)
	* @param w argument of the perigee	(近地点幅角)
	* @param ta true anomaly in degrees	(真近点角)
	*/
	public drawLine(double a, double e, double i, double raan, double w, double ta, int order) { 
		this.a = a;
		this.e = e;
		this.i = i;
		this.raan = raan;
		this.w = w;
		this.ta = ta;
		this.max = 0;
		this.step = 0;
		TwoBody sat = new TwoBody(a, e, i, raan, w, ta);
		double period = sat.getPeriod();
		double tf = period;
		double t0 = 0.0;
		sat.setSteps(steps);
		sat.propagate(t0, tf, this, true);
		points = new double[1][3];
		points[0][0] = sat.rv.x[0];
		points[0][1] = sat.rv.x[1];
		points[0][2] = sat.rv.x[2];

		
		for (int m = 0; m < 200; m++) {
			vertexes[m] = new Point3f();
			vertexes[m].x = (float) XYZ[m][0] / 9000;
			vertexes[m].y = (float) XYZ[m][1] / 9000;
			vertexes[m].z = (float) XYZ[m][2] / 9000;
		}

		// NEW REMOVE
		/*
		 * for(int m=0;m<200;m++) { double[][] bl =
		 * convert3DTo2D(XYZ[m][0]*1000, XYZ[m][1]*1000,XYZ[m][2]*1000);
		 * (BL[order])[m][0] = bl[0][0]; (BL[order])[m][1] = bl[0][1]; }
		 */
		// NEW REMOVE

		Random rm = new Random();
		Color3f[] colors = new Color3f[200];
		for (int m = 0; m < 200; m++)
			colors[m] = new Color3f((float) rm.nextDouble(),
					(float) rm.nextDouble(), (float) rm.nextDouble());
		LineArray lines = new LineArray(200, LineArray.COORDINATES
				| LineArray.COLOR_3);
		lines.setCoordinates(0, vertexes);
		lines.setColors(0, colors);

		LineAttributes lineattributes = new LineAttributes();
		lineattributes.setLineWidth(1.0f);
		lineattributes.setLineAntialiasingEnable(true);
		lineattributes.setLinePattern(0);

		Appearance app = new Appearance();
		app.setLineAttributes(lineattributes);
		this.setGeometry(lines);
		this.setAppearance(app);
	}
	/**
	*@param k the order of satellite
	*/
	public Point3f getPoint(int k) {
		return this.vertexes[k];
	}
	
	public double[][] get3DPoints() {
		return this.XYZ;
	}
	
	//NEW REMOVE
	/*static public double[][][] get2DPoints() {
		return BL;
	}*/
	//NEW REMOVE
	
	/**implement printable function*/
	public void print(double t, double[] y) {
		if (step < XYZ.length) {
			XYZ[step][0] = y[0];
			XYZ[step][1] = y[1];
			XYZ[step][2] = y[2];
			if (y[0] > max)
				max = y[0];
			if (y[1] > max)
				max = y[1];
			if (y[2] > max)
				max = y[2];
			step++;
		}
	}
	/**shixain Printable*/
	
	public void print1(double t, double[] y) {
		
	}
	
	public void print2(double t, double[] y) {
		
	}
	
	//new add
	/**convert 3D to 2D*/
	public double[][] convert3DTo2D(double X, double Y, double Z) {
		double[][] bl = new double[1][2]; 
		if(X>0) {
			bl[0][0] = Math.atan(Y/X)*180/3.1415926;
		}
		else if(X<0&&Y>0) {
			bl[0][0] = (3.1415926+Math.atan(Y/X))*180/3.1415926;
		}
		else if (X<0&&Y<0) {
			bl[0][0] = -(3.1415926-Math.atan(Y/X))*180/3.1415926;
		}
		
		bl[0][1] = Math.atan(Z/Math.sqrt(X*X+Y*Y))*180/3.1415926/**(201.5)*/;
		
		return bl;
	}
	/**convert 3D to 2D*/
	//new add
}