package edu.ntu.arbor.sbchao.androidlogger;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SettingsActivity extends PreferenceActivity {
	
    public static final String PREFS_UPLOAD = "pref_allow_3G_upload";
    public static final String PREFS_LOGGING = "pref_allow_logging";
	
	//private CheckBox checkBoxAllow3GUploading;
	//private CheckBox checkBoxAllowLogging;
	
	//private SharedPreferences settings;

	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_settings);
        addPreferencesFromResource(R.layout.activity_settings);
                
		//settings = getSharedPreferences(LoggingService.PREFS_NAME, 0);
		//Log.i("Preference", "allow 3G? " + String.valueOf(settings.getBoolean(LoggingService.PREFS_UPLOAD, false)));
	}
	
	/*
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
		if(key.equals(PREFS_UPLOAD)){
			
		}
	}*/
	
	/*
	private void addUiListener(){
		checkBoxAllow3GUploading = (CheckBox) findViewById(R.id.checkbox_allow_3G);
		checkBoxAllowLogging = (CheckBox) findViewById(R.id.checkbox_allow_logging);
		
		checkBoxAllow3GUploading.setChecked(settings.getBoolean(LoggingService.PREFS_UPLOAD, false));
		checkBoxAllow3GUploading.setOnCheckedChangeListener(allow3GUploadingListener);
	}
	
	
	private OnCheckedChangeListener allow3GUploadingListener = new OnCheckedChangeListener(){
		@Override
		public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
			SharedPreferences.Editor editor = settings.edit();
			editor.putBoolean(LoggingService.PREFS_UPLOAD, checkBoxAllow3GUploading.isChecked());
			editor.commit();
			Log.i("Preference", "allow 3G? " + String.valueOf(settings.getBoolean(LoggingService.PREFS_UPLOAD, false)));
		}
	};*/
}
