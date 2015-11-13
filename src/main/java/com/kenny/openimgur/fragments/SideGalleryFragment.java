package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.GalleryAdapter2;
import com.kenny.openimgur.classes.ImgurBaseObject;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;

import butterknife.Bind;

/**
 * Created by kcampagna on 9/27/14.
 */
public class SideGalleryFragment extends BaseFragment implements AdapterView.OnItemClickListener, View.OnClickListener {
    @Bind(R.id.list)
    RecyclerView mList;

    private GalleryAdapter2 mAdapter;

    private SideGalleryListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_side_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int padding = (int) getResources().getDimension(R.dimen.content_padding);
        view.setPadding(padding, 0, padding, 0);
        view.setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onClick(View v) {
        int position = mList.getChildAdapterPosition(v);
        if (mListener != null) mListener.onItemSelected(position);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof SideGalleryListener) {
            mListener = (SideGalleryListener) activity;
        }
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) {
            mAdapter.onDestroy();
            mAdapter = null;
        }

        super.onDestroyView();
    }

    public void addGalleryItems(ArrayList<ImgurBaseObject> galleryItems) {
        if (isAdded()) {
            mAdapter = new GalleryAdapter2(getActivity(), SetUniqueList.decorate(galleryItems), this, false);
            mList.setAdapter(mAdapter);
        }
    }

    /**
     * Notifies the list that the user has changed the item being view
     *
     * @param position
     */
    public void onPositionChanged(int position) {
        if (isAdded()) {
            mList.scrollToPosition(position);
        }
    }

    public interface SideGalleryListener {
        void onItemSelected(int position);
    }
}
