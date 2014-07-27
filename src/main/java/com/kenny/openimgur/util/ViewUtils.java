package com.kenny.openimgur.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpenImgurApp;

/**
 * Created by kcampagna on 7/27/14.
 */
public class ViewUtils {

    /**
     * Returns the height of the actionbar and status bar (4.4+) needed for the translucent style
     *
     * @param context
     * @return
     */
    public static int getHeightForTranslucentStyle(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int height = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();

        // On 4.4 + devices, we need to account for the status bar
        if (OpenImgurApp.SDK_VERSION >= Build.VERSION_CODES.KITKAT) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                height += context.getResources().getDimensionPixelSize(resourceId);
            }
        }

        return height;
    }

    /**
     * Returns an empty view to occupy the space present in the translucent style
     *
     * @param context
     * @return
     */
    public static View getHeaderViewForTranslucentStyle(Context context) {
        View v = View.inflate(context, R.layout.empty_header, null);
        int height = getHeightForTranslucentStyle(context);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        v.setLayoutParams(lp);
        return v;
    }
}
