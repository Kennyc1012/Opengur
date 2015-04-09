package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.View;

import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.LogUtil;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 9/5/14.
 */
abstract public class BaseFragment extends Fragment {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    public ImgurUser user;

    public ImgurTheme theme;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        app = OpenImgurApp.getInstance(getActivity());
        user = app.getUser();
        theme = app.getImgurTheme();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        LogUtil.v(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);
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
        ButterKnife.reset(this);
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
}
