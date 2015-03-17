package com.kenny.openimgur.fragments;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
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
import com.kenny.openimgur.classes.ImgurFilters.GallerySection;
import com.kenny.openimgur.classes.ImgurFilters.GallerySort;
import com.kenny.openimgur.classes.ImgurFilters.TimeSort;
import com.kenny.openimgur.classes.ImgurHandler;
import com.kenny.openimgur.ui.MultiStateView;
import com.kenny.openimgur.util.LogUtil;
import com.kenny.openimgur.util.ViewUtils;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 8/14/14.
 */
public class GalleryFragment extends BaseGridFragment implements GalleryFilterFragment.FilterListener {
    private static final String KEY_SECTION = "section";

    private static final String KEY_SORT = "sort";

    private static final String KEY_TOP_SORT = "galleryTopSort";

    private static final String KEY_SHOW_VIRAL = "showViral";

    private GallerySection mSection = GallerySection.HOT;

    private GallerySort mSort = GallerySort.TIME;

    private TimeSort mTimeSort = TimeSort.DAY;

    private boolean mShowViral = true;

    public static GalleryFragment createInstance() {
        return new GalleryFragment();
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
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {
        startActivity(ViewActivity.createIntent(getActivity(), items, position));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.gallery, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                refresh();
                return true;

            case R.id.filter:
                if (mListener != null) mListener.onUpdateActionBar(false);

                GalleryFilterFragment fragment = GalleryFilterFragment.createInstance(mSort, mSection, mTimeSort, mShowViral);
                fragment.setFilterListener(this);
                getFragmentManager().beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(android.R.id.content, fragment, "filter")
                        .commit();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Returns the URL based on the selected sort and section
     *
     * @return
     */
    private String getGalleryUrl() {
        if (mSort == GallerySort.HIGHEST_SCORING) {
            return String.format(Endpoints.GALLERY_TOP.getUrl(), mSection.getSection(), mSort.getSort(),
                    mTimeSort.getSort(), mCurrentPage, mShowViral);
        }

        return String.format(Endpoints.GALLERY.getUrl(), mSection.getSection(), mSort.getSort(), mCurrentPage, mShowViral);
    }

    @Override
    public void onFilterChange(GallerySection section, GallerySort sort, TimeSort timeSort, boolean showViral) {
        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().remove(fm.findFragmentByTag("filter")).commit();
        if (mListener != null) mListener.onUpdateActionBar(true);

        // Null values represent that the filter was canceled
        if (section == null || sort == null || timeSort == null ||
                (section == mSection && mSort == sort && mShowViral == showViral && timeSort == mTimeSort)) {
            return;
        }

        if (getAdapter() != null) {
            getAdapter().clear();
        }

        mSection = section;
        mSort = sort;
        mTimeSort = timeSort;
        mShowViral = showViral;
        mCurrentPage = 0;
        mIsLoading = true;
        mHasMore = true;
        mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);

        if (mListener != null) {
            mListener.onLoadingStarted();
            mListener.onUpdateActionBarTitle(getString(mSection.getResourceId()));
        }

        fetchGallery();
    }

    private ImgurHandler mHandler = new ImgurHandler() {
        @Override
        public void handleMessage(Message msg) {
            if (isRemoving() || getActivity().isFinishing()) {
                LogUtil.w(TAG, "Fragment is being removed, or activity is finishing, not delivering message");
                return;
            }
            
            switch (msg.what) {
                case MESSAGE_ACTION_COMPLETE:
                    List<ImgurBaseObject> gallery = (List<ImgurBaseObject>) msg.obj;

                    if (getAdapter() == null) {
                        mGrid.addHeaderView(ViewUtils.getHeaderViewForTranslucentStyle(getActivity(), 0));
                        setAdapter(new GalleryAdapter(getActivity(), SetUniqueList.decorate(gallery)));
                    } else {
                        getAdapter().addItems(gallery);
                    }

                    if (mListener != null) {
                        mListener.onLoadingComplete();
                    }

                    mMultiStateView.setViewState(MultiStateView.ViewState.CONTENT);

                    // Due to MultiStateView setting the views visibility to GONE, the list will not reset to the top
                    // If they change the filter or refresh
                    if (mCurrentPage == 0) {
                        mMultiStateView.post(new Runnable() {
                            @Override
                            public void run() {
                                mGrid.setSelection(0);
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
                        mMultiStateView.setErrorButtonText(R.id.errorButton, R.string.retry);
                        mMultiStateView.setErrorButtonClickListener(R.id.errorButton, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mMultiStateView.setViewState(MultiStateView.ViewState.LOADING);
                                fetchGallery();
                            }
                        });

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

    @Override
    protected void fetchGallery() {
        makeRequest(getGalleryUrl());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_SECTION, mSection.getSection());
        outState.putString(KEY_SORT, mSort.getSort());
        outState.putBoolean(KEY_SHOW_VIRAL, mShowViral);
        outState.putString(KEY_TOP_SORT, mTimeSort.getSort());
    }

    @Override
    protected void onRestoreSavedInstance(Bundle savedInstanceState) {
        super.onRestoreSavedInstance(savedInstanceState);

        if (savedInstanceState == null) {
            SharedPreferences pref = app.getPreferences();
            mSection = GallerySection.getSectionFromString(pref.getString(KEY_SECTION, null));
            mSort = GallerySort.getSortFromString(pref.getString(KEY_SORT, null));
            mTimeSort = TimeSort.getSortFromString(pref.getString(KEY_TOP_SORT, null));
            mShowViral = pref.getBoolean(KEY_SHOW_VIRAL, true);
        } else {
            mSort = GallerySort.getSortFromString(savedInstanceState.getString(KEY_SORT, GallerySort.TIME.getSort()));
            mSection = GallerySection.getSectionFromString(savedInstanceState.getString(KEY_SECTION, GallerySection.HOT.getSection()));
            mTimeSort = TimeSort.getSortFromString(savedInstanceState.getString(KEY_TOP_SORT, null));
            mShowViral = savedInstanceState.getBoolean(KEY_SHOW_VIRAL, true);
        }

        if (mListener != null)
            mListener.onUpdateActionBarTitle(getString(mSection.getResourceId()));
    }

    @Override
    protected void saveFilterSettings() {
        app.getPreferences().edit()
                .putString(KEY_SECTION, mSection.getSection())
                .putBoolean(KEY_SHOW_VIRAL, mShowViral)
                .putString(KEY_TOP_SORT, mTimeSort.getSort())
                .putString(KEY_SORT, mSort.getSort()).apply();
    }

    @Override
    public ImgurBusEvent.EventType getEventType() {
        return ImgurBusEvent.EventType.GALLERY;
    }

    @Override
    protected ImgurHandler getHandler() {
        return mHandler;
    }
}
