package com.kenny.openimgur.activities;

import android.os.Bundle;

import com.kenny.openimgur.R;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class MuzeiSettingsActivity extends BaseActivity {
    // Preference Keys
    public static final String KEY_WIFI = "muzeiWifi";

    public static final String KEY_NSFW = "muzeiNSFW";

    public static final String KEY_UPDATE = "muzeiUpdate";

    public static final String KEY_SOURCE = "muzeiSource";

    public static final String KEY_INPUT = "muzeiInput";

    public static final String KEY_TOPIC = "muzeiTopic";

    // Preference Values
    public static final String UPDATE_1_HOUR = "1";

    public static final String UPDATE_3_HOURS = "3";

    public static final String UPDATE_6_HOURS = "6";

    public static final String UPDATE_12_HOURS = "12";

    public static final String UPDATE_24_HOURS = "24";

    public static final String SOURCE_VIRAL = "viral";

    public static final String SOURCE_USER_SUB = "usersub";

    public static final String SOURCE_TOPICS = "topics";

    public static final String SOURCE_REDDIT = "reddit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_muzei_settings);
        getSupportActionBar().setTitle(R.string.muzei_settings);
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark : R.style.Theme_Opengur_Light_DarkActionBar;
    }
}
