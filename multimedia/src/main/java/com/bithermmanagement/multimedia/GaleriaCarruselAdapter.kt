package com.bithermmanagement.multimedia

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.multimedia.R
import com.bumptech.glide.Glide
import java.io.File

data class FotoItem(
    val tipo: String,
    val file: File?,
    val esObligatoria: Boolean,
    val esBotonAnadir: Boolean = false
)

class GaleriaCarruselAdapter(
    private val context: Context,
    private val equipoId: String,
    private val onFotoClick: (FotoItem) -> Unit,
    private val onBorrarClick: (FotoItem) -> Unit,
    private val onEditarClick: (FotoItem) -> Unit,
    private val onAnadirClick: () -> Unit
) : RecyclerView.Adapter<GaleriaCarruselAdapter.FotoViewHolder>() {

    private var fotos = mutableListOf<FotoItem>()

    fun actualizarFotos(fotosObligatorias: List<FotoItem>, fotosExtra: List<FotoItem>) {
        fotos.clear()
        
        // Añadir fotos obligatorias
        fotos.addAll(fotosObligatorias)
        
        // Añadir fotos extra
        fotos.addAll(fotosExtra)
        
        // Añadir botón de añadir al final
        fotos.add(FotoItem("", null, false, true))
        
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FotoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_foto_galeria, parent, false)
        return FotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: FotoViewHolder, position: Int) {
        val foto = fotos[position]
        holder.bind(foto)
    }

    override fun getItemCount(): Int = fotos.size

    inner class FotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgFoto: ImageView = itemView.findViewById(R.id.imgFoto)
        private val overlayControles: LinearLayout = itemView.findViewById(R.id.overlayControles)
        private val btnBorrar: ImageView = itemView.findViewById(R.id.btnBorrar)
        private val btnEditar: ImageView = itemView.findViewById(R.id.btnEditar)
        private val txtTipoFoto: TextView = itemView.findViewById(R.id.txtTipoFoto)
        private val btnAnadir: LinearLayout = itemView.findViewById(R.id.btnAnadir)

        fun bind(foto: FotoItem) {
            if (foto.esBotonAnadir) {
                // Mostrar botón de añadir
                imgFoto.visibility = View.GONE
                overlayControles.visibility = View.GONE
                btnAnadir.visibility = View.VISIBLE
                
                btnAnadir.setOnClickListener { onAnadirClick() }
                return
            }

            // Mostrar foto normal
            imgFoto.visibility = View.VISIBLE
            btnAnadir.visibility = View.GONE

            if (foto.file != null && foto.file.exists()) {
                // Foto existe - cargar imagen y mostrar controles
                Glide.with(context).load(foto.file).into(imgFoto)
                overlayControles.visibility = View.VISIBLE
                txtTipoFoto.text = foto.tipo
                
                // Configurar controles
                btnBorrar.setOnClickListener { onBorrarClick(foto) }
                btnEditar.setOnClickListener { onEditarClick(foto) }
                itemView.setOnClickListener { onFotoClick(foto) }
            } else {
                // Foto no existe - mostrar placeholder
                imgFoto.setImageResource(R.drawable.ic_photo_placeholder)
                overlayControles.visibility = View.GONE
                itemView.setOnClickListener { onFotoClick(foto) }
            }
        }
    }
} 