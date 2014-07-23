package com.kenny.openimgur.util;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.kenny.openimgur.classes.ImgurPhoto;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by kcampagna on 6/22/14.
 */
public class FileUtil {

    /**
     * Saves a photo to the given file. If the file currently exists, it will be deleted
     *
     * @param photo The object to save
     * @param file  the file to save to
     * @return if successful
     */
    public static boolean savePhoto(@NonNull ImgurPhoto photo, @NonNull File file) {
        if (TextUtils.isEmpty(photo.getLink())) {
            return false;
        }

        if (file.exists()) {
            file.delete();
        }

        InputStream in = null;
        BufferedOutputStream buffer = null;
        boolean didFinish = false;

        try {
            buffer = new BufferedOutputStream(new FileOutputStream(file));
            in = new URL(photo.getLink()).openStream();
            byte byt[] = new byte[1024];
            int i;

            for (long l = 0L; (i = in.read(byt)) != -1; l += i) {
                buffer.write(byt, 0, i);
            }

            didFinish = true;
        } catch (IOException e) {
            e.printStackTrace();
            didFinish = false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (buffer != null) {
                    buffer.flush();
                    buffer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return didFinish;
    }

    /**
     * Returns a human readble file size
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
    public static long getDirectorySize(@NonNull File directory) {
        if (!directory.isDirectory()) {
            return 0;
        }

        long size = 0;
        for (File file : directory.listFiles()) {
            size += file.length();
        }

        return size;
    }
}
