package edu.ntu.arbor.sbchao.androidlogger;

import java.util.ArrayList;

import android.net.NetworkInfo;
import android.text.format.Time;

public class DataManager {
	
	private ArrayList<String> columnNameList = new ArrayList<String>();
	
	public static final String DEVICE_ID = "deviceId";	
	public static final String BAT_LEVEL = "batLevel";
	public static final String BAT_SCALE = "batScale";
	public static final String BAT_VOLTAGE = "batVoltage";
	public static final String BAT_STATUS = "batStatus";
	public static final String BAT_PLUGGED = "batPlugged";
	public static final String BAT_PERCENTAGE = "batPercentage";
	public static final String IS_GPS_ENABLED = "isGPSProviderEnabled";
	public static final String IS_NETWORK_ENABLED = "isNetworkProviderEnabled";
	public static final String GPS_PROVIDER_STATUS = "GPSProviderStatus";
	public static final String NETWORK_PROVIDER_STATUS = "networkProviderStatus";
	public static final String LOC_ACCURACY = "locAccuracy";
	public static final String LOC_PROVIDER = "locProvider";
	public static final String LOC_ALTITUDE = "locAltitude";
	public static final String LOC_LATITUDE = "locLatitude";
	public static final String LOC_LONGITUDE = "locLongitude";
	public static final String LOC_SPEED = "locSpeed";
	public static final String CALL_STATE = "callState";
	public static final String INCOMING_NUMBER = "incomingNumber";
	public static final String CONNECTIVITY = "connectivity";
	public static final String ACTIVE_NETWORK_TYPE = "activeNetworkType";
	public static final String IS_MOBILE_AVAILABLE = "isMobileAvailable";
	public static final String IS_MOBILE_CONNECTED = "isMobileConnected";
	public static final String IS_MOBILE_FAILOVER = "isMobileFailover";
	public static final String IS_MOBILE_ROAMING = "isMobileRoaming";
	public static final String MOBILE_STATE = "mobileState";
	public static final String IS_WIFI_AVAILABLE = "isWifiAvailable";
	public static final String IS_WIFI_CONNECTED = "isWifiConnected";
	public static final String IS_WIFI_FAILOVER = "isWifiFailover";
	public static final String IS_WIFI_ROAMING = "isWifiRoaming";
	public static final String WIFI_STATE = "wifiState";
	public static final String PROCESS_CURRENT_CLASS = "processCurrentClass";
	public static final String PROCESS_CURRENT_PACKAGE = "processCurrentPackage";
	public static final String AVAIL_MEM = "availMem";
	public static final String IS_LOW_MEMORY = "isLowMemory";	
	public static final String TIME = "time";
	
	
	//Data
	String deviceId;
	Time now;
	
    //Battery information
	int batLevel;
	int batScale;
	int batVoltage;
	int batStatus;
	int batPlugged;
	double batPercentage;
	
	//Location Information	
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
	
    //calling status
	int callState; //CALL_STATE_IDLE, CALL_STATE_RINGING or CALL_STATE_OFFHOOK; see http://developer.android.com/reference/android/telephony/TelephonyManager.html
	String inNumber;
	
	//network status
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
		
	String processCurrentClass;
	String processCurrentPackage;			
	long availMem;		
	boolean isLowMemory;
	
	public DataManager(){
		registerColumnNames();
	}
	
	
	public int getColumnCount(){
		return columnNameList.size();
	}
	
	public String getColumnName(int index){
		return columnNameList.get(index);
		
	}
	
	private void reg(String columnName){
		this.columnNameList.add(columnName);		
	}
		
	private void registerColumnNames(){
		reg(DEVICE_ID);		
		reg(TIME);
		
	    //Battery information
		reg(BAT_LEVEL);
		reg(BAT_SCALE);
		reg(BAT_VOLTAGE);
		reg(BAT_STATUS);
		reg(BAT_PLUGGED);
		reg(BAT_PERCENTAGE);
		
		//Location Information		
		reg(IS_GPS_ENABLED);
		reg(IS_NETWORK_ENABLED);
		reg(GPS_PROVIDER_STATUS);
		reg(NETWORK_PROVIDER_STATUS);
	    reg(LOC_ACCURACY);
	    reg(LOC_PROVIDER);
	    reg(LOC_ALTITUDE);
	    reg(LOC_LATITUDE);
	    reg(LOC_LONGITUDE);	        
	    reg(LOC_SPEED);	    
		
	    //calling status	    
		reg(CALL_STATE);
		reg(INCOMING_NUMBER);
		
		//network status		
		reg(CONNECTIVITY);
		reg(ACTIVE_NETWORK_TYPE);
		reg(IS_MOBILE_AVAILABLE);
		reg(IS_MOBILE_CONNECTED);
		reg(IS_MOBILE_FAILOVER);
		reg(IS_MOBILE_ROAMING);
		reg(MOBILE_STATE);		
		reg(IS_WIFI_AVAILABLE);		
		reg(IS_WIFI_CONNECTED);
		reg(IS_WIFI_FAILOVER);
		reg(IS_WIFI_ROAMING);
		reg(WIFI_STATE);
		
		reg(PROCESS_CURRENT_CLASS);
		reg(PROCESS_CURRENT_PACKAGE);
		reg(AVAIL_MEM);			
		reg(IS_LOW_MEMORY);
	}
	
}
