package com.kenny.openimgur.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.activities.SettingsActivity;

import io.fabric.sdk.android.Fabric;

/**
 * Created by kcampagna on 4/18/16.
 */
public class FabricUtil {

    public static void init(@NonNull Context context, @NonNull SharedPreferences preferences) {
        // Start crashlytics if enabled
        if (!BuildConfig.DEBUG && preferences.getBoolean(SettingsActivity.KEY_CRASHLYTICS, true)) {
            Fabric.with(context, new Crashlytics());
        }
    }

    public static boolean hasFabricAvailable() {
        return true;
    }
}
