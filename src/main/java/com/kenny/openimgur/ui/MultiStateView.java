package com.kenny.openimgur.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;

/**
 * View that handles different states. Idea from https://github.com/jenzz/Android-MultiStateListView
 * Created by kcampagna on 7/6/14.
 */
public class MultiStateView extends FrameLayout {
    private static final int UNKNOWN_VIEW = -1;

    private static final int CONTENT_VIEW = 0;

    private static final int ERROR_VIEW = 1;

    private static final int EMPTY_VIEW = 2;

    private static final int LOADING_VIEW = 3;

    public enum ViewState {
        CONTENT,
        LOADING,
        EMPTY,
        ERROR
    }

    private LayoutInflater mInflater;

    private View mContentView;

    private View mLoadingView;

    private View mErrorView;

    private View mEmptyView;

    private ViewState mViewState;

    public MultiStateView(Context context) {
        this(context, null);
    }

    public MultiStateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mInflater = LayoutInflater.from(getContext());
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MultiStateView);

        int loadingViewResId = a.getResourceId(R.styleable.MultiStateView_loadingView, -1);
        if (loadingViewResId > -1) {
            mLoadingView = mInflater.inflate(loadingViewResId, this, false);
            addView(mLoadingView, mLoadingView.getLayoutParams());
        }

        int emptyViewResId = a.getResourceId(R.styleable.MultiStateView_emptyView, -1);
        if (emptyViewResId > -1) {
            mEmptyView = mInflater.inflate(emptyViewResId, this, false);
            addView(mEmptyView, mEmptyView.getLayoutParams());
        }

        int errorViewResId = a.getResourceId(R.styleable.MultiStateView_errorView, -1);
        if (errorViewResId > -1) {
            mErrorView = mInflater.inflate(errorViewResId, this, false);
            addView(mErrorView, mErrorView.getLayoutParams());
        }

        int viewState = a.getInt(R.styleable.MultiStateView_viewState, UNKNOWN_VIEW);
        if (viewState != UNKNOWN_VIEW) {
            switch (viewState) {
                case CONTENT_VIEW:
                    mViewState = ViewState.CONTENT;
                    break;

                case ERROR_VIEW:
                    mViewState = ViewState.EMPTY;
                    break;

                case EMPTY_VIEW:
                    mViewState = ViewState.EMPTY;
                    break;

                case LOADING_VIEW:
                    mViewState = ViewState.LOADING;
                    break;
            }
        }

        a.recycle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mContentView == null) throw new IllegalArgumentException("Content view is not defined");
        setView();
    }

    @Override
    public void addView(View child) {
        if (isValidContentView(child)) mContentView = child;
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (isValidContentView(child)) mContentView = child;
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (isValidContentView(child)) mContentView = child;
        super.addView(child, index, params);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (isValidContentView(child)) mContentView = child;
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (isValidContentView(child)) mContentView = child;
        super.addView(child, width, height);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params) {
        if (isValidContentView(child)) mContentView = child;
        return super.addViewInLayout(child, index, params);
    }

    @Override
    protected boolean addViewInLayout(View child, int index, ViewGroup.LayoutParams params, boolean preventRequestLayout) {
        if (isValidContentView(child)) mContentView = child;
        return super.addViewInLayout(child, index, params, preventRequestLayout);
    }

    /**
     * Sets the text for the Error View
     *
     * @param textViewId   TextView id in the ErrorView
     * @param errorMessage String resource of the error message
     */
    public void setErrorText(@IdRes int textViewId, @StringRes int errorMessage) {
        if (mErrorView == null) {
            throw new NullPointerException("Error view is null");
        }

        TextView errorTextView = (TextView) mErrorView.findViewById(textViewId);
        if (errorTextView != null) errorTextView.setText(errorMessage);
    }

    /**
     * Sets the text for the Error View
     *
     * @param textViewId   TextView id in the ErrorView
     * @param errorMessage String  of the error message
     */
    public void setErrorText(@IdRes int textViewId, String errorMessage) {
        if (mErrorView == null) {
            throw new NullPointerException("Error view is null");
        }

        TextView errorTextView = (TextView) mErrorView.findViewById(textViewId);
        if (errorTextView != null) errorTextView.setText(errorMessage);
    }

    /**
     * Sets the text for the empty view
     *
     * @param textViewId   TextView id in the Empty View
     * @param errorMessage The error message
     */
    public void setEmptyText(@IdRes int textViewId, String errorMessage) {
        if (mEmptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        TextView emptyTextView = (TextView) mEmptyView.findViewById(textViewId);
        if (emptyTextView != null) emptyTextView.setText(errorMessage);
    }

    /**
     * Returns the view associated with the view state
     *
     * @param state
     * @return
     */
    public View getView(ViewState state) {
        switch (state) {
            case LOADING:
                return mLoadingView;

            case CONTENT:
                return mContentView;

            case EMPTY:
                return mEmptyView;

            case ERROR:
                return mErrorView;

            default:
                return null;
        }
    }

    /**
     * Sets the onClick listener for the error views button
     *
     * @param buttonId
     * @param onClickListener
     */
    public void setErrorButtonClickListener(@IdRes int buttonId, OnClickListener onClickListener) {
        if (mErrorView == null) {
            throw new NullPointerException("Error View is null");
        }

        View button = mErrorView.findViewById(buttonId);

        if (button != null) button.setOnClickListener(onClickListener);
    }

    /**
     * Sets the text of the error view's button
     *
     * @param buttonId
     * @param stringId
     */
    public void setErrorButtonText(@IdRes int buttonId, @StringRes int stringId) {
        if (mErrorView == null) {
            throw new NullPointerException("Error View is null");
        }

        Button view = (Button) mErrorView.findViewById(buttonId);

        if (view != null) view.setText(stringId);
    }

    /**
     * Sets the drawable for the empty state
     *
     * @param emptyView
     * @param drawable
     */
    public void setEmptyDrawable(@IdRes int emptyView, Drawable drawable) {
        if (mEmptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        View view = mEmptyView.findViewById(emptyView);

        if (view instanceof TextView) {
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageDrawable(drawable);
        }
    }

    /**
     * Sets the drawable for the error view
     *
     * @param errorView
     * @param drawable
     */
    public void setErrorDrawable(@IdRes int errorView, Drawable drawable) {
        if (mErrorView == null) {
            throw new NullPointerException("Empty view is null");
        }

        View view = mErrorView.findViewById(errorView);

        if (view instanceof TextView) {
            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null);
        } else if (view instanceof ImageView) {
            ((ImageView) view).setImageDrawable(drawable);
        }
    }

    /**
     * Gets the current view state
     *
     * @return
     */
    public ViewState getViewState() {
        return mViewState;
    }

    /**
     * Sets the current view state
     *
     * @param state
     */
    public void setViewState(ViewState state) {
        if (state != mViewState) {
            mViewState = state;
            setView();
        }
    }

    /**
     * Shows the view based on the View State
     */
    private void setView() {
        switch (mViewState) {
            case CONTENT:
                if (mContentView == null) {
                    throw new NullPointerException("Content View is null");
                }

                mContentView.setVisibility(View.VISIBLE);

                if (mLoadingView != null) {
                    mLoadingView.setVisibility(View.GONE);
                }

                if (mErrorView != null) {
                    mErrorView.setVisibility(View.GONE);
                }

                if (mEmptyView != null) {
                    mEmptyView.setVisibility(View.GONE);
                }

                break;

            case LOADING:
                if (mLoadingView == null) {
                    throw new NullPointerException("Loading View is null");
                }

                mLoadingView.setVisibility(View.VISIBLE);

                if (mContentView != null) {
                    mContentView.setVisibility(View.GONE);
                }

                if (mErrorView != null) {
                    mErrorView.setVisibility(View.GONE);
                }

                if (mEmptyView != null) {
                    mEmptyView.setVisibility(View.GONE);
                }

                break;

            case EMPTY:
                if (mEmptyView == null) {
                    throw new NullPointerException("Empty View is null");
                }

                mEmptyView.setVisibility(View.VISIBLE);

                if (mLoadingView != null) {
                    mLoadingView.setVisibility(View.GONE);
                }

                if (mErrorView != null) {
                    mErrorView.setVisibility(View.GONE);
                }

                if (mContentView != null) {
                    mContentView.setVisibility(View.GONE);
                }

                break;

            case ERROR:
                if (mErrorView == null) {
                    throw new NullPointerException("Error View is null");
                }

                mErrorView.setVisibility(View.VISIBLE);

                if (mLoadingView != null) {
                    mLoadingView.setVisibility(View.GONE);
                }

                if (mContentView != null) {
                    mContentView.setVisibility(View.GONE);
                }

                if (mEmptyView != null) {
                    mEmptyView.setVisibility(View.GONE);
                }

                break;
        }
    }

    /**
     * Checks if the given view is valid for the Content View
     *
     * @param view
     * @return
     */
    private boolean isValidContentView(View view) {
        if (mContentView != null && mContentView != view) {
            return false;
        }

        return view != mLoadingView && view != mErrorView && view != mEmptyView;
    }
}