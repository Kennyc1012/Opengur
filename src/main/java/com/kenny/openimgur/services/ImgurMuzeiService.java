package com.kenny.openimgur.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.kenny.openimgur.activities.MuzeiSettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class ImgurMuzeiService extends RemoteMuzeiArtSource {
    private static final String TAG = "ImgurMuzeiService";

    // If unable to get an image, DickButt will be shown, dealwithit.gif
    private static final String FALLBACK_URL = "http://i.imgur.com/ICt6W7X.png";

    private static final String FALLBACK_TITLE = "DickButt";

    private static final String FALLBACK_BYLINE = "K.C. Green";

    private static final String FALLBACK_SUBREDDIT = "aww";

    // 8 is aww
    private static final String FALLBACK_TOPIC_ID = "8";

    private static final Random sRandom = new Random();

    public ImgurMuzeiService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        OpengurApp app = OpengurApp.getInstance(getApplicationContext());
        SharedPreferences pref = app.getPreferences();
        boolean allowNSFW = pref.getBoolean(MuzeiSettingsActivity.KEY_NSFW, false);
        boolean wifiOnly = pref.getBoolean(MuzeiSettingsActivity.KEY_WIFI, true);
        String source = pref.getString(MuzeiSettingsActivity.KEY_SOURCE, MuzeiSettingsActivity.SOURCE_VIRAL);
        String updateInterval = pref.getString(MuzeiSettingsActivity.KEY_UPDATE, MuzeiSettingsActivity.UPDATE_3_HOURS);

        if (wifiOnly) {
            ConnectivityManager cm = (ConnectivityManager) app.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork == null || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                LogUtil.w(TAG, "Not connected to WiFi when user requires it");
                return;
            }
        }

        String url;
        String title;
        String byline;
        Uri uri;
        List<ImgurPhoto> photos = makeRequest(getClientForSource(source, pref), allowNSFW);

        if (photos != null && !photos.isEmpty()) {
            ImgurPhoto photo = photos.get(sRandom.nextInt(photos.size()));
            url = photo.getLink();
            title = photo.getTitle();
            byline = TextUtils.isEmpty(photo.getAccount()) ? "?????" : photo.getAccount();
        } else {
            url = FALLBACK_URL;
            title = FALLBACK_TITLE;
            byline = FALLBACK_BYLINE;
        }

        uri = Uri.parse(url);
        publishArtwork(new Artwork.Builder()
                .imageUri(uri)
                .title(title)
                .byline(byline)
                .viewIntent(new Intent(Intent.ACTION_VIEW, uri))
                .build());

        scheduleUpdate(System.currentTimeMillis() + getNextUpdateTime(updateInterval));
    }

    private List<ImgurPhoto> makeRequest(ApiClient client, boolean allowNSFW) {
        try {
            JSONObject json = client.doWork(null);
            int statusCode = json.getInt(ApiClient.KEY_STATUS);

            if (statusCode == ApiClient.STATUS_OK) {
                JSONArray arr = json.getJSONArray(ApiClient.KEY_DATA);

                if (arr == null || arr.length() <= 0) {
                    LogUtil.v(TAG, "Did not receive any items in the json array");
                    return null;
                }

                List<ImgurPhoto> objects = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    ImgurPhoto photo = null;

                    // Ignore albums
                    if (item.has("is_album") && item.getBoolean("is_album")) {
                        // Skip albums
                    } else {
                        photo = new ImgurPhoto(item);
                    }

                    // Gifs can not be used for Muzei
                    boolean canAdd = photo != null && !photo.isAnimated();

                    if (canAdd && (allowNSFW || !photo.isNSFW())) {
                        objects.add(photo);
                    }
                }

                return objects;
            }
        } catch (IOException ex) {
            LogUtil.e(TAG, "Exception while making API request", ex);
        } catch (JSONException ex) {
            LogUtil.e(TAG, "Unable to parse JSON response", ex);
        }

        return null;
    }

    /**
     * Returns the time until the next update
     *
     * @param updateInterval The preference value for the next update
     * @return
     */
    private long getNextUpdateTime(String updateInterval) {
        if (MuzeiSettingsActivity.UPDATE_1_HOUR.equals(updateInterval)) {
            return DateUtils.HOUR_IN_MILLIS;
        } else if (MuzeiSettingsActivity.UPDATE_6_HOURS.equals(updateInterval)) {
            return DateUtils.HOUR_IN_MILLIS * 6;
        } else if (MuzeiSettingsActivity.UPDATE_12_HOURS.equals(updateInterval)) {
            return DateUtils.HOUR_IN_MILLIS * 12;
        } else if (MuzeiSettingsActivity.UPDATE_24_HOURS.equals(updateInterval)) {
            return DateUtils.DAY_IN_MILLIS;
        }

        return DateUtils.HOUR_IN_MILLIS * 3;
    }

    /**
     * Returns the Api client to use for getting photos
     *
     * @param source The source for displaying photos
     * @return
     */
    private ApiClient getClientForSource(String source, SharedPreferences pref) {
        String url;

        if (MuzeiSettingsActivity.SOURCE_REDDIT.equals(source)) {
            String query = pref.getString(MuzeiSettingsActivity.KEY_INPUT, FALLBACK_SUBREDDIT);
            url = String.format(Endpoints.SUBREDDIT.getUrl(), query.replaceAll("\\s", ""), ImgurFilters.RedditSort.TOP.getSort(), ImgurFilters.TimeSort.ALL.getSort(), 0);
        } else if (MuzeiSettingsActivity.SOURCE_VIRAL.equals(source)) {
            url = String.format(Endpoints.GALLERY.getUrl(), ImgurFilters.GallerySection.HOT.getSection(), ImgurFilters.GallerySort.TIME.getSort(), 0, false);
        } else if (MuzeiSettingsActivity.SOURCE_USER_SUB.equals(source)) {
            url = String.format(Endpoints.GALLERY.getUrl(), ImgurFilters.GallerySection.USER.getSection(), ImgurFilters.GallerySort.VIRAL.getSort(), 0, false);
        } else {
            int topicId = Integer.valueOf(pref.getString(MuzeiSettingsActivity.KEY_TOPIC, FALLBACK_TOPIC_ID));
            url = String.format(Endpoints.TOPICS.getUrl(), topicId, ImgurFilters.GallerySort.VIRAL.getSort(), 0);
        }


        return new ApiClient(url, ApiClient.HttpRequest.GET);
    }
}
