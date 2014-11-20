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
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.PointsBar;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ImgurViewFragment extends BaseFragment implements ImgurListener {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private static final String KEY_ITEMS = "items";

    private MultiStateView mMultiView;

    private ListView mListView;

    private ImgurBaseObject mImgurObject;

    private PhotoAdapter mPhotoAdapter;

    private static final long FIVE_MB = 5 * 1024 * 1024;

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
        mListView = (ListView) mMultiView.findViewById(R.id.list);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);
        handleArguments(getArguments(), savedInstanceState);
    }

    /**
     * Handles the arguments based to the fragment
     *
     * @param args               The Bundle arguments supplied when creating the fragment
     * @param savedInstanceState The arguments that were saved for the fragment
     */
    private void handleArguments(Bundle args, Bundle savedInstanceState) {
        // We have a savedInstanceState, use them over args
        if (savedInstanceState != null) {
            mImgurObject = savedInstanceState.getParcelable(KEY_IMGUR_OBJECT);
            List<ImgurPhoto> photos = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
            mPhotoAdapter = new PhotoAdapter(getActivity(), photos, this);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        } else {
            setupFragmentWithObject((ImgurBaseObject) args.getParcelable(KEY_IMGUR_OBJECT));
        }
    }

    /**
     * Sets up the fragment with the given ImgurBaseObject
     *
     * @param object
     */
    private void setupFragmentWithObject(ImgurBaseObject object) {
        mImgurObject = object;

        if (mImgurObject instanceof ImgurPhoto) {
            List<ImgurPhoto> photo = new ArrayList<ImgurPhoto>(1);
            photo.add(((ImgurPhoto) mImgurObject));
            mPhotoAdapter = new PhotoAdapter(getActivity(), photo, this);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        } else if (((ImgurAlbum) mImgurObject).getAlbumPhotos() == null || ((ImgurAlbum) mImgurObject).getAlbumPhotos().isEmpty()) {
            String url = String.format(Endpoints.ALBUM.getUrl(), mImgurObject.getId());
            ApiClient api = new ApiClient(url, ApiClient.HttpRequest.GET);
            api.doWork(ImgurBusEvent.EventType.ALBUM_DETAILS, mImgurObject.getId(), null);
        } else {
            mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), this);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        }
    }

    /**
     * Creates the header for the photo/album. This MUST be called before settings the adapter for pre 4.4 devices
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


        int totalPoints = mImgurObject.getDownVotes() + mImgurObject.getUpVotes();
        pointText.setText((mImgurObject.getUpVotes() - mImgurObject.getDownVotes()) + " " + getString(R.string.points));
        pointsBar.setUpVotes(mImgurObject.getUpVotes());
        pointsBar.setTotalPoints(totalPoints);
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

        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            CustomLinkMovement.getInstance().addListener(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
        CustomLinkMovement.getInstance().removeListener(this);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // In case a gif is playing, this will cause them to stop when swiped the fragment is swiped away
        if (!isVisibleToUser && mPhotoAdapter != null) mPhotoAdapter.notifyDataSetChanged();
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            switch (event.eventType) {
                case ALBUM_DETAILS:
                    if (mImgurObject.getId().equals(event.id) && mPhotoAdapter == null) {
                        int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

                        if (statusCode == ApiClient.STATUS_OK) {
                            JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);

                            for (int i = 0; i < arr.length(); i++) {
                                ImgurPhoto photo = new ImgurPhoto(arr.getJSONObject(i));
                                ((ImgurAlbum) mImgurObject).addPhotoToAlbum(photo);
                            }

                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, mImgurObject);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
                        }

                    }
                    break;
            }
        } catch (JSONException e) {
            LogUtil.e(TAG, "Error while receiving event", e);
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.STATUS_JSON_EXCEPTION);
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

        LogUtil.e(TAG, "Error received from Event Bus", e);
    }

    @Override
    public void onPhotoTap(View view) {
        // Try to pause either the gif or video if it is currently playing
        if (mPhotoAdapter.attemptToPause(view)) return;

        int position = mListView.getPositionForView(view) - mListView.getHeaderViewsCount();
        startActivity(ViewPhotoActivity.createIntent(getActivity(), mPhotoAdapter.getItem(position)));
    }

    @Override
    public void onPhotoLongTapListener(View view) {
        final int position = mListView.getPositionForView(view) - mListView.getHeaderViewsCount();

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
    public void onPlayTap(final ProgressBar prog, final ImageButton play, final ImageView image, final VideoView video) {
        final ImgurPhoto photo = mPhotoAdapter.getItem(mListView.getPositionForView(image) - mListView.getHeaderViewsCount());

        if (image.getVisibility() == View.VISIBLE && image.getDrawable() instanceof GifDrawable) {
            play.setVisibility(View.GONE);
            ((GifDrawable) image.getDrawable()).start();
            return;
        }

        if (video.getVisibility() == View.VISIBLE && video.getDuration() > 0) {
            play.setVisibility(View.GONE);
            video.start();
            return;
        }

        play.setVisibility(View.GONE);
        prog.setVisibility(View.VISIBLE);

        // Load regular gifs if they are less than 5mb
        if (photo.getSize() <= FIVE_MB && !photo.isLinkAThumbnail()) {
            ImageLoader loader = app.getImageLoader();
            File file = DiskCacheUtils.findInCache(photo.getLink(), loader.getDiskCache());

            if (FileUtil.isFileValid(file)) {
                if (!ImageUtil.loadAndDisplayGif(image, photo.getLink(), app.getImageLoader())) {
                    SnackBar.show(getActivity(), R.string.loading_image_error);
                    prog.setVisibility(View.GONE);
                    play.setVisibility(View.VISIBLE);
                } else {
                    prog.setVisibility(View.GONE);
                }
            } else {
                loader.loadImage(photo.getLink(), new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String s, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String s, View view, FailReason failReason) {
                        if (image != null && getActivity() != null) {
                            SnackBar.show(getActivity(), R.string.loading_image_error);
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                        if (image != null && getActivity() != null) {
                            if (!ImageUtil.loadAndDisplayGif(image, s, app.getImageLoader())) {
                                SnackBar.show(getActivity(), R.string.loading_image_error);
                                prog.setVisibility(View.GONE);
                                play.setVisibility(View.VISIBLE);
                            } else {
                                prog.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onLoadingCancelled(String s, View view) {
                        if (image != null && getActivity() != null) {
                            SnackBar.show(getActivity(), R.string.loading_image_error);
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        } else {
            File file = VideoCache.getInstance().getVideoFile(photo.getMP4Link());

            if (FileUtil.isFileValid(file)) {
                video.setVisibility(View.VISIBLE);
                video.setVideoPath(file.getAbsolutePath());
                image.setVisibility(View.GONE);
                prog.setVisibility(View.GONE);
                video.start();
            } else {
                VideoCache.getInstance().putVideo(photo.getMP4Link(), new VideoCache.VideoCacheListener() {
                    @Override
                    public void onVideoDownloadStart(String key, String url) {
                        if (image != null && getActivity() != null) {
                            prog.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onVideoDownloadFailed(Exception ex, String url) {
                        LogUtil.e(TAG, "Unable to download video", ex);

                        if (image != null && getActivity() != null) {
                            SnackBar.show(getActivity(), R.string.loading_image_error);
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onVideoDownloadComplete(File file) {
                        if (video != null && getActivity() != null) {
                            video.setVideoPath(file.getAbsolutePath());
                            image.setVisibility(View.GONE);
                            prog.setVisibility(View.GONE);
                            video.setVisibility(View.VISIBLE);
                            video.start();
                        }
                    }
                });
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

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    mImgurObject = ((ImgurBaseObject) msg.obj);
                    mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), ImgurViewFragment.this);
                    createHeader();
                    mListView.setAdapter(mPhotoAdapter);
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiView.setErrorText(R.id.errorMessage, ApiClient.getErrorCodeStringResource((Integer) msg.obj));
                    mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mPhotoAdapter.retainItems());
        }

        outState.putParcelable(KEY_IMGUR_OBJECT, mImgurObject);
        super.onSaveInstanceState(outState);
    }
}
