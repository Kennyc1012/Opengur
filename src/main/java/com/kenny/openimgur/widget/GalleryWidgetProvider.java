package com.kenny.openimgur.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.RemoteViews;

import com.kenny.openimgur.R;

/**
 * Widget provider which tells the launcher to update the widgets
 * Created by John on 4/3/2015.
 */
public class GalleryWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_VIEW = "ACTION_VIEW";

    public static final String EXTRA_ITEM = "EXTRA_ITEM";

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_NEXT)) {
            RemoteViews rv = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            rv.showNext(R.id.widget_list);

            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(
                    intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID), rv);
        } else if (intent.getAction().equals(ACTION_PREVIOUS)) {
            RemoteViews rv = new RemoteViews(context.getPackageName(),
                    R.layout.widget_layout);

            rv.showPrevious(R.id.widget_list);

            AppWidgetManager.getInstance(context).partiallyUpdateAppWidget(
                    intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                            AppWidgetManager.INVALID_APPWIDGET_ID), rv);
        } else if (intent.getAction().equals(ACTION_VIEW)) {
            Intent viewIntent = new Intent(Intent.ACTION_VIEW);
            viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String url = intent.getStringExtra(EXTRA_ITEM);
            if (TextUtils.isEmpty(url)) {
                return;
            }
            viewIntent.setData(Uri.parse(url));
            try {
                context.startActivity(viewIntent);
            } catch (ActivityNotFoundException e) {
                //This will never happen since we can handle the link
            }
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // update each of the widgets with the remote adapter
        for (int widgetId : appWidgetIds) {

            // Here we setup the intent which points to the StackViewService which will
            // provide the views for this collection.
            Intent intent = new Intent(context, GalleryWidgetRemoteViewsService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            // When intents are compared, the extras are ignored, so we need to embed the extras
            // into the data so that the extras will not be ignored.
            intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
            rv.setRemoteAdapter(R.id.widget_list, intent);

            // The empty view is displayed when the collection has no items. It should be a sibling
            // of the collection view.
            rv.setEmptyView(R.id.widget_list, R.id.widget_empty);

            setIntents(context, widgetId, rv);

            appWidgetManager.updateAppWidget(widgetId, rv);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private void setIntents(Context context, int widgetId, RemoteViews rv) {
        Intent nextIntent = new Intent(context, GalleryWidgetProvider.class);
        nextIntent.setAction(GalleryWidgetProvider.ACTION_NEXT);
        nextIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        nextIntent.setData(Uri.parse(nextIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent nextPendingIntent = PendingIntent.getBroadcast(context, 0, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_next, nextPendingIntent);

        Intent previousIntent = new Intent(context, GalleryWidgetProvider.class);
        previousIntent.setAction(GalleryWidgetProvider.ACTION_PREVIOUS);
        previousIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        previousIntent.setData(Uri.parse(previousIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent previousPendingIntent = PendingIntent.getBroadcast(context, 0, previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setOnClickPendingIntent(R.id.widget_previous, previousPendingIntent);

        // Here we setup the a pending intent template. Individuals items of a collection
        // cannot setup their own pending intents, instead, the collection as a whole can
        // setup a pending intent template, and the individual items can set a fillInIntent
        // to create unique before on an item to item basis.
        Intent toastIntent = new Intent(context, GalleryWidgetProvider.class);
        toastIntent.setAction(GalleryWidgetProvider.ACTION_VIEW);
        toastIntent.setData(Uri.parse(toastIntent.toUri(Intent.URI_INTENT_SCHEME)));
        PendingIntent toastPendingIntent = PendingIntent.getBroadcast(context, 0, toastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        rv.setPendingIntentTemplate(R.id.widget_list, toastPendingIntent);

    }
}