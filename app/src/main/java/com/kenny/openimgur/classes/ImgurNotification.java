package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.kenny.openimgur.util.DBContracts;

/**
 * Created by Kenny-PC on 8/8/2015.
 */
public class ImgurNotification implements Parcelable, Comparable<ImgurNotification> {
    public static final int TYPE_MESSAGE = 1;

    public static final int TYPE_REPLY = 2;

    private int mId;

    private int mType;

    private String mContent;

    private String mAuthor;

    private String mContentId;

    private String mAlbumCover;

    private long mDate;

    private boolean mViewed;

    ImgurNotification(Parcel in) {
        mId = in.readInt();
        mType = in.readInt();
        mContent = in.readString();
        mAuthor = in.readString();
        mContentId = in.readString();
        mAlbumCover = in.readString();
        mDate = in.readLong();
        mViewed = in.readInt() == 1;
    }

    public ImgurNotification(Cursor cursor) {
        if (cursor != null) {
            mId = cursor.getInt(DBContracts.NotificationContract.COLUMN_INDEX_ID);
            mContent = cursor.getString(DBContracts.NotificationContract.COLUMN_INDEX_CONTENT);
            mAuthor = cursor.getString(DBContracts.NotificationContract.COLUMN_INDEX_AUTHOR);
            mContentId = cursor.getString(DBContracts.NotificationContract.COLUMN_INDEX_CONTENT_ID);
            mAlbumCover = cursor.getString(DBContracts.NotificationContract.COLUMN_INDEX_ALBUM_COVER);
            mDate = cursor.getLong(DBContracts.NotificationContract.COLUMN_INDEX_DATE);
            mType = cursor.getInt(DBContracts.NotificationContract.COLUMN_INDEX_TYPE);
            mViewed = cursor.getInt(DBContracts.NotificationContract.COLUMN_INDEX_VIEWED) == 1;
        }
    }

    public long getDate() {
        // Timestamps will be in seconds, convert to milliseconds
        return mDate * DateUtils.SECOND_IN_MILLIS;
    }

    public String getContentId() {
        return mContentId;
    }

    public String getAlbumCover() {
        return mAlbumCover;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getContent() {
        return mContent;
    }

    public int getType() {
        return mType;
    }

    public int getId() {
        return mId;
    }

    public boolean hasViewed() {
        return mViewed;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeInt(mType);
        dest.writeString(mContent);
        dest.writeString(mAuthor);
        dest.writeString(mContentId);
        dest.writeString(mAlbumCover);
        dest.writeLong(mDate);
        dest.writeInt(mViewed ? 1 : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ImgurNotification)) return false;
        return ((ImgurNotification) o).getId() == getId();
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mType;
        return result;
    }

    public static final Creator<ImgurNotification> CREATOR = new Creator<ImgurNotification>() {
        public ImgurNotification createFromParcel(Parcel in) {
            return new ImgurNotification(in);
        }

        public ImgurNotification[] newArray(int size) {
            return new ImgurNotification[size];
        }
    };

    @Override
    public int compareTo(ImgurNotification another) {
        try {
            long lhs_date = getDate();
            long rhs_date = another.getDate();
            return lhs_date < rhs_date ? -1 : (lhs_date == rhs_date ? 0 : 1);
        } catch (Exception ex) {
            return 0;
        }
    }
}
