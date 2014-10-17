package com.kenny.openimgur.classes;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

public interface ImgurListener {
    void onPhotoTap(View view);

    void onPlayTap(ProgressBar prog, ImageButton play, ImageView image, VideoView video);

    void onLinkTap(View view, @Nullable String url);

    void onViewRepliesTap(View view);

    void onPhotoLongTapListener(View view);
}