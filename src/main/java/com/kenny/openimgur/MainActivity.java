package com.kenny.openimgur;

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
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.mirko.tbv.TabBarView;

import java.util.Locale;

/**
 * Created by kcampagna on 8/14/14.
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, TabActivityListener {

    private TabBarView mTabBar;

    private ViewPager mPager;

    private View mUploadMenu;

    private FloatingActionButton mUploadButton;

    private FloatingActionButton mCameraUpload;

    private FloatingActionButton mGalleryUpload;

    private FloatingActionButton mLinkUpload;

    private boolean uploadMenuOpen = false;

    private boolean uploadMenuShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageLimit(0);
        mPager.setAdapter(new TabPageAdapter(getFragmentManager()));
        mTabBar = (TabBarView) View.inflate(getApplicationContext(), R.layout.tab_ab_layout, null);
        mTabBar.setViewPager(mPager);
        mUploadMenu = findViewById(R.id.uploadMenu);
        mUploadButton = (FloatingActionButton) mUploadMenu.findViewById(R.id.uploadButton);
        mLinkUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.linkUpload);
        mCameraUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.cameraUpload);
        mGalleryUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.galleryUpload);
        mUploadButton.setOnClickListener(this);
        mLinkUpload.setOnClickListener(this);
        mCameraUpload.setOnClickListener(this);
        mGalleryUpload.setOnClickListener(this);
        setActionBarView(mTabBar);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void oHideActionBar(boolean shouldShow) {
        setActionBarVisibility(shouldShow);
        animateUploadMenuButton(shouldShow);
    }

    @Override
    public void onError(int errorCode) {
        mUploadMenu.setVisibility(View.GONE);
        uploadMenuShowing = false;
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.uploadButton:
                animateUploadMenu();
                break;

            case R.id.cameraUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_CAMERA));
                break;

            case R.id.galleryUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_GALLERY));
                break;

            case R.id.linkUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_LINK));
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
            uploadMenuShowing = false;
            // Add extra distance to the hiding of the button if on KitKat due to the translucent nav bar
            float hideDistance = OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT ? mUploadButton.getHeight() * 2 : mUploadButton.getHeight();
            mUploadMenu.animate().setInterpolator(new DecelerateInterpolator()).translationY(hideDistance).setDuration(350).start();
        } else if (shouldShow && !uploadMenuShowing) {
            uploadMenuShowing = true;
            mUploadMenu.animate().setInterpolator(new DecelerateInterpolator()).translationY(0).setDuration(350).start();
        }
    }

    /**
     * Animates the opening/closing of the Upload button
     */
    private void animateUploadMenu() {
        int uploadButtonHeight = mUploadButton.getHeight();
        int menuButtonHeight = mCameraUpload.getHeight();
        AnimatorSet set = new AnimatorSet().setDuration(500L);
        String translation = isLandscape() ? "translationX" : "translationY";

        if (!uploadMenuOpen) {
            uploadMenuOpen = true;

            set.playTogether(
                    ObjectAnimator.ofFloat(mCameraUpload, translation, 0, (uploadButtonHeight + 25) * -1),
                    ObjectAnimator.ofFloat(mLinkUpload, translation, 0, ((2 * menuButtonHeight) + uploadButtonHeight + 75) * -1),
                    ObjectAnimator.ofFloat(mGalleryUpload, translation, (menuButtonHeight + uploadButtonHeight + 50) * -1)
            );

            set.setInterpolator(new OvershootInterpolator());
            set.start();
        } else {
            uploadMenuOpen = false;

            set.playTogether(
                    ObjectAnimator.ofFloat(mCameraUpload, translation, 0),
                    ObjectAnimator.ofFloat(mLinkUpload, translation, 0),
                    ObjectAnimator.ofFloat(mGalleryUpload, translation, 0)
            );

            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    private class TabPageAdapter extends FragmentStatePagerAdapter implements TabBarView.IconTabProvider {

        private final int[] mTabIcons = {R.drawable.ic_action_gallery, R.drawable.ic_action_reddit, R.drawable.ic_action_user};

        public TabPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GalleryFragment.createInstance();

                case 1:
                    return RedditFragment.createInstance();

                case 2:
                    return RedditFragment.createInstance();

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
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.gallery).toUpperCase(l);
                case 1:
                    return getString(R.string.sub_reddit).toUpperCase(l);
                case 2:
                    return getString(R.string.profile);
                default:
                    throw new IndexOutOfBoundsException();
            }
        }
    }
}
