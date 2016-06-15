package com.kenny.openimgur.api.responses;

/**
 * Created by kcampagna on 7/13/15.
 */
public class CommentPostResponse extends BaseResponse {
    public Data data;

    public static class Data {
        public String id;
    }
}
