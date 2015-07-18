package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.UploadedPhoto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.Bind;

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
            convertView = mInflater.inflate(R.layout.upload_item, parent, false);
            holder = new UploadHolder(convertView);
        } else {
            holder = (UploadHolder) convertView.getTag();
        }

        String url;

        if (photo.isAlbum()) {
            url = String.format(Endpoints.ALBUM_COVER.getUrl(), photo.getCoverId() + ImgurPhoto.THUMBNAIL_GALLERY);
            holder.albumIndicator.setVisibility(View.VISIBLE);
        } else {
            url = ImageUtil.getThumbnail(photo.getUrl(), ImgurPhoto.THUMBNAIL_GALLERY);
            holder.albumIndicator.setVisibility(View.GONE);
        }

        displayImage(holder.image, url);
        return convertView;
    }

    static class UploadHolder extends ImgurBaseAdapter.ImgurViewHolder {
        @Bind(R.id.image)
        ImageView image;

        @Bind(R.id.albumIndicator)
        ImageView albumIndicator;

        public UploadHolder(View view) {
            super(view);
        }
    }
}
