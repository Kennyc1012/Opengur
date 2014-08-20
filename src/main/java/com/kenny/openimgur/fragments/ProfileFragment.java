package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.classes.TabActivityListener;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ViewUtils;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 8/18/14.
 */
public class ProfileFragment extends Fragment implements ImgurListener {
    private static final String TAG = ProfileFragment.class.getSimpleName();

    private static final String KEY_ENDPOINT = "endpoint";

    private static final String KEY_CURRENT_POSITION = "position";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_QUALITY = "quality";

    private static final String KEY_CURRENT_PAGE = "page";

    private static final String KEY_USERNAME = "username";

    private static final String KEY_FROM_MAIN = "fromMain";

    private static final int PAGE = 2;

    private MultiStateView mMultiView;

    private HeaderGridView mGridView;

    private WebView mWebView;

    private int mCurrentPage = 0;

    private Endpoints mCurrentEndpoint = Endpoints.ACCOUNT_GALLERY_FAVORITES;

    private GalleryAdapter mAdapter;

    private ImgurUser mSelectedUser;

    private ApiClient mApiClient;

    private TabActivityListener mListener;

    private String mQuality;

    private boolean mIsLoading = false;

    private boolean mHasMore = true;

    private boolean mDidAddProfileToErrorView = false;

    private boolean mFromMain = true;

    private int mPreviousItem;

    public static ProfileFragment createInstance(@Nullable String userName, boolean isFromMain) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putBoolean(KEY_FROM_MAIN, isFromMain);

        if (!TextUtils.isEmpty(userName)) {

            args.putString(KEY_USERNAME, userName);
            fragment.setArguments(args);
        }

