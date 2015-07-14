package com.kenny.openimgur.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.MessagesAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.ConverastionResponse;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.snackbar.SnackBar;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kcampagna on 12/25/14.
 */
public class ConvoThreadActivity extends BaseActivity implements AbsListView.OnScrollListener {
    public static final int REQUEST_CODE = 102;

    public static final String KEY_BLOCKED_CONVO = "blocked_convo";

    private static final String KEY_CONVO = "convo";

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.convoList)
    ListView mListView;

    @InjectView(R.id.messageInput)
    EditText mMessageInput;

    private ImgurConvo mConvo;

    private MessagesAdapter mAdapter;

    private boolean mHasMore = true;

    private boolean mIsLoading = false;

    private boolean mHasScrolledInitially = false;

    private int mCurrentPage = 1;

    public static Intent createIntent(Context context, @NonNull ImgurConvo convo) {
        return new Intent(context, ConvoThreadActivity.class).putExtra(KEY_CONVO, convo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_convo_thread);
        mListView.setOnScrollListener(this);
        handleData(savedInstanceState);
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
                mMultiView.setViewState(MultiStateView.ViewState.LOADING);

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
            mAdapter = new MessagesAdapter(getApplicationContext(), mConvo.getMessages());
            mListView.setAdapter(mAdapter);
            mListView.setSelection(mAdapter.getCount() - 1);
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
            SnackBar.show(this, R.string.convo_message_hint);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.ViewState.LOADING);
            fetchMessages();
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // NOOP
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // We want to check for when they are at the TOP of the list this time as the messages come in reverse order
        // from the api (newest to oldest)

        if (mHasMore && !mIsLoading && totalItemCount > 0 && firstVisibleItem == 0 && mHasScrolledInitially) {
            mCurrentPage++;
            fetchMessages();
        }
    }

    private void fetchMessages() {
        // Having an id of -1 means that they are starting the convo from the Info fragment where an id is not known
        if (mConvo.getId().equals("-1")) onEmpty();
        mIsLoading = true;
        ApiClient.getService().getMessages(mConvo.getId(), mCurrentPage, new Callback<ConverastionResponse>() {
            @Override
            public void success(ConverastionResponse converastionResponse, Response response) {
                if (converastionResponse.data.hasMessages()) {
                    boolean scrollToBottom = false;

                    if (mAdapter == null) {
                        mAdapter = new MessagesAdapter(getApplicationContext(), converastionResponse.data.getMessages());
                        mListView.setAdapter(mAdapter);
                        // Start at the bottom of the list when we receive the first set of messages
                        scrollToBottom = true;
                    } else {
                        // The list needs to be reorder so that the newly fetch messages will
                        // be at the top of the list as they will be older
                        List<ImgurMessage> retainedMessages = mAdapter.retainItems();
                        converastionResponse.data.getMessages().addAll(retainedMessages);
                        mAdapter.clear();
                        mAdapter.addItems(converastionResponse.data.getMessages());
                    }


                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                    if (scrollToBottom) {
                        mMultiView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListView != null) mListView.setSelection(mAdapter.getCount() - 1);
                                mHasScrolledInitially = true;
                            }
                        });
                    }
                } else {
                    onEmpty();
                }
            }

            @Override
            public void failure(RetrofitError error) {
                // TODO
            }
        });
    }

    private void sendMessage(final ImgurMessage message) {
        if (mAdapter == null) {
            List<ImgurMessage> messages = new ArrayList<>();
            messages.add(message);
            mAdapter = new MessagesAdapter(getApplicationContext(), messages);
            mListView.setAdapter(mAdapter);
        } else {
            mAdapter.addItem(message);
        }

        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        mListView.setSelection(mAdapter.getCount() - 1);
        mMessageInput.setText(null);

        ApiClient.getService().sendMessage(mConvo.getWithAccount(), message.getBody(), new Callback<BasicResponse>() {
            @Override
            public void success(BasicResponse basicResponse, Response response) {
                if (mAdapter != null) mAdapter.onMessageSendComplete(basicResponse.data, message.getId());
            }

            @Override
            public void failure(RetrofitError error) {
                // TODO Error
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
                        ApiClient.getService().blockUser(mConvo.getWithAccount(), mConvo.getWithAccount(), new Callback<BasicResponse>() {
                            @Override
                            public void success(BasicResponse basicResponse, Response response) {
                                if (basicResponse.data) {
                                    setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_BLOCKED_CONVO, mConvo));
                                    finish();
                                } else {
                                    SnackBar.show(ConvoThreadActivity.this, R.string.error_generic);
                                }
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                // TODO Error
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
                        ApiClient.getService().reportUser(mConvo.getWithAccount(), mConvo.getWithAccount(), new Callback<BasicResponse>() {
                            @Override
                            public void success(BasicResponse basicResponse, Response response) {
                                if (basicResponse.data) {
                                    SnackBar.show(ConvoThreadActivity.this, getString(R.string.convo_user_reported, mConvo.getWithAccount()));
                                } else {
                                    SnackBar.show(ConvoThreadActivity.this, R.string.error_generic);
                                }
                            }

                            @Override
                            public void failure(RetrofitError error) {
                                // TODO Error
                            }
                        });
                    }
                }).show();
    }

    private void onEmpty() {
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setEmptyText(R.id.emptyMessage, getString(R.string.convo_message_hint));
            mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONVO, mConvo);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
    }
}
