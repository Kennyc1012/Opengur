package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class Upload implements Parcelable {

    private String mLocation;

    private String mUploadedLink;

    private String mTitle;

    private String mDesc;

    private boolean mIsLink;

    Upload(Parcel in) {
        mLocation = in.readString();
        mUploadedLink = in.readString();
        mTitle = in.readString();
        mDesc = in.readString();
        mIsLink = in.readInt() == 1;
    }

    public Upload(String location, boolean isLink) {
        mLocation = location;
        mIsLink = isLink;
    }

    public Upload(String location) {
        this(location, false);
    }

    public String getLocation() {
        return mLocation;
    }

    public boolean isLink() {
        return mIsLink;
    }

    public String getUploadedLink() {
        return mUploadedLink;
    }

    public void setUploadedLink(String link) {
        mUploadedLink = link;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getDescription() {
        return mDesc;
    }

    public void setDescription(String description) {
        mDesc = description;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Upload)) return false;

        return ((Upload) o).getLocation().equals(getLocation());
    }

    public static final Creator<Upload> CREATOR = new Creator<Upload>() {
        public Upload createFromParcel(Parcel in) {
            return new Upload(in);
        }

        public Upload[] newArray(int size) {
            return new Upload[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mLocation);
        out.writeString(mUploadedLink);
        out.writeString(mTitle);
        out.writeString(mDesc);
        out.writeInt(mIsLink ? 1 : 0);
    }
}
