package com.kenny.openimgur.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.CheckBox;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.clans.fab.FloatingActionMenu;
import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.NavAdapter;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.fragments.GalleryFilterFragment;
import com.kenny.openimgur.fragments.GalleryFragment;
import com.kenny.openimgur.fragments.MemeFragment;
import com.kenny.openimgur.fragments.NavFragment;
import com.kenny.openimgur.fragments.RandomFragment;
import com.kenny.openimgur.fragments.RedditFilterFragment;
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.fragments.TopicsFragment;
import com.kenny.openimgur.fragments.UploadedPhotosFragment;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 10/19/14.
 */
public class MainActivity extends BaseActivity implements NavFragment.NavigationListener, FragmentListener {
    private static final String KEY_CURRENT_PAGE = "current_page";

    @InjectView(R.id.drawerLayout)
    DrawerLayout mDrawer;

    @InjectView(R.id.uploadMenu)
    FloatingActionMenu mUploadMenu;

    @InjectView(R.id.toolBar)
    Toolbar mToolBar;

    private NavFragment mNavFragment;

    private int mCurrentPage = -1;

    private ImgurTheme mSavedTheme;

    private boolean mIsDarkTheme;

    private boolean mNagOnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolBar();
        mNavFragment = (NavFragment) getFragmentManager().findFragmentById(R.id.navDrawer);
        mNavFragment.configDrawerLayout(mDrawer);
        mNagOnExit = app.getPreferences().getBoolean(SettingsActivity.KEY_CONFIRM_EXIT, true);
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar() {
        if (isLandscape() && !isTablet()) {
            // Don't add the extra padding
        } else {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mToolBar.getLayoutParams();
            lp.setMargins(0, ViewUtils.getStatusBarHeight(getApplicationContext()), 0, 0);
            mToolBar.setLayoutParams(lp);
        }

        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void onNavigationItemSelected(int position) {
        if (position <= 2 && mCurrentPage == position) return;
        changePage(position);
        mDrawer.closeDrawers();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mNavFragment.onOptionsItemSelected(item)) return true;
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

        if (savedInstanceState == null) {
            changePage(app.getPreferences().getInt(KEY_CURRENT_PAGE, NavAdapter.PAGE_GALLERY));
        } else {
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, NavAdapter.PAGE_GALLERY);
        }

        mNavFragment.setSelectedPage(mCurrentPage);
    }

    /**
     * Changes the fragment
     *
     * @param position The pages position
     */
    private void changePage(int position) {
        Fragment fragment = null;

        switch (position) {
            case NavAdapter.PAGE_GALLERY:
                fragment = GalleryFragment.createInstance();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_PROFILE:
                startActivityForResult(ProfileActivity.createIntent(getApplicationContext(), null), ProfileActivity.REQUEST_CODE);
                break;

            case NavAdapter.PAGE_SUBREDDIT:
                fragment = RedditFragment.createInstance();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_RANDOM:
                fragment = RandomFragment.createInstance();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_UPLOADS:
                fragment = UploadedPhotosFragment.createInstance();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_SETTINGS:
                mSavedTheme = ImgurTheme.copy(theme);
                mIsDarkTheme = app.getPreferences().getBoolean(SettingsActivity.KEY_DARK_THEME, mSavedTheme.isDarkTheme);
                startActivityForResult(SettingsActivity.createIntent(getApplicationContext()), SettingsActivity.REQUEST_CODE);
                break;

            case NavAdapter.PAGE_FEEDBACK:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "kennyc.developer@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Open Imgur Feedback");

                if (emailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback)));
                } else {
                    SnackBar.show(this, R.string.cant_launch_intent);
                }
                break;

            case NavAdapter.PAGE_TOPICS:
                fragment = new TopicsFragment();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_MEME:
                fragment = new MemeFragment();
                mCurrentPage = position;
                break;
        }

        if (fragment != null) {
            getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
            onUpdateActionBar(true);
        }
    }

    @Override
    protected void onDestroy() {
        SharedPreferences pref = app.getPreferences();
        SharedPreferences.Editor edit = pref.edit();
        long lastClear = app.getPreferences().getLong("lastClear", 0);

        // We will clear the cache every 4 days
        if (lastClear == 0) {
            edit.putLong("lastClear", System.currentTimeMillis());
        } else {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastClear >= DateUtils.DAY_IN_MILLIS * 4) {
                app.deleteAllCache();
                edit.putLong("lastClear", currentTime);
            }
        }

        edit.putInt(KEY_CURRENT_PAGE, mCurrentPage).apply();
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
        if (shouldShow) {
            mUploadMenu.showMenuButton(true);
        } else {
            mUploadMenu.hideMenuButton(true);
        }
    }

    @Override
    public void onLoadingComplete() {
        if (mUploadMenu.isMenuButtonHidden()) mUploadMenu.showMenuButton(false);
    }

    @Override
    public void onLoadingStarted() {
        if (!mUploadMenu.isMenuButtonHidden()) mUploadMenu.hideMenuButton(false);
    }

    @Override
    public void onError(int errorCode) {
        if (!mUploadMenu.isMenuButtonHidden()) mUploadMenu.hideMenuButton(false);
    }

    @Override
    public void onUpdateActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onDrawerToggle(boolean isOpen) {
        // NOOP
    }

    @OnClick({R.id.linkUpload, R.id.cameraUpload, R.id.galleryUpload})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cameraUpload:
                startActivityForResult(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_CAMERA), UploadActivity.REQUEST_CODE);
                mUploadMenu.close(true);
                break;

            case R.id.galleryUpload:
                startActivityForResult(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_GALLERY), UploadActivity.REQUEST_CODE);
                mUploadMenu.close(true);
                break;

            case R.id.linkUpload:
                startActivityForResult(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_LINK), UploadActivity.REQUEST_CODE);
                mUploadMenu.close(true);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(Gravity.START)) {
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
        } else if (mUploadMenu.isOpened()) {
            mUploadMenu.close(true);
            return;
        } else if (mNagOnExit) {
            showExitNag();
            return;
        }

        super.onBackPressed();
    }

    private void showExitNag() {
        new MaterialDialog.Builder(this)
                .title(R.string.exit)
                .customView(R.layout.exit_nag, false)
                .negativeText(R.string.cancel)
                .positiveText(R.string.yes)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        CheckBox cb = (CheckBox) dialog.getCustomView().findViewById(R.id.askAgainCB);

                        if (cb != null && cb.isChecked()) {
                            app.getPreferences().edit().putBoolean(SettingsActivity.KEY_CONFIRM_EXIT, false).apply();
                        }

                        finish();
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Set the theme if coming from the settings activity
            case SettingsActivity.REQUEST_CODE:
                ImgurTheme theme = OpenImgurApp.getInstance(getApplicationContext()).getImgurTheme();
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

            case UploadActivity.REQUEST_CODE:
                Fragment current = getFragmentManager().findFragmentById(R.id.container);
                if (current != null) current.onActivityResult(requestCode, resultCode, data);
                break;

            case ProfileActivity.REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getBooleanExtra(ProfileActivity.KEY_LOGGED_IN, false)) {
                        app = OpenImgurApp.getInstance(getApplicationContext());
                        if (mNavFragment != null) mNavFragment.onUserLogin(app.getUser());
                    } else if (data.getBooleanExtra(ProfileActivity.KEY_LOGGED_OUT, false)) {
                        if (mNavFragment != null) mNavFragment.onUserLogin(null);
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
