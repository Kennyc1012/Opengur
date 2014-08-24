package com.kenny.openimgur.fragments;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.TextViewRoboto;

/**
 * Created by kcampagna on 8/24/14.
 */
public class LoadingDialogFragment extends DialogFragment {
    private static final String KEY_MESSAGE = "message";

    public static LoadingDialogFragment createInstance(@StringRes int message) {
        LoadingDialogFragment fragment = new LoadingDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.loading_dialog, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        View c1 = view.findViewById(R.id.circleOne);
        View c2 = view.findViewById(R.id.circleTwo);
        View c3 = view.findViewById(R.id.circleThree);
        TextViewRoboto message = (TextViewRoboto) view.findViewById(R.id.message);
        message.setText(getArguments().getInt(KEY_MESSAGE));
        final AnimatorSet set = new AnimatorSet();
        set.playSequentially(
                buildAnimation(c1),
                buildAnimation(c2),
                buildAnimation(c3)
        );

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                set.start();
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
