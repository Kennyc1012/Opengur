package com.kenny.openimgur.ui;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * View that handles different states. Idea from https://github.com/jenzz/Android-MultiStateListView
 * Created by kcampagna on 7/6/14.
 */
public class MultiStateView extends com.kennyc.view.MultiStateView {

    public MultiStateView(Context context) {
        super(context);
    }

    public MultiStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the text for the Error View
     *
     * @param textViewId   TextView id in the ErrorView
     * @param errorMessage String resource of the error message
     */
    public void setErrorText(@IdRes int textViewId, @StringRes int errorMessage) {
        View errorView = getView(MultiStateView.VIEW_STATE_ERROR);

        if (errorView == null) {
            throw new NullPointerException("Error view is null");
        }

        TextView errorTextView = (TextView) errorView.findViewById(textViewId);
        if (errorTextView != null) errorTextView.setText(errorMessage);
    }

    /**
     * Sets the text for the Error View
     *
     * @param textViewId   TextView id in the ErrorView
     * @param errorMessage String  of the error message
     */
    public void setErrorText(@IdRes int textViewId, String errorMessage) {
        View errorView = getView(MultiStateView.VIEW_STATE_ERROR);

        if (errorView == null) {
            throw new NullPointerException("Error view is null");
        }

        TextView errorTextView = (TextView) errorView.findViewById(textViewId);
        if (errorTextView != null) errorTextView.setText(errorMessage);
    }

    /**
     * Sets the text for the empty view
     *
     * @param textViewId   TextView id in the Empty View
     * @param emptyMessage The empty message
     */
    public void setEmptyText(@IdRes int textViewId, String emptyMessage) {
        View emptyView = getView(VIEW_STATE_EMPTY);

        if (emptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        TextView emptyTextView = (TextView) emptyView.findViewById(textViewId);
        if (emptyTextView != null) emptyTextView.setText(emptyMessage);
    }

    /**
     * Sets the text for the empty view
     *
     * @param textViewId   TextView id in the Empty View
     * @param emptyMessage The empty message
     */
    public void setEmptyText(@IdRes int textViewId, @StringRes int emptyMessage) {
        View emptyView = getView(VIEW_STATE_EMPTY);

        if (emptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        TextView emptyTextView = (TextView) emptyView.findViewById(textViewId);
        if (emptyTextView != null) emptyTextView.setText(emptyMessage);
    }
}