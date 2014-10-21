package com.kenny.openimgur.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
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
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.List;

/**
 * Created by kcampagna on 7/27/14.
 */
public class GalleryAdapter extends BaseAdapter {
    public static final int MAX_ITEMS = 200;

    private LayoutInflater mInflater;

    private ImageLoader mImageLoader;

    private SetUniqueList<ImgurBaseObject> mObjects;

    private String mThumbnailQuality;

    private DisplayImageOptions mOptions;

    public GalleryAdapter(Context context, List<ImgurBaseObject> objects) {
        mInflater = LayoutInflater.from(context);
        mImageLoader = OpenImgurApp.getInstance(context).getImageLoader();
        mObjects = SetUniqueList.decorate(objects);
        mOptions = ImageUtil.getDisplayOptionsForGallery().build();
        String quality = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
        mThumbnailQuality = ImgurPhoto.getQualityValue(quality);
    }

    /**
     * Clears all the items from the adapter
     */
    public void clear() {
        if (mObjects != null) {
            mObjects.clear();
        }

        notifyDataSetInvalidated();
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
            mObjects.addAll(items);
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

    public ImgurBaseObject[] getAllItems() {
        ImgurBaseObject[] items = new ImgurBaseObject[mObjects.size()];
        mObjects.toArray(items);
        return items;
    }

    @Override
    public int getCount() {
        return mObjects != null ? mObjects.size() : 0;
    }

    @Override
    public ImgurBaseObject getItem(int position) {
        return mObjects != null ? mObjects.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        ImgurBaseObject obj = getItem(position);
        String photoUrl;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gallery_item, parent, false);
            holder = new ViewHolder();
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.tv = (TextViewRoboto) convertView.findViewById(R.id.score);
            holder.imageViewAware = new ImageViewAware(holder.image, true);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Get the appropriate photo to display
        if (obj instanceof ImgurPhoto) {
            ImgurPhoto photoObject = ((ImgurPhoto) obj);

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasMP4Link() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(mThumbnailQuality, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto) obj).getThumbnail(mThumbnailQuality, false, null);
            }
        } else {
            photoUrl = ((ImgurAlbum) obj).getCoverUrl(mThumbnailQuality);
        }

        mImageLoader.cancelDisplayTask(holder.imageViewAware);
        mImageLoader.displayImage(photoUrl, holder.imageViewAware, mOptions);
        holder.tv.setText(obj.getScore() + " " + holder.tv.getContext().getString(R.string.points));
        return convertView;
    }

    private final static class ViewHolder {
        ImageView image;

        TextViewRoboto tv;

        ImageViewAware imageViewAware;
    }
}
