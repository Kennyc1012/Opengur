package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.ImgurTag;
import com.kenny.openimgur.ui.PointsBar;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.kenny.openimgur.util.LogUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.Bind;
import pl.droidsonroids.gif.GifDrawable;

public class PhotoAdapter extends BaseRecyclerAdapter<ImgurPhoto> {
    private static final int VIEW_HEADER = 1;

    private static final int VIEW_PHOTO = 2;

    private static final long PHOTO_SIZE_LIMIT = 1024 * 1024 * 2;

    private static final long PHOTO_PIXEL_LIMIT = 2048;

    private ImgurListener mListener;

    private ImgurBaseObject mImgurObject;

    public PhotoAdapter(Context context, List<ImgurPhoto> photos, ImgurBaseObject object, ImgurListener listener) {
        super(context, photos, true);
        mListener = listener;
        mImgurObject = object;
    }

    /**
     * Removes all items from list and ImgurListener is removed
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        clear();
        mListener = null;
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForView().build();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_HEADER : VIEW_PHOTO;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        BaseViewHolder holder;

        if (viewType == VIEW_HEADER) {
            holder = new PhotoTitleHolder(mInflater.inflate(R.layout.image_header, parent, false));
        } else {
            holder = new PhotoViewHolder(mInflater.inflate(R.layout.view_photo_item, parent, false));
            setClickListener((PhotoViewHolder) holder);
            holder.itemView.setTag(holder);
        }

        return holder;
    }

    @Override
    public int getItemCount() {
        // Pad the count for the header
        return super.getItemCount() + 1;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        // Remove one from position to account for the header
        position--;

        if (holder instanceof PhotoTitleHolder) {
            PhotoTitleHolder titleHolder = (PhotoTitleHolder) holder;

            if (!TextUtils.isEmpty(mImgurObject.getTitle())) {
                titleHolder.title.setText(mImgurObject.getTitle());
            }

            if (!TextUtils.isEmpty(mImgurObject.getAccount())) {
                titleHolder.author.setText("- " + mImgurObject.getAccount());
            } else {
                titleHolder.author.setText("- ?????");
            }

            if (!TextUtils.isEmpty(mImgurObject.getTopic())) {
                titleHolder.topic.setText(mImgurObject.getTopic());
            }

            int totalPoints = mImgurObject.getDownVotes() + mImgurObject.getUpVotes();
            int votePoints = mImgurObject.getUpVotes() - mImgurObject.getDownVotes();
            titleHolder.points.setText(mResources.getQuantityString(R.plurals.points, votePoints, votePoints));
            titleHolder.pointsBar.setPoints(mImgurObject.getUpVotes(), totalPoints);

            if (mImgurObject.isFavorited() || ImgurBaseObject.VOTE_UP.equals(mImgurObject.getVote())) {
                titleHolder.points.setTextColor(mResources.getColor(R.color.notoriety_positive));
            } else if (ImgurBaseObject.VOTE_DOWN.equals(mImgurObject.getVote())) {
                titleHolder.points.setTextColor(mResources.getColor(R.color.notoriety_negative));
            }

            if (mImgurObject.getTags() != null && !mImgurObject.getTags().isEmpty()) {
                Drawable tagDrawable;
                int size = mImgurObject.getTags().size();
                StringBuilder builder = new StringBuilder();

                // Tag icon is already dark themed
                if (mIsDarkTheme) {
                    tagDrawable = ResourcesCompat.getDrawable(mResources, R.drawable.ic_action_tag_16dp, null);
                } else {
                    tagDrawable = ImageUtil.tintDrawable(R.drawable.ic_action_tag_16dp, mResources, Color.BLACK);
                }

                for (int i = 0; i < size; i++) {
                    builder.append(mImgurObject.getTags().get(i).getName());
                    if (i != size - 1) builder.append(", ");
                }

                titleHolder.tags.setText(builder.toString());
                titleHolder.tags.setCompoundDrawablesWithIntrinsicBounds(tagDrawable, null, null, null);
                titleHolder.tags.setVisibility(View.VISIBLE);
            }
        } else {
            PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
            ImgurPhoto photo = getItem(position);
            String url = getPhotoUrl(photo);
            photoHolder.prog.setVisibility(View.GONE);
            photoHolder.video.setVisibility(View.GONE);
            photoHolder.image.setVisibility(View.VISIBLE);

            if (photoHolder.video.isPlaying()) {
                photoHolder.video.stopPlayback();
            }

            if (photo.isAnimated()) {
                photoHolder.play.setVisibility(View.VISIBLE);
            } else {
                photoHolder.play.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(photo.getDescription())) {
                photoHolder.desc.setVisibility(View.VISIBLE);
                photoHolder.desc.setText(photo.getDescription());
            } else {
                photoHolder.desc.setVisibility(View.GONE);
            }

            // Don't display titles for items with only 1 photo, it will be shown in the header
            if (!TextUtils.isEmpty(photo.getTitle()) && getItemCount() - 1 > 1) {
                photoHolder.title.setVisibility(View.VISIBLE);
                photoHolder.title.setText(photo.getTitle());
            } else {
                photoHolder.title.setVisibility(View.GONE);
            }

            displayImage(photoHolder.image, url);
        }
    }

    public void updateHeader(RecyclerView.ViewHolder holder) {
        if (holder instanceof PhotoTitleHolder) {
            PhotoTitleHolder titleHolder = (PhotoTitleHolder) holder;
            Resources res = titleHolder.author.getResources();
            int totalPoints = mImgurObject.getDownVotes() + mImgurObject.getUpVotes();
            int votePoints = mImgurObject.getUpVotes() - mImgurObject.getDownVotes();
            titleHolder.points.setText(mResources.getQuantityString(R.plurals.points, votePoints, votePoints));
            titleHolder.pointsBar.setPoints(mImgurObject.getUpVotes(), totalPoints);

            if (mImgurObject.isFavorited() || ImgurBaseObject.VOTE_UP.equals(mImgurObject.getVote())) {
                titleHolder.points.setTextColor(res.getColor(R.color.notoriety_positive));
            } else if (ImgurBaseObject.VOTE_DOWN.equals(mImgurObject.getVote())) {
                titleHolder.points.setTextColor(res.getColor(R.color.notoriety_negative));
            }
        } else {
            LogUtil.v(TAG, "ViewHolder not instance of PhotoTitleHolder");
        }
    }

    /**
     * Sets all the click listeners for the appropriate views
     *
     * @param holder
     */
    private void setClickListener(final PhotoViewHolder holder) {
        holder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    if (mListener != null) {
                        mListener.onPlayTap(holder.prog, holder.play, holder.image, holder.video, holder.itemView);
                    }
                }
            }
        });

        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onPhotoTap(holder.itemView);
                }
            }
        });

        holder.video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && mListener != null) {
                    mListener.onPhotoTap(holder.itemView);
                }

                return true;
            }
        });

        holder.image.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mListener != null) {
                    mListener.onPhotoLongTapListener(holder.itemView);
                }
                return true;
            }
        });

        holder.video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.setLooping(true);
            }
        });

        // holder.title.setMovementMethod(CustomLinkMovement.getInstance(mListener));
        //holder.desc.setMovementMethod(CustomLinkMovement.getInstance(mListener));
    }

    /**
     * Returns the photo url for the ImgurPhoto Object
     *
     * @param photo
     * @return
     */
    private String getPhotoUrl(ImgurPhoto photo) {
        String url;

        // Check if we have an mp4 and if we should load its thumbnail
        if (photo.isAnimated() && photo.hasMP4Link() && photo.isLinkAThumbnail()) {
            if (photo.getSize() > PHOTO_SIZE_LIMIT || photo.getHeight() > PHOTO_PIXEL_LIMIT || photo.getWidth() > PHOTO_PIXEL_LIMIT) {
                url = photo.getThumbnail(ImgurPhoto.THUMBNAIL_HUGE, true, FileUtil.EXTENSION_GIF);
            } else {
                url = photo.getLink();
            }
        } else if (photo.getSize() > PHOTO_SIZE_LIMIT || photo.getHeight() > PHOTO_PIXEL_LIMIT || photo.getWidth() > PHOTO_PIXEL_LIMIT) {
            url = photo.getThumbnail(ImgurPhoto.THUMBNAIL_HUGE, false, null);
        } else {
            url = photo.getLink();
        }

        return url;
    }

    /**
     * Attempts to pause the currently playing gif or video
     *
     * @param view
     * @return If pausing was successfuly
     */
    public boolean attemptToPause(View view) {
        if (!(view.getTag() instanceof PhotoViewHolder)) return false;

        PhotoViewHolder holder = (PhotoViewHolder) view.getTag();
        if (holder.image.getDrawable() instanceof GifDrawable) {
            GifDrawable gif = (GifDrawable) holder.image.getDrawable();

            if (gif.isPlaying()) {
                gif.pause();
                holder.play.setVisibility(View.VISIBLE);
                return true;
            }
        } else if (holder.video.isPlaying()) {
            holder.video.pause();
            holder.play.setVisibility(View.VISIBLE);
            return true;
        }

        return false;
    }

    public void setTags(List<ImgurTag> tags) {
        mImgurObject.setTags(tags);
        notifyDataSetChanged();
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof PhotoViewHolder) {
            PhotoViewHolder photoViewHolder = (PhotoViewHolder) holder;

            if (photoViewHolder.image.getDrawable() instanceof GifDrawable) {
                GifDrawable gif = (GifDrawable) photoViewHolder.image.getDrawable();
                gif.stop();
                gif.recycle();
            }

            photoViewHolder.video.stopPlayback();
        }
    }

    static class PhotoViewHolder extends BaseViewHolder {
        @Bind(R.id.image)
        ImageView image;

        @Bind(R.id.play)
        FloatingActionButton play;

        @Bind(R.id.progressBar)
        ProgressBar prog;

        @Bind(R.id.desc)
        TextView desc;

        @Bind(R.id.title)
        TextView title;

        @Bind(R.id.videoView)
        VideoView video;

        public PhotoViewHolder(View view) {
            super(view);
        }
    }

    static class PhotoTitleHolder extends BaseViewHolder {
        @Bind(R.id.title)
        TextView title;

        @Bind(R.id.author)
        TextView author;

        @Bind(R.id.pointText)
        TextView points;

        @Bind(R.id.topic)
        TextView topic;

        @Bind(R.id.tags)
        TextView tags;

        @Bind(R.id.pointsBar)
        PointsBar pointsBar;

        public PhotoTitleHolder(View view) {
            super(view);
        }
    }
}
