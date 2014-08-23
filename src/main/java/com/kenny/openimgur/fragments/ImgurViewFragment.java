package com.kenny.openimgur.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ViewPhotoActivity;
import com.kenny.openimgur.adapters.PhotoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.PointsBar;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ImgurViewFragment extends Fragment {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private MultiStateView mMultiView;

    private ListView mListView;

    private ImgurBaseObject mImgurObject;

    private PhotoAdapter mPhotoAdapter;

    public static ImgurViewFragment createInstance(@NonNull ImgurBaseObject obj) {
        ImgurViewFragment fragment = new ImgurViewFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_IMGUR_OBJECT, obj);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imgur_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiStateView);
        mListView = (ListView) view.findViewById(R.id.list);
        mImgurObject = getArguments().getParcelable(KEY_IMGUR_OBJECT);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);

        if (mImgurObject instanceof ImgurPhoto) {
            List<ImgurPhoto> photo = new ArrayList<ImgurPhoto>(1);
            photo.add(((ImgurPhoto) mImgurObject));
            mPhotoAdapter = new PhotoAdapter(getActivity(), photo, mImgurListener);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        } else if (((ImgurAlbum) mImgurObject).getAlbumPhotos() == null || ((ImgurAlbum) mImgurObject).getAlbumPhotos().isEmpty()) {
            String url = String.format(Endpoints.ALBUM.getUrl(), mImgurObject.getId());
            ApiClient api = new ApiClient(url, ApiClient.HttpRequest.GET);
            api.doWork(ImgurBusEvent.EventType.ALBUM_DETAILS, mImgurObject.getId(), null);
        } else {
            mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), mImgurListener);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        }

        mListView.setOnScrollListener(new PauseOnScrollListener(OpenImgurApp.getInstance().getImageLoader(), false, true,
                new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                        // NOP
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                        // NOOP
                    }
                }
        ));
    }

    /**
     * Creates the header for the photo/album
     */
    private void createHeader() {
        View headerView = View.inflate(getActivity(), R.layout.image_header, null);
        TextViewRoboto title = (TextViewRoboto) headerView.findViewById(R.id.title);
        TextViewRoboto author = (TextViewRoboto) headerView.findViewById(R.id.author);
        PointsBar pointsBar = (PointsBar) headerView.findViewById(R.id.pointsBar);
        TextViewRoboto pointText = (TextViewRoboto) headerView.findViewById(R.id.pointText);

        if (!TextUtils.isEmpty(mImgurObject.getTitle())) {
            title.setText(mImgurObject.getTitle());
        }

        if (!TextUtils.isEmpty(mImgurObject.getAccount())) {
            author.setText("- " + mImgurObject.getAccount());
        } else {
            author.setText("- ?????");
        }

        pointText.setText(mImgurObject.getScore() + " " + getString(R.string.points));
        pointsBar.setUpVotes(mImgurObject.getUpVotes());
        pointsBar.setTotalPoints(mImgurObject.getDownVotes() + mImgurObject.getUpVotes());
        mListView.addHeaderView(headerView);
    }

    @Override
    public void onDestroyView() {
        if (mPhotoAdapter != null) {
            mPhotoAdapter.clear();
            mPhotoAdapter = null;
        }

        mListView = null;
        mMultiView = null;
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroyView();
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

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.ALBUM_DETAILS &&
                mImgurObject.getId().equals(event.id) && mPhotoAdapter == null) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);

                    for (int i = 0; i < arr.length(); i++) {
                        ImgurPhoto photo = new ImgurPhoto(arr.getJSONObject(i));
                        ((ImgurAlbum) mImgurObject).addPhotoToAlbum(photo);
                    }

                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, statusCode);
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                handleError(ApiClient.STATUS_JSON_EXCEPTION);
            }
        }
    }

    /**
     * Event Method that is fired if EventBus throws an error
     *
     * @param event
     */
    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();
        e.printStackTrace();

        if (e instanceof JSONException) {
            handleError(ApiClient.STATUS_JSON_EXCEPTION);
        } else if (e instanceof IOException) {
            handleError(ApiClient.STATUS_IO_EXCEPTION);
        }
    }

    private ImgurListener mImgurListener = new ImgurListener() {
        @Override
        public void onPhotoTap(ImageView image) {
            int position = mListView.getPositionForView(image) - mListView.getHeaderViewsCount();
            startActivity(ViewPhotoActivity.createIntent(getActivity(), mPhotoAdapter.getItem(position)));
        }

        @Override
        public void onPlayTap(final ProgressBar prog, final ImageView image, final ImageButton play) {
            final ImageLoader loader = OpenImgurApp.getInstance().getImageLoader();
            int position = mListView.getPositionForView(image) - mListView.getHeaderViewsCount();
            prog.setVisibility(View.VISIBLE);
            play.setVisibility(View.GONE);
            final ImgurPhoto photo = mPhotoAdapter.getItem(position);
            File file = DiskCacheUtils.findInCache(photo.getLink(), loader.getDiskCache());

            // Need to do a check since we might be loading a thumbnail and not the actual gif here
            if (!FileUtil.isFileValid(file)) {
                loader.loadImage(photo.getLink(), new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String s, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String s, View view, FailReason failReason) {
                        Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        prog.setVisibility(View.GONE);
                        play.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                        if (!ImageUtil.loadAndDisplayGif(image, s, OpenImgurApp.getInstance().getImageLoader())) {
                            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        } else {
                            prog.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onLoadingCancelled(String s, View view) {
                        Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        prog.setVisibility(View.GONE);
                        play.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                if (!ImageUtil.loadAndDisplayGif(image, photo.getLink(), OpenImgurApp.getInstance().getImageLoader())) {
                    Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                    prog.setVisibility(View.GONE);
                    play.setVisibility(View.VISIBLE);
                } else {
                    prog.setVisibility(View.GONE);
                }
            }
        }

        @Override
        public void onLinkTap(View textView, String url) {
        }

        @Override
        public void onViewRepliesTap(View view) {
            // NOOP
        }
    };

    /**
     * Handles any errors from the Api
     *
     * @param errorCode
     */
    private void handleError(int errorCode) {
        // TODO handle errors
    }

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), mImgurListener);
                    createHeader();
                    mListView.setAdapter(mPhotoAdapter);
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case MESSAGE_ACTION_FAILED:
                    handleError((Integer) msg.obj);
                    break;
            }

            super.handleMessage(msg);
        }
    };
}
