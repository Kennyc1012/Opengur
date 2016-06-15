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

    /**
     * Called when the photos should start uploading
     *
     * @param submitToGallery If they should be submitted to the Imgur gallery
     * @param title           Optional title
     * @param description     Optional Description
     * @param topic           Optional Topic
     */
    void onUpload(boolean submitToGallery, String title, String description, ImgurTopic topic);

}
