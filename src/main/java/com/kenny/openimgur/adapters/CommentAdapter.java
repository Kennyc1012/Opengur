package com.kenny.openimgur.adapters;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

public class CommentAdapter extends ImgurBaseAdapter {
    private LayoutInflater mInflater;

    private ImgurListener mListener;

    private int mSelectedIndex = -1;

    private String mOP;

    public CommentAdapter(Context context, List<ImgurComment> comments, ImgurListener listener) {
        super(context, comments);
        mInflater = LayoutInflater.from(context);
        mListener = listener;
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return null;
    }

    @Override
    public ArrayList<ImgurComment> retainItems() {
        return new ArrayList<ImgurComment>(getAllItems());
    }

    /**
     * Removes all items from list and ImgurListener is removed
     */
    public void destroy() {
        clear();
        mListener = null;
    }

    @Override
    public ImgurComment getItem(int position) {
        return (ImgurComment) super.getItem(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CommentViewHolder holder;
        final ImgurComment comment = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.comment_item, parent, false);
            holder = new CommentViewHolder(convertView);
            holder.replies.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onViewRepliesTap(view);
                    }
                }
            });

            holder.comment.setMovementMethod(CustomLinkMovement.getInstance(mListener));
        } else {
            holder = (CommentViewHolder) convertView.getTag();
        }

        holder.comment.setText(comment.getComment());
        holder.author.setText(constructSpan(comment, holder.author.getContext()));
        Linkify.addLinks(holder.comment, Linkify.WEB_URLS);
        holder.score.setText(String.valueOf(comment.getPoints()));
        holder.score.setBackgroundResource(comment.getPoints() >= 0 ? R.drawable.positive_circle : R.drawable.negative_circle);

        if (comment.getReplyCount() > 0) {
            holder.replies.setText(comment.getReplyCount() > 9 ? "9+" : String.valueOf(comment.getReplyCount()));
            holder.replies.setVisibility(View.VISIBLE);
        } else {
            holder.replies.setVisibility(View.GONE);
        }

        convertView.setBackgroundColor(position == mSelectedIndex ?
                convertView.getResources().getColor(R.color.comment_bg_selected) :
                convertView.getResources().getColor(android.R.color.transparent));

        return convertView;
    }

    /**
     * Sets the currently selected item. If the item selected is the one that is already selected, it is deselected
     *
     * @param index
     * @return If the selected item was already selected
     */
    public boolean setSelectedIndex(int index) {
        boolean wasSelected = mSelectedIndex == index;
        mSelectedIndex = wasSelected ? -1 : index;
        notifyDataSetChanged();

        return wasSelected;
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
        boolean isOp = isOP(author);
        int spanLength = author.length();

        if (isOp) {
            // TODO Other languages for OP?
            sb.append(" OP");
            spanLength += 3;
        }

        sb.append(" : ").append(date);
        Spannable span = new SpannableString(sb.toString());

        int green = context.getResources().getColor(R.color.notoriety_positive);

        if (isOp) {
            span.setSpan(new ForegroundColorSpan(green), author.length() + 1, spanLength, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

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
                        DateUtils.FORMAT_NUMERIC_DATE);
    }

    public void setOP(String op) {
        mOP = op;
    }

    private boolean isOP(String user) {
        if (!TextUtils.isEmpty(mOP)) {
            return mOP.equals(user);
        }

        return false;
    }

    static class CommentViewHolder extends ImgurViewHolder {
        @InjectView(R.id.author)
        TextViewRoboto author;

        @InjectView(R.id.comment)
        TextViewRoboto comment;

        @InjectView(R.id.score)
        TextViewRoboto score;

        @InjectView(R.id.replies)
        TextViewRoboto replies;

        public CommentViewHolder(View view) {
            super(view);
        }
    }
}
