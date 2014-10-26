package com.kenny.openimgur;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.kenny.openimgur.fragments.SettingsFragment;
import com.kenny.snackbar.SnackBar;

/**
 * Created by kcampagna on 6/30/14.
 */
public class SettingsActivity extends BaseActivity {
    public static final String NSFW_KEY = "allowNSFW";

    public static final String CACHE_LIFE_KEY = "cacheLife";

    public static final String CURRENT_CACHE_SIZE_KEY = "currentCacheSize";

    public static final String THUMBNAIL_QUALITY_KEY = "thumbnailQuality";

    public static final String THUMBNAIL_QUALITY_LOW = "low";

    public static final String THUMBNAIL_QUALITY_MEDIUM = "medium";

    public static final String THUMBNAIL_QUALITY_HIGH = "high";

    public static final String CACHE_LIFE_NEVER = "never";

    public static final String CACHE_LIFE_EXIT = "exit";

    public static final String CACHE_LIFE_3 = "three";

    public static final String CACHE_LIFE_7 = "seven";

    public static final String CACHE_LIFE_14 = "fourteen";

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
}
