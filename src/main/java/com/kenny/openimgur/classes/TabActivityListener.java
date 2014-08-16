package com.kenny.openimgur.classes;

/**
 * Created by kcampagna on 8/14/14.
 */
public interface TabActivityListener {
    void oHideActionBar(boolean shouldShow);

    void onLoadingStarted();

    void onLoadingComplete();

    void onError(int errorCode);
}
