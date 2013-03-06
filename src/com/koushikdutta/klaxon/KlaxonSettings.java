package com.koushikdutta.klaxon;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.Settings;
import android.text.format.DateFormat;

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
		return android.text.format.DateFormat.is24HourFormat(context);
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

	public void setFixWeather(boolean fix)
	{
		mEditor.putBoolean("FixWeather", fix);
		mEditor.commit();
	}

	public boolean getFixWeather()
	{
		return mPreferences.getBoolean("FixWeather", false);
	}
}
