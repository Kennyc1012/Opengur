package com.kenny.openimgur.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.Upload;

import butterknife.Bind;

public class UploadEditActivity extends BaseActivity {
    private static final String KEY_UPLOAD = "upload";

    public static final String KEY_UPDATED_UPLOAD = "updated_upload";

    @Bind(R.id.image)
    ImageView mImage;

    @Bind(R.id.title)
    EditText mTitle;

    @Bind(R.id.desc)
    EditText mDescription;

    private Upload mUpload;

    public static Intent createIntent(Context context, @NonNull Upload upload) {
        return new Intent(context, UploadEditActivity.class).putExtra(KEY_UPLOAD, upload);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_edit);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mUpload = getIntent().getParcelableExtra(KEY_UPLOAD);
        String url = mUpload.isLink() ? mUpload.getLocation() : "file://" + mUpload.getLocation();
        app.getImageLoader().displayImage(url, mImage);
        mTitle.setText(mUpload.getTitle());
        mDescription.setText(mUpload.getDescription());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = processUpdates();
            if (intent != null) {
                setResult(Activity.RESULT_OK, intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = processUpdates();

        if (intent != null) {
            setResult(Activity.RESULT_OK, intent);
        }

        super.onBackPressed();
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
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Upload_Edit : R.style.Theme_Opengur_Light_DarkActionBar_Upload_Edit;
    }
}
