package edu.ntu.arbor.sbchao.androidlogger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import edu.ntu.arbor.sbchao.androidlogger.LoggingService.LocalBinder;

public class MobileStatusActivity extends Activity {
	private static final int UPDATE_INFO = 0x0000;
	private static final int ENABLE_LOCATION_PROVIDER = 0x0001;
	
	private boolean mBound = false;	
	private LoggingService mService = null;
	private ThreadHandler mHandler = new ThreadHandler();
	
	private static int UPDATE_FREQUENCY = 1000;
	
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
					mHandler.postDelayed(updateInfoThread, UPDATE_FREQUENCY);
					break;			
				case ENABLE_LOCATION_PROVIDER:
					if(mBound && (!mService.isGPSProviderEnabled || !mService.isMobileConnected)){
						showEnableLocationDialog();
					} else {
						mHandler.postDelayed(enableLocationProviderThread, UPDATE_FREQUENCY);
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
        setContentView(R.layout.activity_mobile_status);
         
        addUiListeners();

        //Start LoggingService
        Intent intent = new Intent(MobileStatusActivity.this, LoggingService.class);
        startService(intent);
        mHandler.postDelayed(enableLocationProviderThread, 0);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LoggingService
        Intent intent = new Intent(MobileStatusActivity.this, LoggingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);  
        
        //Update info on UI
        mHandler.postDelayed(updateInfoThread, 0);
        
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

    private void addUiListeners(){
        
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
    }
    
    //Display current status of the mobile
    private void updateStatus(){
    	
    	if(mBound) {
            textApp.setText(String.valueOf(mService.processCurrentPackage));
            textApp2.setText(String.valueOf(mService.process2ndPackage));
            textApp3.setText(String.valueOf(mService.process3rdPackage));
            textBatLevel.setText(String.valueOf(mService.batLevel));
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

}
