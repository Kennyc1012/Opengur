package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;

import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kcampagna on 12/25/14.
 */
public class ImgurMessage extends ImgurBaseObject {
    private static final String KEY_SENDER_ID = "sender_id";

    private static final String KEY_BODY = "body";

    private static final String KEY_FROM = "from";

    private static final String KEY_IS_SENDING = "is_sending";

    private String mBody;

    private String mFrom;

    private int mSenderId;

    private boolean mIsSending = false;

    public ImgurMessage(JSONObject json) {
        super(null);
        // TODO
        try {
            if (!json.isNull(KEY_BODY)) {
                mBody = json.getString(KEY_BODY);
            }

            if (!json.isNull(KEY_SENDER_ID)) {
                mSenderId = json.getInt(KEY_SENDER_ID);
            }

            if (!json.isNull(KEY_FROM)) {
                mFrom = json.getString(KEY_FROM);
            }

            if (!json.isNull(KEY_IS_SENDING)) {
                mIsSending = json.getBoolean(KEY_IS_SENDING);
            }
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Error Decoding JSON", ex);
        }
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
        mIsSending = in.readInt() == 1;
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(mSenderId);
        out.writeString(mBody);
        out.writeString(mFrom);
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
      /*  JSONObject json = new JSONObject();
        long currentTime = System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS;

        try {
            json.put(KEY_SENDER_ID, senderId);
            json.put(KEY_BODY, message);
            json.put(KEY_ID, String.valueOf(currentTime));
            json.put(KEY_DATE, currentTime);
            json.put(KEY_IS_SENDING, true);
        } catch (JSONException ex) {
            LogUtil.e("ImgurMessage", "Error creating json object", ex);
            json = null;
        }

        if (json != null) {
            return new ImgurMessage(json);
        }*/
        // TODO

        return null;
    }
}
