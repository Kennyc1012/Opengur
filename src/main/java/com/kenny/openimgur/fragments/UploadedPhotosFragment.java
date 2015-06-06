package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CheckBox;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.FullScreenPhotoActivity;
import com.kenny.openimgur.activities.UploadActivity;
import com.kenny.openimgur.adapters.UploadAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.UploadedPhoto;
import com.kenny.openimgur.ui.HeaderGridView;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;

import java.util.List;

import butterknife.InjectView;

/**
 * Created by Kenny-PC on 1/14/2015.
 */
public class UploadedPhotosFragment extends BaseFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener {
    @InjectView(R.id.multiView)
    public MultiStateView mMultiStateView;

    @InjectView(R.id.grid)
    public HeaderGridView mGrid;

    private FragmentListener mListener;

    private UploadAdapter mAdapter;

    private int mPreviousItem = 0;

    public static Fragment createInstance() {
        return new UploadedPhotosFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof FragmentListener) {
            mListener = (FragmentListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_uploads, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mListener != null)
            mListener.onUpdateActionBarTitle(getString(R.string.uploaded_photos_title));
        mGrid.setOnItemClickListener(this);
        mGrid.setOnItemLongClickListener(this);
        mGrid.setOnScrollListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) {
            List<UploadedPhoto> photos = app.getSql().getUploadedPhotos(true);

            if (!photos.isEmpty()) {
                mAdapter = new UploadAdapter(getActivity(), photos);
                mGrid.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                mGrid.setAdapter(mAdapter);
                mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
            } else {
                mMultiStateView.setViewState(MultiStateView.ViewState.EMPTY);
                if (mListener != null) mListener.onUpdateActionBar(true);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;

        if (adapterPosition >= 0) {
            UploadedPhoto photo = mAdapter.getItem(adapterPosition);
            startActivity(FullScreenPhotoActivity.createIntent(getActivity(), photo.getUrl()));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;

        if (adapterPosition >= 0) {
            final UploadedPhoto photo = mAdapter.getItem(adapterPosition);

            new AlertDialog.Builder(getActivity(), theme.getDialogTheme())
                    .setItems(R.array.uploaded_photos_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 0. Share 1. Copy Link 2. Delete

                            switch (which) {
                                case 0:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, photo.getUrl());

                                    if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(Intent.createChooser(shareIntent, getString(R.string.send_feedback)));
                                    } else {
                                        SnackBar.show(getActivity(), R.string.cant_launch_intent);
                                    }
                                    break;

                                case 1:
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", photo.getUrl()));
                                    SnackBar.show(getActivity(),R.string.link_copied);
                                    break;

                                case 2:
                                    View deleteView = LayoutInflater.from(getActivity()).inflate(R.layout.upload_delete_confirm, null);
                                    final CheckBox cb = (CheckBox) deleteView.findViewById(R.id.imgurDelete);

                                    new AlertDialog.Builder(getActivity(), theme.getDialogTheme())
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    if (cb.isChecked()) {
                                                        String url = String.format(Endpoints.IMAGE_DELETE.getUrl(), photo.getDeleteHash());
                                                        // We don't care about any conformation here
                                                        new ApiClient(url, ApiClient.HttpRequest.DELETE).doWork(null, null, null);
                                                    }

                                                    app.getSql().deleteUploadedPhoto(photo);
                                                    mAdapter.removeItem(photo);

                                                    if (mAdapter.isEmpty()) {
                                                        mMultiStateView.setViewState(MultiStateView.ViewState.EMPTY);
                                                        if (mListener != null)
                                                            mListener.onUpdateActionBar(true);
                                                    }
                                                }
                                            })
                                            .setView(deleteView)
                                            .show();
                            }
                        }
                    }).show();
            return true;
        }
        return false;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // NOOP
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Hide the actionbar when scrolling down, show when scrolling up
        if (firstVisibleItem > mPreviousItem && mListener != null) {
            mListener.onUpdateActionBar(false);
        } else if (firstVisibleItem < mPreviousItem && mListener != null) {
            mListener.onUpdateActionBar(true);
        }

        mPreviousItem = firstVisibleItem;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == UploadActivity.REQUEST_CODE) {
            // A new photo was uploaded, refresh the data
            mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
            List<UploadedPhoto> photos = app.getSql().getUploadedPhotos(true);

            if (mAdapter != null) {
                mAdapter.clear();
                mAdapter.addItems(photos);
            } else {
                mAdapter = new UploadAdapter(getActivity(), photos);
                mGrid.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                mGrid.setAdapter(mAdapter);
            }

            mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
