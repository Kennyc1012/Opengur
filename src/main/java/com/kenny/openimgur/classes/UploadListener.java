package com.kenny.openimgur.classes;

public interface UploadListener {

    void onPhotoAdded();

    void onPhotoRemoved(int remaining);
}
