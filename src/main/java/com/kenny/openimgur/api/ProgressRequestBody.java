package com.kenny.openimgur.api;

import com.kenny.openimgur.util.FileUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.RequestBody;

import java.io.File;
import java.io.IOException;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * Created by Kenny-PC on 3/22/2015.
 * RequestBody that can notify an uploads progress
 */
public class ProgressRequestBody extends RequestBody {
    private static final int SEGMENT_SIZE = 2048;

    private File mFile;

    private MediaType mType;

    private ProgressListener mListener;

    private int mPercentage = -1;

    public ProgressRequestBody(File file, MediaType type, ProgressListener listener) {
        mFile = file;
        mType = type;
        mListener = listener;
    }

    public interface ProgressListener {
        void onTransferred(int percentage);
    }

    @Override
    public MediaType contentType() {
        return mType;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        Source source = null;
        try {
            source = Okio.source(mFile);
            float total = 0;
            float read;

            while ((read = source.read(sink.buffer(), SEGMENT_SIZE)) != -1) {
                total += read;
                sink.flush();
                float percentage = total / contentLength() * 100;

                if ((int) percentage > mPercentage) {
                    mPercentage = (int) percentage;
                    if (mListener != null) mListener.onTransferred(mPercentage);
                }
            }
        } finally {
            FileUtil.closeStream(source);
        }
    }

    @Override
    public long contentLength() throws IOException {
        return mFile.length();
    }
}
