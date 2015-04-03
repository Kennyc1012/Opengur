package com.kenny.openimgur.widget;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * Service to allow asynchronous gallery_widget updating
 * Created by John on 4/3/2015.
 */
public class GalleryWidgetService extends IntentService {

    private static final String TAG = GalleryWidgetService.class.getSimpleName();

    private static final String ACTION_UPDATE_WIDGET = "action_update_widget";
    private static final String EXTRA_WIDGET_ID = "extra_widget_id";

    /**
     * Starts the service which will update the gallery_widget
     * @param context context to start the service
     * @param widgetId id of the gallery_widget to update
     */
    public static void updateWidget(Context context, int widgetId) {
        Intent intent = new Intent(context, GalleryWidgetService.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        intent.putExtra(EXTRA_WIDGET_ID, widgetId);
        context.startService(intent);
    }

    public GalleryWidgetService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case ACTION_UPDATE_WIDGET:
                updateWidget(intent.getIntExtra(EXTRA_WIDGET_ID, -1));
                break;
            default:
                break;
        }
    }

    private void updateWidget(final int widgetId) {
        if (widgetId == -1) {
            return;
        }
        
        final AppWidgetManager manager = AppWidgetManager.getInstance(this);
        final RemoteViews updateViews = new RemoteViews(getPackageName(), R.layout.widget_layout);
        final String testImage = "http://static.guim.co.uk/sys-images/Guardian/Pix/pictures/2014/4/11/1397210130748/Spring-Lamb.-Image-shot-2-011.jpg";
        ((OpenImgurApp)getApplication()).getImageLoader()
                .loadImage(testImage, new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        updateViews.setImageViewBitmap(R.id.widget_image, loadedImage);
                        updateViews.setTextViewText(R.id.widget_caption, testImage);
                        manager.updateAppWidget(widgetId, updateViews);
                    }
                });
    }
}
