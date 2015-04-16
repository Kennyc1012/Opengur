package com.kenny.openimgur.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;

/**
 * Created by kcampagna on 4/16/15.
 */
public class ExperimentalSettingsFragment extends BasePreferenceFragment implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindListPreference(findPreference(SettingsActivity.KEY_THREAD_SIZE));
        setHasOptionsMenu(true);

        new MaterialDialog.Builder(getActivity())
                .title(R.string.caution)
                .content(R.string.pref_experimental_warning_msg)
                .positiveText(getString(R.string.okay).toUpperCase())
                .show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.experimental_settings, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reset:
                new MaterialDialog.Builder(getActivity())
                        .title(R.string.pref_experimental_settings)
                        .content(R.string.pref_experimental_reset_msg)
                        .negativeText(R.string.cancel)
                        .positiveText(R.string.yes)
                        .callback(new MaterialDialog.ButtonCallback() {
                            @Override
                            public void onPositive(MaterialDialog dialog) {
                                super.onPositive(dialog);
                                SharedPreferences.Editor edit = mApp.getPreferences().edit();
                                edit.putString(SettingsActivity.KEY_THREAD_SIZE, SettingsActivity.THREAD_SIZE_5);
                                onPreferenceChange(findPreference(SettingsActivity.KEY_THREAD_SIZE), SettingsActivity.THREAD_SIZE_5);
                                edit.apply();
                            }
                        }).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getPreferenceXML() {
        return R.xml.experimental_settings;
    }
}
