package com.kenny.openimgur.fragments;

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
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient2;
import com.kenny.openimgur.classes.ImgurBaseObject2;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.snackbar.SnackBar;

import java.util.ArrayList;

/**
 * Created by kcampagna on 12/27/14.
 */
public class ProfileUploadsFragment extends BaseGridFragment2 implements AdapterView.OnItemLongClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (user == null) {
            throw new IllegalStateException("User is not logged in");
        }
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
        ApiClient2.getService().getProfileUploads(user.getUsername(), mCurrentPage, this);
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject2> items) {
        // TODO
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;

        if (adapterPosition >= 0) {
            final ImgurBaseObject2 photo = getAdapter().getItem(adapterPosition);

            new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                    .setItems(R.array.uploaded_photos_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 0. Share 1. Copy Link 2. Delete

                            switch (which) {
                                case 0:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, photo.getLink());

                                    if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(Intent.createChooser(shareIntent, getString(R.string.send_feedback)));
                                    } else {
                                        SnackBar.show(getActivity(), R.string.cant_launch_intent);
                                    }
                                    break;

                                case 1:
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", photo.getLink()));
                                    break;

                                case 2:
                                    new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                                            .setMessage(R.string.profile_delete_image)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
                                                    // TODO Delete Call
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
    protected void onEmptyResults() {
        mHasMore = false;
        mIsLoading = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
            String errorMessage = getString(R.string.profile_no_uploads);
            mMultiStateView.setErrorText(R.id.errorMessage, errorMessage);
            mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
        }
    }

    @Override
    protected boolean showPoints() {
        return false;
    }
}
