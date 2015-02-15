package com.kenny.openimgur.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 8/2/14.
 */
public class UploadActivity extends BaseActivity {
    public static final int REQUEST_CODE = 100;

    private static final String PREF_NOTIFY_NO_USER = "notify_no_user";

    private static final String KEY_FILE_PATH = "filePath";

    private static final String KEY_IS_UPLOADING = "isuploading";

    public static final String KEY_UPLOAD_TYPE = "upload_type";

    public static final int UPLOAD_TYPE_CAMERA = 0;

    public static final int UPLOAD_TYPE_GALLERY = 1;

    public static final int UPLOAD_TYPE_LINK = 2;

    private static final int REQUEST_CODE_CAMERA = 123;

    private static final int REQUEST_CODE_GALLERY = 321;

    private static final long TEXT_DELAY = 1000L;

    private static final String DFRAGMENT_DECODING = "decoding";

    private static final String DFRAGMENT_UPLOADING = "uploading";

    @InjectView(R.id.previewImage)
    ImageView mPreviewImage;

    @InjectView(R.id.title)
    EditText mTitle;

    @InjectView(R.id.desc)
    EditText mDesc;

    @InjectView(R.id.url)
    EditText mLink;

    @InjectView(R.id.galleryUpload)
    CheckBox mGalleryCB;

    @InjectView(R.id.uploadButton)
    Button mUploadButton;

    private File mTempFile = null;

    private File mCameraFile = null;

    private boolean mIsValidLink = false;

    private boolean mIsUploading = false;

    private boolean mNotifyNoUser;

    public static Intent createIntent(Context context, int uploadType) {
        return new Intent(context, UploadActivity.class).putExtra(KEY_UPLOAD_TYPE, uploadType);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setTitle(getString(R.string.upload));
        setContentView(R.layout.activity_upload);
        mNotifyNoUser = app.getPreferences().getBoolean(PREF_NOTIFY_NO_USER, true);

        if (theme.isDarkTheme) {
            int bgColor = getResources().getColor(R.color.card_bg_dark);
            ((CardView) findViewById(R.id.photoContainer)).setCardBackgroundColor(bgColor);
            ((CardView) findViewById(R.id.infoContainer)).setCardBackgroundColor(bgColor);
            mPreviewImage.setImageDrawable(ImageUtil.getDrawableForDarkTheme(R.drawable.photo_placeholder, getResources()));
        }
    }

    /**
     * Handles the Bundle arguments if they exists
     *
     * @param savedInstanceState
     */
    private void handleBundle(Bundle savedInstanceState) {
        int uploadType = UPLOAD_TYPE_LINK;
        if (savedInstanceState == null) {
            if (getIntent().getExtras() != null) {
                Intent intent = getIntent();

                // Image sent via share intent
                if (Intent.ACTION_SEND.equals(intent.getAction())) {
                    String type = intent.getType();
                    LogUtil.v(TAG, "Received an image via Share intent, type " + type);

                    if ("text/plain".equals(type)) {
                        String link = intent.getStringExtra(Intent.EXTRA_TEXT);
                        init(true);
                        mLink.setText(link);
                    } else {
                        Uri photoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                        init(false);
                        decodeUri(photoUri);
                    }
                    return;
                } else {
                    uploadType = getIntent().getExtras().getInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);
                }
            }

            if (uploadType != UPLOAD_TYPE_LINK) {
                int requestCode = uploadType == UPLOAD_TYPE_GALLERY ? REQUEST_CODE_GALLERY : REQUEST_CODE_CAMERA;
                Intent intent = createPhotoIntent(uploadType);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, requestCode);
                } else {
                    finish();
                    Toast.makeText(getApplicationContext(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
                }

            }
        } else {
            uploadType = savedInstanceState.getInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);
            mIsUploading = savedInstanceState.getBoolean(KEY_IS_UPLOADING, false);

