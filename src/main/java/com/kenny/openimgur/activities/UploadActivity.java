package com.kenny.openimgur.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.kenny.openimgur.R;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.responses.TopicResponse;
import com.kenny.openimgur.classes.ImgurTopic;
import com.kenny.openimgur.fragments.UploadFragment;
import com.kenny.openimgur.ui.ViewPager;
import com.kenny.openimgur.util.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by Kenny-PC on 6/20/2015.
 */
public class UploadActivity extends BaseActivity {
    private static final String KEY_PASSED_FILE = "passed_file";

    private static final String PREF_NOTIFY_NO_USER = "notify_no_user";

    @Bind(R.id.pager)
    ViewPager mPager;

    public static Intent createIntent(Context context) {
        return new Intent(context, UploadActivity.class);
    }

    public static Intent createIntent(Context context, @NonNull File file) {
        return createIntent(context).putExtra(KEY_PASSED_FILE, file.getAbsolutePath());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        getSupportActionBar().setTitle(R.string.upload);
        checkForTopics();
        checkForNag();
        checkIntent(getIntent());
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String path = null;
            boolean isLink = false;
            ArrayList<Uri> photoUris = null;

            if (intent.hasExtra(KEY_PASSED_FILE)) {
                LogUtil.v(TAG, "Received file from intent");
                path = intent.getStringExtra(KEY_PASSED_FILE);
            } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
                String type = intent.getType();
                LogUtil.v(TAG, "Received an image via Share intent, type " + type);

                if ("text/plain".equals(type)) {
                    path = intent.getStringExtra(Intent.EXTRA_TEXT);
                    isLink = true;
                } else {
                    Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                    photoUris = new ArrayList<>(1);
                    photoUris.add(uri);
                }
            } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                photoUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

                if (photoUris != null && !photoUris.isEmpty()) {
                    LogUtil.v(TAG, "Received " + photoUris.size() + " images via Share intent");
                }
            }

            mPager.setAdapter(new UploadPagerAdapter(getFragmentManager(), path, isLink, photoUris));
        } else {
            mPager.setAdapter(new UploadPagerAdapter(getFragmentManager()));
        }
    }

    private boolean showCancelDialog() {
        new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                .setTitle(R.string.upload_cancel_title)
                .setMessage(R.string.upload_cancel_msg)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();

        return true;
    }

    /**
     * Checks if we have cached topics to display for the info fragment
     */
    private void checkForTopics() {
        List<ImgurTopic> topics = app.getSql().getTopics();

        if (topics == null || topics.isEmpty()) {
            LogUtil.v(TAG, "No topics found, fetching");
            ApiClient.getService().getDefaultTopics().enqueue(new Callback<TopicResponse>() {
                @Override
                public void onResponse(Response<TopicResponse> response, Retrofit retrofit) {
                    if (response != null && response.body() != null) app.getSql().addTopics(response.body().data);
                }

                @Override
                public void onFailure(Throwable t) {
                    LogUtil.e(TAG, "Failed to receive topics", t);
                }
            });
        } else {
            LogUtil.v(TAG, "Topics in database");
        }
    }

    /**
     * Checks if the user is not logged in and if we should nag about it
     */
    private boolean checkForNag() {
        boolean nag = app.getPreferences().getBoolean(PREF_NOTIFY_NO_USER, true);

        if (nag && user == null) {
            View nagView = LayoutInflater.from(this).inflate(R.layout.no_user_nag, null);
            final CheckBox cb = (CheckBox) nagView.findViewById(R.id.dontNotify);

            new AlertDialog.Builder(this, theme.getAlertDialogTheme())
                    .setTitle(R.string.not_logged_in)
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (cb.isChecked()) {
                                app.getPreferences().edit().putBoolean(PREF_NOTIFY_NO_USER, false).apply();
                            }
                        }
                    })
                    .setPositiveButton(R.string.yes, null)
                    .setView(nagView)
                    .show();

            return true;
        }

        return false;
    }

    @Override
    protected int getStyleRes() {
        return theme.isDarkTheme ? R.style.Theme_Opengur_Dark_Upload : R.style.Theme_Opengur_Light_DarkActionBar_Upload;
    }

    private static class UploadPagerAdapter extends FragmentPagerAdapter {
        Bundle mUploadArgs = null;

        public UploadPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        public UploadPagerAdapter(FragmentManager manager, String path, boolean isLink, ArrayList<Uri> photoUris) {
            this(manager);
            mUploadArgs = UploadFragment.createArguments(path, isLink, photoUris);
        }


        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return UploadFragment.newInstance(mUploadArgs);

                case 1:
                    // TODO
                    return null;

                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    }
}
