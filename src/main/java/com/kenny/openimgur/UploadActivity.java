package com.kenny.openimgur;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.fragments.PopupDialogViewBuilder;
import com.kenny.openimgur.ui.SnackBar;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
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

    private static final int MSG_SEARCH_URL = 11;

    private File mTempFile = null;

    private File mCameraFile = null;

    private ImageView mPreviewImage;

    private EditText mTitle;

    private EditText mDesc;

    private EditText mLink;

    private CheckBox mGalleryCB;

    private boolean mIsValidLink = false;

    private LoadingDialogFragment mLoadingDialog;

    public static Intent createIntent(Context context, int uploadType) {
        return new Intent(context, UploadActivity.class).putExtra(KEY_UPLOAD_TYPE, uploadType);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        getActionBar().setTitle(R.string.upload);
        mPreviewImage = (ImageView) findViewById(R.id.previewImage);
        mTitle = (EditText) findViewById(R.id.title);
        mDesc = (EditText) findViewById(R.id.desc);
        mLink = (EditText) findViewById(R.id.url);
        mGalleryCB = (CheckBox) findViewById(R.id.galleryUpload);
        int uploadType = UPLOAD_TYPE_LINK;
        if (getIntent().getExtras() != null) {
            uploadType = getIntent().getExtras().getInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);
        }

        if (uploadType != UPLOAD_TYPE_LINK) {
            int requestCode = uploadType == UPLOAD_TYPE_GALLERY ? REQUEST_CODE_GALLERY : REQUEST_CODE_CAMERA;
            startActivityForResult(createPhotoIntent(uploadType), requestCode);
        }

        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload:
                if (app.getUser() == null) {
                    final AlertDialog dialog = new AlertDialog.Builder(UploadActivity.this).create();
                    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

                    dialog.setView(new PopupDialogViewBuilder(getApplicationContext()).setTitle(R.string.not_logged_in)
                            .setMessage(R.string.not_logged_in_msg).setNegativeButton(R.string.cancel, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.dismiss();
                                }
                            }).setPositiveButton(R.string.yes, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    upload(mTitle.getText().toString(), mDesc.getText().toString(), false);
                                }
                            }).build());
                } else {
                    String title = mTitle.getText().toString();
                    String desc = mDesc.getText().toString();

                    if (mGalleryCB.isChecked() && TextUtils.isEmpty(title)) {
                        // Shake the edit text to show that they have not enetered any text
                        ObjectAnimator.ofFloat(mTitle, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0).setDuration(750L).start();
                        return false;
                    }

                    upload(title, desc, mGalleryCB.isChecked());
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int status = event.json.getInt(ApiClient.KEY_STATUS);

            switch (event.eventType) {
                case UPLOAD:
                    if (status == ApiClient.STATUS_OK) {
                        // If our id is true, that means we need to upload the resulting id to the gallery
                        if (event.id.equals(String.valueOf(true))) {

                        } else {

                        }

                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
                    }

                    break;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        }
    }

    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        event.getThrowable().printStackTrace();
    }

    /**
     * Uploads the image/url to Imgur
     *
     * @param title           Optional title for image. Required if uploading to gallery
     * @param description     Option description for image
     * @param uploadToGallery If the image should be uploaded to the gallery
     */
    private void upload(@Nullable String title, @Nullable String description, boolean uploadToGallery) {
        mLoadingDialog = LoadingDialogFragment.createInstance(R.string.uploading);
        getFragmentManager().beginTransaction().add(mLoadingDialog, "uploading");
        ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);

        // Link upload
        if (mIsValidLink) {
            FormEncodingBuilder builder = new FormEncodingBuilder()
                    .add("image", mLink.getText().toString())
                    .add("type", "URL");

            if (!TextUtils.isEmpty(title)) {
                builder.add("title", title);
            }

            if (!TextUtils.isEmpty(description)) {
                builder.add("description", description);
            }

            RequestBody body = builder.build();
            client.doWork(ImgurBusEvent.EventType.UPLOAD, String.valueOf(uploadToGallery), body);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Hide the upload button if there is no image ready for upload
        menu.findItem(R.id.upload).setVisible(!(!mIsValidLink && mTempFile == null && mCameraFile == null));
        return super.onPrepareOptionsMenu(menu);
    }

    private void init() {
        mLink.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // Only when these files are null will we listen for the text change
                mIsValidLink = false;
                invalidateOptionsMenu();
                if (mTempFile == null && mCameraFile == null && !TextUtils.isEmpty(charSequence)) {
                    mHandler.removeMessages(MSG_SEARCH_URL);
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_SEARCH_URL, charSequence.toString()), TEXT_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mGalleryCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                // Don't allow uploads to gallery when they are not logged in
                if (checked && app.getUser() == null) {
                    SnackBar.show(UploadActivity.this, R.string.gallery_no_user);
                    button.setChecked(false);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            LoadImageTask task;

            switch (requestCode) {
                case REQUEST_CODE_CAMERA:
                    if (FileUtil.isFileValid(mCameraFile)) {
                        task = new LoadImageTask();
                        task.displayLoadingFragment();
                        FileUtil.scanFile(Uri.fromFile(mCameraFile), getApplicationContext());
                        task.execute(mCameraFile);
                    } else {
                        // TODO Error
                    }

                    break;

                case REQUEST_CODE_GALLERY:
                    task = new LoadImageTask();
                    task.displayLoadingFragment();
                    mTempFile = FileUtil.createFile(data.getData(), getContentResolver());

                    if (FileUtil.isFileValid(mTempFile)) {
                        if (mTempFile.getAbsolutePath().endsWith(".gif")) {
                            try {
                                mPreviewImage.setImageDrawable(new GifDrawable(mTempFile));
                            } catch (IOException ex) {
                                Log.e(TAG, "Unable to play gif, falling back to still image", ex);
                                // Load the image without playing it if the gif fails
                                task.execute(mTempFile);
                            }
                        } else {
                            task.execute(mTempFile);
                        }

                    } else {
                        task.dismissDialog();
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
        mTitle = null;
        mDesc = null;
        mLink = null;
        mGalleryCB = null;
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
                mCameraFile = FileUtil.createFile(FileUtil.EXTENSION_JPEG);

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
                case MSG_SEARCH_URL:
                    app.getImageLoader().loadImage((String) msg.obj, new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String s, View view) {

                        }

                        @Override
                        public void onLoadingFailed(String s, View view, FailReason failReason) {
                            mIsValidLink = false;
                            mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                            SnackBar.show(UploadActivity.this, R.string.invalid_url);
                            invalidateOptionsMenu();
                        }

                        @Override
                        public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                            mIsValidLink = true;

                            if (url.endsWith(".gif")) {
                                if (!ImageUtil.loadAndDisplayGif(mPreviewImage, url, app.getImageLoader())) {
                                    mPreviewImage.setImageBitmap(bitmap);
                                }
                            } else {
                                mPreviewImage.setImageBitmap(bitmap);
                            }

                            invalidateOptionsMenu();
                        }

                        @Override
                        public void onLoadingCancelled(String s, View view) {
                            mIsValidLink = false;
                            mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                            SnackBar.show(UploadActivity.this, R.string.invalid_url);
                            invalidateOptionsMenu();
                        }
                    });
                    break;

                case MESSAGE_ACTION_COMPLETE:
                    break;

                case MESSAGE_ACTION_FAILED:
                    SnackBar.show(UploadActivity.this, (Integer) msg.obj);
                    break;
            }
        }
    };

    /**
     * Decodes the selected image in the background for displaying
     */
    private class LoadImageTask extends AsyncTask<File, Void, Bitmap> {
        LoadingDialogFragment fragment;

        private String rotatedFilePath;

        @Override
        protected Bitmap doInBackground(File... files) {
            try {
                File file = files[0];

                if (!FileUtil.isFileValid(file)) {
                    Log.w(TAG, "Invalid file trying to be decoded");
                    return null;
                }

                rotatedFilePath = checkExifData(file);
                return ImageUtil.decodeSampledBitmapFromResource(!TextUtils.isEmpty(rotatedFilePath) ? new File(rotatedFilePath) : file, 1024, 1024);
            } catch (Exception e) {
                Log.e(TAG, "Error decoding Image", e);
            }

            return null;
        }

        /**
         * Cheks the EXIF data of the file and rotates and saves it accordingly.
         *
         * @param file File to check
         * @return The new path of the rotated file. Null if no rotation is needed
         */
        private String checkExifData(File file) {
            int rotation = ImageUtil.getImageRotation(file);
            File rotationFile;
            String newPath = null;

            if (rotation != ExifInterface.ORIENTATION_NORMAL && rotation != ExifInterface.ORIENTATION_UNDEFINED) {
                Log.v(TAG, "EXIF Rotation detected, rotating image");
                // Down Sample the images to not take up a lot of memory
                Bitmap bitmap = ImageUtil.decodeSampledBitmapFromResource(file, 1024, 1024);
                Matrix matrix = new Matrix();

                switch (rotation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;

                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(-90);
                        break;
                }

                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                rotationFile = FileUtil.createFile(FileUtil.EXTENSION_JPEG);
                if (!ImageUtil.saveBitmapToFile(bitmap, rotationFile)) {
                    // Delete the rotation file if it fails to save and null it out
                    if (FileUtil.isFileValid(rotationFile)) {
                        rotationFile.delete();
                    }
                } else {
                    newPath = rotationFile.getAbsolutePath();
                }

                bitmap.recycle();
            }

            return newPath;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mPreviewImage.setImageBitmap(bitmap);
                mLink.setVisibility(View.GONE);

                if (!TextUtils.isEmpty(rotatedFilePath)) {
                    // Set whichever file is currently in use to the newly rotated image
                    if (FileUtil.isFileValid(mCameraFile)) {
                        mCameraFile.delete();
                        mCameraFile = new File(rotatedFilePath);
                    } else if (FileUtil.isFileValid(mTempFile)) {
                        mTempFile.delete();
                        mTempFile = new File(rotatedFilePath);
                    }
                }
            } else {
                // Delete any of the files if an error occurred
                if (FileUtil.isFileValid(mCameraFile)) {
                    mCameraFile.delete();
                }

                if (FileUtil.isFileValid(mTempFile)) {
                    mTempFile.delete();
                }

                if (!TextUtils.isEmpty(rotatedFilePath)) {
                    File f = new File(rotatedFilePath);

                    if (FileUtil.isFileValid(f)) {
                        f.delete();
                    }
                }
            }

            invalidateOptionsMenu();
            dismissDialog();
        }

        public void displayLoadingFragment() {
            fragment = LoadingDialogFragment.createInstance(R.string.decoding_image);
            getFragmentManager().beginTransaction().add(fragment, "decoding").commit();
        }

        public void dismissDialog() {
            if (fragment != null) {
                fragment.dismiss();
                fragment = null;
            }

        }
    }
}
