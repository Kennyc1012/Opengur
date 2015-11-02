package com.kenny.openimgur.classes;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by kcampagna on 8/14/14.
 */
public interface FragmentListener {
    void onUpdateActionBarSpinner(List<ImgurTopic> topics, @Nullable ImgurTopic currentTopic);

    void onUpdateActionBarTitle(String title);

    void onUpdateActionBar(boolean shouldShow);

    void onLoadingStarted();

    void onLoadingComplete();

    void onError();
}
