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

/**
 * package-level logging flag
 */

package koushikdutta.klaxon;

import android.os.SystemClock;
import android.util.Config;

class Log {
    public final static String LOGTAG = "Klaxon";

    static final boolean LOGV = AlarmClock.DEBUG ? Config.LOGD : Config.LOGV;

    static void v(String logMe) {
        android.util.Log.v(LOGTAG, /* SystemClock.uptimeMillis() + " " + */ logMe);
    }

    static void i(String logMe) {
        android.util.Log.i(LOGTAG, /* SystemClock.uptimeMillis() + " " + */ logMe);
    }
    
    static void w(String logMe) {
        android.util.Log.w(LOGTAG, /* SystemClock.uptimeMillis() + " " + */ logMe);
    }

    static void d(String logMe) {
        android.util.Log.d(LOGTAG, /* SystemClock.uptimeMillis() + " " + */ logMe);
    }

    static void e(String logMe) {
        android.util.Log.e(LOGTAG, logMe);
    }

    static void e(String logMe, Exception ex) {
        android.util.Log.e(LOGTAG, logMe, ex);
    }
}