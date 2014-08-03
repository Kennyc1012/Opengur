package com.kenny.openimgur;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
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

    public static final String CACHE_SIZE_128_MB = "128";

    public static final String CACHE_SIZE_256_MB = "256";

    public static final String CACHE_SIZE_512_MB = "512";

    public static final String CACHE_SIZE_1_GB = "1024";

    public static final String THUMBNAIL_QUALITY_LOW = "low";

    public static final String THUMBNAIL_QUALITY_MEDIUM = "medium";

    public static final String THUMBNAIL_QUALITY_HIGH = "high";

    private OpenImgurApp mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = ((OpenImgurApp) getApplication());
        addPreferencesFromResource(R.xml.settings);
        bindPreference(findPreference(CACHE_SIZE_KEY));
        bindPreference(findPreference(THUMBNAIL_QUALITY_KEY));

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
        long cacheSize = FileUtil.getDirectorySize(mApp.getImageLoader().getDiskCache().getDirectory());
        findPreference(CURRENT_CACHE_SIZE_KEY).setSummary(FileUtil.humanReadableByteCount(cacheSize, false));
    }

    private void bindPreference(Preference preference) {
        preference.setOnPreferenceChangeListener(this);

        onPreferenceChange(preference, mApp.getPreferences().getString(preference.getKey(), ""));
    }

    public static Intent createIntent(Context context) {
        return new Intent(context, SettingsActivity.class);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(o.toString());
            if (prefIndex >= 0) {
                preference.setSummary(listPreference.getEntries()[prefIndex]);
            }
            return true;
        } else {
            // Only have list preferences so far
        }

        return false;
    }
}
