package com.koushikdutta.klaxon;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.provider.Settings;

public class AlarmSettings
{
	SharedPreferences mPreferences;
	Editor mEditor;
	Context mContext;
	String mAlarmId;
	Thread mThread = null;

	void commit()
	{
		mEditor.commit();
		//		if (mThread == null)
		//		{
		//			mThread = new Thread()
		//			{
		//				@Override
		//				public void run()
		//				{
		//					try
		//					{
		//						Thread.sleep(2000);
		//						mEditor.commit();
		//					}
		//					catch(Exception ex)
		//					{
		//					}
		//					finally
		//					{
		//						mThread = null;
		//					}
		//				}
		//			};
		//			mThread.start();
		//		}
	}

	public AlarmSettings(Context context, String alarmId)
	{
		mContext = context;
		mAlarmId = alarmId;
		mPreferences = mContext.getSharedPreferences(String.format("AlarmSettings%s", mAlarmId), Context.MODE_PRIVATE);
		mEditor = mPreferences.edit();
	}

	public String getId()
	{
		return mAlarmId;
	}

	public void setEnabled(boolean value)
	{
		mEditor.putBoolean("AlarmEnabled", value);
		commit();
	}

	public boolean getEnabled()
	{
		return mPreferences.getBoolean("AlarmEnabled", true);
	}

	public boolean getVibrateEnabled()
	{
		return mPreferences.getBoolean("VibrateEnabled", true);
	}

	public void setVibrateEnabled(boolean value)
	{
		mEditor.putBoolean("VibrateEnabled", value);
		commit();
	}

	public void Delete()
	{
		mEditor.clear();
		commit();
		KlaxonSettings settings = new KlaxonSettings(mContext);
		ArrayList<String> allAlarmIds = settings.getAlarmIds();
		allAlarmIds.remove(mAlarmId);
		settings.setAlarmIds(allAlarmIds);
	}

	public boolean[] getAlarmDays()
	{
		boolean[] alarmDays = new boolean[7];
		for (int i = 0; i < alarmDays.length; i++)
		{
			alarmDays[i] = mPreferences.getBoolean(String.format("AlarmDays%d", i), false);
		}
		return alarmDays;
	}

	public void setAlarmDays(boolean[] alarmDays)
	{
		for (int i = 0; i < alarmDays.length; i++)
		{
			mEditor.putBoolean(String.format("AlarmDays%d", i), alarmDays[i]);
		}
		commit();
	}

	static public ArrayList<AlarmSettings> getAlarms(Context context)
	{
		KlaxonSettings settings = new KlaxonSettings(context);
		ArrayList<String> allAlarmIds = settings.getAlarmIds();

		ArrayList<AlarmSettings> ret = new ArrayList<AlarmSettings>();
		for (String alarmId : allAlarmIds)
		{
			ret.add(new AlarmSettings(context, alarmId));
		}
		return ret;
	}

	public int getHour()
	{
		return mPreferences.getInt("Hour", 1);
	}

	public void setHour(int hour)
	{
		mEditor.putInt("Hour", hour);
		commit();
	}

	public int getMinutes()
	{
		return mPreferences.getInt("Minutes", new Date().getMinutes());
	}

	public void setMinutes(int minutes)
	{
		mEditor.putInt("Minutes", minutes);
		commit();
	}

