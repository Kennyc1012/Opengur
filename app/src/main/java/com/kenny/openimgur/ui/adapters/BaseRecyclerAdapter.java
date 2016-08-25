package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import butterknife.ButterKnife;

/**
 * Created by kcampagna on 8/25/16.
 */

public abstract class BaseRecyclerAdapter<T> extends com.kennyc.adapters.BaseRecyclerAdapter<T, BaseRecyclerAdapter.BaseViewHolder> {

    protected final String TAG = getClass().getSimpleName();

    protected boolean isDarkTheme = false;

    private ImageLoader imageLoader;

    public BaseRecyclerAdapter(Context context, List<T> collection, boolean hasImageLoader) {
        super(context, collection);
        if (hasImageLoader) imageLoader = ImageUtil.getImageLoader(context);
        isDarkTheme = OpengurApp.getInstance(context).getImgurTheme().isDarkTheme;
    }

    /**
     * Simple constructor for creating a BaseRecyclerAdapter
     *
     * @param context    The context the adapter is running in
     * @param collection A list of items to populate the adapter with, can be null. If passing a null list,
     *                   {@link #addItem(Object)} will throw an exception as the list type is undefined. The list
     *                   needs to be created first with {@link #addItems(List)}
     */
    public BaseRecyclerAdapter(Context context, List<T> collection) {
        this(context, collection, false);
    }

    /**
     * Displays the image
     *
     * @param imageView
     * @param url
     */
    protected void displayImage(ImageView imageView, String url) {
        if (imageLoader == null) {
            throw new IllegalStateException("Image Loader has not been created");
        }

        imageLoader.cancelDisplayTask(imageView);
        imageLoader.displayImage(url, imageView, getDisplayOptions());
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
     * Frees up any resources tied to the adapter. Should be called in an activities onDestroy lifecycle method if needed
     */
    public void onDestroy() {
        super.onDestroy();
        LogUtil.v(TAG, "onDestroy");
    }

    public abstract static class BaseViewHolder extends RecyclerView.ViewHolder {
        public BaseViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}
