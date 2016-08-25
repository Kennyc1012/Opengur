package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.adapters.GalleryAdapter;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.collections.SetUniqueList;
import com.kenny.openimgur.util.ViewUtils;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * Created by kcampagna on 9/27/14.
 */
public class SideGalleryFragment extends BaseFragment implements View.OnClickListener {
    @BindView(R.id.list)
    RecyclerView mList;

    private GalleryAdapter mAdapter;

    private SideGalleryListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_side_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewUtils.setRecyclerViewGridDefaults(getActivity(), mList, 1, getResources().getDimensionPixelOffset(R.dimen.content_padding));
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
            mAdapter = new GalleryAdapter(getActivity(), SetUniqueList.decorate(galleryItems), this, false);
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
