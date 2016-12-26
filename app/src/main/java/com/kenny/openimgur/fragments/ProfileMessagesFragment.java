package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ProfileMessagesFragment extends BaseFragment {

    // TODO Remove after several versions

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_convos, container, false);
    }
}
