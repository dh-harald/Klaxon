package com.koushikdutta.klaxon;

import java.util.Calendar;
import java.util.GregorianCalendar;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;


public class AlarmActivity extends DeskClock
{
	AlarmSettings mSettings;
	MediaPlayer mPlayer;
	Handler mHandler = new Handler();
	KlaxonSettings mKlaxonSettings;
	SensorManager mSensorManager;
	SensorListener mListener;
	int mSnoozeTime;
	GregorianCalendar mExpireTime;
	MenuItem mOffMenuItem;
	TextView mNextAlarm;
	Vibrator mVibrator;
	boolean mVibrateEnabled = true;
	final static String LOGTAG = "Klaxon";
	SQLiteDatabase mDatabase;
	private KeyguardManager mKeyguardManager;
	private KeyguardManager.KeyguardLock mKeyguardLock = null;

	static boolean isFlat(int orientation)
	{
		return orientation == ORIENTATION_FACEDOWN || orientation == ORIENTATION_FACEUP;
	}

	static int toOrientation(float x, float y, float z)
	{
		float absX = Math.abs(x);
		float absY = Math.abs(y);
		float absZ = Math.abs(z);
		if (absX > absY)
		{
			if (absX > absZ)
			{
				if (absX < 8)
					return ORIENTATION_UNDEFINED;
				if (x > 0)
					return ORIENTATION_LANDSCAPE;
				return ORIENTATION_REVERSELANDSCAPE;
			}
		}
		else if (absY > absZ)
		{
			if (absY < 8)
				return ORIENTATION_UNDEFINED;
			if (y > 0)
				return ORIENTATION_PORTRAIT;
			return ORIENTATION_REVERSEPORTRAIT;
		}

		if (absZ < 8)
			return ORIENTATION_UNDEFINED;
		if (z > 0)
			return ORIENTATION_FACEDOWN;
		return ORIENTATION_FACEUP;
	}

	static int toOrientation2(float x, float y, float z)
	{
		float absX = Math.abs(x);
		float absY = Math.abs(y);
		float absZ = Math.abs(z);
		if (absX > absY)
		{
			if (absX > absZ)
			{
				if (absX < .7)
					return ORIENTATION_UNDEFINED;
				if (x > 0)
					return ORIENTATION_LANDSCAPE;
				return ORIENTATION_REVERSELANDSCAPE;
			}
		}
		else if (absY > absZ)
		{
			if (absY < .7)
				return ORIENTATION_UNDEFINED;
			if (y > 0)
				return ORIENTATION_PORTRAIT;
			return ORIENTATION_REVERSEPORTRAIT;
		}

		if (absZ < .7)
			return ORIENTATION_UNDEFINED;
		if (z > 0)
			return ORIENTATION_FACEDOWN;
		return ORIENTATION_FACEUP;
	}

	static final int ORIENTATION_UNDEFINED = -1;
	static final int ORIENTATION_LANDSCAPE = 0;
	static final int ORIENTATION_REVERSELANDSCAPE = 1;
	static final int ORIENTATION_PORTRAIT = 2;
	static final int ORIENTATION_REVERSEPORTRAIT = 3; // upside down
	static final int ORIENTATION_FACEDOWN = 4;
	static final int ORIENTATION_FACEUP = 5;

	static final int EXPIRE_TIME = 30;

