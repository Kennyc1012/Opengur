package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 6/30/14.
 */
public class SettingsActivity extends BaseActivity {
    private static final String KEY_IS_EXPERIMENTAL = "isExperimental";

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

    public static final String KEY_CONFIRM_EXIT = "confirmExit";

    public static final String KEY_THREAD_SIZE = "threadSize";

    public static final String THREAD_SIZE_5 = "5";

    public static final String THREAD_SIZE_7 = "7";

    public static final String THREAD_SIZE_10 = "10";

    public static final String THREAD_SIZE_12 = "12";

    public static final String KEY_TAGS = "autoLoadTags";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isExperimental = getIntent().getBooleanExtra(KEY_IS_EXPERIMENTAL, false);
        getSupportActionBar().setTitle(isExperimental ? R.string.pref_experimental_settings : R.string.action_settings);
        setContentView(isExperimental ? R.layout.activity_settings_experimental : R.layout.activity_settings);

    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    public static Intent createIntent(Context context, boolean isExperimental) {
        return createIntent(context).putExtra(KEY_IS_EXPERIMENTAL, isExperimental);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Settings_Theme_Dark : R.style.Settings_Theme_Light;
    }
}
