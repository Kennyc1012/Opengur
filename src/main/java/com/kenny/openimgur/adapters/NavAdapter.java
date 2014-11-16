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

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.TextViewRoboto;

/**
 * Created by kcampagna on 10/19/14.
 */
public class NavAdapter extends BaseAdapter {

    /*
        0. Profile
        1. Subreddit
        2. Gallery
        3. Divider (No Text)
        4. Settings
        5. Feedback
     */

    private static final int VIEW_TYPE_PRIMARY = 0;

    private static final int VIEW_TYPE_DIVIDER = 1;

    private static final int VIEW_TYPE_PROFILE = 2;

    private String[] mTitles;

    private LayoutInflater mInflater;

    private int mSelectedPosition;

    private ImgurUser mUser;

    public NavAdapter(Context context, ImgurUser user) {
        mInflater = LayoutInflater.from(context);
        mTitles = context.getResources().getStringArray(R.array.nav_items);
        mUser = user;
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
            holder = new NavHolder();
            convertView = mInflater.inflate(R.layout.nav_item, parent, false);
            holder.icon = (ImageView) convertView.findViewById(R.id.icon);
            holder.title = (TextViewRoboto) convertView.findViewById(R.id.title);
            convertView.setTag(holder);
        } else {
            holder = (NavHolder) convertView.getTag();
        }

        Resources res = convertView.getResources();
        holder.icon.setImageDrawable(getDrawable(position, res));
        holder.title.setText(getItem(position));
        convertView.setBackgroundColor(mSelectedPosition == position ? Color.LTGRAY : Color.WHITE);
        return convertView;
    }

    private View renderProfile(int position, ViewGroup parent) {
        View view = mInflater.inflate(R.layout.nav_profile, parent, false);
        Resources res = view.getResources();
        TextViewRoboto name = (TextViewRoboto) view.findViewById(R.id.profileName);
        name.setText(mUser != null ? mUser.getUsername() : getItem(position));
        TextViewRoboto rep = (TextViewRoboto) view.findViewById(R.id.reputation);
        rep.setText(mUser != null ? mUser.getNotoriety().getStringId() : R.string.login_msg);
        rep.setTextColor(mUser != null ? res.getColor(mUser.getNotoriety().getNotorietyColor()) : Color.WHITE);
        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_PROFILE;
        } else if (position == 3) {
            return VIEW_TYPE_DIVIDER;
        }

        return VIEW_TYPE_PRIMARY;
    }

    @Override
    public boolean isEnabled(int position) {
        return position != 3;
    }

    /**
     * Sets which position is currently selected in the list
     *
     * @param position
     */
    public void setSelectedPosition(int position) {
        // A position greater than 2 will result in a new activity, so we won't need to update the selected item
        if (position > 2) return;

        mSelectedPosition = position;
        notifyDataSetChanged();
    }

    /**
     * Returns the drawable for the list item
     *
     * @param position The position in the list
     * @param res      Local resources
     * @return
     */
    private Drawable getDrawable(int position, Resources res) {
        switch (position) {
            case 1:
                return res.getDrawable(R.drawable.ic_action_gallery);

            case 2:
                return res.getDrawable(R.drawable.ic_action_reddit_nav);

            case 4:
                return res.getDrawable(R.drawable.ic_action_settings);

            case 5:
                return res.getDrawable(R.drawable.ic_action_email);
        }

        return null;
    }

    /**
     * Updates the Logged in user
     *
     * @param user The newly logged in user
     */
    public void onUsernameChange(ImgurUser user) {
        mUser = user;
        notifyDataSetChanged();
    }

    static class NavHolder {
        TextViewRoboto title;

        ImageView icon;
    }
}
