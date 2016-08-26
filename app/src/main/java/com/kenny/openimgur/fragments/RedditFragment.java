package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FilterQueryProvider;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurService;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurFilters.RedditSort;
import com.kenny.openimgur.ui.adapters.SearchAdapter;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.SqlHelper;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 8/14/14.
 */
public class RedditFragment extends BaseGridFragment {
    public static final String KEY_PINNED_SUBREDDITS = "pinnedSubreddits";

    private static final String KEY_QUERY = "query";

    private static final String KEY_SORT = "redditSort";

    private static final String KEY_TOP_SORT = "redditTopSort";

    @BindView(R.id.mySubreddits)
    Button mMySubredditsBtn;

    String mQuery;

    private RedditSort mSort;

    private ImgurFilters.TimeSort mTopSort;

    SearchAdapter mCursorAdapter;

    private MenuItem mSearchMenuItem;

    private SearchView mSearchView;

    private final Set<String> mPinnedSubs = new HashSet<>();

    public static RedditFragment newInstance() {
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
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
            if (mListener != null)
                mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_COMPLETE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.reddit, menu);
        mSearchMenuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);

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

        if (getActivity() != null) {
            mSearchView.setQueryHint(getString(R.string.enter_sub_reddit));
            mCursorAdapter = new SearchAdapter(getActivity(), SqlHelper.getInstance(getActivity()).getSubReddits(), DBContracts.SubRedditContract.COLUMN_NAME);
            mCursorAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                @Override
                public Cursor runQuery(CharSequence constraint) {
                    return SqlHelper.getInstance(getActivity()).getSubReddits(constraint.toString());
                }
            });
            mSearchView.setSuggestionsAdapter(mCursorAdapter);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean hasPinned = !mPinnedSubs.isEmpty();
        menu.findItem(R.id.mySubreddits).setVisible(hasPinned);

        if (mMySubredditsBtn != null) {
            mMySubredditsBtn.setVisibility(hasPinned ? View.VISIBLE : View.GONE);
        }

        MenuItem action = menu.findItem(R.id.mySubredditAction);

        if (TextUtils.isEmpty(mQuery)) {
            action.setVisible(false);
        } else {
            action.setVisible(true);
            boolean isPinned = mPinnedSubs.contains(mQuery);
            action.setTitle(isPinned ? R.string.my_subreddits_remove : R.string.my_subreddits_add);
            action.setIcon(isPinned ? R.drawable.ic_remove_circle_outline_24dp : R.drawable.ic_add_circle_outline_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter:
                Activity activity = getActivity();
                View anchor;
                anchor = activity.findViewById(R.id.filter);
                if (anchor == null) anchor = activity.findViewById(R.id.refresh);
                if (anchor == null) anchor = activity.findViewById(R.id.search);

                PopupMenu m = new PopupMenu(getActivity(), anchor);
                m.inflate(R.menu.filter_reddit);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.newest:
                                onFilterChanged(RedditSort.TIME, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.topDay:
                                onFilterChanged(RedditSort.TOP, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.topWeek:
                                onFilterChanged(RedditSort.TOP, ImgurFilters.TimeSort.WEEK);
                                return true;

                            case R.id.topMonth:
                                onFilterChanged(RedditSort.TOP, ImgurFilters.TimeSort.MONTH);
                                return true;

                            case R.id.topYear:
                                onFilterChanged(RedditSort.TOP, ImgurFilters.TimeSort.YEAR);
                                return true;

                            case R.id.topAll:
                                onFilterChanged(RedditSort.TOP, ImgurFilters.TimeSort.ALL);
                                return true;
                        }

                        return false;
                    }
                });

                m.show();
                return true;

            case R.id.refresh:
                if (!TextUtils.isEmpty(mQuery)) refresh();
                return true;

            case R.id.mySubreddits:
                viewMySubreddits();
                return true;

            case R.id.mySubredditAction:
                int messageId;

                if (mPinnedSubs.contains(mQuery)) {
                    mPinnedSubs.remove(mQuery);
                    messageId = R.string.subreddit_removed;
                } else {
                    mPinnedSubs.add(mQuery);
                    messageId = R.string.subreddit_added;
                }

                getActivity().invalidateOptionsMenu();
                Snackbar.make(getSnackbarView(), messageId, Snackbar.LENGTH_LONG).show();
                saveSubreddits();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    boolean search(String query) {
        if (!mIsLoading && !TextUtils.isEmpty(query)) {
            if (getAdapter() != null) {
                getAdapter().clear();
            }

            mQuery = query;
            mCurrentPage = 0;
            mHasMore = true;
            mSearchView.setQuery(mQuery, false);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

            if (mListener != null) {
                mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
                mListener.onUpdateActionBarTitle(mQuery);
            }

            fetchGallery();
            mSearchMenuItem.collapseActionView();
            return true;
        }

        return false;
    }

    @OnClick(R.id.mySubreddits)
    public void viewMySubreddits() {
        // Alphabetize the list
        List<String> subreddits = new ArrayList<>(mPinnedSubs);
        Collections.sort(subreddits);
        final String[] items = new String[subreddits.size()];
        subreddits.toArray(items);

        new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                .setTitle(R.string.my_subreddits)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String query = items[i];

                        if (!query.equals(mQuery)) {
                            search(items[i]);
                        }
                    }
                })
                .setPositiveButton(R.string.close, null)
                .show();
    }

    void onFilterChanged(RedditSort sort, ImgurFilters.TimeSort topSort) {
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
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            mCurrentPage = 0;
            mIsLoading = true;
            mHasMore = true;
            fetchGallery();
        }

        saveFilterSettings();
    }

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
        SharedPreferences pref = app.getPreferences();

        if (savedInstanceState == null) {
            mSort = RedditSort.getSortFromString(pref.getString(KEY_SORT, RedditSort.TIME.getSort()));
            mTopSort = ImgurFilters.TimeSort.getSortFromString(pref.getString(KEY_TOP_SORT, ImgurFilters.TimeSort.DAY.getSort()));
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

        Set<String> pinned = pref.getStringSet(KEY_PINNED_SUBREDDITS, null);
        if (pinned != null) mPinnedSubs.addAll(pinned);
    }

    private void saveFilterSettings() {
        app.getPreferences().edit()
                .putString(KEY_SORT, mSort.getSort()).putString(KEY_TOP_SORT, mTopSort.getSort())
                .apply();
    }

    private void saveSubreddits() {
        if (mPinnedSubs.isEmpty()) {
            // Remove from shared preferences if saved
            if (app.getPreferences().contains(KEY_PINNED_SUBREDDITS))
                app.getPreferences().edit().remove(KEY_PINNED_SUBREDDITS).apply();
        } else {
            app.getPreferences().edit().putStringSet(KEY_PINNED_SUBREDDITS, mPinnedSubs).apply();
        }
    }

    @Override
    public void onDestroyView() {
        mSearchMenuItem = null;
        mSearchView = null;

        if (mCursorAdapter != null && mCursorAdapter.getCursor() != null
                && !mCursorAdapter.getCursor().isClosed()) {
            mCursorAdapter.getCursor().close();
        }

        super.onDestroyView();
    }

    @Override
    protected void fetchGallery() {
        if (TextUtils.isEmpty(mQuery)) return;
        super.fetchGallery();

        ImgurService apiService = ApiClient.getService();
        String query = mQuery.replaceAll("\\s", "");
        if (query.startsWith("r/")) query = query.replace("r/", "");

        if (mSort == RedditSort.TOP) {
            apiService.getSubRedditForTopSorted(query, mTopSort.getSort(), mCurrentPage).enqueue(this);
        } else {
            apiService.getSubReddit(query, mSort.getSort(), mCurrentPage).enqueue(this);
        }
    }

    @Override
    protected void onApiResult(@NonNull GalleryResponse galleryResponse) {
        super.onApiResult(galleryResponse);

        if (mCurrentPage == 0 && !galleryResponse.data.isEmpty()) {
            SqlHelper sql = SqlHelper.getInstance(getActivity());
            sql.addSubReddit(mQuery);

            if (mCursorAdapter == null) {
                mCursorAdapter = new SearchAdapter(getActivity(), sql.getSubReddits(mQuery), DBContracts.SubRedditContract.COLUMN_NAME);
                mSearchView.setSuggestionsAdapter(mCursorAdapter);
            } else {
                mCursorAdapter.changeCursor(sql.getSubReddits(mQuery));
            }

            mCursorAdapter.notifyDataSetChanged();
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    protected void onEmptyResults() {
        super.onEmptyResults();
        ViewUtils.setEmptyText(mMultiStateView, R.id.empty, getString(R.string.reddit_empty, mQuery));
        mQuery = null;
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.SETTINGS) {
            // Reload pinned subreddits
            mPinnedSubs.clear();
            Set<String> pinned = app.getPreferences().getStringSet(KEY_PINNED_SUBREDDITS, null);
            if (pinned != null) mPinnedSubs.addAll(pinned);
            getActivity().invalidateOptionsMenu();
        }
    }
}
