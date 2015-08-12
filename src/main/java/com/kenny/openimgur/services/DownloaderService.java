package com.kenny.openimgur.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.BaseNotification;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;

import java.io.File;

/**
 * Created by kcampagna on 6/30/14.
 */
public class DownloaderService extends IntentService {
    private static final String FOLDER_NAME = "OpenImgur";

    private static final String TAG = DownloaderService.class.getSimpleName();

    private static final String KEY_IMAGE_URL = "image_url";

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        return new Intent(context, DownloaderService.class).putExtra(DownloaderService.KEY_IMAGE_URL, url);
    }

    public DownloaderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            String url = intent.getStringExtra(KEY_IMAGE_URL);

            if (TextUtils.isEmpty(url)) {
                LogUtil.e(TAG, "Nothing was passed in to be downloaded");
                return;
            }

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER_NAME);
            file.mkdirs();
            String photoFileName;
            boolean isUsingVideoLink = false;
            String photoType = LinkUtils.getImageType(url);
            String photoId = String.valueOf(System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);

            // JPEG Image
            if (ImgurPhoto.IMAGE_TYPE_JPEG.equals(photoType)) {
                photoFileName = photoId + FileUtil.EXTENSION_JPEG;
            } else if (ImgurPhoto.IMAGE_TYPE_GIF.equals(photoType)) {
                // Gif Image, urls don't need to be tested for an mp4 here
                photoFileName = photoId + FileUtil.EXTENSION_GIF;
            } else if (LinkUtils.isVideoLink(url)) {
                // Check the photo link for videos
                isUsingVideoLink = true;
                photoFileName = photoId + FileUtil.EXTENSION_MP4;
            } else {
                photoFileName = photoId + FileUtil.EXTENSION_PNG;
            }

            File photoFile = new File(file.getAbsolutePath(), photoFileName);
            LogUtil.v(TAG, "Downloading image to " + photoFile.getAbsolutePath());
            DownloadNotification notification = new DownloadNotification(getApplicationContext(), photoId.hashCode());
            boolean saved;

            // Check if the video is already in our cache
            if (isUsingVideoLink) {
                File cachedFile = VideoCache.getInstance().getVideoFile(url);

                if (FileUtil.isFileValid(cachedFile)) {
                    LogUtil.v(TAG, "Video file present in cache, copying");
                    saved = FileUtil.copyFile(cachedFile, photoFile);
                } else {
                    saved = FileUtil.saveUrl(url, photoFile);
                }
            } else {
                saved = FileUtil.saveUrl(url, photoFile);
            }

            if (saved) {
                LogUtil.v(TAG, "Image download completed");
                Uri fileUri = Uri.fromFile(photoFile);

                // Let the system know we have a new file
                FileUtil.scanFile(fileUri, getApplicationContext());
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType(photoType);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                PendingIntent shareP = PendingIntent.getActivity(getApplicationContext(), 0, Intent.createChooser(shareIntent, getString(R.string.share)), PendingIntent.FLAG_UPDATE_CURRENT);

                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                viewIntent.setDataAndType(fileUri, isUsingVideoLink ? "video/mp4" : photoType);
                PendingIntent viewP = PendingIntent.getActivity(getApplicationContext(), 1, viewIntent, PendingIntent.FLAG_ONE_SHOT);

                // Get the correct preview image for the notification based on if it is a video or not
                Bitmap bm = isUsingVideoLink ? ImageUtil.toGrayScale(ThumbnailUtils.createVideoThumbnail(photoFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND)) :
                        ImageUtil.toGrayScale(ImageUtil.decodeSampledBitmapFromResource(photoFile, 256, 256));

                notification.onDownloadComplete(bm, viewP, shareP);
            } else {
                LogUtil.w(TAG, "Image download failed");
                notification.onError();
            }

        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while downloading image", e);
        }
    }

    private static class DownloadNotification extends BaseNotification {
        private int mPhotoHash;

        public DownloadNotification(Context context, int photoHash) {
            super(context);
            mPhotoHash = photoHash;
            builder.setProgress(0, 0, true);
            postNotification();
        }

        @NonNull
        @Override
        protected String getTitle() {
            return app.getString(R.string.image_downloading);
        }

        @Override
        protected int getNotificationId() {
            return mPhotoHash;
        }

        public void onDownloadComplete(Bitmap bitmap, PendingIntent viewIntent, PendingIntent shareIntent) {
            if (bitmap != null) {
                NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
                bigPicStyle.setBigContentTitle(app.getString(R.string.download_complete));
                bigPicStyle.setSummaryText(app.getString(R.string.tap_to_view));
                bigPicStyle.bigPicture(bitmap);
                builder.setStyle(bigPicStyle)
                        .setLargeIcon(bitmap);
            }

            builder.setProgress(0, 0, false)
                    .setContentIntent(viewIntent)
                    .addAction(R.drawable.ic_share_white_24dp, app.getString(R.string.share), shareIntent)
                    .setContentTitle(app.getString(R.string.download_complete))
                    .setContentText(app.getString(R.string.tap_to_view));

            postNotification();
        }

        public void onError() {
            builder.setProgress(0, 0, false)
                    .setContentTitle(app.getString(R.string.error))
                    .setContentText(app.getString(R.string.download_error));

            postNotification();
        }
    }
}
