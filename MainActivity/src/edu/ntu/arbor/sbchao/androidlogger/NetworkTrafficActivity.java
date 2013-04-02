package edu.ntu.arbor.sbchao.androidlogger;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.LoggingService.LocalBinder;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster.DevOpenHelper;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao.Properties;

public class NetworkTrafficActivity extends Activity {
	
	private static final int UPDATE_NETWORK = 0x0002;
	
	private TextView text3GTodayTx;
	private TextView text3GTodayRx;
	private TextView text3GTodayTotal;
	private TextView textWifiTodayTx;
	private TextView textWifiTodayRx;
	private TextView textWifiTodayTotal;
	private TextView text3GWeekTx;
	private TextView text3GWeekRx;
	private TextView text3GWeekTotal;
	private TextView text3GMonthTx;
	private TextView text3GMonthRx;
	private TextView text3GMonthTotal;
	private TextView textWifiWeekTx;
	private TextView textWifiWeekRx;
	private TextView textWifiWeekTotal;
	private TextView textWifiMonthTx;
	private TextView textWifiMonthRx;
	private TextView textWifiMonthTotal;
	
	private boolean mBound = false;	
	private LoggingService mService = null;
	private ThreadHandler mHandler = new ThreadHandler();
	
	//Local database
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private MobileLogDao mobileLogDao;
	private NetworkLogDao networkLogDao;
	
	private static int UPDATE_FREQUENCY = 10000;
	
	private boolean displayed = false;
	
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

	private ProgressDialog mDialog;
	
