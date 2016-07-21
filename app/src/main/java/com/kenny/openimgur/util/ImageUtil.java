package com.kenny.openimgur.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ImageView;

import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.CircleBitmapDisplayer;
import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.impl.ext.LruDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
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

    private static ImageLoader imageLoader;

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
            if (width > height) {
                inSampleSize = Math.round((float) height / (float) reqHeight);
            } else {
                inSampleSize = Math.round((float) width / (float) reqWidth);
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
     * @param imageLoader The Imageloader where we will retrieve the image from
     * @return if successful
     */
    public static boolean loadAndDisplayGif(@Nullable ImageView imageView, @NonNull String url, @NonNull ImageLoader imageLoader) {
        File file = DiskCacheUtils.findInCache(url, imageLoader.getDiskCache());
        return loadAndDisplayGif(imageView, file);
    }

    /**
     * Loads a gif into an image view. The gif must have been saved to the disk cache before calling this method or it will fail
     *
     * @param imageView The ImageView where the gif will be displayed
     * @param file      File of the gif
     * @return
     */
    public static boolean loadAndDisplayGif(@Nullable ImageView imageView, @Nullable File file) {
        if (imageView == null) return false;

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

    public static ImageLoader getImageLoader(@NonNull Context context) {
        if (imageLoader == null || !imageLoader.isInited()) {
            initImageLoader(context.getApplicationContext());
            imageLoader = ImageLoader.getInstance();
        }

        return imageLoader;
    }

    /**
     * Initializes the ImageLoader
     *
     * @param context App context
     */
    public static void initImageLoader(Context context) {
        long discCacheSize = 1024 * 1024;
        DiskCache discCache;
        int threadPoolSize;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String discCacheAllowance = pref.getString(SettingsActivity.KEY_CACHE_SIZE, SettingsActivity.CACHE_SIZE_512MB);
        String threadSize = pref.getString(SettingsActivity.KEY_THREAD_SIZE, SettingsActivity.THREAD_SIZE_5);
        String cacheKey = pref.getString(SettingsActivity.KEY_CACHE_LOC, SettingsActivity.CACHE_LOC_INTERNAL);
        File baseDir = getCacheDirectory(context, cacheKey);
        checkForOldCache(pref, baseDir);
        File dir = new File(baseDir, "image_cache");


        switch (discCacheAllowance) {
            case SettingsActivity.CACHE_SIZE_256MB:
                discCacheSize *= 256;
                break;

            case SettingsActivity.CACHE_SIZE_1GB:
                discCacheSize *= 1024;
                break;

            case SettingsActivity.CACHE_SIZE_2GB:
                discCacheSize *= 2048;
                break;

            case SettingsActivity.CACHE_SIZE_UNLIMITED:
                discCacheSize = -1;
                break;

            case SettingsActivity.CACHE_SIZE_512MB:
            default:
                discCacheSize *= 512;
                break;
        }

        switch (threadSize) {
            case SettingsActivity.THREAD_SIZE_7:
                threadPoolSize = 7;
                break;

            case SettingsActivity.THREAD_SIZE_10:
                threadPoolSize = 10;
                break;

            case SettingsActivity.THREAD_SIZE_12:
                threadPoolSize = 12;
                break;

            case SettingsActivity.THREAD_SIZE_5:
            default:
                threadPoolSize = 5;
                break;
        }

        if (discCacheSize > 0) {
            try {
                discCache = new LruDiskCache(dir, new Md5FileNameGenerator(), discCacheSize);
                LogUtil.v(TAG, "Disc cache set to " + FileUtil.humanReadableByteCount(discCacheSize, false));
            } catch (IOException e) {
                LogUtil.e(TAG, "Unable to set the disc cache, falling back to unlimited", e);
                discCache = new UnlimitedDiskCache(dir);
            }
        } else {
            LogUtil.v(TAG, "Disc cache set to unlimited");
            discCache = new UnlimitedDiskCache(dir);
        }

        final int memory = (int) (Runtime.getRuntime().maxMemory() / 8);
        LogUtil.v(TAG, "Disc Cache located at " + discCache.getDirectory().getAbsolutePath());
        LogUtil.v(TAG, "Using " + FileUtil.humanReadableByteCount(memory, false) + "  for memory cache");
        LogUtil.v(TAG, "Using " + threadPoolSize + " threads for image loader");

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                .threadPoolSize(threadPoolSize)
                .denyCacheImageMultipleSizesInMemory()
                .diskCache(discCache)
                .defaultDisplayImageOptions(getDefaultDisplayOptions().build())
                .memoryCacheSize(memory)
                .build();

        if (ImageLoader.getInstance().isInited()) {
            ImageLoader.getInstance().destroy();
        }

        ImageLoader.getInstance().init(config);

        // Check our cache to see if we should delete it
        long lastClear = pref.getLong("lastClear", 0);

        // We will clear the cache every 3 days
        if (lastClear == 0) {
            pref.edit().putLong("lastClear", System.currentTimeMillis()).apply();
        } else {
            long currentTime = System.currentTimeMillis();

            if (currentTime - lastClear >= DateUtils.DAY_IN_MILLIS * 3) {
                LogUtil.v(TAG, "Cache older than 3 days, clearing");
                ImageLoader.getInstance().clearMemoryCache();
                ImageLoader.getInstance().clearDiskCache();
                VideoCache.getInstance().deleteCache();
                pref.edit().putLong("lastClear", currentTime).apply();
            }
        }
    }

    /**
     * Returns the display options for the image loader when loading for the gallery.
     * Fades in the images when loaded from the network. Also uses Bitmap.Config.RGB_565 for less memory usage
     *
     * @return
     */
    public static DisplayImageOptions.Builder getDisplayOptionsForGallery() {
        return getDefaultDisplayOptions()
                .displayer(new FadeInBitmapDisplayer(250, true, false, false))
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY);
    }

    /**
     * Returns the display options for viewing an image in the view activity
     * Uses a placeholder whiling loading images
     *
     * @return
     */
    public static DisplayImageOptions.Builder getDisplayOptionsForView() {
        return getDefaultDisplayOptions()
                .showImageOnLoading(new ColorDrawable(Color.TRANSPARENT));
    }

    public static DisplayImageOptions.Builder getDisplayOptionsForComments() {
        return getDefaultDisplayOptions()
                .displayer(new CircleBitmapDisplayer(OpengurApp.getInstance().getResources()));
    }

    public static DisplayImageOptions.Builder getDisplayOptionsForFullscreen() {
        return getDefaultDisplayOptions()
                .cacheInMemory(false);
    }

    public static DisplayImageOptions.Builder getDisplayOptionsForPhotoPicker() {
        return getDisplayOptionsForGallery()
                .cacheOnDisk(false)
                .considerExifParams(true)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED);
    }

    /**
     * Returns the default display options for the image loader
     * Resets view before loading, caches in memory and on disk.
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
     * Returns the images rotation from it's EXIF data
     *
     * @param file Image file
     * @return EXIF rotation, Undefined if no orientation was obtained
     */
    public static int getImageRotation(File file) {
        if (!FileUtil.isFileValid(file)) {
            return ExifInterface.ORIENTATION_UNDEFINED;
        }

        try {
            ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        } catch (Exception e) {
            LogUtil.e(TAG, "Unable to get EXIF data from file", e);
        }

        return ExifInterface.ORIENTATION_UNDEFINED;
    }

    /**
     * Returns the directory to be used for caching
     *
     * @param context
     * @param key
     * @return
     */
    public static File getCacheDirectory(Context context, String key) {
        if (SettingsActivity.CACHE_LOC_EXTERNAL.equals(key) &&
                Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return context.getExternalCacheDir();
        }

        return context.getCacheDir();
    }

    /**
     * Returns the thumbnail for the given image url
     *
     * @param url
     * @param thumbnailSize
     * @return
     */
    public static String getThumbnail(String url, String thumbnailSize) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(thumbnailSize)) {
            Log.w(TAG, "Url or thumbnailSize is empty");
            return null;
        }

        String[] fileExtension = url.split("^(.*[\\.])");
        String[] imageUrl = url.split("\\.\\w+$");

        if (fileExtension.length > 0 && imageUrl.length > 0) {
            return imageUrl[imageUrl.length - 1] + thumbnailSize + "." + fileExtension[fileExtension.length - 1];
        }

        return null;
    }

    /**
     * Returns a drawable that has been tinted
     *
     * @param drawableId
     * @param resources
     * @param color
     * @return
     */
    public static Drawable tintDrawable(@DrawableRes int drawableId, @NonNull Resources resources, int color) {
        Drawable drawable = ResourcesCompat.getDrawable(resources, drawableId, null).mutate();
        drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        return drawable;
    }

    /**
     * Saves a bitmap to a local file
     *
     * @param bitmap
     * @return
     */
    public static File saveBitmap(@NonNull Bitmap bitmap) {
        File file = FileUtil.createFile(".jpeg");
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (Exception e) {
            e.printStackTrace();
            file = null;
        } finally {
            FileUtil.closeStream(out);
        }

        return file;
    }

    /**
     * Decodes a file path to a bitmap and returns its width and height. This <b><i>WILL NOT</i></b> load the image int memory
     *
     * @param file The file path to decode
     * @return An int array containing the width and height of the bitmap
     */
    @NonNull
    public static int[] getBitmapDimensions(@NonNull File file) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return new int[]{options.outWidth, options.outHeight};
    }

    /**
     * Returns the entire size of the image cache, including videos
     *
     * @param context
     * @return
     */
    public static long getTotalImageCacheSize(@NonNull Context context) {
        long cacheSize = FileUtil.getDirectorySize(getImageLoader(context).getDiskCache().getDirectory());
        cacheSize += VideoCache.getInstance().getCacheSize();
        return cacheSize;
    }

    /**
     * Checks if the user is using the new cache directory of images. If they are not, the old one will be deleted for updating.
     * <p/>
     * TODO Delete the method after several versions
     *
     * @param preferences
     * @param cacheDir
     */
    private static void checkForOldCache(@NonNull SharedPreferences preferences, @NonNull File cacheDir) {
        if (!preferences.getBoolean("has_updated_cache", false)) {
            preferences.edit().putBoolean("has_updated_cache", true).apply();

            for (File f : cacheDir.listFiles()) {
                if (!f.isDirectory()) f.delete();
            }
        }
    }
}
