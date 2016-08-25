package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.OpengurApp;

import java.util.List;

/**
 * Created by kcampagna on 2/21/15.
 */
public class TopicsAdapter extends ArrayAdapter<ImgurTopic> {
    private final int mColor;

    public TopicsAdapter(Context context, List<ImgurTopic> topics) {
        super(context, R.layout.support_simple_spinner_dropdown_item, topics);
        Resources res = context.getResources();
        boolean isDark = OpengurApp.getInstance(context).getImgurTheme().isDarkTheme;
        mColor = isDark ? res.getColor(R.color.bg_dark) : res.getColor(R.color.bg_light);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.topics_tb_item, parent, false);
        }

        TextView label = (TextView) convertView;
        label.setText(this.getItem(position).getName());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(this.getItem(position).getName());
        label.setBackgroundColor(mColor);
        return label;
    }
}
