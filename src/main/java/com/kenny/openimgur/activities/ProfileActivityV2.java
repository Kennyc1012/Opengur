package com.kenny.openimgur.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.astuetz.PagerSlidingTabStrip;
import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.fragments.ProfileCommentsFragment;
import com.kenny.openimgur.fragments.ProfileFavoritesFragment;
import com.kenny.openimgur.fragments.ProfileInfoFragment;
import com.kenny.openimgur.fragments.ProfileMessagesFragment;
import com.kenny.openimgur.fragments.ProfileSubmissionsFragment;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;

import java.io.IOException;

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileActivityV2 extends BaseActivity {
    private static final String KEY_USERNAME = "username";

    private static final String KEY_USER = "user";

    @InjectView(R.id.slidingTabs)
    PagerSlidingTabStrip mSlidingTabs;

    @InjectView(R.id.pager)
    ViewPager mPager;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    private ImgurUser mSelectedUser;

    private ProfilePager mAdapter;

    public static Intent createIntent(Context context, @Nullable String userName) {
        Intent intent = new Intent(context, ProfileActivityV2.class);

        if (!TextUtils.isEmpty(userName)) {
            intent.putExtra(KEY_USERNAME, userName);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().hide();
        mSlidingTabs.setBackgroundColor(getResources().getColor(theme.primaryColor));
        mSlidingTabs.setTextColor(Color.WHITE);
        handleData(savedInstanceState, getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    private void handleData(Bundle savedInstanceState, Intent args) {
        if (savedInstanceState == null) {
            if (args.hasExtra(KEY_USERNAME)) {
                LogUtil.v(TAG, "User present in Bundle extras");
                String username = args.getStringExtra(KEY_USERNAME);
                mSelectedUser = app.getSql().getUser(username);
                configUser(username);
            } else if (app.getUser() != null) {
                LogUtil.v(TAG, "User already logged in");
                mSelectedUser = app.getUser();
                configUser(null);
            } else {
                LogUtil.v(TAG, "No user present. Showing Login screen");
                configWebView();
            }
        } else if (savedInstanceState.containsKey(KEY_USER)) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
            configUser(null);
        }
    }

    private void configUser(String username) {
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        // Load the new user data if we haven't viewed the user within 24 hours
        if (mSelectedUser == null || System.currentTimeMillis() - mSelectedUser.getLastSeen() >= DateUtils.DAY_IN_MILLIS) {
            LogUtil.v(TAG, "Selected user is null or data is too old, fetching new data");
            String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), mSelectedUser == null ? username : mSelectedUser.getUsername());
            new ApiClient(detailsUrls, ApiClient.HttpRequest.GET).doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, null, null);
        } else {
            LogUtil.v(TAG, "Selected user present in database and has valid data");
            mAdapter = new ProfilePager(getApplicationContext(), getFragmentManager(), mSelectedUser);
            mPager.setAdapter(mAdapter);
            mSlidingTabs.setViewPager(mPager);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        }
    }

    /**
     * Configures the webview to handle a user logging in
     */
    private void configWebView() {
        getSupportActionBar().show();
        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        WebView webView = (WebView) mMultiView.getView(MultiStateView.ViewState.EMPTY).findViewById(R.id.loginWebView);
        webView.loadUrl(Endpoints.LOGIN.getUrl());
        webView.setWebViewClient(new WebViewClient() {
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
                                long expiresIn = Long.parseLong(innerSplit[1]);
                                accessTokenExpiration = System.currentTimeMillis() + (expiresIn * DateUtils.SECOND_IN_MILLIS);
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

                    // Make sure that everything was set
                    if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(accessToken) &&
                            !TextUtils.isEmpty(refreshToken) && accessTokenExpiration > 0) {
                        ImgurUser newUser = new ImgurUser(username, accessToken, refreshToken, accessTokenExpiration);
                        app.setUser(newUser);
                        user = newUser;
                        mSelectedUser = newUser;
                        LogUtil.v(TAG, "User " + newUser.getUsername() + " logged in");
                        String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), newUser.getUsername());
                        new ApiClient(detailsUrls, ApiClient.HttpRequest.GET).doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, null, null);
                        CookieManager.getInstance().removeAllCookie();
                        view.clearHistory();
                        view.clearCache(true);
                        view.clearFormData();
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, R.string.error_generic);
                    }

                } else {
                    // Didn't get our tokens from the response, they probably denied accessed, just reshow the login page
                    LogUtil.w(TAG, "URL didn't contain a '#'. User denied access");
                    view.loadUrl(Endpoints.LOGIN.getUrl());
                }

                return true;
            }
        });
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.PROFILE_DETAILS) {
            try {
                int status = event.json.getInt(ApiClient.KEY_STATUS);

                if (status == ApiClient.STATUS_OK) {
                    if (mSelectedUser == null) {
                        mSelectedUser = new ImgurUser(event.json);
                    } else {
                        mSelectedUser.parseJsonForValues(event.json);
                    }

                    if (mSelectedUser.isSelf()) {
                        app.getSql().updateUserInfo(mSelectedUser);
                    } else {
                        app.getSql().insertProfile(mSelectedUser);
                    }

                    mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE);
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(status));
                }

            } catch (JSONException ex) {

            }

        }
    }

    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        LogUtil.e(TAG, "Error received from Event Bus", e);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedUser != null) {
            outState.putParcelable(KEY_USER, mSelectedUser);
        }
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    mAdapter = new ProfilePager(getApplicationContext(), getFragmentManager(), mSelectedUser);
                    mPager.setAdapter(mAdapter);
                    mSlidingTabs.setViewPager(mPager);
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;
            }
        }
    };

    private static class ProfilePager extends FragmentStatePagerAdapter {
        private static final int NUM_PAGES_SELF = 6;

        private static final int NUM_PAGES_FAR = 4;

        private String[] mTitles;

        private ImgurUser mUser;

        private int mNumPages;


        public ProfilePager(Context context, FragmentManager fm, ImgurUser user) {
            super(fm);
            mTitles = context.getResources().getStringArray(R.array.profile_tabs);
            mUser = user;
            mNumPages = mUser.isSelf() ? NUM_PAGES_SELF : NUM_PAGES_FAR;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                // Info
                case 0:
                    return ProfileInfoFragment.createInstance(mUser);

                // Submissions
                case 1:
                    return ProfileSubmissionsFragment.createInstance(mUser);

                // Favorites
                case 2:
                    return ProfileFavoritesFragment.createInstance(mUser);

                // Comments
                case 3:
                    return ProfileCommentsFragment.createInstance(mUser);

                // Uploads
                case 4:
                    return ProfileInfoFragment.createInstance(mUser);

                // Messages
                case 5:
                    return new ProfileMessagesFragment();

                default:
                    throw new IndexOutOfBoundsException("How did this happen?");
            }
        }

        @Override
        public int getCount() {
            return mNumPages;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }
}
