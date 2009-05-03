package com.koushikdutta.klaxon;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class SnoozePreference extends ListPreference
{
	AlarmSettings mSettings;
	OnSnoozeChangeListener mListener;
	int mSnoozeTime = 10;

	public interface OnSnoozeChangeListener
	{
		public void onSnoozeChanged();
	}

	public void setOnSnoozeChangeListener(OnSnoozeChangeListener listener)
	{
		mListener = listener;
	}

	public SnoozePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public void setAlarmSettings(AlarmSettings settings)
	{
		mSettings = settings;
	}

	@Override
	protected void onDialogClosed(boolean positiveResult)
	{
		if (positiveResult)
		{
			mSettings.setSnoozeTime(mSnoozeTime);
			if (mListener != null)
				mListener.onSnoozeChanged();
		}
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder)
	{
		CharSequence[] entries = getEntries();

		if (entries == null)
		{
			throw new IllegalStateException("SnoozePreference requires an entries array.");
		}
		mSnoozeTime = mSettings.getSnoozeTime();
		int index = Math.min(Math.max(mSnoozeTime / 5, 0), entries.length - 1);
		builder.setSingleChoiceItems(entries, index, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				if (which == 0)
					mSnoozeTime = 0;
				else
					mSnoozeTime = which * 5;
			}
		});
	}
}
