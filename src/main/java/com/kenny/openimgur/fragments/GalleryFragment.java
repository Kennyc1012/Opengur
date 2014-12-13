package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.activities.ViewActivity;
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

import org.apache.commons.collections15.list.SetUniqueList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 8/14/14.
 */
public class GalleryFragment extends BaseFragment implements GalleryFilterFragment.FilterListener, AbsListView.OnScrollListener {
    private static final String KEY_SECTION = "section";

    private static final String KEY_SORT = "sort";

    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final String KEY_SHOW_VIRAL = "showViral";

    public enum GallerySection {
        HOT("hot"),
        USER("user");

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
            if (HOT.getSection().equals(section)) {
                return HOT;
            }

            return USER;
        }

        /**
         * Returns the position in the enum array of the given section
         *
         * @param section
         * @return
         */
        public static int getPositionFromSection(GallerySection section) {
            GallerySection[] sections = GallerySection.values();

            for (int i = 0; i < sections.length; i++) {
                if (section.equals(sections[i])) {
                    if (i == 1) {
                        return 49;
                    } else if (i == 2) {
                        return 99;
                    }

                    return i;
                }
            }

            return 0;
        }

        /**
         * Returns the GallerySection in the enum array from the given position
         *
         * @param position
         * @return
         */
        public static GallerySection getSectionFromPosition(int position) {
            GallerySection[] sections = GallerySection.values();

            for (GallerySection section : sections) {
                if (section == sections[position]) {
                    return section;
                }
            }

            return GallerySection.HOT;
        }

