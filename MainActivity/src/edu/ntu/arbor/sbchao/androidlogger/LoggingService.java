package edu.ntu.arbor.sbchao.androidlogger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DataManager;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.LogInfo;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.LogManager;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.UploadingService;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao;

public class LoggingService extends Service {

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private final IBinder mBinder = new LocalBinder();        
    static boolean isServiceRunning = false;
        
    //Things related to the periodical logging
    private AlarmManager mAlarmMgr;  
    private PendingIntent mPendingIntent;

    boolean isUsing = true;
    static final int HIGH_RECORD_FREQ = 10000;      //10 seconds
    static final int LOW_RECORD_FREQ  = 120000;    //2 minutes
    int recordFreq = HIGH_RECORD_FREQ;
        
    private static LogManager mLogMgr;
    //private static DataManager mDataMgr;
    
	String deviceId;
	
	protected final static String ACTION_WRITE_TO_LOG = "ACTION_WRITE_TO_LOG";
	protected final static String ACTION_ACCELEROMETER_READ = "ACTION_ACCELEROMETER_READ";
	
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
	String process2ndPackage;
	String process3rdPackage;
	long availMem;		
	boolean isLowMemory;
	
	//Network Traffic snapshots
	TrafficSnapshot prevTraf = null;
	TrafficSnapshot latestTraf = null;	
	
	//Motion sensor
	private SensorManager mSensorMgr;
	private boolean accelerometerPresent;
	private Sensor accelerometerSensor;
	private float accX;
	private float accY;
	private float accZ;
	protected float linearAccZ;
	protected float linearAccY;
	protected float linearAccX;
		
	//Database
    private SQLiteDatabase db;
    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private MobileLogDao mobileLogDao;
    private NetworkLogDao networkLogDao;
    private Cursor cursor;
    
    //Settings
	private boolean is3GUploadEnabled;
	private boolean isLoggingAllowed;
	private PendingIntent mAccIntent;
	
	
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
		
		//Load settings
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	    is3GUploadEnabled = settings.getBoolean(SettingsActivity.PREFS_UPLOAD, false);	    
	    isLoggingAllowed  = settings.getBoolean(SettingsActivity.PREFS_LOGGING, true);
	    Editor editor = settings.edit();
	    editor.putBoolean(SettingsActivity.PREFS_UPLOAD, is3GUploadEnabled);
	    editor.putBoolean(SettingsActivity.PREFS_LOGGING, isLoggingAllowed);
	    editor.commit();
	    Log.i("Upload via 3G??", String.valueOf(is3GUploadEnabled));
		
	    //Register receivers so that we can monitor changes of sensor
		if(!isServiceRunning){
			registerServices();
			deviceId = mTelMgr.getDeviceId();
			Log.i("deviceId", deviceId);
			Log.i("onStartCommand", "registering services");
		}
		
		LogManager.addLogInfo("http://140.112.42.22:7380/netdbmobileminer_test/", "log", "AndroidLogger", "Unuploaded", "Uploaded", "log", DataManager.getDefaultDataManager());
		LogManager.addLogInfo("http://140.112.42.22:7380/netdbmobileminer_test/network_traffic.php", "network", "AndroidLogger", "Unuploaded_network", "Uploaded_network", "network", DataManager.getNetworkDataManager());
		LogManager.addLogInfo("http://140.112.42.22:7380/netdbmobileminer_test/activity.php", "activty", "AndroidLogger", "Unuploaded_activity", "Uploaded_activity", "activity", DataManager.getDailyActivityDataManager());
		//LogManager.addLogInfo("http://10.0.2.2/netdbmobileminer_test/", "log", "AndroidLogger", "Unuploaded", "Uploaded", "log", DataManager.getDefaultDataManager());
		//LogManager.addLogInfo("http://10.0.2.2/netdbmobileminer_test/network.php", "network", "AndroidLogger", "Unuploaded_network", "Uploaded_network", "network", DataManager.getNetworkDataManager());
		//LogManager.addLogInfo("http://10.0.2.2/netdbmobileminer_test/activity.php", "activty", "AndroidLogger", "Unuploaded_activity", "Uploaded_activity", "activity", DataManager.getDailyActivityDataManager());
		
