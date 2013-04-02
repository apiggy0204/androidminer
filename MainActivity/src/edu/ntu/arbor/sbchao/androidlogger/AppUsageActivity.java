package edu.ntu.arbor.sbchao.androidlogger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DataManager;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster.DevOpenHelper;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao.Properties;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao;

public class AppUsageActivity extends ListActivity {
	
	public static String PREFS_CHART_MODE = "chart_mode";
	public static String MODE_DAILY = "daily";
	public static String MODE_HOURLY = "hourly";
	private static String APP_USAGE_PATH = "http://140.112.42.22:7380/netdbmobileminer_test/getAppUsage.php";
	//private static String APP_USAGE_PATH = "http://10.0.2.2/netdbmobileminer_test/getAppUsage.php";
	
	private String mMode;
	
	private ArrayList<Map<String, String>> mListItemMaps = new ArrayList<Map<String, String>>();  
	private ArrayList<String> mPkgNameList = new ArrayList<String>();
	private double maxMinute = 0;
	XYMultipleSeriesDataset dataset;
	XYMultipleSeriesRenderer renderer;
	
	//Local database
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private MobileLogDao mobileLogDao;
	private NetworkLogDao networkLogDao;
	
	private ProgressDialog mDialog;

	
	protected void onCreate(Bundle savedInstanceState) {		
        super.onCreate(savedInstanceState);        

        //Read mode
        if(getIntent().getExtras() != null){
        	mMode = getIntent().getExtras().getString(PREFS_CHART_MODE);
        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        	Editor editor = settings.edit();
        	editor.putString(PREFS_CHART_MODE, mMode);
        	editor.commit();
        } else {
        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        	mMode = settings.getString(PREFS_CHART_MODE, MODE_DAILY);
        }
        
        //Load app list and create ui
        new LoadAppListTask().execute();
	}
	
