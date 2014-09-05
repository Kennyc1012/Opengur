package com.kenny.openimgur.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiscCache;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LargestLimitedMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 7/1/14.
 */
public class ImageUtil {
    private static final String TAG = "ImageUtil";

    // 8MB
    private static final int MEMORY_CACHE_LIMIT = 8388608;

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
                LogUtil.e(TAG, "Unable to play gif", e);
            }
        } else {
            LogUtil.w(TAG, "Gif file is invalid");
        }

        return false;
    }

    /**
     * Initializes the ImageLoader
     */
    public static void initImageLoader(Context context, long cache) {
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPoolSize(7)
                .denyCacheImageMultipleSizesInMemory()
                .diskCache(new LruDiscCache(context.getCacheDir(), new HashCodeFileNameGenerator(), cache))
                .defaultDisplayImageOptions(getDefaultDisplayOptions().build())
                .memoryCache(new LargestLimitedMemoryCache(MEMORY_CACHE_LIMIT))
                .build();

        ImageLoader.getInstance().init(config);
    }

    /**
     * Returns the display options for the image loader when loading for the gallery
     *
     * @return
     */
    public static DisplayImageOptions.Builder getDisplayOptionsForGallery() {
        return getDefaultDisplayOptions()
                .displayer(new FadeInBitmapDisplayer(750, true, false, false));
    }

    /**
     * Returns the display options for viewing an image in the view activity
     *
     * @return
     */
    public static DisplayImageOptions.Builder getDisplayOptionsForView() {
        return getDefaultDisplayOptions().showImageOnLoading(R.drawable.place_holder);
    }

    /**
     * Returns the default display options for the image loader
     *
     * @return
     */
    public static DisplayImageOptions.Builder getDefaultDisplayOptions() {
        return new DisplayImageOptions.Builder()
                .resetViewBeforeLoading(true)
                .cacheInMemory(true)
                .cacheOnDisk(true);
    }

    /**
     * Saves a bitmap to a local file
     *
     * @param bitmap The bitmap to save
     * @param file   The file to save the bitmap to
     * @return If successful
     */
    public static boolean saveBitmapToFile(Bitmap bitmap, File file) {
        if (!FileUtil.isFileValid(file)) {
            LogUtil.w(TAG, "Invalid file, can not save bitmap");
            return false;
        }

        try {
            FileOutputStream fileStream = new FileOutputStream(file);
            // Compress the file slightly
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fileStream);
            FileUtil.closeStream(fileStream);
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "Unable to save bitmap to file", e);
        }

        return false;
    }

    /**
     * Returns the images rotation from it's EXIF data
     *
     * @param file Image file
     * @return EXIF rotation, Undefined if no orientation was obtained
     */
    public static int getImageRotation(File file) {
        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        } catch (Exception e) {
            LogUtil.e(TAG, "Unable to get EXIF data from file", e);
        }

        return ExifInterface.ORIENTATION_UNDEFINED;
    }
}
