package com.kenny.openimgur.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurFilters.GallerySection;
import com.kenny.openimgur.classes.ImgurFilters.GallerySort;
import com.kenny.openimgur.classes.ImgurFilters.TimeSort;
import com.kenny.openimgur.util.ViewUtils;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 10/25/14.
 */
public class GalleryFilterFragment extends BaseFragment implements SeekBar.OnSeekBarChangeListener,
        RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    private static final String KEY_SECTION = "section";
    private static final String KEY_SORT = "sort";
    private static final String KEY_VIRAL = "showViral";
    private static final String KEY_TIME_SORT = "timeSort";

    @InjectView(R.id.sectionGroup)
    RadioGroup mSectionRG;

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

    @InjectView(R.id.showViral)
    CheckBox mShowViral;

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
    public static GalleryFilterFragment createInstance(GallerySort sort, GallerySection section, TimeSort timeSort, boolean showViral) {
        Bundle args = new Bundle();
        GalleryFilterFragment fragment = new GalleryFilterFragment();
        args.putString(KEY_SECTION, section.getSection());
        args.putString(KEY_SORT, sort.getSort());
        args.putBoolean(KEY_VIRAL, showViral);
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
        return inflater.inflate(R.layout.fragment_filter_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        configToolBar((Toolbar) view.findViewById(R.id.toolBar));
        ((TextView) view.findViewById(R.id.sectionTitle)).setTextColor(getResources().getColor(theme.darkColor));
        ((TextView) view.findViewById(R.id.sortTitle)).setTextColor(getResources().getColor(theme.darkColor));
        ((TextView) view.findViewById(R.id.dateTitle)).setTextColor(getResources().getColor(theme.darkColor));
        Bundle args = getArguments();
        GallerySort sort = GallerySort.getSortFromString(args.getString(KEY_SORT, null));
        GallerySection section = GallerySection.getSectionFromString(args.getString(KEY_SECTION, null));
        TimeSort timeSort = TimeSort.getSortFromString(args.getString(KEY_TIME_SORT, null));
        mShowViral.setChecked(args.getBoolean(KEY_VIRAL, true));

        switch (section) {
            case USER:
                mSectionRG.check(R.id.userSubRB);
                mShowViral.setVisibility(View.VISIBLE);
                mOptionalSort.setText(R.string.filter_rising);
                mDateRangerContainer.setVisibility(View.INVISIBLE);
                break;

            case HOT:
                if (sort == GallerySort.HIGHEST_SCORING) {
                    mDateRangerContainer.setVisibility(View.VISIBLE);
                } else {
                    mDateRangerContainer.setVisibility(View.INVISIBLE);
                }

                mOptionalSort.setText(R.string.filter_highest_scoring);
                mOptionalSort.setAlpha(1.0f);
                mSectionRG.check(R.id.viralRB);
                mShowViral.setVisibility(View.GONE);
                break;
        }

        switch (sort) {
            case HIGHEST_SCORING:
            case RISING:
                mOptionalSort.setTextColor(getResources().getColor(theme.accentColor));
                mSeekBar.setProgress(50);
                mOptionalSort.setTypeface(null, Typeface.BOLD);
                break;

            case VIRAL:
                mViral.setTextColor(getResources().getColor(theme.accentColor));
                mSeekBar.setProgress(100);
                mViral.setTypeface(null, Typeface.BOLD);
                break;

            case TIME:
                mSeekBar.setProgress(0);
                mTime.setTextColor(getResources().getColor(theme.accentColor));
                mTime.setTypeface(null, Typeface.BOLD);
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
        mSectionRG.setOnCheckedChangeListener(this);

        // I've never found fragment transaction animations to work properly, so we will animate the view
        // when it is added to the fragment manager
        view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.filter_appear));
    }

    /**
     * Updates the Range TextViews appropriately
     *
     * @param topSort
     */
    private void updateTextView(ImgurFilters.TimeSort topSort) {
        int selected = getResources().getColor(theme.accentColor);
        int black = Color.BLACK;
        int tfNormal = Typeface.NORMAL;
        int tfBold = Typeface.BOLD;

        mDay.setTextColor(topSort == ImgurFilters.TimeSort.DAY ? selected : black);
        mDay.setTypeface(null, topSort == ImgurFilters.TimeSort.DAY ? tfBold : tfNormal);
        mWeek.setTextColor(topSort == ImgurFilters.TimeSort.WEEK ? selected : black);
        mWeek.setTypeface(null, topSort == ImgurFilters.TimeSort.WEEK ? tfBold : tfNormal);
        mMonth.setTextColor(topSort == ImgurFilters.TimeSort.MONTH ? selected : black);
        mMonth.setTypeface(null, topSort == ImgurFilters.TimeSort.MONTH ? tfBold : tfNormal);
        mYear.setTextColor(topSort == ImgurFilters.TimeSort.YEAR ? selected : black);
        mYear.setTypeface(null, topSort == ImgurFilters.TimeSort.YEAR ? tfBold : tfNormal);
        mAll.setTextColor(topSort == ImgurFilters.TimeSort.ALL ? selected : black);
        mAll.setTypeface(null, topSort == ImgurFilters.TimeSort.ALL ? tfBold : tfNormal);
    }

    private void configToolBar(Toolbar tb) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tb.getLayoutParams();
        lp.setMargins(0, ViewUtils.getStatusBarHeight(getActivity()), 0, 0);
        tb.setLayoutParams(lp);
        tb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss(null, null, null);
            }
        });
        tb.setBackgroundColor(getResources().getColor(theme.primaryColor));
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

        if (seekBar == mSeekBar) {
            boolean isViral = mSectionRG.getCheckedRadioButtonId() == R.id.viralRB;

            if (position <= 33) {
                mSeekBar.setProgress(0);
                mTime.setTextColor(getResources().getColor(theme.accentColor));
                mViral.setTextColor(Color.BLACK);
                mOptionalSort.setTextColor(Color.BLACK);
                mTime.setTypeface(null, Typeface.BOLD);
                mViral.setTypeface(null, Typeface.NORMAL);
                mOptionalSort.setTypeface(null, Typeface.NORMAL);

                if (isViral && mDateRangerContainer.getVisibility() == View.VISIBLE) {
                    toggleDataSeekBar(false);
                }
            } else if (position <= 66) {
                mSeekBar.setProgress(50);
                mTime.setTextColor(Color.BLACK);
                mViral.setTextColor(Color.BLACK);
                mOptionalSort.setTextColor(getResources().getColor(theme.accentColor));
                mTime.setTypeface(null, Typeface.NORMAL);
                mViral.setTypeface(null, Typeface.NORMAL);
                mOptionalSort.setTypeface(null, Typeface.BOLD);

                if (isViral && mDateRangerContainer.getVisibility() == View.INVISIBLE) {
                    toggleDataSeekBar(true);
                }
            } else {
                mSeekBar.setProgress(100);
                mTime.setTextColor(Color.BLACK);
                mViral.setTextColor(getResources().getColor(theme.accentColor));
                mOptionalSort.setTextColor(Color.BLACK);
                mTime.setTypeface(null, Typeface.NORMAL);
                mViral.setTypeface(null, Typeface.BOLD);
                mOptionalSort.setTypeface(null, Typeface.NORMAL);

                if (isViral && mDateRangerContainer.getVisibility() == View.VISIBLE) {
                    toggleDataSeekBar(false);
                }
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

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.userSubRB:
                mShowViral.setVisibility(View.VISIBLE);
                mOptionalSort.setText(R.string.filter_rising);
                toggleDataSeekBar(false);
                break;

            case R.id.viralRB:
                mOptionalSort.setText(R.string.filter_highest_scoring);
                mShowViral.setVisibility(View.GONE);
                if (mSeekBar.getProgress() == 50) toggleDataSeekBar(true);
                break;
        }
    }

    private void toggleDataSeekBar(final boolean shouldShow) {
        mDateRangerContainer.animate().alpha(shouldShow ? 1.0f : 0.0f).setDuration(300L)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        animation.removeAllListeners();
                        if (mDateRangerContainer != null) {
                            mDateRangerContainer.setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
                        }
                    }
                });
    }

    @OnClick({R.id.negative, R.id.positive})
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.negative:
                dismiss(null, null, null);
                break;

            case R.id.positive:
                GallerySort sort;
                GallerySection section;
                TimeSort timeSort;
                int position = mSeekBar.getProgress();
                int timePosition = mDateSeekBar.getProgress();

                if (mSectionRG.getCheckedRadioButtonId() == R.id.userSubRB) {
                    section = GallerySection.USER;

                    if (position == 0) {
                        sort = GallerySort.TIME;
                    } else if (position == 50) {
                        sort = GallerySort.RISING;
                    } else {
                        sort = GallerySort.VIRAL;
                    }
                } else {
                    section = GallerySection.HOT;

                    if (position == 0) {
                        sort = GallerySort.TIME;
                    } else if (position == 50) {
                        sort = GallerySort.HIGHEST_SCORING;
                    } else {
                        sort = GallerySort.VIRAL;
                    }
                }

                if (timePosition <= 10) {
                    timeSort = TimeSort.DAY;
                } else if (timePosition <= 30) {
                    timeSort = TimeSort.WEEK;
                } else if (timePosition <= 50) {
                    timeSort = TimeSort.MONTH;
                } else if (timePosition <= 70) {
                    timeSort = TimeSort.YEAR;
                } else {
                    timeSort = TimeSort.ALL;
                }

                dismiss(sort, section, timeSort);
                break;
        }
    }

    /**
     * Animates the removal of the dialog
     *
     * @param sort
     * @param section
     */
    public void dismiss(final GallerySort sort, final GallerySection section, final TimeSort timeSort) {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.filter_disappear);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (mListener != null)
                    mListener.onFilterChange(section, sort, timeSort, mShowViral.isChecked());
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
     * Sets the listener for the Filter Fragment
     *
     * @param listener
     */
    public void setFilterListener(FilterListener listener) {
        mListener = listener;
    }

    public static interface FilterListener {
        void onFilterChange(GallerySection section, GallerySort sort, TimeSort timeSort, boolean showViral);
    }

}
