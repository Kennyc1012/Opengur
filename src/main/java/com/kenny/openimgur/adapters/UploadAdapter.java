package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.UploadedPhoto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.InjectView;

/**
 * Created by Kenny-PC on 1/14/2015.
 */
public class UploadAdapter extends ImgurBaseAdapter<UploadedPhoto> {
    public UploadAdapter(Context context, List<UploadedPhoto> photos) {
        super(context, photos, true);
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForGallery().build();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        UploadHolder holder;
        UploadedPhoto photo = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gallery_item, parent, false);
            holder = new UploadHolder(convertView);
        } else {
            holder = (UploadHolder) convertView.getTag();
        }

        displayImage(holder.image, ImageUtil.getThumbnail(photo.getUrl(), ImgurPhoto.THUMBNAIL_GALLERY));
        return convertView;
    }

    static class UploadHolder extends ImgurBaseAdapter.ImgurViewHolder {
        @InjectView(R.id.image)
        ImageView image;

        @InjectView(R.id.like_score)
        TextView score;

        public UploadHolder(View view) {
            super(view);
            score.setVisibility(View.GONE);
        }
    }
}
