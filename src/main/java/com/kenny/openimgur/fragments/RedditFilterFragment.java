package com.kenny.openimgur.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.util.ViewUtils;

/**
 * Created by kcampagna on 10/26/14.
 */
public class RedditFilterFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener,
        RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    private static final String KEY_SORT = "sort";

    private static final String KEY_TOP_SORT = "top_sort";

    private RedditFilterListener mListener;

    private SeekBar mSeekBar;

    private TextView mDay;

    private TextView mWeek;

    private TextView mMonth;

    private TextView mYear;

    private TextView mAll;

    private RadioGroup mSortRG;

    private View mDateRangeContainer;

    public static RedditFilterFragment createInstance(RedditFragment.RedditSort sort, RedditFragment.RedditTopSort topSort) {
        RedditFilterFragment fragment = new RedditFilterFragment();
        Bundle args = new Bundle(2);
        args.putSerializable(KEY_SORT, sort);
        args.putSerializable(KEY_TOP_SORT, topSort);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter_reddit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configToolBar((Toolbar) view.findViewById(R.id.toolBar));
        mSeekBar = (SeekBar) view.findViewById(R.id.dateSeekBar);
        mSortRG = (RadioGroup) view.findViewById(R.id.sortRG);
        mDateRangeContainer = view.findViewById(R.id.dateRangeContainer);
        mDay = (TextView) view.findViewById(R.id.day);
        mWeek = (TextView) view.findViewById(R.id.week);
        mMonth = (TextView) view.findViewById(R.id.month);
        mYear = (TextView) view.findViewById(R.id.year);
        mAll = (TextView) view.findViewById(R.id.all);
        ((TextView) view.findViewById(R.id.sortTitle)).setTextColor(getResources().getColor(theme.darkColor));
        ((TextView) view.findViewById(R.id.dateTitle)).setTextColor(getResources().getColor(theme.darkColor));

        Bundle args = getArguments();
        RedditFragment.RedditSort sort = (RedditFragment.RedditSort) args.getSerializable(KEY_SORT);
        RedditFragment.RedditTopSort topSort = (RedditFragment.RedditTopSort) args.getSerializable(KEY_TOP_SORT);
        mSortRG.check(sort == RedditFragment.RedditSort.TIME ? R.id.newestRB : R.id.topRB);
        mDateRangeContainer.setVisibility(sort == RedditFragment.RedditSort.TOP ? View.VISIBLE : View.GONE);

        switch (topSort) {
            case DAY:
                mSeekBar.setProgress(0);
                mDay.setTypeface(null, Typeface.BOLD);
                mDay.setTextColor(getResources().getColor(theme.accentColor));
                break;

            case WEEK:
                mSeekBar.setProgress(20);
                mWeek.setTypeface(null, Typeface.BOLD);
                mWeek.setTextColor(getResources().getColor(theme.accentColor));
                break;

            case MONTH:
                mSeekBar.setProgress(40);
                mMonth.setTypeface(null, Typeface.BOLD);
                mMonth.setTextColor(getResources().getColor(theme.accentColor));
                break;

            case YEAR:
                mSeekBar.setProgress(60);
                mYear.setTypeface(null, Typeface.BOLD);
                mYear.setTextColor(getResources().getColor(theme.accentColor));
                break;

            case ALL:
                mSeekBar.setProgress(80);
                mAll.setTypeface(null, Typeface.BOLD);
                mAll.setTextColor(getResources().getColor(theme.accentColor));
                break;
        }

        mSeekBar.setOnSeekBarChangeListener(this);
        mSortRG.setOnCheckedChangeListener(this);
        view.findViewById(R.id.negative).setOnClickListener(this);
        view.findViewById(R.id.positive).setOnClickListener(this);

        // I've never found fragment transaction animations to work properly, so we will animate the view
        // when it is added to the fragment manager
        view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.filter_appear));
    }

    private void configToolBar(Toolbar tb) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tb.getLayoutParams();
        lp.setMargins(0, ViewUtils.getStatusBarHeight(getActivity()), 0, 0);
        tb.setLayoutParams(lp);
        tb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss(null, null);
            }
        });
        tb.setBackgroundColor(getResources().getColor(theme.primaryColor));
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        ObjectAnimator anim;
        switch (checkedId) {
            case R.id.newestRB:
                anim = ObjectAnimator.ofFloat(mDateRangeContainer, "alpha", 1.0f, 0.0f).setDuration(300L);

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationStart(animation);
                        animation.removeAllListeners();
                        mDateRangeContainer.setVisibility(View.GONE);
                    }
                });

                anim.start();
                break;

            case R.id.topRB:
                anim = ObjectAnimator.ofFloat(mDateRangeContainer, "alpha", 0.0f, 1.0f).setDuration(300L);

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        animation.removeAllListeners();
                        mDateRangeContainer.setVisibility(View.VISIBLE);
                    }
                });

                anim.start();
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.negative:
                dismiss(null, null);
                break;

            case R.id.positive:
                RedditFragment.RedditSort sort = mSortRG.getCheckedRadioButtonId() == R.id.newestRB
                        ? RedditFragment.RedditSort.TIME : RedditFragment.RedditSort.TOP;

                RedditFragment.RedditTopSort topSort;
                int position = mSeekBar.getProgress();

                if (position <= 10) {
                    topSort = RedditFragment.RedditTopSort.DAY;
                } else if (position <= 30) {
                    topSort = RedditFragment.RedditTopSort.WEEK;
                } else if (position <= 50) {
                    topSort = RedditFragment.RedditTopSort.MONTH;
                } else if (position <= 70) {
                    topSort = RedditFragment.RedditTopSort.YEAR;
                } else {
                    topSort = RedditFragment.RedditTopSort.ALL;
                }

                dismiss(sort, topSort);
                break;
        }
    }

    /**
     * Updates the Range TextViews appropriately
     *
     * @param topSort
     */
    private void updateTextView(RedditFragment.RedditTopSort topSort) {
        int selected = getResources().getColor(theme.accentColor);
        int black = Color.BLACK;
        int tfNormal = Typeface.NORMAL;
        int tfBold = Typeface.BOLD;

        mDay.setTextColor(topSort == RedditFragment.RedditTopSort.DAY ? selected : black);
        mDay.setTypeface(null, topSort == RedditFragment.RedditTopSort.DAY ? tfBold : tfNormal);
        mWeek.setTextColor(topSort == RedditFragment.RedditTopSort.WEEK ? selected : black);
        mWeek.setTypeface(null, topSort == RedditFragment.RedditTopSort.WEEK ? tfBold : tfNormal);
        mMonth.setTextColor(topSort == RedditFragment.RedditTopSort.MONTH ? selected : black);
        mMonth.setTypeface(null, topSort == RedditFragment.RedditTopSort.MONTH ? tfBold : tfNormal);
        mYear.setTextColor(topSort == RedditFragment.RedditTopSort.YEAR ? selected : black);
        mYear.setTypeface(null, topSort == RedditFragment.RedditTopSort.YEAR ? tfBold : tfNormal);
        mAll.setTextColor(topSort == RedditFragment.RedditTopSort.ALL ? selected : black);
        mAll.setTypeface(null, topSort == RedditFragment.RedditTopSort.ALL ? tfBold : tfNormal);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        //NOOP
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        //NOOP
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int position = seekBar.getProgress();

        if (position <= 10) {
            seekBar.setProgress(0);
            updateTextView(RedditFragment.RedditTopSort.DAY);
        } else if (position <= 30) {
            seekBar.setProgress(20);
            updateTextView(RedditFragment.RedditTopSort.WEEK);
        } else if (position <= 50) {
            seekBar.setProgress(40);
            updateTextView(RedditFragment.RedditTopSort.MONTH);
        } else if (position <= 70) {
            seekBar.setProgress(60);
            updateTextView(RedditFragment.RedditTopSort.YEAR);
        } else {
            seekBar.setProgress(80);
            updateTextView(RedditFragment.RedditTopSort.ALL);
        }
    }

    /**
     * Animates the removal of the dialog
     *
     * @param sort
     * @param topSort
     */
    public void dismiss(final RedditFragment.RedditSort sort, final RedditFragment.RedditTopSort topSort) {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.filter_disappear);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (mListener != null) mListener.onFilterChanged(sort, topSort);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        getView().startAnimation(anim);
    }

    /**
     * Sets the listener to receive callbacks from the fragment
     *
     * @param listener
     */
    public void setFilterListener(RedditFilterListener listener) {
        mListener = listener;
    }

    public static interface RedditFilterListener {
        void onFilterChanged(RedditFragment.RedditSort sort, RedditFragment.RedditTopSort topSort);
    }
}
