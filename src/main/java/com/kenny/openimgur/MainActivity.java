package com.kenny.openimgur;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.fragments.GalleryFilterFragment;
import com.kenny.openimgur.fragments.GalleryFragment;
import com.kenny.openimgur.fragments.NavFragment;
import com.kenny.openimgur.fragments.ProfileFragment;
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.util.ViewUtils;

/**
 * Created by kcampagna on 10/19/14.
 */
public class MainActivity extends BaseActivity implements NavFragment.NavigationListener, FragmentListener, View.OnClickListener {
    private static final String KEY_CURRENT_PAGE = "current_page";

    private static final int PAGE_GALLERY = 0;

    private static final int PAGE_SUBREDDIT = 1;

    private static final int PAGE_PROFILE = 2;

    private static final int PAGE_SETTINGS = 4;

    private static final int PAGE_FEEDBACK = 5;

    private int mCurrentPage = -1;

    private NavFragment mNavFragment;

    private DrawerLayout mDrawer;

    private View mUploadMenu;

    private FloatingActionButton mCameraUpload;

    private FloatingActionButton mGalleryUpload;

    private FloatingActionButton mLinkUpload;

    private FloatingActionButton mUploadButton;

    private float mUploadButtonHeight;

    private float mUploadMenuButtonHeight;

    private int mNavBarHeight = -1;

    private boolean uploadMenuOpen = false;

    private boolean uploadMenuShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        setContentView(R.layout.activity_main);
        mNavFragment = (NavFragment) getFragmentManager().findFragmentById(R.id.navDrawer);
        mDrawer = (DrawerLayout) findViewById(R.id.drawerLayout);
        mNavFragment.configDrawerLayout(mDrawer);
        mUploadMenu = findViewById(R.id.uploadMenu);
        mUploadButton = (FloatingActionButton) mUploadMenu.findViewById(R.id.uploadButton);
        mLinkUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.linkUpload);
        mCameraUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.cameraUpload);
        mGalleryUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.galleryUpload);
        mUploadButton.setOnClickListener(this);
        mLinkUpload.setOnClickListener(this);
        mCameraUpload.setOnClickListener(this);
        mGalleryUpload.setOnClickListener(this);
        mUploadButtonHeight = getResources().getDimension(R.dimen.fab_button_radius);
        mUploadMenuButtonHeight = getResources().getDimension(R.dimen.fab_button_radius_smaller);
    }

    @Override
    public void onListItemSelected(int position) {
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            menu.clear();
        }

        return super.onPrepareOptionsMenu(menu);
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
            changePage(app.getPreferences().getInt(KEY_CURRENT_PAGE, PAGE_GALLERY));
        } else {
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, PAGE_GALLERY);
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
            case PAGE_GALLERY:
                fragment = GalleryFragment.createInstance();
                mCurrentPage = position;
                break;

            case PAGE_PROFILE:
                fragment = ProfileFragment.createInstance(null);
                mCurrentPage = position;
                break;

            case PAGE_SUBREDDIT:
                fragment = RedditFragment.createInstance();
                mCurrentPage = position;
                break;

            case PAGE_SETTINGS:
                startActivity(SettingsActivity.createIntent(getApplicationContext()));
                break;

            case PAGE_FEEDBACK:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "kennyc.developer@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Open Imgur Feedback");
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_feedback)));
                break;
        }

        if (fragment != null) {
            getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }
    }

    @Override
    protected void onDestroy() {
        app.getPreferences().edit().putInt(KEY_CURRENT_PAGE, mCurrentPage).apply();
        app.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onUpdateActionBar(boolean shouldShow) {
        setActionBarVisibility(shouldShow);
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
    public void onUpdateUser(String username, String defaultTitle) {
        if (mNavFragment != null) mNavFragment.onUsernameChange(username, defaultTitle);
    }

    @Override
    public void onDrawerToggle(boolean isOpen) {
        if (isOpen) {
            setActionBarVisibility(true);
        }

        supportInvalidateOptionsMenu();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.uploadButton:
                animateUploadMenu();
                break;

            case R.id.cameraUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_CAMERA));
                animateUploadMenu();
                break;

            case R.id.galleryUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_GALLERY));
                animateUploadMenu();
                break;

            case R.id.linkUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_LINK));
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
            if (OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
                if (mNavBarHeight == -1) mNavBarHeight = ViewUtils.getNavigationBarHeight(getApplicationContext());
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
}
