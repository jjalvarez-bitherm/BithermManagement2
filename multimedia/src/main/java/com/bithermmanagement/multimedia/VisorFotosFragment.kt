package com.bithermmanagement.multimedia

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bithermmanagement.multimedia.R
import com.github.chrisbanes.photoview.PhotoView
import com.github.chrisbanes.photoview.OnPhotoTapListener
import com.github.chrisbanes.photoview.OnScaleChangedListener
import android.graphics.BitmapFactory
import java.io.File

class VisorFotosFragment : DialogFragment() {
    
    companion object {
        private const val ARG_FILE_PATH = "file_path"
        private const val ARG_TITULO = "titulo"
        
        fun newInstance(filePath: String, titulo: String): VisorFotosFragment {
            return VisorFotosFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILE_PATH, filePath)
                    putString(ARG_TITULO, titulo)
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_visor_fotos, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val filePath = arguments?.getString(ARG_FILE_PATH) ?: return
        val titulo = arguments?.getString(ARG_TITULO) ?: "Foto"
        
        val photoView = view.findViewById<PhotoView>(R.id.photoView)
        val btnCerrar = view.findViewById<ImageView>(R.id.btnCerrar)
        val btnCompartir = view.findViewById<ImageView>(R.id.btnCompartir)
        val txtTitulo = view.findViewById<TextView>(R.id.txtTitulo)
        val txtZoom = view.findViewById<TextView>(R.id.txtZoom)
        
        // Configurar título
        txtTitulo.text = titulo
        
        // Cargar imagen
        val file = File(filePath)
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            photoView.setImageBitmap(bitmap)
        }
        
        // Configurar listeners
        btnCerrar.setOnClickListener {
            dismiss()
        }
        
        btnCompartir.setOnClickListener {
            // TODO: Implementar compartir
        }
        
        // Listener para zoom
        photoView.setOnScaleChangeListener(object : OnScaleChangedListener {
            override fun onScaleChange(scaleFactor: Float, focusX: Float, focusY: Float) {
                val zoomPercent = (scaleFactor * 100).toInt()
                txtZoom.text = "Zoom: ${zoomPercent}%"
            }
        })
        
        // Listener para tap
        photoView.setOnPhotoTapListener(object : OnPhotoTapListener {
            override fun onPhotoTap(view: ImageView?, x: Float, y: Float) {
                // Toggle visibilidad de controles
                val barraSuperior = requireView().findViewById<View>(R.id.barraSuperior)
                val txtZoom = requireView().findViewById<TextView>(R.id.txtZoom)
                
                barraSuperior.visibility = if (barraSuperior.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                txtZoom.visibility = if (txtZoom.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        })
    }
    
    override fun onStart() {
        super.onStart()
        // Configurar el diálogo para pantalla completa
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }
} 