package com.kenny.openimgur.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

/**
 * Created by kcampagna on 2/23/16.
 */
public class CenteredDrawable extends Drawable {
    private Bitmap mBitmap;

    private int mBitmapWidth;

    private int mBitmapHeight;

    public CenteredDrawable(@NonNull Bitmap bitmap) {
        mBitmap = bitmap;
        mBitmapWidth = mBitmap.getWidth();
        mBitmapHeight = mBitmap.getHeight();
    }

    @Override
    public void draw(Canvas canvas) {
        int width = Math.abs(getBounds().right);
        int height = Math.abs(getBounds().bottom);
        int x = (width / 2) - (mBitmapWidth / 2);
        int y = (height / 2) - (mBitmapHeight / 2);
        canvas.drawBitmap(mBitmap, x, y, null);
    }

    @Override
    public void setAlpha(int alpha) {
        // NOOP
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        // NOOP
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
