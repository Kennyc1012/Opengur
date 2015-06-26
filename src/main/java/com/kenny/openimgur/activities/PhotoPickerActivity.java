package com.kenny.openimgur.activities;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.PhotoPickerAdapter;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class PhotoPickerActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<List<String>>, View.OnClickListener {
    private static int LOADER_ID = PhotoPickerActivity.class.hashCode();

    @InjectView(R.id.grid)
    RecyclerView mGrid;

    @InjectView(R.id.multiView)
    MultiStateView mMultiStateView;

    private PhotoPickerAdapter mAdapter;

    public static Intent createInstance(Context context) {
        return new Intent(context, PhotoPickerActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
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
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        String path = mAdapter.getItem(mGrid.getLayoutManager().getPosition(v));
        mAdapter.onSelected(path, v);
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
            mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
        } else {
            mMultiStateView.setEmptyText(R.id.emptyMessage, getString(R.string.device_no_photos));
            mMultiStateView.setViewState(MultiStateView.ViewState.EMPTY);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<String>> loader) {
        if (mAdapter == null || mAdapter.isEmpty()) {
            if (mMultiStateView != null)
                mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
        }
    }

    @Override
    protected int getStyleRes() {
        return R.style.Theme_AppCompat;
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
                    photos.add(cursor.getString(0));
                }

                LogUtil.v(TAG, "Found " + photos.size() + " photos");
                cursor.close();
                return photos;
            }

            LogUtil.v(TAG, "Did not find any photos");
            return null;
        }
    }
}
