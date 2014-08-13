package com.kenny.openimgur;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.readystatesoftware.systembartint.SystemBarTintManager;

/**
 * Created by kcampagna on 6/21/14.
 */
public class BaseActivity extends Activity {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    public ImgurUser user;

    private boolean mShouldTint = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        app = OpenImgurApp.getInstance();
        user = app.getUser();

        if (mShouldTint) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            // enable status bar tint
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setTintColor(getResources().getColor(R.color.status_bar_tint));
        }
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.v(TAG, "onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * Sets if the activity should allow for status bar tinting, will only affect devices 4.4+
     *
     * @param tint
     */
    public void setShouldTint(boolean tint) {
        mShouldTint = tint;
    }
}