        return fragment;
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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiView);
        mGridView = (HeaderGridView) mMultiView.findViewById(R.id.grid);
        mGridView.setOnScrollListener(mScrollListner);
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

        handleBundle(savedInstanceState, getArguments());
    }

    /**
     * Handles the arguments past to the fragment
     *
     * @param args
     */
    private void handleArguments(Bundle args) {
        mFromMain = args.getBoolean(KEY_FROM_MAIN, false);
        OpenImgurApp app = OpenImgurApp.getInstance(getActivity());

        if (args != null && args.containsKey(KEY_USERNAME)) {
            Log.v(TAG, "User present in Bundle extras");
            String username = args.getString(KEY_USERNAME);
            mSelectedUser = app.getSql().getUser(username);
            configUser(username);
        } else if (app.getUser() != null) {
            Log.v(TAG, "User already logged in");
            mSelectedUser = app.getUser();
            configUser(null);
        } else {
            Log.v(TAG, "No user present. Showing Login screen");
            configWebView();
        }
    }

    private void handleBundle(Bundle savedInstanceState, Bundle args) {
        if (savedInstanceState == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            mQuality = pref.getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
            handleArguments(args);
        } else {
            mQuality = savedInstanceState.getString(KEY_QUALITY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
            mCurrentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, 0);

            if (savedInstanceState.containsKey(KEY_ITEMS) && savedInstanceState.containsKey(KEY_USERNAME)) {
                mSelectedUser = savedInstanceState.getParcelable(KEY_USERNAME);
                mCurrentEndpoint = savedInstanceState.getString(KEY_ENDPOINT, null).equals(Endpoints.ACCOUNT_GALLERY_FAVORITES.getUrl()) ? Endpoints.ACCOUNT_GALLERY_FAVORITES : Endpoints.ACCOUNT_SUBMISSIONS;
                ImgurBaseObject[] items = (ImgurBaseObject[]) savedInstanceState.getParcelableArray(KEY_ITEMS);
                int currentPosition = savedInstanceState.getInt(KEY_CURRENT_POSITION, 0);
                mAdapter = new GalleryAdapter(getActivity(), new ArrayList<ImgurBaseObject>(Arrays.asList(items)), mQuality);
                mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                mGridView.addHeaderView(ViewUtils.getProfileView(mSelectedUser, getActivity(), mMultiView, this));
                mGridView.setAdapter(mAdapter);
                mGridView.setSelection(currentPosition);

                if (mListener != null) {
                    mListener.onLoadingComplete(PAGE);
                }
            } else {
                handleArguments(args);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!mFromMain) {
                    getActivity().finish();
                }

                return true;

            case R.id.favorites:
            case R.id.submissions:
                mCurrentPage = 0;
                mCurrentEndpoint = item.getItemId() == R.id.favorites ? Endpoints.ACCOUNT_GALLERY_FAVORITES : Endpoints.ACCOUNT_SUBMISSIONS;

                if (mAdapter != null) {
                    mAdapter.clear();
                    mAdapter.notifyDataSetChanged();
                }

                getActivity().invalidateOptionsMenu();
                getGalleryData();
                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                return true;

            case R.id.logout:
                final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
                dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
                dialog.setView(new PopupDialogViewBuilder(getActivity()).setTitle(R.string.logout)
                        .setMessage(R.string.logout_confirm).setNegativeButton(R.string.cancel, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dialog.dismiss();
                            }
                        }).setPositiveButton(R.string.yes, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                OpenImgurApp.getInstance(getActivity()).onLogout();
                                getActivity().invalidateOptionsMenu();
                                dialog.dismiss();
                            }
                        }).build());
                dialog.show();
                return true;

            case R.id.refresh:
                // Force the app to fetch new data for the user
                String username = mSelectedUser.getUsername();
                mSelectedUser = null;
                configUser(username);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mSelectedUser == null) {
            menu.removeItem(R.id.favorites);
            menu.removeItem(R.id.submissions);
            menu.removeItem(R.id.logout);
            menu.removeItem(R.id.refresh);
        } else {

            if (!mSelectedUser.isSelf()) {
                //  menu.removeItem(R.id.logout);
            }

            if (mCurrentEndpoint == Endpoints.ACCOUNT_SUBMISSIONS) {
                menu.removeItem(R.id.submissions);
                menu.findItem(R.id.favorites).setShowAsAction(mFromMain ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_IF_ROOM);
            } else {
                menu.removeItem(R.id.favorites);
                menu.findItem(R.id.submissions).setShowAsAction(mFromMain ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroyView() {
        mWebView = null;
        mMultiView = null;
        mHandler.removeCallbacksAndMessages(null);

        if (mAdapter != null) {
            mAdapter.clear();
            mAdapter = null;
        }

        super.onDestroyView();
    }

    /**
     * Configures the webview to handle a user logging in
     */
    private void configWebView() {
        if (mListener != null) {
            mListener.onLoadingStarted(PAGE);
        }

        mWebView = (WebView) mMultiView.findViewById(R.id.loginWebView);
        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        // Add the empty space so the webview isnt cut off by the action bar
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mWebView.getLayoutParams();
        lp.setMargins(0, ViewUtils.getHeightForTranslucentStyle(getActivity()), 0, 0);
        mWebView.loadUrl(Endpoints.LOGIN.getUrl());
        CookieManager.getInstance().setAcceptCookie(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("#")) {
                    // We will extract the info from the callback url
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    String[] outerSplit = url.split("\\#")[1].split("\\&");
                    String username = null;
                    String accessToken = null;
                    String refreshToken = null;
                    long accessTokenExpiration = 0;
                    int index = 0;

                    for (String s : outerSplit) {
                        String[] innerSplit = s.split("\\=");

                        switch (index) {
                            // Access Token
                            case 0:
                                accessToken = innerSplit[1];
                                break;

                            // Access Token Expiration
                            case 1:
                                long expiresIn = Long.parseLong(innerSplit[1]);
                                accessTokenExpiration = System.currentTimeMillis() + (expiresIn * DateUtils.SECOND_IN_MILLIS);
                                break;

                            // Token Type, not using
                            case 2:
                                //NO OP
                                break;

                            // Refresh Token
                            case 3:
                                refreshToken = innerSplit[1];
                                break;

                            // Username
                            case 4:
                                username = innerSplit[1];
                                break;
                        }

                        index++;
                    }

                    // Make sure that everything was set
                    if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(accessToken) &&
                            !TextUtils.isEmpty(refreshToken) && accessTokenExpiration > 0) {
                        ImgurUser newUser = new ImgurUser(username, accessToken, refreshToken, accessTokenExpiration);
                        OpenImgurApp.getInstance(getActivity()).setUser(newUser);
                        mSelectedUser = newUser;
                        Log.v(TAG, "User " + newUser.getUsername() + " logged in");
                        String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), newUser.getUsername());
                        mApiClient = new ApiClient(detailsUrls, ApiClient.HttpRequest.GET);
                        mApiClient.doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, null, null);
                        mWebView.clearHistory();
                        mWebView.clearCache(true);
                        mWebView.clearFormData();
                    } else {
                        // TODO Error
                    }

                } else {
                    // Didn't get our tokens from the response, they probably denied accessed, just reshow the login page
                    Log.w(TAG, "URL didn't contain a '#'. User denied access");
                    mWebView.loadUrl(Endpoints.LOGIN.getUrl());
                }

                return false;
            }
        });
    }

    /**
     * Checks the database if there is cached data for the user and whether data should be loaded if it is old
     *
     * @param username
     */
    private void configUser(String username) {
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        // Load the new user data if we haven't viewed the user within 24 hours
        if (mSelectedUser == null || System.currentTimeMillis() - mSelectedUser.getLastSeen() >= DateUtils.DAY_IN_MILLIS) {
            Log.v(TAG, "Selected user is null or data is too old, fetching new data");
            String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), mSelectedUser == null ? username : mSelectedUser.getUsername());

            if (mApiClient == null) {
                mApiClient = new ApiClient(detailsUrls, ApiClient.HttpRequest.GET);
            } else {
                mApiClient.setRequestType(ApiClient.HttpRequest.GET);
                mApiClient.setUrl(detailsUrls);
            }

            mIsLoading = true;
            mApiClient.doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, null, null);
        } else {
            Log.v(TAG, "Selected user present in database and has valid data, fetching gallery");
            getGalleryData();
        }
    }

    /**
     * Gets either the users favorites or submissions passed on the current selection
     */
    private void getGalleryData() {
        String url = null;
        if (mCurrentEndpoint == Endpoints.ACCOUNT_GALLERY_FAVORITES) {
            url = String.format(mCurrentEndpoint.getUrl(), mSelectedUser.getUsername());
        } else {
            url = String.format(mCurrentEndpoint.getUrl(), mSelectedUser.getUsername(), mCurrentPage);
        }

        if (mApiClient == null) {
            mApiClient = new ApiClient(url, ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setRequestType(ApiClient.HttpRequest.GET);
            mApiClient.setUrl(url);
        }

        mIsLoading = true;
        mApiClient.doWork(ImgurBusEvent.EventType.ACCOUNT_GALLERY_FAVORITES, null, null);
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int status = event.json.getInt(ApiClient.KEY_STATUS);

            if (status == ApiClient.STATUS_OK) {
                switch (event.eventType) {
                    case PROFILE_DETAILS:
                        if (mSelectedUser == null) {
                            mSelectedUser = new ImgurUser(event.json);
                        } else {
                            mSelectedUser.parseJsonForValues(event.json);
                        }

                        OpenImgurApp.getInstance(getActivity()).getSql().insertProfile(mSelectedUser);
                        getGalleryData();
                        break;

                    case ACCOUNT_GALLERY_FAVORITES:
                    case ACCOUNT_SUBMISSIONS:
                        List<ImgurBaseObject> objects = new ArrayList<ImgurBaseObject>();
                        JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);

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
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onEventMainThread(ThrowableFailureEvent event) {
        // TODO Errors
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ImgurHandler.MESSAGE_ACTION_COMPLETE:
                    if (mListener != null) {
                        mListener.onLoadingComplete(PAGE);
                    }

                    List<ImgurBaseObject> objects = (List<ImgurBaseObject>) msg.obj;
                    mHasMore = !objects.isEmpty() && mCurrentEndpoint == Endpoints.ACCOUNT_SUBMISSIONS;

                    if (objects.size() > 0) {
                        if (mAdapter == null) {
                            mAdapter = new GalleryAdapter(getActivity(), objects, mQuality);
                            mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                            mGridView.addHeaderView(ViewUtils.getProfileView(mSelectedUser, getActivity(), mMultiView, ProfileFragment.this));
                            mGridView.setAdapter(mAdapter);
                        } else {
                            mAdapter.addItems(objects);
                            mAdapter.notifyDataSetChanged();
                        }
                    } else if (mAdapter == null || mAdapter.isEmpty()) {
                        if (!mDidAddProfileToErrorView) {
                            mDidAddProfileToErrorView = true;
                            LinearLayout container = (LinearLayout) mMultiView.getErrorView().findViewById(R.id.container);
                            container.addView(ViewUtils.getProfileView(mSelectedUser, getActivity(), mMultiView, ProfileFragment.this), 0);
                            container.addView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0), 0);
                        }

                        String errorMessage = mCurrentEndpoint == Endpoints.ACCOUNT_SUBMISSIONS ?
                                getString(R.string.profile_no_submissions, mSelectedUser.getUsername()) :
                                getString(R.string.profile_no_favorites, mSelectedUser.getUsername());

                        mMultiView.setErrorText(R.id.errorMessage, errorMessage);
                    }

                    getActivity().invalidateOptionsMenu();
                    mMultiView.setViewState(mAdapter == null || mAdapter.isEmpty() ? MultiStateView.ViewState.ERROR : MultiStateView.ViewState.CONTENT);
                    if (mCurrentEndpoint == Endpoints.ACCOUNT_SUBMISSIONS && mCurrentPage == 0) {
                        mMultiView.post(new Runnable() {
                            @Override
                            public void run() {
                                mGridView.setSelection(0);
                            }
                        });
                    }

                    break;

                case ImgurHandler.MESSAGE_ACTION_FAILED:

                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }

            mIsLoading = false;
        }
    };

    @Override
    public void onPhotoTap(ImageView parent) {
        // NOOP
    }

    @Override
    public void onPlayTap(ProgressBar prog, ImageView image, ImageButton play) {
        // NOOP
    }

    @Override
    public void onLinkTap(View view, @Nullable String url) {
        if (!TextUtils.isEmpty(url)) {
            if (url.matches(REGEX_IMAGE_URL)) {
                PopupImageDialogFragment.getInstance(url, url.endsWith(".gif"), true)
                        .show(getFragmentManager(), "popup");
            } else if (url.matches(REGEX_IMGUR_IMAGE)) {
                String[] split = url.split("\\/");
                PopupImageDialogFragment.getInstance(split[split.length - 1], false, false)
                        .show(getFragmentManager(), "popup");
            } else {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(getActivity(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onViewRepliesTap(View view) {
        // NOOP
    }

    private PauseOnScrollListener mScrollListner = new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
            new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {

                }

                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                    if (firstVisibleItem > mPreviousItem) {
                        if (mListener != null) {
                            mListener.oHideActionBar(false);
                        }
                    } else if (firstVisibleItem < mPreviousItem) {
                        if (mListener != null) {
                            mListener.oHideActionBar(true);
                        }
                    }

                    mPreviousItem = firstVisibleItem;

                    // Load more items when hey get to the end of the list
                    if (totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount && !mIsLoading && mHasMore) {
                        mIsLoading = true;
                        mCurrentPage++;
                        getGalleryData();
                    }
                }
            }
    );

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_QUALITY, mQuality);
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        outState.putString(KEY_ENDPOINT, mCurrentEndpoint.getUrl());
        outState.putParcelable(KEY_USERNAME, mSelectedUser);

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArray(KEY_ITEMS, mAdapter.getAllitems());
            outState.putInt(KEY_CURRENT_POSITION, mGridView.getFirstVisiblePosition());
        }

        super.onSaveInstanceState(outState);
    }
}
