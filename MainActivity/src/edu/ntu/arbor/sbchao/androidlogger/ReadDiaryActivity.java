package edu.ntu.arbor.sbchao.androidlogger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.SimpleAdapter;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.logmanager.DatabaseManager;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;

public class ReadDiaryActivity extends FragmentActivity {
	
	private static final int LOAD_DIARY = 0x0000;
	
	private boolean displayed = false;
	private ArrayList<Map<String, String>> mListItemMaps = new ArrayList<Map<String, String>>();  
	private ArrayList<ActivityLog> mLogs = new ArrayList<ActivityLog>();
	
	//Database access
	private DatabaseManager mDbMgr;

	//Ui
	public SimpleAdapter adapter;
	private ProgressDialog mDialog;
	private DiaryListFragment mListFragment;
	private SupportMapFragment mMapFragment;
	private GoogleMap mGoogleMap;
	
    private Handler mHandler = new Handler() {
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
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
//        addUiListeners();
        setContentView(R.layout.activity_read_diary);
        mListFragment = (DiaryListFragment) getSupportFragmentManager().findFragmentById(R.id.list);
        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mGoogleMap = mMapFragment.getMap();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(mMapFragment);
        
        mDbMgr = new DatabaseManager(this);
        
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        mDbMgr.openDb();
        
        //Update info on UI
        mHandler.postDelayed(queryDiaryThread, 0); 
        if(!displayed){
			mDialog = new ProgressDialog(ReadDiaryActivity.this);
	        mDialog.setMessage("Loading diaries...");
	        mDialog.setCancelable(false);
	        mDialog.show();
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        mDbMgr.closeDb();
    }
    
//    protected void onListItemClick(ListView l, View v, final int position, long id) {  
//        super.onListItemClick(l, v, position, id);
//        //TODO
//        AlertDialog.Builder adb=new AlertDialog.Builder(ReadDiaryActivity.this);
//        adb.setTitle("Delete?");
//        adb.setMessage("Are you sure you want to delete it?");
//        
//        adb.setNegativeButton("Cancel", null);
//        adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
//            public void onClick(DialogInterface dialog, int which) {
//            	mDbMgr.getActivityLogDao().delete(mLogs.remove(position));
//                mListItemMaps.remove(position);
//                adapter.notifyDataSetChanged();
//            }});
//        adb.show();
//    }

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
	
    //List today's activity
    private class QueryDiaryTask extends AsyncTask<Void, Void, Void>{
    	private PolylineOptions lineOptions = new PolylineOptions();
    	List<ActivityLog> logs;
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			//TODO query the database and 		
			
			Log.i("doInBackgound", "start!");
			QueryBuilder<ActivityLog> qb = mDbMgr.getActivityLogDao().queryBuilder();
	        qb.where(qb.and(ActivityLogDao.Properties.StartTime.ge(getStartOfToday()), ActivityLogDao.Properties.EndTime.le(getEndOfToday())));			
	        logs = qb.list();
	        
	        for(ActivityLog log : logs){
	        	Log.i("doInBackground", "Activity Name: " + log.getActivityName());
	        	Date startTime = log.getStartTime();
	        	Date endTime = log.getEndTime();
	        	
	        	//List mobile logs during start time and end time of this daily activity
	        	QueryBuilder<MobileLog> mqb = mDbMgr.getMobileLogDao().queryBuilder();
	        	mqb.where(mqb.and(MobileLogDao.Properties.Time.ge(startTime), MobileLogDao.Properties.Time.le(endTime)));
	        	List<MobileLog> mobileLogs = mqb.list();
	        	for(MobileLog mlog : mobileLogs){
	        		//TODO location information
	        		Log.i("doInBackground", "Location: (" + String.valueOf(mlog.getLat()) + ", " + String.valueOf(mlog.getLon()) + ")");
//	        		LatLng position = new LatLng(Double.valueOf(mlog.getLat()), Double.valueOf(mlog.getLon()));
//	        		lineOptions.add(position);
	        	}
	        	lineOptions.width(2);
                lineOptions.color(Color.RED);
	        	
	        	//Add to the list view
	        	HashMap<String, String> map = new HashMap<String, String>();  
	            map.put("name", log.getActivityName());  
	            map.put("desc", "From " + startTime.toGMTString() + " to " + endTime.toGMTString());  
	            mListItemMaps.add(map); 
	            mLogs.add(log);
	        }
	        
	        Log.i("doInBackgound", "finish!!");
			return null;
		}
		
    	@Override
    	protected void onPostExecute(Void result){
    		//mGoogleMap.addPolyline(lineOptions);
    		
//    		//TODO Update UI after doInBackground() finishes
//	        adapter = new SimpleAdapter(ReadDiaryActivity.this, mListItemMaps,  
//	                // SDK 库中提供的一个包含两个 TextView 的layout  
//	        		android.R.layout.simple_list_item_2,   
//	                new String[] { "name", "desc" }, // maps 中的两个 key  
//	                new int[] { android.R.id.text1, android.R.id.text2 }// 两个TextView的 id   
//	        );  
//	        ReadDiaryActivity.this.setListAdapter(adapter);
	        
    		mListFragment.update(logs);
    		
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
	        c.roll(Calendar.DAY_OF_YEAR, true); //increment a day	        
	        return c.getTime();
		}
	};
    
}
