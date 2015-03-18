package com.kenny.openimgur.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewPhotoActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.snackbar.SnackBar;

import org.apache.commons.collections15.list.SetUniqueList;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 12/27/14.
 */
public class ProfileUploadsFragment extends BaseGridFragment implements AdapterView.OnItemLongClickListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (user == null) {
            throw new IllegalStateException("User is not logged in");
        }
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
        return ImgurBusEvent.EventType.ACCOUNT_UPLOADS;
    }

    @Override
    protected void fetchGallery() {
        String url = String.format(Endpoints.ACCOUNT_IMAGES.getUrl(), user.getUsername(), mCurrentPage);
        makeRequest(url);
    }

    @Override
    protected ImgurHandler getHandler() {
        return mHandler;
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        ImgurBaseObject obj = items.get(position);

        if (obj instanceof ImgurPhoto) {
            startActivity(ViewPhotoActivity.createIntent(getActivity(), (ImgurPhoto) obj));
        } else {
            startActivity(ViewPhotoActivity.createIntent(getActivity(), obj.getLink(), obj.getLink().endsWith("mp4")));
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        int headerSize = mGrid.getNumColumns() * mGrid.getHeaderViewCount();
        int adapterPosition = position - headerSize;

        if (adapterPosition >= 0) {
            final ImgurBaseObject photo = getAdapter().getItem(adapterPosition);

            new MaterialDialog.Builder(getActivity())
                    .items(R.array.uploaded_photos_options)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog materialDialog, View view, int which, CharSequence charSequence) {
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
                                    new MaterialDialog.Builder(getActivity())
                                            .content(R.string.profile_delete_image)
                                            .negativeText(R.string.cancel)
                                            .positiveText(R.string.yes)
                                            .callback(new MaterialDialog.ButtonCallback() {
                                                @Override
                                                public void onPositive(MaterialDialog dialog) {
                                                    String url = String.format(Endpoints.IMAGE_DELETE.getUrl(), photo.getDeleteHash());
                                                    new ApiClient(url, ApiClient.HttpRequest.DELETE).doWork(ImgurBusEvent.EventType.IMAGE_DELETE, photo.getId(), null);
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
    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.IMAGE_DELETE) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);
                boolean success = event.json.getBoolean(ApiClient.KEY_DATA);

                if (statusCode == ApiClient.STATUS_OK && success) {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_IMAGE_DELETED, event.id);
                } else {
                    mHandler.sendMessage(ImgurHandler.MESSAGE_IMAGE_DELETED, false);
                }
            } catch (JSONException e) {
                LogUtil.e(TAG, "Error parsing JSON", e);
                getHandler().sendMessage(ImgurHandler.MESSAGE_ACTION_FAILED, ApiClient.getErrorCodeStringResource(ApiClient.STATUS_JSON_EXCEPTION));
            }
        } else {
            super.onEventAsync(event);
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
                        String errorMessage = getString(R.string.profile_no_uploads);
                        mMultiStateView.setErrorText(R.id.errorMessage, errorMessage);
                        mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
                    }
                    break;

                case MESSAGE_IMAGE_DELETED:
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

                        SnackBar.show(getActivity(), R.string.profile_delete_success);
                    } else {
                        mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);
                        SnackBar.show(getActivity(), R.string.profile_delete_failure);
                    }
                    break;
            }

            mIsLoading = false;
            super.handleMessage(msg);
        }
    };

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
}
