package com.kenny.openimgur.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/**
 * Widget provider which tells the launcher to update the widgets
 * Created by John on 4/3/2015.
 */
public class GalleryWidgetProvider extends AppWidgetProvider {

    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Perform this loop procedure for each App Widget that belongs to this provider
        for (int appWidgetId : appWidgetIds) {
            //Defer the update to the service to prevent ANRs
            GalleryWidgetService.updateWidget(context, appWidgetId);
        }
    }
}