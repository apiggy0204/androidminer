package edu.ntu.arbor.sbchao.androidlogger;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.LoggingService.LocalBinder;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster.DevOpenHelper;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao.Properties;

public class MainActivity extends Activity {
	
	private static final int UPDATE_INFO = 0x0000;
	private static final int ENABLE_LOCATION_PROVIDER = 0x0001;
	private static final int UPDATE_NETWORK = 0x0002;
	
	private Button buttonQuit;
	private Button buttonRefresh;
	private Button buttonStopLogging;
	//private TextView textInfo;
	//private TextView textAppUsage;
	//private Spinner spinner1;
	//private Spinner spinner2;
	/*
	private Button buttonTimeFilter;
	private Button buttonCancelTimeFilter;
	private Button buttonHourlyUsage;
	private Button buttonDailyUsage;
	*/
	
	private TextView text3GTodayTx;
	private TextView text3GTodayRx;
	private TextView text3GTodayTotal;
	private TextView textWifiTodayTx;
	private TextView textWifiTodayRx;
	private TextView textWifiTodayTotal;
	private TextView text3GWeekTx;
	private TextView text3GWeekRx;
	private TextView text3GWeekTotal;
	private TextView text3GMonthTx;
	private TextView text3GMonthRx;
	private TextView text3GMonthTotal;
	private TextView textWifiWeekTx;
	private TextView textWifiWeekRx;
	private TextView textWifiWeekTotal;
	private TextView textWifiMonthTx;
	private TextView textWifiMonthRx;
	private TextView textWifiMonthTotal;
	
	
	
	private boolean mBound = false;	
	private LoggingService mService = null;
	private ThreadHandler mHandler = new ThreadHandler();
	
	//Local database
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private MobileLogDao mobileLogDao;
	private NetworkLogDao networkLogDao;
	
	private static int UPDATE_FREQUENCY = 10000;
	
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
	
	
    private final class ThreadHandler extends Handler {
	    public ThreadHandler() {
            super();
        }
		@Override
		public void handleMessage(Message msg) {						
			switch(msg.what){
				case UPDATE_INFO:
					if(mBound){
						updateStatus();						
					}
					mHandler.postDelayed(updateInfoThread, 1000);
					break;
				case UPDATE_NETWORK:
					if(mBound){						
						//updateNetworkTraffic();
						new UpdateNetworkTrafficTask().execute();
					}
					mHandler.postDelayed(updateNetworkThread, UPDATE_FREQUENCY);
					break;
				case ENABLE_LOCATION_PROVIDER:
					if(mBound && (!mService.isGPSProviderEnabled && !mService.isMobileConnected)){
						showEnableLocationDialog();
					} else {
						mHandler.postDelayed(enableLocationProviderThread, 1000);
					}
					break;				
				default:
					assert(false);
					break;
			}
			super.handleMessage(msg);
	    }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         
        addUiListeners();

        //Start LoggingService
        Intent intent = new Intent(MainActivity.this, LoggingService.class);
        startService(intent);
                        
        //Ask user to enable location providers
        //mHandler.postDelayed(enableLocationProviderThread, 1000);
        
        //Set up local databases
        DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "MobileLog", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        mobileLogDao = daoSession.getMobileLogDao();
        networkLogDao = daoSession.getNetworkLogDao();
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LoggingService
        Intent intent = new Intent(MainActivity.this, LoggingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);  
        
        //Update info on UI
        mHandler.postDelayed(updateInfoThread, 0);
        mHandler.postDelayed(updateNetworkThread, 0); 
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
        //getMenuInflater().inflate(R.menu.activity_main, menu);
    	menu.add(0, Menu.FIRST, 0, "Settings...");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case Menu.FIRST:
        	Log.i("onOptionsItemSelected", "selected!");
            Intent intent = new Intent();            
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private Runnable updateInfoThread = new Runnable(){
		@Override
		public void run() {
			try {				
				Message msg = new Message();
				msg.what = UPDATE_INFO;
				mHandler.sendMessage(msg);
			}  
			catch(Exception e) {
				e.printStackTrace();
				Log.e("UpdateInfoThread", e.getMessage());
			}
		}
    };
    
