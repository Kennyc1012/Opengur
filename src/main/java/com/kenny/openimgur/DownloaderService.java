package com.kenny.openimgur;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;

import java.io.File;

/**
 * Created by kcampagna on 6/30/14.
 */
public class DownloaderService extends IntentService {
    private static final String FOLDER_NAME = "OpenImgur";

    public static final String TAG = DownloaderService.class.getSimpleName();

    public static final String KEY_IMAGE = "image";

    public DownloaderService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            if (intent.hasExtra(KEY_IMAGE)) {
                ImgurPhoto photo = intent.getParcelableExtra(KEY_IMAGE);
                File file = new File(Environment.getExternalStorageDirectory(), FOLDER_NAME);
                file.mkdirs();

                String photoFileName = null;
                if (photo.getType().equals(ImgurPhoto.IMAGE_TYPE_JPEG)) {
                    photoFileName = photo.getId() + ".jpeg";
                } else if (photo.getType().equals(ImgurPhoto.IMAGE_TYPE_GIF)) {
                    photoFileName = photo.getId() + ".gif";
                } else {
                    photoFileName = photo.getId() + ".png";
                }

                File photoFile = new File(file.getAbsolutePath(), photoFileName);
                Log.v(TAG, "Downloading image to " + photoFile.getAbsolutePath());

                int notificationId = photo.getLink().hashCode();
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setContentTitle(getString(R.string.image_downloading))
                        .setContentText(getString(R.string.downloading_msg)).setAutoCancel(true).setProgress(0, 0, true).setLargeIcon(icon)
                        .setSmallIcon(R.drawable.ic_launcher);
                manager.notify(notificationId, builder.build());

                if (FileUtil.savePhoto(photo, photoFile)) {
                    Log.v(TAG, "Image download completed");
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
                    viewIntent.setDataAndType(fileUri, photo.getType());
                    PendingIntent viewP = PendingIntent.getActivity(getApplicationContext(), 1, viewIntent, PendingIntent.FLAG_ONE_SHOT);

                    Bitmap bm = ImageUtil.toGrayscale(ImageUtil.decodeSampledBitmapFromResource(photoFile, 256, 256));
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
                    Log.w(TAG, "Image download failed");
                    builder.setProgress(0, 0, false).setContentTitle(getString(R.string.error)).setContentText(getString(R.string.download_error));
                    manager.notify(notificationId, builder.build());
                }
            } else {
                Log.w(TAG, "No photo passed in Intent");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
