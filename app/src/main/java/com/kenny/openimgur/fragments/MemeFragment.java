package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.MemeActivity;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.GalleryResponse;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.collections.SetUniqueList;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.SqlHelper;
import com.kennyc.view.MultiStateView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        inflater.inflate(R.menu.meme_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (getAdapter() != null) getAdapter().clear();
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                fetchGallery();
                return true;

            case R.id.importPhoto:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");

                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(intent, RequestCodes.SELECT_PHOTO);
                } else {
                    Snackbar.make(getSnackbarView(), R.string.cant_launch_intent, Snackbar.LENGTH_LONG).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void fetchGallery() {
        super.fetchGallery();
        ApiClient.getService().getDefaultMemes().enqueue(this);
    }

    @Override
    protected void onItemSelected(View view, int position, ArrayList<ImgurBaseObject> items) {
        if (isApiLevel(Build.VERSION_CODES.LOLLIPOP)) {

            View v = view.findViewById(R.id.image);

            ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity(), v, getString(R.string.gallery_item_transition));
            startActivity(MemeActivity.createIntent(getActivity(), items.get(position)), options.toBundle());
        } else {
            startActivity(MemeActivity.createIntent(getActivity(), items.get(position)));
        }
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);

        if (getAdapter() == null || getAdapter().isEmpty()) {
            List<ImgurBaseObject> memes = SqlHelper.getInstance(getActivity()).getMemes();

            if (!memes.isEmpty()) {
                LogUtil.v(TAG, "Memes found in database");
                setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(memes), this, showPoints()));
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                mHasMore = false;
            }
        }
    }

    @Override
    protected void onApiResult(GalleryResponse galleryResponse) {
        super.onApiResult(galleryResponse);
        mHasMore = false;

        if (!galleryResponse.data.isEmpty()) {
            SqlHelper sql = SqlHelper.getInstance(getActivity());
            sql.deleteFromTable(DBContracts.MemeContract.TABLE_NAME);
            sql.addMemes(galleryResponse.data);
        }
    }

    @Override
    protected boolean showPoints() {
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.SELECT_PHOTO && resultCode == Activity.RESULT_OK) {
            File file = FileUtil.createFile(data.getData(), getActivity());

            if (FileUtil.isFileValid(file)) {
                startActivity(MemeActivity.createIntent(getActivity(), file));
            } else {
                Snackbar.make(getSnackbarView(), R.string.upload_decode_failure, Snackbar.LENGTH_LONG).show();
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
