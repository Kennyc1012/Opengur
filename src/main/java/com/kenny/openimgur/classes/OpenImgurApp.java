package com.kenny.openimgur.classes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.crashlytics.android.Crashlytics;
import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import io.fabric.sdk.android.Fabric;

/**
 * Created by kcampagna on 6/14/14.
 */
public class OpenImgurApp extends Application implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "OpenImgur";

    private static boolean USE_STRICT_MODE = BuildConfig.DEBUG;

    private static OpenImgurApp instance;

    private ImageLoader mImageLoader;

    public int sdkVersion = Build.VERSION.SDK_INT;

    private SharedPreferences mPref;

    private SqlHelper mSql;

    private ImgurUser mUser;

    private boolean mIsFetchingAccessToken = false;

    private ImgurTheme mTheme = ImgurTheme.GREY;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPref.registerOnSharedPreferenceChangeListener(this);
        mSql = new SqlHelper(getApplicationContext());
        mUser = mSql.getUser();
        checkRefreshToken();
        mTheme = ImgurTheme.getThemeFromString(mPref.getString(SettingsActivity.THEME_KEY, ImgurTheme.GREY.themeName));
        mTheme.isDarkTheme = mPref.getBoolean(SettingsActivity.KEY_DARK_THEME, false);

        // Start crashlytics if enabled
        if (!BuildConfig.DEBUG && mPref.getBoolean(SettingsActivity.KEY_CRASHLYTICS, true)) {
            Fabric.with(this, new Crashlytics());
        }

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

    public ImageLoader getImageLoader() {
        if (mImageLoader == null || !mImageLoader.isInited()) {
            ImageUtil.initImageLoader(getApplicationContext());
            mImageLoader = ImageLoader.getInstance();
        }

        return mImageLoader;
    }

    public static OpenImgurApp getInstance() {
        return instance;
    }

    public static OpenImgurApp getInstance(Context context) {
        return context != null ? (OpenImgurApp) context.getApplicationContext() : instance;
    }

    public SharedPreferences getPreferences() {
        return mPref;
    }

    public SqlHelper getSql() {
        return mSql;
    }

    public ImgurUser getUser() {
        return mUser;
    }

    public void setUser(ImgurUser user) {
        this.mUser = user;
        mSql.insertUser(user);
    }

    /**
     * Deletes all of the cache, including the unused partition (external/internal)
     */
    public void deleteAllCache() {
        ImageLoader imageLoader = getImageLoader();
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
        mSql.onUserLogout();
    }

    /**
     * Refreshes the user's access token
     *
     * @return If the token will be refreshed
     */
    public boolean checkRefreshToken() {
        if (mUser != null && !mUser.isAccessTokenValid() && !mIsFetchingAccessToken) {
            LogUtil.v(TAG, "Token expired or is expiring soon, requesting new token");
            mIsFetchingAccessToken = true;
            new RefreshTokenTask().execute(mUser);
            return true;
        }

        LogUtil.v(TAG, "User is null, token is still valid, or currently request a token, no need to request a new token");
        return false;
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
     * Called when a refresh token has been received.
     *
     * @param json
     */
    private synchronized boolean onReceivedRefreshToken(JSONObject json) {
        try {
            String accessToken = json.getString(ImgurUser.KEY_ACCESS_TOKEN);
            String refreshToken = json.getString(ImgurUser.KEY_REFRESH_TOKEN);
            long expiration = System.currentTimeMillis() + (json.getLong(ImgurUser.KEY_EXPIRES_IN) * DateUtils.SECOND_IN_MILLIS);
            mUser.setTokens(accessToken, refreshToken, expiration);
            mSql.updateUserTokens(accessToken, refreshToken, expiration);
            LogUtil.v(TAG, "New refresh token received");
            mIsFetchingAccessToken = false;
            return true;
        } catch (JSONException e) {
            LogUtil.e(TAG, "Error parsing refresh token result", e);
            mIsFetchingAccessToken = false;
            return false;
        }
    }

    private class RefreshTokenTask extends AsyncTask<ImgurUser, Void, Boolean> {

        @Override
        protected Boolean doInBackground(ImgurUser... imgurUsers) {
            if (imgurUsers == null || imgurUsers.length <= 0) {
                return false;
            }

            try {
                ImgurUser user = imgurUsers[0];

                final RequestBody body = new FormEncodingBuilder()
                        .add("client_id", ApiClient.CLIENT_ID)
                        .add("client_secret", ApiClient.CLIENT_SECRET)
                        .add("refresh_token", user.getRefreshToken())
                        .add("grant_type", "refresh_token").build();

                ApiClient client = new ApiClient(Endpoints.REFRESH_TOKEN.getUrl(), ApiClient.HttpRequest.POST);
                return onReceivedRefreshToken(client.doWork(body));
            } catch (Exception e) {
                LogUtil.e(TAG, "Error parsing user tokens", e);
                mIsFetchingAccessToken = false;
                return false;
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LogUtil.v(TAG, "Preference " + key + " changed");

        switch (key) {
            case SettingsActivity.KEY_THREAD_SIZE:
            case SettingsActivity.CACHE_SIZE_KEY:
                ImageUtil.initImageLoader(getApplicationContext());
                break;
        }
    }
}
