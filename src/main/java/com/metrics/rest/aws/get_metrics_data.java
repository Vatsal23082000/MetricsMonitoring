package com.metrics.rest.aws;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;

@Path("/Aws")
public class get_metrics_data {
    static List<Double> a1 = new ArrayList<>();
    static List<Instant> a2 = new ArrayList<>();
    static String chartTitle;
    public static class LineChartExample extends JFrame {

        private static final long serialVersionUID = 1L;

        //this class is made for creating the chart (not required for getting data)
        public LineChartExample(String title) {
            super(title);
            // Create dataset
            DefaultCategoryDataset dataset = createDataset();
            // Create chart
            JFreeChart chart = ChartFactory.createLineChart(
                    chartTitle, // Chart title
                    "Date and Time", // X-Axis Label
                    "Average Value", // Y-Axis Label
                    dataset
            );
            ChartPanel chartPanel = new ChartPanel(chart) ;

            chartPanel.addChartMouseListener(new ChartMouseListener() {

                @Override
                public void chartMouseClicked(ChartMouseEvent e) {
                    final ChartEntity entity = e.getEntity();
                    System.out.println(entity + " " + entity.getArea());
                }

                @Override
                public void chartMouseMoved(ChartMouseEvent e) {
                }
            });
            chart.setBackgroundPaint(Color.white);
            ChartUtilities.applyCurrentTheme(chart);
            final CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.black);
            plot.setRangeGridlinePaint(Color.white);
            CategoryAxis catAxis = plot.getDomainAxis();
            CategoryItemRenderer rendu = plot.getRenderer();
            rendu.setSeriesPaint(0, new Color(255,255,100));
            catAxis.setCategoryLabelPositions(CategoryLabelPositions.DOWN_90);
            catAxis.setTickLabelPaint(new Color(0,0,0,0));
            int time=0;
            int frame =5;
            int hour = 900;
            time=5*hour/24;

            for(int timeAxis=0;timeAxis<a2.size();timeAxis=timeAxis+time)
            {
                catAxis.setTickLabelPaint(a2.get(timeAxis), Color.black);
            }
            Font nwfont=new Font("Arial",0,10);
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setTickLabelFont(nwfont);
            catAxis.setTickLabelFont(nwfont);
            File imageFile = new File("LineChart.png");
            int width = 1200;
            int height = 700;
            plot.setOutlinePaint(Color.GRAY);
            plot.setOutlineStroke(new BasicStroke(2.0f));
            try {
                ChartUtilities.saveChartAsPNG(imageFile, chart, width, height);
            } catch (IOException ex) {
                System.err.println(ex);
            }
            ChartPanel panel = new ChartPanel(chart);
            setContentPane(panel);
        }
    }

    private static DefaultCategoryDataset createDataset() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for(int data=0;data<a1.size();data++)
        {
            dataset.addValue(a1.get(data),chartTitle, a2.get(data));
        }
        return dataset;
    }


    @GET
    @Path("/Metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public static void getMetData(
            @NotNull
            @QueryParam("bucket_name") String bucket_name,

            @NotNull
            @QueryParam("metric") String metric,

            @NotNull
            @QueryParam("filter") String filter,

            @NotNull
            @QueryParam("filter_value") String filter_value,

            @NotNull
            @QueryParam("names") String names) throws SQLException, IOException {
        Region region = Region.US_WEST_1;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                "Will need your aws access key",
                "will require your aws secret access key");
        CloudWatchClient cloudWatchClient = CloudWatchClient.builder()
                .region(region)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();


        try {
            String bucket;
            bucket = bucket_name;

            Instant start = Instant.parse("2022-05-02T10:12:35Z");
            Instant endDate = Instant.now();

            Dimension n1 = Dimension.builder()
                    .name("BucketName")
                    .value(bucket)
                    .build();

            chartTitle = metric;

            Dimension n2 = Dimension.builder()
                    .name(filter)
                    .value(filter_value)
                    .build();

            Metric met = Metric.builder()
                    .metricName(metric)
                    .namespace(names)
                    .dimensions(n1,n2)
                    .build();

            MetricStat metStat = MetricStat.builder()
                    .stat("Average")
                    .period(300)
                    .metric(met)
                    .build();

            MetricDataQuery dataQUery = MetricDataQuery.builder()
                    .metricStat(metStat)
                    .returnData(true)
                    .id("foo2")
                    .build();

            List<MetricDataQuery> dq = new ArrayList();
            dq.add(dataQUery);

            //Making the request for fetching the data
            GetMetricDataRequest getMetReq = GetMetricDataRequest.builder()
                    .maxDatapoints(100800)
                    .startTime(start)
                    .endTime(endDate)
                    .metricDataQueries(dq)
                    .scanBy("TimestampAscending")
                    .build();

            //this part fetches the data.
            GetMetricDataResponse response = cloudWatchClient.getMetricData(getMetReq);
            List<MetricDataResult> data = response.metricDataResults();

            try {
                PrintWriter out = new PrintWriter(new FileWriter("y.txt"));
                PrintWriter out1 = new PrintWriter(new FileWriter("x.txt"));
                for (int i = 0; i < data.size(); i++) {
                    MetricDataResult item = (MetricDataResult) data.get(i);

                    List<Double> it = item.values();
                    List<Instant> it1 = item.timestamps();
                    for(int j=0;j<it.size();j++)
                    {
                        out.println(it.get(j));
                        out1.println(it1.get(j));
                        a1.add(it.get(j));
                        a2.add(it1.get(j));
                        System.out.print(it.get(j));
                        System.out.print(" ");
                        System.out.println(it1.get(j));
                    }

                }

                out.close();
                out1.close();

            }
            catch(IOException e1) {
                System.out.println("Error during reading/writing");
            }

            SwingUtilities.invokeLater(() -> {
                LineChartExample example = new LineChartExample("Metrics");
                example.setAlwaysOnTop(true);
                example.pack();
                example.setSize(1200, 700);

                example.setVisible(true);
                int domainAxis = example.getX();

            });
        } catch (CloudWatchException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

}
