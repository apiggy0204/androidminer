package edu.ntu.arbor.sbchao.androidlogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

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
import android.os.Environment;
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
import android.widget.Toast;

public class LoggingService extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();
        
    static boolean isServiceRunning = false;
    static final int HIGH_RECORD_FREQ = 6000;      //6 seconds
    static final int LOW_RECORD_FREQ  = 120000;    //2 minutes
    int recordFreq = HIGH_RECORD_FREQ;    
        
    private static LogManager mLogMgr;
    private static DataManager mDataMgr = new DataManager();
    /*
    //static final String SERVER_PATH = "http://netdbmobileminer.appspot.com";
    static final String SERVER_PATH = "http://10.0.2.2:8082/";
    //Logger file information
    static final String LOG_DIR_PATH = "/AndroidLogger";
    static final String LOG_UNUPLOADED_PATH = "/Unuploaded";
    static final String LOG_UPLOADED_PATH = "/Uploaded";
    String logFilename = null;
    FileOutputStream logFos = null;*/ 
    
    //sdcard availability
    //boolean mExternalStorageAvailable = false;
	//boolean mExternalStorageWriteable = false;    	
	
    //TODO 123
    
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
	boolean isGPSProviderEnabled;	
	boolean isNetworkProviderEnabled;
	int GPSProviderStatus; //OUT_OF_SERVICE = 0; TEMPORARILY_UNAVAILABLE = 1; AVAILABLE = 2
	int networkProviderStatus;
    double locAccuracy; //The effective range (in meter) of confidence interval = 68%
    String locProvider; //gps or network
    double locAltitude;
    double locLatitude;
    double locLongitude;	        
    double locSpeed;
    double locTime; //Not recorded; for comparison of better location only
	
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
	        //stopSelf(msg.arg1);
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
	
	private Runnable uploadingTask = new Runnable(){
		@Override
		public void run() {
			mLogMgr.uploadAll();									
		}
	};
	
	private void registerServices(){
		registerReceiver(mBatteryChangedReceiver,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(mScreenChangedReceiver,new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(mScreenChangedReceiver,new IntentFilter(Intent.ACTION_SCREEN_OFF)); 
		registerReceiver(mDateChangedReceiver,new IntentFilter(Intent.ACTION_DATE_CHANGED));
		
		mLocMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);		
		mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
		mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
		
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
				
				Thread uploadThread = new Thread(uploadingTask);
				uploadThread.start();
				
				/*
				mLogManager.createLocalLogFileForToday();
				//move files into sdcard
				boolean success = moveToExternalStorage();
				Log.i("mDateChangeReceiver", "moveToExternalStorage: " + String.valueOf(success));				
							
				Thread uploadThread = new Thread(uploadTask);
				uploadThread.start();
				*/
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
	    public void onLocationChanged(Location location) {
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
	        Log.i("onLocationChanged", "time:" + String.valueOf(locTime));
	        //TODO isBetterLocation
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
		
		//TODO if (isMobileConnected || isWifiConnected ) { send network statistics... }
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
		//device id
		String toWrite = deviceId + "\t";
		
		//Current time
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();				
		toWrite += String.valueOf(now.year) + "/" + String.valueOf(now.month) + "/" + String.valueOf(now.monthDay) 
				+ " " + now.format("%T") + "\t"; 
		
		
		//Battery
		toWrite += String.valueOf(batLevel) + "\t" + String.valueOf(batScale) + "\t" + String.valueOf(batVoltage) + "\t" 
				+ String.valueOf(batStatus) + "\t" + String.valueOf(batPlugged) + "\t" + String.valueOf(batPercentage) + "\t";
		
		//Location
		toWrite += String.valueOf(isGPSProviderEnabled) + "\t" + String.valueOf(isNetworkProviderEnabled) + "\t" 
				+ String.valueOf(GPSProviderStatus) + "\t" + String.valueOf(networkProviderStatus) + "\t"
				+ String.valueOf(locAccuracy) + "\t" + locProvider + "\t" + String.valueOf(locAltitude) + "\t" 
				+ String.valueOf(locLatitude) + "\t" + String.valueOf(locLongitude) + "\t" + String.valueOf(locSpeed) + "\t";
				//+ String.valueOf(locTime) + "\t";
		
		//Calling
		toWrite += String.valueOf(callState) + "\t" + inNumber + "\t";
		
		//Connectivity
		toWrite += String.valueOf(connectivity) + "\t" + String.valueOf(activeNetworkType) + "\t"
				+ String.valueOf(isMobileAvailable) + "\t" + String.valueOf(isMobileConnected) + "\t"
				+ String.valueOf(isMobileFailover) + "\t" + String.valueOf(isMobileRoaming) + "\t"
				+ String.valueOf(mobileState) + "\t" + String.valueOf(isWifiAvailable) + "\t"
				+ String.valueOf(isWifiConnected) + "\t" + String.valueOf(isWifiFailover) + "\t"
				+ String.valueOf(isWifiRoaming) + "\t" + String.valueOf(wifiState) + "\t";
		
		//Process
		toWrite += processCurrentClass + "\t" + processCurrentPackage + "\t" + String.valueOf(availMem) + "\t"		
				+ String.valueOf(isLowMemory) + "\n";
				
		
		try {
			if (mLogMgr.logFos != null){
				mLogMgr.logFos.write(toWrite.getBytes());
			}
		} catch (IOException e) {
			Log.e("writeToLogFile", "Cannot write into the file: " + mLogMgr.logFilename);			
		}		
		Log.v("writeToLogFile", "The file is " + getFilesDir() + "/" + mLogMgr.logFilename);
		Log.v("writeToLogFile", "Write Successfully!");
	}
	
	/*
	//Check if external storage is available and create directories if they don't exist
	private void checkExternalStorage(){
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		    //TODO ask the user if she wants to make sdcard available? 
		}

		//Create directories if they do not exists
		if( mExternalStorageAvailable && mExternalStorageWriteable ){
			String extPath = Environment.getExternalStorageDirectory().getPath();			
			
			File extDir = new File(extPath + LOG_DIR_PATH);
			if (!extDir.exists()){				
				boolean success = extDir.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}			
			File unuploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UNUPLOADED_PATH);			
			if (!unuploadedPath.exists()){
				boolean success = unuploadedPath.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}			
			File uploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UPLOADED_PATH);			
			if (!uploadedPath.exists()){
				boolean success = uploadedPath.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}
		}
	}*/
	
	/*
	private void createLocalLogFileForToday(){
    	Date today = new Date();
		DateFormat format = new SimpleDateFormat("MMdd", Locale.getDefault());		
		String newLogFilename = "log_" + format.format(today) + ".txt";		
		
		if( logFos == null || !newLogFilename.equals(logFilename)){			
			try {
				if(logFos != null){
					logFos.close();
				}
				logFilename = newLogFilename;
				logFos = openFileOutput(logFilename, Context.MODE_APPEND);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e("logfile", "cannot open log file: " + logFilename);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("logfile", "cannot close log file:" + logFilename);
			}
		}		
		Log.i("logfile", "create local file " + logFilename + "successfully");
    }
	
	*/
	
	/*
	//Move local log files to sdcard once a day
	private boolean moveToExternalStorage(){
		
		checkExternalStorage();		
		if( mExternalStorageAvailable && mExternalStorageWriteable ){
			
			String extPath = Environment.getExternalStorageDirectory().getPath();
			File unuploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UNUPLOADED_PATH);
			//try {
				//logFos.close();
			//} catch (IOException e) {
			//	Log.e("moveToExternalStorage", "cannot close the file stream!");
			//	e.printStackTrace();
			//}
			
			String dirPath = getFilesDir().getPath();
			String [] fList = fileList();	
			boolean success = true;
			for (String filename : fList){
				File src  = new File(dirPath + "/" + filename);
				File dest = new File(unuploadedPath.getPath() + "/" + filename);
				try {
					//Ignore file for today while moving files to sdcard
					if(!src.getName().equals(logFilename)){
						copyFile(src, dest);					
						boolean deleted = deleteFile(filename);
						Log.i("moveToExternalStorage", "move the file: " + src.getPath() + " successfully to " + dest.getParent() + "? " + String.valueOf(deleted));
					}
				} 				
				catch (IOException e) {
					Log.e("moveToExternalStorage", "cannot move the file: " + src.getPath());
					e.printStackTrace();
					success = false;
				}				
			}			
			return success;
		} 		
		else {		
			Log.i("moveToExternalStorage", "Cannot write into the sdcard temporarily");
			return false;
		}		
	}
	
	//A helper function which copies a file
	private void copyFile(File src, File dst) throws IOException {
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dst);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
	*/
	
}
	
	
	
	
