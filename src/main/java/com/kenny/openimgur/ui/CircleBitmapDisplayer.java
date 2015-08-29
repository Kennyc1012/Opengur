package com.kenny.openimgur.ui;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
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
        imageAware.setImageDrawable(getRoundedBitmapDrawable(mResources, bitmap));
    }

    /**
     * Creates a round drawable
     *
     * @param res
     * @param bitmap
     * @return
     */
    @Nullable
    public static RoundedBitmapDrawable getRoundedBitmapDrawable(Resources res, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        float radius = Math.max(bitmap.getWidth(), bitmap.getHeight()) / 2.0f;
        return getRoundedBitmapDrawable(res, bitmap, radius);
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
    public static RoundedBitmapDrawable getRoundedBitmapDrawable(Resources res, Bitmap bitmap, float radius) {
        if (bitmap == null || bitmap.isRecycled()) return null;
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(res, bitmap);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    /**
     * Returns a bitmap that has been rounded
     *
     * @param bitmap
     * @return
     */
    @Nullable
    public static Bitmap getRoundedBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        Bitmap output;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);
        }

        float radius;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            radius = bitmap.getHeight() / 2;
        } else {
            radius = bitmap.getWidth() / 2;
        }

        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.BLACK);
        canvas.drawCircle(radius, radius, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}
