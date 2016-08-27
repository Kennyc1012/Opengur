package com.kenny.openimgur.classes;

/**
 * Created by kcampagna on 7/11/15.
 */

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 6/21/14.
 */
public class ImgurComment extends ImgurBaseObject {
    @SerializedName("author")
    private String mAuthor;

    @SerializedName("author_id")
    private String mAuthorId;

    @SerializedName("comment")
    private String mComment;

    @SerializedName("image_id")
    private String mImageId;

    @SerializedName("album_cover")
    private String mAlbumCoverId;

    @SerializedName("on_album")
    private boolean mIsAlbumComment;

    @SerializedName("deleted")
    private boolean mIsDeleted;

    @SerializedName("children")
    private List<ImgurComment> mChildrenComments;

    @SerializedName("parent_id")
    private long mParentId;

    @SerializedName("points")
    private long mPoints;

    ImgurComment(Parcel in) {
        super(in);
        mAuthor = in.readString();
        mAuthorId = in.readString();
        mComment = in.readString();
        mImageId = in.readString();
        mAlbumCoverId = in.readString();
        mIsDeleted = in.readInt() == 1;
        mIsAlbumComment = in.readInt() == 1;
        mChildrenComments = new ArrayList<>();
        in.readTypedList(mChildrenComments, ImgurComment.CREATOR);
        mParentId = in.readLong();
        mPoints = in.readLong();
    }

    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeString(mAuthor);
        out.writeString(mAuthorId);
        out.writeString(mComment);
        out.writeString(mImageId);
        out.writeString(mAlbumCoverId);
        out.writeInt(mIsDeleted ? 1 : 0);
        out.writeInt(mIsAlbumComment ? 1 : 0);
        out.writeTypedList(mChildrenComments);
        out.writeLong(mParentId);
        out.writeLong(mPoints);
    }

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ImgurComment> CREATOR = new Parcelable.Creator<ImgurComment>() {
        public ImgurComment createFromParcel(Parcel in) {
            return new ImgurComment(in);
        }

        public ImgurComment[] newArray(int size) {
            return new ImgurComment[size];
        }
    };

    /**
     * Returns the number of replies the comment has
     *
     * @return
     */
    public int getReplyCount() {
        return mChildrenComments != null ? mChildrenComments.size() : 0;
    }

    public List<ImgurComment> getReplies() {
        return mChildrenComments;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public String getAuthorId() {
        return mAuthorId;
    }

    public String getComment() {
        return mComment;
    }

    public boolean IsDeleted() {
        return mIsDeleted;
    }

    public long getPoints() {
        return mPoints;
    }

    public long getParentId() {
        return mParentId;
    }

    public boolean isAlbumComment() {
        return mIsAlbumComment;
    }

    public String getImageId() {
        return mImageId;
    }

    public String getAlbumCoverId() {
        return mAlbumCoverId;
    }

    @Override
    public void setVote(String vote) {
        String currentVote = getVote();

        if (!TextUtils.isEmpty(getVote())) {
            if (vote.equals(VOTE_UP) && currentVote.equals(VOTE_DOWN)) {
                mPoints += 2;
            } else if (vote.equals(VOTE_DOWN) && currentVote.equals(VOTE_UP)) {
                mPoints -= 2;
            }
        } else if (vote.equals(VOTE_DOWN)) {
            mPoints--;
        } else {
            mPoints++;
        }

        super.setVote(vote);
    }

    @Override
    public String toString() {
        return "ID: " + getId() +
                " AUTHOR: " + getAuthor();
    }
}
