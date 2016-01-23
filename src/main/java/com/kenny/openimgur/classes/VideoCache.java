package com.kenny.openimgur.classes;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by kcampagna on 10/9/14.
 */
public class VideoCache {
    private static final String TAG = "VideoCache";

    private static VideoCache mInstance;

    private File mCacheDir;

    private Md5FileNameGenerator mKeyGenerator;

    public static VideoCache getInstance() {
        if (mInstance == null) {
            mInstance = new VideoCache();
        }

        return mInstance;
    }

    private VideoCache() {
        OpengurApp app = OpengurApp.getInstance();
        String cacheKey = app.getPreferences().getString(SettingsActivity.KEY_CACHE_LOC, SettingsActivity.CACHE_LOC_INTERNAL);
        File dir = ImageUtil.getCacheDirectory(app.getApplicationContext(), cacheKey);
        mCacheDir = new File(dir, "video_cache");
        mCacheDir.mkdirs();
        mKeyGenerator = new Md5FileNameGenerator();
    }

    public void setCacheDirectory(File dir) {
        mCacheDir = new File(dir, "video_cache");
        mCacheDir.mkdirs();
    }

    /**
     * Downloads and saves the video file to the cache
     *
     * @param url      The url of the video
     * @param listener Optional VideoCacheListener
     */
    public void putVideo(@Nullable String url, @Nullable VideoCacheListener listener) {
        if (TextUtils.isEmpty(url)) {
            Exception e = new IllegalArgumentException("Url is null");
            LogUtil.e(TAG, "Invalid url", e);
            if (listener != null) listener.onVideoDownloadFailed(e, url);
            return;
        }

        url = getMP4Url(url);
        String key = mKeyGenerator.generate(url);
        File file = getVideoFile(key);

        if (FileUtil.isFileValid(file)) {
            LogUtil.v(TAG, "File already exists, deleting existing file and replacing it");
            file.delete();
        }

        try {
            if (file == null) {
                file = new File(mCacheDir, key + ".mp4");
            }

            file.createNewFile();
        } catch (IOException e) {
            LogUtil.e(TAG, "Error creating file", e);
            if (listener != null) listener.onVideoDownloadFailed(e, url);
        }

        if (FileUtil.isFileValid(file)) {
            new DownloadVideo(key, url, listener).execute(file);
        } else if (listener != null) {
            Exception e = new FileNotFoundException("Unable to create file for download");
            LogUtil.e(TAG, "Error creating file", e);
            listener.onVideoDownloadFailed(e, url);
        }
    }

    /**
     * Returns the cached video file for the given url. NULL if it does not exist
     *
     * @param url
     * @return
     */
    public File getVideoFile(String url) {
        if (TextUtils.isEmpty(url)) return null;


        String key = mKeyGenerator.generate(getMP4Url(url));
        File file = new File(mCacheDir, key + ".mp4");
        return FileUtil.isFileValid(file) ? file : null;
    }

    public void deleteCache() {
        FileUtil.deleteDirectory(mCacheDir);
        mCacheDir.mkdirs();
    }

    public long getCacheSize() {
        return FileUtil.getDirectorySize(mCacheDir);
    }

    public interface VideoCacheListener {
        // Called when the Video download starts
        void onVideoDownloadStart(String key, String url);

        // Called when the video download fails
        void onVideoDownloadFailed(Exception ex, String url);

        // Called when the video download completes
        void onVideoDownloadComplete(File file);
    }

    private static class DownloadVideo extends AsyncTask<File, Void, Object> {
        private String mKey;

        private VideoCacheListener mListener;

        private String mUrl;

        public DownloadVideo(String key, String url, VideoCacheListener listener) {
            this.mKey = key;
            this.mListener = listener;
            this.mUrl = url;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mListener != null) mListener.onVideoDownloadStart(mKey, mUrl);
        }

        @Override
        protected Object doInBackground(File... file) {
            InputStream in = null;
            BufferedOutputStream buffer = null;
            LogUtil.v(TAG, "Downloading video from " + mUrl);
            File writeFile = null;

            try {
                in = new URL(mUrl).openStream();
                writeFile = file[0];
                buffer = new BufferedOutputStream(new FileOutputStream(writeFile));
                byte byt[] = new byte[1024];
                int i;

                for (long l = 0L; (i = in.read(byt)) != -1; l += i) {
                    buffer.write(byt, 0, i);
                }

                buffer.flush();
                return writeFile;
            } catch (Exception e) {
                LogUtil.e(TAG, "An error occurred whiling downloading video", e);
                // Delete the file if an exception occurs to allow download retries
                if (writeFile != null) writeFile.delete();
                return e;
            } finally {
                FileUtil.closeStream(in);
                FileUtil.closeStream(buffer);
            }
        }

        @Override
        protected void onPostExecute(Object o) {
            if (o instanceof File) {
                LogUtil.v(TAG, "Video downloaded successfully to " + ((File) o).getAbsolutePath());
                if (mListener != null) {
                    mListener.onVideoDownloadComplete((File) o);
                }
            } else if (mListener != null) {
                mListener.onVideoDownloadFailed((Exception) o, mUrl);
            }
        }
    }

    /**
     * Removes either a .gifv or .webm extension in favor for .mp4
     *
     * @param url
     * @return
     */
    @NonNull
    private String getMP4Url(@NonNull String url) {
        if (url.endsWith(".gifv")) {
            return url.replace(".gifv", ".mp4");
        } else if (url.endsWith(".webm")) {
            return url.replace(".webm", ".mp4");
        }

        return url;
    }
}
