package com.kenny.openimgur.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.BaseNotification;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.RequestCodes;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by kcampagna on 6/30/14.
 */
public class DownloaderService extends IntentService {
    private static final String FOLDER_NAME = "Opengur";

    private static final String TAG = DownloaderService.class.getSimpleName();

    private static final String KEY_IMAGE_URLS = "image_urls";

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        ArrayList<String> urls = new ArrayList<>(1);
        urls.add(url);
        return createIntent(context, urls);
    }

    public static Intent createIntent(@NonNull Context context, ArrayList<String> urls) {
        return new Intent(context, DownloaderService.class).putExtra(DownloaderService.KEY_IMAGE_URLS, urls);
    }

    public DownloaderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Get a wake lock so any long uploads will not be interrupted
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire();

        try {
            Context context = getApplicationContext();
            ArrayList<String> photoUrls = intent.getStringArrayListExtra(KEY_IMAGE_URLS);

            if (photoUrls == null || photoUrls.isEmpty()) {
                LogUtil.e(TAG, "Nothing was passed in to be downloaded");
                return;
            }


            boolean isMultiUpload = photoUrls.size() > 1;
            DownloadNotification notification = new DownloadNotification(context, photoUrls.size());
            int count = 1;
            int totalDownloaded = 0;
            int totalPhotos = photoUrls.size();

            // Make any needed folders
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER_NAME);
            file.mkdirs();
            String directoryPath = file.getAbsolutePath();
            LogUtil.v(TAG, "Downloading " + totalPhotos + " photos");

            for (String url : photoUrls) {
                File savedFile = saveUrl(url, directoryPath);
                notification.updateMessage(count);

                if (FileUtil.isFileValid(savedFile)) {
                    totalDownloaded++;
                    LogUtil.v(TAG, "Image download completed for URL " + url);
                    Uri fileUri = Uri.fromFile(savedFile);
                    // Let the system know we have a new file
                    FileUtil.scanFile(fileUri, context);

                    // Single image downloads will show multiple options and a preview in the notification
                    if (!isMultiUpload) {
                        Uri shareUri = FileProvider.getUriForFile(context, OpengurApp.AUTHORITY, savedFile);
                        String photoType = LinkUtils.getImageType(url);
                        boolean isVideoLink = savedFile.getAbsolutePath().endsWith(FileUtil.EXTENSION_MP4);

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(photoType);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                        PendingIntent shareP = PendingIntent.getActivity(context, RequestCodes.DOWNLOAD_SHARE, Intent.createChooser(shareIntent, getString(R.string.share)), PendingIntent.FLAG_UPDATE_CURRENT);

                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        viewIntent.setDataAndType(shareUri, isVideoLink ? "video/mp4" : photoType);
                        PendingIntent viewP = PendingIntent.getActivity(context, RequestCodes.DOWNLOAD_VIEW, viewIntent, PendingIntent.FLAG_ONE_SHOT);

                        Intent deleteIntent = NotificationReceiver.createDeleteIntent(context, notification.getNotificationId(), savedFile.getAbsolutePath());
                        PendingIntent deleteP = PendingIntent.getBroadcast(context, RequestCodes.DOWNLOAD_DELETE, deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                        // Get the correct preview image for the notification based on if it is a video or not
                        Bitmap bm = isVideoLink ? ImageUtil.toGrayScale(ThumbnailUtils.createVideoThumbnail(savedFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND)) :
                                ImageUtil.toGrayScale(ImageUtil.decodeSampledBitmapFromResource(savedFile, 256, 256));

                        notification.onSingleImageDownloadComplete(bm, viewP, shareP, deleteP);
                    }
                } else if (!isMultiUpload) {
                    notification.onError();
                }

                count++;
            }

            if (isMultiUpload) {
                if (totalDownloaded == totalPhotos) {
                    LogUtil.v(TAG, "All photos downloaded successfully");
                    notification.onMultiImageDownloadComplete(totalDownloaded);
                } else {
                    LogUtil.w(TAG, totalDownloaded + " of " + totalPhotos + " downloaded successfully");
                    notification.onMultiImageDownloadError(totalDownloaded, totalPhotos);
                }

            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while downloading image", e);
        } finally {
            NetworkUtils.releaseWakeLock(wakeLock);
        }
    }

    @Nullable
    private String getPhotoFileName(@Nullable String url) {
        if (TextUtils.isEmpty(url)) return null;

        String photoFileName;
        String photoType = LinkUtils.getImageType(url);
        String photoId = LinkUtils.getId(url);
        if (TextUtils.isEmpty(photoId)) photoId = String.valueOf(System.currentTimeMillis());

        if (ImgurPhoto.IMAGE_TYPE_JPEG.equals(photoType)) {
            photoFileName = photoId + FileUtil.EXTENSION_JPEG;
        } else if (ImgurPhoto.IMAGE_TYPE_GIF.equals(photoType)) {
            photoFileName = photoId + FileUtil.EXTENSION_GIF;
        } else if (LinkUtils.isVideoLink(url)) {
            photoFileName = photoId + FileUtil.EXTENSION_MP4;
        } else {
            photoFileName = photoId + FileUtil.EXTENSION_PNG;
        }

        return photoFileName;
    }

    /**
     * Attempts to save the url to the device
     *
     * @param url           URL of the photo
     * @param directoryPath The parent directory where the photo will be saved
     * @return The {@link File} representing the photo. NULL may be returned if unsuccessful
     */
    @Nullable
    private File saveUrl(@NonNull String url, @NonNull String directoryPath) {
        File photoFile = null;
        String fileName = getPhotoFileName(url);

        if (!TextUtils.isEmpty(fileName)) {
            boolean isVideoLink = fileName.endsWith(FileUtil.EXTENSION_MP4);
            photoFile = new File(directoryPath, fileName);
            File cachedFile;

            // Check if we already downloaded the image before
            if (isVideoLink) {
                cachedFile = VideoCache.getInstance().getVideoFile(url);
            } else {
                cachedFile = ImageUtil.getImageLoader(getApplicationContext()).getDiskCache().get(url);
            }

            if (FileUtil.isFileValid(cachedFile)) {
                LogUtil.v(TAG, "Image present in cache, copying");
                FileUtil.copyFile(cachedFile, photoFile);
            } else {
                LogUtil.v(TAG, "Downloading image to " + photoFile.getAbsolutePath());
                FileUtil.saveUrl(url, photoFile);
            }
        }

        return photoFile;
    }

    private static class DownloadNotification extends BaseNotification {
        private int mId;

        private int mNumPhotos;

        public DownloadNotification(Context context, int numPhotos) {
            super(context);
            mNumPhotos = numPhotos;
            mId = (int) System.currentTimeMillis();

            if (numPhotos == 1) {
                builder.setProgress(0, 0, false);
                postNotification();
            }
        }

        @NonNull
        @Override
        protected String getTitle() {
            return resources.getQuantityString(R.plurals.download_notif_title, mNumPhotos);
        }

        @Override
        protected int getNotificationId() {
            return mId;
        }

        /**
         * Updates the message for the notification. This should only be called when multiple images are being downloaded
         *
         * @param currentPhotoNumber The current photo number that is being downloaded
         */
        public void updateMessage(int currentPhotoNumber) {
            String message = resources.getString(R.string.download_notif_message, currentPhotoNumber, mNumPhotos);
            builder.setProgress(mNumPhotos, currentPhotoNumber, false);
            builder.setContentText(message);
            postNotification();
        }

        /**
         * Called when a single image has been downloaded. A single image will have a view and share action
         *
         * @param bitmap      The {@link Bitmap} for display in the notification
         * @param viewIntent  The {@link PendingIntent} for viewing the image
         * @param shareIntent The {@link PendingIntent} for sharing the image
         */
        public void onSingleImageDownloadComplete(Bitmap bitmap, PendingIntent viewIntent, PendingIntent shareIntent, PendingIntent deleteIntent) {
            String title = resources.getString(R.string.download_notif_complete);

            if (bitmap != null) {
                NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
                bigPicStyle.setBigContentTitle(title);
                bigPicStyle.setSummaryText(resources.getString(R.string.tap_to_view));
                bigPicStyle.bigPicture(bitmap);
                builder.setStyle(bigPicStyle)
                        .setLargeIcon(bitmap);
            }

            builder.setProgress(0, 0, false)
                    .setContentIntent(viewIntent)
                    .addAction(R.drawable.ic_share_24dp, resources.getString(R.string.share), shareIntent)
                    .addAction(R.drawable.ic_delete_24dp, resources.getString(R.string.delete), deleteIntent)
                    .setContentTitle(title)
                    .setContentText(resources.getString(R.string.tap_to_view));

            postNotification();
        }

        /**
         * Called when all images have successfully downloaded
         *
         * @param totalImages The total number of images downloaded
         */
        public void onMultiImageDownloadComplete(int totalImages) {
            builder.setProgress(0, 0, false)
                    .setContentTitle(resources.getString(R.string.download_notif_complete))
                    .setContentText(resources.getString(R.string.download_notif_multi_message, totalImages));

            postNotification();
        }

        /**
         * Called when not all images are downloaded successfully
         *
         * @param totalDownloaded The total number of images that were downloaded
         * @param totalPhotos     The total number of photos that were supposed to be downloaded
         */
        public void onMultiImageDownloadError(int totalDownloaded, int totalPhotos) {
            builder.setProgress(0, 0, false)
                    .setContentTitle(resources.getString(R.string.download_notif_error))
                    .setContentText(resources.getString(R.string.download_notif_multi_error_msg, totalDownloaded, totalPhotos));

            postNotification();
        }

        public void onError() {
            builder.setProgress(0, 0, false)
                    .setContentTitle(resources.getString(R.string.download_notif_error))
                    .setContentText(resources.getString(R.string.download_error));

            postNotification();
        }
    }
}
