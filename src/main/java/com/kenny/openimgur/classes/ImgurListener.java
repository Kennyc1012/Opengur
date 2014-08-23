package com.kenny.openimgur.classes;

import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

public interface ImgurListener {
    public static final String REGEX_IMAGE_URL = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://\\S+(.jpg|.jpeg|.gif|.png)$";

    public static final String REGEX_IMGUR_IMAGE = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/(?!=\\/)\\w+$";

    void onPhotoTap(ImageView parent);

    void onPlayTap(ProgressBar prog, ImageView image, ImageButton play);

    void onLinkTap(View view, @Nullable String url);

    void onViewRepliesTap(View view);
}