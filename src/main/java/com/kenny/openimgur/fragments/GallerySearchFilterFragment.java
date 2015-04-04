package com.kenny.openimgur.fragments;

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
import android.widget.SeekBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.util.ViewUtils;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 4/3/15.
 */
public class GallerySearchFilterFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener {
    private static final String KEY_SORT = "sort";

    private static final String KEY_TIME_SORT = "timeSort";

    @InjectView(R.id.time)
    TextView mTime;

    @InjectView(R.id.optionalSort)
    TextView mOptionalSort;

    @InjectView(R.id.viral)
    TextView mViral;

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

    @InjectView(R.id.sortSeekBar)
    SeekBar mSeekBar;

    @InjectView(R.id.dateSeekBar)
    SeekBar mDateSeekBar;

    @InjectView(R.id.dateRangeContainer)
    View mDateRangerContainer;

    private FilterListener mListener;

    /**
     * Creates a new instance of GalleryFilterFragment
     *
     * @param sort      The current GallerySort
     * @param section   The current GallerySection
     * @param showViral If viral images should be shown in User Sub
     * @return
     */
    public static GallerySearchFilterFragment createInstance(ImgurFilters.GallerySort sort, ImgurFilters.TimeSort timeSort) {
        Bundle args = new Bundle();
        GallerySearchFilterFragment fragment = new GallerySearchFilterFragment();
        args.putString(KEY_SORT, sort.getSort());
        args.putString(KEY_TIME_SORT, timeSort.getSort());
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
        return inflater.inflate(R.layout.fragment_filter_gallery_search, container, false);
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
        ImgurFilters.GallerySort sort = ImgurFilters.GallerySort.getSortFromString(args.getString(KEY_SORT, null));
        ImgurFilters.TimeSort timeSort = ImgurFilters.TimeSort.getSortFromString(args.getString(KEY_TIME_SORT, null));

        switch (sort) {
            case HIGHEST_SCORING:
            case RISING:
                mOptionalSort.setTextColor(getResources().getColor(theme.accentColor));
                mSeekBar.setProgress(50);
                mOptionalSort.setTypeface(null, Typeface.BOLD);
                mDateRangerContainer.setVisibility(View.VISIBLE);
                break;

            case VIRAL:
                mViral.setTextColor(getResources().getColor(theme.accentColor));
                mSeekBar.setProgress(100);
                mViral.setTypeface(null, Typeface.BOLD);
                mDateRangerContainer.setVisibility(View.INVISIBLE);
                break;

            case TIME:
                mSeekBar.setProgress(0);
                mTime.setTextColor(getResources().getColor(theme.accentColor));
                mTime.setTypeface(null, Typeface.BOLD);
                mDateRangerContainer.setVisibility(View.INVISIBLE);
                break;
        }


        switch (timeSort) {
            default:
            case DAY:
                mDateSeekBar.setProgress(0);
                break;

            case WEEK:
                mDateSeekBar.setProgress(20);
                break;

            case MONTH:
                mDateSeekBar.setProgress(40);
                break;

            case YEAR:
                mDateSeekBar.setProgress(60);
                break;

            case ALL:
                mDateSeekBar.setProgress(80);
                break;
        }

        updateTextView(timeSort);
        mSeekBar.setOnSeekBarChangeListener(this);
        mDateSeekBar.setOnSeekBarChangeListener(this);

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
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int position = seekBar.getProgress();
        int defaultColor = theme.isDarkTheme ? Color.WHITE : Color.BLACK;

        if (seekBar == mSeekBar) {

            if (position <= 33) {
                mSeekBar.setProgress(0);
                mTime.setTextColor(getResources().getColor(theme.accentColor));
                mViral.setTextColor(defaultColor);
                mOptionalSort.setTextColor(defaultColor);
                mTime.setTypeface(null, Typeface.BOLD);
                mViral.setTypeface(null, Typeface.NORMAL);
                mOptionalSort.setTypeface(null, Typeface.NORMAL);
                mDateRangerContainer.setVisibility(View.INVISIBLE);
            } else if (position <= 66) {
                mSeekBar.setProgress(50);
                mTime.setTextColor(defaultColor);
                mViral.setTextColor(defaultColor);
                mOptionalSort.setTextColor(getResources().getColor(theme.accentColor));
                mTime.setTypeface(null, Typeface.NORMAL);
                mViral.setTypeface(null, Typeface.NORMAL);
                mOptionalSort.setTypeface(null, Typeface.BOLD);
                mDateRangerContainer.setVisibility(View.VISIBLE);
            } else {
                mSeekBar.setProgress(100);
                mTime.setTextColor(defaultColor);
                mViral.setTextColor(getResources().getColor(theme.accentColor));
                mOptionalSort.setTextColor(defaultColor);
                mTime.setTypeface(null, Typeface.NORMAL);
                mViral.setTypeface(null, Typeface.BOLD);
                mOptionalSort.setTypeface(null, Typeface.NORMAL);
                mDateRangerContainer.setVisibility(View.INVISIBLE);
            }
        } else {
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

    public void dismiss(final ImgurFilters.GallerySort sort, final ImgurFilters.TimeSort timeSort) {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.filter_disappear);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (mListener != null) mListener.onFilterChange(sort, timeSort);
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

    public void setListener(FilterListener listener) {
        mListener = listener;
    }

    @OnClick({R.id.negative, R.id.positive})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.negative:
                dismiss(null, null);
                break;

            case R.id.positive:
                ImgurFilters.GallerySort sort;
                ImgurFilters.TimeSort timeSort;
                int position = mSeekBar.getProgress();
                int timePosition = mDateSeekBar.getProgress();

                if (timePosition <= 10) {
                    timeSort = ImgurFilters.TimeSort.DAY;
                } else if (timePosition <= 30) {
                    timeSort = ImgurFilters.TimeSort.WEEK;
                } else if (timePosition <= 50) {
                    timeSort = ImgurFilters.TimeSort.MONTH;
                } else if (timePosition <= 70) {
                    timeSort = ImgurFilters.TimeSort.YEAR;
                } else {
                    timeSort = ImgurFilters.TimeSort.ALL;
                }

                if(position<=0){
                    sort = ImgurFilters.GallerySort.TIME;
                }else if (position<=50){
                    sort = ImgurFilters.GallerySort.HIGHEST_SCORING;
                }else{
                    sort = ImgurFilters.GallerySort.VIRAL;
                }

                dismiss(sort, timeSort);
                break;
        }
    }

    public interface FilterListener {
        void onFilterChange(ImgurFilters.GallerySort sort, ImgurFilters.TimeSort timeSort);
    }
}
