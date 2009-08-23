package com.koushikdutta.klaxon;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ClockActivity extends Activity {
	Handler mHandler = new Handler();
	TextView mDateView;
	TextView mNextAlarm;
	AlarmSettingsOld mNextAlarmSettings;
	KlaxonSettings mSettings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.bedclock);

		LayoutInflater inflater = getLayoutInflater();

		LinearLayout clockLayout = (LinearLayout) findViewById(R.id.ClockLayout);

		DigitalClock digi = (DigitalClock)((ViewGroup)inflater.inflate(R.layout.digital_clock, clockLayout)).getChildAt(0);

		/* monitor time ticks, time changed, timezone */
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		registerReceiver(mIntentReceiver, filter, null, mHandler);

		mNextAlarmSettings = AlarmSettingsOld.scheduleNextAlarm(this);

		mDateView = (TextView) findViewById(R.id.DateLayout);
		mNextAlarm = (TextView) findViewById(R.id.NextAlarmLayout);
		refreshTimes();
		
		mSettings = new KlaxonSettings(this);
		int color = mSettings.getBedClockColor(0xFF202080);
		mDateView.setTextColor(color);
		mNextAlarm.setTextColor(color);
		digi.setColorOn(color);
	}

	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refreshTimes();
		}
	};

	void refreshTimes() {
		mNextAlarm.setText("");
		if (mNextAlarmSettings != null) {
			Long nextAlarmTime = mNextAlarmSettings.getNextAlarmTime();
			if (nextAlarmTime != null) {
				GregorianCalendar now = new GregorianCalendar();
				long nowMs = now.getTimeInMillis();
				long delta = nextAlarmTime - nowMs;
				long hours = delta / (1000 * 60 * 60) % 24;
				long minutes = delta / (1000 * 60) % 60;
				long days = delta / (1000 * 60 * 60 * 24);
				String text = String.format(
						"%s: %d days, %d hours, and %d minutes.", mNextAlarmSettings.getName(), days, hours,
						minutes);
				mNextAlarm.setText(text);
			}
		}

		GregorianCalendar greg = new GregorianCalendar();
		SimpleDateFormat df = new SimpleDateFormat("EEEE, MMMM dd");
		String date = df.format(greg.getTime());
		mDateView.setText(date);
	}
}
