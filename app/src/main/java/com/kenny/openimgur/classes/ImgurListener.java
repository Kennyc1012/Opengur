package com.kenny.openimgur.classes;

import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.kenny.openimgur.ui.VideoView;

public interface ImgurListener {
    void onPhotoTap(View view);

    void onPlayTap(ProgressBar prog, FloatingActionButton play, ImageView image, VideoView video, View root);

    void onLinkTap(View view, @Nullable String url);

    void onViewRepliesTap(View view);

    void onPhotoLongTapListener(View view);
}