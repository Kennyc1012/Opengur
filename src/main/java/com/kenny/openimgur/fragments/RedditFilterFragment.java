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
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurFilters.RedditSort;
import com.kenny.openimgur.util.ViewUtils;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 10/26/14.
 */
public class RedditFilterFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener,
        RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    private static final String KEY_SORT = "sort";

    private static final String KEY_TOP_SORT = "top_sort";

    private FilterListener mListener;

    @InjectView(R.id.dateSeekBar)
    SeekBar mSeekBar;

    @InjectView(R.id.day)
    TextView mDay;

    @InjectView(R.id.week)
    TextView mWeek;

    @InjectView(R.id.month)
    TextView mMonth;

    @InjectView(R.id.year)
    TextView mYear;

    @InjectView(R.id.all)
    TextView mAll;

    @InjectView(R.id.sortRG)
    RadioGroup mSortRG;

    @InjectView(R.id.dateRangeContainer)
    View mDateRangeContainer;

    public static RedditFilterFragment createInstance(RedditSort sort, ImgurFilters.TimeSort topSort) {
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

        if (theme.isDarkTheme) {
            view.setBackgroundColor(getResources().getColor(R.color.background_material_dark));
        } else {
            view.setBackgroundColor(getResources().getColor(R.color.background_material_light));
        }

        configToolBar((Toolbar) view.findViewById(R.id.toolBar));
        Bundle args = getArguments();
        RedditSort sort = (RedditSort) args.getSerializable(KEY_SORT);
        ImgurFilters.TimeSort topSort = (ImgurFilters.TimeSort) args.getSerializable(KEY_TOP_SORT);
        mSortRG.check(sort == RedditSort.TIME ? R.id.newestRB : R.id.topRB);
        mDateRangeContainer.setVisibility(sort == RedditSort.TOP ? View.VISIBLE : View.GONE);

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

    @OnClick({R.id.negative, R.id.positive})
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.negative:
                dismiss(null, null);
                break;

            case R.id.positive:
                RedditSort sort = mSortRG.getCheckedRadioButtonId() == R.id.newestRB
                        ? RedditSort.TIME : RedditSort.TOP;

                ImgurFilters.TimeSort topSort;
                int position = mSeekBar.getProgress();

                if (position <= 10) {
                    topSort = ImgurFilters.TimeSort.DAY;
                } else if (position <= 30) {
                    topSort = ImgurFilters.TimeSort.WEEK;
                } else if (position <= 50) {
                    topSort = ImgurFilters.TimeSort.MONTH;
                } else if (position <= 70) {
                    topSort = ImgurFilters.TimeSort.YEAR;
                } else {
                    topSort = ImgurFilters.TimeSort.ALL;
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
    private void updateTextView(ImgurFilters.TimeSort topSort) {
        int selected = getResources().getColor(theme.accentColor);
        int defaultColor = theme.isDarkTheme ? Color.WHITE : Color.BLACK;
        int tfNormal = Typeface.NORMAL;
        int tfBold = Typeface.BOLD;

        mDay.setTextColor(topSort == ImgurFilters.TimeSort.DAY ? selected : defaultColor);
        mDay.setTypeface(null, topSort == ImgurFilters.TimeSort.DAY ? tfBold : tfNormal);
        mWeek.setTextColor(topSort == ImgurFilters.TimeSort.WEEK ? selected : defaultColor);
        mWeek.setTypeface(null, topSort == ImgurFilters.TimeSort.WEEK ? tfBold : tfNormal);
        mMonth.setTextColor(topSort == ImgurFilters.TimeSort.MONTH ? selected : defaultColor);
        mMonth.setTypeface(null, topSort == ImgurFilters.TimeSort.MONTH ? tfBold : tfNormal);
        mYear.setTextColor(topSort == ImgurFilters.TimeSort.YEAR ? selected : defaultColor);
        mYear.setTypeface(null, topSort == ImgurFilters.TimeSort.YEAR ? tfBold : tfNormal);
        mAll.setTextColor(topSort == ImgurFilters.TimeSort.ALL ? selected : defaultColor);
        mAll.setTypeface(null, topSort == ImgurFilters.TimeSort.ALL ? tfBold : tfNormal);
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
            updateTextView(ImgurFilters.TimeSort.DAY);
        } else if (position <= 30) {
            seekBar.setProgress(20);
            updateTextView(ImgurFilters.TimeSort.WEEK);
        } else if (position <= 50) {
            seekBar.setProgress(40);
            updateTextView(ImgurFilters.TimeSort.MONTH);
        } else if (position <= 70) {
            seekBar.setProgress(60);
            updateTextView(ImgurFilters.TimeSort.YEAR);
        } else {
            seekBar.setProgress(80);
            updateTextView(ImgurFilters.TimeSort.ALL);
        }
    }

    /**
     * Animates the removal of the dialog
     *
     * @param sort
     * @param topSort
     */
    public void dismiss(final RedditSort sort, final ImgurFilters.TimeSort topSort) {
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
    public void setFilterListener(FilterListener listener) {
        mListener = listener;
    }

    public interface FilterListener {
        void onFilterChanged(RedditSort sort, ImgurFilters.TimeSort topSort);
    }
}
