package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MuzeiSettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.util.LogUtil;

import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

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
        mInputPreference.setOnPreferenceChangeListener(this);
        String savedSubReddit = mApp.getPreferences().getString(MuzeiSettingsActivity.KEY_INPUT, "aww");
        mInputPreference.setSummary(savedSubReddit);
        List<ImgurTopic> topics = mApp.getSql().getTopics();

        if (topics != null && !topics.isEmpty()) {
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
            LogUtil.v(TAG, "No topics found, fetching");
            ApiClient.getService().getDefaultTopics(new Callback<TopicResponse>() {
                @Override
                public void success(TopicResponse topicResponse, Response response) {
                    if (!isAdded()) return;

                    mApp.getSql().addTopics(topicResponse.data);

                    if (!topicResponse.data.isEmpty()) {
                        String[] topicNames = new String[topicResponse.data.size()];
                        String[] topicIds = new String[topicResponse.data.size()];

                        for (int i = 0; i < topicResponse.data.size(); i++) {
                            ImgurTopic t = topicResponse.data.get(i);
                            topicNames[i] = t.getName();
                            topicIds[i] = String.valueOf(t.getId());
                        }

                        mTopicPreference.setEntries(topicNames);
                        mTopicPreference.setEntryValues(topicIds);
                    } else {
                        LogUtil.w(TAG, "Still no topics, what do we do!");
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    if (!isAdded()) return;
                    LogUtil.e(TAG, "Failed to receive topics with status code " + error.getResponse().getStatus(), error);
                    // TODO Some error?
                }
            });
        }

        bindListPreference(findPreference(MuzeiSettingsActivity.KEY_SOURCE));
        bindListPreference(findPreference(MuzeiSettingsActivity.KEY_UPDATE));
        bindListPreference(mTopicPreference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = super.onPreferenceChange(preference, newValue);

        if (preference.getKey().equals(MuzeiSettingsActivity.KEY_SOURCE)) {
            toggleSource(newValue.toString());
        } else if (preference.getKey().equals(MuzeiSettingsActivity.KEY_INPUT)) {
            preference.setSummary(newValue.toString());
            result = true;
        }

        return result;
    }

    private void toggleSource(String source) {
        if (MuzeiSettingsActivity.SOURCE_REDDIT.equals(source)) {
            mScreen.removePreference(mTopicPreference);
            mScreen.addPreference(mInputPreference);
        } else if (MuzeiSettingsActivity.SOURCE_TOPICS.equals(source)) {
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
