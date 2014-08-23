package com.kenny.openimgur;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kenny.openimgur.adapters.CommentAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.fragments.CommentPopupFragment;
import com.kenny.openimgur.fragments.ImgurViewFragment;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.ui.MultiStateView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ViewActivity extends BaseActivity implements View.OnClickListener {

    private static final String KEY_POSITION = "position";

    private static final String KEY_OBJECTS = "objects";

    private ViewPager mViewPager;

    private SlidingUpPanelLayout mSlidingPane;

    private MultiStateView mMultiView;

    private ListView mCommentList;

    private ImageButton mPanelButton;

    private ImageButton mUpVoteBtn;

    private ImageButton mDownVoteBtn;

    private CommentAdapter mCommentAdapter;

    private ImgurBaseObject[] mImgurObjects;

    // Keeps track of the previous list of comments as we progress in the stack
    private LongSparseArray<ArrayList<ImgurComment>> mCommentArray = new LongSparseArray<ArrayList<ImgurComment>>();

    // Keeps track of the position of the comment list when navigating in the stack
    private LongSparseArray<Integer> mPreviousCommentPositionArray = new LongSparseArray<Integer>();

    private int mCurrentPosition = 0;

    private ApiClient mApiClient;

    private boolean mIsActionBarShowing = true;

    private ActionMode mActionMode;

    private ImgurComment mSelectedComment;

    private String mGalleryId = null;

    private View mCommentListHeader;

    public static Intent createIntent(Context context, ImgurBaseObject[] objects, int position) {
        Intent intent = new Intent(context, ViewActivity.class);
        intent.putExtra(KEY_POSITION, position);
        intent.putExtra(KEY_OBJECTS, objects);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);
        mMultiView = (MultiStateView) findViewById(R.id.multiView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        mSlidingPane = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        mCommentListHeader = View.inflate(getApplicationContext(), R.layout.previous_comments_header, null);
        mCommentList = (ListView) mMultiView.findViewById(R.id.commentList);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPosition = position;
                loadComments();
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

        mUpVoteBtn = (ImageButton) findViewById(R.id.upVoteBtn);
        mUpVoteBtn.setOnClickListener(this);
        mDownVoteBtn = (ImageButton) findViewById(R.id.downVoteBtn);
        mDownVoteBtn.setOnClickListener(this);
        findViewById(R.id.commentBtn).setOnClickListener(this);
        if (mSlidingPane != null) initSlidingView();
        handleIntent(getIntent());
    }

    private void initSlidingView() {
        mPanelButton = (ImageButton) findViewById(R.id.panelUpBtn);
        mPanelButton.setOnClickListener(this);
        mSlidingPane.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float v) {
                if (v >= 0.75f && mIsActionBarShowing) {
                    getActionBar().hide();
                    mIsActionBarShowing = false;
                } else if (v <= 0.75 && !mIsActionBarShowing) {
                    getActionBar().show();
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

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            mGalleryId = intent.getData().getPathSegments().get(1);
        } else if (!intent.hasExtra(KEY_OBJECTS) || !intent.hasExtra(KEY_POSITION)) {
            // TODO Error
            finish();
            return;
        } else {
            mCurrentPosition = intent.getIntExtra(KEY_POSITION, 0);
            Parcelable[] passedArray = intent.getParcelableArrayExtra(KEY_OBJECTS);
            mImgurObjects = new ImgurBaseObject[passedArray.length];
            System.arraycopy(passedArray, 0, mImgurObjects, 0, passedArray.length);
        }
    }

    private void loadComments() {
        ImgurBaseObject imgurBaseObject = mImgurObjects[mCurrentPosition];
        String url = String.format(Endpoints.COMMENTS.getUrl(), imgurBaseObject.getId());

        if (mApiClient == null) {
            mApiClient = new ApiClient(url, ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setRequestType(ApiClient.HttpRequest.GET);
            mApiClient.setUrl(url);
        }

        if (mCommentAdapter != null) {
            mCommentAdapter.clear();
            mCommentAdapter.notifyDataSetChanged();
        }

        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        handleComments();
        invalidateOptionsMenu();
    }

    private static class BrowsingAdapter extends FragmentStatePagerAdapter {
        private ImgurBaseObject[] objects;

        public BrowsingAdapter(FragmentManager fm, ImgurBaseObject[] objects) {
            super(fm);
            this.objects = objects;
        }

        @Override
        public Fragment getItem(int position) {
            return ImgurViewFragment.createInstance(objects[position]);
        }

        @Override
        public int getCount() {
            return objects != null ? objects.length : 0;
        }
    }

    /**
     * Executes the Api call for Comments
     */
    private void handleComments() {
        if (mApiClient != null) {
            mApiClient.doWork(ImgurBusEvent.EventType.COMMENTS, null, null);
        }
    }

    /**
     * Changes the Comment List to the next thread of comments
     *
     * @param comment
     */

    private void nextComments(final ImgurComment comment) {
        mSelectedComment = null;
        mCommentAdapter.setSelectedIndex(-1);
        if (mActionMode != null) mActionMode.finish();

        mCommentList.animate().translationX(-mCommentList.getWidth()).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCommentArray.put(Long.valueOf(comment.getId()), new ArrayList<ImgurComment>(mCommentAdapter.getItems()));
                mPreviousCommentPositionArray.put(Long.valueOf(comment.getId()), mCommentList.getFirstVisiblePosition());
                mCommentAdapter.clear();

                if (mCommentList.getHeaderViewsCount() <= 0) {
                    mCommentList.addHeaderView(mCommentListHeader);
                }

                mCommentAdapter.addComments(comment.getReplies());
                mCommentAdapter.notifyDataSetChanged();
                mCommentList.setSelection(0);
                ObjectAnimator.ofFloat(mCommentList, "translationX", mCommentList.getWidth(), 0).setDuration(250).start();
            }
        }).start();
    }

    /**
     * Changes the comment list to the previous thread of comments
     *
     * @param comment
     */
    private void previousComments(final ImgurComment comment) {
        mSelectedComment = null;
        mCommentAdapter.setSelectedIndex(-1);
        if (mActionMode != null) mActionMode.finish();

        mCommentList.animate().translationX(mCommentList.getWidth()).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mPreviousCommentPositionArray.size() <= 1) {
                    mCommentList.removeHeaderView(mCommentListHeader);
                }

                mCommentAdapter.clear();
                mCommentAdapter.addComments(mCommentArray.get(comment.getParentId()));
                mCommentArray.remove(comment.getParentId());
                mCommentAdapter.notifyDataSetChanged();
                mCommentList.setSelection(mPreviousCommentPositionArray.get(comment.getParentId()));
                mPreviousCommentPositionArray.remove(comment.getParentId());
                ObjectAnimator.ofFloat(mCommentList, "translationX", -mCommentList.getWidth(), 0).setDuration(250).start();
            }
        }).start();
    }

    @Override
    public void onBackPressed() {
        if (mSlidingPane != null && mSlidingPane.isPanelExpanded()) {
            if (mCommentAdapter != null && !mCommentAdapter.isEmpty() &&
                    mCommentArray != null && mCommentArray.size() > 0) {
                previousComments(mCommentAdapter.getItem(0));
            } else {
                mSlidingPane.collapsePanel();
            }

            return;
        } else if (mSlidingPane == null && mCommentAdapter != null && !mCommentAdapter.isEmpty() &&
                mCommentArray != null && mCommentArray.size() > 0) {
            previousComments(mCommentAdapter.getItem(0));
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Check if the activity was opened externally by a link click
        if (!TextUtils.isEmpty(mGalleryId)) {
            mApiClient = new ApiClient(String.format(Endpoints.GALLERY_ITEM_DETAILS.getUrl(), mGalleryId), ApiClient.HttpRequest.GET);
            mApiClient.doWork(ImgurBusEvent.EventType.GALLERY_ITEM_INFO, null, null);
            mGalleryId = null;
        } else {
            mViewPager.setAdapter(new BrowsingAdapter(getFragmentManager(), mImgurObjects));

            // If we start on the 0 page, the onPageSelected event won't fire
            if (mCurrentPosition == 0) {
                loadComments();
            } else {
                mViewPager.setCurrentItem(mCurrentPosition);
            }
        }

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
        mPanelButton = null;
        mApiClient = null;
        mCommentList = null;
        mImgurObjects = null;
        mSlidingPane = null;
        mCommentArray.clear();
        mCommentArray = null;
        mPreviousCommentPositionArray.clear();
        mPreviousCommentPositionArray = null;
        mHandler.removeCallbacksAndMessages(null);

        if (mCommentAdapter != null) {
            mCommentAdapter.clear();
            mCommentAdapter = null;
        }

        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.panelUpBtn:
                if (mSlidingPane.isPanelExpanded()) {
                    mSlidingPane.collapsePanel();
                } else {
                    mSlidingPane.expandPanel();
                }

                break;

            case R.id.upVoteBtn:
            case R.id.downVoteBtn:
                if (user != null) {
                    String vote = view.getId() == R.id.upVoteBtn ? ImgurBaseObject.VOTE_UP : ImgurBaseObject.VOTE_DOWN;
                    String upVoteUrl = String.format(Endpoints.GALLERY_VOTE.getUrl(), mImgurObjects[mCurrentPosition].getId(), vote);

                    if (mApiClient == null) {
                        mApiClient = new ApiClient(upVoteUrl, ApiClient.HttpRequest.GET);
                    } else {
                        mApiClient.setUrl(upVoteUrl);
                        mApiClient.setRequestType(ApiClient.HttpRequest.GET);
                    }

                    mApiClient.doWork(ImgurBusEvent.EventType.GALLERY_VOTE, vote, null);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.commentBtn:
                if (user != null && user.isAccessTokenValid()) {
                    Fragment fragment = CommentPopupFragment.createInstance(mImgurObjects[mCurrentPosition].getId(), null);
                    getFragmentManager().beginTransaction().add(fragment, "comment").commit();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

            switch (event.eventType) {
                case COMMENTS:
                    if (statusCode == ApiClient.STATUS_OK) {
                        if (event.httpRequest == ApiClient.HttpRequest.GET) {
                            JSONArray data = event.json.getJSONArray(ApiClient.KEY_DATA);
                            List<ImgurComment> comments = new ArrayList<ImgurComment>(data.length());
                            for (int i = 0; i < data.length(); i++) {
                                comments.add(new ImgurComment(data.getJSONObject(i)));
                            }

                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, comments);
                        } else if (event.httpRequest == ApiClient.HttpRequest.POST) {
                            // TODO notify user
                        }
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
                    }

                    break;

                case GALLERY_VOTE:
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, event.id);
                    break;

                case GALLERY_ITEM_INFO:
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
                        mImgurObjects = new ImgurBaseObject[]{(ImgurBaseObject) imgurObject};

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mViewPager.setAdapter(new BrowsingAdapter(getFragmentManager(), mImgurObjects));
                                invalidateOptionsMenu();
                                loadComments();
                            }
                        });
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
                    }
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
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
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.STATUS_IO_EXCEPTION);
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.STATUS_JSON_EXCEPTION);
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.STATUS_INTERNAL_ERROR);
        }

        event.getThrowable().printStackTrace();
    }

    private ImgurListener mImgurListener = new ImgurListener() {
        @Override
        public void onPhotoTap(ImageView parent) {
        }

        @Override
        public void onPlayTap(ProgressBar prog, ImageView image, ImageButton play) {
        }

        @Override
        public void onLinkTap(View view, String url) {
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
            } else {
                onListItemClick(mCommentList.getPositionForView(view));
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
    };

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
                    final ImgurBaseObject imgurObj = mImgurObjects[mCurrentPosition];
                    String url;

                    if (imgurObj instanceof ImgurAlbum) {
                        url = String.format(Endpoints.FAVORITE_ALBUM.getUrl(), imgurObj.getId());
                    } else {
                        url = String.format(Endpoints.FAVORITE_IMAGE.getUrl(), imgurObj.getId());
                    }

                    final RequestBody body = new FormEncodingBuilder().add("id", imgurObj.getId()).build();
                    mApiClient.setUrl(url);
                    mApiClient.setRequestType(ApiClient.HttpRequest.POST);
                    mApiClient.doWork(ImgurBusEvent.EventType.FAVORITE, imgurObj.getId(), body);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
                }
                return true;

            case R.id.refresh:
                loadComments();
                return true;

            case R.id.profile:
                startActivity(ProfileActivity.createIntent(getApplicationContext(), mImgurObjects[mCurrentPosition].getAccount()));
                return true;

            case R.id.reddit:
                String url = String.format("http://reddit.com%s", mImgurObjects[mCurrentPosition].getRedditLink());
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mImgurObjects != null) {
            if (TextUtils.isEmpty(mImgurObjects[mCurrentPosition].getAccount())) {
                menu.findItem(R.id.profile).setVisible(false);
            }

            if (TextUtils.isEmpty(mImgurObjects[mCurrentPosition].getRedditLink())) {
                menu.findItem(R.id.reddit).setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void onListItemClick(int position) {
        if (position >= 0) {
            boolean shouldClose = mCommentAdapter.setSelectedIndex(position);

            if (shouldClose) {
                mSelectedComment = null;
                if (mActionMode != null) mActionMode.finish();
            } else {
                mSelectedComment = mCommentAdapter.getItem(position);
                if (mActionMode == null) mActionMode = startActionMode(new ActionBarCallBack());
            }
        } else {
            // Header view
            previousComments(mCommentAdapter.getItem(0));
        }
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    Object object = msg.obj;

                    if (object instanceof List) {
                        List<ImgurComment> comments = (List<ImgurComment>) msg.obj;

                        if (!comments.isEmpty()) {
                            if (mCommentAdapter == null) {
                                mCommentAdapter = new CommentAdapter(getApplicationContext(), comments, mImgurListener);
                                // Add and remove the header view for pre 4.4 header support
                                mCommentList.addHeaderView(mCommentListHeader);
                                mCommentList.removeHeaderView(mCommentListHeader);
                                mCommentList.setAdapter(mCommentAdapter);
                            } else {
                                mCommentArray.clear();
                                mPreviousCommentPositionArray.clear();
                                mCommentList.removeHeaderView(mCommentListHeader);
                                mCommentAdapter.addComments(comments);
                                mCommentAdapter.notifyDataSetChanged();
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
                    } else if (object instanceof String) {
                        String vote = (String) msg.obj;
                        View animateView = ImgurBaseObject.VOTE_UP.equals(vote) ? mUpVoteBtn : mDownVoteBtn;
                        AnimatorSet set = new AnimatorSet();
                        set.playTogether(
                                ObjectAnimator.ofFloat(animateView, "scaleY", 1.0f, 1.5f, 1.0f),
                                ObjectAnimator.ofFloat(animateView, "scaleX", 1.0f, 1.5f, 1.0f)
                        );

                        set.setDuration(1000L).setInterpolator(new OvershootInterpolator());
                        set.start();
                    }

                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, ApiClient.getErrorCodeStringResource((Integer) msg.obj));
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;
            }

            super.handleMessage(msg);
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

    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.upVote:
                case R.id.downVote:
                    if (user != null) {
                        String vote = item.getItemId() == R.id.upVote ? ImgurBaseObject.VOTE_UP : ImgurBaseObject.VOTE_DOWN;
                        String url = String.format(Endpoints.COMMENT_VOTE.getUrl(), mSelectedComment.getId(), vote);
                        RequestBody body = new FormEncodingBuilder().add("id", mSelectedComment.getId()).build();
                        mApiClient.setUrl(url);
                        mApiClient.setRequestType(ApiClient.HttpRequest.POST);
                        mApiClient.doWork(ImgurBusEvent.EventType.COMMENTS, null, body);
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
                    }

                    mode.finish();
                    return true;

                case R.id.profile:
                    startActivity(ProfileActivity.createIntent(getApplicationContext(), mSelectedComment.getAuthor()));
                    mode.finish();
                    return true;

                case R.id.reply:
                    if (user != null) {
                        Fragment fragment = CommentPopupFragment.createInstance(mImgurObjects[mCurrentPosition].getId(), mSelectedComment.getId());
                        getFragmentManager().beginTransaction().add(fragment, "comment").commit();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.user_not_logged_in, Toast.LENGTH_SHORT).show();
                    }

                    return true;
            }

            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.comment_cab_menu, menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            mSelectedComment = null;
            mCommentAdapter.setSelectedIndex(-1);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

    }
}