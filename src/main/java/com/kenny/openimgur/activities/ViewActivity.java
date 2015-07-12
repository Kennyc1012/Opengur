package com.kenny.openimgur.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.cocosw.bottomsheet.BottomSheet;
import com.cocosw.bottomsheet.BottomSheetListener;
import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.CommentAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ApiClient2;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.api.responses.CommentResponse;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum2;
import com.kenny.openimgur.classes.ImgurBaseObject2;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.fragments.CommentPopupFragment;
import com.kenny.openimgur.fragments.ImgurViewFragment;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.fragments.SideGalleryFragment;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ViewActivity extends BaseActivity implements View.OnClickListener, ImgurListener,
        SideGalleryFragment.SideGalleryListener, BottomSheetListener {
    private enum CommentSort {
        BEST("best"),
        NEW("new"),
        TOP("top");

        private final String mSort;

        CommentSort(String sort) {
            mSort = sort;
        }

        public String getSort() {
            return mSort;
        }

        public static CommentSort getSortFromPosition(int position) {
            return CommentSort.values()[position];
        }

        public static String[] getItemsForArray(Context context) {
            CommentSort[] items = CommentSort.values();
            String[] values = new String[items.length];

            for (int i = 0; i < items.length; i++) {
                switch (items[i]) {
                    case BEST:
                        values[i] = context.getString(R.string.sort_best);
                        break;

                    case TOP:
                        values[i] = context.getString(R.string.sort_top);
                        break;

                    case NEW:
                        values[i] = context.getString(R.string.sort_new);
                        break;
                }
            }

            return values;
        }

        public static CommentSort getSortFromString(String item) {
            for (CommentSort s : CommentSort.values()) {
                if (s.getSort().equals(item)) {
                    return s;
                }
            }

            return BEST;
        }
    }

    private static final String KEY_VIEW_FOR_ALBUM = "view_link_for_album";

    private static final String DIALOG_LOADING = "loading";

    private static final String KEY_COMMENT = "comments";

    private static final String KEY_POSITION = "position";

    private static final String KEY_OBJECTS = "objects";

    private static final String KEY_SORT = "commentSort";

    private static final String KEY_LOAD_COMMENTS = "autoLoadComments";

    private static final String KEY_PANEL_EXPANDED = "panelExpanded";

    @InjectView(R.id.pager)
    ViewPager mViewPager;

    @InjectView(R.id.sliding_layout)
    SlidingUpPanelLayout mSlidingPane;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.commentList)
    RecyclerView mCommentList;

    @InjectView(R.id.panelUpBtn)
    ImageButton mPanelButton;

    @InjectView(R.id.upVoteBtn)
    ImageButton mUpVoteBtn;

    @InjectView(R.id.downVoteBtn)
    ImageButton mDownVoteBtn;

    private CommentAdapter mCommentAdapter;

    private int mCurrentPosition = 0;

    private ApiClient mApiClient;

    private boolean mIsActionBarShowing = true;

    private String mGalleryId = null;

    private boolean mIsResuming = false;

    private boolean mLoadComments;

    private BrowsingAdapter mPagerAdapter;

    private CommentSort mCommentSort;

    private SideGalleryFragment mSideGalleryFragment;

    public static Intent createIntent(Context context, ArrayList<ImgurBaseObject2> objects, int position) {
        Intent intent = new Intent(context, ViewActivity.class);
        intent.putExtra(KEY_POSITION, position);
        intent.putExtra(KEY_OBJECTS, objects);
        return intent;
    }

    public static Intent createIntent(Context context, String url, boolean isAlbumLink) {
        return new Intent(context, ViewActivity.class).setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(url)).putExtra(KEY_VIEW_FOR_ALBUM, isAlbumLink);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        mCommentList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mSideGalleryFragment = (SideGalleryFragment) getFragmentManager().findFragmentById(R.id.sideGallery);
        ((Button) mMultiView.getView(MultiStateView.ViewState.ERROR).findViewById(R.id.errorButton)).setText(R.string.load_comments);

        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (!mIsResuming) {
                    mCurrentPosition = position;
                    fetchComments();
                    invalidateOptionsMenu();

                    if (mSideGalleryFragment != null) {
                        mSideGalleryFragment.onPositionChanged(position);
                    }
                }

                mIsResuming = false;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        initSlidingView();
        handleIntent(getIntent(), savedInstanceState);
    }

    private void initSlidingView() {
        mSlidingPane.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                if (v >= 0.75f && mIsActionBarShowing) {
                    getSupportActionBar().hide();
                    mIsActionBarShowing = false;
                } else if (v <= 0.75 && !mIsActionBarShowing) {
                    getSupportActionBar().show();
                    mIsActionBarShowing = true;
                }
            }

            @Override
            public void onPanelCollapsed(View view) {
                if (mPanelButton.getRotation() == 180)
                    ObjectAnimator.ofFloat(mPanelButton, "rotation", 180, 0).setDuration(200).start();
            }

            @Override
            public void onPanelExpanded(View view) {
                if (mPanelButton.getRotation() == 0)
                    ObjectAnimator.ofFloat(mPanelButton, "rotation", 0, 180).setDuration(200).start();
            }

            @Override
            public void onPanelAnchored(View view) {
            }

            @Override
            public void onPanelHidden(View view) {
            }
        });
    }

    /**
     * Handles the Intent arguments
     *
     * @param intent             Arguments
     * @param savedInstanceState Bundle if restoring
     */
    private void handleIntent(Intent intent, Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_OBJECTS)) {
            LogUtil.v(TAG, "Bundle present, will restore in onPostCreate");
            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            LogUtil.v(TAG, "Received Gallery via ACTION_VIEW");
            mGalleryId = intent.getData().getPathSegments().get(1);
        } else if (!intent.hasExtra(KEY_OBJECTS) || !intent.hasExtra(KEY_POSITION)) {
            SnackBar.show(ViewActivity.this, R.string.error_generic);
            finish();
        } else {
            mCurrentPosition = intent.getIntExtra(KEY_POSITION, 0);
            ArrayList<ImgurBaseObject2> objects = intent.getParcelableArrayListExtra(KEY_OBJECTS);
            mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);

            if (mSideGalleryFragment != null) {
                mSideGalleryFragment.addGalleryItems(objects);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mCommentSort = CommentSort.getSortFromString(app.getPreferences().getString(KEY_SORT, null));
            mLoadComments = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(KEY_LOAD_COMMENTS, true);

            // Check if the activity was opened externally by a link click
            if (!TextUtils.isEmpty(mGalleryId)) {
                boolean isAlbumLink = getIntent().getBooleanExtra(KEY_VIEW_FOR_ALBUM, false);
                fetchItemDetails(mGalleryId, isAlbumLink);
            } else {
                mViewPager.setAdapter(mPagerAdapter);

                // If we start on the 0 page, the onPageSelected event won't fire
                if (mCurrentPosition == 0) {
                    fetchComments();
                } else {
                    mViewPager.setCurrentItem(mCurrentPosition);
                }
            }
        } else {
            mCommentSort = CommentSort.getSortFromString(savedInstanceState.getString(KEY_SORT, CommentSort.NEW.getSort()));
            mLoadComments = savedInstanceState.getBoolean(KEY_LOAD_COMMENTS, true);
            mIsResuming = true;
            mCurrentPosition = savedInstanceState.getInt(KEY_POSITION, 0);
            ArrayList<ImgurBaseObject2> objects = savedInstanceState.getParcelableArrayList(KEY_OBJECTS);
            mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);
            mViewPager.setAdapter(mPagerAdapter);
            mViewPager.setCurrentItem(mCurrentPosition);

            if (mSideGalleryFragment != null) {
                 mSideGalleryFragment.addGalleryItems(objects);
            }

            List<ImgurComment> comments = savedInstanceState.getParcelableArrayList(KEY_COMMENT);

            if (comments != null) {
                mCommentAdapter = new CommentAdapter(this, comments, this);
                mCommentList.setAdapter(mCommentAdapter);
            }

            if (mLoadComments) {
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            } else {
                mMultiView.setErrorText(R.id.errorMessage, R.string.comments_off);
                mMultiView.setViewState(MultiStateView.ViewState.ERROR);
            }

            if (savedInstanceState.getBoolean(KEY_PANEL_EXPANDED, false))
                getSupportActionBar().hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        if (mCommentAdapter != null) {
            CustomLinkMovement.getInstance().addListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        CustomLinkMovement.getInstance().removeListener(this);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        dismissDialogFragment(DIALOG_LOADING);
        dismissDialogFragment("comment");

        if (mCommentAdapter != null) {
            mCommentAdapter.onDestroy();
            mCommentAdapter = null;
        }

        if (mPagerAdapter != null) {
            mPagerAdapter.clear();
            mPagerAdapter = null;
        }

        app.getPreferences().edit().putString(KEY_SORT, mCommentSort.getSort()).apply();
        super.onDestroy();
    }

    @OnClick({R.id.panelUpBtn, R.id.upVoteBtn, R.id.downVoteBtn, R.id.commentBtn, R.id.sortComments, R.id.errorButton})
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.panelUpBtn:
                if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                } else {
                    mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
                break;

            case R.id.upVoteBtn:
            case R.id.downVoteBtn:
                if (user != null) {
                    ImgurBaseObject2 obj = mPagerAdapter.getImgurItem(mCurrentPosition);
                    String vote = view.getId() == R.id.upVoteBtn ? ImgurBaseObject2.VOTE_UP : ImgurBaseObject2.VOTE_DOWN;
                    String upVoteUrl = String.format(Endpoints.GALLERY_VOTE.getUrl(), obj.getId(), vote);
                    obj.setVote(vote);

                    if (mApiClient == null) {
                        mApiClient = new ApiClient(upVoteUrl, ApiClient.HttpRequest.POST);
                    } else {
                        mApiClient.setUrl(upVoteUrl);
                        mApiClient.setRequestType(ApiClient.HttpRequest.POST);
                    }

                    mApiClient.doWork(ImgurBusEvent.EventType.GALLERY_VOTE, vote, new FormEncodingBuilder().add("vote", vote).build());
                    ImgurViewFragment fragment = (ImgurViewFragment) mPagerAdapter.instantiateItem(mViewPager, mCurrentPosition);

                    if (fragment != null && fragment.isAdded() && fragment.getUserVisibleHint()) {
                        fragment.setVote(vote);
                    }
                } else {
                    SnackBar.show(ViewActivity.this, R.string.user_not_logged_in);
                }
                break;

            case R.id.commentBtn:
                if (user != null && user.isAccessTokenValid()) {
                    DialogFragment fragment = CommentPopupFragment.createInstance(mPagerAdapter.getImgurItem(mCurrentPosition).getId(), null);
                    showDialogFragment(fragment, "comment");
                } else {
                    SnackBar.show(ViewActivity.this, R.string.user_not_logged_in);
                }
                break;

            case R.id.sortComments:
                new AlertDialog.Builder(ViewActivity.this, theme.getAlertDialogTheme())
                        .setTitle(R.string.sort_by)
                        .setItems(CommentSort.getItemsForArray(getApplicationContext()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CommentSort sort = CommentSort.getSortFromPosition(which);

                                if (sort != mCommentSort) {
                                    mCommentSort = sort;

                                    // Don't bother making an Api call if the item has no comments
                                    if (mCommentAdapter != null && !mCommentAdapter.isEmpty()) {
                                        fetchComments();
                                    }
                                }
                            }
                        }).show();
                break;

            case R.id.errorButton:
                // Set load comments to true, load comments, then set it back to its original value to persist its state
                boolean loadComments = mLoadComments;
                mLoadComments = true;
                fetchComments();
                mLoadComments = loadComments;
                break;
        }
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        // If the event is COMMENT_POSTING, it will not contain a json object or HTTPRequest type
        if (ImgurBusEvent.EventType.COMMENT_POSTING.equals(event.eventType)) {
            mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_COMMENT_POSTING);
            return;
        }

        try {
            int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
            switch (event.eventType) {

                case COMMENT_POSTED:
                    if (statusCode == ApiClient.STATUS_OK) {
                        JSONObject json = event.json.getJSONObject(ApiClient.KEY_DATA);
                        // A successful comment post will return its id
                        mHandler.sendMessage(ImgurHandler.MESSAGE_COMMENT_POSTED, json.has("id"));
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                    }
                    break;

                case COMMENT_VOTE:
                    if (statusCode == ApiClient.STATUS_OK) {
                        boolean success = event.json.getBoolean(ApiClient.KEY_SUCCESS);
                        mHandler.sendMessage(ImgurHandler.MESSAGE_COMMENT_VOTED, success);
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_COMMENT_VOTED, ApiClient.getErrorCodeStringResource(statusCode));
                    }

                    break;

                case GALLERY_VOTE:
                    if (statusCode == ApiClient.STATUS_OK) {
                        if (event.json.getBoolean(ApiClient.KEY_SUCCESS)) {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_GALLERY_VOTE_COMPLETE, event.id);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_GALLERY_VOTE_COMPLETE, R.string.error_generic);
                        }
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_GALLERY_VOTE_COMPLETE, ApiClient.getErrorCodeStringResource(statusCode));
                    }
                    break;

                case FAVORITE:
                    ImgurBaseObject2 obj = mPagerAdapter.getImgurItem(mCurrentPosition);
                    if (obj.getId().equals(event.id)) {
                        obj.setIsFavorite(!obj.isFavorited());
                        invalidateOptionsMenu();
                    }
                    break;
            }
        } catch (JSONException e) {
            LogUtil.e(TAG, "Error Decoding JSON", e);
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
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
    public void onPhotoTap(View View) {
    }

    @Override
    public void onPhotoLongTapListener(View View) {
    }

    @Override
    public void onPlayTap(final ProgressBar prog, final FloatingActionButton play, final ImageView image, final VideoView video, final View view) {
    }

    @Override
    public void onLinkTap(View view, String url) {
        if (!TextUtils.isEmpty(url) && canDoFragmentTransaction()) {
            LinkUtils.LinkMatch match = LinkUtils.findImgurLinkMatch(url);

            switch (match) {
                case GALLERY:
                case ALBUM:
                    Intent intent = ViewActivity.createIntent(getApplicationContext(), url, match == LinkUtils.LinkMatch.ALBUM).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case IMAGE_URL_QUERY:
                    int index = url.indexOf("?");
                    url = url.substring(0, index);
                    // Intentional fallthrough
                case IMAGE_URL:
                    getFragmentManager().beginTransaction().add(PopupImageDialogFragment.getInstance(url, url.endsWith(".gif"), true, false), "popup").commitAllowingStateLoss();
                    break;

                case DIRECT_LINK:
                    boolean isAnimated = LinkUtils.isLinkAnimated(url);
                    boolean isVideo = LinkUtils.isVideoLink(url);
                    getFragmentManager().beginTransaction().add(PopupImageDialogFragment.getInstance(url, isAnimated, true, isVideo), "popup").commitAllowingStateLoss();
                    break;

                case IMAGE:
                    String[] split = url.split("\\/");
                    getFragmentManager().beginTransaction().add(PopupImageDialogFragment.getInstance(split[split.length - 1], false, false, false), "popup").commitAllowingStateLoss();
                    break;

                case USER_CALLOUT:
                    startActivity(ProfileActivity.createIntent(getApplicationContext(), url.replace("@", "")));
                    break;

                case NONE:
                default:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        SnackBar.show(ViewActivity.this, R.string.cant_launch_intent);
                    }
                    break;
            }
        } else {
            if (view.getLayoutParams() instanceof RecyclerView.LayoutParams) {
                onListItemClick(mCommentList.getLayoutManager().getPosition(view));
            } else {
                // This is super ugly, in order to the get position from the layout manager, we need the root view
                View parent = (View) view.getParent();
                if (parent != null) parent = (View) parent.getParent();

                if (parent != null && parent.getLayoutParams() instanceof RecyclerView.LayoutParams) {
                    onListItemClick(mCommentList.getLayoutManager().getPosition(parent));
                }
            }
        }
    }

    @Override
    public void onViewRepliesTap(View view) {
        int position = mCommentList.getLayoutManager().getPosition(view);

        if (!mCommentAdapter.isExpanded(position)) {
            mCommentAdapter.expandComments(view, position);
        } else {
            mCommentAdapter.collapseComments(view, position);
        }
    }

    @Override
    public void onItemSelected(int position) {
        mViewPager.setCurrentItem(position);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mPagerAdapter == null || mPagerAdapter.getImgurItem(mCurrentPosition) == null) {
            LogUtil.w(TAG, "Unable to retrieve Imgur object from adapter");
            return false;
        }

        final ImgurBaseObject2 imgurObj = mPagerAdapter.getImgurItem(mCurrentPosition);

        switch (item.getItemId()) {
            case R.id.favorite:
                if (user != null) {
                    String url;

                    if (imgurObj instanceof ImgurAlbum2) {
                        url = String.format(Endpoints.FAVORITE_ALBUM.getUrl(), imgurObj.getId());
                    } else {
                        url = String.format(Endpoints.FAVORITE_IMAGE.getUrl(), imgurObj.getId());
                    }

                    final RequestBody body = new FormEncodingBuilder().add("id", imgurObj.getId()).build();

                    if (mApiClient == null) {
                        mApiClient = new ApiClient(url, ApiClient.HttpRequest.POST);
                    } else {
                        mApiClient.setUrl(url);
                        mApiClient.setRequestType(ApiClient.HttpRequest.POST);
                    }

                    mApiClient.doWork(ImgurBusEvent.EventType.FAVORITE, imgurObj.getId(), body);
                } else {
                    SnackBar.show(ViewActivity.this, R.string.user_not_logged_in);
                }
                return true;

            case R.id.profile:
                startActivity(ProfileActivity.createIntent(getApplicationContext(), imgurObj.getAccount()));
                return true;

            case R.id.reddit:
                if (TextUtils.isEmpty(imgurObj.getRedditLink())) {
                    LogUtil.w(TAG, "Item does not have a reddit link");
                    return false;
                }

                String url = String.format("https://reddit.com%s", imgurObj.getRedditLink());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    SnackBar.show(ViewActivity.this, R.string.cant_launch_intent);
                }
                return true;


            case R.id.share:
                startActivity(Intent.createChooser(imgurObj.getShareIntent(), getString(R.string.share)));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mPagerAdapter != null && mPagerAdapter.getCount() > 0) {
            ImgurBaseObject2 imgurObject = mPagerAdapter.getImgurItem(mCurrentPosition);

            if (TextUtils.isEmpty(imgurObject.getAccount())) {
                menu.findItem(R.id.profile).setVisible(false);
            }

            if (TextUtils.isEmpty(imgurObject.getRedditLink())) {
                menu.findItem(R.id.reddit).setVisible(false);
            }

            menu.findItem(R.id.favorite).setIcon(imgurObject.isFavorited() ?
                    R.drawable.ic_action_favorite : R.drawable.ic_action_unfavorite);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCommentAdapter != null && !mCommentAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_COMMENT, mCommentAdapter.retainItems());
        }

        outState.putBoolean(KEY_LOAD_COMMENTS, mLoadComments);
        outState.putString(KEY_SORT, mCommentSort.getSort());

        if (mPagerAdapter != null && !mPagerAdapter.isEmpty()) {
            outState.putInt(KEY_POSITION, mViewPager.getCurrentItem());
            outState.putParcelableArrayList(KEY_OBJECTS, mPagerAdapter.retainItems());
        }

        if (mSlidingPane != null)
            outState.putBoolean(KEY_PANEL_EXPANDED, mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED);
    }

    private void onListItemClick(int position) {
        if (mCommentAdapter == null) return;

        if (position >= 0) {
            boolean shouldClose = mCommentAdapter.setSelectedIndex(position);

            if (!shouldClose) {
                new BottomSheet.Builder(this, R.style.BottomSheet_StyleDialog)
                        .title(R.string.options)
                        .grid()
                        .sheet(R.menu.comment_menu)
                        .listener(this)
                        .object(mCommentAdapter.getItem(position))
                        .show();
            }
        }
    }

    @Override
    public void onSheetDismissed(Object object) {
        mCommentAdapter.setSelectedIndex(-1);
    }

    @Override
    public void onItemClicked(int id, Object object) {
        if (!(object instanceof ImgurComment)) {
            LogUtil.w(TAG, "onItemClicked did not yield an ImgurComment");
            return;
        }

        ImgurComment comment = (ImgurComment) object;

        switch (id) {
            case R.id.upVote:
            case R.id.downVote:
                if (user != null) {
                    String vote = id == R.id.upVote ? ImgurBaseObject2.VOTE_UP : ImgurBaseObject2.VOTE_DOWN;
                    String url = String.format(Endpoints.COMMENT_VOTE.getUrl(), comment.getId(), vote);
                    RequestBody body = new FormEncodingBuilder().add("id", comment.getId()).build();

                    if (mApiClient == null) {
                        mApiClient = new ApiClient(url, ApiClient.HttpRequest.POST);
                    } else {
                        mApiClient.setUrl(url);
                        mApiClient.setRequestType(ApiClient.HttpRequest.POST);
                    }

                    mApiClient.doWork(ImgurBusEvent.EventType.COMMENT_VOTE, null, body);
                } else {
                    SnackBar.show(ViewActivity.this, R.string.user_not_logged_in);
                }
                break;

            case R.id.profile:
                startActivity(ProfileActivity.createIntent(ViewActivity.this, comment.getAuthor()));
                break;

            case R.id.reply:
                if (user != null) {
                    DialogFragment fragment = CommentPopupFragment.createInstance(mPagerAdapter.getImgurItem(mCurrentPosition).getId(), comment.getId());
                    showDialogFragment(fragment, "comment");
                } else {
                    SnackBar.show(ViewActivity.this, R.string.user_not_logged_in);
                }
                break;

            case R.id.copy:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.comment), comment.getComment());
                clipboard.setPrimaryClip(clip);
                SnackBar.show(ViewActivity.this, R.string.comment_copied);
                break;
        }
    }

    @Override
    public void onSheetShown(Object object) {
        if (object instanceof ImgurComment) {
            LogUtil.v(TAG, "ImgurComment Selected " + object);
        }
    }

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GALLERY_VOTE_COMPLETE:
                    if (msg.obj instanceof String) {
                        // To show conformation of a vote on an image/gallery, we will animate whichever vote button was pressed
                        String vote = (String) msg.obj;
                        View animateView = ImgurBaseObject2.VOTE_UP.equals(vote) ? mUpVoteBtn : mDownVoteBtn;
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(
                                ObjectAnimator.ofFloat(animateView, "scaleY", 1.0f, 2.0f, 1.0f),
                                ObjectAnimator.ofFloat(animateView, "scaleX", 1.0f, 2.0f, 1.0f)
                        );

                        set.setDuration(1500L).setInterpolator(new OvershootInterpolator());
                        set.start();
                    } else {
                        SnackBar.show(ViewActivity.this, msg.obj instanceof Integer ? (Integer) msg.obj : R.string.error_generic);
                    }
                    break;

                case MESSAGE_COMMENT_POSTING:
                    // We want to popup a "Loading" dialog while the comment is being posted
                    showDialogFragment(LoadingDialogFragment.createInstance(R.string.posting_comment, false), DIALOG_LOADING);
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, msg.obj instanceof Integer ? (Integer) msg.obj : R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;

                case MESSAGE_COMMENT_VOTED:
                    if (msg.obj instanceof Boolean) {
                        int stringId = (Boolean) msg.obj ? R.string.vote_cast : R.string.error_generic;
                        SnackBar.show(ViewActivity.this, stringId);
                    } else {
                        SnackBar.show(ViewActivity.this, msg.obj instanceof Integer ? (Integer) msg.obj : R.string.error_generic);
                    }
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    @OnClick(R.id.errorButton)
    public void fetchComments() {
        if (mLoadComments && mPagerAdapter != null) {
            ImgurBaseObject2 imgurBaseObject = mPagerAdapter.getImgurItem(mCurrentPosition);

            if (imgurBaseObject == null) {
                LogUtil.w(TAG, "Object returned is null, can not load comments");
                return;
            }

            ApiClient2.getService().getComments(imgurBaseObject.getId(), mCommentSort.getSort(), new Callback<CommentResponse>() {
                @Override
                public void success(CommentResponse commentResponse, Response response) {
                    if (mPagerAdapter == null || mPagerAdapter.getImgurItem(mCurrentPosition) == null) {
                        return;
                    }

                    if (!commentResponse.data.isEmpty()) {
                        ImgurBaseObject2 imgurBaseObject = mPagerAdapter.getImgurItem(mCurrentPosition);
                        ImgurComment comment = commentResponse.data.get(0);

                        if (comment.getImageId().equals(imgurBaseObject.getId())) {
                            // We only show the comments for the correct gallery item
                            if (mCommentAdapter == null) {
                                mCommentAdapter = new CommentAdapter(ViewActivity.this, commentResponse.data, ViewActivity.this);
                                mCommentList.setAdapter(mCommentAdapter);
                            } else {
                                mCommentAdapter.addItems(commentResponse.data);
                            }

                            mCommentAdapter.setOP(imgurBaseObject.getAccount());
                            mCommentAdapter.clearExpansionInfo();
                            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                            mMultiView.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (mCommentList != null) mCommentList.scrollToPosition(0);
                                }
                            });
                        }
                    } else {
                        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    //TODO
                }
            });
        } else if (mMultiView != null && mMultiView.getViewState() != MultiStateView.ViewState.ERROR) {
            mMultiView.setErrorText(R.id.errorMessage, R.string.comments_off);
            mMultiView.setViewState(MultiStateView.ViewState.ERROR);
        }
    }

    private void fetchItemDetails(final String id, final boolean isAlbum) {
        if (isAlbum) {
            ApiClient2.getService().getAlbumImages(id, new Callback<AlbumResponse>() {
                @Override
                public void success(AlbumResponse albumResponse, Response response) {
                    ImgurAlbum2 album = new ImgurAlbum2(mGalleryId, null, getIntent().getData().toString());
                    mGalleryId = null;
                    album.addPhotosToAlbum(albumResponse.data);
                   /* final ArrayList<ImgurBaseObject> objects = new ArrayList<>(1);
                    objects.add(album);
                    mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);
                    mViewPager.setAdapter(mPagerAdapter);
                    invalidateOptionsMenu();
                    fetchComments();*/
                    // TODO
                }

                @Override
                public void failure(RetrofitError error) {
                    // TODO
                }
            });
        } else {
            ApiClient2.getService().getGalleryDetails(id, new Callback<GalleryResponse>() {
                @Override
                public void success(GalleryResponse galleryResponse, Response response) {
                    /* final ArrayList<ImgurBaseObject> objects = new ArrayList<>(1);
                    objects.add(album);
                    mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);
                    mViewPager.setAdapter(mPagerAdapter);
                    invalidateOptionsMenu();
                    fetchComments();*/
                    // TODO
                }

                @Override
                public void failure(RetrofitError error) {
                    // TODO
                }
            });
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
    }

    private static class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.5f;

        private static final float MIN_ALPHA = 1.0f;

        @Override
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            int pageHeight = view.getHeight();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0);

            } else if (position <= 1) { // [-1,1]
                // Modify the default slide transition to shrink the page as well
                float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }

                // Scale the page down (between MIN_SCALE and 1)
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

                // Fade the page relative to its size.
                view.setAlpha(MIN_ALPHA +
                        (scaleFactor - MIN_SCALE) /
                                (1 - MIN_SCALE) * (1 - MIN_ALPHA));

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }

    private static class BrowsingAdapter extends FragmentStatePagerAdapter {
        private ArrayList<ImgurBaseObject2> objects;

        private boolean mDisplayTags;

        public BrowsingAdapter(Context context, FragmentManager fm, ArrayList<ImgurBaseObject2> objects) {
            super(fm);
            this.objects = objects;
            mDisplayTags = OpengurApp.getInstance(context).getPreferences().getBoolean(SettingsActivity.KEY_TAGS, true);
        }

        @Override
        public Fragment getItem(int position) {
            return ImgurViewFragment.createInstance(objects.get(position), mDisplayTags);
        }

        @Override
        public int getCount() {
            return objects != null ? objects.size() : 0;
        }

        @Nullable
        public ImgurBaseObject2 getImgurItem(int position) {
            if (objects == null || position >= objects.size()) return null;
            return objects.get(position);
        }

        public void clear() {
            if (objects != null) {
                objects.clear();
            }
        }

        public boolean isEmpty() {
            return objects == null || objects.isEmpty();
        }

        public ArrayList<ImgurBaseObject2> retainItems() {
            return new ArrayList<>(objects);
        }
    }
}
