package com.kenny.openimgur;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by kcampagna on 6/22/14.
 */
public class ViewPhotoActivity extends BaseActivity {

    private static final String KEY_URL = "url";

    private static final String KEY_IMAGE = "image";

    private static final String KEY_IS_VIDEO = "is_video";

    private ImageView mImageView;

    private MultiStateView mMultiView;

    private VideoView mVideoView;

    private ImgurPhoto photo;

    private String mUrl;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, ViewPhotoActivity.class).putExtra(KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url, boolean isVideo) {
        return new Intent(context, ViewPhotoActivity.class).putExtra(KEY_URL, url).putExtra(KEY_IS_VIDEO, isVideo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setShouldTint(false);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent == null || (!intent.hasExtra(KEY_IMAGE) && !intent.hasExtra(KEY_URL))) {
            Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.image_popup_fragment);
        photo = getIntent().getParcelableExtra(KEY_IMAGE);
        mUrl = intent.getStringExtra(KEY_URL);
        mMultiView = (MultiStateView) findViewById(R.id.multiView);
        mImageView = (ImageView) mMultiView.getView(MultiStateView.ViewState.CONTENT).findViewById(R.id.image);
        mVideoView = (VideoView) mMultiView.getView(MultiStateView.ViewState.CONTENT).findViewById(R.id.video);
        boolean isVideo = intent.getBooleanExtra(KEY_IS_VIDEO, false);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (isVideo) {
            displayVideo();
        } else {
            if (photo != null) {
                if (photo.isAnimated()) {
                    if (photo.isLinkAThumbnail() || photo.getSize() > (1024 * 1024 * 5)) {
                        mUrl = photo.getMP4Link();
                        displayVideo();
                        return;
                    } else {
                        mUrl = photo.getLink();
                    }
                } else {
                    mUrl = photo.getLink();
                    displayImage();
                }
            }

            displayImage();
        }
    }

    /**
     * Displays the image
     */
    private void displayImage() {
        app.getImageLoader().displayImage(mUrl, mImageView, ImageUtil.getDisplayOptionsForView().build(), new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {
            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                finish();
                Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                PhotoViewAttacher photoView = new PhotoViewAttacher(mImageView);
                photoView.setMaximumScale(10f);

                if ((photo != null && photo.isAnimated()) || (!TextUtils.isEmpty(mUrl) && mUrl.endsWith(".gif"))) {
                    // The file SHOULD be in our cache if the image has successfully loaded
                    if (!ImageUtil.loadAndDisplayGif(mImageView, photo != null ? photo.getLink() : mUrl, app.getImageLoader())) {
                        finish();
                        Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                photoView.update();
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
            }
        });
    }

    private void displayVideo() {
        File file = VideoCache.getInstance().getVideoFile(mUrl);

        if (FileUtil.isFileValid(file)) {
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            mVideoView.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                }
            });

            mVideoView.setVideoPath(file.getAbsolutePath());
            // mVideoView.setMediaController(new MediaController(this));
            mVideoView.start();
        } else {
            // Should never happen
            VideoCache.getInstance().putVideo(mUrl, new VideoCache.VideoCacheListener() {
                @Override
                public void onVideoDownloadStart(String key, String url) {

                }

                @Override
                public void onVideoDownloadFailed(Exception ex, String url) {
                    finish();
                    Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onVideoDownloadComplete(File file) {
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    mVideoView.setVisibility(View.VISIBLE);
                    mImageView.setVisibility(View.GONE);

                    mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mediaPlayer) {
                            mediaPlayer.setLooping(true);
                        }
                    });

                    mVideoView.setVideoPath(file.getAbsolutePath());
                    // mVideoView.setMediaController(new MediaController(ViewPhotoActivity.this));
                    mVideoView.start();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                startService(DownloaderService.createIntent(getApplicationContext(), photo));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (photo == null) {
            menu.removeItem(R.id.download);
        }

        return super.onPrepareOptionsMenu(menu);
    }
}
