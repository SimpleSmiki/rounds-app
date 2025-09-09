package simple.smiki.imageloadlib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {
    private lateinit var diskCacheManager: DiskCacheManager

    fun init(context: Context) {
        diskCacheManager = DiskCacheManager(context)
    }

    /**
     * Loads an image from a URL into the provided ImageView,
     * using a cache and a placeholder.
     */
    fun loadImage(url: String, placeholder: Drawable?, imageView: ImageView) {
        // Step 1: Set the placeholder image immediately
        imageView.setImageDrawable(placeholder)

        // Step 2: Check the memory cache for the image
        val memCachedBitmap = MemoryCacheManager.get(url)
        if (memCachedBitmap != null) {
            imageView.setImageBitmap(memCachedBitmap)
            return
        }

        // Step 3: Check the disk cache for the image
        val diskCachedBitmap = diskCacheManager.get(url)
        if (diskCachedBitmap != null) {
            MemoryCacheManager.put(url, diskCachedBitmap)
            imageView.setImageBitmap(diskCachedBitmap)
            return
        }

        // Step 4: If not in any cache, start a background task to download the image
        CoroutineScope(Dispatchers.IO).launch {
            val downloadedBitmap = downloadImage(url)
            if (downloadedBitmap != null) {
                // Save the downloaded bitmap to both caches
                diskCacheManager.put(url, downloadedBitmap)
                MemoryCacheManager.put(url, downloadedBitmap)

                // Switch to the main thread to update the UI
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(downloadedBitmap)
                }
            }
        }
    }

    private fun downloadImage(url: String): Bitmap? {
        var connection: HttpURLConnection? = null
        return try {
            val urlConnection = URL(url)
            connection = urlConnection.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            val inputStream = connection.inputStream
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Clears all cached images from memory and disk.
     */
    fun invalidateCache() {
        MemoryCacheManager.clear()
        diskCacheManager.clear()
    }
}