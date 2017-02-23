/* 
 * Copyright 2016 University of Science and Technology of China , Infonet
 * 
 */
package Develop;



import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import chartAnalysisWindow.src.chartWindow.AddChartFrame;
import chartAnalysisWindow.src.chartWindow.AnalysisWindow;
import chartAnalysisWindow.src.chartWindow.Loadtxt;
import core.DTNHost;
import core.SimClock;

import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main_Window extends JFrame implements ActionListener, ChangeListener{
	private static final String PATH_GRAPHICS = "buttonGraphics/";	
	private static final String ICON_PLAY = "Play16.gif";
	private static final String ICON_PAUSE = "Pause16.gif";
	private static final String ICON_FastForward = "FastForward16.gif";
	private static final String ICON_REPORT = "Report.gif";
	private static final String ICON_Stop = "Stop.gif";
	private static final String ICON_Parameter = "Parameter.gif";	
	private static final String ICON_Bigger = "Bigger.gif";
	private static final String ICON_Smaller = "Smaller.gif";
	private static final String TEXT_SEPS = "simulated seconds per second";
	/** Default width for the GUI window */
	public static final int WIN_DEFAULT_WIDTH = 1280;
	/** Default height for the GUI window */
	public static final int WIN_DEFAULT_HEIGHT = 720;

	public JPanel ButtonMenus;
	public JButton playButton;
	public JButton end;
	public JButton report;
	public JButton FastForward;
	public JButton parameter;
	public JButton Smaller;
	public JButton Bigger;
	private static JSplitPane JSP0;
	private static JSplitPane JSP1;
	private static JSplitPane JSP2;
	private static JSplitPane JSP3;
	protected static boolean simPaused = true;
	protected static boolean simCancelled = false;
    private JFileChooser chooser;
	private JTabbedPane tabs = new JTabbedPane();
	private JDesktopPane desktopPane;
	
	public ActionListener e;

	private JPanel nodeStatus;//节点状态栏
	private JInternalFrame internal2DFrame;
	private JInternalFrame internal3DFrame;
	private JPanel fileMenus;
	private JPanel NodeList;
    public final JMenuItem[] items = {
		  	new JMenuItem("读取配置文件"), new JMenuItem("参数配置"), new JMenuItem("2D轨道界面"), new JMenuItem("联系我们"),
			new JMenuItem("退出"), new JMenuItem("图表分析"),new JMenuItem("3D轨道界面"), new JMenuItem("关于"),
    };
    private List<DTNHost> hosts;
    private List<JButton> nodeButton;
    private InfoPanel infoPanel;
    private JPanel desktop;

    /**3D、2D轨道界面**/
    private moveEarth Orbit_3D;
    private Play Orbit_2D;
    
	/** simtime of last UI update */
    private JLabel sepsField;	// simulated events per second field
	private long lastUpdate;
	private static final int EPS_AVG_TIME = 2000;
	private double lastSimTime;
    
	public Main_Window(InfoPanel infoPanel){//EventLog elp, List<DTNHost> hosts) {
		super("卫星仿真系统");
		
		this.infoPanel = infoPanel;
		this.sepsField = new JLabel("0.00");
		this.sepsField.setToolTipText(TEXT_SEPS);
		JLabel time = new JLabel("仿真时间:");
		JLabel s = new JLabel("s");
		
		final String liquid =  "javax.swing.plaf.nimbus.NimbusLookAndFeel";
	  	try {
			UIManager.setLookAndFeel(liquid);
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			//	设置皮肤
	  	this.getContentPane().setBackground(Color.lightGray);			// 设置背景颜色
	  	
	  	
		setSize(WIN_DEFAULT_WIDTH,WIN_DEFAULT_HEIGHT);
	    desktop = new JPanel();
	    getContentPane().add(desktop);
	    
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
	    final JMenu[] menus = {
	    		new JMenu("文件   "),new JMenu("工具   "),
				new JMenu("图形窗口   "),new JMenu("帮助   "),
	    };

	    items[2].setEnabled(false);
	    items[6].setEnabled(false);//仿真还没开始时，先设置3D和2D窗口显示按钮为不可用
	    
	    for (int i=0;i<items.length; i++){
	    	this.items[i].addActionListener(new MenuActionListener());//添加菜单栏动作监听器
			menus[i%4].add(items[i]);
	    };
	    JMenuBar mb = new JMenuBar();
	    for (JMenu jm:menus){
	    	mb.add(jm);
	    };
	    this.fileMenus = new JPanel();
	    this.fileMenus.setLayout(new GridLayout(2,1));
	    this.fileMenus.add(mb);
	    
	    //设置用来放置一排按钮,这里和第一排肯定要用两个面板
	    ButtonMenus = new JPanel();
	    ButtonMenus.setLayout(new BoxLayout(ButtonMenus, BoxLayout.X_AXIS));
	    playButton = addButton(simPaused ? ICON_PLAY : ICON_PAUSE);
	    playButton.addMouseMotionListener(new MouseAdapter(){  
	       public void mouseMoved(MouseEvent e) {  
	    	   if(simPaused == true){
		    	   playButton.setToolTipText("开始仿真");
	           }  	    		  
	    	   else{
				   playButton.setToolTipText("暂停仿真");
	    	   }
	    	   }
	       }
	    );  
	    
	    end = new JButton();
	    end.addActionListener(this);
	    end.setIcon(createImageIcon(ICON_Stop));
	    end.addMouseMotionListener(new MouseAdapter(){  
		       public void mouseMoved(MouseEvent e) {  
		    	   end.setToolTipText("结束仿真");
		           }  
		});
	    //end.setContentAreaFilled(false);
	    
	    report = new JButton();
	    report.setIcon(createImageIcon(ICON_REPORT));
	    //report.setContentAreaFilled(false);
	    //report.addActionListener(this);
	    report.addActionListener(new OpenActionListener());
	    report.addMouseMotionListener(new MouseAdapter(){  
		       public void mouseMoved(MouseEvent e) {  
		    	   report.setToolTipText("生成报告");
		           }  
		});
	    
	    
//	    FastForward = new JButton();
//	    FastForward.setIcon(createImageIcon(ICON_FastForward));
//	    FastForward.addActionListener(this);
//	    FastForward.addMouseMotionListener(new MouseAdapter(){  
//		       public void mouseMoved(MouseEvent e) {  
//		    	   FastForward.setToolTipText("快进");
//		           }  
//		});
//	    
//	    Smaller = new JButton();
//	    Smaller.setIcon(createImageIcon(ICON_Smaller));
//	    Smaller.addActionListener(this);
//	    Smaller.addMouseMotionListener(new MouseAdapter(){  
//		       public void mouseMoved(MouseEvent e) {  
//		    	   Smaller.setToolTipText("缩小");
//		           }  
//		});
//	    
//	    Bigger = new JButton();
//	    Bigger.setIcon(createImageIcon(ICON_Bigger));
//	    Bigger.addActionListener(this);
//	    Bigger.addMouseMotionListener(new MouseAdapter(){  
//		       public void mouseMoved(MouseEvent e) {  
//		    	   Bigger.setToolTipText("放大");
//		           }  
//		});
	    
	    parameter = new JButton();
	    parameter.setIcon(createImageIcon(ICON_Parameter));
	    //parameter.setContentAreaFilled(false);
        parameter.addActionListener(new ActionListener() {	//按钮出来之后要弹出参数配置界面
            public void actionPerformed(ActionEvent e) {
                new RouterInfo();
            }
        });
        parameter.addMouseMotionListener(new MouseAdapter(){  
		       public void mouseMoved(MouseEvent e) {  
		    	   parameter.setToolTipText("参数设置");
		           }  
		});
        
	    ButtonMenus.add(end);
//	    ButtonMenus.add(FastForward);
//	    ButtonMenus.add(Smaller);
//	    ButtonMenus.add(Bigger);
	    ButtonMenus.add(parameter);
	    ButtonMenus.add(report);
	    ButtonMenus.add(Box.createHorizontalStrut(18));
	    
	    JSeparator sep = new JSeparator(SwingConstants.VERTICAL);	//添加分割线
	    sep.setPreferredSize(new Dimension(20,20));
	    sep.setMaximumSize(new Dimension(20,20));
	    sep.setMinimumSize(new Dimension(20,20));
	    ButtonMenus.add(sep);
	    
	    ButtonMenus.add(time);										//添加时间显示
	    ButtonMenus.add(Box.createHorizontalStrut(3));
	    ButtonMenus.add(sepsField);
	    ButtonMenus.add(Box.createHorizontalStrut(3));
	    ButtonMenus.add(s);

	    fileMenus.add(ButtonMenus);
	  
	    //---------------------------设置节点列表----------------------------//	  	
	    this.NodeList = new JPanel();
	    this.NodeList.setBorder(new TitledBorder("节点列表"));

	  

		desktopPane = new JDesktopPane();
		//desktopPane.setBackground(Color.LIGHT_GRAY);
		//System.out.println(desktopPane.getBackground());
	    
	    //---------------------------设置事件窗口----------------------------//
	    JPanel Event = new JPanel();
        Event.setLayout(new BoxLayout(Event,BoxLayout.Y_AXIS));						//	沿着Y轴进行布局
		//Event.setBorder();
	    Event.setBorder(new TitledBorder("事件窗口"));
	    
	    
	    //	设置splitPane1的分隔线位置，0.1是相对于splitPane1的大小而定。
	    JSP1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,false,desktopPane, Event);
	    JSP1.setResizeWeight(0.7);													//	设置splitPane1的分隔线位置，0.1是相对于splitPane1的大小而定。
	    
	    JScrollPane Jscrollp = new JScrollPane(NodeList);		
	    Jscrollp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);	//	不用水平滚动轴
	    JSP2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,false,JSP1,Jscrollp);
	  	JSP2.setResizeWeight(0.97);
	  	JSP3 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,false,fileMenus,JSP2);	
	  	JSP3.setResizeWeight(0.01);

	  	add(JSP3);
	}
	/**
	 * 后台完成初始化以后，调用此函数更新事件窗口的UI，用于显示后台的实时事件
	 * @param eventLog
	 */
	public void resetEventLog(EventLog eventLog){
		this.infoPanel.setBackground(JSP1.getBackground());	
	    this.infoPanel.setBorder(new TitledBorder("信息面板"));
	    JSP0 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,false,new JScrollPane(eventLog),new JScrollPane(this.infoPanel));
	    JSP0.setResizeWeight(0.6);
		this.JSP1.setBottomComponent(JSP0);
