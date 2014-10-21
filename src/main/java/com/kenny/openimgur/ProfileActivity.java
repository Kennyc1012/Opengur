package com.kenny.openimgur;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.kenny.openimgur.fragments.ProfileFragment;

/**
 * Empty activity needed to display the profile fragment. This will be used for profiles that are not accessed from the main screen
 * Created by kcampagna on 7/26/14.
 */
public class ProfileActivity extends BaseActivity {
    private static final String KEY_USERNAME = "username";

    public static Intent createIntent(Context context, @Nullable String user) {
        Intent intent = new Intent(context, ProfileActivity.class);

        if (!TextUtils.isEmpty(user)) {
            intent.putExtra(KEY_USERNAME, user);
        }

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.empty_header);

        if (savedInstanceState == null) {
            Intent data = getIntent();
            String username = null;

            if (data != null && data.hasExtra(KEY_USERNAME)) {
                username = data.getStringExtra(KEY_USERNAME);
            }

            getFragmentManager().beginTransaction().add(R.id.content, ProfileFragment.createInstance(username)).commit();
            getSupportActionBar().setTitle(R.string.profile);
        }
    }
}