package com.kenny.openimgur.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.view.View;

import com.kenny.openimgur.activities.BaseActivity;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 9/5/14.
 */
abstract public class BaseFragment extends Fragment {
    public final String TAG = getClass().getSimpleName();

    public OpengurApp app;

    public ImgurUser user;

    public ImgurTheme theme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        app = OpengurApp.getInstance(getActivity());
        user = app.getUser();
        theme = app.getImgurTheme();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        LogUtil.v(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onResume() {
        LogUtil.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        LogUtil.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        LogUtil.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        LogUtil.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        LogUtil.v(TAG, "onDestroyView");
        ButterKnife.unbind(this);
        super.onDestroyView();
    }

    /**
     * Returns if the fragment is able to complete a FragmentTransaction based on its lifecycle phase
     *
     * @return
     */
    protected boolean canDoFragmentTransaction() {
        Activity activity = getActivity();
        return activity != null && !activity.isFinishing() && !activity.isChangingConfigurations() && isAdded() && !isRemoving() && getUserVisibleHint();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void setStatusBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() instanceof BaseActivity) {
            ((BaseActivity) getActivity()).setStatusBarColor(color);
        }
    }

    /**
     * Sets the color of the status bar, only for SDK 21+ devices
     *
     * @param color
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColorResource(@ColorRes int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && getActivity() instanceof BaseActivity) {
            setStatusBarColor(getResources().getColor(color));
        }
    }
}