    private final class ThreadHandler extends Handler {
	    public ThreadHandler() {
            super();
        }
		@Override
		public void handleMessage(Message msg) {						
			switch(msg.what){
				case UPDATE_NETWORK:
					if(mBound){						
						new UpdateNetworkTrafficTask().execute();
					}
					mHandler.postDelayed(updateNetworkThread, UPDATE_FREQUENCY);
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
        setContentView(R.layout.activity_network_traffic);
         
        addUiListeners();

        //Start LoggingService
        Intent intent = new Intent(NetworkTrafficActivity.this, LoggingService.class);
        startService(intent);
                        
        //Ask user to enable location providers
        //mHandler.postDelayed(enableLocationProviderThread, 1000);
        
        //Set up local databases
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
    protected void onStart() {
        super.onStart();
        // Bind to LoggingService
        Intent intent = new Intent(NetworkTrafficActivity.this, LoggingService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);  
        
        //Update info on UI
        mHandler.postDelayed(updateNetworkThread, 0); 
        if(!displayed){
			mDialog = new ProgressDialog(NetworkTrafficActivity.this);
	        mDialog.setMessage("Loading network traffic statistics...");
	        mDialog.setCancelable(false);
	        mDialog.show();
        }
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
    
    private Runnable updateNetworkThread = new Runnable(){
		@Override
		public void run() {
			try {				
				Message msg = new Message();
				msg.what = UPDATE_NETWORK;
				mHandler.sendMessage(msg);
			}  
			catch(Exception e) {
				e.printStackTrace();
				Log.e("UpdateInfoThread", e.getMessage());
			}
		}
    };

    private void addUiListeners(){     
           
        text3GTodayTx = (TextView) findViewById(R.id.text_3G_today_tx);
        text3GTodayRx = (TextView) findViewById(R.id.text_3G_today_rx);
        text3GTodayTotal = (TextView) findViewById(R.id.text_3G_today_total);
        
        text3GWeekTx = (TextView) findViewById(R.id.text_3G_week_tx);
        text3GWeekRx = (TextView) findViewById(R.id.text_3G_week_rx);
        text3GWeekTotal = (TextView) findViewById(R.id.text_3G_week_total);
        
        text3GMonthTx = (TextView) findViewById(R.id.text_3G_month_tx);
        text3GMonthRx = (TextView) findViewById(R.id.text_3G_month_rx);
        text3GMonthTotal = (TextView) findViewById(R.id.text_3G_month_total);
        
        textWifiTodayTx = (TextView) findViewById(R.id.text_wifi_today_tx);
        textWifiTodayRx = (TextView) findViewById(R.id.text_wifi_today_rx);
        textWifiTodayTotal = (TextView) findViewById(R.id.text_wifi_today_total);
        
        textWifiWeekTx = (TextView) findViewById(R.id.text_wifi_week_tx);
        textWifiWeekRx = (TextView) findViewById(R.id.text_wifi_week_rx);
        textWifiWeekTotal = (TextView) findViewById(R.id.text_wifi_week_total);  

        textWifiMonthTx = (TextView) findViewById(R.id.text_wifi_month_tx);
        textWifiMonthRx = (TextView) findViewById(R.id.text_wifi_month_rx);
        textWifiMonthTotal = (TextView) findViewById(R.id.text_wifi_month_total);         
    }
	
    private class UpdateNetworkTrafficTask extends AsyncTask<Void, Void, Void>{
    	
    	private long byte3GTodayTx;
    	private long byte3GTodayRx;
    	private long byteWifiTodayTx;
    	private long byteWifiTodayRx;
    	private long byte3GWeekTx;
    	private long byte3GWeekRx;
    	private long byte3GMonthTx;
    	private long byte3GMonthRx;
    	private long byteWifiWeekTx;
    	private long byteWifiWeekRx;
    	private long byteWifiMonthTx;
    	private long byteWifiMonthRx;
    	
    	@Override
    	protected void onPostExecute(Void result){
	        text3GTodayRx.setText(toBytes(byte3GTodayRx));
	        text3GTodayTx.setText(toBytes(byte3GTodayTx));
	        text3GTodayTotal.setText(toBytes(byte3GTodayRx+byte3GTodayTx));
	        textWifiTodayRx.setText(toBytes(byteWifiTodayRx));
	        textWifiTodayTx.setText(toBytes(byteWifiTodayTx));
	        textWifiTodayTotal.setText(toBytes(byteWifiTodayRx+byteWifiTodayTx));
	        text3GWeekRx.setText(toBytes(byte3GWeekRx));
	        text3GWeekTx.setText(toBytes(byte3GWeekTx));
	        text3GWeekTotal.setText(toBytes(byte3GWeekRx+byte3GWeekTx));
	        textWifiWeekRx.setText(toBytes(byteWifiWeekRx));
	        textWifiWeekTx.setText(toBytes(byteWifiWeekTx));
	        textWifiWeekTotal.setText(toBytes(byteWifiWeekRx+byteWifiWeekTx));
	        text3GMonthRx.setText(toBytes(byte3GMonthRx));
	        text3GMonthTx.setText(toBytes(byte3GMonthTx));
	        text3GMonthTotal.setText(toBytes(byte3GMonthRx+byte3GMonthTx));
	        textWifiMonthRx.setText(toBytes(byteWifiMonthRx));
	        textWifiMonthTx.setText(toBytes(byteWifiMonthTx));
	        textWifiMonthTotal.setText(toBytes(byteWifiMonthRx+byteWifiMonthTx));
	        
	        if(!displayed){
	        	displayed = true;
	        	mDialog.dismiss();
	        }
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			if(mBound){		
				
				Log.i("doInBackgound", "start!");
		        QueryBuilder<NetworkLog> qb = networkLogDao.queryBuilder();
		    
		        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getTodayStartDate())));	        
		        List<NetworkLog> logs = qb.list();
		        for(NetworkLog log : logs){        	
		        	byte3GTodayRx += log.getReceivedByte();
		        	byte3GTodayTx += log.getTransmittedByte();
		        }	        	        

				//Thread.sleep(100);
				
		        //WiFi network traffic
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getTodayStartDate())));
		        logs = qb.list();
		        for(NetworkLog log : logs){	
		        	byteWifiTodayRx += log.getReceivedByte();
		        	byteWifiTodayTx += log.getTransmittedByte();
		        }	        	        

