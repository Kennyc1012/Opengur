package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.annotation.NonNull;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MuzeiSettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

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
            EventBus.getDefault().register(this);
            new ApiClient(Endpoints.TOPICS_DEFAULTS.getUrl(), ApiClient.HttpRequest.GET)
                    .doWork(ImgurBusEvent.EventType.TOPICS, null, null);
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
    public void onDestroyView() {
        if (EventBus.getDefault().isRegistered(this)) EventBus.getDefault().unregister(this);
        super.onDestroyView();
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.TOPICS) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

                if (statusCode == ApiClient.STATUS_OK) {
                    List<ImgurTopic> topics = new ArrayList<>();
                    JSONArray array = event.json.getJSONArray(ApiClient.KEY_DATA);

                    for (int i = 0; i < array.length(); i++) {
                        topics.add(new ImgurTopic(array.getJSONObject(i)));
                    }

                    mApp.getSql().addTopics(topics);
                    getActivity().runOnUiThread(mTopicRunnable);
                } else {
                    // TODO Some error?
                }

            } catch (JSONException e) {
                LogUtil.e(TAG, "Error parsing JSON", e);
                // What to do on error?
            }
        }
    }

    private final Runnable mTopicRunnable = new Runnable() {
        @Override
        public void run() {
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
                LogUtil.w(TAG, "Still no topics, what do we do!");
            }
        }
    };

    @Override
    protected int getPreferenceXML() {
        return R.xml.muzei_settings;
    }
}
