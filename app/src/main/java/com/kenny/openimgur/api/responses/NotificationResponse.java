package com.kenny.openimgur.api.responses;

import android.support.annotation.NonNull;

import com.kenny.openimgur.classes.ImgurComment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kenny-PC on 8/8/2015.
 */
public class NotificationResponse extends BaseResponse {
    public Data data;

    public boolean hasNotifications() {
        return data != null && !data.replies.isEmpty();
    }

    public static class Data {
        @NonNull
        public List<Replies> replies = new ArrayList<>();
    }

    public static class Replies {
        public int id;

        public boolean viewed;

        public ImgurComment content;
    }
}
