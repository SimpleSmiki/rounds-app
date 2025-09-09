package simple.smiki.imageloadlib

import android.graphics.Bitmap
import android.util.LruCache

object MemoryCacheManager {

    private val maxCacheSize: Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

    private val lruCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(maxCacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            // The cache size is calculated in KB
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Retrieves a bitmap from the memory cache.
     * @return The Bitmap if it exists, otherwise null.
     */
    fun get(url: String): Bitmap? {
        return lruCache.get(url)
    }

    /**
     * Adds a bitmap to the memory cache.
     */
    fun put(url: String, bitmap: Bitmap) {
        if (get(url) == null) {
            lruCache.put(url, bitmap)
        }
    }

    /**
     * Clears all bitmaps from the memory cache.
     */
    fun clear() {
        lruCache.evictAll()
    }
}