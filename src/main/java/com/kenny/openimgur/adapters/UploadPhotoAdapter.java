package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.classes.Upload;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.List;

import butterknife.InjectView;

/**
 * Created by Kenny-PC on 6/21/2015.
 */
public class UploadPhotoAdapter extends BaseRecyclerAdapter<Upload> {
    private boolean mIsDarkTheme;

    public UploadPhotoAdapter(Context context, List<Upload> uploads) {
        super(context, uploads, true);
        mIsDarkTheme = OpengurApp.getInstance(context).getImgurTheme().isDarkTheme;
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForPhotoPicker().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UploadPhotoHolder holder = new UploadPhotoHolder(mInflater.inflate(R.layout.upload_photo_item, parent, false));
        Resources res = holder.overflow.getResources();
        Drawable dr = mIsDarkTheme ? ResourcesCompat.getDrawable(res, R.drawable.ic_more_vert_white_24dp, null) : ImageUtil.tintDrawable(R.drawable.ic_more_vert_white_24dp, res, Color.BLACK);
        holder.overflow.setImageDrawable(dr);
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        UploadPhotoHolder uploadHolder = (UploadPhotoHolder) holder;
        Upload upload = getItem(position);

        if (TextUtils.isEmpty(upload.getDescription())) {
            uploadHolder.description.setText(R.string.upload_empty_desc);
        } else {
            uploadHolder.description.setText(upload.getDescription());
        }

        if (TextUtils.isEmpty(upload.getTitle())) {
            uploadHolder.title.setText(R.string.upload_empty_title);
        } else {
            uploadHolder.title.setText(upload.getTitle());
        }

        String photoLocation = upload.isLink() ? upload.getLocation() : "file://" + upload.getLocation();
        displayImage(uploadHolder.image, photoLocation);
    }

    static class UploadPhotoHolder extends BaseViewHolder {
        @InjectView(R.id.title)
        TextView title;

        @InjectView(R.id.desc)
        TextView description;

        @InjectView(R.id.image)
        ImageView image;

        @InjectView(R.id.overflow)
        ImageButton overflow;

        public UploadPhotoHolder(View view) {
            super(view);
        }
    }
}
