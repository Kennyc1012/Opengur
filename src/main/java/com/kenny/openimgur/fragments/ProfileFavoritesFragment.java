package com.kenny.openimgur.fragments;

import com.kenny.openimgur.api.ImgurBusEvent;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurHandler;

import java.util.ArrayList;

/**
 * Created by kcampagna on 12/20/14.
 */
public class ProfileFavoritesFragment extends BaseGridFragment {
    @Override
    protected void saveFilterSettings() {
        // NOOP
    }

    @Override
    public ImgurBusEvent.EventType getEventType() {
        return ImgurBusEvent.EventType.ACCOUNT_GALLERY_FAVORITES;
    }

    @Override
    protected void fetchGallery() {

    }

    @Override
    protected ImgurHandler getHandler() {
        return null;
    }

    @Override
    protected void onItemSelected(int position, ArrayList<ImgurBaseObject> items) {

    }
}
