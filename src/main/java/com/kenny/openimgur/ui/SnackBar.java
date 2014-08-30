package com.kenny.openimgur.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import com.kenny.openimgur.R;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by kcampagna on 8/28/14.
 */
public class SnackBar {
    private static final String TAG = "SnackBar";

    /**
     * Shows a Snack Bar
     *
     * @param activity The activity to show the Snack Bar in
     * @param message  The Sting Resource of the message to show
     */
    public static void show(Activity activity, @StringRes int message) {
        SnackBarItem sbi = new SnackBarItem(message);
        SnackBarManager mngr = SnackBarManager.getInstance();
        mngr.addSnackBar(activity, sbi);

        if (!mngr.isShowingSnackBar()) {
            SnackBarManager.getInstance().showSnackbars(activity);
        }
    }

    /**
     * Canels all SnackBars for the given activity
     *
     * @param activity
     */
    public static void cancelSnackBars(Activity activity) {
        SnackBarManager.getInstance().cancelSnackBars(activity);
    }

    private static class SnackBarManager implements SnackBarListener {
        private ConcurrentHashMap<Activity, ConcurrentLinkedQueue<SnackBarItem>> mQueue =
                new ConcurrentHashMap<Activity, ConcurrentLinkedQueue<SnackBarItem>>();

        private static SnackBarManager mManager;

        private boolean mIsShowingSnackBar = false;

        public static SnackBarManager getInstance() {
            if (mManager == null) {
                mManager = new SnackBarManager();
            }

            return mManager;
        }

        /**
         * Cancels all Snack Bar messages for an activity
         *
         * @param activity
         */
        public void cancelSnackBars(Activity activity) {
            ConcurrentLinkedQueue<SnackBarItem> list = mQueue.get(activity);

            if (list != null) {
                Log.v(TAG, "Canceling " + list.size() + " SnackBars");
                for (SnackBarItem items : list) {
                    items.cancel();
                }

                list.clear();
                mQueue.remove(activity);
            }
        }

        /**
         * Adds a Snack Bar to The queue to be displayed
         *
         * @param activity
         * @param item
         */
        public void addSnackBar(Activity activity, SnackBarItem item) {
            ConcurrentLinkedQueue<SnackBarItem> list = mQueue.get(activity);

            if (list == null) {
                list = new ConcurrentLinkedQueue<SnackBarItem>();
                mQueue.put(activity, list);
            }

            list.add(item);
        }

        /**
         * Shows the next SnackBar for the current activity
         *
         * @param activity
         */
        public void showSnackbars(Activity activity) {
            ConcurrentLinkedQueue<SnackBarItem> list = mQueue.get(activity);

            if (list != null) {
                mIsShowingSnackBar = true;
                list.peek().show(activity, this);
            }
        }

        @Override
        public void onSnackBarFinished(Activity activity, SnackBarItem snackBar) {
            ConcurrentLinkedQueue<SnackBarItem> list = mQueue.get(activity);

            if (list != null) {
                list.remove(snackBar);

                if (list.peek() == null) {
                    Log.v(TAG, "No more Snack Bars to be display for the activity");
                    mQueue.remove(activity);
                    mIsShowingSnackBar = false;
                } else {
                    Log.v(TAG, "Remaining Snack Bars to be shown " + list.size());
                    mIsShowingSnackBar = true;
                    list.peek().show(activity, this);
                }
            }
        }

        public int getSnackBarQueueSize() {
            return mQueue.size();
        }

        public boolean isShowingSnackBar() {
            return mIsShowingSnackBar;
        }
    }

    private static class SnackBarItem {
        public View view;

        public int message;

        public AnimatorSet animator;

        private float mSnackBackHeight;

        SnackBarItem(int message) {
            this.message = message;
        }

        /**
         * Shows the Snack Bar
         *
         * @param activity
         * @param listener
         */
        public void show(final Activity activity, final SnackBarListener listener) {
            FrameLayout parent = (FrameLayout) activity.findViewById(android.R.id.content);
            view = activity.getLayoutInflater().inflate(R.layout.snack_bar, parent, false);
            ((TextViewRoboto) view.findViewById(R.id.message)).setText(message);
            mSnackBackHeight = view.getContext().getResources().getDimension(R.dimen.snack_bar_height);
            animator = new AnimatorSet();
            animator.setInterpolator(new DecelerateInterpolator());
            parent.addView(view);

            animator.playSequentially(
                    ObjectAnimator.ofFloat(view, "translationY", mSnackBackHeight, 0).setDuration(500L),
                    ObjectAnimator.ofFloat(view, "translationY", 0, 0).setDuration(2000L),
                    ObjectAnimator.ofFloat(view, "translationY", 0, mSnackBackHeight).setDuration(500L)
            );

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (view != null) {
                        FrameLayout parent = (FrameLayout) view.getParent();

                        if (parent != null) {
                            parent.removeView(view);
                        }
                    }

                    dispose(listener, activity);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    if (view != null) {
                        FrameLayout parent = (FrameLayout) view.getParent();

                        if (parent != null) {
                            parent.removeView(view);
                        }
                    }

                    dispose(listener, activity);
                }
            });

            animator.start();
        }

        /**
         * Cancels the Snack Bar from being displayed
         */
        public void cancel() {
            if (animator != null) {
                animator.cancel();
            }

            dispose(null, null);
        }

        /**
         * Cleans up the Snack Bar when finished
         *
         * @param listener
         * @param activity
         */
        private void dispose(SnackBarListener listener, Activity activity) {
            if (animator != null) {
                animator.removeAllListeners();
                animator = null;
            }

            view = null;

            if (listener != null) {
                listener.onSnackBarFinished(activity, this);
            }
        }
    }

    private static interface SnackBarListener {
        void onSnackBarFinished(Activity activity, SnackBarItem snackBar);
    }
}
