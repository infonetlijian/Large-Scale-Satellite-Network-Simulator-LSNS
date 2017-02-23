package chartAnalysisWindow.src.chartWindow;

/**
 * Created by ustc on 2016/12/8.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AddChartFrame {


    public Loadtxt load;
    public int res=0;
    private String txtPath;
    public int type=0;
    JFrame frame = new JFrame();
    JPanel panel = new JPanel();
    Container content = frame.getContentPane();
    JToolBar toolbar = new JToolBar();
    JLabel label = new JLabel("  图表类型  ");
    JLabel imageLabel = new JLabel("");
    JButton confButton = new JButton("参数配置");
    JButton ExitButton = new JButton("退出");
    JComboBox comboBox=new JComboBox();
    public AddChartFrame(Loadtxt load0){
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      //  txtPath = input;

        load =load0;
        SelectChart t =new SelectChart(load,0,700,400);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(dim.width/5,dim.height/5,dim.width/2,dim.height/2);
        showChart();
        ExitButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                System.exit(0);
            }
        });
        confButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final chartConfig conf1 =new chartConfig(load);
                conf1.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

                conf1.addWindowListener(new WindowListener() {
                    @Override
                    public void windowOpened(WindowEvent e) {

                    }

                    @Override
                    public void windowClosing(WindowEvent e) {
                    	
                    }

                    @Override
                    public void windowClosed(WindowEvent e) {
                    	
                        load=conf1.loadUpdate();
                        double m=1;
                        res=1;
                        int height = (int)(frame.getSize().getHeight()*m);
                        int width = (int)(frame.getSize().getWidth()*m);
                        // imageLabel.setBounds(100,100,(int)(width/1.2),(int)(height/1.2));
                        imageLabel.setBounds((int)(width/10),(int)(height/10),(int)(width/1.4),(int)(height/1.4));


                        int labelheight = (int)(imageLabel.getSize().getHeight()*m);
                        int labelwidth = (int)(imageLabel.getSize().getWidth()*m);
                        int frameheight = (int)(frame.getSize().getHeight());
                        int framewidth = (int)(frame.getSize().getWidth());

                        SelectChart t =new SelectChart(load,type,labelwidth,labelheight);
                        showChart();


                    }

                    @Override
                    public void windowIconified(WindowEvent e) {

                    }

                    @Override
                    public void windowDeiconified(WindowEvent e) {

                    }

                    @Override
                    public void windowActivated(WindowEvent e) {

                    }

                    @Override
                    public void windowDeactivated(WindowEvent e) {

                    }
                });
               // showChart();
            }
        });

        comboBox.addItem("默认");
        comboBox.addItem("折线图");
        comboBox.addItem("柱状图");

        //toolbar.add(ExitButton);
        toolbar.add(label);
        toolbar.add(comboBox);
        toolbar.add(confButton);
        content.add(toolbar,BorderLayout.NORTH);
        content.add(panel,BorderLayout.CENTER);
        frame.add(imageLabel);
//        showChart();
        comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() ==ItemEvent.SELECTED){

                    double m =1;
                    res =1;
                    int height = (int)(frame.getSize().getHeight()*m);
                    int width = (int)(frame.getSize().getWidth()*m);
                    imageLabel.setBounds((int)(width/10),(int)(height/10),(int)(width/1.4),(int)(height/1.4));

                    int labelheight = (int)(imageLabel.getSize().getHeight()*m);
                    int labelwidth = (int)(imageLabel.getSize().getWidth()*m);
                    int frameheight = (int)(frame.getSize().getHeight());
                    int framewidth = (int)(frame.getSize().getWidth());



                    SelectChart t =new SelectChart(load,comboBox.getSelectedIndex(),labelwidth,labelheight);
                    type = comboBox.getSelectedIndex();



                    showChart();


                }

            }
        });


        frame.addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {

                double m =1;
                res=1;
                int height = (int)(frame.getSize().getHeight()*m);
                int width = (int)(frame.getSize().getWidth()*m);
               // imageLabel.setBounds(100,100,(int)(width/1.2),(int)(height/1.2));
                imageLabel.setBounds((int)(width/10),(int)(height/10),(int)(width/1.4),(int)(height/1.4));


                int labelheight = (int)(imageLabel.getSize().getHeight()*m);
                int labelwidth = (int)(imageLabel.getSize().getWidth()*m);
                int frameheight = (int)(frame.getSize().getHeight());
                int framewidth = (int)(frame.getSize().getWidth());

                SelectChart t =new SelectChart(load,type,labelwidth,labelheight);
                showChart();


            }
        });

        frame.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
               // System.out.print("qwe"+"\n");
                double m =1;
                res=res+1;
                if(res%30 == 0){
                      int height = (int)(frame.getSize().getHeight()*m);
                      int width = (int)(frame.getSize().getWidth()*m);
                    imageLabel.setBounds((int)(width/10),(int)(height/10),(int)(width/1.4),(int)(height/1.4));


                    int labelheight = (int)(imageLabel.getSize().getHeight()*m);
                    int labelwidth = (int)(imageLabel.getSize().getWidth()*m);
                    int frameheight = (int)(frame.getSize().getHeight());
                    int framewidth = (int)(frame.getSize().getWidth());


                    SelectChart t =new SelectChart(load,type,labelwidth,labelheight);
                    showChart();

                }



            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });
       //   frame.setVisible(true);

    }


    public void showChart(){
      //  ImageIcon icon = new ImageIcon("F://mychat.jpg");analusis\analysisChart.jpg
        ImageIcon icon = new ImageIcon("analysis\\analysisChart.jpg");
if(res==0) {
    icon.setImage(icon.getImage().getScaledInstance(icon.getIconWidth(), icon.getIconHeight(), Image.SCALE_DEFAULT));
    res=1;
}else {
    int labelheight = (int) (imageLabel.getSize().getHeight());
    int labelwidth = (int) (imageLabel.getSize().getWidth());
    icon.setImage(icon.getImage().getScaledInstance(labelwidth  ,labelheight, Image.SCALE_DEFAULT));
};
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setIcon(icon);
        //frame.pack();
        frame.setVisible(true);


    }




}
