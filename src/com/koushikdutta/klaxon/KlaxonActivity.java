package com.koushikdutta.klaxon;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class KlaxonActivity extends Activity
{
	KlaxonSettings mSettings;
	MenuItem mAddAlarm;
	MenuItem mPreferences;
	MenuItem mBedClock;
	ListView mListView;
	LayoutInflater mInflater;
	AlarmAdapter mAdapter;

	class AlarmAdapter extends ArrayAdapter<AlarmSettings>
	{
		public AlarmAdapter(Context context)
		{
			super(context, R.id.TimeText);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			if (view == null)
				view = mInflater.inflate(R.layout.alarmtime, parent, false);
			final AlarmSettings settings = getItem(position);
			//DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
			SimpleDateFormat df;
			if (mIs24HourMode)
				df = new SimpleDateFormat("H:mm");
			else
				df = new SimpleDateFormat("h:mm");

			String time = df.format(new Date(2008, 1, 1, settings.getHour(), settings.getMinutes()));
			
			View panel12 = view.findViewById(R.id.Panel12Hour);
			panel12.setVisibility(!mIs24HourMode ? View.VISIBLE : View.INVISIBLE);
			
			TextView tv = (TextView) view.findViewById(R.id.TimeText);
			tv.setText(time);
			ColorStateList normalColors = tv.getTextColors();
			ColorStateList alphaColors = normalColors.withAlpha(0x40);
			TextView am = (TextView) view.findViewById(R.id.AMText);
			TextView pm = (TextView) view.findViewById(R.id.PMText);
			TextView name = (TextView) view.findViewById(R.id.NameText);
			String nameString = settings.getName();
			if (nameString == null || "".equals(nameString))
			{
				name.setVisibility(View.GONE);
			}
			else
			{
				name.setVisibility(View.VISIBLE);
				name.setText(settings.getName());
			}

			if (settings.getHour() >= 12)
			{
				am.setTextColor(alphaColors);
				pm.setTextColor(normalColors);
			}
			else
			{
				am.setTextColor(normalColors);
				pm.setTextColor(alphaColors);
			}
			boolean[] alarmDays = settings.getAlarmDays();
			for (int i = 0; i < alarmDays.length; i++)
			{
				TextView dv = (TextView) view.findViewById(R.id.Day0 + i);
				if (alarmDays[i])
					dv.setTextColor(normalColors);
				else
					dv.setTextColor(alphaColors);
			}
			CheckBox cb = (CheckBox) view.findViewById(R.id.EnabledCheckBox);
			cb.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					settings.setEnabled(isChecked);
					AlarmSettings.scheduleNextAlarm(KlaxonActivity.this);
				}
			});
			cb.setChecked(settings.getEnabled());

			final int index = position;
			view.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					EditAlarm(settings.getId(), index);
				}
			});

			view.setOnLongClickListener(new OnLongClickListener()
			{
				public boolean onLongClick(View arg0)
				{
					mAdapter.remove(settings);
					settings.Delete();
					return true;
				}
			});

			boolean isOneShot = settings.isOneShot();
			view.findViewById(R.id.AlarmDays).setVisibility(isOneShot ? View.GONE : View.VISIBLE);
			view.findViewById(R.id.OneShot).setVisibility(isOneShot ? View.VISIBLE : View.GONE);

			return view;
		}
	}

	LinearLayout mClockLayout;
	boolean mIs24HourMode = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String value = Settings.System.getString(getContentResolver(), Settings.System.TIME_12_24);
		mIs24HourMode = !(value == null || value.equals("12"));

		mSettings = new KlaxonSettings(this);

		if (mSettings.getIsFirstStart())
		{
			mSettings.setIsFirstStart(false);

			ArrayList<String> alarmIds = mSettings.getAlarmIds();

			boolean alarmDays[] = new boolean[] { true, true, true, true, true, false, false };

			String id = UUID.randomUUID().toString();
			AlarmSettings newSet = new AlarmSettings(this, id);
			newSet.setEnabled(false);
			newSet.setHour(8);
			newSet.setMinutes(0);
			newSet.setAlarmDays(alarmDays);
			alarmIds.add(id);

			mSettings.setAlarmIds(alarmIds);
		}

		setContentView(R.layout.klaxonactivity);
		mInflater = LayoutInflater.from(this);
		mClockLayout = (LinearLayout) findViewById(R.id.ClockLayout);
		mClockLayout.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				Intent intent = new Intent(KlaxonActivity.this, ClockPicker.class);
				startActivityForResult(intent, REQUEST_CLOCK_FACE);
			}
		});
		refreshClockFace();

		mAdapter = new AlarmAdapter(this);
		mListView = (ListView) findViewById(R.id.AlarmList);
		mListView.setAdapter(mAdapter);
		mListView.setVerticalScrollBarEnabled(true);
		mListView.setItemsCanFocus(true);
		RefreshAlarms();

		AlarmSettings.scheduleNextAlarm(this);
	}

	void refreshClockFace()
	{
		mClockLayout.removeAllViews();
		int id = mSettings.getClockFace();
		mInflater.inflate(id, mClockLayout);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		mAddAlarm = menu.add(0, 0, 0, "Add Alarm");
		mAddAlarm.setIcon(android.R.drawable.ic_menu_add);

		mBedClock = menu.add("Bed Clock");
		mBedClock.setIcon(android.R.drawable.ic_menu_today);
		
		// mPreferences = menu.add(0, 0, 0, "Settings");
		// mPreferences.setIcon(android.R.drawable.ic_menu_preferences);

		return true;
	}

	void EditAlarm(String alarmId, int position)
	{
		Intent intent = new Intent(this, AlarmEditActivity.class);
		intent.putExtra("Id", alarmId);
		intent.putExtra("Position", position);
		startActivityForResult(intent, REQUEST_ALARM_EDIT);
	}

	static final int REQUEST_ALARM_EDIT = 0;
	static final int REQUEST_CLOCK_FACE = 1;

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		if (item == mAddAlarm)
		{
			String id = UUID.randomUUID().toString();
			ArrayList<String> alarmIds = mSettings.getAlarmIds();
			alarmIds.add(id);
			mSettings.setAlarmIds(alarmIds);
			AlarmSettings settings = new AlarmSettings(this, id);
			settings.setHour(new Date().getHours());
			settings.setMinutes(new Date().getMinutes());
			mAdapter.add(settings);
			EditAlarm(id, mAdapter.getCount() - 1);
		}
		else if (item == mPreferences)
		{

		}
		else if (item == mBedClock)
		{
			Intent i = new Intent(this, ClockActivity.class);
			startActivity(i);
		}

		return super.onMenuItemSelected(featureId, item);
	}

	void RefreshAlarms()
	{
		ArrayList<AlarmSettings> settings = AlarmSettings.getAlarms(this);
		Object alarmSettings = settings.toArray();

		mAdapter.clear();
		for (AlarmSettings setting : settings)
		{
			mAdapter.add(setting);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_ALARM_EDIT)
		{
			if (data != null)
			{
				Bundle extras = data.getExtras();
				String id = extras.getString("Id");
				int position = extras.getInt("Position", -1);
				if (id != null && position != -1)
				{
					AlarmSettings item = mAdapter.getItem(position);
					boolean deleted = extras.getBoolean("Deleted");
					mAdapter.remove(item);
					if (!deleted)
						mAdapter.insert(item, position);
					else
						item.Delete();
				}
				AlarmSettings.scheduleNextAlarm(this);
			}
		}
		else if (requestCode == REQUEST_CLOCK_FACE)
		{
			refreshClockFace();
		}

		super.onActivityResult(requestCode, resultCode, data);
	}
}