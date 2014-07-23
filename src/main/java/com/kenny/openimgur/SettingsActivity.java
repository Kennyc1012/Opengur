package com.kenny.openimgur;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.FileUtil;

/**
 * Created by kcampagna on 6/30/14.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    public static final String CACHE_SIZE_KEY = "cacheSize";

    public static final String CURRENT_CACHE_SIZE_KEY = "currentCacheSize";

    public static final String THUMBNAIL_QUALITY_KEY = "thumbnailQuality";

    public static final int CACHE_SIZE_128_MB = 0;

    public static final int CACHE_SIZE_256_MB = 1;

    public static final int CACHE_SIZE_512_MB = 2;

    public static final int CACHE_SIZE_1_GB = 3;

    public static final int THUMBNAIL_QUALITY_LOW = 0;

    public static final int THUMBNAIL_QUALITY_MEDIUM = 1;

    public static final int THUMBNAIL_QUALITY_HIGH = 2;

    private OpenImgurApp mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        findPreference(CACHE_SIZE_KEY).setOnPreferenceChangeListener(this);
        findPreference(THUMBNAIL_QUALITY_KEY).setOnPreferenceChangeListener(this);

        findPreference(CURRENT_CACHE_SIZE_KEY).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                builder.setTitle(R.string.clear_cache).setMessage(R.string.clear_cache_message);
                builder.setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mApp.getImageLoader().clearDiskCache();
                        long cacheSize = FileUtil.getDirectorySize(mApp.getCacheDir());
                        preference.setSummary(FileUtil.humanReadableByteCount(cacheSize, false));
                    }
                }).show();
                return true;
            }
        });

        getActionBar().setDisplayShowHomeEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
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
    protected void onResume() {
        super.onResume();
        mApp = ((OpenImgurApp) getApplication());
        long cacheSize = FileUtil.getDirectorySize(mApp.getImageLoader().getDiskCache().getDirectory());
        findPreference(CURRENT_CACHE_SIZE_KEY).setSummary(FileUtil.humanReadableByteCount(cacheSize, false));

        int cacheLimit = Integer.parseInt(mApp.getPreferences().getString(CACHE_SIZE_KEY, String.valueOf(CACHE_SIZE_256_MB)));
        String[] cacheLimits = getResources().getStringArray(R.array.pref_cache_size_items);
        findPreference(CACHE_SIZE_KEY).setSummary(cacheLimits[cacheLimit]);

        int quality = Integer.parseInt(mApp.getPreferences().getString(THUMBNAIL_QUALITY_KEY, String.valueOf(THUMBNAIL_QUALITY_LOW)));
        String[] qualities = getResources().getStringArray(R.array.pref_thumbnail_quality);
        findPreference(THUMBNAIL_QUALITY_KEY).setSummary(qualities[quality]);

    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String key = preference.getKey();

        if (key.equals(CACHE_SIZE_KEY)) {
            int cacheLimit = Integer.parseInt(o.toString());
            String[] cacheLimits = getResources().getStringArray(R.array.pref_cache_size_items);
            preference.setSummary(cacheLimits[cacheLimit]);
            return true;
        } else if (key.equals(THUMBNAIL_QUALITY_KEY)) {
            int quality = Integer.parseInt(o.toString());
            String[] qualities = getResources().getStringArray(R.array.pref_thumbnail_quality);
            preference.setSummary(qualities[quality]);
            return true;
        }

        return false;
    }
}
