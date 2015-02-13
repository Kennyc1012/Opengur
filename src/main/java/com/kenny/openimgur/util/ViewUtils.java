package com.kenny.openimgur.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.TextViewRoboto;

import java.text.SimpleDateFormat;
import java.util.Date;

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
        return getActionBarHeight(context) + getStatusBarHeight(context);
    }

    /**
     * Returns the height of the actionbar
     *
     * @param context
     * @return
     */
    public static int getActionBarHeight(Context context) {
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[]{android.support.v7.appcompat.R.attr.actionBarSize});

        int abHeight = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return abHeight;
    }

    /**
     * Returns an empty view to occupy the space present in the translucent style
     *
     * @param context
     * @param additionalHeight Additional height to be added to the view
     * @return
     */
    public static View getHeaderViewForTranslucentStyle(Context context, int additionalHeight) {
        View v = View.inflate(context, R.layout.empty_header, null);
        int height = getHeightForTranslucentStyle(context);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height + additionalHeight);
        v.setLayoutParams(lp);
        return v;
    }

    /**
     * Returns the height of the navigation bar
     *
     * @param context
     * @return
     */
    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");

        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }

        return 0;
    }

    /**
     * Returns the height of the status bar
     *
     * @param context
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        int height = 0;

        // On 4.4 + devices, we need to account for the status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                height = context.getResources().getDimensionPixelSize(resourceId);
            }
        }

        return height;
    }

    /**
     * Returns if a SwipeRefreshLayout should be able to refresh given a ListView
     *
     * @param view
     * @return
     */
    public static boolean canRefreshInListView(AbsListView view) {
        boolean canRefresh = false;

        if (view != null && view.getChildCount() > 0) {
            boolean firstItemVisible = view.getFirstVisiblePosition() == 0;
            boolean topOfFirstItemVisible = view.getChildAt(0).getTop() == 0;
            canRefresh = firstItemVisible && topOfFirstItemVisible;
        }

        return canRefresh;
    }

    /**
     * Returns an empty view to occupy the space of the navigation bar for a translucent style for comments and messages fragment
     *
     * @param context
     * @return
     */
    public static View getFooterViewForComments(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.profile_comment_item, null);
        int height = getNavigationBarHeight(context);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        v.setLayoutParams(lp);
        return v;
    }
}
