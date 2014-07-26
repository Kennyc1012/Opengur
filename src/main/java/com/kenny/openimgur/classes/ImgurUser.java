package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import com.kenny.openimgur.util.DBContracts.UserContract;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kcampagna on 7/25/14.
 */
public class ImgurUser implements Parcelable {
    private static final String KEY_DATA = "data";

    private static final String KEY_ID = "id";

    private static final String KEY_USERNAME = "url";

    private static final String KEY_BIO = "bio";

    private static final String KEY_CREATED = "created";

    private static final String KEY_PRO_EXPIRATION = "pro_expiration";

    private static final String KEY_REPUTATION = "reputation";

    private int mId;

    private String mUsername;

    private String mBio;

    private long mCreated;

    private long mProExpiration = -1;

    private long mReputation;

    private String mAccessToken;

    private String mRefreshToken;

    private long mAccessTokenExpiration;

    public ImgurUser(JSONObject json) {
        parseJsonForValues(json);
    }

    public ImgurUser(String accessToken, String refreshToken, long accessTokenExpiration) {
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mAccessTokenExpiration = accessTokenExpiration;
    }

    public ImgurUser(Cursor cursor) {
        mId = cursor.getInt(UserContract.COLUMN_INDEX_ID);
        mUsername = cursor.getString(UserContract.COLUMN_INDEX_NAME);
        mBio = cursor.getString(UserContract.COLUMN_INDEX_BIO);
        mCreated = cursor.getLong(UserContract.COLUMN_INDEX_CREATED);
        mProExpiration = cursor.getLong(UserContract.COLUMN_INDEX_PRO_EXPIRATION);
        mReputation = cursor.getLong(UserContract.COLUMN_INDEX_REPUTATION);
        mAccessToken = cursor.getString(UserContract.COLUMN_INDEX_ACCESS_TOKEN);
        mRefreshToken = cursor.getString(UserContract.COLUMN_INDEX_REFRESH_TOKEN);
        mAccessTokenExpiration = cursor.getLong(UserContract.COLUMN_INDEX_ACCESS_TOKEN_EXPIRATION);
    }

    private ImgurUser(Parcel in) {
        mUsername = in.readString();
        mBio = in.readString();
        mAccessToken = in.readString();
        mRefreshToken = in.readString();
        mId = in.readInt();
        mAccessTokenExpiration = in.readLong();
        mProExpiration = in.readLong();
        mCreated = in.readLong();
        mReputation = in.readLong();
    }

    /**
     * Parses the given json object for a Member's values
     *
     * @param json
     * @return If successful
     */
    public boolean parseJsonForValues(JSONObject json) {
        try {
            if (json.has(KEY_DATA)) {
                JSONObject data = json.getJSONObject(KEY_DATA);

                if (json.has(KEY_ID)) {
                    mId = json.getInt(KEY_ID);
                }

                if (json.has(KEY_USERNAME)) {
                    mUsername = json.getString(KEY_USERNAME);
                }

                if (json.has(KEY_BIO)) {
                    mBio = json.getString(KEY_BIO);
                }

                if (json.has(KEY_REPUTATION)) {
                    mReputation = json.getLong(KEY_REPUTATION);
                }

                if (json.has(KEY_CREATED)) {
                    mCreated = json.getLong(KEY_CREATED);
                }

                // Can be a boolean if they are not a pro user
                if (json.has(KEY_PRO_EXPIRATION) && json.get(KEY_PRO_EXPIRATION) instanceof Long) {
                    mProExpiration = json.getLong(KEY_PRO_EXPIRATION);
                }
                return true;
            }

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public long getAccessTokenExpiration() {
        return mAccessTokenExpiration;
    }

    public String getRefreshToken() {
        return mRefreshToken;
    }

    public String getAccessToken() {
        return mAccessToken;
    }

    public long getReputation() {
        return mReputation;
    }

    public long getProExpiration() {
        return mProExpiration;
    }

    public String getBio() {
        return mBio;
    }

    public long getCreated() {
        return mCreated;
    }

    public String getUsername() {
        return mUsername;
    }

    public int getId() {
        return mId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mUsername);
        parcel.writeString(mBio);
        parcel.writeString(mAccessToken);
        parcel.writeString(mRefreshToken);
        parcel.writeInt(mId);
        parcel.writeLong(mAccessTokenExpiration);
        parcel.writeLong(mProExpiration);
        parcel.writeLong(mCreated);
        parcel.writeLong(mReputation);
    }

    public static final Parcelable.Creator<ImgurUser> CREATOR = new Parcelable.Creator<ImgurUser>() {
        public ImgurUser createFromParcel(Parcel in) {
            return new ImgurUser(in);
        }

        public ImgurUser[] newArray(int size) {
            return new ImgurUser[size];
        }
    };
}
