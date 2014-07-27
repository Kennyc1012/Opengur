package com.kenny.openimgur.classes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.util.SqlHelper;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LargestLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import de.greenrobot.event.util.AsyncExecutor;

/**
 * Created by kcampagna on 6/14/14.
 */
public class OpenImgurApp extends Application {
    private static final String TAG = "OpenImgur";

    private static final long FILE_CACHE_LIMIT_1_GB = 1073741824L;

    private static final long FILE_CACHE_LIMIT_512_MB = 536870912L;

    private static final long FILE_CACHE_LIMIT_256_MB = 268435456L;

    private static final long FILE_CACHE_LIMIT_128_MB = 134217728L;

    // 8MB
    private static final int MEMORY_CACHE_LIMIT = 8388608;

    // We will get a new refresh token when it expires in 5 minutes or less
    private static final long TOKEN_CUTOFF_TIME = DateUtils.MINUTE_IN_MILLIS * 5;

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
    }

    public ImageLoader getImageLoader() {
        if (mImageLoader == null || !mImageLoader.isInited()) {
            initImageLoader();
            mImageLoader = ImageLoader.getInstance();
        }

        return mImageLoader;
    }

    /**
     * Initializes the ImageLoader
     */
    private void initImageLoader() {
        Context context = getApplicationContext();
        int cacheLimit = Integer.parseInt(mPref.getString(SettingsActivity.CACHE_SIZE_KEY, "0"));
        long cache;

        switch (cacheLimit) {
            case SettingsActivity.CACHE_SIZE_128_MB:
                cache = FILE_CACHE_LIMIT_128_MB;
                break;

            case SettingsActivity.CACHE_SIZE_512_MB:
                cache = FILE_CACHE_LIMIT_512_MB;
                break;

            case SettingsActivity.CACHE_SIZE_1_GB:
                cache = FILE_CACHE_LIMIT_1_GB;
                break;


            case SettingsActivity.CACHE_SIZE_256_MB:
            default:
                cache = FILE_CACHE_LIMIT_256_MB;
                break;
        }

        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPoolSize(7)
                .denyCacheImageMultipleSizesInMemory()
                .diskCache(new LruDiscCache(getCacheDir(), new HashCodeFileNameGenerator(), cache))
                .defaultDisplayImageOptions(options)
                .memoryCache(new LargestLimitedMemoryCache(MEMORY_CACHE_LIMIT))
                .build();

        ImageLoader.getInstance().init(config);
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
     * Refreshes the user's access token
     *
     * @return If the token will be refreshed
     */
    public boolean checkRefreshToken() {
        if (mUser != null && mUser.getAccessTokenExpiration() - System.currentTimeMillis() <= TOKEN_CUTOFF_TIME) {
            Log.v(TAG, "Token expired or is expiring soon, requesting new token");
            final ApiClient client = new ApiClient(Endpoints.REFRESH_TOKEN.getUrl(), ApiClient.HttpRequest.POST);
            final RequestBody body = new FormEncodingBuilder()
                    .add("client_id", ApiClient.CLIENT_ID)
                    .add("client_secret", ApiClient.CLIENT_SECRET)
                    .add("refresh_token", mUser.getRefreshToken())
                    .add("grant_type", "refresh_token").build();

            AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
                @Override
                public void run() throws Exception {
                    client.doWork(ImgurBusEvent.EventType.REFRESH_TOKEN, null, body);
                }
            });
            return true;
        } else {
            Log.v(TAG, "User is not or token is still valid, no need to request a new token");
        }

        return false;
    }

    /**
     * Called when a refresh token has been received.
     *
     * @param json
     */
    public void onReceivedRefreshToken(JSONObject json) {
        try {
            String accessToken = json.getString(ImgurUser.KEY_ACCESS_TOKEN);
            String refreshToken = json.getString(ImgurUser.KEY_REFRESH_TOKEN);
            long expiresIn = json.getLong(ImgurUser.KEY_EXPIRES_IN);
            mUser.setTokens(accessToken, refreshToken, expiresIn);
            mSql.updateUserTokens(accessToken, refreshToken, System.currentTimeMillis() + expiresIn);
            Log.v(TAG, "New refresh token received");
        } catch (JSONException e) {
            e.printStackTrace();
            Log.w(TAG, "Error parsing refresh token result");
        }

    }
}
