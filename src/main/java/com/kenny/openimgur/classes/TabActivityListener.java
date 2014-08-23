package com.kenny.openimgur.classes;

/**
 * Created by kcampagna on 8/14/14.
 */
public interface TabActivityListener {
    void oHideActionBar(boolean shouldShow);

    void onLoadingStarted(int page);

    void onLoadingComplete(int page);

    void onError(int errorCode, int page);
}
