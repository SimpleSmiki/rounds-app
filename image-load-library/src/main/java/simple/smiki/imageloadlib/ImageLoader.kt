package simple.smiki.imageloadlib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageLoader {
    private const val TAG = "ImageLoader"

    private lateinit var diskCacheManager: DiskCacheManager

    fun init(context: Context) {
        diskCacheManager = DiskCacheManager(context)
    }

    /**
     * Loads an image from a URL into the provided ImageView,
     * using a cache and a placeholder.
     */
    fun loadImage(url: String, placeholder: Drawable, imageView: ImageView) {
        imageView.setImageDrawable(placeholder)

        // Check memory cache first
        val memCachedBitmap = MemoryCacheManager.get(url)
        if (memCachedBitmap != null) {
            imageView.setImageBitmap(memCachedBitmap)
            return
        }

        // Check disk cache next
        val diskCachedBitmap = diskCacheManager.get(url)
        if (diskCachedBitmap != null) {
            MemoryCacheManager.put(url, diskCachedBitmap)
            imageView.setImageBitmap(diskCachedBitmap)
            return
        }

        // Use a coroutine to download and set the image
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wait for the view to be laid out to get dimensions
                val targetWidth = imageView.width
                val targetHeight = imageView.height
                if (targetWidth == 0 || targetHeight == 0) {
                    Log.w(TAG, "ImageView dimensions are 0. Skipping download for $url")
                    return@launch
                }

                val downloadedBitmap = downloadImage(url, targetWidth, targetHeight)
                if (downloadedBitmap != null) {
                    diskCacheManager.put(url, downloadedBitmap)
                    MemoryCacheManager.put(url, downloadedBitmap)
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(downloadedBitmap)
                    }
                } else {
                    Log.e(TAG, "Failed to download or decode bitmap for $url")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading image from $url", e)
                withContext(Dispatchers.Main) {
                    // Optionally set an error drawable here
                }
            }
        }
    }

    private fun downloadImage(url: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        var connection: HttpURLConnection? = null
        try {
            val urlConnection = URL(url)
            connection = urlConnection.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val inputStream = connection.inputStream
            return decodeSampledBitmapFromStream(inputStream, targetWidth, targetHeight)
        } catch (e: IOException) {
            Log.e(TAG, "Network error during download: ${e.message}")
            return null
        } catch (e: Exception) {
            Log.e(TAG, "General error during download: ${e.message}")
            return null
        } finally {
            connection?.disconnect()
        }
    }

    private fun decodeSampledBitmapFromStream(
        inputStream: InputStream,
        targetWidth: Int,
        targetHeight: Int
    ): Bitmap? {
        // First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        val bufferedStream = inputStream.buffered()
        bufferedStream.mark(inputStream.available())
        BitmapFactory.decodeStream(bufferedStream, null, options)

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        bufferedStream.reset()
        return BitmapFactory.decodeStream(bufferedStream, null, options)
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > targetHeight || width > targetWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Clears all cached images from memory and disk.
     */
    fun invalidateCache() {
        MemoryCacheManager.clear()
        diskCacheManager.clear()
    }
}