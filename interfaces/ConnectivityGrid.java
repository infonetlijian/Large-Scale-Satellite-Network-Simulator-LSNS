/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package interfaces;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import movement.MovementModel;
import core.Coord;
import core.DTNHost;
import core.DTNSim;
import core.Neighbors;
import core.NetworkInterface;
import core.Settings;
import core.SettingsError;
import core.SimClock;
import core.World;

/**
 * <P>
 * Overlay grid of the world where each interface is put on a cell depending
 * of its location. This is used in cell-based optimization of connecting
 * the interfaces.</P>
 * 
 * <P>The idea in short:<BR>
 * Instead of checking for every interface if some of the other interfaces are close
 * enough (this approach obviously doesn't scale) we check only interfaces that
 * are "close enough" to be possibly connected. Being close enough is
 * determined by keeping track of the approximate location of the interfaces 
 * by storing them in overlay grid's cells and updating the cell information
 * every time the interfaces move. If two interfaces are in the same cell or in 
 * neighboring cells, they have a chance of being close enough for
 * connection. Then only that subset of interfaces is checked for possible
 * connectivity. 
 * </P>
 * <P>
 * <strong>Note:</strong> this class does NOT support negative
 * coordinates. Also, it makes sense to normalize the coordinates to start
 * from zero to conserve memory. 
 */
public class ConnectivityGrid extends ConnectivityOptimizer {

	/**
	 * Cell based optimization cell size multiplier -setting id ({@value}).
	 * Used in {@link World#OPTIMIZATION_SETTINGS_NS} name space.
	 * Single ConnectivityCell's size is the biggest radio range times this.
	 * Larger values save memory and decrease startup time but may result in
	 * slower simulation.
	 * Default value is {@link #DEF_CON_CELL_SIZE_MULT}.
	 * Smallest accepted value is 1.
	 */
	public static final String CELL_SIZE_MULT_S = "cellSizeMult";
	/** default value for cell size multiplier ({@value}) */
	public static final int DEF_CON_CELL_SIZE_MULT = 1;
	
	private GridCell[][][] cells;//GridCell这个类，创建一个实例代表一个单独的网格，整个world创建了一个三维数组存储这个网格，每个网格内又存储了当前在其中的host的networkinterface
	private HashMap<NetworkInterface, GridCell> ginterfaces;
	private int cellSize;
	private int rows;
	private int cols;
	private int zs;//新增三维变量
	private static int worldSizeX;
	private static int worldSizeY;
	private static int worldSizeZ;//新增
	private static int cellSizeMultiplier;

	static HashMap<Integer,ConnectivityGrid> gridobjects;

	static {
		DTNSim.registerForReset(ConnectivityGrid.class.getCanonicalName());
		reset();
	}
	
	public static void reset() {
		gridobjects = new HashMap<Integer, ConnectivityGrid>();

		Settings s = new Settings(MovementModel.MOVEMENT_MODEL_NS);
		int [] worldSize = s.getCsvInts(MovementModel.WORLD_SIZE,3);//参数从2维修改为3维
		worldSizeX = worldSize[0];
		worldSizeY = worldSize[1];
		worldSizeZ = worldSize[2];
		
		s.setNameSpace(World.OPTIMIZATION_SETTINGS_NS);		
		if (s.contains(CELL_SIZE_MULT_S)) {
			cellSizeMultiplier = s.getInt(CELL_SIZE_MULT_S);
		}
		else {
			cellSizeMultiplier = DEF_CON_CELL_SIZE_MULT;
		}
		if (cellSizeMultiplier < 1) {
			throw new SettingsError("Too small value (" + cellSizeMultiplier +
					") for " + World.OPTIMIZATION_SETTINGS_NS + 
					"." + CELL_SIZE_MULT_S);
		}
	}

