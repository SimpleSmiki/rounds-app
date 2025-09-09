package simple.smiki.roundsapp

import retrofit2.http.GET

/**
 * Retrofit interface for fetching the image list.
 */
interface ApiService {
    @GET("/image_list.json")
    suspend fun getImages(): List<ImageItem>
}