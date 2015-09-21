package com.kenny.openimgur.fragments;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.kenny.openimgur.adapters.PhotoAdapter;
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
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarItem;
import com.kenny.snackbar.SnackBarListener;
import com.kennyc.bottomsheet.BottomSheet;
import com.kennyc.view.MultiStateView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
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

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mListView;

    private ImgurBaseObject mImgurObject;

    private PhotoAdapter mPhotoAdapter;

    private boolean mDisplayTags = true;

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
                    SnackBar.show(getActivity(), R.string.link_copied);
                }
                break;

            case R.id.favorite:
                if (user != null) {
                    favoriteItem();
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
                BottomSheet shareDialog = BottomSheet.createShareBottomSheet(getActivity(), mImgurObject.getShareIntent(), R.string.share);
                if (shareDialog != null) {
                    shareDialog.show();
                } else {
                    SnackBar.show(getActivity(), R.string.cant_launch_intent);
                }
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
                    SnackBar.show(getActivity(), R.string.user_not_logged_in);
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
    private void setupFragmentWithObject(ImgurBaseObject object) {
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

        super.onDestroyView();
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
                            ImgurPhoto photo = mPhotoAdapter.getItem(position);
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
                                    if (checkPermissions()) getActivity().startService(DownloaderService.createIntent(getActivity(), link));
                                    break;

                                // Share
                                case 2:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, link);
                                    BottomSheet shareDialog = BottomSheet.createShareBottomSheet(getActivity(), shareIntent, R.string.share);

                                    if (shareDialog != null) {
                                        shareDialog.show();
                                    } else {
                                        SnackBar.show(getActivity(), R.string.cant_launch_intent);
                                    }
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mPhotoAdapter.retainItems());
        }

        outState.putBoolean(KEY_DISPLAY_TAGS, mDisplayTags);
        outState.putParcelable(KEY_IMGUR_OBJECT, mImgurObject);
    }

    private boolean checkPermissions() {
        @PermissionUtils.PermissionLevel int permissionLevel = PermissionUtils.getPermissionLevel(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        switch (permissionLevel) {
            case PermissionUtils.PERMISSION_AVAILABLE:
                return true;

            case PermissionUtils.PERMISSION_DENIED:
                new SnackBarItem.Builder(getActivity())
                        .setMessageResource(R.string.permission_rationale_download)
                        .setActionMessageResource(R.string.okay)
                        .setAutoDismiss(false)
                        .setSnackBarListener(new SnackBarListener() {
                            @Override
                            public void onSnackBarStarted(Object o) {
                                // NOOP
                            }

                            @Override
                            public void onSnackBarFinished(Object o, boolean actionClicked) {
                                if (actionClicked) {
                                    FragmentCompat.requestPermissions(ImgurViewFragment.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSIONS);
                                } else {
                                    SnackBar.show(getActivity(), R.string.permission_denied);
                                }
                            }
                        }).show();
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
            default:
                FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSIONS);
                break;
        }

        return false;
    }

    private void downloadAlbum() {
        if (mImgurObject instanceof ImgurAlbum) {
            // TODO Check for permission for Marshmallow
            if (mPhotoAdapter != null && !mPhotoAdapter.isEmpty()) {
                ArrayList<String> urls = new ArrayList<>(mPhotoAdapter.getItemCount());

                for (ImgurPhoto p : mPhotoAdapter.retainItems()) {
                    urls.add(p.getLink());
                }

                getActivity().startService(DownloaderService.createIntent(getActivity(), urls));
            } else {
                SnackBar.show(getActivity(), R.string.error_generic);
            }
        } else {
            LogUtil.w(TAG, "Item is not an album");
        }
    }

    private void fetchAlbumImages() {
        ApiClient.getService().getAlbumImages(mImgurObject.getId(), new Callback<AlbumResponse>() {
            @Override
            public void success(AlbumResponse albumResponse, Response response) {
                if (!isAdded()) return;

                if (albumResponse != null && !albumResponse.data.isEmpty()) {
                    ((ImgurAlbum) mImgurObject).addPhotosToAlbum(albumResponse.data);
                    mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(), mImgurObject, ImgurViewFragment.this);
                    mListView.setAdapter(mPhotoAdapter);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    fetchTags();
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch album images", error);
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(error));
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }

    private void fetchTags() {
        // No need to request if the object already has tags, they will be set in the adapter
        if (mDisplayTags && (mImgurObject.getTags() == null || mImgurObject.getTags().isEmpty())) {
            ApiClient.getService().getTags(mImgurObject.getId(), new Callback<TagResponse>() {
                @Override
                public void success(TagResponse tagResponse, Response response) {
                    if (!isAdded() || tagResponse == null) return;

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

    private void fetchGalleryDetails() {
        ApiClient.getService().getGalleryDetails(mImgurObject.getId(), new Callback<BasicObjectResponse>() {
            @Override
            public void success(BasicObjectResponse basicObjectResponse, Response response) {
                if (!isAdded()) return;

                if (basicObjectResponse != null && basicObjectResponse.data != null) {
                    setupFragmentWithObject(basicObjectResponse.data);
                } else {
                    ViewUtils.setErrorText(mMultiView, R.id.errorMessage, R.string.error_generic);
                    mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch gallery details", error);
                ViewUtils.setErrorText(mMultiView, R.id.errorMessage, ApiClient.getErrorCode(error));
                mMultiView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }

    private void favoriteItem() {
        String id = mImgurObject.getId();
        Callback<BasicResponse> cb = new Callback<BasicResponse>() {
            @Override
            public void success(BasicResponse basicResponse, Response response) {
                if (!isAdded()) return;

                if (basicResponse != null && basicResponse.success) {
                    mImgurObject.setIsFavorite(!mImgurObject.isFavorited());
                    getActivity().invalidateOptionsMenu();
                } else {
                    SnackBar.show(getActivity(), R.string.error_generic);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to favorite item", error);
                SnackBar.show(getActivity(), R.string.error_generic);
            }
        };

        if (mImgurObject instanceof ImgurPhoto) {
            ApiClient.getService().favoriteImage(id, id, cb);
        } else {
            ApiClient.getService().favoriteAlbum(id, id, cb);
        }
    }

    private void reportItem(int reason) {
        ApiClient.getService().reportPost(mImgurObject.getId(), reason, new Callback<BasicResponse>() {
            @Override
            public void success(BasicResponse basicResponse, Response response) {
                if (!isAdded()) return;

                if (basicResponse != null && basicResponse.data) {
                    SnackBar.show(getActivity(), R.string.report_post_success);
                } else {
                    SnackBar.show(getActivity(), R.string.report_post_failure);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                SnackBar.show(getActivity(), R.string.report_post_failure);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSIONS:
                boolean granted = PermissionUtils.verifyPermissions(grantResults);
                int message = granted ? R.string.permission_granted : R.string.permission_denied;
                SnackBar.show(getActivity(), message);
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
