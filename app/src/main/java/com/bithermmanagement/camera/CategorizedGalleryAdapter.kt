package com.bithermmanagement.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CategorizedGalleryAdapter(
    private val onPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<CategorizedGalleryAdapter.PhotoViewHolder>() {
    
    private var photos: List<String> = emptyList()
    
    fun updatePhotos(newPhotos: List<String>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_photo, parent, false)
        return PhotoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }
    
    override fun getItemCount(): Int = photos.size
    
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        
        fun bind(photoPath: String) {
            // Cargar imagen con Glide
            Glide.with(itemView.context)
                .load(File(photoPath))
                .centerCrop()
                .placeholder(R.drawable.ic_image_placeholder)
                .into(imageView)
            
            // Mostrar timestamp
            val file = File(photoPath)
            val lastModified = Date(file.lastModified())
            val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            tvTimestamp.text = formatter.format(lastModified)
            
            // Click listener
            itemView.setOnClickListener {
                onPhotoClick(photoPath)
            }
        }
    }
}