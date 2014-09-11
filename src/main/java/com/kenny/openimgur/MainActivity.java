package com.kenny.openimgur;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.classes.TabActivityListener;
import com.kenny.openimgur.fragments.GalleryFragment;
import com.kenny.openimgur.fragments.ProfileFragment;
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.util.ViewUtils;
import com.mirko.tbv.TabBarView;

import java.util.Locale;

/**
 * Created by kcampagna on 8/14/14.
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, TabActivityListener {

    private ViewPager mPager;

    private View mUploadMenu;

    private FloatingActionButton mCameraUpload;

    private FloatingActionButton mGalleryUpload;

    private FloatingActionButton mLinkUpload;

    private boolean uploadMenuOpen = false;

    private boolean uploadMenuShowing = false;

    private int mNavBarHeight = -1;

    private float mUploadButtonHeight;

    private float mUploadMenuButtonHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setShouldDisplayHome(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(2);
        mPager.setAdapter(new TabPageAdapter(getFragmentManager(), getResources().getStringArray(R.array.pager_titles)));
        TabBarView tabBar = (TabBarView) View.inflate(getApplicationContext(), R.layout.tab_ab_layout, null);
        tabBar.setViewPager(mPager);
        mUploadMenu = findViewById(R.id.uploadMenu);
        FloatingActionButton uploadButton = (FloatingActionButton) mUploadMenu.findViewById(R.id.uploadButton);
        mLinkUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.linkUpload);
        mCameraUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.cameraUpload);
        mGalleryUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.galleryUpload);
        uploadButton.setOnClickListener(this);
        mLinkUpload.setOnClickListener(this);
        mCameraUpload.setOnClickListener(this);
        mGalleryUpload.setOnClickListener(this);
        setActionBarView(tabBar);
        mUploadButtonHeight = getResources().getDimension(R.dimen.fab_button_radius);
        mUploadMenuButtonHeight = getResources().getDimension(R.dimen.fab_button_radius_smaller);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(SettingsActivity.createIntent(getApplicationContext()));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onHideActionBar(boolean shouldShow) {
        setActionBarVisibility(shouldShow);
        animateUploadMenuButton(shouldShow);
    }

    @Override
    public void onError(int errorCode, int page) {
        if (page == mPager.getCurrentItem()) {
            mUploadMenu.setVisibility(View.GONE);
            uploadMenuShowing = false;
        }
    }

    @Override
    public void onLoadingComplete(int page) {
        if (page == mPager.getCurrentItem()) {
            mUploadMenu.setVisibility(View.VISIBLE);
            uploadMenuShowing = true;
        }
    }

    @Override
    public void onLoadingStarted(int page) {
        if (page == mPager.getCurrentItem()) {
            mUploadMenu.setVisibility(View.GONE);
            uploadMenuShowing = false;
        }
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
     * Animates the upload button
     *
     * @param shouldShow If the button should be shown
     */
    private void animateUploadMenuButton(boolean shouldShow) {
        if (!shouldShow && uploadMenuShowing) {
            float hideDistance;
            uploadMenuShowing = false;
            // Add extra distance to the hiding of the button if on KitKat due to the translucent nav bar
            if (OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
                if (mNavBarHeight == -1) mNavBarHeight = ViewUtils.getNavigationBarHeight(getApplicationContext());
                hideDistance = mNavBarHeight + mUploadButtonHeight + (mUploadButtonHeight / 2);
            } else {
                hideDistance = mUploadButtonHeight;
            }

            mUploadMenu.animate().setInterpolator(new DecelerateInterpolator()).translationY(hideDistance).setDuration(350).start();
            // Close the menu if it is open
            if (uploadMenuOpen) animateUploadMenu();
        } else if (shouldShow && !uploadMenuShowing) {
            uploadMenuShowing = true;
            mUploadMenu.animate().setInterpolator(new DecelerateInterpolator()).translationY(0).setDuration(350).start();
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
                    ObjectAnimator.ofFloat(mLinkUpload, "alpha", 0.0f, 1.0f)
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
                    ObjectAnimator.ofFloat(mLinkUpload, "alpha", 1.0f, 0.0f)
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

            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    private static class TabPageAdapter extends FragmentStatePagerAdapter implements TabBarView.IconTabProvider {
        private final int[] mTabIcons = {R.drawable.ic_action_gallery, R.drawable.ic_action_reddit, R.drawable.ic_action_user};

        private String[] mTitles;

        public TabPageAdapter(FragmentManager fm, String[] titles) {
            super(fm);
            mTitles = titles;

        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GalleryFragment.createInstance();

                case 1:
                    return RedditFragment.createInstance();

                case 2:
                    return ProfileFragment.createInstance(null, true);

                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int getCount() {
            return mTabIcons.length;
        }

        @Override
        public int getPageIconResId(int position) {
            return mTabIcons[position];
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position >= mTitles.length) {
                throw new IndexOutOfBoundsException("TabPagerAdapter Index out of bounds. How did that happen?");
            }

            Locale l = Locale.getDefault();
            return mTitles[position].toUpperCase(l);
        }
    }
}
