package com.kenny.openimgur.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.FragmentListener;
import com.kenny.openimgur.classes.ImgurTheme;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.classes.OpengurApp;
import com.kenny.openimgur.fragments.BaseGridFragment;
import com.kenny.openimgur.fragments.GalleryFragment;
import com.kenny.openimgur.fragments.MemeFragment;
import com.kenny.openimgur.fragments.RandomFragment;
import com.kenny.openimgur.fragments.RedditFragment;
import com.kenny.openimgur.fragments.TopicsFragment;
import com.kenny.openimgur.fragments.UploadedPhotosFragment;
import com.kenny.openimgur.ui.adapters.TopicsAdapter;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.RequestCodes;
import com.kenny.openimgur.util.SqlHelper;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by kcampagna on 10/19/14.
 */
public class MainActivity extends BaseActivity implements FragmentListener, NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {
    private static final String KEY_CURRENT_PAGE = "current_page";

    public static final int PAGE_PROFILE = 0;

    public static final int PAGE_GALLERY = 1;

    public static final int PAGE_TOPICS = 2;

    public static final int PAGE_MEME = 3;

    public static final int PAGE_SUBREDDIT = 4;

    public static final int PAGE_RANDOM = 5;

    public static final int PAGE_UPLOADS = 6;

    public static final int PAGE_SETTINGS = 7;

    public static final int PAGE_BETA = 8;

    public static final int PAGE_FEEDBACK = 9;

    @BindView(R.id.drawerLayout)
    DrawerLayout mDrawer;

    @BindView(R.id.fab)
    FloatingActionButton mUploadButton;

    @BindView(R.id.toolBar)
    Toolbar mToolBar;

    @BindView(R.id.navigationView)
    NavigationView mNavigationView;

    @BindView(R.id.topicsSpinner)
    Spinner mTopicsSpinner;

    @BindView(R.id.appbar)
    AppBarLayout mAppBar;

    @BindView(R.id.coordinatorLayout)
    CoordinatorLayout mCoordinatorLayout;

    ImageView mAvatar;

    TextView mName;

    TextView mRep;

    TextView mBadge;

    View mBadgeContainer;

    private int mCurrentPage = -1;

    private ImgurTheme mSavedTheme;

    private boolean mIsDarkTheme;

    private boolean mNagOnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View headerView = mNavigationView.getHeaderView(0);
        mAvatar = (ImageView) headerView.findViewById(R.id.profileImg);
        mName = (TextView) headerView.findViewById(R.id.profileName);
        mRep = (TextView) headerView.findViewById(R.id.reputation);
        mBadge = (TextView) headerView.findViewById(R.id.badgeCount);
        mBadgeContainer = headerView.findViewById(R.id.badgeContainer);
        mBadgeContainer.setOnClickListener(this);
        headerView.setOnClickListener(this);
        mNavigationView.setNavigationItemSelectedListener(this);
        ColorStateList selector = theme.getNavigationColors(getResources());
        mNavigationView.setItemTextColor(selector);
        mNavigationView.setItemIconTintList(selector);
        setupToolBar();
        mNagOnExit = app.getPreferences().getBoolean(SettingsActivity.KEY_CONFIRM_EXIT, true);

        mTopicsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Fragment fragment = getFragmentManager().findFragmentById(R.id.container);

