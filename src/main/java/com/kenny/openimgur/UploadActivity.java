package com.kenny.openimgur;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.kenny.openimgur.util.FileUtil;

import java.io.File;

/**
 * Created by kcampagna on 8/2/14.
 */
public class UploadActivity extends BaseActivity {
    public static final String KEY_UPLOAD_TYPE = "upload_type";

    public static final int UPLOAD_TYPE_CAMERA = 0;

    public static final int UPLOAD_TYPE_GALLERY = 1;

    public static final int UPLOAD_TYPE_LINK = 2;

    private static final int REQUEST_CODE_CAMERA = 123;

    private static final int REQUEST_CODE_GALLERY = 321;

    private File mTempFile = null;

    private File mCameraFile = null;

    public static Intent createIntent(Context context, int uploadType) {
        return new Intent(context, UploadActivity.class).putExtra(KEY_UPLOAD_TYPE, uploadType);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        ActionBar ab = getActionBar();
        ab.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowHomeEnabled(true);

        if (getIntent().getExtras() != null) {
            int uploadType = getIntent().getExtras().getInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);

            if (uploadType != UPLOAD_TYPE_LINK) {
                int requestCode = uploadType == UPLOAD_TYPE_GALLERY ? REQUEST_CODE_GALLERY : REQUEST_CODE_CAMERA;
                Intent intent = createPhotoIntent(uploadType);

                if (intent != null) {
                    startActivityForResult(intent, requestCode);
                } else {
                    // TODO Error
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_CAMERA:
                    if (mCameraFile != null && mCameraFile.exists()) {

                    } else {
                        // TODO Error
                    }

                    break;

                case REQUEST_CODE_GALLERY:
                    mTempFile = FileUtil.createFile(data.getData(), getContentResolver());

                    if (mTempFile != null && mTempFile.exists()) {

                    } else {
                        // TODO Error
                    }

                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Delete the temporary file if one exists
        if (mTempFile != null && mTempFile.exists()) {
            mTempFile.delete();
        }

        super.onDestroy();
    }

    /**
     * Creates the intent for the given uploadType
     *
     * @param uploadType
     * @return
     */
    private Intent createPhotoIntent(int uploadType) {
        Intent intent = null;
        switch (uploadType) {
            case UPLOAD_TYPE_CAMERA:
                mCameraFile = FileUtil.createFile();

                if (mCameraFile != null) {
                    intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(mCameraFile));
                }

                break;

            case UPLOAD_TYPE_GALLERY:
            default:
                intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                break;
        }

        return intent;
    }
}
