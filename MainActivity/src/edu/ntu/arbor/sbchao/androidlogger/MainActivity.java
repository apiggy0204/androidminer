package edu.ntu.arbor.sbchao.androidlogger;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.LoggingService.LocalBinder;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster.DevOpenHelper;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao.Properties;

public class MainActivity extends Activity {
	
	private Button buttonQuit;
	private Button buttonRefresh;
	private Button buttonStopLogging;
	private TextView textInfo;
	private TextView textAppUsage;
	private Spinner spinner1;
	private Spinner spinner2;
	private Button buttonTimeFilter;
	private Button buttonCancelTimeFilter;
	private Button buttonHourlyUsage;
	private Button buttonDailyUsage;
	
	boolean mBound = false;
	LoggingService mService = null;
	
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private MobileLogDao mobileLogDao;
	
	private ServiceConnection mConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;
			Log.i("onServiceConnected", "Service has been bound!");
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.i("onServiceDisconnected", "Service has been unbound");
		}
	};
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        addUiListeners();

        //Start LoggingService
        Intent intent = new Intent(MainActivity.this, LoggingService.class);
        startService(intent);
        
        //Ask user to enable location providers
        showEnableLocationDialog();
        
        //Access local databases
        /*
        DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "MobileLog", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        mobileLogDao = daoSession.getMobileLogDao();
        */ 
        
        /*
        final GraphicalView gv = createIntent();
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.main_relative_layout);
        rl.addView(gv);*/
        


    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LoggingService
        Intent intent = new Intent(MainActivity.this, LoggingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);        						
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Unbound from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void addUiListeners(){
        
        buttonQuit = (Button) findViewById(R.id.button_quit);        
        buttonQuit.setOnClickListener(quitListener);
        buttonRefresh = (Button) findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(refreshListener);
        buttonStopLogging = (Button) findViewById(R.id.button_stop_logging);
        buttonStopLogging.setOnClickListener(stopLoggingListener);
        textInfo = (TextView) findViewById(R.id.text_info);
        
        /*
        buttonHourlyUsage = (Button) findViewById(R.id.button_hourly_app_usage);
        buttonHourlyUsage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this, AppUsageActivity.class);
				Bundle extras = new Bundle();
				extras.putString("mode", AppUsageActivity.MODE_HOURLY);
				i.putExtras(extras);
				startActivity(i);
			}});
        buttonDailyUsage = (Button) findViewById(R.id.button_daily_app_usage);
        buttonDailyUsage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent i = new Intent(MainActivity.this, AppUsageActivity.class);
				Bundle extras = new Bundle();
				extras.putString("mode", AppUsageActivity.MODE_DAILY);
				i.putExtras(extras);
				startActivity(i);
			}});
        */
        
        /*
        textAppUsage = (TextView) findViewById(R.id.text_app_usage);
        spinner1 = (Spinner) findViewById(R.id.spinner1);
        spinner2 = (Spinner) findViewById(R.id.spinner2);
        buttonTimeFilter = (Button) findViewById(R.id.button_time_filter);
        buttonTimeFilter.setOnClickListener(filterListener);
        buttonCancelTimeFilter = (Button) findViewById(R.id.button_cancel_time_filter);
        buttonCancelTimeFilter.setOnClickListener(cancelFilterListener);
        */
        
        /*
        String [] nums = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"};
        ArrayAdapter<String> numList = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, nums);
        spinner1.setAdapter(numList);
        spinner2.setAdapter(numList);
        */
    }
    
    //Display current status of the mobile
    private void displayInfo(){
    	if(mBound == false){
    		Intent intent = new Intent(MainActivity.this, LoggingService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);            
    	} 
    	
    	else {
    		
	    	String toWrite = "Current Process: " + String.valueOf(mService.processCurrentPackage) + "\n"
	    					+ "batLevel: " + String.valueOf(mService.batLevel) + "\n"
	    					+ "batStatus: " + String.valueOf(mService.batStatus) + "\n"
	    					+ "GPSProviderStatus: " + String.valueOf(mService.gpsStatus) + "\n"
	    					+ "networkProviderStatus: " + String.valueOf(mService.networkStatus) + "\n"
	    					+ "3G network status: " + String.valueOf(mService.mobileState) + "\n"
	    					+ "wifi network status: " + String.valueOf(mService.wifiState) + "\n"
	    					+ "isLowMemory: " + String.valueOf(mService.isLowMemory) + "\n";
	    	
	    	if(mService.mLocation != null){
	    		toWrite += "locAccuracy: " + String.valueOf(mService.mLocation.getAccuracy()) + "\n"
						+ "locLatitude: " + String.valueOf(mService.mLocation.getLatitude()) + "\n"
						+ "locLongitude: " + String.valueOf(mService.mLocation.getLongitude()) + "\n"
						+ "locProvider: " + String.valueOf(mService.mLocation.getProvider()) + "\n"
						+ "locSpeed: " + String.valueOf(mService.mLocation.getSpeed()) + "\n";
	    	} else {
	    		toWrite += "location: no location available!" + "\n";
	    	}
	
	    	textInfo.setText(toWrite);
    	}
    }  
    
    private void showEnableLocationDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("網路定位");		
		builder.setMessage("這個程式可以記錄你的位置。你要開啟利用網路或GPS查詢位置的功能嗎?");
		builder.setPositiveButton("好", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {				
				Intent optionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				//optionsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(optionsIntent);
				dialog.dismiss();				
			}
		});
		builder.setNegativeButton("現在不要", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				dialog.dismiss();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
    
    private OnClickListener quitListener = new OnClickListener(){
		@Override
		public void onClick(View view) {
			finish();
		}
    };
    
    private OnClickListener stopLoggingListener = new OnClickListener(){
		@Override
		public void onClick(View view) {
			Log.i("mBound", String.valueOf(mBound));
			if (mBound) {
	            unbindService(mConnection);
	            mBound = false;
	        }	        
	        Intent intent = new Intent(MainActivity.this, LoggingService.class);
	        stopService(intent); 				        
		}
    };
    private OnClickListener refreshListener = new OnClickListener(){
		@Override
		public void onClick(View view) {
			displayInfo();
		}
    };
    private OnClickListener filterListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			
			int startHourOfDay = spinner1.getSelectedItemPosition();
			int endHourOfDay = spinner2.getSelectedItemPosition();
			
			if (startHourOfDay != AdapterView.INVALID_POSITION && endHourOfDay != AdapterView.INVALID_POSITION){
				
				
				
				double cumUseTime = 0;
				/*
		        QueryBuilder<MobileLog> qb = mobileLogDao.queryBuilder(); 
		        if (startHourOfDay < endHourOfDay){
		        	qb.where(Properties.ProcessCurrentPackage.eq("edu.ntu.arbor.sbchao.androidlogger"), qb.and(Properties.HourOfDay.ge(startHourOfDay), Properties.HourOfDay.le(endHourOfDay)));
		        }
		        else {
		        	qb.where(Properties.ProcessCurrentPackage.eq("edu.ntu.arbor.sbchao.androidlogger"), qb.or(Properties.HourOfDay.ge(startHourOfDay), Properties.HourOfDay.le(endHourOfDay)));
		        }
		        List<MobileLog> loggers = qb.list();
		        for(MobileLog mobileLog : loggers){
		        	cumUseTime += mobileLog.getRecordFreq();
		        }*/
		        
		        textAppUsage.setText("�A�w�ϥγo��app: " + String.valueOf(cumUseTime/60000) + "����");
			}
		}
	};
	
	private OnClickListener cancelFilterListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			showAppUsage();
		}		
	}; 
	
	private void showAppUsage(){
        double cumUseTime = 0;
        QueryBuilder<MobileLog> qb = mobileLogDao.queryBuilder(); 
        List<MobileLog> loggers = qb.where(Properties.ProcessCurrentPackage.eq("edu.ntu.arbor.sbchao.androidlogger")).list();
        for(MobileLog mobileLog : loggers){
        	cumUseTime += mobileLog.getRecordFreq();
        }
        textAppUsage.setText("�A�w�ϥγo��app: " + String.valueOf(cumUseTime/60000) + "����");
	}
	
	
}
