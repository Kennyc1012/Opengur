package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.SideGalleryAdapter;
import com.kenny.openimgur.classes.ImgurBaseObject;

/**
 * Created by kcampagna on 9/27/14.
 */
public class SideGalleryFragment extends BaseFragment {
    private SideGalleryAdapter mAdapter;

    private ListView mListView;

    private SideGalleryListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = (ListView) view.findViewById(R.id.list);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (mListener != null) {
                    mListener.onItemSelected(position);
                }
            }
        });
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
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListView = null;
        mAdapter = null;
    }

    public void addGalleryItems(ImgurBaseObject[] galleryItems) {
        if (isAdded()) {
            mAdapter = new SideGalleryAdapter(getActivity(), app.getImageLoader(), galleryItems);
            mListView.setAdapter(mAdapter);
        }
    }

    /**
     * Notifies the list that the user has changed the item being view
     *
     * @param position
     */
    public void onPositionChanged(int position) {
        if (isAdded() && mAdapter != null) {
            mListView.setSelection(position);
        }
    }

    public interface SideGalleryListener {
        void onItemSelected(int position);
    }
}
