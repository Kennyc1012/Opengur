package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class to hold common values between Imgur Api responses
 */
public abstract class ImgurBaseObject implements Parcelable {
    private static final String KEY_ID = "id";

    private static final String KEY_UP_VOTES = "ups";

    private static final String KEY_DOWN_VOTES = "downs";

    private static final String KEY_VIEWS = "views";

    private static final String KEY_TITLE = "title";

    private static final String KEY_DESCRIPTION = "description";

    private static final String KEY_ACCOUNT = "account_url";

    private static final String KEY_ACCOUNT_ID = "account_id";

    private static final String KEY_DATE = "datetime";

    private static final String KEY_BANDWIDTH = "bandwidth";

    private static final String KEY_LINK = "link";

    private static final String KEY_SCORE = "score";

    private int mUpVotes;

    private int mDownVotes;

    private int mViews;

    private int mScore;

    private String mId;

    private String mTitle;

    private String mDescription;

    private String mAccount;

    private String mAccountId;

    private String mLink;

    private long mDate;

    private long mBandwidth;


    protected ImgurBaseObject(JSONObject json) {
        parseJson(json);
    }

    /**
     * Parses the JSON for common values
     *
     * @param json
     */
    private void parseJson(JSONObject json) {
        try {
            if (json.has(KEY_ID)) {
                mId = json.getString(KEY_ID);
            }

            if (json.has(KEY_UP_VOTES)) {
                mUpVotes = json.getInt(KEY_UP_VOTES);
            }

            if (json.has(KEY_DOWN_VOTES)) {
                mDownVotes = json.getInt(KEY_DOWN_VOTES);
            }

            if (json.has(KEY_VIEWS)) {
                mViews = json.getInt(KEY_VIEWS);
            }

            if (json.has(KEY_TITLE)) {
                mTitle = json.getString(KEY_TITLE);
            }

            if (json.has(KEY_DESCRIPTION)) {
                mDescription = json.getString(KEY_DESCRIPTION);
            }

            if (json.has(KEY_DATE)) {
                mDate = json.getLong(KEY_DATE);
            }

            if (json.has(KEY_BANDWIDTH)) {
                mBandwidth = json.getLong(KEY_BANDWIDTH);
            }

            if (json.has(KEY_ACCOUNT)) {
                mAccount = json.getString(KEY_ACCOUNT);
            }

            if (json.has(KEY_ACCOUNT_ID)) {
                mAccountId = json.getString(KEY_ACCOUNT_ID);
            }

            if (json.has(KEY_LINK)) {
                mLink = json.getString(KEY_LINK);
            }

            if (json.has(KEY_SCORE)) {
                mScore = json.getInt(KEY_SCORE);
            }

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    public int getUpVotes() {
        return mUpVotes;
    }

    public int getDownVotes() {
        return mDownVotes;
    }

    public int getViews() {
        return mViews;
    }

    public int getScore() {
        return mScore;
    }

    public String getId() {
        return mId;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public String getAccount() {
        return mAccount;
    }

    public String getAccountId() {
        return mAccountId;
    }

    public String getLink() {
        return mLink;
    }

    public long getDate() {
        return mDate;
    }

    public long getBandwidth() {
        return mBandwidth;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mUpVotes);
        out.writeInt(mDownVotes);
        out.writeInt(mViews);
        out.writeInt(mScore);
        out.writeString(mId);
        out.writeString(mTitle);
        out.writeString(mDescription);
        out.writeString(mAccount);
        out.writeString(mAccountId);
        out.writeString(mLink);
        out.writeLong(mDate);
        out.writeLong(mBandwidth);
    }

    protected ImgurBaseObject(Parcel in) {
        mUpVotes = in.readInt();
        mDownVotes = in.readInt();
        mViews = in.readInt();
        mScore = in.readInt();
        mId = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mAccount = in.readString();
        mAccountId = in.readString();
        mLink = in.readString();
        mDate = in.readLong();
        mBandwidth = in.readLong();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (!(object instanceof ImgurBaseObject)) {
            return false;
        }

        return ((ImgurBaseObject) object).mId == this.mId;
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }
}
