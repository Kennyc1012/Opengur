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

import butterknife.Bind;

/**
 * Created by Kenny-PC on 8/1/2015.
 */
public class ConvoAdapter extends BaseRecyclerAdapter<ImgurConvo> {
    private int mCircleSize;
    private int mDividerColor;

    private View.OnClickListener mClickListener;
    private View.OnLongClickListener mLongClickListener;

    public ConvoAdapter(Context context, List<ImgurConvo> convos, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
        super(context, convos);
        mCircleSize = context.getResources().getDimensionPixelSize(R.dimen.avatar_size);
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
        mDividerColor = mIsDarkTheme ? context.getResources().getColor(R.color.primary_dark_material_light) : context.getResources().getColor(R.color.primary_dark_material_dark);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.profile_comment_item, parent, false);
        view.setOnLongClickListener(mLongClickListener);
        view.setOnClickListener(mClickListener);
        ConvoViewHolder holder = new ConvoViewHolder(view);
        holder.divider.setBackgroundColor(mDividerColor);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        ConvoViewHolder convoViewHolder = (ConvoViewHolder) holder;
        ImgurConvo convo = getItem(position);

        String firstLetter = convo.getWithAccount().substring(0, 1);
        int color = ColorGenerator.DEFAULT.getColor(convo.getWithAccount());

        convoViewHolder.image.setImageDrawable(
                TextDrawable.builder()
                        .beginConfig()
                        .toUpperCase()
                        .width(mCircleSize)
                        .height(mCircleSize)
                        .endConfig()
                        .buildRound(firstLetter, color));

        convoViewHolder.author.setText(convo.getWithAccount());
        convoViewHolder.message.setText(convo.getLastMessage());
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

    static class ConvoViewHolder extends BaseViewHolder {
        @Bind(R.id.author)
        TextView author;

        @Bind(R.id.comment)
        TextView message;

        @Bind(R.id.image)
        ImageView image;

        @Bind(R.id.divider)
        View divider;

        public ConvoViewHolder(View view) {
            super(view);
        }
    }
}
