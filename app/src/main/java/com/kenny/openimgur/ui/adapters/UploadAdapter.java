package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.UploadedPhoto;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Kenny-PC on 1/14/2015.
 */
public class UploadAdapter extends BaseRecyclerAdapter<UploadedPhoto> {
    private View.OnClickListener mClickListener;

    private View.OnLongClickListener mLongClickListener;

    public UploadAdapter(Context context, List<UploadedPhoto> photos, View.OnClickListener listener, View.OnLongClickListener longClickListener) {
        super(context, photos, true);
        mClickListener = listener;
        mLongClickListener = longClickListener;
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForGallery().build();
    }

    @Override
    public void onDestroy() {
        mClickListener = null;
        mLongClickListener = null;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UploadHolder holder = new UploadHolder(inflateView(R.layout.upload_item, parent));
        holder.itemView.setOnClickListener(mClickListener);
        holder.itemView.setOnLongClickListener(mLongClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        UploadHolder uploadHolder = (UploadHolder) holder;
        UploadedPhoto photo = getItem(position);

        String url;

        if (photo.isAlbum()) {
            url = String.format(ImgurAlbum.ALBUM_COVER_URL, photo.getCoverId() + ImgurPhoto.THUMBNAIL_GALLERY);
            uploadHolder.albumIndicator.setVisibility(View.VISIBLE);
        } else {
            url = ImageUtil.getThumbnail(photo.getUrl(), ImgurPhoto.THUMBNAIL_GALLERY);
            uploadHolder.albumIndicator.setVisibility(View.GONE);
        }

        displayImage(uploadHolder.image, url);
        // TODO Album count
    }

    static class UploadHolder extends BaseRecyclerAdapter.BaseViewHolder {
        @BindView(R.id.image)
        ImageView image;

        @BindView(R.id.albumIndicator)
        ImageView albumIndicator;

        public UploadHolder(View view) {
            super(view);
        }
    }
}
