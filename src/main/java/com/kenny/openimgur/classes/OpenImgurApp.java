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

import com.kenny.openimgur.BuildConfig;
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

    private static final boolean USE_STRICT_MODE = BuildConfig.DEBUG;

    private static OpenImgurApp instance;

    private ImageLoader mImageLoader;

    public int sdkVersion = Build.VERSION.SDK_INT;

    private SharedPreferences mPref;

    private SqlHelper mSql;

    private ImgurUser mUser;

    private boolean mIsFetchingAccessToken = false;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mSql = new SqlHelper(getApplicationContext());
        mUser = mSql.getUser();

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

    /**
     * Returns the type of active internet connection. Null if not connected
     *
     * @return
     */
    public Integer getConnectionType() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected() ? info.getType() : null;
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
     * Deletes all of the caches
     */
    public void deleteAllCache() {
        mImageLoader.clearDiskCache();
        mImageLoader.clearMemoryCache();
        VideoCache.getInstance().deleteCache();
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
}
