package simple.smiki.imageloadlib

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import simple.smiki.imageloadlib.caches.Cache
import simple.smiki.imageloadlib.caches.DiskCacheManager
import simple.smiki.imageloadlib.caches.MemoryCacheManager
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "ImageLoader"

class ImageLoader private constructor(
    private val memoryCache: Cache,
    private val diskCache: Cache
) {

    companion object {
        @Volatile
        private var INSTANCE: ImageLoader? = null

        fun getInstance(context: Context): ImageLoader =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageLoader(
                    MemoryCacheManager,
                    DiskCacheManager(context)
                ).also { INSTANCE = it }
            }
    }

    private val activeDownloads = ConcurrentHashMap<ImageView, Job>()

    inner class Builder() {
        private lateinit var url: String
        private var placeholder: Drawable? = null
        private var errorDrawable: Drawable? = null
        private var fadeInAnimation: Boolean = false

        fun load(url: String): Builder {
            this.url = url
            return this
        }

        fun withPlaceholder(drawable: Drawable?): Builder {
            this.placeholder = drawable
            return this
        }

        fun withErrorDrawable(drawable: Drawable?): Builder {
            this.errorDrawable = drawable
            return this
        }

        fun withFadeInAnimation(): Builder {
            this.fadeInAnimation = true
            return this
        }

        fun into(imageView: ImageView) {

            // Cancel any previous download for this ImageView
            activeDownloads[imageView]?.cancel()

            imageView.setImageDrawable(placeholder)
            imageView.tag = url

            // Check memory cache first
            val memCachedBitmap = memoryCache.get(url)
            if (memCachedBitmap != null) {
                setBitmap(imageView, memCachedBitmap)
                return
            }

            // Check disk cache next
            val diskCachedBitmap = diskCache.get(url)
            if (diskCachedBitmap != null) {
                memoryCache.put(url, diskCachedBitmap)
                setBitmap(imageView, diskCachedBitmap)
                return
            }

            // Defer the download until the ImageView is laid out and its dimensions are available
            imageView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    // We only want to run this once.
                    imageView.viewTreeObserver.removeOnPreDrawListener(this)

                    // Check again if the tag has changed (the view has been recycled)
                    if (imageView.tag != url) {
                        return true
                    }

                    val targetWidth = imageView.width
                    val targetHeight = imageView.height

                    if (targetWidth > 0 && targetHeight > 0) {
                        startLoad(imageView, url, targetWidth, targetHeight)
                    } else {
                        Log.w(TAG, "ImageView dimensions are 0 after onPreDraw. Skipping download for $url")
                    }

                    return true
                }

            })

        }

        private fun startLoad(
            imageView: ImageView,
            url: String,
            targetWidth: Int,
            targetHeight: Int
        ) {
            val job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    val downloadedBitmap = downloadImage(url, targetWidth, targetHeight)
                    if (downloadedBitmap != null) {
                        diskCache.put(url, downloadedBitmap)
                        memoryCache.put(url, downloadedBitmap)
                        withContext(Dispatchers.Main) {
                            if (imageView.tag == url) {
                                setBitmap(imageView, downloadedBitmap)
                            } else {
                                Log.d(TAG, "ImageView recycled. Skipping bitmap update for $url")
                            }
                        }
                    } else {
                        Log.e(TAG, "Failed to download or decode bitmap for $url")
                        withContext(Dispatchers.Main) {
                            setErrorDrawable(imageView)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading image from $url", e)
                    withContext(Dispatchers.Main) {
                        setErrorDrawable(imageView)
                    }
                } finally {
                    activeDownloads.remove(imageView)
                }
            }
            activeDownloads[imageView] = job
        }

        private fun setBitmap(imageView: ImageView, bitmap: Bitmap) {
            if (fadeInAnimation) {
                imageView.alpha = 0f
                imageView.setImageBitmap(bitmap)
                imageView.animate().alpha(1f).setDuration(200).start()
            } else {
                imageView.setImageBitmap(bitmap)
            }
        }

        private fun setErrorDrawable(imageView: ImageView) {
            if (imageView.tag == url) {
                imageView.setImageDrawable(errorDrawable)
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
        memoryCache.clear()
        diskCache.clear()
    }
}