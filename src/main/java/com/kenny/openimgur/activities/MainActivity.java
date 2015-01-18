package com.kenny.openimgur.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
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
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.NavAdapter;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.fragments.GalleryFilterFragment;
import com.kenny.openimgur.fragments.GalleryFragment;
import com.kenny.openimgur.fragments.NavFragment;
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.fragments.UploadedPhotosFragment;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 10/19/14.
 */
public class MainActivity extends BaseActivity implements NavFragment.NavigationListener, FragmentListener, View.OnClickListener {
    private static final String KEY_CURRENT_PAGE = "current_page";

    @InjectView(R.id.drawerLayout)
    DrawerLayout mDrawer;

    @InjectView(R.id.uploadMenu)
    View mUploadMenu;

    @InjectView(R.id.cameraUpload)
    FloatingActionButton mCameraUpload;

    @InjectView(R.id.galleryUpload)
    FloatingActionButton mGalleryUpload;

    @InjectView(R.id.linkUpload)
    FloatingActionButton mLinkUpload;

    @InjectView(R.id.uploadButton)
    FloatingActionButton mUploadButton;

    @InjectView(R.id.toolBar)
    Toolbar mToolBar;

    private NavFragment mNavFragment;

    private int mCurrentPage = -1;

    private float mUploadButtonHeight;

    private float mUploadMenuButtonHeight;

    private int mNavBarHeight = -1;

    private boolean uploadMenuOpen = false;

    private boolean uploadMenuShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolBar();
        mNavFragment = (NavFragment) getFragmentManager().findFragmentById(R.id.navDrawer);
        mNavFragment.configDrawerLayout(mDrawer);
        Resources res = getResources();
        int accentColor = res.getColor(theme.accentColor);
        mUploadButton.setColor(accentColor);
        mLinkUpload.setColor(accentColor);
        mCameraUpload.setColor(accentColor);
        mGalleryUpload.setColor(accentColor);
        mUploadButtonHeight = getResources().getDimension(R.dimen.fab_button_radius);
        mUploadMenuButtonHeight = getResources().getDimension(R.dimen.fab_button_radius_smaller);
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

        mToolBar.setBackgroundColor(getResources().getColor(app.getImgurTheme().primaryColor));
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
                //fragment = ProfileFragment.createInstance(null);
                //mCurrentPage = position;
                startActivity(ProfileActivity.createIntent(getApplicationContext(), null));
                break;

            case NavAdapter.PAGE_SUBREDDIT:
                fragment = RedditFragment.createInstance();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_UPLOADS:
                fragment = UploadedPhotosFragment.createInstance();
                mCurrentPage = position;
                break;

            case NavAdapter.PAGE_SETTINGS:
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

        // We will clear the cache every 7 days
        if (lastClear == 0) {
            edit.putLong("lastClear", System.currentTimeMillis());
        } else {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastClear >= DateUtils.DAY_IN_MILLIS * 7) {
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
        animateUploadMenuButton(shouldShow);
    }

    @Override
    public void onLoadingComplete() {
        mUploadMenu.setVisibility(View.VISIBLE);
        uploadMenuShowing = true;
    }

    @Override
    public void onLoadingStarted() {
        mUploadMenu.setVisibility(View.GONE);
        uploadMenuShowing = false;
    }

    @Override
    public void onError(int errorCode) {
        mUploadMenu.setVisibility(View.GONE);
        uploadMenuShowing = false;
    }

    @Override
    public void onUpdateActionBarTitle(String title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onUpdateUser(ImgurUser user) {
        if (mNavFragment != null) mNavFragment.onUsernameChange(user);
    }

    @Override
    public void onDrawerToggle(boolean isOpen) {
        // NOOP
    }

    @OnClick({R.id.uploadButton, R.id.linkUpload, R.id.cameraUpload, R.id.galleryUpload})
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.uploadButton:
                animateUploadMenu();
                break;

            case R.id.cameraUpload:
                startActivityForResult(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_CAMERA), UploadActivity.REQUEST_CODE);
                animateUploadMenu();
                break;

