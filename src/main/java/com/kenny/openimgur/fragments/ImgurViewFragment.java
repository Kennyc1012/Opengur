package com.kenny.openimgur.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.FullScreenPhotoActivity;
import com.kenny.openimgur.activities.ProfileActivity;
import com.kenny.openimgur.adapters.PhotoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.ApiClient2;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.api.responses.TagResponse;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurAlbum2;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurBaseObject2;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurPhoto2;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.services.DownloaderService;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
import pl.droidsonroids.gif.GifDrawable;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ImgurViewFragment extends BaseFragment implements ImgurListener {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private static final String KEY_DISPLAY_TAGS = "display_tags";

    private static final String KEY_ITEMS = "items";

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.list)
    RecyclerView mListView;

    private ImgurBaseObject2 mImgurObject;

    private PhotoAdapter mPhotoAdapter;

    private boolean mDisplayTags = true;

    private static final long FIVE_MB = 5 * 1024 * 1024;

    public static ImgurViewFragment createInstance(@NonNull ImgurBaseObject2 obj, boolean displayTags) {
        ImgurViewFragment fragment = new ImgurViewFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_IMGUR_OBJECT, obj);
        args.putBoolean(KEY_DISPLAY_TAGS, displayTags);
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
        return inflater.inflate(R.layout.fragment_imgur_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleArguments(getArguments(), savedInstanceState);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.view_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (mPhotoAdapter != null) mPhotoAdapter.clear();
                mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                String url = String.format(Endpoints.GALLERY_ITEM_DETAILS.getUrl(), mImgurObject.getId());
                ApiClient api = new ApiClient(url, ApiClient.HttpRequest.GET);
                api.doWork(ImgurBusEvent.EventType.GALLERY_ITEM_INFO, mImgurObject.getId(), null);
                return true;

            case R.id.copy_album_link:
                if (mImgurObject instanceof ImgurAlbum2) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("link", mImgurObject.getLink()));
                    SnackBar.show(getActivity(), R.string.link_copied);
                }
                break;

            case R.id.favorite:
                if (user != null) {
                   /* String url;
                    // TODO
                    if (imgurObj instanceof ImgurAlbum2) {
                        url = String.format(Endpoints.FAVORITE_ALBUM.getUrl(), imgurObj.getId());
                    } else {
                        url = String.format(Endpoints.FAVORITE_IMAGE.getUrl(), imgurObj.getId());
                    }

                    final RequestBody body = new FormEncodingBuilder().add("id", imgurObj.getId()).build();

                    if (mApiClient == null) {
                        mApiClient = new ApiClient(url, ApiClient.HttpRequest.POST);
                    } else {
                        mApiClient.setUrl(url);
                        mApiClient.setRequestType(ApiClient.HttpRequest.POST);
                    }

                    mApiClient.doWork(ImgurBusEvent.EventType.FAVORITE, imgurObj.getId(), body);*/
                } else {
                    SnackBar.show(getActivity(), R.string.user_not_logged_in);
                }
                return true;

            case R.id.profile:
                startActivity(ProfileActivity.createIntent(getActivity(), mImgurObject.getAccount()));
                return true;

            case R.id.reddit:
                if (TextUtils.isEmpty(mImgurObject.getRedditLink())) {
                    LogUtil.w(TAG, "Item does not have a reddit link");
                    return false;
                }

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com" + mImgurObject.getRedditLink()));
                if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    SnackBar.show(getActivity(), R.string.cant_launch_intent);
                }
                return true;


            case R.id.share:
                startActivity(Intent.createChooser(mImgurObject.getShareIntent(), getString(R.string.share)));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.copy_album_link).setVisible(mImgurObject instanceof ImgurAlbum2);

        if (TextUtils.isEmpty(mImgurObject.getAccount())) {
            menu.findItem(R.id.profile).setVisible(false);
        }

        if (TextUtils.isEmpty(mImgurObject.getRedditLink())) {
            menu.findItem(R.id.reddit).setVisible(false);
        }

        menu.findItem(R.id.favorite).setIcon(mImgurObject.isFavorited() ?
                R.drawable.ic_action_favorite : R.drawable.ic_action_unfavorite);
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
            mDisplayTags = savedInstanceState.getBoolean(KEY_DISPLAY_TAGS, true);
            mImgurObject = savedInstanceState.getParcelable(KEY_IMGUR_OBJECT);
            List<ImgurPhoto2> photos = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
            mPhotoAdapter = new PhotoAdapter(getActivity(), photos, mImgurObject, this);
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            fetchTags();
        } else {
            mDisplayTags = args.getBoolean(KEY_DISPLAY_TAGS, true);
            setupFragmentWithObject((ImgurBaseObject2) args.getParcelable(KEY_IMGUR_OBJECT));
        }
    }

    /**
     * Sets up the fragment with the given ImgurBaseObject
     *
     * @param object
     */
    private void setupFragmentWithObject(ImgurBaseObject2 object) {
        mImgurObject = object;

        if (mImgurObject instanceof ImgurPhoto2) {
            if (mImgurObject.getUpVotes() != Integer.MIN_VALUE) {
                List<ImgurPhoto2> photo = new ArrayList<>(1);
                photo.add(((ImgurPhoto2) mImgurObject));
                mPhotoAdapter = new PhotoAdapter(getActivity(), photo, mImgurObject, this);
                mListView.setAdapter(mPhotoAdapter);
                mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                fetchTags();
            } else {
                LogUtil.v(TAG, "Item does not contain scores, fetching from api");
                String url = String.format(Endpoints.GALLERY_ITEM_DETAILS.getUrl(), mImgurObject.getId());
                ApiClient api = new ApiClient(url, ApiClient.HttpRequest.GET);
                api.doWork(ImgurBusEvent.EventType.GALLERY_ITEM_INFO, mImgurObject.getId(), null);
            }
        } else if (((ImgurAlbum2) mImgurObject).getAlbumPhotos() == null || ((ImgurAlbum2) mImgurObject).getAlbumPhotos().isEmpty()) {
            fetchAlbumImages();
        } else {
            mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum2) mImgurObject).getAlbumPhotos(), mImgurObject, this);
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
            fetchTags();
        }
    }

    public void setVote(String vote) {
        // Trigger the adapter to redraw displaying the vote
        if (mImgurObject != null) mImgurObject.setVote(vote);

        if (mPhotoAdapter != null) {
            LinearLayoutManager manager = ((LinearLayoutManager) mListView.getLayoutManager());
            int firstVisible = manager.findFirstCompletelyVisibleItemPosition();

            if (firstVisible == 0) {
                // The header is visible, update it without triggering a list redraw
                View view = mListView.getChildAt(0);
                if (view != null) mPhotoAdapter.updateHeader(mListView.getChildViewHolder(view));
            }
            // If the header is not visible, it will get updated when it becomes visible
        }
    }

    @Override
    public void onDestroyView() {
        if (mPhotoAdapter != null) {
            mPhotoAdapter.onDestroy();
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
        // Ignore other requests
        if (!mImgurObject.getId().equals(event.id)) return;

        try {
            int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
            switch (event.eventType) {

                case GALLERY_ITEM_INFO:
                    if (statusCode == ApiClient.STATUS_OK) {
                        ImgurBaseObject object;
                        JSONObject data = event.json.getJSONObject(ApiClient.KEY_DATA);

                        if (data.has("is_album") && data.getBoolean("is_album")) {
                            object = new ImgurAlbum(data);
                        } else {
                            object = new ImgurPhoto(data);
                        }

                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, object);
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, statusCode);
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

        // Adjust position for header
        final int position = mListView.getLayoutManager().getPosition(view) - 1;
        startActivity(FullScreenPhotoActivity.createIntent(getActivity(), mPhotoAdapter.retainItems(), position));
    }

    @Override
    public void onPhotoLongTapListener(View view) {
        // Adjust position for header
        final int position = mListView.getLayoutManager().getPosition(view) - 1;

        if (position >= 0) {
            new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                    .setItems(R.array.photo_long_press_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ImgurPhoto2 photo = mPhotoAdapter.getItem(position);
                            String link;

                            if (photo.isLinkAThumbnail() && photo.hasMP4Link()) {
                                link = photo.getMP4Link();
                            } else {
                                link = photo.getLink();
                            }

                            switch (which) {
                                // Copy
                                case 0:
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", link));
                                    break;

                                // Download
                                case 1:
                                    getActivity().startService(DownloaderService.createIntent(getActivity(), link));
                                    break;

                                // Share
                                case 2:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, link);
                                    startActivity(shareIntent);
                                    break;
                            }
                        }
                    }).show();
        }
    }

    @Override
    public void onPlayTap(final ProgressBar prog, final FloatingActionButton play, final ImageView image, final VideoView video, final View view) {
        // Adjust position for header
        final int position = mListView.getLayoutManager().getPosition(view) - 1;
        final ImgurPhoto2 photo = mPhotoAdapter.getItem(position);

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
                    if (getActivity() == null || !isAdded() || isRemoving()) return;

                    if (msg.obj instanceof ImgurBaseObject) {
                        setupFragmentWithObject((ImgurBaseObject2) msg.obj);
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
        super.onSaveInstanceState(outState);

        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mPhotoAdapter.retainItems());
        }

        outState.putBoolean(KEY_DISPLAY_TAGS, mDisplayTags);
        outState.putParcelable(KEY_IMGUR_OBJECT, mImgurObject);
    }

    private void fetchAlbumImages() {
        ApiClient2.getService().getAlbumImages(mImgurObject.getId(), new Callback<AlbumResponse>() {
            @Override
            public void success(AlbumResponse albumResponse, Response response) {
                if (!isAdded()) return;

                if (!albumResponse.data.isEmpty()) {
                    ((ImgurAlbum2) mImgurObject).addPhotosToAlbum(albumResponse.data);
                    mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum2) mImgurObject).getAlbumPhotos(), mImgurObject, ImgurViewFragment.this);
                    mListView.setAdapter(mPhotoAdapter);
                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    fetchTags();
                } else {
                    // TODO
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                // TODO
            }
        });
    }

    private void fetchTags() {
        // No need to request if the object already has tags, they will be set in the adapter
        if (mDisplayTags && (mImgurObject.getTags() == null || mImgurObject.getTags().isEmpty())) {
            ApiClient2.getService().getTags(mImgurObject.getId(), new Callback<TagResponse>() {
                @Override
                public void success(TagResponse tagResponse, Response response) {
                    if (!isAdded()) return;

                    if (tagResponse.data != null && !tagResponse.data.tags.isEmpty()) {
                        mImgurObject.setTags(tagResponse.data.tags);
                        if (mPhotoAdapter != null) mPhotoAdapter.setTags(tagResponse.data.tags);
                    } else {
                        LogUtil.v(TAG, "Did not receive any tags");
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    if (!isAdded()) return;
                    // Just ignore any bad tag responses
                    LogUtil.e(TAG, "Received an error while fetching tags", error);
                }
            });
        }
    }
}