	/**
	 * Creates a new overlay connectivity grid
	 * @param cellSize Cell's edge's length (must be larger than the largest
	 * 	radio coverage's diameter)
	 */
	private ConnectivityGrid(int cellSize) {
		this.rows = worldSizeY/cellSize + 1;
		this.cols = worldSizeX/cellSize + 1;
		this.zs = worldSizeZ/cellSize + 1;//新增
		System.out.println(cellSize+"  "+this.rows+"  "+this.cols+"  "+this.zs);
		// leave empty cells on both sides to make neighbor search easier 
		this.cells = new GridCell[rows+2][cols+2][zs+2];
		this.cellSize = cellSize;

		for (int i=0; i<rows+2; i++) {
			for (int j=0; j<cols+2; j++) {
				for (int n=0;n<zs+2; n++)//新增三维变量
					this.cells[i][j][n] = new GridCell();
			}
		}
		ginterfaces = new HashMap<NetworkInterface,GridCell>();
	}

	/**
	 * Returns a connectivity grid object based on a hash value
	 * @param key A hash value that separates different interfaces from each other
	 * @param maxRange Maximum range used by the radio technology using this 
	 *  connectivity grid. 
	 * @return The connectivity grid object for a specific interface
	 */
	public static ConnectivityGrid ConnectivityGridFactory(int key, 
			double maxRange) {
		if (gridobjects.containsKey((Integer)key)) {
			return (ConnectivityGrid)gridobjects.get((Integer)key);
		} else {
			ConnectivityGrid newgrid = 
				new ConnectivityGrid((int)Math.ceil(maxRange * 
						cellSizeMultiplier));//每次生成一个新的网格ConnectivityGrid，分配给每一个节点的网络接口
			gridobjects.put((Integer)key,newgrid);
			return newgrid;
		}
	}

	/**
	 * Adds a network interface to the overlay grid
	 * @param ni The new network interface
	 */
	public void addInterface(NetworkInterface ni) {
		GridCell c = cellFromCoord(ni.getLocation());
		c.addInterface(ni);
		ginterfaces.put(ni,c);
	}

	/** 
	 * Removes a network interface from the overlay grid 
	 * @param ni The interface to be removed
	 */
	public void removeInterface(NetworkInterface ni) {
		GridCell c = ginterfaces.get(ni);
		if (c != null) {
			c.removeInterface(ni);
		}
		ginterfaces.remove(ni);
	}

	/**
	 * Adds interfaces to overlay grid
	 * @param interfaces Collection of interfaces to add
	 */
	public void addInterfaces(Collection<NetworkInterface> interfaces) {
		for (NetworkInterface n : interfaces) {
			addInterface(n);
		}
	}

	/**
	 * Checks and updates (if necessary) interface's position in the grid
	 * @param ni The interface to update
	 */
	public void updateLocation(NetworkInterface ni) {
		GridCell oldCell = (GridCell)ginterfaces.get(ni);//ginterfaces是HashMap<NetworkInterface, GridCell>映射
		GridCell newCell = cellFromCoord(ni.getLocation());//返回其绑定的host当前所在坐标，再从坐标转换成对应网格

		if (newCell != oldCell) {//一旦移动到了另一个网格当中，则需要相应更新
			oldCell.moveInterface(ni, newCell);//此函数把此网卡ni从旧的网格当中移到新的网格当中存储
			
			ginterfaces.put(ni,newCell);//更新映射表中对应的网格值
		}
	}

	/**
	 * Finds all neighboring cells and the cell itself based on the coordinates
	 * @param c The coordinates
	 * @return Array of neighboring cells 
	 */
	private GridCell[] getNeighborCellsByCoord(Coord c) {
		// +1 due empty cells on both sides of the matrix
		int row = (int)(c.getY()/cellSize) + 1;
		int col = (int)(c.getX()/cellSize) + 1;
		int z = (int)(c.getZ()/cellSize) + 1;
		return getNeighborCells(row,col,z);
	}

