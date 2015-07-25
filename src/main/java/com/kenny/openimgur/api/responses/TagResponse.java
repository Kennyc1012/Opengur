package com.kenny.openimgur.api.responses;

import android.support.annotation.NonNull;

import com.kenny.openimgur.classes.ImgurTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kcampagna on 7/12/15.
 */
public class TagResponse extends BaseResponse {
    public Data data;

    // Tags are returned in an array inside a data object,Boooo
    public static class Data {
        @NonNull
        public List<ImgurTag> tags = new ArrayList<>();
    }
}
