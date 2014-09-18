package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.kenny.openimgur.R;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.DBContracts.UserContract;
import com.kenny.openimgur.util.LogUtil;

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

    public static final String KEY_ACCESS_TOKEN = "access_token";

    public static final String KEY_REFRESH_TOKEN = "refresh_token";

    public static final String KEY_EXPIRES_IN = "expires_in";

    // We will get a new refresh token when it expires in 5 minutes or less
    private static final long TOKEN_CUTOFF_TIME = DateUtils.MINUTE_IN_MILLIS * 5;

    private int mId;

    private String mUsername;

    private String mBio;

    private long mCreated;

    private long mProExpiration = -1;

    private long mReputation;

    private long mLastSeen;

    private String mAccessToken;

    private String mRefreshToken;

    private long mAccessTokenExpiration;

    private Notoriety mNotoriety;

    public enum Notoriety {
        FOREVER_ALONE,
        NEUTRAL,
        ACCEPTED,
        LIKED,
        TRUSTED,
        IDOLIZED,
        RENOWNED,
        GLORIOUS;

        /**
         * Gets the notoriety based on the user's reputation
         *
         * @param rep
         * @return
         */
        public static Notoriety getNotoriety(long rep) {
            if (rep < 0) {
                return FOREVER_ALONE;
            } else if (rep < 400) {
                return NEUTRAL;
            } else if (rep < 1000) {
                return ACCEPTED;
            } else if (rep < 2000) {
                return LIKED;
            } else if (rep < 4000) {
                return TRUSTED;
            } else if (rep < 8000) {
                return IDOLIZED;
            } else if (rep < 20000) {
                return RENOWNED;
            }

            return GLORIOUS;
        }

        /**
         * Returns the string that represents the Notoriety level
         *
         * @return
         */
        public int getStringId() {
            switch (this) {
                case NEUTRAL:
                    return R.string.notoriety_neutral;

                case ACCEPTED:
                    return R.string.notoriety_accepted;

                case LIKED:
                    return R.string.notoriety_liked;

                case TRUSTED:
                    return R.string.notoriety_trusted;

                case IDOLIZED:
                    return R.string.notoriety_idolized;

                case RENOWNED:
                    return R.string.notoriety_renowned;

                case GLORIOUS:
                    return R.string.notoriety_glorious;

                default:
                case FOREVER_ALONE:
                    return R.string.notoriety_forever_alone;
            }
        }
    }

    public ImgurUser(JSONObject json) {
        parseJsonForValues(json);
    }

    public ImgurUser(String username, String accessToken, String refreshToken, long accessTokenExpiration) {
        mUsername = username;
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mAccessTokenExpiration = accessTokenExpiration;
    }

    public ImgurUser(Cursor cursor, boolean isLoggedInUser) {
        if (isLoggedInUser) {
            mId = cursor.getInt(UserContract.COLUMN_INDEX_ID);
            mUsername = cursor.getString(UserContract.COLUMN_INDEX_NAME);
            mCreated = cursor.getLong(UserContract.COLUMN_INDEX_CREATED);
            mProExpiration = cursor.getLong(UserContract.COLUMN_INDEX_PRO_EXPIRATION);
            mAccessToken = cursor.getString(UserContract.COLUMN_INDEX_ACCESS_TOKEN);
            mRefreshToken = cursor.getString(UserContract.COLUMN_INDEX_REFRESH_TOKEN);
            mAccessTokenExpiration = cursor.getLong(UserContract.COLUMN_INDEX_ACCESS_TOKEN_EXPIRATION);
        } else {
            mId = cursor.getInt(DBContracts.ProfileContract.COLUMN_INDEX_ID);
            mUsername = cursor.getString(DBContracts.ProfileContract.COLUMN_INDEX_USERNAME);
            mBio = cursor.getString(DBContracts.ProfileContract.COLUMN_INDEX_BIO);
            mReputation = cursor.getLong(DBContracts.ProfileContract.COLUMN_INDEX_REP);
            mLastSeen = cursor.getLong(DBContracts.ProfileContract.COLUMN_INDEX_LAST_SEEN);
            mNotoriety = Notoriety.getNotoriety(mReputation);
        }
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
        mLastSeen = in.readLong();
        mNotoriety = Notoriety.getNotoriety(mReputation);
    }

    /**
     * Parses the given json object for a Member's values
     *
     * @param json
     * @return If successful
     */
    public boolean parseJsonForValues(JSONObject json) {
        try {
            if (json.has(KEY_DATA) && !json.get(KEY_DATA).equals(null)) {
                JSONObject data = json.getJSONObject(KEY_DATA);

                if (data.has(KEY_ID) && !data.get(KEY_ID).equals(null)) {
                    mId = data.getInt(KEY_ID);
                }

                if (data.has(KEY_USERNAME) && !data.get(KEY_USERNAME).equals(null)) {
                    mUsername = data.getString(KEY_USERNAME);
                }

                if (data.has(KEY_BIO) && !data.get(KEY_BIO).equals(null)) {
                    mBio = data.getString(KEY_BIO);
                }

                if (data.has(KEY_REPUTATION) && !data.get(KEY_REPUTATION).equals(null)) {
                    mReputation = data.getLong(KEY_REPUTATION);
                    mNotoriety = Notoriety.getNotoriety(mReputation);
                }

                if (data.has(KEY_CREATED) && !data.get(KEY_CREATED).equals(null)) {
                    mCreated = data.getLong(KEY_CREATED) * 1000L;
                }

                // Can be a boolean if they are not a pro user
                if (data.has(KEY_PRO_EXPIRATION) && !data.get(KEY_PRO_EXPIRATION).equals(null)
                        && data.get(KEY_PRO_EXPIRATION) instanceof Long) {
                    mProExpiration = data.getLong(KEY_PRO_EXPIRATION) * 1000L;
                }

                mLastSeen = System.currentTimeMillis();
                return true;
            }

        } catch (JSONException ex) {
            LogUtil.e("ImgurUser", "Error Decoding JSON", ex);
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

    public Notoriety getNotoriety() {
        return mNotoriety;
    }

    public long getLastSeen() {
        return mLastSeen;
    }

    /**
     * Sets the user's tokens
     *
     * @param accessToken
     * @param refreshToken
     * @param expiresIn
     */
    public void setTokens(String accessToken, String refreshToken, long expiresIn) {
        mAccessToken = accessToken;
        mRefreshToken = refreshToken;
        mAccessTokenExpiration = expiresIn;
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
        parcel.writeLong(mLastSeen);
    }

    public static final Parcelable.Creator<ImgurUser> CREATOR = new Parcelable.Creator<ImgurUser>() {
        public ImgurUser createFromParcel(Parcel in) {
            return new ImgurUser(in);
        }

        public ImgurUser[] newArray(int size) {
            return new ImgurUser[size];
        }
    };

    @Override
    public String toString() {
        return "Username: " + mUsername + " Id: " + mId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (!(object instanceof ImgurUser)) {
            return false;
        }

        if (object == this) {
            return true;
        }

        ImgurUser user = (ImgurUser) object;
        return user.mId == this.getId() || user.getUsername().equals(this.getUsername());
    }

    public boolean isSelf() {
        ImgurUser user = OpenImgurApp.getInstance().getUser();

        if (user != null) {
            return user.equals(this);
        }

        return false;
    }

    /**
     * Returns if the users Access Token is valid. Will return false if one is not present
     *
     * @return
     */
    public boolean isAccessTokenValid() {
        if (!TextUtils.isEmpty(mAccessToken)) {
            return mAccessTokenExpiration - System.currentTimeMillis() > TOKEN_CUTOFF_TIME;
        }

        return false;
    }
}
