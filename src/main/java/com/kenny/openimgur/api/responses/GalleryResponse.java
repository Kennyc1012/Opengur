package com.kenny.openimgur.api.responses;

import android.support.annotation.NonNull;

import com.kenny.openimgur.classes.ImgurBaseObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/11/15.
 */
public class GalleryResponse extends BaseResponse {
    @NonNull
    public List<ImgurBaseObject> data = new ArrayList<>();
}