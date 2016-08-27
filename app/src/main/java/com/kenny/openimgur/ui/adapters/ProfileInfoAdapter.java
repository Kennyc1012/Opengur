package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurTrophy;
import com.kenny.openimgur.classes.ImgurUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;

/**
 * Created by kcampagna on 1/21/16.
 */
public class ProfileInfoAdapter extends BaseRecyclerAdapter<ImgurTrophy> {
    private static final int VIEW_TYPE_HEADER = 0;

    private static final int VIEW_TYPE_TROPHY = 1;

    private int mDividerColor;

    private ImgurUser mUser;

    ImgurListener mListener;

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());

    public ProfileInfoAdapter(Context context, @Nullable List<ImgurTrophy> trophies, @NonNull ImgurUser user, ImgurListener listener) {
        super(context, trophies, true);
        mUser = user;
        mListener = listener;
        mDividerColor = isDarkTheme ? getColor(R.color.primary_dark_light) : Color.BLACK;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_TROPHY;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder holder;

        switch (viewType) {
            case VIEW_TYPE_HEADER:
                holder = new InfoHolder(inflateView(R.layout.profile_info_header, parent));
                break;

            case VIEW_TYPE_TROPHY:
            default:
                holder = new TrophyHolder(inflateView(R.layout.trophy_item, parent));
                ((TrophyHolder) holder).divider.setBackgroundColor(mDividerColor);
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mListener != null) mListener.onPhotoTap(v);
                    }
                });
                break;
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        if (holder instanceof InfoHolder) {
            InfoHolder infoHolder = (InfoHolder) holder;
            String dateFormat = mDateFormat.format(new Date(mUser.getCreated()));
            infoHolder.notoriety.setText(mUser.getNotoriety().getStringId());
            infoHolder.notoriety.setTextColor(getColor(mUser.getNotoriety().getNotorietyColor()));
            infoHolder.rep.setText(getResources().getString(R.string.profile_rep, mUser.getReputation()));
            infoHolder.date.setText(getResources().getString(R.string.profile_date, dateFormat));

            if (!TextUtils.isEmpty(mUser.getBio())) {
                infoHolder.bio.setText(mUser.getBio());

                if (mListener != null) {
                    infoHolder.bio.setMovementMethod(CustomLinkMovement.getInstance(mListener));
                    Linkify.addLinks(infoHolder.bio, Linkify.WEB_URLS);
                }
            } else {
                infoHolder.bio.setText(getResources().getString(R.string.profile_bio_empty, mUser.getUsername()));
            }
        } else if (holder instanceof TrophyHolder) {
            TrophyHolder trophyHolder = (TrophyHolder) holder;
            ImgurTrophy trophy = getItem(position - 1);
            String date = mDateFormat.format(new Date(trophy.getDate() * DateUtils.SECOND_IN_MILLIS));
            trophyHolder.trophyName.setText(String.format("%s - %s", trophy.getName(), date));
            trophyHolder.trophyDesc.setText(trophy.getDescription());
            displayImage(trophyHolder.trophyImage, trophy.getTrophyImagePath());
        }
    }

    @Override
    public int getItemCount() {
        // Pad for the header
        return super.getItemCount() + 1;
    }

    @Override
    public void onDestroy() {
        mListener = null;
        super.onDestroy();
    }

    static class InfoHolder extends BaseViewHolder {
        @BindView(R.id.notoriety)
        TextView notoriety;

        @BindView(R.id.rep)
        TextView rep;

        @BindView(R.id.bio)
        TextView bio;

        @BindView(R.id.date)
        TextView date;

        public InfoHolder(View v) {
            super(v);
        }
    }

    static class TrophyHolder extends BaseViewHolder {
        @BindView(R.id.image)
        ImageView trophyImage;

        @BindView(R.id.name)
        TextView trophyName;

        @BindView(R.id.desc)
        TextView trophyDesc;

        @BindView(R.id.divider)
        View divider;

        public TrophyHolder(View v) {
            super(v);
        }
    }
}
