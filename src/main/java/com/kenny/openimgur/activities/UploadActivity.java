package com.kenny.openimgur.activities;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.UploadPhotoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.PhotoUploadListener;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.fragments.UploadEditDialogFragment;
import com.kenny.openimgur.fragments.UploadInfoFragment;
import com.kenny.openimgur.fragments.UploadLinkDialogFragment;
import com.kenny.openimgur.services.UploadService;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarItem;
import com.kenny.snackbar.SnackBarListener;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class UploadActivity extends BaseActivity implements PhotoUploadListener {
    private static final String KEY_PASSED_FILE = "passed_file";

    private static final String KEY_SAVED_ITEMS = "saved_items";

    private static final String PREF_NOTIFY_NO_USER = "notify_no_user";

    private static final String[] PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mRecyclerView;

    @Bind(R.id.uploadContainer)
    View mUploadContainer;

    private UploadPhotoAdapter mAdapter;

    private File mTempFile;

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
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        mMultiView.setEmptyText(R.id.emptyMessage, R.string.upload_empty_message);
        new ItemTouchHelper(mSimpleItemTouchCallback).attachToRecyclerView(mRecyclerView);
        getSupportActionBar().setTitle(R.string.upload);
        checkForTopics();

        if (!checkForNag()) {
            if (!checkPermissions()) return;
        }

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
            mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
        } else {
            mAdapter = new UploadPhotoAdapter(this, uploads, this);
            mRecyclerView.setAdapter(mAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
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
                break;

            case R.id.galleryBtn:
                startActivityForResult(PhotoPickerActivity.createInstance(getApplicationContext()), RequestCodes.SELECT_PHOTO);
                break;

            case R.id.linkBtn:
                getFragmentManager().beginTransaction()
                        .add(UploadLinkDialogFragment.newInstance(null), UploadLinkDialogFragment.TAG)
                        .commit();
                break;
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Not_Translucent_Dark : R.style.Theme_Not_Translucent_Light;
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
        Upload upload = mAdapter.getItem(position);

        getFragmentManager().beginTransaction()
                .add(UploadEditDialogFragment.createInstance(upload), UploadEditDialogFragment.TAG)
                .commit();
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
            ApiClient.getService().getDefaultTopics(new Callback<TopicResponse>() {
                @Override
                public void success(TopicResponse topicResponse, Response response) {
                    if (topicResponse != null) app.getSql().addTopics(topicResponse.data);
                }

                @Override
                public void failure(RetrofitError error) {
                    LogUtil.e(TAG, "Failed to receive topics", error);
                    // TODO Some error?
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
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            checkPermissions();
                        }
                    })
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
    private boolean checkPermissions() {
        @PermissionUtils.PermissionLevel int permissionLevel = PermissionUtils.getPermissionType(this, PERMISSIONS);

        switch (permissionLevel) {
            case PermissionUtils.PERMISSION_AVAILABLE:
                LogUtil.v(TAG, "Permissions available");
                return true;

            case PermissionUtils.PERMISSION_DENIED:
                mUploadContainer.setVisibility(View.GONE);

                new SnackBarItem.Builder(this)
                        .setMessageResource(R.string.permission_rationale_upload)
                        .setActionMessageResource(R.string.okay)
                        .setAutoDismiss(false)
                        .setSnackBarListener(new SnackBarListener() {
                            @Override
                            public void onSnackBarStarted(Object o) {
                                LogUtil.v(TAG, "Permissions have been denied before, showing rationale");
                            }

                            @Override
                            public void onSnackBarFinished(Object o, boolean actionClicked) {
                                if (actionClicked) {
                                    ActivityCompat.requestPermissions(UploadActivity.this, PERMISSIONS, RequestCodes.REQUEST_PERMISSIONS);
                                } else {
                                    Toast.makeText(getApplicationContext(), R.string.permission_denied, Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                        }).show();
                break;

            case PermissionUtils.PERMISSION_UNAVAILABLE:
            default:
                LogUtil.v(TAG, "Prompting for permissions");
                ActivityCompat.requestPermissions(this, PERMISSIONS, RequestCodes.REQUEST_PERMISSIONS);
                break;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.SELECT_PHOTO:
                if (resultCode == Activity.RESULT_OK && data != null && data.hasExtra(PhotoPickerActivity.KEY_PHOTOS)) {
                    List<String> photos = data.getStringArrayListExtra(PhotoPickerActivity.KEY_PHOTOS);
                    List<Upload> uploads = new ArrayList<>(photos.size());

                    for (String s : photos) {
                        uploads.add(new Upload(s));
                    }

                    if (mAdapter == null) {
                        mAdapter = new UploadPhotoAdapter(this, uploads, this);
                        mRecyclerView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(uploads);
                    }

                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    supportInvalidateOptionsMenu();
                }
                break;

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
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSIONS:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    SnackBar.show(this, R.string.permission_granted);
                    mUploadContainer.setVisibility(View.VISIBLE);
                    checkIntent(getIntent());
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    private ItemTouchHelper.SimpleCallback mSimpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            mAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
            int adapterPosition = viewHolder.getAdapterPosition();
            Upload upload = mAdapter.removeItem(adapterPosition);
            if (mAdapter.isEmpty()) mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);

            SnackBar.cancelSnackBars(UploadActivity.this);
            new SnackBarItem.Builder(UploadActivity.this)
                    .setMessageResource(R.string.upload_photo_removed)
                    .setActionMessageResource(R.string.undo)
                    .setObject(new Object[]{adapterPosition, upload})
                    .setSnackBarListener(new SnackBarListener() {
                        @Override
                        public void onSnackBarStarted(Object o) {

                        }

                        @Override
                        public void onSnackBarFinished(Object object, boolean actionPressed) {
                            if (actionPressed && object instanceof Object[]) {
                                Object[] objects = (Object[]) object;
                                int position = (int) objects[0];
                                Upload upload = (Upload) objects[1];
                                mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                                mAdapter.addItem(upload, position);
                                supportInvalidateOptionsMenu();
                            }
                        }
                    })
                    .show();

            supportInvalidateOptionsMenu();
        }
    };

    private static class DecodeImagesTask extends AsyncTask<List<Uri>, Void, List<Upload>> {
        WeakReference<UploadActivity> mActivty;

        public DecodeImagesTask(@NonNull UploadActivity activity) {
            mActivty = new WeakReference<>(activity);
        }

        @Override
        protected List<Upload> doInBackground(List<Uri>... params) {
            if (params != null && params[0] != null && !params[0].isEmpty()) {
                ContentResolver resolver = mActivty.get().getContentResolver();
                List<Uri> photoUris = params[0];

                List<Upload> uploads = new ArrayList<>(photoUris.size());

                for (Uri uri : photoUris) {
                    File file = FileUtil.createFile(uri, resolver);
                    if (FileUtil.isFileValid(file)) uploads.add(new Upload(file.getAbsolutePath()));
                }

                return uploads;
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Upload> uploads) {
            if (mActivty != null && mActivty.get() != null) {
                mActivty.get().onUrisDecoded(uploads);
                mActivty.clear();
                mActivty = null;
            }
        }
    }
}
