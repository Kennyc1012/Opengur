package com.kenny.openimgur.api;

import android.support.annotation.Nullable;

import org.json.JSONObject;

/**
 * Class for handling events on the bus
 */
public class ImgurBusEvent {
    public enum EventType {
        GALLERY,
        COMMENTS,
        ITEM_DETAILS,
        ALBUM_DETAILS,
        PROFILE_DETAILS,
        ACCOUNT_GALLERY_FAVORITES,
        ACCOUNT_SUBMISSIONS,
        COMMENT_POSTING,
        COMMENT_VOTE,
        GALLERY_VOTE,
        FAVORITE,
        GALLERY_ITEM_INFO,
        UPLOAD,
        GALLERY_SUBMISSION,
        REDDIT_SEARCH,
        ACCOUNT_COMMENTS
    }

    public JSONObject json;

    public EventType eventType;

    public ApiClient.HttpRequest httpRequest;

    // An optional unique ID for the event
    @Nullable
    public String id;

    public ImgurBusEvent(JSONObject json, EventType eventType, ApiClient.HttpRequest httpRequest, String id) {
        this.json = json;
        this.eventType = eventType;
        this.httpRequest = httpRequest;
        this.id = id;
    }
}