package com.kenny.openimgur.classes;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.util.LinkUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/11/15.
 */
public class ImgurBaseObject implements Parcelable {
    protected final String TAG = getClass().getSimpleName();

    public static final String VOTE_UP = "up";

    public static final String VOTE_DOWN = "down";

    @SerializedName("ups")
    private int mUpVotes;

    @SerializedName("downs")
    private int mDownVotes;

    @SerializedName("id")
    private String mId;

    @SerializedName("title")
    private String mTitle;

    @SerializedName("description")
    private String mDescription;

    @SerializedName("account_url")
    private String mAccount;

    @SerializedName("link")
    private String mLink;

    @SerializedName("reddit_comments")
    private String mRedditLink;

    @SerializedName("vote")
    private String mVote;

    @SerializedName("deletehash")
    private String mDeleteHash;

    @SerializedName("topic")
    private String mTopic;

    @SerializedName("datetime")
    private long mDate;

    @SerializedName("favorite")
    private boolean mIsFavorited;

    @SerializedName("nsfw")
    private boolean mIsNSFW;

    private boolean mIsListed = true;

    private List<ImgurTag> mTags;

    public ImgurBaseObject(String id, String title, String link) {
        mId = id;
        mTitle = title;
        mLink = link;
    }

    public ImgurBaseObject(String id, String title, String link, String deleteHash) {
        this(id, title, link);
        mDeleteHash = deleteHash;
    }

    public int getUpVotes() {
        return mUpVotes;
    }

    public int getDownVotes() {
        return mDownVotes;
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

    public String getLink() {
        return mLink;
    }

    public long getDate() {
        return mDate;
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

    public void setTags(List<ImgurTag> tags) {
        mTags = tags;
    }

    public List<ImgurTag> getTags() {
        return mTags;
    }

    /**
     * Returns the gallery link
     *
     * @return
     */
    public String getGalleryLink() {
        return ApiClient.IMGUR_GALLERY_URL + getId();
    }

    public void setLink(String link) {
        mLink = link;
    }

    public void setIsListed(boolean isListed) {
        mIsListed = isListed;
    }

    public boolean isListed() {
        return mIsListed;
    }

    /**
     * Converts an objects link from http to https
     */
    public void toHttps() {
        if (LinkUtils.isHttpLink(mLink)) {
            mLink = mLink.replace("http:", "https:");
        }

        if (LinkUtils.isHttpLink(mRedditLink)) {
            mRedditLink = mRedditLink.replace("http:", "https:");
        }
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mUpVotes);
        out.writeInt(mDownVotes);
        out.writeString(mId);
        out.writeString(mTitle);
        out.writeString(mDescription);
        out.writeString(mAccount);
        out.writeString(mLink);
        out.writeString(mRedditLink);
        out.writeString(mVote);
        out.writeString(mDeleteHash);
        out.writeString(mTopic);
        out.writeInt(mIsFavorited ? 1 : 0);
        out.writeInt(mIsNSFW ? 1 : 0);
        out.writeInt(mIsListed ? 1 : 0);
        out.writeLong(mDate);
        out.writeTypedList(mTags);
    }

    public static final Creator<ImgurBaseObject> CREATOR = new Creator<ImgurBaseObject>() {
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
        mId = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        mAccount = in.readString();
        mLink = in.readString();
        mRedditLink = in.readString();
        mVote = in.readString();
        mDeleteHash = in.readString();
        mTopic = in.readString();
        mIsFavorited = in.readInt() == 1;
        mIsNSFW = in.readInt() == 1;
        mIsListed = in.readInt() == 1;
        mDate = in.readLong();
        mTags = new ArrayList<>();
        in.readTypedList(mTags, ImgurTag.CREATOR);
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
        return !TextUtils.isEmpty(mId) ? mId.hashCode() : super.hashCode();
    }

    @Override
    public String toString() {
        return "ID :" + mId
                + "Title :" + mTitle;
    }

    /**
     * Returns an Intent for sharing to application
     *
     * @return
     */
    public Intent getShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String link;

        if (TextUtils.isEmpty(getRedditLink())) {
            link = getGalleryLink();
        } else {
            link = String.format("https://reddit.com%s", getRedditLink());
        }

        shareIntent.putExtra(Intent.EXTRA_TEXT, link);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getTitle());
        return shareIntent;
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