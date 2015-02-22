package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Class for Imgur Albums
 */
public class ImgurAlbum extends ImgurBaseObject {
    private static final String KEY_COVER_ID = "cover";

    private static final String KEY_COVER_WIDTH = "cover_width";

    private static final String KEY_COVER_HEIGHT = "cover_height";

    private String mCoverId;

    private String mCoverUrl;

    private int mCoverWidth;

    private int mCoverHeight;

    private List<ImgurPhoto> mAlbumPhotos;

    public ImgurAlbum(JSONObject json) {
        super(json);
        parseJson(json);
    }

    /**
     * Parses the JSON for common values
     *
     * @param json
     */
    private void parseJson(JSONObject json) {

        try {
            if (!json.isNull(KEY_COVER_ID)) {
                mCoverId = json.getString(KEY_COVER_ID);
                mCoverUrl = String.format(Endpoints.ALBUM_COVER.getUrl(), mCoverId + ImgurPhoto.THUMBNAIL_MEDIUM);
            }

            if (!json.isNull(KEY_COVER_HEIGHT)) {
                mCoverHeight = json.getInt(KEY_COVER_HEIGHT);
            }

            if (!json.isNull(KEY_COVER_WIDTH)) {
                mCoverWidth = json.getInt(KEY_COVER_WIDTH);
            }

        } catch (JSONException ex) {
            LogUtil.e("ImgurAlbum", "Error Decoding JSON", ex);
        }
    }

    private ImgurAlbum(Parcel in) {
        super(in);
        mCoverId = in.readString();
        mCoverUrl = in.readString();
        mCoverWidth = in.readInt();
        mCoverHeight = in.readInt();
        mAlbumPhotos = new ArrayList<>();
        in.readTypedList(mAlbumPhotos, ImgurPhoto.CREATOR);
    }

    public String getCoverId() {
        return mCoverId;
    }

    public int getCoverWidth() {
        return mCoverWidth;
    }

    public int getCoverHeight() {
        return mCoverHeight;
    }

    /**
     * Returns the cover image url of an album
     *
     * @param size Optional parameter of size
     * @return
     */
    public String getCoverUrl(@Nullable String size) {
        if (TextUtils.isEmpty(size)) {
            return String.format(Endpoints.ALBUM_COVER.getUrl(), mCoverId);
        } else {
            return String.format(Endpoints.ALBUM_COVER.getUrl(), mCoverId + size);
        }
    }

    /**
     * Adds a photo to the album
     *
     * @param photo
     */
    public void addPhotoToAlbum(ImgurPhoto photo) {
        if (mAlbumPhotos == null) {
            mAlbumPhotos = new ArrayList<>();
        }

        mAlbumPhotos.add(photo);
    }

    public void addPhotosToAlbum(List<ImgurPhoto> photos) {
        if (mAlbumPhotos == null) {
            mAlbumPhotos = photos;
        } else {
            mAlbumPhotos.addAll(photos);
        }
    }

    public List<ImgurPhoto> getAlbumPhotos() {
        return mAlbumPhotos;
    }

    /**
     * Clears the Album of all its photos
     */
    public void clearAlbum() {
        if (mAlbumPhotos != null) {
            mAlbumPhotos.clear();
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mCoverId);
        out.writeString(mCoverUrl);
        out.writeInt(mCoverWidth);
        out.writeInt(mCoverHeight);
        out.writeTypedList(mAlbumPhotos);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImgurAlbum> CREATOR = new Parcelable.Creator<ImgurAlbum>() {
        public ImgurAlbum createFromParcel(Parcel in) {
            return new ImgurAlbum(in);
        }

        public ImgurAlbum[] newArray(int size) {
            return new ImgurAlbum[size];
        }
    };

}
