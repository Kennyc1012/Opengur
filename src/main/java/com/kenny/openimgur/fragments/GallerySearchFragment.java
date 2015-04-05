package com.kenny.openimgur.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.SearchAdapter;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 3/21/15.
 */
public class GallerySearchFragment extends GalleryFragment implements GallerySearchFilterFragment.FilterListener {
    private static final String KEY_QUERY = "query";

    private String mQuery;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                return setQuery(text);
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

                GallerySearchFilterFragment fragment = GallerySearchFilterFragment.createInstance(mSort, mTimeSort);
                fragment.setListener(this);
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(android.R.id.content, fragment, "filter")
                        .commit();
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
        // Format spaces to use HTML encoded values
        String formattedQuery = mQuery.replace(" ", "%20");

        if (mSort == ImgurFilters.GallerySort.HIGHEST_SCORING) {
            return String.format(Endpoints.GALLERY_SEARCH_TOP.getUrl(), mSort.getSort(), mTimeSort.getSort(), mCurrentPage, formattedQuery);
        }

        return String.format(Endpoints.GALLERY_SEARCH.getUrl(), mSort.getSort(), mCurrentPage, formattedQuery);
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

    public boolean setQuery(String query) {
        if (!TextUtils.isEmpty(query) && !query.equalsIgnoreCase(mQuery)) {
            LogUtil.v(TAG, "setQuery :" + query);
            mQuery = query;
            if (mListener != null) mListener.onUpdateActionBarTitle(mQuery);
            if (mSearchMenuItem != null) mSearchMenuItem.collapseActionView();
            // Refresh will reset all the needed data and call fetchGallery
            refresh();
            return true;
        }

        return false;
    }

    /**
     * Called when we receive no results from the API
     */
    public void onEmptyResults() {
        if (getAdapter() == null || getAdapter().isEmpty()) {
            mMultiStateView.setErrorText(R.id.errorMessage, getString(R.string.reddit_empty, mQuery));
            mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
            if (mListener != null) mListener.onUpdateActionBar(true);
        }
    }

    /**
     * Called when a successful search has been completed by the API
     */
    public void onSuccessfulSearch(){
        app.getSql().addPreviousGallerySearch(mQuery);

        if (mSearchAdapter == null) {
            mSearchAdapter = new SearchAdapter(getActivity(), app.getSql().getSubReddits(mQuery), DBContracts.GallerySearchContract.COLUMN_NAME);
            mSearchView.setSuggestionsAdapter(mSearchAdapter);
        } else {
            mSearchAdapter.changeCursor(app.getSql().getSubReddits(mQuery));
        }

        mSearchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onFilterChange(ImgurFilters.GallerySort sort, ImgurFilters.TimeSort timeSort) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commit();
        if (mListener != null) mListener.onUpdateActionBar(true);

        // Null values represent that the filter was canceled
        if (sort == null || timeSort == null || (mSort == sort && timeSort == mTimeSort)) {
            return;
        }

        if (getAdapter() != null) {
            getAdapter().clear();
        }

        mSort = sort;
        mTimeSort = timeSort;
        mCurrentPage = 0;
        mIsLoading = true;
        mHasMore = true;
        mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
        if (mListener != null) mListener.onLoadingStarted();
        fetchGallery();
    }
}
