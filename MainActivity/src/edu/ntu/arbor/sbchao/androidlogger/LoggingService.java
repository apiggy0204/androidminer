package edu.ntu.arbor.sbchao.androidlogger;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster.DevOpenHelper;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;

public class LoggingService extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();
        
    static boolean isServiceRunning = false;
    static final int HIGH_RECORD_FREQ = 10000;      //10 seconds
    static final int LOW_RECORD_FREQ  = 120000;    //2 minutes
    int recordFreq = HIGH_RECORD_FREQ;
        
    private static LogManager mLogMgr;
    private static DataManager mDataMgr;
    
	String deviceId;
	
    //Battery information
	int batLevel;
	int batScale;
	int batVoltage;
	int batStatus;
	int batPlugged;
	double batPct;
	
	//Location Information
	private LocationManager mLocMgr;
	final static int MIN_LOC_INTERVAL = 30000;
	final static int MIN_LOC_DISTANCE = 50;
	Location mLocation;
	boolean isGPSProviderEnabled;	
	boolean isNetworkProviderEnabled;
	int gpsStatus; //OUT_OF_SERVICE = 0; TEMPORARILY_UNAVAILABLE = 1; AVAILABLE = 2
	int networkStatus;
    
    //calling status
    TelephonyManager mTelMgr;
	int callState; //CALL_STATE_IDLE, CALL_STATE_RINGING or CALL_STATE_OFFHOOK; see http://developer.android.com/reference/android/telephony/TelephonyManager.html
	String inNumber;
	
	//network status
	ConnectivityManager mConnMgr;
	boolean connectivity;
	int activeNetworkType; //Type of connection (if active); TYPE_MOBILE=0, TYPE_WIFI=1
	boolean isMobileAvailable;		
	boolean isMobileConnected;
	boolean isMobileFailover;
	boolean isMobileRoaming;
	NetworkInfo.State mobileState; //http://developer.android.com/reference/android/net/NetworkInfo.State.html
								   //CONNECTED; CONNECTING; DISCONNECTED; DISCONNECTING; SUSPENDED; UNKNOWN 
	boolean isWifiAvailable;		
	boolean isWifiConnected;
	boolean isWifiFailover;
	boolean isWifiRoaming;
	NetworkInfo.State wifiState;
	
	ActivityManager mActMgr;
	ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();	
	String processCurrentClass;
	String processCurrentPackage;			
	long availMem;		
	boolean isLowMemory;

	
    private SQLiteDatabase db;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private MobileLogDao mobileLogDao;
    private Cursor cursor;
	
	
	
    @Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.    	
		HandlerThread handlerThread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		handlerThread.start();		
		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = handlerThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		Log.i("onCreate", "service is creating...");
		
		//Register receivers so that we can monitor changes of sensor
		if(!isServiceRunning){
			registerServices();
			deviceId = mTelMgr.getDeviceId();
			Log.i("deviceId", deviceId);
			Log.i("onStartCommand", "registering services");
		}
		
		mLogMgr = new LogManager(this);		
		mLogMgr.checkExternalStorage();
		mLogMgr.createNewLog();
		
		mDataMgr = new DataManager();
		
		
		/*
		DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "MobileLog", null);
        db = helper.getWritableDatabase();
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        mobileLogDao = daoSession.getMobileLogDao();
        */
		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {		
		
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Log.i("onStartCommand", "service starting");
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
		
		// If we get killed, after returning from here, restart
	    return START_STICKY;
	}
	
    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
      
	    public ServiceHandler(Looper looper) {
            super(looper);
        }
		@Override
		public void handleMessage(Message msg) {			
			
			if(!isServiceRunning){
				Log.i("handleMessage", "The logging is not running...");
				isServiceRunning = true;
				mServiceHandler.post(writingToLogTask);				
				Log.i("handleMessage", "The logging is now running!");
			} else {
				Log.i("handleMessage", "The logging is already running...");
			}				     
	    }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
  
    public class LocalBinder extends Binder{
    	LoggingService getService(){
    		return LoggingService.this;
    	}
    }
    
	@Override
	public void onDestroy() {	    
		unregisterServices();
		isServiceRunning = false; //Stop the continuous logging
		
		try {
			mLogMgr.logFos.close();
		} catch (IOException e) {
			Log.e("onDestroy", "cannot close the file output stream");
			e.printStackTrace();
		}
		
		super.onDestroy();
	    Log.i("onDestroy", "service done");
	}
  
	//Write sensor data into log files
	//When isServiceRunning == true, continue to log indefinitely with postDelayed() method.
	//Note that isServiceRunning is set to be false in onDestroy(), which ends the logging.
	private Runnable writingToLogTask = new Runnable(){		
		@Override
		public void run() {			
			if(isServiceRunning){
				getNetworkInfo();		
				monitorProcess();								
				writeToLog();
				
				//writeToDatabase();
				
				mServiceHandler.postDelayed(writingToLogTask, recordFreq);
			} else {				
				Log.v("writingToLogTask", "FINISHED!"); 
			}
		}
	};
	
	private Runnable writingToLogOnceTask = new Runnable(){

		@Override
		public void run() {
			if(isServiceRunning){
				getNetworkInfo();			
				monitorProcess();								
				writeToLog(); //Write to the log
			} else {				
				Log.v("writingToLogOnceTask", "FINISHED!"); 
			}						
		}
		
	};
	
	private void registerServices(){
		registerReceiver(mBatteryChangedReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(mScreenChangedReceiver,new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(mScreenChangedReceiver,new IntentFilter(Intent.ACTION_SCREEN_OFF)); 
		registerReceiver(mDateChangedReceiver,new IntentFilter(Intent.ACTION_DATE_CHANGED));
		
		mLocMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);		
		mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOC_INTERVAL, MIN_LOC_DISTANCE, mLocationListener);
		mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOC_INTERVAL, MIN_LOC_DISTANCE, mLocationListener);
		
		mTelMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		mTelMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		mConnMgr = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		mActMgr = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);				
	}
	
	private void unregisterServices(){
		unregisterReceiver(mBatteryChangedReceiver);
		unregisterReceiver(mDateChangedReceiver);
		unregisterReceiver(mScreenChangedReceiver);
		
		mLocMgr.removeUpdates(mLocationListener);
		mTelMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);		
	}
	
	private BroadcastReceiver mBatteryChangedReceiver = new BroadcastReceiver(){ 
		public void onReceive(Context context,Intent intent){
			String action=intent.getAction();
			if(Intent.ACTION_BATTERY_CHANGED.equals(action)){				
				batLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
				batScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
				batVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
				batStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
				batPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);				
				batPct = (double) batLevel /(double) batScale;
			}
		}
	};
	
	private BroadcastReceiver mDateChangedReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context,Intent intent){
			String action=intent.getAction();
			if(Intent.ACTION_DATE_CHANGED.equals(action)){				
				mLogMgr.createNewLog();
				mLogMgr.moveToExternalStorage();								
			}
		}
	};
	
	//adjust logging frequency according to whether the user activates the screen
	private BroadcastReceiver mScreenChangedReceiver = new BroadcastReceiver(){ 
		public void onReceive(Context context,Intent intent){
			String action=intent.getAction();
			Log.i("Android Logger",action);
			if(Intent.ACTION_SCREEN_ON.equals(action)){				
				Log.i("LOGGER_Screen","SCREEN IS ON");
				recordFreq = HIGH_RECORD_FREQ;				
			}else if (Intent.ACTION_SCREEN_OFF.equals(action)){				
				Log.i("LOGGER_Screen","SCREEN IS OFF");
				recordFreq = LOW_RECORD_FREQ;
			}
		}
	};
	
				
	private LocationListener mLocationListener = new LocationListener(){
	    
		private static final int TWO_MINUTES = 1000 * 60 * 2;
		
		public void onLocationChanged(Location location) {
	    	if(location != null){
	    		if (isBetterLocation(location, mLocation)){
	    			mLocation = location;
	    		}
	    	} else {
	    		mLocation = null;
	    	}
	    }
		
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	Log.i("onStatusChanged", provider);
	    	Log.i("onStatusChanged", String.valueOf(status));	    	
	    	if(provider.equals("gps")) gpsStatus = status;
	    	if(provider.equals("network")) networkStatus = status;	 	    	
	    }

	    public void onProviderEnabled(String provider) {
	    	Log.i("onProviderEnables", provider);
	    	if(provider.equals("gps")) isGPSProviderEnabled = true;
	    	if(provider.equals("network")) isNetworkProviderEnabled = true;
	    	
	    }

	    public void onProviderDisabled(String provider) {
	    	Log.i("onProviderDisables", provider);
	    	if(provider.equals("gps")) isGPSProviderEnabled = false;
	    	if(provider.equals("network")) isNetworkProviderEnabled = false;
	    }	
	    
	    /** Determines whether one Location reading is better than the current Location fix
	      * @param location  The new Location that you want to evaluate
	      * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	      */
	    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
	        if (currentBestLocation == null) {
	            // A new location is always better than no location
	            return true;
	        }

	        // Check whether the new location fix is newer or older
	        long timeDelta = location.getTime() - currentBestLocation.getTime();
	        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
	        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
	        boolean isNewer = timeDelta > 0;

	        // If it's been more than two minutes since the current location, use the new location
	        // because the user has likely moved
	        if (isSignificantlyNewer) {
	            return true;
	        // If the new location is more than two minutes older, it must be worse
	        } else if (isSignificantlyOlder) {
	            return false;
	        }

	        // Check whether the new location fix is more or less accurate
	        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
	        boolean isLessAccurate = accuracyDelta > 0;
	        boolean isMoreAccurate = accuracyDelta < 0;
	        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

	        // Check if the old and new location are from the same provider
	        boolean isFromSameProvider = isSameProvider(location.getProvider(),
	                currentBestLocation.getProvider());

	        // Determine location quality using a combination of timeliness and accuracy
	        if (isMoreAccurate) {
	            return true;
	        } else if (isNewer && !isLessAccurate) {
	            return true;
	        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
	            return true;
	        }
	        return false;
	    }

	    /** Checks whether two providers are the same */
	    private boolean isSameProvider(String provider1, String provider2) {
	        if (provider1 == null) {
	          return provider2 == null;
	        }
	        return provider1.equals(provider2);
	    }
	    
	}; 
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener(){
		public void onCallStateChanged(int state, String incomingNumber){
			Log.i("onCallStateChanged", String.valueOf(state));
			callState = state;
			if (state == TelephonyManager.CALL_STATE_OFFHOOK || state == TelephonyManager.CALL_STATE_RINGING ){
				inNumber = incomingNumber;
			} else { 
				inNumber = "";
			}
			mServiceHandler.postDelayed(writingToLogOnceTask, 0);
		}
	};
	
	private void getNetworkInfo(){
		
		NetworkInfo activeNetworkInfo = mConnMgr.getActiveNetworkInfo();
		if(activeNetworkInfo == null){ connectivity = false; } 
		else {
			connectivity = true;
			activeNetworkType = activeNetworkInfo.getType();
		}
		
		NetworkInfo mobileInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		isMobileAvailable = mobileInfo.isAvailable();		
		isMobileConnected = mobileInfo.isConnected();
		isMobileFailover = mobileInfo.isFailover();
		isMobileRoaming = mobileInfo.isRoaming();
		mobileState = mobileInfo.getState();
				
		NetworkInfo wifiInfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		isWifiAvailable = wifiInfo.isAvailable();		
		isWifiConnected = wifiInfo.isConnected();
		isWifiFailover = wifiInfo.isFailover();
		isWifiRoaming = wifiInfo.isRoaming();
		wifiState = wifiInfo.getState();
		
		if (isMobileConnected || isWifiConnected ) {
			Intent uploadIntent = new Intent(this, UploadingService.class);
			startService(uploadIntent);
		}
		
	}
  	
	private void monitorProcess(){
		
		//TODO running processes in the background
		
		processCurrentPackage = mActMgr.getRunningTasks(1).get(0).topActivity.getPackageName();
		Log.v("processCurrentPackage", processCurrentPackage);
		processCurrentClass = mActMgr.getRunningTasks(1).get(0).topActivity.getClassName();
		Log.v("processCurrentClass", processCurrentClass);
		
		mActMgr.getMemoryInfo(memoryInfo);
		availMem = memoryInfo.availMem;				
		isLowMemory = memoryInfo.lowMemory;		
	}
	

			
	private void writeToLog(){
		
		Log.i("gpsStatus", String.valueOf(gpsStatus));
		
		//Setting data for writing
		mDataMgr.set(DataManager.DEVICE_ID, String.valueOf(deviceId));
		
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();
		
		String timeStr = String.valueOf(now.year) + "-" + String.valueOf(now.month+1) + "-" + String.valueOf(now.monthDay)  //Month = [0-11]
				+ " " + now.format("%T");
		Log.d("setToNow", timeStr);
		mDataMgr.set(DataManager.TIME, timeStr);
		mDataMgr.set(DataManager.RECORD_FREQUENCY, String.valueOf(recordFreq));
		
		mDataMgr.set(DataManager.BAT_STATUS, String.valueOf(batStatus));
		mDataMgr.set(DataManager.BAT_PERCENTAGE, String.valueOf(batPct));
		
		mDataMgr.set(DataManager.GPS_PROVIDER_STATUS, String.valueOf(gpsStatus));
		mDataMgr.set(DataManager.NETWORK_PROVIDER_STATUS, String.valueOf(networkStatus));
		
		if(mLocation != null){
			mDataMgr.set(DataManager.LOC_ACCURACY, String.valueOf(mLocation.getAccuracy()));
			mDataMgr.set(DataManager.LOC_LATITUDE, String.valueOf(mLocation.getLatitude()));
			mDataMgr.set(DataManager.LOC_LONGITUDE, String.valueOf(mLocation.getLongitude()));
			mDataMgr.set(DataManager.LOC_PROVIDER, mLocation.getProvider());
			mDataMgr.set(DataManager.LOC_SPEED, String.valueOf(mLocation.getSpeed()));			
		}
		else { 
			mDataMgr.set(DataManager.LOC_ACCURACY, null);
			mDataMgr.set(DataManager.LOC_LATITUDE, null);
			mDataMgr.set(DataManager.LOC_LONGITUDE, null);
			mDataMgr.set(DataManager.LOC_PROVIDER, null);
			mDataMgr.set(DataManager.LOC_SPEED, null);
		}
		
		mDataMgr.set(DataManager.MOBILE_STATE, String.valueOf(mobileState));
		mDataMgr.set(DataManager.WIFI_STATE, String.valueOf(wifiState));
		
		mDataMgr.set(DataManager.PROCESS_CURRENT_PACKAGE, String.valueOf(processCurrentPackage));
		mDataMgr.set(DataManager.IS_LOW_MEMORY, String.valueOf(isLowMemory));
		
		//Write to the log
		try {
			String toWrite = mDataMgr.toString();
			
			if (mLogMgr.logFos != null){
				mLogMgr.logFos.write(toWrite.getBytes());
			}
		} catch (IOException e) {
			Log.e("writeToLogFile", "Cannot write into the file: " + mLogMgr.logFilename);			
		}		
		Log.v("writeToLogFile", "The file is " + getFilesDir() + "/" + mLogMgr.logFilename);
		Log.v("writeToLogFile", "Write Successfully!");
	}
	
	
	private void writeToDatabase(){
		
		Log.d("Database", "Starting to insert new MobileLog");
		        
        MobileLog mobileLog = null;
        if(mLocation != null){
	        mobileLog = new MobileLog(null, deviceId, new Date(), new Date().getDay(), new Date().getHours(), recordFreq, batStatus, batPct, gpsStatus, networkStatus, 
	        		mobileState.toString(), wifiState.toString(), processCurrentPackage, isLowMemory, (double) mLocation.getAccuracy(), mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getProvider(), 
	        		(double) mLocation.getSpeed());
        }
        else{
	        mobileLog = new MobileLog(null, deviceId, new Date(), new Date().getDay(), new Date().getHours(), recordFreq, batStatus, batPct, gpsStatus, networkStatus, 
	        		mobileState.toString(), wifiState.toString(), processCurrentPackage, isLowMemory, null, null, null, null, null); 
        }
 
        mobileLogDao.insert(mobileLog);
        Log.d("Database", "Inserted new MobileLog, ID: " + mobileLog.getId());

        //cursor.requery();
        
	};
	
}
	
	
	
	
