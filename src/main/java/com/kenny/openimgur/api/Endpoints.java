package com.kenny.openimgur.api;

/**
 * Endpoints enum for the different endpoint URL
 * https://api.imgur.com/endpoints
 */
public enum Endpoints {
    // section/sort/page
    GALLERY("https://api.imgur.com/3/gallery/%s/%s/%d"),

    // item id
    IMAGE_DETAILS("https://api.imgur.com/3/image/%s"),

    // albumid
    ALBUM("https://api.imgur.com/3/gallery/%s/images"),

    //cover id
    ALBUM_COVER("https://i.imgur.com/%s.jpg"),

    // albumid or imageid
    COMMENTS("https://api.imgur.com/3/gallery/%s/comments"),

    // Client-ID
    LOGIN("https://api.imgur.com/oauth2/authorize?client_id=%s&response_type=token");

    private String mUrl;

    Endpoints(String endpoint) {
        mUrl = endpoint;
    }

    public String getUrl() {
        return mUrl;
    }
}
