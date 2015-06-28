package com.kenny.openimgur.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.UploadPhotoAdapter;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.fragments.UploadLinkFragmentDialog;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.snackbar.SnackBar;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class UploadActivity2 extends BaseActivity implements UploadLinkFragmentDialog.LinkListener {
    private static final int REQUEST_CODE_CAMERA = 123;

    private static final int REQUEST_CODE_GALLERY = 321;

    @InjectView(R.id.multiView)
    MultiStateView mMultiView;

    @InjectView(R.id.list)
    RecyclerView mRecyclerView;

    private UploadPhotoAdapter mAdapter;

    private File mTempFile;

    public static Intent createIntent(Context context) {
        return new Intent(context, UploadActivity2.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload2);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    @OnClick({R.id.cameraBtn, R.id.linkBtn, R.id.galleryBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cameraBtn:
                mTempFile = FileUtil.createFile(FileUtil.EXTENSION_JPEG);

                if (FileUtil.isFileValid(mTempFile)) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempFile));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivityForResult(intent, REQUEST_CODE_CAMERA);
                    } else {
                        SnackBar.show(this, R.string.cant_launch_intent);
                    }
                }
                break;

            case R.id.galleryBtn:
                startActivityForResult(PhotoPickerActivity.createInstance(getApplicationContext()), REQUEST_CODE_GALLERY);
                break;

            case R.id.linkBtn:
                getFragmentManager().beginTransaction()
                        .add(new UploadLinkFragmentDialog(), UploadLinkFragmentDialog.TAG)
                        .commit();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(PhotoPickerActivity.KEY_PHOTOS)) {
                    List<String> photos = data.getStringArrayListExtra(PhotoPickerActivity.KEY_PHOTOS);
                    List<Upload> uploads = new ArrayList<>(photos.size());

                    for (String s : photos) {
                        uploads.add(new Upload(s));
                    }

                    if (mAdapter == null) {
                        mAdapter = new UploadPhotoAdapter(this, uploads);
                        mRecyclerView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(uploads);
                    }

                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                }
                break;

            case REQUEST_CODE_CAMERA:
                if (resultCode == Activity.RESULT_OK && FileUtil.isFileValid(mTempFile)) {
                    String fileLocation = mTempFile.getAbsolutePath();
                    Upload upload = new Upload(fileLocation);

                    if (mAdapter == null) {
                        List<Upload> uploadList = new ArrayList<>(1);
                        uploadList.add(upload);
                        mAdapter = new UploadPhotoAdapter(this, uploadList);
                        mRecyclerView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItem(upload);
                    }

                    mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                    mTempFile = null;
                } else {
                    SnackBar.show(this, R.string.upload_camera_error);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
    }

    @Override
    public void onLinkAdded(String link) {
        Upload upload = new Upload(link, true);

        if (mAdapter == null) {
            List<Upload> uploadList = new ArrayList<>(1);
            uploadList.add(upload);
            mAdapter = new UploadPhotoAdapter(this, uploadList);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.addItem(upload);
        }

        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
    }
}
