package com.kenny.openimgur.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ViewUtils;

/**
 * Created by kcampagna on 8/14/14.
 */
public class RedditFragment extends Fragment {
    private static final long SEARCH_DELAY = 1000L;

    private EditText mSearchEditText;

    private MultiStateView mMultiView;

    private GridView mGridView;

    private RedditHandler mHandler = new RedditHandler();

    private String mQuery;

    private GalleryAdapter mAdapter;

    private ApiClient mApiclient;

    public static RedditFragment createInstance() {
        return new RedditFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reddit, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Add the empty view atop the layout to account for style
        ((LinearLayout) view).addView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0), 0);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiStateView);
        mGridView = (GridView) mMultiView.findViewById(R.id.grid);
        mSearchEditText = (EditText) view.findViewById(R.id.search);
        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                mHandler.removeMessages(RedditHandler.MESSAGE_AUTO_SEARCH);
                String text = textView.getText().toString();

                if (!TextUtils.isEmpty(text)) {
                    if (mAdapter != null) {
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    mQuery = text;
                    search();
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    return true;
                }

                return false;
            }
        });

        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                mHandler.removeMessages(RedditHandler.MESSAGE_AUTO_SEARCH);

                if (!TextUtils.isEmpty(charSequence)) {
                    mHandler.sendMessageDelayed(RedditHandler.MESSAGE_AUTO_SEARCH, charSequence, SEARCH_DELAY);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void search() {

    }

    private class RedditHandler extends ImgurHandler {
        public static final int MESSAGE_AUTO_SEARCH = 2;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_AUTO_SEARCH:
                    if (mAdapter != null) {
                        mAdapter.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    mQuery = (String) msg.obj;
                    search();
                    mMultiView.setViewState(MultiStateView.ViewState.LOADING);
                    break;

                case MESSAGE_ACTION_COMPLETE:

                    break;

                default:
                    break;
            }

        }
    }
}
