package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kcampagna on 7/12/15.
 */
public class ImgurTag implements Parcelable {
    @SerializedName("name")
    private String mName;

    @SerializedName("author")
    private String mAuthor;

    @SerializedName("ups")
    private int mUpVotes;

    @SerializedName("downs")
    private int mDownVotes;

    ImgurTag(Parcel in) {
        mName = in.readString();
        mAuthor = in.readString();
        mUpVotes = in.readInt();
        mDownVotes = in.readInt();
    }

    public String getName() {
        return mName;
    }

    public int getDownVotes() {
        return mDownVotes;
    }

    public int getUpVotes() {
        return mUpVotes;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeString(mAuthor);
        dest.writeInt(mUpVotes);
        dest.writeInt(mDownVotes);
    }

    public static final Creator<ImgurTag> CREATOR = new Creator<ImgurTag>() {
        public ImgurTag createFromParcel(Parcel in) {
            return new ImgurTag(in);
        }

        public ImgurTag[] newArray(int size) {
            return new ImgurTag[size];
        }
    };
}
