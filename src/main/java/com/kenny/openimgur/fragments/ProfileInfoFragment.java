package com.kenny.openimgur.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.activities.ProfileActivity;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.InjectView;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileInfoFragment extends BaseFragment implements ImgurListener {
    private static final String KEY_USER = "user";

    @InjectView(R.id.username)
    TextView mUserName;

    @InjectView(R.id.notoriety)
    TextView mNotoriety;

    @InjectView(R.id.rep)
    TextView mRep;

    @InjectView(R.id.bio)
    TextView mBio;

    @InjectView(R.id.date)
    TextView mDate;

    @InjectView(R.id.container)
    ScrollView mContainer;

    @InjectView(R.id.infoContainer)
    LinearLayout mInfoContainer;

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
        Bundle bundle = getArguments();

        if (bundle == null || !bundle.containsKey(KEY_USER)) {
            throw new IllegalArgumentException("Bundle can not be null and must contain a user");
        }

        int tabHeight = getResources().getDimensionPixelSize(R.dimen.tab_bar_height);
        int translucentHeight = ViewUtils.getHeightForTranslucentStyle(getActivity());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mInfoContainer.addView(ViewUtils.getFooterViewForComments(getActivity()));
        }

        mContainer.setPadding(0, tabHeight + translucentHeight, 0, 0);
        mSelectedUser = bundle.getParcelable(KEY_USER);
        setupInfo();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.profile_info, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.message).setVisible(!mSelectedUser.isSelf(app));
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.message:
                if (app.getUser() != null) {
                    ImgurConvo convo = ImgurConvo.createConvo(mSelectedUser.getUsername(), mSelectedUser.getId());
                    startActivity(ConvoThreadActivity.createIntent(getActivity(), convo));
                } else {
                    SnackBar.show(getActivity(), R.string.user_not_logged_in);
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

    /**
     * Sets up the view to display the user's info
     */
    private void setupInfo() {
        mUserName.setBackgroundColor(getResources().getColor(theme.primaryColor));
        String date = new SimpleDateFormat("MMM yyyy").format(new Date(mSelectedUser.getCreated()));
        mNotoriety.setText(mSelectedUser.getNotoriety().getStringId());
        mNotoriety.setTextColor(getResources().getColor(mSelectedUser.getNotoriety().getNotorietyColor()));
        mRep.setText(getString(R.string.profile_rep, mSelectedUser.getReputation()));
        mUserName.setText(mSelectedUser.getUsername());
        mDate.setText(getString(R.string.profile_date, date));

        if (!TextUtils.isEmpty(mSelectedUser.getBio())) {
            mBio.setText(mSelectedUser.getBio());
            mBio.setMovementMethod(CustomLinkMovement.getInstance(this));
            Linkify.addLinks(mBio, Linkify.WEB_URLS);
        } else {
            mBio.setText(getString(R.string.profile_bio_empty, mSelectedUser.getUsername()));
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // Force the action bar to show
        if (getActivity() instanceof ProfileActivity) {
            ((ProfileActivity) getActivity()).onUpdateActionBar(true);
        }
    }

    @Override
    public void onPhotoTap(View view) {
        // NOOP
    }

    @Override
    public void onPlayTap(ProgressBar prog, FloatingActionButton play, ImageView image, VideoView video) {
        // NOOP
    }

    @Override
    public void onLinkTap(View view, @Nullable String url) {
        if (!TextUtils.isEmpty(url)) {
            LinkUtils.LinkMatch match = LinkUtils.findImgurLinkMatch(url);

            switch (match) {
                case GALLERY:
                case ALBUM:
                    Intent intent = ViewActivity.createIntent(getActivity(), url, match == LinkUtils.LinkMatch.ALBUM).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case IMAGE_URL:
                    PopupImageDialogFragment.getInstance(url, url.endsWith(".gif"), true, false)
                            .show(getFragmentManager(), "popup");
                    break;

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

                case NONE:
                default:
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                    if (browserIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        startActivity(browserIntent);
                    } else {
                        SnackBar.show(getActivity(), R.string.cant_launch_intent);
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
}
