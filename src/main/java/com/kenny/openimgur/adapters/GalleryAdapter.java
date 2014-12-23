package com.kenny.openimgur.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.kenny.openimgur.R;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;
import com.kenny.openimgur.ui.TextViewRoboto;
import com.kenny.openimgur.util.FileUtil;
import com.kenny.openimgur.util.ImageUtil;
import com.nostra13.universalimageloader.core.DisplayImageOptions;

import org.apache.commons.collections15.list.SetUniqueList;

import java.util.ArrayList;
import java.util.List;

import butterknife.InjectView;

/**
 * Created by kcampagna on 7/27/14.
 */
public class GalleryAdapter extends ImgurBaseAdapter {
    public static final int MAX_ITEMS = 200;

    public GalleryAdapter(Context context, SetUniqueList<ImgurBaseObject> objects) {
        super(context, objects, true);
    }

    @Override
    protected DisplayImageOptions getDisplayOptions() {
        return ImageUtil.getDisplayOptionsForGallery().build();
    }

    @Override
    public ArrayList<ImgurBaseObject> retainItems() {
        return new ArrayList<ImgurBaseObject>(getAllItems());
    }

    /**
     * Returns a list of objects for the viewing activity. This will return a max of 100 items to avoid memory issues.
     * 50 before and 50 after the currently selected position. If there are not 50 available before or after, it will go to as many as it can
     *
     * @param position The position of the selected items
     * @return
     */
    public ArrayList<ImgurBaseObject> getItems(int position) {
        List<ImgurBaseObject> objects;
        int size = getCount();

        if (position - MAX_ITEMS / 2 < 0) {
            objects = getAllItems().subList(0, size > MAX_ITEMS ? position + (MAX_ITEMS / 2) : size);
        } else {
            objects = getAllItems().subList(position - (MAX_ITEMS / 2), position + (MAX_ITEMS / 2) <= size ? position + (MAX_ITEMS / 2) : size);
        }

        return new ArrayList<>(objects);
    }

    @Override
    public ImgurBaseObject getItem(int position) {
        return (ImgurBaseObject) super.getItem(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GalleryHolder holder;
        ImgurBaseObject obj = getItem(position);
        String photoUrl;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.gallery_item, parent, false);
            holder = new GalleryHolder(convertView);
        } else {
            holder = (GalleryHolder) convertView.getTag();
        }

        // Get the appropriate photo to display
        if (obj instanceof ImgurPhoto) {
            ImgurPhoto photoObject = ((ImgurPhoto) obj);

            // Check if the link is a thumbed version of a large gif
            if (photoObject.hasMP4Link() && photoObject.isLinkAThumbnail() && ImgurPhoto.IMAGE_TYPE_GIF.equals(photoObject.getType())) {
                photoUrl = photoObject.getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, true, FileUtil.EXTENSION_GIF);
            } else {
                photoUrl = ((ImgurPhoto) obj).getThumbnail(ImgurPhoto.THUMBNAIL_GALLERY, false, null);
            }
        } else {
            photoUrl = ((ImgurAlbum) obj).getCoverUrl(ImgurPhoto.THUMBNAIL_GALLERY);
        }

        displayImage(holder.image, photoUrl);
        holder.score.setText((obj.getUpVotes() - obj.getDownVotes()) + " " + holder.score.getContext().getString(R.string.points));
        return convertView;
    }

    static class GalleryHolder extends ImgurViewHolder {
        @InjectView(R.id.image)
        ImageView image;
        @InjectView(R.id.score)
        TextViewRoboto score;

        public GalleryHolder(View view) {
            super(view);
        }
    }
}
