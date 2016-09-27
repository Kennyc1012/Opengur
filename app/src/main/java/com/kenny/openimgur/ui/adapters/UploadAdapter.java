package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.util.DBContracts;
import com.kenny.openimgur.util.ImageUtil;
import com.kennyc.adapters.CursorRecyclerAdapter;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import butterknife.BindView;

/**
 * Created by Kenny-PC on 1/14/2015.
 */
public class UploadAdapter extends CursorRecyclerAdapter<UploadAdapter.UploadHolder> {
    private View.OnClickListener clickListener;

    private View.OnLongClickListener longClickListener;

    private DisplayImageOptions options;

    private ImageLoader loader;

    public UploadAdapter(Context context, Cursor cursor, View.OnClickListener listener, View.OnLongClickListener longClickListener) {
        super(context, cursor);
        clickListener = listener;
        this.longClickListener = longClickListener;
        options = ImageUtil.getDisplayOptionsForGallery().build();
        loader = ImageUtil.getImageLoader(context);
    }

    public void onDestroy() {
        clickListener = null;
        longClickListener = null;
    }

    @Override
    public UploadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UploadHolder holder = new UploadHolder(inflateView(R.layout.upload_item, parent));
        holder.itemView.setOnClickListener(clickListener);
        holder.itemView.setOnLongClickListener(longClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(UploadHolder holder, int position) {
        moveToPosition(position);
        Cursor cursor = super.getCursor();

        String url;
        String photoUrl = cursor.getString(DBContracts.UploadContract.COLUMN_INDEX_URL);
        String converId = cursor.getString(DBContracts.UploadContract.COLUMN_INDEX_COVER_ID);
        boolean isAlbum = cursor.getInt(DBContracts.UploadContract.COLUMN_INDEX_IS_ALBUM) == 1;

        if (isAlbum) {
            url = String.format(ImgurAlbum.ALBUM_COVER_URL, converId + ImgurPhoto.THUMBNAIL_GALLERY);
            holder.albumIndicator.setVisibility(View.VISIBLE);
        } else {
            url = ImageUtil.getThumbnail(photoUrl, ImgurPhoto.THUMBNAIL_GALLERY);
            holder.albumIndicator.setVisibility(View.GONE);
        }

        loader.displayImage(url, holder.image, options);
        // TODO Album count
    }

    public Cursor getCursor() {
        return super.getCursor();
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
