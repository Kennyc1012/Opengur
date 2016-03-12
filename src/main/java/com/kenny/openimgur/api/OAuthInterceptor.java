package com.kenny.openimgur.api;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.kenny.openimgur.api.responses.OAuthResponse;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Call;

/**
 * Created by kcampagna on 7/15/15.
 */
public class OAuthInterceptor implements Interceptor {
    private static final String TAG = OAuthInterceptor.class.getSimpleName();

    private static final Object sLock = new Object();

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Nullable
    private static String sAccessToken = null;

    private int mRetryAttempts = 0;

    public OAuthInterceptor(@Nullable String token) {
        sAccessToken = token;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();
        builder.addHeader(AUTHORIZATION_HEADER, getAuthorizationHeader());
        Request request = builder.method(original.method(), original.body()).build();
        Response response = chain.proceed(request);
        LogUtil.v(TAG, "Response to " + request.url().toString() + " - " + response.code());

        if (response.code() == HttpURLConnection.HTTP_UNAUTHORIZED || response.code() == HttpURLConnection.HTTP_FORBIDDEN) {
            String token = sAccessToken;

            if (!TextUtils.isEmpty(token)) {
                LogUtil.v(TAG, "Token is no longer valid");

                synchronized (sLock) {
                    String currentToken = sAccessToken;

                    // Check if our current token has been updated, if it hasn't fetch a new one.
                    if (!TextUtils.isEmpty(currentToken) && currentToken.equals(token)) {
                        // Try 5 times to get a refresh token
                        while (mRetryAttempts < 5) {
                            sAccessToken = refreshToken(OpengurApp.getInstance());

                            if (!TextUtils.isEmpty(sAccessToken)) {
                                break;
                            }

                            mRetryAttempts++;

                            try {
                                // Delay the next request by several seconds so we aren't bombarding the API
                                Thread.sleep(DateUtils.SECOND_IN_MILLIS * mRetryAttempts);
                            } catch (Exception ex) {
                                LogUtil.v(TAG, "Sleeping thread failed", ex);
                            }
                        }
                    }
                }

                mRetryAttempts = 0;

                if (!TextUtils.isEmpty(sAccessToken)) {
                    Request newRequest = request.newBuilder()
                            .removeHeader(AUTHORIZATION_HEADER)
                            .addHeader(AUTHORIZATION_HEADER, "Bearer " + sAccessToken)
                            .build();

                    return chain.proceed(newRequest);
                } else {
                    OpengurApp.getInstance().onLogout();
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
            retrofit2.Response<OAuthResponse> response = call.execute();

            if (response == null || response.body() == null) {
                LogUtil.e(TAG, "Response came back as null");
                return null;
            }

            OAuthResponse oAuthResponse = response.body();

            if (!TextUtils.isEmpty(oAuthResponse.access_token) && !TextUtils.isEmpty(oAuthResponse.refresh_token)) {
                app.getUser().setTokens(oAuthResponse.access_token, oAuthResponse.refresh_token, oAuthResponse.expires_in);
                SqlHelper.getInstance(app).updateUserTokens(oAuthResponse.access_token, oAuthResponse.refresh_token, oAuthResponse.expires_in);
                return oAuthResponse.access_token;
            }

            app.onLogout();
        } catch (Throwable error) {
            LogUtil.e(TAG, "Error while refreshing token, logging out user", error);
        }

        return null;
    }

    private String getAuthorizationHeader() {
        if (!TextUtils.isEmpty(sAccessToken)) {
            LogUtil.v(TAG, "Access Token present");
            return "Bearer " + sAccessToken;
        } else {
            LogUtil.v(TAG, "No access token present, using Client-ID");
            return "Client-ID " + ApiClient.CLIENT_ID;
        }
    }

    public static void setAccessToken(@Nullable String token) {
        sAccessToken = token;
    }
}
