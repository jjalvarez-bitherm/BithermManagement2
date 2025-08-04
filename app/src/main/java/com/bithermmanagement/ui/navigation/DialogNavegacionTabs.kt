package com.bithermmanagement.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bithermmanagement.R
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayoutMediator

class DialogNavegacionTabs(
    private val origen: LatLng,
    private val destino: LatLng
) : DialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_navegacion_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Mostrar el fragmento del mapa en el contenedor superior
        childFragmentManager.beginTransaction()
            .replace(R.id.contenedorMapa, MapaNavegacionFragment.newInstance(origen, destino))
            .commitNow()
        // Mostrar el fragmento smartwatch en el contenedor inferior
        childFragmentManager.beginTransaction()
            .replace(R.id.contenedorSmartwatch, SmartwatchNavegacionFragment.newInstance(origen, destino))
            .commitNow()
    }
} 