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

import java.io.File;

/**
 * Created by Kenny-PC on 3/22/2015.
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationReceiver";
    private static final String KEY_ACTION = "action";
    private static final String KEY_UPLOADED_URL = "uploaded_url";
    private static final String KEY_RETRY_URL = "retry_url";
    private static final String KEY_RETRY_FILE_PATH = "retry_file_path";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESC = "description";
    private static final String KEY_NOTIF_ID = "notification_id";
    private static final int ACTION_UPLOAD_COPY = 1;
    private static final int ACTION_UPlOAD_RETRY = 2;

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

    /**
     * Creates an intent for when an upload fails and can be re-triggered
     *
     * @param context        App context
     * @param upload         The url or file path to upload
     * @param title          The title of the upload
     * @param description    The description of the upload
     * @param notificationId The id of the notification which the retry action was triggered from
     * @param isFilePath     If the upload is a file path
     * @return
     */
    public static Intent createRetryUploadIntent(Context context, String upload, String title, String description, int notificationId, boolean isFilePath) {
        return new Intent(context, NotificationReceiver.class)
                .putExtra(KEY_ACTION, ACTION_UPlOAD_RETRY)
                .putExtra(isFilePath ? KEY_RETRY_FILE_PATH : KEY_RETRY_URL, upload)
                .putExtra(KEY_TITLE, title)
                .putExtra(KEY_DESC, description)
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

            case ACTION_UPlOAD_RETRY:
                String title = intent.getStringExtra(KEY_TITLE);
                String desc = intent.getStringExtra(KEY_DESC);
                Intent retryIntent;

                if (intent.hasExtra(KEY_RETRY_FILE_PATH)) {
                    String path = intent.getStringExtra(KEY_RETRY_FILE_PATH);
                    retryIntent = UploadService.createIntent(context, title, desc, new File(path), notificationId);
                } else {
                    String path = intent.getStringExtra(KEY_RETRY_URL);
                    retryIntent = UploadService.createIntent(context, title, desc, path, notificationId);
                }

                context.startService(retryIntent);
                break;

            default:
                Log.w(TAG, "Unable to determine action");
                break;
        }
    }
}