		        //Thread.sleep(100);
		        
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getWeekStartDate())));	        
		        logs = qb.list();
		        Log.d("updateNetworkTraffic", "log list size:" + logs.size());
		        for(NetworkLog log : logs){        	
		        	byte3GWeekRx += log.getReceivedByte();
		        	byte3GWeekTx += log.getTransmittedByte();
		        }	        	        
		        
		        //WiFi network traffic
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getWeekStartDate())));
		        logs = qb.list();
		        for(NetworkLog log : logs){
		        	byteWifiWeekRx += log.getReceivedByte();
		        	byteWifiWeekTx += log.getTransmittedByte();
		        }	        	        
		        
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.MobileState.eq("CONNECTED"), Properties.Time.ge(getMonthStartDate())));	        
		        logs = qb.list();
		        for(NetworkLog log : logs){	        	
		        	byte3GMonthRx += log.getReceivedByte();
		        	byte3GMonthTx += log.getTransmittedByte();
		        }	        	        

		        //WiFi network traffic
		        qb = networkLogDao.queryBuilder();
		        qb.where(qb.and(Properties.WifiState.eq("CONNECTED"), Properties.Time.ge(getMonthStartDate())));
		        logs = qb.list();
		        for(NetworkLog log : logs){	
		        	byteWifiMonthRx += log.getReceivedByte();
		        	byteWifiMonthTx += log.getTransmittedByte();
		        }	        	        

		        Log.i("doInBackgound", "finish!!");
			}
			return null;
		}
		
		private String toBytes(long bytes){
			if(bytes < 1024){return String.valueOf(bytes) + "Bytes";}
			else if (bytes >= 1024 && bytes < 1024*1024){
				return String.format("%.1f", bytes/1024.0) + "KB";
			}
			else if (bytes >= 1024*1024 && bytes < 1024*1024*1024){
				return String.format("%.1f", bytes/(1024.0*1024)) + "MB";
			}
			else if (bytes >= 1024*1024*1024 && bytes < 1024*1024*1024*1024){
				return String.format("%.1f", bytes/(1024.0*1024*1024)) + "GB";
			}
			else if (bytes >= 1024*1024*1024*1024){
				return String.format("%.1f", bytes/(1024.0*1024*1024*1024)) + "TB";
			}
			else return "";
		}
		
		private Date getWeekStartDate(){
			Calendar c = Calendar.getInstance();
	        c.clear();
	        c.setTime(new Date());
	        c.clear(Calendar.HOUR);
	        c.clear(Calendar.HOUR_OF_DAY);
	        c.clear(Calendar.MINUTE);
	        c.clear(Calendar.SECOND);
	        c.clear(Calendar.MILLISECOND);
	        c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
	       
	        return c.getTime();
		}
	        
		private Date getTodayStartDate(){    
			Calendar c = Calendar.getInstance();
	        c.clear();
	        c.setTime(new Date());
	        c.clear(Calendar.HOUR);
	        c.clear(Calendar.HOUR_OF_DAY);
	        c.clear(Calendar.MINUTE);
	        c.clear(Calendar.SECOND);
	        c.clear(Calendar.MILLISECOND);
	        
	        return c.getTime();
		}
		 
		private Date getMonthStartDate(){
			Calendar c = Calendar.getInstance();
			c.clear();
		    c.setTime(new Date());
		    c.set(Calendar.DAY_OF_MONTH, 1);
		    c.clear(Calendar.HOUR);
		    c.clear(Calendar.HOUR_OF_DAY);
		    c.clear(Calendar.MINUTE);
		    c.clear(Calendar.SECOND);
		    c.clear(Calendar.MILLISECOND);
		    
		    return c.getTime();
		}
	};
    
}
