package com.kenny.openimgur.classes;

import android.support.v7.graphics.Palette;
import android.util.LruCache;

/**
 * Created by kcampagna on 10/25/14.
 */
public class PaletteCache {
    public static PaletteCache mInstance;

    /**
     * Returns a singleton for the Palette Cache
     *
     * @return
     */
    public static PaletteCache getInstance() {
        if (mInstance == null) {
            mInstance = new PaletteCache();
        }

        return mInstance;
    }

    private LruCache<String, Palette> mCache;

    public PaletteCache() {
        int cacheSize = ((int) (Runtime.getRuntime().maxMemory() / 1024)) / 8;
        mCache = new LruCache<String, Palette>(cacheSize);
    }

    /**
     * Clears all the palette cache
     */
    public void clearCache() {
        mCache.evictAll();
    }

    /**
     * Adds a palette to the cache
     *
     * @param key
     * @param palette
     */
    public synchronized void addPalette(String key, Palette palette) {
        mCache.put(key, palette);
    }

    /**
     * Removes a palette from the cache
     *
     * @param key The key of the Palette
     * @return The removed object, or null if not found in cache
     */
    public synchronized Palette removePalette(String key) {
        return mCache.remove(key);
    }

    /**
     * Returns the palette from the cache
     *
     * @param key The key of the Palette
     * @return
     */
    public synchronized Palette getPalette(String key) {
        return mCache.get(key);
    }
}
