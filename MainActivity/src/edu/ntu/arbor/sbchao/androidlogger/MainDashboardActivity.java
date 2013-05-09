package edu.ntu.arbor.sbchao.androidlogger;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.apps.iosched.ui.widget.DashboardLayout;

import edu.ntu.arbor.sbchao.androidlogger.logmanager.DatabaseManager;

public class MainDashboardActivity extends Activity {
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
		setUi();
		startService(new Intent(this, LoggingService.class));
		
	}
	
	private void setUi(){
		ViewGroup dashboardLayout = new DashboardLayout(this);
		dashboardLayout.setBackgroundColor(Color.BLACK);
		
		ImageButton buttonDiary = new ImageButton(this);
		buttonDiary.setImageResource(R.drawable.main_diary);
		buttonDiary.setBackgroundColor(Color.TRANSPARENT);
		buttonDiary.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, DiaryInputActivity.class);
				startActivity(intent);
		}});
		TextView textDiary = new TextView(this);
		textDiary.setText(R.string.main_diary_input);
		textDiary.setTextColor(Color.WHITE);
		
		ImageButton buttonReadDiary = new ImageButton(this);
		buttonReadDiary.setImageResource(R.drawable.main_read_diary);
		buttonReadDiary.setBackgroundColor(Color.TRANSPARENT);
		buttonReadDiary.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, ReadDiaryActivity.class);
				intent.putExtra("mode", ReadDiaryActivity.LIST);
				startActivity(intent);
				
		}});
		TextView textReadDiary = new TextView(this);
		textReadDiary.setText(R.string.main_read_diary);
		textReadDiary.setTextColor(Color.WHITE);

		
		ImageButton buttonMap = new ImageButton(this);
		buttonMap.setImageResource(R.drawable.main_map);
		buttonMap.setBackgroundColor(Color.TRANSPARENT);
		buttonMap.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {				
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, ReadDiaryActivity.class); //TODO
				startActivity(intent);				
		}});
		TextView textMap = new TextView(this);
		textMap.setText(R.string.main_map);
		textMap.setTextColor(Color.WHITE);
		
		
		ImageButton buttonMobile = new ImageButton(this);
		buttonMobile.setImageResource(R.drawable.main_mobile_status);
		buttonMobile.setBackgroundColor(Color.TRANSPARENT);
		buttonMobile.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, MobileStatusActivity.class);
				startActivity(intent);
			}});

		TextView textMobile = new TextView(this);
		textMobile.setText(R.string.main_mobile_status);
		textMobile.setTextColor(Color.WHITE);
		
		
		ImageButton buttonNetwork = new ImageButton(this);
		buttonNetwork.setImageResource(R.drawable.main_network);
		buttonNetwork.setBackgroundColor(Color.TRANSPARENT);
		buttonNetwork.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, NetworkTrafficActivity.class);
				startActivity(intent);
			}});
		
		TextView textNetwork = new TextView(this);
		textNetwork.setText(R.string.main_network_traffic);
		textNetwork.setTextColor(Color.WHITE);
		    
		ImageButton buttonDailyAppUsage = new ImageButton(this);
		buttonDailyAppUsage.setImageResource(R.drawable.main_daily);
		buttonDailyAppUsage.setBackgroundColor(Color.TRANSPARENT);
		buttonDailyAppUsage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, AppUsageActivity.class);
				Bundle extras = new Bundle();
				extras.putString(AppUsageActivity.PREFS_CHART_MODE, AppUsageActivity.MODE_DAILY);
				intent.putExtras(extras);
				MainDashboardActivity.this.startActivity(intent);
			}});
		
		TextView textDailyAppUsage = new TextView(this);
		textDailyAppUsage.setText(R.string.main_daily_app_usage);
		textDailyAppUsage.setTextColor(Color.WHITE);

		ImageButton buttonHourlyAppUsage = new ImageButton(this);
		buttonHourlyAppUsage.setImageResource(R.drawable.main_houly);
		buttonHourlyAppUsage.setBackgroundColor(Color.TRANSPARENT);
		buttonHourlyAppUsage.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, AppUsageActivity.class);
				Bundle extras = new Bundle();
				extras.putString(AppUsageActivity.PREFS_CHART_MODE, AppUsageActivity.MODE_HOURLY);
				intent.putExtras(extras);
				MainDashboardActivity.this.startActivity(intent);
			}});
		
		TextView textHourlyAppUsage = new TextView(this);
		textHourlyAppUsage.setText(R.string.main_hourly_app_usage);
		textHourlyAppUsage.setTextColor(Color.WHITE);

		ImageButton buttonSettings = new ImageButton(this);
		buttonSettings.setImageResource(R.drawable.main_settings);
		buttonSettings.setBackgroundColor(Color.TRANSPARENT);
		buttonSettings.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
	
				//TODO
				Intent intent = new Intent();
				intent.setClass(MainDashboardActivity.this, SettingsActivity.class);
				startActivity(intent);
			}});
		
		TextView textSettings = new TextView(this);
		textSettings.setText(R.string.main_settings);
		textSettings.setTextColor(Color.WHITE);
		
		ImageButton buttonContact = new ImageButton(this);
		buttonContact.setImageResource(R.drawable.main_contact);
		buttonContact.setBackgroundColor(Color.TRANSPARENT);
		buttonContact.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				Uri uri = Uri.parse("mailto:apiggy0204@gmail.com");  
				Intent it = new Intent(Intent.ACTION_SENDTO, uri);  
				startActivity(it);  
			}});
		
		TextView textContact = new TextView(this);
		textContact.setText(R.string.main_contact);
		textContact.setTextColor(Color.WHITE);

		dashboardLayout.addView(getTextedButton(buttonDiary, textDiary));
		dashboardLayout.addView(getTextedButton(buttonReadDiary, textReadDiary));
		dashboardLayout.addView(getTextedButton(buttonMap, textMap));
		//dashboardLayout.addView(getTextedButton(buttonMobile, textMobile));
		//dashboardLayout.addView(getTextedButton(buttonNetwork, textNetwork));
		//dashboardLayout.addView(getTextedButton(buttonHourlyAppUsage, textHourlyAppUsage));
		//dashboardLayout.addView(getTextedButton(buttonDailyAppUsage, textDailyAppUsage));
		dashboardLayout.addView(getTextedButton(buttonSettings, textSettings));
		dashboardLayout.addView(getTextedButton(buttonContact, textContact));
		
		setContentView(dashboardLayout);
	}
	
	public LinearLayout getTextedButton(ImageButton button, TextView text){
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(1);
		layout.addView(button);
		layout.addView(text);
		text.setGravity(Gravity.CENTER);
		return layout;
	}
	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.activity_main, menu);
    	menu.add(0, Menu.FIRST, 0, "Settings...");
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
        case Menu.FIRST:
        	Log.i("onOptionsItemSelected", "selected!");
            Intent intent = new Intent();            
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
            break;
        }
        return super.onOptionsItemSelected(item);
    }
}
