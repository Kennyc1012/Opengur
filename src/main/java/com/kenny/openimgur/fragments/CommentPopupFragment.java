package com.kenny.openimgur.fragments;

import android.animation.ObjectAnimator;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.snackbar.SnackBar;
import com.squareup.okhttp.FormEncodingBuilder;

import de.greenrobot.event.EventBus;

/**
 * Created by kcampagna on 8/22/14.
 */
public class CommentPopupFragment extends DialogFragment implements View.OnClickListener {
    private static final String KEY_GALLERY_ID = "gallery_id";

    private static final String KEY_PARENT_ID = "parent_id";

    private EditText mEditText;

    private TextViewRoboto mRemaining;

    private String mGalleryId;

    private String mParentId;

    /**
     * Creates an instance of CommentPopupFragment
     *
     * @param galleryId The id of the image or gallery being commented on
     * @param parent    Optional parent comment id for a reply
     * @return
     */
    public static CommentPopupFragment createInstance(@NonNull String galleryId, @Nullable String parent) {
        CommentPopupFragment fragment = new CommentPopupFragment();
        Bundle args = new Bundle();
        args.putString(KEY_GALLERY_ID, galleryId);

        if (!TextUtils.isEmpty(parent)) {
            args.putString(KEY_PARENT_ID, parent);
        }

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
    public void onDestroyView() {
        mEditText = null;
        mRemaining = null;
        super.onDestroyView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_Dialog);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return inflater.inflate(R.layout.comment_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mEditText = (EditText) view.findViewById(R.id.comment);
        mRemaining = (TextViewRoboto) view.findViewById(R.id.remainingCharacters);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.post).setOnClickListener(this);
        Bundle args = getArguments();

        if (args == null || !args.containsKey(KEY_GALLERY_ID)) {
            dismiss();
            SnackBar.show(getActivity(), R.string.error_generic);
            return;
        }

        mGalleryId = args.getString(KEY_GALLERY_ID);
        mParentId = args.getString(KEY_PARENT_ID, null);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // NOOP
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                mRemaining.setText(String.valueOf(140 - charSequence.length()));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // NOOP
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancel:
                dismiss();
                break;

            case R.id.post:
                String comment = mEditText.getText().toString();

                if (!TextUtils.isEmpty(comment)) {
                    // This event posting will trigger the Loading Dialog to be shown in the ViewActivity
                    EventBus.getDefault().post(new ImgurBusEvent(null, ImgurBusEvent.EventType.COMMENT_POSTING, null, null));
                    String url = TextUtils.isEmpty(mParentId) ? String.format(Endpoints.COMMENT.getUrl(), mGalleryId) :
                            String.format(Endpoints.COMMENT_REPLY.getUrl(), mGalleryId, mParentId);
                    ApiClient client = new ApiClient(url, ApiClient.HttpRequest.POST);
                    client.doWork(ImgurBusEvent.EventType.COMMENTS, null, new FormEncodingBuilder().add("comment", comment).build());
                    dismiss();
                } else {
                    // Shake the edit text to show that they have not enetered any text
                    ObjectAnimator.ofFloat(mEditText, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0).setDuration(750L).start();
                }
                break;
        }
    }
}
