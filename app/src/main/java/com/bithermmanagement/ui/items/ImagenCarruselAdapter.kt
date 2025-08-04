package com.bithermmanagement.ui.items

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bumptech.glide.Glide

class ImagenCarruselAdapter(
    private val fotos: MutableList<String>,
    private val onEliminar: (Int) -> Unit,
    private val onVerGrande: (String) -> Unit,
    private val onAddFoto: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TIPO_FOTO = 0
        private const val TIPO_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < fotos.size) TIPO_FOTO else TIPO_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TIPO_FOTO) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto_carrusel, parent, false)
            FotoViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_foto_add, parent, false)
            AddViewHolder(view)
        }
    }

    override fun getItemCount(): Int = fotos.size + 1

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FotoViewHolder && position < fotos.size) {
            val url = fotos[position]
            Glide.with(holder.imageView.context).load(url).placeholder(R.drawable.bg_spinner_rounded).into(holder.imageView)
            holder.btnEliminar.setOnClickListener { onEliminar(position) }
            holder.imageView.setOnClickListener { onVerGrande(url) }
        } else if (holder is AddViewHolder) {
            holder.btnAdd.setOnClickListener { onAddFoto() }
        }
    }

    class FotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageFoto)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarFoto)
    }

    class AddViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val btnAdd: ImageButton = view.findViewById(R.id.btnAddFoto)
    }
} 
 
 