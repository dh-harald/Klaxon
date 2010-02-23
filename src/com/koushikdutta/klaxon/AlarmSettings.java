package com.koushikdutta.klaxon;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import android.app.Activity;
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
			return;
			/*
			// see which upgrade paths we can handle
			switch (newVersion)
			{
			case 1:
			case 2:
				break;
			default:
				createDatabase(db);
				return;
			}
			
			if (oldVersion < 2)
			{
				
			}
			*/
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
		Cursor cursor = database.rawQuery("SELECT * FROM " + GEN_TABLE_NAME + " WHERE " + GEN_FIELD__ID + " = " + id, null);
		if (!cursor.moveToNext())
		{
			cursor.close();
			return null;
		}
		ret.populate(cursor);
		cursor.close();
		return ret;
	}

	public static AlarmSettings getAlarmSettingsById(Context context, long id)
	{
		SQLiteDatabase database = getDatabase(context);
		try
		{
			return getAlarmSettingsById(context, database, id);
		}
		finally
		{
			database.close();
		}
	}
	
	public void populate(Cursor cursor)
	{
		Gen_populate(cursor, Gen_columnIndices(cursor));
	}
	
	public void delete()
	{
		Gen_delete(mDatabase);
		scheduleNextAlarm(mContext);
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
		boolean ret = Gen_insert(mDatabase);
		scheduleNextAlarm(mContext);
		return ret;
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
			if (nextSnooze > now.getTimeInMillis() && nextSnooze < cur.getTimeInMillis())
				return nextSnooze;
			return cur.getTimeInMillis();
		}
		return null;
	}
	
	public static final String ALARM_ALERT_ACTION = "com.koushikdutta.klaxon.ALARM_ALERT";
	public static AlarmSettings scheduleNextAlarm(Context context)
	{
		Log.v("Scheduling next alarm");
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
			intent.putExtra(GEN_FIELD__ID, minAlarmSettings.get_Id());
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
	public static String formatDayAndTime(final Context context, Date dt)
	{
		SimpleDateFormat df;
		if (KlaxonSettings.is24HourMode(context))
			df = new SimpleDateFormat("EEE H:mm");
		else
			df = new SimpleDateFormat("EEE h:mm a");
		
		return df.format(dt);
	}
	
	public static String formatLongDayAndTime(final Context context, Date dt)
	{
		SimpleDateFormat df;
		if (KlaxonSettings.is24HourMode(context))
			df = new SimpleDateFormat("EEEE H:mm");
		else
			df = new SimpleDateFormat("EEEE h:mm a");
		
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
		scheduleNextAlarm(mContext);
	}
	
	
    private static int[] DAY_MAP = new int[] {
        Calendar.MONDAY,
        Calendar.TUESDAY,
        Calendar.WEDNESDAY,
        Calendar.THURSDAY,
        Calendar.FRIDAY,
        Calendar.SATURDAY,
        Calendar.SUNDAY,
    };
    
    public String getDaysOfWeekString(Context context, boolean showNever) {
        StringBuilder ret = new StringBuilder();

        
        int alarmDaysInt = getAlarmDaysBase();
        
        // no days
        if (alarmDaysInt == 0) {
            return showNever ?
                    context.getText(R.string.never).toString() : "";
        }

        // every day
        if (alarmDaysInt == 0x7f) {
            return context.getText(R.string.every_day).toString();
        }

        // count selected days
        int dayCount = 0, days = alarmDaysInt;
        while (days > 0) {
            if ((days & 1) == 1) dayCount++;
            days >>= 1;
        }

        // short or long form?
        DateFormatSymbols dfs = new DateFormatSymbols();
        String[] dayList = (dayCount > 1) ?
                dfs.getShortWeekdays() :
                dfs.getWeekdays();

        // selected days
        for (int i = 0; i < 7; i++) {
            if ((alarmDaysInt & (1 << i)) != 0) {
                ret.append(dayList[DAY_MAP[i]]);
                dayCount -= 1;
                if (dayCount > 0) ret.append(
                        context.getText(R.string.day_concat));
            }
        }
        return ret.toString();
    }
    
  	public static final int REQUEST_ALARM_EDIT = 100;
  	public static final int REQUEST_CLOCK_FACE = 101;
  	public static void editAlarm(Activity activity, long alarmId)
    {
        Intent intent = new Intent(activity, AlarmEditActivity.class);
		intent.putExtra(AlarmSettings.GEN_FIELD__ID, alarmId);
		activity.startActivityForResult(intent, REQUEST_ALARM_EDIT);
	} 
}