    private Runnable updateNetworkThread = new Runnable(){
		@Override
		public void run() {
			try {				
				Message msg = new Message();
				msg.what = UPDATE_NETWORK;
				mHandler.sendMessage(msg);
			}  
			catch(Exception e) {
				e.printStackTrace();
				Log.e("UpdateInfoThread", e.getMessage());
			}
		}
    };
    
    private Runnable enableLocationProviderThread = new Runnable(){
		@Override
		public void run() {
			try {
				Message msg = new Message();
				msg.what = ENABLE_LOCATION_PROVIDER;
				mHandler.sendMessage(msg);	
			}
			catch(Exception e) {
				e.printStackTrace();
				Log.e("UpdateInfoThread", e.getMessage());
			}
		}
    };
    
	private TextView textApp;
	private TextView textApp2;
	private TextView textApp3;
	private TextView textBatLevel;
	private TextView textBatStatus;
	private TextView textGPSProvider;
	private TextView textNetworkProvider;
	private TextView text3G;
	private TextView textWifi;
	private TextView textIsLowMemory;
	private TextView textLocAcc;
	private TextView textLat;
	private TextView textLon;
	private TextView textProvider;
	private TextView textSpeed;
	private Button buttonDailyAppUsage;
	private Button buttonHourlyAppUsage;


    private void addUiListeners(){
        
        buttonQuit = (Button) findViewById(R.id.button_quit);        
        buttonQuit.setOnClickListener(quitListener);
        buttonRefresh = (Button) findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(refreshListener);
        buttonStopLogging = (Button) findViewById(R.id.button_stop_logging);
        buttonStopLogging.setOnClickListener(stopLoggingListener);
        //textInfo = (TextView) findViewById(R.id.text_info);
        
        textApp = (TextView) findViewById(R.id.text_app);
        textApp2 = (TextView) findViewById(R.id.text_app2);
        textApp3 = (TextView) findViewById(R.id.text_app3);
        textBatLevel = (TextView) findViewById(R.id.text_bat_level);
        textBatStatus = (TextView) findViewById(R.id.text_bat_status);
        textGPSProvider = (TextView) findViewById(R.id.text_gps_provider);
        textNetworkProvider = (TextView) findViewById(R.id.text_network_provider);
        text3G = (TextView) findViewById(R.id.text_3G);
        textWifi = (TextView) findViewById(R.id.text_wifi);
        textIsLowMemory = (TextView) findViewById(R.id.text_is_low_memory);
        textLocAcc = (TextView) findViewById(R.id.text_loc_acc);
        textLat = (TextView) findViewById(R.id.text_lat);
        textLon = (TextView) findViewById(R.id.text_lon);
        textProvider = (TextView) findViewById(R.id.text_loc_provider);
        textSpeed = (TextView) findViewById(R.id.text_speed);
           
        text3GTodayTx = (TextView) findViewById(R.id.text_3G_today_tx);
        text3GTodayRx = (TextView) findViewById(R.id.text_3G_today_rx);
        text3GTodayTotal = (TextView) findViewById(R.id.text_3G_today_total);
        
        text3GWeekTx = (TextView) findViewById(R.id.text_3G_week_tx);
        text3GWeekRx = (TextView) findViewById(R.id.text_3G_week_rx);
        text3GWeekTotal = (TextView) findViewById(R.id.text_3G_week_total);
        
        text3GMonthTx = (TextView) findViewById(R.id.text_3G_month_tx);
        text3GMonthRx = (TextView) findViewById(R.id.text_3G_month_rx);
        text3GMonthTotal = (TextView) findViewById(R.id.text_3G_month_total);
        
        textWifiTodayTx = (TextView) findViewById(R.id.text_wifi_today_tx);
        textWifiTodayRx = (TextView) findViewById(R.id.text_wifi_today_rx);
        textWifiTodayTotal = (TextView) findViewById(R.id.text_wifi_today_total);
        
        textWifiWeekTx = (TextView) findViewById(R.id.text_wifi_week_tx);
        textWifiWeekRx = (TextView) findViewById(R.id.text_wifi_week_rx);
        textWifiWeekTotal = (TextView) findViewById(R.id.text_wifi_week_total);  

        textWifiMonthTx = (TextView) findViewById(R.id.text_wifi_month_tx);
        textWifiMonthRx = (TextView) findViewById(R.id.text_wifi_month_rx);
        textWifiMonthTotal = (TextView) findViewById(R.id.text_wifi_month_total); 
        
        buttonDailyAppUsage = (Button) findViewById(R.id.button_app_daily_usage);
        buttonDailyAppUsage.setOnClickListener(dailyAppUsageListener);
        buttonHourlyAppUsage = (Button) findViewById(R.id.button_app_hour_usage);
        buttonHourlyAppUsage.setOnClickListener(hourlyAppUsageListener);
       
    }
    
