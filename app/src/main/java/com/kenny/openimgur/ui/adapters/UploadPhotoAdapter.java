package com.kenny.openimgur.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class UploadPhotoAdapter extends BaseRecyclerAdapter<Upload> {
    private View.OnClickListener mListener;

    public UploadPhotoAdapter(Context context, List<Upload> uploads, View.OnClickListener listener) {
        super(context, uploads, true);
        mListener = listener;
    }

    public UploadPhotoAdapter(Context context, Upload upload, View.OnClickListener listener) {
        this(context, new ArrayList<Upload>(1), listener);
        addItem(upload);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mListener = null;
    }

    public boolean onItemMove(int from, int to) {
        if (from != RecyclerView.NO_POSITION && to != RecyclerView.NO_POSITION) {
            int movement = Math.abs(from - to);
            if (movement == 1) {
                // Item moved only 1 space over, simple swap
                Collections.swap(getAllItems(), from, to);
                notifyItemMoved(from, to);
                return true;
            } else {
                // Shift the items so they get rendered correctly.
                List<Upload> backingList = getAllItems();
                Upload item = backingList.remove(from);
                backingList.add(to, item);
                notifyItemMoved(from, to);
                return true;
            }
        }

        return false;
    }

    public void updateItem(@NonNull Upload upload) {
        int position = -1;
        int size = getItemCount();

        for (int i = 0; i < size; i++) {
            if (getItem(i).equals(upload)) {
                position = i;
                break;
            }
        }

        if (position > -1) {
            removeItem(position);
            addItem(upload, position);
            notifyItemChanged(position);
        }
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForPhotoPicker().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final UploadPhotoHolder holder = new UploadPhotoHolder(inflateView(R.layout.upload_photo_item, parent));
        if (mListener != null) holder.itemView.setOnClickListener(mListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        UploadPhotoHolder uploadHolder = (UploadPhotoHolder) holder;
        Upload upload = getItem(position);

        if (TextUtils.isEmpty(upload.getTitle()) && TextUtils.isEmpty(upload.getDescription())) {
            uploadHolder.contentContainer.setVisibility(View.GONE);
        } else {
            uploadHolder.contentContainer.setVisibility(View.VISIBLE);
            uploadHolder.title.setVisibility(TextUtils.isEmpty(upload.getTitle()) ? View.GONE : View.VISIBLE);
            uploadHolder.desc.setVisibility(TextUtils.isEmpty(upload.getDescription()) ? View.GONE : View.VISIBLE);
        }

        String photoLocation = upload.isLink() ? upload.getLocation() : "file://" + upload.getLocation();
        displayImage(uploadHolder.image, photoLocation);
    }

    static class UploadPhotoHolder extends BaseViewHolder {
        @BindView(R.id.image)
        ImageView image;

        @BindView(R.id.contentContainer)
        View contentContainer;

        @BindView(R.id.title)
        View title;

        @BindView(R.id.desc)
        View desc;

        public UploadPhotoHolder(View view) {
            super(view);
        }
    }
}
