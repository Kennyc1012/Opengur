package com.kenny.openimgur.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.classes.OpengurApp;

import java.util.List;

import butterknife.BindView;

/**
 * Created by kcampagna on 7/27/15.
 */
public class MessagesAdapter extends BaseRecyclerAdapter<ImgurMessage> {
    private final int VIEW_TYPE_OTHER = 0;

    private final int VIEW_TYPE_SELF = 1;

    private int mMargin;

    private int mUserId;

    private int mUserColor;

    private ImgurListener mListener;

    public MessagesAdapter(Context context, int userColor, List<ImgurMessage> messages, ImgurListener listener) {
        super(context, messages);
        mUserId = OpengurApp.getInstance(context).getUser().getId();
        mMargin = (int) (mResources.getDisplayMetrics().widthPixels * .25);
        mListener = listener;
        mUserColor = userColor;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MessagesViewHolder holder = new MessagesViewHolder(mInflater.inflate(R.layout.convo_message, parent, false));
        holder.message.setMovementMethod(CustomLinkMovement.getInstance(mListener));
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) holder.container.getLayoutParams();

        if (viewType == VIEW_TYPE_SELF) {
            holder.container.setBackgroundColor(Color.WHITE);
            lp.setMargins(mMargin, 0, 0, 0);
            lp.gravity = Gravity.RIGHT;
            holder.container.setGravity(Gravity.RIGHT);
            holder.container.setLayoutParams(lp);
            holder.message.setTextColor(Color.BLACK);
            holder.timeStamp.setTextColor(Color.BLACK);
        } else {
            holder.container.setBackgroundColor(mUserColor);
            lp.setMargins(0, 0, mMargin, 0);
            lp.gravity = Gravity.LEFT;
            holder.container.setGravity(Gravity.LEFT);
            holder.container.setLayoutParams(lp);
            holder.message.setTextColor(Color.WHITE);
            holder.timeStamp.setTextColor(Color.WHITE);
        }

        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        ImgurMessage msg = getItem(position);
        return msg.getSenderId() == mUserId ? VIEW_TYPE_SELF : VIEW_TYPE_OTHER;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        MessagesViewHolder messagesViewHolder = (MessagesViewHolder) holder;
        ImgurMessage message = getItem(position);

        messagesViewHolder.message.setText(message.getBody());
        Linkify.addLinks(messagesViewHolder.message, Linkify.WEB_URLS);

        if (message.isSending()) {
            messagesViewHolder.timeStamp.setText(R.string.convo_message_sending);
        } else if (message.getDate() > 0) {
            messagesViewHolder.timeStamp.setText(getDateFormattedTime(message.getDate() * DateUtils.SECOND_IN_MILLIS));
        } else {
            messagesViewHolder.timeStamp.setText(R.string.convo_message_failed);
        }
    }

    private CharSequence getDateFormattedTime(long commentDate) {
        long now = System.currentTimeMillis();
        long difference = System.currentTimeMillis() - commentDate;

        return (difference >= 0 && difference <= DateUtils.MINUTE_IN_MILLIS) ?
                mResources.getString(R.string.moments_ago) :
                DateUtils.getRelativeTimeSpanString(
                        commentDate,
                        now,
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_RELATIVE
                                | DateUtils.FORMAT_ABBREV_ALL);
    }

    /**
     * Updates the status of a sent message
     *
     * @param successful If the message was successfully sent
     * @param id         The id of the message
     */
    public void onMessageSendComplete(boolean successful, String id) {
        // The message will most likely be the last item in the list, or near the end
        for (int i = getItemCount() - 1; i >= 0; i--) {
            ImgurMessage message = getItem(i);

            if (message.getId().equals(id)) {
                message.setIsSending(false);
                if (!successful) message.setDate(-1L);
                notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public void onDestroy() {
        mListener = null;
        super.onDestroy();
    }

    static class MessagesViewHolder extends BaseViewHolder {
        @BindView(R.id.messageContainer)
        LinearLayout container;

        @BindView(R.id.message)
        TextView message;

        @BindView(R.id.timeStamp)
        TextView timeStamp;

        public MessagesViewHolder(View view) {
            super(view);
        }
    }
}
