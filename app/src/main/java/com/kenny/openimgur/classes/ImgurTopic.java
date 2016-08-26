package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.kenny.openimgur.util.DBContracts;

/**
 * Created by kcampagna on 2/19/15.
 */
public class ImgurTopic implements Parcelable {
    @SerializedName("id")
    private int mId;

    @SerializedName("name")
    private String mName;

    @SerializedName("description")
    private String mDesc;

    public ImgurTopic(Cursor cursor) {
        mId = cursor.getInt(DBContracts.TopicsContract.COLUMN_INDEX_ID);
        mName = cursor.getString(DBContracts.TopicsContract.COLUMN_INDEX_NAME);
        mDesc = cursor.getString(DBContracts.TopicsContract.COLUMN_INDEX_DESC);
    }

    ImgurTopic(Parcel in) {
        mId = in.readInt();
        mName = in.readString();
        mDesc = in.readString();
    }

    public String getName() {
        return mName;
    }

    public int getId() {
        return mId;
    }

    public String getDescription() {
        return mDesc;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mId);
        out.writeString(mName);
        out.writeString(mDesc);
    }

    public int describeContents() {
        return 0;
    }

    public static final Creator<ImgurTopic> CREATOR = new Creator<ImgurTopic>() {
        public ImgurTopic createFromParcel(Parcel in) {
            return new ImgurTopic(in);
        }

        public ImgurTopic[] newArray(int size) {
            return new ImgurTopic[size];
        }
    };

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + mName.hashCode();
        result = 31 * result + mDesc.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImgurTopic that = (ImgurTopic) o;
        return mId == that.getId();
    }
}
