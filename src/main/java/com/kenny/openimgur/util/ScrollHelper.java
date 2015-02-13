package com.kenny.openimgur.util;

import android.widget.AbsListView;

/**
 * Created by Kenny-PC on 1/24/2015.
 */
public class ScrollHelper {
    public final static int DIRECTION_UP = 0;
    public final static int DIRECTION_DOWN = 1;
    public final static int DIRECTION_NOT_CHANGED = -1;
    private final static int SCROLL_THRESHOLD = 10;

    private int mLastScrollY = 0;
    private int mPreviousFirstItem = 0;

    public ScrollHelper() {
        // NOOP
    }

    /**
     * Returns the direction the list has scroll
     *
     * @param view             The list
     * @param firstVisibleItem First visible item in the list
     * @param totalItemCount   The total number of items in the list
     * @return
     */
    public int getScrollDirection(AbsListView view, int firstVisibleItem, int totalItemCount) {
        if (totalItemCount <= 0) return DIRECTION_NOT_CHANGED;
        int direction = DIRECTION_NOT_CHANGED;

        // Same item is still in first spot, check for pixel change
        if (firstVisibleItem == mPreviousFirstItem) {
            int newScrollY = getListScrollY(view);
            boolean didChange = Math.abs(mLastScrollY - newScrollY) > SCROLL_THRESHOLD;

            if (didChange) {
                direction = mLastScrollY > newScrollY ? DIRECTION_DOWN : DIRECTION_UP;
                mLastScrollY = newScrollY;
            }
        } else {
            direction = firstVisibleItem > mPreviousFirstItem ? DIRECTION_DOWN : DIRECTION_UP;
            mLastScrollY = getListScrollY(view);
            mPreviousFirstItem = firstVisibleItem;
        }

        return direction;
    }

    /**
     * Gets the top position of the first item in the list view
     *
     * @param view
     * @return
     */
    private int getListScrollY(AbsListView view) {
        if (view == null || view.getChildAt(0) == null) return 0;
        return view.getChildAt(0).getTop();
    }
}
