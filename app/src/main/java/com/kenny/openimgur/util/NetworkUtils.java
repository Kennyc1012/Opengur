package com.kenny.openimgur.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED;
import static android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED;
import static android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_WHITELISTED;

/**
 * Created by kcampagna on 9/1/15.
 */
public class NetworkUtils {
    private static final String TAG = NetworkUtils.class.getSimpleName();

    /**
     * Returns the current {@link NetworkInfo} of the device
     *
     * @param context
     * @return
     */
    public static NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }

    /**
     * Returns if the device is connected to the internet
     *
     * @param context
     * @return
     */
    public static boolean hasInternet(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected());
    }

    /**
     * Returns if the device is connected to WiFi
     *
     * @param context
     * @return
     */
    public static boolean isConnectedToWiFi(Context context) {
        NetworkInfo info = getNetworkInfo(context);
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    public static void releaseWakeLock(@Nullable PowerManager.WakeLock wakeLock) {
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ex) {
            LogUtil.e(TAG, "Unable to release wakelock", ex);
        }
    }

    /**
     * Returns if the users device currently has data saver enabled
     *
     * @param context
     * @return
     */
    public static boolean hasDataSaver(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false;

        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connMgr.isActiveNetworkMetered()) {
            switch (connMgr.getRestrictBackgroundStatus()) {
                case RESTRICT_BACKGROUND_STATUS_ENABLED:
                    // Data saver is enabled
                    return true;

                case RESTRICT_BACKGROUND_STATUS_WHITELISTED:
                case RESTRICT_BACKGROUND_STATUS_DISABLED:
                    // Data saver is either disabled, or the app has been white listed
                    return false;
            }
        }

        return false;
    }
}
