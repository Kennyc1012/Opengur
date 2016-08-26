package com.kenny.openimgur.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Kenny-PC on 7/4/2015.
 */
public class ProfileAlbumsFragment extends BaseGridFragment implements View.OnLongClickListener {
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
    protected void fetchGallery() {
        super.fetchGallery();
        ApiClient.getService().getProfileAlbums(mSelectedUser.getUsername(), mCurrentPage).enqueue(this);
    }

    @Override
    protected void setAdapter(GalleryAdapter adapter) {
        super.setAdapter(adapter);
        adapter.setOnLongClickPressListener(this);
    }

    @Override
    public boolean onLongClick(View v) {
        final ImgurBaseObject album = getAdapter().getItem(mGrid.getChildAdapterPosition(v));
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
                                share(shareIntent, R.string.share);
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
    protected void onApiResult(GalleryResponse galleryResponse) {
        super.onApiResult(galleryResponse);
        if (mSelectedUser.isSelf(app)) mHasMore = false;
    }

    @Override
    protected void onEmptyResults() {
        mHasMore = false;
        mIsLoading = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
            String errorMessage = getString(R.string.profile_no_albums, mSelectedUser.getUsername());
            ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, errorMessage);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
        }
    }

    void deleteAlbum(final ImgurBaseObject album) {
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

        ApiClient.getService().deleteAlbum(album.getDeleteHash()).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && response.body().data) {
                    GalleryAdapter adapter = getAdapter();

                    if (adapter != null) {
                        adapter.removeItem(album);
                    }

                    if (adapter == null || adapter.isEmpty()) {
                        onEmptyResults();
                    } else {
                        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    }

                    Snackbar.make(mMultiStateView, R.string.profile_delete_success_album, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(mMultiStateView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to delete Album", t);
                Snackbar.make(mMultiStateView, R.string.error_generic, Snackbar.LENGTH_LONG).show();
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }
        });

    }
}
