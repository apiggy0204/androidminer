package edu.ntu.arbor.sbchao.androidlogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class LogManager {
	
	//static final String SERVER_PATH = "http://10.0.2.2/netdbmobileminer_test/";
	static final String SERVER_PATH = "http://140.112.42.22:7380/netdbmobileminer_test/";
	static final String LOG_DIR_PATH = "/AndroidLogger";
    static final String LOG_UNUPLOADED_PATH = "/Unuploaded";
    static final String LOG_UPLOADED_PATH = "/Uploaded";
    
    private static DataManager dMgr = new DataManager();
    
	private static boolean mExternalStorageAvailable;
	private static boolean mExternalStorageWriteable;
	
	String logFilename = null;
    FileOutputStream logFos = null;
	private Context service; 
    
    public LogManager(Context service){
    	this.service = service;
    }
    
    public void finish(){
    	
    }
    
	public static void uploadAll(){		
		//TODO make a notification bar
		 
		String extPath = Environment.getExternalStorageDirectory().getPath();
		File unuploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UNUPLOADED_PATH);			
		File[] fileList = unuploadedPath.listFiles();
		if( fileList != null ){
			Log.i("sendStatistics", "There are " + fileList.length + " unuploaded files" );
			for (File file : fileList){			
				boolean uploaded = uploadSingleFile(file);				
				Log.i("sendStatistics", "upload " + file.getPath() + "? " + String.valueOf(uploaded));
				if(uploaded){
					//Move to the uploaded directory
					File dest = new File(extPath + LOG_DIR_PATH + LOG_UPLOADED_PATH + "/" + file.getName());
					boolean isMoved = file.renameTo(dest);
					Log.i("sendStatistics", "The uploaded file has been moved to" + dest.getPath() + "?" + String.valueOf(isMoved));
				}
			}						
		}
		
	}
	
	private static boolean uploadSingleFile(File file){
		
		try {
			LogFileReader reader = new LogFileReader(file, dMgr);
			ArrayList<ArrayList<NameValuePair>> list = reader.all();
			
			for(ArrayList<NameValuePair> params : list){
				boolean success = uploadSingleRecord(params);				
				if( success == false) return false;
			}			
			
		} catch (IOException e1) {
			Log.e("uploadSingleFile", "IOException");
			e1.printStackTrace();
			return false;				
		}								
		return true;
	}
	
	private static boolean uploadSingleRecord(ArrayList<NameValuePair> params){
		HttpPost httpPost = new HttpPost(SERVER_PATH);
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
	public void checkExternalStorage(){
		
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
			
			File extDir = new File(extPath + LOG_DIR_PATH);
			if (!extDir.exists()){				
				boolean success = extDir.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}			
			File unuploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UNUPLOADED_PATH);			
			if (!unuploadedPath.exists()){
				boolean success = unuploadedPath.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}			
			File uploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UPLOADED_PATH);			
			if (!uploadedPath.exists()){
				boolean success = uploadedPath.mkdir();
				Log.i("checkExternalStorage", "mkdir success? " + String.valueOf(success));
			}
		}
	}
	
	public void createNewLog(){
    	Date today = new Date();
		DateFormat format = new SimpleDateFormat("MMdd", Locale.getDefault());		
		String newLogFilename = "log_" + format.format(today) + ".txt";		
		
		if( logFos == null || !newLogFilename.equals(logFilename)){			
			try {
				if(logFos != null){
					logFos.close();
				}
				logFilename = newLogFilename;
				logFos = service.openFileOutput(logFilename, Context.MODE_APPEND);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				Log.e("logfile", "cannot open log file: " + logFilename);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("logfile", "cannot close log file:" + logFilename);
			}
			Log.i("logfile", "create local file " + logFilename + "successfully");
		}		
		
    }
	
	//Move local log files to sdcard once a day
	public boolean moveToExternalStorage(){
		
		checkExternalStorage();		
		if( mExternalStorageAvailable && mExternalStorageWriteable ){
			
			String extPath = Environment.getExternalStorageDirectory().getPath();
			File unuploadedPath = new File(extPath + LOG_DIR_PATH + LOG_UNUPLOADED_PATH);
			
			String dirPath = service.getFilesDir().getPath();
			String [] fList = service.fileList();	
			boolean success = true;
			for (String filename : fList){
				File src  = new File(dirPath + "/" + filename);
				File dest = new File(unuploadedPath.getPath() + "/" + filename);
				try {
					//Ignore file for today while moving files to sdcard
					if(!src.getName().equals(logFilename)){
						copyFile(src, dest);					
						boolean deleted = service.deleteFile(filename);
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
