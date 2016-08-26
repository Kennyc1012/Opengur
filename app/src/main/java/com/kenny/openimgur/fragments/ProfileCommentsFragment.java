package com.kenny.openimgur.fragments;

import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.CommentResponse;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurFilters.CommentSort;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.adapters.ProfileCommentAdapter;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;
import com.kennyc.view.MultiStateView;

import java.util.ArrayList;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 12/22/14.
 */
public class ProfileCommentsFragment extends BaseFragment implements View.OnClickListener {
    private static final String KEY_SORT = "sort";

    private static final String KEY_USER = "user";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_POSITION = "position";

    private static final String KEY_PAGE = "page";

    @BindView(R.id.multiView)
    MultiStateView mMultiStatView;

    @BindView(R.id.commentList)
    RecyclerView mCommentList;

    int mPage = 0;

    boolean mHasMore = true;

    boolean mIsLoading = false;

    ImgurUser mSelectedUser;

    ProfileCommentAdapter mAdapter;

    CommentSort mSort;

    LinearLayoutManager mManager;

    public static Fragment createInstance(@NonNull ImgurUser user) {
        ProfileCommentsFragment fragment = new ProfileCommentsFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(KEY_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_comments, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_comments_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                final String[] items = getResources().getStringArray(R.array.comments_sort);

                new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                        .setTitle(R.string.sort_by)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mSort = CommentSort.getSortType(items[which]);
                                app.getPreferences().edit().putString(KEY_SORT, mSort.getSort()).apply();
                                if (mAdapter != null) mAdapter.clear();
                                mPage = 0;
                                mHasMore = true;
                                mIsLoading = false;
                                fetchComments();
                                mMultiStatView.setViewState(MultiStateView.VIEW_STATE_LOADING);
                            }
                        }).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCommentList.setLayoutManager(mManager = new LinearLayoutManager(getActivity()));

        mCommentList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int visibleItemCount = mManager.getChildCount();
                int totalItemCount = mManager.getItemCount();
                int firstVisibleItemPosition = mManager.findFirstVisibleItemPosition();

                if (mHasMore && !mIsLoading && totalItemCount > 0 && firstVisibleItemPosition + visibleItemCount >= totalItemCount) {
                    mPage++;
                    fetchComments();
                }
            }
        });

        handleArgs(getArguments(), savedInstanceState);
    }

    private void handleArgs(Bundle args, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
            mSort = CommentSort.getSortType(savedInstanceState.getString(KEY_SORT, null));

            if (savedInstanceState.containsKey(KEY_ITEMS)) {
                ArrayList<ImgurComment> comments = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
                mPage = savedInstanceState.getInt(KEY_PAGE, 0);
                mAdapter = new ProfileCommentAdapter(getActivity(), comments, this);
                mCommentList.setAdapter(mAdapter);
                mCommentList.scrollToPosition(savedInstanceState.getInt(KEY_POSITION, 0));
                mMultiStatView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }
        } else {
            if (args == null || !args.containsKey(KEY_USER)) {
                throw new IllegalArgumentException("Bundle can not be null and must contain a user");
            }

            mSelectedUser = args.getParcelable(KEY_USER);
            mSort = CommentSort.getSortType(app.getPreferences().getString(KEY_SORT, null));
        }

        if (mSelectedUser == null) {
            throw new IllegalArgumentException("A Profile must be supplied to the fragment");
        }
    }

    @Override
    public void onClick(View v) {
        ImgurComment comment = mAdapter.getItem(mCommentList.getChildAdapterPosition(v));
        String url = ApiClient.IMGUR_GALLERY_URL + comment.getImageId();
        startActivity(ViewActivity.createIntent(getActivity(), url, false));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiStatView.setViewState(MultiStateView.VIEW_STATE_LOADING);
            fetchComments();
        }
    }

    void fetchComments() {
        mIsLoading = true;
        ApiClient.getService().getProfileComments(mSelectedUser.getUsername(), mSort.getSort(), mPage).enqueue(new Callback<CommentResponse>() {
            @Override
            public void onResponse(Call<CommentResponse> call, Response<CommentResponse> response) {
                if (!isAdded()) return;

                if (response != null && response.body() != null && !response.body().data.isEmpty()) {
                    CommentResponse commentResponse = response.body();

                    if (mAdapter == null) {
                        mAdapter = new ProfileCommentAdapter(getActivity(), commentResponse.data, ProfileCommentsFragment.this);
                        mCommentList.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(commentResponse.data);
                    }

                    mMultiStatView.setViewState(MultiStateView.VIEW_STATE_CONTENT);

                    if (mPage == 0) {
                        mMultiStatView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mCommentList != null) mCommentList.scrollToPosition(0);
                            }
                        });
                    }
                } else {
                    mHasMore = false;
                    LogUtil.v(TAG, "No more comments to be fetched");

                    // Only show empty view when the user has posted no comments
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        ViewUtils.setEmptyText(mMultiStatView, R.id.emptyMessage, getString(R.string.profile_no_comments, mSelectedUser.getUsername()));
                        mMultiStatView.setViewState(MultiStateView.VIEW_STATE_EMPTY);
                    }
                }

                mIsLoading = false;
            }

            @Override
            public void onFailure(Call<CommentResponse> call, Throwable t) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch comments", t);

                if (mAdapter == null || !mAdapter.isEmpty()) {
                    ViewUtils.setErrorText(mMultiStatView, R.id.errorMessage, ApiClient.getErrorCode(t));
                    mMultiStatView.setViewState(MultiStateView.VIEW_STATE_ERROR);
                }

                mIsLoading = false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_USER, mSelectedUser);
        outState.putString(KEY_SORT, mSort.getSort());

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putInt(KEY_POSITION, mManager.findFirstVisibleItemPosition());
            outState.putParcelableArrayList(KEY_ITEMS, mAdapter.retainItems());
            outState.putInt(KEY_PAGE, mPage);
        }
    }
}
