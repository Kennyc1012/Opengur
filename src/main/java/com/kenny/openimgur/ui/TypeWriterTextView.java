package com.kenny.openimgur.ui;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;

/**
 * Created by kcampagna on 8/28/14.
 */
public class TypeWriterTextView extends TextViewRoboto {
    private static final long ANIMATION_DELAY = 25L;

    private int mIndex = 0;

    private String mTextToDisplay;

    private Handler mHandler = new Handler();

    public TypeWriterTextView(Context context) {
        super(context);
    }

    public TypeWriterTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TypeWriterTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Runnable that will loop through the text and display each letter individually
     */
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            setText(mTextToDisplay.subSequence(0, mIndex++));
            if (mIndex <= mTextToDisplay.length()) {
                mHandler.postDelayed(mRunnable, ANIMATION_DELAY);
            }
        }
    };

    /**
     * Set the text and start animating
     *
     * @param resourceId
     */
    public void animateText(int resourceId) {
        mTextToDisplay = getContext().getString(resourceId);
        setText(null);
        mIndex = 0;
        mHandler.removeCallbacks(mRunnable);

        if (!TextUtils.isEmpty(mTextToDisplay)) {
            // No delat for the first letter
            mHandler.post(mRunnable);
        }
    }
}
