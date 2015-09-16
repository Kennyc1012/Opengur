package com.kenny.openimgur.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MemeActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.snackbar.SnackBar;

import org.apache.commons.collections15.list.SetUniqueList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import retrofit.client.Response;

/**
 * Created by Kenny-PC on 3/7/2015.
 */
public class MemeFragment extends BaseGridFragment {

    @Nullable
    private ImgurBaseObject mSelectedItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mListener != null) mListener.onUpdateActionBarTitle(getString(R.string.meme_gen));
    }

    @Override
    public void onDestroyView() {
        mSelectedItem = null;
        super.onDestroyView();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.meme_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (getAdapter() != null) getAdapter().clear();
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchGallery();
                break;

            case R.id.importPhoto:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(intent, RequestCodes.SELECT_PHOTO);
                } else {
                    SnackBar.show(getActivity(), R.string.cant_launch_intent);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void fetchGallery() {
        super.fetchGallery();
        ApiClient.getService().getDefaultMemes(this);
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        mSelectedItem = items.get(position);
        int level = PermissionUtils.getPermissionLevel(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        switch (level) {
            case PermissionUtils.PERMISSION_AVAILABLE:
                startActivity(MemeActivity.createIntent(getActivity(), mSelectedItem));
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
                FragmentCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSIONS);
                break;

            case PermissionUtils.PERMISSION_DENIED:
                new AlertDialog.Builder(getActivity(), app.getImgurTheme().getAlertDialogTheme())
                        .setTitle(R.string.permission_title)
                        .setMessage(R.string.permission_rational_meme)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                FragmentCompat.requestPermissions(MemeFragment.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSIONS);
                            }
                        }).show();
                break;
        }
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);

        if (getAdapter() == null || getAdapter().isEmpty()) {
            List<ImgurBaseObject> memes = app.getSql().getMemes();

            if (memes != null && !memes.isEmpty()) {
                LogUtil.v(TAG, "Memes found in database");
                setUpGridTop();
                setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(memes), showPoints()));
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                mHasMore = false;
            }
        }
    }

    @Override
    public void success(GalleryResponse galleryResponse, Response response) {
        super.success(galleryResponse, response);
        mHasMore = false;

        if (galleryResponse != null && !galleryResponse.data.isEmpty()) {
            app.getSql().deleteMemes();
            app.getSql().addMemes(galleryResponse.data);
        }
    }

    @Override
    protected boolean showPoints() {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.SELECT_PHOTO && resultCode == Activity.RESULT_OK) {
            File file = FileUtil.createFile(data.getData(), getActivity().getContentResolver());

            if (FileUtil.isFileValid(file)) {
                startActivity(MemeActivity.createIntent(getActivity(), file));
            } else {
                SnackBar.show(getActivity(), R.string.upload_decode_failure);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSIONS:
                boolean hasPermission = PermissionUtils.verifyPermissions(grantResults);

                if (hasPermission) {
                    if (mSelectedItem != null) startActivity(MemeActivity.createIntent(getActivity(), mSelectedItem));
                } else {
                    SnackBar.show(getActivity(), R.string.permission_denied);
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
