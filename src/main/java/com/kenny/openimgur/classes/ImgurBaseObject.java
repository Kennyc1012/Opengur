package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class to hold common values between Imgur Api responses
 */
public class ImgurBaseObject implements Parcelable {

    public static final String VOTE_UP = "up";

    public static final String VOTE_DOWN = "down";

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

    private static final String KEY_REDDIT_LINK = "reddit_comments";

    private static final String KEY_FAVORITE = "favorite";

    private static final String KEY_VOTE = "vote";

    private static final String KEY_DELETE_HASH = "deletehash";

    private static final String KEY_NSFW = "nsfw";

    private static final String KEY_GIFV = "gifv";

    private static final String KEY_MP4 = "mp4";

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

    private String mGifVLink;

    private String mMP4Link;

    private String mRedditLink;

    private String mVote;

    private String mDeleteHash;

    private long mDate;

    private long mBandwidth;

    private boolean mIsFavorited;

    private boolean mIsNSFW = false;

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
            if (json.has(KEY_ID) && !json.get(KEY_ID).equals(null)) {
                mId = json.getString(KEY_ID);
            }

            if (json.has(KEY_UP_VOTES) && !json.get(KEY_UP_VOTES).equals(null)) {
                mUpVotes = json.getInt(KEY_UP_VOTES);
            }

            if (json.has(KEY_DOWN_VOTES) && !json.get(KEY_DOWN_VOTES).equals(null)) {
                mDownVotes = json.getInt(KEY_DOWN_VOTES);
            }

            if (json.has(KEY_VIEWS) && !json.get(KEY_VIEWS).equals(null)) {
                mViews = json.getInt(KEY_VIEWS);
            }

            if (json.has(KEY_TITLE) && !json.get(KEY_TITLE).equals(null)) {
                mTitle = json.getString(KEY_TITLE);
            }

            if (json.has(KEY_DESCRIPTION) && !json.get(KEY_DESCRIPTION).equals(null)) {
                mDescription = json.getString(KEY_DESCRIPTION);
            }

            if (json.has(KEY_DATE) && !json.get(KEY_DATE).equals(null)) {
                mDate = json.getLong(KEY_DATE);
            }

            if (json.has(KEY_BANDWIDTH) && !json.get(KEY_BANDWIDTH).equals(null)) {
                mBandwidth = json.getLong(KEY_BANDWIDTH);
            }

            if (json.has(KEY_ACCOUNT) && !json.get(KEY_ACCOUNT).equals(null)) {
                mAccount = json.getString(KEY_ACCOUNT);
            }

            if (json.has(KEY_ACCOUNT_ID) && !json.get(KEY_ACCOUNT_ID).equals(null)) {
                mAccountId = json.getString(KEY_ACCOUNT_ID);
            }

            if (json.has(KEY_LINK) && !json.get(KEY_LINK).equals(null)) {
                mLink = json.getString(KEY_LINK);
            }

            if (json.has(KEY_SCORE) && !json.get(KEY_SCORE).equals(null)) {
                mScore = json.getInt(KEY_SCORE);
            }

            if (json.has(KEY_REDDIT_LINK) && !json.get(KEY_REDDIT_LINK).equals(null)) {
                mRedditLink = json.getString(KEY_REDDIT_LINK);
            }

            if (json.has(KEY_FAVORITE) && !json.get(KEY_FAVORITE).equals(null)) {
                mIsFavorited = json.getBoolean(KEY_FAVORITE);
            }

            if (json.has(KEY_VOTE) && !json.get(KEY_VOTE).equals(null)) {
                mVote = json.getString(KEY_VOTE);
            }

            if (json.has(KEY_DELETE_HASH) && !json.get(KEY_DELETE_HASH).equals(null)) {
                mDeleteHash = json.getString(KEY_DELETE_HASH);
            }

            if (json.has(KEY_NSFW) && !json.get(KEY_NSFW).equals(null)) {
                mIsNSFW = json.getBoolean(KEY_NSFW);
            }

            if (json.has(KEY_GIFV) && !json.get(KEY_GIFV).equals(null)) {
                mGifVLink = json.getString(KEY_GIFV);
            }

            if (json.has(KEY_MP4) && !json.get(KEY_MP4).equals(null)) {
                mMP4Link = json.getString(KEY_MP4);
            }

        } catch (JSONException ex) {
            LogUtil.e("ImgurBaseObject", "Error Decoding JSON", ex);
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

    public String getRedditLink() {
        return mRedditLink;
    }

    public boolean isFavorited() {
        return mIsFavorited;
    }

    public String getVote() {
        return mVote;
    }

    public String getDeleteHash() {
        return mDeleteHash;
    }

    public void setIsFavorite(boolean favorite) {
        mIsFavorited = favorite;
    }

    public boolean isNSFW() {
        return mIsNSFW;
    }

    public boolean hasGifVLink() {
        return !TextUtils.isEmpty(mGifVLink);
    }

    public boolean hasMP4Link() {
        return !TextUtils.isEmpty(mMP4Link);
    }

    public String getMP4Link() {
        return mMP4Link;
    }

    public String getGifVLink() {
        return mGifVLink;
    }

    /**
     * Returns the gallery link
     *
     * @return
     */
    public String getGalleryLink() {
        return new StringBuilder("http://imgur.com/gallery/").append(getId()).toString();
    }

    @Override
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
        out.writeString(mRedditLink);
        out.writeString(mVote);
        out.writeString(mDeleteHash);
        out.writeString(mGifVLink);
        out.writeString(mMP4Link);
        out.writeInt(mIsFavorited ? 1 : 0);
        out.writeInt(mIsNSFW ? 1 : 0);
        out.writeLong(mDate);
        out.writeLong(mBandwidth);
    }

    public static final Parcelable.Creator<ImgurBaseObject> CREATOR = new Parcelable.Creator<ImgurBaseObject>() {
        public ImgurBaseObject createFromParcel(Parcel in) {
            return new ImgurBaseObject(in);
        }

        public ImgurBaseObject[] newArray(int size) {
            return new ImgurBaseObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
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
        mRedditLink = in.readString();
        mVote = in.readString();
        mDeleteHash = in.readString();
        mGifVLink = in.readString();
        mMP4Link = in.readString();
        mIsFavorited = in.readInt() == 1;
        mIsNSFW = in.readInt() == 1;
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

        return ((ImgurBaseObject) object).mId.equals(this.mId);
    }

    @Override
    public int hashCode() {
        return mId.hashCode();
    }
}
