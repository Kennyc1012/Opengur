package com.kenny.openimgur.activities;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.kenny.openimgur.R;
import com.kenny.openimgur.fragments.SettingsFragment;
import com.kenny.snackbar.SnackBar;

/**
 * Created by kcampagna on 6/30/14.
 */
public class SettingsActivity extends BaseActivity {
    public static final int REQUEST_CODE = 200;

    public static final String NSFW_KEY = "allowNSFW";

    public static final String THEME_KEY = "app_theme";

    public static final String CACHE_SIZE_KEY = "cacheSize";

    public static final String CURRENT_CACHE_SIZE_KEY = "currentCacheSize";

    public static final String KEY_ADB = "adb";

    public static final String KEY_CACHE_LOC = "cacheLoc";

    public static final String CACHE_SIZE_UNLIMITED = "unlimited";

    public static final String CACHE_SIZE_256MB = "256";

    public static final String CACHE_SIZE_512MB = "512";

    public static final String CACHE_SIZE_1GB = "1024";

    public static final String CACHE_SIZE_2GB = "2048";

    public static final String CACHE_LOC_INTERNAL = "internal";

    public static final String CACHE_LOC_EXTERNAL = "external";

    public static final String KEY_CRASHLYTICS = "crashlytics";

    public static final String KEY_NSFW_THUMBNAILS = "NSFWThumbnails";

    public static final String KEY_DARK_THEME = "darkTheme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getFragmentManager().beginTransaction().add(R.id.container, SettingsFragment.createInstance()).commit();
        getSupportActionBar().setTitle(R.string.action_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        SnackBar.cancelSnackBars(this);
        super.onDestroy();
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    public void recreate() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) getFragmentManager().beginTransaction().remove(fragment).commit();
        super.recreate();
    }
}
