package chartAnalysisWindow.src.chartWindow;
/**
 * Created by ustc on 2016/12/8.
 */


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CreateBarChart {
    public Loadtxt load;
    private IntervalXYDataset dataset;


    public CreateBarChart(Loadtxt e){
        this.load = e;
        //IntervalXYDataset dataset = createXYDataset(this.load);
        this.dataset = createXYDataset(this.load);
        //步骤2：根据Dataset 生成JFreeChart对象，以及做相应的设置
        JFreeChart freeChart = createChart(this.dataset,this.load);

        //步骤3：将JFreeChart对象输出到文件，Servlet输出流等
        if (load.imageX != 0 && load.imageY != 0){
            saveAsFile(freeChart, "analysis\\analysisChart.jpg", load.imageX , load.imageY);
        }
        else{
        saveAsFile(freeChart, "analysis\\analysisChart.jpg", 700, 400);}
        //   ChartPanel d= new ChartPanel(freeChart);
        // String title = "Combined Category Plot Demo 1";
        // demoShow  demo = new demoShow(freeChart);
        //   demo.pack();
        //   RefineryUtilities.centerFrameOnScreen(demo);
        //  demo.setVisible(true);


    }

    public  void saveAsFile(JFreeChart chart, String outputPath,
                                  int weight, int height) {
        FileOutputStream out = null;

        XYPlot plot = (XYPlot) chart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();

        NumberAxis domainAxis1 = (NumberAxis)plot.getDomainAxis();//x轴设置
        NumberAxis rAxis1 = (NumberAxis)plot.getRangeAxis();//Y轴设置
        ValueAxis domainAxis = plot.getDomainAxis();
        ValueAxis rAxis = plot.getRangeAxis();
      //  this.load.Yunit = Double.toString(rAxis1.getTickUnit().getSize());
     //   load.Xunit = Double.toString(domainAxis1.getTickUnit().getSize());
     //   System.out.print("  hhhhhhhh   "+rAxis1.getTickUnit().toString());



        try {
            File outFile = new File(outputPath);
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            out = new FileOutputStream(outputPath);
            // 保存为PNG
            ChartUtilities.writeChartAsPNG(out, chart, weight, height);
            // 保存为JPEG
            // ChartUtilities.writeChartAsJPEG(out, chart, 500, 400);
            out.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // do nothing
                }
            }
        }
    }

    public static JPanel createDemoPanel(JFreeChart chart) {
        //JFreeChart chart = createChart();
        return new ChartPanel(chart);
    }

    public static JFreeChart createChart(IntervalXYDataset dataset, Loadtxt load) {
        // 创建JFreeChart对象：ChartFactory.createXYLineChart
        JFreeChart jfreechart = ChartFactory.createXYBarChart(load.TITLE, // 标题
                load.XLABEL, // categoryAxisLabel （category轴，横轴，X轴标签）
                false, // valueAxisLabel（value轴，纵轴，Y轴的标签）
                load.YLABEL,
                dataset, // dataset
                PlotOrientation.VERTICAL, false, // legend
                false, // tooltips
                false); // URLs

        // 使用CategoryPlot设置各种参数。以下设置可以省略。
      //  XYPlot plot = (XYPlot) jfreechart.getPlot();
        // 背景色 透明度
      //  XYBarRenderer render = (XYBarRenderer)plot.getRenderer();//用来设置每个柱子的属性
        // render.setBase(12);
        double value =0.6;
        if (load.Margin != null){
            value =  Double.parseDouble(load.Margin );
        }
       // render.setMargin(value);
        // render.setItemMargin(0.001);
        // jfreechart.setBackgroundPaint(Color.GREEN);
       // plot.setBackgroundAlpha(0.5f);
        // 前景色 透明度
       // plot.setForegroundAlpha(0.5f);
        // 其它设置可以参考XYPlot类
        //  plot.setNoDataMessagePaint(Color.GREEN);


        XYPlot plot = (XYPlot) jfreechart.getPlot();
        XYBarRenderer renderer = (XYBarRenderer)plot.getRenderer();

        NumberAxis domainAxis1 = (NumberAxis)plot.getDomainAxis();//x轴设置
        NumberAxis rAxis1 = (NumberAxis)plot.getRangeAxis();//Y轴设置
       // domainAxis1.setTickUnit(new NumberTickUnit(2));


        plot.setDomainGridlinePaint(Color.blue);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.blue);
        plot.setRangeGridlinesVisible(true);
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(Color.magenta);
        renderer.setBaseOutlinePaint(Color.ORANGE);
        renderer.setDrawBarOutline(true);
        renderer.setMargin(value);

        ValueAxis domainAxis = plot.getDomainAxis();
        ValueAxis rAxis = plot.getRangeAxis();
        domainAxis.setTickLabelPaint(Color.red);//X轴的标题文字颜色
        domainAxis.setTickLabelsVisible(true);//X轴的标题文字是否显示
        domainAxis.setAxisLinePaint(Color.red);//X轴横线颜色
        domainAxis.setTickMarksVisible(true);//标记线是否显示
        domainAxis.setTickMarkOutsideLength(3);//标记线向外长度
        domainAxis.setTickMarkInsideLength(3);//标记线向内长度
        domainAxis.setTickMarkPaint(Color.red);//标记线颜色
        // domainAxis.setRange(5, 10);

        rAxis.setTickLabelPaint(Color.red);//Y轴的标题文字颜色
        rAxis.setTickLabelsVisible(true);//Y轴的标题文字是否显示
        rAxis.setAxisLinePaint(Color.red);//Y轴横线颜色
        rAxis.setTickMarksVisible(true);//标记线是否显示
        rAxis.setTickMarkOutsideLength(3);//标记线向外长度
        rAxis.setTickMarkInsideLength(3);//标记线向内长度
        rAxis.setTickMarkPaint(Color.red);//标记线颜色
        rAxis.setTickMarkInsideLength(3);//外刻度线向内长度
        rAxis.setTickMarkPaint(Color.red);//刻度线颜色
        rAxis.setTickLabelsVisible(true);//刻度数值是否显示
