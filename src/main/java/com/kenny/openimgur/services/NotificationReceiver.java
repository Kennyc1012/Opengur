package com.kenny.openimgur.services;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by Kenny-PC on 3/22/2015.
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private static final String KEY_ACTION = "action";
    private static final String KEY_UPLOADED_URL = "uploaded_url";
    private static final String KEY_NOTIF_ID = "notification_id";
    private static final int ACTION_UPLOAD_COPY = 1;

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

            default:
                Log.w(TAG, "Unable to determine action");
                break;
        }
    }
}
