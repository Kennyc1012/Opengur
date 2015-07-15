package com.kenny.openimgur.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 6/30/15.
 */
public class UploadService extends IntentService {
    private static final String TAG = UploadService.class.getSimpleName();

    private static final String KEY_UPLOADS = TAG + ".uploads";

    private static final String KEY_TITLE = TAG + ".title";

    private static final String KEY_TOPIC = TAG + ".topic";

    private static final String KEY_DESC = TAG + ".desc";

    private static final String KEY_SUBMIT_TO_GALLERY = TAG + ".submit.to.gallery";

    // No Topic
    private static final int FALLBACK_TOPIC = 29;

    private NotificationManager mManager;

    private NotificationCompat.Builder mBuilder;

    private int mNotificationId;

    /**
     * Creates service for uploading photos.
     *
     * @param context         App context
     * @param uploads         Photos being uploaded
     * @param submitToGallery If the upload is being submitted to the Imgur Gallery
     * @param title           The title for the Gallery
     * @param desc            The description for the upload
     * @param topic           The Topic for the Gallery
     * @return
     */
    public static Intent createIntent(Context context, @NonNull ArrayList<Upload> uploads, boolean submitToGallery, @Nullable String title, @Nullable String desc, @Nullable ImgurTopic topic) {
        Intent intent = new Intent(context, UploadService.class)
                .putExtra(KEY_UPLOADS, uploads)
                .putExtra(KEY_SUBMIT_TO_GALLERY, submitToGallery);

        if (!TextUtils.isEmpty(title)) intent.putExtra(KEY_TITLE, title);
        if (!TextUtils.isEmpty(desc)) intent.putExtra(KEY_DESC, desc);
        if (topic != null) intent.putExtra(KEY_TOPIC, topic);

        return intent;
    }

