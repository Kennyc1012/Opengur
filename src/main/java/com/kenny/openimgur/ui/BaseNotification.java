package com.kenny.openimgur.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
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

    protected Resources resources;

    public BaseNotification(Context context) {
        this(context, true);
    }

    public BaseNotification(Context context, boolean autoBuild) {
        mManger = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        app = OpengurApp.getInstance(context);
        resources = app.getResources();

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
        return resources.getColor(app.getImgurTheme().primaryColor);
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
    protected void postNotification(Notification notification) {
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
     * Returns a {@link Bitmap} for the notification to use for its Large Icon. If a drawable resource
     * is passed, the from will be ignored. Pass a negative value to ignore the drawable resource. If using the
     * from variable, an icon will be created with the first letter
     *
     * @param drawableResource The drawable resource to use for the icon
     * @param from             Who the notification is from
     * @return
     */
    protected Bitmap createLargeIcon(@DrawableRes int drawableResource, String from) {
        Bitmap bitmap;
        int iconSize = resources.getDimensionPixelSize(R.dimen.notification_icon);

        TextDrawable.IShapeBuilder builder = TextDrawable.builder()
                .beginConfig()
                .toUpperCase()
                .width(iconSize)
                .height(iconSize)
                .endConfig();

        if (drawableResource < 0) {
            int color = ColorGenerator.MATERIAL.getColor(from);
            String firstLetter = from.substring(0, 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bitmap = builder.buildRound(firstLetter, color).toBitmap();
            } else {
                bitmap = builder.buildRect(firstLetter, color).toBitmap();
            }
        } else {
            int color = resources.getColor(app.getImgurTheme().darkColor);
            Bitmap icon = BitmapFactory.decodeResource(resources, drawableResource);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bitmap = builder.buildRound(icon, color).toBitmap();
            } else {
                bitmap = builder.buildRect(icon, color).toBitmap();
            }
        }

        return bitmap;
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
