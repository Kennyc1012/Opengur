package com.kenny.openimgur.api;

import com.google.gson.Gson;
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
    private final Gson gson = new GsonBuilder().create();

    @Override
    public ImgurBaseObject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        boolean isAlbum = object.has("images_count") && object.get("images_count").getAsInt() > 0;
        ImgurBaseObject obj = gson.fromJson(json, isAlbum ? ImgurAlbum.class : ImgurPhoto.class);

        // Need to manually check if the up/down votes are set to null as GSON will initialize it to 0
        if (object.has("in_gallery")) {
            obj.setIsListed(object.get("in_gallery").getAsBoolean());
        } else if (object.has("ups") && object.has("downs")) {
            boolean hasUpVotes = !object.get("ups").isJsonNull();
            boolean hasDownVotes = !object.get("downs").isJsonNull();
            obj.setIsListed(hasUpVotes && hasDownVotes);
        } else {
            obj.setIsListed(false);
        }

        obj.toHttps();
        return obj;
    }
}