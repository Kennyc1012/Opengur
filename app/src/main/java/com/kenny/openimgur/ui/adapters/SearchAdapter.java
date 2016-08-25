package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.DBContracts;

/**
 * Created by kcampagna on 3/2/15.
 */
public class SearchAdapter extends SimpleCursorAdapter {

    private int mColor;

    public SearchAdapter(Context context, Cursor cursor, String columnName) {
        super(context,
                android.R.layout.simple_dropdown_item_1line,
                cursor,
                new String[]{columnName},
                new int[]{android.R.id.text1},
                FLAG_REGISTER_CONTENT_OBSERVER);

        Resources res = context.getResources();
        boolean isDark = OpengurApp.getInstance(context).getImgurTheme().isDarkTheme;
        mColor = isDark ? res.getColor(R.color.bg_dark) : res.getColor(R.color.bg_light);

    }

    public String getTitle(int position) {
        Object item = getItem(position);

        if (item instanceof Cursor) {
            return ((Cursor) item).getString(DBContracts.SubRedditContract.COLUMN_INDEX_NAME);
        }

        return null;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View label = super.getView(position, convertView, parent);
        label.setBackgroundColor(mColor);
        return label;
    }
}
