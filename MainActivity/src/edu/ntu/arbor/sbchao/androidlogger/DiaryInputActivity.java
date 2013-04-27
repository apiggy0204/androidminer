package edu.ntu.arbor.sbchao.androidlogger;


import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Environment;
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
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DataManager;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.LogInfo;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.LogManager;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao;

public class DiaryInputActivity extends Activity {

	public static final int UPDATE_INFO = 0;
	public static final long UPDATE_FREQUENCY = 1000;
	//public Handler mHandler = new ThreadHandler();
	public static final String DATETIME_FORMAT = "yyyy-MM-dd aa hh:mm:ss";
	
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
	
	private Calendar mStartTime = null;
	private Calendar mEndTime = null;
	private String mActivityName;
	private String deviceId;
	
	//Database
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private ActivityLogDao activityLogDao;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_diary);	
		setUi();
		
		deviceId = ((TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
		
		LogManager.addLogInfo("http://140.112.42.22:7380/netdbmobileminer_test/activity.php", "activty", "AndroidLogger", "Unuploaded_activity", "Uploaded_activity", "activity", DataManager.getDailyActivityDataManager());
		LogManager mLogMgr = new LogManager(this);	
		mLogMgr.checkExternalStorage("activity");
		mLogMgr.createNewLog("activity");
		

	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		File dbfile = new File(Environment.getExternalStorageDirectory().getPath(), "AndroidLogger/netdb.db");
		db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);		
        ActivityLogDao.createTable(db, true);
		
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        activityLogDao = daoSession.getActivityLogDao();		
	}	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		try {
			LogManager.getLogInfoByName("activity").getLogFos().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		db.close();
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
		String timeStr = String.valueOf(mStartTime.get(Calendar.YEAR)) + "-" + String.valueOf(mStartTime.get(Calendar.MONTH)) + "-" + String.valueOf(mStartTime.get(Calendar.DAY_OF_MONTH))  
			+ " " + String.valueOf(mStartTime.get(Calendar.HOUR_OF_DAY)) + ":" + String.valueOf(mStartTime.get(Calendar.MINUTE)) + ":" + String.valueOf(mStartTime.get(Calendar.SECOND));
		dataMgr.put(DataManager.START_TIME, timeStr);
		/*
		Time endTime = new Time(Time.getCurrentTimezone());
		endTime.set(mEndTime.getSeconds(), mEndTime.getMinutes(), mEndTime.getHours(), mEndTime.getDate(), mEndTime.getMonth(), mEndTime.getYear());
		timeStr = String.valueOf(endTime.year) + "-" + String.valueOf(endTime.month+1) + "-" + String.valueOf(endTime.monthDay)  //Month = [0-11]
				+ " " + endTime.format("%T");*/
		timeStr = String.valueOf(mEndTime.get(Calendar.YEAR)) + "-" + String.valueOf(mEndTime.get(Calendar.MONTH)) + "-" + String.valueOf(mEndTime.get(Calendar.DAY_OF_MONTH))  
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
			Log.e("writeToLogFile", "Cannot write into the file: " + logInfo.getLogFilename());			
		}		
		Log.v("writeToLogFile", "The file is " + getFilesDir() + "/" + logInfo.getLogFilename());
		Log.v("writeToLogFile", "Write Successfully!");
		
		//Write to the local database
		writeToDatabase();	
	}
	
	public void writeToDatabase(){
		//TODO
		Log.v("Database", "Starting to insert new MobileLog");
		
		ActivityLog log = new ActivityLog(null, deviceId, mStartTime.getTime(), mEndTime.getTime(), mActivityName);
		
        activityLogDao.insert(log);
        Log.v("Database", "Inserted new MobileLog, ID: " + log.getId());
        Log.v("Database", "Size: " + String.valueOf((double)new File(db.getPath()).length()/1024.0) + "kB");
	}
	
    private void setUi(){
    	editTextDoing = (EditText) findViewById(R.id.editText_diary_doing);
    	buttonSetStartTime = (Button) findViewById(R.id.button_diary_choose_start_time);
    	buttonSetEndTime   = (Button) findViewById(R.id.button_diary_choose_end_time);
    	buttonSave = (Button) findViewById(R.id.button_diary_save);
    	buttonCancel = (Button) findViewById(R.id.button_diary_cancel);
    	buttonQuit = (Button) findViewById(R.id.button_diary_quit);
    	textStartTime = (TextView) findViewById(R.id.text_diary_start_time);
    	textEndTime = (TextView) findViewById(R.id.text_diary_end_time);    	    	
    	//timePickerStart = (TimePicker) findViewById(R.id.timePickerStart);
    	//timePickerEnd   = (TimePicker) findViewById(R.id.timePickerEnd);
    	//setTextColorBlack(timePickerStart);
    	//setTextColorBlack(timePickerEnd);    	
    	
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
					//TODO
					Toast.makeText(DiaryInputActivity.this, "欄位不得為空!", Toast.LENGTH_LONG).show();
				} 
				else if( !mStartTime.before(mEndTime) ){
					Toast.makeText(DiaryInputActivity.this, "結束時間不得早於開始時間!", Toast.LENGTH_LONG).show();
				}
				else{					
					writeToLog();
					Toast.makeText(DiaryInputActivity.this, "已儲存!", Toast.LENGTH_LONG).show();
				}
				
				/*isLogging = true;
				textStartTime.setText(DateFormat.format(DATETIME_FORMAT, new Date()));
				textEndTime.setText(DateFormat.format(DATETIME_FORMAT, new Date()));
				mHandler.postDelayed(updateInfoThread, UPDATE_FREQUENCY);*/
		}});
    	
    	buttonCancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				clearForm();
				//isLogging = false;				
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
    }
    
}
