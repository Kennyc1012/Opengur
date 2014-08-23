package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.SqlHelper;
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
public class RedditFragment extends Fragment {
    private static final String KEY_QUERY = "query";

    private static final String KEY_SORT = "sort";

    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_QUALITY = "quality";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final int PAGE = 1;

    private static final long SEARCH_DELAY = 1000L;

    private static final int MIN_SEARCH_CHAR = 4;

    private AutoCompleteTextView mSearchEditText;

    private View mQuickReturnView;

    private MultiStateView mMultiView;

    private HeaderGridView mGridView;

    private ImageButton mClearButton;

    private RedditHandler mHandler = new RedditHandler();

    private String mQuery;

    private GalleryAdapter mAdapter;

    private ApiClient mApiClient;

    private GalleryFragment.GallerySort mSort = GalleryFragment.GallerySort.TIME;

    private TabActivityListener mListener;

    private SqlHelper mSql;

    private int mCurrentPage = 0;

    private boolean mIsLoading = false;

    private boolean mHasMore = true;

    private boolean mInputIsShowing = true;

    private boolean mIsRestoring = false;

    private float mAnimationHeight;

    private int mPreviousItem;

    private String mQuality;

    private ArrayAdapter<String> mSearchAdapter;

    public static RedditFragment createInstance() {
        return new RedditFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mSql = OpenImgurApp.getInstance(getActivity()).getSql();
        return inflater.inflate(R.layout.fragment_reddit, container, false);
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
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        mSearchEditText = null;
        mMultiView = null;
        mGridView = null;
        mClearButton = null;
        mQuickReturnView = null;
        mHandler.removeCallbacksAndMessages(null);

        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }

