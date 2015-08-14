package com.kenny.openimgur.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.ui.BaseNotification;
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
        String title;
        String message;

        if (messageNotifications > 0 && replyNotifications > 0) {
            int totalNotifications = messageNotifications + replyNotifications;
            title = getString(R.string.notifications_multiple_types, totalNotifications);
            message = getString(R.string.notifications_multiple_types_body, totalNotifications);
        } else if (messageNotifications > 0) {
            boolean hasOnlyOne = messageNotifications == 1;
            ImgurMessage imgurMessage = data.messages.get(0).content;
            title = hasOnlyOne ? getString(R.string.notification_new_message, imgurMessage.getFrom()) : getString(R.string.notification_multiple_messages, messageNotifications);
            message = hasOnlyOne ? imgurMessage.getLastMessage() : getString(R.string.notification_multiple_messages_body, messageNotifications);
        } else {
            boolean hasOnlyOne = replyNotifications == 1;
            ImgurComment comment = data.replies.get(0).content;
            title = hasOnlyOne ? getString(R.string.notification_new_reply, comment.getAuthor()) : getString(R.string.notification_multiple_replies, replyNotifications);
            message = hasOnlyOne ? comment.getComment() : getString(R.string.notification_multiple_replies_body, replyNotifications);
        }

        Notification notification = new Notification(getApplicationContext(), title, message);
        // TODO Pending Intents
    }

    private static class Notification extends BaseNotification {
        private String mTitle;

        public Notification(Context context, String title, String message) {
            super(context);
            mTitle = title;
            builder.setContentText(message);
            build(context);
        }

        @Override
        protected Uri getNotificationSound() {
            // TODO
            return super.getNotificationSound();
        }

        @Override
        protected int getVibration() {
            // TODO
            return super.getVibration();
        }

        @NonNull
        @Override
        protected String getTitle() {
            return mTitle;
        }

        @Override
        protected int getNotificationId() {
            return (int) System.currentTimeMillis();
        }
    }
}
