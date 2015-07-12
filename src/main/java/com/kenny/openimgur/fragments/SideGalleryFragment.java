package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.classes.ImgurBaseObject2;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;

import butterknife.InjectView;

/**
 * Created by kcampagna on 9/27/14.
 */
public class SideGalleryFragment extends BaseFragment implements AdapterView.OnItemClickListener {
    @InjectView(R.id.list)
    ListView mListView;

    private GalleryAdapter mAdapter;

    private SideGalleryListener mListener;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int padding = (int) getResources().getDimension(R.dimen.content_padding);
        view.setPadding(padding, 0, padding, 0);
        view.setBackgroundColor(Color.TRANSPARENT);
        mListView = (ListView) view.findViewById(R.id.list);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mListener != null) {
            mListener.onItemSelected(position);
        }
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
        mAdapter = null;
        super.onDestroyView();
    }

    public void addGalleryItems(ArrayList<ImgurBaseObject2> galleryItems) {
        if (isAdded()) {
            mAdapter = new GalleryAdapter(getActivity(), SetUniqueList.decorate(galleryItems));
            mListView.setAdapter(mAdapter);
        }
    }

    /**
     * Notifies the list that the user has changed the item being view
     *
     * @param position
     */
    public void onPositionChanged(int position) {
        if (isAdded()) {
            mListView.setSelection(position);
        }
    }

    public interface SideGalleryListener {
        void onItemSelected(int position);
    }
}
