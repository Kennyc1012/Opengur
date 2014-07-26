package com.kenny.openimgur.classes;

import android.os.Handler;

/**
 * Created by kcampagna on 7/21/14.
 */
public class ImgurHandler extends Handler {
    public static final int MESSAGE_ACTION_COMPLETE = 0;

    public static final int MESSAGE_ACTION_FAILED = -1;

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
