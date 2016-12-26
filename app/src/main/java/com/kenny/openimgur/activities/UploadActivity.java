package com.kenny.openimgur.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.UploadListener;
import com.kenny.openimgur.fragments.UploadFragment;
import com.kenny.openimgur.fragments.UploadInfoFragment;
import com.kenny.openimgur.services.UploadService;
import com.kenny.openimgur.ui.FragmentPagerAdapter;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.util.LogUtil;
import com.pixelcan.inkpageindicator.InkPageIndicator;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class UploadActivity extends BaseActivity implements UploadListener, ViewPager.OnPageChangeListener {
    private static final int PAGE_PHOTOS = 0;

    private static final int PAGE_INFO = 1;

    private static final String KEY_PASSED_FILE = "passed_file";

    private static final String PREF_NOTIFY_NO_USER = "notify_no_user";

    @BindView(R.id.pager)
    ViewPager mPager;

    @BindView(R.id.indicator)
    InkPageIndicator mIndicator;

    @BindView(R.id.indicatorContainer)
    View mIndicatorContainer;

    @BindView(R.id.back)
    Button mBackButton;

    @BindView(R.id.next)
    Button mNextButton;

    private UploadPagerAdapter mAdapter;

    public static Intent createIntent(Context context) {
        return new Intent(context, UploadActivity.class);
    }

    public static Intent createIntent(Context context, @NonNull File file) {
        return createIntent(context).putExtra(KEY_PASSED_FILE, file.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        getSupportActionBar().setTitle(R.string.upload);
        checkForNag();
        checkIntent(getIntent());
        mIndicator.setViewPager(mPager);
        mPager.addOnPageChangeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                UploadFragment fragment = (UploadFragment) mAdapter.getFragmentForPosition(mPager, getFragmentManager(), 0);

                if (fragment != null && fragment.hasPhotosForUpload()) {
                    showCancelDialog();
                    return true;
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String path = null;
            boolean isLink = false;
            ArrayList<Uri> photoUris = null;

            if (intent.hasExtra(KEY_PASSED_FILE)) {
                LogUtil.v(TAG, "Received file from intent");
                path = intent.getStringExtra(KEY_PASSED_FILE);
            } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
                String type = intent.getType();
                LogUtil.v(TAG, "Received an image via Share intent, type " + type);

                if ("text/plain".equals(type)) {
                    path = intent.getStringExtra(Intent.EXTRA_TEXT);
                    isLink = true;
                } else {
                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    photoUris = new ArrayList<>(1);
                    photoUris.add(uri);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                photoUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                if (photoUris != null && !photoUris.isEmpty()) {
                    LogUtil.v(TAG, "Received " + photoUris.size() + " images via Share intent");
                }
            }

            mPager.setAdapter(mAdapter = new UploadPagerAdapter(getFragmentManager(), path, isLink, photoUris));
        } else {
            mPager.setAdapter(mAdapter = new UploadPagerAdapter(getFragmentManager()));
        }
    }

    private boolean showCancelDialog() {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.upload_cancel_title)
                .setMessage(R.string.upload_cancel_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();

        return true;
    }

    /**
     * Checks if the user is not logged in and if we should nag about it
     */
    private boolean checkForNag() {
        boolean nag = app.getPreferences().getBoolean(PREF_NOTIFY_NO_USER, true);

        if (nag && user == null) {
            View nagView = LayoutInflater.from(this).inflate(R.layout.no_user_nag, null);
            final CheckBox cb = (CheckBox) nagView.findViewById(R.id.dontNotify);

            new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                    .setTitle(R.string.not_logged_in)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (cb.isChecked()) {
                                app.getPreferences().edit().putBoolean(PREF_NOTIFY_NO_USER, false).apply();
                            }
                        }
                    })
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(ProfileActivity.createIntent(getApplicationContext(), null, true));
                        }
                    })
                    .setView(nagView)
                    .show();

            return true;
        }

        return false;
    }

    @Override
    public void onPhotoAdded() {
        mPager.setSwiping(true);

        if (mIndicatorContainer.getVisibility() == View.GONE) {
            mIndicatorContainer.setVisibility(View.VISIBLE);
            mBackButton.setEnabled(false);
            mBackButton.setAlpha(0);
            mNextButton.setEnabled(true);
            mNextButton.setAlpha(1);
        }
    }

    @Override
    public void onPhotoRemoved(int remaining) {
        if (remaining <= 0) {
            mPager.setCurrentItem(0, true);
            mPager.setSwiping(false);
            mIndicatorContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onUpload(boolean submitToGallery, String title, String description, ImgurTopic topic) {
        UploadFragment fragment = (UploadFragment) mAdapter.getFragmentForPosition(mPager, getFragmentManager(), 0);

        if (fragment != null && fragment.hasPhotosForUpload()) {
            Intent service = UploadService.createIntent(getApplicationContext(), fragment.getPhotosForUpload(), submitToGallery, title, description, topic);
            startService(service);
            finish();
        }
    }

    @OnClick({R.id.back, R.id.next})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                mPager.setCurrentItem(PAGE_PHOTOS);
                break;

            case R.id.next:
                mPager.setCurrentItem(PAGE_INFO);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        UploadFragment fragment = (UploadFragment) mAdapter.getFragmentForPosition(mPager, getFragmentManager(), 0);

        if (fragment != null && fragment.hasPhotosForUpload()) {
            showCancelDialog();
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mNextButton.setAlpha(position == PAGE_PHOTOS ? 1.0f - positionOffset : 0.0f);
        mBackButton.setAlpha(position == PAGE_PHOTOS ? positionOffset : 1.0f);
    }

    @Override
    public void onPageSelected(int position) {
        mBackButton.setEnabled(position == PAGE_INFO);
        mNextButton.setEnabled(position == PAGE_PHOTOS);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // NOOP
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Upload : R.style.Theme_Opengur_Light_DarkActionBar_Upload;
    }

    private static class UploadPagerAdapter extends FragmentPagerAdapter {
        Bundle mUploadArgs = null;

        public UploadPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        public UploadPagerAdapter(FragmentManager manager, String path, boolean isLink, ArrayList<Uri> photoUris) {
            this(manager);
            mUploadArgs = UploadFragment.createArguments(path, isLink, photoUris);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case PAGE_PHOTOS:
                    return UploadFragment.newInstance(mUploadArgs);

                case PAGE_INFO:
                    return UploadInfoFragment.newInstance();

                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
