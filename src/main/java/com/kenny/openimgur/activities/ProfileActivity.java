package com.kenny.openimgur.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
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
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.astuetz.PagerSlidingTabStrip;
import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.fragments.ProfileCommentsFragment;
import com.kenny.openimgur.fragments.ProfileFavoritesFragment;
import com.kenny.openimgur.fragments.ProfileInfoFragment;
import com.kenny.openimgur.fragments.ProfileMessagesFragment;
import com.kenny.openimgur.fragments.ProfileSubmissionsFragment;
import com.kenny.openimgur.fragments.ProfileUploadsFragment;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;

import org.json.JSONException;

import java.io.IOException;

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileActivity extends BaseActivity implements FragmentListener {
    private static final String REDIRECT_URL = "https://com.kenny.openimgur";

    public static final int REQUEST_CODE = 101;

    public static final String KEY_LOGGED_IN = "logged_in";

    public static final String KEY_LOGGED_OUT = "logged_out";

    private static final String KEY_USERNAME = "username";

    private static final String KEY_USER = "user";

    @InjectView(R.id.slidingTabs)
    PagerSlidingTabStrip mSlidingTabs;

    @InjectView(R.id.pager)
    ViewPager mPager;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.toolBar)
    Toolbar mToolBar;

    @InjectView(R.id.toolBarContainer)
    View mToolbarContainer;

    private ImgurUser mSelectedUser;

    private ProfilePager mAdapter;

    private boolean mIsAnimating = false;

    public static Intent createIntent(Context context, @Nullable String userName) {
        Intent intent = new Intent(context, ProfileActivity.class);

        if (!TextUtils.isEmpty(userName)) {
            intent.putExtra(KEY_USERNAME, userName);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupToolBar();
        mSlidingTabs.setBackgroundColor(getResources().getColor(theme.primaryColor));
        mSlidingTabs.setTextColor(Color.WHITE);
        handleData(savedInstanceState, getIntent());
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar() {
        if (isLandscape() && !isTablet()) {
            // Don't add the extra padding
        } else {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) mToolBar.getLayoutParams();
            lp.setMargins(0, ViewUtils.getStatusBarHeight(getApplicationContext()), 0, 0);
            mToolBar.setLayoutParams(lp);
        }

        mToolBar.setBackgroundColor(getResources().getColor(app.getImgurTheme().primaryColor));
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
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
            String userName = mSelectedUser == null ? username : mSelectedUser.getUsername();
            String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), userName);
            getSupportActionBar().setTitle(userName);
            new ApiClient(detailsUrls, ApiClient.HttpRequest.GET).doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, userName, null);
        } else {
            LogUtil.v(TAG, "Selected user present in database and has valid data");
            mAdapter = new ProfilePager(getApplicationContext(), getFragmentManager(), mSelectedUser);
            mPager.setAdapter(mAdapter);
            mSlidingTabs.setViewPager(mPager);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            getSupportActionBar().setTitle(mSelectedUser.getUsername());
            supportInvalidateOptionsMenu();
        }
    }

    public void onUserLogout() {
        mSelectedUser = null;
        app.onLogout();
        configWebView();
        setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_LOGGED_OUT, true));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.logout).setVisible(mSelectedUser != null && mSelectedUser.isSelf(app));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                new MaterialDialog.Builder(ProfileActivity.this)
                        .title(R.string.logout)
                        .content(R.string.logout_confirm)
                        .negativeText(R.string.cancel)
                        .positiveText(R.string.yes)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                onUserLogout();
                            }
                        }).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Configures the webview to handle a user logging in
     */
    private void configWebView() {
        getSupportActionBar().hide();
        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        WebView webView = (WebView) mMultiView.getView(MultiStateView.ViewState.EMPTY).findViewById(R.id.loginWebView);
        webView.loadUrl(Endpoints.LOGIN.getUrl());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(REDIRECT_URL)) {

                    if (url.contains("/?error=")) {
                        LogUtil.v(TAG, "Error received from URL " + url);
                        view.loadUrl(Endpoints.LOGIN.getUrl());
                        return true;
                    }

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
                        new ApiClient(detailsUrls, ApiClient.HttpRequest.GET).doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, newUser.getUsername(), null);
                        CookieManager.getInstance().removeAllCookie();
                        view.clearHistory();
                        view.clearCache(true);
                        view.clearFormData();
                        getSupportActionBar().show();
                        getSupportActionBar().setTitle(user.getUsername());
                        setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_LOGGED_IN, true));
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, R.string.error_generic);
                    }
                } else {
                    view.loadUrl(url);
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

                    if (mSelectedUser.isSelf(app)) {
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
                    supportInvalidateOptionsMenu();
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;
            }
        }
    };

    @Override
    public void onUpdateActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onUpdateActionBar(boolean shouldShow) {
        boolean isVisibile = mToolbarContainer.getTranslationY() == 0;

        if (isVisibile != shouldShow && !mIsAnimating) {
            //Actionbar visibility has changed
            mIsAnimating = true;
            mToolbarContainer.animate().translationY(shouldShow ? 0 : -mToolBar.getHeight()).setListener(mAnimAdapter);
        }
    }

    private AnimatorListenerAdapter mAnimAdapter = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationCancel(Animator animation) {
            super.onAnimationCancel(animation);
            mIsAnimating = false;
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
            mIsAnimating = false;
        }
    };

    @Override
    public void onLoadingStarted() {
        // NOOP
    }

    @Override
    public void onLoadingComplete() {
        // NOOP
    }

    @Override
    public void onError(int errorCode) {
        // NOOP
    }

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
                    return new ProfileUploadsFragment();

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
