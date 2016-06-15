package com.kenny.openimgur.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 8/3/14.
 */
public class PointsBar extends View {
    private float mTotalPoints;

    private float mUpVotes;

    private Paint mUpPaint = new Paint();

    private int mWidth;

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
        mUpPaint.setColor(getResources().getColor(R.color.notoriety_positive));
        mUpPaint.setStrokeWidth(4);
        mUpPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        setWillNotDraw(false);
        setBackgroundColor(getResources().getColor(R.color.notoriety_negative));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, mWidth * (mUpVotes / mTotalPoints), getHeight(), mUpPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        invalidate();
    }

    public void setPoints(float upVotes, float totalPoints) {
        mTotalPoints = totalPoints;
        mUpVotes = upVotes;
        invalidate();
    }
}
