package com.kenny.openimgur.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewPhotoActivity;
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

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ImgurViewFragment extends BaseFragment implements ImgurListener {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private static final String KEY_ITEMS = "items";

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.list)
    ListView mListView;

    private View mHeaderView;

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
            if (mImgurObject.getUpVotes() != Integer.MIN_VALUE) {
                List<ImgurPhoto> photo = new ArrayList<>(1);
                photo.add(((ImgurPhoto) mImgurObject));
                mPhotoAdapter = new PhotoAdapter(getActivity(), photo, this);
                createHeader();
                mListView.setAdapter(mPhotoAdapter);
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            } else {
                LogUtil.v(TAG, "Item does not contain scores, fetching from api");
                String url = String.format(Endpoints.GALLERY_ITEM_DETAILS.getUrl(), mImgurObject.getId());
                ApiClient api = new ApiClient(url, ApiClient.HttpRequest.GET);
                api.doWork(ImgurBusEvent.EventType.GALLERY_ITEM_INFO, mImgurObject.getId(), null);
            }
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
        mHeaderView = View.inflate(getActivity(), R.layout.image_header, null);
        TextView title = (TextView) mHeaderView.findViewById(R.id.title);
        TextView author = (TextView) mHeaderView.findViewById(R.id.author);
        PointsBar pointsBar = (PointsBar) mHeaderView.findViewById(R.id.pointsBar);
        TextView pointText = (TextView) mHeaderView.findViewById(R.id.pointText);
        TextView topic = (TextView) mHeaderView.findViewById(R.id.topic);

        if (!TextUtils.isEmpty(mImgurObject.getTitle())) {
            title.setText(mImgurObject.getTitle());
        }

        if (!TextUtils.isEmpty(mImgurObject.getAccount())) {
            author.setText("- " + mImgurObject.getAccount());
        } else {
            author.setText("- ?????");
        }

        if (!TextUtils.isEmpty(mImgurObject.getTopic())) {
            topic.setText(mImgurObject.getTopic());
        }

        int totalPoints = mImgurObject.getDownVotes() + mImgurObject.getUpVotes();
        pointText.setText((mImgurObject.getUpVotes() - mImgurObject.getDownVotes()) + " " + getString(R.string.points));
        pointsBar.setUpVotes(mImgurObject.getUpVotes());
        pointsBar.setTotalPoints(totalPoints);

        if (mImgurObject.isFavorited() || ImgurBaseObject.VOTE_UP.equals(mImgurObject.getVote())) {
            pointText.setTextColor(getResources().getColor(R.color.notoriety_positive));
        } else if (ImgurBaseObject.VOTE_DOWN.equals(mImgurObject.getVote())) {
            pointText.setTextColor(getResources().getColor(R.color.notoriety_negative));
        }

        mListView.addHeaderView(mHeaderView);
    }

    public void setVote(String vote) {
        mImgurObject.setVote(vote);

        if (mHeaderView != null) {
            TextView pointText = (TextView) mHeaderView.findViewById(R.id.pointText);
            PointsBar pointsBar = (PointsBar) mHeaderView.findViewById(R.id.pointsBar);

            int totalPoints = mImgurObject.getDownVotes() + mImgurObject.getUpVotes();
            pointText.setText((mImgurObject.getUpVotes() - mImgurObject.getDownVotes()) + " " + getString(R.string.points));
            pointsBar.setUpVotes(mImgurObject.getUpVotes());
            pointsBar.setTotalPoints(totalPoints);

            if (mImgurObject.isFavorited() || ImgurBaseObject.VOTE_UP.equals(mImgurObject.getVote())) {
                pointText.setTextColor(getResources().getColor(R.color.notoriety_positive));
            } else if (ImgurBaseObject.VOTE_DOWN.equals(mImgurObject.getVote())) {
                pointText.setTextColor(getResources().getColor(R.color.notoriety_negative));
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (mPhotoAdapter != null) {
            mPhotoAdapter.destroy();
            mPhotoAdapter = null;
        }

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
        EventBus.getDefault().unregister(this);
        CustomLinkMovement.getInstance().removeListener(this);
        super.onPause();
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
                    if (mImgurObject.getId().equals(event.id)) {
                        int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

                        if (statusCode == ApiClient.STATUS_OK) {
                            JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);
                            List<ImgurPhoto> photos = new ArrayList<>();

                            for (int i = 0; i < arr.length(); i++) {
                                photos.add(new ImgurPhoto(arr.getJSONObject(i)));
                            }

                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, photos);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
                        }
                    }
                    break;

                case GALLERY_ITEM_INFO:
                    if (mImgurObject.getId().equals(event.id)) {
                        int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

                        if (statusCode == ApiClient.STATUS_OK) {
                            ImgurPhoto photo = new ImgurPhoto(event.json.getJSONObject(ApiClient.KEY_DATA));
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, photo);
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
            new MaterialDialog.Builder(getActivity())
                    .items(new String[]{getString(R.string.copy_link)})
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
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
                    if (msg.obj instanceof ImgurBaseObject) {
                        setupFragmentWithObject((ImgurBaseObject) msg.obj);
                    } else if (msg.obj instanceof List && mImgurObject instanceof ImgurAlbum) {
                        ((ImgurAlbum) mImgurObject).addPhotosToAlbum((List<ImgurPhoto>) msg.obj);
                        mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), ImgurViewFragment.this);
                        createHeader();
                        mListView.setAdapter(mPhotoAdapter);
                        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    } else {
                        mMultiView.setErrorText(R.id.errorMessage, R.string.error_generic);
                        mMultiView.setViewState(MultiStateView.ViewState.ERROR);
                    }
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
