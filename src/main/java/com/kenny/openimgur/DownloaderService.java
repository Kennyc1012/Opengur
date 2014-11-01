package com.kenny.openimgur;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;

import java.io.File;

/**
 * Created by kcampagna on 6/30/14.
 */
public class DownloaderService extends IntentService {
    private static final String FOLDER_NAME = "OpenImgur";

    private static final String TAG = DownloaderService.class.getSimpleName();

    private static final String KEY_IMAGE = "image";

    public DownloaderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (intent.hasExtra(KEY_IMAGE)) {
                ImgurPhoto photo = intent.getParcelableExtra(KEY_IMAGE);
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER_NAME);
                file.mkdirs();
                String photoFileName;
                boolean isUsingVideoLink = false;

                if (ImgurPhoto.IMAGE_TYPE_JPEG.equals(photo.getType())) {
                    photoFileName = photo.getId() + FileUtil.EXTENSION_JPEG;
                } else if (ImgurPhoto.IMAGE_TYPE_GIF.equals(photo.getType())) {

                    if (photo.isLinkAThumbnail() && photo.hasMP4Link()) {
                        photoFileName = photo.getId() + FileUtil.EXTENSION_MP4;
                        isUsingVideoLink = true;
                    } else {
                        photoFileName = photo.getId() + FileUtil.EXTENSION_GIF;
                    }

                } else {
                    photoFileName = photo.getId() + FileUtil.EXTENSION_PNG;
                }

                File photoFile = new File(file.getAbsolutePath(), photoFileName);
                LogUtil.v(TAG, "Downloading image to " + photoFile.getAbsolutePath());
                int notificationId = photo.getLink().hashCode();
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notification);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.image_downloading))
                        .setContentText(getString(R.string.downloading_msg)).setAutoCancel(true).setProgress(0, 0, true).setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_launcher);
                manager.notify(notificationId, builder.build());

                boolean saved;

                // Check if the video is already in our cache
                if (isUsingVideoLink) {
                    File cachedFile = VideoCache.getInstance().getVideoFile(photo.getMP4Link());

                    if (FileUtil.isFileValid(cachedFile)) {
                        LogUtil.v(TAG, "Video file present in cache, copying");
                        saved = FileUtil.copyFile(cachedFile, photoFile);
                    } else {
                        saved = FileUtil.savePhoto(photo, photoFile);
                    }
                } else {
                    saved = FileUtil.savePhoto(photo, photoFile);
                }

                if (saved) {
                    LogUtil.v(TAG, "Image download completed");
                    Uri fileUri = Uri.fromFile(photoFile);

                    // Let the system know we have a new file
                    FileUtil.scanFile(fileUri, getApplicationContext());
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType(photo.getType());
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    PendingIntent shareP = PendingIntent.getActivity(getApplicationContext(), 0, shareIntent, PendingIntent.FLAG_ONE_SHOT);

                    Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                    viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    viewIntent.setDataAndType(fileUri, isUsingVideoLink ? "video/mp4" : photo.getType());
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
            } else {
                LogUtil.w(TAG, "No photo passed in Intent");
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Exception while downloading image", e);
        }

    }

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, DownloaderService.class).putExtra(DownloaderService.KEY_IMAGE, photo);
    }
}
