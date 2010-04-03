/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;

/**
 * Glue class: connects AlarmAlert IntentReceiver to AlarmAlert activity. Passes
 * through Alarm ID.
 */
public class AlarmReceiver extends BroadcastReceiver
{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.i("AlarmReceiver received has received a broadcast");
		if (intent.getAction() != null)
			Log.i(intent.getAction());
		try
		{
			if (intent.getAction() != null && !intent.getAction().equals(AlarmSettings.ALARM_ALERT_ACTION))
				return;
			if (intent.getBooleanExtra("sleepmode", false))
			{
				long alarmId = intent.getLongExtra(AlarmSettings.GEN_FIELD__ID, -1);
				AlarmSettings settings = AlarmSettings.getAlarmSettingsById(context, alarmId);
				if (settings == null)
					return;
				String sleepmode = settings.getSleepMode();
				if (sleepmode.equals("Airplane Mode"))
				{
					Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 1);
					AudioManager audio = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
					audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				}
				else if (sleepmode.equals("Vibrate"))
				{
					AudioManager audio = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
					audio.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
				}
				else if (sleepmode.equals("Silent"))
				{
					AudioManager audio = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
					audio.setRingerMode(AudioManager.RINGER_MODE_SILENT);
				}
			}
		}
		finally
		{
			AlarmSettings.scheduleNextAlarm(context);
		}
	}
}
