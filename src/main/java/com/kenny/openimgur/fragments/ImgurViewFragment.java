package com.kenny.openimgur.fragments;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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

import com.kenny.openimgur.R;
import com.kenny.openimgur.ViewPhotoActivity;
import com.kenny.openimgur.adapters.PhotoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.PointsBar;
import com.kenny.openimgur.ui.SnackBar;
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
public class ImgurViewFragment extends BaseFragment {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private static final String KEY_ITEMS = "items";

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

        if (savedInstanceState == null) {
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
        } else {
            List<ImgurPhoto> photos = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
            mPhotoAdapter = new PhotoAdapter(getActivity(), photos, mImgurListener);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        }

        mListView.setOnScrollListener(new PauseOnScrollListener(app.getImageLoader(), false, true,
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
            mPhotoAdapter.destroy();
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

        if (mPhotoAdapter != null && !mPhotoAdapter.isListenerSet()) {
            CustomLinkMovement.getInstance().addListener(mImgurListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        CustomLinkMovement.getInstance().removeListener(mImgurListener);
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
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.STATUS_JSON_EXCEPTION);
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

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        e.printStackTrace();
    }

    private ImgurListener mImgurListener = new ImgurListener() {
        @Override
        public void onPhotoTap(ImageView image) {
            int position = mListView.getPositionForView(image) - mListView.getHeaderViewsCount();
            startActivity(ViewPhotoActivity.createIntent(getActivity(), mPhotoAdapter.getItem(position)));
        }

        @Override
        public void onPhotoLongTapListener(ImageView image) {
            final int position = mListView.getPositionForView(image) - mListView.getHeaderViewsCount();

            if (position >= 0) {
                new AlertDialog.Builder(getActivity()).setItems(new String[]{getString(R.string.copy_link)}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ImgurBaseObject obj = mPhotoAdapter.getItem(position);
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("link", obj.getLink()));
                    }
                }).show();
            }
        }

        @Override
        public void onPlayTap(final ProgressBar prog, final ImageView image, final ImageButton play) {
            ImageLoader loader = app.getImageLoader();
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
                        SnackBar.show(getActivity(), R.string.loading_image_error);
                        prog.setVisibility(View.GONE);
                        play.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                        if (!ImageUtil.loadAndDisplayGif(image, s, app.getImageLoader())) {
                            SnackBar.show(getActivity(), R.string.loading_image_error);
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        } else {
                            prog.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onLoadingCancelled(String s, View view) {
                        SnackBar.show(getActivity(), R.string.loading_image_error);
                        prog.setVisibility(View.GONE);
                        play.setVisibility(View.VISIBLE);
                    }
                });
            } else {
                if (!ImageUtil.loadAndDisplayGif(image, photo.getLink(), app.getImageLoader())) {
                    SnackBar.show(getActivity(), R.string.loading_image_error);
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
                    mMultiView.setErrorText(R.id.errorMessage, ApiClient.getErrorCodeStringResource((Integer) msg.obj));
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;
            }

            super.handleMessage(msg);
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            ArrayList<ImgurPhoto> copy = new ArrayList<ImgurPhoto>(mPhotoAdapter.getPhotos());
            outState.putParcelableArrayList(KEY_ITEMS, copy);
        }
    }
}