            switch (uploadType) {
                case UPLOAD_TYPE_CAMERA:
                    mCameraFile = new File(savedInstanceState.getString(KEY_FILE_PATH));
                    new LoadImageTask(this).execute(mCameraFile);
                    mLink.setVisibility(View.GONE);
                    break;

                case UPLOAD_TYPE_GALLERY:
                    mTempFile = new File(savedInstanceState.getString(KEY_FILE_PATH));
                    new LoadImageTask(this).execute(mTempFile);
                    mLink.setVisibility(View.GONE);
                    break;

                case UPLOAD_TYPE_LINK:
                    // TODO?
                    break;
            }

            if (mIsUploading) {
                showDialogFragment(LoadingDialogFragment.createInstance(R.string.uploading, false), DFRAGMENT_UPLOADING);
            }
        }

        init(uploadType == UPLOAD_TYPE_LINK);
    }

    private void decodeUri(Uri uri) {
        DialogFragment fragment = LoadingDialogFragment.createInstance(R.string.decoding_image, true);
        showDialogFragment(fragment, DFRAGMENT_DECODING);
        mTempFile = FileUtil.createFile(uri, getContentResolver());

        if (FileUtil.isFileValid(mTempFile)) {
            if (mTempFile.getAbsolutePath().endsWith(".gif")) {
                try {
                    mPreviewImage.setImageDrawable(new GifDrawable(mTempFile));
                    // Not using dismissDialogFragment due to fast gif decoding before the fragment
                    // gets added to the view
                    fragment.dismiss();
                    mLink.setVisibility(View.GONE);
                } catch (IOException ex) {
                    LogUtil.e(TAG, "Unable to play gif, falling back to still image", ex);
                    // Load the image without playing it if the gif fails
                    new LoadImageTask(this).execute(mTempFile);
                }
            } else {
                new LoadImageTask(this).execute(mTempFile);
            }

        } else {
            SnackBar.show(this, R.string.upload_gallery_error);
            dismissDialogFragment(DFRAGMENT_DECODING);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        handleBundle(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        try {
            int status = event.json.getInt(ApiClient.KEY_STATUS);
            switch (event.eventType) {
                case UPLOAD:
                    if (status == ApiClient.STATUS_OK) {
                        ImgurPhoto photo = new ImgurPhoto(event.json.getJSONObject(ApiClient.KEY_DATA));
                        app.getSql().insertUploadedPhoto(photo);
                        setResult(Activity.RESULT_OK);

                        // If our id is true, that means we need to upload the resulting id to the gallery
                        if (String.valueOf(true).equals(event.id)) {
                            // If the image was to be uploaded to the gallery, send it there
                            ApiClient client = new ApiClient(String.format(Endpoints.GALLERY_UPLOAD.getUrl(), photo.getId()),
                                    ApiClient.HttpRequest.POST);
                            RequestBody body = new FormEncodingBuilder()
                                    .add("title", photo.getTitle())
                                    .add("terms", "1").build();

                            client.doWork(ImgurBusEvent.EventType.GALLERY_SUBMISSION, photo.getLink(), body);
                            mIsUploading = true;
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, photo);
                        }
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(status));
                    }
                    break;

                case GALLERY_SUBMISSION:
                    if (status == ApiClient.STATUS_OK) {
                        boolean result = event.json.getBoolean(ApiClient.KEY_DATA);

                        if (result) {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, event.id);
                        } else {
                            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, R.string.error_generic);
                        }
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(status));
                    }
                    break;
            }

        } catch (JSONException e) {
            LogUtil.e(TAG, "Error Decoding JSON", e);
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

        LogUtil.e(TAG, "Error received from Event Bus", e);
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
        showDialogFragment(LoadingDialogFragment.createInstance(R.string.uploading, false), DFRAGMENT_UPLOADING);
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
        mIsUploading = true;
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
        showDialogFragment(LoadingDialogFragment.createInstance(R.string.uploading, false), DFRAGMENT_UPLOADING);
        ApiClient client = new ApiClient(Endpoints.UPLOAD.getUrl(), ApiClient.HttpRequest.POST);
        MediaType type;

        if (file.getAbsolutePath().endsWith("png")) {
            type = MediaType.parse("image/png");
        } else if (file.getAbsolutePath().endsWith("gif")) {
            type = MediaType.parse("image/gif");
        } else {
            type = MediaType.parse("image/jpeg");
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
        mIsUploading = true;

    }

    /**
     * Initializes the views for the view
     *
     * @param isLink If the upload is a link
     */
    private void init(boolean isLink) {
        // Don't need to add the listener if the upload type is not a link
        if (isLink) {
            mLink.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                    // Only when these files are null will we listen for the text change
                    mIsValidLink = false;
                    if (mTempFile == null && mCameraFile == null && !TextUtils.isEmpty(charSequence)) {
                        mHandler.removeMessages(ImgurHandler.MESSAGE_SEARCH_URL);
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(ImgurHandler.MESSAGE_SEARCH_URL, charSequence.toString()), TEXT_DELAY);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {

                }
            });
        }

        mGalleryCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                // Don't allow uploads to gallery when they are not logged in
                if (checked && user == null) {
                    SnackBar.show(UploadActivity.this, R.string.gallery_no_user);
                    button.setChecked(false);
                }
            }
        });


        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mIsValidLink && mTempFile == null && mCameraFile == null) {
                    // Nothing selected yet
                    SnackBar.show(UploadActivity.this, R.string.empty_upload);
                } else {
                    if (user == null && mNotifyNoUser) {
                        View nagView = LayoutInflater.from(UploadActivity.this).inflate(R.layout.no_user_nag, null);
                        final CheckBox cb = (CheckBox) nagView.findViewById(R.id.dontNotify);

                        new AlertDialog.Builder(UploadActivity.this)
                                .setTitle(R.string.not_logged_in)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (cb.isChecked()) {
                                            app.getPreferences().edit().putBoolean(PREF_NOTIFY_NO_USER, false).apply();
                                        }

                                        if (mIsValidLink) {
                                            upload(mTitle.getText().toString(), mDesc.getText().toString(), mLink.getText().toString(), false);
                                        } else {
                                            upload(mTitle.getText().toString(), mDesc.getText().toString(), mTempFile != null ?
                                                    mTempFile : mCameraFile, false);
                                        }
                                    }
                                }).setView(nagView).show();
                    } else {
                        String title = mTitle.getText().toString();
                        String desc = mDesc.getText().toString();
                        String link = mLink.getText().toString();

                        if (mGalleryCB.isChecked() && TextUtils.isEmpty(title)) {
                            SnackBar.show(UploadActivity.this, R.string.gallery_upload_no_title);
                            return;
                        }

                        if (mIsValidLink) {
                            upload(title, desc, link, mGalleryCB.isChecked());
                        } else {
                            upload(title, desc, mTempFile != null ?
                                    mTempFile : mCameraFile, mGalleryCB.isChecked());
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_CAMERA:
                    if (FileUtil.isFileValid(mCameraFile)) {
                        showDialogFragment(LoadingDialogFragment.createInstance(R.string.decoding_image, true), DFRAGMENT_DECODING);
                        FileUtil.scanFile(Uri.fromFile(mCameraFile), getApplicationContext());
                        new LoadImageTask(this).execute(mCameraFile);
                    } else {
                        SnackBar.show(this, R.string.upload_camera_error);
                    }

                    break;

                case REQUEST_CODE_GALLERY:
                    decodeUri(data.getData());
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
        if (FileUtil.isFileValid(mTempFile)) {
            outState.putString(KEY_FILE_PATH, mTempFile.getAbsolutePath());
            outState.putInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_GALLERY);
        } else if (FileUtil.isFileValid(mCameraFile)) {
            outState.putString(KEY_FILE_PATH, mCameraFile.getAbsolutePath());
            outState.putInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_CAMERA);
        } else {
            outState.putInt(KEY_UPLOAD_TYPE, UPLOAD_TYPE_LINK);
        }

        outState.putBoolean(KEY_IS_UPLOADING, mIsUploading);
        super.onSaveInstanceState(outState);
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
                case MESSAGE_SEARCH_URL:
                    app.getImageLoader().loadImage((String) msg.obj, new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingFailed(String s, View view, FailReason failReason) {
                            mIsValidLink = false;
                            mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                            SnackBar.show(UploadActivity.this, R.string.invalid_url);
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
                        }

                        @Override
                        public void onLoadingCancelled(String s, View view) {
                            mIsValidLink = false;
                            mPreviewImage.setImageResource(R.drawable.photo_placeholder);
                            SnackBar.show(UploadActivity.this, R.string.invalid_url);
                        }
                    });
                    break;

                case MESSAGE_ACTION_COMPLETE:
                    dismissDialogFragment(DFRAGMENT_UPLOADING);
                    final String url;

                    if (msg.obj instanceof ImgurPhoto) {
                        url = ((ImgurPhoto) msg.obj).getLink();
                    } else {
                        url = (String) msg.obj;
                    }

                    String message = getString(R.string.upload_success, url);
                    Dialog dialog = new AlertDialog.Builder(UploadActivity.this)
                            .setTitle(R.string.upload_complete)
                            .setMessage(message).setNegativeButton(R.string.dismiss, null)
                            .setPositiveButton(R.string.copy_link, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", url));
                                }
                            }).create();

                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finish();
                        }
                    });

                    dialog.show();
                    break;

                case MESSAGE_ACTION_FAILED:
                    mIsUploading = false;
                    dismissDialogFragment(DFRAGMENT_UPLOADING);
                    SnackBar.show(UploadActivity.this, (Integer) msg.obj);
                    break;
            }
        }
    };

    /**
     * Decodes the selected image in the background for displaying
     */
    private static class LoadImageTask extends AsyncTask<File, Void, Bitmap> {
        private WeakReference<UploadActivity> mActivity;

        private DisplayMetrics mMetrics;

        public LoadImageTask(UploadActivity activity) {
            mActivity = new WeakReference<>(activity);
            mMetrics = activity.getResources().getDisplayMetrics();
        }

        @Override
        protected Bitmap doInBackground(File... files) {
            try {
                File file = files[0];

                if (!FileUtil.isFileValid(file)) {
                    LogUtil.w("LoadImageTask", "Invalid file trying to be decoded");
                    return null;
                }

                int orientation = ImageUtil.getImageRotation(file);
                int width = mMetrics.widthPixels > 720 ? 720 : mMetrics.widthPixels;
                int height = mMetrics.heightPixels > 1024 ? 1024 : mMetrics.heightPixels;
                Bitmap bitmap = ImageUtil.decodeSampledBitmapFromResource(file, width, height);

                // If the orientation is normal or not defined, return the original bitmap
                if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
                    LogUtil.v("LoadImageTask", "Image does not need to be rotated, returning bitmap");
                    return bitmap;
                }

                // Clear any memory to free up some space for image rotating
                mActivity.get().app.getImageLoader().clearMemoryCache();

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

                LogUtil.v("LoadImageTask", "Image will be rotated");
                Bitmap rotatedBitmap = null;

                try {
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                } catch (OutOfMemoryError e) {
                    LogUtil.e("LoadImageTask", "Out of memory while rotating image", e);
                    return bitmap;
                }

                return rotatedBitmap;
            } catch (Exception e) {
                LogUtil.e("LoadImageTask", "Error decoding Image", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            UploadActivity activity = mActivity.get();

            if (activity != null) {
                if (bitmap != null) {
                    activity.mPreviewImage.setImageBitmap(bitmap);
                    activity.mLink.setVisibility(View.GONE);
                    activity.mUploadButton.setVisibility(View.VISIBLE);
                } else {
                    SnackBar.show(activity, R.string.upload_decode_error);
                    if (FileUtil.isFileValid(activity.mTempFile)) {
                        activity.mTempFile.delete();
                    }
                }

                activity.dismissDialogFragment(DFRAGMENT_DECODING);
                mActivity.clear();
            }
        }
    }
}
