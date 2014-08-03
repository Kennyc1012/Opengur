package com.kenny.openimgur.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovementMethod;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurListener;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends BaseAdapter {

    private List<ImgurComment> mCurrentComments;

    private LayoutInflater mInflater;

    private long mCurrentTime;

    private ImgurListener mListener;

    public CommentAdapter(Context context, List<ImgurComment> comments, ImgurListener listener) {
        mCurrentComments = comments;
        mInflater = LayoutInflater.from(context);
        mCurrentTime = System.currentTimeMillis();
        mListener = listener;
    }

    /**
     * Returns all comments in the adapter
     *
     * @return
     */
    public List<ImgurComment> getItems() {
        return mCurrentComments;
    }

    public void clear() {
        if (mCurrentComments != null) {
            mCurrentComments.clear();

        }
    }

    public void addComments(List<ImgurComment> comments) {
        if (mCurrentComments == null) {
            mCurrentComments = new ArrayList<ImgurComment>();
        }

        mCurrentComments.addAll(comments);
    }

    @Override
    public int getCount() {
        if (mCurrentComments != null) {
            return mCurrentComments.size();
        }
        return 0;
    }

    @Override
    public ImgurComment getItem(int position) {
        if (mCurrentComments != null && !mCurrentComments.isEmpty()) {
            return mCurrentComments.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CommentViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.comment_item, parent, false);
            holder = new CommentViewHolder();
            holder.comment = (RobotoTextView) convertView.findViewById(R.id.comment);
            holder.author = (RobotoTextView) convertView.findViewById(R.id.author);
            holder.replies = (Button) convertView.findViewById(R.id.replies);
            holder.comment.setMovementMethod(CustomLinkMovementMethod.getInstance(mListener));
            holder.up = (ImageButton) convertView.findViewById(R.id.upVote);
            holder.down = (ImageButton) convertView.findViewById(R.id.downVote);

            holder.up.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onVoteCast(ImgurBaseObject.VOTE_UP, view);
                    }
                }
            });

            holder.down.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onVoteCast(ImgurBaseObject.VOTE_DOWN, view);
                    }
                }
            });

            holder.replies.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onViewRepliesTap(view);
                    }
                }
            });

            convertView.setTag(holder);
        } else {
            holder = (CommentViewHolder) convertView.getTag();
        }

        ImgurComment comment = getItem(position);
        holder.comment.setText(comment.getComment());
        Linkify.addLinks(holder.comment, Linkify.WEB_URLS);
        holder.author.setText(constructSpan(comment, holder.author.getContext()));
        if (comment.getReplyCount() <= 0) {
            holder.replies.setVisibility(View.GONE);
        } else {
            holder.replies.setVisibility(View.VISIBLE);
            holder.replies.setText(convertView.getContext().getString(R.string.comment_replies, comment.getReplyCount()));
        }

        if (!TextUtils.isEmpty(comment.getVote())) {
            if (comment.getVote().equals(ImgurComment.VOTE_DOWN)) {
                convertView.setBackgroundResource(R.drawable.downvote_border);
            } else {
                convertView.setBackgroundResource(R.drawable.upvote_border);
            }
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

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
        StringBuilder sb = new StringBuilder(comment.getAuthor());
        sb.append(" ").append(comment.getPoints()).append(" ").append(context.getString(R.string.points))
                .append(" : ").append(date);
        Spannable span = new SpannableString(sb.toString());

        int color = context.getResources().getColor(android.R.color.holo_green_light);
        if (comment.getPoints() < 0) {
            color = context.getResources().getColor(android.R.color.holo_red_light);
        }

        span.setSpan(new ForegroundColorSpan(color), comment.getAuthor().length(), sb.length() - date.length() - 2,
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
                        DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    private static class CommentViewHolder {
        RobotoTextView author, comment;

        ImageButton up, down;

        Button replies;
    }
}
