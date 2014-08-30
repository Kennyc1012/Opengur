package com.kenny.openimgur;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.SnackBar;
import com.readystatesoftware.systembartint.SystemBarTintManager;

/**
 * Created by kcampagna on 6/21/14.
 */
public class BaseActivity extends Activity {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    public ImgurUser user;

    private boolean mShouldTint = true;

    private boolean mIsActionBarShowing = true;

    private boolean mIsLandscape = false;

    private boolean mShouldShowHome = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        ab.setTitle(null);
        ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
        mIsLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        app = OpenImgurApp.getInstance(getApplicationContext());
        user = app.getUser();

        if (mShouldTint) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            // enable status bar tint
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setTintColor(getResources().getColor(R.color.status_bar_tint));
        }

        if (mShouldShowHome) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
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
        SnackBar.cancelSnackBars(this);
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

    /**
     * Sets if the actionbar will display the back arrow in the action bar
     *
     * @param display
     */
    public void setShouldDisplayHome(boolean display) {
        mShouldShowHome = display;
    }

    /**
     * Sets the action bars view
     *
     * @param view
     */
    public void setActionBarView(View view) {
        getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getActionBar().setCustomView(view);
    }

    /**
     * Shows or hides the actionbar
     *
     * @param shouldShow If the actionbar should be shown
     */
    public void setActionBarVisibility(boolean shouldShow) {
        if (shouldShow && !mIsActionBarShowing) {
            mIsActionBarShowing = true;
            getActionBar().show();
        } else if (!shouldShow && mIsActionBarShowing) {
            mIsActionBarShowing = false;
            getActionBar().hide();
        }
    }

    /**
     * Returns if the current activity is in landscape orientation
     *
     * @return
     */
    public boolean isLandscape() {
        return mIsLandscape;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
