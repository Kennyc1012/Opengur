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

    private static final String KEY_CANCELABLE = "cancelable";

    private View c1, c2, c3;

    private TextViewRoboto message;

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
        c1 = null;
        c2 = null;
        c3 = null;
        message = null;
        set.cancel();
        set = null;
        super.onDestroyView();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setCancelable(getArguments().getBoolean(KEY_CANCELABLE, true));
        c1 = view.findViewById(R.id.circleOne);
        c2 = view.findViewById(R.id.circleTwo);
        c3 = view.findViewById(R.id.circleThree);
        message = (TextViewRoboto) view.findViewById(R.id.message);
        message.setText(getArguments().getInt(KEY_MESSAGE));
        set = new AnimatorSet();
        set.playSequentially(
                buildAnimation(c1),
                buildAnimation(c2),
                buildAnimation(c3)
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
