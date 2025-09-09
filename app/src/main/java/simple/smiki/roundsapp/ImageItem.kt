package simple.smiki.roundsapp

import com.google.gson.annotations.SerializedName

/**
 * Data model for a single image item from the JSON API.
 */
data class ImageItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("imageUrl")
    val url: String
)