package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.NotificationAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.ImgurNotification;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;

import java.util.List;

import butterknife.Bind;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Kenny-PC on 8/9/2015.
 */
public class NotifcationActivity extends BaseActivity implements View.OnClickListener {
    private static final String KEY_ITEMS = "items";
    private static final String KEY_POSIION = "position";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mList;

    private NotificationAdapter mAdapter;

    public static Intent createIntent(Context context) {
        return new Intent(context, NotifcationActivity.class);
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
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            }
        }
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
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            } else {
                LogUtil.v(TAG, "No notifications found in database, making request");
                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                fetchNotifications();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) mAdapter.onDestroy();
    }

    @Override
    public void onClick(View v) {
        // TODO
    }

    private void fetchNotifications() {
        ApiClient.getService().getNotifications(new Callback<NotificationResponse>() {
            @Override
            public void success(NotificationResponse notificationResponse, Response response) {
                if (notificationResponse != null && notificationResponse.data != null) {
                    app.getSql().insertNotifications(notificationResponse);
                    List<ImgurNotification> notifications = app.getSql().getNotifications();
                    mAdapter = new NotificationAdapter(NotifcationActivity.this, notifications, NotifcationActivity.this);
                    mList.setAdapter(mAdapter);
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                } else {
                    // TODO Error
                }
            }

            @Override
            public void failure(RetrofitError error) {
                // TODO Error
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
