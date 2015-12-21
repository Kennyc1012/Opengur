package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.EditText;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.Upload;

import butterknife.Bind;

public class UploadEditActivity extends BaseActivity {
    private static final String KEY_UPLOAD = "upload";

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
        mUpload = getIntent().getParcelableExtra(KEY_UPLOAD);
        app.getImageLoader().displayImage("file://" + mUpload.getLocation(), mImage);
        mTitle.setText(mUpload.getTitle());
        mDescription.setText(mUpload.getDescription());
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Upload : R.style.Theme_Opengur_Light_DarkActionBar_Upload;
    }
}
