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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Displays the time
 */
public class DigitalClock extends LinearLayout
{

	private final static String M12 = "h:mm";
	private final static String M24 = "k:mm";

	private Calendar mCalendar;
	private String mFormat;
	private TextView mTimeDisplay;
	private boolean mAnimate;
	private ContentObserver mFormatChangeObserver;
	private boolean mLive = true;
	private boolean mAttached;
	private LinearLayout mAmPmLayout;
	private TextView mAm, mPm;
	TextView mTime;
	int mColorOnVal;

	private Context mContext;

	public void setColorOn(int colorOn)
	{
		mColorOnVal = colorOn;
		ColorStateList clone = ColorStateList.valueOf(colorOn);
		mTimeDisplay.setTextColor(clone);
		updateTime();
	}

	/* called by system on minute ticks */
	private final Handler mHandler = new Handler();
	private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (mLive && intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED))
			{
				mCalendar = Calendar.getInstance();
			}
			updateTime();
		}
	};
	
	private class FormatChangeObserver extends ContentObserver
	{
		public FormatChangeObserver()
		{
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange)
		{
			setDateFormat();
			updateTime();
		}
	}

	public DigitalClock(Context context)
	{
		this(context, null);
	}

	boolean mIs24HourMode = false;

	public DigitalClock(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		mContext = context;
		String value = Settings.System.getString(mContext.getContentResolver(), Settings.System.TIME_12_24);
		mIs24HourMode = !(value == null || value.equals("12"));
	}

	@Override
	protected void onFinishInflate()
	{
		super.onFinishInflate();

		mTimeDisplay = (TextView) findViewById(R.id.timeDisplay);
		mCalendar = Calendar.getInstance();
		mAmPmLayout = (LinearLayout) findViewById(R.id.am_pm);
		mAm = (TextView) findViewById(R.id.am);
		mPm = (TextView) findViewById(R.id.pm);
		mColorOnVal = mAm.getTextColors().getDefaultColor();

		setDateFormat();
		updateTime();
	}

	@Override
	protected void onAttachedToWindow()
	{
		super.onAttachedToWindow();

		if (mAttached)
			return;
		mAttached = true;

		if (mLive)
		{
			/* monitor time ticks, time changed, timezone */
			IntentFilter filter = new IntentFilter();
			filter.addAction(Intent.ACTION_TIME_TICK);
			filter.addAction(Intent.ACTION_TIME_CHANGED);
			filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
			mContext.registerReceiver(mIntentReceiver, filter, null, mHandler);
		}

		/* monitor 12/24-hour display preference */
		mFormatChangeObserver = new FormatChangeObserver();
		mContext.getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, mFormatChangeObserver);
	}

	@Override
	protected void onDetachedFromWindow()
	{
		super.onDetachedFromWindow();

		if (!mAttached)
			return;
		mAttached = false;

		Drawable background = getBackground();
		if (background instanceof AnimationDrawable)
		{
			((AnimationDrawable) background).stop();
		}

		if (mLive)
		{
			mContext.unregisterReceiver(mIntentReceiver);
		}
		mContext.getContentResolver().unregisterContentObserver(mFormatChangeObserver);
	}

	void updateTime(Calendar c)
	{
		mCalendar = c;
		updateTime();
	}

	private void updateTime()
	{
		if (mLive)
		{
			mCalendar.setTimeInMillis(System.currentTimeMillis());
		}

		SimpleDateFormat df;
		if (mIs24HourMode)
			df = new SimpleDateFormat("H:mm");
		else
			df = new SimpleDateFormat("h:mm");

		CharSequence newTime = df.format(mCalendar.getTime());
		mTimeDisplay.setText(newTime);

		boolean isMorning = mCalendar.get(Calendar.AM_PM) == 0;
		ColorStateList mColorOn = ColorStateList.valueOf(mColorOnVal);
		ColorStateList mColorOff = mColorOn.withAlpha(0x40);
		mAm.setTextColor(isMorning ? mColorOn : mColorOff);
		mPm.setTextColor(isMorning ? mColorOff : mColorOn);
	}

	private void setDateFormat()
	{
		mAmPmLayout.setVisibility(!mIs24HourMode ? View.VISIBLE : View.GONE);
	}

	void setAnimate()
	{
		mAnimate = true;
	}

	public void setTextSize(float size)
	{
		mTimeDisplay.setTextSize(size);
	}

	void setLive(boolean live)
	{
		mLive = live;
	}
}
