package com.kenny.openimgur;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ViewUtils;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 7/26/14.
 */
public class ProfileActivity extends BaseActivity implements ImgurListener {
    public static final String KEY_USERNAME = "username";

    private MultiStateView mMultiView;

    private WebView mWebView;

    private ApiClient mApiClient;

    private ImgurUser mSelectedUser;

    private int mCurrentPage = 0;

    private Endpoints mCurrentEndpoint = Endpoints.ACCOUNT_GALLERY_FAVORITES;

    private GalleryAdapter mAdapter;

    private HeaderGridView mGridView;

    private boolean mIsABShowing = true;

    private int mPreviousItem;

    private boolean mIsLoading = false;

    private boolean mHasMore = true;

    public static Intent createIntent(Context context, @Nullable String user) {
        Intent intent = new Intent(context, ProfileActivity.class);

        if (!TextUtils.isEmpty(user)) {
            intent.putExtra(KEY_USERNAME, user);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        setContentView(R.layout.activity_profile);
        mMultiView = (MultiStateView) findViewById(R.id.multiView);
        mWebView = (WebView) mMultiView.findViewById(R.id.loginWebView);
        mGridView = (HeaderGridView) mMultiView.findViewById(R.id.grid);
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

        PauseOnScrollListener scrollListener = new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
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
                        } else if (firstVisibleItem < mPreviousItem && !mIsABShowing) {
                            mIsABShowing = true;
                            getActionBar().show();
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

        mGridView.setOnScrollListener(scrollListener);

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(KEY_USERNAME)) {
            Log.v(TAG, "User present in Bundle extras");
            String username = getIntent().getStringExtra(KEY_USERNAME);
            mSelectedUser = app.getSql().getUser(username);
            configUser(username);
        } else if (app.getUser() != null) {
            Log.v(TAG, "User already logged in");
            mSelectedUser = app.getUser();
            configUser(null);
        } else {
            configWebView();
        }

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);
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
     * Configures the webview to handle a user logging in
     */
    private void configWebView() {
        Log.v(TAG, "No user present, going to login view");
        getActionBar().hide();
        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        mWebView.setPadding(0, ViewUtils.getHeightForTranslucentStyle(getApplicationContext()), 0, 0);
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
                        app.setUser(newUser);
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
                    // Didn't get our tokens from the response, they probably denied accessed
                    Log.w(TAG, "URL didn't contain a '#'. User denied access");
                    finish();
                }

                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        mWebView = null;
        mMultiView = null;
        mHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        switch (itemId) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.favorites:
            case R.id.submissions:
                mCurrentPage = 0;
                mCurrentEndpoint = itemId == R.id.favorites ? Endpoints.ACCOUNT_GALLERY_FAVORITES : Endpoints.ACCOUNT_SUBMISSIONS;

                if (mAdapter != null) {
                    mAdapter.clear();
                    mAdapter.notifyDataSetChanged();
                }

                invalidateOptionsMenu();
                getGalleryData();
                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                return true;

            case R.id.logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
                builder.setTitle(R.string.logout).setMessage(R.string.logout_confirm).setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                app.onLogout();
                                invalidateOptionsMenu();
                            }
                        }).show();

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
    public boolean onPrepareOptionsMenu(Menu menu) {

        if (mSelectedUser == null) {
            menu.removeItem(R.id.favorites);
            menu.removeItem(R.id.submissions);
            menu.removeItem(R.id.logout);
            menu.removeItem(R.id.refresh);
        } else {

            if (!mSelectedUser.isSelf()) {
                menu.removeItem(R.id.logout);
            }

            if (mCurrentEndpoint == Endpoints.ACCOUNT_SUBMISSIONS) {
                menu.removeItem(R.id.submissions);
            } else {
                menu.removeItem(R.id.favorites);
            }
        }

        return super.onPrepareOptionsMenu(menu);
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

                        app.getSql().insertProfile(mSelectedUser);

                        // Update our user application variable if it is the logged in user
                        if (mSelectedUser.isSelf()) {
                            app.getUser().parseJsonForValues(event.json);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getActionBar().show();
                                    invalidateOptionsMenu();
                                }
                            });
                        }

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
            } else {
                // TODO Error
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void onEventMainThread(ThrowableFailureEvent event) {
        // TODO Errors
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

    /**
     * Initializes the HeaderView for the grid
     *
     * @return
     */
    private View getHeaderView() {
        View header = LayoutInflater.from(getApplicationContext()).inflate(R.layout.profile_header, mMultiView, false);
        String date = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(new Date(mSelectedUser.getCreated()));
        String reputationText = mSelectedUser.getReputation() + " " + getString(R.string.profile_rep_date, date);
        RobotoTextView notoriety = (RobotoTextView) header.findViewById(R.id.notoriety);
        notoriety.setText(mSelectedUser.getNotoriety().getStringId());
        int notorietyColor = mSelectedUser.getNotoriety() == ImgurUser.Notoriety.FOREVER_ALONE ?
                getResources().getColor(android.R.color.holo_red_light) : getResources().getColor(android.R.color.holo_green_light);
        notoriety.setTextColor(notorietyColor);
        ((RobotoTextView) header.findViewById(R.id.rep)).setText(reputationText);
        ((RobotoTextView) header.findViewById(R.id.username)).setText(mSelectedUser.getUsername());

        if (!TextUtils.isEmpty(mSelectedUser.getBio())) {
            RobotoTextView bio = (RobotoTextView) header.findViewById(R.id.bio);
            bio.setText(mSelectedUser.getBio());
            bio.setMovementMethod(CustomLinkMovement.getInstance(this));
            Linkify.addLinks(bio, Linkify.WEB_URLS);
        }

        return header;
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            mIsLoading = false;

            switch (msg.what) {
                case ImgurHandler.MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> objects = (List<ImgurBaseObject>) msg.obj;
                    mHasMore = !objects.isEmpty();

                    if (objects.size() > 0) {
                        if (mAdapter == null) {
                            String quality = app.getPreferences().getString(SettingsActivity.THUMBNAIL_QUALITY_KEY,
                                    SettingsActivity.THUMBNAIL_QUALITY_LOW);
                            mAdapter = new GalleryAdapter(getApplicationContext(), app.getImageLoader(), objects, quality);
                            mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getApplicationContext(),0));
                            mGridView.addHeaderView(getHeaderView());
                            mGridView.setAdapter(mAdapter);
                        } else {
                            mAdapter.addItems(objects);
                            mAdapter.notifyDataSetChanged();
                        }
                    } else if (mAdapter == null || mAdapter.isEmpty()) {
                        // TODO Show empty view
                    }

                    invalidateOptionsMenu();
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case ImgurHandler.MESSAGE_ACTION_FAILED:
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
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
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onViewRepliesTap(View view) {
        // NOOP
    }
}