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

    /**
     * Sends a message to the handler
     *
     * @param what
     * @param obj
     */
    public void sendMessage(int what, Object obj) {
        sendMessage(obtainMessage(what, obj));
    }
}
