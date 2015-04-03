package com.kenny.openimgur.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by John on 4/3/2015.
 */
public class GalleryWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final int ITEM_COUNT = 50;
    private List<String> mWidgetItems = new ArrayList<>();
    private OpenImgurApp mApplication;
    private int mAppWidgetId;

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
        for (int i = 0; i < ITEM_COUNT; i++) {
            mWidgetItems.add("http://static.guim.co.uk/sys-images/Guardian/Pix/pictures/2014/4/11/1397210130748/Spring-Lamb.-Image-shot-2-011.jpg");
        }
    }

    @Override
    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        mWidgetItems.clear();
    }

    @Override
    public int getCount() {
        return ITEM_COUNT;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        final AppWidgetManager manager = AppWidgetManager.getInstance(mApplication);

        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        final String url = mWidgetItems.get(position);
        final RemoteViews updateViews = new RemoteViews(mApplication.getPackageName(), R.layout.widget_item_layout);

        mApplication.getImageLoader()
                .loadImage(url, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        updateViews.setImageViewBitmap(R.id.widget_image, loadedImage);
                        updateViews.setTextViewText(R.id.widget_caption, url);
                        manager.updateAppWidget(mAppWidgetId, updateViews);
                    }
                });

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        updateViews.setOnClickFillInIntent(R.id.widget_image, fillInIntent);

        // You can do heaving lifting in here, synchronously. For example, if you need to
        // process an image, fetch something from the network, etc., it is ok to do it here,
        // synchronously. A loading view will show up in lieu of the actual contents in the
        // interim.

        // Return the remote views object.
        return updateViews;
    }

    @Override
    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
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
    }
}
