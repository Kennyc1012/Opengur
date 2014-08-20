package com.kenny.openimgur.fragments;

import android.content.Context;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.Button;

import com.kenny.openimgur.R;
import com.kenny.openimgur.ui.TextViewRoboto;

/**
 * Creates the view for a popup dialog with an L material design
 * Created by kcampagna on 8/19/14.
 */
public class PopupDialogViewBuilder {
    private TextViewRoboto mTitle;

    private TextViewRoboto mMessage;

    private Button mNegativeBtn;

    private Button mPositiveBtn;

    private View mView;

    public PopupDialogViewBuilder(Context context) {
        mView = View.inflate(context, R.layout.popup_fragment, null);
        mTitle = (TextViewRoboto) mView.findViewById(R.id.title);
        mMessage = (TextViewRoboto) mView.findViewById(R.id.message);
        mNegativeBtn = (Button) mView.findViewById(R.id.negative);
        mPositiveBtn = (Button) mView.findViewById(R.id.positive);
    }

    public PopupDialogViewBuilder setTitle(@StringRes int title) {
        mTitle.setText(title);
        return this;
    }

    public PopupDialogViewBuilder setMessage(@StringRes int message) {
        mMessage.setText(message);
        return this;
    }

    public PopupDialogViewBuilder setNegativeButton(@StringRes int text, View.OnClickListener onClickListener) {
        mNegativeBtn.setText(text);
        mNegativeBtn.setOnClickListener(onClickListener);
        return this;
    }

    public PopupDialogViewBuilder setPositiveButton(@StringRes int text, View.OnClickListener onClickListener) {
        mPositiveBtn.setText(text);
        mPositiveBtn.setOnClickListener(onClickListener);
        return this;
    }

    public View build() {
        return mView;
    }

}
