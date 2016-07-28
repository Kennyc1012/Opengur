package com.kenny.openimgur.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.PhotoResponse;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.LogUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
                String galleryId = LinkUtils.getGalleryId(link);

                if (!TextUtils.isEmpty(galleryId)) {
                    routingIntent = ViewActivity.createGalleryIntent(getApplicationContext(), galleryId);
                }
                break;

            case IMAGE:
                String id = LinkUtils.getId(link);

                if (!TextUtils.isEmpty(id)) {
                    fetchImageDetails(id);
                    return;
                }
                break;

            case ALBUM:
                String albumId = LinkUtils.getAlbumId(link);
                routingIntent = ViewActivity.createAlbumIntent(getApplicationContext(), albumId);
                break;

            case TOPIC:
                String topicId = LinkUtils.getTopicGalleryId(link);
                routingIntent = ViewActivity.createGalleryIntent(getApplicationContext(), topicId);
                break;

            case IMAGE_URL_QUERY:
                int index = link.indexOf("?");
                link = link.substring(0, index);
                boolean isDirectLink = LinkUtils.isDirectImageLink(link);

                if (!isDirectLink) {
                    String extractedId = LinkUtils.getId(link);
                    if (!TextUtils.isEmpty(extractedId)) {
                        fetchImageDetails(extractedId);
                        return;
                    }
                }else{
                    routingIntent = FullScreenPhotoActivity.createIntent(getApplicationContext(), link);
                }
                break;

            case DIRECT_LINK:
                routingIntent = FullScreenPhotoActivity.createIntent(getApplicationContext(), link);
                break;
        }

        if (routingIntent != null) {
            routingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(routingIntent);
        } else {
            LogUtil.w(TAG, "Routing intent not set, bailing");
            Toast.makeText(getApplicationContext(), R.string.error_link_open, Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    @Override
    protected int getStyleRes() {
        // Routing activity is barely visible so the theme won't matter
        return R.style.Theme_Opengur_Light_DarkActionBar;
    }

    private void fetchImageDetails(String id) {
        ApiClient.getService().getImageDetails(id).enqueue(new Callback<PhotoResponse>() {
            @Override
            public void onResponse(Call<PhotoResponse> call, Response<PhotoResponse> response) {
                if (response != null && response.body() != null && response.body().data != null) {
                    ImgurPhoto photo = response.body().data;

                    if (photo != null) {
                        startActivity(FullScreenPhotoActivity.createIntent(getApplicationContext(), photo));
                    }
                }
                finish();
            }

            @Override
            public void onFailure(Call<PhotoResponse> call, Throwable t) {
                finish();
            }
        });
    }
}
