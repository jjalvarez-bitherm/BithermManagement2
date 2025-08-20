package com.bithermmanagement.ausencias.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ausencias.adapters.AbsenceRequestAdapter
import com.bithermmanagement.ausencias.models.AbsenceRequest
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.models.RequestPriority
import com.bithermmanagement.ausencias.models.RequestStatus
import com.bithermmanagement.ausencias.viewmodels.AbsenceViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentAusenciasGestion : Fragment() {

    private val viewModel: AbsenceViewModel by viewModels()
    
    private lateinit var recyclerViewRequests: RecyclerView
    private lateinit var adapter: AbsenceRequestAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var fabNewAbsence: FloatingActionButton
    
    // Pestañas de filtrado
    private lateinit var tabPendientes: TextView
    private lateinit var tabAprobadas: TextView
    private lateinit var tabRechazadas: TextView
    private lateinit var tabCanceladas: TextView
    private lateinit var tabPasadas: TextView
    private lateinit var indicatorTab: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ausencias_gestion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        viewModel.loadAbsenceRequests()
    }

    private fun initViews(view: View) {
        recyclerViewRequests = view.findViewById(R.id.recyclerViewRequests)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        fabNewAbsence = view.findViewById(R.id.fabNewAbsence)
        
        // Inicializar pestañas
        tabPendientes = view.findViewById(R.id.tabPendientes)
        tabAprobadas = view.findViewById(R.id.tabAprobadas)
        tabRechazadas = view.findViewById(R.id.tabRechazadas)
        tabCanceladas = view.findViewById(R.id.tabCanceladas)
        tabPasadas = view.findViewById(R.id.tabPasadas)
        indicatorTab = view.findViewById(R.id.indicatorTab)
    }

    private fun setupRecyclerView() {
        adapter = AbsenceRequestAdapter(
            onApprove = { request -> viewModel.approveAbsenceRequest(request) },
            onReject = { request -> viewModel.rejectAbsenceRequest(request) },
            onCancel = { request -> viewModel.cancelAbsenceRequest(request) },
            onEdit = { request -> viewModel.editAbsenceRequest(request) }
        )
        
        recyclerViewRequests.layoutManager = LinearLayoutManager(context)
        recyclerViewRequests.adapter = adapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.requests.collect { requests ->
                adapter.updateData(requests)
                updateEmptyState(requests.isEmpty())
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    // Mostrar error al usuario
                }
            }
        }
    }

    private fun setupClickListeners() {
        fabNewAbsence.setOnClickListener {
            // Navegar al fragment de nueva ausencia
            // TODO: Implementar navegación
        }
        
        // Configurar pestañas de filtrado
        tabPendientes.setOnClickListener {
            selectTab(tabPendientes, 0)
            viewModel.setStatusFilter(listOf(RequestStatus.PENDIENTE))
        }
        
        tabAprobadas.setOnClickListener {
            selectTab(tabAprobadas, 1)
            viewModel.setStatusFilter(listOf(RequestStatus.APROBADA))
        }
        
        tabRechazadas.setOnClickListener {
            selectTab(tabRechazadas, 2)
            viewModel.setStatusFilter(listOf(RequestStatus.RECHAZADA))
        }
        
        tabCanceladas.setOnClickListener {
            selectTab(tabCanceladas, 3)
            viewModel.setStatusFilter(listOf(RequestStatus.CANCELADA))
        }
        
        tabPasadas.setOnClickListener {
            selectTab(tabPasadas, 4)
            viewModel.setStatusFilter(listOf(RequestStatus.PASADA))
        }
        
        // Seleccionar pestaña por defecto
        selectTab(tabPendientes, 0)
    }
    
    private fun selectTab(selectedTab: TextView, position: Int) {
        // Resetear todas las pestañas
        val allTabs = listOf(tabPendientes, tabAprobadas, tabRechazadas, tabCanceladas, tabPasadas)
        allTabs.forEach { tab ->
            tab.setBackgroundColor(resources.getColor(R.color.gray_800, null))
        }
        
        // Resaltar pestaña seleccionada
        selectedTab.setBackgroundColor(resources.getColor(R.color.purple_600, null))
        
        // Mover indicador
        val tabWidth = selectedTab.width
        indicatorTab.layoutParams = indicatorTab.layoutParams.apply {
            width = tabWidth
        }
        indicatorTab.x = selectedTab.x
    }

    private fun clearAllFilters() {
        // Resetear a la pestaña por defecto
        selectTab(tabPendientes, 0)
        viewModel.clearAllFilters()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyStateContainer.visibility = View.VISIBLE
            recyclerViewRequests.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            recyclerViewRequests.visibility = View.VISIBLE
        }
    }
}
