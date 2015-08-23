package com.kenny.openimgur.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpengurApp;

/**
 * Created by kcampagna on 8/12/15.
 */
public abstract class BaseNotification {
    protected final String TAG = getClass().getSimpleName();

    private NotificationManager mManger;

    protected OpengurApp app;

    protected NotificationCompat.Builder builder;

    public BaseNotification(Context context) {
        this(context, true);
    }

    public BaseNotification(Context context, boolean autoBuild) {
        mManger = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        app = OpengurApp.getInstance(context);

        if (autoBuild) {
            build(context);
        } else {
            builder = new NotificationCompat.Builder(context);
        }
    }

    /**
     * If the notification can be auto canceled
     *
     * @return
     */
    protected boolean canAutoCancel() {
        return true;
    }

    /**
     * Returns the drawable resource for the notification icon
     *
     * @return
     */
    @DrawableRes
    protected int getSmallIcon() {
        return R.drawable.ic_notif;
    }

    /**
     * Returns the notification color. This will only be used on 5.0+ devices
     *
     * @return
     */
    protected int getNotificationColor() {
        Resources res = app.getResources();
        return res.getColor(app.getImgurTheme().primaryColor);
    }

    /**
     * Returns the vibration length of the notification. Return anything 0 or negative to not vibrate
     *
     * @return
     */
    protected long getVibration() {
        return 0;
    }

    /**
     * Returns the {@link Uri} for the notification sound. Return null to not play a sound
     *
     * @return
     */
    protected Uri getNotificationSound() {
        return null;
    }

    /**
     * Posts the notification
     */
    public void postNotification() {
        if (mManger != null && builder != null) postNotification(builder.build());
    }

    /**
     * Posts the notifications
     *
     * @param notification
     */
    public void postNotification(Notification notification) {
        mManger.notify(getNotificationId(), notification);
    }

    protected void build(Context context) {
        if (builder == null) builder = new NotificationCompat.Builder(context);

        builder.setSmallIcon(getSmallIcon())
                .setContentTitle(getTitle())
                .setAutoCancel(canAutoCancel())
                .setColor(getNotificationColor());

        if (getNotificationSound() != null) {
            builder.setSound(getNotificationSound());
        }

        if (getVibration() > 0) {
            builder.setVibrate(new long[]{0, getVibration()});
        }
    }

    /**
     * Returns the string to be used as the title for the notification
     *
     * @return
     */
    @NonNull
    abstract protected String getTitle();

    /**
     * Returns the unique id for the notification
     *
     * @return
     */
    abstract protected int getNotificationId();
}
