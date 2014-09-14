package com.kenny.openimgur;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by kcampagna on 6/22/14.
 */
public class ViewPhotoActivity extends BaseActivity {
    private static final long HIDE_DELAY = DateUtils.SECOND_IN_MILLIS * 3;

    private static final String KEY_URL = "url";

    private static final String KEY_IMAGE = "image";

    private ImageView mImageView;

    private ProgressBar mProgressBar;

    private ImgurPhoto photo;

    private String mUrl;

    public static Intent createIntent(@NonNull Context context, @NonNull ImgurPhoto photo) {
        return new Intent(context, ViewPhotoActivity.class).putExtra(KEY_IMAGE, photo);
    }

    public static Intent createIntent(@NonNull Context context, @NonNull String url) {
        return new Intent(context, ViewPhotoActivity.class).putExtra(KEY_URL, url);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setShouldTint(false);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        if (intent == null || (!intent.hasExtra(KEY_IMAGE) && !intent.hasExtra(KEY_URL))) {
            Toast.makeText(getApplicationContext(), R.string.error_generic, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setContentView(R.layout.activity_view_photo);
        photo = getIntent().getParcelableExtra(KEY_IMAGE);
        mUrl = intent.getStringExtra(KEY_URL);
        mImageView = (ImageView) findViewById(R.id.image);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        app.getImageLoader().displayImage(photo != null ? photo.getLink() : mUrl, mImageView, ImageUtil.getDisplayOptionsForView().build(), new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {
            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                finish();
                Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                mProgressBar.setVisibility(View.GONE);
                 PhotoViewAttacher photoView = new PhotoViewAttacher(mImageView);
                 photoView.setMaximumScale(10f);

                if ((photo != null && photo.isAnimated()) || (!TextUtils.isEmpty(mUrl) && mUrl.endsWith(".gif"))) {
                    // The file SHOULD be in our cache if the image has successfully loaded
                    if (!ImageUtil.loadAndDisplayGif(mImageView, photo != null ? photo.getLink() : mUrl, app.getImageLoader())) {
                        finish();
                        Toast.makeText(getApplicationContext(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                 photoView.update();
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
            }
        });

        getActionBar().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    protected void onDestroy() {
        mImageView = null;
        mProgressBar = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_photo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.download:
                startService(DownloaderService.createIntent(getApplicationContext(), photo));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (photo == null) {
            menu.removeItem(R.id.download);
        }

        return super.onPrepareOptionsMenu(menu);
    }
}
