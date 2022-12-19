package com.metrics.rest.azure;

import com.azure.core.annotation.Get;
import com.azure.core.management.profile.AzureProfile;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.monitor.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.swing.*;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
@Path("/Azure")
public class DesCreateTest {

    static List<Double> a1 = new ArrayList<>();
    static List<DateTime> a2 = new ArrayList<>();
    static String tit;
    static int hour;
    static int frame;
    static String ylab;
    public static class LineChartExample extends JFrame {

        private static final long serialVersionUID = 1L;

        public LineChartExample(String title) {
            super(title);
            // Create dataset
            DefaultCategoryDataset dataset = createDataset();
            // Create chart
            JFreeChart chart = ChartFactory.createLineChart(
                    tit, // Chart title
                    "Date and Time", // X-Axis Label
                    ylab+" Value", // Y-Axis Label
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
            int y1=0;
            if(frame == 1)
            {
                y1 = hour;
            }
            else if(frame == 5)
            {
                y1=5*hour/24;
            }
            else if(frame == 15)
            {
                y1 = 5*hour/24;
                y1=y1/3;
            }
            else if(frame == 30)
            {
                y1 = 5*hour/24;
                y1=y1/6;
            }
            else if(frame == 60)
            {
                y1 = 5*hour/24;
                y1=y1/6;
            }
            for(int i=0;i<a2.size();i=i+y1)
            {
                //String cat_Name = (String) plot.getCategories().get(i-1);
                catAxis.setTickLabelPaint(a2.get(i), Color.black);
            }
            Font nwfont=new Font("Arial",0,10);
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setTickLabelFont(nwfont);
            catAxis.setTickLabelFont(nwfont);
            File imageFile = new File("LineChart.png");
            int width = 1500;
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
            String series1 = tit;
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for(int i=0;i<a1.size();i++)
            {
                dataset.addValue(a1.get(i),series1, a2.get(i));
            }
            return dataset;
        }


        @GET
        @Path("/Metrics")
        @Produces(MediaType.APPLICATION_JSON)
        public  void testCall(@NotNull
                              @QueryParam("metric_name") String metric_name,
                              @NotNull
                              @QueryParam("client") String client,
                              @NotNull
                              @QueryParam("Subscription") String Subscription,
                              @NotNull
                              @QueryParam("password") String password,
                              @NotNull
                              @QueryParam("days") int d1,
                              @NotNull
                              @QueryParam("eco") int d2,
                              @NotNull
                              @QueryParam("Arg") String str,
                              @NotNull
                              @QueryParam("resource") String res) throws SQLException, IOException {
            String tenant = "f636e1c4-18a5-4f8a-9e41-e247bae1387d";
            String clientId=client;
            String passwd=password;
            String sub=Subscription;
            AzureProfile profile = new AzureProfile(tenant, sub, com.azure.core.management.AzureEnvironment.AZURE);
            ApplicationTokenCredentials crede = new ApplicationTokenCredentials(clientId, tenant, passwd, AzureEnvironment.AZURE);
            Azure azure = Azure.authenticate(crede).withSubscription(sub);
            String Id = "/subscriptions/" + sub + "/resourceGroups/" + res + "/providers/Microsoft.Storage/storageAccounts/" +res;
            DateTime record = DateTime.now();

            String metr = metric_name;
            tit = metr;

            var days = d1;
            days = days*24;

            int eco = d2;
            hour = days;
            frame = eco;

            String arg = str;
            ylab=arg;
            for (MetricDefinition metricDefinition : azure.metricDefinitions().listByResource(Id)) {
                if (metricDefinition.name().localizedValue().equalsIgnoreCase(metr)) {
                    MetricCollection metricCollection = metricDefinition.defineQuery()
                            .startingFrom(record.minusHours(days))
                            .endsBefore(record)
                            .withAggregation(arg)
                            .withInterval(Period.minutes(eco))
                            .execute();


                    for (Metric metric : metricCollection.metrics()) {

                        for (TimeSeriesElement timeElement : metric.timeseries()) {


                            System.out.println("\t\tData: ");
                            PrintWriter out = new PrintWriter(new FileWriter("y.txt"));
                            PrintWriter out1 = new PrintWriter(new FileWriter("x.txt"));
                            for (MetricValue data : timeElement.data()) {

                                if(arg.equals("Average"))
                                {
                                    a1.add(data.average());
                                    out.println(data.average());
                                }
                                else if (arg.equals("Total"))
                                {
                                    a1.add(data.total());
                                    out.println(data.total());
                                }
                                else if (arg.equals("Maximum"))
                                {
                                    a1.add(data.maximum());
                                    out.println(data.maximum());
                                }
                                else if (arg.equals("Minimum"))
                                {
                                    a1.add(data.minimum());
                                    out.println(data.minimum());
                                }
                                else if (arg.equals("Count"))
                                {
                                    a1.add(data.count());
                                    out.println(data.count());
                                }
                                a2.add(data.timeStamp());
                                out.println();
                                out1.println(data.timeStamp());
                            }
                            out.close();
                            out1.close();
                        }
                    }
                    break;
                }
            }
            SwingUtilities.invokeLater(() -> {
                LineChartExample example = new LineChartExample("Metrics");
                example.setAlwaysOnTop(true);
                example.pack();
                example.setSize(1800, 1000);

               example.setVisible(true);
                int domainAxis = example.getX();

            });
        }


}


