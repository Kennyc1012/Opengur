package com.kenny.openimgur.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto2;
import com.kenny.openimgur.fragments.FullScreenPhotoFragment;
import com.kenny.openimgur.ui.ViewPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

/**
 * Created by kcampagna on 6/2/15.
 */
public class FullScreenPhotoActivity extends BaseActivity {
    private static final String KEY_IMAGES = "images";

    private static final String KEY_URL = "url";

    private static final String KEY_IMAGE = "image";

    private static final String KEY_START_POSITION = "start_position";

    @InjectView(R.id.pager)
    ViewPager mPager;

    private FullScreenPagerAdapter mAdapter;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto2 photo) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_URL, url);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull ArrayList<ImgurPhoto2> photos, int startingPosition) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_IMAGES, photos).putExtra(KEY_START_POSITION, startingPosition);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if ((intent == null && savedInstanceState == null) || (!intent.hasExtra(KEY_IMAGE) && !intent.hasExtra(KEY_URL) && !intent.hasExtra(KEY_IMAGES))) {
            Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_full_screen);
        setStatusBarColor(Color.BLACK);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        handleArguments(savedInstanceState, intent);
    }

    private void handleArguments(Bundle savedInstanceState, Intent intent) {
        ArrayList<ImgurPhoto2> photos;
        int startingPosition;

        if (savedInstanceState != null) {
            // The adapter should have been set and returned a list when saving state
            photos = savedInstanceState.getParcelableArrayList(KEY_IMAGES);
            startingPosition = savedInstanceState.getInt(KEY_START_POSITION);
        } else {
            if (intent.hasExtra(KEY_IMAGES)) {
                photos = intent.getParcelableArrayListExtra(KEY_IMAGES);
            } else if (intent.hasExtra(KEY_IMAGE)) {
                photos = new ArrayList<>(1);
                photos.add((ImgurPhoto2) intent.getParcelableExtra(KEY_IMAGE));
            } else {
                photos = new ArrayList<>(1);
                String url = intent.getStringExtra(KEY_URL);
                photos.add(new ImgurPhoto2(url));
            }

            startingPosition = intent.getIntExtra(KEY_START_POSITION, 0);
        }

        mAdapter = new FullScreenPagerAdapter(photos, getFragmentManager());
        mPager.setAdapter(mAdapter);
        mPager.setCurrentItem(startingPosition);

        if (mAdapter.getCount() > 1) {
            getSupportActionBar().setTitle(mPager.getCurrentItem() + 1 + "/" + mAdapter.getCount());

            mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                    // NOOP
                }

                @Override
                public void onPageSelected(int position) {
                    getSupportActionBar().setTitle(mPager.getCurrentItem() + 1 + "/" + mAdapter.getCount());
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    // NOOP
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_START_POSITION, mPager.getCurrentItem());
        if (mAdapter != null) outState.putParcelableArrayList(KEY_IMAGES, mAdapter.retainItems());
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
    }

    private static class FullScreenPagerAdapter extends FragmentStatePagerAdapter {
        private List<ImgurPhoto2> mPhotos;

        public FullScreenPagerAdapter(List<ImgurPhoto2> photos, FragmentManager fm) {
            super(fm);
            mPhotos = photos;
        }

        @Override
        public Fragment getItem(int position) {
            return FullScreenPhotoFragment.createInstance(mPhotos.get(position));
        }

        @Override
        public int getCount() {
            return mPhotos != null ? mPhotos.size() : 0;
        }

        public ArrayList<ImgurPhoto2> retainItems() {
            return new ArrayList<>(mPhotos);
        }
    }
}
