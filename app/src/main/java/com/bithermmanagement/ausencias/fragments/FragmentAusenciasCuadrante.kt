package com.bithermmanagement.ausencias.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bithermmanagement.ausencias.adapters.AbsenceCalendarAdapter
import com.bithermmanagement.ausencias.databinding.FragmentAusenciasCuadranteBinding
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.viewmodels.AbsenceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentAusenciasCuadrante : Fragment() {
    
    private var _binding: FragmentAusenciasCuadranteBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var absenceViewModel: AbsenceViewModel
    
    private lateinit var calendarAdapter: AbsenceCalendarAdapter
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAusenciasCuadranteBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        loadInitialData()
    }
    
    private fun setupUI() {
        // Configurar RecyclerView del calendario
        calendarAdapter = AbsenceCalendarAdapter { date ->
            onDateSelected(date)
        }
        
        binding.recyclerViewCalendar.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = calendarAdapter
        }
        
        // Configurar contadores
        updateCounters()
        
        // Configurar fecha actual
        val today = Date()
        binding.textViewCurrentDate.text = dateFormat.format(today)
        binding.textViewCurrentMonth.text = monthFormat.format(today)
        
        // Seleccionar fecha actual
        absenceViewModel.setSelectedDate(today)
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.absences.collectLatest { absences ->
                calendarAdapter.updateAbsences(absences)
                updateCounters()
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.errorMessage.collectLatest { error ->
                error?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    absenceViewModel.clearError()
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.uiState.collectLatest { uiState ->
                if (uiState.showSuccessMessage) {
                    Toast.makeText(context, uiState.successMessage, Toast.LENGTH_SHORT).show()
                    absenceViewModel.clearSuccessMessage()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // Botón flotante para nueva ausencia
        binding.fabNewAbsence.setOnClickListener {
            navigateToNewAbsence()
        }
        
        // Selector de fecha
        binding.buttonSelectDate.setOnClickListener {
            showDatePicker()
        }
        
        // Navegación del calendario
        binding.buttonPreviousMonth.setOnClickListener {
            navigateMonth(-1)
        }
        
        binding.buttonNextMonth.setOnClickListener {
            navigateMonth(1)
        }
        
        // Botones de filtro rápido
        binding.chipToday.setOnClickListener {
            val today = Date()
            absenceViewModel.setSelectedDate(today)
            binding.textViewCurrentDate.text = dateFormat.format(today)
        }
        
        binding.chipThisWeek.setOnClickListener {
            val calendar = Calendar.getInstance()
            val startOfWeek = calendar.apply {
                set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val endOfWeek = calendar.apply {
                add(Calendar.DAY_OF_WEEK, 6)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.time
            
            absenceViewModel.setDateRange(startOfWeek, endOfWeek)
            binding.textViewCurrentDate.text = "${dateFormat.format(startOfWeek)} - ${dateFormat.format(endOfWeek)}"
        }
        
        binding.chipThisMonth.setOnClickListener {
            val calendar = Calendar.getInstance()
            val startOfMonth = calendar.apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            val endOfMonth = calendar.apply {
                set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.time
            
            absenceViewModel.setDateRange(startOfMonth, endOfMonth)
            binding.textViewCurrentDate.text = "${dateFormat.format(startOfMonth)} - ${dateFormat.format(endOfMonth)}"
        }
    }
    
    private fun loadInitialData() {
        val today = Date()
        absenceViewModel.loadAbsencesForDate(today)
        absenceViewModel.loadStatistics()
    }
    
    private fun onDateSelected(date: Date) {
        absenceViewModel.setSelectedDate(date)
        binding.textViewCurrentDate.text = dateFormat.format(date)
        
        // Actualizar mes si es diferente
        val calendar = Calendar.getInstance()
        calendar.time = date
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.time = date
        val selectedMonth = selectedCalendar.get(Calendar.MONTH)
        val selectedYear = selectedCalendar.get(Calendar.YEAR)
        
        if (currentMonth != selectedMonth || currentYear != selectedYear) {
            binding.textViewCurrentMonth.text = monthFormat.format(date)
        }
    }
    
    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        
        datePicker.addOnPositiveButtonClickListener { selection ->
            val date = Date(selection)
            onDateSelected(date)
        }
        
        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }
    
    private fun navigateMonth(direction: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = absenceViewModel.selectedDate.value
        calendar.add(Calendar.MONTH, direction)
        
        val newDate = calendar.time
        absenceViewModel.setSelectedDate(newDate)
        binding.textViewCurrentMonth.text = monthFormat.format(newDate)
        binding.textViewCurrentDate.text = dateFormat.format(newDate)
    }
    
    private fun updateCounters() {
        val absences = absenceViewModel.absences.value
        
        // Contador total de ausencias
        binding.textViewTotalAbsences.text = absences.size.toString()
        
        // Contador por tipo
        val typeCounts = absences.groupBy { it.absenceType }
        binding.textViewVacacionesCount.text = (typeCounts[AbsenceType.VACACIONES]?.size ?: 0).toString()
        binding.textViewPermisosCount.text = (typeCounts[AbsenceType.PERMISO]?.size ?: 0).toString()
        binding.textViewEnfermedadCount.text = (typeCounts[AbsenceType.ENFERMEDAD]?.size ?: 0).toString()
        
        // Contador por estado
        val statusCounts = absences.groupBy { it.status }
        binding.textViewPendientesCount.text = (statusCounts[AbsenceStatus.PENDIENTE]?.size ?: 0).toString()
        binding.textViewAprobadasCount.text = (statusCounts[AbsenceStatus.APROBADA]?.size ?: 0).toString()
        binding.textViewRechazadasCount.text = (statusCounts[AbsenceStatus.RECHAZADA]?.size ?: 0).toString()
    }
    
    private fun navigateToNewAbsence() {
        // Navegar al fragmento de nueva ausencia
        // Esto se implementará cuando creemos la navegación
        Toast.makeText(context, "Nueva ausencia", Toast.LENGTH_SHORT).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
