package koushikdutta.klaxon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import koushikdutta.klaxon.RepeatPreference.OnRepeatChangeListener;
import koushikdutta.klaxon.SnoozePreference.OnSnoozeChangeListener;
import koushikdutta.klaxon.VolumePreference.OnVolumeChangeListener;
import koushikdutta.klaxon.VolumeRampPreference.OnVolumeRampChangeListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.AlertDialog.Builder;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

public class AlarmEditActivity extends PreferenceActivity
{
	MenuItem mDeleteAlarm;
	MenuItem mTestAlarm;
	AlarmSettings mSettings;
	Preference mTimePref;
	Preference mRingtonePref;
	Preference mNamePref;
	RepeatPreference mRepeatPref;
	SnoozePreference mSnoozePref;
	CheckBoxPreference mEnabledPref;
	VolumePreference mVolumePref;
	VolumeRampPreference mVolumeRampPref;
	CheckBoxPreference mVibrateEnabledPref;
	Preference mExpirePref;
	Preference mSleepModePref;
	boolean mIs24HourMode = false;
	SQLiteDatabase mDatabase;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.alarm_prefs);
		Intent i = getIntent();
		mDatabase = AlarmSettings.getDatabase(this);
		mSettings = AlarmSettings.getAlarmSettingsById(this, mDatabase, i.getLongExtra(AlarmSettings.GEN_FIELD__ID, -1));
		setResult(Activity.RESULT_OK, i);

		mNamePref = findPreference("alarmname");
		mTimePref = findPreference("time");
		mRingtonePref = findPreference("alarm");
		mRepeatPref = (RepeatPreference) findPreference("repeat");
		mRepeatPref.setAlarmSettings(mSettings);
		mSnoozePref = (SnoozePreference) findPreference("snooze");
		mSnoozePref.setAlarmSettings(mSettings);
		mEnabledPref = (CheckBoxPreference) findPreference("enabled");
		mEnabledPref.setChecked(mSettings.getEnabled());
		mVolumePref = (VolumePreference) findPreference("volume");
		mVolumePref.setAlarmSettings(mSettings);
		mVolumeRampPref = (VolumeRampPreference) findPreference("volumeramp");
		mVolumeRampPref.setAlarmSettings(mSettings);
		mVibrateEnabledPref = (CheckBoxPreference) findPreference("VibrateEnabled");
		mVibrateEnabledPref.setChecked(mSettings.getVibrateEnabled());
		
		mExpirePref = findPreference("expiretime");
		mExpirePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			int expireIndex = mSettings.getExpireTime() / 5;
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new Builder(AlarmEditActivity.this);
				builder.setTitle(R.string.expire);
				builder.setSingleChoiceItems(R.array.expire_times, expireIndex, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						expireIndex = which;
					}
				});
				builder.setCancelable(true);
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mSettings.setExpireTime(expireIndex * 5);
						mSettings.update();
						refreshExpireSummary();
					}
				});
				builder.create().show();
				return true;
			}
		});
		
		mSleepModePref = findPreference("sleepmode");
		mSleepModePref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			int mFlightModeTime = mSettings.getSleepLeadTime();
			String mFlightMode = "Silent";
			
			public boolean onPreferenceClick(Preference preference) {
				AlertDialog.Builder builder = new Builder(AlarmEditActivity.this);
				builder.setTitle(R.string.sleepmode);
				builder.setSingleChoiceItems(R.array.sleep_mode_times, mFlightModeTime, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mFlightModeTime = which;
					}
				});
				builder.setNegativeButton(android.R.string.cancel, null);
				builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (mFlightModeTime == 0)
						{
							mSettings.setSleepLeadTime(0);
							refreshFlightModeSummary();
							return;
						}
						AlertDialog.Builder builder = new Builder(AlarmEditActivity.this);
						builder.setTitle(R.string.sleepmode);
						builder.setSingleChoiceItems(R.array.sleep_modes, 0, new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								mFlightMode = getResources().getStringArray(R.array.sleep_modes)[which];
							}
						});
						builder.setNegativeButton(android.R.string.cancel, null);
						builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								mSettings.setSleepLeadTime(mFlightModeTime);
								mSettings.setSleepMode(mFlightMode);
								mSettings.update();
								scheduleNextAlarm();
								refreshFlightModeSummary();
							}
						});
						builder.create().show();
					}
				});
				builder.create().show();
				return true;
			}
		});
		refreshFlightModeSummary();

		refreshNameSummary();
		refreshTimeSummary();
		refreshRingtoneSummary();
		refreshRepeatSummary();
		refreshSnoozeSummary();
		refreshVolumeSummary();
		refreshVolumeRampSummary();
		refreshExpireSummary();

		mRepeatPref.setOnRepeatChangeListener(new OnRepeatChangeListener()
		{
			public void onRepeatChanged()
			{
				scheduleNextAlarm();
				refreshRepeatSummary();
			}
		});

		mSnoozePref.setOnSnoozeChangeListener(new OnSnoozeChangeListener()
		{
			public void onSnoozeChanged()
			{
				scheduleNextAlarm();
				refreshSnoozeSummary();
			}
		});

		mVolumePref.setOnVolumeChangeListener(new OnVolumeChangeListener()
		{
			public void onVolumeChanged()
			{
				mSettings.update();
				refreshVolumeSummary();
			}
		});

		mVolumeRampPref.setOnVolumeRampChangeListener(new OnVolumeRampChangeListener()
		{
			public void onVolumeRampChanged()
			{
				mSettings.update();
				refreshVolumeRampSummary();
			}
		});

		String value = Settings.System.getString(getContentResolver(), Settings.System.TIME_12_24);
		mIs24HourMode = !(value == null || value.equals("12"));
		
		Toast tipToast = Toast.makeText(this, "Tip: You can create a one shot alarm by not selecting any Repeat days.", Toast.LENGTH_LONG);
		tipToast.show();
	}
	
	void refreshFlightModeSummary()
	{
		int flightModeTime = mSettings.getSleepLeadTime();
		String flightMode = mSettings.getSleepMode();
		if (flightModeTime == 0 || flightMode == null || (!flightMode.equals("Airplane Mode") && !flightMode.equals("Silent") && !flightMode.equals("Vibrate")))
		{
			mSleepModePref.setSummary(R.string.flight_mode_off);
		}
		else
		{
			mSleepModePref.setSummary(getString(R.string.sleep_mode_summary, flightMode, flightModeTime));
		}
	}
	
	void refreshExpireSummary()
	{
		int expire = mSettings.getExpireTime();
		if (expire == 0)
		{
			mExpirePref.setSummary(R.string.never_expire);
		}
		else
		{
			mExpirePref.setSummary(getString(R.string.expires_after, expire));
		}
	}

	void refreshRepeatSummary()
	{
		boolean[] days = mSettings.getAlarmDays();
		boolean oneShot = AlarmSettings.isOneShot(days);
		String summary = "";
		if (oneShot)
		{
			summary = "One-Shot Alarm";
		}
		else
		{
			final String[] dayNames = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
			for (int i = 0; i < days.length; i++)
			{
				if (days[i])
					summary += dayNames[i] + " ";
			}
		}

		mRepeatPref.setSummary(summary);
	}

	void refreshVolumeSummary()
	{
		String volumeText = String.format("%d Percent", mSettings.getVolume());
		mVolumePref.setSummary(volumeText);
	}

	void refreshVolumeRampSummary()
	{
		int volumeRamp = mSettings.getVolumeRamp();
		if (volumeRamp != 0)
		{
			String volumeRampText = String.format("%d Seconds", volumeRamp);
			mVolumeRampPref.setSummary(volumeRampText);
		}
		else
		{
			mVolumeRampPref.setSummary("Immediately");
		}
	}

	void refreshSnoozeSummary()
	{
		String snoozeText = String.format("%d Minutes", mSettings.getSnoozeTime());
		mSnoozePref.setSummary(snoozeText);
	}

	void refreshTimeSummary()
	{
		SimpleDateFormat df;
		if (mIs24HourMode)
			df = new SimpleDateFormat("H:mm");
		else
			df = new SimpleDateFormat("h:mm a");
		String time = df.format(new Date(2008, 1, 1, mSettings.getHour(), mSettings.getMinutes()));
		mTimePref.setSummary(time);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		mDeleteAlarm = menu.add(0, 0, 0, "Delete Alarm");
		mDeleteAlarm.setIcon(android.R.drawable.ic_menu_delete);
		
		mTestAlarm = menu.add(0, 0, 0, "Test Alarm");
		mTestAlarm.setIcon(R.drawable.ic_menu_clock_face);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if (item == mDeleteAlarm)
		{
			mSettings.delete();
			Intent ret = getIntent();
			ret.putExtra("Deleted", true);
			finish();
		}
		else if (item == mTestAlarm)
		{
			Intent i = new Intent(this, AlarmService.class);
			i.putExtra(AlarmSettings.GEN_FIELD__ID, mSettings.get_Id());
			i.putExtra("AlarmTime", (long)System.currentTimeMillis());
			startService(i);
		}

		return super.onMenuItemSelected(featureId, item);
	}

	void scheduleNextAlarm()
	{
		mSettings.update();

		Long next = mSettings.getNextAlarmTime(false);
		String toastText;
		if (next == null)
		{
			toastText = "This Alarm is disabled.";
		}
		else
		{
			GregorianCalendar now = new GregorianCalendar();
			long nowMs = now.getTimeInMillis();
			long delta = next - nowMs;
			long hours = delta / (1000 * 60 * 60) % 24;
			long minutes = delta / (1000 * 60) % 60;
			long days = delta / (1000 * 60 * 60 * 24);
			if (mSettings.isOneShot())
				toastText = String.format("This one shot alarm will fire in %d days, %d hours, and %d minutes.", days, hours, minutes);
			else
				toastText = String.format("This alarm will fire in %d days, %d hours, and %d minutes.", days, hours, minutes);
		}

		Toast toast = Toast.makeText(this, toastText, Toast.LENGTH_LONG);
		toast.show();
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog d;

		switch (id)
		{
		case DIALOG_TIMEPICKER:
			d = new TimePickerDialog(AlarmEditActivity.this, new OnTimeSetListener()
			{
				public void onTimeSet(TimePicker view, int hourOfDay, int minute)
				{
					mSettings.setHour(hourOfDay);
					mSettings.setMinutes(minute);
					mSettings.setEnabled(true);
					mEnabledPref.setChecked(true);
					refreshTimeSummary();
					scheduleNextAlarm();
				}
			}, 0, 0, false);
			d.setTitle("Time");
			break;
		case DIALOG_NAMEEDITOR:
			final View layout = View.inflate(this, R.layout.alertname, null);
			final EditText input = (EditText) layout.findViewById(R.id.AlarmName);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setIcon(0);
			builder.setTitle("Edit Alarm Name");
			builder.setCancelable(true);
			builder.setPositiveButton("OK", new Dialog.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int which)
				{
					mSettings.setName(input.getText().toString());
					mSettings.update();
					refreshNameSummary();
				}
			});
			builder.setView(layout);
			d = builder.create();
			break;
		case DIALOG_RINGTONEPICKER:
		{
			d = new Dialog(this)
			{
				Button mRingtoneButton;
				Button mMusicButton;

				@Override
				protected void onCreate(Bundle savedInstanceState)
				{
					super.onCreate(savedInstanceState);
					setContentView(R.layout.alerttype);
					setTitle("Sound Type");
					mRingtoneButton = (Button) findViewById(R.id.RingtoneButton);
					mMusicButton = (Button) findViewById(R.id.SongButton);
					mRingtoneButton.setOnClickListener(new Button.OnClickListener()
					{
						public void onClick(View v)
						{
							dismiss();
							Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
							intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSettings.getRingtone());
							intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false);
							intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
							intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
							startActivityForResult(intent, REQUEST_RINGTONE);
						}
					});
					mMusicButton.setOnClickListener(new Button.OnClickListener()
					{
						public void onClick(View v)
						{
							dismiss();
							try
							{
								Intent intent = new Intent();
								intent.setAction(Intent.ACTION_PICK);
								intent.setClassName("com.android.music", "com.android.music.MusicPicker");
								intent.setData(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
								Uri ringtone = mSettings.getRingtone();
								if (ringtone != null)
								{
									intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, ringtone);
								}
								startActivityForResult(intent, REQUEST_MUSIC);
							}
							catch (Exception ex)
							{
								Intent intent = new Intent(AlarmEditActivity.this, TrackBrowserActivity.class);
								Uri ringtone = mSettings.getRingtone();
								if (ringtone != null)
								{
								    String titleUri = ringtone.toString();
								    intent.putExtra("TitleUri", titleUri);
								}
								startActivityForResult(intent, REQUEST_MUSIC);
							}
						}
					});
				}
			};
			break;
		}
		default:
			d = null;
		}

		return d;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		super.onPrepareDialog(id, dialog);

		switch (id)
		{
		case DIALOG_TIMEPICKER:
			TimePickerDialog timePicker = (TimePickerDialog) dialog;
			timePicker.updateTime(mSettings.getHour(), mSettings.getMinutes());
			break;
		case DIALOG_NAMEEDITOR:
			final EditText input = (EditText) dialog.findViewById(R.id.AlarmName);
			input.setText(mSettings.getName());
			break;
		}
	}

	final static int DIALOG_TIMEPICKER = 0;
	final static int DIALOG_RINGTONEPICKER = 1;
	final static int DIALOG_NAMEEDITOR = 2;

	final static int REQUEST_RINGTONE = 0;
	final static int REQUEST_MUSIC = 1;

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
	{
		if (preference == mTimePref)
		{
			showDialog(DIALOG_TIMEPICKER);
		}
		else if (preference == mRingtonePref)
		{
			showDialog(DIALOG_RINGTONEPICKER);
		}
		else if (preference == mEnabledPref)
		{
			mSettings.setEnabled(mEnabledPref.isChecked());
			scheduleNextAlarm();
		}
		else if (preference == mVibrateEnabledPref)
		{
			mSettings.setVibrateEnabled(mVibrateEnabledPref.isChecked());
			mSettings.update();
		}
		else if (preference == mNamePref)
		{
			showDialog(DIALOG_NAMEEDITOR);
		}

		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_RINGTONE)
		{
			if (resultCode != RESULT_CANCELED)
			{
				Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
				if (uri != null)
				{
					mSettings.setRingtone(uri);
					mSettings.update();
					Ringtone rt = RingtoneManager.getRingtone(this, uri);
					mRingtonePref.setSummary(rt.getTitle(this));
				}
			}
		}
		else if (requestCode == REQUEST_MUSIC)
		{
			if (data != null)
			{
				Uri uri = data.getData();
				if (uri != null)
				{
					mSettings.setRingtone(uri);
					mSettings.update();
					refreshRingtoneSummary();
				}
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	void refreshNameSummary()
	{
		mNamePref.setSummary(mSettings.getName());
	}

	void refreshRingtoneSummary()
	{
		Uri ringtone = mSettings.getRingtone();
		if (ringtone != null)
		{
			Cursor c = getContentResolver().query(ringtone, null, null, null, null);
			if (c != null)
			{
				if (!c.isLast())
				{
					c.moveToNext();
					int titleIndex = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
					try
					{
						mRingtonePref.setSummary(c.getString(titleIndex));
					}
					catch(Exception ex)
					{
					}
					startManagingCursor(c);
				}
			}
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mDatabase.close();
	}
}
