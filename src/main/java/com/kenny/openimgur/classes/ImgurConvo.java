package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ImgurConvo extends ImgurBaseObject {
    @SerializedName("with_account")
    private String mWithAccount;

    @SerializedName("last_message_preview")
    private String mLastMessage;

    @SerializedName("message_count")
    private int mMessageCount;

    @SerializedName("with_account_id")
    private int mWithAccountId;

    private ArrayList<ImgurMessage> mMessages;

    private ImgurConvo(Parcel in) {
        super(in);
        mWithAccount = in.readString();
        mLastMessage = in.readString();
        mMessageCount = in.readInt();
        mWithAccountId = in.readInt();
        mMessages = new ArrayList<>();
        in.readList(mMessages, null);
    }

    public ImgurConvo(String with, int withAccountId) {
        super("-1", null, null);
        mWithAccount = with;
        mWithAccountId = withAccountId;
    }

    public String getWithAccount() {
        return mWithAccount;
    }

    public String getLastMessage() {
        return mLastMessage;
    }

    public int getMessageCount() {
        return mMessageCount;
    }

    public int getWithAccountId() {
        return mWithAccountId;
    }

    public void addMessages(ArrayList<ImgurMessage> messages) {
        mMessages = messages;
    }

    public List<ImgurMessage> getMessages() {
        return mMessages;
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mWithAccount);
        out.writeString(mLastMessage);
        out.writeInt(mMessageCount);
        out.writeInt(mWithAccountId);
        out.writeTypedList(mMessages);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImgurConvo> CREATOR = new Parcelable.Creator<ImgurConvo>() {
        public ImgurConvo createFromParcel(Parcel in) {
            return new ImgurConvo(in);
        }

        public ImgurConvo[] newArray(int size) {
            return new ImgurConvo[size];
        }
    };
}
