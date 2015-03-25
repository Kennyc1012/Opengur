package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 3/21/15.
 */
public class GallerySearchFragment extends GalleryFragment {
    private static final String KEY_QUERY = "query";

    private String mQuery;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                setQuery(text);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter:
                if (mListener != null) mListener.onUpdateActionBar(false);
                // TODO Show filter
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public ImgurBusEvent.EventType getEventType() {
        return ImgurBusEvent.EventType.GALLERY_SEARCH;
    }

    @Override
    protected String getGalleryUrl() {
        if (mSort == ImgurFilters.GallerySort.HIGHEST_SCORING) {
            return String.format(Endpoints.GALLERY_SEARCH_TOP.getUrl(), mSort.getSort(), mTimeSort.getSort(), mCurrentPage, mQuery);
        }

        return String.format(Endpoints.GALLERY_SEARCH.getUrl(), mSort.getSort(), mCurrentPage, mQuery);
    }

    @Override
    protected void fetchGallery() {
        if (TextUtils.isEmpty(mQuery)) return;
        super.fetchGallery();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_QUERY, mQuery);
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);
        if (savedInstanceState != null) mQuery = savedInstanceState.getString(KEY_QUERY, null);
    }

    public void setQuery(String query) {
        if (!TextUtils.isEmpty(query) && !query.equalsIgnoreCase(mQuery)) {
            LogUtil.v(TAG, "setQuery :" + query);
            mQuery = query;
            if (mListener != null) mListener.onUpdateActionBarTitle(mQuery);
            if (getAdapter() != null) getAdapter().clear();
            if (mSearchMenuItem != null) mSearchMenuItem.collapseActionView();
            mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
            fetchGallery();
        }
    }
}
