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
import simple.smiki.imageloadlib.ImageLoader
import simple.smiki.roundsapp.databinding.ActivityMainBinding
import simple.smiki.roundsapp.retrofit.NetworkClient

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
                val images = NetworkClient.apiService.getImages()
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