        /**
         * Returns the String Resource for the section
         *
         * @return
         */
        @StringRes
        public int getResourceId() {
            switch (this) {
                case HOT:
                    return R.string.viral;

                case USER:
                    return R.string.user_sub;
            }

            return R.string.viral;
        }
    }

    public enum GallerySort {
        TIME("time"),
        RISING("rising"),
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
            } else if (RISING.getSort().equals(sort)) {
                return RISING;
            }

            return VIRAL;
        }
    }

    @InjectView(R.id.multiStateView)
    MultiStateView mMultiView;

    @InjectView(R.id.grid)
    HeaderGridView mGridView;

    private GallerySection mSection = GallerySection.HOT;

    private GallerySort mSort = GallerySort.TIME;

    private int mCurrentPage = 0;

    private GalleryAdapter mAdapter;

    private FragmentListener mListener;

    private ApiClient mApiClient;

    private boolean mIsLoading = false;

    private boolean mAllowNSFW = false;

    private boolean mShowViral = true;

    private int mPreviousItem = 0;

    public static GalleryFragment createInstance() {
        return new GalleryFragment();
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mAllowNSFW = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(SettingsActivity.NSFW_KEY, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

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
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        mHandler.removeCallbacksAndMessages(null);

        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }

        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        edit.putString(KEY_SECTION, mSection.getSection());
        edit.putBoolean(KEY_SHOW_VIRAL, mShowViral);
        edit.putString(KEY_SORT, mSort.getSort()).apply();
        super.onDestroyView();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int i) {
        //NOOP
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Hide the actionbar when scrolling down, show when scrolling up
        if (firstVisibleItem > mPreviousItem && mListener != null) {
            mListener.onUpdateActionBar(false);
        } else if (firstVisibleItem < mPreviousItem && mListener != null) {
            mListener.onUpdateActionBar(true);
        }

        mPreviousItem = firstVisibleItem;

        // Load more items when hey get to the end of the list
        if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount && !mIsLoading) {
            mIsLoading = true;
            mCurrentPage++;
            getGallery();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGridView.setOnScrollListener(new PauseOnScrollListener(app.getImageLoader(), false, true, this));

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                int headerSize = mGridView.getNumColumns() * mGridView.getHeaderViewCount();
                int adapterPosition = position - headerSize;
                // Don't respond to the header being clicked

                if (adapterPosition >= 0) {
                    ArrayList<ImgurBaseObject> items = mAdapter.getItems(adapterPosition);
                    int itemPosition = adapterPosition;

                    // Get the correct array index of the selected item
                    if (itemPosition > GalleryAdapter.MAX_ITEMS / 2) {
                        itemPosition = items.size() == GalleryAdapter.MAX_ITEMS ? GalleryAdapter.MAX_ITEMS / 2 :
                                items.size() - (mAdapter.getCount() - itemPosition);
                    }

                    startActivity(ViewActivity.createIntent(getActivity(), items, itemPosition));
                }
            }
        });

        handleBundle(savedInstanceState);
    }

    private void handleBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mSection = GallerySection.getSectionFromString(pref.getString(KEY_SECTION, null));
            mSort = GallerySort.getSortFromString(pref.getString(KEY_SORT, null));
            mShowViral = pref.getBoolean(KEY_SHOW_VIRAL, true);
        } else {
            mSort = GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT, GallerySort.TIME.getSort()));
            mSection = GallerySection.getSectionFromString(savedInstanceState.getString(KEY_SECTION, GallerySection.HOT.getSection()));
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
            mShowViral = savedInstanceState.getBoolean(KEY_SHOW_VIRAL, true);

            if (savedInstanceState.containsKey(KEY_ITEMS)) {
                ArrayList<ImgurBaseObject> items = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
                int currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
                mAdapter = new GalleryAdapter(getActivity(), SetUniqueList.decorate(items));
                mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                mGridView.setAdapter(mAdapter);
                mGridView.setSelection(currentPosition);

                if (mListener != null) {
                    mListener.onLoadingComplete();
                }

                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            }
        }

        if (mListener != null)
            mListener.onUpdateActionBarTitle(getString(mSection.getResourceId()));
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
                }

                if (mListener != null) {
                    mListener.onLoadingStarted();
                }

                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                getGallery();
                return true;

            case R.id.filter:
                if (mListener != null) mListener.onUpdateActionBar(false);

                GalleryFilterFragment fragment = GalleryFilterFragment.createInstance(mSort, mSection, mShowViral);
                fragment.setFilterListener(this);
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(android.R.id.content, fragment, "filter")
                        .commit();
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
        return String.format(Endpoints.GALLERY.getUrl(), mSection.getSection(), mSort.getSort(), mCurrentPage, mShowViral);
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
                List<ImgurBaseObject> objects;

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);
                    objects = new ArrayList<ImgurBaseObject>();

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

                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, objects);
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                }

            } catch (JSONException e) {
                LogUtil.e(TAG, "Error parsing JSON", e);
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

    @Override
    public void onFilterChange(GallerySection section, GallerySort sort, boolean showViral) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commit();
        if (mListener != null) mListener.onUpdateActionBar(true);

        // Null values represent that the filter was canceled
        if (section == null || sort == null || (section == mSection && mSort == sort && mShowViral == showViral)) {
            return;
        }

        if (mAdapter != null) {
            mAdapter.clear();
        }

        mSection = section;
        mSort = sort;
        mShowViral = showViral;
        mCurrentPage = 0;
        mIsLoading = true;
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);

        if (mListener != null) {
            mListener.onLoadingStarted();
            mListener.onUpdateActionBarTitle(getString(mSection.getResourceId()));
        }

        getGallery();
    }

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> gallery = (List<ImgurBaseObject>) msg.obj;

                    if (mAdapter == null) {
                        mAdapter = new GalleryAdapter(getActivity(), SetUniqueList.decorate(gallery));
                        mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                        mGridView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(gallery);
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
                        if (mListener != null) {
                            mListener.onError((Integer) msg.obj);
                        }

                        mMultiView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                        mMultiView.setErrorButtonText(R.id.errorButton, R.string.retry);
                        mMultiView.setErrorButtonClickListener(R.id.errorButton, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getGallery();
                            }
                        });

                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    }

                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

            mIsLoading = false;
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SECTION, mSection.getSection());
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putBoolean(KEY_SHOW_VIRAL, mShowViral);

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mAdapter.retainItems());
            outState.putInt(KEY_CURRENT_POSITION, mGridView.getFirstVisiblePosition());
        }

        super.onSaveInstanceState(outState);
    }
}
