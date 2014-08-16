package com.kenny.openimgur;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ViewUtils;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

public class GalleryActivity extends BaseActivity implements View.OnClickListener {
    public enum GallerySection {
        HOT("hot"),
        TOP("top"),
        USER("user");

        private final String mSection;

        private GallerySection(String s) {
            mSection = s;
        }

        public String getSection() {
            return mSection;
        }

        /**
         * Returns the String id that corresponds to the section
         *
         * @param section
         * @return
         */
        public static int getStringId(GallerySection section) {
            switch (section) {
                case USER:
                    return R.string.user_sub;

                case TOP:
                    return R.string.top_score;

                case HOT:
                default:
                    return R.string.viral;
            }
        }

        /**
         * Returns an array of items to populate the dropdown spinner
         *
         * @param context
         * @return
         */
        public static String[] getNavArray(Context context) {
            GallerySection[] items = GallerySection.values();
            String[] nav = new String[items.length];

            for (int i = 0; i < items.length; i++) {
                nav[i] = context.getString(getStringId(items[i]));
            }
            return nav;
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
            } else if (TOP.getSection().equals(section)) {
                return TOP;
            }

            return USER;

        }

        /**
         * Returns the position the item falls in the navigation list
         *
         * @return
         */
        public int getNavListPosition() {
            GallerySection[] sections = GallerySection.values();
            for (int i = 0; i < sections.length; i++) {
                if (this == sections[i]) {
                    return i;
                }
            }
            return 0;
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

    private static final String KEY_DATA = "data";

    private static final String KEY_LIST_POSITION = "listPosition";

    private static final String KEY_CURRENT_PAGE = "currentPage";

    private static final String KEY_GALLERY_SECTION = "gallerySection";

    private static final String KEY_SORT = "sort";

    private GallerySection mSection = GallerySection.HOT;

    private GallerySort mSort = GallerySort.TIME;

    private HeaderGridView mGridView;

    private View mUploadMenu;

    private FloatingActionButton mUploadButton;

    private FloatingActionButton mCameraUpload;

    private FloatingActionButton mGalleryUpload;

    private FloatingActionButton mLinkUpload;

    private MultiStateView mMultiView;

    private RobotoTextView mErrorMessage;

    private GalleryAdapter mAdapter;

    private int mCurrentPage = 0;

    private boolean mIsLoading = false;

    private ApiClient mApiClient;

    private int mPreviousItem;

    private boolean mIsABShowing = true;

    private boolean mFirstLoad = true;

    private boolean uploadMenuOpen = false;

    private boolean isLandscape = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        mUploadMenu = findViewById(R.id.uploadMenu);
        mMultiView = (MultiStateView) findViewById(R.id.multiStateView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        mGridView = (HeaderGridView) findViewById(R.id.grid);
        mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getApplicationContext(),0));
        mErrorMessage = (RobotoTextView) mMultiView.findViewById(R.id.errorMessage);

