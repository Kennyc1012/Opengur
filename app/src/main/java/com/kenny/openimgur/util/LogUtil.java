package com.kenny.openimgur.util;

import android.util.Log;

import com.kenny.openimgur.BuildConfig;

/**
 * Created by kcampagna on 9/5/14.
 */
public class LogUtil {

    public static boolean SHOULD_WRITE_LOGS = BuildConfig.DEBUG;

    public static void onCreateApplication(boolean writeLogs) {
        SHOULD_WRITE_LOGS = writeLogs;
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (SHOULD_WRITE_LOGS) Log.v(tag, msg, tr);
    }

    public static void v(String tag, String msg) {
        if (SHOULD_WRITE_LOGS) Log.v(tag, msg);
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (SHOULD_WRITE_LOGS) Log.w(tag, msg, tr);
    }

    public static void w(String tag, String msg) {
        if (SHOULD_WRITE_LOGS) Log.w(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (SHOULD_WRITE_LOGS) Log.e(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        if (SHOULD_WRITE_LOGS) Log.e(tag, msg);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (SHOULD_WRITE_LOGS) Log.d(tag, msg, tr);
    }

    public static void d(String tag, String msg) {
        if (SHOULD_WRITE_LOGS) Log.d(tag, msg);
    }
}