		mLogMgr = new LogManager(this);			
		mLogMgr.checkExternalStorage("log");
		mLogMgr.createNewLog("log");
		mLogMgr.checkExternalStorage("network");
		mLogMgr.createNewLog("network");
		mLogMgr.checkExternalStorage("activity");
		mLogMgr.createNewLog("activity");
		
		//Access Local Database
		//DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "MobileLog", null);
        //db = helper.getWritableDatabase();
		
		//Database file is in the sdcard
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Log.i("onStartCommand", "service starting");
		/*Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);*/
		
		if(!isServiceRunning){
			Log.i("handleMessage", "The logging is not running...");
			isServiceRunning = true;
			 
			//在AlarmManager設定重覆執行的Server的intent
			mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_WRITE_TO_LOG), PendingIntent.FLAG_UPDATE_CURRENT);
			long triggerAtTime = SystemClock.elapsedRealtime() + recordFreq;
	        mAlarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, recordFreq, mPendingIntent);
	        
	        //Read acc. meter every 50 ms
	        mAccIntent = PendingIntent.getBroadcast(this, 0, new Intent(LoggingService.ACTION_ACCELEROMETER_READ), PendingIntent.FLAG_UPDATE_CURRENT);
			triggerAtTime = SystemClock.elapsedRealtime() + 50;
	        mAlarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME, triggerAtTime, 50, mAccIntent); 
	        
			Log.i("handleMessage", "The logging is now running!");
		} else {
			Log.i("handleMessage", "The logging is already running...");
		}
		
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
			LogManager.getLogInfoByName("log").getLogFos().close();
			LogManager.getLogInfoByName("network").getLogFos().close();
			LogManager.getLogInfoByName("activity").getLogFos().close();
			//mLogMgr.logFos.close();
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
				//Log after few seconds			
				boolean addedToTheQueue = mServiceHandler.postDelayed(writingToLogTask, recordFreq);					
				Log.v("writingToLogTask", "addedToTheQueue? " + String.valueOf(addedToTheQueue) );
				
				updateNetworkInfo();		
				updateProcessInfo();
				updateNetworkTrafficInfo();
				if(isLoggingAllowed){
					uploadLog();
					writeToLog();
					writeToNetworkLog();
				}				
			} else {				
				Log.v("writingToLogTask", "Finished. No more loggings!"); 
				mAlarmMgr.cancel(mPendingIntent);
			}
		}
	};

	//Write to the log when system alarm manager periodically notifies this service
	private BroadcastReceiver mWriteToLogReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action = intent.getAction();
			if(ACTION_WRITE_TO_LOG.equals(action)){
				
				if(isServiceRunning){					
					Log.v("WriteToLogReceiver", "Still logging!!" );
											
					updateNetworkInfo();		
					updateProcessInfo();
					updateNetworkTrafficInfo();
					  
					if(isLoggingAllowed){
						uploadLog();
						writeToLog();
						writeToNetworkLog();
					}				
				} else {				
					Log.v("WriteToLogReceiver", "Finished. No more loggings!"); 
					
				}
			}
		}		
	};

	private void registerServices(){
		
		registerReceiver(this.mWriteToLogReceiver, new IntentFilter(LoggingService.ACTION_WRITE_TO_LOG));
		registerReceiver(this.mAccReadReceiver, new IntentFilter(LoggingService.ACTION_ACCELEROMETER_READ));
		
		registerReceiver(mBatteryChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		registerReceiver(mScreenChangedReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
		registerReceiver(mScreenChangedReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF)); 
		registerReceiver(mDateChangedReceiver, new IntentFilter(Intent.ACTION_DATE_CHANGED));
		
		mLocMgr = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);		
		mLocMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_LOC_INTERVAL, MIN_LOC_DISTANCE, mLocationListener);
		mLocMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_LOC_INTERVAL, MIN_LOC_DISTANCE, mLocationListener);
		
		mTelMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		//mTelMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		mConnMgr = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		mActMgr = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);		
		
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangedListener);
		
		mAlarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
		
		mSensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor> sensorList = mSensorMgr.getSensorList(Sensor.TYPE_ACCELEROMETER); 
		if(sensorList.size() > 0){
			accelerometerPresent = true;
			accelerometerSensor = sensorList.get(0);
			mSensorMgr.registerListener(mAccelerometerListener, accelerometerSensor, 5);
		}
		else{
			accelerometerPresent = false;
		}
		
		sensorList = mSensorMgr.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
		if(sensorList.size() > 0){			
			mSensorMgr.registerListener(mLinearAccListener, sensorList.get(0), 5);
		}

	}
	
	private void unregisterServices(){
		unregisterReceiver(mBatteryChangedReceiver);
		unregisterReceiver(mDateChangedReceiver);
		unregisterReceiver(mScreenChangedReceiver);
		unregisterReceiver(mWriteToLogReceiver);
		
		mLocMgr.removeUpdates(mLocationListener);
		mSensorMgr.unregisterListener(mAccelerometerListener);
		mSensorMgr.unregisterListener(mLinearAccListener);
		
		//mTelMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);		
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangedListener);
	}
	
	//Change logging preference when modified by the user
	private OnSharedPreferenceChangeListener onSharedPreferenceChangedListener = new OnSharedPreferenceChangeListener(){

		@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			if(key.equals(SettingsActivity.PREFS_UPLOAD)){
				is3GUploadEnabled = sharedPreferences.getBoolean(key, false);
				Log.i("onSharedPreferenceChanged", "3G in service has been set to " + is3GUploadEnabled);
			}
			if(key.equals(SettingsActivity.PREFS_LOGGING)){
				isLoggingAllowed = sharedPreferences.getBoolean(key, false);
				Log.i("onSharedPreferenceChanged", "Allow logging in service has been set to " + isLoggingAllowed);
			}
	}};
	
	
	
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
	
	//Create new logs when a new day starts
	private BroadcastReceiver mDateChangedReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context,Intent intent){
			String action=intent.getAction();
			if(Intent.ACTION_DATE_CHANGED.equals(action)){				
				mLogMgr.createNewLog("log");
				mLogMgr.moveToExternalStorage("log");			
				mLogMgr.createNewLog("network");
				mLogMgr.moveToExternalStorage("network");
			}
		}
	};
	
	//adjust logging frequency according to whether the user activates the screen
	private BroadcastReceiver mScreenChangedReceiver = new BroadcastReceiver(){ 
		public void onReceive(Context context,Intent intent){
			String action=intent.getAction();
			Log.v("Android Logger",action);
			if(Intent.ACTION_SCREEN_ON.equals(action)){	
				isUsing = true;
				Log.v("LOGGER_Screen","SCREEN IS ON");
				recordFreq = HIGH_RECORD_FREQ;				
			}else if (Intent.ACTION_SCREEN_OFF.equals(action)){				
				Log.v("LOGGER_Screen","SCREEN IS OFF");
				isUsing = false;
				recordFreq = HIGH_RECORD_FREQ; 
			}
		}
	};
	protected ArrayList<Float> accXList = new ArrayList<Float>();
	protected ArrayList<Float> accYList = new ArrayList<Float>();
	protected ArrayList<Float> accZList = new ArrayList<Float>();
	
	//Record the accelerometer every 50 millisecond
	private BroadcastReceiver mAccReadReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action=intent.getAction();
			if(LoggingService.ACTION_ACCELEROMETER_READ.equals(action)){
				
				//TODO
				accXList.add(accX);
				accYList.add(accY);
				accZList.add(accZ);
				
				if( accXList.size() == 200 ){
					//mean
					float meanX = 0;
					for(Float x : accXList){
						meanX += x/200;
					}
					
					//standard deviation
					float stdX = 0;
					for(Float x : accXList){
						stdX += (x-meanX)*(x-meanX)/200;
					}
					stdX = (float) Math.sqrt(stdX);
					
					float absDiff = 0;
					for(Float x : accXList){
						absDiff += Math.abs(x-meanX)/200;
					}
					
					float avgAcc = 0;
					for(Float x : accXList){
						//TODO
					}
					
					//TODO time between peaks
					
					//TODO
					float [] binnedDist = new float[10];
					float minX = 10000;
					float maxX = -10000;

					for(Float x : accXList){
						if(x > maxX) maxX = x;
						if(x < minX) minX = x;
					}
					Log.i(ACTION_ACCELEROMETER_READ, "min: " + String.valueOf(minX));
					Log.i(ACTION_ACCELEROMETER_READ, "max: " + String.valueOf(maxX));
					
					for(Float x : accXList){
						int index = (int) Math.floor(10*(x-minX)/(maxX-minX));
						Log.i(ACTION_ACCELEROMETER_READ, "index: " + String.valueOf(index));
						if(index < 10) binnedDist[index] += 1/200.0;
						else binnedDist[9] += 1/200.0;
					}
					
					Log.i(ACTION_ACCELEROMETER_READ, String.valueOf(accXList.size()));
					Log.i(ACTION_ACCELEROMETER_READ, String.valueOf(meanX));
					Log.i(ACTION_ACCELEROMETER_READ, String.valueOf(stdX));
					
					for(int i=0; i<10; i++){
						Log.i(ACTION_ACCELEROMETER_READ, String.valueOf(i) + ": " + String.valueOf(binnedDist[i]));
					}
					
					accXList.clear();
					accYList.clear();
					accZList.clear();
					
				}
			}
		}		
	};
	
	private SensorEventListener mAccelerometerListener = new SensorEventListener(){
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			
		} 
		@Override
		public void onSensorChanged(SensorEvent event) {
			accX = event.values[0];
			accY = event.values[1];
			accZ = event.values[2];
		}
	};
	
	private SensorEventListener mLinearAccListener = new SensorEventListener(){
		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			
		} 
		@Override
		public void onSensorChanged(SensorEvent event) {
			linearAccX = event.values[0];
			linearAccY = event.values[1];
			linearAccZ = event.values[2];
		}
	};
	
				
	private LocationListener mLocationListener = new LocationListener(){
	    
		private static final int TWO_MINUTES = 1000 * 60 * 2;
		
		public void onLocationChanged(Location location) {
	    	if(location != null){
	    		//if (isBetterLocation(location, mLocation)){
	    			mLocation = location;
	    		//}
	    	} else {
	    		mLocation = null;
	    	}
	    }
		
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	    	Log.v("onStatusChanged", provider);
	    	Log.v("onStatusChanged", String.valueOf(status));	    	
	    	if(provider.equals("gps")){	    		
	    		gpsStatus = status;
	    		Log.v("onStatusChanged", String.valueOf(status));
	    	}
	    	if(provider.equals("network")){
	    		networkStatus = status;	 	    	
	    		Log.v("onStatusChanged", String.valueOf(status));
	    	}
	    }

	    public void onProviderEnabled(String provider) {
	    	Log.v("onProviderEnables", provider);
	    	if(provider.equals("gps")) isGPSProviderEnabled = true;
	    	if(provider.equals("network")) isNetworkProviderEnabled = true;
	    	
	    }

	    public void onProviderDisabled(String provider) {
	    	Log.v("onProviderDisables", provider);
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
	
	/*
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
	};*/
	
	private void updateNetworkInfo(){
		
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
	}
	
	private void updateNetworkTrafficInfo(){
		prevTraf = latestTraf;
		latestTraf = new TrafficSnapshot(this);
		
		/*
		if (prevTraf != null) {
			for(Object uid :  prevTraf.appNames.keySet().toArray()){
				Log.i("uid", String.valueOf(uid)); 
			}
			//delta_rx.setText(String.valueOf(latest.device.rx-previous.device.rx));
			//delta_tx.setText(String.valueOf(latest.device.tx-previous.device.tx));
		}*/
			
		/*
		HashSet<Integer> intersection = new HashSet<Integer>(latestTraf.apps.keySet());
		
		if (prevTraf != null) {
			intersection.retainAll(prevTraf.apps.keySet());
		}
		
		for (Integer uid : intersection) {
			TrafficRecord latest = latestTraf.apps.get(uid);
			TrafficRecord prev = ( prevTraf == null ? null : prevTraf.apps.get(uid));
			
			String appName = latestTraf.appNames.get(uid);
			
			if(prev != null && latest != null){				
				if(latest.rx - prev.rx > 0 || latest.tx - prev.tx > 0){
					Log.d("TrafficMonitor", "appName: " + appName + ", rx: " + String.valueOf(latest.rx - prev.rx));
					Log.d("TrafficMonitor", "appName: " + appName + ", tx: " + String.valueOf(latest.tx - prev.tx));
				}
			}
			//emitLog(latest_rec.tag, latest_rec, previous_rec, log);
		}*/
		
	}
  	
	private void updateProcessInfo(){
		
		//TODO running processes in the background
		processCurrentPackage = process2ndPackage = process3rdPackage = null;
		List<RunningTaskInfo> list = mActMgr.getRunningTasks(3);
		if( list.size() > 0 ){ 
			processCurrentPackage = list.get(0).topActivity.getPackageName();
			Log.v("processCurrentPackage", processCurrentPackage);
		}		
		if( list.size() > 1 ){ 
			process2ndPackage     = list.get(1).topActivity.getPackageName();
			Log.v("process2ndPackage", process2ndPackage);
		}
		if( list.size() > 2 ){ 
			process3rdPackage     = list.get(2).topActivity.getPackageName();
			Log.v("process3rdPackage", process3rdPackage);
		}
		
		
		//processCurrentClass = mActMgr.getRunningTasks(1).get(0).topActivity.getClassName();
		//Log.v("processCurrentClass", (processCurrentClass == null ? "null" : processCurrentClass));
		
		//List<RunningAppProcessInfo> list = mActMgr.getRunningAppProcesses();
		//Log.i("processName: ", list.get(0).processName);
		//Log.i("processName: ", list.get(1).processName);
		//Log.i("processName: ", list.get(2).processName);
		
		mActMgr.getMemoryInfo(memoryInfo);
		availMem = memoryInfo.availMem;				
		isLowMemory = memoryInfo.lowMemory;		
	}
	
	//Upload whenever there is network connection
	private void uploadLog(){
		updateNetworkInfo();
		if (isWifiConnected || (is3GUploadEnabled && isMobileConnected) ) {
			Intent uploadIntent = new Intent(this, UploadingService.class);
			startService(uploadIntent);
		}
	}
			
	private void writeToLog(){
		
		//Setting data for writing
		LogInfo logInfo = LogManager.getLogInfoByName("log");
		DataManager dataMgr = logInfo.getDataMgr();
		
		dataMgr.put(DataManager.DEVICE_ID, String.valueOf(deviceId));		
		Time now = new Time(Time.getCurrentTimezone());
		now.setToNow();
		
		String timeStr = String.valueOf(now.year) + "-" + String.valueOf(now.month+1) + "-" + String.valueOf(now.monthDay)  //Month = [0-11]
				+ " " + now.format("%T");
		
		dataMgr.put(DataManager.TIME, timeStr);
		dataMgr.put(DataManager.RECORD_FREQUENCY, String.valueOf(recordFreq));
		
		dataMgr.put(DataManager.BAT_STATUS, String.valueOf(batStatus));
		dataMgr.put(DataManager.BAT_PERCENTAGE, String.valueOf(batPct));
		
		dataMgr.put(DataManager.GPS_PROVIDER_STATUS, String.valueOf(gpsStatus));
		dataMgr.put(DataManager.NETWORK_PROVIDER_STATUS, String.valueOf(networkStatus));
		
		if(mLocation != null){
			dataMgr.put(DataManager.LOC_ACCURACY, String.valueOf(mLocation.getAccuracy()));
			dataMgr.put(DataManager.LOC_LATITUDE, String.valueOf(mLocation.getLatitude()));
			dataMgr.put(DataManager.LOC_LONGITUDE, String.valueOf(mLocation.getLongitude()));
			dataMgr.put(DataManager.LOC_PROVIDER, mLocation.getProvider());
			dataMgr.put(DataManager.LOC_SPEED, String.valueOf(mLocation.getSpeed()));			
		}
		else { 
			dataMgr.put(DataManager.LOC_ACCURACY, null);
			dataMgr.put(DataManager.LOC_LATITUDE, null);
			dataMgr.put(DataManager.LOC_LONGITUDE, null);
			dataMgr.put(DataManager.LOC_PROVIDER, null);
			dataMgr.put(DataManager.LOC_SPEED, null);
		}
		
		dataMgr.put(DataManager.MOBILE_STATE, String.valueOf(mobileState));
		dataMgr.put(DataManager.WIFI_STATE, String.valueOf(wifiState));
		
		dataMgr.put(DataManager.PROCESS_CURRENT_PACKAGE, String.valueOf(processCurrentPackage));
		dataMgr.put(DataManager.IS_LOW_MEMORY, String.valueOf(isLowMemory));
		dataMgr.put(DataManager.IS_USING, String.valueOf(isUsing));
		
		//Write to the log
		try {
			String toWrite = dataMgr.toString();			
			if (logInfo.getLogFos() != null){
				logInfo.getLogFos().write(toWrite.getBytes());
			}
		} catch (IOException e) {
			Log.e("writeToLogFile", "Cannot write into the file: " + logInfo.getLogFilename());			
		}		
		Log.v("writeToLogFile", "The file is " + getFilesDir() + "/" + logInfo.getLogFilename());
		Log.v("writeToLogFile", "Write Successfully!");
		
		//Write to the local database
		writeToDatabase();
	}
	
	
	private void writeToNetworkLog(){
		
		//Setting data for writing
		LogInfo logInfo = LogManager.getLogInfoByName("network");
		DataManager dataMgr = logInfo.getDataMgr();
		
		HashSet<Integer> intersection = new HashSet<Integer>(latestTraf.apps.keySet());
		
		if (prevTraf != null) {
			intersection.retainAll(prevTraf.apps.keySet());
		}
		
		for (Integer uid : intersection) {
			TrafficRecord latest = latestTraf.apps.get(uid);
			TrafficRecord prev = ( prevTraf == null ? null : prevTraf.apps.get(uid));
			
			String appName = latestTraf.appNames.get(uid);
			
			if(prev != null && latest != null){				
				if(latest.rx - prev.rx > 0 || latest.tx - prev.tx > 0){
					
					Log.v("TrafficMonitor", "appName: " + appName + ", rx: " + String.valueOf(latest.rx - prev.rx));
					Log.v("TrafficMonitor", "appName: " + appName + ", tx: " + String.valueOf(latest.tx - prev.tx));
					
					dataMgr.put(DataManager.DEVICE_ID, String.valueOf(deviceId));
					Time now = new Time(Time.getCurrentTimezone());
					now.setToNow();
					
					String timeStr = String.valueOf(now.year) + "-" + String.valueOf(now.month+1) + "-" + String.valueOf(now.monthDay)  //Month = [0-11]
							+ " " + now.format("%T");
					
					dataMgr.put(DataManager.TIME, timeStr);
					dataMgr.put(DataManager.RECORD_FREQUENCY, String.valueOf(recordFreq));
					
					dataMgr.put(DataManager.MOBILE_STATE, String.valueOf(mobileState));
					dataMgr.put(DataManager.WIFI_STATE, String.valueOf(wifiState));
					dataMgr.put(DataManager.TRANSMITTED_BYTE, String.valueOf(latest.tx - prev.tx));
					dataMgr.put(DataManager.RECEIVED_BYTE, String.valueOf(latest.rx - prev.rx));
					dataMgr.put(DataManager.APP_NAME, appName);
					dataMgr.put(DataManager.IS_USING, String.valueOf(isUsing));
					
					//Write to the log
					try {
						String toWrite = dataMgr.toString();
						if (logInfo.getLogFos() != null){
							logInfo.getLogFos().write(toWrite.getBytes());
						}
					} catch (IOException e) {
						Log.e("writeToLogFile", "Cannot write into the file: " + logInfo.getLogFilename());			
					}		
					Log.v("writeToLogFile", "The file is " + getFilesDir() + "/" + logInfo.getLogFilename());
					Log.v("writeToLogFile", "Write Successfully!");
										
					//Insert into the local database
					Log.v("Database", "Starting to insert new NetworkLog");	 
					NetworkLog log = new NetworkLog(null, deviceId, new Date(), new Date().getDay(), new Date().getHours(), recordFreq, mobileState.toString(), wifiState.toString(), latest.tx - prev.tx, latest.rx - prev.rx, appName, isUsing);        
			        networkLogDao.insert(log);			        
			        Log.v("Database", "Inserted new networkLog, ID: " + log.getId());
			        Log.v("Database", "Size: " + String.valueOf((double)new File(db.getPath()).length()/1024.0) + "kB");
					
				}
			}
		}
		
		
	}
	
	private void writeToDatabase(){
		
		Log.v("Database", "Starting to insert new MobileLog");
		        
        MobileLog mobileLog = null;
        if(mLocation != null){
	        mobileLog = new MobileLog(null, deviceId, new Date(), new Date().getDay(), new Date().getHours(), recordFreq, batStatus, batPct, gpsStatus, networkStatus, 
	        		mobileState.toString(), wifiState.toString(), processCurrentPackage, isLowMemory, isUsing, (double) mLocation.getAccuracy(), mLocation.getLatitude(), mLocation.getLongitude(), mLocation.getProvider(), 
	        		(double) mLocation.getSpeed());
        }
        else{
	        mobileLog = new MobileLog(null, deviceId, new Date(), new Date().getDay(), new Date().getHours(), recordFreq, batStatus, batPct, gpsStatus, networkStatus, 
	        		mobileState.toString(), wifiState.toString(), processCurrentPackage, isLowMemory, isUsing, null, null, null, null, null); 
        }
 
        mobileLogDao.insert(mobileLog);
        Log.v("Database", "Inserted new MobileLog, ID: " + mobileLog.getId());
        Log.v("Database", "Size: " + String.valueOf((double)new File(db.getPath()).length()/1024.0) + "kB");
        
	};
	
	/*
	private void writeToNetworkDatabase(){
        
        HashSet<Integer> intersection = new HashSet<Integer>(latestTraf.apps.keySet());
		
		if (prevTraf != null) {
			intersection.retainAll(prevTraf.apps.keySet());
		}
		
		for (Integer uid : intersection) {
			
			Log.v("Database", "Starting to insert new MobileLog");	        
	        NetworkLog log = null;
			
			TrafficRecord latest = latestTraf.apps.get(uid);
			TrafficRecord prev = ( prevTraf == null ? null : prevTraf.apps.get(uid));
			
			String appName = latestTraf.appNames.get(uid);
			
			if(prev != null && latest != null){				
				if(latest.rx - prev.rx > 0 || latest.tx - prev.tx > 0){
					
					Log.v("TrafficMonitor", "appName: " + appName + ", rx: " + String.valueOf(latest.rx - prev.rx));
					Log.v("TrafficMonitor", "appName: " + appName + ", tx: " + String.valueOf(latest.tx - prev.tx));
					
			        log = new NetworkLog(null, deviceId, new Date(), new Date().getDay(), new Date().getHours(), recordFreq, mobileState.toString(), wifiState.toString(), latest.tx - prev.tx, latest.rx - prev.rx, appName, isUsing);        
			        networkLogDao.insert(log);
			        
			        Log.v("Database", "Inserted new networkLog, ID: " + log.getId());
			        Log.v("Database", "Size: " + String.valueOf((double)db.getPageSize()/1024) + "kB");
					
				}
			}
		}
	}*/
	
	
}
	
	
	
	
