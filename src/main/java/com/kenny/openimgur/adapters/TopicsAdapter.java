package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTopic;

import java.util.List;

/**
 * Created by kcampagna on 2/21/15.
 */
public class TopicsAdapter extends ArrayAdapter<ImgurTopic> {

    public TopicsAdapter(Context context, List<ImgurTopic> topics) {
        super(context, R.layout.support_simple_spinner_dropdown_item, topics);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(this.getItem(position).getName());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView label = (TextView) super.getView(position, convertView, parent);
        label.setText(this.getItem(position).getName());
        return label;
    }
}
