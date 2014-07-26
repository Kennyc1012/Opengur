package com.kenny.openimgur;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.MultiStateView;

/**
 * Created by kcampagna on 7/26/14.
 */
public class ProfileActivity extends BaseActivity {
    public static final String KEY_USER = "user";

    private MultiStateView mMultiView;

    private WebView mWebView;

    public static Intent createIntent(Context context, @Nullable String user) {
        Intent intent = new Intent(context, ProfileActivity.class);

        if (!TextUtils.isEmpty(user)) {
            intent.putExtra(KEY_USER, user);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mMultiView = (MultiStateView) findViewById(R.id.multiView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        mWebView = (WebView) mMultiView.findViewById(R.id.loginWebView);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(KEY_USER)) {

        } else if (user != null) {

        } else {
            Log.v(TAG, "No user present, going to login view");
            mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
            mWebView.loadUrl(Endpoints.LOGIN.getUrl());
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.contains("#")) {
                        // We will extract the info from the callback url
                        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                        String[] outerSplit = url.split("\\#")[1].split("\\&");
                        String username = null;
                        String accessToken = null;
                        String refreshToken = null;
                        long accessTokenExpiration = 0;
                        int index = 0;

                        for (String s : outerSplit) {
                            String[] innerSplit = s.split("\\=");

                            switch (index) {
                                // Access Token
                                case 0:
                                    accessToken = innerSplit[1];
                                    break;

                                // Access Token Expiration
                                case 1:
                                    long exipiresIn = Long.parseLong(innerSplit[1]);
                                    accessTokenExpiration = System.currentTimeMillis() + exipiresIn;
                                    break;

                                // Token Type, not using
                                case 2:
                                    //NO OP
                                    break;

                                // Refresh Token
                                case 3:
                                    refreshToken = innerSplit[1];
                                    break;

                                // Username
                                case 4:
                                    username = innerSplit[1];
                                    break;
                            }

                            index++;
                        }

                        // Make sure that we everythin was set
                        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(accessToken) &&
                                !TextUtils.isEmpty(refreshToken) && accessTokenExpiration > 0) {
                            ImgurUser user = new ImgurUser(username, accessToken, refreshToken, accessTokenExpiration);
                            OpenImgurApp.getInstance().setUser(user);
                            Log.v(TAG, "User " + user.getUsername() + " logged in");
                        }

                    } else {
                        // Didn't get our tokens from the response, they probably denied accessed
                        Log.w(TAG, "URL didn't contain a '#'. User denied access");
                        view.loadUrl(Endpoints.LOGIN.getUrl());
                    }
                    return false;
                }
            });
        }
    }


}
