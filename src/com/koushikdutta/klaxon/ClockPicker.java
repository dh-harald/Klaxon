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

import java.util.ArrayList;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;

/**
 * Clock face picker for the Alarm Clock application.
 */
public class ClockPicker extends Activity implements AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener
{

	private LayoutInflater mFactory;
	private Gallery mGallery;

	private View mClock;
	private ViewGroup mClockLayout;
	private int mPosition;
	KlaxonSettings mSettings;
	ArrayList<Integer> mClocks;

	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mSettings = new KlaxonSettings(this);
		mClocks = KlaxonSettings.getClocks();

		mFactory = LayoutInflater.from(this);
		setContentView(R.layout.clockpicker);

		mGallery = (Gallery) findViewById(R.id.gallery);
		mGallery.setAdapter(new ClockAdapter());
		mGallery.setOnItemSelectedListener(this);
		mGallery.setOnItemClickListener(this);

		mClockLayout = (ViewGroup) findViewById(R.id.clock_layout);
		mClockLayout.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				selectClock(mPosition);
			}
		});

		mGallery.setSelection(mClocks.indexOf(mSettings.getClockFace()), false);
	}

	public void onItemSelected(AdapterView parent, View v, int position, long id)
	{
		if (mClock != null)
		{
			mClockLayout.removeView(mClock);
		}
		mClock = mFactory.inflate(mClocks.get(position), null);
		mClockLayout.addView(mClock, 0);
		mPosition = position;
	}

	public void onItemClick(AdapterView parent, View v, int position, long id)
	{
		selectClock(position);
	}

	private synchronized void selectClock(int position)
	{
		mSettings.setClockFace(mClocks.get(position));
		finish();
	}

	public void onNothingSelected(AdapterView parent)
	{
	}

	class ClockAdapter extends BaseAdapter
	{

		public ClockAdapter()
		{
		}

		public int getCount()
		{
			ArrayList<Integer> clocks = mClocks;
			return mClocks.size();
		}

		public Object getItem(int position)
		{
			return position;
		}

		public long getItemId(int position)
		{
			return position;
		}

		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View clock = mFactory.inflate(mClocks.get(position), null);
			return clock;
		}

	}
}
