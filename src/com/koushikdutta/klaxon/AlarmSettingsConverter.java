package com.koushikdutta.klaxon;

import android.content.Context;
import android.database.Cursor;

public class AlarmSettingsConverter
{
	Cursor mCursor;
	Context mContext;

	public AlarmSettingsConverter(Context context, Cursor cursor)
	{
		mContext = context;
		mCursor = cursor;
	}
	
	public int getHour()
	{
		return mCursor.getInt(mCursor.getColumnIndex("hour"));
	}

	public int getMinute()
	{
		return mCursor.getInt(mCursor.getColumnIndex("minute"));
	}
	
	public boolean[] getAlarmDays()
	{
		int alarmDaysInt =mCursor.getInt(mCursor.getColumnIndex("alarmdays"));
		
		boolean[] ret = new boolean[7];
		for (int i = 0; i < 7; i++)
		{
			ret[i] = (alarmDaysInt & (1 << i)) != 0;
		}
		
		return ret;
	}
	
	int getId()
	{
		return mCursor.getInt(mCursor.getColumnIndex("id"));
	}
	
	public boolean getEnabled()
	{
		return mCursor.getInt(mCursor.getColumnIndex("enabled")) != 0;
	}
	
	public void setEnabled()
	{
	}
}
