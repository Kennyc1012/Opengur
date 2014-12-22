package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.FloatingActionButton;
import com.kenny.openimgur.ui.TextViewRoboto;

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

    private ImgurUser mUser;

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

        mUser = bundle.getParcelable(KEY_USER);
        setupInfo();
    }

    /**
     * Sets up the view to display the user's info
     */
    private void setupInfo() {
        mFlexibleSpace.setBackgroundColor(getResources().getColor(theme.primaryColor));
        mMessageBtn.setColor(getResources().getColor(theme.accentColor));

        String date = new SimpleDateFormat("MMM yyyy").format(new Date(mUser.getCreated()));
        String reputationText = mUser.getReputation() + " " + getString(R.string.profile_rep_date, date);
        mNotoriety.setText(mUser.getNotoriety().getStringId());
        mNotoriety.setTextColor(getResources().getColor(mUser.getNotoriety().getNotorietyColor()));
        mRep.setText(reputationText);
        mUserName.setText(mUser.getUsername());

        if (!TextUtils.isEmpty(mUser.getBio())) {
            mBio.setText(mUser.getBio());
            /* TODO
            mBio.setMovementMethod(CustomLinkMovement.getInstance(listener));
            Linkify.addLinks(mBio, Linkify.WEB_URLS);*/
        }

        if (mUser.isSelf()) {
            mMessageBtn.setVisibility(View.GONE);
        } else {
            mMessageBtn.setColor(getResources().getColor(theme.accentColor));
        }
    }

    @OnClick({R.id.messageBtn})
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.messageBtn:
                // TODO
                break;
        }
    }
}
