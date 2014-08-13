package com.kenny.openimgur.classes;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

public interface ImgurListener {
    void onPhotoTap(ImageView parent);

    void onPlayTap(ProgressBar prog, ImageView image, ImageButton play);

    void onLinkTap(View view, @Nullable String url);

    void onViewRepliesTap(View view);
}