        super.onDestroy();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiStateView);
        mGridView = (HeaderGridView) mMultiView.findViewById(R.id.grid);
        mGridView.setOnScrollListener(mScrollListener);
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

        mSearchEditText = (AutoCompleteTextView) view.findViewById(R.id.search);
        configurePreviousSearches();

        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {

                mHandler.removeMessages(RedditHandler.MESSAGE_AUTO_SEARCH);
                String text = textView.getText().toString();

                if (!TextUtils.isEmpty(text)) {
                    if (mAdapter != null) {
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    if (mListener != null) {
                        mListener.onLoadingStarted(PAGE);
                    }

                    mQuery = text;
                    mCurrentPage = 0;
                    search();
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    return true;
                }

                return false;
            }
        });

        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if (charSequence.length() > 0 && mClearButton.getAlpha() == 0.0f) {
                    mClearButton.animate().alpha(1.0f).setDuration(300L);
                } else if (charSequence.length() <= 0 && mClearButton.getAlpha() == 1.0f) {
                    mClearButton.animate().alpha(0.0f).setDuration(300L);
                }

                if (!mIsRestoring) {
                    mHandler.removeMessages(RedditHandler.MESSAGE_AUTO_SEARCH);
                    if (!TextUtils.isEmpty(charSequence) && charSequence.length() >= MIN_SEARCH_CHAR) {
                        mHandler.sendMessageDelayed(RedditHandler.MESSAGE_AUTO_SEARCH, charSequence.toString(), SEARCH_DELAY);
                    }
                } else {
                    mIsRestoring = false;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mQuickReturnView = view.findViewById(R.id.quickReturnView);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mQuickReturnView.getLayoutParams();
        float extraSpace = ViewUtils.getHeightForTranslucentStyle(getActivity()) + getResources().getDimension(R.dimen.quick_return_additional_padding);
        lp.setMargins(lp.leftMargin, (int) extraSpace, lp.rightMargin, lp.bottomMargin);
        mClearButton = (ImageButton) view.findViewById(R.id.clear);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSearchEditText.setText(null);
            }
        });
        handleBundle(savedInstanceState);
    }

    private void handleBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mQuality = pref.getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
        } else {
            mIsRestoring = true;
            mQuality = savedInstanceState.getString(KEY_QUALITY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
            mQuery = savedInstanceState.getString(KEY_QUERY, null);

            if (savedInstanceState.containsKey(KEY_ITEMS)) {
                ImgurBaseObject[] items = (ImgurBaseObject[]) savedInstanceState.getParcelableArray(KEY_ITEMS);
                int currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
                setupAdapter(new ArrayList<ImgurBaseObject>(Arrays.asList(items)));
                mGridView.setSelection(currentPosition);

                if (mListener != null) {
                    mListener.onLoadingComplete(PAGE);
                }
            }
        }
    }

    /**
     * Configures the previous search adapter to the AutoCompleteTextView
     */
    private void configurePreviousSearches() {
        String[] searches = mSql.getSubReddits();

        if (searches != null && searches.length > 0) {
            if (mSearchAdapter == null) {
                mSearchAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_dropdown_item_1line, Arrays.asList(searches));
                mSearchEditText.setAdapter(mSearchAdapter);
            } else {
                if (!mSearchAdapter.isEmpty()) {
                    mSearchAdapter.clear();
                }

                mSearchAdapter.addAll(searches);
                mSearchAdapter.notifyDataSetChanged();
            }
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

        mApiClient.doWork(ImgurBusEvent.EventType.GALLERY, mQuery, null);
    }

    /**
     * Returns the url for the Api request
     */
    private String getUrl() {
        return String.format(Endpoints.SUBREDDIT.getUrl(), mQuery.replaceAll("\\s", ""), mSort.getSort(), mCurrentPage);
    }

    private void setupAdapter(List<ImgurBaseObject> objects) {
        if (mAdapter == null) {
            mAdapter = new GalleryAdapter(getActivity(), objects, mQuality);
            mAnimationHeight = getResources().getDimension(R.dimen.quick_return_edit_text_height) +
                    getResources().getDimension(R.dimen.quick_return_additional_padding);
            View header = ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), (int) mAnimationHeight);
            mAnimationHeight += ViewUtils.getHeightForTranslucentStyle(getActivity());
            mGridView.addHeaderView(header);
            mGridView.setAdapter(mAdapter);
        } else {
            mAdapter.addItems(objects);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.GALLERY && event.id.equals(mQuery)) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                List<ImgurBaseObject> objects = null;

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);
                    mHasMore = arr.length() > 0;

                    if (arr.length() <= 0) {
                        mHandler.sendEmptyMessage(RedditHandler.MESSAGE_EMPTY_RESULT);
                        return;
                    }

                    objects = new ArrayList<ImgurBaseObject>();
                    // Only enter into the database if we received results
                    mSql.insertSubReddit(mQuery);


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
        Throwable e = event.getThrowable();

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        event.getThrowable().printStackTrace();
    }

    private class RedditHandler extends ImgurHandler {
        public static final int MESSAGE_AUTO_SEARCH = 2;

        public static final int MESSAGE_EMPTY_RESULT = 3;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_AUTO_SEARCH:
                    if (mAdapter != null) {
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    if (mListener != null) {
                        mListener.onLoadingStarted(PAGE);
                    }

                    mQuery = (String) msg.obj;
                    mCurrentPage = 0;
                    search();
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    break;

                case MESSAGE_EMPTY_RESULT:
                    // Only show the empty view when the list is truly empty
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        mMultiView.setEmptyText(R.id.empty, getString(R.string.reddit_empty, mQuery));
                        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                    }

                    break;

                case MESSAGE_ACTION_COMPLETE:
                    if (mListener != null) {
                        mListener.onLoadingComplete(PAGE);
                    }

                    List<ImgurBaseObject> objects = (List<ImgurBaseObject>) msg.obj;
                    configurePreviousSearches();
                    setupAdapter(objects);
                    mIsLoading = false;
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                    if (mCurrentPage == 0) {
                        mGridView.post(new Runnable() {
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
                            mListener.onError((Integer) msg.obj, PAGE);
                        }

                        mMultiView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    }

                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

        }
    }

    private PauseOnScrollListener mScrollListener = new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
            new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    // Hide the actionbar when scrolling down, show when scrolling up
                    if (firstVisibleItem > mPreviousItem) {
                        if (mListener != null) {
                            mListener.oHideActionBar(false);
                        }

                        if (mInputIsShowing) {
                            mInputIsShowing = false;
                            mQuickReturnView.animate().translationY(-mAnimationHeight).setInterpolator(new DecelerateInterpolator()).setDuration(500L);
                        }
                    } else if (firstVisibleItem < mPreviousItem) {
                        if (mListener != null) {
                            mListener.oHideActionBar(true);
                        }

                        if (!mInputIsShowing) {
                            mInputIsShowing = true;
                            mQuickReturnView.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).setDuration(500L);
                        }
                    }

                    mPreviousItem = firstVisibleItem;

                    // Load more items when hey get to the end of the list
                    if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount && !mIsLoading && mHasMore) {
                        mIsLoading = true;
                        mCurrentPage++;
                        search();
                    }
                }
            }
    );

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_QUALITY, mQuality);
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putString(KEY_QUERY, mQuery);

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArray(KEY_ITEMS, mAdapter.getAllItems());
            outState.putInt(KEY_CURRENT_POSITION, mGridView.getFirstVisiblePosition());
        }

        super.onSaveInstanceState(outState);
    }
}
