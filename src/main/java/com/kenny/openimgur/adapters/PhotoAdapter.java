package com.kenny.openimgur.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

public class PhotoAdapter extends BaseAdapter {
    private List<ImgurPhoto> mPhotos;

    private LayoutInflater mInflater;

    private ImageLoader mImageLoader;

    private ImgurListener mListener;

    private final long PHOTO_SIZE_LIMIT = 1048576L;

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
        PhotoViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.view_photo_item, parent, false);
            holder = new PhotoViewHolder();
            holder.play = (ImageButton) convertView.findViewById(R.id.play);
            holder.image = (ImageView) convertView.findViewById(R.id.image);
            holder.prog = (ProgressBar) convertView.findViewById(R.id.progressBar);
            holder.desc = (TextViewRoboto) convertView.findViewById(R.id.desc);
            holder.title = (TextViewRoboto) convertView.findViewById(R.id.title);

            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onPhotoTap((ImageView) view);
                    }
                }
            });

            final ProgressBar bar = holder.prog;
            final ImageView img = holder.image;
            holder.play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onPlayTap(bar, img, (ImageButton) view);
                    }
                }
            });

            holder.image.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mListener != null) {
                        mListener.onPhotoLongTapListener((ImageView) view);
                    }
                    return true;
                }
            });

            convertView.setTag(holder);
        } else {
            holder = (PhotoViewHolder) convertView.getTag();
        }

        ImgurPhoto photo = getItem(position);
        String url = null;

        // Load a downscaled image if any of the dimensions are greater than 1024 or they are large than 1mb
        if (photo.getWidth() > 1024 || photo.getHeight() > 1024 || photo.getSize() > PHOTO_SIZE_LIMIT) {
            url = photo.getThumbnail(ImgurPhoto.THUMBNAIL_HUGE);
        } else {
            url = photo.getLink();
        }

        holder.prog.setVisibility(View.GONE);

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

    public boolean isListenerSet() {
        return mListener != null;
    }

    public void setImgurListener(ImgurListener listener) {
        mListener = listener;
        notifyDataSetChanged();
    }

    private static class PhotoViewHolder {
        ImageView image;

        ImageButton play;

        ProgressBar prog;

        TextViewRoboto desc, title;
    }
}