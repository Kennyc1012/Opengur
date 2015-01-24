package com.kenny.openimgur.fragments;

import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.adapters.ConvoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.ThrowableFailureEvent;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ProfileMessagesFragment extends BaseFragment implements AdapterView.OnItemClickListener {
    private static final String KEY_ITEMS = "items";

    @InjectView(R.id.multiView)
    MultiStateView mMultiStatView;

    @InjectView(R.id.commentList)
    ListView mListView;

    private ConvoAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (user == null) {
            throw new IllegalStateException("User is not logged in");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_ITEMS)) {
            List<ImgurConvo> items = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
            mAdapter = new ConvoAdapter(getActivity(), items);
            mListView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), getResources().getDimensionPixelSize(R.dimen.tab_bar_height)));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mListView.addFooterView(ViewUtils.getFooterViewForComments(getActivity()));
            }

            mListView.setAdapter(mAdapter);
            mMultiStatView.setViewState(MultiStateView.ViewState.CONTENT);
        }

        mListView.setHeaderDividersEnabled(false);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position = position - mListView.getHeaderViewsCount();
        ImgurConvo convo = mAdapter.getItem(position);
        startActivity(ConvoThreadActivity.createIntent(getActivity(), convo));
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (mAdapter == null || mAdapter.isEmpty()) {
            new ApiClient(Endpoints.ACCOUNT_CONVOS.getUrl(), ApiClient.HttpRequest.GET)
                    .doWork(ImgurBusEvent.EventType.ACCOUNT_CONVOS, null, null);
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.ACCOUNT_CONVOS) {
            try {
                int status = event.json.getInt(ApiClient.KEY_STATUS);

                if (status == ApiClient.STATUS_OK) {
                    JSONArray data = event.json.getJSONArray(ApiClient.KEY_DATA);
                    List<ImgurConvo> convos = new ArrayList<>(data.length());

                    for (int i = 0; i < data.length(); i++) {
                        convos.add(new ImgurConvo(data.getJSONObject(i)));
                    }

                    if (convos.isEmpty()) {
                        mHandler.sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                    } else {
                        mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, convos);
                    }

                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(status));
                }

            } catch (JSONException ex) {
                LogUtil.e(TAG, "Error parsing profile comments", ex);
                mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
            }
        }
    }

    public void onEventMainThread(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();

        if (e instanceof IOException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_IO_EXCEPTION));
        } else if (e instanceof JSONException) {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
        } else {
            mHandler.sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_INTERNAL_ERROR));
        }

        LogUtil.e(TAG, "Error received from Event Bus", e);

    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurConvo> convos = (ArrayList<ImgurConvo>) msg.obj;
                    mAdapter = new ConvoAdapter(getActivity(), convos);
                    mListView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), getResources().getDimensionPixelSize(R.dimen.tab_bar_height)));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        mListView.addFooterView(ViewUtils.getFooterViewForComments(getActivity()));
                    }

                    mListView.setAdapter(mAdapter);
                    mMultiStatView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case MESSAGE_ACTION_FAILED:
                    mMultiStatView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                    mMultiStatView.setViewState(MultiStateView.ViewState.ERROR);
                    break;

                case MESSAGE_EMPTY_RESULT:
                    mMultiStatView.setEmptyText(R.id.emptyMessage, getString(R.string.profile_no_convos));
                    mMultiStatView.setViewState(MultiStateView.ViewState.EMPTY);
                    break;
            }

            super.handleMessage(msg);
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mAdapter.retainItems());
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

      /*  if (isVisibleToUser && mListView != null && mListView.getFirstVisiblePosition() <= 1 && li != null) {
            mListener.onUpdateActionBar(true);
        }*/
    }
}
