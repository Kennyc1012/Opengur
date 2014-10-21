package com.kenny.openimgur.classes;

/**
 * Created by kcampagna on 8/14/14.
 */
public interface FragmentListener {
    void onUpdateActionBarTitle(String title);

    void onHideActionBar(boolean shouldShow);

    void onLoadingStarted();

    void onLoadingComplete();

    void onError(int errorCode);
}
