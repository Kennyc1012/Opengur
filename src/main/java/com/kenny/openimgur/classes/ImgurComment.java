package com.kenny.openimgur.classes;

import android.os.Parcel;
import android.os.Parcelable;

import com.kenny.openimgur.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 6/21/14.
 */
public class ImgurComment extends ImgurBaseObject {
    private static final String KEY_AUTHOR = "author";

    private static final String KEY_AUTHOR_ID = "author_id";

    private static final String KEY_DELETED = "deleted";

    private static final String KEY_CHILDREN = "children";

    private static final String KEY_PARENT_ID = "parent_id";

    private static final String KEY_COMMENT = "comment";

    private static final String KEY_POINTS = "points";

    private static final String KEY_IMAGE_ID = "image_id";

    private static final String KEY_ON_ALBUM = "on_album";

    private static final String KEY_ALBUM_COVER_ID = "album_cover";

    private String mAuthor;

    private String mAuthorId;

    private String mComment;

    private String mImageId;

    private String mAlbumCoverId;

    private boolean mIsAlbumComment;

    private boolean mIsDeleted;

    private List<ImgurComment> mChildrenComments;

    private long mParentId;

    private long mPoints;

    public ImgurComment(JSONObject json) {
        super(json);
        parseJson(json);
    }

    private void parseJson(JSONObject json) {
        try {
            if (!json.isNull(KEY_AUTHOR)) {
                mAuthor = json.getString(KEY_AUTHOR);
            }

            if (!json.isNull(KEY_AUTHOR_ID)) {
                mAuthorId = json.getString(KEY_AUTHOR_ID);
            }

            if (!json.isNull(KEY_DELETED)) {
                mIsDeleted = json.getBoolean(KEY_DELETED);
            }

            if (!json.isNull(KEY_CHILDREN)) {
                JSONArray arr = json.getJSONArray(KEY_CHILDREN);
                mChildrenComments = new ArrayList<>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    ImgurComment comment = new ImgurComment(arr.getJSONObject(i));
                    mChildrenComments.add(comment);
                }
            }

            if (!json.isNull(KEY_PARENT_ID)) {
                mParentId = json.getLong(KEY_PARENT_ID);
            }

            if (!json.isNull(KEY_COMMENT)) {
                mComment = json.getString(KEY_COMMENT);
            }

            if (!json.isNull(KEY_POINTS)) {
                mPoints = json.getLong(KEY_POINTS);
            }

            if (!json.isNull(KEY_IMAGE_ID)) {
                mImageId = json.getString(KEY_IMAGE_ID);
            }

            if (!json.isNull(KEY_ALBUM_COVER_ID)) {
                mAlbumCoverId = json.getString(KEY_ALBUM_COVER_ID);
            }

            if (!json.isNull(KEY_ON_ALBUM)) {
                mIsAlbumComment = json.getBoolean(KEY_ON_ALBUM);
            }

        } catch (JSONException ex) {
            LogUtil.e("ImgurComment", "Error Decoding JSON", ex);
        }
    }

    private ImgurComment(Parcel in) {
        super(in);
        mAuthor = in.readString();
        mAuthorId = in.readString();
        mComment = in.readString();
        mImageId = in.readString();
        mAlbumCoverId = in.readString();
        mIsDeleted = in.readInt() == 1;
        mIsAlbumComment = in.readInt() == 1;
        mChildrenComments = new ArrayList<>();
        in.readList(mChildrenComments, null);
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
}
