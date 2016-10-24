package com.kenny.openimgur.activities;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
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
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.api.responses.BasicObjectResponse;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.CommentPostResponse;
import com.kenny.openimgur.api.responses.CommentResponse;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.fragments.CommentPopupFragment;
import com.kenny.openimgur.fragments.ImgurViewFragment;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.fragments.SideGalleryFragment;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.ui.adapters.CommentAdapter;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.bottomsheet.BottomSheet;
import com.kennyc.bottomsheet.BottomSheetListener;
import com.kennyc.view.MultiStateView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ViewActivity extends BaseActivity implements View.OnClickListener, ImgurListener, SideGalleryFragment.SideGalleryListener, CommentPopupFragment.CommentListener {

    private enum CommentSort {
        BEST, NEW, TOP, WORST;

        public String getApiValue() {
            switch (this) {
                case BEST:
                case NEW:
                case TOP:
                    return name().toLowerCase(Locale.ENGLISH);

                case WORST:
                    return "top";

                default:
                    return "best";
            }
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

                    case WORST:
                        values[i] = context.getString(R.string.sort_worst);
                }
            }

            return values;
        }

        public static CommentSort getSortFromString(String item) {
            for (CommentSort s : CommentSort.values()) {
                if (s.name().equalsIgnoreCase(item)) {
                    return s;
                }
            }

            return BEST;
        }
    }

    private static final String KEY_VIEW_FOR_ALBUM = "view_link_for_album";

    private static final String KEY_COMMENT = "comments";

    private static final String KEY_POSITION = "position";

    private static final String KEY_OBJECTS = "objects";

    private static final String KEY_SORT = "commentSort";

    private static final String KEY_LOAD_COMMENTS = "autoLoadComments";

    private static final String KEY_PANEL_EXPANDED = "panelExpanded";

    private static final String PREF_HIDE_PANEL = "hide_panel";

    public static final String KEY_ENDING_ITEM = "ending_item";

    @BindView(R.id.pager)
    ViewPager mViewPager;

    @BindView(R.id.sliding_layout)
    LinearLayout mSlidingRoot;

    @BindView(R.id.multiView)
    MultiStateView mMultiView;

    @BindView(R.id.commentList)
    RecyclerView mCommentList;

    @BindView(R.id.panelUpBtn)
    ImageButton mPanelButton;

    @BindView(R.id.upVoteBtn)
    ImageButton mUpVoteBtn;

    @BindView(R.id.downVoteBtn)
    ImageButton mDownVoteBtn;

    CommentAdapter mCommentAdapter;

    int mCurrentPosition = 0;

    boolean mIsActionBarShowing = true;

    String mGalleryId = null;

    boolean mIsResuming = false;

    boolean mLoadComments;

    BrowsingAdapter mPagerAdapter;

    CommentSort mCommentSort;

    SideGalleryFragment mSideGalleryFragment;

    BottomSheetBehavior mBottomSheetBehavior;

    public static Intent createIntent(Context context, ArrayList<ImgurBaseObject> objects, int position) {
        Intent intent = new Intent(context, ViewActivity.class);
        intent.putExtra(KEY_POSITION, position);
        intent.putExtra(KEY_OBJECTS, objects);
        return intent;
    }

    public static Intent createIntent(Context context, String url, boolean isAlbumLink) {
        return new Intent(context, ViewActivity.class)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(url))
                .putExtra(KEY_VIEW_FOR_ALBUM, isAlbumLink);
    }

    public static Intent createGalleryIntent(Context context, String id) {
        String url = String.format("http://imgur.com/gallery/%s", id);
        return createIntent(context, url, false);
    }

    public static Intent createAlbumIntent(Context context, String id) {
        String url = String.format("http://imgur.com/a/%s", id);
        return createIntent(context, url, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        mCommentList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mSideGalleryFragment = (SideGalleryFragment) getFragmentManager().findFragmentById(R.id.sideGallery);
        ((Button) mMultiView.getView(MultiStateView.VIEW_STATE_ERROR).findViewById(R.id.errorButton)).setText(R.string.load_comments);

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
        mBottomSheetBehavior = BottomSheetBehavior.from(mSlidingRoot);
        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        if (mPanelButton.getRotation() == 180) {
                            ObjectAnimator.ofFloat(mPanelButton, "rotation", 180, 0).setDuration(200).start();
                        }
                        break;

                    case BottomSheetBehavior.STATE_EXPANDED:
                        if (mPanelButton.getRotation() == 0) {
                            ObjectAnimator.ofFloat(mPanelButton, "rotation", 0, 180).setDuration(200).start();
                        }
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (slideOffset >= 0.75f && mIsActionBarShowing) {
                    getSupportActionBar().hide();
                    mIsActionBarShowing = false;
                } else if (slideOffset <= 0.75 && !mIsActionBarShowing) {
                    getSupportActionBar().show();
                    mIsActionBarShowing = true;
                }
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
            Snackbar.make(mViewPager, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            finish();
        } else {
            mCurrentPosition = intent.getIntExtra(KEY_POSITION, 0);
            ArrayList<ImgurBaseObject> objects = intent.getParcelableArrayListExtra(KEY_OBJECTS);
            mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);

            if (mSideGalleryFragment != null) {
                mSideGalleryFragment.addGalleryItems(objects);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        ImgurBaseObject obj = mPagerAdapter != null ? mPagerAdapter.getImgurItem(mCurrentPosition) : null;
        setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_ENDING_ITEM, obj));
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mBottomSheetBehavior != null) {
            boolean hidePanel = app.getPreferences().getBoolean(PREF_HIDE_PANEL, false);
            adjustPeekSize(hidePanel ? 0 : ViewUtils.getActionBarHeight(this));
            menu.findItem(R.id.hideBar).setChecked(hidePanel);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.hideBar:
                boolean isChecked = item.isChecked();
                item.setChecked(!isChecked);
                app.getPreferences().edit().putBoolean(PREF_HIDE_PANEL, !isChecked).apply();
                adjustPeekSize(isChecked ? 0 : ViewUtils.getActionBarHeight(this));
                return true;

            case android.R.id.home:
                ImgurBaseObject obj = mPagerAdapter != null ? mPagerAdapter.getImgurItem(mCurrentPosition) : null;
                setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_ENDING_ITEM, obj));
                // Not retuning on purpose
                break;
        }

        return super.onOptionsItemSelected(item);
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
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
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
            mCommentSort = CommentSort.getSortFromString(savedInstanceState.getString(KEY_SORT, CommentSort.NEW.name()));
            mLoadComments = savedInstanceState.getBoolean(KEY_LOAD_COMMENTS, true);
            mIsResuming = true;
            mCurrentPosition = savedInstanceState.getInt(KEY_POSITION, 0);
            ArrayList<ImgurBaseObject> objects = savedInstanceState.getParcelableArrayList(KEY_OBJECTS);
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
                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            } else {
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.comments_off);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }

            if (savedInstanceState.getBoolean(KEY_PANEL_EXPANDED, false))
                getSupportActionBar().hide();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCommentAdapter != null) {
            CustomLinkMovement.getInstance().addListener(this);
        }
    }

    @Override
    protected void onPause() {
        CustomLinkMovement.getInstance().removeListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        dismissDialogFragment("comment");

        if (mCommentAdapter != null) {
            mCommentAdapter.onDestroy();
            mCommentAdapter = null;
        }

        if (mPagerAdapter != null) {
            mPagerAdapter.clear();
            mPagerAdapter = null;
        }

        super.onDestroy();
    }

    @OnClick({R.id.panelUpBtn, R.id.upVoteBtn, R.id.downVoteBtn, R.id.commentBtn, R.id.sortComments, R.id.errorButton})
    @Override
    public void onClick(View view) {
        if (!canDoFragmentTransaction()) return;

        switch (view.getId()) {
            case R.id.panelUpBtn:
                if (mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else {
                    mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                break;

            case R.id.upVoteBtn:
            case R.id.downVoteBtn:
                if (user != null && mPagerAdapter != null) {
                    ImgurBaseObject obj = mPagerAdapter.getImgurItem(mCurrentPosition);
                    if (obj == null) return;

                    String vote = view.getId() == R.id.upVoteBtn ? ImgurBaseObject.VOTE_UP : ImgurBaseObject.VOTE_DOWN;
                    obj.setVote(vote);

                    ImgurViewFragment fragment = (ImgurViewFragment) mPagerAdapter.instantiateItem(mViewPager, mCurrentPosition);

                    if (fragment != null && fragment.isAdded() && fragment.getUserVisibleHint()) {
                        fragment.setVote(vote);
                    }

                    voteOnGallery(obj.getId(), vote);
                } else {
                    Snackbar.make(mViewPager, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
                }
                break;

            case R.id.commentBtn:
                if (user != null && mPagerAdapter != null) {
                    DialogFragment fragment = CommentPopupFragment.createInstance(mPagerAdapter.getImgurItem(mCurrentPosition).getId(), null);
                    showDialogFragment(fragment, "comment");
                } else {
                    Snackbar.make(mViewPager, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
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
                                    app.getPreferences().edit().putString(KEY_SORT, mCommentSort.name()).apply();

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
                    String id = LinkUtils.getGalleryId(url);

                    if (!TextUtils.isEmpty(id)) {
                        startActivity(ViewActivity.createGalleryIntent(getApplicationContext(), id).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                    break;

                case ALBUM:
                    String albumId = LinkUtils.getAlbumId(url);
                    Intent intent = ViewActivity.createAlbumIntent(getApplicationContext(), albumId).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case IMAGE_URL_QUERY:
                    int index = url.indexOf("?");
                    url = url.substring(0, index);
                    // Intentional fallthrough
                case IMAGE_URL:
                    boolean isDirectLink = LinkUtils.isDirectImageLink(url);
                    if (!isDirectLink) {
                        url = LinkUtils.getId(url);
                    }

                    getFragmentManager().beginTransaction().add(PopupImageDialogFragment.getInstance(url, LinkUtils.isLinkAnimated(url), isDirectLink, false), "popup").commitAllowingStateLoss();
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
                    String uName = url.replace("@", "");

                    if ("op".equalsIgnoreCase(uName)) {
                        try {
                            uName = mPagerAdapter.getImgurItem(mCurrentPosition).getAccount();
                        } catch (Exception ex) {
                            LogUtil.e(TAG, "Unable to determine OP username from " + uName, ex);
                            uName = null;
                        }
                    }

                    if (!TextUtils.isEmpty(uName))
                        startActivity(ProfileActivity.createIntent(getApplicationContext(), uName));
                    break;

                case USER:
                    String username = LinkUtils.getUsername(url);

                    if (!TextUtils.isEmpty(username)) {
                        startActivity(ProfileActivity.createIntent(getApplicationContext(), username));
                    }
                    break;

                case TOPIC:
                    String topicId = LinkUtils.getTopicGalleryId(url);
                    startActivity(ViewActivity.createGalleryIntent(getApplicationContext(), topicId));
                    break;

                case NONE:
                default:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        Snackbar.make(mViewPager, R.string.cant_launch_intent, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        } else {
            if (view.getLayoutParams() instanceof RecyclerView.LayoutParams) {
                onListItemClick(mCommentList.getChildAdapterPosition(view));
            } else {
                // This is super ugly, in order to the get position from the RecyclerView, we need the root view
                View parent = (View) view.getParent();
                if (parent != null) parent = (View) parent.getParent();

                if (parent != null && parent.getLayoutParams() instanceof RecyclerView.LayoutParams) {
                    onListItemClick(mCommentList.getChildAdapterPosition(parent));
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCommentAdapter != null && !mCommentAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_COMMENT, mCommentAdapter.retainItems());
        }

        outState.putBoolean(KEY_LOAD_COMMENTS, mLoadComments);
        outState.putString(KEY_SORT, mCommentSort.name());

        if (mPagerAdapter != null && !mPagerAdapter.isEmpty()) {
            outState.putInt(KEY_POSITION, mViewPager.getCurrentItem());
            outState.putParcelableArrayList(KEY_OBJECTS, mPagerAdapter.retainItems());
        }

        if (mBottomSheetBehavior != null) {
            outState.putBoolean(KEY_PANEL_EXPANDED, mBottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void onListItemClick(final int position) {
        if (mCommentAdapter == null) return;

        if (position != RecyclerView.NO_POSITION) {
            boolean shouldClose = mCommentAdapter.setSelectedIndex(position);

            if (!shouldClose) {
                final ImgurComment comment = mCommentAdapter.getItem(position);

                if (TextUtils.isEmpty(comment.getAuthor()) || comment.getAuthor().equals("[deleted]")) {
                    mCommentAdapter.setSelectedIndex(-1);
                    return;
                }

                new BottomSheet.Builder(this)
                        .setSheet(R.menu.comment_menu)
                        .setStyle(theme.getBottomSheetTheme())
                        .grid()
                        .setTitle(R.string.options)
                        .setListener(new BottomSheetListener() {
                            @Override
                            public void onSheetShown(BottomSheet bottomSheet) {
                                LogUtil.v(TAG, "ImgurComment Selected " + comment);
                            }

                            @Override
                            public void onSheetItemSelected(BottomSheet bottomSheet, MenuItem menuItem) {
                                int id = menuItem.getItemId();

                                switch (id) {
                                    case R.id.upVote:
                                    case R.id.downVote:
                                        if (user != null) {
                                            String vote = id == R.id.upVote ? ImgurBaseObject.VOTE_UP : ImgurBaseObject.VOTE_DOWN;
                                            comment.setVote(vote);
                                            mCommentAdapter.notifyItemChanged(position);
                                            voteOnComment(comment.getId(), vote);
                                        } else {
                                            Snackbar.make(mViewPager, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
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
                                            Snackbar.make(mViewPager, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
                                        }
                                        break;

                                    case R.id.copy:
                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText(getString(R.string.comment), comment.getComment());
                                        clipboard.setPrimaryClip(clip);
                                        Snackbar.make(mViewPager, R.string.comment_copied, Snackbar.LENGTH_LONG).show();
                                        break;
                                }
                            }

                            @Override
                            public void onSheetDismissed(BottomSheet bottomSheet, int which) {
                                mCommentAdapter.setSelectedIndex(-1);
                            }
                        })
                        .show();
            }
        }
    }

    public void fetchComments() {
        if (mLoadComments && mPagerAdapter != null) {
            ImgurBaseObject imgurBaseObject = mPagerAdapter.getImgurItem(mCurrentPosition);

            if (imgurBaseObject == null) {
                LogUtil.w(TAG, "Object returned is null, can not load comments");
                return;
            }

            if (imgurBaseObject.isListed()) {
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                ApiClient.getService().getComments(imgurBaseObject.getId(), mCommentSort.getApiValue()).enqueue(new Callback<CommentResponse>() {
                    @Override
                    public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                        if (mPagerAdapter == null || mPagerAdapter.getImgurItem(mCurrentPosition) == null) {
                            return;
                        }

                        if (response == null || response.body() == null) {
                            ViewUtils.setErrorText(mMultiView, R.id.errorMessage, response != null ? ApiClient.getErrorCode(response.code()) : R.string.error_generic);
                            mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                            return;
                        }

                        CommentResponse commentResponse = response.body();

                        if (commentResponse.hasComments()) {
                            ImgurBaseObject imgurBaseObject = mPagerAdapter.getImgurItem(mCurrentPosition);
                            String imageId = commentResponse.data.get(0).getImageId();

                            if (TextUtils.isEmpty(imageId) || imgurBaseObject == null) {
                                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                                return;
                            }

                            if (imageId.equals(imgurBaseObject.getId())) {
                                // Reverse the list for worst sorting as it will be loading the Top comments
                                if (mCommentSort == CommentSort.WORST)
                                    Collections.reverse(commentResponse.data);

                                // We only show the comments for the correct gallery item
                                if (mCommentAdapter == null) {
                                    mCommentAdapter = new CommentAdapter(ViewActivity.this, commentResponse.data, ViewActivity.this);
                                    mCommentList.setAdapter(mCommentAdapter);
                                } else {
                                    mCommentAdapter.clear();
                                    mCommentAdapter.clearExpansionInfo();
                                    mCommentAdapter.addItems(commentResponse.data);
                                }

                                mCommentAdapter.setOP(imgurBaseObject.getAccount());
                                mCommentAdapter.clearExpansionInfo();
                                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);

                                mMultiView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mCommentList != null) mCommentList.scrollToPosition(0);
                                    }
                                });
                            }
                        } else {
                            mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                        }
                    }

                    @Override
                    public void onFailure(Call<CommentResponse> call, Throwable t) {
                        LogUtil.e(TAG, "Error fetching comments", t);
                        ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                    }
                });
            } else {
                mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                ViewUtils.setEmptyText(mMultiView, R.id.emptyMessage, R.string.comments_unlisted);
            }
        } else if (mMultiView != null && mMultiView.getViewState() != MultiStateView.VIEW_STATE_ERROR) {
            ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.comments_off);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }
    }

    private void fetchItemDetails(final String id, final boolean isAlbum) {
        if (isAlbum) {
            ApiClient.getService().getAlbumImages(id).enqueue(new Callback<AlbumResponse>() {
                @Override
                public void onResponse(Call<AlbumResponse> call, Response<AlbumResponse> response) {
                    if (response == null || response.body() == null) {
                        ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                        return;
                    }

                    AlbumResponse albumResponse = response.body();
                    ImgurAlbum album = new ImgurAlbum(mGalleryId, null, getIntent().getData().toString());
                    mGalleryId = null;
                    album.addPhotosToAlbum(albumResponse.data);
                    final ArrayList<ImgurBaseObject> objects = new ArrayList<>(1);
                    objects.add(album);
                    mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);
                    mViewPager.setAdapter(mPagerAdapter);
                    invalidateOptionsMenu();
                    fetchComments();
                }

                @Override
                public void onFailure(Call<AlbumResponse> call, Throwable t) {
                    LogUtil.e(TAG, "Unable to fetch album details", t);
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            });
        } else {
            ApiClient.getService().getGalleryDetails(id).enqueue(new Callback<BasicObjectResponse>() {
                @Override
                public void onResponse(Call<BasicObjectResponse> call, Response<BasicObjectResponse> response) {
                    if (response != null && response.body() != null && response.body().data != null) {
                        final ArrayList<ImgurBaseObject> objects = new ArrayList<>(1);
                        objects.add(response.body().data);
                        mPagerAdapter = new BrowsingAdapter(getApplicationContext(), getFragmentManager(), objects);
                        mViewPager.setAdapter(mPagerAdapter);
                        invalidateOptionsMenu();
                        fetchComments();
                    } else {
                        ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                    }
                }

                @Override
                public void onFailure(Call<BasicObjectResponse> call, Throwable t) {
                    LogUtil.e(TAG, "Unable to fetch album details", t);
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            });
        }
    }

    private void voteOnGallery(String id, final String vote) {
        ApiClient.getService().voteOnGallery(id, vote, vote).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (response != null && response.body() != null && response.body().data) {
                    View animateView = ImgurBaseObject.VOTE_UP.equals(vote) ? mUpVoteBtn : mDownVoteBtn;
                    AnimatorSet set = new AnimatorSet();
                    set.playTogether(
                            ObjectAnimator.ofFloat(animateView, "scaleY", 1.0f, 2.0f, 1.0f),
                            ObjectAnimator.ofFloat(animateView, "scaleX", 1.0f, 2.0f, 1.0f)
                    );

                    set.setDuration(1500L).setInterpolator(new OvershootInterpolator());
                    set.start();
                } else {
                    Snackbar.make(mViewPager, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                LogUtil.e(TAG, "Unable to vote on gallery", t);
                Snackbar.make(mViewPager, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    void voteOnComment(String id, final String vote) {
        ApiClient.getService().voteOnComment(id, vote, vote).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (response.body() != null)
                    LogUtil.v(TAG, "Result of comment voting " + response.body().data);
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                LogUtil.e(TAG, "Unable to vote on comment", t);
            }
        });
    }

    @Override
    public void onPostComment(String comment, String galleryId, String parentId) {
        final @MultiStateView.ViewState int viewState = mMultiView.getViewState();
        mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
        Call<CommentPostResponse> call;

        if (!TextUtils.isEmpty(parentId)) {
            call = ApiClient.getService().postCommentReply(galleryId, parentId, comment);
        } else {
            call = ApiClient.getService().postComment(galleryId, comment);
        }

        call.enqueue(new Callback<CommentPostResponse>() {
            @Override
            public void onResponse(Call<CommentPostResponse> call, Response<CommentPostResponse> response) {
                if (response != null && response.body() != null && response.body().data != null && !TextUtils.isEmpty(response.body().data.id)) {
                    Snackbar.make(mViewPager, R.string.comment_post_successful, Snackbar.LENGTH_LONG).show();
                    fetchComments();
                } else {
                    Snackbar.make(mViewPager, R.string.comment_post_unsuccessful, Snackbar.LENGTH_LONG).show();
                    mMultiView.setViewState(viewState);
                }
            }

            @Override
            public void onFailure(Call<CommentPostResponse> call, Throwable t) {
                LogUtil.e(TAG, "Unable to post comment", t);
                Snackbar.make(mViewPager, R.string.comment_post_unsuccessful, Snackbar.LENGTH_LONG).show();
                mMultiView.setViewState(viewState);
            }
        });
    }

    private void adjustPeekSize(int size) {
        mBottomSheetBehavior.setPeekHeight(size);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mViewPager.getLayoutParams();
        lp.setMargins(0, 0, 0, size);
        mViewPager.setLayoutParams(lp);
    }

    static class ZoomOutPageTransformer implements ViewPager.PageTransformer {
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
        private ArrayList<ImgurBaseObject> objects;

        private boolean mDisplayTags;

        public BrowsingAdapter(Context context, FragmentManager fm, ArrayList<ImgurBaseObject> objects) {
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
        public ImgurBaseObject getImgurItem(int position) {
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

        public ArrayList<ImgurBaseObject> retainItems() {
            return new ArrayList<>(objects);
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_View_Dark : R.style.Theme_Opengur_Light_View_Light;
    }
}
