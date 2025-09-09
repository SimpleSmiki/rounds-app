package simple.smiki.roundsapp.retrofit

import retrofit2.http.GET
import simple.smiki.roundsapp.ImageItem

/**
 * Retrofit interface for fetching the image list.
 */
interface ApiService {
    @GET("/image_list.json")
    suspend fun getImages(): List<ImageItem>
}