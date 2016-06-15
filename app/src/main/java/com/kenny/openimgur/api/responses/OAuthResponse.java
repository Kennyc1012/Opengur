package com.kenny.openimgur.api.responses;

/**
 * Created by kcampagna on 7/15/15.
 */
public class OAuthResponse extends BaseResponse {
    public String access_token;

    public String refresh_token;

    public long expires_in;
}
