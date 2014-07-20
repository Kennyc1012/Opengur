package com.kenny.openimgur.fragments;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.MultiStateView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.AsyncExecutor;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 7/19/14.
 */
public class PopupImageDialogFragment extends DialogFragment {
    private static final String KEY_URL = "url";

    private static final String KEY_ANIMATED = "animated";

    private static final String KEY_DIRECT_LINK = "direct_link";

    private MultiStateView mMultiView;

    private ImageView mImage;

    public static PopupImageDialogFragment getInstance(String url, boolean isAnimated, boolean isDirectLink) {
        PopupImageDialogFragment fragment = new PopupImageDialogFragment();
        Bundle args = new Bundle(3);
        args.putString(KEY_URL, url);
        args.putBoolean(KEY_ANIMATED, isAnimated);
        args.putBoolean(KEY_DIRECT_LINK, isDirectLink);
        fragment.setArguments(args);
        return fragment;
    }

    public PopupImageDialogFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.image_popup_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle bundle = getArguments();

        if (bundle == null || !bundle.containsKey(KEY_URL)) {
            dismissAllowingStateLoss();
            return;
        }

        mMultiView = (MultiStateView) view.findViewById(R.id.multiView);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        mImage = (ImageView) mMultiView.findViewById(R.id.image);
        getDialog().setTitle(R.string.photo);
        final String url = bundle.getString(KEY_URL, null);
        final boolean isAnimated = bundle.getBoolean(KEY_ANIMATED, false);
        final boolean isDirectLink = bundle.getBoolean(KEY_DIRECT_LINK, true);

        if (isDirectLink) {
            displayImage(url, isAnimated);
        } else {
            AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
                @Override
                public void run() throws Exception {
                    final ApiClient api = new ApiClient(String.format(Endpoints.IMAGE_DETAILS.getUrl(), url),
                            ApiClient.HttpRequest.GET);
                    api.doWork(ImgurBusEvent.EventType.ITEM_DETAILS, null);
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
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
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayImage(photo.getLink(), photo.isAnimated());
                    }
                });
            } else if (event.eventType == ImgurBusEvent.EventType.ITEM_DETAILS && statusCode != ApiClient.STATUS_OK) {
                Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();
            }
        } catch (JSONException e) {
            Toast.makeText(getActivity(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
        }
    }

    /**
     * Loads the image into the ImageView
     *
     * @param url
     * @param isAnimated
     */
    public void displayImage(String url, final boolean isAnimated) {
        final ImageLoader loader = OpenImgurApp.getInstance().getImageLoader();
        loader.displayImage(url, mImage, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {

            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                dismissAllowingStateLoss();
                Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                if (isAnimated) {
                    File file = DiskCacheUtils.findInCache(s, loader.getDiskCache());
                    if (file != null && file.exists()) {
                        try {
                            ((ImageView) view).setImageDrawable(new GifDrawable(file));
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                            dismissAllowingStateLoss();
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        dismissAllowingStateLoss();
                    }
                }
            }

            @Override
            public void onLoadingCancelled(String s, View view) {

            }
        });
    }
}
