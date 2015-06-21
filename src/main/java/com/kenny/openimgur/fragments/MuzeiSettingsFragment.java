package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MuzeiSettingsActivity;
import com.kenny.openimgur.classes.ImgurTopic;

import java.util.List;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class MuzeiSettingsFragment extends BasePreferenceFragment {

    private EditTextPreference mInputPreference;

    private ListPreference mTopicPreference;

    private PreferenceScreen mScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mScreen = getPreferenceScreen();
        mTopicPreference = (ListPreference) findPreference(MuzeiSettingsActivity.KEY_TOPIC);
        mInputPreference = (EditTextPreference) findPreference(MuzeiSettingsActivity.KEY_INPUT);
        bindListPreference(findPreference(MuzeiSettingsActivity.KEY_SOURCE));
        bindListPreference(findPreference(MuzeiSettingsActivity.KEY_UPDATE));
        bindListPreference(mTopicPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = super.onPreferenceChange(preference, newValue);

        if (preference.getKey().equals(MuzeiSettingsActivity.KEY_SOURCE)) {
            toggleSource(newValue.toString());
        }

        return result;
    }

    private void toggleSource(String source) {
        if (MuzeiSettingsActivity.SOURCE_REDDIT.equals(source)) {
            mScreen.removePreference(mTopicPreference);
            mScreen.addPreference(mInputPreference);
        } else if (MuzeiSettingsActivity.SOURCE_TOPICS.equals(source)) {
            List<ImgurTopic> topics = mApp.getSql().getTopics();

            if (topics != null && topics.size() > 0) {
                String[] topicNames = new String[topics.size()];
                String[] topicIds = new String[topics.size()];

                for (int i = 0; i < topics.size(); i++) {
                    ImgurTopic t = topics.get(i);
                    topicNames[i] = t.getName();
                    topicIds[i] = String.valueOf(t.getId());
                }

                mTopicPreference.setEntries(topicNames);
                mTopicPreference.setEntryValues(topicIds);
            } else {
                // TODO Make API Request
            }

            mScreen.addPreference(mTopicPreference);
            mScreen.removePreference(mInputPreference);
        } else {
            mScreen.removePreference(mTopicPreference);
            mScreen.removePreference(mInputPreference);
        }
    }

    @Override
    protected int getPreferenceXML() {
        return R.xml.muzei_settings;
    }
}
