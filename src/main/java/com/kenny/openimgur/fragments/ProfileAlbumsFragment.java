package com.kenny.openimgur.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Kenny-PC on 7/4/2015.
 */
public class ProfileAlbumsFragment extends BaseGridFragment implements AdapterView.OnItemLongClickListener {
    private static final String KEY_USER = "user";

    private ImgurUser mSelectedUser;

    public static ProfileAlbumsFragment createInstance(@NonNull ImgurUser user) {
        ProfileAlbumsFragment fragment = new ProfileAlbumsFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(KEY_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGrid.setOnItemLongClickListener(this);
    }

    @Override
    protected void fetchGallery() {
        super.fetchGallery();
        ApiClient.getService().getProfileAlbums(mSelectedUser.getUsername(), mCurrentPage, this);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;

        if (adapterPosition >= 0) {
            final ImgurBaseObject album = getAdapter().getItem(adapterPosition);
            String[] options = getResources().getStringArray(mSelectedUser.isSelf(app) ? R.array.uploaded_photos_options : R.array.uploaded_albums_options_not_self);

            new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 0. Share 1. Copy Link 2. Delete

                            switch (which) {
                                case 0:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, album.getLink());

                                    if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(Intent.createChooser(shareIntent, getString(R.string.send_feedback)));
                                    } else {
                                        SnackBar.show(getActivity(), R.string.cant_launch_intent);
                                    }
                                    break;

                                case 1:
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", album.getLink()));
                                    break;

                                case 2:
                                    new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                                            .setMessage(R.string.profile_delete_album)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    deleteAlbum(album);
                                                }
                                            }).show();
                                    break;
                            }
                        }
                    }).show();
            return true;
        }

        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_USER, mSelectedUser);
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);
        if (savedInstanceState != null) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
        } else {
            mSelectedUser = getArguments().getParcelable(KEY_USER);
        }

        if (mSelectedUser == null)
            throw new IllegalArgumentException("Profile must be supplied to fragment");
    }

    @Override
    protected int getAdditionalHeaderSpace() {
        return getResources().getDimensionPixelSize(R.dimen.tab_bar_height);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && mGrid != null && mGrid.getFirstVisiblePosition() <= 1 && mListener != null) {
            mListener.onUpdateActionBar(true);
        }
    }

    @Override
    public void success(GalleryResponse galleryResponse, Response response) {
        super.success(galleryResponse, response);
        if (mSelectedUser.isSelf(app)) mHasMore = false;
    }

    @Override
    protected void onEmptyResults() {
        mHasMore = false;
        mIsLoading = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
            String errorMessage = getString(R.string.profile_no_albums, mSelectedUser.getUsername());
            mMultiStateView.setErrorText(R.id.errorMessage, errorMessage);
            mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
        }
    }

    private void deleteAlbum(final ImgurBaseObject album) {
        mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);

        ApiClient.getService().deleteAlbum(album.getDeleteHash(), new Callback<BasicResponse>() {
            @Override
            public void success(BasicResponse basicResponse, Response response) {
                if (!isAdded()) return;

                if (basicResponse != null && basicResponse.data) {
                    GalleryAdapter adapter = getAdapter();

                    if (adapter != null) {
                        adapter.removeItem(album);
                    }

                    if (adapter == null || adapter.isEmpty()) {
                        onEmptyResults();
                    } else {
                        mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
                    }

                    SnackBar.show(getActivity(), R.string.profile_delete_success_album);
                } else {
                    SnackBar.show(getActivity(), R.string.error_generic);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to delete Album", error);
                SnackBar.show(getActivity(), R.string.error_generic);
                mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
            }
        });

    }
}
