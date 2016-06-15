package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateUtils;

import com.google.gson.annotations.SerializedName;

/**
 * Created by kcampagna on 12/25/14.
 */
public class ImgurMessage extends ImgurBaseObject {
    @SerializedName("body")
    private String mBody;

    @SerializedName("from")
    private String mFrom;

    @SerializedName("last_message")
    private String mLastMessage;

    @SerializedName("sender_id")
    private int mSenderId;

    private boolean mIsSending = false;

    private ImgurMessage(String id, String message, int senderId, long date, boolean isSending) {
        super(id, null, null);
        mBody = message;
        mSenderId = senderId;
        mIsSending = isSending;
        setDate(date);
    }

    public int getSenderId() {
        return mSenderId;
    }

    public String getFrom() {
        return mFrom;
    }

    public String getBody() {
        return mBody;
    }

    /**
     * Returns the last message. This is for use with notifications
     *
     * @return
     */
    public String getLastMessage() {
        return mLastMessage;
    }

    public boolean isSending() {
        return mIsSending;
    }

    public void setIsSending(boolean isSending) {
        mIsSending = isSending;
    }

    public ImgurMessage(Parcel in) {
        super(in);
        mSenderId = in.readInt();
        mBody = in.readString();
        mFrom = in.readString();
        mLastMessage = in.readString();
        mIsSending = in.readInt() == 1;
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(mSenderId);
        out.writeString(mBody);
        out.writeString(mFrom);
        out.writeString(mLastMessage);
        out.writeInt(mIsSending ? 1 : 0);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImgurMessage> CREATOR = new Parcelable.Creator<ImgurMessage>() {
        public ImgurMessage createFromParcel(Parcel in) {
            return new ImgurMessage(in);
        }

        public ImgurMessage[] newArray(int size) {
            return new ImgurMessage[size];
        }
    };

    public static ImgurMessage createMessage(String message, int senderId) {
        long currentTime = System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS;
        String id = String.valueOf(currentTime);
        return new ImgurMessage(id, message, senderId, currentTime, true);
    }
}
