package Develop;

import javax.swing.JWindow;

import java.awt.Color;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JWindow;

@SuppressWarnings ("serial" )
public class WaitingWindow extends JWindow implements Runnable{
	//定义加载窗口大小
	public static final int LOAD_WIDTH = 400;
	public static final int LOAD_HEIGHT = 225;
	// 获取屏幕窗口大小
	public static final int WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
	public static final int HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;
	public JLabel label;
	public JLabel waitTime;

	// 构造函数
	public WaitingWindow() {
		// 创建标签 , 并在标签上放置一张图片
		label = new JLabel(new ImageIcon("images/loading.gif"));
		label.setBounds(0, 0, LOAD_WIDTH, LOAD_HEIGHT - 15);
		waitTime = new JLabel("程序正在载入，等待时间为：0 s");
		waitTime.setBounds(0, LOAD_HEIGHT - 15, LOAD_WIDTH, 15);
		this.add(label);
		this.add(waitTime);

		// 设置布局为空
		this.setLayout(null);
		// 设置窗口初始位置
		this.setLocation((WIDTH - LOAD_WIDTH) / 2, (HEIGHT - LOAD_HEIGHT) / 2);
		// 设置窗口大小
		this.setSize(LOAD_WIDTH, LOAD_HEIGHT);
		// 设置窗口显示
		this.setVisible(true);
		this.setAlwaysOnTop(true);
	}

	@Override
	public void run() {
		for (int i = 0; i < 100000; i++) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			this.setTime(i);
		}

	}
	public void setTime(int i){
		this.waitTime.setText("程序正在载入，等待时间为："+ i +" "+"s");
	}
    /**
     * 加载资源
     */
    private WaitingWindow Loading(){
		WaitingWindow t = new WaitingWindow();
		new Thread(t).start();
		return t;
    }
    
}