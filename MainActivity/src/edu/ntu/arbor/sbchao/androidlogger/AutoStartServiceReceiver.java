package edu.ntu.arbor.sbchao.androidlogger;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStartServiceReceiver extends BroadcastReceiver{
	public void onReceive(Context context, Intent intent) {		
		String action=intent.getAction();		
		if(Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_USER_PRESENT.equals(action) || Intent.ACTION_SCREEN_ON.equals(action) || Intent.ACTION_SCREEN_OFF.equals(action)){
			Log.i("AutoStartServiceReceiver", action);		
			Intent loggingIntent = new Intent(context, LoggingService.class);
    		loggingIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
    		context.startService(loggingIntent);			    		
		}
	}
	
}
