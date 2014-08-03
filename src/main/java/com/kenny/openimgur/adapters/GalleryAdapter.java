package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.R;
import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/27/14.
 */
public class GalleryAdapter extends BaseAdapter {
    private LayoutInflater mInflater;

    private ImageLoader mImageLoader;

    private List<ImgurBaseObject> mObjects;

    private String mThumbnailQuality;

    public GalleryAdapter(Context context, ImageLoader imageLoader, List<ImgurBaseObject> objects, String quality) {
        mInflater = LayoutInflater.from(context);
        mImageLoader = imageLoader;
        this.mObjects = objects;

        if (SettingsActivity.THUMBNAIL_QUALITY_LOW.equals(quality)) {
            mThumbnailQuality = ImgurPhoto.THUMBNAIL_SMALL;
        } else if (SettingsActivity.THUMBNAIL_QUALITY_MEDIUM.equals(quality)) {
            mThumbnailQuality = ImgurPhoto.THUMBNAIL_MEDIUM;
        } else {
            mThumbnailQuality = ImgurPhoto.THUMBNAIL_LARGE;
        }
    }

    /**
     * Clears all the items from the adapter
     */
    public void clear() {
        if (mObjects != null) {
            mObjects.clear();
        }

        notifyDataSetChanged();
    }

    /**
     * Adds an object to the adapter
     *
     * @param obj
     */
    public void addItem(ImgurBaseObject obj) {
        if (mObjects == null) {
            mObjects = new ArrayList<ImgurBaseObject>();
        }

        mObjects.add(obj);
        notifyDataSetChanged();
    }

    /**
     * Adds a list of items into the current list
     *
     * @param items
     */
    public void addItems(List<ImgurBaseObject> items) {
        if (mObjects == null) {
            mObjects = items;
        } else {
            for (ImgurBaseObject obj : items) {
                mObjects.add(obj);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Returns the entire list of objects
     *
     * @return
     */
    public List<ImgurBaseObject> getItems() {
        return mObjects;
    }

    @Override
    public int getCount() {
        if (mObjects != null) {
            return mObjects.size();
        }
        return 0;
    }

    @Override
    public ImgurBaseObject getItem(int position) {
        if (mObjects != null) {
            return mObjects.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gallery_item, parent, false);
            holder = new ViewHolder();
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.tv = (RobotoTextView) convertView.findViewById(R.id.score);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ImgurBaseObject obj = getItem(position);
        mImageLoader.cancelDisplayTask(holder.image);
        String photoUrl = null;

        // Get the appropriate photo to display
        if (obj instanceof ImgurPhoto) {
            photoUrl = ((ImgurPhoto) obj).getThumbnail(mThumbnailQuality);
        } else {
            photoUrl = ((ImgurAlbum) obj).getCoverUrl(mThumbnailQuality);
        }

        mImageLoader.displayImage(photoUrl, holder.image);
        holder.tv.setText(obj.getScore() + " " + holder.tv.getContext().getString(R.string.points));
        return convertView;
    }

    static class ViewHolder {
        ImageView image;

        RobotoTextView tv;
    }
}
