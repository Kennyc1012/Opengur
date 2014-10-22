package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.TextViewRoboto;

/**
 * Created by kcampagna on 10/19/14.
 */
public class NavAdapter extends BaseAdapter {

    /*
        0. Gallery
        1. Subreddit
        2. Profile
        3. Divider (No Text)
        4. Settings
        5. Feedback
     */

    private static final int VIEW_TYPE_PRIMARY = 0;

    private static final int VIEW_TYPE_DIVIDER = 1;

    private static final int VIEW_TYPE_SECONDARY = 2;

    private String[] mTitles;

    private LayoutInflater mInflater;

    private int mSelectedPosition;

    public NavAdapter(Context context, String[] titles) {
        mInflater = LayoutInflater.from(context);
        mTitles = titles;
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
            case VIEW_TYPE_PRIMARY:
                return renderPrimary(position, convertView, parent);

            case VIEW_TYPE_DIVIDER:
                return mInflater.inflate(R.layout.nav_divider, parent, false);

            case VIEW_TYPE_SECONDARY:
            default:
                return renderSecondary(position, parent);
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

        // We will tint the drawables so we don't need to import another set
        Resources res = convertView.getResources();
        holder.icon.setImageDrawable(getDrawable(position, res));
        holder.title.setText(getItem(position));
        holder.title.setTextColor(mSelectedPosition == position ?
                res.getColor(R.color.accent_color_red_200) : res.getColor(R.color.abc_primary_text_material_light));

        return convertView;
    }

    private View renderSecondary(int position, ViewGroup parent) {
        TextViewRoboto title = (TextViewRoboto) mInflater.inflate(R.layout.nav_item_secondary, parent, false);
        title.setText(getItem(position));
        return title;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        if (position <= 2) {
            return VIEW_TYPE_PRIMARY;
        } else if (position < 4) {
            return VIEW_TYPE_DIVIDER;
        }

        return VIEW_TYPE_SECONDARY;
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
        Drawable draw = null;

        switch (position) {
            case 0:
                draw = res.getDrawable(R.drawable.ic_action_gallery).mutate();
                break;

            case 1:
                draw = res.getDrawable(R.drawable.ic_action_reddit).mutate();
                break;

            case 2:
                draw = res.getDrawable(R.drawable.ic_account).mutate();
                break;
        }

        if (draw != null) {
            int color = mSelectedPosition == position ? res.getColor(R.color.accent_color_red_200) : res.getColor(R.color.abc_primary_text_material_light);
            draw.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        return draw;
    }

    /**
     * Updates the title for the profile nav item
     *
     * @param username The username if the user is logged in
     * @param title    The default title for the item
     */
    public void onUsernameChange(String username, String title) {
        mTitles[2] = TextUtils.isEmpty(username) ? title : username;
        notifyDataSetChanged();
    }

    static class NavHolder {
        TextViewRoboto title;

        ImageView icon;
    }
}
