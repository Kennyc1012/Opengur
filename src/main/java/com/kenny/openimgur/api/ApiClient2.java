package com.kenny.openimgur.api;


import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.classes.ImgurBaseObject2;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;


/**
 * Created by kcampagna on 7/10/15.
 */

public class ApiClient2 {
    private static final String TAG = ApiClient.class.getSimpleName();

    private static final String API_URL = "https://api.imgur.com/3";

    private static RestAdapter sRestAdapter;

    private static ImgurService sService;

    public static final String CLIENT_ID = BuildConfig.API_CLIENT_ID;

    public static final String CLIENT_SECRET = BuildConfig.API_CLIENT_SECRET;

    private static final String AUTHORIZATION_HEADER = "Authorization";

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

                if (user != null && user.isAccessTokenValid()) {
                    LogUtil.v(TAG, "Access Token present and valid");
                    request.addHeader(AUTHORIZATION_HEADER, "Bearer " + user.getAccessToken());
                } else {
                    LogUtil.v(TAG, "No access token present or is expiring, using Client-ID");
                    request.addHeader(AUTHORIZATION_HEADER, "Client-ID " + CLIENT_ID);
                }
            }
        };
    }


    private static OkClient getClient() {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(15, TimeUnit.SECONDS);
        // TODO More customization
        return new OkClient(client);
    }


    private static GsonConverter getConverter() {
        Gson gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(ImgurBaseObject2.class, new ImgurSerializer())
                .create();

        return new GsonConverter(gson);
    }
}