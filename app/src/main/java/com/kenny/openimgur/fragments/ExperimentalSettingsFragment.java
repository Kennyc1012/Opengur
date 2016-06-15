package com.kenny.openimgur.fragments;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

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

        new AlertDialog.Builder(getActivity(), mApp.getImgurTheme().getAlertDialogTheme())
                .setTitle(R.string.caution)
                .setMessage(R.string.pref_experimental_warning_msg)
                .setPositiveButton(getString(R.string.okay).toUpperCase(), null)
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
                new AlertDialog.Builder(getActivity(), mApp.getImgurTheme().getAlertDialogTheme())
                        .setTitle(R.string.pref_experimental_settings)
                        .setMessage(R.string.pref_experimental_reset_msg)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
