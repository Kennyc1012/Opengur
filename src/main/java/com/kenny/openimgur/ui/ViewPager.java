package com.kenny.openimgur.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by kcampagna on 6/30/15.
 */
public class ViewPager extends android.support.v4.view.ViewPager {

    public ViewPager(Context context) {
        super(context);
    }

    public ViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception e) {
            // Ingore all errors
        }

        return false;
    }
}
