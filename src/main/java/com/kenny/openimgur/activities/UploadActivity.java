package com.kenny.openimgur.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.UploadPhotoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.PhotoUploadListener;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.fragments.UploadInfoFragment;
import com.kenny.openimgur.fragments.UploadLinkDialogFragment;
import com.kenny.openimgur.services.UploadService;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;
import com.kennyc.view.MultiStateView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class UploadActivity extends BaseActivity implements PhotoUploadListener {
    private static final String KEY_PASSED_FILE = "passed_file";

    private static final String KEY_SAVED_ITEMS = "saved_items";

    private static final String PREF_NOTIFY_NO_USER = "notify_no_user";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mRecyclerView;

    private UploadPhotoAdapter mAdapter;

    private File mTempFile;

    private List<Uri> mPhotoUris = null;

    public static Intent createIntent(Context context) {
        return new Intent(context, UploadActivity.class);
    }

    public static Intent createIntent(Context context, @NonNull File file) {
        return createIntent(context).putExtra(KEY_PASSED_FILE, file.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        Resources res = getResources();
        ViewUtils.setRecyclerViewGridDefaults(getApplicationContext(), mRecyclerView, res.getInteger(R.integer.upload_num_columns), res.getDimensionPixelSize(R.dimen.grid_padding));
        ViewUtils.setEmptyText(mMultiView, R.id.emptyMessage, R.string.upload_empty_message);
        new ItemTouchHelper(mSimpleItemTouchCallback).attachToRecyclerView(mRecyclerView);
        getSupportActionBar().setTitle(R.string.upload);
        checkForTopics();
        checkForNag();

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SAVED_ITEMS)) {
            List<Upload> uploads = savedInstanceState.getParcelableArrayList(KEY_SAVED_ITEMS);
            mAdapter = new UploadPhotoAdapter(this, uploads, this);
            mRecyclerView.setAdapter(mAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        } else {
            checkIntent(getIntent());
        }
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent == null) return;

        List<Upload> uploads = null;

        if (intent.hasExtra(KEY_PASSED_FILE)) {
            LogUtil.v(TAG, "Received file from intent");
            uploads = new ArrayList<>(1);
            uploads.add(new Upload(intent.getStringExtra(KEY_PASSED_FILE)));
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String type = intent.getType();
            LogUtil.v(TAG, "Received an image via Share intent, type " + type);

            if ("text/plain".equals(type)) {
                String link = intent.getStringExtra(Intent.EXTRA_TEXT);
                getFragmentManager().beginTransaction()
                        .add(UploadLinkDialogFragment.newInstance(link), UploadLinkDialogFragment.TAG)
                        .commit();
            } else {
                Uri photoUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                File file = FileUtil.createFile(photoUri, getContentResolver());

                if (FileUtil.isFileValid(file)) {
                    uploads = new ArrayList<>(1);
                    uploads.add(new Upload(file.getAbsolutePath()));
                } else {
                    SnackBar.show(this, R.string.upload_decode_failure);
                }
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> photoUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

            if (photoUris != null && !photoUris.isEmpty()) {
                LogUtil.v(TAG, "Received " + photoUris.size() + " images via Share intent");
                new DecodeImagesTask(this).execute(photoUris);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            }
        }

        if (uploads != null && !uploads.isEmpty()) {
            mAdapter = new UploadPhotoAdapter(this, uploads, this);
            mRecyclerView.setAdapter(mAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        }
    }

    private void onUrisDecoded(@Nullable List<Upload> uploads) {
        if (uploads == null || uploads.isEmpty()) {
            boolean isEmpty = mAdapter == null || mAdapter.isEmpty();
            mMultiView.setViewState(isEmpty ? MultiStateView.VIEW_STATE_EMPTY : MultiStateView.VIEW_STATE_CONTENT);
        } else {
            if (mAdapter == null) {
                mAdapter = new UploadPhotoAdapter(this, uploads, this);
                mRecyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.addItems(uploads);
            }

            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        }

        supportInvalidateOptionsMenu();
        mPhotoUris = null;
    }

    private void onNeedsReadPermission(List<Uri> photoUris) {
        mMultiView.setViewState(mAdapter != null && !mAdapter.isEmpty() ? MultiStateView.VIEW_STATE_CONTENT : MultiStateView.VIEW_STATE_EMPTY);
        mPhotoUris = photoUris;
        int level = PermissionUtils.getPermissionLevel(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        switch (level) {
            // Should never have gotten here if its available
            case PermissionUtils.PERMISSION_AVAILABLE:
                new DecodeImagesTask(this).doInBackground(mPhotoUris);
                break;

            case PermissionUtils.PERMISSION_DENIED:
                int uriSize = photoUris != null ? photoUris.size() : 0;
                new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                        .setTitle(R.string.permission_title)
                        .setMessage(getResources().getQuantityString(R.plurals.permission_rational_file_access, uriSize, uriSize))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(UploadActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_READ);
                            }
                        })
                        .show();
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
                ActivityCompat.requestPermissions(UploadActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_READ);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.upload);
        item.setVisible(mAdapter != null && !mAdapter.isEmpty());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.upload:
                getFragmentManager()
                        .beginTransaction()
                        .add(android.R.id.content, UploadInfoFragment.newInstance(), UploadInfoFragment.class.getSimpleName())
                        .commit();
                getSupportActionBar().hide();
                return true;

            case android.R.id.home:
                if (showCancelDialog()) return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick({R.id.cameraBtn, R.id.linkBtn, R.id.galleryBtn})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cameraBtn:
                if (checkWritePermissions()) startCamera();
                break;

            case R.id.galleryBtn:
                Intent intent = new Intent();
                intent.setType("image/*");
                // Allow multiple selection for API 18+
                if (isApiLevel(Build.VERSION_CODES.JELLY_BEAN_MR2)) intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, RequestCodes.SELECT_PHOTO);
                } else {
                    SnackBar.show(this, R.string.cant_launch_intent);
                }
                break;

            case R.id.linkBtn:
                getFragmentManager().beginTransaction()
                        .add(UploadLinkDialogFragment.newInstance(null), UploadLinkDialogFragment.TAG)
                        .commit();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK && FileUtil.isFileValid(mTempFile)) {
                    FileUtil.scanFile(Uri.fromFile(mTempFile), getApplicationContext());
                    String fileLocation = mTempFile.getAbsolutePath();
                    Upload upload = new Upload(fileLocation);

                    if (mAdapter == null) {
                        mAdapter = new UploadPhotoAdapter(this, upload, this);
                        mRecyclerView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItem(upload);
                    }

                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    mTempFile = null;
                    supportInvalidateOptionsMenu();
                } else {
                    SnackBar.show(this, R.string.upload_camera_error);
                }
                break;

            case RequestCodes.SELECT_PHOTO:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ClipData clipData = data.getClipData();
                    List<Uri> uris = null;

                    // Check if we have multiple images
                    if (clipData != null) {
                        int size = clipData.getItemCount();
                        uris = new ArrayList<>(size);

                        for (int i = 0; i < size; i++) {
                            uris.add(clipData.getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        // If not multiple images, then only one was selected
                        uris = new ArrayList<>(1);
                        uris.add(data.getData());
                    }

                    if (uris != null && !uris.isEmpty()) {
                        mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                        new DecodeImagesTask(this).execute(uris);
                    } else {
                        SnackBar.show(this, R.string.error_generic);
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onLinkAdded(String link) {
        Upload upload = new Upload(link, true);

        if (mAdapter == null) {
            mAdapter = new UploadPhotoAdapter(this, upload, this);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.addItem(upload);
        }

        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onItemClicked(int position) {
        // TODO
    }

    @Override
    public void onItemEdited(Upload upload) {
        mAdapter.notifyDataSetChanged();
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onUpload(boolean submitToGallery, String title, String description, ImgurTopic topic) {
        ArrayList<Upload> uploads = mAdapter.retainItems();
        Intent uploadSerivce = UploadService.createIntent(getApplicationContext(), uploads, submitToGallery, title, description, topic);
        startService(uploadSerivce);
        finish();
    }

    @Override
    public void onBackPressed() {
        Fragment f = getFragmentManager().findFragmentByTag(UploadInfoFragment.class.getSimpleName());

        if (f != null) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(f)
                    .commit();

            getSupportActionBar().show();
        } else if (!showCancelDialog()) {
            super.onBackPressed();
        }
    }

    private boolean showCancelDialog() {
        if (mAdapter == null || mAdapter.isEmpty()) return false;

        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.upload_cancel_title)
                .setMessage(R.string.upload_cancel_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();

        return true;
    }

    /**
     * Checks if we have cached topics to display for the info fragment
     */
    private void checkForTopics() {
        List<ImgurTopic> topics = app.getSql().getTopics();

        if (topics == null || topics.isEmpty()) {
            LogUtil.v(TAG, "No topics found, fetching");
            ApiClient.getService().getDefaultTopics().enqueue(new Callback<TopicResponse>() {
                @Override
                public void onResponse(Response<TopicResponse> response, Retrofit retrofit) {
                    if (response != null && response.body() != null) app.getSql().addTopics(response.body().data);
                }

                @Override
                public void onFailure(Throwable t) {
                    LogUtil.e(TAG, "Failed to receive topics", t);
                }
            });
        } else {
            LogUtil.v(TAG, "Topics in database");
        }
    }

    /**
     * Checks if the user is not logged in and if we should nag about it
     */
    private boolean checkForNag() {
        boolean nag = app.getPreferences().getBoolean(PREF_NOTIFY_NO_USER, true);

        if (nag && user == null) {
            View nagView = LayoutInflater.from(this).inflate(R.layout.no_user_nag, null);
            final CheckBox cb = (CheckBox) nagView.findViewById(R.id.dontNotify);

            new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                    .setTitle(R.string.not_logged_in)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (cb.isChecked()) {
                                app.getPreferences().edit().putBoolean(PREF_NOTIFY_NO_USER, false).apply();
                            }
                        }
                    })
                    .setPositiveButton(R.string.yes, null)
                    .setView(nagView)
                    .show();

            return true;
        }

        return false;
    }

    /**
     * Checks if the needed permissions are available
     *
     * @return If the permissions are available. If false is returned, the necessary prompts will be shown
     */
    private boolean checkWritePermissions() {
        @PermissionUtils.PermissionLevel int permissionLevel = PermissionUtils.getPermissionLevel(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        switch (permissionLevel) {
            case PermissionUtils.PERMISSION_AVAILABLE:
                LogUtil.v(TAG, "Permissions available");
                return true;

            case PermissionUtils.PERMISSION_DENIED:
                new AlertDialog.Builder(this, app.getImgurTheme().getAlertDialogTheme())
                        .setTitle(R.string.permission_title)
                        .setMessage(R.string.permission_rationale_upload)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SnackBar.show(UploadActivity.this, R.string.permission_denied);
                            }
                        }).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(UploadActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        SnackBar.show(UploadActivity.this, R.string.permission_denied);
                    }
                }).show();
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
            default:
                LogUtil.v(TAG, "Prompting for permissions");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                break;
        }

        return false;
    }

    private void startCamera() {
        mTempFile = FileUtil.createFile(FileUtil.EXTENSION_JPEG);

        if (FileUtil.isFileValid(mTempFile)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempFile));
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
            } else {
                SnackBar.show(this, R.string.cant_launch_intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSION_WRITE:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    startCamera();
                } else {
                    SnackBar.show(this, R.string.permission_denied);
                }
                break;

            case RequestCodes.REQUEST_PERMISSION_READ:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    if (mPhotoUris != null) new DecodeImagesTask(this).execute(mPhotoUris);
                } else {
                    SnackBar.show(this, R.string.permission_denied);
                }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_SAVED_ITEMS, mAdapter.retainItems());
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroy();
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark : R.style.Theme_Opengur_Light_DarkActionBar;
    }

    private ItemTouchHelper.SimpleCallback mSimpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT, 0) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            //NOOP
        }
    };

    private static class DecodeImagesTask extends AsyncTask<List<Uri>, Void, List<Upload>> {
        WeakReference<UploadActivity> mActivity;

        boolean needsPermission = false;

        List<Uri> photoUris = null;

        public DecodeImagesTask(@NonNull UploadActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        protected List<Upload> doInBackground(List<Uri>... params) {
            if (params != null && params[0] != null && !params[0].isEmpty()) {
                Activity activity = mActivity.get();
                ContentResolver resolver = activity.getContentResolver();
                photoUris = params[0];
                List<Upload> uploads = new ArrayList<>(photoUris.size());
                boolean hasReadPermission = PermissionUtils.hasPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

                for (Uri uri : photoUris) {
                    try {
                        // Check if the URI is a file as we will need read permissions for it
                        if (!hasReadPermission && "file".equals(uri.getScheme())) {
                            LogUtil.v("DecodeImageTask", "Received a File URI and don't have permissions");
                            needsPermission = true;
                            return null;
                        }

                        File file = FileUtil.createFile(uri, resolver);
                        if (FileUtil.isFileValid(file)) uploads.add(new Upload(file.getAbsolutePath()));
                    } catch (Exception ex) {
                        LogUtil.e("DecodeImageTask", "Unable to decode image", ex);
                    }
                }

                return uploads;
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Upload> uploads) {
            if (mActivity != null && mActivity.get() != null) {
                UploadActivity activity = mActivity.get();

                if (needsPermission) {
                    activity.onNeedsReadPermission(photoUris);
                } else {
                    activity.onUrisDecoded(uploads);
                }

                mActivity.clear();
                mActivity = null;
            }
        }
    }
}
