package com.kenny.openimgur.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ProgressRequestBody;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by Kenny-PC on 3/22/2015.
 */
public class UploadService extends IntentService implements ProgressRequestBody.ProgressListener {
    private static final String TAG = "UploadService";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESC = "description";
    private static final String KEY_URL = "url";
    private static final String KEY_FILE = "file";
    private static final String KEY_NOTIF_ID = "notification_id";

    private NotificationManager mManager;
    NotificationCompat.Builder mBuilder;
    private int mNotificationId;

    public static Intent createIntent(Context context, @Nullable String title, @Nullable String description, @NonNull File file, int notificationId) {
        return new Intent(context, UploadService.class)
                .putExtra(KEY_TITLE, title)
                .putExtra(KEY_DESC, description)
                .putExtra(KEY_FILE, file.getAbsolutePath())
                .putExtra(KEY_NOTIF_ID, notificationId);
    }

    public static Intent createIntent(Context context, @Nullable String title, @Nullable String description, @NonNull String url, int notificationId) {
        return new Intent(context, UploadService.class)
                .putExtra(KEY_TITLE, title)
                .putExtra(KEY_DESC, description)
                .putExtra(KEY_URL, url)
                .putExtra(KEY_NOTIF_ID, notificationId);
    }

    public UploadService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String filePath = intent.getStringExtra(KEY_FILE);
        String url = intent.getStringExtra(KEY_URL);

        if (TextUtils.isEmpty(filePath) && TextUtils.isEmpty(url)) {
            LogUtil.w(TAG, "Nothing passed to upload, bailing");
            return;
        }

        String title = intent.getStringExtra(KEY_TITLE);
        String desc = intent.getStringExtra(KEY_DESC);
        mNotificationId = intent.getIntExtra(KEY_NOTIF_ID, -1);

        if (!TextUtils.isEmpty(filePath)) {
            uploadPhoto(title, desc, filePath);
        } else {
            uploadLink(title, desc, url);
        }
    }

    private void uploadLink(@Nullable String title, @Nullable String description, @NonNull String url) {
        LogUtil.v(TAG, "Received URL to upload");
        ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);

        FormEncodingBuilder builder = new FormEncodingBuilder()
                .add("image", url)
                .add("type", "URL");

        if (!TextUtils.isEmpty(title)) {
            builder.add("title", title);
        }

        if (!TextUtils.isEmpty(description)) {
            builder.add("description", description);
        }

        RequestBody body = builder.build();

        try {
            if (mNotificationId <= 0) mNotificationId = url.hashCode();
            onUploadStarted();
            JSONObject json = client.doWork(body);
            handleResponse(json.getInt(ApiClient.KEY_STATUS), json.getJSONObject(ApiClient.KEY_DATA));
        } catch (IOException | JSONException e) {
            LogUtil.e(TAG, "Error uploading url", e);
            onUploadFailed(title, description, url, false);
        }
    }

    private void uploadPhoto(@Nullable String title, @Nullable String description, @NonNull String filePath) {
        final File file = new File(filePath);

        if (!FileUtil.isFileValid(file)) {
            LogUtil.w(TAG, "File is invalid, bailing");
            return;
        }

        LogUtil.v(TAG, "Received File to upload");
        ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);
        MediaType type;

        if (file.getAbsolutePath().endsWith("png")) {
            type = MediaType.parse("image/png");
        } else if (file.getAbsolutePath().endsWith("gif")) {
            type = MediaType.parse("image/gif");
        } else {
            type = MediaType.parse("image/jpeg");
        }

        MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM)
                .addPart(Headers.of("Content-Disposition", "form-data; name=\"image\""),
                        new ProgressRequestBody(file, type, this));

        builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"type\""),
                RequestBody.create(null, "file"));

        if (!TextUtils.isEmpty(title)) {
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"title\""),
                    RequestBody.create(null, title));
        }

        if (!TextUtils.isEmpty(description)) {
            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"description\""),
                    RequestBody.create(null, description));
        }

        RequestBody body = builder.build();


        try {
            if (mNotificationId <= 0) mNotificationId = file.hashCode();
            onUploadStarted();
            JSONObject json = client.doWork(body);
            handleResponse(json.getInt(ApiClient.KEY_STATUS), json.getJSONObject(ApiClient.KEY_DATA));
        } catch (IOException | JSONException e) {
            LogUtil.e(TAG, "Error uploading file", e);
            onUploadFailed(title, description, filePath, true);
        }
    }

    /**
     * Handles the result of the upload
     *
     * @param status Status received from the upload
     * @param json   JSON object received from the upload
     */
    private void handleResponse(int status, @Nullable JSONObject json) {
        if (status != ApiClient.STATUS_OK || json == null) {
            LogUtil.w(TAG, "Error received while uploading, status code: " + status + " JSON: " + json);
            onUploadFailed(null, null, null, false);
            return;
        }

        ImgurPhoto photo = new ImgurPhoto(json);
        OpenImgurApp.getInstance(getApplicationContext()).getSql().insertUploadedPhoto(photo);
        onUploadComplete(photo.getLink());
    }

    /**
     * Called before the upload begins to set up the notification
     */
    private void onUploadStarted() {
        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.image_uploading))
                .setContentText(getString(R.string.uploading)).setAutoCancel(true).setProgress(0, 100, true).setLargeIcon(icon)
                .setSmallIcon(Build.VERSION.SDK_INT < 21 ? R.drawable.ic_launcher : R.drawable.ic_i);
        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called when upload finishes successfully
     *
     * @param url The url of the uploaded photo
     */
    private void onUploadComplete(String url) {
        Intent intent = NotificationReceiver.createCopyIntent(getApplicationContext(), url, mNotificationId);
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
        mBuilder.setContentTitle(getString(R.string.upload_complete))
                .setContentText(getString(R.string.upload_success, url))
                .addAction(R.drawable.ic_action_copy, getString(R.string.copy_link), pIntent);
        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called if the upload fails while If the content field is not empty, there will be an option to retry
     *
     * @param title       The title used for the upload
     * @param description The description used for the upload
     * @param content     The url or file path of the uploaded photo
     */
    private void onUploadFailed(@Nullable String title, @Nullable String description, @Nullable String content, boolean wasFile) {
        mBuilder.setContentTitle(getString(R.string.error))
                .setContentText(getString(R.string.upload_error));

        if (!TextUtils.isEmpty(content)) {
            Intent intent = NotificationReceiver.createRetryUploadIntent(getApplicationContext(), content, title, description, mNotificationId, wasFile);
            PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_ONE_SHOT);
            mBuilder.addAction(R.drawable.ic_action_upload, getString(R.string.retry), pIntent);
        }

        mManager.notify(mNotificationId, mBuilder.build());
        stopSelf();
    }

    @Override
    public void onTransferred(long transferred, long totalSize) {
        mBuilder.setProgress((int) totalSize, (int) transferred, false);
        mManager.notify(mNotificationId, mBuilder.build());
    }

    @Override
    public void onDestroy() {
        mManager = null;
        mBuilder = null;
        LogUtil.v(TAG, "Finishing service");
        super.onDestroy();
    }
}
