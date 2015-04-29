package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.kenny.openimgur.util.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Base class to hold common values between Imgur Api responses
 */
public class ImgurBaseObject implements Parcelable {

    public static final String VOTE_UP = "up";

    public static final String VOTE_DOWN = "down";

    public static final String KEY_ID = "id";

    private static final String KEY_UP_VOTES = "ups";

    private static final String KEY_DOWN_VOTES = "downs";

    private static final String KEY_VIEWS = "views";

    private static final String KEY_TITLE = "title";

    private static final String KEY_DESCRIPTION = "description";

    private static final String KEY_ACCOUNT = "account_url";

    private static final String KEY_ACCOUNT_ID = "account_id";

    public static final String KEY_DATE = "datetime";

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

    private static final String KEY_TOPIC = "topic";

    private int mUpVotes = Integer.MIN_VALUE;

    private int mDownVotes = Integer.MIN_VALUE;

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

    private String mTopic;

    private long mDate;

    private long mBandwidth;

    private boolean mIsFavorited;

    private boolean mIsNSFW = false;

    private ArrayList<String> mTags;

    public ImgurBaseObject(String id, String title, String link) {
        mId = id;
        mTitle = title;
        mLink = link;
    }

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
            if (!json.isNull(KEY_ID)) {
                mId = json.getString(KEY_ID);
            }

            if (!json.isNull(KEY_UP_VOTES)) {
                mUpVotes = json.getInt(KEY_UP_VOTES);
            }

            if (!json.isNull(KEY_DOWN_VOTES)) {
                mDownVotes = json.getInt(KEY_DOWN_VOTES);
            }

            if (!json.isNull(KEY_VIEWS)) {
                mViews = json.getInt(KEY_VIEWS);
            }

            if (!json.isNull(KEY_TITLE)) {
                mTitle = json.getString(KEY_TITLE);
            }

            if (!json.isNull(KEY_DESCRIPTION)) {
                mDescription = json.getString(KEY_DESCRIPTION);
            }

            if (!json.isNull(KEY_DATE)) {
                mDate = json.getLong(KEY_DATE);
            }

            if (!json.isNull(KEY_BANDWIDTH)) {
                mBandwidth = json.getLong(KEY_BANDWIDTH);
            }

            if (!json.isNull(KEY_ACCOUNT)) {
                mAccount = json.getString(KEY_ACCOUNT);
            }

            if (!json.isNull(KEY_ACCOUNT_ID)) {
                mAccountId = json.getString(KEY_ACCOUNT_ID);
            }

            if (!json.isNull(KEY_LINK)) {
                mLink = json.getString(KEY_LINK);
            }

            if (!json.isNull(KEY_SCORE)) {
                mScore = json.getInt(KEY_SCORE);
            }

            if (!json.isNull(KEY_REDDIT_LINK)) {
                mRedditLink = json.getString(KEY_REDDIT_LINK);
            }

            if (!json.isNull(KEY_FAVORITE)) {
                mIsFavorited = json.getBoolean(KEY_FAVORITE);
            }

            if (!json.isNull(KEY_VOTE)) {
                mVote = json.getString(KEY_VOTE);
            }

            if (!json.isNull(KEY_DELETE_HASH)) {
                mDeleteHash = json.getString(KEY_DELETE_HASH);
            }

            if (!json.isNull(KEY_NSFW)) {
                mIsNSFW = json.getBoolean(KEY_NSFW);
            }

            if (!json.isNull(KEY_GIFV)) {
                mGifVLink = json.getString(KEY_GIFV);
            }

            if (!json.isNull(KEY_MP4)) {
                mMP4Link = json.getString(KEY_MP4);
            }

            if (!json.isNull(KEY_TOPIC)) {
                mTopic = json.getString(KEY_TOPIC);
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

    public void setDate(long date) {
        mDate = date;
    }

    public String getTopic() {
        return mTopic;
    }

    public void setVote(String vote) {
        // If the user had previously voted on the item, we need to update the score
        if (!TextUtils.isEmpty(mVote)) {
            if (vote.equals(VOTE_UP) && mVote.equals(VOTE_DOWN)) {
                mDownVotes--;
                mUpVotes++;
            } else if (vote.equals(VOTE_DOWN) && mVote.equals(VOTE_UP)) {
                mDownVotes++;
                mUpVotes--;
            }
        } else if (vote.equals(VOTE_DOWN)) {
            mDownVotes++;
        } else {
            mUpVotes++;
        }

        mVote = vote;
    }

    public void setTags(ArrayList tags) {
        mTags = tags;
    }

    public ArrayList getTags() {
        return mTags;
    }

    /**
     * Returns the gallery link
     *
     * @return
     */
    public String getGalleryLink() {
        return "https://imgur.com/gallery/" + getId();
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
        out.writeString(mTopic);
        out.writeInt(mIsFavorited ? 1 : 0);
        out.writeInt(mIsNSFW ? 1 : 0);
        out.writeLong(mDate);
        out.writeLong(mBandwidth);
        out.writeStringList(mTags);
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
        mTopic = in.readString();
        mIsFavorited = in.readInt() == 1;
        mIsNSFW = in.readInt() == 1;
        mDate = in.readLong();
        mBandwidth = in.readLong();
        mTags = new ArrayList<>();
        in.readStringList(mTags);
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

    @Override
    public String toString() {
        return "ID :" + mId
                + "Title :" + mTitle;
    }

    /**
     * Returns a thumbnailed version of an image
     *
     * @param id   The id of the image
     * @param link The link of the image
     * @param size The size key to use for the thumbnail
     * @return
     */
    public static String getThumbnail(@NonNull String id, @NonNull String link, @NonNull String size) {
        return link.replace(id, id + size);
    }
}
