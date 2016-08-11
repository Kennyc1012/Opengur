package com.kenny.openimgur.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 8/12/15.
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        OpengurApp app = OpengurApp.getInstance(context);

        // We only care if we have a valid user
        if (app.getUser() != null) {
            LogUtil.v(TAG, "User present, creating notification alarm");
            AlarmReceiver.createNotificationAlarm(context);
        } else {
            LogUtil.v(TAG, "No user present, not creating notification alarm");
        }
    }
}
