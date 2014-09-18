package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

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
     * @param size Size the thumbnail should be
     * @return
     */
    public String getThumbnail(@NonNull String size) {
        if (getLink() != null && getId() != null) {
            String link = getLink();
            link = link.replace(getId(), getId() + size);
            return link;
        }

        return null;
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
}