	private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			refreshNextAlarmText();
			GregorianCalendar now = new GregorianCalendar();
			if (mExpireTime != null && mExpireTime.before(now))
				finish();
		}
	};

	void init()
	{
		if (mDatabase == null)
		{
			Intent intent = getIntent();
			long alarmId = intent.getLongExtra(AlarmSettings.GEN_FIELD__ID, -1);
			mDatabase = AlarmSettings.getDatabase(this);
			mSettings = AlarmSettings.getAlarmSettingsById(this, mDatabase, alarmId);
			mKlaxonSettings = new KlaxonSettings(this);
			mSnoozeTime = mSettings.getSnoozeTime();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		init();
		super.onCreate(savedInstanceState);
		try
		{
			// this is a fix for Cyanogen mod which integrates crap from donut.
			//  getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
			getWindow().addFlags(0x00080000);
		}
		catch(Exception ex)
		{
		}
        mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		
		String name = mSettings.getName();
		if (name != null && !"".equals(name))
			setTitle(name);

		mVibrateEnabled = mSettings.getVibrateEnabled();
		prepareAlarm();

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		// watch for time change events
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_TIME_TICK);
		filter.addAction(Intent.ACTION_TIME_CHANGED);
		filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		registerReceiver(mTimeChangedReceiver, filter, null, mHandler);

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		resumeAlarm();
	}
	
	void refreshNextAlarmText()
	{
		GregorianCalendar now = new GregorianCalendar();
		if (mSnoozeEnd == null || now.after(mSnoozeEnd))
		{
			mNextAlarm.setText(mSettings.getName());
			return;
		}

		long msLeft = mSnoozeEnd.getTimeInMillis() - now.getTimeInMillis();
		int minutesLeft = Math.round((float) msLeft / 1000f / 60f);
		mNextAlarm.setText(String.format("Snoozing: %d minutes", minutesLeft));		
	}
	
	protected void refreshAlarm()
	{
		refreshNextAlarmText();
	}
	
	@Override
	protected void initViews() {
		super.initViews();
		
		findViewById(R.id.desk_clock_buttons).setVisibility(View.GONE);
		findViewById(R.id.desk_clock_alarm_buttons).setVisibility(View.VISIBLE);
		
		ImageButton snoozeButton = (ImageButton) findViewById(R.id.snooze_button);
		snoozeButton.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				snoozeAlarm();
			}
		});

		if (mSnoozeTime == 0)
		{
			snoozeButton.setVisibility(View.INVISIBLE);
		}
		
        findViewById(R.id.weather).setOnClickListener(null);
        findViewById(R.id.nextAlarm).setOnClickListener(null);
        mNextAlarm = (TextView) findViewById(R.id.nextAlarm);
	}

	void stopAlarm()
	{
		Log.v("stopAlarm");
		mSettings.setNextSnooze(0);
		if (mSettings.isOneShot())
			mSettings.setEnabled(false);
		mSettings.update();
		AlarmSettings.scheduleNextAlarm(this);
		finish();
	}

	AudioManager mAudioManager;

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mOffMenuItem = menu.add(0, 0, 0, "Turn Off Alarm");
		mOffMenuItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if (item == mOffMenuItem)
		{
			stopAlarm();
		}
		return super.onMenuItemSelected(featureId, item);
	}

	final int AUDIO_STREAM = AudioManager.STREAM_MUSIC;

	void prepareAlarm()
	{
		Uri ringtoneUri = mSettings.getRingtone();
		try
		{
			if (ringtoneUri != null)
				mPlayer = MediaPlayer.create(this, ringtoneUri);
			else
				mPlayer = MediaPlayer.create(this, R.raw.klaxon);
			mPlayer.setLooping(true);
		}
		catch (Exception e)
		{
			mPlayer = MediaPlayer.create(this, R.raw.klaxon);
			mPlayer.setLooping(true);
		}
		mPlayer.setAudioStreamType(AUDIO_STREAM);
	}

	GregorianCalendar mSnoozeEnd = null;

	void snoozeAlarm()
	{
		if (mSnoozeTime == 0)
			return;
		Log.v("snoozeAlarm");
		stopNotification();

		mVibrator.vibrate(300);
		mExpireTime = null;
		mSnoozeEnd = new GregorianCalendar();
		mSnoozeEnd.add(Calendar.MINUTE, mSnoozeTime);
		refreshNextAlarmText();
		AlarmAlertWakeLock.acquirePartial(this);
		mSettings.setNextSnooze(mSnoozeEnd.getTimeInMillis());
		mSettings.setEnabled(true);
		mSettings.update();
		AlarmSettings.scheduleNextAlarm(this);
		refreshAlarm();
	}

	void stopNotification()
	{
		Log.v("stopNotification");
		AlarmAlertWakeLock.release();
		mVibrator.cancel();
		mStopVolumeAdjustThread = true;
		unregisterSensorListener();
		mPlayer.pause();
	}

	void onFlipAction()
	{
		Log.v("onFlipAction");
		if (mSnoozeEnd == null || mSnoozeEnd.before(new GregorianCalendar()))
		{
			Log.v("Snoozing alarm");
			snoozeAlarm();
		}
		else
		{
			Log.v("Not snoozing alarm; already snoozing.");
		}
	}

	void unregisterSensorListener()
	{
		if (mListener != null && mSensorManager != null)
		{
			mSensorManager.unregisterListener(mListener);
			mListener = null;
		}
	}

	void resumeAlarm()
	{
		disableKeyguard();
		unregisterSensorListener();
		Sensor compass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		if (compass == null)
		{		
			mListener = new SensorListener()
			{
				long mSensorReadingStart = new GregorianCalendar().getTimeInMillis() + 2000;
				boolean mShouldReadSensor = false;
				int mFlatOrientation = -1;
				int mLastOrientation = -1;
				long mOrientationFlipStart = 0;
				int mOrientationLog[] = new int[15];
				float mVectorLengthSqLog[] = new float[15];
				int mSampleCount = 0;
	
				public void onAccuracyChanged(int sensor, int accuracy)
				{
				}
	
				public float getAverageVecLenSq()
				{
					float ret = 0;
					for (int i = 0; i < mVectorLengthSqLog.length; i++)
					{
						ret += mVectorLengthSqLog[i];
					}
					return ret / mVectorLengthSqLog.length;
				}
	
				public int getPrimaryOrientation()
				{
					int orientations[] = new int[6];
					int curMax = 0;
					for (int i = 0; i < mOrientationLog.length; i++)
					{
						int logOrientation = mOrientationLog[i];
						if (logOrientation != -1)
						{
							int orientationCount = ++orientations[logOrientation];
							if (orientationCount > orientations[curMax])
								curMax = logOrientation;
						}
					}
					return curMax;
				}
	
				public void onSensorChanged(int sensor, float[] values)
				{
					float x = values[0];
					float y = -values[1];
					float z = values[2];
					float vecLenSq = x * x + y * y + z * z;
					int newOrientation = toOrientation(x, y, z);
					mOrientationLog[mSampleCount % mOrientationLog.length] = newOrientation;
					mVectorLengthSqLog[mSampleCount % mVectorLengthSqLog.length] = vecLenSq;
					mSampleCount++;
	
					if (mSampleCount < mOrientationLog.length)
						return;
	
					int primaryOrientation = getPrimaryOrientation();
					if (!isFlat(mFlatOrientation))
					{
						if (isFlat(primaryOrientation))
							mFlatOrientation = primaryOrientation;
					}
	
					if (isFlat(primaryOrientation) && primaryOrientation != mFlatOrientation)
					{
						onFlipAction();
						mFlatOrientation = primaryOrientation;
					}
				}
			};
			mSensorManager.registerListener(mListener, SensorManager.SENSOR_ACCELEROMETER);
		}
		else
		{
			mListener = new SensorListener()
			{
				int mLastFlatOrientation = -1;
				long mLastFlatOrientationTime = 0;
				int mCurrentFlatOrientation = -1;
	
				public void onAccuracyChanged(int sensor, int accuracy)
				{
				}
	
				float[] normalize(float[] values)
				{
					float len = (float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
					float[] ret = new float[3];
					ret[0] = values[0] / len;
					ret[1] = values[1] / len;
					ret[2] = values[2] / len;
					return ret;
				}
	
				public void onSensorChanged(int sensor, float[] values)
				{
					float[] norm = normalize(values);
					int orientation = toOrientation2(norm[0], norm[1], norm[2]);
	
					// see if the orientation has changed since the last time
					// and log the change
					if (mLastFlatOrientation != orientation && isFlat(orientation))
					{
						mLastFlatOrientation = orientation;
						mLastFlatOrientationTime = System.currentTimeMillis();
					}
	
					// if there is no current orientation, and there is a last orientation, use that.
					if (mCurrentFlatOrientation == -1 && mLastFlatOrientation != -1)
						mCurrentFlatOrientation = mLastFlatOrientation;
	
					// if there is no current orientation, return
					if (mCurrentFlatOrientation == -1)
						return;
	
					// if our last orientation change was less than a second ago, return
					if (mLastFlatOrientationTime > System.currentTimeMillis() - 1000)
						return;
	
					// if the orientations are the same, return
					if (mLastFlatOrientation == mCurrentFlatOrientation)
						return;
	
					// since we are here, it means that the orientation has changed for over a second
					mCurrentFlatOrientation = mLastFlatOrientation;
					onFlipAction();
				}
			};
			mSensorManager.registerListener(mListener, SensorManager.SENSOR_MAGNETIC_FIELD);
		}

		final double streamMaxVolume = mAudioManager.getStreamMaxVolume(AUDIO_STREAM);
		final double volumeRamp = mSettings.getVolumeRamp();
		final double maxVolume = mSettings.getVolume();
		if (volumeRamp == 0)
		{
			double convertedVolume = streamMaxVolume * maxVolume / 100d;
			mAudioManager.setStreamVolume(AUDIO_STREAM, (int)convertedVolume, 0);
		}
		else
		{
			mAudioManager.setStreamVolume(AUDIO_STREAM, 0, 0);
			mStopVolumeAdjustThread = false;
			new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						double curVolume = 0;
						double volumeStep = maxVolume / volumeRamp;
						for (int i = 0; i < volumeRamp; i++)
						{
							try
							{
								Thread.sleep(1000);
								if (mStopVolumeAdjustThread)
									break;
							}
							catch (InterruptedException e)
							{
							}
							curVolume += volumeStep;
							double convertedVolume = streamMaxVolume * curVolume / 100d;
							mAudioManager.setStreamVolume(AUDIO_STREAM, (int)convertedVolume, 0);
						}
	
						double convertedVolume = streamMaxVolume * maxVolume / 100d;
						mAudioManager.setStreamVolume(AUDIO_STREAM, (int)convertedVolume, 0);
					}
					catch (Exception ex)
					{
						ex.printStackTrace();
					}
				}
			}).start();
		}

		if (mVibrateEnabled)
			mVibrator.vibrate(new long[] { 5000, 1000 }, 0);

		AlarmAlertWakeLock.acquire(this);
		mExpireTime = new GregorianCalendar();
		mExpireTime.add(Calendar.MINUTE, EXPIRE_TIME);
		mPlayer.start();
		mPlayer.seekTo(0);
		refreshNextAlarmText();
	}

	boolean mStopVolumeAdjustThread = false;

	void cleanupAlarm()
	{
		Log.v("cleanupAlarm");
		mStopVolumeAdjustThread = true;
		unregisterSensorListener();
		mVibrator.cancel();
		if (mPlayer != null)
		{
			mPlayer.stop();
			mPlayer.release();
			mPlayer = null;
		}
		AlarmAlertWakeLock.release();
		unregisterReceiver(mTimeChangedReceiver);
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		cleanupAlarm();
		enableKeyguard();
		mDatabase.close();
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		super.onNewIntent(intent);
		resumeAlarm();
	}
	
    private synchronized void enableKeyguard() {
        if (mKeyguardLock != null) {
            mKeyguardLock.reenableKeyguard();
            mKeyguardLock = null;
        }
    }

    private synchronized void disableKeyguard() {
        if (mKeyguardLock == null) {
            mKeyguardLock = mKeyguardManager.newKeyguardLock(LOGTAG);
            mKeyguardLock.disableKeyguard();
        }
    }
    
    @Override
    public void onStop() {
    	Log.v("onStop");
		stopNotification();
		super.onStop();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK)
    		return true;
    	return super.onKeyDown(keyCode, event);
    }
}