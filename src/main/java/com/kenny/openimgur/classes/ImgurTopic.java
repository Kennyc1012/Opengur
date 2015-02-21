package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kcampagna on 2/19/15.
 */
public class ImgurTopic implements Parcelable {
    private static final String KEY_NAME = "name";

    private static final String KEY_ID = "id";

    private static final String KEY_DESC = "description";

    private int mId;

    private String mName;

    private String mDesc;

    public ImgurTopic(Cursor cursor) {
        mId = cursor.getInt(DBContracts.TopicsContract.COLUMN_INDEX_TOPIC_ID);
        mName = cursor.getString(DBContracts.TopicsContract.COLUMN_INDEX_NAME);
        mDesc = cursor.getString(DBContracts.TopicsContract.COLUMN_INDEX_DESC);
    }

    public ImgurTopic(JSONObject json) {
        parseJson(json);
    }

    private ImgurTopic(Parcel in) {
        mId = in.readInt();
        mName = in.readString();
        mDesc = in.readString();
    }

    /**
     * Parses the JSON for common values
     *
     * @param json
     */
    private void parseJson(JSONObject json) {
        try {
            if (!json.isNull(KEY_NAME)) {
                mName = json.getString(KEY_NAME);
            }

            if (!json.isNull(KEY_DESC)) {
                mDesc = json.getString(KEY_DESC);
            }

            if (!json.isNull(KEY_ID)) {
                mId = json.getInt(KEY_ID);
            }

        } catch (JSONException ex) {
            LogUtil.e("ImgurTop", "Error Decoding JSON", ex);
        }
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

    public static final Parcelable.Creator<ImgurTopic> CREATOR = new Parcelable.Creator<ImgurTopic>() {
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
