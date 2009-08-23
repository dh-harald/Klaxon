package com.koushikdutta.klaxon;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;

public class KlaxonSettings
{
	SharedPreferences mPreferences;
	Editor mEditor;
	Context mContext;

	public KlaxonSettings(Context context)
	{
		mContext = context;
		mPreferences = mContext.getSharedPreferences("Klaxon", Context.MODE_PRIVATE);
		mEditor = mPreferences.edit();

	}

	boolean mIs24HourMode = false;

	static boolean is24HourMode(Context context)
	{
		String value24 = Settings.System.getString(context.getContentResolver(), Settings.System.TIME_12_24);
		return  !(value24 == null || value24.equals("12"));
	}

	static public ArrayList<Integer> getClocks()
	{
		ArrayList<Integer> ret = new ArrayList<Integer>();
		ret.add(R.layout.clockgoog);
		ret.add(R.layout.clockdroids);
		ret.add(R.layout.clockdroid2);
		ret.add(R.layout.clockhome);
		ret.add(R.layout.clockdefault);
		ret.add(R.layout.digital_clock);
		return ret;
	}

	public boolean getIsFirstStart()
	{
		return mPreferences.getBoolean("IsFirstStart", true);
	}

	public void setIsFirstStart(boolean value)
	{
		mEditor.putBoolean("IsFirstStart", value);
		mEditor.commit();
	}

	public int getClockFace()
	{
		int clock = mPreferences.getInt("ClockFace", R.layout.clockgoog);
		if (!getClocks().contains(clock))
		{
			clock = R.layout.clockgoog;
			mEditor.putInt("clocklayout", clock);
			mEditor.commit();
		}
		return clock;
	}

	public void setClockFace(int clockFace)
	{
		mEditor.putInt("ClockFace", clockFace);
		mEditor.commit();
	}
	
	public int getBedClockColor(int defaultColor)
	{
		return mPreferences.getInt("BedClockColor", defaultColor);
	}
}
