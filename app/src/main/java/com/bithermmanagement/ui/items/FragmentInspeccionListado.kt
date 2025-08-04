package com.bithermmanagement.ui.items

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.Equipo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.appcompat.widget.SearchView

class FragmentInspeccionListado : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: InspeccionListadoAdapter
    private lateinit var areaSpinner: AutoCompleteTextView
    private lateinit var unitSpinner: AutoCompleteTextView
    private lateinit var btnPlayPause: ImageButton

    private var inspecciones: List<Equipo> = emptyList()
    private var inspeccionesFiltradas: List<Equipo> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.d("FragmentInspeccionListado", "onCreateView llamado")
        val view = inflater.inflate(R.layout.fragment_inspeccion_listado, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        areaSpinner = view.findViewById(R.id.areaSpinner)
        unitSpinner = view.findViewById(R.id.unitSpinner)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentInspeccionListado", "onViewCreated llamado")
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = InspeccionListadoAdapter(emptyList()) { equipo ->
            Log.d("FragmentInspeccionListado", "CLICK en equipo desde adapter: id=${equipo.id}, estado=${equipo.estado}")
            val indiceSeleccionado = inspeccionesFiltradas.indexOfFirst { it.id == equipo.id }
            Log.d("FragmentInspeccionListado", "DEBUG: indiceSeleccionado=$indiceSeleccionado, inspeccionesFiltradas.size=${inspeccionesFiltradas.size}")
            
            if (indiceSeleccionado == -1) {
                Log.e("FragmentInspeccionListado", "ERROR: No se encontró el equipo ${equipo.id} en inspeccionesFiltradas")
                return@InspeccionListadoAdapter
            }
            
            val fragment = FragmentInspeccionEquipo.newInstance(equipo.id)
            Log.d("FragmentInspeccionListado", "NAVEGANDO a FragmentInspeccionEquipo con equipo: ${equipo.id}")
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = adapter
        // Inicializar filtros de área y unidad
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            val areas = withContext(Dispatchers.IO) { db.equipoDao().getAreasUnicas() }
            areaSpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, areas))
            areaSpinner.setOnItemClickListener { _, _, position, _ ->
                val areaSeleccionada = areas[position]
                Log.d("FragmentInspeccionListado", "SELECT en areaSpinner: área seleccionada=$areaSeleccionada")
                // Filtrar unidades por área seleccionada
                lifecycleScope.launch {
                    val unidades = withContext(Dispatchers.IO) { db.equipoDao().getUnidadesPorArea(areaSeleccionada) }
                    unitSpinner.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, unidades))
                    unitSpinner.text.clear()
                    unitSpinner.showDropDown() // Autoabrir
                }
                aplicarFiltros()
            }
        }
        unitSpinner.setOnItemClickListener { _, _, position, _ ->
            val unidadSeleccionada = unitSpinner.adapter.getItem(position) as? String
            Log.d("FragmentInspeccionListado", "SELECT en unitSpinner: unidad seleccionada=$unidadSeleccionada")
            aplicarFiltros()
        }
        btnPlayPause.setOnClickListener {
            Log.d("FragmentInspeccionListado", "CLICK en btnPlayPause")
            if (btnPlayPause.tag == "play") {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
                btnPlayPause.tag = "pause"
                Log.d("FragmentInspeccionListado", "Cambiando a modo PAUSE")
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
                btnPlayPause.tag = "play"
                Log.d("FragmentInspeccionListado", "Cambiando a modo PLAY")
            }
        }
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            inspecciones = withContext(Dispatchers.IO) { db.equipoDao().getAllEquipos() }
            Log.d("FragmentInspeccionListado", "Cargados ${inspecciones.size} equipos desde BD")
            aplicarFiltros()
        }
    }

    private fun aplicarFiltros() {
        val area = areaSpinner.text.toString().trim().lowercase()
        val unidad = unitSpinner.text.toString().trim().lowercase()
        inspeccionesFiltradas = inspecciones.filter { ins ->
            (area.isEmpty() || ins.area?.lowercase() == area)
            && (unidad.isEmpty() || ins.unidad?.lowercase() == unidad)
        }
        Log.d("FragmentInspeccionListado", "aplicarFiltros: ${inspeccionesFiltradas.size} equipos filtrados de ${inspecciones.size} totales (área='$area', unidad='$unidad')")
        adapter.actualizarLista(inspeccionesFiltradas)
    }
} 