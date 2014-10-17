package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Object for Photos
 */
public class ImgurPhoto extends ImgurBaseObject {
    // 160x160
    public static final String THUMBNAIL_SMALL = "s";

    // 320x320
    public static final String THUMBNAIL_MEDIUM = "m";

    // 640x640
    public static final String THUMBNAIL_LARGE = "l";

    // 1024x1024
    public static final String THUMBNAIL_HUGE = "h";

    private static final String KEY_TYPE = "type";

    private static final String KEY_WIDTH = "width";

    private static final String KEY_HEIGHT = "height";

    private static final String KEY_ANIMATED = "animated";

    private static final String KEY_SIZE = "size";

    public static final String IMAGE_TYPE_PNG = "image/png";

    public static final String IMAGE_TYPE_JPEG = "image/jpeg";

    public static final String IMAGE_TYPE_GIF = "image/gif";

    private String mType;

    private int mWidth;

    private int mHeight;

    private boolean mIsAnimated;

    private long mSize;

    public ImgurPhoto(JSONObject json) {
        super(json);
        parseJson(json);
    }

    /**
     * Parses the JSON for related values
     *
     * @param json
     */
    private void parseJson(JSONObject json) {
        try {
            if (json.has(KEY_TYPE) && !json.get(KEY_TYPE).equals(null)) {
                mType = json.getString(KEY_TYPE);
            }

            if (json.has(KEY_ANIMATED) && !json.get(KEY_ANIMATED).equals(null)) {
                mIsAnimated = json.getBoolean(KEY_ANIMATED);
            }

            if (json.has(KEY_HEIGHT) && !json.get(KEY_HEIGHT).equals(null)) {
                mHeight = json.getInt(KEY_HEIGHT);
            }

            if (json.has(KEY_WIDTH) && !json.get(KEY_WIDTH).equals(null)) {
                mWidth = json.getInt(KEY_WIDTH);
            }

            if (json.has(KEY_SIZE) && !json.get(KEY_SIZE).equals(null)) {
                mSize = json.getInt(KEY_SIZE);
            }
        } catch (JSONException ex) {
            LogUtil.e("ImgurPhoto", "Error Decoding JSON", ex);
        }
    }

    public long getSize() {
        return mSize;
    }

    public boolean isAnimated() {
        return mIsAnimated;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getWidth() {
        return mWidth;
    }

    public String getType() {
        return mType;
    }

    private ImgurPhoto(Parcel in) {
        super(in);
        mType = in.readString();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mIsAnimated = in.readInt() == 1;
        mSize = in.readLong();
    }

    /**
     * Returns the link to the images thumbnail
     *
     * @param size         Size the thumbnail should be
     * @param recreateLink If the link should be recreated due to a thumbnail being present in the link
     * @param ext          The extension for the recreated link, usually will be for large gifs
     * @return
     */
    public String getThumbnail(@NonNull String size, boolean recreateLink, @Nullable String ext) {
        if (getLink() != null && getId() != null) {

            if (recreateLink) {
                return new StringBuilder("http://i.imgur.com/").append(getId()).append(size).append(ext).toString();
            } else {
                return getLink().replace(getId(), getId() + size);
            }
        }

        return null;
    }

    /**
     * Returns if the link provided by the Api already has a thumbnail worked into it. This is used for larger gifs
     *
     * @return
     */
    public boolean isLinkAThumbnail() {
        if (TextUtils.isEmpty(getId()) || TextUtils.isEmpty(getLink())) {
            return false;
        }

        String idFromUrl = LinkUtils.getId(getLink());
        return !getId().equals(idFromUrl);
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mType);
        out.writeInt(mWidth);
        out.writeInt(mHeight);
        out.writeInt(mIsAnimated ? 1 : 0);
        out.writeLong(mSize);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImgurPhoto> CREATOR = new Parcelable.Creator<ImgurPhoto>() {
        public ImgurPhoto createFromParcel(Parcel in) {
            return new ImgurPhoto(in);
        }

        public ImgurPhoto[] newArray(int size) {
            return new ImgurPhoto[size];
        }
    };

    /**
     * Returns the quality string based on the settings value for quality
     *
     * @param quality
     * @return
     */
    public static String getQualityValue(String quality) {
        if (SettingsActivity.THUMBNAIL_QUALITY_LOW.equals(quality)) {
            return ImgurPhoto.THUMBNAIL_SMALL;
        } else if (SettingsActivity.THUMBNAIL_QUALITY_MEDIUM.equals(quality)) {
            return ImgurPhoto.THUMBNAIL_MEDIUM;
        }

        return ImgurPhoto.THUMBNAIL_LARGE;
    }
}
