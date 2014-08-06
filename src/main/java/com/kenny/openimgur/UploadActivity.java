package com.kenny.openimgur;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;
import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

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

    private static final long TEXT_DELAY = 1000L;

    private static final int MESSSGE_SEARCH_URL = 11;

    private File mTempFile = null;

    private File mCameraFile = null;

    private ImageView mPreviewImage;

    private FloatingActionButton mUploadBtn;

    // Will double as the url edit text as well
    private EditText mTitle;

    private EditText mDesc;

    private boolean mIsValidLink = false;

    public static Intent createIntent(Context context, int uploadType) {
        return new Intent(context, UploadActivity.class).putExtra(KEY_UPLOAD_TYPE, uploadType);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        mPreviewImage = (ImageView) findViewById(R.id.previewImage);
        mTitle = (EditText) findViewById(R.id.title);
        mDesc = (EditText) findViewById(R.id.desc);
        mUploadBtn = (FloatingActionButton) findViewById(R.id.uploadBtn);
        mUploadBtn.setEnabled(false);

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

        init();
    }

    private void init() {
        mTitle.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // Only when these files are null will we listen for the text change
                mIsValidLink = false;

                if (mTempFile == null && mCameraFile == null && !TextUtils.isEmpty(charSequence)) {
                    mHandler.removeMessages(MESSSGE_SEARCH_URL);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSSGE_SEARCH_URL, charSequence.toString()), TEXT_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        mUploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_CAMERA:
                    if (FileUtil.isFileValid(mCameraFile)) {
                        FileUtil.scanFile(Uri.fromFile(mCameraFile), getApplicationContext());
                        Bitmap cameraBm = ImageUtil.decodeSampledBitmapFromResource(mCameraFile, getResources().getDisplayMetrics().widthPixels,
                                getResources().getDisplayMetrics().heightPixels / 3);

                        if (cameraBm != null) {
                            mPreviewImage.setImageBitmap(cameraBm);
                            mUploadBtn.setEnabled(true);
                        }

                    } else {
                        // TODO Error
                    }

                    break;

                case REQUEST_CODE_GALLERY:
                    mTempFile = FileUtil.createFile(data.getData(), getContentResolver());

                    if (FileUtil.isFileValid(mTempFile)) {
                        if (mTempFile.getAbsolutePath().endsWith(".gif")) {
                            try {
                                mPreviewImage.setImageDrawable(new GifDrawable(mTempFile));
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                                // TODO Error
                            }
                        } else {
                            Bitmap tempBm = ImageUtil.decodeSampledBitmapFromResource(mTempFile, getResources().getDisplayMetrics().widthPixels,
                                    getResources().getDisplayMetrics().widthPixels / 3);

                            if (tempBm != null) {
                                mPreviewImage.setImageBitmap(tempBm);
                                mUploadBtn.setEnabled(true);
                            } else {
                                // TODO Error
                            }
                        }

                    } else {
                        // TODO Error
                    }

                    break;
            }
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        // Delete the temporary file if one exists
        if (FileUtil.isFileValid(mTempFile)) {
            mTempFile.delete();
        }

        mPreviewImage = null;
        mUploadBtn = null;
        mTitle = null;
        mDesc = null;
        mHandler.removeCallbacksAndMessages(null);
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
                mCameraFile = FileUtil.createFile(".jpg");

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

    private ImgurHandler mHandler = new ImgurHandler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSSGE_SEARCH_URL:
                    app.getImageLoader().loadImage((String) msg.obj, new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String s, View view) {

                        }

                        @Override
                        public void onLoadingFailed(String s, View view, FailReason failReason) {
                            mUploadBtn.setEnabled(false);
                            mIsValidLink = true;
                            mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                            Toast.makeText(getApplicationContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                            mUploadBtn.setEnabled(true);
                            mIsValidLink = true;

                            if (!ImageUtil.loadAndDisplayGif(mPreviewImage, url, app.getImageLoader())) {
                                mPreviewImage.setImageBitmap(bitmap);
                            }
                        }

                        @Override
                        public void onLoadingCancelled(String s, View view) {
                            mIsValidLink = true;
                            mUploadBtn.setEnabled(false);
                            mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                            Toast.makeText(getApplicationContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;
            }
        }
    };
}
