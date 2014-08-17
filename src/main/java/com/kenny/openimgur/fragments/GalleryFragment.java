package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.classes.TabActivityListener;
import com.kenny.openimgur.ui.FilterDialogFragment;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.ViewUtils;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 8/14/14.
 */
public class GalleryFragment extends Fragment implements FilterDialogFragment.FilterListener {
    private static final String KEY_SECTION = "section";

    private static final String KEY_QUALITY = "quality";

    public enum GallerySection {
        HOT("hot"),
        USER("user"),
        TOP("top");

        private final String mSection;

        private GallerySection(String s) {
            mSection = s;
        }

        public String getSection() {
            return mSection;
        }

        /**
         * Returns the Enum value for the section based on the string
         *
         * @param section
         * @return
         */
        public static GallerySection getSectionFromString(String section) {
            return HOT.getSection().equals(section) ? HOT : USER;
        }
    }

    public enum GallerySort {
        TIME("time"),
        VIRAL("viral");

        private final String mSort;

        private GallerySort(String s) {
            mSort = s;
        }

        public String getSort() {
            return mSort;
        }

        /**
         * Returns the Enum value based on a string
         *
         * @param sort
         * @return
         */
        public static GallerySort getSortFromString(String sort) {
            if (TIME.getSort().equals(sort)) {
                return TIME;
            }

            return VIRAL;
        }

        /**
         * Returns a string array for the popup dialog for choosing filter options
         *
         * @param context
         * @return
         */
        public static String[] getPopupArray(Context context) {
            GallerySort[] items = GallerySort.values();
            String[] array = new String[items.length];
            for (int i = 0; i < items.length; i++) {
                array[i] = context.getString(getStringId(items[i]));
            }

            return array;
        }

        /**
         * Returns the string id for the corresponding GallerySort enum
         *
         * @param sort
         * @return
         */
        public static int getStringId(GallerySort sort) {
            switch (sort) {
                case TIME:
                    return R.string.filter_time;

                case VIRAL:
                default:
                    return R.string.filter_popular;
            }
        }
    }

    private GallerySection mSection = GallerySection.HOT;

    private GallerySort mSort = GallerySort.TIME;

    private int mCurrentPage = 0;

    private MultiStateView mMultiView;

    private HeaderGridView mGridView;

    private TextViewRoboto mErrorMessage;

    private GalleryAdapter mAdapter;

    private TabActivityListener mListener;

    private ApiClient mApiClient;

    private boolean mIsLoading = false;

    private int mPreviousItem = 0;

    public static GalleryFragment createInstance() {
        return new GalleryFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof TabActivityListener) {
            mListener = (TabActivityListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSort = GallerySort.getSortFromString(pref.getString("sort", null));
        mSection = GallerySection.getSectionFromString(pref.getString("section", null));
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.ViewState.LOADING);
            mIsLoading = true;

            if (mListener != null) {
                mListener.onLoadingStarted();
            }

            getGallery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        mMultiView = null;
        mErrorMessage = null;
        mGridView = null;
        mHandler.removeCallbacksAndMessages(null);

        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }

        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        edit.putString("section", mSection.getSection());
        edit.putString("sort", mSort.getSort()).apply();
        super.onDestroyView();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiStateView);
        mGridView = (HeaderGridView) mMultiView.findViewById(R.id.grid);
        mErrorMessage = (TextViewRoboto) mMultiView.findViewById(R.id.errorMessage);
        mGridView.setOnScrollListener(mScrollListener);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
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

        if (getArguments() != null) {
            mSection = GallerySection.getSectionFromString(getArguments().getString(KEY_SECTION, null));
        }
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
                mCurrentPage = 0;
                mIsLoading = true;

                if (mAdapter != null) {
                    mAdapter.clear();
                    mAdapter.notifyDataSetChanged();
                }

                if (mListener != null) {
                    mListener.onLoadingStarted();
                }

                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                getGallery();
                return true;

            case R.id.filter:
                FilterDialogFragment filter = FilterDialogFragment.createInstance(mSort, mSection);
                filter.setFilterListner(this);
                filter.show(getFragmentManager(), "filter");
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns the URL based on the selected sort and section
     *
     * @return
     */
    private String getGalleryUrl() {
        return String.format(Endpoints.GALLERY.getUrl(), mSection.getSection(),
                mSort.getSort(), mCurrentPage);
    }

    /**
     * Queries the Api for Gallery items
     */
    private void getGallery() {
        if (mApiClient == null) {
            mApiClient = new ApiClient(getGalleryUrl(), ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setUrl(getGalleryUrl());
        }

        mApiClient.doWork(ImgurBusEvent.EventType.GALLERY, mSection.getSection(), null);
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.GALLERY && mSection.getSection().equals(event.id)) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                List<ImgurBaseObject> objects = null;

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);
                    objects = new ArrayList<ImgurBaseObject>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject item = arr.getJSONObject(i);

                        if (item.has("is_album") && item.getBoolean("is_album")) {
                            ImgurAlbum a = new ImgurAlbum(item);
                            objects.add(a);
                        } else {
                            ImgurPhoto p = new ImgurPhoto(item);
                            objects.add(p);
                        }
                    }

                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, objects);
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                }

            } catch (JSONException e) {
                e.printStackTrace();
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
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.ViewState.ERROR);
            mErrorMessage.setText(R.string.error_generic);
        }

        event.getThrowable().printStackTrace();
    }

    @Override
    public void onFilterChange(GallerySection section, GallerySort sort) {
        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
        }

        mSection = section;
        mSort = sort;
        mCurrentPage = 0;
        mIsLoading = true;
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        getGallery();
    }

    private PauseOnScrollListener mScrollListener = new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
            new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    // Hide the actionbar when scrolling down, show when scrolling up
                    if (firstVisibleItem > mPreviousItem && mListener != null) {
                        mListener.oHideActionBar(false);
                    } else if (firstVisibleItem < mPreviousItem && mListener != null) {
                        mListener.oHideActionBar(true);
                    }

                    mPreviousItem = firstVisibleItem;

                    // Load more items when hey get to the end of the list
                    if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount && !mIsLoading) {
                        mIsLoading = true;
                        mCurrentPage++;
                        getGallery();
                    }
                }
            }
    );

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> gallery = (List<ImgurBaseObject>) msg.obj;

                    if (mAdapter == null) {
                        String quality = PreferenceManager.getDefaultSharedPreferences(getActivity())
                                .getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
                        mAdapter = new GalleryAdapter(getActivity(), OpenImgurApp.getInstance().getImageLoader(), gallery, quality);
                        mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                        mGridView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(gallery);
                        mAdapter.notifyDataSetChanged();
                    }

                    if (mListener != null) {
                        mListener.onLoadingComplete();
                    }


                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                    // Due to MultiStateView setting the views visibility to GONE, the list will not reset to the top
                    // If they change the filter or refresh
                    if (mCurrentPage == 0) {
                        mMultiView.post(new Runnable() {
                            @Override
                            public void run() {
                                mGridView.setSelection(0);
                            }
                        });
                    }
                    break;

                case MESSAGE_ACTION_FAILED:
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        mErrorMessage.setText((Integer) msg.obj);

                        if (mListener != null) {
                            mListener.onError((Integer) msg.obj);
                        }

                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    }

                    break;

                default:
                    if (mListener != null) {
                        mListener.onLoadingComplete();
                    }

                    super.handleMessage(msg);
                    break;
            }

            mIsLoading = false;
        }
    };

}
