package com.kenny.openimgur.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.activities.ProfileActivity;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.classes.CustomLinkMovement;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.ui.VideoView;
import com.kenny.openimgur.util.LinkUtils;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileInfoFragment extends BaseFragment implements View.OnClickListener, ImgurListener {
    private static final String KEY_USER = "user";

    @InjectView(R.id.userName)
    TextView mUserName;

    @InjectView(R.id.flexibleSpace)
    FrameLayout mFlexibleSpace;

    @InjectView(R.id.notoriety)
    TextViewRoboto mNotoriety;

    @InjectView(R.id.rep)
    TextViewRoboto mRep;

    @InjectView(R.id.bio)
    TextViewRoboto mBio;

    @InjectView(R.id.messageBtn)
    FloatingActionButton mMessageBtn;

    @InjectView(R.id.container)
    View mContainer;

    private ImgurUser mSelectedUser;

    public static ProfileInfoFragment createInstance(@NonNull ImgurUser user) {
        ProfileInfoFragment fragment = new ProfileInfoFragment();
        Bundle args = new Bundle(1);
        args.putParcelable(KEY_USER, user);
        fragment.setArguments(args);
        return fragment;
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
        mContainer.setPadding(0, tabHeight + translucentHeight, 0, 0);
        mSelectedUser = bundle.getParcelable(KEY_USER);
        setupInfo();
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
        mFlexibleSpace.setBackgroundColor(getResources().getColor(theme.primaryColor));
        mMessageBtn.setColor(getResources().getColor(theme.accentColor));

        String date = new SimpleDateFormat("MMM yyyy").format(new Date(mSelectedUser.getCreated()));
        String reputationText = mSelectedUser.getReputation() + " " + getString(R.string.profile_rep_date, date);
        mNotoriety.setText(mSelectedUser.getNotoriety().getStringId());
        mNotoriety.setTextColor(getResources().getColor(mSelectedUser.getNotoriety().getNotorietyColor()));
        mRep.setText(reputationText);
        mUserName.setText(mSelectedUser.getUsername());

        if (!TextUtils.isEmpty(mSelectedUser.getBio())) {
            mBio.setText(mSelectedUser.getBio());
            mBio.setMovementMethod(CustomLinkMovement.getInstance(this));
            Linkify.addLinks(mBio, Linkify.WEB_URLS);
        }

        Drawable icon = mSelectedUser.isSelf() ? getResources().getDrawable(R.drawable.ic_logout) :
                getResources().getDrawable(R.drawable.ic_action_message);
        mMessageBtn.setDrawable(icon);
        mMessageBtn.setColor(getResources().getColor(theme.accentColor));
    }

    @OnClick({R.id.messageBtn})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.messageBtn:
                if (mSelectedUser.isSelf(app)) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.logout)
                            .setMessage(R.string.logout_confirm)
                            .setNegativeButton(R.string.cancel, null)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ((ProfileActivity) getActivity()).onUserLogout();
                                }
                            }).show();
                } else {
                    if (app.getUser() != null) {
                        ImgurConvo convo = ImgurConvo.createConvo(mSelectedUser.getUsername(), mSelectedUser.getId());
                        startActivity(ConvoThreadActivity.createIntent(getActivity(), convo));
                    } else {
                        SnackBar.show(getActivity(), R.string.user_not_logged_in);
                    }
                }
                break;
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
    public void onPlayTap(ProgressBar prog, ImageButton play, ImageView image, VideoView video) {
        // NOOP
    }

    @Override
    public void onLinkTap(View view, @Nullable String url) {
        if (!TextUtils.isEmpty(url)) {
            LinkUtils.LinkMatch match = LinkUtils.findImgurLinkMatch(url);

            switch (match) {
                case GALLERY:
                    Intent intent = ViewActivity.createIntent(getActivity(), url).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;

                case IMAGE_URL:
                    PopupImageDialogFragment.getInstance(url, url.endsWith(".gif"), true, false)
                            .show(getFragmentManager(), "popup");
                    break;

                case VIDEO_URL:
                    PopupImageDialogFragment.getInstance(url, true, true, true)
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
