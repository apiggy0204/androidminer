package edu.ntu.arbor.sbchao.androidlogger;

import android.app.Activity;


import java.util.ArrayList;
import java.util.List;
 
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer.Orientation;
 
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.app.Activity;
import android.os.Bundle;
import android.widget.RelativeLayout;
 
 
public class BarChartTestActivity extends Activity {
 
     
     
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
 
        final GraphicalView gv = createIntent();

        //RelativeLayout rl = (RelativeLayout) findViewById(R.id.graph);
        //rl.addView(gv);

    }
     
    
    public GraphicalView createIntent() {
        String[] titles = new String[] { "Amount" };
        List<double[]> values = new ArrayList<double[]>();
          
        values.add(new double[] { 5230, 7300, 9240,10230, 11300, 10040,14230, 12300, 14240, 11300, 10040,14240});
 
        int[] colors = new int[] { Color.parseColor("#77c4d3")};
        XYMultipleSeriesRenderer renderer = buildBarRenderer(colors);
        renderer.setOrientation(Orientation.HORIZONTAL);
        setChartSettings(renderer, "Production Details", "Products", "Production", 0.5,
            12.5, 0, 24000, Color.BLACK, Color.BLACK);
        renderer.setXLabels(1);
        renderer.setYLabels(10);
        renderer.addXTextLabel(1, "Jan");
        renderer.addXTextLabel(2, "Feb");
        renderer.addXTextLabel(3, "Mar");
        renderer.addXTextLabel(4, "Apr");
        renderer.addXTextLabel(5, "May");
        renderer.addXTextLabel(6, "Jun");
        renderer.addXTextLabel(7, "Jul");
        renderer.addXTextLabel(8, "Aug");
        renderer.addXTextLabel(9, "Sep");
        renderer.addXTextLabel(10, "Oct");
        renderer.addXTextLabel(11, "Nov");
        renderer.addXTextLabel(12, "Dec");
        int length = renderer.getSeriesRendererCount();
        for (int i = 0; i < length; i++) {
          SimpleSeriesRenderer seriesRenderer = renderer.getSeriesRendererAt(i);
          seriesRenderer.setDisplayChartValues(true);
        }
 
 
        final GraphicalView grfv = ChartFactory.getBarChartView(BarChartTestActivity.this, buildBarDataset(titles, values), renderer,Type.DEFAULT);
   
         
        return grfv;
      }
      protected XYMultipleSeriesRenderer buildBarRenderer(int[] colors) {
            XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
            renderer.setAxisTitleTextSize(16);
            renderer.setChartTitleTextSize(20);
            renderer.setLabelsTextSize(15);
            renderer.setLegendTextSize(15);
            renderer.setBarSpacing(1);
             
            renderer.setMarginsColor(Color.parseColor("#EEEDED"));
            renderer.setXLabelsColor(Color.BLACK);
            renderer.setYLabelsColor(0,Color.BLACK);
             
            renderer.setApplyBackgroundColor(true);
            renderer.setBackgroundColor(Color.parseColor("#FBFBFC"));
             
            int length = colors.length;
            for (int i = 0; i < length; i++) {
              SimpleSeriesRenderer r = new SimpleSeriesRenderer();
              r.setColor(colors[i]);
              //r.setChartvalueAngle(-90);
              r.setChartValuesSpacing(15);
              renderer.addSeriesRenderer(r);
            }
            return renderer;
          }
      protected XYMultipleSeriesDataset buildBarDataset(String[] titles, List<double[]> values) {
            XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
            int length = titles.length;
            for (int i = 0; i < length; i++) {
              CategorySeries series = new CategorySeries(titles[i]);
              double[] v = values.get(i);
              int seriesLength = v.length;
              for (int k = 0; k < seriesLength; k++) {
                series.add(v[k]);
              }
              dataset.addSeries(series.toXYSeries());
            }
            return dataset;
          }
      protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle,
              String yTitle, double xMin, double xMax, double yMin, double yMax, int axesColor,
              int labelsColor) {
            renderer.setChartTitle(title);
            renderer.setYLabelsAlign(Align.RIGHT);
            renderer.setXTitle(xTitle);
            renderer.setYTitle(yTitle);
            renderer.setXAxisMin(xMin);
            renderer.setXAxisMax(xMax);
            renderer.setYAxisMin(yMin);
            renderer.setYAxisMax(yMax);
            renderer.setMargins(new int[] { 10, 65, 10, 15 });
            renderer.setAxesColor(axesColor);
            renderer.setLabelsColor(labelsColor);
          }
     
}