package com.kenny.openimgur.activities;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.ViewUtils;

import butterknife.BindView;
import butterknife.OnClick;

public class UploadEditActivity extends BaseActivity {
    private static final long FADE_DURATION = 200L;

    private static final String KEY_UPLOAD = "upload";

    public static final String KEY_UPDATED_UPLOAD = "updated_upload";

    public static final String KEY_UPDATED_DELETED = "updated_deleted";

    @BindView(R.id.image)
    ImageView mImage;

    @BindView(R.id.title)
    EditText mTitle;

    @BindView(R.id.desc)
    EditText mDescription;

    @BindView(R.id.titleWrapper)
    TextInputLayout mTitleWrapper;

    @BindView(R.id.descWrapper)
    TextInputLayout mDescWrapper;

    @BindView(R.id.fab)
    FloatingActionButton mFab;

    private Upload mUpload;

    public static Intent createIntent(Context context, @NonNull Upload upload) {
        return new Intent(context, UploadEditActivity.class).putExtra(KEY_UPLOAD, upload);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_edit);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#33000000")));
        mUpload = getIntent().getParcelableExtra(KEY_UPLOAD);
        String url = mUpload.isLink() ? mUpload.getLocation() : "file://" + mUpload.getLocation();
        ImageUtil.getImageLoader(this).displayImage(url, mImage, ImageUtil.getDisplayOptionsForPhotoPicker().build());
        mTitle.setText(mUpload.getTitle());
        mDescription.setText(mUpload.getDescription());

        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP) && savedInstanceState == null) {
            getWindow().getEnterTransition().addListener(new Transition.TransitionListener() {
                @Override
                public void onTransitionStart(Transition transition) {
                    // NOOP
                }

                @Override
                public void onTransitionEnd(Transition transition) {
                    mTitleWrapper.setVisibility(View.VISIBLE);
                    mDescWrapper.setVisibility(View.VISIBLE);
                    ObjectAnimator.ofFloat(mTitleWrapper, "alpha", 0.0f, 1.0f).setDuration(FADE_DURATION).start();
                    ObjectAnimator.ofFloat(mDescWrapper, "alpha", 0.0f, 1.0f).setDuration(FADE_DURATION).start();
                    mFab.show();
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
        } else {
            mTitleWrapper.setVisibility(View.VISIBLE);
            mDescWrapper.setVisibility(View.VISIBLE);
            mFab.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.upload_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onPause() {
        mTitle.setCursorVisible(false);
        mDescription.setCursorVisible(false);
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                Intent intent = new Intent();
                intent.putExtra(KEY_UPDATED_DELETED, mUpload);
                setResult(Activity.RESULT_OK, intent);
                finishActivity();
                return true;

            case android.R.id.home:
                finishActivity();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.fab)
    public void onFabClick() {
        Intent intent = processUpdates();
        if (intent != null) {
            setResult(Activity.RESULT_OK, intent);
        }

        finishActivity();
    }

    @Nullable
    private Intent processUpdates() {
        String desc = mDescription.getText().toString();
        String title = mTitle.getText().toString();

        if (TextUtils.isEmpty(desc) && TextUtils.isEmpty(mUpload.getDescription())
                && TextUtils.isEmpty(title) && TextUtils.isEmpty(mUpload.getTitle())) {
            return null;
        }

        if (!desc.equals(mUpload.getDescription()) || !title.equals(mUpload.getTitle())) {
            // Something was changed, return it.
            Intent intent = new Intent();
            Upload upload = new Upload(mUpload.getLocation(), mUpload.isLink());
            upload.setTitle(title);
            upload.setDescription(desc);
            intent.putExtra(KEY_UPDATED_UPLOAD, upload);
            return intent;
        }

        return null;
    }

    @Override
    public void onBackPressed() {
        finishActivity();
    }

    private void finishActivity() {
        // Set these views to gone so the transitions look smooth
        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
            mFab.hide();
            mTitleWrapper.animate().alpha(0.0f).setDuration(FADE_DURATION);
            mDescWrapper.animate().alpha(0.0f).setDuration(FADE_DURATION).withEndAction(new Runnable() {
                @Override
                public void run() {
                    supportFinishAfterTransition();
                }
            });

            return;
        }

        supportFinishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ViewUtils.fixTransitionLeak(this);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Upload_Edit : R.style.Theme_Opengur_Light_DarkActionBar_Upload_Edit;
    }
}
