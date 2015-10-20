package com.kenny.openimgur.api;


import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.R;
import com.kenny.openimgur.api.responses.ConvoResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.squareup.okhttp.OkHttpClient;

import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;


/**
 * Created by kcampagna on 7/10/15.
 */

public class ApiClient {
    private static final String TAG = ApiClient.class.getSimpleName();

    private static final String API_URL = "https://api.imgur.com";

    private static Retrofit sRestAdapter;

    private static ImgurService sService;

    public static final String CLIENT_ID = BuildConfig.API_CLIENT_ID;

    public static final String CLIENT_SECRET = BuildConfig.API_CLIENT_SECRET;

    /**
     * Returns the service used for API requests
     *
     * @return
     */
    public static ImgurService getService() {
        if (sRestAdapter == null || sService == null) {
            ImgurUser user = OpengurApp.getInstance().getUser();

            sRestAdapter = new Retrofit.Builder()
                    .baseUrl(API_URL)
                    .client(getClient(user))
                    .addConverterFactory(getConverter())
                    .build();

            sService = sRestAdapter.create(ImgurService.class);
        }

        return sService;

    }

    private static OkHttpClient getClient(@Nullable ImgurUser user) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(15, TimeUnit.SECONDS);
        client.interceptors().add(new OAuthInterceptor(user != null ? user.getAccessToken() : null));
        return client;
    }

    private static GsonConverterFactory getConverter() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ImgurBaseObject.class, new ImgurSerializer())
                .registerTypeAdapter(ConvoResponse.class, new ConvoResponse())
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