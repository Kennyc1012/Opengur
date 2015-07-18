package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.BaseActivity;
import com.kenny.openimgur.adapters.TopicsAdapter;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.PhotoUploadListener;
import com.kenny.snackbar.SnackBar;

import java.util.List;

import butterknife.Bind;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

/**
 * Created by Kenny-PC on 7/4/2015.
 */
public class UploadInfoFragment extends BaseFragment {
    @Bind(R.id.title)
    EditText mTitle;

    @Bind(R.id.desc)
    EditText mDesc;

    @Bind(R.id.titleInputLayout)
    TextInputLayout mTitleInputLayout;

    @Bind(R.id.topicSpinner)
    Spinner mTopicSpinner;

    @Bind(R.id.gallerySwitch)
    CheckBox mGallerySwitch;

    @Bind(R.id.topicHeader)
    View mTopicHeader;

    private PhotoUploadListener mListener;

    public static UploadInfoFragment newInstance() {
        return new UploadInfoFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof PhotoUploadListener) mListener = (PhotoUploadListener) activity;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onPause() {
        mTitle.setCursorVisible(false);
        mDesc.setCursorVisible(false);
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        List<ImgurTopic> topics = OpengurApp.getInstance(getActivity()).getSql().getTopics();
        mTopicSpinner.setAdapter(new TopicsAdapter(getActivity(), topics));

        Toolbar tb = (Toolbar) view.findViewById(R.id.toolBar);
        tb.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction()
                        .remove(UploadInfoFragment.this)
                        .commit();

                ((BaseActivity) getActivity()).getSupportActionBar().show();
            }
        });

        tb.setTitle(R.string.upload_info);

        // There is an issue with the TextInputLayout where hints aren't being honored via XML
        mTitle.setHint(R.string.upload_title);
        mDesc.setHint(R.string.upload_desc);
    }

    @OnCheckedChanged(R.id.gallerySwitch)
    public void onCheckChanged(boolean checked) {
        if (checked && app.getUser() == null) {
            mGallerySwitch.setChecked(false);
            SnackBar.show(getActivity(), R.string.upload_gallery_no_user);
            return;
        }

        int visibility = checked ? View.VISIBLE : View.GONE;
        mTopicSpinner.setVisibility(visibility);
        mTopicHeader.setVisibility(visibility);
    }

    @OnClick(R.id.upload)
    public void onUploadClick() {
        String title = mTitle.getText().toString();
        String desc = mDesc.getText().toString();
        ImgurTopic topic = null;
        SpinnerAdapter adapter = mTopicSpinner.getAdapter();

        if (adapter != null && !adapter.isEmpty()) {
            int selectedPosition = mTopicSpinner.getSelectedItemPosition();
            topic = (ImgurTopic) adapter.getItem(selectedPosition > -1 ? selectedPosition : 0);
        }

        if (mGallerySwitch.isChecked()) {
            if (TextUtils.isEmpty(title)) {
                mTitleInputLayout.setError(getString(R.string.upload_gallery_no_title));
            } else {
                if (mListener != null) mListener.onUpload(true, title, desc, topic);
            }
        } else {
            if (mListener != null) mListener.onUpload(false, title, desc, topic);
        }
    }
}
