package com.kenny.openimgur.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.adapters.ProfileCommentAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.CommentResponse;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurComment;
import com.kenny.openimgur.classes.ImgurFilters.CommentSort;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ScrollHelper;
import com.kenny.openimgur.util.ViewUtils;

import java.util.ArrayList;

import butterknife.InjectView;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by kcampagna on 12/22/14.
 */
public class ProfileCommentsFragment extends BaseFragment implements AbsListView.OnScrollListener, AdapterView.OnItemClickListener {
    private static final String KEY_SORT = "sort";

    private static final String KEY_USER = "user";

    private static final String KEY_ITEMS = "items";

    private static final String KEY_POSITION = "position";

    private static final String KEY_PAGE = "page";

    @InjectView(R.id.multiView)
    MultiStateView mMultiStatView;

    @InjectView(R.id.commentList)
    ListView mListView;

    private int mPage = 0;

    private boolean mHasMore = true;

    private boolean mIsLoading = false;

    private ImgurUser mSelectedUser;

    private ProfileCommentAdapter mAdapter;

    private CommentSort mSort;

    private ScrollHelper mScrollHelper = new ScrollHelper();

    private FragmentListener mListener;

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
                                if (mAdapter != null) mAdapter.clear();
                                mPage = 0;
                                mHasMore = true;
                                mIsLoading = false;
                                fetchComments();
                                mMultiStatView.setViewState(MultiStateView.ViewState.LOADING);
                            }
                        }).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView.setOnScrollListener(this);
        mListView.setOnItemClickListener(this);
        mListView.setHeaderDividersEnabled(false);
        Drawable commentDivider = getResources().getDrawable(theme.isDarkTheme ? R.drawable.divider_dark : R.drawable.divider_light);
        mListView.setDivider(commentDivider);
        handleArgs(getArguments(), savedInstanceState);
    }

    private void handleArgs(Bundle args, Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
            mSort = CommentSort.getSortType(savedInstanceState.getString(KEY_SORT, null));

            if (savedInstanceState.containsKey(KEY_ITEMS)) {
                ArrayList<ImgurComment> comments = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
                mPage = savedInstanceState.getInt(KEY_PAGE, 0);
                mAdapter = new ProfileCommentAdapter(getActivity(), comments);
                mListView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), getResources().getDimensionPixelSize(R.dimen.tab_bar_height)));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mListView.addFooterView(ViewUtils.getFooterViewForComments(getActivity()));
                }

                mListView.setAdapter(mAdapter);
                mListView.setSelection(savedInstanceState.getInt(KEY_POSITION, 0));
                mMultiStatView.setViewState(MultiStateView.ViewState.CONTENT);
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
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // NOOP
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

        if (!mIsLoading && mHasMore && totalItemCount > 0 && firstVisibleItem + visibleItemCount >= totalItemCount) {
            mPage++;
            fetchComments();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position = position - mListView.getHeaderViewsCount();

        if (position >= 0 && position < mAdapter.getCount()) {
            ImgurComment comment = mAdapter.getItem(position);
            String url = "https://imgur.com/gallery/" + comment.getImageId();
            startActivity(ViewActivity.createIntent(getActivity(), url, false));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter == null || mAdapter.isEmpty()) {
            mMultiStatView.setViewState(MultiStateView.ViewState.LOADING);
            fetchComments();
        }
    }

    private void fetchComments() {
        ApiClient.getService().getProfileComments(mSelectedUser.getUsername(), mSort.getSort(), mPage, new Callback<CommentResponse>() {
            @Override
            public void success(CommentResponse commentResponse, Response response) {
                if (!isAdded()) return;

                if (!commentResponse.data.isEmpty()) {
                    if (mAdapter == null) {
                        mAdapter = new ProfileCommentAdapter(getActivity(), commentResponse.data);
                        mListView.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), getResources().getDimensionPixelSize(R.dimen.tab_bar_height)));
                        mListView.setAdapter(mAdapter);
                    } else {
                        mAdapter.addItems(commentResponse.data);
                    }

                    mMultiStatView.setViewState(MultiStateView.ViewState.CONTENT);

                    if (mPage == 0) {
                        mMultiStatView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mListView != null) mListView.setSelection(0);
                            }
                        });
                    }
                } else {
                    mHasMore = false;
                    LogUtil.v(TAG, "No more comments to be fetched");

                    // Only show empty view when the user has posted no comments
                    if (mAdapter == null || mAdapter.isEmpty()) {
                        mMultiStatView.setEmptyText(R.id.emptyMessage, getString(R.string.profile_no_comments, mSelectedUser.getUsername()));
                        mMultiStatView.setViewState(MultiStateView.ViewState.EMPTY);
                    }
                }

                mIsLoading = false;
            }

            @Override
            public void failure(RetrofitError error) {
                if (!isAdded()) return;
                LogUtil.e(TAG, "Unable to fetch comments", error);

                if (mAdapter == null || !mAdapter.isEmpty()) {
                    mMultiStatView.setErrorText(R.id.errorMessage, ApiClient.getErrorCode(error));
                    mMultiStatView.setViewState(MultiStateView.ViewState.ERROR);
                }

                mIsLoading = false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        app.getPreferences().edit().putString(KEY_SORT, mSort.getSort()).apply();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_USER, mSelectedUser);
        outState.putString(KEY_SORT, mSort.getSort());

        if (mAdapter != null && !mAdapter.isEmpty()) {
            outState.putInt(KEY_POSITION, mListView.getFirstVisiblePosition());
            outState.putParcelableArrayList(KEY_ITEMS, mAdapter.retainItems());
            outState.putInt(KEY_PAGE, mPage);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && mListView != null && mListView.getFirstVisiblePosition() <= 1 && mListener != null) {
            mListener.onUpdateActionBar(true);
        }
    }
}
