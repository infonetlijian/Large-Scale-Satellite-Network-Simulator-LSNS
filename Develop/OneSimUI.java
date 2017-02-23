/* 
 * Copyright 2016 University of Science and Technology of China , Infonet
 * 
 */
package Develop;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.sun.j3d.utils.applet.MainFrame;

import ui.DTNSimTextUI;
import core.DTN2Manager;
import core.DTNHost;
import core.Settings;
import core.SimClock;
import core.SimScenario;

import java.awt.*;

public class OneSimUI extends DTNSimTextUI{
	private long lastUpdateRt;									// real time of last ui update
	private long startTime; 									// simulation start time
	private  EventLog eventLog;
	/** namespace of scenario settings ({@value})*/
	public static final String SCENARIO_NS = "Scenario";
	/** end time -setting id ({@value})*/
	public static final String END_TIME_S = "endTime";
	/** update interval -setting id ({@value})*/
	public static final String UP_INT_S = "updateInterval";
	/** How often the UI view is updated (milliseconds) */     
	public static final long UI_UP_INTERVAL = 60000;
	/** List of hosts in this simulation */
	protected List<DTNHost> hosts;
	
	public Main_Window main;
	InfoPanel infoPanel;
	/**
	 * Initializes the simulator model.
	 */
	private void NewWindow() {
		/**初始化图形界面*/
		//this.eventLog = new EventLog(this);
		//this.hosts = this.scen.getHosts();
		this.infoPanel = new InfoPanel(this);
		main = new Main_Window(this.infoPanel);//eventLog,hosts);
		//scen.addMessageListener(eventLog);
		//scen.addConnectionListener(eventLog);
		
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setLocationRelativeTo(null);
		main.setVisible(true);	
	}

	/**
	 * Starts the simulation.
	 */
	public void start() {
		startGUI();
		
		while(true){
			while(main.getPaused() == true){			// 界面等待确定配置参数
				try {
					 synchronized (this){
						wait(10);
					 }
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/**重置初始化配置，清除之前的初始化实体，避免后续初始化无效**/
			/**进行初始化**/
			super.initModel();
			setUI();		
			runSim();
			main.setPaused(false);//一次仿真跑完之后让系统处于暂停状态，不会循环开始下一轮仿真
			main.parameter.setEnabled(true);//重新允许编辑配置界面
		}
	}
	/**
	 * 在UI确定配置参数且完成初始化之后，刷新UI显示，包括事件窗口，节点列表以及3D图形显示
	 */
	private void setUI(){
		main.setNodeList(this.scen.getHosts());//刷新节点列表显示
		resetEvenetLog();
		reset3DWindow();
		main.resetSimCancelled();	//重置SimCancelled的值
		main.parameter.setEnabled(false);
	}
	/**
	 * 刷新UI中的事件窗口
	 */
	private void resetEvenetLog(){
		this.eventLog = new EventLog(this);//添加时间窗口
	    eventLog.setBorder(new TitledBorder("事件窗口"));
	    main.resetEventLog(eventLog);
		scen.addMessageListener(eventLog);
		scen.addConnectionListener(eventLog);
	}
	/**
	 * 刷新UI中的3D图形窗口
	 */
	private void reset3DWindow(){
		//this.hosts = this.scen.getHosts();
		main.set3DWindow();//在初始化之后再调用3D窗口
	    main.items[2].setEnabled(true);
	    main.items[6].setEnabled(true);//仿真开始时，设置3D和2D窗口显示按钮为可用
	}
	/**
	 * 开启GUI界面
	 */
	private void startGUI() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
			    public void run() {
					try {
						NewWindow();
					} catch (AssertionError e) {
						processAssertionError(e);
					}
			    }
			});
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	protected void processAssertionError(AssertionError e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void runSim(){
		Settings s = new Settings(SCENARIO_NS);
			
		while(main.getPaused() == true){			// 界面等待确定配置参数
			try {
				 synchronized (this){
					wait(10);
				 }
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.setParameter();
		double simTime = SimClock.getTime();
		double endTime = scen.getEndTime();
		
		// ----------------------- 用于测试参数 --------------------------------//
		System.out.println("仿真时间"+"  "+endTime);
		System.out.println("更新时间："+"  "+scen.getUpdateInterval());
		// ----------------------- 用于测试参数 --------------------------------//
		
		
		startTime = System.currentTimeMillis();
		lastUpdateRt = startTime;
		
		DTN2Manager.setup(world);
		while (simTime < endTime && !main.getSimCancelled()){			
			if (main.getPaused()) {
				try {
					 synchronized (this){
						wait(10);
					 }
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				try {
					world.update();
					this.updateTime();   					//用于更新仿真时间
				} catch (AssertionError e) {
					e.printStackTrace();
					done();
					return;
				}
				simTime = SimClock.getTime();
			}
			this.update(false);
		}
		
		double duration = (System.currentTimeMillis() - startTime)/1000.0;
		
		simDone = true;
		done();
		this.update(true); // force final UI update
		
		print("Simulation done in " + String.format("%.2f", duration) + "s");
	}
	/**
	 * Updates user interface if the long enough (real)time (update interval)
	 * has passed from the previous update.
	 * @param forced If true, the update is done even if the next update
	 * interval hasn't been reached.
	 */
	private void update(boolean forced) {
		long now = System.currentTimeMillis();
		long diff = now - this.lastUpdateRt;
		double dur = (now - startTime)/1000.0;
		if (forced || (diff > UI_UP_INTERVAL)) {
			// simulated seconds/second 
			double ssps = ((SimClock.getTime() - lastUpdate)*1000) / diff;
			this.lastUpdateRt = System.currentTimeMillis();
			this.lastUpdate = SimClock.getTime();
		}		
	}
	
	private void print(String txt) {
		System.out.println(txt);
	}
	
	/**
	 * 当从界面重新配置参数之后，将参数重新写入到scen中，更新相应参数，不妨碍原有程序读取。
	 */
	private void setParameter(){
		Settings s = new Settings(SCENARIO_NS);
		double interval =  s.getDouble(UP_INT_S);	//	更新时间
		scen.setUpdateInterval(interval);
		System.out.println(interval);
		
		double endTime = s.getDouble(END_TIME_S);	//	结束时间
		scen.setEndTime(endTime);
	}
	
    /**
     * Sets a node's graphical presentation in the center of the playfield view
     * @param host The node to center
     */
    public void setFocus(DTNHost host) {
    	//centerViewAt(host.getLocation());
    	infoPanel.showInfo(host);
    	//showPath(host.getPath()); // show path on the playfield
    }
    /**
     * Returns the info panel of the GUI
     * @return the info panel of the GUI
     */
    public InfoPanel getInfoPanel() {
    	return this.infoPanel;
    }
    /**
     * 更新仿真时间
     */
    private void updateTime() {
    	double simTime = SimClock.getTime();
    	this.lastUpdate = simTime;
    	main.setSimTime(simTime); //update time to control panel
    }
}