// 所有Y标记线是否显示（如果前面设置rAxis.setMinorTickMarksVisible(true); 则其照样显示）
        rAxis.setTickMarksVisible(true);
        rAxis.setAxisLinePaint(Color.red);//Y轴竖线颜色
        rAxis.setAxisLineVisible(true);//Y轴竖线是否显示
//设置最高的一个 Item 与图片顶端的距离 (在设置rAxis.setRange(100, 600);情况下不起作用)
        rAxis.setUpperMargin(0.15);
//设置最低的一个 Item 与图片底端的距离
        rAxis.setLowerMargin(0.15);
        rAxis.setAutoRange(true);//是否自动适应范围
        rAxis.setVisible(true);//Y轴内容是否显示

        //设置距离图片左端距离
        domainAxis.setUpperMargin(0.2);
        //设置距离图片右端距离
        domainAxis.setLowerMargin(0.2);
        //数据轴精度
        NumberAxis na = (NumberAxis) plot.getRangeAxis();
        na.setAutoRangeIncludesZero(true);
        //  DecimalFormat df = new DecimalFormat("#0.000");
        //数据轴数据标签的显示格式
        //  na.setNumberFormatOverride(df);
        //设置柱的透明度
        plot.setForegroundAlpha(1.0f);

      //  System.out.print(load.Xmin +"dddddddd");
        if(load.Xmin != null && load.Xmax != null){

            domainAxis.setRange(Double.parseDouble(load.Xmin), Double.parseDouble(load.Xmax));
          //  System.out.print("load.Xmin");
        }else{
            load.Xmin = Double.toString(domainAxis.getRange().getLowerBound());
            load.Xmax = Double.toString(domainAxis.getRange().getUpperBound());
        }
        if(load.Ymin != null && load.Ymax != null){

            rAxis.setRange(Double.parseDouble(load.Ymin), Double.parseDouble(load.Ymax));
        }else{
            load.Ymin = Double.toString(rAxis.getRange().getLowerBound());
            load.Ymax = Double.toString(rAxis.getRange().getUpperBound());
        }
        if(load.Xunit != null){
            domainAxis1.setTickUnit(new NumberTickUnit(Double.parseDouble(load.Xunit)));
        }else{
        //    load.Xunit = Double.toString(domainAxis1.getTickUnit().getSize());
          //  System.out.print("\n"+load.Xunit+"\n");
        }
        if(load.Yunit != null){
            rAxis1.setTickUnit(new NumberTickUnit(Double.parseDouble(load.Yunit)));
        }else{
          //  load.Yunit = Double.toString(rAxis1.getTickUnit().getSize());
        }






        plot.setRenderer(renderer);



        return jfreechart;
    }

    /**
     * 创建XYDataset对象
     *
     */
    private static XYSeriesCollection createXYDataset(Loadtxt load) {
        XYSeries xyseries1 = new XYSeries("");
        double x =0;
        double y =0;
        for (int i=0;i<load.dataX.size();i++){
            // xyseries1.add(1987, 50);
            x =  Double.parseDouble(load.dataX.get(i));
            y =  Double.parseDouble(load.dataY.get(i));
            xyseries1.add(x,y);


        }


        XYSeriesCollection xySeriesCollection = new XYSeriesCollection(xyseries1);

        return xySeriesCollection;
    }

    public Loadtxt loadUpdate(){

        return this.load;
    }





}
