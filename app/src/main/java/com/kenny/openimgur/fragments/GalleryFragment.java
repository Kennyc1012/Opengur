package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.PopupMenu;
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
import com.kenny.openimgur.activities.GallerySearchActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurService;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurFilters.GallerySection;
import com.kenny.openimgur.classes.ImgurFilters.GallerySort;
import com.kenny.openimgur.classes.ImgurFilters.TimeSort;
import com.kenny.openimgur.ui.adapters.SearchAdapter;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.kennyc.view.MultiStateView;

/**
 * Created by kcampagna on 8/14/14.
 */
public class GalleryFragment extends BaseGridFragment {
    private static final String KEY_SECTION = "section";

    private static final String KEY_SORT = "sort";

    private static final String KEY_TOP_SORT = "galleryTopSort";

    private static final String KEY_SHOW_VIRAL = "showViral";

    protected GallerySection mSection = GallerySection.HOT;

    protected GallerySort mSort = GallerySort.TIME;

    protected TimeSort mTimeSort = TimeSort.DAY;

    boolean mShowViral = true;

    protected SearchView mSearchView;

    protected MenuItem mSearchMenuItem;

    protected SearchAdapter mSearchAdapter;

    public static GalleryFragment newInstance() {
        return new GalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.gallery, menu);
        mSearchMenuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(mSearchMenuItem);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                if (!TextUtils.isEmpty(text)) {
                    startActivity(GallerySearchActivity.createIntent(getActivity(), text));
                    mSearchMenuItem.collapseActionView();
                    return true;
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
        mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionSelect(int position) {
                String query = mSearchAdapter.getTitle(position);

                if (GalleryFragment.this instanceof GallerySearchFragment) {
                    ((GallerySearchFragment) GalleryFragment.this).setQuery(query);
                    GalleryFragment.this.refresh();
                } else {
                    startActivity(GallerySearchActivity.createIntent(getActivity(), query));
                }

                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {
                String query = mSearchAdapter.getTitle(position);

                if (GalleryFragment.this instanceof GallerySearchFragment) {
                    ((GallerySearchFragment) GalleryFragment.this).setQuery(query);
                    GalleryFragment.this.refresh();
                } else {
                    startActivity(GallerySearchActivity.createIntent(getActivity(), query));
                }

                return true;
            }
        });

        // Guard against potential "Not attached to activity" exception, although I don't see how that's possible >_>
        if (getActivity() != null) {
            mSearchView.setQueryHint(getString(R.string.gallery_search_hint));

            if (mSearchView.getSuggestionsAdapter() == null) {
                mSearchAdapter = new SearchAdapter(getActivity(), SqlHelper.getInstance(getActivity()).getPreviousGallerySearches(), DBContracts.GallerySearchContract.COLUMN_NAME);
                mSearchAdapter.setFilterQueryProvider(new FilterQueryProvider() {
                    @Override
                    public Cursor runQuery(CharSequence constraint) {
                        return SqlHelper.getInstance(getActivity()).getPreviousGallerySearches(constraint.toString());
                    }
                });
                mSearchView.setSuggestionsAdapter(mSearchAdapter);
            }
        } else {
            LogUtil.w(TAG, "onCreateOptionsMenu: Not attached to activity yet");
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.showViral);
        if (item != null) {
            item.setChecked(mShowViral);
            item.setVisible(mSection == GallerySection.USER);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh();
                return true;

            case R.id.filter:
                Activity activity = getActivity();
                View anchor;
                anchor = activity.findViewById(R.id.filter);
                if (anchor == null) anchor = activity.findViewById(R.id.refresh);
                if (anchor == null) anchor = activity.findViewById(R.id.search);

                PopupMenu m = new PopupMenu(getActivity(), anchor);
                m.inflate(getFilterMenu());
                m.setOnMenuItemClickListener(getMenuItemClickListener());
                m.show();
                return true;

            case R.id.showViral:
                boolean checked = !item.isChecked();
                item.setChecked(checked);
                onFilterChange(mSection, mSort, mTimeSort, checked);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        mSearchView = null;
        mSearchMenuItem = null;

        if (mSearchAdapter != null && mSearchAdapter.getCursor() != null
                && !mSearchAdapter.getCursor().isClosed()) {
            mSearchAdapter.getCursor().close();
        }

        super.onDestroyView();
    }

