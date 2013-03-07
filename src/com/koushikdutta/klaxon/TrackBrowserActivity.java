package com.koushikdutta.klaxon;

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class TrackBrowserActivity extends ListActivity
{
	RadioGroup mGroup;
	RingtoneManager mManager;
	Cursor mCursor;

	public class TrackAdapter extends CursorAdapter
	{
		public TrackAdapter(Context context, Cursor c)
		{
			super(context, c);
		}

		RadioButton mCurrentChecked;

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent)
		{
			View view = mInflater.inflate(R.layout.track, null);
			view.setBackgroundResource(android.R.drawable.menuitem_background);
			view.setFocusable(true);
			view.setClickable(true);
			TextView title = (TextView) view.findViewById(R.id.TrackTitle);
			title.setTextSize(20);
			final RadioButton button = (RadioButton) view.findViewById(R.id.SelectTrackButton);
			view.setOnClickListener(new View.OnClickListener()
			{
				public void onClick(View v)
				{
					button.setChecked(true);
				}
			});
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor)
		{
			TextView title = (TextView) view.findViewById(R.id.TrackTitle);
			String titleText = cursor.getString(TITLE_INDEX);
			title.setText(titleText);
			final Cursor thisCursor = cursor;
			long id = thisCursor.getLong(ID_INDEX);
			final RadioButton button = (RadioButton) view.findViewById(R.id.SelectTrackButton);
			final Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
			final String uriString = uri.toString();
			button.setOnCheckedChangeListener(new RadioButton.OnCheckedChangeListener()
			{
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
				{
					if (isChecked)
					{
						if (mCurrentChecked != null && mCurrentChecked != button)
							mCurrentChecked.setChecked(false);
						mCurrentChecked = button;
						String oldUriString = mCurrentUri;
						String localUriString = mCurrentUri = uriString;
						mIntent.putExtra("TitleUri", uriString);
						mIntent.setData(uri);
						// don't start the player if it is null; that implies that the sound is being selected upon init
						if (mMediaPlayer != null)
						{
							try
							{
								mMediaPlayer.stop();
							}
							catch (Exception ex)
							{
							}
							mMediaPlayer = MediaPlayer.create(TrackBrowserActivity.this, uri);
							mMediaPlayer.setLooping(true);
							mMediaPlayer.start();
						}
						else
						{
							// initialize it so the next time a sound is chosen, it plays
							mMediaPlayer = MediaPlayer.create(TrackBrowserActivity.this, uri);
						}
					}
				}
			});

			final String currentUri = mCurrentUri;
			if (mCurrentUri != null)
				button.setChecked(mCurrentUri.equals(uriString));
			else
				button.setChecked(true);
		}
	}

	int TITLE_INDEX;
	int ID_INDEX;
	String mCurrentUri;
	Intent mIntent;
	MediaPlayer mMediaPlayer;
	LayoutInflater mInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mInflater = LayoutInflater.from(this);
		mManager = new RingtoneManager(this);
		mIntent = getIntent();
		setResult(Activity.RESULT_OK, mIntent);
		Bundle bundle = mIntent.getExtras();
		if (bundle != null)
			mCurrentUri = bundle.getString("TitleUri");

		ListView lv = new ListView(this);
		lv.setId(android.R.id.list);
		lv.setItemsCanFocus(true);
		setContentView(lv);

		mCursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
		//startManagingCursor(mCursor);
		if (mCursor != null)
		{
			ID_INDEX = mCursor.getColumnIndex(MediaStore.Audio.Media._ID);
			TITLE_INDEX = mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
			TrackAdapter adapter = new TrackAdapter(this, mCursor);
			setListAdapter(adapter);
		}
	}

	@Override
	protected void onDestroy()
	{
		try
		{
			if (mMediaPlayer != null)
				mMediaPlayer.stop();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}

		super.onDestroy();
	}
}
