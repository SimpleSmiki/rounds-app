package simple.smiki.roundsapp.data

import com.google.gson.annotations.SerializedName

/**
 * Data model for a single image item from the JSON API.
 */
data class ImageItem(
    @SerializedName("id")
    val id: Int,

    @SerializedName("imageUrl")
    val url: String
)