package com.kenny.openimgur.fragments;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.devspark.robototextview.widget.RobotoTextView;
import com.kenny.openimgur.R;
import com.kenny.openimgur.ViewPhotoActivity;
import com.kenny.openimgur.api.ApiClient;
import com.kenny.openimgur.api.Endpoints;
import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurListener;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.classes.OpenImgurApp;
import com.kenny.openimgur.ui.MultiStateView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.util.AsyncExecutor;
import de.greenrobot.event.util.ThrowableFailureEvent;
import pl.droidsonroids.gif.GifDrawable;

/**
 * Created by kcampagna on 7/12/14.
 */
public class ImgurViewFragment extends Fragment {
    private static final String KEY_IMGUR_OBJECT = "imgurobject";

    private MultiStateView mMultiView;

    private ListView mListView;

    private ImgurBaseObject mImgurObject;

    private PhotoAdapter mPhotoAdapter;

    public static ImgurViewFragment createInstance(@NonNull ImgurBaseObject obj) {
        ImgurViewFragment fragment = new ImgurViewFragment();
        Bundle args = new Bundle();
        args.putParcelable(KEY_IMGUR_OBJECT, obj);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_imgur_view, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMultiView = (MultiStateView) view.findViewById(R.id.multiStateView);
        mListView = (ListView) view.findViewById(R.id.list);
        mImgurObject = getArguments().getParcelable(KEY_IMGUR_OBJECT);
        mMultiView.setViewState(MultiStateView.ViewState.LOADING);

        if (mImgurObject instanceof ImgurPhoto) {
            List<ImgurPhoto> photo = new ArrayList<ImgurPhoto>(1);
            photo.add(((ImgurPhoto) mImgurObject));
            mPhotoAdapter = new PhotoAdapter(getActivity(), photo, ((OpenImgurApp) getActivity().getApplication()).getImageLoader(),
                    mImgurListener);
            createHeader();
            mListView.setAdapter(mPhotoAdapter);
            mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
        } else {
            AsyncExecutor.create().execute(new AsyncExecutor.RunnableEx() {
                @Override
                public void run() throws Exception {
                    String url = String.format(Endpoints.ALBUM.getUrl(), mImgurObject.getId());
                    ApiClient api = new ApiClient(url, ApiClient.HttpRequest.GET);
                    api.doWork(ImgurBusEvent.EventType.ALBUM_DETAILS, mImgurObject.getId());
                }
            });
        }
    }

    /**
     * Creates the header for the photo/album
     */
    private void createHeader() {
        View headerView = View.inflate(getActivity(), R.layout.image_header, null);
        RobotoTextView title = (RobotoTextView) headerView.findViewById(R.id.title);
        RobotoTextView author = (RobotoTextView) headerView.findViewById(R.id.author);


        if (!TextUtils.isEmpty(mImgurObject.getTitle()) && !mImgurObject.getTitle().equals("null")) {
            title.setText(mImgurObject.getTitle());
        }

        if (!TextUtils.isEmpty(mImgurObject.getAccount()) && !mImgurObject.getAccount().equals("null")) {
            author.setText("- " + mImgurObject.getAccount());
        } else {
            author.setText("- ?????");
        }

        mListView.addHeaderView(headerView);
    }

    private static class PhotoAdapter extends BaseAdapter {
        private List<ImgurPhoto> mPhotos;

        private LayoutInflater mInflater;

        private ImageLoader mImageLoader;

        private ImgurListener mListener;

        private final long PHOTO_SIZE_LIMIT = 1048576L;

        public PhotoAdapter(Context context, List<ImgurPhoto> photos,
                            ImageLoader loader, ImgurListener listener) {
            mInflater = LayoutInflater.from(context);
            mPhotos = photos;
            mImageLoader = loader;
            mListener = listener;
        }

        public void clear() {
            if (mPhotos != null) {
                mPhotos.clear();
            }
        }

        /**
         * Returns all photos in the adapter
         *
         * @return
         */
        public List<ImgurPhoto> getItems() {
            return mPhotos;
        }

        @Override
        public int getCount() {
            if (mPhotos != null) {
                return mPhotos.size();
            }

            return 0;
        }

        @Override
        public ImgurPhoto getItem(int position) {
            if (mPhotos != null) {
                return mPhotos.get(position);
            }

            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            PhotoViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.view_photo_item, parent, false);
                holder = new PhotoViewHolder();
                holder.play = (ImageButton) convertView.findViewById(R.id.play);
                holder.image = (ImageView) convertView.findViewById(R.id.image);
                holder.prog = (ProgressBar) convertView.findViewById(R.id.progressBar);
                holder.desc = (RobotoTextView) convertView.findViewById(R.id.desc);
                holder.title = (RobotoTextView) convertView.findViewById(R.id.title);

