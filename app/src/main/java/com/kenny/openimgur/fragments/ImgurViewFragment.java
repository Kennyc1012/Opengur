package com.kenny.openimgur.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentCompat;
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
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.AlbumResponse;
import com.kenny.openimgur.api.responses.BasicObjectResponse;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.TagResponse;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.services.DownloaderService;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.ui.adapters.PhotoAdapter;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import pl.droidsonroids.gif.GifDrawable;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ImgurViewFragment extends BaseFragment implements ImgurListener {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private static final String KEY_DISPLAY_TAGS = "display_tags";

    private static final String KEY_ITEMS = "items";

    @BindView(R.id.multiView)
    MultiStateView mMultiView;

    @BindView(R.id.list)
    RecyclerView mListView;

    ImgurBaseObject mImgurObject;

    PhotoAdapter mPhotoAdapter;

    boolean mDisplayTags = true;

    private static final long FIVE_MB = 5 * 1024 * 1024;

    public static ImgurViewFragment createInstance(@NonNull ImgurBaseObject obj, boolean displayTags) {
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

        mMultiView.findViewById(R.id.errorButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPhotoAdapter != null) mPhotoAdapter.clear();
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchGalleryDetails();
            }
        });
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
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchGalleryDetails();
                return true;

            case R.id.copy_album_link:
                if (mImgurObject instanceof ImgurAlbum) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("link", mImgurObject.getLink()));
                    Snackbar.make(mMultiView, R.string.link_copied, Snackbar.LENGTH_LONG).show();
                }
                break;

            case R.id.favorite:
                if (user != null) {
                    favoriteItem();
                } else {
                    Snackbar.make(mMultiView, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
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
                    Snackbar.make(mMultiView, R.string.cant_launch_intent, Snackbar.LENGTH_LONG).show();
                }
                return true;


            case R.id.share:
                share(mImgurObject.getShareIntent(), R.string.share);
                return true;

            case R.id.report:
                if (user != null) {
                    new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                            .setTitle(R.string.report_reason)
                            .setItems(R.array.report_reasons, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Report reasons are in the order of their array index, just add 1 for the API
                                    reportItem(which + 1);
                                }
                            }).show();
                } else {
                    Snackbar.make(mMultiView, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
                }
                break;

            case R.id.download_album:
                if (checkPermissions()) {
                    if (NetworkUtils.isConnectedToWiFi(getActivity())) {
                        downloadAlbum();
                    } else {
                        new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                                .setTitle(R.string.download_no_wifi_title)
                                .setMessage(R.string.download_no_wifi_msg)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        downloadAlbum();
                                    }
                                }).show();
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean isAlbum = mImgurObject instanceof ImgurAlbum;
        menu.findItem(R.id.copy_album_link).setVisible(isAlbum);
        menu.findItem(R.id.download_album).setVisible(isAlbum);

        if (TextUtils.isEmpty(mImgurObject.getAccount())) {
            menu.findItem(R.id.profile).setVisible(false);
        }

        if (TextUtils.isEmpty(mImgurObject.getRedditLink())) {
            menu.findItem(R.id.reddit).setVisible(false);
        } else {
            menu.findItem(R.id.report).setVisible(false);
        }

        menu.findItem(R.id.favorite).setIcon(mImgurObject.isFavorited() ?
                R.drawable.ic_action_favorite_24dp : R.drawable.ic_action_unfavorite_24dp);
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
            List<ImgurPhoto> photos = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
            mPhotoAdapter = new PhotoAdapter(getActivity(), photos, mImgurObject, this);
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            fetchTags();
        } else {
            mDisplayTags = args.getBoolean(KEY_DISPLAY_TAGS, true);
            setupFragmentWithObject((ImgurBaseObject) args.getParcelable(KEY_IMGUR_OBJECT));
        }
    }

    /**
     * Sets up the fragment with the given ImgurBaseObject
     *
     * @param object
     */
    void setupFragmentWithObject(ImgurBaseObject object) {
        mImgurObject = object;

        if (mImgurObject instanceof ImgurPhoto) {
            List<ImgurPhoto> photo = new ArrayList<>(1);
            photo.add(((ImgurPhoto) mImgurObject));
            mPhotoAdapter = new PhotoAdapter(getActivity(), photo, mImgurObject, this);
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            fetchTags();
        } else if (((ImgurAlbum) mImgurObject).getAlbumPhotos() == null || ((ImgurAlbum) mImgurObject).getAlbumPhotos().isEmpty()) {
            fetchAlbumImages();
        } else {
            mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), mImgurObject, this);
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            fetchTags();
        }
    }

    public void setVote(String vote) {
        // Trigger the adapter to redraw displaying the vote
        if (mImgurObject != null) mImgurObject.setVote(vote);
        if (mPhotoAdapter != null) mPhotoAdapter.notifyItemChanged(0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mPhotoAdapter != null) {
            mPhotoAdapter.onDestroy();
            mPhotoAdapter = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            CustomLinkMovement.getInstance().addListener(this);
        }
    }

    @Override
    public void onPause() {
        CustomLinkMovement.getInstance().removeListener(this);
        super.onPause();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // In case a gif is playing, this will cause them to stop when swiped the fragment is swiped away
        if (!isVisibleToUser && mPhotoAdapter != null) mPhotoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPhotoTap(View view) {
        // Try to pause either the gif or video if it is currently playing
        if (mPhotoAdapter.attemptToPause(view)) return;

        // Adjust position for header
        final int position = mListView.getLayoutManager().getPosition(view) - 1;
        startActivityForResult(FullScreenPhotoActivity.createIntent(getActivity(), mPhotoAdapter.retainItems(), mImgurObject, position), RequestCodes.FULL_SCREEN_VIEW);
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
                            ImgurPhoto photo = mPhotoAdapter.getItem(position);
                            String link = getLink(photo);

                            switch (which) {
                                // Copy
                                case 0:
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", link));
                                    break;

                                // Download
                                case 1:
                                    if (checkPermissions())
                                        getActivity().startService(DownloaderService.createIntent(getActivity(), link));
                                    break;

                                // Share
                                case 2:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, link);
                                    share(shareIntent, R.string.share);
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
        final ImgurPhoto photo = mPhotoAdapter.getItem(position);

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
            final ImageLoader loader = ImageUtil.getImageLoader(getActivity());
            File file = DiskCacheUtils.findInCache(photo.getLink(), loader.getDiskCache());

            if (FileUtil.isFileValid(file)) {
                if (!ImageUtil.loadAndDisplayGif(image, photo.getLink(), loader)) {
                    Snackbar.make(mMultiView, R.string.loading_image_error, Snackbar.LENGTH_LONG).show();
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
                            Snackbar.make(mMultiView, R.string.loading_image_error, Snackbar.LENGTH_LONG).show();
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                        if (image != null && getActivity() != null) {
                            if (!ImageUtil.loadAndDisplayGif(image, s, loader)) {
                                Snackbar.make(mMultiView, R.string.loading_image_error, Snackbar.LENGTH_LONG).show();
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
                            Snackbar.make(mMultiView, R.string.loading_image_error, Snackbar.LENGTH_LONG).show();
                            prog.setVisibility(View.GONE);
                            play.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        } else {
            File file = VideoCache.getInstance().getVideoFile(photo.getVideoLink());

            if (FileUtil.isFileValid(file)) {
                video.setVisibility(View.VISIBLE);
                video.setVideoPath(file.getAbsolutePath());
                image.setVisibility(View.GONE);
                prog.setVisibility(View.GONE);
                video.start();
            } else {
                VideoCache.getInstance().putVideo(photo.getVideoLink(), new VideoCache.VideoCacheListener() {
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
                            Snackbar.make(mMultiView, R.string.loading_image_error, Snackbar.LENGTH_LONG).show();
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

                    @Override
                    public void onProgress(int downloaded, int total) {

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            if (isApiLevel(Build.VERSION_CODES.N) && mPhotoAdapter.getItemCount() > GalleryAdapter.MAX_ITEMS) {
                return;
            }

            try {
                outState.putParcelableArrayList(KEY_ITEMS, mPhotoAdapter.retainItems());
            } catch (NullPointerException npe) {
                // This shouldn't be crashing, but for some reason is, need to figure out why
                LogUtil.e(TAG, "NPE while saving state", npe);
            }
        }

        outState.putBoolean(KEY_DISPLAY_TAGS, mDisplayTags);
        outState.putParcelable(KEY_IMGUR_OBJECT, mImgurObject);
    }

    String getLink(ImgurPhoto photo) {
        String link;

        if (photo.isLinkAThumbnail() && photo.hasVideoLink()) {
            link = photo.getVideoLink();
        } else {
            link = photo.getLink();
        }

        return link;
    }

    boolean checkPermissions() {
        @PermissionUtils.PermissionLevel int permissionLevel = PermissionUtils.getPermissionLevel(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        switch (permissionLevel) {
            case PermissionUtils.PERMISSION_AVAILABLE:
                return true;

            case PermissionUtils.PERMISSION_DENIED:
                Snackbar.make(mMultiView, R.string.permission_rationale_download, Snackbar.LENGTH_LONG)
                        .setAction(R.string.okay, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                FragmentCompat.requestPermissions(ImgurViewFragment.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                            }
                        }).show();
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
            default:
                FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                break;
        }

        return false;
    }

    void downloadAlbum() {
        if (mImgurObject instanceof ImgurAlbum) {
            if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
                ArrayList<String> urls = new ArrayList<>(mPhotoAdapter.getItemCount());

                for (ImgurPhoto p : mPhotoAdapter.retainItems()) {
                    urls.add(p.getLink());
                }

                getActivity().startService(DownloaderService.createIntent(getActivity(), urls));
            } else {
                Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            }
        } else {
            LogUtil.w(TAG, "Item is not an album");
        }
    }

    private void fetchAlbumImages() {
        ApiClient.getService().getAlbumImages(mImgurObject.getId()).enqueue(new Callback<AlbumResponse>() {
            @Override
            public void onResponse(Call<AlbumResponse> call, Response<AlbumResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().hasData()) {
                    ((ImgurAlbum) mImgurObject).addPhotosToAlbum(response.body().data);
                    mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), mImgurObject, ImgurViewFragment.this);
                    mListView.setAdapter(mPhotoAdapter);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    fetchTags();
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, response != null ? ApiClient.getErrorCode(response.code()) : R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void onFailure(Call<AlbumResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch album images", t);
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }

    void fetchTags() {
        // No need to request if the object already has tags, they will be set in the adapter
        if (mDisplayTags && mImgurObject != null && mImgurObject.isListed() && (mImgurObject.getTags() == null || mImgurObject.getTags().isEmpty())) {
            ApiClient.getService().getTags(mImgurObject.getId()).enqueue(new Callback<TagResponse>() {
                @Override
                public void onResponse(Call<TagResponse> call, Response<TagResponse> response) {
                    if (!isAdded() || response == null || response.body() == null) return;

                    TagResponse tagResponse = response.body();

                    if (tagResponse.data != null && !tagResponse.data.tags.isEmpty()) {
                        mImgurObject.setTags(tagResponse.data.tags);
                        if (mPhotoAdapter != null) mPhotoAdapter.notifyItemChanged(0);
                    } else {
                        LogUtil.v(TAG, "Did not receive any tags");
                    }
                }

                @Override
                public void onFailure(Call<TagResponse> call, Throwable t) {
                    LogUtil.e(TAG, "Received an error while fetching tags", t);
                }
            });
        }
    }

    void fetchGalleryDetails() {
        ApiClient.getService().getGalleryDetails(mImgurObject.getId()).enqueue(new Callback<BasicObjectResponse>() {
            @Override
            public void onResponse(Call<BasicObjectResponse> call, Response<BasicObjectResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().data != null) {
                    setupFragmentWithObject(response.body().data);
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void onFailure(Call<BasicObjectResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch gallery details", t);
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(t));
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }

    private void favoriteItem() {
        String id = mImgurObject.getId();
        Call<BasicResponse> call;

        if (mImgurObject instanceof ImgurPhoto) {
            call = ApiClient.getService().favoriteImage(id, id);
        } else {
            call = ApiClient.getService().favoriteAlbum(id, id);
        }

        call.enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().success) {
                    mImgurObject.setIsFavorite(!mImgurObject.isFavorited());
                    getActivity().invalidateOptionsMenu();
                } else {
                    Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to favorite item", t);
                Snackbar.make(mMultiView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    void reportItem(int reason) {
        ApiClient.getService().reportPost(mImgurObject.getId(), reason).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().data) {
                    Snackbar.make(mMultiView, R.string.report_post_success, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(mMultiView, R.string.report_post_failure, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Error reporting post", t);
                Snackbar.make(mMultiView, R.string.report_post_failure, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSION_WRITE:
                boolean granted = PermissionUtils.verifyPermissions(grantResults);

                if (granted) {
                    // Kick off the download if only one photo
                    if (mImgurObject instanceof ImgurPhoto) {
                        String link = getLink((ImgurPhoto) mImgurObject);
                        getActivity().startService(DownloaderService.createIntent(getActivity(), link));
                    } else {
                        Snackbar.make(mMultiView, R.string.permission_granted, Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(mMultiView, R.string.permission_denied, Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.FULL_SCREEN_VIEW && resultCode == Activity.RESULT_OK) {
            int endingPosition = data != null ? data.getIntExtra(FullScreenPhotoActivity.KEY_ENDING_POSITION, -1) : -1;
            // Pad the ending position to account for the header
            if (endingPosition >= 0 && mListView != null) mListView.scrollToPosition(endingPosition + 1);
        }
    }
}
