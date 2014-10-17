package com.kenny.openimgur.util;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kcampagna on 9/8/14.
 */
public class LinkUtils {
    private static final String REGEX_IMAGE_URL = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://\\S+(.jpg|.jpeg|.gif|.png)$";

    private static final String REGEX_VIDEO_URL = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\S+(.mp4|.gifv)$";

    private static final String REGEX_IMGUR_IMAGE = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_GALLERY = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/gallery\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_USER = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/user\\/(?!=\\/)\\w+$";

    // Pattern used to extra an ID from a url
    private static final Pattern ID_PATTERN = Pattern.compile(".com\\/(.*)\\W");

    public static enum LinkMatch {
        IMAGE_URL,
        VIDEO_URL,
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
            } else if (url.matches(REGEX_VIDEO_URL)) {
                match = LinkMatch.VIDEO_URL;
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

    /**
     * Extracts the ID of an image from a URL. This will not work for album links (/a/abcde)
     *
     * @param url The url of the image
     * @return The id of the image, or null if not found
     */
    public static String getId(String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }

        Matcher match = ID_PATTERN.matcher(url);

        if (match.find()) {
            return match.group(1);
        }

        return null;
    }
}
