package com.kenny.openimgur.classes;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Class that handles clicking of links in a text View
 */
public class CustomLinkMovementMethod extends LinkMovementMethod {

    private static CustomLinkMovementMethod mInstance;

    private ImgurListener mListener;

    public static MovementMethod getInstance(ImgurListener listener) {
        if (mInstance == null) {
            mInstance = new CustomLinkMovementMethod();
        }

        mInstance.mListener = listener;
        return mInstance;
    }

    @Override
    public boolean onTouchEvent(final TextView widget, final Spannable buffer, final MotionEvent event) {
        final int action = event.getAction();
        if (action == MotionEvent.ACTION_UP) {
            final int x = (int) event.getX() - widget.getTotalPaddingLeft() + widget.getScrollX();
            final int y = (int) event.getY() - widget.getTotalPaddingTop() + widget.getScrollY();
            final Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            final int off = layout.getOffsetForHorizontal(line, x);
            final ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
            if (link.length != 0 && link[0] instanceof URLSpan) {
                if (mListener != null) {
                    mListener.onLinkTap(widget, ((URLSpan) link[0]).getURL());
                }
                return false;
            }
        }
        return true;
    }
}