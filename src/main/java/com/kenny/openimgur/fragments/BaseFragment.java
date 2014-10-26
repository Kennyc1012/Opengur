package com.kenny.openimgur.fragments;

import android.app.Fragment;
import android.os.Bundle;

import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 9/5/14.
 */
abstract public class BaseFragment extends Fragment {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    public ImgurUser user;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtil.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        app = OpenImgurApp.getInstance(getActivity());
        user = app.getUser();
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
        super.onDestroyView();
    }
}
