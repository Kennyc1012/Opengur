package com.kenny.openimgur.fragments;

import android.os.Bundle;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MuzeiSettingsActivity;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class MuzeiSettingsFragment extends BasePreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindListPreference(findPreference(MuzeiSettingsActivity.KEY_SOURCE));
        bindListPreference(findPreference(MuzeiSettingsActivity.KEY_UPDATE));
    }

    @Override
    protected int getPreferenceXML() {
        return R.xml.muzei_settings;
    }
}