        // Pauses the ImageLoader from loading when the user is flinging the list
        PauseOnScrollListener scrollListener = new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        // Hide the actionbar when scrolling down, show when scrolling up
                        if (firstVisibleItem > mPreviousItem && mIsABShowing && !uploadMenuOpen) {
                            mIsABShowing = false;
                            getActionBar().hide();
                            animateUploadMenuButton(true);
                        } else if (firstVisibleItem < mPreviousItem && !mIsABShowing) {
                            mIsABShowing = true;
                            getActionBar().show();
                            animateUploadMenuButton(false);
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
        mGridView.setOnScrollListener(scrollListener);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                int headerSize = mGridView.getNumColumns() * mGridView.getHeaderViewCount();
                int adapterPosition = position - headerSize;

                // Don't respond to the header being clicked
                if (adapterPosition >= 0) {
                    startActivity(ViewActivity.createIntent(getApplicationContext(), mAdapter.getItems(), adapterPosition));
                }
            }
        });

        mUploadButton = (FloatingActionButton) mUploadMenu.findViewById(R.id.uploadButton);
        mLinkUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.linkUpload);
        mCameraUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.cameraUpload);
        mGalleryUpload = (FloatingActionButton) mUploadMenu.findViewById(R.id.galleryUpload);
        mUploadButton.setOnClickListener(this);
        mLinkUpload.setOnClickListener(this);
        mCameraUpload.setOnClickListener(this);
        mGalleryUpload.setOnClickListener(this);

        if (savedInstanceState != null) {
            mSort = GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT));
            mSection = GallerySection.getSectionFromString(savedInstanceState.getString(KEY_GALLERY_SECTION));
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
            List<ImgurBaseObject> data = savedInstanceState.getParcelableArrayList(KEY_DATA);
            mApiClient = new ApiClient(getGalleryUrl(), ApiClient.HttpRequest.GET);

            if (data != null && !data.isEmpty()) {
                String quality = app.getPreferences().getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
                mAdapter = new GalleryAdapter(getApplicationContext(), app.getImageLoader(), data, quality);
                mGridView.setAdapter(mAdapter);
                mGridView.setSelection(savedInstanceState.getInt(KEY_LIST_POSITION, 0));
                initActionBar(getActionBar());
                mFirstLoad = false;
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            } else {
                getGallery();
            }
        } else {
            SharedPreferences pref = app.getPreferences();
            mSort = GallerySort.getSortFromString(pref.getString("sort", null));
            mSection = GallerySection.getSectionFromString(pref.getString("section", null));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gallery, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
           /* case R.id.filter:
                AlertDialog.Builder builder = new AlertDialog.Builder(GalleryActivity.this);
                builder.setTitle(R.string.filter_popup_title);
                builder.setItems(GallerySort.getPopupArray(getApplicationContext()), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mSort = GallerySort.values()[i];
                        mCurrentPage = 0;
                        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                        mIsLoading = true;

                        if (mAdapter != null) {
                            mAdapter.clear();
                            mAdapter.notifyDataSetChanged();
                        }

                        getGallery();
                    }
                }).show();

                return true;

            case R.id.settings:
                startActivity(SettingsActivity.createIntent(getApplicationContext()));
                return true;*/

            case R.id.refresh:
                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                mCurrentPage = 0;
                mIsLoading = true;

                if (mAdapter != null) {
                    mAdapter.clear();
                    mAdapter.notifyDataSetChanged();
                }

                getGallery();
                return true;

            case R.id.profile:
                startActivity(ProfileActivity.createIntent(getApplicationContext(), null));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called after data is loaded upon first opening the app
     *
     * @param actionBar
     */
    private void initActionBar(ActionBar actionBar) {
        SpinnerAdapter mSpinnerAdapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_dropdown_item, GallerySection.getNavArray(getApplicationContext()));
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(mSpinnerAdapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int position, long l) {
                GallerySection section = GallerySection.values()[position];
                if (section != mSection) {
                    mIsLoading = true;
                    mCurrentPage = 0;
                    mSection = section;

                    if (mAdapter != null) {
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    mUploadMenu.setVisibility(View.GONE);
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    getGallery();
                    return true;
                }
                return false;
            }
        });

        actionBar.setSelectedNavigationItem(mSection.getNavListPosition());
    }

    @Override
    public void onDestroy() {
        if (mAdapter != null && isFinishing()) {
            mAdapter.clear();
            mAdapter = null;
        }

        mApiClient = null;
        mGridView = null;
        mMultiView = null;
        mCameraUpload = null;
        mGalleryUpload = null;
        mLinkUpload = null;
        mUploadButton = null;
        mErrorMessage = null;
        mHandler.removeCallbacksAndMessages(null);
        SharedPreferences.Editor edit = app.getPreferences().edit();
        edit.putString("section", mSection.getSection()).apply();
        edit.putString("sort", mSort.getSort()).apply();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        if (mFirstLoad || mAdapter == null || mAdapter.isEmpty()) {
            mIsLoading = true;
            getGallery();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArray(KEY_DATA, mAdapter.getItems());
            outState.putInt(KEY_LIST_POSITION, mGridView.getFirstVisiblePosition());
        }

        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_GALLERY_SECTION, mSection.getSection());
    }

    /**
     * Animates the upload button
     *
     * @param shouldHide If the button should be hidden
     */
    private void animateUploadMenuButton(boolean shouldHide) {
        mUploadButton.clearAnimation();

        if (shouldHide) {
            // Add extra distance to the hiding of the button if on KitKat due to the translucent nav bar
            float hideDistance = OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT ? mUploadButton.getHeight() * 2 : mUploadButton.getHeight();
            mUploadMenu.animate().setInterpolator(new DecelerateInterpolator()).translationY(hideDistance).setDuration(350).start();
        } else {
            mUploadMenu.animate().setInterpolator(new DecelerateInterpolator()).translationY(0).setDuration(350).start();
        }
    }

    /**
     * Animates the opening/closing of the Upload button
     */
    private void animateUploadMenu() {
        int uploadButtonHeight = mUploadButton.getHeight();
        int menuButtonHeight = mCameraUpload.getHeight();
        AnimatorSet set = new AnimatorSet().setDuration(500L);
        String translation = isLandscape ? "translationX" : "translationY";

        if (!uploadMenuOpen) {
            uploadMenuOpen = true;

            set.playTogether(
                    ObjectAnimator.ofFloat(mCameraUpload, translation, 0, (uploadButtonHeight + 25) * -1),
                    ObjectAnimator.ofFloat(mLinkUpload, translation, 0, ((2 * menuButtonHeight) + uploadButtonHeight + 75) * -1),
                    ObjectAnimator.ofFloat(mGalleryUpload, translation, (menuButtonHeight + uploadButtonHeight + 50) * -1)
            );

            set.setInterpolator(new OvershootInterpolator());
            set.start();
        } else {
            uploadMenuOpen = false;

            set.playTogether(
                    ObjectAnimator.ofFloat(mCameraUpload, translation, 0),
                    ObjectAnimator.ofFloat(mLinkUpload, translation, 0),
                    ObjectAnimator.ofFloat(mGalleryUpload, translation, 0)
            );

            set.setInterpolator(new DecelerateInterpolator());
            set.start();
        }
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.GALLERY) {
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

    /**
     * Queries the Api for Gallery items
     */
    private void getGallery() {
        if (mApiClient == null) {
            mApiClient = new ApiClient(getGalleryUrl(), ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setUrl(getGalleryUrl());
        }

        mApiClient.doWork(ImgurBusEvent.EventType.GALLERY, null, null);
    }

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            if (mFirstLoad) {
                initActionBar(getActionBar());
                mFirstLoad = false;
            }

            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    mUploadMenu.setVisibility(View.VISIBLE);
                    List<ImgurBaseObject> gallery = (List<ImgurBaseObject>) msg.obj;

                    if (mAdapter == null) {
                        String quality = app.getPreferences().getString(SettingsActivity.THUMBNAIL_QUALITY_KEY,
                                SettingsActivity.THUMBNAIL_QUALITY_LOW);

                        mAdapter = new GalleryAdapter(getApplicationContext(), app.getImageLoader(), gallery, quality);
                        mGridView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(gallery);
                        mAdapter.notifyDataSetChanged();
                    }

                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case MESSAGE_ACTION_FAILED:
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        mUploadMenu.setVisibility(View.GONE);
                        mErrorMessage.setText((Integer) msg.obj);
                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    }

                    break;
            }

            mIsLoading = false;
            super.handleMessage(msg);
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.uploadButton:
                animateUploadMenu();
                break;

            case R.id.cameraUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_CAMERA));
                break;

            case R.id.galleryUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_GALLERY));
                break;

            case R.id.linkUpload:
                startActivity(UploadActivity.createIntent(getApplicationContext(), UploadActivity.UPLOAD_TYPE_LINK));
                break;
        }
    }
}
