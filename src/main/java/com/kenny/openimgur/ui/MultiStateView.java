package com.kenny.openimgur.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kenny.openimgur.R;

/**
 * View that handles different states. Idea from https://github.com/jenzz/Android-MultiStateListView
 * Created by kcampagna on 7/6/14.
 */
public class MultiStateView extends FrameLayout {
    private static final String TAG = MultiStateView.class.getSimpleName();

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

    private ViewState mViewState = ViewState.CONTENT;

    public MultiStateView(Context context) {
        super(context);
        init(null);
    }

    public MultiStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public MultiStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mInflater = LayoutInflater.from(getContext());

        if (attrs != null) {
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

            int contentViewId = a.getResourceId(R.styleable.MultiStateView_contentView, -1);
            if (contentViewId > -1) {
                mContentView = mInflater.inflate(contentViewId, this, false);
                addView(mContentView, mContentView.getLayoutParams());
            }

            a.recycle();
        }

        setViewState(mViewState);
    }

    /**
     * Sets the view for the Empty View
     *
     * @param resId Layout Resource for the Empty View
     */
    public void setEmptyView(@LayoutRes int resId) {
        mEmptyView = mInflater.inflate(resId, this, false);
        if (findViewById(mEmptyView.getId()) == null) {
            addView(mEmptyView);
        }
    }

    /**
     * Sets the view for the Empty View
     *
     * @param view
     */
    public void setEmptyView(@NonNull View view) {
        mEmptyView = view;
        if (findViewById(mEmptyView.getId()) == null) {
            addView(mEmptyView);
        }
    }

    /**
     * Sets the view for the Loading View
     *
     * @param resId Layout Resource for the Loading View
     */
    public void setLoadingView(@LayoutRes int resId) {
        mLoadingView = mInflater.inflate(resId, this, false);
        if (findViewById(mLoadingView.getId()) == null) {
            addView(mLoadingView);
        }
    }

    /**
     * Sets the view for the Loading View
     *
     * @param view
     */
    public void setLoadingView(@NonNull View view) {
        mLoadingView = view;
        if (findViewById(mLoadingView.getId()) == null) {
            addView(mLoadingView);
        }
    }

    /**
     * Sets the view for the Content View
     *
     * @param resId Layout Resource for the Content View
     */
    public void setContentView(@LayoutRes int resId) {
        mContentView = mInflater.inflate(resId, this, false);
        if (findViewById(mContentView.getId()) == null) {
            addView(mContentView);
        }
    }

    /**
     * Sets the view for the Content View
     *
     * @param view
     */
    public void setContentView(@NonNull View view) {
        mContentView = view;
        if (findViewById(mContentView.getId()) == null) {
            addView(mContentView);
        }
    }

    /**
     * Sets the view for the Error View
     *
     * @param resId Layout Resource for the Error View
     */
    public void setErrorView(@LayoutRes int resId) {
        mErrorView = mInflater.inflate(resId, this, false);
        if (findViewById(mErrorView.getId()) == null) {
            addView(mErrorView);
        }
    }

    /**
     * Sets the view for the Error View
     *
     * @param view
     */
    public void setErrorView(@NonNull View view) {
        mErrorView = view;
        if (findViewById(mErrorView.getId()) == null) {
            addView(mErrorView);
        }
    }

    /**
     * Sets the ErrorView along with the error message. The view MUST contain a TextView with the given id
     *
     * @param view         ErrorView
     * @param textViewId   IdRes of the TextView within the ErrorView
     * @param errorMessage StringRes of the error message
     */
    public void setErrorView(@NonNull View view, @IdRes int textViewId, @StringRes int errorMessage) {
        mErrorView = view;
        TextView errorTextView = (TextView) mErrorView.findViewById(textViewId);
        if (errorTextView != null) {
            errorTextView.setText(errorMessage);
        }

        if (findViewById(mErrorView.getId()) == null) {
            addView(mErrorView);
        }
    }

    /**
     * Sets the ErrorView along with the error message. The view MUST contain a TextView with the given id
     *
     * @param resId        Layout Resource for the Empty View
     * @param textViewId   IdRes of the TextView within the ErrorView
     * @param errorMessage StringRes of the error message
     */
    public void setErrorView(@LayoutRes int resId, @IdRes int textViewId, @StringRes int errorMessage) {
        mErrorView = mInflater.inflate(resId, this, false);
        TextView errorTextView = (TextView) mErrorView.findViewById(textViewId);
        if (errorTextView != null) {
            errorTextView.setText(errorMessage);
        }

        if (findViewById(mErrorView.getId()) == null) {
            addView(mErrorView);
        }
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
        if (errorTextView != null) {
            errorTextView.setText(errorMessage);
        }
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
        if (errorTextView != null) {
            errorTextView.setText(errorMessage);
        }
    }

    /**
     * Sets the text for the Empty View
     *
     * @param textViewId   TextView id in the Empty View
     * @param errorMessage The error message
     */
    public void setEmptyText(@IdRes int textViewId, @StringRes int errorMessage) {
        if (mEmptyView == null) {
            throw new NullPointerException("Empty view is null");
        }

        TextView emptyTextView = (TextView) mEmptyView.findViewById(textViewId);
        if (emptyTextView != null) {
            emptyTextView.setText(errorMessage);
        }
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
        if (emptyTextView != null) {
            emptyTextView.setText(errorMessage);
        }
    }

    public View getErrorView() {
        return mErrorView;
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
        mViewState = state;
        setView();
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

        invalidate();
    }
}