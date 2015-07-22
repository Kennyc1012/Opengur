package com.kenny.openimgur.api;


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
import com.kenny.openimgur.util.LogUtil;
import com.squareup.okhttp.OkHttpClient;

import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;


/**
 * Created by kcampagna on 7/10/15.
 */

public class ApiClient {
    private static final String TAG = ApiClient.class.getSimpleName();

    private static final String API_URL = "https://api.imgur.com";

    private static RestAdapter sRestAdapter;

    private static ImgurService sService;

    public static final String CLIENT_ID = BuildConfig.API_CLIENT_ID;

    public static final String CLIENT_SECRET = BuildConfig.API_CLIENT_SECRET;

    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Returns the service used for API requests
     *
     * @return
     */
    public static ImgurService getService() {
        if (sRestAdapter == null || sService == null) {
            sRestAdapter = new RestAdapter.Builder()
                    .setEndpoint(API_URL)
                    .setLogLevel(BuildConfig.DEBUG ? RestAdapter.LogLevel.BASIC : RestAdapter.LogLevel.NONE)
                    .setRequestInterceptor(getRequestInterceptor())
                    .setClient(getClient())
                    .setConverter(getConverter())
                    .build();

            sService = sRestAdapter.create(ImgurService.class);
        }

        return sService;

    }

    private static RequestInterceptor getRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                ImgurUser user = OpengurApp.getInstance().getUser();

                if (user != null) {
                    LogUtil.v(TAG, "Access Token present");
                    request.addHeader(AUTHORIZATION_HEADER, "Bearer " + user.getAccessToken());
                } else {
                    LogUtil.v(TAG, "No access token present, using Client-ID");
                    request.addHeader(AUTHORIZATION_HEADER, "Client-ID " + CLIENT_ID);
                }
            }
        };
    }

    private static OkClient getClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(15, TimeUnit.SECONDS);
        client.interceptors().add(new OAuthInterceptor());
        return new OkClient(client);
    }

    private static GsonConverter getConverter() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ImgurBaseObject.class, new ImgurSerializer())
                .registerTypeAdapter(ConvoResponse.class, new ConvoResponse())
                .create();

        return new GsonConverter(gson);
    }

    /**
     * Returns the string resource for the error thrown by Retrofit
     *
     * @param error The thrown error
     * @return
     */
    @StringRes
    public static int getErrorCode(RetrofitError error) {
        switch (error.getKind()) {
            case NETWORK:
                return R.string.error_network;

            case HTTP:
                if (error.getResponse() != null) {
                    return getErrorCode(error.getResponse().getStatus());
                } else {
                    return R.string.error_generic;
                }

            case CONVERSION:
            case UNEXPECTED:
            default:
                return R.string.error_generic;
        }
    }

    /**
     * Returns the string resource for the HTTP status thrown in an error
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