//		this.JSP1.setBottomComponent(new JScrollPane(eventLog));
	}

	/**
	 * 后台完成初始化以后，调用此函数更新节点列表的UI，用于显示后台的节点
	 * @param hosts
	 */
	public void setNodeList(List<DTNHost> hosts){
		DTNHost.reset();//清空DTNHost中用于命名的全局变量的值
		
		this.nodeButton = new ArrayList<JButton>();
		this.hosts = hosts;//同步后台生成的节点列表
	    this.NodeList = new JPanel();
	    this.NodeList.setLayout(new GridLayout(hosts.size(), 1));
	    this.NodeList.setBorder(new TitledBorder("节点列表"));
	    for (int i = 0; i < hosts.size(); i++){
	    	JButton nodeButton = new JButton(hosts.get(i).toString());
	    	this.nodeButton.add(nodeButton);//按钮与后台的节点列表顺序严格对应
	    	nodeButton.addActionListener(this);
	    	this.NodeList.add(nodeButton);
	    }
	    JScrollPane Jscrollp = new JScrollPane(NodeList);		
	    Jscrollp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);	//	不用水平滚动轴
	    JSP2.setRightComponent(Jscrollp);
	}
	
	/**
	 * 用于显示在UI中选中的message或者host的信息
	 * @param host
	 */
	public void newInfoPanel(DTNHost host){
		this.infoPanel.setBackground(JSP1.getBackground());	
		this.infoPanel.showInfo(host);
	    this.infoPanel.setBorder(new TitledBorder("节点信息"));
//		desktopPane.add(this.infoPanel);
	    JSP0.setRightComponent(new JScrollPane(this.infoPanel));		// 在EventLog的右边显示
	}
	/**
	 * 在后台初始化完成后调用，在UI中生成3D和2D的卫星轨道界面
	 * @param hosts
	 */
	public void set3DWindow(){
		desktopPane.removeAll();
		desktopPane.setBackground(Color.LIGHT_GRAY);

	    //---------------------------设置三维界面----------------------------//	  	
		internal3DFrame = new JInternalFrame("三维场景", true, true, true, true);
		internal3DFrame.setLocation(0, 0);
		internal3DFrame.setSize(500, 300);
		internal3DFrame.setVisible(true);
		
		Orbit_3D = new moveEarth();
		Orbit_3D.init(hosts);
		//NEW ADD
		new Thread(Orbit_3D).start();//使卫星节点运动起来
		//NEW ADD
	    internal3DFrame.getContentPane().add(Orbit_3D);
	    desktopPane.add("三维场景",internal3DFrame);
	    
	    //---------------------------设置二维界面----------------------------//	  	
	    
	    internal2DFrame = new JInternalFrame("二维场景", true, true, true, true);
		internal2DFrame.setLocation(500, 0);
		internal2DFrame.setSize(500, 300);
		internal2DFrame.setVisible(true);

		Orbit_2D = new Play(Orbit_3D.BL,hosts.size()); //加了个参数hosts.size()；
		Orbit_2D.init();
		/**待检查！！！**/
		Orbit_2D.zoom(internal2DFrame.getWidth(), internal2DFrame.getHeight());
		/**待检查！！！**/
		new Thread(Orbit_2D.getJP()).start();  //新增，使二维界面中节点运动
	    internal2DFrame.getContentPane().add(Orbit_2D);
	    desktopPane.add("二维场景",internal2DFrame);
	    
	    internal2DFrame.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
		        Orbit_2D.zoom(internal2DFrame.getWidth(), internal2DFrame.getHeight());
			}
		});
	    
	}
	/**
	 * 刷新卫星轨道3D图形界面，主要是为了在窗口被用户关闭后，可以重新显示
	 */
	public void refresh3DWindow(){
		desktopPane.remove(internal3DFrame);
		internal3DFrame.setLocation(0, 0);
		internal3DFrame.setSize(500, 300);
		internal3DFrame.setVisible(true);
		desktopPane.add("三维场景",internal3DFrame);
	}
	/**
	 * 刷新卫星轨道2D图形界面，主要是为了在窗口被用户关闭后，可以重新显示
	 */
	public void refresh2DWindow(){
		desktopPane.remove(internal2DFrame);
		internal2DFrame.setLocation(500, 0);
		internal2DFrame.setSize(500, 300);
		internal2DFrame.setVisible(true);
		desktopPane.add("二维场景",internal2DFrame);
	}
	/**
	 * 界面动作实现功能
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.playButton) {
			setPaused(simPaused);
		}
		else if (e.getSource() == this.end){		
			setPaused(false); 
			this.simPaused = true;
			this.simCancelled = true;
			//System.exit(0);
			this.setSimTime(0);			//重置仿真时间
		}
		else{
			for (int i = 0; i < hosts.size(); i++){
				if (e.getSource() == this.nodeButton.get(i)){
					newInfoPanel(this.hosts.get(i));
					break;
				}
			}
		}
	}
	
	/**
	 * 菜单栏动作监听器
	 */
	 class MenuActionListener implements ActionListener{
		 public void  actionPerformed(ActionEvent e){
			 switch(((JMenuItem)e.getSource()).getText()){
				 case "读取配置文件":{
			          	JFileChooser fileChooser = new JFileChooser("");
			            fileChooser.setDialogTitle("选择分析文件");
			            FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
			            fileChooser.setFileFilter(filter);
			            JLabel label = new JLabel();
			            int n = fileChooser.showOpenDialog(fileChooser);
			            if (n == fileChooser.APPROVE_OPTION){
			                String input =fileChooser.getSelectedFile().getPath();
			                new AddChartFrame(new Loadtxt(input));
			            }
			            break;
				 }
				 case "退出":{
					 System.exit(0);//退出程序
				 }
				 case "关于":{
					 JOptionPane.showMessageDialog(null, "The copy right is resevered by USTC, Infonet Lab \n"
					 		+ "The code is written based on THE ONE ", "关于", JOptionPane.YES_OPTION);
					 break;
				 }
				 case "联系我们":{
					 JOptionPane.showMessageDialog(null, "The code is powered by USTC, Infonet Lab"
					 		, "联系我们", JOptionPane.YES_OPTION);
					 break;
				 }
				 case "3D轨道界面":{
					 //set3DWindow();
					 refresh3DWindow();
					 break;
				 }
				 case "2D轨道界面":{
					 refresh2DWindow();
					 break;
				 }
				 case "图表分析":{
			            JFileChooser fileChooser = new JFileChooser("reports//");
			            fileChooser.setDialogTitle("选择分析文件");
			            FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
			            fileChooser.setFileFilter(filter);
			            JLabel label = new JLabel();
			            int n = fileChooser.showOpenDialog(fileChooser);
			            if (n == fileChooser.APPROVE_OPTION){
			                String input =fileChooser.getSelectedFile().getPath();
			                new AddChartFrame(new Loadtxt(input));			           
			            }
			            break;
				 }
				 case "参数配置":{
					 new RouterInfo();
					 break;
				 }
			 }

		 }
	 }
	 /**
	  * 分析图表按钮动作监听器
	  */
    class OpenActionListener implements ActionListener{
        public void  actionPerformed(ActionEvent e){
            JFileChooser fileChooser = new JFileChooser("reports//");
            fileChooser.setDialogTitle("选择分析文件");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
            fileChooser.setFileFilter(filter);
            JLabel label = new JLabel();
            int n = fileChooser.showOpenDialog(fileChooser);
            if (n == fileChooser.APPROVE_OPTION){
                String input =fileChooser.getSelectedFile().getPath();
                new AddChartFrame(new Loadtxt(input));
            }
        }
    }
    
	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	public boolean getSimCancelled(){
		return this.simCancelled;
	}
	public boolean getPaused(){
		return this.simPaused;
	}
	
	private ImageIcon createImageIcon(String path) {
		java.net.URL imgURL = getClass().getResource(PATH_GRAPHICS+path);
		return new ImageIcon(imgURL);
	}
	
	private JButton addButton(String iconPath) {
		JButton button = new JButton(createImageIcon(iconPath));
		button.addActionListener(this);
		//button.setContentAreaFilled(false);
		ButtonMenus.add(button);
		return button;
	}
	/**
	 * 用于在图形界面点击终止按钮后，重置simCancelled的值，以进行下一次仿真
	 */
	public void resetSimCancelled(){
		if (this.simCancelled == true)
			this.simCancelled = false;
	}
	/**
	 * Sets simulation to pause or play.
	 * @param paused If true, simulation is put to pause
	 */
	public void setPaused(boolean paused) {
		if (!paused) {//暂停状态
			this.playButton.setIcon(createImageIcon(ICON_PLAY));
			this.simPaused = true;
			Orbit_3D.setFlag(false);
			Orbit_2D.setFlag(false);
		}
		else {//运行状态
			this.playButton.setIcon(createImageIcon(ICON_PAUSE));
			this.simPaused = false;
			Orbit_3D.setFlag(true);
			Orbit_2D.setFlag(true);
		}
	}
	
	/**
	 * 设置仿真时间
	 * @param time The time to show
	 */
	public void setSimTime(double time) {
		long timeSinceUpdate = System.currentTimeMillis() - this.lastUpdate;
		
		if (timeSinceUpdate > EPS_AVG_TIME) {
			double val = ((time - this.lastSimTime) * 1000)/timeSinceUpdate;
			String sepsValue = String.format("%.2f 1/s", val);

			this.sepsField.setText(sepsValue);
			this.lastSimTime = time;
			this.lastUpdate = System.currentTimeMillis();
		}
		else {
			this.sepsField.setText(String.format("%.1f", time));
		}
		
		if(time == 0){
			String sepsValue = String.format("%.2f 1/s", time);
			this.sepsField.setText(sepsValue);
		}
	}
}






