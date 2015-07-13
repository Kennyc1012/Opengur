package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MemeActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient2;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

import retrofit.client.Response;

/**
 * Created by Kenny-PC on 3/7/2015.
 */
public class MemeFragment extends BaseGridFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mListener != null) mListener.onUpdateActionBarTitle(getString(R.string.meme_gen));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.topics, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (getAdapter() != null) getAdapter().clear();
                mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
                fetchGallery();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void fetchGallery() {
        super.fetchGallery();
        ApiClient2.getService().getDefaultMemes(this);
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        startActivity(MemeActivity.createIntent(getActivity(), items.get(position)));
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);

        if (getAdapter() == null || getAdapter().isEmpty()) {
            List<ImgurBaseObject> memes = app.getSql().getMemes();

            if (memes != null && !memes.isEmpty()) {
                LogUtil.v(TAG, "Memes found in database");
                setUpGridTop();
                setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(memes), showPoints()));
                mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
                mHasMore = false;
            }
        }
    }

    @Override
    public void success(GalleryResponse galleryResponse, Response response) {
        super.success(galleryResponse, response);
        mHasMore = false;

        if (!galleryResponse.data.isEmpty()) {
            app.getSql().deleteMemes();
            app.getSql().addMemes(galleryResponse.data);
        }
    }

    @Override
    protected boolean showPoints() {
        return false;
    }
}
