package com.kenny.openimgur.activities;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.PhotoPickerAdapter;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class PhotoPickerActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<String>>, View.OnClickListener {
    private static final int LOADER_ID = PhotoPickerActivity.class.hashCode();

    private static final String KEY_SAVED_PHOTOS = "saved_photos";

    private static final String KEY_SAVED_CHECKED_PHOTOS = "saved_checked_photos";

    public static final String KEY_PHOTOS = PhotoPickerActivity.class.getSimpleName() + ".photos";

    @Bind(R.id.grid)
    RecyclerView mGrid;

    @Bind(R.id.multiView)
    MultiStateView mMultiStateView;

    private PhotoPickerAdapter mAdapter;

    public static Intent createInstance(Context context) {
        return new Intent(context, PhotoPickerActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
        ActionBar ab = getSupportActionBar();
        ab.setTitle(R.string.photo_picker_title);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SAVED_PHOTOS)) {
            List<String> photos = savedInstanceState.getStringArrayList(KEY_SAVED_PHOTOS);
            List<String> checkedPhotos = savedInstanceState.getStringArrayList(KEY_SAVED_CHECKED_PHOTOS);
            mAdapter = new PhotoPickerAdapter(getApplicationContext(), mGrid, photos, this);

            if (checkedPhotos != null) {
                mAdapter.setCheckedPhotos(checkedPhotos);
                ab.setTitle(getString(R.string.photos_selected, checkedPhotos.size()));
            }

            mGrid.setAdapter(mAdapter);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) {
            if (getLoaderManager().getLoader(LOADER_ID) != null) {
                getLoaderManager().getLoader(LOADER_ID).reset();
            } else {
                getLoaderManager().initLoader(LOADER_ID, null, this).forceLoad();
            }
        }
    }

    @Override
    protected void onDestroy() {
        getLoaderManager().destroyLoader(LOADER_ID);
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        String path = mAdapter.getItem(mGrid.getLayoutManager().getPosition(v));
        int selected = mAdapter.onSelected(path, v);
        ActionBar ab = getSupportActionBar();

        if (selected > 0) {
            ab.setTitle(getString(R.string.photos_selected, selected));
        } else {
            ab.setTitle(R.string.photo_picker_title);
        }

        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.photo_picker, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.done);
        boolean hasSelection = mAdapter != null && mAdapter.getSelectedCount() > 0;
        item.setVisible(hasSelection);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                if (mAdapter != null) {
                    ArrayList<String> photos = mAdapter.getCheckedPhotos();

                    if (photos != null && !photos.isEmpty()) {
                        Intent intent = new Intent();
                        intent.putExtra(KEY_PHOTOS, photos);
                        setResult(Activity.RESULT_OK, intent);
                    } else {
                        setResult(Activity.RESULT_CANCELED);
                    }
                }

                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<List<String>> onCreateLoader(int id, Bundle args) {
        return new FetchImagesTask(this);
    }

    @Override
    public void onLoadFinished(Loader<List<String>> loader, List<String> data) {
        if (data != null && !data.isEmpty()) {
            mAdapter = new PhotoPickerAdapter(this, mGrid, data, this);
            mGrid.setAdapter(mAdapter);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        } else {
            ViewUtils.setEmptyText(mMultiStateView, R.id.emptyMessage, getString(R.string.device_no_photos));
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
        if (mAdapter == null || mAdapter.isEmpty()) {
            if (mMultiStateView != null)
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null) {
            outState.putStringArrayList(KEY_SAVED_PHOTOS, mAdapter.retainItems());
            outState.putStringArrayList(KEY_SAVED_CHECKED_PHOTOS, mAdapter.getCheckedPhotos());
        }
    }

    private static class FetchImagesTask extends AsyncTaskLoader<List<String>> {
        private static final String TAG = "FetchImagesTask";

        public FetchImagesTask(Context context) {
            super(context);
        }

        @Override
        public List<String> loadInBackground() {
            String orderBy = MediaStore.Images.Media.DATE_TAKEN;
            String[] projection = {MediaStore.Images.Media.DATA};
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = getContext().getContentResolver().query(uri, projection, null, null, orderBy);

            if (cursor != null && cursor.getCount() > 0) {
                List<String> photos = new ArrayList<>();

                while (cursor.moveToNext()) {
                    photos.add("file://" + cursor.getString(0));
                }

                LogUtil.v(TAG, "Found " + photos.size() + " photos");
                cursor.close();
                // Reverse the list so newest are first
                Collections.reverse(photos);
                return photos;
            }

            LogUtil.v(TAG, "Did not find any photos");
            return null;
        }
    }
}
