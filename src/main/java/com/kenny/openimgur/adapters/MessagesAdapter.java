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

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.classes.OpengurApp;

import java.util.List;

import butterknife.Bind;

/**
 * Created by kcampagna on 7/27/15.
 */
public class MessagesAdapter extends BaseRecyclerAdapter<ImgurMessage> {

    private int mMargin;

    private int mUserId;

    private ImgurListener mListener;

    public MessagesAdapter(Context context, List<ImgurMessage> messages, ImgurListener listener) {
        super(context, messages);
        mUserId = OpengurApp.getInstance(context).getUser().getId();
        mMargin = (int) (context.getResources().getDisplayMetrics().widthPixels * .25);
        mListener = listener;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        MessagesViewHolder holder = new MessagesViewHolder(mInflater.inflate(R.layout.convo_message, parent, false));
        holder.message.setMovementMethod(CustomLinkMovement.getInstance(mListener));
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        MessagesViewHolder messagesViewHolder = (MessagesViewHolder) holder;
        ImgurMessage message = getItem(position);

        messagesViewHolder.message.setText(message.getBody());
        Linkify.addLinks(messagesViewHolder.message, Linkify.WEB_URLS);
        messagesViewHolder.configView(message, mMargin, mUserId);

        if (message.isSending()) {
            messagesViewHolder.timeStamp.setText(R.string.convo_message_sending);
        } else if (message.getDate() > 0) {
            messagesViewHolder.timeStamp.setText(getDateFormattedTime(message.getDate() * DateUtils.SECOND_IN_MILLIS, messagesViewHolder.container.getContext()));
        } else {
            messagesViewHolder.timeStamp.setText(R.string.convo_message_failed);
        }
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
        @Bind(R.id.messageContainer)
        LinearLayout container;

        @Bind(R.id.message)
        TextView message;

        @Bind(R.id.timeStamp)
        TextView timeStamp;

        public MessagesViewHolder(View view) {
            super(view);
        }

        public void configView(ImgurMessage imgurMessage, int margin, int userId) {
            boolean isUser = userId == imgurMessage.getSenderId();
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) container.getLayoutParams();

            if (isUser) {
                container.setBackgroundColor(Color.WHITE);
                lp.setMargins(margin, 0, 0, 0);
                lp.gravity = Gravity.RIGHT;
                container.setGravity(Gravity.RIGHT);
                container.setLayoutParams(lp);
                message.setTextColor(Color.BLACK);
                timeStamp.setTextColor(Color.BLACK);
            } else {
                container.setBackgroundColor(ColorGenerator.DEFAULT.getColor(imgurMessage.getFrom()));
                lp.setMargins(0, 0, margin, 0);
                lp.gravity = Gravity.LEFT;
                container.setGravity(Gravity.LEFT);
                container.setLayoutParams(lp);
                message.setTextColor(Color.WHITE);
                timeStamp.setTextColor(Color.WHITE);
            }
        }
    }
}
