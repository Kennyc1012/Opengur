package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.annotation.NonNull;
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
import com.kenny.openimgur.collections.SetUniqueList;
import com.kenny.openimgur.ui.CenteredDrawable;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class GalleryAdapter extends BaseRecyclerAdapter<ImgurBaseObject> {
    public static final int MAX_ITEMS = 300;

    private int mUpvoteColor;

    private int mDownVoteColor;

    private boolean mAllowNSFWThumb;

    private View.OnClickListener mClickListener;

    private View.OnLongClickListener mLongClickListener;

    private boolean mShowPoints = true;

    private String mThumbnailQuality;

    public GalleryAdapter(Context context, SetUniqueList<ImgurBaseObject> objects, View.OnClickListener listener) {
        super(context, objects, true);
        mUpvoteColor = getColor(R.color.notoriety_positive);
        mDownVoteColor = getColor(R.color.notoriety_negative);
        SharedPreferences pref = OpengurApp.getInstance(context).getPreferences();
        mAllowNSFWThumb = pref.getBoolean(SettingsActivity.KEY_NSFW_THUMBNAILS, false);
        mThumbnailQuality = pref.getString(SettingsActivity.KEY_THUMBNAIL_QUALITY, ImgurPhoto.THUMBNAIL_GALLERY);
        mClickListener = listener;
    }

    public GalleryAdapter(Context context, SetUniqueList<ImgurBaseObject> objects, View.OnClickListener listener, boolean showPoints) {
        this(context, objects, listener);
        mShowPoints = showPoints;
    }

    public void setOnLongClickPressListener(View.OnLongClickListener listener) {
        mLongClickListener = listener;
        if (getItemCount() > 0) notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        mClickListener = null;
        mLongClickListener = null;
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
        View view = inflateView(R.layout.gallery_item, parent);
        view.setOnClickListener(mClickListener);
        view.setOnLongClickListener(mLongClickListener);
        return new GalleryHolder(view);
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        GalleryHolder galleryHolder = (GalleryHolder) holder;
        ImgurBaseObject obj = getItem(position);

        // Get the appropriate photo to display
        if (obj.isNSFW() && !mAllowNSFWThumb) {
            galleryHolder.image.setImageResource(R.drawable.ic_nsfw);
            galleryHolder.itemType.setVisibility(View.GONE);
        } else if (obj instanceof ImgurPhoto) {
            ImgurPhoto photoObject = ((ImgurPhoto) obj);
            String photoUrl;

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasVideoLink() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto) obj).getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, false, null);
            }

            displayImage(galleryHolder.image, photoUrl);

            if (photoObject.isAnimated()) {
                galleryHolder.itemType.setVisibility(View.VISIBLE);
                galleryHolder.itemType.setImageResource(R.drawable.ic_gif_24dp);
                galleryHolder.itemType.setBackgroundColor(getColor(R.color.black_55));
            } else {
                galleryHolder.itemType.setVisibility(View.GONE);
            }
        } else if (obj instanceof ImgurAlbum) {
            ImgurAlbum album = ((ImgurAlbum) obj);
            displayImage(galleryHolder.image, album.getCoverUrl(mThumbnailQuality));
            int albumSize = album.getAlbumImageCount();

            if (albumSize <= 1) {
                galleryHolder.itemType.setVisibility(View.GONE);
                galleryHolder.itemType.setBackground(null);
            } else {
                int albumImageId;

                switch (albumSize) {
                    case 2:
                        albumImageId = R.drawable.numeric_2_box_24dp;
                        break;

                    case 3:
                        albumImageId = R.drawable.numeric_3_box_24dp;
                        break;

                    case 4:
                        albumImageId = R.drawable.numeric_4_box_24dp;
                        break;

                    case 5:
                        albumImageId = R.drawable.numeric_5_box_24dp;
                        break;

                    case 6:
                        albumImageId = R.drawable.numeric_6_box_24dp;
                        break;

                    case 7:
                        albumImageId = R.drawable.numeric_7_box_24dp;
                        break;

                    case 8:
                        albumImageId = R.drawable.numeric_8_box_24dp;
                        break;

                    case 9:
                        albumImageId = R.drawable.numeric_9_box_24dp;
                        break;

                    default:
                        albumImageId = R.drawable.numeric_9_plus_box_24dp;
                        break;
                }

                galleryHolder.itemType.setImageResource(albumImageId);
                galleryHolder.itemType.setVisibility(View.VISIBLE);
                galleryHolder.itemType.setBackground(null);
            }
        } else {
            String url = ImgurBaseObject.getThumbnail(obj.getId(), obj.getLink(), ImgurPhoto.THUMBNAIL_GALLERY);
            displayImage(galleryHolder.image, url);
            galleryHolder.itemType.setVisibility(View.GONE);
        }

        if (mShowPoints) {
            int totalPoints = obj.getUpVotes() - obj.getDownVotes();
            galleryHolder.score.setText(getResources().getQuantityString(R.plurals.points, totalPoints, totalPoints));
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
        // The drawable CAN NOT be Vectors as they do not get converted to bitmap on decoding
        int drawableRes = isDarkTheme ? R.drawable.ic_broken_image_white_48dp : R.drawable.ic_broken_image_black_48dp;
        CenteredDrawable dr = new CenteredDrawable(BitmapFactory.decodeResource(getResources(), drawableRes));
        return ImageUtil.getDisplayOptionsForGallery().showImageOnFail(dr).build();
    }

    public void setAllowNSFW(boolean allowNSFW) {
        if (mAllowNSFWThumb != allowNSFW) {
            mAllowNSFWThumb = allowNSFW;
            notifyDataSetChanged();
        }
    }

    public void setThumbnailQuality(@NonNull Context context, String quality) {
        if (!mThumbnailQuality.equals(quality)) {
            LogUtil.v(TAG, "Updating thumbnail quality to " + quality);
            // Clear any memory cache we may have for the new thumbnail
            ImageUtil.getImageLoader(context).clearMemoryCache();
            mThumbnailQuality = quality;
            notifyDataSetChanged();
        }
    }

    public static class GalleryHolder extends BaseViewHolder {
        @BindView(R.id.image)
        ImageView image;

        @BindView(R.id.score)
        TextView score;

        @BindView(R.id.itemType)
        ImageView itemType;

        public GalleryHolder(View view) {
            super(view);
        }
    }
}