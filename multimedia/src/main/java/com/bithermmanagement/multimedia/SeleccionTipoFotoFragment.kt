package com.bithermmanagement.multimedia

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment

class SeleccionTipoFotoFragment : DialogFragment() {
    
    private var onTipoSeleccionado: ((EditorWhatsAppStyleFragment.TipoFoto) -> Unit)? = null
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_seleccion_tipo_foto)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_seleccion_tipo_foto, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        configurarBotones(view)
    }
    
    private fun configurarBotones(view: View) {
        view.findViewById<Button>(R.id.btnFotoEquipo).setOnClickListener {
            onTipoSeleccionado?.invoke(EditorWhatsAppStyleFragment.TipoFoto.FOTO_EQUIPO)
            dismiss()
        }
        
        view.findViewById<Button>(R.id.btnFotoUbicacion).setOnClickListener {
            onTipoSeleccionado?.invoke(EditorWhatsAppStyleFragment.TipoFoto.FOTO_UBICACION)
            dismiss()
        }
        
        view.findViewById<Button>(R.id.btnFotoManifold).setOnClickListener {
            onTipoSeleccionado?.invoke(EditorWhatsAppStyleFragment.TipoFoto.FOTO_MANIFOLD)
            dismiss()
        }
        
        view.findViewById<Button>(R.id.btnFotoExtras).setOnClickListener {
            onTipoSeleccionado?.invoke(EditorWhatsAppStyleFragment.TipoFoto.EXTRAS)
            dismiss()
        }
        
        view.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dismiss()
        }
    }
    
    fun setOnTipoSeleccionadoListener(listener: (EditorWhatsAppStyleFragment.TipoFoto) -> Unit) {
        onTipoSeleccionado = listener
    }
    
    companion object {
        fun newInstance(): SeleccionTipoFotoFragment {
            return SeleccionTipoFotoFragment()
        }
    }
} 