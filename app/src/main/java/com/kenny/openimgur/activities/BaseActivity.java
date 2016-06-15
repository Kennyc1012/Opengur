package com.kenny.openimgur.activities;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kennyc.bottomsheet.BottomSheet;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 6/21/14.
 */
abstract public class BaseActivity extends AppCompatActivity {
    public final String TAG = getClass().getSimpleName();

    public OpengurApp app;

    public ImgurUser user;

    public ImgurTheme theme;

    private boolean mIsLandscape = false;

    private boolean mIsTablet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtil.v(TAG, "onCreate");
        app = OpengurApp.getInstance(getApplicationContext());
        onSetStyle(app.getImgurTheme());
        super.onCreate(savedInstanceState);
        ActionBar ab = getSupportActionBar();

        if (ab != null) {
            ab.setTitle(null);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
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
        ButterKnife.bind(this);
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
                supportFinishAfterTransition();
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
        try {
            getFragmentManager().beginTransaction().add(fragment, title).commit();
        } catch (IllegalStateException ex) {
            LogUtil.e(TAG, "Unable to show dialog fragment", ex);
        }
    }

    /**
     * Dismisses the dialog fragment with the given title
     *
     * @param title The title of the Dialog Fragment
     */
    public void dismissDialogFragment(String title) {
        Fragment fragment = getFragmentManager().findFragmentByTag(title);

        if (fragment != null && fragment instanceof DialogFragment) {
            ((DialogFragment) fragment).dismissAllowingStateLoss();
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
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setStatusBarColor(int color) {
        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
            getWindow().setStatusBarColor(color);
        }
    }

    /**
     * Sets the color of the status bar, only for SDK 21+ devices
     *
     * @param color
     */
    public void setStatusBarColorResource(@ColorRes int color) {
        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
            setStatusBarColor(getResources().getColor(color));
        }
    }

    /**
     * Sets the Task Description for Lollipop devices
     *
     * @param title
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    protected void updateTaskDescription(String title) {
        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {
            Resources res = getResources();
            if (TextUtils.isEmpty(title)) title = res.getString(R.string.app_name);
            Bitmap icon = BitmapFactory.decodeResource(res, R.drawable.ic_launcher);
            int color = res.getColor(theme.primaryColor);
            ActivityManager.TaskDescription description = new ActivityManager.TaskDescription(title, icon, color);
            setTaskDescription(description);
        }
    }

    /**
     * Returns if the activity is able to complete a FragmentTransaction based on its lifecycle phase
     *
     * @return
     */
    protected boolean canDoFragmentTransaction() {
        return !isDestroyed() && !isFinishing() && !isChangingConfigurations();
    }

    /**
     * Called before the super call on {@link #onCreate(Bundle)} so the style can be set for the activity
     */
    private void onSetStyle(ImgurTheme imgurTheme) {
        theme = imgurTheme;
        setTheme(getStyleRes());
        theme.applyTheme(getTheme());
        updateTaskDescription(null);
    }

    /**
     * Returns the style resource for the activity.
     *
     * @return
     */
    @StyleRes
    protected abstract int getStyleRes();

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
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, getString(title)));
            } else {
                Toast.makeText(getApplicationContext(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
            }
        } else {
            BottomSheet shareDialog = BottomSheet.createShareBottomSheet(this, intent, title, true);
            if (shareDialog != null) {
                shareDialog.show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.cant_launch_intent, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
