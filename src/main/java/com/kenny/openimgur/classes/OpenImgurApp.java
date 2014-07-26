package com.kenny.openimgur.classes;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.PreferenceManager;

import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.util.SqlHelper;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LargestLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Created by kcampagna on 6/14/14.
 */
public class OpenImgurApp extends Application {
    private static final long FILE_CACHE_LIMIT_1_GB = 1073741824L;

    private static final long FILE_CACHE_LIMIT_512_MB = 536870912L;

    private static final long FILE_CACHE_LIMIT_256_MB = 268435456L;

    private static final long FILE_CACHE_LIMIT_128_MB = 134217728L;

    // 8MB
    private static final int MEMORY_CACHE_LIMIT = 8388608;

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
    }
}
