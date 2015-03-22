package com.kenny.openimgur.services;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
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

    private static final String KEY_IMAGE = "image";

    private static final String KEY_IMAGE_URL = "image_url";

    public DownloaderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            ImgurPhoto photo = intent.getParcelableExtra(KEY_IMAGE);
            String url = intent.getStringExtra(KEY_IMAGE_URL);

            if (photo == null && TextUtils.isEmpty(url)) {
                LogUtil.e(TAG, "Nothing was passed in to be downloaded");
                return;
            }

            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER_NAME);
            file.mkdirs();
            String photoFileName;
            boolean isUsingVideoLink = false;
            String photoType = photo != null ? photo.getType() : LinkUtils.getImageType(url);
            String photoId = photo != null ? photo.getId() : String.valueOf(System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);

            // JPEG Image
            if (ImgurPhoto.IMAGE_TYPE_JPEG.equals(photoType)) {
                photoFileName = photoId + FileUtil.EXTENSION_JPEG;
            } else if (ImgurPhoto.IMAGE_TYPE_GIF.equals(photoType)) {
                // Gif Image, urls don't need to be tested for an mp4 here
                if (photo != null && photo.isLinkAThumbnail() && photo.hasMP4Link()) {
                    photoFileName = photoId + FileUtil.EXTENSION_MP4;
                    isUsingVideoLink = true;
                } else {
                    photoFileName = photoId + FileUtil.EXTENSION_GIF;
                }
            } else if (photo == null && LinkUtils.isVideoLink(url)) {
                // Check the photo link for videos
                isUsingVideoLink = true;
                photoFileName = photoId + FileUtil.EXTENSION_MP4;
            } else {
                photoFileName = photoId + FileUtil.EXTENSION_PNG;
            }

            File photoFile = new File(file.getAbsolutePath(), photoFileName);
            LogUtil.v(TAG, "Downloading image to " + photoFile.getAbsolutePath());
            int notificationId = photoId.hashCode();
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.image_downloading))
                    .setContentText(getString(R.string.downloading_msg)).setAutoCancel(true).setProgress(0, 0, true).setLargeIcon(icon)
                    .setSmallIcon(Build.VERSION.SDK_INT < 21 ? R.drawable.ic_launcher : R.drawable.ic_i);
            manager.notify(notificationId, builder.build());

            boolean saved;

            // Check if the video is already in our cache
            if (isUsingVideoLink) {
                String videoUrl = photo != null ? photo.getMP4Link() : url;
                File cachedFile = VideoCache.getInstance().getVideoFile(videoUrl);

                if (FileUtil.isFileValid(cachedFile)) {
                    LogUtil.v(TAG, "Video file present in cache, copying");
                    saved = FileUtil.copyFile(cachedFile, photoFile);
                } else {
                    String urlToSave = photo != null ? photo.getLink() : url;
                    saved = FileUtil.saveUrl(urlToSave, photoFile);
                }
            } else {
                String urlToSave = photo != null ? photo.getLink() : url;
                saved = FileUtil.saveUrl(urlToSave, photoFile);
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
                PendingIntent shareP = PendingIntent.getActivity(getApplicationContext(), 0, shareIntent, PendingIntent.FLAG_ONE_SHOT);

                Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                viewIntent.setDataAndType(fileUri, isUsingVideoLink ? "video/mp4" : photoType);
                PendingIntent viewP = PendingIntent.getActivity(getApplicationContext(), 1, viewIntent, PendingIntent.FLAG_ONE_SHOT);

                // Get the correct preview image for the notification based on if it is a video or not
                Bitmap bm = isUsingVideoLink ? ImageUtil.toGrayScale(ThumbnailUtils.createVideoThumbnail(photoFile.getAbsolutePath(), MediaStore.Video.Thumbnails.MINI_KIND)) :
                        ImageUtil.toGrayScale(ImageUtil.decodeSampledBitmapFromResource(photoFile, 256, 256));

                if (bm != null) {
                    NotificationCompat.BigPictureStyle bigPicStyle = new NotificationCompat.BigPictureStyle();
                    bigPicStyle.setBigContentTitle(getString(R.string.download_complete));
                    bigPicStyle.setSummaryText(getString(R.string.tap_to_view));
                    bigPicStyle.bigLargeIcon(icon);
                    bigPicStyle.bigPicture(bm);
                    builder.setStyle(bigPicStyle);
                }

                builder.setProgress(0, 0, false).setContentIntent(viewP).addAction(R.drawable.ic_action_share, getString(R.string.share), shareP)
                        .setContentTitle(getString(R.string.download_complete)).setContentText(getString(R.string.tap_to_view)).setLargeIcon(bm);
                manager.notify(notificationId, builder.build());
            } else {
                LogUtil.w(TAG, "Image download failed");
                builder.setProgress(0, 0, false).setContentTitle(getString(R.string.error)).setContentText(getString(R.string.download_error));
                manager.notify(notificationId, builder.build());
            }

        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while downloading image", e);
        }

    }

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, DownloaderService.class).putExtra(DownloaderService.KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        return new Intent(context, DownloaderService.class).putExtra(DownloaderService.KEY_IMAGE_URL, url);
    }
}
