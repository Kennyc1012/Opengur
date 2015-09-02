package com.kenny.openimgur.activities;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.fragments.FullScreenPhotoFragment;
import com.kenny.openimgur.services.DownloaderService;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarItem;
import com.kenny.snackbar.SnackBarListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

/**
 * Created by kcampagna on 6/2/15.
 */
public class FullScreenPhotoActivity extends BaseActivity {
    private static final String KEY_IMAGES = "images";

    private static final String KEY_URL = "url";

    private static final String KEY_IMAGE = "image";

    private static final String KEY_START_POSITION = "start_position";

    @Bind(R.id.pager)
    ViewPager mPager;

    private FullScreenPagerAdapter mAdapter;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_URL, url);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull ArrayList<ImgurPhoto> photos, int startingPosition) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.full_screen, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isAlbumDownloadable = mAdapter != null && mAdapter.getCount() > 1;
        menu.findItem(R.id.download_album).setVisible(isAlbumDownloadable);
        return super.onPrepareOptionsMenu(menu);
    }

    private void downloadAlbum() {
        if (NetworkUtils.isConnectedToWiFi(getApplicationContext())) {
            ArrayList<String> urls = new ArrayList<>(mAdapter.getCount());

            for (ImgurPhoto p : mAdapter.mPhotos) {
                urls.add(p.getLink());
            }

            startService(DownloaderService.createIntent(getApplicationContext(), urls));
        } else {
            new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                    .setTitle(R.string.download_no_wifi_title)
                    .setMessage(R.string.download_no_wifi_msg)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ArrayList<String> urls = new ArrayList<>(mAdapter.getCount());

                            for (ImgurPhoto p : mAdapter.mPhotos) {
                                urls.add(p.getLink());
                            }

                            startService(DownloaderService.createIntent(getApplicationContext(), urls));
                        }
                    }).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download_album:
                @PermissionUtils.PermissionLevel int permissionLevel = PermissionUtils.getPermissionType(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                switch (permissionLevel) {
                    case PermissionUtils.PERMISSION_AVAILABLE:
                        downloadAlbum();
                        break;

                    case PermissionUtils.PERMISSION_DENIED:
                        new SnackBarItem.Builder(this)
                                .setMessageResource(R.string.permission_rationale_download)
                                .setActionMessageResource(R.string.okay)
                                .setAutoDismiss(false)
                                .setSnackBarListener(new SnackBarListener() {
                                    @Override
                                    public void onSnackBarStarted(Object o) {
                                        LogUtil.v(TAG, "Permissions have been denied before, showing rationale");
                                    }

                                    @Override
                                    public void onSnackBarFinished(Object o, boolean actionClicked) {
                                        if (actionClicked) {
                                            ActivityCompat.requestPermissions(FullScreenPhotoActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSIONS);
                                        } else {
                                            Toast.makeText(getApplicationContext(), R.string.permission_denied, Toast.LENGTH_LONG).show();
                                            finish();
                                        }
                                    }
                                }).show();
                        break;

                    case PermissionUtils.PERMISSION_UNAVAILABLE:
                    default:
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSIONS);
                        break;
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleArguments(Bundle savedInstanceState, Intent intent) {
        ArrayList<ImgurPhoto> photos;
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
                photos.add((ImgurPhoto) intent.getParcelableExtra(KEY_IMAGE));
            } else {
                photos = new ArrayList<>(1);
                String url = intent.getStringExtra(KEY_URL);
                photos.add(new ImgurPhoto(url));
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

        supportInvalidateOptionsMenu();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSIONS:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    SnackBar.show(this, R.string.permission_granted);
                    downloadAlbum();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static class FullScreenPagerAdapter extends FragmentStatePagerAdapter {
        private List<ImgurPhoto> mPhotos;

        public FullScreenPagerAdapter(List<ImgurPhoto> photos, FragmentManager fm) {
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

        public ArrayList<ImgurPhoto> retainItems() {
            return new ArrayList<>(mPhotos);
        }
    }
}
