package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.ViewActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 8/14/14.
 */
public class RedditFragment extends BaseFragment implements RedditFilterFragment.RedditFilterListener, AbsListView.OnScrollListener {
    public enum RedditSort {
        TIME("time"),
        TOP("top");

        private final String mSort;

        RedditSort(String sort) {
            mSort = sort;
        }

        public String getSort() {
            return mSort;
        }

        public static RedditSort getSortFromString(String item) {
            for (RedditSort s : RedditSort.values()) {
                if (s.getSort().equals(item)) {
                    return s;
                }
            }

            return TIME;
        }
    }

    public enum RedditTopSort {
        DAY("day"),
        WEEK("week"),
        MONTH("month"),
        YEAR("year"),
        ALL("all");

        private final String mSort;

        RedditTopSort(String sort) {
            mSort = sort;
        }

        public String getSort() {
            return mSort;
        }

        public static RedditTopSort getSortFromString(String sort) {
            for (RedditTopSort s : RedditTopSort.values()) {
                if (s.getSort().equals(sort)) {
                    return s;
                }
            }

            return DAY;
        }
    }

    private static final String KEY_QUERY = "query";

    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final String KEY_SORT = "redditSort";

    private static final String KEY_TOP_SORT = "redditTopSort";

    private MultiStateView mMultiView;

    private HeaderGridView mGridView;

    private String mQuery;

    private GalleryAdapter mAdapter;

    private ApiClient mApiClient;

    private FragmentListener mListener;

    private int mCurrentPage = 0;

    private boolean mIsLoading = false;

    private boolean mHasMore = true;

    private boolean mAllowNSFW = false;

    private int mPreviousItem;

    private RedditSort mSort;

    private RedditTopSort mTopSort;

