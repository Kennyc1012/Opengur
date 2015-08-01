package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.SettingsActivity;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.ui.GridItemDecoration;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;

public class GalleryAdapter2 extends BaseRecyclerAdapter<ImgurBaseObject> {
    public static final int MAX_ITEMS = 200;

    private int mUpvoteColor;

    private int mDownVoteColor;

    private boolean mAllowNSFWThumb;

    private View.OnClickListener mClickListener;

    private boolean mShowPoints = true;

    private String mThumbnailQuality;

    public GalleryAdapter2(Context context, RecyclerView view, SetUniqueList<ImgurBaseObject> objects, View.OnClickListener listener) {
        super(context, objects, true);
        Resources res = context.getResources();
        int gridSize = res.getInteger(R.integer.gallery_num_columns);
        view.setLayoutManager(new GridLayoutManager(context, gridSize));
        view.addItemDecoration(new GridItemDecoration(res.getDimensionPixelSize(R.dimen.grid_padding), gridSize));
        mUpvoteColor = res.getColor(R.color.notoriety_positive);
        mDownVoteColor = res.getColor(R.color.notoriety_negative);
        SharedPreferences pref = OpengurApp.getInstance(context).getPreferences();
        mAllowNSFWThumb = pref.getBoolean(SettingsActivity.KEY_NSFW_THUMBNAILS, false);
        mThumbnailQuality = pref.getString(SettingsActivity.KEY_THUMBNAIL_QUALITY, ImgurPhoto.THUMBNAIL_GALLERY);
        mClickListener = listener;
    }

    public GalleryAdapter2(Context context, RecyclerView view, SetUniqueList<ImgurBaseObject> objects, View.OnClickListener listener, boolean showPoints) {
        this(context, view, objects, listener);
        mShowPoints = showPoints;
    }

    @Override
    public void onDestroy() {
        mClickListener = null;
        clear();
        super.onDestroy();
    }

    /**
     * Returns a list of objects for the viewing activity. This will return a max of 200 items to avoid memory issues.
     * 100 before and 100 after the currently selected position. If there are not 100 available before or after, it will go to as many as it can
     *
     * @param position The position of the selected items
     * @return
     */
    public ArrayList<ImgurBaseObject> getItems(int position) {
        List<ImgurBaseObject> objects;
        int size = getItemCount();

        if (position - MAX_ITEMS / 2 < 0) {
            objects = getAllItems().subList(0, size > MAX_ITEMS ? position + (MAX_ITEMS / 2) : size);
        } else {
            objects = getAllItems().subList(position - (MAX_ITEMS / 2), position + (MAX_ITEMS / 2) <= size ? position + (MAX_ITEMS / 2) : size);
        }

        return new ArrayList<>(objects);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.gallery_item2, parent, false);
        view.setOnClickListener(mClickListener);
        return new GalleryHolder(view);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        GalleryHolder galleryHolder = (GalleryHolder) holder;
        ImgurBaseObject obj = getItem(position);

        // Get the appropriate photo to display
        if (obj.isNSFW() && !mAllowNSFWThumb) {
            galleryHolder.image.setImageResource(R.drawable.ic_nsfw);
        } else if (obj instanceof ImgurPhoto) {
            ImgurPhoto photoObject = ((ImgurPhoto) obj);
            String photoUrl;

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasMP4Link() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto) obj).getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, false, null);
            }

            displayImage(galleryHolder.image, photoUrl);

        } else if (obj instanceof ImgurAlbum) {
            displayImage(galleryHolder.image, ((ImgurAlbum) obj).getCoverUrl(ImgurPhoto.THUMBNAIL_GALLERY));
        } else {
            String url = ImgurBaseObject.getThumbnail(obj.getId(), obj.getLink(), ImgurPhoto.THUMBNAIL_GALLERY);
            displayImage(galleryHolder.image, url);
        }

        if (obj.getUpVotes() != Integer.MIN_VALUE) {
            galleryHolder.score.setText((obj.getUpVotes() - obj.getDownVotes()) + " " + galleryHolder.score.getContext().getString(R.string.points));
            galleryHolder.score.setVisibility(View.VISIBLE);
        } else {
            galleryHolder.score.setVisibility(View.GONE);
        }

        if (obj.isFavorited() || ImgurBaseObject.VOTE_UP.equals(obj.getVote())) {
            galleryHolder.score.setTextColor(mUpvoteColor);
        } else if (ImgurBaseObject.VOTE_DOWN.equals(obj.getVote())) {
            galleryHolder.score.setTextColor(mDownVoteColor);
        } else {
            galleryHolder.score.setTextColor(Color.WHITE);
        }
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForGallery().build();
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

    public static class GalleryHolder extends BaseViewHolder {
        @Bind(R.id.image)
        ImageView image;

        @Bind(R.id.score)
        TextView score;

        public GalleryHolder(View view) {
            super(view);
        }
    }
}