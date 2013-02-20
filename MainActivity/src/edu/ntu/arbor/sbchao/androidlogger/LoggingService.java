package edu.ntu.arbor.sbchao.androidlogger;

import java.io.IOException;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

public class LoggingService extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();
        
    static boolean isServiceRunning = false;
    static final int HIGH_RECORD_FREQ = 30000;      //30 seconds
    static final int LOW_RECORD_FREQ  = 180000;    //3 minutes
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
	double batPercentage;
	
	//Location Information
	private LocationManager mLocMgr;
	final static int MIN_LOC_INTERVAL = 30000;
	final static int MIN_LOC_DISTANCE = 50;
	Location mLocation;
	boolean isGPSProviderEnabled;	
	boolean isNetworkProviderEnabled;
	int GPSProviderStatus; //OUT_OF_SERVICE = 0; TEMPORARILY_UNAVAILABLE = 1; AVAILABLE = 2
	int networkProviderStatus;
    
	/*
	double locAccuracy; //The effective range (in meter) of confidence interval = 68%
    String locProvider; //gps or network
    double locAltitude;
    double locLatitude;
    double locLongitude;	        
    double locSpeed;
    double locTime; //Not recorded; for comparison of better location only
    */
	
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
	
	/*
	private Runnable uploadingTask = new Runnable(){
		@Override
		public void run() {
			mLogMgr.uploadAll();
		}
	};*/
	
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
				batPercentage = (double) batLevel /(double) batScale;
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
	    		//TODO isBetterLocation
	    		if (isBetterLocation(location, mLocation)){
	    			mLocation = location;
	    		}
	    		/*
		    	Log.i("onLocationChanged", "(" + location.getLatitude() + ", " + location.getLongitude() + ")");	    	
		        // Called when a new location is found by the network location provider.
		        locAccuracy = location.getAccuracy();
		        locProvider = location.getProvider();
		        locAltitude = location.getAltitude();
		        locLatitude = location.getLatitude();
		        locLongitude = location.getLongitude();	        
		        locSpeed = location.getSpeed();
		        locTime = location.getTime();	        
		        Log.i("onLocationChanged", "provider:" + String.valueOf(locProvider));
		        Log.i("onLocationChanged", "acc:" + String.valueOf(locAccuracy));
		        Log.i("onLocationChanged", "speed:" + String.valueOf(locSpeed));
		        Log.i("onLocationChanged", "time:" + String.valueOf(locTime));*/
		        
	    	} else {
	    		mLocation = null;
	    	}
	        
	    }
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	Log.i("onStatusChanged", provider);
	    	Log.i("onStatusChanged", String.valueOf(status));	    	
	    	if(provider=="gps") GPSProviderStatus = status;
	    	if(provider=="network") networkProviderStatus = status;	 	    	
	    }

	    public void onProviderEnabled(String provider) {
	    	Log.i("onProviderEnables", provider);
	    	if(provider=="gps") isGPSProviderEnabled = true;
	    	if(provider=="network") isNetworkProviderEnabled = true;
	    	
	    }

	    public void onProviderDisabled(String provider) {
	    	Log.i("onProviderDisables", provider);
	    	if(provider=="gps") isGPSProviderEnabled = false;
	    	if(provider=="network") isNetworkProviderEnabled = false;
	    	//TODO
	    	//Ask user to turn on the network-based provider
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
			//Thread uploadThread = new Thread(uploadingTask);
			//uploadThread.start();
			Intent uploadIntent = new Intent(this, UploadingService.class);
			startService(uploadIntent);
		}
		
	}
  	
	private void monitorProcess(){
		processCurrentPackage = mActMgr.getRunningTasks(1).get(0).topActivity.getPackageName();
		Log.v("processCurrentPackage", processCurrentPackage);
		processCurrentClass = mActMgr.getRunningTasks(1).get(0).topActivity.getClassName();
		Log.v("processCurrentClass", processCurrentClass);
		
		mActMgr.getMemoryInfo(memoryInfo);
		availMem = memoryInfo.availMem;		
		Log.v("availMem", String.valueOf(availMem));
		isLowMemory = memoryInfo.lowMemory;
		Log.v("isLowMemory", String.valueOf(isLowMemory));
	}
	

			
	private void writeToLog(){
		
		mDataMgr.set(DataManager.DEVICE_ID, String.valueOf(deviceId));
		
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();
		String timeStr = String.valueOf(now.year) + "/" + String.valueOf(now.month) + "/" + String.valueOf(now.monthDay) 
				+ " " + now.format("%T");
		mDataMgr.set(DataManager.TIME, timeStr);
		mDataMgr.set(DataManager.RECORD_FREQUENCY, String.valueOf(recordFreq));
		
		mDataMgr.set(DataManager.BAT_STATUS, String.valueOf(batStatus));
		mDataMgr.set(DataManager.BAT_PERCENTAGE, String.valueOf(batPercentage));
		
		mDataMgr.set(DataManager.GPS_PROVIDER_STATUS, String.valueOf(GPSProviderStatus));
		mDataMgr.set(DataManager.NETWORK_PROVIDER_STATUS, String.valueOf(networkProviderStatus));
		
		if(mLocation == null){
			mDataMgr.set(DataManager.LOC_ACCURACY, null);
			mDataMgr.set(DataManager.LOC_LATITUDE, null);
			mDataMgr.set(DataManager.LOC_LONGITUDE, null);
			mDataMgr.set(DataManager.LOC_PROVIDER, null);
			mDataMgr.set(DataManager.LOC_SPEED, null);
		}
		else{ 
			mDataMgr.set(DataManager.LOC_ACCURACY, String.valueOf(mLocation.getAccuracy()));
			mDataMgr.set(DataManager.LOC_LATITUDE, String.valueOf(mLocation.getLatitude()));
			mDataMgr.set(DataManager.LOC_LONGITUDE, String.valueOf(mLocation.getLongitude()));
			mDataMgr.set(DataManager.LOC_PROVIDER, mLocation.getProvider());
			mDataMgr.set(DataManager.LOC_SPEED, String.valueOf(mLocation.getSpeed()));
		}
		/*else{ 
			mDataMgr.set(DataManager.LOC_ACCURACY, String.valueOf(locAccuracy));
			mDataMgr.set(DataManager.LOC_LATITUDE, String.valueOf(locLatitude));
			mDataMgr.set(DataManager.LOC_LONGITUDE, String.valueOf(locLongitude));
			mDataMgr.set(DataManager.LOC_PROVIDER, String.valueOf(locProvider));
			mDataMgr.set(DataManager.LOC_SPEED, String.valueOf(locSpeed));
		}*/
		
		mDataMgr.set(DataManager.MOBILE_STATE, String.valueOf(mobileState));
		mDataMgr.set(DataManager.WIFI_STATE, String.valueOf(wifiState));
		
		mDataMgr.set(DataManager.PROCESS_CURRENT_PACKAGE, String.valueOf(processCurrentPackage));
		mDataMgr.set(DataManager.IS_LOW_MEMORY, String.valueOf(isLowMemory));
		/*
		//device id
		String toWrite = deviceId + "\t";
		
		//Current time
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();				
		toWrite += String.valueOf(now.year) + "/" + String.valueOf(now.month) + "/" + String.valueOf(now.monthDay) 
				+ " " + now.format("%T") + "\t"; 
		
		toWrite += String.valueOf(recordFreq) + "\t";
		
		//Battery
		//toWrite += String.valueOf(batLevel) + "\t" + String.valueOf(batScale) + "\t" + String.valueOf(batVoltage) + "\t" 
		//		+ String.valueOf(batStatus) + "\t" + String.valueOf(batPlugged) + "\t" + String.valueOf(batPercentage) + "\t";
		toWrite += String.valueOf(batStatus) + "\t" + String.valueOf(batPercentage) + "\t";
		
		
		//Location
		toWrite += String.valueOf(isGPSProviderEnabled) + "\t" + String.valueOf(isNetworkProviderEnabled) + "\t" 
				+ String.valueOf(GPSProviderStatus) + "\t" + String.valueOf(networkProviderStatus) + "\t"
				+ String.valueOf(locAccuracy) + "\t" + locProvider + "\t" // + String.valueOf(locAltitude) + "\t" 
				+ String.valueOf(locLatitude) + "\t" + String.valueOf(locLongitude) + "\t" + String.valueOf(locSpeed) + "\t";				
		
		//Calling
		//toWrite += String.valueOf(callState) + "\t" + inNumber + "\t";
		
		//Connectivity
		toWrite += String.valueOf(connectivity) + "\t" + String.valueOf(activeNetworkType) + "\t"
				+ String.valueOf(isMobileAvailable) + "\t" + String.valueOf(isMobileConnected) + "\t"
				+ String.valueOf(isMobileFailover) + "\t" + String.valueOf(isMobileRoaming) + "\t"
				+ String.valueOf(mobileState) + "\t" + String.valueOf(isWifiAvailable) + "\t"
				+ String.valueOf(isWifiConnected) + "\t" + String.valueOf(isWifiFailover) + "\t"
				+ String.valueOf(isWifiRoaming) + "\t" + String.valueOf(wifiState) + "\t";
		
		//Process
		toWrite += processCurrentClass + "\t" + processCurrentPackage + "\t" //+ String.valueOf(availMem) + "\t"		
				+ String.valueOf(isLowMemory) + "\n";
				
		*/
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
	
}
	
	
	
	
