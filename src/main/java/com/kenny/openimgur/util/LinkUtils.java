package com.kenny.openimgur.util;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.classes.ImgurPhoto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kcampagna on 9/8/14.
 */
public class LinkUtils {
    private static final String TAG = LinkUtils.class.getSimpleName();

    private static final String REGEX_IMAGE_URL = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://\\S+(.jpg|.jpeg|.gif|.png)$";

    private static final String REGEX_IMGUR_IMAGE = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_GALLERY = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/gallery\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_USER = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/user\\/.+";

    private static final String REGEX_IMGUR_ALBUM = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/a\\/(?!=\\/)\\w+$";

    private static final String REGEX_IMGUR_DIRECT_LINK = "^([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/" +
            "(m.imgur.com|imgur.com|i.imgur.com)\\/(?!=\\/)\\w+(.jpg|.jpeg|.gif|.png|.gifv|.mp4|.webm)$";

    private static final String REGEX_IMGUR_USER_CALLOUT = "@\\w+";

    private static final String REGEX_IMAGE_URL_QUERY = "([hH][tT][tT][pP]|[hH][tT][tT][pP][sS])://\\S+(.jpg|.jpeg|.gif|.png)\\?\\w+$";

    private static final String REGEX_IMGUR_PHOTO_PNG = "([hH][tT][tT][pP]|[hH][tT][tT][pP][sS]):\\/\\/(m.imgur.com\\/|imgur.com\\/|i.imgur.com\\/)\\w+\\.png$";

    // Pattern used to extra an ID from a url
    private static final Pattern ID_PATTERN = Pattern.compile(".com\\/(.*)\\W");

    // Pattern used to extra a username from a url
    private static final Pattern USER_PATTERN = Pattern.compile("(?<=/user/)(?!=/)\\w+");

    public enum LinkMatch {
        IMAGE_URL,
        IMAGE,
        GALLERY,
        USER,
        ALBUM,
        DIRECT_LINK,
        USER_CALLOUT,
        IMAGE_URL_QUERY,
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
            if (url.matches(REGEX_IMGUR_DIRECT_LINK)) {
                match = LinkMatch.DIRECT_LINK;
            } else if (url.matches(REGEX_IMAGE_URL)) {
                match = LinkMatch.IMAGE_URL;
            } else if (url.matches(REGEX_IMGUR_IMAGE)) {
                match = LinkMatch.IMAGE;
            } else if (url.matches(REGEX_IMGUR_GALLERY)) {
                match = LinkMatch.GALLERY;
            } else if (url.matches(REGEX_IMGUR_USER)) {
                match = LinkMatch.USER;
            } else if (url.matches(REGEX_IMGUR_ALBUM)) {
                match = LinkMatch.ALBUM;
            } else if (url.matches(REGEX_IMGUR_USER_CALLOUT)) {
                match = LinkMatch.USER_CALLOUT;
            } else if (url.matches(REGEX_IMAGE_URL_QUERY)) {
                match = LinkMatch.IMAGE_URL_QUERY;
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
    @Nullable
    public static String getId(@Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }

        Matcher match = ID_PATTERN.matcher(url);

        if (match.find()) {
            String id = match.group(1);
            LogUtil.v(TAG, "Id " + id + " extracted from url " + url);
            return id;
        }

        return null;
    }

    /**
     * Returns a username from a given url
     *
     * @param url
     * @return
     */
    @Nullable
    public static String getUsername(@Nullable String url) {
        if (TextUtils.isEmpty(url)) return null;
        Matcher match = USER_PATTERN.matcher(url);

        if (match.find()) {
            String username = match.group();
            LogUtil.v(TAG, "Username " + username + " extracted from url " + url);
            return username;
        }

        return null;
    }

    /**
     * Returns the Image Type based on the url
     *
     * @param url
     * @return
     */
    public static String getImageType(String url) {
        if (TextUtils.isEmpty(url)) return null;
        url = url.toLowerCase();

        if (url.endsWith("jpeg") || url.endsWith("jpg")) {
            return ImgurPhoto.IMAGE_TYPE_JPEG;
        } else if (url.endsWith("gif")) {
            return ImgurPhoto.IMAGE_TYPE_GIF;
        }

        return ImgurPhoto.IMAGE_TYPE_PNG;
    }

    /**
     * Returns if the link is a video
     *
     * @param url
     * @return
     */
    public static boolean isVideoLink(String url) {
        if (TextUtils.isEmpty(url)) return false;

        url = url.toLowerCase();
        return url.endsWith(".gifv") || url.endsWith("mp4") || url.endsWith(".webm");
    }

    /**
     * Returns if the url link is animated (gif/gifv/webm)
     *
     * @param url
     * @return
     */
    public static boolean isLinkAnimated(@Nullable String url) {
        if (!TextUtils.isEmpty(url)) {
            url = url.toLowerCase();
            return url.endsWith(".gif") || isVideoLink(url);
        }

        return false;
    }

    /**
     * Returns if the link is a png photo hosted on Imgur
     *
     * @param url
     * @return
     */
    public static boolean isImgurPNG(@Nullable String url) {
        if (TextUtils.isEmpty(url)) return false;

        return url.matches(REGEX_IMGUR_PHOTO_PNG);
    }
}
