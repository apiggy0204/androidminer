package edu.ntu.arbor.sbchao.androidlogger;


import java.io.IOException;
import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import edu.ntu.arbor.sbchao.androidlogger.LoggingService.LocalBinder;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DataManager;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DatabaseManager;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.LogInfo;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.LogManager;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;

public class DiaryInputActivity extends Activity {

	public static final int UPDATE_INFO = 0;
	//public static final long UPDATE_FREQUENCY = 1000;
	//public Handler mHandler = new ThreadHandler();
	public static final String DATETIME_FORMAT = "yyyy-MM-dd aa hh:mm:ss";
	
	public static final String PREF_ACTIVITY_NAME = "prefActivityName";
	public static final String PREF_START_TIME = "prefStartTime";
	public static final String PREF_END_TIME = "prefEndTime";
	
	private static final int DIALOG_SET_START_TIME = 0;
	private static final int DIALOG_SET_END_TIME = 1;
	
	private Button buttonSave;
	private Button buttonCancel;
	private TextView textStartTime;
	private TextView textEndTime;
	private Button buttonSetStartTime;
	private Button buttonSetEndTime;
	private EditText editTextDoing;
	private Button buttonQuit;
	private Button buttonStartNow;
	private Button buttonEndNow;
	
	private NotificationManager mNotificationManager;
	
	private Calendar mStartTime = null;
	private Calendar mEndTime = null;
	private String mActivityName;
	private String deviceId;
	
	//Database
	DatabaseManager mDbMgr;
	
	//Service
	private boolean mBound = false;	
	private LoggingService mService = null;	
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
		setContentView(R.layout.activity_diary);	
		setUi();
		
		if(savedInstanceState != null){
			mActivityName = (String) savedInstanceState.getSerializable(PREF_ACTIVITY_NAME);
			mStartTime = (Calendar) savedInstanceState.getSerializable(PREF_START_TIME);
			mEndTime = (Calendar) savedInstanceState.getSerializable(PREF_END_TIME);
		}
		