    //Display current status of the mobile
    private void updateStatus(){
    	
    	if(mBound) {
            textApp.setText(String.valueOf(mService.processCurrentPackage));
            textApp2.setText(String.valueOf(mService.process2ndPackage));
            textApp3.setText(String.valueOf(mService.process3rdPackage));
            textBatLevel.setText(String.valueOf(mService.batLevel));
            //textBatStatus.setText(String.valueOf(mService.batStatus));
            switch(mService.batStatus){
            	case 1:
            		textBatStatus.setText("UNKNOWN");
            		break;
            	case 2:
            		textBatStatus.setText("CHARGING");
            		break;
            	case 3:
            		textBatStatus.setText("DISCHARGING");
            		break;
            	case 4:
            		textBatStatus.setText("NOT CHARGING");
            		break;
            	case 5:
            		textBatStatus.setText("FULL");
            		break;
            }
            //textGPSProvider.setText(String.valueOf(mService.gpsStatus));
            //textNetworkProvider.setText(String.valueOf(mService.networkStatus));
            switch(mService.gpsStatus){
            	case 0:
            		textGPSProvider.setText("OUT_OF_SERVICE");
            		break;
            	case 1:
            		textGPSProvider.setText("TEMPORARILY UNAVAILABLE");
            		break;
            	case 2:
            		textGPSProvider.setText("AVAILABLE");
            		break;
            }
            switch(mService.networkStatus){
	        	case 0:
	        		textNetworkProvider.setText("OUT_OF_SERVICE");
	        		break;
	        	case 1:
	        		textNetworkProvider.setText("TEMPORARILY UNAVAILABLE");
	        		break;
	        	case 2:
	        		textNetworkProvider.setText("AVAILABLE");
	        		break;
            }
            text3G.setText(String.valueOf(mService.mobileState));
            textWifi.setText(String.valueOf(mService.wifiState));
            textIsLowMemory.setText(String.valueOf(mService.isLowMemory));
            
            if(mService.mLocation != null){
	            textLocAcc.setText(String.valueOf(mService.mLocation.getAccuracy()) + " m");
	            textLat.setText(String.valueOf(mService.mLocation.getLatitude()));
	            textLon.setText(String.valueOf(mService.mLocation.getLongitude()));
	            textProvider.setText(String.valueOf(mService.mLocation.getProvider()));
	            textSpeed.setText(String.valueOf(mService.mLocation.getSpeed()) + " m/s");	            
            }
            
    		/*
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
	
	    	textInfo.setText(toWrite);*/    		    		
    	}
    }
	
    private class UpdateNetworkTrafficTask extends AsyncTask<Void, Void, Void>{
    	
    	private long byte3GTodayTx;
    	private long byte3GTodayRx;
    	private long byteWifiTodayTx;
    	private long byteWifiTodayRx;
    	private long byte3GWeekTx;
    	private long byte3GWeekRx;
    	private long byte3GMonthTx;
    	private long byte3GMonthRx;
    	private long byteWifiWeekTx;
    	private long byteWifiWeekRx;
    	private long byteWifiMonthTx;
    	private long byteWifiMonthRx;
    	
