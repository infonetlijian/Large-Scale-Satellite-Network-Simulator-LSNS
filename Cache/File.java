/*
 * copyright 2017 ustc, Infonet
 */
package Cache;

import java.util.ArrayList;
import java.util.Random;
import core.SimClock;

public class File {
    private String id;
    private int size;
    private int fromAddressID;
    private double timeRequest;
    private boolean initFile;             							//	用于判断是否为初始化之后放入缓存中的文件
    private ArrayList<Integer> data = new ArrayList<Integer>();    	//	用动态链表来表示文件中数据
    private int dataSize=100;										//	数据大小为100
    
    public String getId() {
        return id;
    }

    public int getSize() {
        return size;
    }

    public int getFromAddressID() {
        return fromAddressID;
    }
    
    public double getTimeRequest(){
    	return this.timeRequest;
    }
    
    public void setTimeRequest(double time){
		this.timeRequest = time;
    }
    
    public void setInitFile(File file){
    	file.initFile=false;
    }
    
    public boolean getInitFile(){
    	return this.initFile;
    }
    
    /**
     * 对文件的复制函数，原因是不希望按引用传递参数。
     * @param File
     * @return
     */
	public File copyFrom(File File) {	
		//System.out.println(File.getId());
		File f = new File();		
		f.id = File.getId();
		f.size = File.getSize();
		f.timeRequest = File.getTimeRequest();
		f.fromAddressID = File.getFromAddressID();
		f.initFile = File.getInitFile();
		//f.data= File.data;
		return f;
	}
    
    public void setSize(int size) {						//文件的大小是随机设置的
        //Random random = new Random();
//        int max=5000;
//        int min=size/10;
//        this.size=random.nextInt(max)%(max-min+1) + min;
    	this.size = size;
    }

    public void setFromAddressID(int  nrofHosts) {
        Random random = new Random();
        this.fromAddressID = random.nextInt(nrofHosts);
        
//        System.out.println("文件的fromID为："+this.fromAddressID);
    }
    
    public ArrayList<Integer> getData(){
    	return this.data;
    }
    
    public void copyData(File File){
    	this.data = File.getData();
    }
    
    
    /**
     * 对文件进行初始化
     * @param ID为文件的ID
     * @param nrofHosts为节点的个数
     */
    public File(int ID, int nrofHosts){
        this.id="filename"+ID;
        this.timeRequest= SimClock.getTime();			// 文件在创建的时候，请求时间就为当前文件创建时间。
        this.initFile= true;
        setSize(20000);
        setFromAddressID(nrofHosts);					// 具体放在哪一个节点上也是随机的

        
        for(int i=0;i<dataSize;i++){
        	int m;
        	m=getRandomInt(100);						// data数据中存储的也是随机数
        	data.add(i, m);
        }
    }
    
    public File(){    }
    
    /**
     * 用于生成data数据中要存储的随机数
     * @param max 为最大值
     * @return 返回随机数
     */
    public int getRandomInt(int max){					
    	int m;
    	Random random = new Random();
        m=random.nextInt(max)%(max+1);
        return m;
    }
}
