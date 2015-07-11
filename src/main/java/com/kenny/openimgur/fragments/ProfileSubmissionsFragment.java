package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient2;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.MultiStateView;

/**
 * Created by kcampagna on 12/23/14.
 */
public class ProfileSubmissionsFragment extends BaseGridFragment2 {
    private static final String KEY_USER = "user";

    private ImgurUser mSelectedUser;

    public static ProfileSubmissionsFragment newInstance(@NonNull ImgurUser user) {
        ProfileSubmissionsFragment fragment = new ProfileSubmissionsFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(KEY_USER, user);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    protected void fetchGallery() {
        ApiClient2.getService().getProfileSubmissions(mSelectedUser.getUsername(), mCurrentPage, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_USER, mSelectedUser);
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);
        if (savedInstanceState != null) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
        } else {
            mSelectedUser = getArguments().getParcelable(KEY_USER);
        }

        if (mSelectedUser == null)
            throw new IllegalArgumentException("Profile must be supplied to fragment");
    }

    @Override
    protected int getAdditionalHeaderSpace() {
        return getResources().getDimensionPixelSize(R.dimen.tab_bar_height);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && mGrid != null && mGrid.getFirstVisiblePosition() <= 1 && mListener != null) {
            mListener.onUpdateActionBar(true);
        }
    }

    @Override
    protected void onEmptyResults() {
        mHasMore = false;

        if (getAdapter() == null || getAdapter().isEmpty()) {
            String errorMessage = getString(R.string.profile_no_submissions, mSelectedUser.getUsername());
            mMultiStateView.setErrorText(R.id.errorMessage, errorMessage);
            mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
        }
    }
}
