package edu.ntu.arbor.sbchao.androidlogger.logmanager;

import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LogInfo {
	
	public String serverPath;
	public String localPath;
	public String dirPath;
	public String unuploadedPath;
	public String uploadedPath;	
	public String logName; //To identify the type of the log
	public String logFilename = null;    
    DataManager dataMgr = null;
    
    public FileOutputStream logFos = null;
    
	
	public LogInfo(String serverPath, String localPath, String dirPath, String unuploadedPath,
			String uploadedPath, String logName, DataManager dataMgr) {
		super();
		this.serverPath = serverPath;
		this.localPath = localPath;
		this.dirPath = dirPath;
		this.unuploadedPath = unuploadedPath;
		this.uploadedPath = uploadedPath;
		this.logName = logName;
		this.dataMgr = dataMgr;
	}

	public String getNewLogFileName(){
		Date today = new Date();
		DateFormat format = new SimpleDateFormat("MMdd", Locale.getDefault());		
		String newLogFilename = logName + format.format(today) + ".txt";
		return newLogFilename;
	}

	public String getServerPath() {
		return serverPath;
	}

	public void setServerPath(String serverPath) {
		this.serverPath = serverPath;
	}

	public String getDirPath() {
		return dirPath;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	public String getUnuploadedPath() {
		return unuploadedPath;
	}

	public void setUnuploadedPath(String unuploadedPath) {
		this.unuploadedPath = unuploadedPath;
	}

	public String getUploadedPath() {
		return uploadedPath;
	}

	public void setUploadedPath(String uploadedPath) {
		this.uploadedPath = uploadedPath;
	}

	public String getLogName() {
		return logName;
	}

	public void setLogName(String logName) {
		this.logName = logName;
	}

	public String getLogFilename() {
		return logFilename;
	}

	public void setLogFilename(String logFilename) {
		this.logFilename = logFilename;
	}

	public FileOutputStream getLogFos() {
		return logFos;
	}

	public void setLogFos(FileOutputStream logFos) {
		this.logFos = logFos;
	}
	
	public DataManager getDataMgr() {
		return dataMgr;
	}

	public void setDataMgr(DataManager dataMgr) {
		this.dataMgr = dataMgr;
	}

	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}
    
    /*
    String serverPath = "http://140.112.42.22:7380/netdbmobileminer_test/";
	String dirPath = "/AndroidLogger";
    String unuploadedPath = "/Unuploaded";
    String uploadedPath = "/Uploaded";
    String logName = "log"; //To identify the type of the log
    */
    


    
}
