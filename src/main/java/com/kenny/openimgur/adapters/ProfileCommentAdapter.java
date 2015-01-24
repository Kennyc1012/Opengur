package com.kenny.openimgur.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by kcampagna on 12/22/14.
 */
public class ProfileCommentAdapter extends ImgurBaseAdapter<ImgurComment> {

    public ProfileCommentAdapter(Context context, List<ImgurComment> comments) {
        super(context, comments, true);
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForComments().build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CommentViewHolder holder;
        ImgurComment comment = getItem(position);
        String photoUrl;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.profile_comment_item, parent, false);
            holder = new CommentViewHolder(convertView);
        } else {
            holder = (CommentViewHolder) convertView.getTag();
        }

        holder.author.setText(constructSpan(comment, convertView.getContext()));
        holder.comment.setText(comment.getComment());

        if (comment.isAlbumComment() && !TextUtils.isEmpty(comment.getAlbumCoverId())) {
            photoUrl = String.format(Endpoints.ALBUM_COVER.getUrl(), comment.getAlbumCoverId() + ImgurPhoto.THUMBNAIL_SMALL);
        } else {
            photoUrl = "https://imgur.com/" + comment.getImageId() + ImgurPhoto.THUMBNAIL_SMALL + ".jpeg";
        }

        displayImage(holder.image, photoUrl);

        return convertView;
    }

    /**
     * Creates the spannable object for the authors name, points, and time
     *
     * @param comment
     * @param context
     * @return
     */
    private Spannable constructSpan(ImgurComment comment, Context context) {
        CharSequence date = getDateFormattedTime(comment.getDate() * 1000L, context);
        String author = comment.getAuthor();
        StringBuilder sb = new StringBuilder(author);
        int spanLength = author.length();

        sb.append(" ").append(comment.getPoints()).append(" ").append(context.getString(R.string.points))
                .append(" : ").append(date);
        Spannable span = new SpannableString(sb.toString());

        int color = context.getResources().getColor(R.color.notoriety_positive);
        if (comment.getPoints() < 0) {
            color = context.getResources().getColor(R.color.notoriety_negative);
        }

        span.setSpan(new ForegroundColorSpan(color), spanLength, sb.length() - date.length() - 2,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return span;
    }

    private CharSequence getDateFormattedTime(long commentDate, Context context) {
        long now = System.currentTimeMillis();
        long difference = System.currentTimeMillis() - commentDate;

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                context.getResources().getString(R.string.moments_ago) :
                DateUtils.getRelativeTimeSpanString(
                        commentDate,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE
                                | DateUtils.FORMAT_ABBREV_ALL);
    }

    static class CommentViewHolder {
        @InjectView(R.id.author)
        TextViewRoboto author;

        @InjectView(R.id.comment)
        TextViewRoboto comment;

        @InjectView(R.id.image)
        ImageView image;

        public CommentViewHolder(View view) {
            ButterKnife.inject(this, view);
            view.setTag(this);
        }
    }
}
