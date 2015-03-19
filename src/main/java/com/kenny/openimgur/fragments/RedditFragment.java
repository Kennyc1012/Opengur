package com.kenny.openimgur.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.adapters.RedditSearchAdapter;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurFilters.RedditSort;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 8/14/14.
 */
public class RedditFragment extends BaseGridFragment implements RedditFilterFragment.FilterListener {

    private static final String KEY_QUERY = "query";

    private static final String KEY_SORT = "redditSort";

    private static final String KEY_TOP_SORT = "redditTopSort";

    private String mQuery;

    private RedditSort mSort;

    private ImgurFilters.TimeSort mTopSort;

    private RedditSearchAdapter mCursorAdapter;

    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;

    public static RedditFragment createInstance() {
        return new RedditFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reddit, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getAdapter() == null || getAdapter().isEmpty()) {
            mIsLoading = false;
            mMultiStateView.setViewState(MultiStateView.ViewState.EMPTY);
            if (mListener != null) mListener.onLoadingComplete();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.reddit, menu);
        mSearchMenuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setQueryHint(getString(R.string.enter_sub_reddit));

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                return search(text);
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        mCursorAdapter = new RedditSearchAdapter(getActivity(), app.getSql().getSubReddits());
        mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return app.getSql().getSubReddits(constraint);
            }
        });
        mSearchView.setSuggestionsAdapter(mCursorAdapter);
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                String title = mCursorAdapter.getTitle(position);
                return search(title);
            }

            @Override
            public boolean onSuggestionClick(int position) {
                String title = mCursorAdapter.getTitle(position);
                return search(title);
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    private boolean search(String query) {
        if (!mIsLoading && !TextUtils.isEmpty(query)) {
            if (getAdapter() != null) {
                getAdapter().clear();
            }

            mQuery = query;
            mCurrentPage = 0;
            mHasMore = true;
            mSearchView.setQuery(mQuery, false);
            mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);

            if (mListener != null) {
                mListener.onLoadingStarted();
                mListener.onUpdateActionBarTitle(mQuery);
            }

            fetchGallery();
            mSearchMenuItem.collapseActionView();
            return true;
        }

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter:
                if (mListener != null) mListener.onUpdateActionBar(false);

                RedditFilterFragment fragment = RedditFilterFragment.createInstance(mSort, mTopSort);
                fragment.setFilterListener(this);
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(android.R.id.content, fragment, "filter")
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        startActivity(ViewActivity.createIntent(getActivity(), items, position));
    }

    @Override
    public void onFilterChanged(RedditSort sort, ImgurFilters.TimeSort topSort) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commit();
        if (mListener != null) mListener.onUpdateActionBar(true);

        // Null values represent that the filter was canceled
        if (sort == null || topSort == null) {
            return;
        }

        if (sort == mSort && topSort == mTopSort) {
            // Don't fetch data if they haven't changed anything
            LogUtil.v(TAG, "Filters have not been updated");
            return;
        }

        mSort = sort;
        mTopSort = topSort;

        // Don't bother making an Api call if the there is no present query
        if (getAdapter() != null && !getAdapter().isEmpty() && !TextUtils.isEmpty(mQuery)) {
            getAdapter().clear();
            mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
            mCurrentPage = 0;
            mIsLoading = true;
            mHasMore = true;
            fetchGallery();
        }
    }

    /**
     * Returns the url for the Api request
     */
    private String getUrl() {
        // Strip out any white space before sending the query, all subreddits don't have spaces
        return String.format(Endpoints.SUBREDDIT.getUrl(), mQuery.replaceAll("\\s", ""), mSort.getSort(),
                mTopSort.getSort(), mCurrentPage);
    }

    private void setupAdapter(List<ImgurBaseObject> objects) {
        if (getAdapter() == null) {
            View header = ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0);
            mGrid.addHeaderView(header);
            setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(objects)));
        } else {
            getAdapter().addItems(objects);
        }
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_EMPTY_RESULT:
                    // Only show the empty view when the list is truly empty
                    if (getAdapter() == null || getAdapter().isEmpty()) {
                        mMultiStateView.setEmptyText(R.id.empty, getString(R.string.reddit_empty, mQuery));
                        mMultiStateView.setViewState(MultiStateView.ViewState.EMPTY);
                        if (mListener != null) mListener.onUpdateActionBar(true);
                    }

                    mIsLoading = false;
                    break;

                case MESSAGE_ACTION_COMPLETE:
                    if (mListener != null) {
                        mListener.onLoadingComplete();
                    }

                    app.getSql().addSubReddit(mQuery);
                    List<ImgurBaseObject> objects = (List<ImgurBaseObject>) msg.obj;
                    setupAdapter(objects);
                    mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);

                    if (mCurrentPage == 0) {
                        if (mCursorAdapter == null) {
                            mCursorAdapter = new RedditSearchAdapter(getActivity(), app.getSql().getSubReddits(mQuery));
                            mSearchView.setSuggestionsAdapter(mCursorAdapter);
                        } else {
                            mCursorAdapter.changeCursor(app.getSql().getSubReddits(mQuery));
                        }

                        mCursorAdapter.notifyDataSetChanged();
                        mGrid.post(new Runnable() {
                            @Override
                            public void run() {
                                mGrid.setSelection(0);
                            }
                        });
                    }

                    mIsLoading = false;
                    break;

                case MESSAGE_ACTION_FAILED:
                    if (getAdapter() == null || getAdapter().isEmpty()) {
                        if (mListener != null) {
                            mListener.onError((Integer) msg.obj);
                        }

                        mMultiStateView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                        mMultiStateView.setErrorButtonText(R.id.errorButton, R.string.retry);
                        mMultiStateView.setErrorButtonClickListener(R.id.errorButton, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
                                fetchGallery();
                            }
                        });

                        mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
                    }

                    mIsLoading = false;
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_QUERY, mQuery);
        outState.putString(KEY_TOP_SORT, mTopSort.getSort());
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);
        if (savedInstanceState == null) {
            mSort = RedditSort.getSortFromString(app.getPreferences().getString(KEY_SORT, RedditSort.TIME.getSort()));
            mTopSort = ImgurFilters.TimeSort.getSortFromString(app.getPreferences().getString(KEY_TOP_SORT, RedditSort.TIME.getSort()));
        } else {
            mQuery = savedInstanceState.getString(KEY_QUERY, null);
            mSort = RedditSort.getSortFromString(savedInstanceState.getString(KEY_SORT, RedditSort.TIME.getSort()));
            mTopSort = ImgurFilters.TimeSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, ImgurFilters.TimeSort.DAY.getSort()));
        }

        if (TextUtils.isEmpty(mQuery) && mListener != null) {
            mListener.onUpdateActionBarTitle(getString(R.string.subreddit));
        } else if (mListener != null) {
            mListener.onUpdateActionBarTitle(mQuery);
        }
    }

    @Override
    protected void saveFilterSettings() {
        app.getPreferences().edit()
                .putString(KEY_SORT, mSort.getSort())
                .putString(KEY_TOP_SORT, mTopSort.getSort())
                .apply();
    }

    @Override
    public void onDestroyView() {
        mSearchMenuItem = null;
        mSearchView = null;
        super.onDestroyView();
    }

    @Override
    public ImgurBusEvent.EventType getEventType() {
        return ImgurBusEvent.EventType.REDDIT_SEARCH;
    }

    @Override
    protected void fetchGallery() {
        if (!TextUtils.isEmpty(mQuery)) makeRequest(getUrl());
    }

    @Override
    protected ImgurHandler getHandler() {
        return mHandler;
    }
}
