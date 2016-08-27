package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.kenny.openimgur.util.LinkUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kcampagna on 7/11/15.
 */
public class ImgurPhoto extends ImgurBaseObject {
    private static final Pattern ID_PATTERN = Pattern.compile("(?<=.com\\/)\\w+");

    // 160x160
    public static final String THUMBNAIL_SMALL = "s";

    // 320x320
    public static final String THUMBNAIL_MEDIUM = "m";

    // 640x640
    public static final String THUMBNAIL_LARGE = "l";

    // 1024x1024
    public static final String THUMBNAIL_HUGE = "h";

    public static final String THUMBNAIL_GALLERY = "b";

    public static final String IMAGE_TYPE_PNG = "image/png";

    public static final String IMAGE_TYPE_JPEG = "image/jpeg";

    public static final String IMAGE_TYPE_GIF = "image/gif";

    @SerializedName("type")
    private String mType;

    @SerializedName("mp4")
    private String mMP4Link;

    @SerializedName("webm")
    private String mWebMLink;

    @SerializedName("width")
    private int mWidth;

    @SerializedName("height")
    private int mHeight;

    @SerializedName("animated")
    private boolean mIsAnimated;

    @SerializedName("size")
    private long mSize;

    @SerializedName("mp4_size")
    private long mMP4Size;

    @SerializedName("webm_size")
    private long mWebMSize;

    public ImgurPhoto(String link) {
        super(null, null, link);
        mIsAnimated = LinkUtils.isLinkAnimated(link);
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

    public boolean hasVideoLink() {
        return !TextUtils.isEmpty(mMP4Link) || !TextUtils.isEmpty(mWebMLink);
    }

    @Nullable
    public String getVideoLink() {
        if (!TextUtils.isEmpty(mWebMLink) && mWebMSize < mMP4Size) {
            return mWebMLink;
        } else if (!TextUtils.isEmpty(mMP4Link)) {
            return mMP4Link;
        }

        return null;
    }

    ImgurPhoto(Parcel in) {
        super(in);
        mType = in.readString();
        mMP4Link = in.readString();
        mWebMLink = in.readString();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mIsAnimated = in.readInt() == 1;
        mSize = in.readLong();
        mMP4Size = in.readLong();
        mWebMSize = in.readLong();
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
                return "https://i.imgur.com/" + getId() + size + ext;
            } else {
                return getThumbnail(getId(), getLink(), size);
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

        Matcher match = ID_PATTERN.matcher(getLink());

        if (match.find()) {
            String id = match.group(0);
            return !getId().equals(id);
        }

        return false;
    }

    @Override
    public void toHttps() {
        super.toHttps();

        if (LinkUtils.isHttpLink(mMP4Link)) {
            mMP4Link = mMP4Link.replace("http:", "https:");
        }

        if (LinkUtils.isHttpLink(mWebMLink)) {
            mWebMLink = mWebMLink.replace("http:", "https:");
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mType);
        out.writeString(mMP4Link);
        out.writeString(mWebMLink);
        out.writeInt(mWidth);
        out.writeInt(mHeight);
        out.writeInt(mIsAnimated ? 1 : 0);
        out.writeLong(mSize);
        out.writeLong(mMP4Size);
        out.writeLong(mWebMSize);
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
