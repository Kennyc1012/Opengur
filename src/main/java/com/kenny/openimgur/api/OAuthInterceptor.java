package com.kenny.openimgur.api;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.api.responses.OAuthResponse;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.HttpURLConnection;

import retrofit.Call;

/**
 * Created by kcampagna on 7/15/15.
 */
public class OAuthInterceptor implements Interceptor {
    private static final String TAG = OAuthInterceptor.class.getSimpleName();

    private static final Object sLock = new Object();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);

        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED || response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
            String token = ApiClient.getAccessToken();

            if (!TextUtils.isEmpty(token)) {
                LogUtil.v(TAG, "Token is no longer valid");

                synchronized (sLock) {
                    String currentToken = ApiClient.getAccessToken();

                    // Check if our current token has been updated, if it hasn't fetch a new one.
                    if (!TextUtils.isEmpty(currentToken) && currentToken.equals(token)) {
                        ApiClient.setAccessToken(refreshToken(OpengurApp.getInstance()));
                    }
                }

                if (!TextUtils.isEmpty(ApiClient.getAccessToken())) {
                    Request newRequest = request.newBuilder()
                            .removeHeader(ApiClient.AUTHORIZATION_HEADER)
                            .addHeader(ApiClient.AUTHORIZATION_HEADER, "Bearer " + ApiClient.getAccessToken())
                            .build();

                    return chain.proceed(newRequest);
                }
            } else {
                LogUtil.w(TAG, "Received unauthorized status from API but no access token present... wat?");
            }
        }

        return response;
    }

    @Nullable
    private String refreshToken(OpengurApp app) {
        try {
            Call<OAuthResponse> call = ApiClient.getService().refreshToken(ApiClient.CLIENT_ID, ApiClient.CLIENT_SECRET, app.getUser().getRefreshToken(), "refresh_token");
            retrofit.Response<OAuthResponse> response = call.execute();

            if (response == null || response.body() == null) {
                LogUtil.e(TAG, "Respons came back as null");
                app.onLogout();
                return null;
            }

            OAuthResponse oAuthResponse = response.body();

            if (!TextUtils.isEmpty(oAuthResponse.access_token) && !TextUtils.isEmpty(oAuthResponse.refresh_token)) {
                app.getUser().setTokens(oAuthResponse.access_token, oAuthResponse.refresh_token, oAuthResponse.expires_in);
                app.getSql().updateUserTokens(oAuthResponse.access_token, oAuthResponse.refresh_token, oAuthResponse.expires_in);
                return oAuthResponse.access_token;
            }

            app.onLogout();
            return null;
        } catch (Throwable error) {
            LogUtil.e(TAG, "Error while refreshing token, logging out user", error);
            app.onLogout();
        }

        return null;
    }
}
