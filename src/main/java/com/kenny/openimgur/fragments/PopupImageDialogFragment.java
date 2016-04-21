package com.kenny.openimgur.fragments;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.FullScreenPhotoActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.PhotoResponse;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kennyc.view.MultiStateView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 7/19/14.
 */
public class PopupImageDialogFragment extends DialogFragment implements VideoCache.VideoCacheListener {
    private static final long PHOTO_SIZE_LIMIT = 1024 * 1024 * 5;

    private static final String KEY_URL = "url";

    private static final String KEY_ANIMATED = "animated";

    private static final String KEY_DIRECT_LINK = "direct_link";

    private static final String KEY_IS_VIDEO = "video";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.image)
    ImageView mImage;

    @Bind(R.id.video)
    VideoView mVideo;

    private String mImageUrl;

    public static PopupImageDialogFragment getInstance(String url, boolean isAnimated, boolean isDirectLink, boolean isVideo) {
        PopupImageDialogFragment fragment = new PopupImageDialogFragment();
        Bundle args = new Bundle(4);
        args.putString(KEY_URL, url);
        args.putBoolean(KEY_ANIMATED, isAnimated);
        args.putBoolean(KEY_DIRECT_LINK, isDirectLink);
        args.putBoolean(KEY_IS_VIDEO, isVideo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NO_TITLE, OpengurApp.getInstance(getActivity()).getImgurTheme().getDialogTheme());
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.image_popup_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle == null || !bundle.containsKey(KEY_URL)) {
            dismiss();
            return;
        }

        ButterKnife.bind(this, view);
        mImageUrl = bundle.getString(KEY_URL, null);
        boolean isAnimated = bundle.getBoolean(KEY_ANIMATED, false);
        boolean isDirectLink = bundle.getBoolean(KEY_DIRECT_LINK, true);
        boolean isVideo = bundle.getBoolean(KEY_IS_VIDEO, false);

        if (isDirectLink) {
            if (isVideo) {
                displayVideo(mImageUrl);
            } else {
                displayImage(mImageUrl, isAnimated);
            }
        } else {
            fetchImageDetails();
        }

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAllowingStateLoss();
                startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mImageUrl));
            }
        });

        mVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                dismissAllowingStateLoss();
                mVideo.stopPlayback();
                startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mImageUrl));
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideo != null && mVideo.getDuration() > 0) {
            mVideo.start();
        }
    }

    @Override
    public void onPause() {
        if (mVideo != null && mVideo.isPlaying()) {
            mVideo.stopPlayback();
        }

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        OpengurApp.getInstance(getActivity()).getImageLoader().cancelDisplayTask(mImage);
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    /**
     * Loads the image into the ImageView
     *
     * @param url
     * @param isAnimated
     */
    public void displayImage(String url, final boolean isAnimated) {
        OpengurApp.getInstance(getActivity()).getImageLoader().displayImage(url, mImage, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {

            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                if (isAdded()) {
                    dismissAllowingStateLoss();
                    Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                if (isAdded()) {
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    if (isAnimated) {
                        if (!ImageUtil.loadAndDisplayGif((ImageView) view, s, OpengurApp.getInstance(getActivity()).getImageLoader())) {
                            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                            dismissAllowingStateLoss();
                        }
                    }
                }
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
                if (isAdded()) {
                    dismissAllowingStateLoss();
                    Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Loads the video to be played
     *
     * @param url
     */
    public void displayVideo(String url) {
        File file = VideoCache.getInstance().getVideoFile(url);

        if (FileUtil.isFileValid(file)) {
            displayVideo(file);
        } else {
            VideoCache.getInstance().putVideo(url, this);
        }
    }

    /**
     * Displays the video once the file is loaded
     *
     * @param file
     */
    public void displayVideo(File file) {
        // The visibility needs to be set before the video is loaded or it won't play
        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        mVideo.setVisibility(View.VISIBLE);
        // Needs to be set so the video is not dimmed
        mVideo.setZOrderOnTop(true);
        mImage.setVisibility(View.GONE);

        mVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
            }
        });

        mVideo.setVideoPath(file.getAbsolutePath());
        mVideo.start();
    }

    @Override
    public void onVideoDownloadStart(String key, String url) {
        // NOOP
    }

    @Override
    public void onVideoDownloadFailed(Exception ex, String url) {
        if (isAdded() && isResumed() && getActivity() != null) {
            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
        }
    }

    @Override
    public void onVideoDownloadComplete(File file) {
        if (isAdded() && isResumed()) {
            displayVideo(file);
        }
    }

    private void fetchImageDetails() {
        ApiClient.getService().getImageDetails(mImageUrl).enqueue(new Callback<PhotoResponse>() {
            @Override
            public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().data != null) {
                    ImgurPhoto photo = response.body().data;

                    if (photo.isAnimated()) {
                        if (photo.isLinkAThumbnail() || photo.getSize() > PHOTO_SIZE_LIMIT) {
                            mImageUrl = photo.getVideoLink();
                            displayVideo(mImageUrl);
                        } else {
                            mImageUrl = photo.getLink();
                            displayImage(mImageUrl, photo.isAnimated());
                        }
                    } else {
                        mImageUrl = photo.getLink();
                        displayImage(mImageUrl, photo.isAnimated());
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();
            }
        });
    }
}
