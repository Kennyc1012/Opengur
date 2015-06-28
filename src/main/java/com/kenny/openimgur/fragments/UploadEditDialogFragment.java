package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.PhotoUploadListener;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.util.ImageUtil;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by Kenny-PC on 6/28/2015.
 */
public class UploadEditDialogFragment extends DialogFragment {
    public static final String TAG = UploadEditDialogFragment.class.getSimpleName();
    private static final String KEY_UPLOAD = "upload";

    private PhotoUploadListener mListener;
    private Upload mUpload;

    @InjectView(R.id.title)
    EditText mTitle;

    @InjectView(R.id.desc)
    EditText mDesc;

    @InjectView(R.id.image)
    ImageView mImage;

    public static UploadEditDialogFragment createInstance(Upload upload) {
        UploadEditDialogFragment fragment = new UploadEditDialogFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(KEY_UPLOAD, upload);
        fragment.setArguments(args);
        return fragment;
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
    public void onStart() {
        super.onStart();
        if (getDialog() == null) return;
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setStyle(DialogFragment.STYLE_NO_TITLE, OpengurApp.getInstance(getActivity()).getImgurTheme().getDialogTheme());
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload_edit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
        Bundle args = getArguments();

        if (args == null || !args.containsKey(KEY_UPLOAD)) {
            throw new IllegalStateException("No arguments passed");
        }

        mUpload = args.getParcelable(KEY_UPLOAD);
        mTitle.setText(mUpload.getTitle());
        mDesc.setText(mUpload.getDescription());
        String photoLocation = mUpload.isLink() ? mUpload.getLocation() : "file://" + mUpload.getLocation();

        OpengurApp.getInstance(getActivity())
                .getImageLoader()
                .displayImage(photoLocation, mImage, ImageUtil.getDisplayOptionsForPhotoPicker().build());
    }

    @Override
    public void onDestroyView() {
        ButterKnife.reset(this);
        super.onDestroyView();
    }

    @OnClick({R.id.cancel, R.id.okay})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okay:
                mUpload.setTitle(mTitle.getText().toString());
                mUpload.setDescription(mDesc.getText().toString());
                if (mListener != null) mListener.onItemEdited(mUpload);
                // Intentional fall through
            case R.id.cancel:
                dismissAllowingStateLoss();
                break;
        }
    }
}
