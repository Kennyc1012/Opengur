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

    private String mAuthor;

    private String mAuthorId;

    private String mComment;

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
            if (json.has(KEY_AUTHOR) && !json.get(KEY_AUTHOR).equals(null)) {
                mAuthor = json.getString(KEY_AUTHOR);
            }

            if (json.has(KEY_AUTHOR_ID) && !json.get(KEY_AUTHOR_ID).equals(null)) {
                mAuthorId = json.getString(KEY_AUTHOR_ID);
            }

            if (json.has(KEY_DELETED) && !json.get(KEY_DELETED).equals(null)) {
                mIsDeleted = json.getBoolean(KEY_DELETED);
            }

            if (json.has(KEY_CHILDREN) && !json.get(KEY_CHILDREN).equals(null)) {
                JSONArray arr = json.getJSONArray(KEY_CHILDREN);
                mChildrenComments = new ArrayList<ImgurComment>(arr.length());
                for (int i = 0; i < arr.length(); i++) {
                    ImgurComment comment = new ImgurComment(arr.getJSONObject(i));
                    mChildrenComments.add(comment);
                }
            }

            if (json.has(KEY_PARENT_ID) && !json.get(KEY_PARENT_ID).equals(null)) {
                mParentId = json.getLong(KEY_PARENT_ID);
            }

            if (json.has(KEY_COMMENT) && !json.get(KEY_COMMENT).equals(null)) {
                mComment = json.getString(KEY_COMMENT);
            }

            if (json.has(KEY_POINTS) && !json.get(KEY_POINTS).equals(null)) {
                mPoints = json.getLong(KEY_POINTS);
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
        mIsDeleted = in.readInt() == 1;
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
        out.writeInt(mIsDeleted ? 1 : 0);
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
}
