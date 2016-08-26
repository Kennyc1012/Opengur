package com.kenny.openimgur.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.collections.SetUniqueList;
import com.kenny.openimgur.fragments.FullScreenPhotoFragment;
import com.kenny.openimgur.services.DownloaderService;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 6/2/15.
 */
public class FullScreenPhotoActivity extends BaseActivity implements View.OnClickListener {
    private static final String KEY_IMAGES = "images";

    private static final String KEY_URL = "url";

    private static final String KEY_IMAGE = "image";

    private static final String KEY_OBJECT = "object";

    private static final String KEY_START_POSITION = "start_position";

    public static final String KEY_ENDING_POSITION = "ending_position";

    @BindView(R.id.pager)
    ViewPager mPager;

    @BindView(R.id.grid)
    RecyclerView mGrid;

    View mDecorView;

    FullScreenPagerAdapter mAdapter;

    @Nullable
    VisibilityHandler mHandler;

    @Nullable
    BottomSheetBehavior mBottomSheetBehavior;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_URL, url);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull ArrayList<ImgurPhoto> photos, @NonNull ImgurBaseObject obj, int startingPosition) {
        // Passing too many items in the intent might cause a crash
        if (photos.size() > GalleryAdapter.MAX_ITEMS) {
            return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_OBJECT, obj).putExtra(KEY_START_POSITION, startingPosition);
        } else {
            return new Intent(context, FullScreenPhotoActivity.class).putExtra(KEY_IMAGES, photos).putExtra(KEY_START_POSITION, startingPosition);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent == null && savedInstanceState == null) {
            Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_full_screen);
        handleArguments(savedInstanceState, intent);
        setStatusBarColor(Color.BLACK);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (hasImmersiveMode()) {
            mDecorView = getWindow().getDecorView();
            mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int flags) {
                    boolean isVisible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

                    if (isVisible && mHandler != null) {
                        mHandler.removeMessages(0);
                        Message msg = mHandler.obtainMessage(0, mDecorView);
                        mHandler.sendMessageDelayed(msg, VisibilityHandler.HIDE_DELAY);
                    }
                }
            });
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (hasImmersiveMode()) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            mHandler = new VisibilityHandler();
            Message msg = mHandler.obtainMessage(0, mDecorView);
            mHandler.sendMessageDelayed(msg, VisibilityHandler.HIDE_DELAY);
        }
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
        menu.findItem(R.id.gallery_view).setVisible(isAlbumDownloadable);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) mHandler.removeMessages(0);
        if (mGrid != null && mGrid.getAdapter() instanceof GalleryAdapter) ((GalleryAdapter) mGrid.getAdapter()).onDestroy();
        super.onDestroy();
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
                @PermissionUtils.PermissionLevel int permissionLevel = PermissionUtils.getPermissionLevel(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                switch (permissionLevel) {
                    case PermissionUtils.PERMISSION_AVAILABLE:
                        downloadAlbum();
                        break;

                    case PermissionUtils.PERMISSION_DENIED:
                        Snackbar.make(mPager, R.string.permission_rationale_download, Snackbar.LENGTH_LONG)
                                .setAction(R.string.okay, new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        ActivityCompat.requestPermissions(FullScreenPhotoActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                                    }
                                })
                                .show();
                        break;

                    case PermissionUtils.PERMISSION_NEVER_ASKED:
                    default:
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                        break;
                }

                return true;

            case R.id.gallery_view:
                if (mBottomSheetBehavior != null) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
                return true;

            case android.R.id.home:
                int position = mPager != null ? mPager.getCurrentItem() : -1;
                if (position >= 0) setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_ENDING_POSITION, position));
                // intentional break
                break;
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
            } else if (intent.hasExtra(KEY_OBJECT)) {
                loadObject((ImgurBaseObject) intent.getParcelableExtra(KEY_OBJECT), intent.getIntExtra(KEY_START_POSITION, 0));
                return;
            } else {
                photos = new ArrayList<>(1);
                String url = intent.getStringExtra(KEY_URL);
                photos.add(new ImgurPhoto(url));
            }

            startingPosition = intent.getIntExtra(KEY_START_POSITION, 0);
        }

        setupPager(photos, startingPosition);
    }

    private boolean hasImmersiveMode() {
        return isApiLevel(Build.VERSION_CODES.KITKAT) && app.getPreferences().getBoolean(SettingsActivity.KEY_IMMERSIVE_MODE, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_START_POSITION, mPager.getCurrentItem());
        if (mAdapter != null) outState.putParcelableArrayList(KEY_IMAGES, mAdapter.retainItems());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSION_WRITE:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    downloadAlbum();
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (mBottomSheetBehavior != null && (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED || mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED)) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }

        int position = mPager != null ? mPager.getCurrentItem() : -1;
        if (position >= 0) setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_ENDING_POSITION, position));
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (mBottomSheetBehavior == null) return;

        int adapterPosition = mGrid.getChildAdapterPosition(v);

        if (adapterPosition != RecyclerView.NO_POSITION) {
            mPager.setCurrentItem(adapterPosition);
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }
    }

    private void loadObject(@Nullable ImgurBaseObject obj, final int startingPosition) {
        if (obj == null) {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Should be the case
        if (obj instanceof ImgurAlbum) {
            ApiClient.getService().getAlbumImages(obj.getId()).enqueue(new Callback<AlbumResponse>() {
                @Override
                public void onResponse(Call<AlbumResponse> call, Response<AlbumResponse> response) {
                    if (response != null && response.body() != null && response.body().hasData()) {
                        setupPager(response.body().data, startingPosition);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<AlbumResponse> call, Throwable t) {
                    Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        } else {
            Toast.makeText(this, R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    void setupPager(@Nullable List<ImgurPhoto> photos, int startingPosition) {
        if (photos == null) {
            Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
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

        if (photos.size() > 1) {
            mBottomSheetBehavior = BottomSheetBehavior.from(mGrid);
            ViewUtils.setRecyclerViewGridDefaults(this, mGrid);
            List<ImgurBaseObject> adapterList = new ArrayList<>(photos.size());
            adapterList.addAll(photos);
            mGrid.setAdapter(new GalleryAdapter(this, SetUniqueList.decorate(adapterList), this, false));
            mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    ActionBar ab = getSupportActionBar();

                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        ab.hide();
                    } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        ab.show();
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {

                }
            });
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        }

        supportInvalidateOptionsMenu();
    }

    private static class FullScreenPagerAdapter extends FragmentStatePagerAdapter {
        List<ImgurPhoto> mPhotos;

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

    static class VisibilityHandler extends Handler {
        public static final long HIDE_DELAY = DateUtils.SECOND_IN_MILLIS * 3;

        @Override
        public void handleMessage(Message msg) {
            if (msg.obj instanceof View) {
                ((View) msg.obj).setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);
            }

            super.handleMessage(msg);
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_View_Dark : R.style.Theme_Opengur_Light_View_Light;
    }
}
