package edu.ntu.arbor.sbchao.androidlogger.logmanager;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class UploadingService extends IntentService {

	public UploadingService() {
		super("UploadingService");
	}
	
	public UploadingService(String name) {
		super("UploadingService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("UploadingService", "start!");
		LogManager.uploadAll();
		Log.i("UploadingService", "finished!");
	}

}
