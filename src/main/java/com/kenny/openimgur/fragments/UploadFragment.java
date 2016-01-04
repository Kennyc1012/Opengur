package com.kenny.openimgur.fragments;


import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
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
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.UploadEditActivity;
import com.kenny.openimgur.adapters.UploadPhotoAdapter;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.classes.UploadListener;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarItem;
import com.kenny.snackbar.SnackBarListener;
import com.kennyc.view.MultiStateView;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class UploadFragment extends BaseFragment implements View.OnClickListener {
    private static final String KEY_PASSED_FILE = "passed_file";

    private static final String KEY_PASSED_LINK = "passed_link";

    private static final String KEY_PASSED_URIS = "passed_uri_list";

    private static final String KEY_SAVED_ITEMS = "saved_items";

    @Bind(R.id.multiView)
    MultiStateView mMultiView;

    @Bind(R.id.list)
    RecyclerView mRecyclerView;

    private UploadPhotoAdapter mAdapter;

    private File mTempFile;

    private List<Uri> mPhotoUris = null;

    private UploadListener mListener;

    public static UploadFragment newInstance(@Nullable Bundle args) {
        UploadFragment fragment = new UploadFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    public static Bundle createArguments(String path, boolean isLink, ArrayList<Uri> photoUris) {
        Bundle args = null;

        if (!TextUtils.isEmpty(path)) {
            args = new Bundle(1);
            if (!isLink) {
                args.putString(KEY_PASSED_FILE, path);
            } else {
                args.putString(KEY_PASSED_LINK, path);
            }

            return args;
        } else if (photoUris != null && !photoUris.isEmpty()) {
            args = new Bundle(1);
            args.putParcelableArrayList(KEY_PASSED_URIS, photoUris);

        }

        return args;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof UploadListener) mListener = (UploadListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Resources res = getResources();
        ViewUtils.setRecyclerViewGridDefaults(getActivity(), mRecyclerView, res.getInteger(R.integer.upload_num_columns), res.getDimensionPixelSize(R.dimen.grid_padding));
        ViewUtils.setEmptyText(mMultiView, R.id.emptyMessage, R.string.upload_empty_message);
        new ItemTouchHelper(mSimpleItemTouchCallback).attachToRecyclerView(mRecyclerView);

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_SAVED_ITEMS)) {
            List<Upload> uploads = savedInstanceState.getParcelableArrayList(KEY_SAVED_ITEMS);
            mAdapter = new UploadPhotoAdapter(getActivity(), uploads, this);
            mRecyclerView.setAdapter(mAdapter);
            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            if (mListener != null) mListener.onPhotoAdded();
        } else {
            handleArgs(getArguments());
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.upload, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.camera:
                if (checkWritePermissions()) startCamera();
                return true;

            case R.id.gallery:
                Intent intent = new Intent();
                intent.setType("image/*");
                // Allow multiple selection for API 18+
                if (isApiLevel(Build.VERSION_CODES.JELLY_BEAN_MR2))
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setAction(Intent.ACTION_GET_CONTENT);

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(intent, RequestCodes.SELECT_PHOTO);
                } else {
                    SnackBar.show(getActivity(), R.string.cant_launch_intent);
                }
                return true;

            case R.id.link:
                getChildFragmentManager().beginTransaction()
                        .add(UploadLinkDialogFragment.newInstance(null), UploadLinkDialogFragment.TAG)
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleArgs(@Nullable Bundle args) {
        if (args == null) {
            mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
            return;
        }

        List<Upload> uploads = null;

        if (args.containsKey(KEY_PASSED_FILE)) {
            LogUtil.v(TAG, "Received file from bundle");
            uploads = new ArrayList<>(1);
            uploads.add(new Upload(args.getString(KEY_PASSED_FILE)));
        } else if (args.containsKey(KEY_PASSED_LINK)) {
            mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
            String link = args.getString(KEY_PASSED_LINK);
            getFragmentManager().beginTransaction()
                    .add(UploadLinkDialogFragment.newInstance(link), UploadLinkDialogFragment.TAG)
                    .commit();
        } else if (args.containsKey(KEY_PASSED_URIS)) {
            ArrayList<Uri> photoUris = args.getParcelableArrayList(KEY_PASSED_URIS);

            if (photoUris != null && !photoUris.isEmpty()) {
                LogUtil.v(TAG, "Received " + photoUris.size() + " images via Share intent");
                new DecodeImagesTask(this).execute(photoUris);
                mMultiView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            }
        }

        if (uploads != null && !uploads.isEmpty()) {
            mAdapter = new UploadPhotoAdapter(getActivity(), uploads, this);
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
                mAdapter = new UploadPhotoAdapter(getActivity(), uploads, this);
                mRecyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.addItems(uploads);
            }

            mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        }

        mPhotoUris = null;

        if (mListener != null) {
            if (mAdapter == null || mAdapter.isEmpty()) {
                mListener.onPhotoRemoved(0);
            } else {
                mListener.onPhotoAdded();
            }
        }
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
                new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                        .setTitle(R.string.permission_title)
                        .setMessage(getResources().getQuantityString(R.plurals.permission_rational_file_access, uriSize, uriSize))
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_READ);
                            }
                        })
                        .show();
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_READ);
                break;
        }
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
                new AlertDialog.Builder(getActivity(), app.getImgurTheme().getAlertDialogTheme())
                        .setTitle(R.string.permission_title)
                        .setMessage(R.string.permission_rationale_upload)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                SnackBar.show(getActivity(), R.string.permission_denied);
                            }
                        }).setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        SnackBar.show(getActivity(), R.string.permission_denied);
                    }
                }).show();
                break;

            case PermissionUtils.PERMISSION_NEVER_ASKED:
            default:
                LogUtil.v(TAG, "Prompting for permissions");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                break;
        }

        return false;
    }

    private void startCamera() {
        mTempFile = FileUtil.createFile(FileUtil.EXTENSION_JPEG);

        if (FileUtil.isFileValid(mTempFile)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mTempFile));
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivityForResult(intent, RequestCodes.TAKE_PHOTO);
            } else {
                SnackBar.show(getActivity(), R.string.cant_launch_intent);
            }
        }
    }

    @Nullable
    public ArrayList<Upload> getPhotosForUpload() {
        if (mAdapter != null && !mAdapter.isEmpty()) {
            return mAdapter.retainItems();
        }

        return null;
    }

    public boolean hasPhotosForUpload() {
        return getPhotosForUpload() != null;
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroyView();
    }

    public void onClick(View view) {
        int position = mRecyclerView.getChildAdapterPosition(view);

        if (position != RecyclerView.NO_POSITION) {
            Upload upload = mAdapter.getItem(position);
            Intent editIntent = UploadEditActivity.createIntent(getActivity(), upload);

            if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
                View v = view.findViewById(R.id.image);

                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), v, getString(R.string.transition_upload_photo));
                startActivityForResult(editIntent, RequestCodes.UPLOAD_EDIT, options.toBundle());
            } else {
                startActivityForResult(editIntent, RequestCodes.UPLOAD_EDIT);
            }
        }
    }

    public void onLinkAdded(String link) {
        Upload upload = new Upload(link, true);

        if (mAdapter == null) {
            mAdapter = new UploadPhotoAdapter(getActivity(), upload, this);
            mRecyclerView.setAdapter(mAdapter);
        } else {
            mAdapter.addItem(upload);
        }

        mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSION_WRITE:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    startCamera();
                } else {
                    SnackBar.show(getActivity(), R.string.permission_denied);
                }
                break;

            case RequestCodes.REQUEST_PERMISSION_READ:
                if (PermissionUtils.verifyPermissions(grantResults)) {
                    if (mPhotoUris != null) new DecodeImagesTask(this).execute(mPhotoUris);
                } else {
                    SnackBar.show(getActivity(), R.string.permission_denied);
                }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.TAKE_PHOTO:
                if (resultCode == Activity.RESULT_OK && FileUtil.isFileValid(mTempFile)) {
                    FileUtil.scanFile(Uri.fromFile(mTempFile), getActivity());
                    String fileLocation = mTempFile.getAbsolutePath();
                    Upload upload = new Upload(fileLocation);

                    if (mAdapter == null) {
                        mAdapter = new UploadPhotoAdapter(getActivity(), upload, this);
                        mRecyclerView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItem(upload);
                    }

                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    mTempFile = null;
                } else {
                    SnackBar.show(getActivity(), R.string.upload_camera_error);
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
                        SnackBar.show(getActivity(), R.string.error_generic);
                    }
                }
                break;

            case RequestCodes.UPLOAD_EDIT:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Upload upload;

                    if (data.hasExtra(UploadEditActivity.KEY_UPDATED_UPLOAD)) {
                        upload = data.getParcelableExtra(UploadEditActivity.KEY_UPDATED_UPLOAD);

                        if (upload != null) {
                            mAdapter.updateItem(upload);
                        }
                    } else if (data.hasExtra(UploadEditActivity.KEY_UPDATED_DELETED)) {
                        upload = data.getParcelableExtra(UploadEditActivity.KEY_UPDATED_DELETED);

                        if (upload != null) {
                            int itemIndex = mAdapter.indexOf(upload);

                            if (itemIndex > -1) {
                                mAdapter.removeItem(itemIndex);
                                if (mListener != null) mListener.onPhotoRemoved(mAdapter.getItemCount());
                                if (mAdapter.isEmpty()) mMultiView.setViewState(MultiStateView.VIEW_STATE_EMPTY);

                                new SnackBarItem.Builder(getActivity())
                                        .setMessageResource(R.string.upload_photo_removed)
                                        .setActionMessageResource(R.string.undo)
                                        .setObject(new Object[]{itemIndex, upload})
                                        .setSnackBarListener(new SnackBarListener() {
                                            @Override
                                            public void onSnackBarStarted(Object o) {
                                                // NOOP
                                            }

                                            @Override
                                            public void onSnackBarFinished(Object object, boolean actionPressed) {
                                                if (actionPressed && object instanceof Object[]) {
                                                    Object[] objects = (Object[]) object;
                                                    int position = (int) objects[0];
                                                    Upload upload = (Upload) objects[1];
                                                    mMultiView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                                                    mAdapter.addItem(upload, position);
                                                }
                                            }
                                        })
                                        .show();
                            }

                        }
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_SAVED_ITEMS, mAdapter.retainItems());
        }
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
        WeakReference<UploadFragment> mFragment;

        boolean needsPermission = false;

        List<Uri> photoUris = null;

        public DecodeImagesTask(@NonNull UploadFragment fragment) {
            mFragment = new WeakReference<>(fragment);
        }

        @Override
        protected List<Upload> doInBackground(List<Uri>... params) {
            if (params != null && params[0] != null && !params[0].isEmpty()) {
                Activity activity = mFragment.get().getActivity();
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
                        if (FileUtil.isFileValid(file))
                            uploads.add(new Upload(file.getAbsolutePath()));
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
            if (mFragment != null && mFragment.get() != null) {
                UploadFragment fragment = mFragment.get();

                if (needsPermission) {
                    fragment.onNeedsReadPermission(photoUris);
                } else {
                    fragment.onUrisDecoded(uploads);
                }

                mFragment.clear();
                mFragment = null;
            }
        }
    }
}