    protected void onFilterChange(@NonNull GallerySection section, @NonNull GallerySort sort, @NonNull TimeSort timeSort, boolean showViral) {
        // Don't change if the same filter was selected
        if (section == mSection && mSort == sort && mShowViral == showViral && timeSort == mTimeSort) {
            return;
        }

        if (getAdapter() != null) {
            getAdapter().clear();
        }

        mSection = section;
        mSort = sort;
        mTimeSort = timeSort;
        mShowViral = showViral;
        mCurrentPage = 0;
        mIsLoading = true;
        mHasMore = true;
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        if (mListener != null) {
            mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
            mListener.onUpdateActionBarTitle(getString(mSection.getResourceId()));
        }

        fetchGallery();
        getActivity().invalidateOptionsMenu();
        saveFilterSettings();
    }

    @Override
    protected void fetchGallery() {
        super.fetchGallery();
        ImgurService apiService = ApiClient.getService();

        if (mSort == GallerySort.HIGHEST_SCORING) {
            apiService.getGalleryForTopSorted(mSection.getSection(), mTimeSort.getSort(), mCurrentPage).enqueue(this);
        } else {
            apiService.getGallery(mSection.getSection(), mSort.getSort(), mCurrentPage, mShowViral).enqueue(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SECTION, mSection.getSection());
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putBoolean(KEY_SHOW_VIRAL, mShowViral);
        outState.putString(KEY_TOP_SORT, mTimeSort.getSort());
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);

        if (savedInstanceState == null) {
            SharedPreferences pref = app.getPreferences();
            mSection = GallerySection.getSectionFromString(pref.getString(KEY_SECTION, null));
            mSort = GallerySort.getSortFromString(pref.getString(KEY_SORT, null));
            mTimeSort = TimeSort.getSortFromString(pref.getString(KEY_TOP_SORT, null));
            mShowViral = pref.getBoolean(KEY_SHOW_VIRAL, true);
        } else {
            mSort = GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT, GallerySort.TIME.getSort()));
            mSection = GallerySection.getSectionFromString(savedInstanceState.getString(KEY_SECTION, GallerySection.HOT.getSection()));
            mTimeSort = TimeSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, null));
            mShowViral = savedInstanceState.getBoolean(KEY_SHOW_VIRAL, true);
        }

        if (mListener != null)
            mListener.onUpdateActionBarTitle(getString(mSection.getResourceId()));
    }

    protected void saveFilterSettings() {
        app.getPreferences().edit()
                .putString(KEY_SECTION, mSection.getSection())
                .putBoolean(KEY_SHOW_VIRAL, mShowViral)
                .putString(KEY_TOP_SORT, mTimeSort.getSort())
                .putString(KEY_SORT, mSort.getSort()).apply();
    }

    @MenuRes
    protected int getFilterMenu() {
        return R.menu.filter_gallery;
    }

    protected PopupMenu.OnMenuItemClickListener getMenuItemClickListener() {
        return new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.userSubNew:
                        onFilterChange(GallerySection.USER, GallerySort.TIME, TimeSort.DAY, mShowViral);
                        return true;

                    case R.id.userSubRising:
                        onFilterChange(GallerySection.USER, GallerySort.RISING, TimeSort.DAY, mShowViral);
                        return true;

                    case R.id.userSubPopularity:
                        onFilterChange(GallerySection.USER, GallerySort.VIRAL, TimeSort.DAY, mShowViral);
                        return true;

                    case R.id.viralNew:
                        onFilterChange(GallerySection.HOT, GallerySort.TIME, TimeSort.DAY, mShowViral);
                        return true;

                    case R.id.viralPopularity:
                        onFilterChange(GallerySection.HOT, GallerySort.VIRAL, TimeSort.DAY, mShowViral);
                        return true;

                    case R.id.viralDay:
                        onFilterChange(GallerySection.HOT, GallerySort.HIGHEST_SCORING, TimeSort.DAY, mShowViral);
                        return true;

                    case R.id.viralWeek:
                        onFilterChange(GallerySection.HOT, GallerySort.HIGHEST_SCORING, TimeSort.WEEK, mShowViral);
                        return true;

                    case R.id.viralMonth:
                        onFilterChange(GallerySection.HOT, GallerySort.HIGHEST_SCORING, TimeSort.MONTH, mShowViral);
                        return true;

                    case R.id.viralYear:
                        onFilterChange(GallerySection.HOT, GallerySort.HIGHEST_SCORING, TimeSort.YEAR, mShowViral);
                        return true;

                    case R.id.viralAll:
                        onFilterChange(GallerySection.HOT, GallerySort.HIGHEST_SCORING, TimeSort.ALL, mShowViral);
                        return true;
                }

                return false;
            }
        };
    }
}
