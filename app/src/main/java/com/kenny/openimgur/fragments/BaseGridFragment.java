package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.collections.SetUniqueList;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Base class for fragments that display images in a grid like style
 * Created by Kenny Campagna on 12/13/2014.
 */
public abstract class BaseGridFragment extends BaseFragment implements Callback<GalleryResponse>, View.OnClickListener {
    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final String KEY_REQUEST_ID = "requestId";

    private static final String KEY_HAS_MORE = "hasMore";

    @BindView(R.id.multiView)
    protected MultiStateView mMultiStateView;

    @BindView(R.id.grid)
    protected RecyclerView mGrid;

    @BindView(R.id.refreshLayout)
    protected SwipeRefreshLayout mRefreshLayout;

    @Nullable
    @BindView(R.id.loadingFooter)
    View mLoadingFooter;

    protected FragmentListener mListener;

    protected boolean mIsLoading = false;

    protected int mCurrentPage = 0;

    protected String mRequestId = null;

    private boolean mAllowNSFW;

    protected boolean mHasMore = true;

    protected GridLayoutManager mManager;

    private GalleryAdapter mAdapter;

    ImageLoader imageLoader;

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAllowNSFW = app.getPreferences().getBoolean(SettingsActivity.NSFW_KEY, false);
        ViewUtils.setRecyclerViewGridDefaults(getActivity(), mGrid);
        imageLoader = ImageUtil.getImageLoader(getActivity());

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
                        imageLoader.resume();
                        break;

                    case RecyclerView.SCROLL_STATE_SETTLING:
                        imageLoader.pause();
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
        GalleryAdapter adapter = getAdapter();

