package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.kenny.openimgur.util.DBContracts;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Kenny-PC on 8/8/2015.
 */
public class ImgurNotification implements Parcelable {
    public static final int TYPE_MESSAGE = 1;
    public static final int TYPE_REPLY = 2;

    private int mId;
    private int mType;
    private String mContent;
    private String mAuthor;
    private String mContentId;
    private String mAlbumCover;
    private long mDate;

    private ImgurNotification(Parcel in) {
        mId = in.readInt();
        mType = in.readInt();
        mContent = in.readString();
        mAuthor = in.readString();
        mContentId = in.readString();
        mAlbumCover = in.readString();
        mDate = in.readLong();
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
    }

    public static final Parcelable.Creator<ImgurNotification> CREATOR = new Parcelable.Creator<ImgurNotification>() {
        public ImgurNotification createFromParcel(Parcel in) {
            return new ImgurNotification(in);
        }

        public ImgurNotification[] newArray(int size) {
            return new ImgurNotification[size];
        }
    };

    /**
     * Sorts a list of notifications by newest first
     *
     * @param notifications
     */
    public static void sort(List<ImgurNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) return;

        Collections.sort(notifications, new Comparator<ImgurNotification>() {
            @Override
            public int compare(ImgurNotification lhs, ImgurNotification rhs) {
                return (int) (rhs.getDate() - lhs.getDate());
            }
        });
    }
}
