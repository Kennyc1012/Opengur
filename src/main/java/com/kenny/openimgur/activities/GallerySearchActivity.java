package com.kenny.openimgur.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.RelativeLayout;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.fragments.GallerySearchFragment;
import com.kenny.openimgur.util.ViewUtils;

import butterknife.InjectView;

/**
 * Created by kcampagna on 3/21/15.
 */
public class GallerySearchActivity extends BaseActivity implements FragmentListener {
    private static final String KEY_QUERY = "query";

    private GallerySearchFragment mFragment;

    @InjectView(R.id.toolBar)
    Toolbar mToolBar;

    public static Intent createIntent(Context context, String query) {
        return new Intent(context, GallerySearchActivity.class).putExtra(KEY_QUERY, query);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_search);
        mFragment = (GallerySearchFragment) getFragmentManager().findFragmentById(R.id.searchFragment);
        String query;

        if (savedInstanceState != null) {
            query = savedInstanceState.getString(KEY_QUERY, null);
        } else {
            query = getIntent().getStringExtra(KEY_QUERY);
        }

        setupToolBar(query);
        mFragment.setQuery(query);
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar(String query) {
        if (isLandscape() && !isTablet()) {
            // Don't add the extra padding
        } else {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mToolBar.getLayoutParams();
            lp.setMargins(0, ViewUtils.getStatusBarHeight(getApplicationContext()), 0, 0);
            mToolBar.setLayoutParams(lp);
        }

        mToolBar.setBackgroundColor(getResources().getColor(app.getImgurTheme().primaryColor));
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
    public void onError(int errorCode) {

    }
}
