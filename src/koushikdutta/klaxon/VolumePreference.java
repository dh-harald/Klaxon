package koushikdutta.klaxon;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class VolumePreference extends ListPreference
{
	AlarmSettings mSettings;
	OnVolumeChangeListener mListener;
	int mVolume = 100;

	public interface OnVolumeChangeListener
	{
		public void onVolumeChanged();
	}

	public void setOnVolumeChangeListener(OnVolumeChangeListener listener)
	{
		mListener = listener;
	}

	public VolumePreference(Context context, AttributeSet attrs)
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
			mSettings.setVolume(mVolume);
			if (mListener != null)
				mListener.onVolumeChanged();
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
		mVolume = mSettings.getVolume();
		int index = Math.min(Math.max((mVolume - 50) / 10, 0), entries.length - 1);
		builder.setSingleChoiceItems(entries, index, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				mVolume = which * 10 + 50;
			}
		});
	}
}
