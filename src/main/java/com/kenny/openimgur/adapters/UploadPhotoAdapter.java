package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.PhotoUploadListener;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class UploadPhotoAdapter extends BaseRecyclerAdapter<Upload> {
    private PhotoUploadListener mListener;

    public UploadPhotoAdapter(Context context, List<Upload> uploads, PhotoUploadListener listener) {
        super(context, uploads, true);
        mListener = listener;
    }

    public UploadPhotoAdapter(Context context, Upload upload, PhotoUploadListener listener) {
        this(context, new ArrayList<Upload>(1), listener);
        addItem(upload);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener = null;
    }

    public void onItemMove(int from, int to) {
        Collections.swap(getAllItems(), from, to);
        notifyItemMoved(from, to);
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForPhotoPicker().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final UploadPhotoHolder holder = new UploadPhotoHolder(mInflater.inflate(R.layout.upload_photo_item, parent, false));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onItemClicked(holder.getAdapterPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        UploadPhotoHolder uploadHolder = (UploadPhotoHolder) holder;
        Upload upload = getItem(position);

        String photoLocation = upload.isLink() ? upload.getLocation() : "file://" + upload.getLocation();
        displayImage(uploadHolder.image, photoLocation);
    }

    static class UploadPhotoHolder extends BaseViewHolder {
        @Bind(R.id.image)
        ImageView image;

        public UploadPhotoHolder(View view) {
            super(view);
        }
    }
}
