package com.kenny.openimgur.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
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

    protected OpengurApp app;

    protected NotificationCompat.Builder builder;

    public BaseNotification(Context context) {
        app = OpengurApp.getInstance(context);
        builder = new NotificationCompat.Builder(context)
                .setSmallIcon(getSmallIcon())
                .setContentTitle(getTitle())
                .setAutoCancel(canAutoCancel())
                .setColor(getNotificationColor());
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
     * Posts the notification
     *
     * @param manager
     */
    public void notify(@NonNull NotificationManager manager) {
        if (builder != null) manager.notify(getNotificationId(), builder.build());
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
