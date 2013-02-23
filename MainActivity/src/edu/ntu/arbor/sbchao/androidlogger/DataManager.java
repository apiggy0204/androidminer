package edu.ntu.arbor.sbchao.androidlogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.net.NetworkInfo;
import android.text.format.Time;
import android.util.Log;

public class DataManager {
	
	private ArrayList<String> mColumnNameList = new ArrayList<String>();
	private Map<String, String> mMap = new HashMap<String, String>();
	
	public static final String DEVICE_ID = "deviceId";	
	public static final String BAT_LEVEL = "batLevel";
	public static final String BAT_SCALE = "batScale";
	public static final String BAT_VOLTAGE = "batVoltage";
	public static final String BAT_STATUS = "batStatus";
	public static final String BAT_PLUGGED = "batPlugged";
	public static final String BAT_PERCENTAGE = "batPct";
	public static final String IS_GPS_ENABLED = "isGPSProviderEnabled";
	public static final String IS_NETWORK_ENABLED = "isNetworkProviderEnabled";
	public static final String GPS_PROVIDER_STATUS = "gpsStatus";
	public static final String NETWORK_PROVIDER_STATUS = "networkStatus";
	public static final String LOC_ACCURACY = "locAcc";
	public static final String LOC_PROVIDER = "locProvider";
	public static final String LOC_ALTITUDE = "alt";
	public static final String LOC_LATITUDE = "lat";
	public static final String LOC_LONGITUDE = "lon";
	public static final String LOC_SPEED = "speed";
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
	public static final String RECORD_FREQUENCY = "recordFreq";
	
	public DataManager(){
		registerColumnNames();
	}
	
	public int getColumnCount(){
		return mColumnNameList.size();
	}
	
	public String getColumnName(int index){
		return mColumnNameList.get(index);
		
	}
	
	private void reg(String columnName){
		this.mColumnNameList.add(columnName);		
	}
		
	private void registerColumnNames(){
		reg(DEVICE_ID);		
		reg(TIME);
		reg(RECORD_FREQUENCY);
		
	    //Battery information
		//reg(BAT_LEVEL);
		//reg(BAT_SCALE);
		//reg(BAT_VOLTAGE);
		reg(BAT_STATUS);
		//reg(BAT_PLUGGED);
		reg(BAT_PERCENTAGE);
		
		//Location Information		
		//reg(IS_GPS_ENABLED);
		//reg(IS_NETWORK_ENABLED);
		reg(GPS_PROVIDER_STATUS);
		reg(NETWORK_PROVIDER_STATUS);
	    reg(LOC_ACCURACY);
	    reg(LOC_PROVIDER);
	    //reg(LOC_ALTITUDE);
	    reg(LOC_LATITUDE);
	    reg(LOC_LONGITUDE);	        
	    reg(LOC_SPEED);	    
		
	    //calling status	    
		//reg(CALL_STATE);
		//reg(INCOMING_NUMBER);
		
		//network status		
		//reg(CONNECTIVITY);
		//reg(ACTIVE_NETWORK_TYPE);
		//reg(IS_MOBILE_AVAILABLE);
		//reg(IS_MOBILE_CONNECTED);
		//reg(IS_MOBILE_FAILOVER);
		//reg(IS_MOBILE_ROAMING);
		reg(MOBILE_STATE);		
		//reg(IS_WIFI_AVAILABLE);		
		//reg(IS_WIFI_CONNECTED);
		//reg(IS_WIFI_FAILOVER);
		//reg(IS_WIFI_ROAMING);
		reg(WIFI_STATE);
		
		//reg(PROCESS_CURRENT_CLASS);
		reg(PROCESS_CURRENT_PACKAGE);
		//reg(AVAIL_MEM);			
		reg(IS_LOW_MEMORY);
	}
	
	public boolean set(String columnName, String value){
		if (mColumnNameList.contains(columnName)){
			mMap.put(columnName, value);
			return true;
		}
		else return false; 		
	}
	
	public String get(String columnName){
		if (mColumnNameList.contains(columnName)){
			return mMap.get(columnName);			
		}
		else return null;
	}
	
	public String toString(){
		String str = "";
		for (String columnName : mColumnNameList){
			String value = mMap.get(columnName);			
			if( value == null || value.equals("null")){
				str += "\t";
			} 
			else {
				str += value + "\t";
			}
		}
		str += "\n";
		return str;
	}
	
}
