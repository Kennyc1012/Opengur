package com.kenny.openimgur.util;

import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.LruCache;

import com.kenny.openimgur.classes.ImgurBaseObject;

import java.util.ArrayList;
import java.util.List;

public class StateSaver {
    private static final String TAG = StateSaver.class.getSimpleName();

    private LruCache<String, ArrayList<? extends Parcelable>> savedStateCache = null;

    private static final StateSaver instance = new StateSaver();

    public static StateSaver instance() {
        return instance;
    }

    StateSaver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LogUtil.v(TAG, "Build is Targeting N or higher, creating cache");
            savedStateCache = new LruCache<>(1024 * 5);
        } else {
            LogUtil.v(TAG, "Build targeting lower than N, no cache needed");
        }
    }

    public void onSaveState(@NonNull Bundle bundle, @NonNull String key, ArrayList<? extends Parcelable> data) {
        if (savedStateCache != null) {
            LogUtil.v(TAG, "Saving data to cache");
            savedStateCache.put(key, data);
        } else {
            LogUtil.v(TAG, "Saving data to Bundle");
            bundle.putParcelableArrayList(key, data);
        }
    }

    @Nullable
    public <T extends Parcelable> ArrayList<T> getData(@Nullable Bundle bundle, @NonNull String key) {
        if (savedStateCache != null) {
            try {
                LogUtil.v(TAG, "Fetching item from cache, size: " + savedStateCache.size());
                ArrayList<T> data = (ArrayList<T>) savedStateCache.remove(key);
                LogUtil.v(TAG, "Size after fetch: " + savedStateCache.size());
                return data;
            } catch (ClassCastException ex) {
                LogUtil.w(TAG, "Error fetching list from cache", ex);
                return null;
            }
        }

        if (bundle != null) return bundle.getParcelableArrayList(key);
        return null;
    }

    public void remove(@NonNull String key, List<ImgurBaseObject> objects) {
        if (savedStateCache != null && objects != null && !objects.isEmpty()) {
            LogUtil.v(TAG, "Removing items from cache, current size: " + savedStateCache.size());

            for (ImgurBaseObject obj : objects) {
                savedStateCache.remove(key + "." + obj.hashCode());
            }

            LogUtil.v(TAG, "Cache size now: " + savedStateCache.size());
        }
    }

    public boolean contains(@NonNull String key, @Nullable Bundle bundle) {
        if (savedStateCache != null) {
            return savedStateCache.get(key) != null;
        } else if (bundle != null) {
            bundle.containsKey(key);
        }

        return false;
    }
}
