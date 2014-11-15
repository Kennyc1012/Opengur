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
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import java.util.ArrayList;

/**
 * Created by kcampagna on 9/27/14.
 */
public class SideGalleryAdapter extends BaseAdapter {
    private ArrayList<ImgurBaseObject> mGalleryItems;

    private LayoutInflater mInflater;

    private String mQuality;

    private ImageLoader mImageLoader;

    private DisplayImageOptions mOptions;

    public SideGalleryAdapter(Context context, ImageLoader imageLoader, ArrayList<ImgurBaseObject> objects) {
        mInflater = LayoutInflater.from(context);
        mImageLoader = imageLoader;
        mGalleryItems = objects;
        mOptions = ImageUtil.getDisplayOptionsForGallery().build();
        String quality = PreferenceManager.getDefaultSharedPreferences(context).getString(SettingsActivity.THUMBNAIL_QUALITY_KEY, SettingsActivity.THUMBNAIL_QUALITY_LOW);
        mQuality = ImgurPhoto.getQualityValue(quality);
    }

    @Override
    public int getCount() {
        return mGalleryItems != null ? mGalleryItems.size() : 0;
    }

    @Override
    public ImgurBaseObject getItem(int i) {
        return mGalleryItems != null ? mGalleryItems.get(i) : null;
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

        ImgurBaseObject object = getItem(position);
        String photoUrl;

        // Get the appropriate photo to display
        if (object instanceof ImgurPhoto) {
            ImgurPhoto photoObject = ((ImgurPhoto) object);

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasMP4Link() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(mQuality, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto) object).getThumbnail(mQuality, false, null);
            }
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
