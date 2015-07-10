package com.kenny.openimgur.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurUser;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;

import org.apache.commons.collections15.list.SetUniqueList;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kenny-PC on 7/4/2015.
 */
public class ProfileAlbumsFragment extends BaseGridFragment implements AdapterView.OnItemLongClickListener {
    private static final String KEY_USER = "user";

    private ImgurUser mSelectedUser;

    public static ProfileAlbumsFragment createInstance(@NonNull ImgurUser user) {
        ProfileAlbumsFragment fragment = new ProfileAlbumsFragment();
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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mGrid.setOnItemLongClickListener(this);
    }

    @Override
    protected void saveFilterSettings() {
        // NOOP
    }

    @Override
    public ImgurBusEvent.EventType getEventType() {
        return ImgurBusEvent.EventType.USER_ALBUMS;
    }

    @Override
    protected void fetchGallery() {
        String url = String.format(Endpoints.USER_ALBUMS.getUrl(), mSelectedUser.getUsername(), mCurrentPage);
        makeRequest(url);
    }

    @Override
    protected ImgurHandler getHandler() {
        return mHandler;
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        startActivity(ViewActivity.createIntent(getActivity(), items, position));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;

        if (adapterPosition >= 0) {
            final ImgurBaseObject photo = getAdapter().getItem(adapterPosition);

            new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                    .setItems(R.array.uploaded_photos_options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 0. Share 1. Copy Link 2. Delete

                            switch (which) {
                                case 0:
                                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                    shareIntent.setType("text/plain");
                                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share));
                                    shareIntent.putExtra(Intent.EXTRA_TEXT, photo.getLink());

                                    if (shareIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                                        startActivity(Intent.createChooser(shareIntent, getString(R.string.send_feedback)));
                                    } else {
                                        SnackBar.show(getActivity(), R.string.cant_launch_intent);
                                    }
                                    break;

                                case 1:
                                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboard.setPrimaryClip(ClipData.newPlainText("link", photo.getLink()));
                                    break;

                                case 2:
                                    new AlertDialog.Builder(getActivity(), theme.getAlertDialogTheme())
                                            .setMessage(R.string.profile_delete_album)
                                            .setNegativeButton(R.string.cancel, null)
                                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String url = String.format(Endpoints.ALBUM_DELETE.getUrl(), photo.getDeleteHash());
                                                    new ApiClient(url, ApiClient.HttpRequest.DELETE).doWork(ImgurBusEvent.EventType.ALBUM_DELETE, photo.getId(), null);
                                                    mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
                                                }
                                            }).show();
                                    break;
                            }
                        }
                    }).show();
            return true;
        }

        return false;
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
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        // Need to override the event method because the API doesn't respond saying these are Albums, so we need to force it, mostly a copy and paste
        if (event.eventType == getEventType() && event.id.equals(mRequestId)) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                List<ImgurBaseObject> objects;

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);

                    if (arr == null || arr.length() <= 0) {
                        mHasMore = false;
                        LogUtil.v(TAG, "Did not receive any items in the json array");
                        getHandler().sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                        return;
                    }

                    objects = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        ImgurAlbum imgurObject = new ImgurAlbum(arr.getJSONObject(i));

                        if (allowNSFW() || !imgurObject.isNSFW()) {
                            objects.add(imgurObject);
                        }
                    }

                    if (objects.size() <= 0) {
                        mHasMore = false;
                        getHandler().sendEmptyMessage(ImgurHandler.MESSAGE_EMPTY_RESULT);
                    } else {
                        getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_COMPLETE, objects);
                    }
                } else {
                    getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(statusCode));
                }

            } catch (JSONException e) {
                LogUtil.e(TAG, "Error parsing JSON", e);
                getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
            }
        } else if (event.eventType == ImgurBusEvent.EventType.ALBUM_DELETE) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                boolean success = event.json.getBoolean(ApiClient.KEY_DATA);

                if (statusCode == ApiClient.STATUS_OK && success) {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ALBUM_DELETED, event.id);
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_ALBUM_DELETED, false);
                }
            } catch (JSONException e) {
                LogUtil.e(TAG, "Error parsing JSON", e);
                getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
            }
        }
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            mRefreshLayout.setRefreshing(false);
            switch (msg.what) {
                case ImgurHandler.MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> items = (List<ImgurBaseObject>) msg.obj;
                    GalleryAdapter adapter = getAdapter();

                    if (adapter == null) {
                        setUpGridTop();
                        setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(items)));
                    } else {
                        adapter.addItems(items);
                    }

                    // The endpoint returns all favorites for a self user, no need for loading on scroll
                    if (mSelectedUser.isSelf(app)) mHasMore = false;
                    mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
                    break;

                case ImgurHandler.MESSAGE_ACTION_FAILED:
                    if (getAdapter() == null || getAdapter().isEmpty()) {
                        mMultiStateView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                        mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
                    }
                    break;

                case MESSAGE_EMPTY_RESULT:
                    if (getAdapter() == null || getAdapter().isEmpty()) {
                        String errorMessage = getString(R.string.profile_no_albums, mSelectedUser.getUsername());
                        mMultiStateView.setErrorText(R.id.errorMessage, errorMessage);
                        mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
                    }
                    break;

                case MESSAGE_ALBUM_DELETED:
                    if (msg.obj instanceof String) {
                        GalleryAdapter gAdapter = getAdapter();

                        if (gAdapter != null) {
                            gAdapter.removeItem((String) msg.obj);

                            if (gAdapter.isEmpty()) {
                                mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
                            } else {
                                mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
                            }
                        }

                        SnackBar.show(getActivity(), R.string.profile_delete_success_album);
                    } else {
                        mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
                        SnackBar.show(getActivity(), R.string.profile_delete_failure_album);
                    }
                    break;
            }

            mIsLoading = false;
            super.handleMessage(msg);
        }
    };
}
