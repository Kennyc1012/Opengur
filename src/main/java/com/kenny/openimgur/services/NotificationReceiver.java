package com.kenny.openimgur.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.activities.NotificationActivity;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Kenny-PC on 3/22/2015.
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";

    private static final String KEY_ACTION = "action";

    private static final String KEY_UPLOADED_URL = "uploaded_url";

    private static final String KEY_NOTIF_ID = "notification_id";

    private static final String KEY_NOTIFICATION_CONTENT = "notification_content";

    private static final int ACTION_UPLOAD_COPY = 1;

    private static final int ACTION_NOTIFICATION_CLICKED = 2;

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

    @Override
    public void onReceive(Context context, Intent intent) {
        int action = intent.getIntExtra(KEY_ACTION, -1);
        LogUtil.v(TAG, "Action Received: " + action);
        int notificationId = intent.getIntExtra(KEY_NOTIF_ID, -1);

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

                if (content instanceof ImgurConvo) {
                    dest = ConvoThreadActivity.createIntent(context, (ImgurConvo) content);
                } else if (content instanceof ImgurComment) {
                    dest = ViewActivity.createIntent(context, "https://imgur.com/gallery/" + ((ImgurComment) content).getImageId(), false);
                } else {
                    dest = NotificationActivity.createIntent(context);
                }

                if (content != null) {
                    SqlHelper sql = OpengurApp.getInstance(context).getSql();
                    String ids = sql.getNotificationIds(content);
                    sql.markNotificationRead(content);

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
                }

                dest.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(dest);
                break;

            default:
                Log.w(TAG, "Unable to determine action");
                break;
        }
    }
}
