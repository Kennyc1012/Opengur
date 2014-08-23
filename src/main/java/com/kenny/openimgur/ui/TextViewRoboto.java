package com.kenny.openimgur.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.kenny.openimgur.R;

/**
 * Created by kcampagna on 8/17/14.
 */
public class TextViewRoboto extends TextView {
    public static final int ROBOTO_LIGHT = 0;

    public static final int ROBOTO_REGULAR = 1;

    public static final int ROBOTO_BOLD = 2;

    public static final int ROBOTO_THIN = 3;

    public TextViewRoboto(Context context) {
        super(context);
        init(null);
    }

    public TextViewRoboto(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TextViewRoboto(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.TextViewRoboto);
            int typeFace = a.getInt(R.styleable.TextViewRoboto_robotoFont, ROBOTO_REGULAR);

            try {
                switch (typeFace) {
                    case ROBOTO_LIGHT:
                        setTypeface(Typeface.createFromAsset(getResources().getAssets(), "RobotoCondensed-Light.ttf"));
                        break;

                    case ROBOTO_REGULAR:
                        setTypeface(Typeface.createFromAsset(getResources().getAssets(), "RobotoCondensed-Regular.ttf"));
                        break;

                    case ROBOTO_BOLD:
                        setTypeface(Typeface.createFromAsset(getResources().getAssets(), "RobotoCondensed-Bold.ttf"));
                        break;

                    case ROBOTO_THIN:
                        setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Roboto-Thin.ttf"));
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            a.recycle();
        }
    }
}
