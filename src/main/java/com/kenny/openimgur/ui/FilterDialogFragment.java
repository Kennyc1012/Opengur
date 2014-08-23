package com.kenny.openimgur.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.fragments.GalleryFragment;

/**
 * Created by kcampagna on 8/17/14.
 */
public class FilterDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String KEY_SECTION = "section";

    private static final String KEY_SORT = "sort";

    private RadioGroup mSectionRG;

    private RadioGroup mSortRG;

    private FilterListener mListener;

    public static FilterDialogFragment createInstance(GalleryFragment.GallerySort sort, GalleryFragment.GallerySection section) {
        Bundle args = new Bundle();
        FilterDialogFragment fragment = new FilterDialogFragment();
        args.putString(KEY_SECTION, section.getSection());
        args.putString(KEY_SORT, sort.getSort());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        mSectionRG = null;
        mSortRG = null;
        mListener = null;
        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.filter_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSectionRG = (RadioGroup) view.findViewById(R.id.sectionRG);
        mSortRG = (RadioGroup) view.findViewById(R.id.filterRG);
        GalleryFragment.GallerySection section = GalleryFragment.GallerySection.HOT;
        GalleryFragment.GallerySort sort = GalleryFragment.GallerySort.VIRAL;

        if (getArguments() != null) {
            Bundle bundle = getArguments();
            section = GalleryFragment.GallerySection.getSectionFromString(bundle.getString(KEY_SECTION,
                    GalleryFragment.GallerySection.HOT.getSection()));
            sort = GalleryFragment.GallerySort.getSortFromString(bundle.getString(KEY_SORT, GalleryFragment.GallerySort.VIRAL.getSort()));
        }

        switch (section) {
            case HOT:
                mSectionRG.check(R.id.viral);
                break;

            case TOP:
                mSectionRG.check(R.id.top);
                break;

            case USER:
                mSectionRG.check(R.id.user);
                break;
        }

        switch (sort) {
            case TIME:
                mSortRG.check(R.id.time);
                break;

            case VIRAL:
                mSortRG.check(R.id.popular);
                break;
        }
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.accept).setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel:
                dismissAllowingStateLoss();
                break;

            case R.id.accept:
                if (mListener != null) {
                    GalleryFragment.GallerySection section = GalleryFragment.GallerySection.HOT;
                    GalleryFragment.GallerySort sort = GalleryFragment.GallerySort.VIRAL;

                    switch (mSortRG.getCheckedRadioButtonId()) {
                        case R.id.time:
                            sort = GalleryFragment.GallerySort.TIME;
                            break;

                        case R.id.popular:
                            sort = GalleryFragment.GallerySort.VIRAL;
                            break;
                    }

                    switch (mSectionRG.getCheckedRadioButtonId()) {
                        case R.id.user:
                            section = GalleryFragment.GallerySection.USER;
                            break;

                        case R.id.top:
                            section = GalleryFragment.GallerySection.TOP;
                            break;

                        case R.id.viral:
                            section = GalleryFragment.GallerySection.HOT;
                            break;
                    }

                    mListener.onFilterChange(section, sort);
                }

                dismissAllowingStateLoss();
                break;
        }
    }

    public void setFilterListner(FilterListener listner) {
        mListener = listner;
    }

    public interface FilterListener {
        void onFilterChange(GalleryFragment.GallerySection section, GalleryFragment.GallerySort sort);
    }
}
