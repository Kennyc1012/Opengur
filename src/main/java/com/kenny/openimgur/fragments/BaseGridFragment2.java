package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.adapters.GalleryAdapter2;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Base class for fragments that display images in a grid like style
 * Created by Kenny Campagna on 12/13/2014.
 */
public abstract class BaseGridFragment2 extends BaseFragment implements Callback<GalleryResponse>, View.OnClickListener {
    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final String KEY_REQUEST_ID = "requestId";

    private static final String KEY_HAS_MORE = "hasMore";

    @Bind(R.id.multiView)
    protected MultiStateView mMultiStateView;

    @Bind(R.id.grid)
    protected RecyclerView mGrid;

    @Bind(R.id.refreshLayout)
    protected SwipeRefreshLayout mRefreshLayout;

    protected FragmentListener mListener;

    protected boolean mIsLoading = false;

    protected int mCurrentPage = 0;

    protected String mRequestId = null;

    private boolean mAllowNSFW;

    protected boolean mHasMore = true;

    protected GridLayoutManager mManager;

    private GalleryAdapter2 mAdapter;

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
        mGrid.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mManager == null) {
                    mManager = (GridLayoutManager) recyclerView.getLayoutManager();
                }

                int visibleItemCount = mManager.getChildCount();
                int totalItemCount = mManager.getItemCount();
                int firstVisibleItemPosition = mManager.findFirstVisibleItemPosition();

                // Load more items when hey get to the end of the list
                if (mHasMore && totalItemCount > 0 && firstVisibleItemPosition + visibleItemCount >= totalItemCount && !mIsLoading) {
                    mIsLoading = true;
                    mCurrentPage++;
                    fetchGallery();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_IDLE:
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        app.getImageLoader().resume();
                        break;

                    case RecyclerView.SCROLL_STATE_SETTLING:
                        app.getImageLoader().pause();
                        break;
                }
            }
        });

        mRefreshLayout.setColorSchemeColors(getResources().getColor(theme.accentColor));
        int bgColor = theme.isDarkTheme ? R.color.bg_dark : R.color.bg_light;
        mRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(bgColor));
        onRestoreSavedInstance(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        GalleryAdapter2 adapter = getAdapter();
        SharedPreferences pref = app.getPreferences();
        boolean nsfwThumb = pref.getBoolean(SettingsActivity.KEY_NSFW_THUMBNAILS, false);
        mAllowNSFW = pref.getBoolean(SettingsActivity.NSFW_KEY, false);

        if (adapter != null) {
            adapter.setAllowNSFW(nsfwThumb);
            adapter.setThumbnailQuality(pref.getString(SettingsActivity.KEY_THUMBNAIL_QUALITY, ImgurPhoto.THUMBNAIL_GALLERY));
        }

        if (adapter == null || adapter.isEmpty()) {
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            mIsLoading = true;

            if (mListener != null) {
                mListener.onLoadingStarted();
            }

            fetchGallery();
        }
    }

    protected void setAdapter(GalleryAdapter2 adapter) {
        mAdapter = adapter;
        mGrid.setAdapter(mAdapter);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
    }

    protected GalleryAdapter2 getAdapter() {
        return mAdapter;
    }

    @Override
    public void onClick(View v) {
        int adapterPosition = mGrid.getChildAdapterPosition(v);
        ArrayList<ImgurBaseObject> items = getAdapter().getItems(adapterPosition);
        int itemPosition = adapterPosition;

        // Get the correct array index of the selected item
        if (itemPosition > GalleryAdapter.MAX_ITEMS / 2) {
            itemPosition = items.size() == GalleryAdapter.MAX_ITEMS
                    ? GalleryAdapter.MAX_ITEMS / 2
                    : items.size() - (getAdapter().getItemCount() - itemPosition);
        }

        onItemSelected(itemPosition, items);
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
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        fetchGallery();
    }

    @Nullable
    @OnClick(R.id.errorButton)
    public void onRetryClick() {
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
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
                setAdapter(new GalleryAdapter2(getActivity(), mGrid, SetUniqueList.decorate(items), this, showPoints()));
                mGrid.scrollToPosition(currentPosition);

                if (mListener != null) {
                    mListener.onLoadingComplete();
                }

                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
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
            GridLayoutManager manager = (GridLayoutManager) mGrid.getLayoutManager();
            outState.putInt(KEY_CURRENT_POSITION, manager.findFirstVisibleItemPosition());
        }
    }

    @Override
    public void onDestroyView() {
        GalleryAdapter2 adapter = getAdapter();
        if (adapter != null) adapter.onDestroy();
        saveFilterSettings();
        super.onDestroyView();
    }

    /**
     * Callback for when an item is selected from the grid
     *
     * @param position The position of the item in the list of items
     * @param items    The list of items that will be able to paged between
     */
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        startActivity(ViewActivity.createIntent(getActivity(), items, position));
    }

    /**
     * Configured the ApiClient to make the api request
     */
    protected void fetchGallery() {
        mIsLoading = true;
    }

    @Override
    public void onResponse(Response<GalleryResponse> response, Retrofit retrofit) {
        if (!isAdded()) return;

        if (response != null) {
            if (response.body() != null) {
                onApiResult(response.body());
            } else {
                onApiFailure(ApiClient.getErrorCode(response.code()));
            }
        } else {
            onApiFailure(R.string.error_generic);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        if (!isAdded()) return;
        onApiFailure(ApiClient.getErrorCode(t));

    }

    protected void onApiResult(@NonNull GalleryResponse galleryResponse) {
        if (!galleryResponse.data.isEmpty()) {
            if (getAdapter() == null) {
                setAdapter(new GalleryAdapter2(getActivity(), mGrid, SetUniqueList.decorate(galleryResponse.data), this, showPoints()));
            } else {
                getAdapter().addItems(galleryResponse.data);
            }

            if (mMultiStateView != null)
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);

            if (mCurrentPage == 0) {
                if (mListener != null) mListener.onLoadingComplete();

                mGrid.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mGrid != null) mGrid.scrollToPosition(0);
                    }
                });
            }
        } else {
            onEmptyResults();
        }

        mIsLoading = false;
        if (mRefreshLayout != null) mRefreshLayout.setRefreshing(false);
    }

    protected void onApiFailure(@StringRes int errorString) {
        if (getAdapter() == null || getAdapter().isEmpty()) {
            if (mListener != null) mListener.onError();
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, errorString);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }

        mIsLoading = false;
        if (mRefreshLayout != null) mRefreshLayout.setRefreshing(false);
    }

    protected void onEmptyResults() {
        mHasMore = false;

        if (mMultiStateView.getView(MultiStateView.VIEW_STATE_EMPTY) != null && (getAdapter() == null || getAdapter().isEmpty())) {
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
        }

        if (mListener != null) mListener.onUpdateActionBar(true);
    }

    /**
     * Save any filter settings when the fragment is destroyed
     */
    protected void saveFilterSettings() {

    }

    /**
     * If the adapter should show the points on the images
     *
     * @return
     */
    protected boolean showPoints() {
        return true;
    }
}
