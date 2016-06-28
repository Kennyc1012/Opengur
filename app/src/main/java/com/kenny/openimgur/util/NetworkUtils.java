package com.kenny.openimgur.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.annotation.Nullable;

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
}
