package com.kenny.openimgur.services;

import android.app.IntentService;
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
import com.kenny.openimgur.api.responses.BasicObjectResponse;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.PhotoResponse;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.ui.BaseNotification;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.SqlHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Response;

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

    private UploadNotification mNotification;

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
            mNotification = new UploadNotification(getApplicationContext());

            ImgurTopic topic = intent.getParcelableExtra(KEY_TOPIC);
            String title = intent.getStringExtra(KEY_TITLE);
            String desc = intent.getStringExtra(KEY_DESC);
            ArrayList<Upload> uploads = intent.getParcelableArrayListExtra(KEY_UPLOADS);
            boolean submitToGallery = intent.getBooleanExtra(KEY_SUBMIT_TO_GALLERY, false);

            int totalUploads = uploads.size();
            LogUtil.v(TAG, "Starting upload of " + totalUploads + " images");
            List<ImgurPhoto> uploadedPhotos = new ArrayList<>(totalUploads);
            SqlHelper sql = SqlHelper.getInstance(getApplicationContext());

            for (int i = 0; i < totalUploads; i++) {
                Upload u = uploads.get(i);
                mNotification.onPhotoUploading(totalUploads, i + 1);
                Response<PhotoResponse> response = null;

                try {
                    LogUtil.v(TAG, "Uploading photo " + (i + 1) + " of " + totalUploads);

                    if (u.isLink()) {
                        response = ApiClient.getService().uploadLink(u.getLocation(), u.getTitle(), u.getDescription(), "URL").execute();
                    } else {
                        File file = new File(u.getLocation());

                        if (FileUtil.isFileValid(file)) {
                            String type;

                            if (file.getAbsolutePath().endsWith("png")) {
                                type = "image/png";
                            } else if (file.getAbsolutePath().endsWith("gif")) {
                                type = "image/gif";
                            } else {
                                type = "image/jpeg";
                            }

                            RequestBody uploadFile = RequestBody.create(MediaType.parse(type), file);
                            RequestBody uploadTitle = null;
                            RequestBody uploadDesc = null;
                            RequestBody uploadType = RequestBody.create(MediaType.parse("text/plain"), "file");

                            if (!TextUtils.isEmpty(u.getTitle())) {
                                uploadTitle = RequestBody.create(MediaType.parse("text/plain"), u.getTitle());
                            }

                            if (!TextUtils.isEmpty(u.getDescription())) {
                                uploadDesc = RequestBody.create(MediaType.parse("text/plain"), u.getDescription());
                            }

                            response = ApiClient.getService().uploadPhoto(uploadFile, uploadTitle, uploadDesc, uploadType).execute();
                        } else {
                            LogUtil.w(TAG, "Unable to find file at location " + u.getLocation());
                        }
                    }
                } catch (Exception ex) {
                    LogUtil.e(TAG, "Error uploading image", ex);
                    response = null;
                }

                if (response != null && response.body() != null && response.body().data != null) {
                    sql.insertUploadedPhoto(response.body().data);
                    uploadedPhotos.add(response.body().data);
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
                mNotification.onPhotoUploadFailed();
            }
        } finally {
            NetworkUtils.releaseWakeLock(wakeLock);
        }
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
            mNotification.onPartialPhotoUpload(uploadedPhotos.get(0), total);
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
                mNotification.onSuccessfulUpload(photo);
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
            String coverId = uploadedPhotos.get(0).getId();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < uploadedPhotos.size(); i++) {
                sb.append(uploadedPhotos.get(i).getId());
                if (i != uploadedPhotos.size() - 1) sb.append(",");
            }

            Response<BasicObjectResponse> response = ApiClient.getService().createAlbum(sb.toString(), coverId, title, desc).execute();

            if (response != null && response.body() != null && response.body().data != null) {
                ImgurBaseObject object = response.body().data;

                // The response only contains the id and the delete hash, we need to construct the object from them
                String link = ApiClient.IMGUR_URL + "a/" + object.getId();
                ImgurAlbum album = new ImgurAlbum(object.getId(), title, link, object.getDeleteHash());
                album.setCoverId(coverId);
                SqlHelper.getInstance(getApplicationContext()).insertUploadedAlbum(album);

                if (!submitToGallery) {
                    LogUtil.v(TAG, "Album creation successful");
                    mNotification.onSuccessfulUpload(album);
                } else {
                    LogUtil.v(TAG, "Submitting album to gallery with title " + title);
                    submitToGallery(title, topic != null ? topic.getId() : FALLBACK_TOPIC, album);
                }
            } else {
                LogUtil.w(TAG, "Response did not receive an object");
                mNotification.onAlbumCreationFailed();
            }
        } catch (Exception ex) {
            LogUtil.e(TAG, "Error while creating album", ex);
            mNotification.onAlbumCreationFailed();
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
        mNotification.onSubmitToGallery();

        try {
            Response<BasicResponse> response = ApiClient.getService().submitToGallery(upload.getId(), title, topicId, "1").execute();

            if (response != null && response.body() != null && response.body().data) {
                mNotification.onSuccessfulUpload(upload);
            } else {
                mNotification.onGallerySubmitFailed(upload);
            }
        } catch (Exception ex) {
            LogUtil.e(TAG, "Error while submitting to gallery", ex);
            mNotification.onGallerySubmitFailed(upload);
        }
    }

    @Override
    public void onDestroy() {
        LogUtil.v(TAG, "onDestroy");
        mNotification = null;
        super.onDestroy();
    }

    private static class UploadNotification extends BaseNotification {
        private int notificationId;

        public UploadNotification(Context context) {
            super(context);
            notificationId = (int) System.currentTimeMillis();
            builder.setContentTitle(app.getString(R.string.upload_notif_starting))
                    .setContentText(app.getString(R.string.upload_notif_starting_content));
        }

        /**
         * Called when a photo has begun to upload to the API
         *
         * @param totalUploads The total number of photos to upload
         * @param uploadNumber The current photo number that is being uploaded
         */
        public void onPhotoUploading(int totalUploads, int uploadNumber) {
            String contentText = app.getResources().getQuantityString(R.plurals.upload_notif_photos, totalUploads, uploadNumber, totalUploads);

            builder.setContentTitle(app.getString(R.string.upload_notif_in_progress))
                    .setContentText(contentText);

            if (totalUploads > 1) {
                builder.setProgress(totalUploads, uploadNumber, false);
            } else {
                builder.setProgress(0, 0, true);
            }

            postNotification();
        }

        /**
         * Called when everything has been successfully uploaded/created and the success notification should be displayed
         *
         * @param obj
         */
        public void onSuccessfulUpload(ImgurBaseObject obj) {
            String url = obj.getLink();
            Intent intent = NotificationReceiver.createCopyIntent(app, url, getNotificationId());
            PendingIntent pIntent = PendingIntent.getBroadcast(app, RequestCodes.UPLOADS, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.setContentTitle(app.getString(R.string.upload_complete))
                    .setContentText(app.getString(R.string.upload_success, url))
                    .addAction(R.drawable.ic_action_copy_24dp, app.getString(R.string.copy_link), pIntent)
                    .setProgress(0, 0, false)
                    .setContentInfo(null);

            postNotification();
        }

        /**
         * Called when submitting to the gallery has failed
         *
         * @param upload
         */
        public void onGallerySubmitFailed(ImgurBaseObject upload) {
            String url = upload.getLink();
            Intent intent = NotificationReceiver.createCopyIntent(app, url, getNotificationId());
            PendingIntent pIntent = PendingIntent.getBroadcast(app, RequestCodes.UPLOADS, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.setContentTitle(app.getString(R.string.error))
                    .setContentText(app.getString(R.string.upload_gallery_failed))
                    .addAction(R.drawable.ic_action_copy_24dp, app.getString(R.string.copy_link), pIntent)
                    .setProgress(0, 0, false)
                    .setContentInfo(null)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(app.getString(R.string.upload_gallery_failed_long)));

            postNotification();
        }

        /**
         * Called when creating the album failed
         */
        public void onAlbumCreationFailed() {
            builder.setContentTitle(app.getString(R.string.error))
                    .setContentText(app.getString(R.string.upload_album_failed))
                    .setProgress(0, 0, false)
                    .setContentInfo(null)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(app.getString(R.string.upload_album_failed_long)));

            postNotification();
        }

        /**
         * Called when there is an error while uploading
         */
        public void onPhotoUploadFailed() {
            builder.setContentTitle(app.getString(R.string.error))
                    .setContentText(app.getString(R.string.upload_notif_error))
                    .setProgress(0, 0, false)
                    .setAutoCancel(true);

            postNotification();
        }

        /**
         * Called when submitting to gallery
         */
        public void onSubmitToGallery() {
            builder.setContentTitle(app.getString(R.string.upload_gallery_title))
                    .setContentText(app.getString(R.string.upload_gallery_message))
                    .setProgress(0, 0, true);

            postNotification();
        }

        /**
         * Called when not every photo was uploaded successfully.
         *
         * @param photo The photo that was uploaded
         * @param total How many photos were supposed to be uploaded
         */
        public void onPartialPhotoUpload(ImgurPhoto photo, int total) {
            // Show the notification immediately as an album will not be created for only 1 photo
            String msg = app.getString(R.string.upload_notif_error_upload_complete_long, 1, total);
            String url = photo.getLink();
            Intent intent = NotificationReceiver.createCopyIntent(app, url, getNotificationId());
            PendingIntent pIntent = PendingIntent.getBroadcast(app, RequestCodes.UPLOADS, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            builder.setContentTitle(app.getString(R.string.error))
                    .setContentText(app.getString(R.string.upload_notif_error_upload_incomplete_short))
                    .setProgress(0, 0, false)
                    .setAutoCancel(true)
                    .addAction(R.drawable.ic_action_copy_24dp, app.getString(R.string.copy_link), pIntent)
                    .setStyle(new NotificationCompat
                            .BigTextStyle()
                            .bigText(msg));

            postNotification();
        }

        @NonNull
        @Override
        protected String getTitle() {
            return "";
        }

        @Override
        protected int getNotificationId() {
            return notificationId;
        }
    }
}
