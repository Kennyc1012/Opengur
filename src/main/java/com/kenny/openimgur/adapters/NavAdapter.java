package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

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
     */

    private String[] mTitles;

    private LayoutInflater mInflater;

    private int mSelectedPosition;

    public NavAdapter(Context context, int titles) {
        mInflater = LayoutInflater.from(context);
        mTitles = context.getResources().getStringArray(titles);
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
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.nav_item, parent, false);
        }

        // We will tint the drawables so we don't need to import another set
        Resources res = convertView.getResources();
        Drawable draw = getDrawable(position, res);
        switch (position) {
            case 0:
                ((TextViewRoboto) convertView).setCompoundDrawablesWithIntrinsicBounds(draw, null, null, null);
                break;

            case 1:
                ((TextViewRoboto) convertView).setCompoundDrawablesWithIntrinsicBounds(draw, null, null, null);
                break;

            case 2:
                ((TextViewRoboto) convertView).setCompoundDrawablesWithIntrinsicBounds(draw, null, null, null);
                break;
        }

        ((TextViewRoboto) convertView).setText(getItem(position));
        ((TextViewRoboto) convertView).setTextColor(mSelectedPosition == position ?
                res.getColor(R.color.accent_color_red_200) : res.getColor(R.color.abc_primary_text_material_light));

        return convertView;
    }

    /**
     * Sets which position is currently selected in the list
     *
     * @param position
     */
    public void setSelectedPosition(int position) {
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
                draw = res.getDrawable(R.drawable.ic_action_user).mutate();
                break;
        }

        if (draw != null) {
            int color = mSelectedPosition == position ? res.getColor(R.color.accent_color_red_200) : res.getColor(R.color.abc_primary_text_material_light);
            draw.setColorFilter(color, PorterDuff.Mode.MULTIPLY);
        }

        return draw;
    }
}
