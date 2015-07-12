package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.adapters.ConvoAdapter;
import com.kenny.openimgur.api.ApiClient2;
import com.kenny.openimgur.api.responses.ConvoResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.ScrollHelper;
import com.kenny.openimgur.util.ViewUtils;

import java.util.List;

import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ProfileMessagesFragment extends BaseFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener {
    private static final String KEY_ITEMS = "items";

    @InjectView(R.id.multiView)
    MultiStateView mMultiStatView;

    @InjectView(R.id.commentList)
    ListView mListView;

    private ConvoAdapter mAdapter;

    private FragmentListener mListener;

    private ScrollHelper mScrollHelper = new ScrollHelper();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (user == null) {
            throw new IllegalStateException("User is not logged in");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof FragmentListener) mListener = (FragmentListener) activity;
    }

    @Override
    public void onDetach() {
        mListener = null;
        super.onDetach();
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
        mListView.setOnItemLongClickListener(this);
        mListView.setOnScrollListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position = position - mListView.getHeaderViewsCount();

        if (position >= 0 && position < mAdapter.getCount()) {
            ImgurConvo convo = mAdapter.getItem(position);
            startActivityForResult(ConvoThreadActivity.createIntent(getActivity(), convo), ConvoThreadActivity.REQUEST_CODE);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Hide the actionbar when scrolling down, show when scrolling up
        switch (mScrollHelper.getScrollDirection(view, firstVisibleItem, totalItemCount)) {
            case ScrollHelper.DIRECTION_DOWN:
                if (mListener != null) mListener.onUpdateActionBar(false);
                break;

            case ScrollHelper.DIRECTION_UP:
                if (mListener != null) mListener.onUpdateActionBar(true);
                break;

            case ScrollHelper.DIRECTION_NOT_CHANGED:
            default:
                break;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // NOOP
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        position = position - mListView.getHeaderViewsCount();

        if (position >= 0) {
            final ImgurConvo convo = mAdapter.getItem(position);
            new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                    .setTitle(R.string.convo_delete)
                    .setMessage(R.string.convo_delete_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteConvo(convo);
                        }
                    }).show();

            return true;
        }

        return false;
    }

    private void deleteConvo(ImgurConvo convo) {
       /* String url = String.format(Endpoints.DELETE_CONVO.getUrl(), convo.getId());
        // We don't care about the response
        new ApiClient(url, ApiClient.HttpRequest.DELETE).doWork(ImgurBusEvent.EventType.CONVO_DELETE, convo.getId(), null);
        mAdapter.removeItem(convo.getId());

        if (mAdapter.isEmpty()) {
            mMultiStatView.setViewState(MultiStateView.ViewState.EMPTY);
        }*/

        // TODO Delete
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter == null || mAdapter.isEmpty()) {
            fetchConvos();
        }
    }

    private void fetchConvos() {
        ApiClient2.getService().getConversations(new Callback<ConvoResponse>() {
            @Override
            public void success(ConvoResponse convoResponse, Response response) {
                if (convoResponse.data != null && !convoResponse.data.isEmpty()) {
                    mAdapter = new ConvoAdapter(getActivity(), convoResponse.data);
                    mListView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), getResources().getDimensionPixelSize(R.dimen.tab_bar_height)));
                    mListView.setAdapter(mAdapter);
                    mMultiStatView.setViewState(MultiStateView.ViewState.CONTENT);
                } else {
                    mMultiStatView.setEmptyText(R.id.emptyMessage, getString(R.string.profile_no_convos));
                    mMultiStatView.setViewState(MultiStateView.ViewState.EMPTY);
                }
            }

            @Override
            public void failure(RetrofitError error) {
                // TODO Error
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putParcelableArrayList(KEY_ITEMS, mAdapter.retainItems());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ConvoThreadActivity.REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ImgurConvo convo = data.getParcelableExtra(ConvoThreadActivity.KEY_BLOCKED_CONVO);
                    if (convo != null && mAdapter != null) deleteConvo(convo);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