                holder.image.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onPhotoTap((ImageView) view);
                        }
                    }
                });

                final ProgressBar bar = holder.prog;
                final ImageView img = holder.image;
                holder.play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mListener != null) {
                            mListener.onPlayTap(bar, img, (ImageButton) view);
                        }
                    }
                });

                convertView.setTag(holder);
            } else {
                holder = (PhotoViewHolder) convertView.getTag();
            }

            ImgurPhoto photo = getItem(position);
            String url = null;

            // Load a downscaled image if any of the dimensions are greater than 1024 or they are large than 1mb
            if (photo.getWidth() > 1024 || photo.getHeight() > 1024 || photo.getSize() > PHOTO_SIZE_LIMIT) {
                url = photo.getThumbnail(ImgurPhoto.THUMBNAIL_HUGE);
            } else {
                url = photo.getLink();
            }

            holder.prog.setVisibility(View.GONE);

            if (photo.isAnimated()) {
                holder.play.setVisibility(View.VISIBLE);
            } else {
                holder.play.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(photo.getDescription()) && !photo.getDescription().equals("null")) {
                holder.desc.setText(photo.getDescription());
            } else {
                holder.desc.setText("");
            }

            if (!TextUtils.isEmpty(photo.getTitle()) && !photo.getTitle().equals("null")) {
                holder.title.setText(photo.getTitle());
            } else {
                holder.title.setText("");
            }

            mImageLoader.cancelDisplayTask(holder.image);
            mImageLoader.displayImage(url, holder.image);

            return convertView;
        }

        private static class PhotoViewHolder {
            ImageView image;

            ImageButton play;

            ProgressBar prog;

            RobotoTextView desc, title;
        }

    }

    /**
     * Plays the gif file from the list item
     *
     * @param file  Gif file
     * @param image The ImageView where it is to be displayed
     * @param prog  The ProgressBar that is in a visible state
     * @param play  The ImageButton that is in a visible state
     */
    private void playGif(File file, ImageView image, ProgressBar prog, ImageButton play) {
        if (file != null && file.exists()) {
            try {
                GifDrawable drawable = new GifDrawable(file);
                image.setImageDrawable(drawable);
                prog.setVisibility(View.GONE);
            } catch (IOException e) {
                Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                prog.setVisibility(View.GONE);
                play.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (mPhotoAdapter != null) {
            mPhotoAdapter.clear();
            mPhotoAdapter = null;
        }

        mListView = null;
        mMultiView = null;
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onEventAsync(@NonNull ImgurBusEvent event) {
        if (event.eventType == ImgurBusEvent.EventType.ALBUM_DETAILS &&
                mImgurObject.getId().equals(event.id) && mPhotoAdapter == null) {
            try {
                int statusCode = event.json.getInt(ApiClient.KEY_STATUS);

                if (statusCode == ApiClient.STATUS_OK) {
                    JSONArray arr = event.json.getJSONArray(ApiClient.KEY_DATA);

                    for (int i = 0; i < arr.length(); i++) {
                        ImgurPhoto photo = new ImgurPhoto(arr.getJSONObject(i));
                        ((ImgurAlbum) mImgurObject).addPhotoToAlbum(photo);
                    }
                }

                onAlbumLoadComplete(statusCode);
            } catch (JSONException e) {
                e.printStackTrace();
                onAlbumLoadComplete(ApiClient.STATUS_JSON_EXCEPTION);
            }
        }
    }

    /**
     * Event Method that is fired if EventBus throws an error
     *
     * @param event
     */
    public void onEventAsync(ThrowableFailureEvent event) {
        Throwable e = event.getThrowable();
        e.printStackTrace();

        if (e instanceof JSONException) {
            onAlbumLoadComplete(ApiClient.STATUS_JSON_EXCEPTION);
        } else if (e instanceof IOException) {
            onAlbumLoadComplete(ApiClient.STATUS_IO_EXCEPTION);
        }
    }

    private ImgurListener mImgurListener = new ImgurListener() {
        @Override
        public void onPhotoTap(ImageView image) {
            int position = mListView.getPositionForView(image) - mListView.getHeaderViewsCount();
            startActivity(ViewPhotoActivity.createIntent(getActivity(), mPhotoAdapter.getItem(position)));
        }

        @Override
        public void onPlayTap(final ProgressBar prog, final ImageView image, final ImageButton play) {
            final ImageLoader loader = OpenImgurApp.getInstance().getImageLoader();
            int position = mListView.getPositionForView(image) - mListView.getHeaderViewsCount();
            prog.setVisibility(View.VISIBLE);
            play.setVisibility(View.GONE);
            final ImgurPhoto photo = mPhotoAdapter.getItem(position);
            File file = DiskCacheUtils.findInCache(photo.getLink(),
                    loader.getDiskCache());

            // If the gif is not in the cache, we will load it from the network and display it
            if (file != null) {
                playGif(file, image, prog, play);
            } else {
                // Cancel the image from loading
                loader.cancelDisplayTask(image);
                loader.loadImage(photo.getLink(), new ImageLoadingListener() {
                    @Override
                    public void onLoadingStarted(String s, View view) {

                    }

                    @Override
                    public void onLoadingFailed(String s, View view, FailReason failReason) {
                        Toast.makeText(getActivity(), R.string.loading_image_error, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                        File file = DiskCacheUtils.findInCache(photo.getLink(),
                                loader.getDiskCache());
                        playGif(file, image, prog, play);
                    }

                    @Override
                    public void onLoadingCancelled(String s, View view) {

                    }
                });
            }
        }

        @Override
        public void onLinkTap(TextView textView, String url) {
            //NOOP
        }
    };

    /**
     * Configures the List once the Api responds
     *
     * @param statusCode
     */
    private void onAlbumLoadComplete(final int statusCode) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (statusCode) {
                    case ApiClient.STATUS_OK:
                        mPhotoAdapter = new PhotoAdapter(getActivity(), ((ImgurAlbum) mImgurObject).getAlbumPhotos(),
                                ((OpenImgurApp) getActivity().getApplication()).getImageLoader(), mImgurListener);

                        createHeader();
                        mListView.setAdapter(mPhotoAdapter);
                        mMultiView.setViewState(MultiStateView.ViewState.CONTENT);
                        break;
                }
            }
        });
    }
}
