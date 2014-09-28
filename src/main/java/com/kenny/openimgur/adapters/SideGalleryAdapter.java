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
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

/**
 * Created by kcampagna on 9/27/14.
 */
public class SideGalleryAdapter extends BaseAdapter {
    private ImgurBaseObject[] mGalleryItems;

    private LayoutInflater mInflater;

    private String mQuality;

    private ImageLoader mImageLoader;

    private DisplayImageOptions mOptions;

    public SideGalleryAdapter(Context context, ImageLoader imageLoader, ImgurBaseObject[] objects) {
        mInflater = LayoutInflater.from(context);
        mImageLoader = imageLoader;
        String quality = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
        mGalleryItems = objects;
        mOptions = ImageUtil.getDisplayOptionsForGallery().build();

        if (SettingsActivity.THUMBNAIL_QUALITY_LOW.equals(quality)) {
            mQuality = ImgurPhoto.THUMBNAIL_SMALL;
        } else if (SettingsActivity.THUMBNAIL_QUALITY_MEDIUM.equals(quality)) {
            mQuality = ImgurPhoto.THUMBNAIL_MEDIUM;
        } else {
            mQuality = ImgurPhoto.THUMBNAIL_LARGE;
        }
    }

    @Override
    public int getCount() {
        return mGalleryItems != null ? mGalleryItems.length : 0;
    }

    @Override
    public Object getItem(int i) {
        return mGalleryItems != null ? mGalleryItems[i] : null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.gallery_side_item, parent, false);
            holder.image = new ImageViewAware((ImageView) convertView.findViewById(R.id.image), true);
            holder.title = (TextViewRoboto) convertView.findViewById(R.id.title);
            holder.points = (TextViewRoboto) convertView.findViewById(R.id.points);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ImgurBaseObject object = mGalleryItems[position];
        String photoUrl;

        // Get the appropriate photo to display
        if (object instanceof ImgurPhoto) {
            photoUrl = ((ImgurPhoto) object).getThumbnail(mQuality);
        } else {
            photoUrl = ((ImgurAlbum) object).getCoverUrl(mQuality);
        }

        mImageLoader.cancelDisplayTask(holder.image);
        mImageLoader.displayImage(photoUrl, holder.image, mOptions);
        holder.title.setText(object.getTitle());
        holder.points.setText(object.getScore() + " " + holder.points.getContext().getString(R.string.points));
        return convertView;
    }

    static class ViewHolder {
        ImageViewAware image;

        TextViewRoboto title, points;
    }
}
