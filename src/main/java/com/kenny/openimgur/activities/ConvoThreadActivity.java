package com.kenny.openimgur.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.clans.fab.FloatingActionButton;
import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.MessagesAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.squareup.okhttp.FormEncodingBuilder;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

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

    private ApiClient mApiClient;

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
                reportBlockUser(Endpoints.CONVO_BLOCK);
                break;

            case R.id.report:
                reportBlockUser(Endpoints.CONVO_REPORT);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void reportBlockUser(final Endpoints endpoint) {
        String title;
        String message;

        if (endpoint == Endpoints.CONVO_REPORT) {
            title = getString(R.string.convo_report_title, mConvo.getWithAccount());
            message = getString(R.string.convo_report_message, mConvo.getWithAccount());
        } else {
            title = getString(R.string.convo_block_title, mConvo.getWithAccount());
            message = getString(R.string.convo_block_message, mConvo.getWithAccount());
        }

        new MaterialDialog.Builder(this)
                .title(title)
                .content(message)
                .negativeText(R.string.cancel)
                .positiveText(R.string.yes)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        ImgurBusEvent.EventType event = endpoint == Endpoints.CONVO_REPORT ?
                                ImgurBusEvent.EventType.CONVO_REPORT : ImgurBusEvent.EventType.CONVO_BLOCK;

                        ApiClient client = new ApiClient(String.format(endpoint.getUrl(), mConvo.getWithAccount()), ApiClient.HttpRequest.POST);
                        client.doWork(event, mConvo.getId(), new FormEncodingBuilder().add("username", mConvo.getWithAccount()).build());
                        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    }
                }).show();
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
            String url = String.format(Endpoints.SEND_MESSAGE.getUrl(), mConvo.getWithAccount());

            if (mApiClient == null) {
                mApiClient = new ApiClient(url, ApiClient.HttpRequest.POST);
            } else {
                mApiClient.setUrl(url);
                mApiClient.setRequestType(ApiClient.HttpRequest.POST);
            }

            FormEncodingBuilder builder = new FormEncodingBuilder()
                    .add("body", message)
                    .add("recipient", mConvo.getWithAccount());

            ImgurMessage newMessage = ImgurMessage.createMessage(message, user.getId());

            if (newMessage != null) {
                if (mAdapter == null) {
                    List<ImgurMessage> messages = new ArrayList<>();
                    messages.add(newMessage);
                    mAdapter = new MessagesAdapter(getApplicationContext(), messages);
                    mListView.setAdapter(mAdapter);
                } else {
                    mAdapter.addItem(newMessage);
                }

                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                mListView.setSelection(mAdapter.getCount() - 1);
                mApiClient.doWork(ImgurBusEvent.EventType.MESSAGE_SEND, newMessage.getId(), builder.build());
                mMessageInput.setText(null);
            } else {
                SnackBar.show(this, R.string.error_generic);
            }
        } else {
            SnackBar.show(this, R.string.convo_message_hint);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiView.setViewState(MultiStateView.ViewState.LOADING);
            fetchMessages();
        }
    }

    private void fetchMessages() {
        // Having an id of -1 means that they are starting the convo from the Info fragment where an id is not known
        if (mConvo.getId().equals("-1")) {
            if (mAdapter == null || mAdapter.isEmpty()) {
                mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                return;
            }
        }

        String url = String.format(Endpoints.MESSAGES.getUrl(), mConvo.getId(), mCurrentPage);

        if (mApiClient == null) {
            mApiClient = new ApiClient(url, ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setUrl(url);
            mApiClient.setRequestType(ApiClient.HttpRequest.GET);
        }

        mIsLoading = true;
        mApiClient.doWork(ImgurBusEvent.EventType.CONVO_MESSAGES, mConvo.getId(), null);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
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

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int status = event.json.getInt(ApiClient.KEY_STATUS);

            if (status == ApiClient.STATUS_OK) {
                switch (event.eventType) {
                    case CONVO_MESSAGES:
                        if (!mConvo.getId().equals(event.id)) return;

                        JSONArray array = event.json.getJSONObject(ApiClient.KEY_DATA).getJSONArray("messages");
                        List<ImgurMessage> messages = new ArrayList<>(array.length());

                        for (int i = 0; i < array.length(); i++) {
                            messages.add(new ImgurMessage(array.getJSONObject(i)));
                        }

                        if (messages.isEmpty()) {
                            mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, messages);
                        }
                        break;

                    case MESSAGE_SEND:
                        boolean success = event.json.getBoolean(ApiClient.KEY_SUCCESS);
                        mHandler.sendMessage(ImgurHandler.MESSAGE_MESSAGE_SENT, new Object[]{success, event.id});
                        break;

                    case CONVO_REPORT:
                        mHandler.sendMessage(ImgurHandler.MESSAGE_CONVO_REPORTED, event.json.getBoolean(ApiClient.KEY_SUCCESS));
                        break;

                    case CONVO_BLOCK:
                        mHandler.sendMessage(ImgurHandler.MESSAGE_CONVO_BLOCKED, event.json.getBoolean(ApiClient.KEY_SUCCESS));
                        break;

                }
            } else {
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(status));
            }
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Error decoding JSON", ex);
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        }
    }

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

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurMessage> messages = (ArrayList<ImgurMessage>) msg.obj;
                    boolean scrollToBottom = false;

                    if (mAdapter == null) {
                        mAdapter = new MessagesAdapter(getApplicationContext(), messages);
                        mListView.setAdapter(mAdapter);
                        // Start at the bottom of the list when we receive the first set of messages
                        scrollToBottom = true;
                    } else {
                        // The list needs to be reorder so that the newly fetch messages will
                        // be at the top of the list as they will be older
                        List<ImgurMessage> retainedMessages = mAdapter.retainItems();
                        messages.addAll(retainedMessages);
                        mAdapter.clear();
                        mAdapter.addItems(messages);
                    }


                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);

                    if (scrollToBottom) {
                        mMultiView.post(new Runnable() {
                            @Override
                            public void run() {
                                mListView.setSelection(mAdapter.getCount() - 1);
                                mHasScrolledInitially = true;
                            }
                        });
                    }
                    break;

                case MESSAGE_EMPTY_RESULT:
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        mMultiView.setEmptyText(R.id.emptyMessage, getString(R.string.convo_message_hint));
                        mMultiView.setViewState(MultiStateView.ViewState.EMPTY);
                    }

                    mHasMore = false;
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;

                case MESSAGE_MESSAGE_SENT:
                    Object[] results = (Object[]) msg.obj;
                    boolean success = Boolean.TRUE.equals(results[0]);
                    String id = (String) results[1];

                    if (mAdapter != null) {
                        mAdapter.onMessageSendComplete(success, id);
                    }
                    break;

                case MESSAGE_CONVO_BLOCKED:
                    if (msg.obj instanceof Boolean) {
                        if ((Boolean) msg.obj) {
                            setResult(Activity.RESULT_OK, new Intent().putExtra(KEY_BLOCKED_CONVO, mConvo));
                            finish();
                        } else {
                            SnackBar.show(ConvoThreadActivity.this, R.string.error_generic);
                        }
                    } else {
                        SnackBar.show(ConvoThreadActivity.this, R.string.error_generic);
                    }
                    break;

                case MESSAGE_CONVO_REPORTED:
                    if (msg.obj instanceof Boolean) {
                        if ((Boolean) msg.obj) {
                            SnackBar.show(ConvoThreadActivity.this, getString(R.string.convo_user_reported, mConvo.getWithAccount()));
                        } else {
                            SnackBar.show(ConvoThreadActivity.this, R.string.error_generic);
                        }
                    } else {
                        SnackBar.show(ConvoThreadActivity.this, R.string.error_generic);
                    }

                    break;
            }

            mIsLoading = false;
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_CONVO, mConvo);
    }
}
