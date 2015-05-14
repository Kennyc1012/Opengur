package com.kenny.openimgur.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurMessage;
import com.kenny.openimgur.classes.OpengurApp;

import java.util.List;

import butterknife.InjectView;

/**
 * Created by kcampagna on 12/25/14.
 */
public class MessagesAdapter extends ImgurBaseAdapter<ImgurMessage> {
    private int mMargin;

    private int mUserId;

    public MessagesAdapter(Context context, List<ImgurMessage> messages) {
        super(context, messages);
        mUserId = OpengurApp.getInstance(context).getUser().getId();
        mMargin = (int) (context.getResources().getDisplayMetrics().widthPixels * .25);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MessagesViewHolder holder;
        ImgurMessage message = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.convo_message, parent, false);
            holder = new MessagesViewHolder(convertView);
        } else {
            holder = (MessagesViewHolder) convertView.getTag();
        }

        holder.configView(message, mMargin, mUserId);
        holder.message.setText(message.getBody());

        if (message.isSending()) {
            holder.timeStamp.setText(R.string.convo_message_sending);
        } else if (message.getDate() > 0) {
            holder.timeStamp.setText(getDateFormattedTime(message.getDate() * 1000, convertView.getContext()));
        } else {
            holder.timeStamp.setText(R.string.convo_message_failed);
        }

        return convertView;
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
        for (int i = getCount() - 1; i >= 0; i--) {
            ImgurMessage message = getItem(i);

            if (message.getId().equals(id)) {
                message.setIsSending(false);
                if (!successful) message.setDate(-1L);
                notifyDataSetChanged();
                break;
            }
        }
    }

    static class MessagesViewHolder extends ImgurViewHolder {
        @InjectView(R.id.messageContainer)
        LinearLayout container;

        @InjectView(R.id.message)
        TextView message;

        @InjectView(R.id.timeStamp)
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
