package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.BasicResponse;
import com.kenny.openimgur.api.responses.ConvoResponse;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.ui.adapters.ConvoAdapter;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.StateSaver;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.List;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 12/24/14.
 */
public class ProfileMessagesFragment extends BaseFragment implements View.OnClickListener, View.OnLongClickListener {
    private static final String KEY_ITEMS = "items";

    @BindView(R.id.multiView)
    MultiStateView mMultiStateView;

    @BindView(R.id.commentList)
    RecyclerView mMessageList;

    ConvoAdapter mAdapter;

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
    public void onDestroyView() {
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        ImgurConvo convo = mAdapter.getItem(mMessageList.getChildAdapterPosition(v));
        startActivityForResult(ConvoThreadActivity.createIntent(getActivity(), convo), RequestCodes.CONVO);
    }

    @Override
    public boolean onLongClick(View v) {
        final ImgurConvo convo = mAdapter.getItem(mMessageList.getChildAdapterPosition(v));
        new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                .setTitle(R.string.convo_delete)
                .setMessage(R.string.convo_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteConversation(convo.getId());
                    }
                }).show();

        return true;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMessageList.setLayoutManager(new LinearLayoutManager(getActivity()));
        List<ImgurConvo> items = StateSaver.instance().getData(savedInstanceState, KEY_ITEMS);

        if (items != null && !items.isEmpty()) {
            mAdapter = new ConvoAdapter(getActivity(), items, this, this);
            mMessageList.setAdapter(mAdapter);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) fetchConvos();
    }

    private void fetchConvos() {
        ApiClient.getService().getConversations().enqueue(new Callback<ConvoResponse>() {
            @Override
            public void onResponse(Call<ConvoResponse> call, Response<ConvoResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null) {
                    ConvoResponse convoResponse = response.body();

                    if (convoResponse.data != null && !convoResponse.data.isEmpty()) {
                        mAdapter = new ConvoAdapter(getActivity(), convoResponse.data, ProfileMessagesFragment.this, ProfileMessagesFragment.this);
                        mMessageList.setAdapter(mAdapter);
                        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
                    } else {
                        ViewUtils.setEmptyText(mMultiStateView, R.id.emptyMessage, getString(R.string.profile_no_convos));
                        mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                    }
                } else {
                    ViewUtils.setEmptyText(mMultiStateView, R.id.emptyMessage, getString(R.string.profile_no_convos));
                    mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                }
            }

            @Override
            public void onFailure(Call<ConvoResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch convos", t);
                ViewUtils.setErrorText(mMultiStateView, R.id.errorMessage, ApiClient.getErrorCode(t));
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_ERROR);
            }
        });
    }

    void deleteConversation(String id) {
        mAdapter.removeItem(id);

        if (mAdapter.isEmpty()) {
            ViewUtils.setEmptyText(mMultiStateView, R.id.emptyMessage, getString(R.string.profile_no_convos));
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
        }

        ApiClient.getService().deleteConversation(id).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (response != null && response.body() != null) {
                    LogUtil.v(TAG, "Result of convo deletion " + response);
                } else {
                    LogUtil.v(TAG, "Received an empty response when deleting convo");
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                LogUtil.e(TAG, "Error deleting conversation", t);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null && !mAdapter.isEmpty()) {
            StateSaver.instance().onSaveState(outState, KEY_ITEMS, mAdapter.retainItems());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RequestCodes.CONVO:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ImgurConvo convo = data.getParcelableExtra(ConvoThreadActivity.KEY_BLOCKED_CONVO);
                    if (convo != null && mAdapter != null) deleteConversation(convo.getId());
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
