package Develop;


import java.awt.BorderLayout;  
import java.awt.Color;  
import java.awt.GradientPaint;  
import java.awt.Graphics;  
import java.awt.Graphics2D;  
import javax.swing.*;  
public class class1 extends JFrame{  
    private myPanel p;  
    public class1(String name){  
        super();            //继承父类的构造方法  
        setTitle(name);                 //名字  
        setBounds(0,0,300,300);     //大小  
        BorderLayout bl = new BorderLayout();  
        bl.setHgap(20);  
        bl.setVgap(20);  
        getContentPane().setLayout(bl);//布局管理  
            p = new myPanel("jarvischu");   
            p.setBounds(0, 0, 150, 150);  
            getContentPane().add(p,bl.CENTER);  
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);//设置默认关闭操作  
    }                                                                                                             
    public static void main(String args[]){  
    	class1 frame = new class1("JarvisChu");  
        frame.setVisible(true);       
    }   
}  
class myPanel extends JPanel{  
    private String m_Name;  
    public myPanel(String name){  
        m_Name = name;  
    }     
    public void paint(Graphics g){  
        Graphics2D g2d = (Graphics2D)g;  
        GradientPaint grdp = new GradientPaint(0,0,Color.blue,100,50,Color.RED);  
                                                           //创建一个渐变填充的对象  
        g2d.setPaint(grdp);                        //选中该Paint对象  
        g2d.fillRect(0, 0, 150, 150);  
    }  
}  