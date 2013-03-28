package edu.ntu.arbor.sbchao.androidlogger.logmanager;

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
	public static final String IS_USING = "isUsing";
	public static final String HOUR_OF_DAY = "hourOfDay";
	public static final String DAY_OF_WEEK = "dayOfWeek";
	public static final String APP_NAME = "appName";
	public static final String TRANSMITTED_BYTE = "transmittedByte";
	public static final String RECEIVED_BYTE = "receivedByte";
	
	public static DataManager getDefaultDataManager(){
		DataManager mgr = new DataManager();
		mgr.reg(DEVICE_ID);		
		mgr.reg(TIME);
		mgr.reg(RECORD_FREQUENCY);
		
	    //Battery information
		mgr.reg(BAT_STATUS);
		mgr.reg(BAT_PERCENTAGE);
		
		//Location Information		
		mgr.reg(GPS_PROVIDER_STATUS);
		mgr.reg(NETWORK_PROVIDER_STATUS);
		mgr.reg(LOC_ACCURACY);
	    mgr.reg(LOC_PROVIDER);
	    mgr.reg(LOC_LATITUDE);
	    mgr.reg(LOC_LONGITUDE);	        
	    mgr.reg(LOC_SPEED);	    
		
		//network status		
	    mgr.reg(MOBILE_STATE);		
	    mgr.reg(WIFI_STATE);
		
	    mgr.reg(PROCESS_CURRENT_PACKAGE);			
	    mgr.reg(IS_LOW_MEMORY);
	    mgr.reg(IS_USING);
		return mgr;
	}
	
	public static DataManager getNetworkDataManager(){
		DataManager mgr = new DataManager();
		mgr.reg(DEVICE_ID);
		mgr.reg(TIME);
		mgr.reg(RECORD_FREQUENCY);
		mgr.reg(MOBILE_STATE);		
		mgr.reg(WIFI_STATE);
		mgr.reg(TRANSMITTED_BYTE);
		mgr.reg(RECEIVED_BYTE);
		mgr.reg(APP_NAME);
		mgr.reg(IS_USING);
		return mgr;
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
		
	
	
	public boolean put(String columnName, String value){
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
	
	/*
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
		reg(IS_USING);
	}*/
	
}