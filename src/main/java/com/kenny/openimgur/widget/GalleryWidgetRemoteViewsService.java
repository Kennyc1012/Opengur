package com.kenny.openimgur.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.kenny.openimgur.classes.OpenImgurApp;

/**
 * Created by John on 4/3/2015.
 */
public class GalleryWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new GalleryWidgetRemoteViewsFactory((OpenImgurApp) getApplication(), intent);
    }

}