package chartAnalysisWindow.src.chartWindow;

/**
 * Created by ustc on 2016/12/19.
 */

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.regex.Pattern;

//public class chartConfig extends JFrame implements ActionListener,ChangeListener {
public class chartConfig extends JFrame implements ActionListener,ChangeListener {
    public JButton Confirm;
    public JButton Reset;
    public JButton Concel;
    public JButton Adaption;
    private JTextField X_min;
    private JTextField X_max;
    private JTextField X_unit;
    private JTextField Y_min;
    private JTextField Y_max;
    private JTextField Y_unit;

public Loadtxt load;



    public chartConfig(Loadtxt load0){
        super("参数配置");
        load =load0;
        this.setSize(400, 500);
        JTabbedPane tabs = new JTabbedPane();
    //    this.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.setLayout((LayoutManager)null);
         JPanel jp1 = this.test();


        tabs.add("坐标轴设置", jp1);

        JPanel ButtonMenu = new JPanel();
        ButtonMenu.setLayout(new BoxLayout(ButtonMenu, 0));
      //  this.Reset = new JButton("默认");
      //  this.Reset.addActionListener(this);
        this.Concel = new JButton("退出");
        this.Concel.addActionListener(this);
        this.Confirm = new JButton("应用");
        this.Confirm.addActionListener(this);
        this.Adaption = new JButton("自适应坐标轴");
        this.Adaption.addActionListener(this);
     //   ButtonMenu.add(Box.createHorizontalStrut(10));
     //   ButtonMenu.add(this.Reset);
        ButtonMenu.add(Box.createHorizontalStrut(10));
        ButtonMenu.add(this.Concel);
        ButtonMenu.add(Box.createHorizontalStrut(10));
        ButtonMenu.add(this.Confirm);
        ButtonMenu.add(Box.createHorizontalStrut(10));
        ButtonMenu.add(this.Adaption);
        tabs.setBounds(0, 0, 666, 380);
        ButtonMenu.setBounds(20, 405, 300, 30);
        this.add(tabs);
        this.add(ButtonMenu);
        this.setLocationRelativeTo((Component)null);
        this.setResizable(false);
        this.setVisible(true);
    }

    public JPanel test() {
        JPanel jp1 = new JPanel();
        JPanel LinkFirst = new JPanel();
        LinkFirst.setBorder(new TitledBorder("图表配置界面"));
        LinkFirst.setLayout((LayoutManager)null);
        JLabel label1 = new JLabel("X轴间隔 ：", 4);
        if(load.Xunit != null){
        this.X_unit = new JTextField(load.Xunit);}
        else {
            this.X_unit = new JTextField("0");
        }
        label1.setBounds(0, 25, 80, 30);
        this.X_unit.setBounds(95, 25, 170, 30);
        LinkFirst.add(label1);
        LinkFirst.add(this.X_unit);
        JLabel label2 = new JLabel("Y轴间隔 ：", 4);
        if(load.Yunit != null){
        this.Y_unit = new JTextField(load.Yunit, 0);}
        else {
            this.Y_unit = new JTextField("0", 0);
        }
        this.Y_unit.addActionListener(this);
        label2.setBounds(0, 65, 80, 30);
        this.Y_unit.setBounds(95, 65, 170, 30);
        LinkFirst.add(label2);
        LinkFirst.add(this.Y_unit);
        JLabel label3 = new JLabel("X轴范围 ：", 4);
        this.X_min = new JTextField(load.Xmin);
        this.X_max = new JTextField(load.Xmax);
        label3.setBounds(0, 105, 80, 30);
        this.X_min.setBounds(95, 105, 50, 30);
        JLabel labelInterval = new JLabel("-");
        labelInterval.setBounds(160, 105, 50, 30);
        this.X_max.setBounds(180, 105, 50, 30);
        LinkFirst.add(label3);
        LinkFirst.add(this.X_min);
        LinkFirst.add(labelInterval);
        LinkFirst.add(this.X_max);
        JLabel label4 = new JLabel("Y轴范围 ：", 4);
        this.Y_min = new JTextField(load.Ymin);
        this.Y_max = new JTextField(load.Ymax);
        JLabel labelInterval2 = new JLabel("-");
        labelInterval2.setBounds(160, 145, 20, 30);
        label4.setBounds(0, 145, 80, 30);
        this.Y_min.setBounds(95, 145, 50, 30);
        this.Y_max.setBounds(180, 145, 50, 30);
        LinkFirst.add(label4);
        LinkFirst.add(this.Y_min);
        LinkFirst.add(this.Y_max);
        LinkFirst.add(labelInterval2);

        JLabel labelnote = new JLabel("注： 0 为自适应参数");
        labelnote.setBounds(50, 245, 200, 30);
        LinkFirst.add(labelnote);




    jp1.setLayout((LayoutManager)null);
        LinkFirst.setBounds(10, 0, 330, 350);
        jp1.add(LinkFirst);
        return jp1;
    }

   public void ParaSet(){


if(confCherk()) {
    //System.out.print(X_unit.getText());
    if (Double.parseDouble(this.X_unit.getText()) != 0) {
        load.Xunit = this.X_unit.getText();
    } else {
        load.Xunit = null;
    }
    if (Double.parseDouble(this.Y_unit.getText()) != 0) {
        load.Yunit = this.Y_unit.getText();
    } else {
        load.Yunit = null;
    }

    load.Xmin = this.X_min.getText();
    load.Xmax = this.X_max.getText();
    load.Ymin = this.Y_min.getText();
    load.Ymax = this.Y_max.getText();
    dispose();

}
       else {
    new errorFrame();

    //System.out.print("error");

}



   }

    public void stateChanged(ChangeEvent e) {
    }
    public void actionPerformed(ActionEvent e) {
        if(e.getSource() == this.Confirm) {
           ParaSet();

           // System.exit(1);
         //   dispose();


           // this.setVisible(false);
        }

        if(e.getSource() == this.Concel) {
            this.setVisible(false);
        }
        if(e.getSource() == this.Adaption) {
            this.X_unit.setText("0");
            this.Y_unit.setText("0");
        }

        e.getSource();
    }

    public void setConf(){




    }
    public Loadtxt loadUpdate(){
        return load;

    }


    public boolean confCherk(){
        String s1 =X_unit.getText() ;
        String s2 =Y_unit.getText() ;
        String s3 =X_min.getText() ;
        String s4 =X_max.getText() ;
        String s5 = Y_min.getText();
        String s6 = Y_max.getText();


        if(valueCherk(s1)&&valueCherk(s2)&&valueCherk(s3)&&valueCherk(s4)&&valueCherk(s5)&&valueCherk(s6)){
            boolean X_compare = Double.parseDouble(s6)>Double.parseDouble(s5);
            boolean Y_compare = Double.parseDouble(s4)>Double.parseDouble(s3);
            boolean X_u = Double.parseDouble(s6)>=0;
            boolean Y_u = Double.parseDouble(s4)>=0;
            return X_compare&&Y_compare && X_u &&Y_u;
        }
        else return false;


    }
    public boolean valueCherk(String s){

        Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
        return pattern.matcher(s).matches();
    }


    public static void main(String[] args){
       // new chartConfig();

    }

}




