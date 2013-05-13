package edu.ntu.arbor.sbchao.androidlogger;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
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

	public static final String TAG = "DiaryInputActivity";
	public static final int UPDATE_INFO = 0;
	//public static final long UPDATE_FREQUENCY = 1000;
	//public Handler mHandler = new ThreadHandler();
	public static final String DATETIME_FORMAT = "yyyy-MM-dd aa hh:mm:ss";
	
	public static final String[] defaultActivities = new String[] {"請選擇...", "研究", "吃飯", "休閒", "運動", "出遊", "搭交通工具", "其他"};
	ArrayList<String> activityList = new ArrayList<String>();
	public static final String PREF_DIARY = "prefsDiary";
	public static final String PREF_ACTIVITY_NAME = "prefActivityName";
	public static final String PREF_START_TIME = "prefStartTime";
	public static final String PREF_END_TIME = "prefEndTime";
	public static final String PREF_ACTIVITY_SET = "prefsActivitySet";
	private static final String PREF_COMMENT = "prefComment";
	private static final String PREF_ACTIVITY_ID = "prefActivityId";
	
	private static final int DIALOG_SET_START_TIME = 0;
	private static final int DIALOG_SET_END_TIME = 1;
	protected static final int DIALOG_SET_START_DATE = 2;
	protected static final int DIALOG_SET_END_DATE = 3;


	
	
	private Button buttonSave;
	private Button buttonCancel;
	private TextView textStartTime;
	private TextView textEndTime;
	private Button buttonSetStartTime;
	private Button buttonSetEndTime;
	//private EditText editTextDoing;
	private Button buttonQuit;
	private Button buttonStartNow;
	private Button buttonEndNow;
	private AutoCompleteTextView autoCompleteTextDoing;
	private Spinner spinnerDoing;
	private Button buttonSetStartDate;
	private Button buttonSetEndDate;
	private EditText editTextComment;
	
	private NotificationManager mNotificationManager;
	
	private Calendar mStartTime = null;
	private Calendar mEndTime = null;
	private String mActivityName = "";
	private int mActivityId = 0;
	private String mComment = "";
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
			mComment = (String) savedInstanceState.getSerializable(PREF_COMMENT);
			mActivityId = savedInstanceState.getInt(PREF_ACTIVITY_ID);
		}
		
		deviceId = ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mDbMgr = new DatabaseManager(this);	
		

	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(PREF_ACTIVITY_NAME, mActivityName);
		outState.putSerializable(PREF_START_TIME, mStartTime);
		outState.putSerializable(PREF_END_TIME, mEndTime);
		outState.putSerializable(PREF_COMMENT, mComment);
		outState.putInt(PREF_ACTIVITY_ID, mActivityId);
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
				
		//Restore previous inputs via shared preferences
		SharedPreferences settings = getSharedPreferences(PREF_DIARY , 0);
		
		mActivityName = settings.getString(PREF_ACTIVITY_NAME, "");
		
		mComment = settings.getString(PREF_COMMENT, "");
		editTextComment.setText(mComment);
		//editTextDoing.setText(mActivityName);
		
		mActivityId = settings.getInt(PREF_ACTIVITY_ID, 0);	
		spinnerDoing.setSelection(mActivityId);
		
		//autoCompleteTextDoing.setText(mActivityName);
		
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
		
		/*
		String joinedStr = settings.getString(PREF_ACTIVITY_SET, "");
		Log.d(TAG, "onResume joinedStr: " + joinedStr);
		if(joinedStr != ""){
			for(String str : joinedStr.split(";")){
				activityList.add(str);
			}
		}*/
		
		mNotificationManager.cancelAll();
	}	
	
	@Override
	protected void onPause() {
		super.onPause();
		mDbMgr.closeDb();
		
		//Save current inputs via shared preferences
		SharedPreferences settings = getSharedPreferences(PREF_DIARY, 0);
	    Editor editor = settings.edit();
	    editor.remove(PREF_COMMENT);
	    editor.remove(PREF_ACTIVITY_NAME);
	    editor.remove(PREF_START_TIME);
	    editor.remove(PREF_END_TIME);	
	    editor.remove(PREF_ACTIVITY_ID);
	    
	    //mActivityName = editTextDoing.getText().toString();
	    mComment = editTextComment.getText().toString();
	    if(mActivityName != null) editor.putString(PREF_ACTIVITY_NAME, mActivityName);
	    if(mComment != null) editor.putString(PREF_COMMENT, mComment);
	    if(mStartTime != null) editor.putLong(PREF_START_TIME, mStartTime.getTimeInMillis());
	    if(mEndTime != null) editor.putLong(PREF_END_TIME, mEndTime.getTimeInMillis());
	    editor.putInt(PREF_ACTIVITY_ID, spinnerDoing.getSelectedItemPosition());
	    
	    /*
		String joinedStr = "";
		for(String act : activityList){			
			joinedStr += (act + ";");			
		}
		Log.d(TAG, "onPause joinedStr: " + joinedStr);
		editor.putString(PREF_ACTIVITY_SET, joinedStr);*/
	    editor.commit();
	    
	}
	
	@Override
	@Deprecated
	protected Dialog onCreateDialog(int id) {
    	//TimePickerDialog tpd = null;
    	//DatePickerDialog dpd = null;
    	Calendar c = Calendar.getInstance();
    	switch (id) {      	
	        case DIALOG_SET_START_TIME:  	        	
	        	return new TimePickerDialog(this, onStartTimeSetListener, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);  	 
	        	
	        case DIALOG_SET_END_TIME:  	        	
	        	return  new TimePickerDialog(this, onEndTimeSetListener, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false); 
	        	 
	        case DIALOG_SET_START_DATE:  	        	
	        	return  new DatePickerDialog(this, onStartDateSetListener, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));  	 
	        	
	        case DIALOG_SET_END_DATE:  	        	
	        	return  new DatePickerDialog(this, onEndDateSetListener, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)); 
	        	  
	    }  
	    //return tpd;  
		return null;
	}
	private OnDateSetListener onStartDateSetListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			if(mStartTime == null){
	            Calendar c = Calendar.getInstance();
	            c.set(Calendar.YEAR, year);
	            c.set(Calendar.MONTH, monthOfYear);   
	            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
	            mStartTime = c;
			} else {
				mStartTime.set(Calendar.YEAR, year);
				mStartTime.set(Calendar.MONTH, monthOfYear);   
				mStartTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			}
			textStartTime.setText(DateFormat.format(DATETIME_FORMAT, mStartTime.getTime()));
		}  
    };  
    
	private OnDateSetListener onEndDateSetListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			if(mEndTime == null){
	            Calendar c = Calendar.getInstance();
	            c.set(Calendar.YEAR, year);
	            c.set(Calendar.MONTH, monthOfYear);   
	            c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
	            mEndTime = c;
			} else {
				mEndTime.set(Calendar.YEAR, year);
				mEndTime.set(Calendar.MONTH, monthOfYear);   
				mEndTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			}
			textEndTime.setText(DateFormat.format(DATETIME_FORMAT, mEndTime.getTime()));
		}  
    }; 
    
	private OnTimeSetListener onStartTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) { 			
			if(mStartTime == null){
	            Calendar c = Calendar.getInstance();
	            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
	            c.set(Calendar.MINUTE, minute);            
	            mStartTime = c;
			} else {
				mStartTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				mStartTime.set(Calendar.MINUTE, minute);    
			}
            textStartTime.setText(DateFormat.format(DATETIME_FORMAT, mStartTime.getTime()));
        }  
    };  

    private OnTimeSetListener onEndTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) { 
			if(mEndTime == null){
	            Calendar c = Calendar.getInstance();
	            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
	            c.set(Calendar.MINUTE, minute);         
	            mEndTime = c;
			} else {
				mEndTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
				mEndTime.set(Calendar.MINUTE, minute);   
			}
            textEndTime.setText(DateFormat.format(DATETIME_FORMAT, mEndTime.getTime()));
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
		Log.v("Database", "Starting to insert new ActivityLog");
		
		ActivityLog log = new ActivityLog(null, deviceId, mStartTime.getTime(), mEndTime.getTime(), mActivityName, false, mComment);
		
		mDbMgr.getActivityLogDao().insert(log);
        Log.v("Database", "Inserted new ActivityLog, ID: " + log.getId());
        //Log.v("Database", "Size: " + String.valueOf((double)new File(db.getPath()).length()/1024.0) + "kB");
	}
	
    private void setUi(){
    	//editTextDoing = (EditText) findViewById(R.id.editText_diary_doing);
    	//autoCompleteTextDoing = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_doing);
    	buttonSetStartTime = (Button) findViewById(R.id.button_diary_choose_start_time);
    	buttonSetEndTime   = (Button) findViewById(R.id.button_diary_choose_end_time);
    	buttonSave = (Button) findViewById(R.id.button_diary_save);
    	buttonCancel = (Button) findViewById(R.id.button_diary_cancel);
    	buttonQuit = (Button) findViewById(R.id.button_diary_quit);
    	buttonStartNow = (Button) findViewById(R.id.button_diary_start_now);
    	buttonEndNow = (Button) findViewById(R.id.button_diary_end_now);
    	buttonSetStartDate = (Button) findViewById(R.id.button_diary_choose_start_date);
    	buttonSetEndDate = (Button) findViewById(R.id.button_diary_choose_end_date);
    	textStartTime = (TextView) findViewById(R.id.text_diary_start_time);
    	textEndTime = (TextView) findViewById(R.id.text_diary_end_time);
    	editTextComment = (EditText) findViewById(R.id.editText_diary_comment);
    	spinnerDoing = (Spinner) findViewById(R.id.spinner_diary_doing);
    	
    	buttonSetStartDate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				showDialog(DIALOG_SET_START_DATE);		
			}
		});
    	
    	buttonSetEndDate.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				showDialog(DIALOG_SET_END_DATE);
			}
		});
    	
    	buttonStartNow.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Calendar c = Calendar.getInstance();     
	            mStartTime = c;
	            textStartTime.setText(DateFormat.format(DATETIME_FORMAT, c.getTime()));
	            
	            sendNotification();
	            //Toast.makeText(DiaryInputActivity.this, "已開始記錄!", Toast.LENGTH_LONG).show();
	            //finish();
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
    	
    	buttonSave.setOnClickListener(onButtonSaveClickedListener);
    	
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
    	
    	//Add user-specified activities to Spinner from preferences
    	setSpinner();
        
        //ArrayAdapter<String> adapterAuto = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, activityList);
    	//autoCompleteTextDoing.setAdapter(adapterAuto);
    	//autoCompleteTextDoing.setThreshold(1);
    	
    }
    
    //Add user-specified activities to Spinner from preferences
    private void setSpinner(){    	
    	SharedPreferences settings = getSharedPreferences(PREF_DIARY, 0);
    	//Get activity list from preferences
    	/*
    	String joinedStr = settings.getString(PREF_ACTIVITY_SET, "");
    	Log.d(TAG, "setUi joinedStr: " + joinedStr);
    	String activityArr [];
		if(joinedStr.equals("")){ 
			//Default options when a user first uses this app
			for(String act : defaultActivities){
				activityList.add(act);
			}
			activityArr = (String[]) activityList.toArray(new String[0]);
		} else {
			activityArr = joinedStr.split(";");
			for(String act : activityArr){
				activityList.add(act);
			}
		}*/		
    	
    	//Get activity list from preferences
    	String activityArr [];
		for(String act : defaultActivities){
			activityList.add(act);
		}
		activityArr = (String[]) activityList.toArray(new String[0]);
		
		//Add activities to the spinner
    	ArrayAdapter<String> adapterSpinner = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, activityArr);
        spinnerDoing.setAdapter(adapterSpinner);
        spinnerDoing.setOnItemSelectedListener(new OnItemSelectedListener(){
        	public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        		//if(position != 0){
        			mActivityName = adapterView.getSelectedItem().toString();
        			mActivityId = position;
	        		//editTextDoing.setText(adapterView.getSelectedItem().toString());
	        		//mActivityName = editTextDoing.getText().toString();
        		//}
        	}  
        	public void onNothingSelected(AdapterView<?> arg0) {  
        		mActivityName = null;
        		mActivityId = 0;
                //editTextDoing.setText("");
                //mActivityName = editTextDoing.getText().toString();
        	}  
        });            
        spinnerDoing.setVisibility(View.VISIBLE);  
    }
    
    private OnClickListener onButtonSaveClickedListener = new OnClickListener(){
		@Override
		public void onClick(View arg0) {
			
			mComment = editTextComment.getText().toString();
			Log.i(TAG, "mComment: " + mComment);
			Log.i(TAG, "mActivityName: " + mActivityName);
			Log.i(TAG, "mActivityId: " + mActivityId);
			
			if(mActivityId == 0 || mActivityName == null || mActivityName.equals("") || mStartTime == null || mEndTime == null){
				Toast.makeText(DiaryInputActivity.this, "請選擇活動類別和時間!", Toast.LENGTH_LONG).show();
			} 
			else if( !mStartTime.before(mEndTime) ){
				Toast.makeText(DiaryInputActivity.this, "結束時間不得早於開始時間!", Toast.LENGTH_LONG).show();
			}
			else{
				//Add new user-specified activity to the preferences so that it can be conveniently selected in the spinner
				/*if( !activityList.contains(mActivityName) ) activityList.add(mActivityName);					 
				SharedPreferences settings = getSharedPreferences(PREF_DIARY, 0);
			    Editor editor = settings.edit();	    				    
				String joinedStr = "";
				for(String act : activityList){			
					joinedStr += (act + ";");
				}
				Log.d(TAG, "onSaveClicked joinedStr: " + joinedStr);
				editor.putString(PREF_ACTIVITY_SET, joinedStr);				
				editor.commit();
				*/
				
				//Write to log and database
				writeToLog();
				Toast.makeText(DiaryInputActivity.this, "已儲存!", Toast.LENGTH_LONG).show();
				clearForm();
				
				//By default, next activity's start time = this activity's end time
				mStartTime = mEndTime;
				
				finish();
				//Use this to notify users to write diaries if they don't for a while 
				mService.lastSavedActivityTime = Calendar.getInstance(); 
			}
		}
	};
    
    private void clearForm(){
    	//editTextDoing.setText("");
    	editTextComment.setText("");
    	//autoCompleteTextDoing.setText("");
    	textStartTime.setText("");
    	textEndTime.setText("");
    	
    	mStartTime = null;
    	mEndTime   = null;
    	mActivityName = null;
    	mComment = null;
    	mActivityId = 0;
    }
    
    private void sendNotification(){
    	//mActivityName = editTextDoing.getText().toString();
    	//TODO
    	//mActivityName = autoCompleteTextDoing.getText().toString();
    	NotificationCompat.Builder mBuilder =
    	        new NotificationCompat.Builder(this)
    	        .setSmallIcon(R.drawable.netdbfans)
    	        .setContentTitle("目前活動: " + mActivityName + " " + mComment)
    	        .setContentText("點擊以儲存目前活動");
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