	/**
	 * Returns an array of Cells that contains the neighbors of a certain
	 * cell and the cell itself.
	 * @param row Row index of the cell
	 * @param col Column index of the cell
	 * @return Array of neighboring Cells
	 */
	private GridCell[] getNeighborCells(int row, int col ,int z) {//新增三维变量
		return new GridCell[] {
			cells[row-1][col-1][z-1],cells[row-1][col-1][z],cells[row-1][col-1][z+1],//1st row,1st col
			cells[row-1][col][z-1],cells[row-1][col][z],cells[row-1][col][z+1],//1st row,2nd col
			cells[row-1][col+1][z-1],cells[row-1][col+1][z],cells[row-1][col+1][z+1],//1st row,3rd col
			
			cells[row][col-1][z-1],cells[row][col-1][z],cells[row][col-1][z+1],//2nd row,1st col
			cells[row][col][z-1],cells[row][col][z],cells[row][col][z+1],//2nd row,2nd col
			cells[row][col+1][z-1],cells[row][col+1][z],cells[row][col+1][z+1],//2nd row,3rd col
			
			cells[row+1][col-1][z-1],cells[row+1][col-1][z],cells[row+1][col-1][z+1],//3rd row,1st col
			cells[row+1][col][z-1],cells[row+1][col][z],cells[row+1][col][z+1],//3rd row,2nd col
			cells[row+1][col+1][z-1],cells[row+1][col+1][z],cells[row+1][col+1][z+1]//3rd row,3rd col
					
		};
	}

	/**
	 * Get the cell having the specific coordinates
	 * @param c Coordinates
	 * @return The cell
	 */
	private GridCell cellFromCoord(Coord c) {
		// +1 due empty cells on both sides of the matrix
		int row = (int)(c.getY()/cellSize) + 1; 
		int col = (int)(c.getX()/cellSize) + 1;
		int z = (int)(c.getZ()/cellSize) + 1;
		assert row > 0 && row <= rows && col > 0 && col <= cols : "Location " + 
		c + " is out of world's bounds";
		
		return this.cells[row][col][z];
	}

	/**
	 * Returns all interfaces that use the same technology and channel
	 */
	public Collection<NetworkInterface> getAllInterfaces() {
		return (Collection<NetworkInterface>)ginterfaces.keySet();
	}

	/**
	 * Returns all interfaces that are "near" (i.e., in neighboring grid cells) 
	 * and use the same technology and channel as the given interface 
	 * @param ni The interface whose neighboring interfaces are returned
	 * @return List of near interfaces
	 */
	public Collection<NetworkInterface> getNearInterfaces(
			NetworkInterface ni) {
		ArrayList<NetworkInterface> niList = new ArrayList<NetworkInterface>();
		GridCell loc = (GridCell)ginterfaces.get(ni);//ginterfaces是HashMap<NetworkInterface, GridCell>映射
		/**
		 * 下面改写，加了判断语句，即便是相邻的圈内也要在通信半径内才算邻居
		 */
		Coord c = ni.getLocation();//节点当前的位置坐标
		
		if (loc != null) {	
			GridCell[] neighbors = 
				getNeighborCellsByCoord(c);//得到可能与本节点成为邻居的9*9*9个相邻网格数组，依次检查这些数组内的
			
			//ni.getHost().getNeighbors()即返回ni网络接口对应的host的邻居数据库
			
			List<NetworkInterface> potentialNeighbors = 
					new ArrayList<NetworkInterface>(neighbors.length);//潜在邻居对象，即相邻9个网格内的卫星节点
			for (int i=0; i < neighbors.length; i++) {
				for (NetworkInterface interf : neighbors[i].getInterfaces()){//相邻网格内节点的网络接口，依次检查是不是在距离范围之内									
					if (ni.getHost().getNeighbors().JudgeNeighbors(interf.getHost().getLocation(), c))//判断是否为邻居
						niList.add(interf);//确认是邻居节点的列表
					else
						potentialNeighbors.add(interf);//添加潜在邻居对象（但还不是邻居对象），即相邻9个网格内的卫星节点，做预测用
				}
				//niList.addAll(neighbors[i].getInterfaces());//直接把这些网格内的节点都视作邻居节点加入?待改??????????????????????????????????????????????????
			}
			ni.getHost().getNeighbors().changePotentialNeighbors(potentialNeighbors);//放入记录的需要预测的节点
		}
		//System.out.print(this.host);
		//System.out.print(SimClock.getTime());
		//System.out.println(niList);//Networkinterface的显示函数  :return "net interface " + this.address + " of " + this.host + ". Connections: " +	this.connections;
		
		return niList;
	}
	

