package simple.smiki.roundsapp.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * A singleton object to manage the Retrofit network client.
 * This ensures the client is initialized only once and is easily accessible.
 */
object NetworkClient {

    private const val BASE_URL = "https://zipoapps-storage-test.nyc3.digitaloceanspaces.com"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
