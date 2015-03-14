package com.kenny.openimgur.adapters;

import android.content.Context;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import org.apache.commons.collections15.list.SetUniqueList;

/**
 * Created by Kenny-PC on 3/14/2015.
 */
public class MemeAdapter extends GalleryAdapter {

    public MemeAdapter(Context context, SetUniqueList<ImgurBaseObject> items) {
        super(context, items);
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForGallery()
                .showImageOnLoading(R.drawable.place_holder).build();
    }
}
