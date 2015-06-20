package com.kenny.openimgur.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.ui.GridItemDecoration;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import butterknife.InjectView;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class PhotoPickerAdapter extends BaseRecyclerAdapter<String> {
    private View.OnClickListener mClickListener;

    private int mTintColor;

    private final Set<String> mCheckPhotos = new HashSet<>();

    public PhotoPickerAdapter(Context context, RecyclerView view, List<String> photos, View.OnClickListener listener) {
        super(context, photos, true);
        Resources res = context.getResources();
        int gridSize = res.getInteger(R.integer.gallery_num_columns);
        view.setLayoutManager(new GridLayoutManager(context, gridSize));
        view.addItemDecoration(new GridItemDecoration(res.getDimensionPixelSize(R.dimen.grid_padding), gridSize));
        mTintColor = res.getColor(OpengurApp.getInstance(context).getImgurTheme().accentColor);
        mClickListener = listener;
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForPhotoPicker().build();
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PhotoViewHolder holder = new PhotoViewHolder(mInflater.inflate(R.layout.photo_picker_item, parent, false));
        holder.itemView.setOnClickListener(mClickListener);
        holder.checkMark.setImageDrawable(ImageUtil.tintDrawable(R.drawable.ic_check_circle_white_36dp, holder.checkMark.getResources(), mTintColor));
        return holder;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
        String path = getItem(position);

        photoHolder.checkMark.setVisibility(mCheckPhotos.contains(path) ? View.VISIBLE : View.GONE);
        displayImage(photoHolder.image, "file://" + path);
    }

    public int onSelected(String path, View view) {
        boolean isSelected = mCheckPhotos.contains(path);

        if (!isSelected) {
            mCheckPhotos.add(path);
            view.findViewById(R.id.checkMark).setVisibility(View.VISIBLE);
        } else {
            mCheckPhotos.remove(path);
            view.findViewById(R.id.checkMark).setVisibility(View.GONE);
        }

        return mCheckPhotos.size();
    }

    public int getSelectedCount() {
        return mCheckPhotos.size();
    }

    @Nullable
    public ArrayList<String> getCheckedPhotos() {
        if (mCheckPhotos.size() > 0) {
            ArrayList<String> checkedPhotos = new ArrayList<>(mCheckPhotos.size());
            Iterator<String> iterator = mCheckPhotos.iterator();

            while (iterator.hasNext()) {
                checkedPhotos.add(iterator.next());
            }

            return checkedPhotos;
        }

        return null;
    }

    static class PhotoViewHolder extends BaseViewHolder {
        @InjectView(R.id.image)
        ImageView image;

        @InjectView(R.id.checkMark)
        ImageView checkMark;

        public PhotoViewHolder(View view) {
            super(view);
        }
    }
}

