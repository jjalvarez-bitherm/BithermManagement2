package com.bithermmanagement.camera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import java.io.File

class CategorizedPhotoViewerAdapter(
    private val photos: List<String>
) : RecyclerView.Adapter<CategorizedPhotoViewerAdapter.PhotoViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo_viewer, parent, false)
        return PhotoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }
    
    override fun getItemCount(): Int = photos.size
    
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        
        fun bind(photoPath: String) {
            Glide.with(itemView.context)
                .load(File(photoPath))
                .into(photoView)
        }
    }
}