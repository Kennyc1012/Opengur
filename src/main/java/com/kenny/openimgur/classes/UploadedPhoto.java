package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.kenny.openimgur.util.DBContracts;

/**
 * Created by Kenny-PC on 1/14/2015.
 */
public class UploadedPhoto implements Parcelable {
    private int mId;

    private String mUrl;

    private String mDeleteHash;

    private String mCoverId;

    private long mDate;

    private boolean mIsAlbum;

    public UploadedPhoto(Cursor cursor) {
        mUrl = cursor.getString(DBContracts.UploadContract.COLUMN_INDEX_URL);
        mDeleteHash = cursor.getString(DBContracts.UploadContract.COLUMN_INDEX_DELETE_HASH);
        mCoverId = cursor.getString(DBContracts.UploadContract.COLUMN_INDEX_COVER_ID);
        mDate = cursor.getLong(DBContracts.UploadContract.COLUMN_INDEX_DATE);
        mId = cursor.getInt(DBContracts.UploadContract.COLUMN_INDEX_ID);
        mIsAlbum = cursor.getInt(DBContracts.UploadContract.COLUMN_INDEX_IS_ALBUM) == 1;
    }

    private UploadedPhoto(Parcel in) {
        mId = in.readInt();
        mUrl = in.readString();
        mDeleteHash = in.readString();
        mCoverId = in.readString();
        mDate = in.readLong();
        mIsAlbum = in.readInt() == 1;
    }

    public long getDate() {
        return mDate;
    }

    public String getDeleteHash() {
        return mDeleteHash;
    }

    public String getUrl() {
        return mUrl;
    }

    public int getId() {
        return mId;
    }

    public boolean isAlbum() {
        return mIsAlbum;
    }

    public String getCoverId() {
        return mCoverId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mUrl);
        dest.writeString(mDeleteHash);
        dest.writeString(mCoverId);
        dest.writeLong(mDate);
        dest.writeInt(mIsAlbum ? 1 : 0);
    }

    public static final Parcelable.Creator<UploadedPhoto> CREATOR = new Parcelable.Creator<UploadedPhoto>() {
        public UploadedPhoto createFromParcel(Parcel in) {
            return new UploadedPhoto(in);
        }

        public UploadedPhoto[] newArray(int size) {
            return new UploadedPhoto[size];
        }
    };
}
