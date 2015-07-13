package com.kenny.openimgur.api.responses;

import android.support.annotation.Nullable;

import com.kenny.openimgur.classes.ImgurPhoto;

/**
 * Created by kcampagna on 7/12/15.
 */
public class PhotoResponse extends BaseResponse {
    @Nullable
    public ImgurPhoto data;
}
