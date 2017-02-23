package Develop;

import java.awt.*;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class test extends JFrame{
    /**
     * @param args
     */
    public static void main(String[] args) {
        test frame = new test("TEST");
        frame.setVisible(true);
    }
    public test(String title){
        this.setSize(new Dimension(200,300));
        this.setTitle(title);
        this.getContentPane().add(new JPanel(){
            @Override
            public void paintComponent(Graphics g){
                // TODO Auto-generated method stub
                g.drawLine(40,40,80,40);
            }
        });
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}