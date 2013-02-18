package edu.ntu.arbor.sbchao.androidlogger;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
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
	private TextView textHello;
	
	boolean mBound = false;
	LoggingService mService;
	
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
        buttonQuit = (Button) findViewById(R.id.button_quit);        
        buttonQuit.setOnClickListener(quitListener);
        buttonRefresh = (Button) findViewById(R.id.button_refresh);
        buttonRefresh.setOnClickListener(refreshListener);
        buttonStopLogging = (Button) findViewById(R.id.button_stop_logging);
        buttonStopLogging.setOnClickListener(stopLoggingListener);
        textHello = (TextView) findViewById(R.id.text_hello);        
        
        //Start LoggingService
        Intent intent = new Intent(MainActivity.this, LoggingService.class);
        startService(intent);                
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
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
    
    //Display current status of the mobile
    private void displayInfo(){
    	textHello.setText("你的電量還有" + String.valueOf(mService.batLevel));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
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
