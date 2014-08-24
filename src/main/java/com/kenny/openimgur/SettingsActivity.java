package com.kenny.openimgur;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebView;

import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.fragments.LoadingDialogFragment;
import com.kenny.openimgur.fragments.PopupDialogViewBuilder;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.SqlHelper;

/**
 * Created by kcampagna on 6/30/14.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    public static final String REDDIT_SEARCH_KEY = "subreddit";

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
        findPreference(REDDIT_SEARCH_KEY).setOnPreferenceClickListener(this);
        findPreference(CURRENT_CACHE_SIZE_KEY).setOnPreferenceClickListener(this);
        findPreference("licenses").setOnPreferenceClickListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ActionBar ab = getActionBar();
        ab.setDisplayShowHomeEnabled(true);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
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
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            findPreference("version").setSummary(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        if (preference.getKey().equals(CURRENT_CACHE_SIZE_KEY)) {
            final AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this).create();
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

            dialog.setView(new PopupDialogViewBuilder(getApplicationContext()).setTitle(R.string.clear_cache)
                    .setMessage(R.string.clear_cache_message).setNegativeButton(R.string.cancel, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                        }
                    }).setPositiveButton(R.string.yes, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.dismiss();
                            new DeleteCacheTask().execute();
                        }
                    }).build());
            dialog.show();
            return true;
        } else if (preference.getKey().equals(REDDIT_SEARCH_KEY)) {
            new SqlHelper(getApplicationContext()).deleteAllSubRedditSearches();
            return true;
        } else if (preference.getKey().equals("licenses")) {
            AlertDialog dialog = new AlertDialog.Builder(SettingsActivity.this)
                    .setNegativeButton(R.string.dismiss, null).create();
            dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            WebView webView = new WebView(this);
            webView.loadUrl("file:///android_asset/licenses.html");
            dialog.setView(webView);
            dialog.show();
            return true;
        }

        return false;
    }

    private class DeleteCacheTask extends AsyncTask<Void, Void, Long> {
        LoadingDialogFragment fragment;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            fragment = LoadingDialogFragment.createInstance(R.string.one_moment);
            getFragmentManager().beginTransaction().add(fragment, "loading").commit();
        }

        @Override
        protected Long doInBackground(Void... voids) {
            mApp.getImageLoader().clearDiskCache();
            return FileUtil.getDirectorySize(mApp.getCacheDir());
        }

        @Override
        protected void onPostExecute(Long cacheSize) {
            findPreference(CURRENT_CACHE_SIZE_KEY).setSummary(FileUtil.humanReadableByteCount(cacheSize, false));

            if (fragment != null && fragment.isAdded()) {
                fragment.dismissAllowingStateLoss();
                fragment = null;
            }
        }
    }
}
