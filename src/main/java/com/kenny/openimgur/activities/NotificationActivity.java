package com.kenny.openimgur.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.NotificationAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.NotificationResponse;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurNotification;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;
import com.kennyc.view.MultiStateView;

import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Kenny-PC on 8/9/2015.
 */
public class NotificationActivity extends BaseActivity implements View.OnClickListener, View.OnLongClickListener, ActionMode.Callback {
    public static final String KEY_USER_NOT_LOGGED_IN = "no_user";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_POSITION = "position";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mList;

    private NotificationAdapter mAdapter;

    private ActionMode mMode;

    public static Intent createIntent(Context context) {
        return new Intent(context, NotificationActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (user == null) {
            setResult(Activity.RESULT_CANCELED, new Intent().putExtra(KEY_USER_NOT_LOGGED_IN, true));
            Toast.makeText(getApplicationContext(), R.string.notification_no_user, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        getSupportActionBar().setTitle(R.string.notifications);
        setContentView(R.layout.activity_notifications);
        mList.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        if (savedInstanceState != null) {
            List<ImgurNotification> notifications = savedInstanceState.getParcelableArrayList(KEY_ITEMS);

            if (notifications != null && !notifications.isEmpty()) {
                int position = savedInstanceState.getInt(KEY_POSITION, 0);
                mList.setAdapter(mAdapter = new NotificationAdapter(this, notifications, this, this));
                mList.scrollToPosition(position);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.notifications, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchNotifications();
                return true;

            case R.id.delete:
                if (mAdapter != null && !mAdapter.isEmpty()) {
                    new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                            .setTitle(R.string.delete)
                            .setMessage(R.string.notification_delete_all_msg)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mAdapter.clear();
                                    app.getSql().deleteFromTable(DBContracts.NotificationContract.TABLE_NAME);
                                    mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                                }
                            }).show();
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAdapter == null || mAdapter.isEmpty()) {
            List<ImgurNotification> notifications = app.getSql().getNotifications(false);

            if (!notifications.isEmpty()) {
                LogUtil.v(TAG, "Notifications present in database");
                mAdapter = new NotificationAdapter(this, notifications, this, this);
                mList.setAdapter(mAdapter);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                markNotificationsRead();
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
        markNotificationsRead();
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

        if (mMode != null) {
            mAdapter.setSelected(notification);

            if (mAdapter.hasSelectedItems()) {
                mMode.setTitle(getString(R.string.notification_selected, mAdapter.getSelectedCount()));
            } else {
                mMode.finish();
            }

            return;
        }

        if (notification.getType() == ImgurNotification.TYPE_MESSAGE) {
            ImgurConvo convo = new ImgurConvo(notification.getContentId(), notification.getAuthor(), 0);
            startActivity(ConvoThreadActivity.createIntent(getApplicationContext(), convo));
        } else {
            String url = "https://imgur.com/gallery/" + notification.getContentId();
            Intent intent = ViewActivity.createIntent(getApplicationContext(), url, !TextUtils.isEmpty(notification.getAlbumCover()));
            startActivity(intent);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        ImgurNotification notification = mAdapter.getItem(mList.getChildAdapterPosition(v));
        mAdapter.setSelected(notification);

        if (mAdapter.hasSelectedItems()) {
            if (mMode == null) mMode = startActionMode(this);
            mMode.setTitle(getString(R.string.notification_selected, mAdapter.getSelectedCount()));
        } else {
            mMode.finish();
        }

        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.notification_cab, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                app.getSql().deleteNotifications(mAdapter.getSelectedNotifications());
                mAdapter.deleteNotifications();
                mode.finish();
                if (mAdapter.isEmpty()) mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                break;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mAdapter.setSelected(null);
        mMode = null;
    }

    private void fetchNotifications() {
        ApiClient.getService().getNotifications().enqueue(new Callback<NotificationResponse>() {
            @Override
            public void onResponse(Response<NotificationResponse> response, Retrofit retrofit) {
                if (response == null || response.body() == null) {
                    if (mAdapter == null || mAdapter.isEmpty()) mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                    return;
                }

                NotificationResponse notificationResponse = response.body();

                if (notificationResponse != null && notificationResponse.hasNotifications()) {
                    app.getSql().insertNotifications(notificationResponse);
                    List<ImgurNotification> notifications = app.getSql().getNotifications(false);
                    // Mark any notifications immediately read
                    markNotificationsRead();

                    if (notifications.isEmpty() && (mAdapter == null || mAdapter.isEmpty())) {
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                    } else {
                        if (mAdapter != null) {
                            mAdapter.clear();
                            mAdapter.addItems(notifications);
                        } else {
                            mAdapter = new NotificationAdapter(NotificationActivity.this, notifications, NotificationActivity.this, NotificationActivity.this);
                            mList.setAdapter(mAdapter);
                        }

                        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    }
                } else if (mAdapter == null || mAdapter.isEmpty()) {
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                } else {
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                if (mAdapter == null || mAdapter.isEmpty()) {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                } else {
                    SnackBar.show(NotificationActivity.this, ApiClient.getErrorCode(t));
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
            outState.putInt(KEY_POSITION, manager.findFirstVisibleItemPosition());
        }
    }

    private void markNotificationsRead() {
        String ids = app.getSql().getNotificationIds();

        // Mark all the notifications read when loaded
        if (!TextUtils.isEmpty(ids)) {
            app.getSql().markNotificationsRead();
            ApiClient.getService().markNotificationsRead(ids).enqueue(new Callback<BasicResponse>() {
                @Override
                public void onResponse(Response<BasicResponse> response, Retrofit retrofit) {
                    if (response != null && response.body() != null) {
                        LogUtil.v(TAG, "Marking Notifications Read Response " + response.body().data);
                    } else {
                        LogUtil.w(TAG, "Did not receive a response when marking notifications read");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LogUtil.e(TAG, "Failure marking notifications read, error", t);
                }
            });
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark : R.style.Theme_Opengur_Light_DarkActionBar;
    }
}
