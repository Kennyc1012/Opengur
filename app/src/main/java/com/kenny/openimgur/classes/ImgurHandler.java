package com.kenny.openimgur.classes;

import android.os.Handler;

/**
 * Created by kcampagna on 7/21/14.
 */
public class ImgurHandler extends Handler {
    public static final int MESSAGE_ACTION_FAILED = -1;

    public static final int MESSAGE_ACTION_COMPLETE = 0;

    public static final int MESSAGE_SEARCH_URL = 1;

    public static final int MESSAGE_GALLERY_VOTE_COMPLETE = 2;

    public static final int MESSAGE_COMMENT_POSTING = 3;

    public static final int MESSAGE_COMMENT_POSTED = 4;

    public static final int MESSAGE_COMMENT_VOTED = 5;

    public static final int MESSAGE_EMPTY_RESULT = 6;

    public static final int MESSAGE_ITEM_DETAILS = 7;

    public static final int MESSAGE_MESSAGE_SENT = 8;

    public static final int MESSAGE_IMAGE_DELETED = 9;

    public static final int MESSAGE_CONVO_BLOCKED = 10;

    public static final int MESSAGE_CONVO_REPORTED = 11;

    public static final int MESSAGE_TAGS_RECEIVED = 12;

    public static final int MESSAGE_ALBUM_DELETED = 13;

    /**
     * Sends a message to the handler
     *
     * @param what
     * @param obj
     */
    public void sendMessage(int what, Object obj) {
        sendMessage(obtainMessage(what, obj));
    }

    public void sendMessageDelayed(int what, Object obj, long delay) {
        sendMessageDelayed(obtainMessage(what, obj), delay);
    }
}
