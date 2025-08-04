package com.bithermmanagement.ui.items

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.utils.ImageUtils
import com.github.chrisbanes.photoview.PhotoView
import com.bumptech.glide.Glide
import java.io.File

class VisorImagenesAdapter(
    private val imagenes: List<String>,
    private val onEliminarClick: (Int) -> Unit,
    private val onSustituirClick: (Int) -> Unit,
    private val onAnadirClick: () -> Unit,
    private val onHacerPrincipalClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IMAGEN = 0
        private const val VIEW_TYPE_ANADIR = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == imagenes.size) VIEW_TYPE_ANADIR else VIEW_TYPE_IMAGEN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGEN -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_visor_imagen, parent, false)
                ImagenViewHolder(view)
            }
            VIEW_TYPE_ANADIR -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_visor_anadir, parent, false)
                AnadirViewHolder(view)
            }
            else -> throw IllegalArgumentException("Tipo de vista no válido")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImagenViewHolder -> {
                holder.bind(imagenes[position])
            }
            is AnadirViewHolder -> {
                holder.bind()
            }
        }
    }

    override fun getItemCount(): Int = imagenes.size + 1

    inner class ImagenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btnEliminar)
        private val btnSustituir: ImageButton = itemView.findViewById(R.id.btnSustituir)

        fun bind(rutaImagen: String) {
            Glide.with(itemView.context)
                .load(File(rutaImagen))
                .into(photoView)

            btnEliminar.setOnClickListener { onEliminarClick(adapterPosition) }
            btnSustituir.setOnClickListener { onSustituirClick(adapterPosition) }

            // Menú emergente en pulsación larga
            photoView.setOnLongClickListener {
                val opciones = arrayOf("Hacer foto principal", "Eliminar foto", "Sustituir foto")
                AlertDialog.Builder(itemView.context)
                    .setTitle("Opciones de imagen")
                    .setItems(opciones) { _, which ->
                        when (which) {
                            0 -> onHacerPrincipalClick(adapterPosition)
                            1 -> onEliminarClick(adapterPosition)
                            2 -> onSustituirClick(adapterPosition)
                        }
                    }
                    .show()
                true
            }
        }
    }

    inner class AnadirViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val btnAnadir: ImageButton = itemView.findViewById(R.id.btnAnadir)

        fun bind() {
            btnAnadir.setOnClickListener { onAnadirClick() }
        }
    }
} 