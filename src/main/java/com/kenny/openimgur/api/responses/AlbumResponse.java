package com.kenny.openimgur.api.responses;

import android.support.annotation.NonNull;

import com.kenny.openimgur.classes.ImgurPhoto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/12/15.
 */
public class AlbumResponse extends BaseResponse {
    @NonNull
    public List<ImgurPhoto> data = new ArrayList<>();
}
