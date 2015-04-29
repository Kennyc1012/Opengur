package com.kenny.openimgur.fragments;

import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.openimgur.R;
import com.kenny.openimgur.activities.ViewActivity;
import com.kenny.openimgur.adapters.GalleryAdapter;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.ui.MultiStateView;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 1/27/15.
 */
public class RandomFragment extends BaseGridFragment {

    public static RandomFragment createInstance() {
        return new RandomFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mListener != null) mListener.onUpdateActionBarTitle(getString(R.string.random));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.random, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void saveFilterSettings() {
        // NOOP
    }

    @Override
    public ImgurBusEvent.EventType getEventType() {
        return ImgurBusEvent.EventType.RANDOM_GALLERY;
    }

    @Override
    protected void fetchGallery() {
        String url = String.format(Endpoints.RANDOM.getUrl(), mCurrentPage);
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

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            mRefreshLayout.setRefreshing(false);
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> gallery = (List<ImgurBaseObject>) msg.obj;

                    if (getAdapter() == null) {
                        setUpGridTop();
                        setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(gallery)));
                    } else {
                        getAdapter().addItems(gallery);
                    }

                    mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);

                    // Due to MultiStateView setting the views visibility to GONE, the list will not reset to the top
                    // If they change the filter or refresh
                    if (mCurrentPage == 0) {
                        if (mListener != null) mListener.onLoadingComplete();

                        mMultiStateView.post(new Runnable() {
                            @Override
                            public void run() {
                                if (mGrid != null) mGrid.setSelection(0);
                            }
                        });
                    }
                    break;

                case MESSAGE_ACTION_FAILED:
                    if (getAdapter() == null || getAdapter().isEmpty()) {
                        if (mListener != null) {
                            mListener.onError((Integer) msg.obj);
                        }

                        mMultiStateView.setErrorText(R.id.errorMessage, (Integer) msg.obj);
                        mMultiStateView.setViewState(MultiStateView.ViewState.ERROR);
                    }
                    break;

                case MESSAGE_EMPTY_RESULT:
                default:
                    mIsLoading = false;
                    super.handleMessage(msg);
                    break;
            }

            mIsLoading = false;
        }
    };
}
