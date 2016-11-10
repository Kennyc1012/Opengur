package com.kenny.openimgur.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MuzeiSettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.NetworkUtils;
import com.kenny.openimgur.util.SqlHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class ImgurMuzeiService extends RemoteMuzeiArtSource {
    private static final String TAG = "ImgurMuzeiService";

    // If unable to get an image, DickButt will be shown, dealwithit.gif
    private static final String FALLBACK_URL = "http://i.imgur.com/myiMdIq.jpeg";

    private static final String FALLBACK_SUBREDDIT = "aww";

    // 8 is aww
    private static final String FALLBACK_TOPIC_ID = "8";

    private static final Random sRandom = new Random();

    private static final int RETRY_ATTEMPTS = 5;

    @Nullable
    private String mSubreddit = null;

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
        Context context = getApplicationContext();

        if (NetworkUtils.hasDataSaver(context)) {
            LogUtil.w(TAG, "Data saver enabled, not updating");
            return;
        }

        OpengurApp app = OpengurApp.getInstance(context);
        SharedPreferences pref = app.getPreferences();
        boolean allowNSFW = pref.getBoolean(MuzeiSettingsActivity.KEY_NSFW, false);
        boolean wifiOnly = pref.getBoolean(MuzeiSettingsActivity.KEY_WIFI, true);
        String source = pref.getString(MuzeiSettingsActivity.KEY_SOURCE, MuzeiSettingsActivity.SOURCE_VIRAL);
        String updateInterval = pref.getString(MuzeiSettingsActivity.KEY_UPDATE, MuzeiSettingsActivity.UPDATE_3_HOURS);

        if (wifiOnly && !NetworkUtils.isConnectedToWiFi(context)) {
            LogUtil.w(TAG, "Not connected to WiFi when user requires it");
            return;
        }

        String url;
        String title;
        String byline;
        Uri uri;
        List<ImgurPhoto> photos = fetchImages(source, pref, allowNSFW);

        if (photos != null && !photos.isEmpty()) {
            ImgurPhoto photo = photos.get(sRandom.nextInt(photos.size()));
            SqlHelper sql = SqlHelper.getInstance(context);

            if (sql.getMuzeiLastSeen(photo.getLink()) != -1) {
                // We have seen this image already, lets try to get another
                LogUtil.v(TAG, "Got an image we have already seen, going to try for another");
                int retryCount = 0;

                while (retryCount < RETRY_ATTEMPTS) {
                    photo = photos.get(sRandom.nextInt(photos.size()));

                    if (sql.getMuzeiLastSeen(photo.getLink()) == -1) {
                        LogUtil.v(TAG, "Received a valid image, retry count at " + retryCount);
                        break;
                    }

                    LogUtil.v(TAG, "Received another duplicate, retry count at " + retryCount);
                    retryCount++;
                }
            } else {
                LogUtil.v(TAG, "Link does not exist in database");
            }

            sql.addMuzeiLink(photo.getLink());
            url = photo.getLink();
            title = photo.getTitle();

            if (source.equals(MuzeiSettingsActivity.SOURCE_REDDIT)) {
                byline = TextUtils.isEmpty(mSubreddit) ? pref.getString(MuzeiSettingsActivity.KEY_INPUT, FALLBACK_SUBREDDIT) : mSubreddit;
                mSubreddit = null;
            } else {
                byline = TextUtils.isEmpty(photo.getAccount()) ? "?????" : photo.getAccount();
            }
        } else {
            url = FALLBACK_URL;
            title = getString(R.string.muzei_error_title);
            byline = getString(R.string.muzei_error_byline);
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

    @Nullable
    private List<ImgurPhoto> fetchImages(String source, SharedPreferences pref, boolean allowNSFW) {
        Response<GalleryResponse> response = null;
        Call<GalleryResponse> call;

        try {
            if (MuzeiSettingsActivity.SOURCE_REDDIT.equals(source)) {
                String query = pref.getString(MuzeiSettingsActivity.KEY_INPUT, FALLBACK_SUBREDDIT);
                String[] split = query.split(",");

                if (split.length > 1) {
                    query = split[sRandom.nextInt(split.length)];
                }

                mSubreddit = query.replaceAll("\\s", "");
                call = ApiClient.getService().getSubReddit(mSubreddit, ImgurFilters.RedditSort.TIME.getSort(), 0);
            } else if (MuzeiSettingsActivity.SOURCE_USER_SUB.equals(source)) {
                call = ApiClient.getService().getGallery(ImgurFilters.GallerySection.USER.getSection(), ImgurFilters.GallerySort.VIRAL.getSort(), 0, false);
            } else if (MuzeiSettingsActivity.SOURCE_TOPICS.equals(source)) {
                int topicId = Integer.valueOf(pref.getString(MuzeiSettingsActivity.KEY_TOPIC, FALLBACK_TOPIC_ID));
                call = ApiClient.getService().getTopic(topicId, ImgurFilters.GallerySort.VIRAL.getSort(), 0);
            } else {
                call = ApiClient.getService().getGallery(ImgurFilters.GallerySection.HOT.getSection(), ImgurFilters.GallerySort.TIME.getSort(), 0, false);
            }

            response = call.execute();
        } catch (Exception ex) {
            Log.e(TAG, "Error fetching images for muzei", ex);
        }

        if (response != null && response.body() != null && !response.body().data.isEmpty()) {
            // Go through the responses and exclude albums, gifs, and check NSFW status
            List<ImgurPhoto> photos = new ArrayList<>();

            for (ImgurBaseObject obj : response.body().data) {
                if (obj instanceof ImgurPhoto && !((ImgurPhoto) obj).isAnimated() && (allowNSFW || !obj.isNSFW())) {
                    photos.add((ImgurPhoto) obj);
                }
            }

            return photos;
        }

        return null;
    }
}
