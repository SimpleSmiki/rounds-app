package simple.smiki.roundsapp

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import simple.smiki.imageloadlib.ImageLoader
import simple.smiki.roundsapp.databinding.ListItemImageBinding

/**
 * Adapter for the RecyclerView to display images from the list.
 */
class ImageAdapter(
    private val context: Context,
    private val images: List<ImageItem>
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(val binding: ListItemImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ListItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageItem = images[position]

        holder.binding.textView.text = "ID: ${imageItem.id}"

        val placeholder = ContextCompat.getDrawable(context, R.drawable.ic_placeholder)

        ImageLoader.getInstance(context)
            .Builder(imageItem.url)
            .placeholder(placeholder)
            .into(holder.binding.imageView)
    }

    override fun getItemCount(): Int {
        return images.size
    }
}

