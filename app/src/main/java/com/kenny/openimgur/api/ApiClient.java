package com.kenny.openimgur.api;


import android.support.annotation.StringRes;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.FileUtil;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created by kcampagna on 7/10/15.
 */

public class ApiClient {
    public static final String IMGUR_URL = "https://imgur.com/";

    public static final String IMGUR_GALLERY_URL = IMGUR_URL + "gallery/";

    private static final String API_URL = "https://api.imgur.com";

    private static Retrofit sRestAdapter;

    private static ImgurService sService;

    // 10MB
    private static final long CACHE_SIZE = 10 * 1024 * 1024;

    public static final String CLIENT_ID = BuildConfig.API_CLIENT_ID;

    public static final String CLIENT_SECRET = BuildConfig.API_CLIENT_SECRET;

    /**
     * Returns the service used for API requests
     *
     * @return
     */
    public static ImgurService getService() {
        if (sRestAdapter == null || sService == null) {
            sRestAdapter = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .client(getClient())
                    .addConverterFactory(getConverter())
                    .build();

            sService = sRestAdapter.create(ImgurService.class);
        }

        return sService;
    }

    private static OkHttpClient getClient() {
        OpengurApp app = OpengurApp.getInstance();
        ImgurUser user = app.getUser();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .addInterceptor(new OAuthInterceptor(user != null ? user.getAccessToken() : null));

        File cacheDir = app.getCacheDir();

        if (FileUtil.isFileValid(cacheDir)) {
            File cache = new File(cacheDir, "http_cache");
            builder.cache(new Cache(cache, CACHE_SIZE));
        }

        return builder.build();
    }

    private static GsonConverterFactory getConverter() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ImgurBaseObject.class, new ImgurSerializer())
                .create();

        return GsonConverterFactory.create(gson);
    }

    /**
     * Returns the string resource for the error thrown by Retrofit
     *
     * @param error The thrown error
     * @return
     */
    @StringRes
    public static int getErrorCode(Throwable error) {
        if (error instanceof UnknownHostException) {
            return R.string.error_network;
        }

        return R.string.error_generic;
    }

    /**
     * Returns the string resource for the HTTP status returned by the API
     *
     * @param httpStatus
     * @return
     */
    @StringRes
    public static int getErrorCode(int httpStatus) {
        switch (httpStatus) {
            case HttpURLConnection.HTTP_FORBIDDEN:
                return R.string.error_403;

            case HttpURLConnection.HTTP_UNAUTHORIZED:
                return R.string.error_401;

            case HttpURLConnection.HTTP_UNAVAILABLE:
                return R.string.error_503;

            case 429:
                return R.string.error_429;

            default:
                return R.string.error_generic;
        }
    }
}