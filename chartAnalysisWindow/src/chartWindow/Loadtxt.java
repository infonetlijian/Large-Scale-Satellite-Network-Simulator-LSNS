package chartAnalysisWindow.src.chartWindow;

/**
 * Created by ustc on 2016/12/8.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class Loadtxt {
    private String FILEPATH = "input.txt";
    final static String DIVIDE = "below is the data";
    public String TITLE = null;
    public String XLABEL = null;
    public String YLABEL = null;
    public String ChartType = null;
    public String Margin = null;
    public String Xmin = null;
    public String Xmax = null;
    public String Ymin = null;
    public String Ymax = null;
    public String Xunit = null;
    public String Yunit = null;
    public int imageX=0;
    public int imageY=0;


  //  public HashMap<String, String> map;
  public HashMap<String, String> map;
    public ArrayList<String> dataX;
    public ArrayList<String> dataY;


    public Loadtxt(String filepath) {
        this.FILEPATH = filepath;
        this.map = new HashMap<String, String>();
        this.dataX = new ArrayList<>();
        this.dataY = new ArrayList<>();
        readTXT();
    }
    private void readTXT() {
        BufferedReader bufr = null;
        String buffer = null;
        boolean para = true;
        try {
            bufr = new BufferedReader(new FileReader(FILEPATH));

            while ((buffer = bufr.readLine()) != null) {
                //    System.out.print(buffer +"\n");


                if (buffer.startsWith(DIVIDE) && (para == true)) {
                    para = false;
                    buffer = bufr.readLine();

                }
                if (para == true) {
                    divide(buffer);
                } else {
                    dividedata(buffer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufr != null) {
                    bufr.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        getParameters();

    }
    private void divide(String buffer) {
        //buffer = buffer.toLowerCase().trim();
        buffer = buffer.trim();
        if (buffer.startsWith("#") || buffer.isEmpty()) {
            return;
        } else {
            String[] value = buffer.split("=", 2);
            this.map.put(value[0].trim(), value[1].trim());
           // System.out.print(buffer +"\n");

        }
    }

    private void dividedata(String buffer) {
        buffer = buffer.toLowerCase().trim();
        if (buffer.startsWith("#") || buffer.isEmpty()) {
            return;
        } else {
            String[] value = buffer.split(" ", 2);
            //   System.out.println("these are data");
            this.dataX.add(value[0]);
            this.dataY.add(value[1]);

            // map.put(value[0],value[1]);
        }
    }

    private void getParameters(){
        this.TITLE=this.map.get("title");
        this.XLABEL=this.map.get("xlabel");
        this.YLABEL=this.map.get("ylabel");
        this.ChartType=this.map.get("type");
        this.Margin = this.map.get("margin");
        this.Xmin = this.map.get("x_min");
        this.Xmax = this.map.get("x_max");
        this.Xunit = this.map.get("x_unit");
        this.Ymin = this.map.get("y_min");
        this.Ymax = this.map.get("y_max");
        this.Yunit = this.map.get("y_unit");
        //this.Margin = this.map.get("margin");



    }


}
