package com.kenny.openimgur.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
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
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.services.DownloaderService;
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

/**
 * Created by kcampagna on 6/2/15.
 */
public class FullScreenPhotoFragment extends BaseFragment {
    private static final String KEY_IMGUR_OBJECT = "imgur_photo_object";

    private static final String KEY_VIDEO_POSITION = "position";

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
                displayImage();
            }
        } else {
            mUrl = mPhoto.getLink();
            displayImage();
        }
    }

    /**
     * Displays the image
     */
    private void displayImage() {
        app.getImageLoader().loadImage(mUrl, new ImageSize(1, 1), ImageUtil.getDisplayOptionsForView().build(), new SimpleImageLoadingListener() {
            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                mMultiView.setViewState(MultiStateView.ViewState.ERROR);
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                bitmap.recycle();

                if ((mPhoto != null && mPhoto.isAnimated()) || (!TextUtils.isEmpty(mUrl) && mUrl.endsWith(".gif"))) {
                    // Display our gif in a standard image view
                    if (!ImageUtil.loadAndDisplayGif(mGifImageView, mPhoto != null ? mPhoto.getLink() : mUrl, app.getImageLoader())) {
                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    } else {
                        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    }
                } else {
                    // Static images will use the TouchImageView to render the image. This allows large(tall) images to render better and be better legible
                    try {
                        File file = app.getImageLoader().getDiskCache().get(s);
                        if (FileUtil.isFileValid(file)) {
                            // We will enable tiling if any of the image dimensions are above 2048 px (Canvas draw limit)
                            int[] dimensions = ImageUtil.getBitmapDimensions(file);
                            boolean enableTiling = dimensions[0] > 2048 || dimensions[1] > 2048;
                            Uri fileUril = Uri.fromFile(file);

                            mImageView.setImage(ImageSource.uri(fileUril).tiling(enableTiling));
                            mImageView.setMaxScale(10.0f);
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
            mVideoView.start();
        } else {
            VideoCache.getInstance().putVideo(mUrl, new VideoCache.VideoCacheListener() {
                @Override
                public void onVideoDownloadStart(String key, String url) {

                }

                @Override
                public void onVideoDownloadFailed(Exception ex, String url) {
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.view_photo, menu);
        ShareActionProvider share = (ShareActionProvider) MenuItemCompat.getActionProvider(menu.findItem(R.id.share));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
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
        share.setShareIntent(shareIntent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                getActivity().startService(DownloaderService.createIntent(getActivity(), mUrl));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_IMGUR_OBJECT, mPhoto);
        if (mVideoView.isPlaying()) outState.putInt(KEY_VIDEO_POSITION, mVideoView.getCurrentPosition());
    }
}
