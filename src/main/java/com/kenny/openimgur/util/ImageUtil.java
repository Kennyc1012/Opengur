package com.kenny.openimgur.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 7/1/14.
 */
public class ImageUtil {

    /**
     * Converts a bitmap to grayscale
     *
     * @param bmpOriginal
     * @return
     */
    public static Bitmap toGrayScale(Bitmap bmpOriginal) {
        if (bmpOriginal == null) {
            return null;
        }
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        // RGB_565 uses half the amount of pixels than ARGB_8888. Since this will be used for a notification,
        // and is just gray we'll use RGB_565 to save on some memory
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Bitmap decodeSampledBitmapFromResource(File file, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    /**
     * Loads a gif into an image view. The gif must have been saved to the disk cache before calling this method or it will fail
     *
     * @param imageView   The ImageView where the gif will be displayed
     * @param url         The url of the image. This is the key for the cached image
     * @param imageLoader The Imageloader where we will retreive the image from
     * @return if successful
     */
    public static boolean loadAndDisplayGif(@NonNull ImageView imageView, @NonNull String url, @NonNull ImageLoader imageLoader) {
        File file = DiskCacheUtils.findInCache(url, imageLoader.getDiskCache());

        if (FileUtil.isFileValid(file)) {
            try {
                imageView.setImageDrawable(new GifDrawable(file));
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
