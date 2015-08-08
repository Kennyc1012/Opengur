package com.kenny.openimgur.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.fragments.GalleryFilterFragment;
import com.kenny.openimgur.fragments.GalleryFragment;
import com.kenny.openimgur.fragments.MemeFragment;
import com.kenny.openimgur.fragments.RandomFragment;
import com.kenny.openimgur.fragments.RedditFilterFragment;
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.fragments.TopicsFragment;
import com.kenny.openimgur.fragments.UploadedPhotosFragment;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.snackbar.SnackBar;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by kcampagna on 10/19/14.
 */
public class MainActivity extends BaseActivity implements FragmentListener, NavigationView.OnNavigationItemSelectedListener {
    private static final String KEY_CURRENT_PAGE = "current_page";

    public static final int PAGE_PROFILE = 0;

    public static final int PAGE_GALLERY = 1;

    public static final int PAGE_TOPICS = 2;

    public static final int PAGE_MEME = 3;

    public static final int PAGE_SUBREDDIT = 4;

    public static final int PAGE_RANDOM = 5;

    public static final int PAGE_UPLOADS = 6;

    public static final int PAGE_SETTINGS = 7;

    public static final int PAGE_BETA = 8;

    public static final int PAGE_FEEDBACK = 9;

    @Bind(R.id.drawerLayout)
    DrawerLayout mDrawer;

    @Bind(R.id.fab)
    FloatingActionButton mUploadButton;

    @Bind(R.id.toolBar)
    Toolbar mToolBar;

    @Bind(R.id.navigationView)
    NavigationView mNavigationView;

    private int mCurrentPage = -1;

    private ImgurTheme mSavedTheme;

    private boolean mIsDarkTheme;

    private boolean mNagOnExit;

    private boolean mIsFABShowing = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNavigationView.setNavigationItemSelectedListener(this);
        ColorStateList selector = theme.getNavigationColors(getResources());
        mNavigationView.setItemTextColor(selector);
        mNavigationView.setItemIconTintList(selector);
        setupToolBar();
        mNagOnExit = app.getPreferences().getBoolean(SettingsActivity.KEY_CONFIRM_EXIT, true);
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar() {
        setSupportActionBar(mToolBar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu);
        ab.setHomeButtonEnabled(true);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        menuItem.setChecked(true);
        changePage(menuItem.getItemId());
        mDrawer.closeDrawers();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Dismiss any filter fragments that might be left behind
        if (getFragmentManager().findFragmentByTag("filter") != null) {
            FragmentManager fm = getFragmentManager();
            fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commitAllowingStateLoss();
        }

        int fragmentPage;
        int menuItemId = R.id.nav_gallery;

        if (savedInstanceState == null) {
            fragmentPage = app.getPreferences().getInt(KEY_CURRENT_PAGE, PAGE_GALLERY);
        } else {
            fragmentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, PAGE_GALLERY);
        }

        if (mNavigationView.getMenu() != null) {
            switch (fragmentPage) {
                case PAGE_TOPICS:
                    menuItemId = R.id.nav_topics;
                    break;

                case PAGE_SUBREDDIT:
                    menuItemId = R.id.nav_reddit;
                    break;

                case PAGE_MEME:
                    menuItemId = R.id.nav_meme;
                    break;

                case PAGE_RANDOM:
                    menuItemId = R.id.nav_random;
                    break;

                case PAGE_UPLOADS:
                    menuItemId = R.id.nav_uploads;
                    break;

                case PAGE_GALLERY:
                default:
                    menuItemId = R.id.nav_gallery;
                    break;
            }

            MenuItem item = mNavigationView.getMenu().findItem(menuItemId);
            if (item != null) item.setChecked(true);
        }

