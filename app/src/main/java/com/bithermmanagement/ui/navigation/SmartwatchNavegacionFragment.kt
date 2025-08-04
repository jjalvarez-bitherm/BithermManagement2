package com.bithermmanagement.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import com.google.android.gms.maps.model.LatLng

class SmartwatchNavegacionFragment : Fragment() {
    
    private var origen: LatLng? = null
    private var destino: LatLng? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_smartwatch_navegacion, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Configurar navegación por smartwatch
    }
    
    companion object {
        fun newInstance(origen: LatLng, destino: LatLng): SmartwatchNavegacionFragment {
            return SmartwatchNavegacionFragment().apply {
                this.origen = origen
                this.destino = destino
            }
        }
    }
} 