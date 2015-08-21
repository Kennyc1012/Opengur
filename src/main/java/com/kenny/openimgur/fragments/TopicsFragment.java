package com.kenny.openimgur.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurService;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 2/19/15.
 */
public class TopicsFragment extends BaseGridFragment implements TopicsFilterFragment.FilterListener {
    private static final String KEY_TOPIC_ID = "topics_id";

    private static final String KEY_SORT = "topics_sort";

    private static final String KEY_TOP_SORT = "topics_topSort";

    private static final String KEY_TOPIC = "topics_topic";

    private ImgurTopic mTopic;

    private ImgurFilters.GallerySort mSort = ImgurFilters.GallerySort.TIME;

    private ImgurFilters.TimeSort mTimeSort = ImgurFilters.TimeSort.DAY;

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
        inflater.inflate(R.menu.gallery, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (mTopic != null) {
                    refresh();
                }
                return true;

            case R.id.filter:
                if (mListener != null) mListener.onUpdateActionBar(false);

                TopicsFilterFragment fragment = TopicsFilterFragment.createInstance(mTopic, mSort, mTimeSort);
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
    public void onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(R.id.search);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onFilterChange(ImgurTopic topic, ImgurFilters.GallerySort sort, ImgurFilters.TimeSort timeSort) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commit();
        if (mListener != null) mListener.onUpdateActionBar(true);

        if (topic == null || sort == null || timeSort == null ||
                (topic.equals(mTopic) && sort == mSort && mTimeSort == timeSort)) {
            LogUtil.v(TAG, "Filters have not been updated");
            return;
        }

        LogUtil.v(TAG, "Filters updated, Topic: " + topic.getName() + " Sort: " + sort.getSort() + " TimeSort: " + timeSort.getSort());
        mTopic = topic;
        mSort = sort;
        mTimeSort = timeSort;
        mCurrentPage = 0;
        mIsLoading = true;
        mHasMore = true;
        if (getAdapter() != null) getAdapter().clear();
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        if (mListener != null) {
            mListener.onLoadingStarted();
            mListener.onUpdateActionBarTitle(mTopic.getName());
        }

        fetchGallery();
    }

    @Override
    protected void saveFilterSettings() {
        app.getPreferences().edit()
                .putInt(KEY_TOPIC_ID, mTopic != null ? mTopic.getId() : -1)
                .putString(KEY_TOP_SORT, mTimeSort.getSort())
                .putString(KEY_SORT, mSort.getSort()).apply();
    }

    @Override
    protected void fetchGallery() {
        if (mTopic == null) return;
        super.fetchGallery();

        ImgurService apiService = ApiClient.getService();

        if (mSort == ImgurFilters.GallerySort.HIGHEST_SCORING) {
            apiService.getTopicForTopSorted(mTopic.getId(), mTimeSort.getSort(), mCurrentPage, this);
        } else {
            apiService.getTopic(mTopic.getId(), mSort.getSort(), mCurrentPage, this);
        }
    }

    @Override
    protected void onEmptyResults() {
        mIsLoading = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
                        /* No results came back from the api, topic must have been removed.
                         This needs to be confirmed that this can happen */
            String message = getString(R.string.topics_empty_result, mTopic.getName());
            app.getSql().deleteTopic(mTopic.getId());
            mMultiStateView.setErrorText(R.id.errorMessage, message);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }

        if (mListener != null) mListener.onUpdateActionBar(true);
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);

        if (savedInstanceState == null) {
            SharedPreferences pref = app.getPreferences();
            mSort = ImgurFilters.GallerySort.getSortFromString(pref.getString(KEY_SORT, null));
            mTimeSort = ImgurFilters.TimeSort.getSortFromString(pref.getString(KEY_TOP_SORT, null));
            mTopic = app.getSql().getTopic(pref.getInt(KEY_TOPIC_ID, -1));
        } else {
            mSort = ImgurFilters.GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT, ImgurFilters.GallerySort.TIME.getSort()));
            mTimeSort = ImgurFilters.TimeSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, null));
            mTopic = savedInstanceState.getParcelable(KEY_TOPIC);
        }

        if (mTopic == null) {
            if (mListener != null) mListener.onUpdateActionBarTitle(getString(R.string.topics));

            mMultiStateView.post(new Runnable() {
                @Override
                public void run() {
                    if (isAdded() && mMultiStateView != null) {
                        mMultiStateView.setErrorText(R.id.errorMessage, R.string.topics_empty_message);
                        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                    }
                }
            });
        } else if (mListener != null) {
            mListener.onUpdateActionBarTitle(mTopic.getName());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_TOPIC, mTopic);
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_TOP_SORT, mTimeSort.getSort());
    }
}