    	@Override
    	protected void onPostExecute(Void result){
	        text3GTodayRx.setText(toBytes(byte3GTodayRx));
	        text3GTodayTx.setText(toBytes(byte3GTodayTx));
	        text3GTodayTotal.setText(toBytes(byte3GTodayRx+byte3GTodayTx));
	        textWifiTodayRx.setText(toBytes(byteWifiTodayRx));
	        textWifiTodayTx.setText(toBytes(byteWifiTodayTx));
	        textWifiTodayTotal.setText(toBytes(byteWifiTodayRx+byteWifiTodayTx));
	        text3GWeekRx.setText(toBytes(byte3GWeekRx));
	        text3GWeekTx.setText(toBytes(byte3GWeekTx));
	        text3GWeekTotal.setText(toBytes(byte3GWeekRx+byte3GWeekTx));
	        textWifiWeekRx.setText(toBytes(byteWifiWeekRx));
	        textWifiWeekTx.setText(toBytes(byteWifiWeekTx));
	        textWifiWeekTotal.setText(toBytes(byteWifiWeekRx+byteWifiWeekTx));
	        text3GMonthRx.setText(toBytes(byte3GMonthRx));
	        text3GMonthTx.setText(toBytes(byte3GMonthTx));
	        text3GMonthTotal.setText(toBytes(byte3GMonthRx+byte3GMonthTx));
	        textWifiMonthRx.setText(toBytes(byteWifiMonthRx));
	        textWifiMonthTx.setText(toBytes(byteWifiMonthTx));
	        textWifiMonthTotal.setText(toBytes(byteWifiMonthRx+byteWifiMonthTx));
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			if(mBound){		
				
				Log.i("doInBackgound", "start!");
				//try {
			        QueryBuilder<NetworkLog> qb = networkLogDao.queryBuilder();
			    
			        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getTodayStartDate())));	        
			        long cumRxBytes = 0;
			        long cumTxBytes = 0;
			        List<NetworkLog> logs = qb.list();
			        
			        for(NetworkLog log : logs){        	
			        	byte3GTodayRx += log.getReceivedByte();
			        	byte3GTodayTx += log.getTransmittedByte();
			        }	        	        

					//Thread.sleep(100);
					
