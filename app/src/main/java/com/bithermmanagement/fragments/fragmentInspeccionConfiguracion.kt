package com.bithermmanagement.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import com.bithermmanagement.ui.items.FragmentInspeccionConfiguracion

class fragmentInspeccionConfiguracion : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("InspeccionConfigNav", "fragmentInspeccionConfiguracion.onCreate llamado")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("InspeccionConfigNav", "fragmentInspeccionConfiguracion.onCreateView llamado")
        // Inflar un layout simple con FrameLayout contenedor
        return inflater.inflate(R.layout.fragment_inspeccion_config_nav, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("InspeccionConfigNav", "fragmentInspeccionConfiguracion.onViewCreated llamado")
        // Añadir el fragmento real como hijo
        if (childFragmentManager.findFragmentById(R.id.child_fragment_container) == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.child_fragment_container, FragmentInspeccionConfiguracion())
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("InspeccionConfigNav", "fragmentInspeccionConfiguracion.onResume llamado")
    }

    override fun onPause() {
        super.onPause()
        Log.d("InspeccionConfigNav", "fragmentInspeccionConfiguracion.onPause llamado")
    }
} 