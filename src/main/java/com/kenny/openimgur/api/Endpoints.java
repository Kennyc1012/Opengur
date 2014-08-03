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

    LOGIN("https://api.imgur.com/oauth2/authorize?client_id=" + ApiClient.CLIENT_ID + "&response_type=token"),

    REFRESH_TOKEN("https://api.imgur.com/oauth2/token"),

    // Username
    PROFILE("https://api.imgur.com/3/account/%s"),

    // Username
    ACCOUNT_GALLERY_FAVORITES("https://api.imgur.com/3/account/%s/gallery_favorites"),

    // Username/Page
    ACCOUNT_SUBMISSIONS("https://api.imgur.com/3/account/%s/submissions/%d"),

    // comment id/vote
    COMMENT_VOTE("https://api.imgur.com/3/comment/%s/vote/%s"),

    GALLERY_VOTE("https://api.imgur.com/3/gallery/%s/vote/%s");

    private final String mUrl;

    Endpoints(String endpoint) {
        mUrl = endpoint;
    }

    public String getUrl() {
        return mUrl;
    }
}
