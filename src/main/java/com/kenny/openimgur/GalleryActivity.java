package com.kenny.openimgur;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.AsyncExecutor;
import de.greenrobot.event.util.ThrowableFailureEvent;

public class GalleryActivity extends BaseActivity {
    public enum GallerySection {
        HOT("hot"),
        TOP("top"),
        USER("user");

        private String mSection;

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

        private String mSort;

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

    private ImageButton mUploadButton;

    private MultiStateView mMultiView;

    private ImageView mErrorImage;

    private RobotoTextView mErrorMessage;

    private GalleryAdapter mAdapter;

    private int mCurrentPage = 0;

    private boolean mIsLoading = false;

    private ApiClient mApiClient;

    private int mPreviousItem;

    private boolean mIsABShowing = true;

    private boolean mFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        mUploadButton = (ImageButton) findViewById(R.id.uploadButton);
        mMultiView = (MultiStateView) findViewById(R.id.multiStateView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        mGridView = (HeaderGridView) findViewById(R.id.grid);
        mGridView.addHeaderView(createEmptyGridHeader());
        mErrorImage = (ImageView) mMultiView.findViewById(R.id.errorImage);
        mErrorMessage = (RobotoTextView) mMultiView.findViewById(R.id.errorMessage);

        // Pauses the ImageLoader from loading when the user is flinging the list
        PauseOnScrollListener scrollListner = new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {

                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        // Hide the actionbar when scrolling down, show when scrolling up
                        if (firstVisibleItem > mPreviousItem && mIsABShowing) {
                            mIsABShowing = false;
                            getActionBar().hide();
                            animateUploadButton(true);
                        } else if (firstVisibleItem < mPreviousItem && !mIsABShowing) {
                            mIsABShowing = true;
                            getActionBar().show();
                            animateUploadButton(false);
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
        mGridView.setOnScrollListener(scrollListner);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                int headerSize = mGridView.getNumColumns() * mGridView.getHeaderViewCount();
                int actualPosition = position - headerSize;

                // Don't respond to the header being clicked
                if (actualPosition >= 0) {
                    startActivity(ViewActivity.createIntent(getApplicationContext(), (ArrayList<ImgurBaseObject>) mAdapter.getItems(), actualPosition));
                }
            }
        });

        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "TODO Image Upload", Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState != null) {
            mSort = GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT));
            mSection = GallerySection.getSectionFromString(savedInstanceState.getString(KEY_GALLERY_SECTION));
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);
            List<ImgurBaseObject> data = savedInstanceState.getParcelableArrayList(KEY_DATA);
            mApiClient = new ApiClient(getGalleryUrl(), ApiClient.HttpRequest.GET);

            if (data != null && !data.isEmpty()) {
                String quality = app.getPreferences().getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, String.valueOf(SettingsActivity.THUMBNAIL_QUALITY_LOW));
                mAdapter = new GalleryAdapter(getApplicationContext(), app.getImageLoader(), data, Integer.parseInt(quality));
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
            case R.id.filter:
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
                return true;

            case R.id.refresh:
                if (mApiClient != null) {
                    mApiClient.clearCache();
                }

                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                mCurrentPage = 0;
                mIsLoading = true;

                if (mAdapter != null) {
                    mAdapter.clear();
                }

                getGallery();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static class GalleryAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        private ImageLoader mImageLoader;

        private List<ImgurBaseObject> mObjects;

        private String mThumbnailQuality;

        public GalleryAdapter(Context context, ImageLoader imageLoader, List<ImgurBaseObject> objects, int quality) {
            mInflater = LayoutInflater.from(context);
            mImageLoader = imageLoader;
            this.mObjects = objects;

            switch (quality) {
                case SettingsActivity.THUMBNAIL_QUALITY_MEDIUM:
                    mThumbnailQuality = ImgurPhoto.THUMBNAIL_MEDIUM;
                    break;

                case SettingsActivity.THUMBNAIL_QUALITY_HIGH:
                    mThumbnailQuality = ImgurPhoto.THUMBNAIL_LARGE;
                    break;

                case SettingsActivity.THUMBNAIL_QUALITY_LOW:
                default:
                    mThumbnailQuality = ImgurPhoto.THUMBNAIL_SMALL;
            }
        }

        /**
         * Clears all the items from the adapter
         */
        public void clear() {
            if (mObjects != null) {
                mObjects.clear();
            }

            notifyDataSetChanged();
        }

        /**
         * Adds an object to the adapter
         *
         * @param obj
         */
        public void addItem(ImgurBaseObject obj) {
            if (mObjects == null) {
                mObjects = new ArrayList<ImgurBaseObject>();
            }

            mObjects.add(obj);
            notifyDataSetChanged();
        }

        /**
         * Adds a list of items into the current list
         *
         * @param items
         */
        public void addItems(List<ImgurBaseObject> items) {
            if (mObjects == null) {
                mObjects = items;
            } else {
                for (ImgurBaseObject obj : items) {
                    mObjects.add(obj);
                }
            }

            notifyDataSetChanged();
        }

        /**
         * Returns the entire list of objects
         *
         * @return
         */
        public List<ImgurBaseObject> getItems() {
            return mObjects;
        }

        @Override
        public int getCount() {
            if (mObjects != null) {
                return mObjects.size();
            }
            return 0;
        }

        @Override
        public ImgurBaseObject getItem(int position) {
            if (mObjects != null) {
                return mObjects.get(position);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.gallery_item, parent, false);
                holder = new ViewHolder();
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.tv = (RobotoTextView) convertView.findViewById(R.id.score);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ImgurBaseObject obj = getItem(position);
            mImageLoader.cancelDisplayTask(holder.image);
            String photoUrl = null;

            // Get the appropriate photo to display
            if (obj instanceof ImgurPhoto) {
                photoUrl = ((ImgurPhoto) obj).getThumbnail(mThumbnailQuality);
            } else {
                photoUrl = ((ImgurAlbum) obj).getCoverUrl(mThumbnailQuality);
            }

            mImageLoader.displayImage(photoUrl, holder.image);
            holder.tv.setText(obj.getScore() + " " + holder.tv.getContext().getString(R.string.points));
            return convertView;
        }

        class ViewHolder {
            ImageView image;

            RobotoTextView tv;
        }

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
            outState.putParcelableArrayList(KEY_DATA, (ArrayList<ImgurBaseObject>) mAdapter.getItems());
            outState.putInt(KEY_LIST_POSITION, mGridView.getFirstVisiblePosition());
        }

        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putString(KEY_GALLERY_SECTION, mSection.getSection());
    }

    /**
     * Creates the empty header for the gridview
     *
     * @return
     */
    private View createEmptyGridHeader() {
        View v = View.inflate(getApplicationContext(), R.layout.empty_header, null);
        final TypedArray styledAttributes = getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int abHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, abHeight);

        // On 4.4 + devices, we need to account for the status bar
        if (OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                lp.height += getResources().getDimensionPixelSize(resourceId);
            }
        }

        v.setLayoutParams(lp);
        return v;
    }

    /**
     * Animates the upload button
     *
     * @param shouldHide If the button should be hidden
     */
    private void animateUploadButton(boolean shouldHide) {
        mUploadButton.clearAnimation();

        if (shouldHide) {
            // Normally we would just do the buttons height, but since in KitKat the nav bar is transparent,
            // We need to add some extra space to fully hide it
            mUploadButton.animate().translationY(mUploadButton.getHeight() * 2).setDuration(350).start();
        } else {
            mUploadButton.animate().translationY(0).setDuration(350).start();
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
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getErrorCodeStringResource(statusCode));
                }

            } catch (JSONException e) {
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
            }
        }
    }

    /**
     * Event Method that is fired if EventBus throws an error
     *
     * @param event
     */
    public void onEventMainThread(ThrowableFailureEvent event) {
        ImageUtil.setErrorImage(mErrorImage, System.currentTimeMillis());
        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
        event.getThrowable().printStackTrace();
    }

    /**
     * Returns the string resource for the given error code
     *
     * @param statusCode
     * @return
     */
    @StringRes
    private int getErrorCodeStringResource(int statusCode) {
        switch (statusCode) {
            case ApiClient.STATUS_FORBIDDEN:
                return R.string.error_403;

            case ApiClient.STATUS_INVALID_PERMISSIONS:
                return R.string.error_401;

            case ApiClient.STATUS_RATING_LIMIT:
                return R.string.error_429;

            case ApiClient.STATUS_OVER_CAPACITY:
                return R.string.error_503;

            case ApiClient.STATUS_EMPTY_RESPONSE:
                return R.string.error_800;

            case ApiClient.STATUS_IO_EXCEPTION:
            case ApiClient.STATUS_JSON_EXCEPTION:
            case ApiClient.STATUS_NOT_FOUND:
            case ApiClient.STATUS_INTERNAL_ERROR:
            case ApiClient.STATUS_INVALID_PARAM:
            default:
                return R.string.error_generic;
        }
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

        AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
            @Override
            public void run() throws Exception {
                mApiClient.doWork(ImgurBusEvent.EventType.GALLERY, null);
            }
        });
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
                    mUploadButton.setVisibility(View.VISIBLE);
                    List<ImgurBaseObject> gallery = (List<ImgurBaseObject>) msg.obj;

                    if (mAdapter == null) {
                        String quality = app.getPreferences().getString(SettingsActivity.THUMBNAIL_QUALITY_KEY,
                                String.valueOf(SettingsActivity.THUMBNAIL_QUALITY_LOW));

                        mAdapter = new GalleryAdapter(getApplicationContext(), app.getImageLoader(), gallery,
                                Integer.parseInt(quality));

                        mGridView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(gallery);
                        mAdapter.notifyDataSetChanged();
                    }

                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case MESSAGE_ACTION_FAILED:
                    mUploadButton.setVisibility(View.GONE);
                    ImageUtil.setErrorImage(mErrorImage, System.currentTimeMillis());
                    mErrorMessage.setText((Integer) msg.obj);
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;
            }

            mIsLoading = false;
            super.handleMessage(msg);
        }
    };
}