		deviceId = ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mDbMgr = new DatabaseManager(this);			
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
		outState.putSerializable(PREF_ACTIVITY_NAME, mActivityName);
		outState.putSerializable(PREF_START_TIME, mStartTime);
		outState.putSerializable(PREF_END_TIME, mEndTime);
	}
	
	@Override
	protected void onStart(){
		super.onStart();
		Intent intent = new Intent(DiaryInputActivity.this, LoggingService.class);
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
	protected void onResume() {
		super.onResume();
		mDbMgr.openDb();
		
		SharedPreferences settings = getSharedPreferences(PREF_ACTIVITY_NAME, 0);
		
		mActivityName = settings.getString(PREF_ACTIVITY_NAME, "");
		editTextDoing.setText(mActivityName);
		
		Long startTimeInMillis = settings.getLong(PREF_START_TIME, -1);
		if(startTimeInMillis == -1) mStartTime = null;
		else {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(startTimeInMillis);
			mStartTime = c;
			this.textStartTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
		}
		
		Long endTimeInMillis = settings.getLong(PREF_END_TIME, -1);
		if(endTimeInMillis == -1) mEndTime = null;
		else {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(endTimeInMillis);
			mEndTime = c;
			this.textEndTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
		}		
		
		mNotificationManager.cancelAll();
	}	
	
	@Override
	protected void onPause() {
		super.onPause();
		mDbMgr.closeDb();
		
		SharedPreferences settings = getSharedPreferences(PREF_ACTIVITY_NAME, 0);
	    Editor editor = settings.edit();
	    editor.clear();
	    mActivityName = editTextDoing.getText().toString();
	    if(mActivityName != null) editor.putString(PREF_ACTIVITY_NAME, mActivityName);
	    if(mStartTime != null) editor.putLong(PREF_START_TIME, mStartTime.getTimeInMillis());
	    if(mEndTime != null) editor.putLong(PREF_END_TIME, mEndTime.getTimeInMillis());
	    editor.commit();
	    
	}
	
	@Override
	@Deprecated
	protected Dialog onCreateDialog(int id) {
    	TimePickerDialog tpd = null;
    	Calendar c = Calendar.getInstance();
    	switch (id) {      	
	        case DIALOG_SET_START_TIME:  	        	
	        	tpd = new TimePickerDialog(this, onStartTimeSetListener, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);  	 
	        	break;
	        case DIALOG_SET_END_TIME:  	        	
	        	tpd = new TimePickerDialog(this, onEndTimeSetListener, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false); 
	        	break;  
	    }  
	    return tpd;  
	}

	private OnTimeSetListener onStartTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) { 			
            //mHourStart = hourOfDay;  
            //mMinuteStart = minute;
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);            
            mStartTime = c;
            textStartTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
        }  
    };  

    private OnTimeSetListener onEndTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) { 			
            //mHourEnd = hourOfDay;  
            //mMinuteEnd = minute;
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);         
            mEndTime = c;
            textEndTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
        }  
    };

	private void writeToLog(){
			
		//Setting data for writing
		LogInfo logInfo = LogManager.getLogInfoByName("activity");
		DataManager dataMgr = logInfo.getDataMgr();
		
		dataMgr.put(DataManager.DEVICE_ID, String.valueOf(deviceId));
		
		/*
		Time startTime = new Time(Time.getCurrentTimezone());
		startTime.set(mStartTime.getSeconds(), mStartTime.getMinutes(), mStartTime.getHours(), mStartTime.getDate(), mStartTime.getMonth(), mStartTime.getYear());
		String timeStr = String.valueOf(startTime.year) + "-" + String.valueOf(startTime.month+1) + "-" + String.valueOf(startTime.monthDay)  //Month = [0-11]
				+ " " + startTime.format("%T");*/
		String timeStr = String.valueOf(mStartTime.get(Calendar.YEAR)) + "-" + String.valueOf(mStartTime.get(Calendar.MONTH)+1) + "-" + String.valueOf(mStartTime.get(Calendar.DAY_OF_MONTH))  
			+ " " + String.valueOf(mStartTime.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(mStartTime.get(Calendar.MINUTE)) + ":" + String.valueOf(mStartTime.get(Calendar.SECOND));
		dataMgr.put(DataManager.START_TIME, timeStr);
		/*
		Time endTime = new Time(Time.getCurrentTimezone());
		endTime.set(mEndTime.getSeconds(), mEndTime.getMinutes(), mEndTime.getHours(), mEndTime.getDate(), mEndTime.getMonth(), mEndTime.getYear());
		timeStr = String.valueOf(endTime.year) + "-" + String.valueOf(endTime.month+1) + "-" + String.valueOf(endTime.monthDay)  //Month = [0-11]
				+ " " + endTime.format("%T");*/
		timeStr = String.valueOf(mEndTime.get(Calendar.YEAR)) + "-" + String.valueOf(mEndTime.get(Calendar.MONTH+1)) + "-" + String.valueOf(mEndTime.get(Calendar.DAY_OF_MONTH))  
				+ " " + String.valueOf(mEndTime.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(mEndTime.get(Calendar.MINUTE)) + ":" + String.valueOf(mEndTime.get(Calendar.SECOND));
		dataMgr.put(DataManager.END_TIME, timeStr);
		
		dataMgr.put(DataManager.ACTIVITY_NAME, mActivityName);
		

		//Write to the log
		try {
			String toWrite = dataMgr.toString();			
			if (logInfo.getLogFos() != null){
				logInfo.getLogFos().write(toWrite.getBytes());
			}
		} catch (IOException e) {
			Log.e("writeToLogFile", "Cannot write into the file: " + logInfo.getLogFilename() + e.getMessage());			
		}		
		Log.v("writeToLogFile", "The file is " + getFilesDir() + "/" + logInfo.getLogFilename());
		Log.v("writeToLogFile", "Write Successfully!");
		
		//Write to the local database
		writeToDatabase();	
	}
	
	public void writeToDatabase(){
		//TODO test if this is good
		Log.v("Database", "Starting to insert new ActivityLog");
		
		ActivityLog log = new ActivityLog(null, deviceId, mStartTime.getTime(), mEndTime.getTime(), mActivityName, false);
		
		mDbMgr.getActivityLogDao().insert(log);
        Log.v("Database", "Inserted new ActivityLog, ID: " + log.getId());
        //Log.v("Database", "Size: " + String.valueOf((double)new File(db.getPath()).length()/1024.0) + "kB");
	}
	
    private void setUi(){
    	editTextDoing = (EditText) findViewById(R.id.editText_diary_doing);
    	buttonSetStartTime = (Button) findViewById(R.id.button_diary_choose_start_time);
    	buttonSetEndTime   = (Button) findViewById(R.id.button_diary_choose_end_time);
    	buttonSave = (Button) findViewById(R.id.button_diary_save);
    	buttonCancel = (Button) findViewById(R.id.button_diary_cancel);
    	buttonQuit = (Button) findViewById(R.id.button_diary_quit);
    	buttonStartNow = (Button) findViewById(R.id.button_diary_start_now);
    	buttonEndNow = (Button) findViewById(R.id.button_diary_end_now);
    	textStartTime = (TextView) findViewById(R.id.text_diary_start_time);
    	textEndTime = (TextView) findViewById(R.id.text_diary_end_time);
    	
    	buttonStartNow.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Calendar c = Calendar.getInstance();     
	            mStartTime = c;
	            textStartTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
	            
	            sendNotification();
	            Toast.makeText(DiaryInputActivity.this, "已開始記錄!", Toast.LENGTH_LONG).show();
	            finish();
			}
		});
    	
    	buttonEndNow.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Calendar c = Calendar.getInstance();     
	            mEndTime = c;
	            textEndTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
			}
		});  
    	
    	buttonSetStartTime.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				showDialog(DIALOG_SET_START_TIME);				
			}
		});
    	
    	buttonSetEndTime.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				showDialog(DIALOG_SET_END_TIME);				
			}
		});
    	
    	buttonSave.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				
				mActivityName = editTextDoing.getText().toString();
				if(mActivityName == null || mActivityName.equals("") || mStartTime == null || mEndTime == null){
					Toast.makeText(DiaryInputActivity.this, "欄位不得為空!", Toast.LENGTH_LONG).show();
				} 
				else if( !mStartTime.before(mEndTime) ){
					Toast.makeText(DiaryInputActivity.this, "結束時間不得早於開始時間!", Toast.LENGTH_LONG).show();
				}
				else{					
					writeToLog();
					Toast.makeText(DiaryInputActivity.this, "已儲存!", Toast.LENGTH_LONG).show();
					clearForm();					
					mService.lastSavedActivityTime = Calendar.getInstance(); 
					finish();
				}
		}});
    	
    	buttonCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				clearForm();			
		}});
    	
    	buttonQuit.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
    }
    
    
    private void clearForm(){
    	editTextDoing.setText("");
    	textStartTime.setText("");
    	textEndTime.setText("");
    	mStartTime = null;
    	mEndTime   = null;
    	mActivityName = "";
    }
    
    private void sendNotification(){
    	mActivityName = editTextDoing.getText().toString();
    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(this)
    	        .setSmallIcon(R.drawable.netdbfans)
    	        .setContentTitle("目前活動: " + mActivityName)
    	        .setContentText("點擊以記錄目前活動");
    	// Creates an explicit intent for an Activity in your app
    	Intent resultIntent = new Intent(this, DiaryInputActivity.class);

    	// The stack builder object will contain an artificial back stack for the
    	// started Activity.
    	// This ensures that navigating backward from the Activity leads out of
    	// your application to the Home screen.
    	TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
    	// Adds the back stack for the Intent (but not the Intent itself)
    	stackBuilder.addParentStack(DiaryInputActivity.class);
    	// Adds the Intent that starts the Activity to the top of the stack
    	stackBuilder.addNextIntent(resultIntent);
    	PendingIntent resultPendingIntent =
    	        stackBuilder.getPendingIntent(
    	            0,
    	            PendingIntent.FLAG_UPDATE_CURRENT
    	        );
    	mBuilder.setContentIntent(resultPendingIntent);
    	
    	// mId allows you to update the notification later on.
    	mNotificationManager.notify(0, mBuilder.build());
    }
    
    /*
	private static ViewGroup setTextColorWhite(ViewGroup v) {
        int count = v.getChildCount();
        for (int i = 0; i < count; i++) {
            View c = v.getChildAt(i);
            if(c instanceof ViewGroup){
                setTextColorWhite((ViewGroup) c);
            } else
            if(c instanceof TextView){
                ((TextView) c).setTextColor(Color.WHITE);
            }
        }
        return v;
    }*/
}
