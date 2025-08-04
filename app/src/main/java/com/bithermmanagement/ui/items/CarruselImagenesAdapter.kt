package com.bithermmanagement.ui.items

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bumptech.glide.Glide
import java.io.File

class CarruselImagenesAdapter(
    private val imagenes: List<String>
) : RecyclerView.Adapter<CarruselImagenesAdapter.ViewHolder>() {

    init {
        Log.d("CarruselAdapter", "Adapter creado con ${imagenes.size} imágenes")
        imagenes.forEachIndexed { index, imagen ->
            Log.d("CarruselAdapter", "Imagen $index: $imagen")
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("CarruselAdapter", "Creando ViewHolder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrusel_imagen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imagen = imagenes[position]
        Log.d("CarruselAdapter", "Vinculando imagen en posición $position: $imagen")
        
        if (imagen.isNotBlank() && File(imagen).exists()) {
            Log.d("CarruselAdapter", "Cargando imagen desde archivo: $imagen")
            Glide.with(holder.itemView.context)
                .load(File(imagen))
                .placeholder(R.drawable.bg_spinner_rounded)
                .error(R.drawable.bg_spinner_rounded)
                .centerCrop()
                .into(holder.imageView)
        } else {
            Log.d("CarruselAdapter", "Mostrando placeholder - archivo no existe o está vacío: $imagen")
            holder.imageView.setImageResource(R.drawable.bg_spinner_rounded)
        }
    }

    override fun getItemCount(): Int {
        Log.d("CarruselAdapter", "getItemCount llamado: ${imagenes.size}")
        return imagenes.size
    }
} 