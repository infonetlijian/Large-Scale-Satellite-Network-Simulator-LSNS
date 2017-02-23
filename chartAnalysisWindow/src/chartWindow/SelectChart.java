package chartAnalysisWindow.src.chartWindow;

/**
 * Created by ustc on 2016/12/8.
 */
public class SelectChart {
    public Loadtxt load;

    public SelectChart(String FilePath, int i) {

        this.load = new Loadtxt(FilePath);
        String type = this.load.ChartType;
        switch (i) {
            case 0: {
                if (type == null || type == "line") {

                    CreateLineChart chart = new CreateLineChart(this.load);
                    load = chart.loadUpdate();
                } else {
                    CreateBarChart chart = new CreateBarChart(this.load);
                    load = chart.loadUpdate();
                }

                break;
            }

            case 1:
                CreateLineChart chart = new CreateLineChart(this.load);
                load = chart.loadUpdate();
                break;
            case 2:
                CreateBarChart chart2 = new CreateBarChart(this.load);
                load = chart2.loadUpdate();
                break;

        }

    }

    public SelectChart(String FilePath, int i, int x, int y) {
        this.load = new Loadtxt(FilePath);
        this.load.imageX =x;
        this.load.imageY =y;
        String type = this.load.ChartType;
        switch (i) {
            case 0: {
                if (type == null || type == "line") {

                    CreateLineChart chart = new CreateLineChart(this.load);
                    load = chart.loadUpdate();
                } else {
                    CreateBarChart chart = new CreateBarChart(this.load);
                    load = chart.loadUpdate();
                }
                break;
            }

            case 1:
                CreateLineChart chart = new CreateLineChart(this.load);
                load = chart.loadUpdate();
                break;
            case 2:
                CreateBarChart chart2 = new CreateBarChart(this.load);
                load = chart2.loadUpdate();
                break;


        }




    }


    public SelectChart(Loadtxt load0, int i, int x, int y) {
        this.load = load0;
        this.load.imageX =x;
        this.load.imageY =y;
        String type = this.load.ChartType;
        switch (i) {
            case 0: {
                if (type == null || type == "line") {

                    CreateLineChart chart = new CreateLineChart(this.load);
                    load = chart.loadUpdate();
                } else {
                    CreateBarChart chart = new CreateBarChart(this.load);
                    load = chart.loadUpdate();
                }
                break;
            }

            case 1:
                CreateLineChart chart = new CreateLineChart(this.load);
                load = chart.loadUpdate();
                break;
            case 2:
                CreateBarChart chart2 = new CreateBarChart(this.load);
                load = chart2.loadUpdate();
                break;


        }




    }

    public Loadtxt getload(){
        return load;
    }
}
