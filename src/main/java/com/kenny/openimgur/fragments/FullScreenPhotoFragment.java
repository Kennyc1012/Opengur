package com.kenny.openimgur.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.services.DownloaderService;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;

import butterknife.InjectView;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 6/2/15.
 */
public class FullScreenPhotoFragment extends BaseFragment {
    private static final String KEY_IMGUR_OBJECT = "imgur_photo_object";

    private static final String KEY_VIDEO_POSITION = "position";

    private static final long GIF_DELAY = 350L;

    @InjectView(R.id.image)
    SubsamplingScaleImageView mImageView;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.video)
    VideoView mVideoView;

    @InjectView(R.id.gifImage)
    ImageView mGifImageView;

    private ImgurPhoto mPhoto;

    private String mUrl;

    private boolean mStartedToLoad = false;

    private PhotoHandler mHandler = new PhotoHandler();

    public static FullScreenPhotoFragment createInstance(@NonNull ImgurPhoto photo) {
        FullScreenPhotoFragment fragment = new FullScreenPhotoFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(KEY_IMGUR_OBJECT, photo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_full_screen, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView.setErrorText(R.id.errorMessage, R.string.error_generic);
        ((Button) mMultiView.getView(MultiStateView.ViewState.ERROR).findViewById(R.id.errorButton)).setText(null);

        if (savedInstanceState != null) {
            mPhoto = savedInstanceState.getParcelable(KEY_IMGUR_OBJECT);
        } else if (getArguments() != null) {
            mPhoto = getArguments().getParcelable(KEY_IMGUR_OBJECT);
        }

        if (mPhoto == null) {
            mMultiView.setViewState(MultiStateView.ViewState.ERROR);
        } else {
            configView(savedInstanceState);
        }
    }

    private void configView(Bundle savedInstanceState) {
        if (mPhoto.isAnimated()) {
            if (mPhoto.isLinkAThumbnail() || mPhoto.getSize() > (1024 * 1024 * 5)) {
                mUrl = mPhoto.getMP4Link();
                displayVideo(savedInstanceState);
            } else {
                mUrl = mPhoto.getLink();
                if (LinkUtils.isVideoLink(mUrl)) {
                    displayVideo(savedInstanceState);
                } else {
                    displayImage();
                }
            }
        } else {
            mUrl = mPhoto.getLink();
            displayImage();
        }
    }

    @Override
    public void onDestroyView() {
        mHandler.removeMessages(0);
        mHandler = null;

        // Free up some memory
        if (mGifImageView.getDrawable() instanceof GifDrawable) {
            ((GifDrawable) mGifImageView.getDrawable()).recycle();
        } else if (mVideoView.getDuration() > 0) {
            mVideoView.suspend();
        } else {
            mImageView.recycle();
        }

        super.onDestroyView();
    }

    /**
     * Displays the image
     */
    private void displayImage() {
        app.getImageLoader().loadImage(mUrl, new ImageSize(1, 1), ImageUtil.getDisplayOptionsForFullscreen().build(), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                mStartedToLoad = false;
                if (getActivity() == null | !isAdded() || isRemoving()) return;

                mMultiView.setViewState(MultiStateView.ViewState.ERROR);
            }

            @Override
            public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                mStartedToLoad = false;
                bitmap.recycle();
                if (getActivity() == null | !isAdded() || isRemoving()) return;

                if (url.endsWith(".gif")) {
                    displayGif(url);
                } else {
                    // Static images will use the TouchImageView to render the image. This allows large(tall) images to render better and be better legible
                    try {
                        File file = app.getImageLoader().getDiskCache().get(url);
                        if (FileUtil.isFileValid(file)) {
                            // We will enable tiling if any of the image dimensions are above 2048 px (Canvas draw limit)
                            int[] dimensions = ImageUtil.getBitmapDimensions(file);
                            boolean enableTiling = dimensions[0] > 2048 || dimensions[1] > 2048;
                            LogUtil.v(TAG, "Tiling enabled for image " + enableTiling);
                            Uri fileUri = Uri.fromFile(file);

                            mImageView.setOnImageEventListener(new SubsamplingScaleImageView.OnImageEventListener() {
                                @Override
                                public void onReady() {

                                }

                                @Override
                                public void onImageLoaded() {

                                }

                                @Override
                                public void onPreviewLoadError(Exception e) {

                                }

                                @Override
                                public void onImageLoadError(Exception e) {
                                    LogUtil.e(TAG, "Error loading image", e);
                                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                                }

                                @Override
                                public void onTileLoadError(Exception e) {
                                    LogUtil.e(TAG, "Error creating tile", e);
                                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                                }
                            });

                            mImageView.setMinimumTileDpi(160);
                            mImageView.setImage(ImageSource.uri(fileUri).dimensions(dimensions[0], dimensions[1]).tiling(enableTiling));
                            mVideoView.setVisibility(View.GONE);
                            mGifImageView.setVisibility(View.GONE);
                            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                        } else {
                            mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                        }
                    } catch (Exception e) {
                        LogUtil.e(TAG, "Error creating tile bitmap", e);
                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    }
                }
            }

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                super.onLoadingStarted(imageUri, view);
                mStartedToLoad = true;
            }
        });
    }

    private void displayVideo(Bundle savedInstance) {
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
            if (getUserVisibleHint()) mVideoView.start();
        } else {
            VideoCache.getInstance().putVideo(mUrl, new VideoCache.VideoCacheListener() {
                @Override
                public void onVideoDownloadStart(String key, String url) {

                }

                @Override
                public void onVideoDownloadFailed(Exception ex, String url) {
                    if (getActivity() == null | !isAdded() || isRemoving()) return;

                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                }

                @Override
                public void onVideoDownloadComplete(File file) {
                    if (getActivity() == null | !isAdded() || isRemoving()) return;

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
                    if (getUserVisibleHint()) mVideoView.start();
                }
            });
        }
    }

    private void displayGif(String url) {
        // Display our gif in a standard image view

        if (getUserVisibleHint()) {
            // Auto play the gif if we are visible
            File file = app.getImageLoader().getDiskCache().get(url);
            if (!ImageUtil.loadAndDisplayGif(mGifImageView, file)) {
                mMultiView.setViewState(MultiStateView.ViewState.ERROR);
            } else {
                mVideoView.setVisibility(View.GONE);
                mImageView.setVisibility(View.GONE);
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            }
        } else {
            mVideoView.setVisibility(View.GONE);
            mImageView.setVisibility(View.GONE);
            app.getImageLoader().displayImage(url, mGifImageView);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.view_photo, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                getActivity().startService(DownloaderService.createIntent(getActivity(), mUrl));
                return true;

            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String link;

                if (mPhoto != null && !TextUtils.isEmpty(mPhoto.getTitle())) {
                    link = mPhoto.getTitle() + " ";
                    if (TextUtils.isEmpty(mPhoto.getRedditLink())) {
                        link += mPhoto.getGalleryLink();
                    } else {
                        link += String.format("https://reddit.com%s", mPhoto.getRedditLink());
                    }
                } else {
                    link = mUrl;
                }

                shareIntent.putExtra(Intent.EXTRA_TEXT, link);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_IMGUR_OBJECT, mPhoto);
        if (mVideoView.isPlaying())
            outState.putInt(KEY_VIDEO_POSITION, mVideoView.getCurrentPosition());
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            if (mGifImageView != null && mGifImageView.getDrawable() instanceof GifDrawable) {
                ((GifDrawable) mGifImageView.getDrawable()).start();
            } else if (mVideoView != null && mVideoView.getDuration() > 0) {
                mVideoView.start();
            } else {
                mHandler.sendEmptyMessageDelayed(0, GIF_DELAY);
            }
        } else {
            if (mGifImageView != null && mGifImageView.getDrawable() instanceof GifDrawable) {
                ((GifDrawable) mGifImageView.getDrawable()).pause();
            } else if (mVideoView != null && mVideoView.getDuration() > 0) {
                mVideoView.pause();
            }
        }
    }

    private class PhotoHandler extends ImgurHandler {
        @Override
        public void handleMessage(Message msg) {
            if (getUserVisibleHint() && !mStartedToLoad && mGifImageView != null && LinkUtils.isLinkAnimated(mUrl) && !LinkUtils.isVideoLink(mUrl)) {
                displayGif(mUrl);
            }

            super.handleMessage(msg);
        }
    }
}
