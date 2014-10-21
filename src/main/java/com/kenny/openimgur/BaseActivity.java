package com.kenny.openimgur;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;
import com.readystatesoftware.systembartint.SystemBarTintManager;

/**
 * Created by kcampagna on 6/21/14.
 */
public class BaseActivity extends ActionBarActivity {
    public final String TAG = getClass().getSimpleName();

    public OpenImgurApp app;

    public ImgurUser user;

    private boolean mShouldTint = true;

    private boolean mIsActionBarShowing = true;

    private boolean mIsLandscape = false;

    private boolean mShouldShowHome = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.v(TAG, "onCreate");
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
        app = OpenImgurApp.getInstance(getApplicationContext());
        user = app.getUser();

        if (mShouldTint) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setTintColor(getResources().getColor(R.color.status_bar_tint));
        }
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
        super.onPause();
        SnackBar.cancelSnackBars(this);
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
     * Sets if the activity should allow for status bar tinting, will only affect devices 4.4+
     *
     * @param tint
     */
    public void setShouldTint(boolean tint) {
        mShouldTint = tint;
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
}
