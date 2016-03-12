package com.kenny.openimgur.api.responses;

import com.kenny.openimgur.classes.ImgurComment;

import java.util.List;

/**
 * Created by kcampagna on 7/11/15.
 */
public class CommentResponse extends BaseResponse {
    public List<ImgurComment> data;

    public boolean hasComments() {
        return data != null && !data.isEmpty();
    }
}