    public UploadService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null || !intent.hasExtra(KEY_UPLOADS)) {
            LogUtil.w(TAG, "Did not receive any arguments");
            stopSelf();
            return;
        }

        // Get a wake lock so any long uploads will not be interrupted
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        try {
            OpengurApp app = OpengurApp.getInstance(getApplicationContext());
            onUploadStarted(app);

            ImgurTopic topic = intent.getParcelableExtra(KEY_TOPIC);
            String title = intent.getStringExtra(KEY_TITLE);
            String desc = intent.getStringExtra(KEY_DESC);
            ArrayList<Upload> uploads = intent.getParcelableArrayListExtra(KEY_UPLOADS);
            boolean submitToGallery = intent.getBooleanExtra(KEY_SUBMIT_TO_GALLERY, false);

            int totalUploads = uploads.size();
            LogUtil.v(TAG, "Starting upload of " + uploads.size() + " images");
            ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);
            List<ImgurPhoto> uploadedPhotos = new ArrayList<>(totalUploads);
            SqlHelper sql = app.getSql();

            for (int i = 0; i < totalUploads; i++) {
                Upload u = uploads.get(i);
                RequestBody body = createUploadRequestBody(u);
                onPhotoUploading(totalUploads, i + 1);

                if (body != null) {
                    LogUtil.v(TAG, "Uploading photo " + (i + 1) + " of " + totalUploads);
                    // This will be synchronous
                    JSONObject json = client.doWork(body);
                    int status = json.getInt(ApiClient.KEY_STATUS);

                    if (status == ApiClient.STATUS_OK) {
                        ImgurPhoto photo = new ImgurPhoto(json.getJSONObject(ApiClient.KEY_DATA));
                        uploadedPhotos.add(photo);
                        sql.insertUploadedPhoto(photo);
                    } else {
                        LogUtil.w(TAG, "Photo failed to uploaded, status code returned " + status);
                    }
                }
            }

            if (uploads.size() == uploadedPhotos.size()) {
                // All photos uploaded correctly
                LogUtil.v(TAG, "All photos successfully uploaded, number of photos uploaded " + uploadedPhotos.size());
                onPhotosUploaded(uploadedPhotos, submitToGallery, title, desc, topic);
            } else if (uploadedPhotos.size() > 1) {
                // Some of the photos did not upload correctly
                LogUtil.w(TAG, uploadedPhotos.size() + " of " + uploads.size() + " photos were uploaded successfully");
                onPartialPhotoUpload(uploadedPhotos, uploads.size(), title, desc);
            } else {
                // No photos were uploaded, double you tee eff mate
                LogUtil.w(TAG, "No photos were uploaded");
                onPhotoUploadFailed();
            }
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Error parsing JSON", ex);
            onPhotoUploadFailed();
        } catch (IOException ex) {
            LogUtil.e(TAG, "Error while uploading", ex);
            onPhotoUploadFailed();
        } finally {
            if (wakeLock.isHeld()) wakeLock.release();
        }
    }

    /**
     * Called when the upload is about to begin.
     *
     * @param app
     */
    private void onUploadStarted(OpengurApp app) {
        mNotificationId = (int) System.currentTimeMillis();
        mManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        mBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.upload_notif_starting))
                .setContentText(getString(R.string.upload_notif_starting_content))
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(getResources().getColor(app.getImgurTheme().primaryColor));

        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called when a photo has begun to upload to the API
     *
     * @param totalUploads The total number of photos to upload
     * @param uploadNumber The current photo number that is being uploaded
     */
    private void onPhotoUploading(int totalUploads, int uploadNumber) {
        String contentText = getResources().getQuantityString(R.plurals.upload_notif_photos, totalUploads, uploadNumber, totalUploads);

        mBuilder.setContentTitle(getString(R.string.upload_notif_in_progress))
                .setContentText(contentText);

        if (totalUploads > 1) {
            mBuilder.setProgress(totalUploads, uploadNumber, false);
        } else {
            mBuilder.setProgress(0, 0, true);
        }

        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called when there is an error while uploading
     */
    private void onPhotoUploadFailed() {
        mBuilder.setContentTitle(getString(R.string.error))
                .setContentText(getString(R.string.upload_notif_error))
                .setProgress(0, 0, false)
                .setAutoCancel(true);

        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called when not every photo was uploaded successfully. This <b><i>will not</i></b> upload to the gallery if requested to
     *
     * @param uploadedPhotos The photos that were uploaded successfully
     * @param total          The total number of photos that should have been uploaded
     * @param title          The optional title of the upload
     * @param desc           The optional title of the upload
     */
    private void onPartialPhotoUpload(List<ImgurPhoto> uploadedPhotos, int total, @Nullable String title, @Nullable String desc) {
        if (uploadedPhotos.size() == 1) {
            // Show the notification immediately as an album will not be created for only 1 photo
            String msg = getString(R.string.upload_notif_error_upload_complete_long, uploadedPhotos.size(), total);
            String url = uploadedPhotos.get(0).getLink();
            Intent intent = NotificationReceiver.createCopyIntent(getApplicationContext(), url, mNotificationId);
            PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            mBuilder.setContentTitle(getString(R.string.error))
                    .setContentText(getString(R.string.upload_notif_error_upload_incomplete_short))
                    .setProgress(0, 0, false)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_action_copy, getString(R.string.copy_link), pIntent)
                    .setStyle(new NotificationCompat
                            .BigTextStyle()
                            .bigText(msg));

            mManager.notify(mNotificationId, mBuilder.build());
        } else {
            createAlbum(uploadedPhotos, false, title, desc, null);
        }
    }

    /**
     * Called when all photos have successfully been uploaded. Will attempt to create an album and/or upload to the gallery if desired
     *
     * @param uploadedPhotos  The photos that were uploaded
     * @param submitToGallery If submitting to the gallery
     * @param title           The optional title of the upload
     * @param desc            The optional title of the upload
     * @param topic           The optional topic when submitting to the gallery
     */
    private void onPhotosUploaded(List<ImgurPhoto> uploadedPhotos, boolean submitToGallery, @Nullable String title, @Nullable String desc, @Nullable ImgurTopic topic) {
        if (uploadedPhotos.size() == 1) {
            ImgurPhoto photo = uploadedPhotos.get(0);

            if (!submitToGallery) {
                LogUtil.v(TAG, "Image uploaded successfully, not submitting to gallery");
                onSuccessfulUpload(photo);
            } else {
                // Upload to gallery
                LogUtil.v(TAG, "Uploading image to gallery with title " + title);
                submitToGallery(title, topic != null ? topic.getId() : FALLBACK_TOPIC, photo);
            }
        } else {
            LogUtil.v(TAG, "Creating album");
            createAlbum(uploadedPhotos, submitToGallery, title, desc, topic);
        }
    }

    /**
     * Called when submitting to the gallery has failed
     *
     * @param upload
     */
    private void onGallerySubmitFailed(ImgurBaseObject upload) {
        String url = upload.getLink();
        Intent intent = NotificationReceiver.createCopyIntent(getApplicationContext(), url, mNotificationId);
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setContentTitle(getString(R.string.error))
                .setContentText(getString(R.string.upload_gallery_failed))
                .addAction(R.drawable.ic_action_copy, getString(R.string.copy_link), pIntent)
                .setProgress(0, 0, false)
                .setContentInfo(null)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.upload_gallery_failed_long)));

        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called when creating the album failed
     */
    private void onAlbumCreationFailed() {
        mBuilder.setContentTitle(getString(R.string.error))
                .setContentText(getString(R.string.upload_album_failed))
                .setProgress(0, 0, false)
                .setContentInfo(null)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.upload_album_failed_long)));

        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Called when everything has been successfully uploaded/created and the success notification should be displayed
     *
     * @param obj
     */
    private void onSuccessfulUpload(ImgurBaseObject obj) {
        String url = obj.getLink();
        Intent intent = NotificationReceiver.createCopyIntent(getApplicationContext(), url, mNotificationId);
        PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        mBuilder.setContentTitle(getString(R.string.upload_complete))
                .setContentText(getString(R.string.upload_success, url))
                .addAction(R.drawable.ic_action_copy, getString(R.string.copy_link), pIntent)
                .setProgress(0, 0, false)
                .setContentInfo(null);

        mManager.notify(mNotificationId, mBuilder.build());
    }

    /**
     * Creates the {@link RequestBody} for the image upload
     *
     * @param upload
     * @return
     */
    @Nullable
    private RequestBody createUploadRequestBody(@NonNull Upload upload) {
        RequestBody body = null;

        if (upload.isLink()) {
            FormEncodingBuilder builder = new FormEncodingBuilder()
                    .add("image", upload.getLocation())
                    .add("type", "URL");

            if (!TextUtils.isEmpty(upload.getTitle())) {
                builder.add("title", upload.getTitle());
            }

            if (!TextUtils.isEmpty(upload.getDescription())) {
                builder.add("description", upload.getDescription());
            }

            body = builder.build();
        } else {
            File file = new File(upload.getLocation());
            if (FileUtil.isFileValid(file)) {
                MediaType type;

                if (file.getAbsolutePath().endsWith("png")) {
                    type = MediaType.parse("image/png");
                } else if (file.getAbsolutePath().endsWith("gif")) {
                    type = MediaType.parse("image/gif");
                } else {
                    type = MediaType.parse("image/jpeg");
                }

                MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM)
                        .addPart(Headers.of("Content-Disposition", "form-data; name=\"image\""), RequestBody.create(type, file))
                        .addPart(Headers.of("Content-Disposition", "form-data; name=\"type\""), RequestBody.create(null, "file"));

                if (!TextUtils.isEmpty(upload.getTitle())) {
                    builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"title\""), RequestBody.create(null, upload.getTitle()));
                }

                if (!TextUtils.isEmpty(upload.getDescription())) {
                    builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"description\""), RequestBody.create(null, upload.getDescription()));
                }

                body = builder.build();
            } else {
                LogUtil.w(TAG, "Invalid file, unable to create RequestBody");
            }
        }

        return body;
    }

    /**
     * Creates the album from the uploaded photos
     *
     * @param uploadedPhotos  The photos that were uploaded
     * @param submitToGallery If submitting to the gallery
     * @param title           The optional title of the upload
     * @param desc            The optional title of the upload
     * @param topic           The optional topic when submitting to the gallery
     * @return
     */
    private void createAlbum(@NonNull List<ImgurPhoto> uploadedPhotos, boolean submitToGallery, @Nullable String title, @Nullable String desc, @Nullable ImgurTopic topic) {
        try {
            ApiClient client = new ApiClient(Endpoints.ALBUM_CREATION.getUrl(), ApiClient.HttpRequest.POST);
            String coverId = uploadedPhotos.get(0).getId();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < uploadedPhotos.size(); i++) {
                sb.append(uploadedPhotos.get(i).getId());
                if (i != uploadedPhotos.size() - 1) sb.append(",");
            }

            FormEncodingBuilder builder = new FormEncodingBuilder()
                    .add("ids", sb.toString())
                    .add("cover", coverId);
            if (!TextUtils.isEmpty(title)) builder.add("title", title);
            if (!TextUtils.isEmpty(desc)) builder.add("description", desc);

            JSONObject json = client.doWork(builder.build());
            int status = json.getInt(ApiClient.KEY_STATUS);

            if (status == ApiClient.STATUS_OK) {
                ImgurAlbum album = new ImgurAlbum(json.getJSONObject(ApiClient.KEY_DATA));
                // The object will not be fully completed, add the needed fields
                album.setLink("https://imgur.com/a/" + album.getId());
                album.setCoverId(coverId);
                OpengurApp.getInstance(getApplicationContext()).getSql().insertUploadedAlbum(album);

                if (!submitToGallery) {
                    LogUtil.v(TAG, "Album creation successful");
                    onSuccessfulUpload(album);
                } else {
                    LogUtil.v(TAG, "Submitting album to gallery with title " + title);
                    submitToGallery(title, topic != null ? topic.getId() : FALLBACK_TOPIC, album);
                }
            } else {
                LogUtil.v(TAG, "Unable to submit to gallery with status " + status);
                onAlbumCreationFailed();
            }
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Error while parsing JSON while creating album", ex);
            onAlbumCreationFailed();
        } catch (IOException ex) {
            LogUtil.e(TAG, "Error while creating album", ex);
            onAlbumCreationFailed();
        }
    }

    /**
     * Submits the photo/album to the Imgur Gallery
     *
     * @param title   The title for the gallery
     * @param topicId The topic id for the gallery
     * @param upload  The item being submitted to the gallery
     */
    private void submitToGallery(@NonNull String title, int topicId, ImgurBaseObject upload) {
        mBuilder.setContentTitle(getString(R.string.upload_gallery_title))
                .setContentText(getString(R.string.upload_gallery_message))
                .setProgress(0, 0, true);

        mManager.notify(mNotificationId, mBuilder.build());

        try {
            ApiClient client = new ApiClient(String.format(Endpoints.GALLERY_UPLOAD.getUrl(), upload.getId()), ApiClient.HttpRequest.POST);

            RequestBody body = new FormEncodingBuilder()
                    .add("title", title)
                    .add("topic", String.valueOf(topicId))
                    .add("terms", "1").build();

            JSONObject json = client.doWork(body);
            int status = json.getInt(ApiClient.KEY_STATUS);

            if (status == ApiClient.STATUS_OK) {
                boolean result = json.getBoolean(ApiClient.KEY_DATA);
                LogUtil.v(TAG, "Result of gallery submission " + result);

                if (result) {
                    onSuccessfulUpload(upload);
                } else {
                    onGallerySubmitFailed(upload);
                }

            } else {
                LogUtil.v(TAG, "Unable to submit to gallery with status " + status);
                onGallerySubmitFailed(upload);
            }
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Error while parsing JSON while uploading", ex);
            onGallerySubmitFailed(upload);
        } catch (IOException ex) {
            LogUtil.e(TAG, "Error while uploading", ex);
            onGallerySubmitFailed(upload);
        }
    }

    @Override
    public void onDestroy() {
        LogUtil.v(TAG, "onDestroy");
        mManager = null;
        mBuilder = null;
        super.onDestroy();
    }
}
