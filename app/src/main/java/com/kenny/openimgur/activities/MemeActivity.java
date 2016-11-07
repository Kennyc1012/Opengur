package com.kenny.openimgur.activities;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.transition.Transition;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.PermissionUtils;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.bottomsheet.BottomSheet;
import com.kennyc.bottomsheet.BottomSheetListener;
import com.kennyc.view.MultiStateView;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by Kenny-PC on 3/8/2015.
 */
public class MemeActivity extends BaseActivity {
    private static final String KEY_OBJECT = "imgur_object";

    private static final String KEY_FILE_PATH = "file_path";

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.topText)
    TextView mTopText;

    @BindView(R.id.bottomText)
    TextView mBottomText;

    @BindView(R.id.content)
    View mView;

    @BindView(R.id.multiView)
    MultiStateView mMultiStateView;

    private ImgurBaseObject mObject;

    public static Intent createIntent(Context context, ImgurBaseObject object) {
        return new Intent(context, MemeActivity.class).putExtra(KEY_OBJECT, object);
    }

    public static Intent createIntent(Context context, @NonNull File file) {
        return new Intent(context, MemeActivity.class).putExtra(KEY_FILE_PATH, file.getAbsolutePath());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meme_create);
        Intent intent = getIntent();
        boolean hasTransition;

        if (intent == null) {
            LogUtil.w(TAG, "No object was found in the intent");
            finish();
            return;
        }

        if (intent.hasExtra(KEY_OBJECT)) {
            mObject = intent.getParcelableExtra(KEY_OBJECT);
            hasTransition = isApiLevel(Build.VERSION_CODES.LOLLIPOP) && savedInstanceState == null;
        } else {
            String path = intent.getStringExtra(KEY_FILE_PATH);
            mObject = new ImgurBaseObject("-1", null, "file:///" + path);
            // Locally imported images will not have an activity transition
            hasTransition = false;
        }

        getSupportActionBar().setTitle(mObject.getTitle());
        loadImage(hasTransition);
        mView.setDrawingCacheEnabled(true);

        if (hasTransition) {
            getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    // NOOP
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    mTopText.setVisibility(View.VISIBLE);
                    mBottomText.setVisibility(View.VISIBLE);
                    ObjectAnimator.ofFloat(mTopText, "alpha", 0.0f, 1.0f).setDuration(200).start();
                    ObjectAnimator.ofFloat(mBottomText, "alpha", 0.0f, 1.0f).setDuration(200).start();
                    getWindow().getEnterTransition().removeListener(this);
                }

                @Override
                public void onTransitionCancel(Transition transition) {
                    // NOOP
                }

                @Override
                public void onTransitionPause(Transition transition) {
                    // NOOP
                }

                @Override
                public void onTransitionResume(Transition transition) {
                    // NOOP
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.meme, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                int level = PermissionUtils.getPermissionLevel(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

                switch (level) {
                    case PermissionUtils.PERMISSION_AVAILABLE:
                        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

                        // Clear any memory cache before we save to avoid an OOM
                        ImageUtil.getImageLoader(this).clearMemoryCache();
                        new SaveMemeTask().execute(this);
                        break;

                    case PermissionUtils.PERMISSION_NEVER_ASKED:
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                        break;

                    case PermissionUtils.PERMISSION_DENIED:
                        new AlertDialog.Builder(this, app.getImgurTheme().getAlertDialogTheme())
                                .setTitle(R.string.permission_title)
                                .setMessage(R.string.permission_rational_meme)
                                .setNegativeButton(R.string.cancel, null)
                                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        ActivityCompat.requestPermissions(MemeActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, RequestCodes.REQUEST_PERMISSION_WRITE);
                                    }
                                }).show();
                        break;
                }
                return true;

            case R.id.fontSize:
                new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                        .setTitle(R.string.meme_font_size)
                        .setItems(R.array.meme_font_sizes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                float textSize;
                                switch (which) {
                                    case 0:
                                        // Small
                                        textSize = getResources().getDimensionPixelSize(R.dimen.meme_text_small);
                                        break;

                                    case 1:
                                        // Medium
                                        textSize = getResources().getDimensionPixelSize(R.dimen.meme_text_medium);
                                        break;

                                    case 2:
                                        // Large
                                        textSize = getResources().getDimensionPixelSize(R.dimen.meme_text_large);
                                        break;

                                    case 3:
                                        // X Large
                                        textSize = getResources().getDimensionPixelSize(R.dimen.meme_text_xlarge);
                                        break;

                                    default:
                                        LogUtil.v(TAG, "Unable to determine text size selected, defaulting to medium");
                                        textSize = getResources().getDimensionPixelSize(R.dimen.meme_text_medium);
                                        break;
                                }

                                mTopText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                                mBottomText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                            }
                        }).show();

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadImage(boolean hasTransition) {
        if (hasTransition) {
            mTopText.setVisibility(View.GONE);
            mBottomText.setVisibility(View.GONE);
            ActivityCompat.postponeEnterTransition(this);
        }

        ImageUtil.getImageLoader(this).displayImage(mObject.getLink(), mImage, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                ActivityCompat.startPostponedEnterTransition(MemeActivity.this);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                super.onLoadingFailed(imageUri, view, failReason);
                finish();
                // Toast error message
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                super.onLoadingCancelled(imageUri, view);
                finish();
            }

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                super.onLoadingStarted(imageUri, view);
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            }
        });
    }

    void onMemeCreated(@Nullable final File file) {
        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        dismissDialogFragment("saving");

        if (FileUtil.isFileValid(file)) {
            final Uri fileUri = Uri.fromFile(file);
            FileUtil.scanFile(fileUri, getApplicationContext());
            new BottomSheet.Builder(this)
                    .setSheet(R.menu.meme_saved)
                    .setStyle(theme.getBottomSheetTheme())
                    .setTitle(R.string.meme_success)
                    .setListener(new BottomSheetListener() {
                        @Override
                        public void onSheetShown(BottomSheet bottomSheet) {
                            // NOOP
                        }

                        @Override
                        public void onSheetItemSelected(BottomSheet bottomSheet, MenuItem menuItem) {
                            switch (menuItem.getItemId()) {
                                case R.id.share:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType(ImgurPhoto.IMAGE_TYPE_JPEG);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                    startActivity(shareIntent);
                                    break;

                                case R.id.upload:
                                    startActivity(UploadActivity.createIntent(getApplicationContext(), file));
                                    break;
                            }
                        }

                        @Override
                        public void onSheetDismissed(BottomSheet bottomSheet, @DismissEvent int dismissEvent) {
                            finish();
                        }
                    })
                    .show();
        } else {
            Snackbar.make(mMultiStateView, R.string.meme_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    @OnClick({R.id.topText, R.id.bottomText})
    public void onClick(final View view) {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.meme_input_title)
                .setView(R.layout.meme_caption)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog ad = (AlertDialog) dialog;
                        EditText editText = (EditText) ad.findViewById(R.id.memeCaption);
                        String text = editText.getText().toString();

                        if (view == mBottomText) {
                            mBottomText.setText(text);
                        } else if (view == mTopText) {
                            mTopText.setText(text);
                        }
                    }
                }).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ViewUtils.fixTransitionLeak(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case RequestCodes.REQUEST_PERMISSION_WRITE:
                boolean hasPermission = PermissionUtils.verifyPermissions(grantResults);

                if (hasPermission) {
                    mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);

                    // Clear any memory cache before we save to avoid an OOM
                    ImageUtil.getImageLoader(this).clearMemoryCache();
                    new SaveMemeTask().execute(this);
                } else {
                    Snackbar.make(mMultiStateView, R.string.permission_denied, Snackbar.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark : R.style.Theme_Opengur_Light_DarkActionBar;
    }

    static class SaveMemeTask extends AsyncTask<MemeActivity, Void, File> {
        private static final String TAG = "SaveMemeTask";

        WeakReference<MemeActivity> mActivity;

        @Override
        protected File doInBackground(MemeActivity... params) {
            if (params == null || params.length <= 0) {
                throw new IllegalArgumentException("No Activity passed to save bitmap");
            }

            mActivity = new WeakReference<>(params[0]);
            Bitmap drawingCache = mActivity.get().mView.getDrawingCache();

            try {
                if (drawingCache != null) {
                    File file = ImageUtil.saveBitmap(drawingCache);

                    if (FileUtil.isFileValid(file)) {
                        LogUtil.v(TAG, "Meme saved successfully");

                        return file;
                    }
                }
            } catch (Exception ex) {
                LogUtil.e(TAG, "Error saving Meme bitmap", ex);
            } finally {
                if (drawingCache != null) drawingCache.recycle();
            }

            LogUtil.v(TAG, "Unable to save Meme");
            return null;
        }

        @Override
        protected void onPostExecute(File result) {
            MemeActivity activity = mActivity.get();

            if (activity != null) {
                activity.onMemeCreated(result);
                mActivity.clear();
                mActivity = null;
            }
        }
    }
}
