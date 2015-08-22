package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.NotificationAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurNotification;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;

import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Kenny-PC on 8/9/2015.
 */
public class NotificationActivity extends BaseActivity implements View.OnClickListener {
    private static final String KEY_ITEMS = "items";
    private static final String KEY_POSIION = "position";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mList;

    private NotificationAdapter mAdapter;

    public static Intent createIntent(Context context) {
        return new Intent(context, NotificationActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (user == null) throw new IllegalStateException("No user present");
        getSupportActionBar().setTitle(R.string.notifications);
        setContentView(R.layout.activity_notifications);
        mList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        if (savedInstanceState != null) {
            List<ImgurNotification> notifications = savedInstanceState.getParcelableArrayList(KEY_ITEMS);

            if (notifications != null && !notifications.isEmpty()) {
                int position = savedInstanceState.getInt(KEY_POSIION, 0);
                mList.setAdapter(mAdapter = new NotificationAdapter(this, notifications, this));
                mList.scrollToPosition(position);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topics, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchNotifications();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAdapter == null || mAdapter.isEmpty()) {
            List<ImgurNotification> notifications = app.getSql().getNotifications();

            if (!notifications.isEmpty()) {
                LogUtil.v(TAG, "Notifications present in database");
                mAdapter = new NotificationAdapter(this, notifications, this);
                mList.setAdapter(mAdapter);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                String ids = app.getSql().getNotificationIds();

                // Mark all the notifications read when loaded
                if (!TextUtils.isEmpty(ids)) {
                    ApiClient.getService().markNotificationsRead(ids, new Callback<BasicResponse>() {
                        @Override
                        public void success(BasicResponse basicResponse, Response response) {
                            // Don't care about response
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            LogUtil.e(TAG, "Failure marking notifications read, error", error);
                        }
                    });
                }
            } else {
                LogUtil.v(TAG, "No notifications found in database, making request");
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchNotifications();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdapter != null) mAdapter.onDestroy();
        app.getSql().deleteNotifications();
        super.onDestroy();
    }

    @OnClick(R.id.errorButton)
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.errorButton) {
            mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            fetchNotifications();
            return;
        }

        ImgurNotification notification = mAdapter.getItem(mList.getChildAdapterPosition(v));

        if (notification.getType() == ImgurNotification.TYPE_MESSAGE) {
            ImgurConvo convo = new ImgurConvo(notification.getContentId(), notification.getAuthor(), 0);
            startActivity(ConvoThreadActivity.createIntent(getApplicationContext(), convo));
        } else {
            String url = "https://imgur.com/gallery/" + notification.getContentId();
            Intent intent = ViewActivity.createIntent(getApplicationContext(), url, !TextUtils.isEmpty(notification.getAlbumCover()));
            startActivity(intent);
        }
    }

    private void fetchNotifications() {
        ApiClient.getService().getNotifications(new Callback<NotificationResponse>() {
            @Override
            public void success(NotificationResponse notificationResponse, Response response) {
                if (notificationResponse != null && notificationResponse.data != null) {
                    app.getSql().insertNotifications(notificationResponse);
                    List<ImgurNotification> notifications = app.getSql().getNotifications();

                    if (notifications.isEmpty() && (mAdapter == null || mAdapter.isEmpty())) {
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                    } else {
                        // Don't remove any notifications that may have been cleared when first viewed
                        if (mAdapter != null) {
                            mAdapter.addItems(notifications);
                        } else {
                            mAdapter = new NotificationAdapter(NotificationActivity.this, notifications, NotificationActivity.this);
                            mList.setAdapter(mAdapter);
                        }

                        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    }
                } else {
                    mMultiView.setErrorText(R.id.errorMessage, R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (mAdapter == null || mAdapter.isEmpty()) {
                    mMultiView.setErrorText(R.id.errorMessage, ApiClient.getErrorCode(error));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                } else {
                    SnackBar.show(NotificationActivity.this, ApiClient.getErrorCode(error));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mAdapter.retainItems());
            LinearLayoutManager manager = (LinearLayoutManager) mList.getLayoutManager();
            outState.putInt(KEY_POSIION, manager.findFirstVisibleItemPosition());
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
    }
}
