package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.ImageUtil;

import butterknife.InjectView;

/**
 * Created by kcampagna on 10/19/14.
 */
public class NavAdapter extends BaseAdapter {
    public static final int PAGE_PROFILE = 0;

    public static final int PAGE_GALLERY = 1;

    public static final int PAGE_TOPICS = 2;

    public static final int PAGE_MEME = 3;

    public static final int PAGE_SUBREDDIT = 4;

    public static final int PAGE_RANDOM = 5;

    public static final int PAGE_UPLOADS = 6;

    public static final int PAGE_DIVIDER = 7;

    public static final int PAGE_SETTINGS = 8;

    public static final int PAGE_FEEDBACK = 9;

    private static final int VIEW_TYPE_PRIMARY = 0;

    private static final int VIEW_TYPE_DIVIDER = 1;

    private static final int VIEW_TYPE_PROFILE = 2;

    private String[] mTitles;

    private LayoutInflater mInflater;

    private int mSelectedPosition;

    private ImgurUser mUser;

    private int mSelectedColor;

    private int mDefaultColor;

    private int mProfileColor;

    private int mDividerColor;

    public NavAdapter(Context context, ImgurUser user) {
        ImgurTheme theme = OpengurApp.getInstance(context).getImgurTheme();
        mInflater = LayoutInflater.from(context);
        Resources res = context.getResources();
        mTitles = res.getStringArray(R.array.nav_items);
        mUser = user;
        mSelectedColor = res.getColor(theme.accentColor);
        mProfileColor = res.getColor(theme.primaryColor);
        mDefaultColor = res.getColor(theme.isDarkTheme ? R.color.primary_text_default_material_dark : R.color.primary_text_default_material_light);
        mDividerColor = res.getColor(theme.isDarkTheme ? R.color.primary_material_light : R.color.primary_material_dark);

    }

    @Override
    public int getCount() {
        return mTitles != null ? mTitles.length : 0;
    }

    @Override
    public String getItem(int position) {
        return mTitles != null ? mTitles[position] : null;
    }

    @Override
    public long getItemId(int position) {
        return Math.abs(getItem(position).hashCode());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_DIVIDER:
                View view = mInflater.inflate(R.layout.nav_divider, parent, false);
                view.setBackgroundColor(mDividerColor);
                return view;

            case VIEW_TYPE_PROFILE:
                return renderProfile(position, parent);

            case VIEW_TYPE_PRIMARY:
            default:
                return renderPrimary(position, convertView, parent);
        }
    }

    private View renderPrimary(int position, View convertView, ViewGroup parent) {
        NavHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.nav_item, parent, false);
            holder = new NavHolder(convertView);
        } else {
            holder = (NavHolder) convertView.getTag();
        }

        boolean isSelected = mSelectedPosition == position;
        holder.icon.setImageDrawable(getDrawable(position, convertView.getResources(), isSelected));
        holder.title.setText(getItem(position));
        holder.title.setTextColor(isSelected ? mSelectedColor : mDefaultColor);
        return convertView;
    }

    private View renderProfile(int position, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.nav_profile, parent, false);
        TextView name = (TextView) view.findViewById(R.id.profileName);
        name.setText(mUser != null ? mUser.getUsername() : getItem(position));
        name.setTextColor(mSelectedPosition == position ? mSelectedColor : Color.WHITE);
        TextView rep = (TextView) view.findViewById(R.id.reputation);
        rep.setText(mUser != null ? mUser.getNotoriety().getStringId() : R.string.login_msg);
        ImageView img = (ImageView) view.findViewById(R.id.profileImg);

        if (mUser != null) {
            int size = img.getResources().getDimensionPixelSize(R.dimen.avatar_size);
            String firstLetter = mUser.getUsername().substring(0, 1);

            img.setImageDrawable(TextDrawable.builder()
                    .beginConfig()
                    .toUpperCase()
                    .width(size)
                    .height(size)
                    .endConfig()
                    .buildRound(firstLetter, mSelectedColor));
        } else {
            img.setImageResource(R.drawable.ic_account_circle);
        }

        view.setBackgroundColor(mProfileColor);
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == PAGE_PROFILE) {
            return VIEW_TYPE_PROFILE;
        } else if (position == PAGE_DIVIDER) {
            return VIEW_TYPE_DIVIDER;
        }

        return VIEW_TYPE_PRIMARY;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != NavAdapter.PAGE_DIVIDER;
    }

    /**
     * Sets which position is currently selected in the list
     *
     * @param position
     */
    public void setSelectedPosition(int position) {
        if (position == PAGE_PROFILE || position >= PAGE_DIVIDER) return;

        mSelectedPosition = position;
        notifyDataSetChanged();
    }

    /**
     * Returns the drawable for the list item
     *
     * @param position   The position in the list
     * @param res        Local resources
     * @param isSelected If the position is selected
     * @return
     */
    private Drawable getDrawable(int position, Resources res, boolean isSelected) {
        int drawableId = -1;

        switch (position) {
            case PAGE_GALLERY:
                drawableId = R.drawable.ic_action_gallery;
                break;

            case PAGE_SUBREDDIT:
                drawableId = R.drawable.ic_action_reddit;
                break;

            case PAGE_RANDOM:
                drawableId = R.drawable.ic_action_shuffle;
                break;

            case PAGE_UPLOADS:
                drawableId = R.drawable.ic_action_upload;
                break;

            case PAGE_SETTINGS:
                drawableId = R.drawable.ic_action_settings;
                break;

            case PAGE_FEEDBACK:
                drawableId = R.drawable.ic_action_email;
                break;

            case PAGE_TOPICS:
                drawableId = R.drawable.ic_action_quotes;
                break;

            case PAGE_MEME:
                drawableId = R.drawable.ic_action_meme;
                break;
        }

        if (drawableId > -1) {
            return ImageUtil.tintDrawable(drawableId, res, isSelected ? mSelectedColor : mDefaultColor);
        }

        return null;
    }

    /**
     * Updates the Logged in user
     *
     * @param user The newly logged in user
     */
    public void onUserLogin(ImgurUser user) {
        mUser = user;
        notifyDataSetChanged();
    }

    static class NavHolder extends ImgurBaseAdapter.ImgurViewHolder {
        @InjectView(R.id.title)
        TextView title;

        @InjectView(R.id.icon)
        ImageView icon;

        public NavHolder(View view) {
            super(view);
        }
    }
}