        if (adapter == null || adapter.isEmpty()) {
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            mIsLoading = true;
            if (mListener != null)
                mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
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
    public void onClick(View v) {
        int adapterPosition = mGrid.getChildAdapterPosition(v);

        if (adapterPosition != RecyclerView.NO_POSITION) {
            ArrayList<ImgurBaseObject> items = getAdapter().getItems(adapterPosition);
            int itemPosition = adapterPosition;

            // Get the correct array index of the selected item
            if (itemPosition > GalleryAdapter.MAX_ITEMS / 2) {
                itemPosition = items.size() == GalleryAdapter.MAX_ITEMS
                        ? GalleryAdapter.MAX_ITEMS / 2
                        : items.size() - (getAdapter().getItemCount() - itemPosition);
            }

            onItemSelected(v, itemPosition, items);
        }
    }

    public void refresh() {
        mHasMore = true;
        mCurrentPage = 0;
        mIsLoading = true;
        if (getAdapter() != null) getAdapter().clear();
        if (mListener != null)
            mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_STARTED);
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
                setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(items), this, showPoints()));
                mGrid.scrollToPosition(currentPosition);
                if (mListener != null)
                    mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_COMPLETE);
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
        GalleryAdapter adapter = getAdapter();

        if (adapter != null && !adapter.isEmpty()) {
            // Saving the entire adapter can cause a crash in N
            if (isApiLevel(Build.VERSION_CODES.N) && adapter.getItemCount() > GalleryAdapter.MAX_ITEMS) {
                return;
            }

            outState.putParcelableArrayList(KEY_ITEMS, adapter.retainItems());
            GridLayoutManager manager = (GridLayoutManager) mGrid.getLayoutManager();
            outState.putInt(KEY_CURRENT_POSITION, manager.findFirstVisibleItemPosition());
        }
    }

    @Override
    public void onDestroyView() {
        GalleryAdapter adapter = getAdapter();
        if (adapter != null) adapter.onDestroy();
        super.onDestroyView();
    }

    /**
     * Callback for when an item is selected from the grid
     *
     * @param view     The {@link View} selected
     * @param position The position of the item in the list of items
     * @param items    The list of items that will be able to paged between
     */
    protected void onItemSelected(View view, int position, ArrayList<ImgurBaseObject> items) {
        startActivityForResult(ViewActivity.createIntent(getActivity(), items, position), RequestCodes.GALLERY_VIEW);
    }

    /**
     * Configured the ApiClient to make the api request
     */
    protected void fetchGallery() {
        if (mLoadingFooter != null) mLoadingFooter.setVisibility(View.VISIBLE);
        mIsLoading = true;
    }

    @Override
    public void onResponse(Call<GalleryResponse> call, Response<GalleryResponse> response) {
        if (!isAdded()) return;

        if (response != null) {
            GalleryResponse galleryResponse = response.body();

            if (galleryResponse != null) {
                galleryResponse.purgeNSFW(mAllowNSFW);
                onApiResult(galleryResponse);
            } else {
                onApiFailure(ApiClient.getErrorCode(response.code()));
            }
        } else {
            onApiFailure(R.string.error_generic);
        }
    }

    @Override
    public void onFailure(Call<GalleryResponse> call, Throwable t) {
        LogUtil.e(TAG, "Error fetching gallery items", t);
        if (!isAdded()) return;
        onApiFailure(ApiClient.getErrorCode(t));
        if (mLoadingFooter != null) mLoadingFooter.setVisibility(View.GONE);
    }

    protected void onApiResult(@NonNull GalleryResponse galleryResponse) {
        if (!galleryResponse.data.isEmpty()) {
            galleryResponse.purgeNSFW(mAllowNSFW);

            if (getAdapter() == null) {
                setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(galleryResponse.data), this, showPoints()));
            } else {
                getAdapter().addItems(galleryResponse.data);
            }

            if (mMultiStateView != null)
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);

            if (mCurrentPage == 0 && mListener != null) {
                mListener.onFragmentStateChange(FragmentListener.STATE_LOADING_COMPLETE);
            }
        } else {
            onEmptyResults();
        }

        mIsLoading = false;
        if (mRefreshLayout != null) mRefreshLayout.setRefreshing(false);
        if (mLoadingFooter != null) mLoadingFooter.setVisibility(View.GONE);
    }

    protected void onApiFailure(@StringRes int errorString) {
        if (getAdapter() == null || getAdapter().isEmpty()) {
            if (mListener != null) mListener.onFragmentStateChange(FragmentListener.STATE_ERROR);
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, errorString);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }

        mIsLoading = false;
        if (mRefreshLayout != null) mRefreshLayout.setRefreshing(false);
        if (mLoadingFooter != null) mLoadingFooter.setVisibility(View.GONE);
    }

    protected void onEmptyResults() {
        if (mLoadingFooter != null) mLoadingFooter.setVisibility(View.GONE);
        mHasMore = false;

        if (mMultiStateView.getView(MultiStateView.VIEW_STATE_EMPTY) != null && (getAdapter() == null || getAdapter().isEmpty())) {
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
        }
    }

    /**
     * If the adapter should show the points on the images
     *
     * @return
     */
    protected boolean showPoints() {
        return true;
    }

    /**
     * Returns the view to attach a {@link android.support.design.widget.Snackbar} to
     *
     * @return
     */
    @NonNull
    protected View getSnackbarView() {
        return mListener != null ? mListener.getSnackbarView() : mMultiStateView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.GALLERY_VIEW:
                if (data != null && mAdapter != null) {
                    ImgurBaseObject obj = data.getParcelableExtra(ViewActivity.KEY_ENDING_ITEM);

                    if (obj != null) {
                        int adapterPosition = mAdapter.indexOf(obj);
                        if (adapterPosition >= 0) {
                            if (mManager == null) mManager = (GridLayoutManager) mGrid.getLayoutManager();
                            int visibleItemCount = mManager.getChildCount();
                            int firstVisibleItemPosition = mManager.findFirstVisibleItemPosition();

                            // Update the grid to the item they ended on
                            if (adapterPosition < firstVisibleItemPosition || adapterPosition > firstVisibleItemPosition + visibleItemCount) {
                                mGrid.scrollToPosition(adapterPosition);
                            }
                        }
                    }
                }
                break;

            case RequestCodes.SETTINGS:
                GalleryAdapter adapter = getAdapter();

                if (adapter != null) {
                    SharedPreferences pref = app.getPreferences();
                    boolean nsfwThumb = pref.getBoolean(SettingsActivity.KEY_NSFW_THUMBNAILS, false);
                    mAllowNSFW = pref.getBoolean(SettingsActivity.NSFW_KEY, false);
                    adapter.setAllowNSFW(nsfwThumb);
                    adapter.setThumbnailQuality(getActivity(), pref.getString(SettingsActivity.KEY_THUMBNAIL_QUALITY, ImgurPhoto.THUMBNAIL_GALLERY));
                }
                break;
        }
    }
}
