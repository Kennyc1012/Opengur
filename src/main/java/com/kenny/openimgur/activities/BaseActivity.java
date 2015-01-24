package com.kenny.openimgur.activities;

import android.annotation.SuppressLint;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 6/21/14.
 */
abstract public class BaseActivity extends ActionBarActivity {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    public ImgurUser user;

    public ImgurTheme theme;

    private boolean mIsActionBarShowing = true;

    private boolean mIsLandscape = false;

    private boolean mShouldShowHome = true;

    private boolean mIsTablet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.v(TAG, "onCreate");
        app = OpenImgurApp.getInstance(getApplicationContext());
        theme = app.getImgurTheme();
        theme.applyTheme(getTheme());
        super.onCreate(savedInstanceState);
        ActionBar ab = getSupportActionBar();

        if (ab != null) {
            ab.setTitle(null);

            if (mShouldShowHome) {
                ab.setDisplayHomeAsUpEnabled(true);
                ab.setDisplayShowHomeEnabled(true);
            }
        } else {
            LogUtil.w(TAG, "Action bar is null. Unable to set defaults");
        }

        mIsLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        user = app.getUser();
        mIsTablet = getResources().getBoolean(R.bool.is_tablet);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        ButterKnife.inject(this);
    }

    @Override
    protected void onStart() {
        LogUtil.v(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        LogUtil.v(TAG, "onRestart");
        super.onRestart();
    }

    @Override
    protected void onResume() {
        LogUtil.v(TAG, "onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        LogUtil.v(TAG, "onPause");
        SnackBar.cancelSnackBars(this);
        super.onPause();
    }

    @Override
    protected void onStop() {
        LogUtil.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtil.v(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * Shows or hides the actionbar
     *
     * @param shouldShow If the actionbar should be shown
     */
    public void setActionBarVisibility(boolean shouldShow) {
        if (shouldShow && !mIsActionBarShowing) {
            mIsActionBarShowing = true;
            getSupportActionBar().show();
        } else if (!shouldShow && mIsActionBarShowing) {
            mIsActionBarShowing = false;
            getSupportActionBar().hide();
        }
    }

    /**
     * Shows or hides the actionbar
     *
     * @param toolbar    The toolbar that is taking the place of the actionbar
     * @param shouldShow If the actionbar should be shown
     * @return The visibility of the action Bar
     */
    protected boolean setActionBarVisibility(Toolbar toolbar, boolean shouldShow) {
        if (shouldShow && !mIsActionBarShowing) {
            mIsActionBarShowing = true;
            toolbar.animate().translationY(0);
        } else if (!shouldShow && mIsActionBarShowing) {
            mIsActionBarShowing = false;
            toolbar.animate().translationY(-toolbar.getHeight());
        }

        return mIsActionBarShowing;
    }

    /**
     * Returns if the action bar visibility should change. This does not hide/remove the action bar
     *
     * @param shouldShow If the actionbar should be shown
     * @return
     */
    protected boolean shouldChangeActionBarVisibility(boolean shouldShow) {
        if (shouldShow && !mIsActionBarShowing) {
            mIsActionBarShowing = true;
            return true;
        } else if (!shouldShow && mIsActionBarShowing) {
            mIsActionBarShowing = false;
            return true;
        }

        return false;
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

    /**
     * Shows a Dialog Fragment
     *
     * @param fragment The Dialog Fragment to Display
     * @param title    The title for the Dialog Fragment
     */
    public void showDialogFragment(DialogFragment fragment, String title) {
        getFragmentManager().beginTransaction().add(fragment, title).commit();
    }

    /**
     * Dismisses the dialog fragment with the given title
     *
     * @param title The title of the Dialog Fragment
     */
    public void dismissDialogFragment(String title) {
        Fragment fragment = getFragmentManager().findFragmentByTag(title);

        if (fragment != null && fragment instanceof DialogFragment) {
            ((DialogFragment) fragment).dismiss();
        }
    }

    /**
     * Returns if the current device is a tablet (600dp+ width)
     *
     * @return
     */
    public boolean isTablet() {
        return mIsTablet;
    }

    /**
     * Sets the color of the status bar, only for SDK 21+ devices
     *
     * @param color
     */
    @SuppressLint("NewApi")
    public void setStatusBarColor(int color) {
        if (app.sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(color);
        }
    }
}
