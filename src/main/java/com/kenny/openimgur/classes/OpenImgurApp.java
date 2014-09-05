package com.kenny.openimgur.classes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kcampagna on 6/14/14.
 */
public class OpenImgurApp extends Application {
    private static final String TAG = "OpenImgur";

    public static final long FILE_CACHE_LIMIT_1_GB = 1073741824L;

    public static final long FILE_CACHE_LIMIT_512_MB = 536870912L;

    public static final long FILE_CACHE_LIMIT_256_MB = 268435456L;

    public static final long FILE_CACHE_LIMIT_128_MB = 134217728L;

    private static OpenImgurApp instance;

    private ImageLoader mImageLoader;

    public static final int SDK_VERSION = Build.VERSION.SDK_INT;

    private SharedPreferences mPref;

    private SqlHelper mSql;

    private ImgurUser mUser;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSql = new SqlHelper(getApplicationContext());
        mUser = mSql.getUser();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }

    public ImageLoader getImageLoader() {
        if (mImageLoader == null || !mImageLoader.isInited()) {
            String cacheLimit = mPref.getString(SettingsActivity.CACHE_SIZE_KEY, SettingsActivity.CACHE_SIZE_256_MB);
            long cache;

            if (SettingsActivity.CACHE_SIZE_128_MB.equals(cacheLimit)) {
                cache = FILE_CACHE_LIMIT_128_MB;
            } else if (SettingsActivity.CACHE_SIZE_256_MB.equals(cacheLimit)) {
                cache = FILE_CACHE_LIMIT_256_MB;
            } else if (SettingsActivity.CACHE_SIZE_512_MB.equals(cacheLimit)) {
                cache = FILE_CACHE_LIMIT_512_MB;
            } else {
                cache = FILE_CACHE_LIMIT_1_GB;
            }

            ImageUtil.initImageLoader(getApplicationContext(), cache);
            mImageLoader = ImageLoader.getInstance();
        }

        return mImageLoader;
    }

    /**
     * Returns if the device has an active internet connection
     *
     * @return
     */
    public boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = cm.getActiveNetworkInfo();

        return info != null && info.isConnectedOrConnecting();
    }

    public static OpenImgurApp getInstance() {
        return instance;
    }

    public static OpenImgurApp getInstance(Context context) {
        return (OpenImgurApp) context.getApplicationContext();
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
        if (mUser != null && !mUser.isAccessTokenValid()) {
            LogUtil.v(TAG, "Token expired or is expiring soon, requesting new token");
            new RefreshTokenTask().execute(mUser);
            return true;
        } else {
            LogUtil.v(TAG, "User is null or token is still valid, no need to request a new token");
        }

        return false;
    }

    /**
     * Called when a refresh token has been received.
     *
     * @param json
     */
    private boolean onReceivedRefreshToken(JSONObject json) {
        try {
            String accessToken = json.getString(ImgurUser.KEY_ACCESS_TOKEN);
            String refreshToken = json.getString(ImgurUser.KEY_REFRESH_TOKEN);
            long expiresIn = json.getLong(ImgurUser.KEY_EXPIRES_IN);

            synchronized (mUser) {
                mUser.setTokens(accessToken, refreshToken, expiresIn);
            }

            mSql.updateUserTokens(accessToken, refreshToken, System.currentTimeMillis() + (expiresIn * DateUtils.SECOND_IN_MILLIS));
            LogUtil.v(TAG, "New refresh token received");
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            LogUtil.w(TAG, "Error parsing refresh token result", e);
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
                return false;
            }
        }
    }
}
