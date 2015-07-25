package com.kenny.openimgur.api;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kenny.openimgur.classes.ImgurAlbum;
import com.kenny.openimgur.classes.ImgurBaseObject;
import com.kenny.openimgur.classes.ImgurPhoto;

import java.lang.reflect.Type;

/**
 * Created by kcampagna on 7/11/15.
 */
public class ImgurSerializer implements JsonDeserializer<ImgurBaseObject> {
    @Override
    public ImgurBaseObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        boolean isAlbum = object.has("images_count") && object.get("images_count").getAsInt() > 0;
        return new GsonBuilder().create().fromJson(json, isAlbum ? ImgurAlbum.class : ImgurPhoto.class);
    }
}