package com.kenny.openimgur.classes;

import android.text.Layout;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/**
 * Class that handles clicking of links in a text View
 */
public class CustomLinkMovement extends LinkMovementMethod {

    private static CustomLinkMovement sInstance;

    private Set<ImgurListener> mListeners = new HashSet<>();

    public static CustomLinkMovement getInstance(ImgurListener listener) {
        if (sInstance == null) {
            sInstance = new CustomLinkMovement();
        }

        sInstance.mListeners.add(listener);
        return sInstance;
    }

    public static CustomLinkMovement getInstance() {
        if (sInstance == null) {
            sInstance = new CustomLinkMovement();
        }

        return sInstance;
    }

    public void addListener(ImgurListener listener) {
        mListeners.add(listener);
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
                for (ImgurListener l : mListeners) {
                    if (l != null) {
                        l.onLinkTap(widget, ((URLSpan) link[0]).getURL());
                    }
                }

                return true;
            }

            for (ImgurListener l : mListeners) {
                if (l != null) {
                    l.onLinkTap(widget, null);
                }
            }
        }

        return super.onTouchEvent(widget, buffer, event);
    }

    /**
     * Removes the ImgurListener from the set
     *
     * @param listener
     */
    public void removeListener(ImgurListener listener) {
        mListeners.remove(listener);
    }
}