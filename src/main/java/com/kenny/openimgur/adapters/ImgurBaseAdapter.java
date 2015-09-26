package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 11/15/14.
 */
public abstract class ImgurBaseAdapter<T> extends BaseAdapter {
    protected final String TAG = getClass().getSimpleName();

    private List<T> mItems;

    protected ImageLoader mImageLoader;

    protected LayoutInflater mInflater;

    protected Resources mResources;

    public ImgurBaseAdapter(Context context, List<T> collection, boolean hasImageLoader) {
        if (hasImageLoader) mImageLoader = OpengurApp.getInstance(context).getImageLoader();
        mItems = collection;
        mInflater = LayoutInflater.from(context);
        mResources = context.getResources();
    }

    public ImgurBaseAdapter(Context context, List<T> collection) {
        this(context, collection, false);
    }

    /**
     * Adds an item to the list
     *
     * @param object
     */
    public void addItem(T object) {
        mItems.add(object);
        notifyDataSetChanged();
    }

    /**
     * Adds a list of items to the adapter list
     *
     * @param items
     */
    public void addItems(List<T> items) {
        if (items == null || items.isEmpty()) {
            LogUtil.w(TAG, "List is empty or null");
            return;
        }

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
    public void removeItem(T object) {
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
    public T removeItem(int position) {
        if (mItems != null) {
            T removedItem = mItems.remove(position);
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
    protected List<T> getAllItems() {
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
    public T getItem(int position) {
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
            throw new IllegalStateException("Image Loader has not been created");
        }

        mImageLoader.cancelDisplayTask(imageView);
        mImageLoader.displayImage(url, imageView, getDisplayOptions());
    }

    /**
     * Returns the display options to be used for the image loader in the adapter
     *
     * @return
     */
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDefaultDisplayOptions().build();
    }

    /**
     * Returns an ArrayList of the items in the adapter, used for saving the items for configuration changes
     *
     * @return
     */
    public ArrayList<T> retainItems() {
        return new ArrayList<>(mItems);
    }

    public abstract static class ImgurViewHolder {
        public ImgurViewHolder(View view) {
            ButterKnife.bind(this, view);
            view.setTag(this);
        }
    }
}
