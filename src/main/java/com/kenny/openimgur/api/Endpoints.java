package com.kenny.openimgur.api;

/**
 * Endpoints enum for the different endpoint URL
 * https://api.imgur.com/endpoints
 */
public enum Endpoints {

    REFRESH_TOKEN("https://api.imgur.com/oauth2/token"),

    // Username
    CONVO_REPORT("https://api.imgur.com/3/conversations/report/%s"),

    CONVO_BLOCK("https://api.imgur.com/3/conversations/block/%s");

    private final String mUrl;

    Endpoints(String endpoint) {
        mUrl = endpoint;
    }

    public String getUrl() {
        return mUrl;
    }
}
