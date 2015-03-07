package com.kenny.openimgur.activities;

import android.content.Intent;
import android.os.Bundle;

import com.kenny.openimgur.R;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 9/17/14.
 */
public class RoutingActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_view);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            LogUtil.w(TAG, "No url was passed. How did that happen?");
            finish();
            return;
        }

        String link = intent.getData().toString();
        LogUtil.v(TAG, "Received link " + link);
        LinkUtils.LinkMatch match = LinkUtils.findImgurLinkMatch(link);
        Intent routingIntent = null;

        switch (match) {
            case USER:
                String username = link.substring(link.lastIndexOf("/") + 1);
                LogUtil.v(TAG, "Username " + username + " extracted from url");
                routingIntent = ProfileActivity.createIntent(getApplicationContext(), username);
                break;

            case GALLERY:
            case IMAGE:
            case ALBUM:
                routingIntent = ViewActivity.createIntent(getApplicationContext(), link);
                break;
        }

        if (routingIntent != null) {
            routingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(routingIntent);
        } else {
            LogUtil.w(TAG, "Routing intent not set, bailing");
        }

        finish();
    }
}
