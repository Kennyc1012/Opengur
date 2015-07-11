package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.classes.ImgurAlbum2;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurBaseObject2;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurPhoto2;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

/**
 * Created by kcampagna on 7/27/14.
 */
public class GalleryAdapter2 extends ImgurBaseAdapter<ImgurBaseObject2> {
    public static final int MAX_ITEMS = 200;

    private int mUpvoteColor;

    private int mDownVoteColor;

    private boolean mAllowNSFWThumb;

    private String mThumbnailQuality;

    public GalleryAdapter2(Context context, SetUniqueList<ImgurBaseObject2> objects) {
        super(context, objects, true);
        mUpvoteColor = context.getResources().getColor(R.color.notoriety_positive);
        mDownVoteColor = context.getResources().getColor(R.color.notoriety_negative);
        SharedPreferences pref = OpengurApp.getInstance(context).getPreferences();
        mAllowNSFWThumb = pref.getBoolean(SettingsActivity.KEY_NSFW_THUMBNAILS, false);
        mThumbnailQuality = pref.getString(SettingsActivity.KEY_THUMBNAIL_QUALITY, ImgurPhoto.THUMBNAIL_GALLERY);
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForGallery().build();
    }

    /**
     * Returns a list of objects for the viewing activity. This will return a max of 200 items to avoid memory issues.
     * 100 before and 100 after the currently selected position. If there are not 100 available before or after, it will go to as many as it can
     *
     * @param position The position of the selected items
     * @return
     */
    public ArrayList<ImgurBaseObject2> getItems(int position) {
        List<ImgurBaseObject2> objects;
        int size = getCount();

        if (position - MAX_ITEMS / 2 < 0) {
            objects = getAllItems().subList(0, size > MAX_ITEMS ? position + (MAX_ITEMS / 2) : size);
        } else {
            objects = getAllItems().subList(position - (MAX_ITEMS / 2), position + (MAX_ITEMS / 2) <= size ? position + (MAX_ITEMS / 2) : size);
        }

        return new ArrayList<>(objects);
    }

    /**
     * Removes an item from the adapter given an id
     *
     * @param id The id of the item
     * @return If the item was removed
     */
    public boolean removeItem(String id) {
        List<ImgurBaseObject2> items = getAllItems();
        boolean removed = false;

        for (ImgurBaseObject2 obj : items) {
            if (obj.getId().equals(id)) {
                removeItem(obj);
                removed = true;
                break;
            }
        }

        return removed;
    }

    public void setAllowNSFW(boolean allowNSFW) {
        mAllowNSFWThumb = allowNSFW;
        notifyDataSetChanged();
    }

    public void setThumbnailQuality(String quality) {
        if (!mThumbnailQuality.equals(quality)) {
            LogUtil.v(TAG, "Updating thumbnail quality to " + quality);
            // Clear any memory cache we may have for the new thumbnail
            mImageLoader.clearMemoryCache();
            mThumbnailQuality = quality;
            notifyDataSetChanged();
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GalleryHolder holder;
        ImgurBaseObject2 obj = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gallery_item, parent, false);
            holder = new GalleryHolder(convertView);
        } else {
            holder = (GalleryHolder) convertView.getTag();
        }

        // Get the appropriate photo to display
        if (obj.isNSFW() && !mAllowNSFWThumb) {
            holder.image.setImageResource(R.drawable.ic_nsfw);
        } else if (obj instanceof ImgurPhoto2) {
            ImgurPhoto2 photoObject = ((ImgurPhoto2) obj);
            String photoUrl;

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasMP4Link() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(mThumbnailQuality, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto2) obj).getThumbnail(mThumbnailQuality, false, null);
            }

            displayImage(holder.image, photoUrl);

        } else if (obj instanceof ImgurAlbum2) {
            displayImage(holder.image, ((ImgurAlbum2) obj).getCoverUrl(mThumbnailQuality));
        } else {
            String url = ImgurBaseObject.getThumbnail(obj.getId(), obj.getLink(), mThumbnailQuality);
            displayImage(holder.image, url);
        }

        if (obj.getUpVotes() != Integer.MIN_VALUE) {
            holder.score.setText((obj.getUpVotes() - obj.getDownVotes()) + " " + holder.score.getContext().getString(R.string.points));
            holder.score.setVisibility(View.VISIBLE);
        } else {
            holder.score.setVisibility(View.GONE);
        }

        if (obj.isFavorited() || ImgurBaseObject.VOTE_UP.equals(obj.getVote())) {
            holder.score.setTextColor(mUpvoteColor);
        } else if (ImgurBaseObject.VOTE_DOWN.equals(obj.getVote())) {
            holder.score.setTextColor(mDownVoteColor);
        } else {
            holder.score.setTextColor(Color.WHITE);
        }

        return convertView;
    }

    static class GalleryHolder extends ImgurViewHolder {
        @InjectView(R.id.image)
        ImageView image;

        @InjectView(R.id.score)
        TextView score;

        public GalleryHolder(View view) {
            super(view);
        }
    }
}
