package com.kenny.openimgur.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

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
                String username = LinkUtils.getUsername(link);

                if (!TextUtils.isEmpty(username)) {
                    routingIntent = ProfileActivity.createIntent(getApplicationContext(), username);
                }
                break;

            case GALLERY:
                String id = LinkUtils.getGalleryId(link);

                if (!TextUtils.isEmpty(id)) {
                    routingIntent = ViewActivity.createGalleryIntentIntent(getApplicationContext(), id);
                }
                break;

            case IMAGE:
                routingIntent = ViewActivity.createIntent(getApplicationContext(), link, false);
                break;

            case ALBUM:
                String albumId = LinkUtils.getAlbumId(link);
                routingIntent = ViewActivity.createAlbumIntent(getApplicationContext(), albumId);
                break;

            case IMAGE_URL_QUERY:
                int index = link.indexOf("?");
                link = link.substring(0, index);
                // Intentional fallthrough
            case DIRECT_LINK:
                routingIntent = FullScreenPhotoActivity.createIntent(getApplicationContext(), link);
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

    @Override
    protected int getStyleRes() {
        // Routing activity is barely visible so the theme won't matter
        return R.style.Theme_Opengur_Light_DarkActionBar;
    }
}