	@Override
	public void onResume(){
		super.onResume();
        //Set up local databases
		File dbfile = new File(Environment.getExternalStorageDirectory().getPath(), "AndroidLogger/netdb.db");
		db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);		
		MobileLogDao.createTable(db, true);
        NetworkLogDao.createTable(db, true);
        
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        mobileLogDao = daoSession.getMobileLogDao();
        networkLogDao = daoSession.getNetworkLogDao(); 
	}
	
	@Override
	public void onPause(){
		super.onPause();
		db.close();		
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {  
        super.onListItemClick(l, v, position, id);
        
        //TODO
        new GetAppUsageTask().execute(mPkgNameList.get(position));
        /*
        XYMultipleSeriesDataset dataset = getBarDataset(mPkgNameList.get(position));
        XYMultipleSeriesRenderer renderer = getBarRenderer();  
        Intent intent = ChartFactory.getBarChartIntent(this, dataset, renderer, Type.STACKED);  
        startActivity(intent);
        */    
    }  
	
	/*
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate if the process is
		// killed and restarted.
		Log.i("saving...", mMode);
		savedInstanceState.putString("mode", mMode);
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.		
		mMode = savedInstanceState.getString("mode");
	}
	*/
	
	//Loading the app list when this activity is first activated
	private class LoadAppListTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected void onPreExecute(){
			mDialog = new ProgressDialog(AppUsageActivity.this);
	        mDialog.setMessage("Loading the app list...");
	        mDialog.setCancelable(false);
	        mDialog.show();
		}
		
		@Override
		protected Void doInBackground(Void... params) {			
	        for (ApplicationInfo app : getPackageManager().getInstalledApplications(0)) {
	        	HashMap<String, String> map = new HashMap<String, String>();  
	            map.put("name", app.loadLabel(getPackageManager()).toString());  
	            map.put("desc", app.packageName);  
	            mListItemMaps.add(map);  
	            mPkgNameList.add(app.packageName);
	        }
			return null;
		}
		
		@Override
    	protected void onPostExecute(Void result){
	        SimpleAdapter adapter = new SimpleAdapter(AppUsageActivity.this, mListItemMaps,  
	                // SDK 库中提供的一个包含两个 TextView 的layout  
	        android.R.layout.simple_list_item_2,   
	                new String[] { "name", "desc" }, // maps 中的两个 key  
	                new int[] { android.R.id.text1, android.R.id.text2 }// 两个TextView的 id   
	        );  
	        AppUsageActivity.this.setListAdapter(adapter);
	        
			if(mDialog != null){
				mDialog.dismiss();
				mDialog = null;
			}
		}
	};
	

	private class GetAppUsageTask extends AsyncTask<String, Void, Void>{
		
		@Override
		protected void onPreExecute(){
			mDialog = new ProgressDialog(AppUsageActivity.this);
	        mDialog.setMessage("Reading from the database...");
	        mDialog.setCancelable(false);
	        mDialog.show();
		}
		
		@Override
		protected Void doInBackground(String... params) {
			for(String param : params){
				dataset = getBarDataset(param);
				renderer = getBarRenderer();  
				Intent intent = ChartFactory.getBarChartIntent(AppUsageActivity.this, dataset, renderer, Type.STACKED);  
		        startActivity(intent);
			}
			return null;
		}
		
		@Override
    	protected void onPostExecute(Void result){	
			if(mDialog != null){
				mDialog.dismiss();
				mDialog = null;
			}
		}
	};
	
	/** 
     * XYMultipleSeriesDataset 类型的对象，用于提供图表需要表示的数据集， 
     * 这里我们用 getBarDemoDataset 来得到它。 
     */  
    private XYMultipleSeriesDataset getBarDataset(String packageName) {  
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();  
                 
        CategorySeries series = new CategorySeries(packageName);
        
        int max = 0;        
        if(mMode.equals(MODE_DAILY)){        	
        	for (int i = 0; i <= 6; i++) {  
            	double minute = getAppUsage(packageName, i); //TODO
            	if(minute > maxMinute) maxMinute = minute;
            	series.add(minute);
            } 
        }
        else if(mMode.equals(MODE_HOURLY)){         	
            for (int i = 1; i < 24; i++) {  
            	double minute = getAppUsage(packageName, i); //TODO
            	if(minute > maxMinute) maxMinute = minute;
            	series.add(minute);
            } 
        }
        /*
        for (int i = 0; i <= max; i++) {  
        	double minute = getAppUsage(packageName, i); //TODO
        	if(minute > maxMinute) maxMinute = minute;
        	series.add(minute);
        } */
        dataset.addSeries(series.toXYSeries());  
          
        return dataset;  
    }  
    
    /** 
     * XYMultipleSeriesRenderer 类型的对象，用于提供图表展现时的一些样式， 
     * 这里我们用 getBarDemoRenderer 方法来得到它。 
     * getBarDemoRenderer 方法构建了一个 XYMultipleSeriesRenderer 用来设置2个系列各自的颜色 
     */  
    public XYMultipleSeriesRenderer getBarRenderer() {  
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();  
        SimpleSeriesRenderer r = new SimpleSeriesRenderer();  
        r.setColor(Color.BLUE);  
        renderer.addSeriesRenderer(r);    
        setChartSettings(renderer);  
        return renderer;  
    }  
    /** 
     * setChartSettings 方法设置了下坐标轴样式。 
     */  
    private void setChartSettings(XYMultipleSeriesRenderer renderer) {  
        if(mMode.equals(MODE_DAILY)){ 
        	renderer.setChartTitle("Daily App Usage");
        	renderer.setXTitle("Day");
        	renderer.setXAxisMin(-0.5);
        	renderer.setXAxisMax(8.5);
        	
        	renderer.addXTextLabel(1, "Sun");
            renderer.addXTextLabel(2, "Mon");
            renderer.addXTextLabel(3, "Tue");
            renderer.addXTextLabel(4, "Wed");
            renderer.addXTextLabel(5, "Thu");
            renderer.addXTextLabel(6, "Fri");
            renderer.addXTextLabel(7, "Sat");
        }
        else if(mMode.equals(MODE_HOURLY)){
        		renderer.setChartTitle("Hourly App Usage");
        		renderer.setXTitle("Hour");
        		renderer.setXAxisMin(-0.5);  
                renderer.setXAxisMax(23.5);  
                
                for(int i=0; i<=24; i++){
                	renderer.addXTextLabel(i, String.valueOf(i));
                }
    	}
        //renderer.setXTitle("Day");  
        renderer.setYTitle("Minute");  
        //renderer.setXAxisMin(0.5);  
        //renderer.setXAxisMax(6.5);  
        renderer.setYAxisMin(0);  
        //renderer.setYAxisMax(24*60);
        renderer.setYAxisMax(1.5 * maxMinute); // Adjust according to max minute
        
        renderer.setBackgroundColor(Color.WHITE);
        renderer.setMarginsColor(Color.WHITE);
        
        renderer.setLabelsColor(Color.DKGRAY);
        renderer.setAxesColor(Color.DKGRAY);
        renderer.setGridColor(Color.DKGRAY);
        
    }  
	
    //param is hourOfDay or dayOfWeek
    protected double getAppUsage(String pkgName, int param){
    	double cumMinutes = 0;
    	QueryBuilder<MobileLog> qb = mobileLogDao.queryBuilder();
    	if(mMode.equals(MODE_DAILY)){
    		qb.where(qb.and(Properties.ProcessCurrentPackage.eq(pkgName), Properties.DayOfWeek.eq(param), Properties.IsUsing.eq(true)));
    	}
    	else if(mMode.equals(MODE_HOURLY)){
    		qb.where(qb.and(Properties.ProcessCurrentPackage.eq(pkgName), Properties.HourOfDay.eq(param), Properties.IsUsing.eq(true)));
    	}
        	        
        
        List<MobileLog> logs = qb.list();
        HashSet<String> dateSet = new HashSet<String>(); 
        
        for(MobileLog log : logs){        	
        	cumMinutes += log.getRecordFreq()/60000.0;
        	SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        	String dateString = df.format(log.getTime());
        	dateSet.add(dateString);        	
        }	        	      
        Log.v("DateString", "size: " + String.valueOf(dateSet.size()));
        //return cumMinutes;
    	return (dateSet.size() == 0 ? 0 : cumMinutes/dateSet.size());
    }
    
    protected String getAppUsageViaNetwork(String pkgName, int param) {
    	
    	ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
    	String ret = null;
    	
    	TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);;
    	params.add(new BasicNameValuePair(DataManager.DEVICE_ID, "352668044581922")); //TODO
    	params.add(new BasicNameValuePair(DataManager.PROCESS_CURRENT_PACKAGE, pkgName));
    	if(mMode.equals(MODE_DAILY)){
    		params.add(new BasicNameValuePair(DataManager.DAY_OF_WEEK, String.valueOf(param)));
    	}
    	else if(mMode.equals(MODE_HOURLY)){
    		params.add(new BasicNameValuePair(DataManager.HOUR_OF_DAY, String.valueOf(param)));
    	}
    	
    	HttpPost httpPost = new HttpPost(APP_USAGE_PATH);
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse resp = new DefaultHttpClient().execute(httpPost);
			if (resp.getStatusLine().getStatusCode() == 200){	
				ret = EntityUtils.toString(resp.getEntity());
				Log.v("getAppUsage", ret);
			} else {
				Log.e("getAppUsage", "fail!");
			}			
		} catch (ClientProtocolException e){
			Log.e("getAppUsage", e.getMessage());
			
		} catch (IOException e){
			Log.e("getAppUsage", e.getMessage());
		}
		return ret;
	}

}
