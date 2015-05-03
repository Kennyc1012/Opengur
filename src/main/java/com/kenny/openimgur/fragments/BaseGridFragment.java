package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ScrollHelper;
import com.kenny.openimgur.util.ViewUtils;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.apache.commons.collections15.list.SetUniqueList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.Optional;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Base class for fragments that display images in a grid like style
 * Created by Kenny Campagna on 12/13/2014.
 */
public abstract class BaseGridFragment extends BaseFragment implements AbsListView.OnScrollListener, AdapterView.OnItemClickListener {
    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final String KEY_REQUEST_ID = "requestId";

    private static final String KEY_HAS_MORE = "hasMore";

    @InjectView(R.id.multiView)
    protected MultiStateView mMultiStateView;

    @InjectView(R.id.grid)
    protected HeaderGridView mGrid;

    @InjectView(R.id.refreshLayout)
    protected SwipeRefreshLayout mRefreshLayout;

    protected FragmentListener mListener;

    protected ApiClient mApiClient;

    protected boolean mIsLoading = false;

    protected int mCurrentPage = 0;

    protected String mRequestId = null;

    private boolean mAllowNSFW;

    protected boolean mHasMore = true;

    private GalleryAdapter mAdapter;

    private ScrollHelper mScrollHelper = new ScrollHelper();

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentListener) mListener = (FragmentListener) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAllowNSFW = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.NSFW_KEY, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGrid.setOnScrollListener(new PauseOnScrollListener(app.getImageLoader(), false, true, this));
        mGrid.setOnItemClickListener(this);
        mRefreshLayout.setColorSchemeColors(getResources().getColor(theme.accentColor));
        int bgColor = theme.isDarkTheme ? R.color.background_material_dark : R.color.background_material_light;
        mRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(bgColor));
        onRestoreSavedInstance(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean nsfwThumb = pref.getBoolean(SettingsActivity.KEY_NSFW_THUMBNAILS, false);
        mAllowNSFW = pref.getBoolean(SettingsActivity.NSFW_KEY, false);
        if (getAdapter() != null) getAdapter().setAllowNSFW(nsfwThumb);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (getAdapter() == null || getAdapter().isEmpty()) {
            mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
            mIsLoading = true;

            if (mListener != null) {
                mListener.onLoadingStarted();
            }

            fetchGallery();
        }
    }

    protected void setAdapter(GalleryAdapter adapter) {
        mAdapter = adapter;
        mGrid.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    protected GalleryAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // NOOP
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Hide the actionbar when scrolling down, show when scrolling up
        switch (mScrollHelper.getScrollDirection(view, firstVisibleItem, totalItemCount)) {
            case ScrollHelper.DIRECTION_DOWN:
                if (mListener != null) mListener.onUpdateActionBar(false);
                break;

            case ScrollHelper.DIRECTION_UP:
                if (mListener != null) mListener.onUpdateActionBar(true);
                break;
        }

        // Load more items when hey get to the end of the list
        if (mHasMore && totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount && !mIsLoading) {
            mIsLoading = true;
            mCurrentPage++;
            fetchGallery();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;
        // Don't respond to the header being clicked

        if (adapterPosition >= 0) {
            ArrayList<ImgurBaseObject> items = getAdapter().getItems(adapterPosition);
            int itemPosition = adapterPosition;

            // Get the correct array index of the selected item
            if (itemPosition > GalleryAdapter.MAX_ITEMS / 2) {
                itemPosition = items.size() == GalleryAdapter.MAX_ITEMS ? GalleryAdapter.MAX_ITEMS / 2 :
                        items.size() - (getAdapter().getCount() - itemPosition);
            }

            onItemSelected(itemPosition, items);
        }
    }

    public boolean allowNSFW() {
        return mAllowNSFW;
    }

    public void refresh() {
        mHasMore = true;
        mCurrentPage = 0;
        mIsLoading = true;
        if (getAdapter() != null) getAdapter().clear();
        if (mListener != null) mListener.onLoadingStarted();
        mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
        fetchGallery();
    }

    @Optional
    @OnClick(R.id.errorButton)
    public void onRetryClick() {
        mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
        fetchGallery();
    }

    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
            mRequestId = savedInstanceState.getString(KEY_REQUEST_ID, null);
            mHasMore = savedInstanceState.getBoolean(KEY_HAS_MORE, true);

            if (savedInstanceState.containsKey(KEY_ITEMS)) {
                ArrayList<ImgurBaseObject> items = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
                int currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
                setUpGridTop();
                setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(items)));
                mGrid.setSelection(currentPosition);

                if (mListener != null) {
                    mListener.onLoadingComplete();
                }

                mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putString(KEY_REQUEST_ID, mRequestId);
        outState.putBoolean(KEY_HAS_MORE, mHasMore);

        if (getAdapter() != null && !getAdapter().isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, getAdapter().retainItems());
            outState.putInt(KEY_CURRENT_POSITION, mGrid.getFirstVisiblePosition());
        }
    }

    @Override
    public void onDestroyView() {
        if (getAdapter() != null) {
            getAdapter().clear();
            setAdapter(null);
        }

        saveFilterSettings();
        EventBus.getDefault().unregister(this);
        if (getHandler() != null) getHandler().removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == getEventType() && event.id.equals(mRequestId)) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                List<ImgurBaseObject> objects;

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);

                    if (arr == null || arr.length() <= 0) {
                        mHasMore = false;
                        LogUtil.v(TAG, "Did not receive any items in the json array");
                        getHandler().sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                        return;
                    }

                    objects = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);
                        ImgurBaseObject imgurObject;

                        if (item.has("is_album") && item.getBoolean("is_album")) {
                            imgurObject = new ImgurAlbum(item);
                        } else {
                            imgurObject = new ImgurPhoto(item);
                        }

                        if (allowNSFW() || !imgurObject.isNSFW()) {
                            objects.add(imgurObject);
                        }
                    }

                    if (objects.size() <= 0) {
                        mHasMore = false;
                        getHandler().sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                    } else {
                        getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, objects);
                    }
                } else {
                    getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                }

            } catch (JSONException e) {
                LogUtil.e(TAG, "Error parsing JSON", e);
                getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
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
            getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        LogUtil.e(TAG, "Error received from Event Bus", e);
    }

    protected void makeRequest(String url) {
        if (mApiClient == null) {
            mApiClient = new ApiClient(url, ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setUrl(url);
        }

        mRequestId = url;
        mApiClient.doWork(getEventType(), mRequestId, null);
    }

    /**
     * Handles setting the header of the grid to an empty view and padding the refresh
     * layout to appear below that same header
     */
    protected void setUpGridTop() {
        View emptyView = ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), getAdditionalHeaderSpace());
        mGrid.addHeaderView(emptyView);
        padRefreshBelowView(emptyView);
    }

    private void padRefreshBelowView(final View headerView) {
        ViewUtils.onPreDraw(headerView, new Runnable() {
            @Override
            public void run() {
                if (isAdded()) {
                    mRefreshLayout.setProgressViewOffset(false, headerView.getBottom(),
                            headerView.getBottom() + getResources().getDimensionPixelSize(R.dimen.refresh_pull_amount));
                }
            }
        });
    }

    /**
     * Returns any additional space needed for the header view for the grid.
     * Only should be overridden when the value is > than 0
     *
     * @return
     */
    protected int getAdditionalHeaderSpace() {
        return 0;
    }

    /**
     * Save any filter settings when the fragment is destroyed
     */
    protected abstract void saveFilterSettings();

    /**
     * The EventType of the Bus Event for the fragment
     *
     * @return
     */
    public abstract ImgurBusEvent.EventType getEventType();

    /**
     * Configured the ApiClient to make the api request
     */
    protected abstract void fetchGallery();

    /**
     * Returns the Handler tied to the fragment
     *
     * @return
     */
    protected abstract ImgurHandler getHandler();

    /**
     * Callback for when an item is selected from the grid
     *
     * @param position The position of the item in the list of items
     * @param items    The list of items that will be able to paged between
     */
    protected abstract void onItemSelected(int position, ArrayList<ImgurBaseObject> items);
}
