package com.kenny.openimgur.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateUtils;

import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;

/**
 * Created by kcampagna on 8/12/15.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = AlarmReceiver.class.getSimpleName();

    /**
     * Creates the alarm for fetching notifications
     *
     * @param context
     */
    public static void createNotificationAlarm(Context context) {
        SharedPreferences pref = OpengurApp.getInstance(context).getPreferences();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, RequestCodes.NOTIFICATION_ALARM, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long nextAlarm = getNextAlarmTime(pref);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextAlarm, pIntent);
        LogUtil.v(TAG, "Next notification alarm set for " + nextAlarm / DateUtils.MINUTE_IN_MILLIS + " minutes");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        OpengurApp app = OpengurApp.getInstance(context);

        if (app.getUser() != null) {
            LogUtil.v(TAG, "User present, fetching notifications");
            context.startService(NotificationService.createIntent(context));
        } else {
            LogUtil.v(TAG, "No user present, not fetching notifications");
        }
    }

    /**
     * Returns the next time for the notifications alarm
     *
     * @param pref
     * @return
     */
    private static long getNextAlarmTime(SharedPreferences pref) {
        String val = pref.getString(SettingsActivity.KEY_NOTIFICATION_FREQUENCY, SettingsActivity.NOTIFICATION_TIME_30);
        long updateTime = DateUtils.SECOND_IN_MILLIS;

        switch (val) {
            case SettingsActivity.NOTIFICATION_TIME_15:
                updateTime *= 15;
                break;

            case SettingsActivity.NOTIFICATION_TIME_60:
                updateTime *= 60;
                break;

            case SettingsActivity.NOTIFICATION_TIME_120:
                updateTime *= 120;
                break;

            case SettingsActivity.NOTIFICATION_TIME_30:
            default:
                updateTime *= 30;
                break;
        }

        return updateTime;
    }
}
