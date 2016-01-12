package com.kenny.openimgur.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.amulyakhare.textdrawable.MaterialColor;
import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.MessagesAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.ConversationResponse;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.fragments.PopupImageDialogFragment;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by kcampagna on 12/25/14.
 */
public class ConvoThreadActivity extends BaseActivity implements ImgurListener {
    public static final String KEY_BLOCKED_CONVO = "blocked_convo";

    private static final String KEY_CONVO = "convo";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.convoList)
    RecyclerView mConvoList;

    @Bind(R.id.messageInput)
    EditText mMessageInput;

    private ImgurConvo mConvo;

    private MessagesAdapter mAdapter;

    private boolean mHasMore = true;

    private boolean mIsLoading = false;

    private boolean mHasScrolledInitially = false;

    private int mCurrentPage = 1;

    private LinearLayoutManager mLayoutManager;

    public static Intent createIntent(Context context, @NonNull ImgurConvo convo) {
        return new Intent(context, ConvoThreadActivity.class).putExtra(KEY_CONVO, convo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convo_thread);
        mConvoList.setLayoutManager(mLayoutManager = new LinearLayoutManager(getApplicationContext()));
        handleData(savedInstanceState);

        mConvoList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int totalItemCount = mLayoutManager.getItemCount();
                int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();

                if (mHasMore && !mIsLoading && totalItemCount > 0 && firstVisiblePosition == 0 && mHasScrolledInitially) {
                    mCurrentPage++;
                    fetchMessages();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.convo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);

                if (mAdapter != null) mAdapter.clear();
                mHasMore = true;
                mCurrentPage = 1;
                fetchMessages();
                break;

            case R.id.block:
                blockUser();
                break;

            case R.id.report:
                reportUser();
                break;

            case R.id.profile:
                startActivity(ProfileActivity.createIntent(getApplicationContext(), mConvo.getWithAccount()));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleData(Bundle savedInstance) {
        if (savedInstance != null && savedInstance.containsKey(KEY_CONVO)) {
            mConvo = savedInstance.getParcelable(KEY_CONVO);
        } else {
            mConvo = getIntent().getParcelableExtra(KEY_CONVO);
        }

        if (mConvo.getMessages() != null && !mConvo.getMessages().isEmpty()) {
            mAdapter = new MessagesAdapter(getApplicationContext(), MaterialColor.getColor(mConvo.getWithAccount()), mConvo.getMessages(), this);
            mConvoList.setAdapter(mAdapter);
            mConvoList.scrollToPosition(mAdapter.getItemCount() - 1);
            mHasScrolledInitially = true;
        }

        getSupportActionBar().setTitle(mConvo.getWithAccount());
    }

    @OnClick({R.id.sendBtn})
    void onSendClick() {
        String message = mMessageInput.getText().toString();

        if (!TextUtils.isEmpty(message.trim())) {
            sendMessage(ImgurMessage.createMessage(message, user.getId()));
        } else {
            Snackbar.make(mMultiView, R.string.convo_message_hint, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            fetchMessages();
        }

        if (mAdapter != null) CustomLinkMovement.getInstance().addListener(this);
    }

    @Override
    protected void onPause() {
        CustomLinkMovement.getInstance().removeListener(this);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroy();
    }

    private void fetchMessages() {
        // Having an id of -1 means that they are starting the convo from the Info fragment where an id is not known
        if (mConvo.getId().equals("-1")) {
            onEmpty();
            return;
        }

        mIsLoading = true;
        ApiClient.getService().getMessages(mConvo.getId(), mCurrentPage).enqueue(new Callback<ConversationResponse>() {
            @Override
            public void onResponse(Response<ConversationResponse> response, Retrofit retrofit) {
                mIsLoading = false;

                if (response == null || response.body() == null) {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                    return;
                }

                ConversationResponse conversationResponse = response.body();

                if (conversationResponse.data.hasMessages()) {
                    boolean scrollToBottom = false;

                    if (mAdapter == null) {
                        mAdapter = new MessagesAdapter(getApplicationContext(), MaterialColor.getColor(mConvo.getWithAccount()), conversationResponse.data.getMessages(), ConvoThreadActivity.this);
                        mConvoList.setAdapter(mAdapter);
                        // Start at the bottom of the list when we receive the first set of messages
                        scrollToBottom = true;
                    } else {
                        // The list needs to be reorder so that the newly fetch messages will
                        // be at the top of the list as they will be older
                        List<ImgurMessage> retainedMessages = mAdapter.retainItems();
                        conversationResponse.data.getMessages().addAll(retainedMessages);
                        mAdapter.clear();
                        mAdapter.addItems(conversationResponse.data.getMessages());
                        if (mLayoutManager.findFirstVisibleItemPosition() != 0)
                            scrollToBottom = true;
                    }


                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);

                    if (scrollToBottom) {
                        mMultiView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mConvoList != null)
                                    mConvoList.scrollToPosition(mAdapter.getItemCount() - 1);
                                mHasScrolledInitially = true;
                            }
                        });
                    }
                } else {
                    onEmpty();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LogUtil.e(TAG, "Error fetching message", t);
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                mIsLoading = false;
            }
        });
    }

    private void sendMessage(final ImgurMessage message) {
        if (mAdapter == null) {
            List<ImgurMessage> messages = new ArrayList<>();
            messages.add(message);
            mAdapter = new MessagesAdapter(getApplicationContext(), MaterialColor.getColor(mConvo.getWithAccount()), messages, this);
            mConvoList.setAdapter(mAdapter);
        } else {
            mAdapter.addItem(message);
        }

        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        mConvoList.scrollToPosition(mAdapter.getItemCount() - 1);
        mMessageInput.setText(null);

        ApiClient.getService().sendMessage(mConvo.getWithAccount(), message.getBody()).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Response<BasicResponse> response, Retrofit retrofit) {
                boolean success = response != null && response.body() != null && response.body().data;
                if (mAdapter != null) mAdapter.onMessageSendComplete(success, message.getId());
            }

            @Override
            public void onFailure(Throwable t) {
                LogUtil.e(TAG, "Error sending message", t);
                if (mAdapter != null) mAdapter.onMessageSendComplete(false, message.getId());
            }
        });
    }

    private void blockUser() {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(getString(R.string.convo_block_title, mConvo.getWithAccount()))
                .setMessage(getString(R.string.convo_block_message, mConvo.getWithAccount()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ApiClient.getService().blockUser(mConvo.getWithAccount(), mConvo.getWithAccount()).enqueue(new Callback<BasicResponse>() {

                            @Override
                            public void onResponse(Response<BasicResponse> response, Retrofit retrofit) {
                                if (response != null && response.body() != null && response.body().data) {
                                    setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_BLOCKED_CONVO, mConvo));
                                    finish();
                                } else {
                                    Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LogUtil.e(TAG, "Unable to block user", t);
                                Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                }).show();
    }

    private void reportUser() {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(getString(R.string.convo_report_title, mConvo.getWithAccount()))
                .setMessage(getString(R.string.convo_report_message, mConvo.getWithAccount()))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ApiClient.getService().reportUser(mConvo.getWithAccount(), mConvo.getWithAccount()).enqueue(new Callback<BasicResponse>() {
                            @Override
                            public void onResponse(Response<BasicResponse> response, Retrofit retrofit) {
                                if (response != null && response.body() != null && response.body().data) {
                                    Snackbar.make(mMultiView, getString(R.string.convo_user_reported, mConvo.getWithAccount()), Snackbar.LENGTH_LONG).show();
                                } else {
                                    Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LogUtil.e(TAG, "Unable to report user", t);
                                Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                            }
                        });
                    }
                }).show();
    }

    private void onEmpty() {
        mHasMore = false;

        if (mAdapter == null || mAdapter.isEmpty()) {
            ViewUtils.setEmptyText(mMultiView, R.id.emptyMessage, getString(R.string.convo_message_hint));
            mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
        }
    }

    @Override
    public void onPhotoTap(View view) {
        // NOOP
    }

    @Override
    public void onPlayTap(ProgressBar prog, FloatingActionButton play, ImageView image, VideoView video, View root) {
        // NOOP
    }

    @Override
    public void onLinkTap(View view, @Nullable String url) {
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

                case NONE:
                default:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                    if (browserIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        Snackbar.make(mMultiView, R.string.cant_launch_intent, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    @Override
    public void onViewRepliesTap(View view) {
        // NOOP
    }

    @Override
    public void onPhotoLongTapListener(View view) {
        // NOOP
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONVO, mConvo);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark : R.style.Theme_Opengur_Light_DarkActionBar;
    }
}
