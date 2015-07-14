package com.kenny.openimgur.api;

/**
 * Endpoints enum for the different endpoint URL
 * https://api.imgur.com/endpoints
 */
public enum Endpoints {

    REFRESH_TOKEN("https://api.imgur.com/oauth2/token");

    private final String mUrl;

    Endpoints(String endpoint) {
        mUrl = endpoint;
    }

    public String getUrl() {
        return mUrl;
    }
}