			        //WiFi network traffic
			        qb = networkLogDao.queryBuilder();
			        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getTodayStartDate())));
			        logs = qb.list();
			        for(NetworkLog log : logs){	
			        	byteWifiTodayRx += log.getReceivedByte();
			        	byteWifiTodayTx += log.getTransmittedByte();
			        }	        	        

			        //Thread.sleep(100);
			        
			        qb = networkLogDao.queryBuilder();
			        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getWeekStartDate())));	        
			        cumRxBytes = 0;
			        cumTxBytes = 0;
			        logs = qb.list();
			        Log.d("updateNetworkTraffic", "log list size:" + logs.size());
			        for(NetworkLog log : logs){        	
			        	byte3GWeekRx += log.getReceivedByte();
			        	byte3GWeekTx += log.getTransmittedByte();
			        }	        	        

			        //Thread.sleep(100);
			        
			        //WiFi network traffic
			        qb = networkLogDao.queryBuilder();
			        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getWeekStartDate())));
			        logs = qb.list();
			        for(NetworkLog log : logs){
			        	byteWifiWeekRx += log.getReceivedByte();
			        	byteWifiWeekTx += log.getTransmittedByte();
			        }	        	        

			        //Thread.sleep(100);
			        
			        qb = networkLogDao.queryBuilder();
			        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getMonthStartDate())));	        
			        cumRxBytes = 0;
			        cumTxBytes = 0;
			        logs = qb.list();
			        
			        for(NetworkLog log : logs){	        	
			        	byte3GMonthRx += log.getReceivedByte();
			        	byte3GMonthTx += log.getTransmittedByte();
			        }	        	        

			        //WiFi network traffic
			        qb = networkLogDao.queryBuilder();
			        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getMonthStartDate())));
			        logs = qb.list();
			        for(NetworkLog log : logs){	
			        	byteWifiMonthRx += log.getReceivedByte();
			        	byteWifiMonthTx += log.getTransmittedByte();
			        }	        	        

			        //Thread.sleep(100);
			        
					//}
			        //catch (InterruptedException e) {
			        	//TODO
					//}	 
			        Log.i("doInBackgound", "finish!!");
			}
			return null;
		}
	};
    
	private void updateNetworkTraffic(){
		if(mBound){			
			//try {
		        QueryBuilder<NetworkLog> qb = networkLogDao.queryBuilder();
		    
		        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getTodayStartDate())));	        
		        long cumRxBytes = 0;
		        long cumTxBytes = 0;
		        List<NetworkLog> logs = qb.list();
		        
		        for(NetworkLog log : logs){        	
		        	cumRxBytes += log.getReceivedByte();
		        	cumTxBytes += log.getTransmittedByte();
		        }	        	        
		        
		        text3GTodayRx.setText(toBytes(cumRxBytes));
		        text3GTodayTx.setText(toBytes(cumTxBytes));
		        text3GTodayTotal.setText(toBytes(cumTxBytes+cumRxBytes));
		        
				//Thread.sleep(100);
				
		        //WiFi network traffic
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getTodayStartDate())));
		        logs = qb.list();
		        for(NetworkLog log : logs){	
		        	cumRxBytes += log.getReceivedByte();
		        	cumTxBytes += log.getTransmittedByte();
		        }	        	        
		        
		        textWifiTodayRx.setText(toBytes(cumRxBytes));
		        textWifiTodayTx.setText(toBytes(cumTxBytes));
		        textWifiTodayTotal.setText(toBytes(cumTxBytes+cumRxBytes));
		        
		        //Thread.sleep(100);
		        
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getWeekStartDate())));	        
		        cumRxBytes = 0;
		        cumTxBytes = 0;
		        logs = qb.list();
		        Log.d("updateNetworkTraffic", "log list size:" + logs.size());
		        for(NetworkLog log : logs){        	
		        	cumRxBytes += log.getReceivedByte();
		        	cumTxBytes += log.getTransmittedByte();
		        }	        	        
		        
		        text3GWeekRx.setText(toBytes(cumRxBytes));
		        text3GWeekTx.setText(toBytes(cumTxBytes));
		        text3GWeekTotal.setText(toBytes(cumTxBytes+cumRxBytes));
		        
		        //Thread.sleep(100);
		        
		        //WiFi network traffic
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getWeekStartDate())));
		        logs = qb.list();
		        for(NetworkLog log : logs){
		        	cumRxBytes += log.getReceivedByte();
		        	cumTxBytes += log.getTransmittedByte();
		        }	        	        
		        
		        textWifiWeekRx.setText(toBytes(cumRxBytes));
		        textWifiWeekTx.setText(toBytes(cumTxBytes));
		        textWifiWeekTotal.setText(toBytes(cumTxBytes+cumRxBytes));
		        
		        //Thread.sleep(100);
		        
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getMonthStartDate())));	        
		        cumRxBytes = 0;
		        cumTxBytes = 0;
		        logs = qb.list();
		        
		        for(NetworkLog log : logs){	        	
		        	cumRxBytes += log.getReceivedByte();
		        	cumTxBytes += log.getTransmittedByte();
		        }	        	        
		        
		        //Thread.sleep(100);
		        
		        text3GMonthRx.setText(toBytes(cumRxBytes));
		        text3GMonthTx.setText(toBytes(cumTxBytes));
		        text3GMonthTotal.setText(toBytes(cumTxBytes+cumRxBytes));
		        
		        //WiFi network traffic
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getMonthStartDate())));
		        logs = qb.list();
		        for(NetworkLog log : logs){	
		        	cumRxBytes += log.getReceivedByte();
		        	cumTxBytes += log.getTransmittedByte();
		        }	        	        
		        
		        textWifiMonthRx.setText(toBytes(cumRxBytes));
		        textWifiMonthTx.setText(toBytes(cumTxBytes));
		        textWifiMonthTotal.setText(toBytes(cumTxBytes+cumRxBytes));
		        
		        //Thread.sleep(100);
	        
			//}
	        //catch (InterruptedException e) {
	        	//TODO
			//}	        
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
			updateStatus();
			updateNetworkTraffic();
		}
    };
    private OnClickListener hourlyAppUsageListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, AppUsageActivity.class);
			Bundle extras = new Bundle();
			extras.putString(AppUsageActivity.PREFS_CHART_MODE, AppUsageActivity.MODE_HOURLY);
			intent.putExtras(extras);
			MainActivity.this.startActivity(intent);			
		}
	};
		
    private OnClickListener dailyAppUsageListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			Intent intent = new Intent();
			intent.setClass(MainActivity.this, AppUsageActivity.class);
			Bundle extras = new Bundle();
			extras.putString(AppUsageActivity.PREFS_CHART_MODE, AppUsageActivity.MODE_DAILY);
			intent.putExtras(extras);
			MainActivity.this.startActivity(intent);			
		}    	
    };
    
    /*
    private OnClickListener filterListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			
			int startHourOfDay = spinner1.getSelectedItemPosition();
			int endHourOfDay = spinner2.getSelectedItemPosition();
			
			if (startHourOfDay != AdapterView.INVALID_POSITION && endHourOfDay != AdapterView.INVALID_POSITION){
				
				
				
				double cumUseTime = 0;
				
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
		        }
		        
		        textAppUsage.setText("�A�w�ϥγo��app: " + String.valueOf(cumUseTime/60000) + "����");
			}
		}
	};*/
	
	/*
	private OnClickListener cancelFilterListener = new OnClickListener(){
		@Override
		public void onClick(View v) {
			//updateAppUsage();
		}		
	};*/ 
	
	/*
	private void updateAppUsage(){
        double cumUseTime = 0;
        QueryBuilder<MobileLog> qb = mobileLogDao.queryBuilder(); 
        List<MobileLog> loggers = qb.where(Properties.ProcessCurrentPackage.eq("edu.ntu.arbor.sbchao.androidlogger")).list();
        for(MobileLog mobileLog : loggers){
        	cumUseTime += mobileLog.getRecordFreq();
        }
        textAppUsage.setText("�A�w�ϥγo��app: " + String.valueOf(cumUseTime/60000) + "����");
	}*/

	
	private static String toBytes(long bytes){
		if(bytes < 1024){return String.valueOf(bytes) + "Bytes";}
		else if (bytes >= 1024 && bytes < 1024*1024){
			return String.format("%.1f", bytes/1024.0) + "KB";
		}
		else if (bytes >= 1024*1024 && bytes < 1024*1024*1024){
			return String.format("%.1f", bytes/(1024.0*1024)) + "MB";
		}
		else if (bytes >= 1024*1024*1024 && bytes < 1024*1024*1024*1024){
			return String.format("%.1f", bytes/(1024.0*1024*1024)) + "GB";
		}
		else if (bytes >= 1024*1024*1024*1024){
			return String.format("%.1f", bytes/(1024.0*1024*1024*1024)) + "TB";
		}
		else return "";
	}
	
	private Date getWeekStartDate(){
		Calendar c = Calendar.getInstance();
        c.clear();
        c.setTime(new Date());
        c.clear(Calendar.HOUR);
        c.clear(Calendar.HOUR_OF_DAY);
        c.clear(Calendar.MINUTE);
        c.clear(Calendar.SECOND);
        c.clear(Calendar.MILLISECOND);
        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
        //Log.d("getWeekDate - Month", String.valueOf(c.getTime().getMonth()));
        //Log.d("getWeekDate - Date", String.valueOf(c.getTime().getDate()));	   
        //Log.d("getWeekDate - after?", String.valueOf(c.getTime().before(new Date())));
        return c.getTime();
	}
        
	private Date getTodayStartDate(){    
		Calendar c = Calendar.getInstance();
        c.clear();
        c.setTime(new Date());
        c.clear(Calendar.HOUR);
        c.clear(Calendar.HOUR_OF_DAY);
        c.clear(Calendar.MINUTE);
        c.clear(Calendar.SECOND);
        c.clear(Calendar.MILLISECOND);
        Log.d("getTodayDate", String.valueOf(c.getTime().getDate()));
        return c.getTime();
	}
	 
	private Date getMonthStartDate(){
		Calendar c = Calendar.getInstance();
		c.clear();
	    c.setTime(new Date());
	    c.set(Calendar.DAY_OF_MONTH, 1);
	    c.clear(Calendar.HOUR);
	    c.clear(Calendar.HOUR_OF_DAY);
	    c.clear(Calendar.MINUTE);
	    c.clear(Calendar.SECOND);
	    c.clear(Calendar.MILLISECOND);
	    Log.d("getMonthDate", String.valueOf(c.getTime().getDate()));
	    return c.getTime();
	}
	
}
