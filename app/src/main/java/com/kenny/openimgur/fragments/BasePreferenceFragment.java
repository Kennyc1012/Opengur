package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.kenny.openimgur.classes.OpengurApp;

/**
 * Created by kcampagna on 4/16/15.
 */
public abstract class BasePreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {
    protected final String TAG = getClass().getSimpleName();

    protected OpengurApp mApp;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp = OpengurApp.getInstance(getActivity());
        addPreferencesFromResource(getPreferenceXML());
    }

    /**
     * Binds a list preference to the OnPreferenceChangeListener and sets it summary
     *
     * @param preference The preference to bind
     */
    protected void bindListPreference(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, mApp.getPreferences().getString(preference.getKey(), ""));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int prefIndex = listPreference.findIndexOfValue(newValue.toString());
            if (prefIndex >= 0) preference.setSummary(listPreference.getEntries()[prefIndex]);
            return true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    // Returns the xml file to load the preferences from
    protected abstract int getPreferenceXML();
}
