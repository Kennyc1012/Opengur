package com.kenny.openimgur.api.responses;

import android.support.annotation.Nullable;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.kenny.openimgur.classes.ImgurConvo;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by kcampagna on 7/12/15.
 */
public class ConvoResponse extends BaseResponse implements JsonDeserializer<ConvoResponse> {
    @Nullable
    public List<ImgurConvo> data;

    @Override
    public ConvoResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // An empty convo list will come back as false instead of an empty array
        if (!jsonObject.get("data").isJsonPrimitive()) {
            return new GsonBuilder().create().fromJson(json, ConvoResponse.class);
        } else {
            ConvoResponse response = new ConvoResponse();
            response.success = jsonObject.get("success").getAsBoolean();
            response.status = jsonObject.get("status").getAsInt();
            return response;
        }
    }
}