                // Should always be the case if the spinner is visible
                if (fragment instanceof TopicsFragment) {
                    ImgurTopic topic = (ImgurTopic) mTopicsSpinner.getAdapter().getItem(mTopicsSpinner.getSelectedItemPosition());
                    ((TopicsFragment) fragment).onTopicChanged(topic);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // NOOP
            }
        });
    }

    /**
     * Sets up the tool bar to take the place of the action bar
     */
    private void setupToolBar() {
        setSupportActionBar(mToolBar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setHomeAsUpIndicator(R.drawable.ic_action_navigation_menu_24dp);
        ab.setHomeButtonEnabled(true);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem menuItem) {
        if (menuItem.getGroupId() != R.id.nonCheckableGroup) menuItem.setChecked(true);
        changePage(menuItem.getItemId());
        mDrawer.closeDrawers();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawer.openDrawer(GravityCompat.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Dismiss any filter fragments that might be left behind
        if (getFragmentManager().findFragmentByTag("filter") != null) {
            FragmentManager fm = getFragmentManager();
            fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commitAllowingStateLoss();
        }

        int fragmentPage;
        int menuItemId = R.id.nav_gallery;

        if (savedInstanceState == null) {
            fragmentPage = app.getPreferences().getInt(KEY_CURRENT_PAGE, PAGE_GALLERY);
        } else {
            fragmentPage = savedInstanceState.getInt(KEY_CURRENT_PAGE, PAGE_GALLERY);
        }

        if (mNavigationView.getMenu() != null) {
            switch (fragmentPage) {
                case PAGE_TOPICS:
                    menuItemId = R.id.nav_topics;
                    break;

                case PAGE_SUBREDDIT:
                    menuItemId = R.id.nav_reddit;
                    break;

                case PAGE_MEME:
                    menuItemId = R.id.nav_meme;
                    break;

                case PAGE_RANDOM:
                    menuItemId = R.id.nav_random;
                    break;

                case PAGE_UPLOADS:
                    menuItemId = R.id.nav_uploads;
                    break;

                case PAGE_GALLERY:
                default:
                    menuItemId = R.id.nav_gallery;
                    break;
            }

            MenuItem item = mNavigationView.getMenu().findItem(menuItemId);
            if (item != null) item.setChecked(true);
        }

        if (getFragmentManager().findFragmentById(R.id.container) == null) changePage(menuItemId);
        updateUserHeader(user);
        checkForImgurNag();
    }

    private void updateUserHeader(@Nullable ImgurUser user) {
        if (user != null) {
            int size = getResources().getDimensionPixelSize(R.dimen.avatar_size);
            String firstLetter = user.getUsername().substring(0, 1).toUpperCase();

            mAvatar.setImageDrawable(new TextDrawable.Builder()
                    .setWidth(size)
                    .setHeight(size)
                    .setShape(TextDrawable.DRAWABLE_SHAPE_OVAL)
                    .setColor(getResources().getColor(theme.accentColor))
                    .setText(firstLetter)
                    .build());

            mName.setText(user.getUsername());
            mRep.setText(user.getNotoriety().getStringId());
            mBadgeContainer.setVisibility(View.VISIBLE);
            int notificationCount = SqlHelper.getInstance(getApplicationContext()).getNotifications(true).size();
            updateNotificationBadge(notificationCount);
        } else {
            mAvatar.setImageResource(R.drawable.ic_account_circle_24dp);
            mName.setText(R.string.profile);
            mRep.setText(R.string.login_msg);
            mBadge.setVisibility(View.GONE);
            mBadgeContainer.setVisibility(View.GONE);
        }
    }

    private void updateNotificationBadge(int notificationCount) {
        mBadge.setVisibility(notificationCount > 0 ? View.VISIBLE : View.GONE);
        String badgeText = null;

        if (notificationCount > 0) {
            badgeText = notificationCount > 9 ? "9+" : String.valueOf(notificationCount);
        }

        mBadge.setText(badgeText);
    }

    /**
     * Changes the fragment
     *
     * @param menuItemId The menu item id of the page selected
     */
    private void changePage(int menuItemId) {
        Fragment fragment = null;

        switch (menuItemId) {
            case R.id.nav_gallery:
                if (mCurrentPage == PAGE_GALLERY) return;
                fragment = GalleryFragment.newInstance();
                mCurrentPage = PAGE_GALLERY;
                break;

            case R.id.nav_reddit:
                if (mCurrentPage == PAGE_SUBREDDIT) return;
                fragment = RedditFragment.newInstance();
                mCurrentPage = PAGE_SUBREDDIT;
                break;

            case R.id.nav_random:
                if (mCurrentPage == PAGE_RANDOM) return;
                fragment = RandomFragment.newInstance();
                mCurrentPage = PAGE_RANDOM;
                break;

            case R.id.nav_uploads:
                if (mCurrentPage == PAGE_UPLOADS) return;
                fragment = UploadedPhotosFragment.createInstance();
                mCurrentPage = PAGE_UPLOADS;
                break;

            case R.id.nav_settings:
                mSavedTheme = ImgurTheme.copy(theme);
                mIsDarkTheme = app.getPreferences().getBoolean(SettingsActivity.KEY_DARK_THEME, mSavedTheme.isDarkTheme);
                startActivityForResult(SettingsActivity.createIntent(getApplicationContext()), RequestCodes.SETTINGS);
                break;

            case R.id.nav_feedback:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "kennyc.developer@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Opengur Feedback");
                share(emailIntent, R.string.send_feedback);
                break;

            case R.id.nav_topics:
                if (mCurrentPage == PAGE_TOPICS) return;
                fragment = new TopicsFragment();
                mCurrentPage = PAGE_TOPICS;
                break;

            case R.id.nav_meme:
                if (mCurrentPage == PAGE_MEME) return;
                fragment = new MemeFragment();
                mCurrentPage = PAGE_MEME;
                break;

            case R.id.nav_beta:
                new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                        .setTitle(R.string.beta_test)
                        .setMessage(R.string.beta_message)
                        .setNegativeButton(R.string.beta_no, null)
                        .setPositiveButton(R.string.beta_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/apps/testing/com.kennyc.open.imgur"));

                                if (browserIntent.resolveActivity(getPackageManager()) != null) {
                                    startActivity(browserIntent);
                                } else {
                                    Snackbar.make(mCoordinatorLayout, R.string.cant_launch_intent, Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }).show();
                break;
        }

        if (fragment != null) {
            getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
            boolean hasSpinner = fragment instanceof TopicsFragment;
            mTopicsSpinner.setVisibility(hasSpinner ? View.VISIBLE : View.GONE);
            getSupportActionBar().setDisplayShowTitleEnabled(!hasSpinner);
            mAppBar.setExpanded(true);
            app.getPreferences().edit().putInt(KEY_CURRENT_PAGE, mCurrentPage).apply();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_PAGE, mCurrentPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onFragmentStateChange(@FragmentState int state) {
        switch (state) {
            case FragmentListener.STATE_LOADING_COMPLETE:
                mUploadButton.show();
                break;

            case FragmentListener.STATE_LOADING_STARTED:
            case FragmentListener.STATE_ERROR:
                mUploadButton.hide();
                break;
        }
    }

    @Override
    public void onUpdateActionBarTitle(String title) {
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.setTitle(title);
    }

    @Override
    public void onUpdateActionBarSpinner(List<ImgurTopic> topics, @Nullable ImgurTopic currentTopic) {
        int selectedPosition = 0;

        if (currentTopic != null) {
            for (int i = 0; i < topics.size(); i++) {
                if (topics.get(i).equals(currentTopic)) {
                    selectedPosition = i;
                    break;
                }
            }
        }

        mTopicsSpinner.setAdapter(new TopicsAdapter(this, topics));
        mTopicsSpinner.setSelection(selectedPosition);
    }

    @Override
    public View getSnackbarView() {
        return mCoordinatorLayout;
    }

    @OnClick(R.id.fab)
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                startActivity(UploadActivity.createIntent(getApplicationContext()));
                break;

            case R.id.header:
                mDrawer.closeDrawers();
                startActivityForResult(ProfileActivity.createIntent(getApplicationContext(), null), RequestCodes.PROFILE);
                break;

            case R.id.badgeContainer:
                mDrawer.closeDrawers();
                startActivityForResult(NotificationActivity.createIntent(getApplicationContext()), RequestCodes.NOTIFICATIONS);
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawers();
            return;
        } else if (mNagOnExit) {
            showExitNag();
            return;
        }

        super.onBackPressed();
    }

    private void showExitNag() {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.exit)
                .setView(R.layout.exit_nag)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (dialog instanceof AlertDialog) {
                            CheckBox cb = (CheckBox) ((AlertDialog) dialog).findViewById(R.id.askAgainCB);

                            if (cb != null && cb.isChecked()) {
                                app.getPreferences().edit().putBoolean(SettingsActivity.KEY_CONFIRM_EXIT, false).apply();
                            }
                        } else {
                            LogUtil.w(TAG, "Dialog was not an alert dialog... but how?");
                        }

                        finish();
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Set the theme if coming from the settings activity
            case RequestCodes.SETTINGS:
                ImgurTheme theme = OpengurApp.getInstance(getApplicationContext()).getImgurTheme();
                mNagOnExit = app.getPreferences().getBoolean(SettingsActivity.KEY_CONFIRM_EXIT, true);

                if (mSavedTheme == null || theme != mSavedTheme || mIsDarkTheme != theme.isDarkTheme) {
                    Intent intent = getIntent();
                    overridePendingTransition(0, 0);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    finish();
                    overridePendingTransition(0, 0);
                    startActivity(intent);
                }

                Fragment fragment = getFragmentManager().findFragmentById(R.id.container);

                if (fragment instanceof BaseGridFragment) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
                break;

            case RequestCodes.PROFILE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    if (data.getBooleanExtra(ProfileActivity.KEY_LOGGED_IN, false)) {
                        app = OpengurApp.getInstance(getApplicationContext());
                        updateUserHeader(app.getUser());
                    } else if (data.getBooleanExtra(ProfileActivity.KEY_LOGGED_OUT, false)) {
                        updateUserHeader(null);
                    }
                }
                break;

            case RequestCodes.NOTIFICATIONS:
                // Notifications will clear when going into the activity
                updateNotificationBadge(0);

                if (data != null && data.getBooleanExtra(NotificationActivity.KEY_USER_NOT_LOGGED_IN, false)) {
                    updateUserHeader(null);
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void checkForImgurNag() {
        SharedPreferences pf = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if (pf.getBoolean("showImgurNag", true)) {
            new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                    .setTitle(R.string.unsupported)
                    .setMessage(R.string.unsupported_msg)
                    .setNegativeButton(R.string.close, null)
                    .setPositiveButton(R.string.install, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String imgurPackage = "com.imgur.mobile";

                            try {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + imgurPackage)));
                            } catch (android.content.ActivityNotFoundException anfe) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + imgurPackage)));
                            }
                        }
                    }).show();

            pf.edit().putBoolean("showImgurNag", false).apply();
        }
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Main_Dark : R.style.Theme_Opengur_Light_Main_Light;
    }
}
