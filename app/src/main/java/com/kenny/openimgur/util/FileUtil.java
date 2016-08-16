package com.kenny.openimgur.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.classes.ImgurPhoto;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by kcampagna on 6/22/14.
 */
public class FileUtil {
    private static final String TAG = "FileUtil";

    public static final String EXTENSION_JPEG = ".jpeg";

    public static final String EXTENSION_PNG = ".png";

    public static final String EXTENSION_GIF = ".gif";

    public static final String EXTENSION_MP4 = ".mp4";

    private static final String FOLDER_NAME = "Opengur";

    /**
     * Saves a url to a given file
     *
     * @param url  The url to save
     * @param file The file to save to
     * @return If successful
     */
    public static boolean saveUrl(String url, @NonNull File file) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }

        if (file.exists()) {
            file.delete();
        }

        InputStream in;

        try {
            in = new URL(url).openStream();
        } catch (IOException e) {
            LogUtil.e(TAG, "Unable to open stream from url", e);
            return false;
        }

        return writeInputStreamToFile(in, file);
    }

    /**
     * Writes an input stream to a file. The input stream will be closed.
     *
     * @param in
     * @param file
     * @return
     */
    public static boolean writeInputStreamToFile(@NonNull InputStream in, @NonNull File file) {
        BufferedOutputStream buffer = null;
        boolean didFinish = false;

        try {
            buffer = new BufferedOutputStream(new FileOutputStream(file));
            byte byt[] = new byte[1024];
            int i;

            for (long l = 0L; (i = in.read(byt)) != -1; l += i) {
                buffer.write(byt, 0, i);
            }

            buffer.flush();
            didFinish = true;
        } catch (IOException e) {
            LogUtil.e(TAG, "Error saving photo", e);
            didFinish = false;
        } finally {
            closeStream(in);
            closeStream(buffer);
        }

        return didFinish;
    }

    /**
     * Returns a human readable file size
     *
     * @param bytes
     * @param si
     * @return
     */
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Returns the file size of the directory
     *
     * @param directory
     * @return
     */
    public static long getDirectorySize(File directory) {
        if (!isFileValid(directory) || !directory.isDirectory()) {
            return 0;
        }

        long size = 0;
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) size += file.length();
            }
        }

        return size;
    }

    /**
     * Creates a new file
     *
     * @param extension The extension of the file
     * @return
     */
    public static File createFile(String extension) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER_NAME);
        dir.mkdirs();
        File file = new File(dir.getAbsolutePath(), timeStamp + extension);

        try {
            if (file.createNewFile()) {
                return file;
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Error creating file", e);
        }

        return null;

    }

    /**
     * Takes a Uri and saves it to a file
     *
     * @param uri
     * @param context
     * @return
     */
    public static File createFile(Uri uri, @NonNull Context context) {
        InputStream in;
        ContentResolver resolver = context.getContentResolver();
        String type = resolver.getType(uri);
        String extension;

        if (ImgurPhoto.IMAGE_TYPE_GIF.equals(type)) {
            extension = EXTENSION_GIF;
        } else if (ImgurPhoto.IMAGE_TYPE_PNG.equals(type)) {
            extension = EXTENSION_PNG;
        } else {
            extension = EXTENSION_JPEG;
        }

        try {
            in = resolver.openInputStream(uri);
        } catch (FileNotFoundException e) {
            LogUtil.e(TAG, "Unable to open input stream", e);
            return null;
        }

        // Create files from a uri in our cache directory so they eventually get deleted
        String timeStamp = String.valueOf(System.currentTimeMillis());
        File cacheDir = ImageUtil.getImageLoader(context).getDiskCache().getDirectory();
        File tempFile = new File(cacheDir, timeStamp + extension);

        if (writeInputStreamToFile(in, tempFile)) {
            return tempFile;
        } else {
            // If writeInputStreamToFile fails, delete the excess file
            tempFile.delete();
        }

        return null;
    }

    /**
     * Tells the Media Scanner that a new file is present
     *
     * @param file
     * @param context
     */
    public static void scanFile(Uri file, Context context) {
        Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        scan.setData(file);
        context.sendBroadcast(scan);
    }

    /**
     * Tells the Media Scanner to scan an entire directory if present
     *
     * @param directory
     * @param context
     */
    public static void scanDirectory(@Nullable File directory, Context context) {
        if (isFileValid(directory) && directory.isDirectory()) {
            File[] files = directory.listFiles();

            if (files != null) {
                for (File f : files) {
                    scanFile(Uri.fromFile(f), context);
                }
            }
        }
    }

    /**
     * Returns if the given file is not null and exists in the file system
     *
     * @param file
     * @return
     */
    public static boolean isFileValid(@Nullable File file) {
        return file != null && file.exists();
    }

    /**
     * Closes a stream of data
     *
     * @param closeable
     */
    public static void closeStream(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ex) {
                LogUtil.e(TAG, "Unable to close stream", ex);
            }
        }
    }

    /**
     * Copies one file to another
     *
     * @param sourceFile The file to copy
     * @param destFile   The destination file to copy to
     * @return If successful
     */
    public static boolean copyFile(File sourceFile, File destFile) {
        if (isFileValid(destFile)) {
            destFile.delete();
        }

        FileChannel source = null;
        FileChannel destination = null;
        boolean success = false;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
            success = true;
        } catch (Exception e) {
            LogUtil.e(TAG, "Error whiling copying file", e);
            success = false;
        } finally {
            closeStream(source);
            closeStream(destination);
        }

        return success;
    }

    /**
     * Deletes all the files in a given directory
     *
     * @param dir
     */
    public static void deleteDirectory(File dir) {
        if (isFileValid(dir) && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0)

                for (File f : files) {
                    f.delete();
                }
        }
    }
}
