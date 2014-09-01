package com.kenny.openimgur;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
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
import android.util.DisplayMetrics;
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
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.fragments.PopupDialogViewBuilder;
import com.kenny.openimgur.ui.SnackBar;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
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
    private static final MediaType MEDIA_TYPE_PNG = MediaType.parse("image/png");

    private static final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");

    private static final MediaType MEDIA_TYPE_GIF = MediaType.parse("image/gif");

    private static final String KEY_FILE_PATH = "filePath";

    public static final String KEY_UPLOAD_TYPE = "upload_type";

    public static final int UPLOAD_TYPE_CAMERA = 0;

    public static final int UPLOAD_TYPE_GALLERY = 1;

    public static final int UPLOAD_TYPE_LINK = 2;

    private static final int REQUEST_CODE_CAMERA = 123;

    private static final int REQUEST_CODE_GALLERY = 321;

    private static final long TEXT_DELAY = 1000L;

    private static final int MSG_SEARCH_URL = 11;

    private static final String DFRAGMENT_DECODING = "decoding";

    private static final String DFRAGMENT_UPLOADING = "uploading";

    private File mTempFile = null;

    private File mCameraFile = null;

    private ImageView mPreviewImage;

    private EditText mTitle;

    private EditText mDesc;

    private EditText mLink;

    private CheckBox mGalleryCB;

    private boolean mIsValidLink = false;

    private boolean mDidRotateImage = false;

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
        handleBundle(savedInstanceState);
        init();
    }

    /**
     * Handles the Bundle arguments if they exists
     *
     * @param savedInstanceState
     */
    private void handleBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            int uploadType = UPLOAD_TYPE_LINK;
            if (getIntent().getExtras() != null) {
                uploadType = getIntent().getExtras().getInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);
            }

            if (uploadType != UPLOAD_TYPE_LINK) {
                int requestCode = uploadType == UPLOAD_TYPE_GALLERY ? REQUEST_CODE_GALLERY : REQUEST_CODE_CAMERA;
                startActivityForResult(createPhotoIntent(uploadType), requestCode);
            }
        } else {
            int uploadType = savedInstanceState.getInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);

            switch (uploadType) {
                case UPLOAD_TYPE_CAMERA:
                    mCameraFile = new File(savedInstanceState.getString(KEY_FILE_PATH));
                    new LoadImageTask().execute(mCameraFile);
                    mLink.setVisibility(View.GONE);
                    break;

                case UPLOAD_TYPE_GALLERY:
                    mTempFile = new File(savedInstanceState.getString(KEY_FILE_PATH));
                    new LoadImageTask().execute(mTempFile);
                    mLink.setVisibility(View.GONE);
                    break;

                case UPLOAD_TYPE_LINK:
                    // TODO?
                    break;
            }
        }
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
                            })
                            .setPositiveButton(R.string.yes, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.dismiss();
                                    if (mIsValidLink) {
                                        upload(mTitle.getText().toString(), mDesc.getText().toString(), mLink.getText().toString(), false);
                                    } else {
                                        upload(mTitle.getText().toString(), mDesc.getText().toString(), mTempFile != null ?
                                                mTempFile : mCameraFile, false);
                                    }
                                }
                            }).build());

                    dialog.show();
                } else {
                    String title = mTitle.getText().toString();
                    String desc = mDesc.getText().toString();
                    String link = mLink.getText().toString();

                    if (mGalleryCB.isChecked() && TextUtils.isEmpty(title)) {
                        // Shake the edit text to show that they have not enetered any text
                        ObjectAnimator.ofFloat(mTitle, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0).setDuration(750L).start();
                        return false;
                    }

                    if (mIsValidLink) {
                        upload(title, desc, link, mGalleryCB.isChecked());
                    } else {
                        upload(title, desc, mTempFile != null ?
                                mTempFile : mCameraFile, false);
                    }
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
                        ImgurPhoto photo = new ImgurPhoto(event.json.getJSONObject(ApiClient.KEY_DATA));

                        // If our id is true, that means we need to upload the resulting id to the gallery
                        if (event.id.equals(String.valueOf(true))) {

                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, photo);
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
     * Uploads the url to Imgur
     *
     * @param title           Optional title for image. Required if uploading to gallery
     * @param description     Option description for image
     * @param url             Picture Url
     * @param uploadToGallery If the image should be uploaded to the gallery
     */
    private void upload(@Nullable String title, @Nullable String description, @NonNull String url, boolean uploadToGallery) {
        showDialogFragment(LoadingDialogFragment.createInstance(R.string.uploading), DFRAGMENT_UPLOADING);
        ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);

        FormEncodingBuilder builder = new FormEncodingBuilder()
                .add("image", url)
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

    /**
     * Uploads the file to Imgur
     *
     * @param title           Optional title for image. Required if uploading to gallery
     * @param description     Option description for image
     * @param file            File to upload
     * @param uploadToGallery If the image should be uploaded to the gallery
     */
    private void upload(@Nullable String title, @Nullable String description, @NonNull File file, boolean uploadToGallery) {
        showDialogFragment(LoadingDialogFragment.createInstance(R.string.uploading), DFRAGMENT_UPLOADING);

        // Make sure the image isn't a gif. They can't be rotated
        if (mDidRotateImage && mPreviewImage.getDrawable() instanceof BitmapDrawable) {
            new RotateImageTask(file, uploadToGallery, title, description).execute(((BitmapDrawable) mPreviewImage.getDrawable()).getBitmap());
        } else {
            ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);
            MediaType type;

            if (file.getAbsolutePath().endsWith("png")) {
                type = MEDIA_TYPE_PNG;
            } else if (file.getAbsolutePath().endsWith("gif")) {
                type = MEDIA_TYPE_GIF;
            } else {
                type = MEDIA_TYPE_JPEG;
            }

            MultipartBuilder builder = new MultipartBuilder().type(MultipartBuilder.FORM)
                    .addPart(Headers.of("Content-Disposition", "form-data; name=\"image\""),
                            RequestBody.create(type, file));

            builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"type\""),
                    RequestBody.create(null, "file"));

            if (!TextUtils.isEmpty(title)) {
                builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"title\""),
                        RequestBody.create(null, title));
            }

            if (!TextUtils.isEmpty(description)) {
                builder.addPart(Headers.of("Content-Disposition", "form-data; name=\"description\""),
                        RequestBody.create(null, description));
            }

            client.doWork(ImgurBusEvent.EventType.UPLOAD, String.valueOf(uploadToGallery), builder.build());
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
                        showDialogFragment(LoadingDialogFragment.createInstance(R.string.decoding_image), DFRAGMENT_DECODING);
                        FileUtil.scanFile(Uri.fromFile(mCameraFile), getApplicationContext());
                        task.execute(mCameraFile);
                    } else {
                        SnackBar.show(this, R.string.upload_camera_error);
                    }

                    break;

                case REQUEST_CODE_GALLERY:
                    task = new LoadImageTask();
                    showDialogFragment(LoadingDialogFragment.createInstance(R.string.decoding_image), DFRAGMENT_DECODING);
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
                        SnackBar.show(this, R.string.upload_gallery_error);
                        dismissDialogFragment(DFRAGMENT_DECODING);
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
        if (FileUtil.isFileValid(mTempFile) && isFinishing()) {
            mTempFile.delete();
        }

        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (FileUtil.isFileValid(mTempFile)) {
            outState.putString(KEY_FILE_PATH, mTempFile.getAbsolutePath());
            outState.putInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_GALLERY);
        } else if (FileUtil.isFileValid(mCameraFile)) {
            outState.putString(KEY_FILE_PATH, mCameraFile.getAbsolutePath());
            outState.putInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_CAMERA);
        } else {
            outState.putInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);
        }
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
                    dismissDialogFragment(DFRAGMENT_UPLOADING);
                    final ImgurPhoto photo = (ImgurPhoto) msg.obj;
                    String message = getString(R.string.upload_success, photo.getLink());

                    final AlertDialog dialog = new AlertDialog.Builder(UploadActivity.this).create();
                    dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

                    dialog.setView(new PopupDialogViewBuilder(getApplicationContext()).setTitle(R.string.upload_complete)
                            .setMessage(message).setNegativeButton(R.string.dismiss, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.dismiss();
                                    finish();
                                }
                            }).setPositiveButton(R.string.copy_link, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dialog.dismiss();
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", photo.getLink()));
                                    finish();

                                }
                            }).build());
                    dialog.show();
                    break;

                case MESSAGE_ACTION_FAILED:
                    dismissDialogFragment(DFRAGMENT_UPLOADING);
                    SnackBar.show(UploadActivity.this, (Integer) msg.obj);
                    break;
            }
        }
    };

    /**
     * Decodes the selected image in the background for displaying
     */
    private class LoadImageTask extends AsyncTask<File, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(File... files) {
            try {
                File file = files[0];

                if (!FileUtil.isFileValid(file)) {
                    Log.w(TAG, "Invalid file trying to be decoded");
                    return null;
                }

                int orientation = ImageUtil.getImageRotation(file);
                DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = metrics.widthPixels > 1024 ? 1024 : metrics.widthPixels;
                int height = metrics.heightPixels > 1024 ? 1024 : metrics.heightPixels;
                Bitmap bitmap = ImageUtil.decodeSampledBitmapFromResource(file, width, height);

                // If the orientation is normal or not defined, return the original bitmap
                if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                    return bitmap;
                }

                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.setScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.setRotate(180);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(-90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(-90);
                        break;
                    default:
                        return bitmap;
                }

                mDidRotateImage = true;
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                bitmap.recycle();
                return rotatedBitmap;

            } catch (Exception e) {
                Log.e(TAG, "Error decoding Image", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                mPreviewImage.setImageBitmap(bitmap);
                mLink.setVisibility(View.GONE);
            } else {
                SnackBar.show(UploadActivity.this, R.string.upload_decode_error);
                if (FileUtil.isFileValid(mTempFile)) {
                    mTempFile.delete();
                }
            }

            invalidateOptionsMenu();
            dismissDialogFragment(DFRAGMENT_DECODING);
        }
    }

    /**
     * Class that saves the rotated bitmap then uploads it
     */
    private class RotateImageTask extends AsyncTask<Bitmap, Void, Boolean> {
        private boolean mShouldUploadToGallery = false;

        private File mOriginalFile;

        private File mNewFile;

        private String mTitle;

        private String mDesc;

        public RotateImageTask(File originalFile, boolean uploadToGallery, String title, String desc) {
            mShouldUploadToGallery = uploadToGallery;
            mOriginalFile = originalFile;
            mTitle = title;
            mDesc = desc;
        }

        @Override
        protected Boolean doInBackground(Bitmap... bitmap) {
            boolean successful = false;
            try {
                mNewFile = FileUtil.createFile(FileUtil.EXTENSION_JPEG);
                successful = ImageUtil.saveBitmapToFile(bitmap[0], mNewFile);
            } catch (Exception e) {
                Log.e(TAG, "Error saving rotated image", e);
            }

            mDidRotateImage = false;
            return successful;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            File file;
            if (result && FileUtil.isFileValid(mNewFile)) {
                Log.v(TAG, "Rotated image saved successfully");
                file = mNewFile;
            } else {
                Log.w(TAG, "Rotated image unable to be saved, defaulting to original");
                file = mOriginalFile;
            }

            upload(mTitle, mDesc, file, mShouldUploadToGallery);
        }
    }
}
