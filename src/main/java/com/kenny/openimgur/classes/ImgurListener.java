package com.kenny.openimgur.classes;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public interface ImgurListener {
    void onPhotoTap(ImageView parent);

    void onPlayTap(ProgressBar prog, ImageView image, ImageButton play);

    void onLinkTap(TextView textView, String url);

    void onViewRepliesTap(View view);
}