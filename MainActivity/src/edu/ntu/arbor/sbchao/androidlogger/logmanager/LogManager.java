package edu.ntu.arbor.sbchao.androidlogger.logmanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class LogManager {
	
	//static final String SERVER_PATH = "http://10.0.2.2/netdbmobileminer_test/";
	/*static final String SERVER_PATH = "http://140.112.42.22:7380/netdbmobileminer_test/";
	static final String LOG_DIR_PATH = "/AndroidLogger";
    static final String LOG_UNUPLOADED_PATH = "/Unuploaded";
    static final String LOG_UPLOADED_PATH = "/Uploaded";
    static final String TAG = "log"; //To identify the type of the log*/
    
    /*String serverPath = "http://140.112.42.22:7380/netdbmobileminer_test/";
	String dirPath = "/AndroidLogger";
    String unuploadedPath = "/Unuploaded";
    String uploadedPath = "/Uploaded";
    String tagName = "log"; //To identify the type of the log*/
        
    private static HashMap<String, LogInfo> logInfos = new HashMap<String, LogInfo>();
    
    //private static DataManager dMgr = new DataManager();
    
	private static boolean mExternalStorageAvailable;
	private static boolean mExternalStorageWriteable;
	
	//public String logFilename = null;
    //public FileOutputStream logFos = null;
	
    private Context context; 
    
    public LogManager(Context context){
    	this.context = context;
    }
    
    /*
    public LogManager(Context context, String serverPath, String dirPath, String unuploadedPath, String uploadedPath, String tagName){
    	this.context = context;
    	this.serverPath = serverPath;
    	this.dirPath = dirPath;
    	this.unuploadedPath = unuploadedPath;
    	this.uploadedPath = uploadedPath;
    	this.tagName = tagName;    	
    }*/

    
    public static void addLogInfo(String serverPath, String localPath, String dirPath, String unuploadedPath, String uploadedPath, String logName, DataManager mgr){
    	if( logInfos.containsKey(logName) == false ){
    		logInfos.put(logName, new LogInfo(serverPath, localPath, dirPath, unuploadedPath, uploadedPath, logName, mgr));
    	}
    }
    
    public static LogInfo getLogInfoByName(String logName){
		return logInfos.get(logName);
	}
    
    /*
    public static FileOutputStream getFosByName(String logName){
    	return logInfos.get(logName).logFos;
    }*/
    
	public static void uploadAll(){
		//TODO make a notification bar
		
		for(String logName : logInfos.keySet()){
		
			LogInfo info = logInfos.get(logName);
			
			String extPath = Environment.getExternalStorageDirectory().getPath();
			File logDir = new File(extPath, info.dirPath);
			File unuploadedDir = new File(logDir, info.unuploadedPath);	
			File uploadedDir   = new File(logDir, info.uploadedPath); 
			File[] fileList = unuploadedDir.listFiles();
			
			if( fileList != null ){
				Log.i("sendStatistics", "There are " + fileList.length + " unuploaded files" );
				for (File file : fileList){			
					boolean uploaded = uploadSingleFile(file, info);				
					Log.i("sendStatistics", "upload " + file.getPath() + "? " + String.valueOf(uploaded));
					if(uploaded){
						//Move to the uploaded directory
						File dest = new File(uploadedDir, file.getName());
						boolean isMoved = file.renameTo(dest);
						Log.i("sendStatistics", "The uploaded file has been moved to" + dest.getPath() + "?" + String.valueOf(isMoved));
					}
				}						
			}
		}
		
	}
	
	private static boolean uploadSingleFile(File file, LogInfo info){
		
		try {
			LogFileReader reader = new LogFileReader(file, info.getDataMgr()); //TODO
			ArrayList<ArrayList<NameValuePair>> list = reader.all();
			
			for(ArrayList<NameValuePair> params : list){
				boolean success = uploadSingleRecord(params, info);
				if( success == false) return false;
			}			
			
		} catch (IOException e1) {
			Log.e("uploadSingleFile", "IOException");
			e1.printStackTrace();
			return false;
		}								
		return true;
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
	
	//Check if external storage is available and create directories if they don't exist
	public void checkExternalStorage(String logName){
		
		LogInfo info = logInfos.get(logName);
		
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		    //TODO ask the user if she wants to make sdcard available? 
		}

		//Create directories if they do not exists
		if( mExternalStorageAvailable && mExternalStorageWriteable ){
			String extPath = Environment.getExternalStorageDirectory().getPath();			
			
			File logDir = new File(extPath, info.dirPath);
			if (!logDir.exists()){				
				boolean success = logDir.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}			
			File unuploadedPath = new File(logDir, info.unuploadedPath);			
			if (!unuploadedPath.exists()){
				boolean success = unuploadedPath.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}			
			File uploadedPath = new File(logDir, info.uploadedPath);			
			if (!uploadedPath.exists()){
				boolean success = uploadedPath.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}
		}
	}
	
	public void createNewLog(String logName){				
		
		LogInfo info = logInfos.get(logName);
		
		/*
    	Date today = new Date();
		DateFormat format = new SimpleDateFormat("MMdd", Locale.getDefault());		
		String newLogFilename = logName + format.format(today) + ".txt";*/
		
		String newLogFilename = info.getNewLogFileName();
		
		if( info.logFilename == null || !newLogFilename.equals(info.logFilename)){			
			try {
				if(info.logFos != null){
					info.logFos.close();
				}
				info.logFilename = newLogFilename;
				File localDir = context.getDir(info.getLocalPath(), 0);
				
				Log.i("createNewLog", "localDir: " + info.getLocalPath());
				Log.i("createNewLog", "localDir: " + localDir.getPath());
				
				File newFile = new File(localDir, newLogFilename);
				info.setLogFos(new FileOutputStream(newFile));
				//File newFile = new File(localDir, info.logFilename);
				//info.logFos = context.openFileOutput(newFile.getPath(), Context.MODE_APPEND);
				Log.i("createNewLog", "create local file " + newFile.getPath() + "successfully");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e("createNewLog", "cannot open log file: " + info.logFilename);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("createNewLog", "cannot close log file:" + info.logFilename);
			}
			
		}		
		
    }
	
	//Move local log files to sdcard once a day
	public boolean moveToExternalStorage(String logName){
		
		LogInfo info = logInfos.get(logName);
		
		checkExternalStorage(logName);		
		if( mExternalStorageAvailable && mExternalStorageWriteable ){
			
			String extPath = Environment.getExternalStorageDirectory().getPath();
			File logDir = new File(extPath, info.dirPath);
			File unuploadedDir = new File(logDir, info.unuploadedPath);
			//Log.d("unuploadedDir", logName + " " + unuploadedDir.getPath());
			
			//String dirPath = context.getFilesDir().getPath();
			//String [] fList = context.fileList();
			File localDir = context.getDir(info.getLocalPath(), 0);			
			String [] fList = localDir.list();
			
			boolean success = true;
			for (String filename : fList){
				File src  = new File(localDir, filename);
				File dest = new File(unuploadedDir, filename);
				try {
					//Ignore file for today while moving files to sdcard
					if(!src.getName().equals(info.logFilename)){
						copyFile(src, dest);					
						//boolean deleted = context.deleteFile(filename);
						boolean deleted = src.delete();
						Log.i("moveToExternalStorage", "move the file: " + src.getPath() + " successfully to " + dest.getParent() + "? " + String.valueOf(deleted));
					}
				} 				
				catch (IOException e) {
					Log.e("moveToExternalStorage", "cannot move the file: " + src.getPath());
					e.printStackTrace();
					success = false;
				}				
			}			
			return success;
			
		} 		
		else {		
			Log.i("moveToExternalStorage", "Cannot write into the sdcard temporarily");
			return false;
		}		
	}
	
	
	
	//A helper function which copies a file
	private void copyFile(File src, File dst) throws IOException {
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dst);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}


}
