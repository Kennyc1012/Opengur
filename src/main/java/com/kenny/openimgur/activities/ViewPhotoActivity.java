package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.diegocarloslima.byakugallery.lib.TileBitmapDrawable;
import com.diegocarloslima.byakugallery.lib.TouchImageView;
import com.kenny.openimgur.DownloaderService;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;

import butterknife.InjectView;
import uk.co.senab.photoview.PhotoView;

/**
 * Created by kcampagna on 6/22/14.
 */
public class ViewPhotoActivity extends BaseActivity implements TileBitmapDrawable.OnInitializeListener {
    private static final String KEY_VIDEO_POSITION = "position";

    private static final String KEY_URL = "url";

    private static final String KEY_IMAGE = "image";

    private static final String KEY_IS_VIDEO = "is_video";

    @InjectView(R.id.image)
    TouchImageView mImageView;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.video)
    VideoView mVideoView;

    @InjectView(R.id.gifImage)
    PhotoView mGifImageView;

    @Nullable
    private ImgurPhoto photo;

    @Nullable
    private String mUrl;

    private boolean mIsVideo = false;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, ViewPhotoActivity.class).putExtra(KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url, boolean isVideo) {
        return new Intent(context, ViewPhotoActivity.class).putExtra(KEY_URL, url).putExtra(KEY_IS_VIDEO, isVideo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(Color.BLACK);
        Intent intent = getIntent();

        if (intent == null || (!intent.hasExtra(KEY_IMAGE) && !intent.hasExtra(KEY_URL))) {
            Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_view_image);
        photo = getIntent().getParcelableExtra(KEY_IMAGE);
        mUrl = intent.getStringExtra(KEY_URL);
        boolean isVideo = intent.getBooleanExtra(KEY_IS_VIDEO, false);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        if (isVideo) {
            displayVideo(savedInstanceState);
        } else {
            if (photo != null) {
                if (photo.isAnimated()) {
                    if (photo.isLinkAThumbnail() || photo.getSize() > (1024 * 1024 * 5)) {
                        mUrl = photo.getMP4Link();
                        displayVideo(savedInstanceState);
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
        mIsVideo = false;
        app.getImageLoader().loadImage(mUrl, new ImageSize(1, 1), ImageUtil.getDisplayOptionsForView().build(), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                finish();
                Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                bitmap.recycle();

                if ((photo != null && photo.isAnimated()) || (!TextUtils.isEmpty(mUrl) && mUrl.endsWith(".gif"))) {
                    // Display our gif in a standard image view
                    if (!ImageUtil.loadAndDisplayGif(mGifImageView, photo != null ? photo.getLink() : mUrl, app.getImageLoader())) {
                        finish();
                        Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                    } else {
                        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    }
                } else {
                    // Static images will use the TouchImageView to render the image. This allows large(tall) images to render better and be better legible
                    try {
                        File file = app.getImageLoader().getDiskCache().get(s);
                        if (FileUtil.isFileValid(file)) {
                            // Clear any memory cache to free up some resources
                            app.getImageLoader().clearMemoryCache();
                            TileBitmapDrawable.attachTileBitmapDrawable(mImageView, file.getAbsolutePath(), null, ViewPhotoActivity.this);
                            mImageView.setMaxScale(10.0f);
                            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                        } else {
                            finish();
                            Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, "Error creating tile bitmap", e);
                        onError(e);
                    }
                }
            }
        });
    }

    private void displayVideo(Bundle savedInstance) {
        mIsVideo = true;
        File file = VideoCache.getInstance().getVideoFile(mUrl);
        final int position;

        // Check if our video was playing during a rotation
        if (savedInstance != null) {
            position = savedInstance.getInt(KEY_VIDEO_POSITION, 0);
        } else {
            position = 0;
        }

        if (FileUtil.isFileValid(file)) {
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            mVideoView.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);

            mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.seekTo(position);
                }
            });

            mVideoView.setVideoPath(file.getAbsolutePath());
            mVideoView.start();
        } else {
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
                            mediaPlayer.seekTo(position);
                        }
                    });

                    mVideoView.setVideoPath(file.getAbsolutePath());
                    mVideoView.start();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_photo, menu);
        ShareActionProvider share = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.share));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
        String link;

        if (photo != null) {
            link = photo.getTitle() + " ";
            if (TextUtils.isEmpty(photo.getRedditLink())) {
                link += photo.getGalleryLink();
            } else {
                link += String.format("http://reddit.com%s", photo.getRedditLink());
            }
        } else {
            link = mUrl;
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, link);
        share.setShareIntent(shareIntent);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                if (photo != null) {
                    startService(DownloaderService.createIntent(getApplicationContext(), photo));
                } else {
                    startService(DownloaderService.createIntent(getApplicationContext(), mUrl));
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mIsVideo && mVideoView.isPlaying()) {
            outState.putInt(KEY_VIDEO_POSITION, mVideoView.getCurrentPosition());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStartInitialization() {
        LogUtil.v(TAG, "Decoding Image with BitmapRegionDecoder");
    }

    @Override
    public void onEndInitialization() {
        LogUtil.v(TAG, "Successfully decoded image with BitmapRegionDecoder");
    }

    @Override
    public void onError(Exception e) {
        LogUtil.e(TAG, "Error decoding image with BitmapRegionDecoder", e);
        // If the image fails to load from the BitmapRegionDecoder, use the default pinch to zoom
        String url = photo != null ? photo.getLink() : mUrl;

        if (!TextUtils.isEmpty(url)) {
            mGifImageView.setVisibility(View.VISIBLE);
            mVideoView.setVisibility(View.GONE);
            mImageView.setVisibility(View.GONE);
            app.getImageLoader().displayImage(url, mGifImageView);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        } else {
            finish();
            Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        TileBitmapDrawable.clearCache();
        super.onDestroy();
    }
}