    public static RedditFragment createInstance() {
        return new RedditFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAllowNSFW = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.NSFW_KEY, false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reddit, container, false);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof FragmentListener) {
            mListener = (FragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        }
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        mMultiView = null;
        mGridView = null;
        mHandler.removeCallbacksAndMessages(null);

        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }

        app.getPreferences().edit()
                .putString(KEY_SORT, mSort.getSort())
                .putString(KEY_TOP_SORT, mTopSort.getSort())
                .apply();
        
        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiStateView);
        mGridView = (HeaderGridView) mMultiView.findViewById(R.id.grid);
        mGridView.setOnScrollListener(new PauseOnScrollListener(app.getImageLoader(), false, true, this));

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                int headerSize = mGridView.getNumColumns() * mGridView.getHeaderViewCount();
                int adapterPosition = position - headerSize;
                // Don't respond to the header being clicked

                if (adapterPosition >= 0) {
                    ImgurBaseObject[] items = mAdapter.getItems(adapterPosition);
                    int itemPosition = adapterPosition;

                    // Get the correct array index of the selected item
                    if (itemPosition > GalleryAdapter.MAX_ITEMS / 2) {
                        itemPosition = items.length == GalleryAdapter.MAX_ITEMS ? GalleryAdapter.MAX_ITEMS / 2 :
                                items.length - (mAdapter.getCount() - itemPosition);
                    }

                    startActivity(ViewActivity.createIntent(getActivity(), items, itemPosition));
                }
            }
        });

        handleBundle(savedInstanceState);
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        //NOOP
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Hide the actionbar when scrolling down, show when scrolling up
        if (firstVisibleItem > mPreviousItem) {
            if (mListener != null) {
                mListener.onUpdateActionBar(false);
            }
        } else if (firstVisibleItem < mPreviousItem) {
            if (mListener != null) {
                mListener.onUpdateActionBar(true);
            }
        }

        mPreviousItem = firstVisibleItem;

        // Load more items when hey get to the end of the list
        if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount && !mIsLoading && mHasMore) {
            mCurrentPage++;
            search();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.reddit, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.search));
        searchView.setQueryHint(getString(R.string.enter_sub_reddit));

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String text) {
                if (!mIsLoading && !TextUtils.isEmpty(text)) {
                    if (mAdapter != null) {
                        mAdapter.clear();
                    }

                    mQuery = text;
                    mCurrentPage = 0;
                    search();
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);

                    if (mListener != null) {
                        mListener.onLoadingStarted();
                        mListener.onUpdateActionBarTitle(mQuery);
                    }

                    return true;
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.filter:
                if (mListener != null) mListener.onUpdateActionBar(false);

                RedditFilterFragment fragment = RedditFilterFragment.createInstance(mSort, RedditTopSort.DAY);
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
    public void onFilterChanged(RedditSort sort, RedditTopSort topSort) {
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
        if (mAdapter != null && !mAdapter.isEmpty() && !TextUtils.isEmpty(mQuery)) {
            mAdapter.clear();
            mMultiView.setViewState(MultiStateView.ViewState.LOADING);
            mCurrentPage = 0;
            search();
        }
    }

    private void handleBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            mSort = RedditSort.getSortFromString(app.getPreferences().getString(KEY_SORT, RedditSort.TIME.getSort()));
            mTopSort = RedditTopSort.getSortFromString(app.getPreferences().getString(KEY_TOP_SORT, RedditSort.TIME.getSort()));
        } else {
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
            mQuery = savedInstanceState.getString(KEY_QUERY, null);
            mSort = RedditSort.getSortFromString(savedInstanceState.getString(KEY_SORT, RedditSort.TIME.getSort()));
            mTopSort = RedditTopSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, RedditTopSort.DAY.getSort()));

            if (savedInstanceState.containsKey(KEY_ITEMS)) {
                ImgurBaseObject[] items = (ImgurBaseObject[]) savedInstanceState.getParcelableArray(KEY_ITEMS);
                int currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
                setupAdapter(new ArrayList<ImgurBaseObject>(Arrays.asList(items)));
                mGridView.setSelection(currentPosition);

                if (mListener != null) {
                    mListener.onLoadingComplete();
                }

                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            }
        }

        if (TextUtils.isEmpty(mQuery) && mListener != null) {
            mListener.onUpdateActionBarTitle(getString(R.string.subreddit));
        } else if (mListener != null) {
            mListener.onUpdateActionBarTitle(mQuery);
        }
    }

    /**
     * Performs the Api search
     */
    private void search() {
        if (mApiClient == null) {
            mApiClient = new ApiClient(getUrl(), ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setUrl(getUrl());
        }

        mIsLoading = true;
        mApiClient.doWork(ImgurBusEvent.EventType.REDDIT_SEARCH, mQuery, null);
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
        if (mAdapter == null) {
            mAdapter = new GalleryAdapter(getActivity(), objects);
            View header = ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0);
            mGridView.addHeaderView(header);
            mGridView.setAdapter(mAdapter);
        } else {
            mAdapter.addItems(objects);
        }
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.REDDIT_SEARCH && mQuery.equals(event.id)) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);
                    mHasMore = arr.length() > 0;

                    if (!mHasMore) {
                        mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                        return;
                    }

                    List<ImgurBaseObject> objects = new ArrayList<ImgurBaseObject>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        ImgurBaseObject imgurObject;

                        if (item.has("is_album") && item.getBoolean("is_album")) {
                            imgurObject = new ImgurAlbum(item);
                        } else {
                            imgurObject = new ImgurPhoto(item);
                        }

                        if (!mAllowNSFW && !imgurObject.isNSFW()) {
                            objects.add(imgurObject);
                        } else if (mAllowNSFW) {
                            objects.add(imgurObject);
                        }
                    }

                    if (objects.size() <= 0) {
                        mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, objects);
                    }
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                }

            } catch (JSONException e) {
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
            }
        }
    }

    /**
     * Event Method that is fired if EventBus throws an error
     *
     * @param event
     */
    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        LogUtil.e(TAG, "Error received from Event Bus", e);

    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

                case MESSAGE_EMPTY_RESULT:
                    // Only show the empty view when the list is truly empty
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        mMultiView.setEmptyText(R.id.empty, getString(R.string.reddit_empty, mQuery));
                        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                    }

                    mIsLoading = false;
                    break;

                case MESSAGE_ACTION_COMPLETE:
                    if (mListener != null) {
                        mListener.onLoadingComplete();
                    }

                    List<ImgurBaseObject> objects = (List<ImgurBaseObject>) msg.obj;
                    setupAdapter(objects);
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                    if (mCurrentPage == 0) {
                        mGridView.post(new Runnable() {
                            @Override
                            public void run() {
                                mGridView.setSelection(0);
                            }
                        });
                    }

                    mIsLoading = false;
                    break;

                case MESSAGE_ACTION_FAILED:
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        if (mListener != null) {
                            mListener.onError((Integer) msg.obj);
                        }

                        mMultiView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                        mMultiView.setErrorButtonText(R.id.errorButton, R.string.retry);
                        mMultiView.setErrorButtonClickListener(R.id.errorButton, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                search();
                            }
                        });

                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
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
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putString(KEY_QUERY, mQuery);
        outState.putString(KEY_TOP_SORT, mTopSort.getSort());

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArray(KEY_ITEMS, mAdapter.getAllItems());
            outState.putInt(KEY_CURRENT_POSITION, mGridView.getFirstVisiblePosition());
        }

        super.onSaveInstanceState(outState);
    }
}
