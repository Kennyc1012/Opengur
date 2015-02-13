package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.TextViewRoboto;

import butterknife.InjectView;

/**
 * Created by kcampagna on 10/19/14.
 */
public class NavAdapter extends BaseAdapter {
    public static final int PAGE_PROFILE = 0;
    public static final int PAGE_GALLERY = 1;
    public static final int PAGE_SUBREDDIT = 2;
    public static final int PAGE_RANDOM = 3;
    public static final int PAGE_UPLOADS = 4;
    public static final int PAGE_DIVIDER = 5;
    public static final int PAGE_SETTINGS = 6;
    public static final int PAGE_FEEDBACK = 7;
    private static final int VIEW_TYPE_PRIMARY = 0;
    private static final int VIEW_TYPE_DIVIDER = 1;
    private static final int VIEW_TYPE_PROFILE = 2;

    private String[] mTitles;

    private LayoutInflater mInflater;

    private int mSelectedPosition;

    private ImgurUser mUser;

    private int mSelectedColor;

    private int mDefaultColor;

    private int mProfileColor;;

    public NavAdapter(Context context, ImgurUser user) {
        ImgurTheme theme = OpenImgurApp.getInstance(context).getImgurTheme();
        mInflater = LayoutInflater.from(context);
        Resources res = context.getResources();
        mTitles = res.getStringArray(R.array.nav_items);
        mUser = user;
        mSelectedColor = res.getColor(theme.accentColor);
        mProfileColor = res.getColor(theme.primaryColor);
        mDefaultColor = res.getColor(theme.isDarkTheme ? R.color.primary_text_default_material_dark : R.color.primary_text_default_material_light);

    }

    public void onUpdateTheme(ImgurTheme theme, Resources res) {
        mSelectedColor = res.getColor(theme.accentColor);
        mProfileColor = res.getColor(theme.primaryColor);
        notifyDataSetChanged();
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
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_DIVIDER:
                return mInflater.inflate(R.layout.nav_divider, parent, false);

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
            Resources res = convertView.getResources();
        } else {
            holder = (NavHolder) convertView.getTag();
        }

        boolean isSelected = mSelectedPosition == position;
        holder.icon.setImageDrawable(getDrawable(position, convertView.getResources(), isSelected));
        holder.title.setText(getItem(position));
        return convertView;
    }

    private View renderProfile(int position, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.nav_profile, parent, false);
        TextViewRoboto name = (TextViewRoboto) view.findViewById(R.id.profileName);
        name.setText(mUser != null ? mUser.getUsername() : getItem(position));
        name.setTextColor(mSelectedPosition == position ? mSelectedColor : Color.WHITE);
        TextViewRoboto rep = (TextViewRoboto) view.findViewById(R.id.reputation);
        rep.setText(mUser != null ? mUser.getNotoriety().getStringId() : R.string.login_msg);
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
        Drawable drawable = null;
        switch (position) {
            case PAGE_GALLERY:
                drawable = res.getDrawable(R.drawable.ic_action_gallery).mutate();
                break;

            case PAGE_SUBREDDIT:
                drawable = res.getDrawable(R.drawable.ic_action_reddit).mutate();
                break;

            case PAGE_RANDOM:
                drawable = res.getDrawable(R.drawable.ic_action_shuffle).mutate();
                break;

            case PAGE_UPLOADS:
                drawable = res.getDrawable(R.drawable.ic_action_upload).mutate();
                break;

            case PAGE_SETTINGS:
                drawable = res.getDrawable(R.drawable.ic_action_settings).mutate();
                break;

            case PAGE_FEEDBACK:
                drawable = res.getDrawable(R.drawable.ic_action_email).mutate();
                break;
        }

        if (drawable != null) {
            drawable.setColorFilter(isSelected ? mSelectedColor : mDefaultColor, PorterDuff.Mode.SRC_ATOP);
        }

        return drawable;
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
        TextViewRoboto title;
        @InjectView(R.id.icon)
        ImageView icon;

        public NavHolder(View view) {
            super(view);
        }
    }
}
