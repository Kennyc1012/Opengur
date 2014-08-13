package com.kenny.openimgur;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ViewUtils;

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
public class ProfileActivity extends BaseActivity {
    public static final String KEY_USERNAME = "username";

    private MultiStateView mMultiView;

    private WebView mWebView;

    private ApiClient mApiClient;

    private ImgurUser mSelectedUser;

    private int mCurrentPage;

    private Endpoints mCurrentEndpoint = Endpoints.ACCOUNT_GALLERY_FAVORITES;

    private GalleryAdapter mAdapter;

    private HeaderGridView mGridView;

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
        setContentView(R.layout.activity_profile);
        mMultiView = (MultiStateView) findViewById(R.id.multiView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
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

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(KEY_USERNAME)) {
            Log.v(TAG, "User present in Bundle extras");
            String username = getIntent().getStringExtra(KEY_USERNAME);
            String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), username);
            mApiClient = new ApiClient(detailsUrls, ApiClient.HttpRequest.GET);
            mApiClient.doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, null, null);
        } else if (app.getUser() != null) {
            Log.v(TAG, "User already logged in");
            mSelectedUser = app.getUser();
            String detailsUrls = String.format(Endpoints.PROFILE.getUrl(), mSelectedUser.getUsername());
            mApiClient = new ApiClient(detailsUrls, ApiClient.HttpRequest.GET);
            mApiClient.doWork(ImgurBusEvent.EventType.PROFILE_DETAILS, null, null);
        } else {
            configWebView();
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
    }

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
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem favorites = menu.findItem(R.id.favorites);
        MenuItem submissions = menu.findItem(R.id.submissions);

        if (mSelectedUser == null) {
            favorites.setVisible(false);
            submissions.setVisible(false);
            menu.removeItem(R.id.logout);
        } else {

            if (!mSelectedUser.isSelf()) {
                menu.removeItem(R.id.logout);
            }

            favorites.setVisible(mCurrentEndpoint == Endpoints.ACCOUNT_SUBMISSIONS);
            submissions.setVisible(mCurrentEndpoint == Endpoints.ACCOUNT_GALLERY_FAVORITES);
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

        mApiClient.setUrl(url);
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

        if (!TextUtils.isEmpty(mSelectedUser.getBio())) {
            ((RobotoTextView) header.findViewById(R.id.bio)).setText(mSelectedUser.getBio());
        }

        return header;
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ImgurHandler.MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> objects = (List<ImgurBaseObject>) msg.obj;

                    if (mAdapter == null) {
                        String quality = app.getPreferences().getString(SettingsActivity.THUMBNAIL_QUALITY_KEY,
                                SettingsActivity.THUMBNAIL_QUALITY_LOW);
                        mAdapter = new GalleryAdapter(getApplicationContext(), app.getImageLoader(), objects, quality);
                        mGridView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getApplicationContext()));
                        mGridView.addHeaderView(getHeaderView());
                        mGridView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(objects);
                        mAdapter.notifyDataSetChanged();
                    }

                    invalidateOptionsMenu();
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case ImgurHandler.MESSAGE_ACTION_FAILED:
                    break;
            }

            super.handleMessage(msg);
        }
    };
}