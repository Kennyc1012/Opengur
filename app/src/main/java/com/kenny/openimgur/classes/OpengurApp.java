package com.kenny.openimgur.classes;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StrictMode;
import android.os.UserManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.api.OAuthInterceptor;
import com.kenny.openimgur.services.AlarmReceiver;
import com.kenny.openimgur.util.FabricUtil;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.SqlHelper;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.lang.reflect.Method;

/**
 * Created by kcampagna on 6/14/14.
 */
public class OpengurApp extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "OpenImgur";

    private static boolean USE_STRICT_MODE = BuildConfig.DEBUG;

    private static OpengurApp sInstance;

    private SharedPreferences mPref;

    private ImgurUser mUser;

    private ImgurTheme mTheme = ImgurTheme.GREY;

    public static final String AUTHORITY = "com.kennyc.open.imgur";

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        stopUserManagerLeak();
        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPref.registerOnSharedPreferenceChangeListener(this);
        mTheme = ImgurTheme.fromPreferences(getResources(), mPref.getInt(SettingsActivity.KEY_THEME_NEW, ImgurTheme.GREY.primaryColor));
        mTheme.isDarkTheme = mPref.getBoolean(SettingsActivity.KEY_DARK_THEME, true);
        mUser = SqlHelper.getInstance(this).getUser();
        if (mUser != null) AlarmReceiver.createNotificationAlarm(this);
        ImageUtil.initImageLoader(getApplicationContext());

        migrateDownloadFolder();
        FabricUtil.init(this, mPref);

        // Check if for ADB logging on a non debug build
        if (!BuildConfig.DEBUG) {
            boolean allowLog = mPref.getBoolean(SettingsActivity.KEY_ADB, false);
            USE_STRICT_MODE = allowLog;
            LogUtil.onCreateApplication(allowLog);
        }

        if (USE_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LogUtil.w(TAG, "Received onLowMemory");
        ImageUtil.getImageLoader(this).clearMemoryCache();
    }

    public static OpengurApp getInstance() {
        return sInstance;
    }

    public static OpengurApp getInstance(Context context) {
        return context != null ? (OpengurApp) context.getApplicationContext() : sInstance;
    }

    public SharedPreferences getPreferences() {
        return mPref;
    }

    public ImgurUser getUser() {
        return mUser;
    }

    public void setUser(ImgurUser user) {
        this.mUser = user;
        SqlHelper.getInstance(this).insertUser(user);
    }

    /**
     * Deletes all of the cache, including the unused partition (external/internal)
     */
    public void deleteAllCache() {
        ImageLoader imageLoader = ImageUtil.getImageLoader(this);
        imageLoader.clearDiskCache();
        imageLoader.clearMemoryCache();
        VideoCache.getInstance().deleteCache();
        String cacheKey = mPref.getString(SettingsActivity.KEY_CACHE_LOC, SettingsActivity.CACHE_LOC_INTERNAL);

        if (SettingsActivity.CACHE_LOC_EXTERNAL.equals(cacheKey)) {
            // Current cache is external, delete internal
            FileUtil.deleteDirectory(getCacheDir());
            File videoCache = new File(getCacheDir(), "video_cache");
            FileUtil.deleteDirectory(videoCache);
        } else if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            // Current cache is internal, delete external
            if (FileUtil.isFileValid(getExternalCacheDir())) {
                File cache = getExternalCacheDir();
                FileUtil.deleteDirectory(cache);
                File videoCache = new File(cache, "video_cache");
                FileUtil.deleteDirectory(videoCache);
            }
        }
    }

    /**
     * Called when the user logs out.
     */
    public void onLogout() {
        mUser = null;
        SqlHelper.getInstance(this).onUserLogout();
        OAuthInterceptor.setAccessToken(null);
    }

    public ImgurTheme getImgurTheme() {
        return mTheme;
    }

    public void setImgurTheme(@NonNull ImgurTheme theme) {
        mTheme = theme;
    }

    public void setAllowLogs(boolean allowLogs) {
        USE_STRICT_MODE = allowLogs;

        if (USE_STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }
    }

    /**
     * Attempts to call static method from {@link UserManager} that is causing an activity leak
     * https://code.google.com/p/android/issues/detail?id=173789
     */
    private void stopUserManagerLeak() {
        try {
            Method method = UserManager.class.getMethod("get", Context.class);
            method.setAccessible(true);

            if (method.isAccessible()) {
                method.invoke(null, getApplicationContext());
                LogUtil.v(TAG, "Able to access method causing leak");
            } else {
                LogUtil.w(TAG, "Unable to access method to stop leak");
            }
        } catch (Throwable ex) {
            LogUtil.e(TAG, "Unable to fix user manager leak", ex);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LogUtil.v(TAG, "Preference " + key + " changed");

        switch (key) {
            case SettingsActivity.KEY_THREAD_SIZE:
            case SettingsActivity.KEY_CACHE_SIZE:
                ImageUtil.initImageLoader(getApplicationContext());
                break;

            case SettingsActivity.KEY_NOTIFICATIONS:
                if (mUser != null && sharedPreferences.getBoolean(key, false)) {
                    AlarmReceiver.createNotificationAlarm(getApplicationContext());
                }
                break;
        }
    }

    /**
     * TODO Remove after several versions
     */
    private void migrateDownloadFolder() {
        if (!mPref.getBoolean("migrated_downloads_v2", false)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OpenImgur");
                    boolean hasPermission = PermissionUtils.hasPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    if (!hasPermission) return;

                    if (FileUtil.isFileValid(dir)) {
                        File newDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Opengur");
                        boolean success = dir.renameTo(newDir);
                        if (success) FileUtil.scanDirectory(newDir, getApplicationContext());
                        LogUtil.v(TAG, "Result of folder renaming " + success);
                    } else {
                        LogUtil.v(TAG, "Directory not found, nothing to do here");
                    }

                    getPreferences().edit().putBoolean("migrated_downloads_v2", true).apply();
                }
            }).start();
        }
    }
}
