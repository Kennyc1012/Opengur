package com.kenny.openimgur.classes;

/**
 * Created by Kenny-PC on 6/28/2015.
 */
public interface PhotoUploadListener {

    void onLinkAdded(String link);

    void onItemClicked(int position);

    void onItemEdited(Upload upload);
}
