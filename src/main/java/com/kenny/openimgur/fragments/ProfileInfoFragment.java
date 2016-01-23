package com.kenny.openimgur.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.activities.ProfileActivity;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.adapters.ProfileInfoAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.TrophyResponse;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurTrophy;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.LinkUtils;
import com.kennyc.view.MultiStateView;

import java.util.List;

import butterknife.Bind;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileInfoFragment extends BaseFragment implements ImgurListener {
    private static final String KEY_TROPHIES = "trophies";

    private static final String KEY_USER = "user";

    @Bind(R.id.list)
    RecyclerView mList;

    @Bind(R.id.container)
    MultiStateView mMultiStateView;

    private ProfileInfoAdapter mAdapter;

    private ImgurUser mSelectedUser;

    public static ProfileInfoFragment createInstance(@NonNull ImgurUser user) {
        ProfileInfoFragment fragment = new ProfileInfoFragment();
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
        return inflater.inflate(R.layout.fragment_profile_info, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        List<ImgurTrophy> trophies = null;

        if (savedInstanceState != null) {
            mSelectedUser = savedInstanceState.getParcelable(KEY_USER);
            trophies = savedInstanceState.getParcelableArrayList(KEY_TROPHIES);
        }

        if (mSelectedUser == null) {
            Bundle bundle = getArguments();
            if (bundle == null || !bundle.containsKey(KEY_USER)) {
                throw new IllegalArgumentException("Bundle can not be null and must contain a user");
            } else {
                mSelectedUser = bundle.getParcelable(KEY_USER);
            }
        }


        mList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        if (trophies != null && !trophies.isEmpty()) {
            mAdapter = new ProfileInfoAdapter(getActivity(), trophies, mSelectedUser, this);
            mList.setAdapter(mAdapter);
            mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
        } else {
            fetchTrophies();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.message).setVisible(app.getUser() != null && !mSelectedUser.isSelf(app));
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.message:
                if (user != null) {
                    ImgurConvo convo = new ImgurConvo(mSelectedUser.getUsername(), mSelectedUser.getId());
                    startActivity(ConvoThreadActivity.createIntent(getActivity(), convo));
                } else {
                    Snackbar.make(mMultiStateView, R.string.user_not_logged_in, Snackbar.LENGTH_LONG).show();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        CustomLinkMovement.getInstance().addListener(this);
    }

    @Override
    public void onPause() {
        CustomLinkMovement.getInstance().removeListener(this);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) mAdapter.onDestroy();
        super.onDestroyView();
    }

    @Override
    public void onPhotoTap(View view) {
        // Adjust for header
        int adapterPosition = mList.getChildAdapterPosition(view) - 1;

        if (adapterPosition != RecyclerView.NO_POSITION) {
            ImgurTrophy trophy = mAdapter.getItem(adapterPosition);
            String dataLink = trophy.getDataLink();
            String id = LinkUtils.getGalleryId(trophy.getDataLink());

            if (!TextUtils.isEmpty(id)) {
                startActivity(ViewActivity.createGalleryIntentIntent(getActivity(), id));
            } else if (!TextUtils.isEmpty(dataLink)) {
                onLinkTap(null, dataLink);
            }
        }
    }

    @Override
    public void onPlayTap(ProgressBar prog, FloatingActionButton play, ImageView image, VideoView video, final View view) {
        // NOOP
    }

    @Override
    public void onLinkTap(View view, @Nullable String url) {
        if (!TextUtils.isEmpty(url)) {
            LinkUtils.LinkMatch match = LinkUtils.findImgurLinkMatch(url);

            switch (match) {
                case GALLERY:
                    String id = LinkUtils.getGalleryId(url);

                    if (!TextUtils.isEmpty(id)) {
                        startActivity(ViewActivity.createGalleryIntentIntent(getActivity(), id));
                    }
                    break;

                case ALBUM:
                    String albumId = LinkUtils.getAlbumId(url);
                    Intent intent = ViewActivity.createAlbumIntent(getActivity(), albumId);
                    startActivity(intent);
                    break;

                case IMAGE_URL:
                    PopupImageDialogFragment.getInstance(url, url.endsWith(".gif"), true, false)
                            .show(getFragmentManager(), "popup");
                    break;

                case IMAGE_URL_QUERY:
                    int index = url.indexOf("?");
                    url = url.substring(0, index);
                    // Intentional fallthrough
                case DIRECT_LINK:
                    boolean isAnimated = LinkUtils.isLinkAnimated(url);
                    boolean isVideo = LinkUtils.isVideoLink(url);
                    PopupImageDialogFragment.getInstance(url, isAnimated, true, isVideo)
                            .show(getFragmentManager(), "popup");
                    break;

                case IMAGE:
                    String[] split = url.split("\\/");
                    PopupImageDialogFragment.getInstance(split[split.length - 1], false, false, false)
                            .show(getFragmentManager(), "popup");
                    break;

                case USER_CALLOUT:
                    startActivity(ProfileActivity.createIntent(getActivity(), url.replace("@", "")));
                    break;

                case USER:
                    String username = LinkUtils.getUsername(url);

                    if (!TextUtils.isEmpty(username)) {
                        startActivity(ProfileActivity.createIntent(getActivity(), username));
                    }
                    break;

                case NONE:
                default:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                    if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        Snackbar.make(mMultiStateView, R.string.cant_launch_intent, Snackbar.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    @Override
    public void onViewRepliesTap(View view) {
        // NOOP
    }

    @Override
    public void onPhotoLongTapListener(View view) {
        // NOOP
    }

    private void fetchTrophies() {
        ApiClient.getService().getProfileTrophies(mSelectedUser.getUsername()).enqueue(new Callback<TrophyResponse>() {
            @Override
            public void onResponse(Call<TrophyResponse> call, Response<TrophyResponse> response) {
                if (response.body() != null && response.body().data != null) {
                    mAdapter = new ProfileInfoAdapter(getActivity(), response.body().data.trophies, mSelectedUser, ProfileInfoFragment.this);
                } else {
                    mAdapter = new ProfileInfoAdapter(getActivity(), null, mSelectedUser, ProfileInfoFragment.this);
                }

                mList.setAdapter(mAdapter);
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }

            @Override
            public void onFailure(Call<TrophyResponse> call, Throwable t) {
                mAdapter = new ProfileInfoAdapter(getActivity(), null, mSelectedUser, ProfileInfoFragment.this);
                mList.setAdapter(mAdapter);
                mMultiStateView.setViewState(MultiStateView.VIEW_STATE_CONTENT);
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putParcelableArrayList(KEY_TROPHIES, mAdapter.retainItems());
        }

        outState.putParcelable(KEY_USER, mSelectedUser);
    }
}
