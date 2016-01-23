package com.kenny.openimgur.classes;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.List;

/**
 * Created by kcampagna on 8/14/14.
 */
public interface FragmentListener {
    int STATE_LOADING_COMPLETE = 1;
    int STATE_LOADING_STARTED = 2;
    int STATE_ERROR = 3;

    @IntDef({STATE_ERROR, STATE_LOADING_COMPLETE, STATE_LOADING_STARTED})
    @interface FragmentState {
    }

    void onUpdateActionBarSpinner(List<ImgurTopic> topics, @Nullable ImgurTopic currentTopic);

    void onUpdateActionBarTitle(String title);

    void onFragmentStateChange(@FragmentState int state);

    View getSnackbarView();
}
