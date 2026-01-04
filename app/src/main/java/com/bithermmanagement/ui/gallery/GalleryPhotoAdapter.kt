package com.bithermmanagement.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class GalleryPhotoAdapter(
    private val photos: List<GalleryPhoto>,
    private val onPhotoClick: (GalleryPhoto, Int) -> Unit,
    private val onPhotoLongClick: (GalleryPhoto, Int) -> Boolean
) : RecyclerView.Adapter<GalleryPhotoAdapter.PhotoViewHolder>() {

    private val selectedPhotos = mutableSetOf<Int>()
    var isSelectionMode = false
        private set
        
    private var onSelectionChanged: (() -> Unit)? = null
    
    fun setOnSelectionChangedListener(listener: () -> Unit) {
        onSelectionChanged = listener
    }

    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val vSelectionOverlay: View = itemView.findViewById(R.id.vSelectionOverlay)
        val ivSelectionCircle: ImageView = itemView.findViewById(R.id.ivSelectionCircle)
        val ivSelectionCheck: ImageView = itemView.findViewById(R.id.ivSelectionCheck)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (isSelectionMode) {
                        toggleSelection(position)
                    } else {
                        onPhotoClick(photos[position], position)
                    }
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (!isSelectionMode) {
                        startSelectionMode()
                        toggleSelection(position)
                    }
                    onPhotoLongClick(photos[position], position)
                } else {
                    false
                }
            }
        }

        fun bind(photo: GalleryPhoto, position: Int) {
            // Cargar thumbnail o icono de carpeta para subcarpetas
            if (photo.isSubfolder) {
                // Es una subcarpeta - mostrar icono de carpeta
                ivThumbnail.setImageResource(R.drawable.ic_folder)
                ivThumbnail.scaleType = ImageView.ScaleType.CENTER
            } else {
                // Es una foto normal - cargar thumbnail
                ivThumbnail.scaleType = ImageView.ScaleType.CENTER_CROP
                Glide.with(itemView.context)
                    .load(photo.file)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .placeholder(R.drawable.ic_photo_library)
                    .error(R.drawable.ic_photo_library)
                    .into(ivThumbnail)
            }

            // Mostrar timestamp si está en modo selección
            if (isSelectionMode) {
                if (photo.isSubfolder) {
                    tvTimestamp.text = photo.file.name // Mostrar nombre de subcarpeta
                } else {
                    val format = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                    tvTimestamp.text = format.format(java.util.Date(photo.timestamp))
                }
                tvTimestamp.visibility = View.VISIBLE
                tvFolderName.visibility = View.GONE
            } else {
                tvTimestamp.visibility = View.GONE
                // Mostrar nombre de subcarpeta siempre si es subcarpeta
                if (photo.isSubfolder) {
                    tvFolderName.text = photo.file.name
                    tvFolderName.visibility = View.VISIBLE
                } else {
                    tvFolderName.visibility = View.GONE
                }
            }

            // Estado de selección
            val isSelected = selectedPhotos.contains(position)
            updateSelectionUI(isSelected)
        }

        private fun updateSelectionUI(isSelected: Boolean) {
            if (isSelectionMode) {
                ivSelectionCircle.visibility = if (isSelected) View.GONE else View.VISIBLE
                ivSelectionCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
                vSelectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            } else {
                ivSelectionCircle.visibility = View.GONE
                ivSelectionCheck.visibility = View.GONE
                vSelectionOverlay.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position], position)
    }

    override fun getItemCount(): Int = photos.size

    private fun toggleSelection(position: Int) {
        if (selectedPhotos.contains(position)) {
            selectedPhotos.remove(position)
        } else {
            selectedPhotos.add(position)
        }
        notifyItemChanged(position)

        // Si no hay selecciones, salir del modo selección
        if (selectedPhotos.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun startSelectionMode() {
        isSelectionMode = true
        notifyDataSetChanged()
    }

    fun enterSelectionMode(initialPosition: Int) {
        isSelectionMode = true
        selectedPhotos.clear()
        selectedPhotos.add(initialPosition)
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedPhotos.clear()
        notifyDataSetChanged()
        onSelectionChanged?.invoke()
    }

    fun getSelectedPhotos(): List<GalleryPhoto> {
        return selectedPhotos.map { photos[it] }
    }

    fun getSelectedCount(): Int = selectedPhotos.size

    fun selectAll() {
        selectedPhotos.clear()
        selectedPhotos.addAll(photos.indices)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPhotos.clear()
        notifyDataSetChanged()
    }
}