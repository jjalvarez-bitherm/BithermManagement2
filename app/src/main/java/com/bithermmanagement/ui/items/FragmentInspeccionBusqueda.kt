package com.bithermmanagement.ui.items

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.EquipoView
import com.bithermmanagement.ui.adapters.PurgadorAdapter
import com.bithermmanagement.ui.dialogs.MultiSelectFilterDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FragmentInspeccionBusqueda : Fragment() {
    private lateinit var searchView: SearchView
    private lateinit var btnFiltroEstado: Button
    private lateinit var btnFiltroArea: Button
    private lateinit var btnFiltroUnidad: Button
    private lateinit var btnFiltroMarca: Button
    private lateinit var btnFiltroModelo: Button
    private lateinit var btnLimpiarFiltros: Button
    private lateinit var recyclerPurgadores: RecyclerView
    private lateinit var adapter: PurgadorAdapter

    private val selectedEstados = mutableSetOf<String>()
    private val selectedAreas = mutableSetOf<String>()
    private val selectedUnidades = mutableSetOf<String>()
    private val selectedMarcas = mutableSetOf<String>()
    private val selectedModelos = mutableSetOf<String>()

    private var equipos = listOf<EquipoView>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_inspeccion_busqueda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        inicializarVistas(view)
        setupRecyclerView()
        setupListeners()
        cargarDatos()
    }

    private fun inicializarVistas(view: View) {
        searchView = view.findViewById(R.id.searchView)
        btnFiltroEstado = view.findViewById(R.id.btnFiltroEstado)
        btnFiltroArea = view.findViewById(R.id.btnFiltroArea)
        btnFiltroUnidad = view.findViewById(R.id.btnFiltroUnidad)
        btnFiltroMarca = view.findViewById(R.id.btnFiltroMarca)
        btnFiltroModelo = view.findViewById(R.id.btnFiltroModelo)
        btnLimpiarFiltros = view.findViewById(R.id.btnLimpiarFiltros)
        recyclerPurgadores = view.findViewById(R.id.recyclerPurgadores)
    }

    private fun setupRecyclerView() {
        adapter = PurgadorAdapter()
        recyclerPurgadores.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@FragmentInspeccionBusqueda.adapter
        }
    }

    private fun setupListeners() {
        btnFiltroEstado.setOnClickListener { mostrarDialogoFiltro("Estado", selectedEstados) { updateEstadosFilter() } }
        btnFiltroArea.setOnClickListener { mostrarDialogoFiltro("Área", selectedAreas) { updateAreasFilter() } }
        btnFiltroUnidad.setOnClickListener { mostrarDialogoFiltro("Unidad", selectedUnidades) { updateUnidadesFilter() } }
        btnFiltroMarca.setOnClickListener { mostrarDialogoFiltro("Marca", selectedMarcas) { updateMarcasFilter() } }
        btnFiltroModelo.setOnClickListener { mostrarDialogoFiltro("Modelo", selectedModelos) { updateModelosFilter() } }
        
        btnLimpiarFiltros.setOnClickListener {
            selectedEstados.clear()
            selectedAreas.clear()
            selectedUnidades.clear()
            selectedMarcas.clear()
            selectedModelos.clear()
            searchView.setQuery("", false)
            filtrarYMostrar()
            actualizarBotonesFiltros()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarYMostrar()
                return true
            }
        })
    }

    private fun mostrarDialogoFiltro(titulo: String, seleccionados: MutableSet<String>, onFilterUpdated: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val opciones = when (titulo) {
                "Estado" -> db.equipoDao().getEstadosUnicos()
                "Área" -> db.equipoDao().getAreasUnicas()
                "Unidad" -> db.equipoDao().getUnidadesUnicas()
                "Marca" -> db.equipoDao().getMarcasUnicas()
                "Modelo" -> db.equipoDao().getModelosUnicos()
                else -> emptyList()
            }

            withContext(Dispatchers.Main) {
                MultiSelectFilterDialog(
                    requireContext(),
                    titulo,
                    opciones,
                    seleccionados
                ) {
                    onFilterUpdated()
                    filtrarYMostrar()
                    actualizarBotonesFiltros()
                }.show()
            }
        }
    }

    private fun cargarDatos() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            equipos = db.equipoDao().getAllEquiposView()
            withContext(Dispatchers.Main) {
                filtrarYMostrar()
            }
        }
    }

    private fun filtrarYMostrar() {
        val texto = searchView.query?.toString()?.lowercase() ?: ""
        val filtrados = equipos.filter { equipo ->
            val matchesSearch = texto.isBlank() ||
                    equipo.id.lowercase().contains(texto) ||
                    equipo.unidad?.lowercase()?.contains(texto) == true ||
                    equipo.area?.lowercase()?.contains(texto) == true ||
                    equipo.marca?.lowercase()?.contains(texto) == true ||
                    equipo.modelo?.lowercase()?.contains(texto) == true ||
                    equipo.estado?.lowercase()?.contains(texto) == true

            val matchesEstado = selectedEstados.isEmpty() || selectedEstados.contains(equipo.estado)
            val matchesArea = selectedAreas.isEmpty() || selectedAreas.contains(equipo.area)
            val matchesUnidad = selectedUnidades.isEmpty() || selectedUnidades.contains(equipo.unidad)
            val matchesMarca = selectedMarcas.isEmpty() || selectedMarcas.contains(equipo.marca)
            val matchesModelo = selectedModelos.isEmpty() || selectedModelos.contains(equipo.modelo)

            matchesSearch && matchesEstado && matchesArea && matchesUnidad && matchesMarca && matchesModelo
        }
        adapter.updateData(filtrados)
    }

    private fun actualizarBotonesFiltros() {
        btnFiltroEstado.text = if (selectedEstados.isEmpty()) "Estado" else "Estado (${selectedEstados.size})"
        btnFiltroArea.text = if (selectedAreas.isEmpty()) "Área" else "Área (${selectedAreas.size})"
        btnFiltroUnidad.text = if (selectedUnidades.isEmpty()) "Unidad" else "Unidad (${selectedUnidades.size})"
        btnFiltroMarca.text = if (selectedMarcas.isEmpty()) "Marca" else "Marca (${selectedMarcas.size})"
        btnFiltroModelo.text = if (selectedModelos.isEmpty()) "Modelo" else "Modelo (${selectedModelos.size})"

        val hasActiveFilters = selectedEstados.isNotEmpty() ||
                selectedAreas.isNotEmpty() ||
                selectedUnidades.isNotEmpty() ||
                selectedMarcas.isNotEmpty() ||
                selectedModelos.isNotEmpty() ||
                searchView.query?.isNotEmpty() == true

        btnLimpiarFiltros.visibility = if (hasActiveFilters) View.VISIBLE else View.GONE
    }

    private fun updateEstadosFilter() {
        // No necesitamos actualizar otros filtros cuando cambia el estado
    }

    private fun updateAreasFilter() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val unidades = db.equipoDao().getUnidadesUnicasPorAreas(selectedAreas.toList())
            withContext(Dispatchers.Main) {
                // Actualizar las unidades disponibles
                selectedUnidades.retainAll(unidades)
                filtrarYMostrar()
            }
        }
    }

    private fun updateUnidadesFilter() {
        // No necesitamos actualizar otros filtros cuando cambia la unidad
    }

    private fun updateMarcasFilter() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val modelos = db.equipoDao().getModelosUnicosPorMarcas(selectedMarcas.toList())
            withContext(Dispatchers.Main) {
                // Actualizar los modelos disponibles
                selectedModelos.retainAll(modelos)
                filtrarYMostrar()
            }
        }
    }

    private fun updateModelosFilter() {
        // No necesitamos actualizar otros filtros cuando cambia el modelo
    }
} 