            case R.id.galleryUpload:
                startActivityForResult(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_GALLERY), UploadActivity.REQUEST_CODE);
                animateUploadMenu();
                break;

            case R.id.linkUpload:
                startActivityForResult(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_LINK), UploadActivity.REQUEST_CODE);
                animateUploadMenu();
                break;
        }
    }

    /**
     * Animates the opening/closing of the Upload button
     */
    private void animateUploadMenu() {
        AnimatorSet set = new AnimatorSet().setDuration(500L);
        String translation = isLandscape() ? "translationX" : "translationY";

        if (!uploadMenuOpen) {
            uploadMenuOpen = true;

            set.playTogether(
                    ObjectAnimator.ofFloat(mCameraUpload, translation, 0, (mUploadButtonHeight + 25) * -1),
                    ObjectAnimator.ofFloat(mLinkUpload, translation, 0, ((2 * mUploadMenuButtonHeight) + mUploadButtonHeight + 75) * -1),
                    ObjectAnimator.ofFloat(mGalleryUpload, translation, (mUploadMenuButtonHeight + mUploadButtonHeight + 50) * -1),
                    ObjectAnimator.ofFloat(mCameraUpload, "alpha", 0.0f, 1.0f),
                    ObjectAnimator.ofFloat(mGalleryUpload, "alpha", 0.0f, 1.0f),
                    ObjectAnimator.ofFloat(mLinkUpload, "alpha", 0.0f, 1.0f),
                    ObjectAnimator.ofFloat(mUploadButton, "rotation", 0.0f, 135.0f)
            );

            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    mCameraUpload.setVisibility(View.VISIBLE);
                    mGalleryUpload.setVisibility(View.VISIBLE);
                    mLinkUpload.setVisibility(View.VISIBLE);
                    animation.removeAllListeners();
                }
            });

            set.setInterpolator(new OvershootInterpolator());
            set.start();
        } else {
            uploadMenuOpen = false;

            set.playTogether(
                    ObjectAnimator.ofFloat(mCameraUpload, translation, 0),
                    ObjectAnimator.ofFloat(mLinkUpload, translation, 0),
                    ObjectAnimator.ofFloat(mGalleryUpload, translation, 0),
                    ObjectAnimator.ofFloat(mCameraUpload, "alpha", 1.0f, 0.0f),
                    ObjectAnimator.ofFloat(mGalleryUpload, "alpha", 1.0f, 0.0f),
                    ObjectAnimator.ofFloat(mLinkUpload, "alpha", 1.0f, 0.0f),
                    ObjectAnimator.ofFloat(mUploadButton, "rotation", 135.0f, 0.0f)
            );

            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    mCameraUpload.setVisibility(View.GONE);
                    mGalleryUpload.setVisibility(View.GONE);
                    mLinkUpload.setVisibility(View.GONE);
                    animation.removeAllListeners();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mCameraUpload.setVisibility(View.GONE);
                    mGalleryUpload.setVisibility(View.GONE);
                    mLinkUpload.setVisibility(View.GONE);
                    animation.removeAllListeners();
                }
            });

            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
        }
    }

    /**
     * Animates the upload button
     *
     * @param shouldShow If the button should be shown
     */
    private void animateUploadMenuButton(boolean shouldShow) {
        if (!shouldShow && uploadMenuShowing) {
            float hideDistance;
            uploadMenuShowing = false;
            hideDistance = mUploadButtonHeight + (mUploadButtonHeight / 2);
            // Add extra distance to the hiding of the button if on KitKat due to the translucent nav bar
            if (app.sdkVersion >= Build.VERSION_CODES.KITKAT) {
                if (mNavBarHeight == -1)
                    mNavBarHeight = ViewUtils.getNavigationBarHeight(getApplicationContext());
                hideDistance += mNavBarHeight;
            }

            mUploadMenu.animate().setInterpolator(new AccelerateDecelerateInterpolator()).translationY(hideDistance).setDuration(350).start();
            // Close the menu if it is open
            if (uploadMenuOpen) animateUploadMenu();
        } else if (shouldShow && !uploadMenuShowing) {
            uploadMenuShowing = true;
            mUploadMenu.animate().setInterpolator(new AccelerateDecelerateInterpolator()).translationY(0).setDuration(350).start();
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
                ((GalleryFilterFragment) fragment).dismiss(null, null);
            } else {
                fm.beginTransaction().remove(fragment).commit();
            }

            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Set the theme if coming from the settings activity
            case SettingsActivity.REQUEST_CODE:
                app = OpenImgurApp.getInstance(getApplicationContext());
                theme = app.getImgurTheme();
                Resources res = getResources();
                int accentColor = res.getColor(theme.accentColor);
                mToolBar.setBackgroundColor(res.getColor(theme.primaryColor));
                mUploadButton.setColor(accentColor);
                mLinkUpload.setColor(accentColor);
                mCameraUpload.setColor(accentColor);
                mGalleryUpload.setColor(accentColor);
                setStatusBarColor(res.getColor(theme.darkColor));
                mNavFragment.onUpdateTheme(theme);
                break;

            case UploadActivity.REQUEST_CODE:
                Fragment current = getFragmentManager().findFragmentById(R.id.container);
                if (current != null) current.onActivityResult(requestCode, resultCode, data);
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
