package com.kenny.openimgur.util;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.GridItemDecoration;
import com.kennyc.view.MultiStateView;

/**
 * Created by kcampagna on 7/27/14.
 */
public class ViewUtils {

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
     * Sets the text for the Error View in a {@link MultiStateView}
     *
     * @param multiStateView The {@link MultiStateView}
     * @param textViewId     TextView id in the ErrorView
     * @param errorMessage   String resource of the error message
     */
    public static void setErrorText(MultiStateView multiStateView, @IdRes int textViewId, @StringRes int errorMessage) {
        if (multiStateView == null) return;

        View errorView = multiStateView.getView(MultiStateView.VIEW_STATE_ERROR);

        if (errorView == null) {
            throw new NullPointerException("Error view is null");
        }

        TextView errorTextView = (TextView) errorView.findViewById(textViewId);
        if (errorTextView != null) errorTextView.setText(errorMessage);
    }

    /**
     * Sets the text for the Error View in a {@link MultiStateView}
     *
     * @param multiStateView The {@link MultiStateView}
     * @param textViewId     TextView id in the ErrorView
     * @param errorMessage   String  of the error message
     */
    public static void setErrorText(MultiStateView multiStateView, @IdRes int textViewId, String errorMessage) {
        if (multiStateView == null) return;

        View errorView = multiStateView.getView(MultiStateView.VIEW_STATE_ERROR);

        if (errorView == null) {
            throw new NullPointerException("Error view is null");
        }

        TextView errorTextView = (TextView) errorView.findViewById(textViewId);
        if (errorTextView != null) errorTextView.setText(errorMessage);
    }

    /**
     * Sets the text for the empty view in a {@link MultiStateView}
     *
     * @param multiStateView The {@link MultiStateView}
     * @param textViewId     TextView id in the Empty View
     * @param emptyMessage   The empty message
     */
    public static void setEmptyText(MultiStateView multiStateView, @IdRes int textViewId, String emptyMessage) {
        if (multiStateView == null) return;

        View emptyView = multiStateView.getView(MultiStateView.VIEW_STATE_EMPTY);

        if (emptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        TextView emptyTextView = (TextView) emptyView.findViewById(textViewId);
        if (emptyTextView != null) emptyTextView.setText(emptyMessage);
    }

    /**
     * Sets the text for the empty view in a {@link MultiStateView}
     *
     * @param multiStateView The {@link MultiStateView}
     * @param textViewId     TextView id in the Empty View
     * @param emptyMessage   The empty message
     */
    public static void setEmptyText(MultiStateView multiStateView, @IdRes int textViewId, @StringRes int emptyMessage) {
        if (multiStateView == null) return;

        View emptyView = multiStateView.getView(MultiStateView.VIEW_STATE_EMPTY);

        if (emptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        TextView emptyTextView = (TextView) emptyView.findViewById(textViewId);
        if (emptyTextView != null) emptyTextView.setText(emptyMessage);
    }

    /**
     * Sets up a {@link RecyclerView} for a Grid style
     *
     * @param context
     * @param recyclerView
     */
    public static void setRecyclerViewGridDefaults(@NonNull Context context, @NonNull RecyclerView recyclerView) {
        Resources res = context.getResources();
        int gridSize = res.getInteger(R.integer.gallery_num_columns);
        recyclerView.setLayoutManager(new GridLayoutManager(context, gridSize));
        recyclerView.addItemDecoration(new GridItemDecoration(res.getDimensionPixelSize(R.dimen.grid_padding), gridSize));
    }
}
