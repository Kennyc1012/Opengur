package com.kenny.openimgur.classes;

public interface UploadListener {

    /**
     * Called when a photo is added
     */
    void onPhotoAdded();

    /**
     * Called when a photo is removed
     *
     * @param remaining The number of remaining photos
     */
    void onPhotoRemoved(int remaining);
}
