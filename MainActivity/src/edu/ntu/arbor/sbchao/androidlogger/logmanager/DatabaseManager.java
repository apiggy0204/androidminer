package edu.ntu.arbor.sbchao.androidlogger.logmanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;
import de.greenrobot.dao.QueryBuilder;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLog;
import edu.ntu.arbor.sbchao.androidlogger.scheme.ActivityLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoMaster;
import edu.ntu.arbor.sbchao.androidlogger.scheme.DaoSession;
import edu.ntu.arbor.sbchao.androidlogger.scheme.MobileLogDao;
import edu.ntu.arbor.sbchao.androidlogger.scheme.NetworkLogDao;
/*
 * This class provides access to databases and is able to upload databases to server.
 * 
 * */
public class DatabaseManager {
	
	//Access Local database
	protected SQLiteDatabase db;
	protected DaoMaster daoMaster;
	protected DaoSession daoSession;
	protected ActivityLogDao activityLogDao;	
	protected NetworkLogDao networkLogDao;
	protected MobileLogDao mobileLogDao;
	
	private Context context;

	public DatabaseManager(Context context){
		this.context = context;
	}
	
	/*
	public class MyDbOpenHelper extends SQLiteOpenHelper{

		public MyDbOpenHelper(Context context, String name,
				CursorFactory factory, int version) {
			super(context, name, factory, version);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			ActivityLogDao.createTable(db, true);
			MobileLogDao.createTable(db, true);
			NetworkLogDao.createTable(db, true);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			Log.i("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
	        ActivityLogDao.dropTable(db, true);
            onCreate(db);
		}
		
	}*/
	
	public void openDb(){
        //Set up local databases
		File dbFile = new File(Environment.getExternalStorageDirectory().getPath(), "AndroidLogger/netdb.db");
		db = new DaoMaster.DevOpenHelper(context, dbFile.getPath(), null).getWritableDatabase();		 
		/*db = SQLiteDatabase.openOrCreateDatabase(dbfile, null);			
		ActivityLogDao.createTable(db, true);
		MobileLogDao.createTable(db, true);
		NetworkLogDao.createTable(db, true);*/
        
        daoMaster = new DaoMaster(db);
        daoSession = daoMaster.newSession();
        mobileLogDao = daoSession.getMobileLogDao();
        activityLogDao = daoSession.getActivityLogDao();   
    }
    
	public void closeDb(){
    	db.close();
    }

	public void uploadActivityLogs(){

        QueryBuilder<ActivityLog> qb = getActivityLogDao().queryBuilder();
        qb.where(ActivityLogDao.Properties.Uploaded.eq(false));			
        List<ActivityLog> logs = qb.list();
                
        for(ActivityLog log : logs){
        	
        	Log.i("DatabaseManager", "Uploading activity logs" + log.getActivityName() + " in the database...");
        	
        	ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        	params.add(new BasicNameValuePair(DataManager.DEVICE_ID, log.getDeviceId()));
        	params.add(new BasicNameValuePair(DataManager.START_TIME, dateToString(log.getStartTime())));
        	params.add(new BasicNameValuePair(DataManager.END_TIME, dateToString(log.getEndTime())));
			params.add(new BasicNameValuePair(DataManager.ACTIVITY_NAME, log.getActivityName()));

			boolean success = uploadSingleRecord(params, LogManager.getActivityloginfo());
			if(success){
				log.setUploaded(true);
				getActivityLogDao().update(log);
			} 
			
        }
	}
	
	private static boolean uploadSingleRecord(ArrayList<NameValuePair> params, LogInfo info){
		HttpPost httpPost = new HttpPost(info.serverPath);
		HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000); //Set timeout to 5 secs.		
		httpPost.setParams(httpParams);
		try {
			httpPost.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			HttpResponse resp = new DefaultHttpClient().execute(httpPost);
			if (resp.getStatusLine().getStatusCode() == 200){				
				Log.v("sendStatistics", "a record has been sent!");
				return true;
			} else {
				Log.e("sendStatistics", "fail!");
			}			
		} catch (ClientProtocolException e){
			Log.e("sendStatistics", e.getMessage());
			
		} catch (IOException e){
			Log.e("sendStatistics", e.getMessage());
		}
		return false;
	}	
	
	private static String dateToString(Date date){
		String timeStr = String.valueOf(date.getYear()+1900) + "-" + String.valueOf(date.getMonth()+1) + "-" + String.valueOf(date.getDate())  
				+ " " + String.valueOf(date.getHours()) + ":" + String.valueOf(date.getMinutes()) + ":" + String.valueOf(date.getSeconds());
		return timeStr;		
	}
	
	public ActivityLogDao getActivityLogDao() {
		return activityLogDao;
	}

	public void setActivityLogDao(ActivityLogDao activityLogDao) {
		this.activityLogDao = activityLogDao;
	}

	public NetworkLogDao getNetworkLogDao() {
		return networkLogDao;
	}

	public void setNetworkLogDao(NetworkLogDao networkLogDao) {
		this.networkLogDao = networkLogDao;
	}

	public MobileLogDao getMobileLogDao() {
		return mobileLogDao;
	}

	public void setMobileLogDao(MobileLogDao mobileLogDao) {
		this.mobileLogDao = mobileLogDao;
	}
    
}
