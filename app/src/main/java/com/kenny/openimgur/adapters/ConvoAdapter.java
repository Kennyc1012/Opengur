package com.kenny.openimgur.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.util.ColorUtils;

import java.util.List;

import butterknife.BindView;

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
        mCircleSize = mResources.getDimensionPixelSize(R.dimen.avatar_size);
        mClickListener = clickListener;
        mLongClickListener = longClickListener;
        mDividerColor = mIsDarkTheme ? getColor(R.color.primary_dark_light) : Color.BLACK;
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

        String firstLetter = convo.getWithAccount().substring(0, 1).toUpperCase();
        int color = ColorUtils.getColor(convo.getWithAccount());

        convoViewHolder.image.setImageDrawable(
                new TextDrawable.Builder()
                        .setWidth(mCircleSize)
                        .setHeight(mCircleSize)
                        .setText(firstLetter)
                        .setColor(color)
                        .setShape(TextDrawable.DRAWABLE_SHAPE_OVAL)
                        .build());

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
        @BindView(R.id.author)
        TextView author;

        @BindView(R.id.comment)
        TextView message;

        @BindView(R.id.image)
        ImageView image;

        @BindView(R.id.divider)
        View divider;

        public ConvoViewHolder(View view) {
            super(view);
        }
    }
}
