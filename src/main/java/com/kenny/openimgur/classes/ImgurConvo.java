package com.kenny.openimgur.classes;

import android.nfc.Tag;
import android.os.Parcel;
import android.os.Parcelable;

import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ImgurConvo extends ImgurBaseObject {
    private static final String KEY_WITH_ACCOUNT = "with_account";

    private static final String KEY_WITH_ACCOUNT_ID = "with_account_id";

    private static final String KEY_LAST_MESSAGE = "last_message_preview";

    private static final String KEY_MESSAGE_COUNT = "message_count";

    private String mWithAccount;

    private String mLastMessage;

    private int mMessageCount;

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

    public ImgurConvo(JSONObject json) {
        super(json);

        try {
            if (!json.isNull(KEY_WITH_ACCOUNT)) {
                mWithAccount = json.getString(KEY_WITH_ACCOUNT);
            }

            if (!json.isNull(KEY_WITH_ACCOUNT_ID)) {
                mWithAccountId = json.getInt(KEY_WITH_ACCOUNT_ID);
            }

            if (!json.isNull(KEY_LAST_MESSAGE)) {
                mLastMessage = json.getString(KEY_LAST_MESSAGE);
            }

            if (!json.isNull(KEY_MESSAGE_COUNT)) {
                mMessageCount = json.getInt(KEY_MESSAGE_COUNT);
            }
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Error Decoding JSON", ex);
        }
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

    public static ImgurConvo createConvo(String with, int withAccountId) {
        JSONObject json = new JSONObject();

        try {
            json.put(KEY_WITH_ACCOUNT_ID, withAccountId);
            json.put(KEY_WITH_ACCOUNT, with);
            json.put(KEY_ID, "-1");
        } catch (JSONException ex) {
            LogUtil.e("ImgurConvo", "Error creating JSON object", ex);
            json = null;
        }

        if (json != null) {
            return new ImgurConvo(json);
        }

        return null;
    }
}
