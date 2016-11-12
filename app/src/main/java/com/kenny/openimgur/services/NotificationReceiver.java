package com.kenny.openimgur.services;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.NotificationActivity;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.SqlHelper;

import java.io.File;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Kenny-PC on 3/22/2015.
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    public static final String KEY_QUICK_REPLY_KEY = "opengur.quickreply";

    private static final String KEY_ACTION = "action";

    private static final String KEY_UPLOADED_URL = "uploaded_url";

    private static final String KEY_NOTIF_ID = "notification_id";

    private static final String KEY_NOTIFICATION_CONTENT = "notification_content";

    private static final String KEY_FILE_PATH = "file_path";

    private static final String KEY_WITH = "chat_with";

    private static final int ACTION_UPLOAD_COPY = 1;

    private static final int ACTION_NOTIFICATION_CLICKED = 2;

    private static final int ACTION_NOTIFICATIONS_READ = 3;

    private static final int ACTION_DELETE = 4;

    private static final int ACTION_QUICK_REPLY = 5;

    /**
     * Returns an intent for when an image is successfully uploaded
     *
     * @param context        App context
     * @param url            The url of the uploaded photo
     * @param notificationId The id of the notification used for dismissal
     * @return
     */
    public static Intent createCopyIntent(Context context, String url, int notificationId) {
        return new Intent(context, NotificationReceiver.class)
                .putExtra(KEY_ACTION, ACTION_UPLOAD_COPY)
                .putExtra(KEY_UPLOADED_URL, url)
                .putExtra(KEY_NOTIF_ID, notificationId);
    }

    public static Intent createNotificationIntent(Context context, @Nullable ImgurBaseObject content) {
        return new Intent(context, NotificationReceiver.class)
                .putExtra(KEY_ACTION, ACTION_NOTIFICATION_CLICKED)
                .putExtra(KEY_NOTIFICATION_CONTENT, content);
    }

    public static Intent createReadNotificationsIntent(Context context, int notificationId) {
        return new Intent(context, NotificationReceiver.class)
                .putExtra(KEY_ACTION, ACTION_NOTIFICATIONS_READ)
                .putExtra(KEY_NOTIF_ID, notificationId);
    }

    public static Intent createDeleteIntent(@NonNull Context context, int notificationId, @NonNull String fileLocation) {
        return new Intent(context, NotificationReceiver.class)
                .putExtra(KEY_ACTION, ACTION_DELETE)
                .putExtra(KEY_FILE_PATH, fileLocation)
                .putExtra(KEY_NOTIF_ID, notificationId);
    }

    public static Intent createQuickReplyIntent(@NonNull Context context, int notificationId, @NonNull String with) {
        return new Intent(context, NotificationReceiver.class)
                .putExtra(KEY_ACTION, ACTION_QUICK_REPLY)
                .putExtra(KEY_WITH, with)
                .putExtra(KEY_NOTIF_ID, notificationId);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        int action = intent.getIntExtra(KEY_ACTION, -1);
        LogUtil.v(TAG, "Action Received: " + action);
        final int notificationId = intent.getIntExtra(KEY_NOTIF_ID, -1);

        switch (action) {
            case ACTION_UPLOAD_COPY:
                String url = intent.getStringExtra(KEY_UPLOADED_URL);

                if (!TextUtils.isEmpty(url)) {
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("link", url));
                    Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, R.string.link_copy_failed, Toast.LENGTH_SHORT).show();
                }

                NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancel(notificationId);
                break;

            case ACTION_NOTIFICATION_CLICKED:
                Intent dest;
                ImgurBaseObject content = intent.getParcelableExtra(KEY_NOTIFICATION_CONTENT);

                if (content instanceof ImgurComment) {
                    dest = ViewActivity.createIntent(context, ApiClient.IMGUR_GALLERY_URL + ((ImgurComment) content).getImageId(), false);
                } else {
                    dest = NotificationActivity.createIntent(context);
                }

                if (content != null) {
                    SqlHelper sql = SqlHelper.getInstance(context);
                    String ids = sql.getNotificationIds(content);
                    sql.markNotificationRead(content);

                    if (!TextUtils.isEmpty(ids)) {
                        ApiClient.getService().markNotificationsRead(ids).enqueue(new Callback<BasicResponse>() {
                            @Override
                            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                                if (response != null && response.body() != null) {
                                    LogUtil.v(TAG, "Result of marking notifications read " + response.body().data);
                                } else {
                                    LogUtil.w(TAG, "Did not receive a response while marking notifications read");
                                }
                            }

                            @Override
                            public void onFailure(Call<BasicResponse> call, Throwable t) {
                                LogUtil.e(TAG, "Failure marking notifications read, error", t);
                            }
                        });
                    }
                }

                dest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dest);
                break;

            case ACTION_NOTIFICATIONS_READ:
                SqlHelper sql = SqlHelper.getInstance(context);
                String ids = sql.getNotificationIds();
                sql.markNotificationsRead();

                if (!TextUtils.isEmpty(ids)) {
                    ApiClient.getService().markNotificationsRead(ids).enqueue(new Callback<BasicResponse>() {
                        @Override
                        public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                            if (response != null && response.body() != null) {
                                LogUtil.v(TAG, "Result of marking notifications read " + response.body().data);
                            } else {
                                LogUtil.w(TAG, "Did not receive a response while marking notifications read");
                            }
                        }

                        @Override
                        public void onFailure(Call<BasicResponse> call, Throwable t) {
                            LogUtil.e(TAG, "Failure marking notifications read, error", t);
                        }
                    });
                }

                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
                break;

            case ACTION_DELETE:
                String filePath = intent.getStringExtra(KEY_FILE_PATH);

                if (!TextUtils.isEmpty(filePath) && PermissionUtils.hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    boolean deleted = new File(filePath).delete();
                    LogUtil.v(TAG, "Result of file deletion " + deleted);
                }

                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
                break;

            case ACTION_QUICK_REPLY:
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null) {
                    CharSequence reply = remoteInput.getCharSequence(KEY_QUICK_REPLY_KEY);
                    String with = intent.getStringExtra(KEY_WITH);

                    if (!TextUtils.isEmpty(reply) && !TextUtils.isEmpty(with)) {
                        ApiClient.getService().sendMessage(with, reply.toString()).enqueue(new Callback<BasicResponse>() {
                            @Override
                            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                                boolean success = response != null && response.body() != null && response.body().data;
                                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
                                LogUtil.v(TAG, "Message send result " + success);
                            }

                            @Override
                            public void onFailure(Call<BasicResponse> call, Throwable t) {
                                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notificationId);
                                LogUtil.e(TAG, "Error sending message", t);
                            }
                        });
                    }
                }
                break;

            default:
                LogUtil.w(TAG, "Unable to determine action");
                break;
        }
    }
}
