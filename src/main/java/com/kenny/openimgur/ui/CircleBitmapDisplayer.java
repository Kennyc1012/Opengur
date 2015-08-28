package com.kenny.openimgur.ui;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;

import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.display.BitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;

/**
 * Created by kcampagna on 12/22/14.
 */
public class CircleBitmapDisplayer implements BitmapDisplayer {

    private final Resources mResources;

    public CircleBitmapDisplayer(Resources resources) {
        mResources = resources;
    }

    @Override
    public void display(Bitmap bitmap, ImageAware imageAware, LoadedFrom loadedFrom) {
        imageAware.setImageDrawable(getRoundedBitmap(mResources, bitmap));
    }

    /**
     * Creates a round drawable
     *
     * @param res
     * @param bitmap
     * @return
     */
    @Nullable
    public static RoundedBitmapDrawable getRoundedBitmap(Resources res, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        float radius = Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f;
        return getRoundedBitmap(res, bitmap, radius);
    }

    /**
     * Creates a drawable with rounded corners
     *
     * @param res
     * @param bitmap
     * @param radius
     * @return
     */
    @Nullable
    public static RoundedBitmapDrawable getRoundedBitmap(Resources res, Bitmap bitmap, float radius) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(res, bitmap);
        drawable.setCornerRadius(radius);
        return drawable;
    }
}
