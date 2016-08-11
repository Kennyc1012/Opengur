package com.kenny.openimgur.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.GridItemDecoration;
import com.kennyc.view.MultiStateView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;

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
     * @param context      App context
     * @param recyclerView The {@link RecyclerView} to setup
     */
    public static void setRecyclerViewGridDefaults(@NonNull Context context, @NonNull RecyclerView recyclerView) {
        Resources res = context.getResources();
        int numColumns = res.getInteger(R.integer.gallery_num_columns);
        int gridSpacing = res.getDimensionPixelSize(R.dimen.grid_padding);
        setRecyclerViewGridDefaults(context, recyclerView, numColumns, gridSpacing);
    }

    /**
     * Sets up a {@link RecyclerView} for a Grid style
     *
     * @param context      App context
     * @param recyclerView The {@link RecyclerView} to setup
     * @param numColumns   Number of columns the {@link RecyclerView} grid has
     * @param gridSpacing  The spacing between the grid items
     */
    public static void setRecyclerViewGridDefaults(@NonNull Context context, @NonNull RecyclerView recyclerView, int numColumns, int gridSpacing) {
        recyclerView.setLayoutManager(new GridLayoutManager(context, numColumns));
        recyclerView.addItemDecoration(new GridItemDecoration(gridSpacing, numColumns));
    }

    /**
     * Attempts to clear the decor view from the transition manager which causes a leak.
     * <p/><a href="http://stackoverflow.com/questions/32698049/sharedelement-and-custom-entertransition-causes-memory-leak">StackOverflow
     * Explanation</a>
     *
     * @param activity
     */
    public static void fixTransitionLeak(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return;

        Class transitionManagerClass = TransitionManager.class;
        try {
            Field runningTransitionsField = transitionManagerClass.getDeclaredField("sRunningTransitions");
            runningTransitionsField.setAccessible(true);
            //noinspection unchecked
            ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>> runningTransitions
                    = (ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>>)
                    runningTransitionsField.get(transitionManagerClass);
            if (runningTransitions.get() == null || runningTransitions.get().get() == null) {
                return;
            }
            ArrayMap map = runningTransitions.get().get();
            View decorView = activity.getWindow().getDecorView();
            if (map.containsKey(decorView)) {
                map.remove(decorView);
            }
        } catch (NoSuchFieldException e) {
            // Nothing
        } catch (IllegalAccessException e) {
            // Nothing
        }
    }
}
