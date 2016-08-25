package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Kenny-PC on 8/1/2015.
 */
public class ProfileCommentAdapter extends BaseRecyclerAdapter<ImgurComment> {
    private int mDividerColor;

    private View.OnClickListener mClickListener;

    public ProfileCommentAdapter(Context context, List<ImgurComment> comments, View.OnClickListener listener) {
        super(context, comments, true);
        mClickListener = listener;
        mDividerColor = isDarkTheme ? getColor(R.color.primary_dark_light) : Color.BLACK;
    }

    @Override
    public void onDestroy() {
        mClickListener = null;
        super.onDestroy();
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForComments().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflateView(R.layout.profile_comment_item, parent);
        view.setOnClickListener(mClickListener);
        CommentViewHolder holder = new CommentViewHolder(view);
        holder.divider.setBackgroundColor(mDividerColor);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        CommentViewHolder commentViewHolder = (CommentViewHolder) holder;
        ImgurComment comment = getItem(position);
        String photoUrl;

        commentViewHolder.author.setText(constructSpan(comment));
        commentViewHolder.comment.setText(comment.getComment());

        if (comment.isAlbumComment() && !TextUtils.isEmpty(comment.getAlbumCoverId())) {
            photoUrl = String.format(ImgurAlbum.ALBUM_COVER_URL, comment.getAlbumCoverId() + ImgurPhoto.THUMBNAIL_SMALL);
        } else {
            photoUrl = ApiClient.IMGUR_URL + comment.getImageId() + ImgurPhoto.THUMBNAIL_SMALL + ".jpeg";
        }

        displayImage(commentViewHolder.image, photoUrl);
    }

    /**
     * Creates the spannable object for the authors name, points, and time
     *
     * @param comment
     * @return
     */
    private Spannable constructSpan(ImgurComment comment) {
        CharSequence date = getDateFormattedTime(comment.getDate() * DateUtils.SECOND_IN_MILLIS);
        String author = comment.getAuthor();
        StringBuilder sb = new StringBuilder(author);
        int spanLength = author.length();
        int points = (int) comment.getPoints();

        sb.append(" ")
                .append(getResources().getQuantityString(R.plurals.points, points, points))
                .append(" : ")
                .append(date);

        Spannable span = new SpannableString(sb.toString());

        int color = comment.getPoints() < 0 ? getColor(R.color.notoriety_negative) : getColor(R.color.notoriety_positive);

        span.setSpan(new ForegroundColorSpan(color), spanLength, sb.length() - date.length() - 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return span;
    }

    private CharSequence getDateFormattedTime(long commentDate) {
        long now = System.currentTimeMillis();
        long difference = System.currentTimeMillis() - commentDate;

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                getResources().getString(R.string.moments_ago) :
                DateUtils.getRelativeTimeSpanString(
                        commentDate,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE
                                | DateUtils.FORMAT_ABBREV_ALL);
    }

    static class CommentViewHolder extends BaseViewHolder {
        @BindView(R.id.author)
        TextView author;

        @BindView(R.id.comment)
        TextView comment;

        @BindView(R.id.image)
        ImageView image;

        @BindView(R.id.divider)
        View divider;

        public CommentViewHolder(View view) {
            super(view);
        }
    }
}
