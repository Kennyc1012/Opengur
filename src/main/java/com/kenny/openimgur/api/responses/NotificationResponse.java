package com.kenny.openimgur.api.responses;

import android.support.annotation.NonNull;

import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kenny-PC on 8/8/2015.
 */
public class NotificationResponse extends BaseResponse {
    public Data data;

    public static class Data {
        @NonNull
        List<Replies> replies = new ArrayList<>();

        @NonNull
        List<Messages> messages = new ArrayList<>();
    }

    public static class Replies {
        public String id;
        public String account_id;
        public boolean viewed;
        public ImgurComment content;
    }

    public static class Messages {
        public String id;
        public String account_id;
        public boolean viewed;
        public ImgurMessage content;
    }
}
