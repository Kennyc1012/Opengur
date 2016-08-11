package com.kenny.openimgur.fragments;

import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Created by Kenny-PC on 6/28/2015.
 */
public class UploadLinkDialogFragment extends DialogFragment implements TextWatcher {
    public static final String TAG = UploadLinkDialogFragment.class.getSimpleName();

    private static final String KEY_LINK = "link";

    private static final long TEXT_DELAY = DateUtils.SECOND_IN_MILLIS;

    @BindView(R.id.link)
    EditText mLink;

    @BindView(R.id.loadingContainer)
    View mLoadingContainer;

    @BindView(R.id.linkValidation)
    TextView mLinkValidation;

    @BindView(R.id.add)
    Button mAddButton;

    @BindView(R.id.loadingIndicator)
    ProgressBar mLoadingIndicator;

    private Unbinder mUnbinder;

    public static DialogFragment newInstance(@Nullable String link) {
        UploadLinkDialogFragment fragment = new UploadLinkDialogFragment();

        if (!TextUtils.isEmpty(link)) {
            Bundle args = new Bundle(1);
            args.putString(KEY_LINK, link);
            fragment.setArguments(args);
        }

        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        if (getDialog() == null) {
            setShowsDialog(false);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() == null) return;
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.fragment_link_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUnbinder = ButterKnife.bind(this, view);
        mLink.addTextChangedListener(this);
        Bundle args = getArguments();

        if (args != null && args.containsKey(KEY_LINK)) {
            mLink.setText(args.getString(KEY_LINK, null));
        }
    }

    @Override
    public void onDestroyView() {
        if (mUnbinder != null) mUnbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // NOOP
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mAddButton.setEnabled(false);

        if (!TextUtils.isEmpty(s.toString())) {
            mHandler.removeMessages(ImgurHandler.MESSAGE_SEARCH_URL);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(ImgurHandler.MESSAGE_SEARCH_URL, s.toString()), TEXT_DELAY);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        // NOOP
    }

    @OnClick({R.id.cancel, R.id.add})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.add:
                if (getParentFragment() instanceof UploadFragment) ((UploadFragment) getParentFragment()).onLinkAdded(mLink.getText().toString());
                // Intentional fall through
            case R.id.cancel:
                dismissAllowingStateLoss();
                break;
        }
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_SEARCH_URL:
                    ImageUtil.getImageLoader(getActivity()).loadImage((String) msg.obj, new SimpleImageLoadingListener() {

                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            if (!isAdded()) return;
                            mLoadingContainer.setVisibility(View.VISIBLE);
                            mLoadingIndicator.setVisibility(View.VISIBLE);
                            mLinkValidation.setText(R.string.upload_checking_link);
                        }

                        @Override
                        public void onLoadingFailed(String s, View view, FailReason failReason) {
                            if (!isAdded()) return;
                            mAddButton.setEnabled(false);
                            mLoadingIndicator.setVisibility(View.INVISIBLE);
                            mLinkValidation.setText(R.string.upload_invalid_link);
                        }

                        @Override
                        public void onLoadingComplete(String url, View view, Bitmap bitmap) {
                            if (!isAdded()) return;
                            mAddButton.setEnabled(true);
                            mLoadingIndicator.setVisibility(View.INVISIBLE);
                            mLinkValidation.setText(R.string.upload_valid_link);
                        }

                        @Override
                        public void onLoadingCancelled(String s, View view) {
                            if (!isAdded()) return;
                            mAddButton.setEnabled(false);
                            mLoadingIndicator.setVisibility(View.INVISIBLE);
                            mLinkValidation.setText(R.string.upload_invalid_link);
                        }
                    });
                    break;
            }
        }
    };
}
