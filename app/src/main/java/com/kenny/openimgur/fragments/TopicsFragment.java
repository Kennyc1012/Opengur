package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurService;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 2/19/15.
 */
public class TopicsFragment extends BaseGridFragment {
    private static final String KEY_TOPIC_ID = "topics_id";

    private static final String KEY_SORT = "topics_sort";

    private static final String KEY_TOP_SORT = "topics_topSort";

    private static final String KEY_TOPIC = "topics_topic";

    ImgurTopic mTopic;

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
        inflater.inflate(R.menu.topics, menu);
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
                Activity activity = getActivity();
                View anchor;
                anchor = activity.findViewById(R.id.filter);
                if (anchor == null) anchor = activity.findViewById(R.id.refresh);

                PopupMenu m = new PopupMenu(getActivity(), anchor);
                m.inflate(R.menu.filter_gallery_search);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.newest:
                                onFilterChange(ImgurFilters.GallerySort.TIME, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.popularity:
                                onFilterChange(ImgurFilters.GallerySort.VIRAL, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.scoringDay:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.DAY);
                                return true;

                            case R.id.scoringWeek:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.WEEK);
                                return true;

                            case R.id.scoringMonth:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.MONTH);
                                return true;

                            case R.id.scoringYear:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.YEAR);
                                return true;

                            case R.id.scoringAll:
                                onFilterChange(ImgurFilters.GallerySort.HIGHEST_SCORING, ImgurFilters.TimeSort.ALL);
                                return true;
                        }

                        return false;
                    }
                });
                m.show();
                return true;

            case R.id.refreshTopics:
                fetchTopics();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void onFilterChange(ImgurFilters.GallerySort sort, ImgurFilters.TimeSort timeSort) {
        if (sort == mSort && mTimeSort == timeSort) {
            LogUtil.v(TAG, "Filters have not been updated");
            return;
        }

        mSort = sort;
        mTimeSort = timeSort;
        mCurrentPage = 0;
        mIsLoading = true;
        mHasMore = true;
        if (getAdapter() != null) getAdapter().clear();
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        if (mListener != null && mTopic != null) {
            mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
        }

        fetchGallery();
        saveFilterSettings();
    }

    private void saveFilterSettings() {
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
            apiService.getTopicForTopSorted(mTopic.getId(), mTimeSort.getSort(), mCurrentPage).enqueue(this);
        } else {
            apiService.getTopic(mTopic.getId(), mSort.getSort(), mCurrentPage).enqueue(this);
        }
    }

    @Override
    protected void onEmptyResults() {
        mIsLoading = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
                        /* No results came back from the api, topic must have been removed.
                         This needs to be confirmed that this can happen */
            String message = getString(R.string.topics_empty_result, mTopic.getName());
            SqlHelper.getInstance(getActivity()).deleteTopic(mTopic.getId());
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, message);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);
        SqlHelper sql = SqlHelper.getInstance(getActivity());

        if (savedInstanceState == null) {
            SharedPreferences pref = app.getPreferences();
            mSort = ImgurFilters.GallerySort.getSortFromString(pref.getString(KEY_SORT, null));
            mTimeSort = ImgurFilters.TimeSort.getSortFromString(pref.getString(KEY_TOP_SORT, null));
            mTopic = sql.getTopic(pref.getInt(KEY_TOPIC_ID, -1));
        } else {
            mSort = ImgurFilters.GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT, ImgurFilters.GallerySort.TIME.getSort()));
            mTimeSort = ImgurFilters.TimeSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, null));
            mTopic = savedInstanceState.getParcelable(KEY_TOPIC);
        }

        List<ImgurTopic> topics = sql.getTopics();

        if (!topics.isEmpty()) {
            if (mTopic == null) mTopic = topics.get(0);
            if (mListener != null) mListener.onUpdateActionBarSpinner(topics, mTopic);
            fetchGallery();
        } else {
            fetchTopics();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_TOPIC, mTopic);
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_TOP_SORT, mTimeSort.getSort());
    }

    public void onTopicChanged(@NonNull ImgurTopic topic) {
        if (mTopic.getId() != topic.getId()) {
            mTopic = topic;
            GalleryAdapter adapter = getAdapter();
            if (adapter != null) adapter.clear();
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            fetchGallery();
            saveFilterSettings();
        }
    }

    private void fetchTopics() {
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        ApiClient.getService().getDefaultTopics().enqueue(new Callback<TopicResponse>() {
            @Override
            public void onResponse(Call<TopicResponse> call, Response<TopicResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && !response.body().data.isEmpty()) {
                    List<ImgurTopic> topics = response.body().data;
                    SqlHelper.getInstance(getActivity()).addTopics(topics);
                    // Auto fetch the first topic
                    if (mTopic == null) mTopic = topics.get(0);
                    if (mListener != null) mListener.onUpdateActionBarSpinner(topics, mTopic);

                    if (getAdapter() == null || getAdapter().isEmpty()) {
                        fetchGallery();
                    } else {
                        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    }
                } else {
                    ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, R.string.error_generic);
                    mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void onFailure(Call<TopicResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch topics", t);
                ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, ApiClient.getErrorCode(t));
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }
}
