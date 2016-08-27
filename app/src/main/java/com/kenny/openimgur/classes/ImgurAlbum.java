package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.kenny.openimgur.api.ApiClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/11/15.
 */
public class ImgurAlbum extends ImgurBaseObject {
    public static final String ALBUM_COVER_URL = ApiClient.IMGUR_URL + "%s.jpg";

    @SerializedName("cover")
    private String mCoverId;

    @SerializedName("images_count")
    private int mAlbumImageCount;

    private List<ImgurPhoto> mAlbumPhotos;

    public ImgurAlbum(String id, String title, String link) {
        super(id, title, link);
    }

    public ImgurAlbum(String id, String title, String link, String deleteHash) {
        super(id, title, link, deleteHash);
    }

    ImgurAlbum(Parcel in) {
        super(in);
        mAlbumImageCount = in.readInt();
        mCoverId = in.readString();
        mAlbumPhotos = new ArrayList<>();
        in.readTypedList(mAlbumPhotos, ImgurPhoto.CREATOR);
    }

    public void setCoverId(String id) {
        mCoverId = id;
    }

    public String getCoverId() {
        return mCoverId;
    }

    /**
     * Returns the cover image url of an album
     *
     * @param size Optional parameter of size
     * @return
     */
    public String getCoverUrl(@Nullable String size) {
        if (TextUtils.isEmpty(size)) {
            return String.format(ALBUM_COVER_URL, mCoverId);
        } else {
            return String.format(ALBUM_COVER_URL, mCoverId + size);
        }
    }

    public void addPhotosToAlbum(List<ImgurPhoto> photos) {
        if (mAlbumPhotos == null) {
            mAlbumPhotos = photos;
        } else {
            mAlbumPhotos.addAll(photos);
        }

        for (ImgurPhoto p : mAlbumPhotos) {
            p.toHttps();
        }
    }

    public List<ImgurPhoto> getAlbumPhotos() {
        return mAlbumPhotos;
    }

    public int getAlbumImageCount() {
        return mAlbumPhotos != null && !mAlbumPhotos.isEmpty() ? mAlbumPhotos.size() : mAlbumImageCount;
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(mAlbumImageCount);
        out.writeString(mCoverId);
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
