package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.fragments.GallerySearchFragment;

import butterknife.Bind;

/**
 * Created by kcampagna on 3/21/15.
 */
public class GallerySearchActivity extends BaseActivity implements FragmentListener {
    private static final String KEY_QUERY = "query";

    @Bind(R.id.toolBar)
    Toolbar mToolBar;

    public static Intent createIntent(Context context, String query) {
        return new Intent(context, GallerySearchActivity.class).putExtra(KEY_QUERY, query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_search);
        setStatusBarColorResource(theme.darkColor);
        GallerySearchFragment fragment = (GallerySearchFragment) getFragmentManager().findFragmentById(R.id.searchFragment);
        String query;

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(KEY_QUERY, null);
        } else {
            query = getIntent().getStringExtra(KEY_QUERY);
        }

        setupToolBar(query);
        fragment.setQuery(query);
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar(String query) {
        mToolBar.setTitle(query);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void onUpdateActionBarTitle(String title) {
        if (mToolBar != null) mToolBar.setTitle(title);
    }

    @Override
    public void onUpdateActionBar(boolean shouldShow) {
        setActionBarVisibility(mToolBar, shouldShow);
    }

    @Override
    public void onLoadingStarted() {
        // NOOP
    }

    @Override
    public void onLoadingComplete() {
        // NOOP
    }

    @Override
    public void onError() {
        // NOOP
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Translucent_Main_Dark : R.style.Theme_Translucent_Main_Light;
    }
}
