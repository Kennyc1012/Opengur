package com.kenny.openimgur.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 6/30/15.
 */
public class ViewPager extends android.support.v4.view.ViewPager {

    private boolean mIsSwipingEnabled = true;

    public ViewPager(Context context) {
        super(context);
        init(null);
    }

    public ViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ViewPager);
            mIsSwipingEnabled = a.getBoolean(R.styleable.ViewPager_swipingEnabled, true);
            a.recycle();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!mIsSwipingEnabled) return false;

        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception e) {
            // Ingore all errors
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return mIsSwipingEnabled && super.onTouchEvent(ev);
    }

    public void setSwiping(boolean enabled) {
        mIsSwipingEnabled = enabled;
    }
}
