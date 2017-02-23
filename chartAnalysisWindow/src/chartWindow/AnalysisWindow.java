package chartAnalysisWindow.src.chartWindow;

/**
 * Created by ustc on 2016/12/8.
 */

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AnalysisWindow {

    private JFrame frame = new JFrame();
    private  JPanel panel  = new JPanel();
    private Container content = frame.getContentPane();
    private  JToolBar toolbar = new JToolBar();
    private JButton OpenButton =new JButton("analysis");
    private JButton ExitButton = new JButton("EXIST");

    public AnalysisWindow(){
        frame.pack();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds(dim.width/5,dim.height/5,dim.width/2,dim.height/2);

        frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        OpenButton.addActionListener(new OpenActionListener());
        ExitButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e){
                System.exit(0);
            }
        });

        toolbar.add(OpenButton);
        toolbar.addSeparator();
        toolbar.add(ExitButton);
        content.add(toolbar,BorderLayout.NORTH);
        content.add(panel,BorderLayout.CENTER);
        frame.setVisible(true);

    }
    class OpenActionListener implements ActionListener{

        public void  actionPerformed(ActionEvent e){
           // JFileChooser fileChooser = new JFileChooser("F://娱乐//picture//");
            JFileChooser fileChooser = new JFileChooser("analysis//");
            fileChooser.setDialogTitle("选择分析文件");
            FileNameExtensionFilter filter = new FileNameExtensionFilter("TEXT FILES", "txt", "text");
            fileChooser.setFileFilter(filter);
            JLabel label = new JLabel();
            int n = fileChooser.showOpenDialog(fileChooser);
            if (n == fileChooser.APPROVE_OPTION){
                String input =fileChooser.getSelectedFile().getPath();
            //    System.out.print(input +"\n");// cancel------// ---------------------------\
               // new AddChartFrame(input);
                Loadtxt load =new Loadtxt(input);
                new AddChartFrame(load);

            }
            else
                label.setText("未选择");
            panel.removeAll();
            panel.add(label);
            content.add(panel);
            panel.updateUI();
            frame.repaint();

        }

    }

    public static void main(String[] args) {
        new AnalysisWindow();
    }
    }
