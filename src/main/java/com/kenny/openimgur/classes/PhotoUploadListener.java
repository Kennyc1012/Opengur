package com.kenny.openimgur.classes;

import android.view.View;

/**
 * Created by Kenny-PC on 6/28/2015.
 */
public interface PhotoUploadListener {

    void onLinkAdded(String link);

    void onItemClicked(View view);

    void onUpload(boolean submitToGallery, String title, String description, ImgurTopic topic);
}
