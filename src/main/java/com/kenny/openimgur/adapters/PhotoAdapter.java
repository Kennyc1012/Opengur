package com.kenny.openimgur.adapters;

import android.content.Context;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import pl.droidsonroids.gif.GifDrawable;

public class PhotoAdapter extends BaseAdapter {
    private List<ImgurPhoto> mPhotos;

    private LayoutInflater mInflater;

    private ImageLoader mImageLoader;

    private ImgurListener mListener;

    private static final long PHOTO_SIZE_LIMIT = 1048576L;

    private DisplayImageOptions mOptions;

    public PhotoAdapter(Context context, List<ImgurPhoto> photos, ImgurListener listener) {
        mInflater = LayoutInflater.from(context);
        mPhotos = photos;
        mImageLoader = OpenImgurApp.getInstance(context).getImageLoader();
        mListener = listener;
        mOptions = ImageUtil.getDisplayOptionsForView().build();
    }

    public void clear() {
        if (mPhotos != null) {
            mPhotos.clear();
        }
    }

    /**
     * Removes all items from list and ImgurListener is removed
     */
    public void destroy() {
        clear();
        mListener = null;
    }

    public List<ImgurPhoto> getPhotos() {
        return mPhotos;
    }

    @Override
    public int getCount() {
        if (mPhotos != null) {
            return mPhotos.size();
        }

        return 0;
    }

    @Override
    public ImgurPhoto getItem(int position) {
        if (mPhotos != null) {
            return mPhotos.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final PhotoViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.view_photo_item, parent, false);
            holder = new PhotoViewHolder();
            holder.root = convertView;
            holder.play = (ImageButton) convertView.findViewById(R.id.play);
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.prog = (ProgressBar) convertView.findViewById(R.id.progressBar);
            holder.desc = (TextViewRoboto) convertView.findViewById(R.id.desc);
            holder.title = (TextViewRoboto) convertView.findViewById(R.id.title);
            holder.video = (VideoView) convertView.findViewById(R.id.videoView);
            holder.video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                }
            });

            setClickListener(holder);
            convertView.setTag(holder);
        } else {
            holder = (PhotoViewHolder) convertView.getTag();
        }

        ImgurPhoto photo = getItem(position);
        String url = getPhotoUrl(photo);
        holder.prog.setVisibility(View.GONE);
        holder.video.setVisibility(View.GONE);
        holder.image.setVisibility(View.VISIBLE);
       // Linkify.addLinks(holder.title, Linkify.WEB_URLS);
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

        mImageLoader.cancelDisplayTask(holder.image);
        mImageLoader.displayImage(url, holder.image, mOptions);
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
            if (photo.getWidth() > 1024 || photo.getHeight() > 1024) {
                url = photo.getThumbnail(ImgurPhoto.THUMBNAIL_HUGE, true, FileUtil.EXTENSION_GIF);
            } else {
                url = photo.getLink();
            }
        } else if (photo.getWidth() > 1024 || photo.getHeight() > 1024 || photo.getSize() > PHOTO_SIZE_LIMIT) {
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

    private static class PhotoViewHolder {
        ImageView image;

        ImageButton play;

        ProgressBar prog;

        TextViewRoboto desc, title;

        VideoView video;

        View root;
    }
}