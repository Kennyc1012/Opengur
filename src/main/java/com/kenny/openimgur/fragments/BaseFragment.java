package com.kenny.openimgur.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.BaseActivity;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kennyc.bottomsheet.BottomSheet;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by kcampagna on 9/5/14.
 */
abstract public class BaseFragment extends Fragment {
    public final String TAG = getClass().getSimpleName();

    public OpengurApp app;

    public ImgurUser user;

    public ImgurTheme theme;

    private Unbinder mUnbinder;

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
        mUnbinder = ButterKnife.bind(this, view);
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
        if (mUnbinder != null) mUnbinder.unbind();
        super.onDestroyView();
    }

    protected boolean isTablet() {
        if (getActivity() instanceof BaseActivity) {
            return ((BaseActivity) getActivity()).isTablet();
        }

        return getResources().getBoolean(R.bool.is_tablet);
    }

    /**
     * Returns if the current API level is at least the supplied level
     *
     * @param apiLevel
     * @return
     */
    protected boolean isApiLevel(int apiLevel) {
        return Build.VERSION.SDK_INT >= apiLevel;
    }

    /**
     * Shares a given {@link Intent} with the system. If Lollipop or higher, the built in sharing system will be used. If lower,
     * {@link BottomSheet} will be used to resemble the style. If the system is unable to handle the intent, an error will be displayed
     *
     * @param intent The intent to share
     * @param title  Title of the share intent
     */
    protected void share(@NonNull Intent intent, @StringRes int title) {
        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, getString(title)));
            } else {
                Toast.makeText(getActivity(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
            }
        } else {
            BottomSheet shareDialog = BottomSheet.createShareBottomSheet(getActivity(), intent, title, true);
            if (shareDialog != null) {
                shareDialog.show();
            } else {
                Toast.makeText(getActivity(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
