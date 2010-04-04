package koushikdutta.klaxon;

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
