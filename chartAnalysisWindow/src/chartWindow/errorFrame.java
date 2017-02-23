package chartAnalysisWindow.src.chartWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Created by ustc on 2016/12/24.
 */
public class errorFrame extends JFrame {

  //  public  JButton confrim = new JButton("确定");
    public  JLabel error = new JLabel("数据格式不正确");
   // JPanel panel = new JPanel();
    public errorFrame(){
        super("警告");
        setBounds(300,300,300,100);
       // confrim.setBounds(100,100,20,20);
       // Container content = getContentPane();
      //  content.add(error,BorderLayout.CENTER);
    //    error.setBounds(310,20,120,40);
        error.setHorizontalAlignment(SwingConstants.CENTER);

        add(error);
        setResizable(false);
       // add(confrim);
        setVisible(true);


    }

   public static void main(String[] args){

       new errorFrame();
   }
}
