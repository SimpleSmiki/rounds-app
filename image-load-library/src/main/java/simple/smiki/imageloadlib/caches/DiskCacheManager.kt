package simple.smiki.imageloadlib.caches

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

private const val CACHE_VALIDITY_MS = 4 * 60 * 60 * 1000L // 4 hours in milliseconds

class DiskCacheManager(context: Context) : Cache {

    private val cacheDir: File = File(context.cacheDir, "image_cache")

    init {
        // Ensure the cache directory exists
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Retrieves a bitmap from the disk cache if it exists and is still valid.
     * @return The Bitmap if valid, otherwise null.
     */
    override fun get(url: String): Bitmap? {
        val file = getFile(url)
        if (file.exists() && isFileValid(file)) {
            return BitmapFactory.decodeFile(file.absolutePath)
        }
        return null
    }

    /**
     * Saves a bitmap to the disk cache.
     */
    override fun put(url: String, bitmap: Bitmap) {
        val file = getFile(url)
        try {
            FileOutputStream(file).use { out ->
                // Compress the bitmap and write it to the file
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            Log.e("ImageLoader", "Failed to save to disk cache: ${e.message}")
        }
    }

    /**
     * Deletes all files from the cache directory.
     */
    override fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun getFile(url: String): File {
        // Use a hash of the URL as the filename to handle special characters and long URLs
        val hashedName = url.toMd5()
        return File(cacheDir, hashedName)
    }

    private fun isFileValid(file: File): Boolean {
        // Check if the file's last modified time is within the 4-hour validity period
        return System.currentTimeMillis() - file.lastModified() < CACHE_VALIDITY_MS
    }

    // Extension function to create a unique hash from the URL
    private fun String.toMd5(): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(this.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}