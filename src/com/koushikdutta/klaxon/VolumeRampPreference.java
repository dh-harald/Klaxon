package com.koushikdutta.klaxon;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class VolumeRampPreference extends ListPreference
{
	AlarmSettings mSettings;
	OnVolumeRampChangeListener mListener;
	int mVolumeRamp = 0;

	public interface OnVolumeRampChangeListener
	{
		public void onVolumeRampChanged();
	}

	public void setOnVolumeRampChangeListener(OnVolumeRampChangeListener listener)
	{
		mListener = listener;
	}

	public VolumeRampPreference(Context context, AttributeSet attrs)
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
			mSettings.setVolumeRamp(mVolumeRamp);
			if (mListener != null)
				mListener.onVolumeRampChanged();
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
		mVolumeRamp = mSettings.getVolumeRamp();
		int index = Math.min(Math.max(mVolumeRamp / 10, 0), entries.length - 1);
		builder.setSingleChoiceItems(entries, index, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				mVolumeRamp = which * 10;
			}
		});
	}
}
