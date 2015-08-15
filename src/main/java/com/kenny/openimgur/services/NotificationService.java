package com.kenny.openimgur.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.ui.BaseNotification;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;

import java.util.HashSet;
import java.util.Set;

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
        boolean enabled = app.getPreferences().getBoolean(SettingsActivity.KEY_NOTIFICATIONS, true);

        if (!enabled) {
            LogUtil.v(TAG, "Notifications have been disabled, not fetching");
            return;
        }

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
        // Remove potential duplicates
        Set<ImgurMessage> messages = new HashSet<>();
        Set<ImgurComment> replies = new HashSet<>();

        for (NotificationResponse.Messages m : data.messages) {
            messages.add(m.content);
        }

        for (NotificationResponse.Replies r : data.replies) {
            replies.add(r.content);
        }

        int messageNotifications = messages.size();
        int replyNotifications = replies.size();
        PendingIntent pendingIntent;
        ImgurBaseObject obj = null;
        String title;
        String message;

        if (messageNotifications > 0 && replyNotifications > 0) {
            int totalNotifications = messageNotifications + replyNotifications;
            title = getString(R.string.notifications_multiple_types, totalNotifications);
            message = getString(R.string.notifications_multiple_types_body, totalNotifications);
        } else if (messageNotifications > 0) {
            boolean hasOnlyOne = messageNotifications == 1;
            ImgurMessage imgurMessage = messages.iterator().next();
            obj = new ImgurConvo(imgurMessage.getId(), imgurMessage.getFrom(), -1);
            title = hasOnlyOne ? getString(R.string.notification_new_message, imgurMessage.getFrom()) : getString(R.string.notification_multiple_messages, messageNotifications);
            message = hasOnlyOne ? imgurMessage.getLastMessage() : getString(R.string.notification_multiple_messages_body, messageNotifications);
        } else {
            boolean hasOnlyOne = replyNotifications == 1;
            ImgurComment comment = replies.iterator().next();
            obj = comment;
            title = hasOnlyOne ? getString(R.string.notification_new_reply, comment.getAuthor()) : getString(R.string.notification_multiple_replies, replyNotifications);
            message = hasOnlyOne ? comment.getComment() : getString(R.string.notification_multiple_replies_body, replyNotifications);
        }

        Intent intent = NotificationReceiver.createNotificationIntent(getApplicationContext(), obj);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), RequestCodes.NOTIFICATIONS, intent, PendingIntent.FLAG_ONE_SHOT);
        Notification notification = new Notification(getApplicationContext(), title, message, pendingIntent);
        notification.postNotification();
    }

    private static class Notification extends BaseNotification {
        private String mTitle;

        private Uri mNotification;

        private boolean mVibrate = false;

        public Notification(Context context, String title, String message, PendingIntent intent) {
            super(context, false);
            SharedPreferences pref = app.getPreferences();
            mVibrate = pref.getBoolean(SettingsActivity.KEY_NOTIFICATION_VIBRATE, true);
            mTitle = title;
            String ringTone = pref.getString(SettingsActivity.KEY_NOTIFICATION_RINGTONE, null);

            if (!TextUtils.isEmpty(ringTone)) {
                try {
                    mNotification = Uri.parse(ringTone);
                } catch (Exception e) {
                    LogUtil.e(TAG, "Unable to parse ringtone", e);
                    mNotification = null;
                }
            }

            build(context);
            builder.setContentText(message);
            builder.setContentIntent(intent);
        }

        @Override
        protected Uri getNotificationSound() {
            return mNotification;
        }

        @Override
        protected long getVibration() {
            return mVibrate ? DateUtils.SECOND_IN_MILLIS : 0;
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
