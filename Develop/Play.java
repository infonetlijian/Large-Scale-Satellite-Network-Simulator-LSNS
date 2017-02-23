package Develop;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import java.awt.font.*;
import java.awt.geom.*;
import java.io.*;
import java.net.URL;
import java.net.URLDecoder;

import javax.imageio.*;

//NEW ADD
import java.util.List;
import java.util.ArrayList;
//NEW ADD

public class Play extends JApplet {
	double[][][] BL;
	PaintPanel panel;
	int number;

	public Play(double[][][] BL,int number) {
		this.number = number;
		this.BL = new double[number][200][2];
		this.BL = BL;
	}
	public void init() {
		panel = new PaintPanel(BL,number);
		getContentPane().add(panel);
	}
	
	public PaintPanel getJP() {
		return this.panel;
	}
	public void setFlag(boolean flag) {
		panel.setFlag(flag);
	}
	public void zoom(int width,int height) {
		panel.zoom(width, height);
	}
}

class PaintPanel extends JPanel implements Runnable{
//	private ImageIcon image;
    private BufferedImage image;
	double[][][]BL;
	int number;
	int step = 0;
	boolean flag;
	private static double WIDTH;
	private static double HEIGHT;
	private static int INCREMENT;
	
	public PaintPanel(double[][][] BL,int number) {
		this.number = number;
		this.BL = new double[number][200][2];
		this.BL = BL;
		this.flag = true;
		setPreferredSize(new Dimension(500,500));
		setBackground(Color.white);
	//	image = new ImageIcon(getClass().getResource("images/earth4.jpg"));
		URL url = getClass().getClassLoader().getResource("images/earth.jpg");
		try {
			image = ImageIO.read(url);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	/*	WIDTH = image.getIconWidth();
		HEIGHT = image.getIconHeight();*/
		WIDTH = image.getWidth();
		HEIGHT = image.getHeight();
		INCREMENT = 20;
	}
	
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.drawImage(image,0,0,this);
	//	image.paintIcon(this,g2d,10,10);
		
	/*	g2d.drawLine(INCREMENT, (int)HEIGHT/2, (int)WIDTH-INCREMENT, (int)HEIGHT/2);
		g2d.drawLine((int)WIDTH-INCREMENT, (int)HEIGHT/2, (int)WIDTH-10, (int)HEIGHT/2-5);
		g2d.drawLine((int)WIDTH-INCREMENT, (int)HEIGHT/2, (int)WIDTH-10, (int)HEIGHT/2+5);
		
		g2d.drawLine((int)WIDTH/2, 40, (int)WIDTH/2, (int)HEIGHT-50);
		g2d.drawLine((int)WIDTH/2, 40, (int)WIDTH/2-10, 50);
		g2d.drawLine((int)WIDTH/2, 40, (int)WIDTH/2+10, 50);*/
		
		g2d.setColor(Color.WHITE);
		
		float[] dash = {5,15};
		BasicStroke bs = new BasicStroke(1,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10.0F,dash,0.0f);
		g2d.setStroke(bs);
		/**经线**/
		for(int i=5;i>=0;i--) {
			g2d.setColor(Color.GRAY);
			g2d.drawLine(((int)WIDTH/2)-((int)WIDTH/2/6)*i-10,0,((int)WIDTH/2)-((int)WIDTH/2/6)*i-10,(int)HEIGHT);
			g2d.drawLine(((int)WIDTH/2)+((int)WIDTH/2/6)*i-10,0,((int)WIDTH/2)+((int)WIDTH/2/6)*i-10,(int)HEIGHT);
			
			g2d.setColor(Color.WHITE);
			String s = String.valueOf(-30*i);
			g2d.drawString(s,((int)WIDTH/2)-((int)WIDTH/2/6)*i-10,10);
			String s1 = String.valueOf(30*i);
			g2d.drawString(s1,((int)WIDTH/2)+((int)WIDTH/2/6)*i-10,10);
		}
		/**纬线**/
		for(int i=3;i>=0;i--) {
			g2d.setColor(Color.GRAY);
			g2d.drawLine(0,((int)HEIGHT/2)-((int)HEIGHT/2/3)*i-10,(int)HEIGHT,((int)HEIGHT/2)-((int)HEIGHT/2/3)*i-10);
			g2d.drawLine(0,((int)HEIGHT/2)+((int)HEIGHT/2/3)*i-10,(int)HEIGHT,((int)HEIGHT/2)+((int)HEIGHT/2/3)*i-10);
			
			g2d.setColor(Color.WHITE);
			String s2 = String.valueOf(-30*i);
			g2d.drawString(s2,30,((int)HEIGHT/2)-((int)HEIGHT/2/3)*i-10);
			String s3 = String.valueOf(30*i);
			g2d.drawString(s3,30,((int)HEIGHT/2)+((int)HEIGHT/2/3)*i-10);
		}
		
		BasicStroke bs1 = new BasicStroke(2);
		g2d.setStroke(bs1);
		
		g2d.translate((int) WIDTH / 2, (int) HEIGHT / 2);

		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0,0);
		for(int order=0;order<number;order++) {
			drawOrbit(order,gp, g2d,BL[order]);
		}
	}
	
	public double Lagrange(double x[],double y[],double value) {
		double sum = 0;
		double L;
		for(int i=0;i<x.length;i++) {
			L = 1;
			for(int j = 0;j<x.length;j++) {
				if(j!=i) {
					L = L*(value-x[j])/(x[i]-x[j]);
				}
			}
			sum = sum+L*y[i];
		}
		return sum;
	}
	
	private void drawOrbit(int order,GeneralPath gp, Graphics2D g2d,double[][] BL) {
		List<Integer> insert_points = new ArrayList<Integer>();
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double[] X = new double[50];
		double[] Y = new double[50];
		for(int i=0;i<50;++i) {
			X[i] = BL[i][0];
			Y[i] = BL[i][1];
			}
		for(int i=0;i<X.length;i++) {
			if(X[i]>max) max = X[i];
			if(X[i]<min) min = X[i];
		}
			g2d.setColor(new Color(0,255,127));
			insert_points.add((int)(((double)WIDTH/375.0)*min));
			insert_points.add((int)(((double)HEIGHT/358.0)*Lagrange(X,Y,min)));
			/**差值法**/
			for(double x = min; x<=max; x = x+0.5) {
				if(x>min&&(Math.abs(/*(int)*/(((double)HEIGHT/358.0)*Lagrange(X,Y,x))-/*(int)*/(((double)HEIGHT/358.0)*Lagrange(X,Y,x-0.1)))<0.3))
				{
					insert_points.add((int)(((double)WIDTH/375.0)*x));
			        insert_points.add((int)(((double)HEIGHT/358.0)*Lagrange(X,Y,x)));
				}
			/*	g2d.drawLine((int)(((double)WIDTH/375.0)*x),(int)(((double)HEIGHT/358.0)*Lagrange(X,Y,x)),
				                  (int)(((double)WIDTH/375.0)*x),(int)(((double)HEIGHT/358.0)*Lagrange(X,Y,x)));*/
			}
			for(int k=0;k<insert_points.size()-1;k+=2) {
				g2d.drawLine(insert_points.get(k),insert_points.get(k+1),insert_points.get(k),insert_points.get(k+1));
			}
			
			g2d.setColor(new Color(255,97,0));
			g2d.fillOval(((int)(((double)WIDTH/375.0)*BL[step][0]))-5,((int)(((double)HEIGHT/358.0)*BL[step][1]))-5,10,10);
	}
	
	public void run() {
		while(true) {
			if(flag == true) {
				try {
					Thread.sleep(600);
			    }
			    catch(InterruptedException e) {
				    //do nothing
			    }
			    step+=1;
				this.repaint();
				if(step>=50/*this.number*/) {
					step = 0;
				}
			}
			else{
				while(flag == false){			// 界面等待确定配置参数
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
	
	public void setFlag(boolean flag) {
		this.flag = flag;
	}
	
	public void zoom(int width,int height) {
	//	this.image.setImage(image.getImage().getScaledInstance(width,height,Image.SCALE_DEFAULT));
	    try {
	    	String str = getClass().getClassLoader().getResource("images/earth.jpg").getPath();
	    	URLDecoder decoder = new URLDecoder();
			String path = decoder.decode(str,"utf-8");//路径的编码格式转换，以便支持路径中含有空格或者中文
			
			BufferedImage src = ImageIO.read(new File(path));
			Image images = src.getScaledInstance(width,height,Image.SCALE_DEFAULT);
			image = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
			Graphics g = image.getGraphics();
			g.drawImage(images,0,0,null);
			//g.dispose();
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
		this.WIDTH = width;
		this.HEIGHT = height;
	}
}