package com.kenny.openimgur.classes;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;

import java.io.BufferedOutputStream;
import java.io.File;
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
        mCacheDir = new File(OpenImgurApp.getInstance().getApplicationContext().getFilesDir(), "video_cache");
        mCacheDir.mkdirs();
        mKeyGenerator = new Md5FileNameGenerator();
    }

    /**
     * Downloads and saves the video file to the cache
     *
     * @param url      The url of the video
     * @param listener Optional VideoCacheListener
     */
    public void putVideo(String url, @Nullable VideoCacheListener listener) {
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
        }
    }

    /**
     * Returns the cached video file for the given url. NULL if it does not exist
     *
     * @param url
     * @return
     */
    public File getVideoFile(String url) {
        String key = mKeyGenerator.generate(url);
        File file = new File(mCacheDir, key + ".mp4");
        return FileUtil.isFileValid(file) ? file : null;
    }

    /**
     * Returns the size in bytes of the video cache
     *
     * @return
     */
    public long getCacheSize() {
        long size = 0;

        if (FileUtil.isFileValid(mCacheDir) && mCacheDir.isDirectory()) {
            for (File f : mCacheDir.listFiles()) {
                size += f.length();
            }
        }

        return size;
    }

    /**
     * Clears the Video Cache
     */
    public void deleteCache() {
        try {
            if (mCacheDir != null) {
                for (File f : mCacheDir.listFiles()) {
                    f.delete();
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "An error occurred whiling clearing the video cache", e);
        }
    }

    public static interface VideoCacheListener {
        void onVideoDownloadStart(String key, String url);

        void onVideoDownloadFailed(Exception ex, String url);

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
            LogUtil.v(TAG, "Downlong video from " + mUrl);

            try {
                in = new URL(mUrl).openStream();
                File writeFile = file[0];
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
}
