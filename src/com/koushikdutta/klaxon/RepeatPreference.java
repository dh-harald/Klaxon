/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.koushikdutta.klaxon;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class RepeatPreference extends ListPreference
{
	AlarmSettings mSettings;
	boolean[] mDays;
	OnRepeatChangeListener mListener;

	public interface OnRepeatChangeListener
	{
		public void onRepeatChanged();
	}
	
	public void setOnRepeatChangeListener(OnRepeatChangeListener listener)
	{
		mListener = listener;
	}

	public RepeatPreference(Context context, AttributeSet attrs)
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
			mSettings.setAlarmDays(mDays);
			if (mListener != null)
				mListener.onRepeatChanged();
		}
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder)
	{
		CharSequence[] entries = getEntries();
		CharSequence[] entryValues = getEntryValues();

		if (entries == null || entryValues == null)
		{
			throw new IllegalStateException("RepeatPreference requires an entries array and an entryValues array.");
		}
		mDays = mSettings.getAlarmDays();
		builder.setMultiChoiceItems(entries, mDays, new DialogInterface.OnMultiChoiceClickListener()
		{
			public void onClick(DialogInterface dialog, int which, boolean isChecked)
			{
				mDays[which] = isChecked;
			}
		});
	}
}
