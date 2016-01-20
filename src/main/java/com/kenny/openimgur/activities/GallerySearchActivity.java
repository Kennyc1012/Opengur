package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.fragments.GallerySearchFragment;

import java.util.List;

import butterknife.Bind;

/**
 * Created by kcampagna on 3/21/15.
 */
public class GallerySearchActivity extends BaseActivity implements FragmentListener {
    private static final String KEY_QUERY = "query";

    @Bind(R.id.toolBar)
    Toolbar mToolBar;

    @Bind(R.id.coordinatorLayout)
    CoordinatorLayout mCoordinatorLayout;

    public static Intent createIntent(Context context, String query) {
        return new Intent(context, GallerySearchActivity.class).putExtra(KEY_QUERY, query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColorResource(theme.darkColor);
        setContentView(R.layout.activity_gallery_search);
        GallerySearchFragment fragment = (GallerySearchFragment) getFragmentManager().findFragmentById(R.id.searchFragment);
        String query;

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(KEY_QUERY, null);
        } else {
            Intent intent = getIntent();

            if (isApiLevel(Build.VERSION_CODES.M) && intent.getAction().equals(Intent.ACTION_PROCESS_TEXT)) {
                query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT);
                if (TextUtils.isEmpty(query)) query = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT_READONLY);
            } else {
                query = intent.getStringExtra(KEY_QUERY);
            }
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
    public void onFragmentStateChange(@FragmentState int state) {
        // NOOP
    }

    @Override
    public void onUpdateActionBarSpinner(List<ImgurTopic> topics, @Nullable ImgurTopic currentTopic) {
        // NOOP
    }

    @Override
    public View getSnackbarView() {
        return mCoordinatorLayout;
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Main_Dark : R.style.Theme_Opengur_Light_Main_Light;
    }
}
