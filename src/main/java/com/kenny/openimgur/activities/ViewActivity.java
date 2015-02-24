package com.kenny.openimgur.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.cocosw.bottomsheet.BottomSheet;
import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.CommentAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.fragments.CommentPopupFragment;
import com.kenny.openimgur.fragments.ImgurViewFragment;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.fragments.SideGalleryFragment;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarItem;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ViewActivity extends BaseActivity implements View.OnClickListener, ImgurListener, SideGalleryFragment.SideGalleryListener {
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

            return NEW;
        }
    }

    private static final String DIALOG_LOADING = "loading";

    private static final String KEY_COMMENT = "comments";

    private static final String KEY_POSITION = "position";

    private static final String KEY_OBJECTS = "objects";

    private static final String KEY_SORT = "commentSort";

    private static final String KEY_LOAD_COMMENTS = "autoLoadComments";

    @InjectView(R.id.pager)
    ViewPager mViewPager;

    @InjectView(R.id.sliding_layout)
    SlidingUpPanelLayout mSlidingPane;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.commentList)
    ListView mCommentList;

    @InjectView(R.id.panelUpBtn)
    ImageButton mPanelButton;

    @InjectView(R.id.upVoteBtn)
    ImageButton mUpVoteBtn;

    @InjectView(R.id.downVoteBtn)
    ImageButton mDownVoteBtn;

    private CommentAdapter mCommentAdapter;

    private TextView mCommentListHeader;

    // Keeps track of the previous list of comments as we progress in the stack
    private LongSparseArray<ArrayList<ImgurComment>> mCommentArray = new LongSparseArray<>();

    // Keeps track of the position of the comment list when navigating in the stack
    private LongSparseArray<Integer> mPreviousCommentPositionArray = new LongSparseArray<>();

    private int mCurrentPosition = 0;

    private ApiClient mApiClient;

    private boolean mIsActionBarShowing = true;

    private ImgurComment mSelectedComment;

    private String mGalleryId = null;

    private boolean mIsResuming = false;

    private boolean mLoadComments;

    private boolean mIsCommentsAnimating = false;

    private BrowsingAdapter mPagerAdapter;

    private CommentSort mCommentSort;

    private SideGalleryFragment mSideGalleryFragment;

    public static Intent createIntent(Context context, ArrayList<ImgurBaseObject> objects, int position) {
        Intent intent = new Intent(context, ViewActivity.class);
        intent.putExtra(KEY_POSITION, position);
        intent.putExtra(KEY_OBJECTS, objects);
        return intent;
    }

    public static Intent createIntent(Context context, String url) {
        return new Intent(context, ViewActivity.class).setAction(Intent.ACTION_VIEW).setData(Uri.parse(url));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        mSideGalleryFragment = (SideGalleryFragment) getFragmentManager().findFragmentById(R.id.sideGallery);
        mMultiView.setErrorButtonText(R.id.errorButton, R.string.load_comments);
        mCommentListHeader = (TextView) View.inflate(this, R.layout.previous_comments_header, null);
        Drawable[] drawables = mCommentListHeader.getCompoundDrawables();

        // Sets the previous comment button color to the themes accent color
        if (drawables != null && drawables.length > 0 && drawables[0] instanceof LayerDrawable) {
            GradientDrawable drawable = (GradientDrawable) ((LayerDrawable) drawables[0]).getDrawable(0);
            drawable.setColor(getResources().getColor(theme.accentColor));
        }
        // Header needs to be added before adapter is set for pre 4.4 devices
        mCommentList.addHeaderView(mCommentListHeader);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (!mIsResuming) {
                    mCurrentPosition = position;
                    loadComments();
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

        mCommentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                onListItemClick(position - mCommentList.getHeaderViewsCount());
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
                ObjectAnimator.ofFloat(mPanelButton, "rotation", 180, 0).setDuration(200).start();
            }

            @Override
            public void onPanelExpanded(View view) {
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
            SnackBar.show(ViewActivity.this, new SnackBarItem.Builder().setMessage(getString(R.string.error_generic)).build());
            finish();
        } else {
            mCurrentPosition = intent.getIntExtra(KEY_POSITION, 0);
            ArrayList<ImgurBaseObject> objects = intent.getParcelableArrayListExtra(KEY_OBJECTS);
            mPagerAdapter = new BrowsingAdapter(getFragmentManager(), objects);

            if (mSideGalleryFragment != null) {
                mSideGalleryFragment.addGalleryItems(objects);
            }
        }
    }

    private void loadComments() {
        if (mLoadComments && mPagerAdapter != null) {
            ImgurBaseObject imgurBaseObject = mPagerAdapter.getImgurItem(mCurrentPosition);
            String url = String.format(Endpoints.COMMENTS.getUrl(), imgurBaseObject.getId(), mCommentSort.getSort());

            if (mApiClient == null) {
                mApiClient = new ApiClient(url, ApiClient.HttpRequest.GET);
            } else {
                mApiClient.setRequestType(ApiClient.HttpRequest.GET);
                mApiClient.setUrl(url);
            }

            if (mCommentAdapter != null) {
                mCommentAdapter.clear();
            }

            mMultiView.setViewState(MultiStateView.ViewState.LOADING);
            mApiClient.doWork(ImgurBusEvent.EventType.COMMENTS, imgurBaseObject.getId(), null);
        } else if (mMultiView != null && mMultiView.getViewState() != MultiStateView.ViewState.ERROR) {
            mMultiView.setErrorText(R.id.errorMessage, R.string.comments_off);
            mMultiView.setViewState(MultiStateView.ViewState.ERROR);
        }
    }

    /**
     * Changes the Comment List to the next thread of comments
     *
     * @param comment
     */
    private void nextComments(final ImgurComment comment) {
        if (mIsCommentsAnimating) return;

        mIsCommentsAnimating = true;
        mSelectedComment = null;
        mCommentAdapter.setSelectedIndex(-1);

        mCommentList.animate().translationX(-mCommentList.getWidth()).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animation.removeAllListeners();

                mCommentArray.put(Long.valueOf(comment.getId()), mCommentAdapter.retainItems());
                mPreviousCommentPositionArray.put(Long.valueOf(comment.getId()), mCommentList.getFirstVisiblePosition());
                mCommentAdapter.clear();

                if (mCommentList.getHeaderViewsCount() <= 0) {
                    mCommentList.addHeaderView(mCommentListHeader);
                }

                mCommentAdapter.addItems(comment.getReplies());
                mCommentList.setSelection(0);
                Animator anim = ObjectAnimator.ofFloat(mCommentList, "translationX", mCommentList.getWidth(), 0);

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animation.removeAllListeners();
                        mIsCommentsAnimating = false;
                    }
                });

                anim.setDuration(250).start();
            }
        }).start();
    }

    /**
     * Changes the comment list to the previous thread of comments
     *
     * @param comment
     */
    private void previousComments(final ImgurComment comment) {
        final Integer previousSelection = mPreviousCommentPositionArray.get(comment.getParentId());

        if (mIsCommentsAnimating || previousSelection == null) {
            LogUtil.w(TAG, "No previous comments to show");
            return;
        }

        mIsCommentsAnimating = true;
        mSelectedComment = null;
        mCommentAdapter.setSelectedIndex(-1);

        mCommentList.animate().translationX(mCommentList.getWidth()).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animation.removeAllListeners();

                if (mPreviousCommentPositionArray.size() <= 1) {
                    mCommentList.removeHeaderView(mCommentListHeader);
                }

                mCommentAdapter.clear();
                mCommentAdapter.addItems(mCommentArray.get(comment.getParentId()));
                mCommentArray.remove(comment.getParentId());
                mCommentAdapter.notifyDataSetChanged();
                mCommentList.setSelection(previousSelection);
                mPreviousCommentPositionArray.remove(comment.getParentId());
                Animator anim = ObjectAnimator.ofFloat(mCommentList, "translationX", -mCommentList.getWidth(), 0);

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        animation.removeAllListeners();
                        mIsCommentsAnimating = false;
                    }
                });

                anim.setDuration(250).start();
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            if (mCommentAdapter != null && !mCommentAdapter.isEmpty() &&
                    mCommentArray != null && mCommentArray.size() > 0) {
                previousComments(mCommentAdapter.getItem(0));
            } else {
                mSlidingPane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }

            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (savedInstanceState == null) {
            mCommentSort = CommentSort.getSortFromString(app.getPreferences().getString(KEY_SORT, CommentSort.NEW.getSort()));
            mLoadComments = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean(KEY_LOAD_COMMENTS, true);

            // Check if the activity was opened externally by a link click
            if (!TextUtils.isEmpty(mGalleryId)) {
                mApiClient = new ApiClient(String.format(Endpoints.GALLERY_ITEM_DETAILS.getUrl(), mGalleryId), ApiClient.HttpRequest.GET);
                mApiClient.doWork(ImgurBusEvent.EventType.GALLERY_ITEM_INFO, mGalleryId, null);
            } else {
                mViewPager.setAdapter(mPagerAdapter);

                // If we start on the 0 page, the onPageSelected event won't fire
                if (mCurrentPosition == 0) {
                    loadComments();
                } else {
                    mViewPager.setCurrentItem(mCurrentPosition);
                }
            }
        } else {
            mCommentSort = CommentSort.getSortFromString(savedInstanceState.getString(KEY_SORT, CommentSort.NEW.getSort()));
            mLoadComments = savedInstanceState.getBoolean(KEY_LOAD_COMMENTS, true);
            mIsResuming = true;
            mCurrentPosition = savedInstanceState.getInt(KEY_POSITION, 0);
            ArrayList<ImgurBaseObject> objects = savedInstanceState.getParcelableArrayList(KEY_OBJECTS);
            mPagerAdapter = new BrowsingAdapter(getFragmentManager(), objects);
            mViewPager.setAdapter(mPagerAdapter);
            mViewPager.setCurrentItem(mCurrentPosition);

            if (mSideGalleryFragment != null) {
                mSideGalleryFragment.addGalleryItems(objects);
            }

            List<ImgurComment> comments = savedInstanceState.getParcelableArrayList(KEY_COMMENT);

            if (comments != null) {
                mCommentAdapter = new CommentAdapter(this, comments, this);
                mCommentList.setAdapter(mCommentAdapter);
                mCommentList.removeHeaderView(mCommentListHeader);
            }

            if (mLoadComments) {
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            } else {
                mMultiView.setErrorText(R.id.errorMessage, R.string.comments_off);
                mMultiView.setViewState(MultiStateView.ViewState.ERROR);
            }
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
        mPreviousCommentPositionArray.clear();
        mCommentArray.clear();

        if (mCommentAdapter != null) {
            mCommentAdapter.destroy();
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
                    ImgurBaseObject obj = mPagerAdapter.getImgurItem(mCurrentPosition);
                    String vote = view.getId() == R.id.upVoteBtn ? ImgurBaseObject.VOTE_UP : ImgurBaseObject.VOTE_DOWN;
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
                new AlertDialog.Builder(ViewActivity.this)
                        .setTitle(R.string.sort_by)
                        .setItems(CommentSort.getItemsForArray(getApplicationContext()), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                CommentSort sort = CommentSort.getSortFromPosition(which);

                                if (sort != mCommentSort) {
                                    mCommentSort = sort;

                                    // Don't bother making an Api call if the item has no comments
                                    if (mCommentAdapter != null && !mCommentAdapter.isEmpty()) {
                                        loadComments();
                                    }
                                }
                            }
                        }).show();
                break;

            case R.id.errorButton:
                // Set load comments to true, load comments, then set it back to its original value to persist its state
                boolean loadComments = mLoadComments;
                mLoadComments = true;
                loadComments();
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
                case COMMENTS:
                    ImgurBaseObject imgurItem = mPagerAdapter.getImgurItem(mCurrentPosition);

                    if (imgurItem.getId().equals(event.id)) {
                        if (statusCode == ApiClient.STATUS_OK) {
                            JSONArray data = event.json.getJSONArray(ApiClient.KEY_DATA);
                            List<ImgurComment> comments = new ArrayList<>(data.length());
                            for (int i = 0; i < data.length(); i++) {
                                comments.add(new ImgurComment(data.getJSONObject(i)));
                            }

                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, comments);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                        }
                    }
                    break;

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

                case GALLERY_ITEM_INFO:
                    if (!TextUtils.isEmpty(mGalleryId) && mGalleryId.equals(event.id)) {
                        mGalleryId = null;

                        if (statusCode == ApiClient.STATUS_OK) {
                            JSONObject json = event.json.getJSONObject(ApiClient.KEY_DATA);
                            Object imgurObject;

                            if (json.has("is_album") && json.getBoolean("is_album")) {
                                imgurObject = new ImgurAlbum(json);

                                if (json.has("images")) {
                                    JSONArray array = json.getJSONArray("images");

                                    for (int i = 0; i < array.length(); i++) {
                                        ((ImgurAlbum) imgurObject).addPhotoToAlbum(new ImgurPhoto(array.getJSONObject(i)));
                                    }
                                }
                            } else {
                                imgurObject = new ImgurPhoto(json);
                            }

                            mHandler.sendMessage(ImgurHandler.MESSAGE_ITEM_DETAILS, imgurObject);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                        }
                    }
                    break;

                case FAVORITE:
                    ImgurBaseObject obj = mPagerAdapter.getImgurItem(mCurrentPosition);
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
    public void onPlayTap(final ProgressBar prog, final ImageButton play, final ImageView image, final VideoView video) {
    }

    @Override
    public void onLinkTap(View view, String url) {
        if (!TextUtils.isEmpty(url)) {
            LinkUtils.LinkMatch match = LinkUtils.findImgurLinkMatch(url);

            switch (match) {
                case GALLERY:
                    Intent intent = ViewActivity.createIntent(getApplicationContext(), url).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case IMAGE_URL:
                    PopupImageDialogFragment.getInstance(url, url.endsWith(".gif"), true, false)
                            .show(getFragmentManager(), "popup");
                    break;

                case VIDEO_URL:
                    PopupImageDialogFragment.getInstance(url, true, true, true)
                            .show(getFragmentManager(), "popup");
                    break;

                case IMAGE:
                    String[] split = url.split("\\/");
                    PopupImageDialogFragment.getInstance(split[split.length - 1], false, false, false)
                            .show(getFragmentManager(), "popup");
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
            onListItemClick(mCommentList.getPositionForView(view) - mCommentList.getHeaderViewsCount());
        }
    }

    @Override
    public void onViewRepliesTap(View view) {
        int position = mCommentList.getPositionForView(view) - mCommentList.getHeaderViewsCount();
        ImgurComment comment = mCommentAdapter.getItem(position);

        if (comment.getReplyCount() > 0) {
            nextComments(comment);
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
        switch (item.getItemId()) {
            case R.id.favorite:
                if (user != null) {
                    final ImgurBaseObject imgurObj = mPagerAdapter.getImgurItem(mCurrentPosition);
                    String url;

                    if (imgurObj instanceof ImgurAlbum) {
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
                startActivity(ProfileActivity.createIntent(getApplicationContext(), mPagerAdapter.getImgurItem(mCurrentPosition).getAccount()));
                return true;

            case R.id.reddit:
                String url = String.format("http://reddit.com%s", mPagerAdapter.getImgurItem(mCurrentPosition).getRedditLink());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    SnackBar.show(ViewActivity.this, new SnackBarItem.Builder().setMessage(getString(R.string.cant_launch_intent)).build());
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mPagerAdapter != null && mPagerAdapter.getCount() > 0) {
            ImgurBaseObject imgurObject = mPagerAdapter.getImgurItem(mCurrentPosition);

            if (TextUtils.isEmpty(imgurObject.getAccount())) {
                menu.findItem(R.id.profile).setVisible(false);
            }

            if (TextUtils.isEmpty(imgurObject.getRedditLink())) {
                menu.findItem(R.id.reddit).setVisible(false);
            }

            menu.findItem(R.id.favorite).setIcon(imgurObject.isFavorited() ?
                    R.drawable.ic_action_unfavorite : R.drawable.ic_action_favorite);

            updateShareProvider((ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.share)), imgurObject);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    /**
     * Updates the share provider to the current selected object
     *
     * @param provider
     * @param imgurBaseObject
     */
    private void updateShareProvider(ShareActionProvider provider, ImgurBaseObject imgurBaseObject) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));

        String link = imgurBaseObject.getTitle() + " ";
        if (TextUtils.isEmpty(imgurBaseObject.getRedditLink())) {
            link += imgurBaseObject.getGalleryLink();
        } else {
            link += String.format("http://reddit.com%s", imgurBaseObject.getRedditLink());
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, link);
        provider.setShareIntent(shareIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the comment list is browsing replies, we need to get the original list of comments
        if (mCommentArray.size() > 0) {
            long key = mCommentArray.keyAt(0);
            outState.putParcelableArrayList(KEY_COMMENT, mCommentArray.get(key));
        } else if (mCommentAdapter != null && !mCommentAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_COMMENT, mCommentAdapter.retainItems());
        }

        outState.putBoolean(KEY_LOAD_COMMENTS, mLoadComments);
        outState.putString(KEY_SORT, mCommentSort.getSort());

        if (mPagerAdapter != null && !mPagerAdapter.isEmpty()) {
            outState.putInt(KEY_POSITION, mViewPager.getCurrentItem());
            outState.putParcelableArrayList(KEY_OBJECTS, mPagerAdapter.retainItems());
        }
    }

    private void onListItemClick(int position) {
        if (position >= 0) {
            boolean shouldClose = mCommentAdapter.setSelectedIndex(position);

            if (shouldClose) {
                mSelectedComment = null;
            } else {
                mSelectedComment = mCommentAdapter.getItem(position);
                showCommentSheet();
            }
        } else {
            // Header view
            previousComments(mCommentAdapter.getItem(0));
        }
    }

    /**
     * Shows the Sheet for selecting an option on the selected comment
     */
    private void showCommentSheet() {
        new BottomSheet.Builder(this, R.style.BottomSheet_StyleDialog)
                .title(R.string.options)
                .grid()
                .sheet(R.menu.comment_menu)
                .listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        switch (which) {
                            case R.id.upVote:
                            case R.id.downVote:
                                if (user != null) {
                                    String vote = which == R.id.upVote ? ImgurBaseObject.VOTE_UP : ImgurBaseObject.VOTE_DOWN;
                                    String url = String.format(Endpoints.COMMENT_VOTE.getUrl(), mSelectedComment.getId(), vote);
                                    RequestBody body = new FormEncodingBuilder().add("id", mSelectedComment.getId()).build();

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
                                startActivity(ProfileActivity.createIntent(ViewActivity.this, mSelectedComment.getAuthor()));
                                break;

                            case R.id.reply:
                                if (user != null) {
                                    DialogFragment fragment = CommentPopupFragment.createInstance(mPagerAdapter.getImgurItem(mCurrentPosition).getId(), mSelectedComment.getId());
                                    showDialogFragment(fragment, "comment");
                                } else {
                                    SnackBar.show(ViewActivity.this, R.string.user_not_logged_in);
                                }
                                break;
                        }
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        mSelectedComment = null;
                        mCommentAdapter.setSelectedIndex(-1);
                    }
                }).show();
    }

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_GALLERY_VOTE_COMPLETE:
                    if (msg.obj instanceof String) {
                        // To show conformation of a vote on an image/gallery, we will animate whichever vote button was pressed
                        String vote = (String) msg.obj;
                        View animateView = ImgurBaseObject.VOTE_UP.equals(vote) ? mUpVoteBtn : mDownVoteBtn;
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

                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurComment> comments = (List<ImgurComment>) msg.obj;

                    if (!comments.isEmpty()) {
                        ImgurBaseObject imgurObject = mPagerAdapter.getImgurItem(mCurrentPosition);

                        if (mCommentAdapter == null) {
                            mCommentAdapter = new CommentAdapter(ViewActivity.this, comments, ViewActivity.this);
                            mCommentAdapter.setOP(imgurObject.getAccount());
                            mCommentList.setAdapter(mCommentAdapter);
                            mCommentList.removeHeaderView(mCommentListHeader);
                        } else {
                            mCommentAdapter.setOP(imgurObject.getAccount());
                            mCommentArray.clear();
                            mPreviousCommentPositionArray.clear();
                            mCommentList.removeHeaderView(mCommentListHeader);
                            mCommentAdapter.addItems(comments);
                        }

                        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                        mMultiView.post(new Runnable() {
                            @Override
                            public void run() {
                                mCommentList.setSelection(0);
                            }
                        });
                    } else {
                        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                    }
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, msg.obj instanceof Integer ? (Integer) msg.obj : R.string.error_generic);
                    mMultiView.setErrorButtonText(R.id.errorButton, R.string.retry);
                    mMultiView.setErrorButtonClickListener(R.id.errorButton, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            loadComments();
                        }
                    });

                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;

                case MESSAGE_COMMENT_POSTED:
                    dismissDialogFragment(DIALOG_LOADING);

                    if (msg.obj instanceof Boolean) {
                        int messageResId = (Boolean) msg.obj ? R.string.comment_post_successful : R.string.comment_post_unsuccessful;
                        SnackBar.show(ViewActivity.this, messageResId);
                        // Manually refresh the comments when we successfully post a comment
                        loadComments();
                    } else {
                        SnackBar.show(ViewActivity.this, msg.obj instanceof Integer ? (Integer) msg.obj : R.string.error_generic);
                    }
                    break;

                case MESSAGE_COMMENT_VOTED:
                    if (msg.obj instanceof Boolean) {
                        int stringId = (Boolean) msg.obj ? R.string.vote_cast : R.string.error_generic;
                        SnackBar.show(ViewActivity.this, stringId);
                    } else {
                        SnackBar.show(ViewActivity.this, msg.obj instanceof Integer ? (Integer) msg.obj : R.string.error_generic);
                    }
                    break;

                case MESSAGE_ITEM_DETAILS:
                    final ArrayList<ImgurBaseObject> objects = new ArrayList<>(1);
                    objects.add((ImgurBaseObject) msg.obj);
                    mPagerAdapter = new BrowsingAdapter(getFragmentManager(), objects);
                    mViewPager.setAdapter(mPagerAdapter);
                    invalidateOptionsMenu();
                    loadComments();
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private static class ZoomOutPageTransformer implements ViewPager.PageTransformer {
        private static final float MIN_SCALE = 0.5f;

        private static final float MIN_ALPHA = 1.0f;

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
        private ArrayList<ImgurBaseObject> objects;

        public BrowsingAdapter(FragmentManager fm, ArrayList<ImgurBaseObject> objects) {
            super(fm);
            this.objects = objects;
        }

        @Override
        public Fragment getItem(int position) {
            return ImgurViewFragment.createInstance(objects.get(position));
        }

        @Override
        public int getCount() {
            return objects != null ? objects.size() : 0;
        }

        public ImgurBaseObject getImgurItem(int position) {
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

        public ArrayList<ImgurBaseObject> retainItems() {
            return new ArrayList<>(objects);
        }
    }
}
