package com.kenny.openimgur.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 8/12/15.
 */
public class NotificationService extends IntentService {
    private static final String TAG = NotificationService.class.getSimpleName();

    public static Intent createIntent(Context context) {
        return new Intent(context, NotificationService.class);
    }

    public NotificationService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        OpengurApp app = OpengurApp.getInstance(getApplicationContext());

        // Make sure we have a valid user
        if (app.getUser() != null) {
            try {
                NotificationResponse response = ApiClient.getService().getNotifications();

                if (response != null && response.data != null) {
                    app.getSql().insertNotifications(response);
                    createNotification(response.data);
                } else {
                    LogUtil.v(TAG, "No notifications found");
                }
            } catch (Exception ex) {
                LogUtil.e(TAG, "Error fetching notifications", ex);
            }

            // Create the next alarm when everything is finished
            AlarmReceiver.createNotificationAlarm(getApplicationContext());
        } else {
            LogUtil.w(TAG, "Can not fetch notifications, no user found");
        }
    }

    private void createNotification(@NonNull NotificationResponse.Data data) {
        int messageNotifications = data.messages.size();
        int replyNotifications = data.replies.size();

        // For only one notification, we will show the content
        if (replyNotifications + messageNotifications == 1) {
            // TODO
        } else {

        }
    }
}
