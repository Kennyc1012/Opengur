package com.kenny.openimgur.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.util.LogUtil;

/**
 * Created by kcampagna on 8/17/14.
 */
public class TextViewRoboto extends TextView {
    public static final int ROBOTO_LIGHT = 0;

    public static final int ROBOTO_REGULAR = 1;

    public static final int ROBOTO_BOLD = 2;

    public static final int ROBOTO_THIN = 3;

    public static final int ROBOTO_MEDIUM = 4;

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
                        if (RobotoTypeFace.ROBOTO_LIGHT == null) {
                            RobotoTypeFace.ROBOTO_LIGHT = Typeface.createFromAsset(getResources().getAssets(), "RobotoCondensed-Light.ttf");
                        }

                        setTypeface(RobotoTypeFace.ROBOTO_LIGHT);
                        break;

                    case ROBOTO_REGULAR:
                        if (RobotoTypeFace.ROBOTO_REGULAR == null) {
                            RobotoTypeFace.ROBOTO_REGULAR = Typeface.createFromAsset(getResources().getAssets(), "RobotoCondensed-Regular.ttf");
                        }

                        setTypeface(RobotoTypeFace.ROBOTO_REGULAR);
                        break;

                    case ROBOTO_BOLD:
                        if (RobotoTypeFace.ROBOTO_BOLD == null) {
                            RobotoTypeFace.ROBOTO_BOLD = Typeface.createFromAsset(getResources().getAssets(), "RobotoCondensed-Bold.ttf");
                        }

                        setTypeface(RobotoTypeFace.ROBOTO_BOLD);
                        break;

                    case ROBOTO_THIN:
                        if (RobotoTypeFace.ROBOTO_THIN == null) {
                            RobotoTypeFace.ROBOTO_THIN = Typeface.createFromAsset(getResources().getAssets(), "Roboto-Thin.ttf");
                        }

                        setTypeface(RobotoTypeFace.ROBOTO_THIN);
                        break;

                    case ROBOTO_MEDIUM:
                        if (RobotoTypeFace.ROBOTO_MEDIUM == null) {
                            RobotoTypeFace.ROBOTO_MEDIUM = Typeface.createFromAsset(getResources().getAssets(), "Roboto-Medium.ttf");
                        }

                        setTypeface(RobotoTypeFace.ROBOTO_MEDIUM);
                        break;
                }
            } catch (Exception ex) {
                LogUtil.e("TextViewRoboto", "Error Setting Roboto Typeface", ex);
            }

            a.recycle();
        }
    }

    static class RobotoTypeFace {
        static Typeface ROBOTO_LIGHT = null;

        static Typeface ROBOTO_THIN = null;

        static Typeface ROBOTO_MEDIUM = null;

        static Typeface ROBOTO_BOLD = null;

        static Typeface ROBOTO_REGULAR = null;
    }
}
