package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.SettingsActivity;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/27/14.
 */
public class GalleryAdapter extends BaseAdapter {
    public static final int MAX_ITEMS = 100;

    private LayoutInflater mInflater;

    private ImageLoader mImageLoader;

    private SetUniqueList<ImgurBaseObject> mObjects;

    private String mThumbnailQuality;

    public GalleryAdapter(Context context, List<ImgurBaseObject> objects, String quality) {
        mInflater = LayoutInflater.from(context);
        mImageLoader = OpenImgurApp.getInstance(context).getImageLoader();
        mObjects = SetUniqueList.decorate(objects);

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
            List<ImgurBaseObject> list = new ArrayList<ImgurBaseObject>();
            list.add(obj);
            mObjects = SetUniqueList.decorate(list);
        } else {
            mObjects.add(obj);
        }

        notifyDataSetChanged();
    }

    /**
     * Adds a list of items into the current list
     *
     * @param items
     */
    public void addItems(List<ImgurBaseObject> items) {
        if (mObjects == null) {
            mObjects = SetUniqueList.decorate(items);
        } else {
            for (ImgurBaseObject obj : items) {
                mObjects.add(obj);
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Returns a list of objects for the viewing activity. This will return a max of 100 items to avoid memory issues.
     * 50 before and 50 after the currently selected position. If there are not 50 available before or after, it will go to as many as it can
     *
     * @param position The position of the selected items
     * @return
     */
    public ImgurBaseObject[] getItems(int position) {
        List<ImgurBaseObject> objects;
        if (position - MAX_ITEMS / 2 < 0) {
            objects = mObjects.subList(0, mObjects.size() > MAX_ITEMS ? position + (MAX_ITEMS / 2) : mObjects.size());
        } else {
            objects = mObjects.subList(position - (MAX_ITEMS / 2), position + (MAX_ITEMS / 2) <= mObjects.size() ? position + (MAX_ITEMS / 2) : mObjects.size());
        }

        ImgurBaseObject[] array = new ImgurBaseObject[objects.size()];
        objects.toArray(array);
        return array;
    }

    public ImgurBaseObject[] getAllitems() {
        ImgurBaseObject[] items = new ImgurBaseObject[mObjects.size()];
        mObjects.toArray(items);
        return items;
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
            holder.tv = (TextViewRoboto) convertView.findViewById(R.id.score);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ImgurBaseObject obj = getItem(position);
        mImageLoader.cancelDisplayTask(holder.image);
        String photoUrl;

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


    private final static class ViewHolder {
        ImageView image;

        TextViewRoboto tv;
    }
}
