package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurConvo;

import java.util.List;

import butterknife.InjectView;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ConvoAdapter extends ImgurBaseAdapter<ImgurConvo> {
    private int mCircleSize;

    public ConvoAdapter(Context context, List<ImgurConvo> convos) {
        super(context, convos);
        mCircleSize = context.getResources().getDimensionPixelSize(R.dimen.avatar_size);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ConvoViewHolder holder;
        ImgurConvo convo = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.profile_comment_item, parent, false);
            holder = new ConvoViewHolder(convertView);
        } else {
            holder = (ConvoViewHolder) convertView.getTag();
        }

        String firstLetter = convo.getWithAccount().substring(0, 1);
        int color = ColorGenerator.DEFAULT.getColor(convo.getWithAccount());

        holder.image.setImageDrawable(
                TextDrawable.builder()
                        .beginConfig()
                        .toUpperCase()
                        .width(mCircleSize)
                        .height(mCircleSize)
                        .endConfig()
                        .buildRound(firstLetter, color));

        holder.author.setText(convo.getWithAccount());
        holder.message.setText(convo.getLastMessage());
        return convertView;
    }

    public void removeItem(String id) {
        List<ImgurConvo> items = retainItems();

        for (ImgurConvo c : items) {
            if (c.getId().equals(id)) {
                removeItem(c);
                break;
            }
        }
    }

    static class ConvoViewHolder extends ImgurViewHolder {
        @InjectView(R.id.author)
        TextView author;

        @InjectView(R.id.comment)
        TextView message;

        @InjectView(R.id.image)
        ImageView image;

        public ConvoViewHolder(View view) {
            super(view);
        }
    }
}
