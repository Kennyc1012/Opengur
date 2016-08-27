package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

public class ImgurTrophy extends ImgurBaseObject {

    @SerializedName("name")
    private String mName;

    @SerializedName("data_link")
    private String mDataLink;

    @SerializedName("image")
    private String mTrophyImage;

    ImgurTrophy(Parcel in) {
        super(in);
        mName = in.readString();
        mDataLink = in.readString();
        mTrophyImage = in.readString();
    }

    @Nullable
    public String getDataLink() {
        return mDataLink;
    }

    public String getName() {
        return mName;
    }

    public String getTrophyImagePath() {
        return mTrophyImage;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mName);
        out.writeString(mDataLink);
        out.writeString(mTrophyImage);
    }

    public static final Parcelable.Creator<ImgurTrophy> CREATOR = new Parcelable.Creator<ImgurTrophy>() {
        public ImgurTrophy createFromParcel(Parcel in) {
            return new ImgurTrophy(in);
        }

        public ImgurTrophy[] newArray(int size) {
            return new ImgurTrophy[size];
        }
    };
}
