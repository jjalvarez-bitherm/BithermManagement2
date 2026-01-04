package com.bithermmanagement.ui.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bithermmanagement.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class GalleryFolderAdapter(
    private var folders: List<GalleryFolder>,
    private val onFolderClick: (GalleryFolder) -> Unit
) : RecyclerView.Adapter<GalleryFolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPreview: ImageView = itemView.findViewById(R.id.ivPreview)
        val tvFolderName: TextView = itemView.findViewById(R.id.tvFolderName)
        val tvPhotoCount: TextView = itemView.findViewById(R.id.tvPhotoCount)
        val vConstructionOverlay: View = itemView.findViewById(R.id.vConstructionOverlay)
        val ivConstruction: ImageView = itemView.findViewById(R.id.ivConstruction)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val folder = folders[position]
                    if (!folder.isUnderConstruction) {
                        onFolderClick(folder)
                    }
                }
            }
        }

        fun bind(folder: GalleryFolder) {
            tvFolderName.text = folder.name
            
            // Configurar el tamaño de span para layout staggered
            val layoutParams = itemView.layoutParams as StaggeredGridLayoutManager.LayoutParams
            layoutParams.isFullSpan = folder.spanSizeX == 2 // Ocupar ancho completo si spanSizeX es 2
            
            // Ajustar altura específicamente para PROYECTOS (120% más alto)
            val baseHeight = 132 // dp base
            val newHeight = if (folder.name == "PROYECTOS") {
                (baseHeight * 1.2).toInt() // 120% para PROYECTOS
            } else {
                baseHeight
            }
            layoutParams.height = (newHeight * itemView.context.resources.displayMetrics.density).toInt()

            // Mostrar información según si está en construcción
            if (folder.isUnderConstruction) {
                tvPhotoCount.text = "En construcción"
                vConstructionOverlay.visibility = View.VISIBLE
                ivConstruction.visibility = View.VISIBLE
                ivPreview.setImageResource(R.drawable.ic_folder)
                itemView.alpha = 0.8f
            } else {
                // Determinar el texto del contador según el tipo de carpeta
                val counterText = when (folder.name) {
                    "PROYECTOS" -> {
                        val subfolderCount = folder.photos.count { it.isSubfolder }
                        if (subfolderCount > 0) "$subfolderCount proyectos" else "${folder.photos.size} fotos"
                    }
                    "DENUNCIAS" -> {
                        val subfolderCount = folder.photos.count { it.isSubfolder }
                        if (subfolderCount > 0) "$subfolderCount denuncias" else "${folder.photos.size} fotos"
                    }
                    "PURGADORES", "FOTO NOTA", "MONITORIZACIÓN" -> {
                        val subfolderCount = folder.photos.count { it.isSubfolder }
                        if (subfolderCount > 0) "$subfolderCount carpetas" else "${folder.photos.size} fotos"
                    }
                    else -> "${folder.photos.size} fotos"
                }
                tvPhotoCount.text = counterText
                vConstructionOverlay.visibility = View.GONE
                ivConstruction.visibility = View.GONE
                itemView.alpha = 1.0f
                
                // Cargar imagen de preview - evitar directorios
                if (folder.coverPhoto != null && !folder.coverPhoto.isSubfolder) {
                    Glide.with(itemView.context)
                        .load(folder.coverPhoto.file)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .placeholder(R.drawable.ic_photo_library)
                        .error(R.drawable.ic_photo_library)
                        .into(ivPreview)
                } else {
                    ivPreview.setImageResource(R.drawable.ic_photo_library)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gallery_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    fun updateFolders(newFolders: List<GalleryFolder>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}