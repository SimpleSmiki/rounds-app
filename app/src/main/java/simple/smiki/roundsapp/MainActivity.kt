package simple.smiki.roundsapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import simple.smiki.imageloadlib.ImageLoader
import simple.smiki.roundsapp.databinding.ActivityMainBinding // Import the generated binding class

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiService: ApiService
    private lateinit var imageAdapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets -> // Use binding.main
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        // Set up Retrofit for API calls
        val retrofit = Retrofit.Builder()
            .baseUrl("https://zipoapps-storage-test.nyc3.digitaloceanspaces.com")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Fetch the images and populate the list
        fetchImages()

        // Set click listener for the invalidate cache button
        binding.invalidateCacheButton.setOnClickListener {
            ImageLoader.getInstance(this).invalidateCache()
            Toast.makeText(this, R.string.cache_invalidated, Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchImages() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val images = apiService.getImages()
                withContext(Dispatchers.Main) {
                    // Update the RecyclerView on the main thread
                    imageAdapter = ImageAdapter(this@MainActivity, images)
                    binding.recyclerView.adapter = imageAdapter
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, R.string.error_fetching_images, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
