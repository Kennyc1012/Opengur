package com.kenny.openimgur.adapters;

import android.content.Context;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.InjectView;
import pl.droidsonroids.gif.GifDrawable;

public class PhotoAdapter extends ImgurBaseAdapter<ImgurPhoto> {
    private ImgurListener mListener;

    private static final long PHOTO_SIZE_LIMIT = 1024 * 1024 * 2;
    private static final long PHOTO_PIXEL_LIMIT = 2048;

    public PhotoAdapter(Context context, List<ImgurPhoto> photos, ImgurListener listener) {
        super(context, photos, true);
        mListener = listener;
    }

    /**
     * Removes all items from list and ImgurListener is removed
     */
    public void destroy() {
        clear();
        mListener = null;
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForView().build();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final PhotoViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.view_photo_item, parent, false);
            holder = new PhotoViewHolder(convertView);
            holder.video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                }
            });

            setClickListener(holder);
        } else {
            holder = (PhotoViewHolder) convertView.getTag();
        }

        ImgurPhoto photo = getItem(position);
        String url = getPhotoUrl(photo);
        holder.prog.setVisibility(View.GONE);
        holder.video.setVisibility(View.GONE);
        holder.image.setVisibility(View.VISIBLE);
        //Linkify.addLinks(holder.title, Linkify.WEB_URLS);
        //Linkify.addLinks(holder.desc, Linkify.WEB_URLS);

        if (holder.video.isPlaying()) {
            holder.video.stopPlayback();
        }

        if (photo.isAnimated()) {
            holder.play.setVisibility(View.VISIBLE);
        } else {
            holder.play.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(photo.getDescription())) {
            holder.desc.setVisibility(View.VISIBLE);
            holder.desc.setText(photo.getDescription());
        } else {
            holder.desc.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(photo.getTitle())) {
            holder.title.setVisibility(View.VISIBLE);
            holder.title.setText(photo.getTitle());
        } else {
            holder.title.setVisibility(View.GONE);
        }

        displayImage(holder.image, url);
        return convertView;
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
                    mListener.onPlayTap(holder.prog, holder.play, holder.image, holder.video);
                }
            }
        });

        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onPhotoTap(view);
                }
            }
        });

        holder.video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && mListener != null) {
                    mListener.onPhotoTap(view);
                }

                return true;
            }
        });

        holder.image.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (mListener != null) {
                    mListener.onPhotoLongTapListener(view);
                }
                return true;
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
        if (view instanceof ImageView && ((ImageView) view).getDrawable() instanceof GifDrawable) {
            GifDrawable gif = (GifDrawable) ((ImageView) view).getDrawable();

            if (gif.isPlaying()) {
                gif.pause();
                RelativeLayout parent = (RelativeLayout) view.getParent();
                parent.findViewById(R.id.play).setVisibility(View.VISIBLE);
                return true;
            }
        } else if (view instanceof VideoView && ((VideoView) view).isPlaying()) {
            ((VideoView) view).pause();
            RelativeLayout parent = (RelativeLayout) view.getParent();
            parent.findViewById(R.id.play).setVisibility(View.VISIBLE);
            return true;
        }

        return false;
    }

    static class PhotoViewHolder extends ImgurViewHolder {
        @InjectView(R.id.image)
        ImageView image;

        @InjectView(R.id.play)
        ImageButton play;

        @InjectView(R.id.progressBar)
        ProgressBar prog;

        @InjectView(R.id.desc)
        TextViewRoboto desc;

        @InjectView(R.id.title)
        TextViewRoboto title;

        @InjectView(R.id.videoView)
        VideoView video;

        View root;

        public PhotoViewHolder(View view) {
            super(view);
            root = view;
        }
    }
}