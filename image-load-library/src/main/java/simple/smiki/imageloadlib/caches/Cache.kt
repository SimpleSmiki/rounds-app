package simple.smiki.imageloadlib.caches

import android.graphics.Bitmap

interface Cache {
    fun get(url: String): Bitmap?
    fun put(url: String, bitmap: Bitmap)
    fun clear()
}