package edu.ntu.arbor.sbchao.androidlogger;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;


public class ReadDiaryActivity extends Activity {
	
	private static final int LOAD_DIARY = 0x0002;
	
	private ThreadHandler mHandler = new ThreadHandler();
	
	//Local database
	private SQLiteDatabase db;
	private DaoMaster daoMaster;
	private DaoSession daoSession;
	private ActivityLogDao activityLogDao;	
	private MobileLogDao mobileLogDao;
	
	private static int UPDATE_FREQUENCY = 10000;
	
	private boolean displayed = false;


	private ProgressDialog mDialog;

	
	
    private final class ThreadHandler extends Handler {
	    public ThreadHandler() {
            super();
        }
		@Override
		public void handleMessage(Message msg) {						
			switch(msg.what){
				case LOAD_DIARY:					
					new QueryDiaryTask().execute();
					//mHandler.postDelayed(queryDiaryThread, UPDATE_FREQUENCY);
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
         
        addUiListeners();

        //Ask user to enable location providers
        //mHandler.postDelayed(enableLocationProviderThread, 1000);
        
        //Set up local databases
		File dbfile = new File(Environment.getExternalStorageDirectory().getPath(), "AndroidLogger/netdb.db");
		db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);		
		ActivityLogDao.createTable(db, true);
        
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        mobileLogDao = daoSession.getMobileLogDao();
        activityLogDao = daoSession.getActivityLogDao();        
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        //Update info on UI
        mHandler.postDelayed(queryDiaryThread, 0); 
        if(!displayed){
			mDialog = new ProgressDialog(ReadDiaryActivity.this);
	        mDialog.setMessage("Loading network traffic statistics...");
	        mDialog.setCancelable(false);
	        mDialog.show();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        db.close();
    }
    
    private Runnable queryDiaryThread = new Runnable(){
		@Override
		public void run() {
			try {				
				Message msg = new Message();
				msg.what = LOAD_DIARY;
				mHandler.sendMessage(msg);
			}  
			catch(Exception e) {
				e.printStackTrace();
				Log.e("UpdateInfoThread", e.getMessage());
			}
		}
    };

    private void addUiListeners(){         
    	//TODO
    }
	
    private class QueryDiaryTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... arg0) {
			//TODO query the database and 		
			
			Log.i("doInBackgound", "start!");
			QueryBuilder<ActivityLog> qb = activityLogDao.queryBuilder();
	        qb.where(qb.and(ActivityLogDao.Properties.StartTime.ge(getStartOfToday()), ActivityLogDao.Properties.EndTime.le(getEndOfToday())));	        
	        List<ActivityLog> logs = qb.list();
	        
	        for(ActivityLog log : logs){
	        	Log.i("doInBackground", "Activity Name: " + log.getActivityName());
	        	Date startTime = log.getStartTime();
	        	Date endTime = log.getEndTime();
	        	
	        	//List mobile logs during start time and end time of this daily activity
	        	QueryBuilder<MobileLog> mqb = mobileLogDao.queryBuilder();
	        	mqb.where(mqb.and(MobileLogDao.Properties.Time.ge(startTime), MobileLogDao.Properties.Time.le(endTime)));
	        	List<MobileLog> mobileLogs = mqb.list();
	        	
	        	for(MobileLog mlog : mobileLogs){
	        		Log.i("doInBackground", "Location: (" + String.valueOf(mlog.getLat()) + ", " + String.valueOf(mlog.getLon()) + ")");
	        	}
	        }
	        
	        Log.i("doInBackgound", "finish!!");
			
			return null;
		}
		
    	@Override
    	protected void onPostExecute(Void result){
    		
    		//TODO Update UI after doInBackground() finishes
    		
    		//Close the "loading" dialog
	        if(!displayed){
	        	displayed = true;
	        	mDialog.dismiss();
	        }
    	}
    	
    	private Date getStartOfToday(){
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
    	
    	private Date getEndOfToday(){
			Calendar c = Calendar.getInstance();
	        c.clear();
	        c.setTime(new Date());
	        c.clear(Calendar.HOUR);
	        c.clear(Calendar.HOUR_OF_DAY);
	        c.clear(Calendar.MINUTE);
	        c.clear(Calendar.SECOND);
	        c.clear(Calendar.MILLISECOND);
	        c.roll(Calendar.DAY_OF_MONTH, true); //increment a day
	        return c.getTime();
		}
	};
    
}
