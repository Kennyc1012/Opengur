/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kenny.openimgur.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurFilters;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GalleryWidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GalleryWidgetRemoteViewsFactory((OpenImgurApp) getApplication(), intent);
    }
}

class GalleryWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final String TAG = GalleryWidgetRemoteViewsFactory.class.getSimpleName();

    private static final int mCount = 20;
    private List<ImgurBaseObject> mWidgetItems = new ArrayList<>();
    private OpenImgurApp mApplication;
    private int mAppWidgetId;

    protected ApiClient mApiClient;
    private ImgurFilters.GallerySection mSection = ImgurFilters.GallerySection.HOT;
    private ImgurFilters.GallerySort mSort = ImgurFilters.GallerySort.TIME;
    private ImgurFilters.TimeSort mTimeSort = ImgurFilters.TimeSort.DAY;
    private boolean mShowViral = true;
    protected int mCurrentPage = 0;
    private boolean mAllowNSFW;

    public GalleryWidgetRemoteViewsFactory(OpenImgurApp app, Intent intent) {
        mApplication = app;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public void onCreate() {
        // In onCreate() you setup any connections / cursors to your data source. Heavy lifting,
        // for example downloading or creating content etc, should be deferred to onDataSetChanged()
        // or getViewAt(). Taking more than 20 seconds in this call will result in an ANR.

        mAllowNSFW = PreferenceManager.getDefaultSharedPreferences(mApplication).getBoolean(SettingsActivity.NSFW_KEY, false);

    }

    @Override
    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        mWidgetItems.clear();
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        ImgurBaseObject object = mWidgetItems.get(position);

        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        final RemoteViews rv = new RemoteViews(mApplication.getPackageName(), R.layout.widget_item);
        rv.setTextViewText(R.id.widget_caption, object.getTitle());

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putInt(GalleryWidgetProvider.EXTRA_ITEM, position);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_caption, fillInIntent);

        // You can do heaving lifting in here, synchronously. For example, if you need to
        // process an image, fetch something from the network, etc., it is ok to do it here,
        // synchronously. A loading view will show up in lieu of the actual contents in the
        // interim.

        rv.setImageViewBitmap(R.id.widget_image,
                mApplication.getImageLoader().loadImageSync(getImageUrl(object)));

        // Return the remote views object.
        return rv;
    }

    private String getImageUrl(ImgurBaseObject obj) {
        //TODO adjust image we load based on widget size?
        if (obj instanceof ImgurPhoto) {
            ImgurPhoto photoObject = ((ImgurPhoto) obj);
            String photoUrl;

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasMP4Link() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto) obj).getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, false, null);
            }

            return photoUrl;

        } else if (obj instanceof ImgurAlbum) {
            return ((ImgurAlbum) obj).getCoverUrl(ImgurPhoto.THUMBNAIL_GALLERY);
        } else {
            return ImgurBaseObject.getThumbnail(obj.getId(), obj.getLink(), ImgurPhoto.THUMBNAIL_GALLERY);
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return new RemoteViews(mApplication.getPackageName(), R.layout.widget_loading);
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
        // This is triggered when you call AppWidgetManager notifyAppWidgetViewDataChanged
        // on the collection view corresponding to this factory. You can do heaving lifting in
        // here, synchronously. For example, if you need to process an image, fetch something
        // from the network, etc., it is ok to do it here, synchronously. The widget will remain
        // in its current state while work is being done here, so you don't need to worry about
        // locking up the widget.
        List<ImgurBaseObject> objects = fetchGallery();
        mWidgetItems.addAll(objects);
    }

    protected List<ImgurBaseObject> fetchGallery() {
        return makeRequest(getGalleryUrl());
    }

    /**
     * Returns the URL based on the selected sort and section
     *
     * @return
     */
    private String getGalleryUrl() {
        if (mSort == ImgurFilters.GallerySort.HIGHEST_SCORING) {
            return String.format(Endpoints.GALLERY_TOP.getUrl(), mSection.getSection(), mSort.getSort(),
                    mTimeSort.getSort(), mCurrentPage, mShowViral);
        }

        return String.format(Endpoints.GALLERY.getUrl(), mSection.getSection(), mSort.getSort(), mCurrentPage, mShowViral);
    }

    protected List<ImgurBaseObject> makeRequest(String url) {
        if (mApiClient == null) {
            mApiClient = new ApiClient(url, ApiClient.HttpRequest.GET);
        } else {
            mApiClient.setUrl(url);
        }

        try {
            JSONObject json = mApiClient.doWork(null);
            int statusCode = json.getInt(ApiClient.KEY_STATUS);
            List<ImgurBaseObject> objects;

            if (statusCode == ApiClient.STATUS_OK) {
                JSONArray arr = json.getJSONArray(ApiClient.KEY_DATA);

                if (arr == null || arr.length() <= 0) {
                    LogUtil.v(TAG, "Did not receive any items in the json array");
                    return null;
                }

                objects = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.getJSONObject(i);
                    ImgurBaseObject imgurObject;

                    if (item.has("is_album") && item.getBoolean("is_album")) {
                        imgurObject = new ImgurAlbum(item);
                    } else {
                        imgurObject = new ImgurPhoto(item);
                    }

                    if (allowNSFW() || !imgurObject.isNSFW()) {
                        objects.add(imgurObject);
                    }
                }
                return objects;

            }

        } catch (Exception e) {
            LogUtil.e(TAG, "Error parsing JSON", e);
        }
        return null;
    }

    private boolean allowNSFW() {
        return mAllowNSFW;
    }

}