        if (getFragmentManager().findFragmentById(R.id.container) == null) changePage(menuItemId);
        updateUserHeader(user);
    }

    private void updateUserHeader(@Nullable ImgurUser user) {
        View header = mNavigationView.findViewById(R.id.header);
        if (header == null) return;

        ImageView avatar = (ImageView) header.findViewById(R.id.profileImg);
        TextView name = (TextView) header.findViewById(R.id.profileName);
        TextView rep = (TextView) header.findViewById(R.id.reputation);
        TextView badge = (TextView) header.findViewById(R.id.badgeCount);
        View badgeContainer = header.findViewById(R.id.badgeContainer);

        if (user != null) {
            int size = getResources().getDimensionPixelSize(R.dimen.avatar_size);
            String firstLetter = user.getUsername().substring(0, 1);

            avatar.setImageDrawable(TextDrawable.builder()
                    .beginConfig()
                    .toUpperCase()
                    .width(size)
                    .height(size)
                    .endConfig()
                    .buildRound(firstLetter, getResources().getColor(theme.accentColor)));

            name.setText(user.getUsername());
            rep.setText(user.getNotoriety().getStringId());
            badge.setVisibility(View.VISIBLE);
            badgeContainer.setVisibility(View.VISIBLE);
            int notificationCount = app.getSql().getNotifications().size();
            String badgeText = null;

            if (notificationCount > 0) {
                badgeText = notificationCount > 9 ? "9+" : String.valueOf(notificationCount);
            }

            badge.setText(badgeText);

            badgeContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO Notification Activity
                }
            });
        } else {
            avatar.setImageResource(R.drawable.ic_account_circle);
            name.setText(R.string.profile);
            rep.setText(R.string.login_msg);
            badge.setVisibility(View.GONE);
            badgeContainer.setVisibility(View.GONE);
        }

        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawer.closeDrawers();
                startActivityForResult(ProfileActivity.createIntent(getApplicationContext(), null), RequestCodes.PROFILE);
            }
        });
    }

    /**
     * Changes the fragment
     *
     * @param menuItemId The menu item id of the page selected
     */
    private void changePage(int menuItemId) {
        Fragment fragment = null;

        switch (menuItemId) {
            case R.id.nav_gallery:
                if (mCurrentPage == PAGE_GALLERY) return;
                fragment = GalleryFragment.newInstance();
                mCurrentPage = PAGE_GALLERY;
                break;

            case R.id.nav_reddit:
                if (mCurrentPage == PAGE_SUBREDDIT) return;
                fragment = RedditFragment.newInstance();
                mCurrentPage = PAGE_SUBREDDIT;
                break;

            case R.id.nav_random:
                if (mCurrentPage == PAGE_RANDOM) return;
                fragment = RandomFragment.newInstance();
                mCurrentPage = PAGE_RANDOM;
                break;

            case R.id.nav_uploads:
                if (mCurrentPage == PAGE_UPLOADS) return;
                fragment = UploadedPhotosFragment.createInstance();
                mCurrentPage = PAGE_UPLOADS;
                break;

            case R.id.nav_settings:
                mSavedTheme = ImgurTheme.copy(theme);
                mIsDarkTheme = app.getPreferences().getBoolean(SettingsActivity.KEY_DARK_THEME, mSavedTheme.isDarkTheme);
                startActivityForResult(SettingsActivity.createIntent(getApplicationContext()), RequestCodes.SETTINGS);
                break;

            case R.id.nav_feedback:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "kennyc.developer@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Open Imgur Feedback");

                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback)));
                } else {
                    SnackBar.show(this, R.string.cant_launch_intent);
                }
                break;

            case R.id.nav_topics:
                if (mCurrentPage == PAGE_TOPICS) return;
                fragment = new TopicsFragment();
                mCurrentPage = PAGE_TOPICS;
                break;

            case R.id.nav_meme:
                if (mCurrentPage == PAGE_MEME) return;
                fragment = new MemeFragment();
                mCurrentPage = PAGE_MEME;
                break;

            case R.id.nav_beta:
                new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                        .setTitle(R.string.beta_test)
                        .setMessage(R.string.beta_message)
                        .setNegativeButton(R.string.beta_no, null)
                        .setPositiveButton(R.string.beta_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://plus.google.com/u/0/communities/107476382114210885879"));

                                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(browserIntent);
                                } else {
                                    SnackBar.show(MainActivity.this, R.string.cant_launch_intent);
                                }
                            }
                        }).show();
                break;
        }

        if (fragment != null) {
            getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
            onUpdateActionBar(true);
        }
    }

    @Override
    protected void onDestroy() {
        app.getPreferences().edit().putInt(KEY_CURRENT_PAGE, mCurrentPage).apply();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onUpdateActionBar(boolean shouldShow) {
        setActionBarVisibility(mToolBar, shouldShow);
        toggleFAB(shouldShow);
    }

    @Override
    public void onLoadingComplete() {
        toggleFAB(true);
    }

    @Override
    public void onLoadingStarted() {
        toggleFAB(false);
    }

    @Override
    public void onError() {
        toggleFAB(false);
    }

    @Override
    public void onUpdateActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    private void toggleFAB(boolean shouldShow) {
        if (shouldShow) {
            if (mIsFABShowing) {
                mIsFABShowing = false;
                mUploadButton.animate().translationY(0);
            }
        } else {
            if (!mIsFABShowing) {
                mIsFABShowing = true;
                mUploadButton.animate().translationY(mUploadButton.getHeight() * 2);
            }
        }
    }

    @OnClick({R.id.fab})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                startActivity(UploadActivity.createIntent(getApplicationContext()));
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawers();
            return;
        } else if (getFragmentManager().findFragmentByTag("filter") != null) {
            FragmentManager fm = getFragmentManager();
            Fragment fragment = fm.findFragmentByTag("filter");

            if (fragment instanceof GalleryFilterFragment) {
                ((GalleryFilterFragment) fragment).dismiss(null, null, null);
            } else if (fragment instanceof RedditFilterFragment) {
                ((RedditFilterFragment) fragment).dismiss(null, null);
            } else {
                fm.beginTransaction().remove(fragment).commit();
            }

            return;
        } else if (mNagOnExit) {
            showExitNag();
            return;
        }

        super.onBackPressed();
    }

    private void showExitNag() {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.exit)
                .setView(R.layout.exit_nag)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog instanceof AlertDialog) {
                            CheckBox cb = (CheckBox) ((AlertDialog) dialog).findViewById(R.id.askAgainCB);

                            if (cb != null && cb.isChecked()) {
                                app.getPreferences().edit().putBoolean(SettingsActivity.KEY_CONFIRM_EXIT, false).apply();
                            }
                        } else {
                            LogUtil.w(TAG, "Dialog was not an alert dialog... but how?");
                        }

                        finish();
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Set the theme if coming from the settings activity
            case RequestCodes.SETTINGS:
                ImgurTheme theme = OpengurApp.getInstance(getApplicationContext()).getImgurTheme();
                mNagOnExit = app.getPreferences().getBoolean(SettingsActivity.KEY_CONFIRM_EXIT, true);

                if (mSavedTheme == null || theme != mSavedTheme || mIsDarkTheme != theme.isDarkTheme) {
                    Intent intent = getIntent();
                    overridePendingTransition(0, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                }
                break;

            case RequestCodes.PROFILE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getBooleanExtra(ProfileActivity.KEY_LOGGED_IN, false)) {
                        app = OpengurApp.getInstance(getApplicationContext());
                        updateUserHeader(app.getUser());
                    } else if (data.getBooleanExtra(ProfileActivity.KEY_LOGGED_OUT, false)) {
                        updateUserHeader(null);
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Translucent_Main_Dark : R.style.Theme_Translucent_Main_Light;
    }
}
