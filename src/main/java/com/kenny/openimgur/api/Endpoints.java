package com.kenny.openimgur.api;

/**
 * Endpoints enum for the different endpoint URL
 * https://api.imgur.com/endpoints
 */
public enum Endpoints {

    REFRESH_TOKEN("https://api.imgur.com/oauth2/token"),

    // comment id/vote
    COMMENT_VOTE("https://api.imgur.com/3/comment/%s/vote/%s"),

    // id,vote
    GALLERY_VOTE("https://api.imgur.com/3/gallery/%s/vote/%s"),

    // Image id
    FAVORITE_IMAGE("https://api.imgur.com/3/image/%s/favorite"),

    // Album id
    FAVORITE_ALBUM("https://api.imgur.com/3/album/%s/favorite"),

    // Album/Image id
    COMMENT("https://api.imgur.com/3/gallery/%s/comment"),

    // albumid or imageid / comment parent
    COMMENT_REPLY("https://api.imgur.com/3/gallery/%s/comment/%s"),

    UPLOAD("https://api.imgur.com/3/upload"),

    // Image/Album id
    GALLERY_UPLOAD("https://api.imgur.com/3/gallery/%s"),

    // ConvoId, page
    MESSAGES("https://api.imgur.com/3/conversations/%s/%d/0"),

    // recipient
    SEND_MESSAGE("https://api.imgur.com/3/conversations/%s"),

    // Convo Id
    DELETE_CONVO("https://api.imgur.com/3/conversations/%s"),

    // Delete hash or image id if owned by account
    IMAGE_DELETE("https://api.imgur.com/3/image/%s"),

    // Username
    CONVO_REPORT("https://api.imgur.com/3/conversations/report/%s"),

    CONVO_BLOCK("https://api.imgur.com/3/conversations/block/%s"),

    ALBUM_CREATION("https://api.imgur.com/3/album"),

    // Albumid/delete hash
    ALBUM_DELETE("https://api.imgur.com/3/album/%s");

    private final String mUrl;

    Endpoints(String endpoint) {
        mUrl = endpoint;
    }

    public String getUrl() {
        return mUrl;
    }
}
