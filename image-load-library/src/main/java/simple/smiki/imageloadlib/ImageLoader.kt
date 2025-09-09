package simple.smiki.imageloadlib

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView

object ImageLoader {
    private lateinit var diskCacheManager: DiskCacheManager
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        diskCacheManager = DiskCacheManager(context)
    }

    /**
     * Loads an image from a URL into the provided ImageView,
     * using a cache and a placeholder.
     */
    fun loadImage(url: String, placeholder: Drawable, imageView: ImageView) {
        // Step 1: Set the placeholder image immediately
        imageView.setImageDrawable(placeholder)

        // Step 2: Check the memory cache for the image
        val memCachedBitmap = MemoryCacheManager.get(url)
        if (memCachedBitmap != null) {
            // Found in memory, load it instantly and return
            imageView.setImageBitmap(memCachedBitmap)
            return
        }

        // Step 3: Check the disk cache for the image
        val diskCachedBitmap = diskCacheManager.get(url)
        if (diskCachedBitmap != null) {
            // Found a valid image on disk, load it and save it to memory cache for future use
            MemoryCacheManager.put(url, diskCachedBitmap)
            imageView.setImageBitmap(diskCachedBitmap)
            return
        }

        // Step 4: If not in any cache, start a background task to download the image
        // (Implementation for this will be added in a future step)
        // Once downloaded, you'll put the bitmap into both caches and update the ImageView.
    }

    /**
     * Clears all cached images from memory and disk.
     */
    fun invalidateCache() {
        MemoryCacheManager.clear()
        diskCacheManager.clear()
    }
}