package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.cocosw.bottomsheet.BottomSheet;
import com.cocosw.bottomsheet.BottomSheetListener;
import com.diegocarloslima.byakugallery.lib.TileBitmapDrawable;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.lang.ref.WeakReference;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by Kenny-PC on 3/8/2015.
 */
public class MemeActivity extends BaseActivity {
    private static final String KEY_OBJECT = "imgur_object";

    @InjectView(R.id.image)
    ImageView mImage;

    @InjectView(R.id.topText)
    TextView mTopText;

    @InjectView(R.id.bottomText)
    TextView mBottomText;

    @InjectView(R.id.content)
    View mView;

    @InjectView(R.id.progressBar)
    ProgressBar mProgressBar;

    private ImgurBaseObject mObject;

    public static Intent createIntent(Context context, ImgurBaseObject object) {
        return new Intent(context, MemeActivity.class).putExtra(KEY_OBJECT, object);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meme_create);
        Intent intent = getIntent();

        if (intent == null || !intent.hasExtra(KEY_OBJECT)) {
            LogUtil.w(TAG, "No object was found in the intent");
            // Toast error message
            finish();
        }

        mObject = intent.getParcelableExtra(KEY_OBJECT);
        getSupportActionBar().setTitle(mObject.getTitle());
        loadImage();
        mView.setDrawingCacheEnabled(true);
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
                LoadingDialogFragment.createInstance(R.string.meme_saving, false)
                        .show(getFragmentManager(), "saving");

                // Clear any memory cache before we save to avoid an OOM
                app.getImageLoader().clearMemoryCache();
                TileBitmapDrawable.clearCache();
                new SaveMemeTask().execute(this);
                return true;

            case R.id.fontSize:
                new MaterialDialog.Builder(this)
                        .title(R.string.meme_font_size)
                        .items(R.array.meme_font_sizes)
                        .itemsCallback(new MaterialDialog.ListCallback() {
                            @Override
                            public void onSelection(MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                float textSize;

                                switch (i) {
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

    private void loadImage() {
        mTopText.setVisibility(View.GONE);
        mBottomText.setVisibility(View.GONE);

        app.getImageLoader().displayImage(mObject.getLink(), mImage, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                mProgressBar.setVisibility(View.GONE);
                mTopText.setVisibility(View.VISIBLE);
                mBottomText.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                super.onLoadingFailed(imageUri, view, failReason);
                mProgressBar.setVisibility(View.GONE);
                finish();
                // Toast error message
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                super.onLoadingCancelled(imageUri, view);
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                super.onLoadingStarted(imageUri, view);
                mProgressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void onMemeCreated(@Nullable File file) {
        dismissDialogFragment("saving");

        if (FileUtil.isFileValid(file)) {
            final Uri fileUri = Uri.fromFile(file);
            FileUtil.scanFile(fileUri, getApplicationContext());
            new BottomSheet.Builder(this, R.style.BottomSheet_StyleDialog)
                    .title(R.string.meme_success)
                    .sheet(R.menu.meme_saved)
                    .listener(new BottomSheetListener() {
                        @Override
                        public void onSheetDismissed(@Nullable Object o) {
                            finish();
                        }

                        @Override
                        public void onItemClicked(int id, @Nullable Object o) {
                            switch (id) {

                                case R.id.share:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType(ImgurPhoto.IMAGE_TYPE_JPEG);
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                    startActivity(shareIntent);
                                    break;

                                case R.id.upload:
                                    Intent uploadIntent = new Intent(getApplicationContext(), UploadActivity.class);
                                    uploadIntent.setAction(Intent.ACTION_SEND);
                                    uploadIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                                    startActivity(uploadIntent);
                                    break;
                            }
                        }

                        @Override
                        public void onSheetShown(@Nullable Object o) {

                        }
                    }).show();
        } else {
            SnackBar.show(this, R.string.meme_failed);
        }
    }

    @OnClick({R.id.topText, R.id.bottomText})
    public void onClick(final View view) {
        new MaterialDialog.Builder(this)
                .title(R.string.meme_input_title)
                .customView(R.layout.meme_caption, false)
                .negativeText(R.string.cancel)
                .positiveText(R.string.save)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        EditText editText = (EditText) dialog.getCustomView().findViewById(R.id.memeCaption);
                        String text = editText.getText().toString();

                        if (view == mBottomText) {
                            mBottomText.setText(text);
                        } else if (view == mTopText) {
                            mTopText.setText(text);
                        }
                    }
                }).show();
    }

    private static class SaveMemeTask extends AsyncTask<MemeActivity, Void, File> {
        private static final String TAG = "SaveMemeTask";

        WeakReference<MemeActivity> mActivity;

        @Override
        protected File doInBackground(MemeActivity... params) {
            if (params == null || params.length <= 0) {
                throw new IllegalArgumentException("No Activity passed to save bitmap");
            }

            mActivity = new WeakReference<>(params[0]);
            Bitmap drawingCahce = mActivity.get().mView.getDrawingCache();

            try {
                if (drawingCahce != null) {
                    File file = ImageUtil.saveBitmap(drawingCahce);

                    if (FileUtil.isFileValid(file)) {
                        LogUtil.v(TAG, "Meme saved successfully");

                        return file;
                    }
                }
            } catch (Exception ex) {
                LogUtil.e(TAG, "Error saving Meme bitmap", ex);
            } finally {
                if (drawingCahce != null) drawingCahce.recycle();
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
