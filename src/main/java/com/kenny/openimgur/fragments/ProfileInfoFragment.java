package com.kenny.openimgur.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ConvoThreadActivity;
import com.kenny.openimgur.activities.ProfileActivity;
import com.kenny.openimgur.classes.ImgurConvo;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.ViewUtils;
import com.kenny.snackbar.SnackBar;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.InjectView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 12/14/14.
 */
public class ProfileInfoFragment extends BaseFragment implements View.OnClickListener {
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
    RelativeLayout mContainer;

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
            /* TODO
            mBio.setMovementMethod(CustomLinkMovement.getInstance(listener));
            Linkify.addLinks(mBio, Linkify.WEB_URLS);*/
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
}
