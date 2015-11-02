package com.kenny.openimgur.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.OpengurApp;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * Created by kcampagna on 8/24/14.
 */
public class LoadingDialogFragment extends DialogFragment {
    private static final String KEY_MESSAGE = "message";

    private static final String KEY_CANCELABLE = "cancelable";

    @Bind(R.id.message)
    TextView mMessage;

    @Bind(R.id.circleOne)
    View mCircleOne;

    @Bind(R.id.circleTwo)
    View mCircleTwo;

    @Bind(R.id.circleThree)
    View mCircleThree;

    private AnimatorSet set;

    public static LoadingDialogFragment createInstance(@StringRes int message, boolean cancelable) {
        LoadingDialogFragment fragment = new LoadingDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_MESSAGE, message);
        args.putBoolean(KEY_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_dialog, container, false);
    }

    @Override
    public void onDestroyView() {
        ButterKnife.unbind(this);

        if (set != null) {
            set.cancel();
            set = null;
        }

        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        CardView cardView = (CardView) view;
        ImgurTheme theme = OpengurApp.getInstance(getActivity()).getImgurTheme();
        cardView.setCardBackgroundColor(getResources().getColor(theme.isDarkTheme ? R.color.bg_dark : R.color.bg_light));
        setCancelable(getArguments().getBoolean(KEY_CANCELABLE, true));
        mMessage.setText(getArguments().getInt(KEY_MESSAGE));
        set = new AnimatorSet();
        set.playSequentially(
                buildAnimation(mCircleOne),
                buildAnimation(mCircleTwo),
                buildAnimation(mCircleThree)
        );

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (set != null) {
                    set.start();
                }
            }
        });

        set.setDuration(400L).start();
    }

    private Animator buildAnimation(View view) {
        Animator animator = AnimatorInflater.loadAnimator(getActivity(), R.animator.loading_animation);
        animator.setTarget(view);
        return animator;
    }
}
