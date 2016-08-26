package com.kenny.openimgur.classes;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.google.gson.annotations.SerializedName;
import com.kenny.openimgur.R;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.DBContracts.UserContract;

/**
 * Created by kcampagna on 7/25/14.
 */
public class ImgurUser implements Parcelable {
    @SerializedName("id")
    private int mId;

    @SerializedName("url")
    private String mUsername;

    @SerializedName("bio")
    private String mBio;

    @SerializedName("created")
    private long mCreated;

    @SerializedName("reputation")
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

        /**
         * Returns the color associated with the notoriety level
         *
         * @return
         */
        public int getNotorietyColor() {
            if (this == FOREVER_ALONE) {
                return R.color.notoriety_negative;
            }

            return R.color.notoriety_positive;
        }
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
            mAccessToken = cursor.getString(UserContract.COLUMN_INDEX_ACCESS_TOKEN);
            mRefreshToken = cursor.getString(UserContract.COLUMN_INDEX_REFRESH_TOKEN);
            mAccessTokenExpiration = cursor.getLong(UserContract.COLUMN_INDEX_ACCESS_TOKEN_EXPIRATION);
            mReputation = cursor.getLong(UserContract.COLUMN_INDEX_REPUTATION);
            mNotoriety = Notoriety.getNotoriety(mReputation);
        } else {
            mId = cursor.getInt(DBContracts.ProfileContract.COLUMN_INDEX_ID);
            mUsername = cursor.getString(DBContracts.ProfileContract.COLUMN_INDEX_USERNAME);
            mBio = cursor.getString(DBContracts.ProfileContract.COLUMN_INDEX_BIO);
            mReputation = cursor.getLong(DBContracts.ProfileContract.COLUMN_INDEX_REP);
            mLastSeen = cursor.getLong(DBContracts.ProfileContract.COLUMN_INDEX_LAST_SEEN);
            mNotoriety = Notoriety.getNotoriety(mReputation);
            mCreated = cursor.getLong(DBContracts.ProfileContract.COLUMN_INDEX_CREATED);
        }
    }

    ImgurUser(Parcel in) {
        mUsername = in.readString();
        mBio = in.readString();
        mAccessToken = in.readString();
        mRefreshToken = in.readString();
        mId = in.readInt();
        mAccessTokenExpiration = in.readLong();
        mCreated = in.readLong();
        mReputation = in.readLong();
        mLastSeen = in.readLong();
        mNotoriety = Notoriety.getNotoriety(mReputation);
    }

    public void copy(ImgurUser user) {
        if (user == null) return;
        mId = user.getId();
        mUsername = user.getUsername();
        mBio = user.getBio();
        mCreated = user.mCreated;
        mReputation = user.getReputation();
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

    public String getBio() {
        return mBio;
    }

    public long getCreated() {
        return mCreated * DateUtils.SECOND_IN_MILLIS;
    }

    public String getUsername() {
        return mUsername;
    }

    public int getId() {
        return mId;
    }

    @NonNull
    public Notoriety getNotoriety() {
        if (mNotoriety == null) mNotoriety = Notoriety.getNotoriety(mReputation);
        return mNotoriety;
    }

    public long getLastSeen() {
        return mLastSeen;
    }

    public void setLastSeen(long lastSeen) {
        mLastSeen = lastSeen;
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
        parcel.writeLong(mCreated);
        parcel.writeLong(mReputation);
        parcel.writeLong(mLastSeen);
    }

    public static final Creator<ImgurUser> CREATOR = new Creator<ImgurUser>() {
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
        ImgurUser user = OpengurApp.getInstance().getUser();
        return user != null && user.equals(this);
    }

    public boolean isSelf(OpengurApp app) {
        if (app.getUser() != null) {
            return app.getUser().equals(this);
        }

        return false;
    }
}
