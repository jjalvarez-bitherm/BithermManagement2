package com.bithermmanagement.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class FotosPagerAdapter(
    private var fotos: List<String>,
    private val onFotoPrincipal: (Int) -> Unit
) : RecyclerView.Adapter<FotosPagerAdapter.FotoViewHolder>() {

    private val infiniteFotos: List<String>
        get() = if (fotos.isEmpty()) emptyList() else fotos + fotos

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_foto, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val fotoUrl = infiniteFotos[position]
        Glide.with(holder.itemView.context)
            .load(fotoUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = infiniteFotos.size

    fun updateFotos(nuevasFotos: List<String>) {
        fotos = nuevasFotos
        notifyDataSetChanged()
    }

    fun hacerFotoPrincipal(position: Int) {
        onFotoPrincipal(position)
    }

    class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
} 