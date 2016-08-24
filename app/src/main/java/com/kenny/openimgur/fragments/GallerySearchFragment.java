package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.adapters.SearchAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurService;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

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
                boolean setQuery = setQuery(text);
                if (setQuery) refresh();
                return setQuery;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    @Override
    protected void fetchGallery() {
        if (TextUtils.isEmpty(mQuery)) return;

        ImgurService apiService = ApiClient.getService();
        mIsLoading = true;

        if (mSort == ImgurFilters.GallerySort.HIGHEST_SCORING) {
            apiService.searchGalleryForTopSorted(mTimeSort.getSort(), mCurrentPage, mQuery).enqueue(this);
        } else {
            apiService.searchGallery(mSort.getSort(), mCurrentPage, mQuery).enqueue(this);
        }
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
            return true;
        }

        return false;
    }

    @Override
    protected void onFilterChange(@NonNull ImgurFilters.GallerySection section, @NonNull ImgurFilters.GallerySort sort, @NonNull ImgurFilters.TimeSort timeSort, boolean showViral) {
        // Don't care about section or showViral
        if (mSort == sort && timeSort == mTimeSort) {
            // Null values represent that the filter was canceled
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
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        if (mListener != null) mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
        fetchGallery();
    }

    @Override
    protected void onApiResult(@NonNull GalleryResponse galleryResponse) {
        super.onApiResult(galleryResponse);
        if (mSearchView != null && mCurrentPage == 0 && !galleryResponse.data.isEmpty()) {
            SqlHelper sql = SqlHelper.getInstance(getActivity());
            sql.addPreviousGallerySearch(mQuery);

            if (mSearchAdapter == null) {
                mSearchAdapter = new SearchAdapter(getActivity(), sql.getPreviousGallerySearches(mQuery), DBContracts.GallerySearchContract.COLUMN_NAME);
                mSearchView.setSuggestionsAdapter(mSearchAdapter);
            } else {
                mSearchAdapter.changeCursor(sql.getPreviousGallerySearches(mQuery));
            }

            mSearchAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onEmptyResults() {
        mHasMore = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, getString(R.string.reddit_empty, mQuery));
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }
    }

    @Override
    protected int getFilterMenu() {
        return R.menu.filter_gallery_search;
    }

    @Override
    protected PopupMenu.OnMenuItemClickListener getMenuItemClickListener() {
        return new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.newest:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.TIME, ImgurFilters.TimeSort.DAY, false);
                        return true;

                    case R.id.popularity:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.VIRAL, ImgurFilters.TimeSort.DAY, false);
                        return true;

                    case R.id.scoringDay:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.DAY, false);
                        return true;

                    case R.id.scoringWeek:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.WEEK, false);
                        return true;

                    case R.id.scoringMonth:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.MONTH, false);
                        return true;

                    case R.id.scoringYear:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.YEAR, false);
                        return true;

                    case R.id.scoringAll:
                        onFilterChange(ImgurFilters.GallerySection.USER, ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.ALL, false);
                        return true;
                }

                return false;
            }
        };
    }
}
