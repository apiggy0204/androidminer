package edu.ntu.arbor.sbchao.androidlogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import edu.ntu.arbor.sbchao.androidlogger.LoggingService.LocalBinder;

public class MainActivity extends Activity {
	
	private Button buttonQuit;
	private Button buttonRefresh;
	private Button buttonStopLogging;
	private TextView textInfo;
	
	boolean mBound = false;
	LoggingService mService = null;
	
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
        
        setListeners();

        //Start LoggingService
        Intent intent = new Intent(MainActivity.this, LoggingService.class);
        startService(intent);
        
        //Ask user to enable location providers
        showEnableLocationDialog();
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
    
    private void setListeners(){
        
        buttonQuit = (Button) findViewById(R.id.button_quit);        
        buttonQuit.setOnClickListener(quitListener);
        buttonRefresh = (Button) findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(refreshListener);
        buttonStopLogging = (Button) findViewById(R.id.button_stop_logging);
        buttonStopLogging.setOnClickListener(stopLoggingListener);
        textInfo = (TextView) findViewById(R.id.text_info);
              
    }
    
    //Display current status of the mobile
    private void displayInfo(){
    	/*if(mBound == false){
    		Intent intent = new Intent(MainActivity.this, LoggingService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);            
    	} */ 
    	
    	String toWrite = "Current Process: " + String.valueOf(mService.processCurrentPackage) + "\n"
    					+ "batLevel: " + String.valueOf(mService.batLevel) + "\n"
    					+ "batStatus: " + String.valueOf(mService.batStatus) + "\n"
    					+ "GPSProviderStatus: " + String.valueOf(mService.GPSProviderStatus) + "\n"
    					+ "networkProviderStatus: " + String.valueOf(mService.networkProviderStatus) + "\n"
    					+ "3G network status: " + String.valueOf(mService.mobileState)
    					+ "wifi network status: " + String.valueOf(mService.wifiState)
    					+ "isLowMemory: " + String.valueOf(mService.isLowMemory);
    	
    	if(mService.mLocation != null){
    		toWrite += "locAccuracy: " + String.valueOf(mService.mLocation.getAccuracy()) + "\n"
					+ "locLatitude: " + String.valueOf(mService.mLocation.getLatitude()) + "\n"
					+ "locLongitude: " + String.valueOf(mService.mLocation.getLongitude()) + "\n"
					+ "locProvider: " + String.valueOf(mService.mLocation.getProvider()) + "\n"
					+ "locSpeed: " + String.valueOf(mService.mLocation.getSpeed()) + "\n";
    	} else {
    		toWrite += "location: no location available!";
    	}

    	textInfo.setText(toWrite);
    }  
    
    private void showEnableLocationDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("開啟定位服務");		
		builder.setMessage("這個程式可以記錄你的位置資訊。要開啟利用網路或GPS查詢位置的功能嗎?");
		builder.setPositiveButton("開啟", new DialogInterface.OnClickListener() {
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
    
}
