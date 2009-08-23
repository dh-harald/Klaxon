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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.Settings;

import com.antlersoft.android.dbimpl.NewInstance;

public class AlarmSettings extends AlarmSettingsBase {

	static class KlaxonDatabaseHelper extends SQLiteOpenHelper
	{
	    private static final String DATABASE_NAME = "klaxon.db";
	    private static final int DATABASE_VERSION = 1;

	    public KlaxonDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			createDatabase(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			createDatabase(db);
		}
		
		private void createDatabase(SQLiteDatabase db)
		{
			db.execSQL("DROP TABLE IF EXISTS " + GEN_TABLE_NAME);
			db.execSQL(AlarmSettingsBase.GEN_CREATE);
		}
	}
	
	static public ArrayList<AlarmSettings> getAlarms(final Context context)
	{
		ArrayList<AlarmSettings> alarms = new  ArrayList<AlarmSettings>();
		KlaxonDatabaseHelper helper = new KlaxonDatabaseHelper(context);
		final SQLiteDatabase db = helper.getWritableDatabase();
		
		AlarmSettings.getAll(db, AlarmSettings.GEN_TABLE_NAME, alarms, 
				new NewInstance<AlarmSettings>()
				{
					public AlarmSettings get() 
					{
						return new AlarmSettings(context, db);
					}
				});
		db.close();
		return alarms;
	}
	
	static public SQLiteDatabase getDatabase(final Context context)
	{
		KlaxonDatabaseHelper helper = new KlaxonDatabaseHelper(context);
		return helper.getWritableDatabase();
	}
	
	Context mContext;
	SQLiteDatabase mDatabase;
	public AlarmSettings(Context context, SQLiteDatabase database)
	{
		mContext = context;
		mDatabase = database;
		setAlarmDays(new boolean[] { true, true, true, true, true, false, false });
		setEnabled(true);
		setHour(new Date().getHours());
		setMinutes(new Date().getMinutes());
		setName("Alarm");
		setNextSnooze(0);
		setSnoozeTime(10);
		setVibrateEnabled(true);
		setVolume(100);
		setVolumeRamp(20);
	}
	
	public static AlarmSettings getAlarmSettingsById(Context context, SQLiteDatabase database, long id)
	{
		AlarmSettings ret = new AlarmSettings(context, database);
		Cursor cursor = database.rawQuery("SELECT * FROM " + GEN_TABLE_NAME + " WHERE " + GEN_FIELD__id + " = " + id, null);
		cursor.moveToNext();
		ret.populate(cursor);
		cursor.close();
		return ret;
	}
	
	public void populate(Cursor cursor)
	{
		Gen_populate(cursor, Gen_columnIndices(cursor));
	}
	
	public void delete()
	{
		Gen_delete(mDatabase);
	}
	
	public boolean[] getAlarmDays()
	{
		boolean[] ret = new boolean[7];
		int alarmDays = getAlarmDaysBase();
		for (int i = 0; i < 7; i++) 
		{
			if ((alarmDays & (1 << i)) != 0)
				ret[i] = true;
		}
		return ret;
	}
	
	public void setAlarmDays(boolean[] alarmDays)
	{
		int alarmDaysInt = 0;
		for (int i = 0; i < 7; i++)
		{
			if (alarmDays[i])
				alarmDaysInt |= (1 << i);
		}
		setAlarmDaysBase(alarmDaysInt);
	}
	
	public boolean isOneShot()
	{
		return isOneShot(getAlarmDays());
	}
	
	public static Cursor getCursor(SQLiteDatabase database)
	{
		return database.rawQuery("SELECT * FROM " + GEN_TABLE_NAME, null);
	}
	
	public boolean insert()
	{
		return Gen_insert(mDatabase);
	}
	
	public Uri getRingtone()
	{
		String uri = getRingtoneBase();
		if (uri == null)
			return null;
		return Uri.parse(uri);
	}
	
	public void setRingtone(Uri uri)
	{
		if (uri == null)
			setRingtoneBase(null);
		else
			setRingtoneBase(uri.toString());
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
			intent.putExtra("AlarmId", minAlarmSettings.get_Id());
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
	
	static boolean isOneShot(boolean[] alarmDays)
	{
		boolean hasDays = false;
		for (int i = 0; i < alarmDays.length; i++)
			hasDays |= alarmDays[i];
		return !hasDays;
	}
	
	public void update()
	{
		Gen_update(mDatabase);
	}
}
