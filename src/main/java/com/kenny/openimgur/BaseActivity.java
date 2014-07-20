package com.kenny.openimgur;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.kenny.openimgur.classes.OpenImgurApp;

/**
 * Created by kcampagna on 6/21/14.
 */
public class BaseActivity extends Activity {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        app = (OpenImgurApp) getApplication();
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
}
