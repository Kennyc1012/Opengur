package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.adapters.TopicsAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.UploadListener;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.SqlHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Kenny-PC on 7/4/2015.
 */
public class UploadInfoFragment extends BaseFragment {
    @BindView(R.id.title)
    EditText mTitle;

    @BindView(R.id.desc)
    EditText mDesc;

    @BindView(R.id.titleInputLayout)
    TextInputLayout mTitleInputLayout;

    @BindView(R.id.topicSpinner)
    Spinner mTopicSpinner;

    @BindView(R.id.gallerySwitch)
    CheckBox mGallerySwitch;

    @BindView(R.id.topicHeader)
    View mTopicHeader;

    private UploadListener mListener;

    public static UploadInfoFragment newInstance() {
        return new UploadInfoFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof UploadListener) mListener = (UploadListener) activity;
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
        checkForTopics();
    }

    @OnCheckedChanged(R.id.gallerySwitch)
    public void onCheckChanged(boolean checked) {
        if (checked && user == null) {
            mGallerySwitch.setChecked(false);
            Snackbar.make(getActivity().findViewById(android.R.id.content), R.string.upload_gallery_no_user, Snackbar.LENGTH_LONG).show();
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

    /**
     * Checks if we have cached topics to display for the info fragment
     */
    private void checkForTopics() {
        List<ImgurTopic> topics = SqlHelper.getInstance(getActivity()).getTopics();

        if (topics.isEmpty()) {
            LogUtil.v(TAG, "No topics found, fetching");
            ApiClient.getService().getDefaultTopics().enqueue(new Callback<TopicResponse>() {
                @Override
                public void onResponse(Call<TopicResponse> call, Response<TopicResponse> response) {
                    if (isAdded() && response != null && response.body() != null) {
                        SqlHelper sql = SqlHelper.getInstance(getActivity());
                        sql.addTopics(response.body().data);
                        List<ImgurTopic> topics = sql.getTopics();
                        mTopicSpinner.setAdapter(new TopicsAdapter(getActivity(), topics));
                    }
                }

                @Override
                public void onFailure(Call<TopicResponse> call, Throwable t) {
                    LogUtil.e(TAG, "Failed to receive topics", t);
                }
            });
        } else {
            LogUtil.v(TAG, "Topics in database");
            mTopicSpinner.setAdapter(new TopicsAdapter(getActivity(), topics));
        }
    }
}