	public Uri getRingtone()
	{
		try
		{
			return Uri.parse(mPreferences.getString("Ringtone", null));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public void setRingtone(Uri ringtone)
	{
		mEditor.putString("Ringtone", ringtone.toString());
		commit();
	}

	public boolean isOneShot()
	{
		return isOneShot(getAlarmDays());
	}

	static boolean isOneShot(boolean[] alarmDays)
	{
		boolean hasDays = false;
		for (int i = 0; i < alarmDays.length; i++)
			hasDays |= alarmDays[i];
		return !hasDays;
	}

	public Long getNextAlarmTime()
	{
		if (!getEnabled())
			return null;
		GregorianCalendar now = new GregorianCalendar(Locale.getDefault());

		long nextSnooze = getNextSnooze();
		if (nextSnooze > now.getTimeInMillis())
			return nextSnooze;

		GregorianCalendar first = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), getHour(), getMinutes(), 0);
		if (first.before(now))
			first.add(Calendar.DATE, 1);
		boolean[] daysOfWeek = getAlarmDays();
		boolean hasDays = !isOneShot(daysOfWeek);
		for (int i = 0; i < 7; i++)
		{
			GregorianCalendar cur = (GregorianCalendar) first.clone();
			cur.add(Calendar.DATE, i);
			int day = (cur.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7;
			if (!daysOfWeek[day] && hasDays)
				continue;
			return cur.getTimeInMillis();
		}
		return null;
	}

	public static final String ALARM_ALERT_ACTION = "com.koushikdutta.klaxon.ALARM_ALERT";

	public static AlarmSettings scheduleNextAlarm(Context context)
	{
		Long minAlarm = null;
		AlarmSettings minAlarmSettings = null;

		ArrayList<AlarmSettings> alarmSettings = getAlarms(context);
		for (AlarmSettings settings : alarmSettings)
		{
			Long alarmTime = settings.getNextAlarmTime();
			if (alarmTime == null || (minAlarm != null && alarmTime > minAlarm))
				continue;

			minAlarm = alarmTime;
			minAlarmSettings = settings;
		}

		Intent intent = new Intent(ALARM_ALERT_ACTION);
		if (minAlarmSettings != null)
		{
			intent.putExtra("AlarmId", minAlarmSettings.getId());
			intent.putExtra("AlarmTime", (long) minAlarm);
		}

		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
		if (minAlarmSettings != null)
		{
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			am.set(AlarmManager.RTC_WAKEUP, minAlarm, sender);
			alarmChanged.putExtra("alarmSet", true);

	        String timeString = formatDayAndTime(context, new java.util.Date(minAlarm));
			saveNextAlarm(context, timeString);
		}
		else
		{
			alarmChanged.putExtra("alarmSet", false);
			saveNextAlarm(context, null);
		}
		context.sendBroadcast(alarmChanged);
		return minAlarmSettings;
	}

	/**
	 * Shows day and time -- used for lock screen
	 */
	private static String formatDayAndTime(final Context context, Date dt)
	{
		SimpleDateFormat df;
		if (KlaxonSettings.is24HourMode(context))
			df = new SimpleDateFormat("EEE H:mm");
		else
			df = new SimpleDateFormat("EEE h:mm a");
		
		return df.format(dt);
	}

	/**
	 * Save time of the next alarm, as a formatted string, into the system
	 * settings so those who care can make use of it.
	 */
	static void saveNextAlarm(final Context context, String timeString)
	{
		Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, timeString);
	}

	public int getSnoozeTime()
	{
		return mPreferences.getInt("SnoozeTime", 10);
	}

	public void setSnoozeTime(int snoozeTime)
	{
		mEditor.putInt("SnoozeTime", snoozeTime);
		commit();
	}

	public void setNextSnooze(long nextSnooze)
	{
		mEditor.putLong("NextSnooze", nextSnooze);
		commit();
	}

	public long getNextSnooze()
	{
		return mPreferences.getLong("NextSnooze", 0);
	}

	public int getVolume()
	{
		return mPreferences.getInt("Volume", 100);
	}

	public void setVolume(int volume)
	{
		mEditor.putInt("Volume", volume);
		commit();
	}

	public int getVolumeRamp()
	{
		return mPreferences.getInt("VolumeRamp", 20);
	}

	public void setVolumeRamp(int volumeRamp)
	{
		mEditor.putInt("VolumeRamp", volumeRamp);
		commit();
	}

	public String getName()
	{
		return mPreferences.getString("Name", null);
	}

	public void setName(String name)
	{
		mEditor.putString("Name", name);
		commit();
	}
}