	/**
	 * Returns a string representation of the ConnectivityCells object
	 * @return a string representation of the ConnectivityCells object
	 */
	public String toString() {
		return getClass().getSimpleName() + " of size " + 
			this.cols + "x" + this.rows + ", cell size=" + this.cellSize;
	}

	/**
	 * A single cell in the cell grid. Contains the interfaces that are 
	 * currently in that part of the grid.
	 */
	public class GridCell {
		// how large array is initially chosen
		private static final int EXPECTED_INTERFACE_COUNT = 5;
		private ArrayList<NetworkInterface> interfaces;//GridCell就是依靠维护网络接口列表，来记录在此网格内的节点，对于全局网格来说，需要保证同一个网络接口不会同时出现在两个GridCell中

		private GridCell() {
			this.interfaces = new ArrayList<NetworkInterface>(
					EXPECTED_INTERFACE_COUNT);
		}

		/**
		 * Returns a list of of interfaces in this cell
		 * @return a list of of interfaces in this cell
		 */
		public ArrayList<NetworkInterface> getInterfaces() {
			return this.interfaces;
		}

		/**
		 * Adds an interface to this cell
		 * @param ni The interface to add
		 */
		public void addInterface(NetworkInterface ni) {
			this.interfaces.add(ni);
		}

		/**
		 * Removes an interface from this cell
		 * @param ni The interface to remove
		 */
		public void removeInterface(NetworkInterface ni) {
			this.interfaces.remove(ni);
		}

		/**
		 * Moves a interface in a Cell to another Cell
		 * @param ni The interface to move
		 * @param to The cell where the interface should be moved to
		 */
		public void moveInterface(NetworkInterface ni, GridCell to) {
			to.addInterface(ni);
			boolean removeOk = this.interfaces.remove(ni); //ArrayList<NetworkInterface> interfaces;
			assert removeOk : "interface " + ni + 
				" not found from cell with " + interfaces.toString();
		}

		/**
		 * Returns a string representation of the cell
		 * @return a string representation of the cell
		 */
		public String toString() {
			return getClass().getSimpleName() + " with " + 
				this.interfaces.size() + " interfaces :" + this.interfaces;
		}
	}
	/**
	 * 用于分簇路由的
	 * @param ni
	 * @param clusterHosts
	 * @return
	 */
	public Collection<NetworkInterface> getNearInterfaces(
			NetworkInterface ni, List<DTNHost> clusterHosts, List<DTNHost> hostsOfGEO) {
		
		ArrayList<NetworkInterface> niList = new ArrayList<NetworkInterface>();
		GridCell loc = (GridCell)ginterfaces.get(ni);//ginterfaces是HashMap<NetworkInterface, GridCell>映射
		/**
		 * 下面改写，加了判断语句，即便是相邻的圈内也要在通信半径内才算邻居
		 */
		Coord c = ni.getLocation();//节点当前的位置坐标
		
		if (loc != null) {	
			GridCell[] neighbors = 
				getNeighborCellsByCoord(c);//得到可能与本节点成为邻居的9*9*9个相邻网格数组，依次检查这些数组内的
			
			List<NetworkInterface> potentialNeighbors = 
					new ArrayList<NetworkInterface>(neighbors.length);//潜在邻居对象，即相邻9个网格内的卫星节点
			for (int i=0; i < neighbors.length; i++) {
				for (NetworkInterface interf : neighbors[i].getInterfaces()){//相邻网格内节点的网络接口，依次检查是不是在距离范围之内									
					if (ni.getHost().getNeighbors().JudgeNeighbors(interf.getHost().getLocation(), c))//判断是否为邻居
						if (clusterHosts.contains(interf.getHost()) || hostsOfGEO.contains(interf.getHost()))//且是否为本簇之内的节点或是GEO节点
							niList.add(interf);//确认是邻居节点的列表
					else
						potentialNeighbors.add(interf);//添加潜在邻居对象（但还不是邻居对象），即相邻9个网格内的卫星节点，做预测用
				}
			}
			ni.getHost().getNeighbors().changePotentialNeighbors(potentialNeighbors);//放入记录的需要预测的节点
		}
		
		return niList;
	}
}