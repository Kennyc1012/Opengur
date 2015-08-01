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
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.Bind;

/**
 * Created by Kenny-PC on 8/1/2015.
 */
public class ProfileCommentAdapter extends BaseRecyclerAdapter<ImgurComment> {
    private int mDividerColor;

    private View.OnClickListener mClickListener;

    public ProfileCommentAdapter(Context context, List<ImgurComment> comments, View.OnClickListener listener) {
        super(context, comments, true);
        mClickListener = listener;
        boolean isDarkTheme = OpengurApp.getInstance(context).getImgurTheme().isDarkTheme;
        mDividerColor = isDarkTheme ? context.getResources().getColor(R.color.primary_dark_material_light) : context.getResources().getColor(R.color.primary_dark_material_dark);
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
        View view = mInflater.inflate(R.layout.profile_comment_item, parent, false);
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

        commentViewHolder.author.setText(constructSpan(comment, commentViewHolder.author.getContext()));
        commentViewHolder.comment.setText(comment.getComment());

        if (comment.isAlbumComment() && !TextUtils.isEmpty(comment.getAlbumCoverId())) {
            photoUrl = String.format(ImgurAlbum.ALBUM_COVER_URL, comment.getAlbumCoverId() + ImgurPhoto.THUMBNAIL_SMALL);
        } else {
            photoUrl = "https://imgur.com/" + comment.getImageId() + ImgurPhoto.THUMBNAIL_SMALL + ".jpeg";
        }

        displayImage(commentViewHolder.image, photoUrl);
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

    static class CommentViewHolder extends BaseViewHolder {
        @Bind(R.id.author)
        TextView author;

        @Bind(R.id.comment)
        TextView comment;

        @Bind(R.id.image)
        ImageView image;

        @Bind(R.id.divider)
        View divider;

        public CommentViewHolder(View view) {
            super(view);
        }
    }
}
