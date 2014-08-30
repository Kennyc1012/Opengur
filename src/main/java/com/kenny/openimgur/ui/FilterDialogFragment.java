package com.kenny.openimgur.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import com.kenny.openimgur.R;
import com.kenny.openimgur.fragments.GalleryFragment;

/**
 * Created by kcampagna on 8/17/14.
 */
public class FilterDialogFragment extends DialogFragment implements View.OnClickListener {
    private static final String KEY_SECTION = "section";

    private static final String KEY_SORT = "sort";

    private RadioGroup mSortRG;

    private FilterListener mListener;

    private SeekBar mSeekBar;

    private TypeWriterTextView mSectionLabel;

    public static FilterDialogFragment createInstance(GalleryFragment.GallerySort sort, GalleryFragment.GallerySection section) {
        Bundle args = new Bundle();
        FilterDialogFragment fragment = new FilterDialogFragment();
        args.putString(KEY_SECTION, section.getSection());
        args.putString(KEY_SORT, sort.getSort());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null)
            return;

        // Dialog Fragments are automatically set to wrap_content, so we need to force the width to fit our view
        int dialogWidth = (int) (getResources().getDisplayMetrics().widthPixels * .85);
        getDialog().getWindow().setLayout(dialogWidth, getDialog().getWindow().getAttributes().height);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    @Override
    public void onDestroyView() {
        mSortRG = null;
        mListener = null;
        mSeekBar = null;
        mSectionLabel = null;
        super.onDestroyView();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.filter_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mSortRG = (RadioGroup) view.findViewById(R.id.filterRG);
        mSeekBar = (SeekBar) view.findViewById(R.id.seekBar);
        mSectionLabel = (TypeWriterTextView) view.findViewById(R.id.section);
        GalleryFragment.GallerySection section = GalleryFragment.GallerySection.HOT;
        GalleryFragment.GallerySort sort = GalleryFragment.GallerySort.VIRAL;

        if (getArguments() != null) {
            Bundle bundle = getArguments();
            section = GalleryFragment.GallerySection.getSectionFromString(bundle.getString(KEY_SECTION,
                    GalleryFragment.GallerySection.HOT.getSection()));
            sort = GalleryFragment.GallerySort.getSortFromString(bundle.getString(KEY_SORT, GalleryFragment.GallerySort.VIRAL.getSort()));
        }

        mSeekBar.setProgress(GalleryFragment.GallerySection.getPositionFromSeection(section));
        mSectionLabel.setText(section.getResourceId());

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

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int position, boolean b) {
                mSectionLabel.animateText(GalleryFragment.GallerySection.getSectionFromPosition(position).getResourceId());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel:
                dismiss();
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

                    section = GalleryFragment.GallerySection.getSectionFromPosition(mSeekBar.getProgress());

                    if (mListener != null) {
                        mListener.onFilterChange(section, sort);
                    }
                }

                dismiss();
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
