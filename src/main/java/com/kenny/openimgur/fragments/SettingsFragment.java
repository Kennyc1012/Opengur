package com.kenny.openimgur.fragments;

import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.RingtonePreference;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.kenny.openimgur.BuildConfig;
import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.VideoCache;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;

import java.io.File;
import java.lang.ref.WeakReference;

public class SettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    private boolean mFirstLaunch = true;

    public static SettingsFragment createInstance() {
        return new SettingsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindListPreference(findPreference(SettingsActivity.KEY_CACHE_SIZE));
        bindListPreference(findPreference(SettingsActivity.KEY_THEME));
        bindListPreference(findPreference(SettingsActivity.KEY_CACHE_LOC));
        bindListPreference(findPreference(SettingsActivity.KEY_THUMBNAIL_QUALITY));
        bindListPreference(findPreference(SettingsActivity.KEY_NOTIFICATION_FREQUENCY));
        findPreference(SettingsActivity.KEY_CURRENT_CACHE_SIZE).setOnPreferenceClickListener(this);
        findPreference("licenses").setOnPreferenceClickListener(this);
        findPreference("openSource").setOnPreferenceClickListener(this);
        findPreference("redditHistory").setOnPreferenceClickListener(this);
        findPreference("mySubreddits").setOnPreferenceClickListener(this);
        findPreference("gallerySearchHistory").setOnPreferenceClickListener(this);
        findPreference("experimentalSettings").setOnPreferenceClickListener(this);
        findPreference(SettingsActivity.KEY_ADB).setOnPreferenceChangeListener(this);
        findPreference(SettingsActivity.KEY_DARK_THEME).setOnPreferenceChangeListener(this);
        findPreference(SettingsActivity.KEY_NOTIFICATION_RINGTONE).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mFirstLaunch = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        long cacheSize = FileUtil.getDirectorySize(mApp.getImageLoader().getDiskCache().getDirectory());
        findPreference(SettingsActivity.KEY_CURRENT_CACHE_SIZE).setSummary(FileUtil.humanReadableByteCount(cacheSize, false));

        try {
            String version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            findPreference("version").setSummary(version);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtil.e(TAG, "Unable to get version summary", e);
        }


        String ringtone = mApp.getPreferences().getString(SettingsActivity.KEY_NOTIFICATION_RINGTONE, null);
        if (!TextUtils.isEmpty(ringtone)) findPreference(SettingsActivity.KEY_NOTIFICATION_RINGTONE).setSummary(getNotificationRingtone(ringtone));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        boolean updated = super.onPreferenceChange(preference, object);
        String key = preference.getKey();

        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(object.toString());

            switch (key) {
                case SettingsActivity.KEY_THEME:
                    boolean isDarkTheme = mApp.getImgurTheme().isDarkTheme;
                    ImgurTheme theme = ImgurTheme.getThemeFromString(listPreference.getEntries()[prefIndex].toString());
                    theme.isDarkTheme = isDarkTheme;
                    mApp.setImgurTheme(theme);
                    if (!mFirstLaunch) getActivity().recreate();
                    updated = true;
                    break;

                case SettingsActivity.KEY_CACHE_LOC:
                    if (!mFirstLaunch)
                        new DeleteCacheTask(SettingsFragment.this, object.toString()).execute();
                    updated = true;
                    break;
            }
        } else if (preference instanceof CheckBoxPreference) {
            switch (key) {
                case SettingsActivity.KEY_ADB:
                    // Ignore if its a debug build
                    if (!BuildConfig.DEBUG) {
                        LogUtil.SHOULD_WRITE_LOGS = (Boolean) object;
                        mApp.setAllowLogs((Boolean) object);
                    }
                    updated = true;
                    break;

                case SettingsActivity.KEY_DARK_THEME:
                    mApp.getImgurTheme().isDarkTheme = (Boolean) object;
                    getActivity().recreate();
                    updated = true;
                    break;
            }
        } else if (preference instanceof RingtonePreference) {
            preference.setSummary(getNotificationRingtone(object.toString()));
            updated = true;
        }

        return updated;
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch (preference.getKey()) {
            case SettingsActivity.KEY_CURRENT_CACHE_SIZE:
                new AlertDialog.Builder(getActivity(), mApp.getImgurTheme().getAlertDialogTheme())
                        .setTitle(R.string.clear_cache)
                        .setMessage(R.string.clear_cache_message)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new DeleteCacheTask(SettingsFragment.this, null).execute();
                            }
                        }).show();
                return true;

            case "licenses":
                WebView webView = new WebView(getActivity());
                new AlertDialog.Builder(getActivity(), mApp.getImgurTheme().getAlertDialogTheme())
                        .setPositiveButton(R.string.dismiss, null)
                        .setView(webView)
                        .show();

                webView.loadUrl("file:///android_asset/licenses.html");
                return true;

            case "openSource":
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Kennyc1012/Opengur"));

                if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(browserIntent);
                } else {
                    SnackBar.show(getActivity(), R.string.cant_launch_intent);
                }

                return true;

            case "redditHistory":
                mApp.getSql().deleteSubReddits();
                SnackBar.show(getActivity(), R.string.pref_reddit_deleted);
                return true;

            case "gallerySearchHistory":
                mApp.getSql().deletePreviousGallerySearch();
                SnackBar.show(getActivity(), R.string.pref_search_deleted);
                return true;

            case "experimentalSettings":
                startActivity(SettingsActivity.createIntent(getActivity(), true));
                return true;

            case "mySubreddits":
                mApp.getPreferences().edit().remove(RedditFragment.KEY_PINNED_SUBREDDITS).apply();
                SnackBar.show(getActivity(), R.string.pref_reddit_deleted);
                return true;
        }

        return super.onPreferenceClick(preference);
    }

    @Nullable
    private String getNotificationRingtone(String uri) {
        try {
            Uri ringTone = Uri.parse(uri);
            return RingtoneManager.getRingtone(getActivity(), ringTone).getTitle(getActivity());
        } catch (Exception ex) {
            LogUtil.e(TAG, "Unable to parse ringtone", ex);
            return null;
        }
    }

    private static class DeleteCacheTask extends AsyncTask<Void, Void, Long> {
        private WeakReference<SettingsFragment> mFragment;

        private String mCacheDirKey;

        public DeleteCacheTask(SettingsFragment fragment, String cacheDirKey) {
            mFragment = new WeakReference<>(fragment);
            mCacheDirKey = cacheDirKey;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mFragment.get().getFragmentManager().beginTransaction().add(LoadingDialogFragment.createInstance(R.string.one_moment, false), "loading").commit();
        }

        @Override
        protected Long doInBackground(Void... voids) {
            SettingsFragment frag = mFragment.get();
            frag.mApp.deleteAllCache();

            if (!TextUtils.isEmpty(mCacheDirKey)) {
                File dir = ImageUtil.getCacheDirectory(frag.getActivity(), mCacheDirKey);
                ImageUtil.initImageLoader(frag.getActivity());
                VideoCache.getInstance().setCacheDirectory(dir);
            }


            return FileUtil.getDirectorySize(frag.mApp.getCacheDir());
        }

        @Override
        protected void onPostExecute(Long cacheSize) {
            SettingsFragment frag = mFragment.get();

            if (frag != null) {
                frag.findPreference(SettingsActivity.KEY_CURRENT_CACHE_SIZE).setSummary(FileUtil.humanReadableByteCount(cacheSize, false));
                Fragment fragment = frag.getFragmentManager().findFragmentByTag("loading");

                if (fragment != null) {
                    ((DialogFragment) fragment).dismiss();
                }

                mFragment.clear();
            }
        }
    }

    @Override
    protected int getPreferenceXML() {
        return R.xml.settings;
    }
}