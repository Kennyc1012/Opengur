package com.kenny.openimgur.fragments;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
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
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 7/19/14.
 */
public class PopupImageDialogFragment extends DialogFragment implements VideoCache.VideoCacheListener {
    private static final long PHOTO_SIZE_LIMIT = 1024 * 1024 * 5;

    private static final String KEY_URL = "url";

    private static final String KEY_ANIMATED = "animated";

    private static final String KEY_DIRECT_LINK = "direct_link";

    private static final String KEY_IS_VIDEO = "video";

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.image)
    ImageView mImage;

    @InjectView(R.id.video)
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

        ButterKnife.inject(this, view);
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
            ApiClient api = new ApiClient(String.format(Endpoints.IMAGE_DETAILS.getUrl(), mImageUrl), ApiClient.HttpRequest.GET);
            api.doWork(ImgurBusEvent.EventType.ITEM_DETAILS, null, null);
        }

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
                startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mImageUrl));
            }
        });

        mVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                dismiss();
                mVideo.stopPlayback();
                startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mImageUrl));
                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        if (mVideo != null && mVideo.getDuration() > 0) {
            mVideo.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);

        if (mVideo != null && mVideo.isPlaying()) {
            mVideo.stopPlayback();
        }
    }

    @Override
    public void onDestroyView() {
        OpengurApp.getInstance(getActivity()).getImageLoader().cancelDisplayTask(mImage);
        mHandler.removeCallbacksAndMessages(null);
        ButterKnife.reset(this);
        super.onDestroyView();
    }

    /**
     * Event Method that receives events from the Bus
     *
     * @param event
     */
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
            if (statusCode == ApiClient.STATUS_OK && event.eventType == ImgurBusEvent.EventType.ITEM_DETAILS) {
                final ImgurPhoto photo = new ImgurPhoto(event.json.getJSONObject(ApiClient.KEY_DATA));
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, photo);
            } else if (event.eventType == ImgurBusEvent.EventType.ITEM_DETAILS && statusCode != ApiClient.STATUS_OK) {
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getString(R.string.loading_image_error));
            }
        } catch (JSONException e) {
            LogUtil.e("PopupImageDialogFramgent", "Unable to parse json result", e);
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getString(R.string.error_generic));
        }
    }

    /**
     * Event Method that is fired if EventBus throws an error
     *
     * @param event
     */
    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getString(ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION)));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getString(ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION)));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, getString(R.string.loading_image_error));
        }

        LogUtil.e("PopupImageDialogFragment", "Error received from Event Bus", e);
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
                    dismiss();
                    Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                if (isAdded()) {
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    if (isAnimated) {
                        if (!ImageUtil.loadAndDisplayGif((ImageView) view, s, OpengurApp.getInstance(getActivity()).getImageLoader())) {
                            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                            dismiss();
                        }
                    }
                }
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
                if (isAdded()) {
                    dismiss();
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
        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
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

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    ImgurPhoto photo = (ImgurPhoto) msg.obj;

                    if (photo.isAnimated()) {
                        if (photo.isLinkAThumbnail() || photo.getSize() > PHOTO_SIZE_LIMIT) {
                            mImageUrl = photo.getMP4Link();
                            displayVideo(mImageUrl);
                        } else {
                            mImageUrl = photo.getLink();
                            displayImage(mImageUrl, photo.isAnimated());
                        }
                    } else {
                        mImageUrl = photo.getLink();
                        displayImage(mImageUrl, photo.isAnimated());
                    }
                    break;

                case MESSAGE_ACTION_FAILED:
                    SnackBar.show(getActivity(), (String) msg.obj);
                    dismiss();
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    @Override
    public void onVideoDownloadStart(String key, String url) {

    }

    @Override
    public void onVideoDownloadFailed(Exception ex, String url) {
        if (isAdded() && isResumed() && getActivity() != null) {
            SnackBar.show(getActivity(), R.string.loading_image_error);
            dismiss();
        }
    }

    @Override
    public void onVideoDownloadComplete(File file) {
        if (isAdded() && isResumed()) {
            displayVideo(file);
        }
    }
}
