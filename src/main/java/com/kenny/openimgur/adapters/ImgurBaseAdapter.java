package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.kenny.openimgur.classes.OpenImgurApp;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 11/15/14.
 */
public abstract class ImgurBaseAdapter extends BaseAdapter {
    protected final String TAG = getClass().getSimpleName();

    private List<Object> mItems;

    private ImageLoader mImageLoader;

    protected LayoutInflater mInflater;

    public ImgurBaseAdapter(Context context, List collection, boolean hasImageLoader) {
        if (hasImageLoader) mImageLoader = OpenImgurApp.getInstance(context).getImageLoader();
        mItems = collection;
        mInflater = LayoutInflater.from(context);
    }

    public ImgurBaseAdapter(Context context, List collection) {
        this(context, collection, false);
    }

    /**
     * Adds an item to the list
     *
     * @param object
     */
    public void addItem(Object object) {
        mItems.add(object);
        notifyDataSetChanged();
    }

    /**
     * Adds a list of items to the adapter list
     *
     * @param items
     */
    public void addItems(List items) {
        if (mItems == null) {
            mItems = items;
        } else {
            mItems.addAll(items);
        }

        notifyDataSetChanged();
    }

    /**
     * Removes an object from the list
     *
     * @param object
     */
    public void removeItem(Object object) {
        if (mItems != null) {
            mItems.remove(object);
            notifyDataSetChanged();
        }
    }

    /**
     * Removes an item at the given position.
     *
     * @param position
     * @return The item removed
     */
    public Object removeItem(int position) {
        if (mItems != null) {
            Object removedItem = mItems.remove(position);
            notifyDataSetChanged();
            return removedItem;
        }

        return null;
    }

    /**
     * Removes all items from the list
     */
    public void clear() {
        if (mItems != null) {
            mItems.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * Returns the entire list
     *
     * @return
     */
    protected List getAllItems() {
        return mItems;
    }

    @Override
    public int getCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return mItems != null ? mItems.get(position) : null;
    }

    /**
     * Displays the image
     *
     * @param imageView
     * @param url
     */
    protected void displayImage(ImageView imageView, String url) {
        if (mImageLoader == null) {
            throw new NullPointerException("Image Loader has not been created");
        }

        mImageLoader.cancelDisplayTask(imageView);
        mImageLoader.displayImage(url, imageView, getDisplayOptions());
    }

    /**
     * Returns the display options to be used for the image loader in the adapter
     *
     * @return
     */
    protected abstract DisplayImageOptions getDisplayOptions();

    /**
     * Returns an ArrayList of the items in the adapter, used for saving the items for configuration changes
     *
     * @return
     */
    public abstract ArrayList<?> retainItems();

    public abstract static class ImgurViewHolder {
        public ImgurViewHolder(View view) {
            ButterKnife.inject(this, view);
            view.setTag(this);
        }
    }
}
