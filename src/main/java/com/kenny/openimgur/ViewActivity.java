package com.kenny.openimgur;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.util.LongSparseArray;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.adapters.CommentAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.fragments.ImgurViewFragment;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.ui.MultiStateView;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.AsyncExecutor;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ViewActivity extends BaseActivity {
    private static final String REGEX_IMAGE_URL = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://\\S+(.jpg|.jpeg|.gif|.png)$";

    private static final String REGEX_IMGUR_IMAGE = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/(m.imgur.com|imgur.com|i.imgur.com)\\/(?!=\\/)\\w+$";

    private static final String KEY_POSITION = "position";

    private static final String KEY_OBJECTS = "objects";

    private ViewPager mViewPager;

    private SlidingUpPanelLayout mSlidingPane;

    private MultiStateView mMultiView;

    private ListView mCommentList;

    private RobotoTextView mPointText;

    private ImageButton mPanelButton;

    private CommentAdapter mCommentAdapter;

    private List<ImgurBaseObject> mImgurObjects;

    // Keeps track of the previous list of comments as we progress in the stack
    private LongSparseArray<ArrayList<ImgurComment>> mCommentArray = new LongSparseArray<ArrayList<ImgurComment>>();

    // Keeps track of the position of the comment list when navigating in the stack
    private LongSparseArray<Integer> mPreviousCommentPositionArray = new LongSparseArray<Integer>();

    private int mCurrentPosition = 0;

    private ApiClient mApiClient;

    private boolean mIsActionBarShowing = true;

    /**
     * Creates an intent for Viewing an image/album
     *
     * @param context  App context
     * @param objects  List of objects for viewing
     * @param position The position to start initially
     * @return
     */
    public static Intent createIntent(Context context, ArrayList<ImgurBaseObject> objects, int position) {
        Intent intent = new Intent(context, ViewActivity.class);
        intent.putExtra(KEY_POSITION, position);
        intent.putExtra(KEY_OBJECTS, objects);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!getIntent().hasExtra(KEY_OBJECTS) || !getIntent().hasExtra(KEY_POSITION)) {
            // TODO Error
            finish();
            return;
        }

        setContentView(R.layout.activity_view);
        mMultiView = (MultiStateView) findViewById(R.id.multiView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        mCommentList = (ListView) mMultiView.findViewById(R.id.commentList);
        mPanelButton = (ImageButton) findViewById(R.id.panelUpBtn);
        mPointText = (RobotoTextView) findViewById(R.id.pointText);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());
        mSlidingPane = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
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

        mCurrentPosition = getIntent().getIntExtra(KEY_POSITION, 0);
        mImgurObjects = getIntent().getParcelableArrayListExtra(KEY_OBJECTS);
        mViewPager.setAdapter(new BrowsingAdapter(getFragmentManager(), mImgurObjects));
        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                ImgurBaseObject imgurBaseObject = mImgurObjects.get(position);
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
                mPointText.setText(imgurBaseObject.getScore() + " " + getString(R.string.points));
                handleComments();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mPanelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSlidingPane.isPanelExpanded()) {
                    mSlidingPane.collapsePanel();
                } else {
                    mSlidingPane.expandPanel();
                }
            }
        });

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setDisplayShowHomeEnabled(true);
    }

    private static class BrowsingAdapter extends FragmentStatePagerAdapter {
        private List<ImgurBaseObject> objects;

        public BrowsingAdapter(FragmentManager fm, List<ImgurBaseObject> objects) {
            super(fm);
            this.objects = objects;
        }

        @Override
        public Fragment getItem(int position) {
            return ImgurViewFragment.createInstance(objects.get(position));
        }

        @Override
        public int getCount() {
            if (objects != null) {
                return objects.size();
            }

            return 0;
        }
    }

    /**
     * Executes the Api call for Comments
     */
    private void handleComments() {
        if (mApiClient != null) {
            AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
                @Override
                public void run() throws Exception {
                    mApiClient.doWork(ImgurBusEvent.EventType.COMMENTS, null, null);
                }
            });
        }
    }

    /**
     * Changes the Comment List to the next thread of comments
     *
     * @param comment
     */
    private void nextComments(final ImgurComment comment) {
        mCommentList.animate().translationX(-mCommentList.getWidth()).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mCommentArray.put(Long.valueOf(comment.getId()), new ArrayList<ImgurComment>(mCommentAdapter.getItems()));
                mPreviousCommentPositionArray.put(Long.valueOf(comment.getId()), mCommentList.getFirstVisiblePosition());
                mCommentAdapter.clear();
                mCommentAdapter.addComments(comment.getReplies());
                mCommentAdapter.notifyDataSetChanged();
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
        mCommentList.animate().translationX(mCommentList.getWidth()).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
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
        if (mSlidingPane.isPanelExpanded()) {
            if (mCommentAdapter != null && !mCommentAdapter.isEmpty() &&
                    mCommentArray != null && mCommentArray.size() > 0) {
                previousComments(mCommentAdapter.getItem(0));
            } else {
                mSlidingPane.collapsePanel();
            }

            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mViewPager.setCurrentItem(mCurrentPosition);
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
        mPointText = null;
        mPanelButton = null;
        mApiClient = null;
        mCommentList = null;
        mImgurObjects.clear();
        mImgurObjects = null;
        mSlidingPane = null;
        mCommentArray.clear();
        mPreviousCommentPositionArray.clear();
        mHandler.removeCallbacksAndMessages(null);

        if (mCommentAdapter != null) {
            mCommentAdapter.clear();
            mCommentAdapter = null;
        }

        super.onDestroy();
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
            if (event.eventType == ImgurBusEvent.EventType.REFRESH_TOKEN) {
                app.onReceivedRefreshToken(event.json);

                if (mApiClient != null) {
                    mApiClient.setBearerToken(app.getUser().getAccessToken());
                }
            } else if (statusCode == ApiClient.STATUS_OK && event.eventType == ImgurBusEvent.EventType.COMMENTS) {
                if (event.httpRequest == ApiClient.HttpRequest.GET) {
                    JSONArray data = event.json.getJSONArray(ApiClient.KEY_DATA);
                    List<ImgurComment> comments = new ArrayList<ImgurComment>(data.length());
                    for (int i = 0; i < data.length(); i++) {
                        comments.add(new ImgurComment(data.getJSONObject(i)));
                    }

                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, comments);
                } else if (event.httpRequest == ApiClient.HttpRequest.DELETE) {

                } else if (event.httpRequest == ApiClient.HttpRequest.POST) {

                }
            } else {
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
            }
        } catch (JSONException e) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.STATUS_JSON_EXCEPTION);
        }

    }

    /**
     * Event Method that is fired if EventBus throws an error
     *
     * @param event
     */
    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();
        e.printStackTrace();

        if (e instanceof JSONException) {
            onCommentsError(ApiClient.STATUS_JSON_EXCEPTION);
        } else if (e instanceof IOException) {
            onCommentsError(ApiClient.STATUS_IO_EXCEPTION);
        }
    }

    /**
     * Displays the appropriate error message based on the status code for the  comment list
     *
     * @param errorCode
     */
    private void onCommentsError(final int errorCode) {
        switch (errorCode) {
            case ApiClient.STATUS_EMPTY_RESPONSE:
                mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                break;

            //TODO the rest of the errors
        }
    }

    private ImgurListener mImgurListener = new ImgurListener() {
        @Override
        public void onPhotoTap(ImageView parent) {
        }

        @Override
        public void onPlayTap(ProgressBar prog, ImageView image, ImageButton play) {
        }

        @Override
        public void onLinkTap(TextView textView, String url) {
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

        @Override
        public void onVoteCast(String vote, View view) {

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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurComment> comments = (List<ImgurComment>) msg.obj;

                    if (!comments.isEmpty()) {
                        if (mCommentAdapter == null) {
                            mCommentAdapter = new CommentAdapter(getApplicationContext(), comments, mImgurListener);
                            mCommentList.setAdapter(mCommentAdapter);
                        } else {
                            mCommentArray.clear();
                            mPreviousCommentPositionArray.clear();
                            mCommentAdapter.addComments(comments);
                            mCommentAdapter.notifyDataSetChanged();
                        }

                        if (mMultiView.getViewState() != MultiStateView.ViewState.CONTENT) {
                            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                        }
                    } else {
                        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                    }

                    break;

                case MESSAGE_ACTION_FAILED:
                    onCommentsError((Integer) msg.obj);
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
}
