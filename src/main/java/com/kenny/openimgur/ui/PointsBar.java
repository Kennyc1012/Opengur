package com.kenny.openimgur.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by kcampagna on 8/3/14.
 */
public class PointsBar extends View {
    private float mTotalPoints;

    private float mUpVotes;

    private Paint mUpPaint = new Paint();

    public PointsBar(Context context) {
        super(context);
        init();
    }

    public PointsBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PointsBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mUpPaint.setColor(getResources().getColor(android.R.color.holo_green_light));
        mUpPaint.setStrokeWidth(4);
        mUpPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        setWillNotDraw(false);
        setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth() * (mUpVotes / mTotalPoints), getHeight(), mUpPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidate();
    }

    public void setTotalPoints(float total) {
        mTotalPoints = total;
        invalidate();
    }

    public void setUpVotes(float upVotes) {
        mUpVotes = upVotes;
        invalidate();
    }
}
