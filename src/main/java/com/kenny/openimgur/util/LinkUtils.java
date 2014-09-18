package com.kenny.openimgur.util;

import android.text.TextUtils;

/**
 * Created by kcampagna on 9/8/14.
 */
public class LinkUtils {
    private static final String REGEX_IMAGE_URL = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://\\S+(.jpg|.jpeg|.gif|.png)$";

    private static final String REGEX_IMGUR_IMAGE = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_GALLERY = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/gallery\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_USER = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/user\\/(?!=\\/)\\w+$";

    public static enum LinkMatch {
        IMAGE_URL,
        IMAGE,
        GALLERY,
        USER,
        NONE
    }

    /**
     * Find the type of imgur link that belongs to the url
     *
     * @param url
     * @return
     */
    public static LinkMatch findImgurLinkMatch(String url) {
        LinkMatch match = LinkMatch.NONE;

        if (!TextUtils.isEmpty(url)) {
            if (url.matches(REGEX_IMAGE_URL)) {
                match = LinkMatch.IMAGE_URL;
            } else if (url.matches(REGEX_IMGUR_IMAGE)) {
                match = LinkMatch.IMAGE;
            } else if (url.matches(REGEX_IMGUR_GALLERY)) {
                match = LinkMatch.GALLERY;
            } else if (url.matches(REGEX_IMGUR_USER)) {
                match = LinkMatch.USER;
            }
        }

        return match;
    }
}
