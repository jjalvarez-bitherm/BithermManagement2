package com.bithermmanagement.ausencias.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.ausencias.databinding.FragmentNuevaAusenciaBinding
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.models.RequestPriority
import com.bithermmanagement.ausencias.viewmodels.AbsenceViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FragmentNuevaAusencia : Fragment() {
    
    private var _binding: FragmentNuevaAusenciaBinding? = null
    private val binding get() = _binding!!
    
    @Inject
    lateinit var absenceViewModel: AbsenceViewModel
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNuevaAusenciaBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos del usuario actual (esto se implementará con el sistema de autenticación)
        loadCurrentUserData()
    }
    
    private fun setupUI() {
        // Configurar spinner de tipo de ausencia
        val absenceTypes = AbsenceType.values().map { it.displayName }
        val absenceTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            absenceTypes
        )
        absenceTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAbsenceType.adapter = absenceTypeAdapter
        
        // Configurar spinner de prioridad
        val priorities = RequestPriority.values().map { it.displayName }
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            priorities
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPriority.adapter = priorityAdapter
        
        // Configurar fecha actual
        val today = Date()
        binding.textViewCurrentDate.text = dateFormat.format(today)
        
        // Configurar campos de fecha
        binding.editTextStartDate.setOnClickListener {
            showStartDatePicker()
        }
        
        binding.editTextEndDate.setOnClickListener {
            showEndDatePicker()
        }
        
        // Configurar validación de fechas
        binding.editTextStartDate.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateDates()
            }
        })
        
        binding.editTextEndDate.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                validateDates()
            }
        })
    }
    
    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            absenceViewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.buttonSubmit.isEnabled = !isLoading
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
                    Toast.makeText(context, "Solicitud creada correctamente", Toast.LENGTH_SHORT).show()
                    absenceViewModel.clearSuccessMessage()
                    clearForm()
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // Botón de envío
        binding.buttonSubmit.setOnClickListener {
            submitRequest()
        }
        
        // Botón de cancelar
        binding.buttonCancel.setOnClickListener {
            navigateBack()
        }
        
        // Botón de limpiar
        binding.buttonClear.setOnClickListener {
            clearForm()
        }
        
        // Botón de calcular días
        binding.buttonCalculateDays.setOnClickListener {
            calculateDays()
        }
    }
    
    private fun loadCurrentUserData() {
        // Esto se implementará con el sistema de autenticación
        // Por ahora, usar datos de ejemplo
        binding.editTextEmployeeName.setText("Juan Pérez")
        binding.editTextEmployeeEmail.setText("juan.perez@empresa.com")
        binding.editTextEmployeeRole.setText("Desarrollador")
    }
    
    private fun showStartDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de inicio")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        
        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedStartDate = Date(selection)
            binding.editTextStartDate.setText(dateFormat.format(selectedStartDate!!))
            validateDates()
        }
        
        datePicker.show(parentFragmentManager, "START_DATE_PICKER")
    }
    
    private fun showEndDatePicker() {
        if (selectedStartDate == null) {
            Toast.makeText(context, "Primero selecciona la fecha de inicio", Toast.LENGTH_SHORT).show()
            return
        }
        
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de fin")
            .setSelection(selectedStartDate!!.time)
            .build()
        
        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedEndDate = Date(selection)
            binding.editTextEndDate.setText(dateFormat.format(selectedEndDate!!))
            validateDates()
        }
        
        datePicker.show(parentFragmentManager, "END_DATE_PICKER")
    }
    
    private fun validateDates() {
        if (selectedStartDate != null && selectedEndDate != null) {
            if (selectedEndDate!!.before(selectedStartDate)) {
                binding.textViewDateError.text = "La fecha de fin no puede ser anterior a la de inicio"
                binding.textViewDateError.visibility = View.VISIBLE
                binding.buttonSubmit.isEnabled = false
            } else {
                binding.textViewDateError.visibility = View.GONE
                binding.buttonSubmit.isEnabled = true
                calculateDays()
            }
        } else {
            binding.buttonSubmit.isEnabled = false
        }
    }
    
    private fun calculateDays() {
        if (selectedStartDate != null && selectedEndDate != null) {
            val diffInMillis = selectedEndDate!!.time - selectedStartDate!!.time
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)
            val totalDays = (diffInDays + 1).toInt() // +1 porque incluimos el día de inicio
            
            binding.textViewTotalDays.text = "$totalDays días"
            binding.textViewTotalDays.visibility = View.VISIBLE
        }
    }
    
    private fun submitRequest() {
        // Validar campos obligatorios
        if (!validateRequiredFields()) {
            return
        }
        
        // Obtener valores de los campos
        val employeeName = binding.editTextEmployeeName.text.toString()
        val employeeEmail = binding.editTextEmployeeEmail.text.toString()
        val employeeRole = binding.editTextEmployeeRole.text.toString()
        val description = binding.editTextDescription.text.toString()
        
        val absenceType = AbsenceType.values()[binding.spinnerAbsenceType.selectedItemPosition]
        val priority = RequestPriority.values()[binding.spinnerPriority.selectedItemPosition]
        
        // Crear la solicitud
        absenceViewModel.createAbsenceRequest(
            employeeId = "current_user_id", // Esto se implementará con el sistema de autenticación
            employeeName = employeeName,
            employeeEmail = employeeEmail,
            employeeRole = employeeRole,
            startDate = selectedStartDate!!,
            endDate = selectedEndDate!!,
            absenceType = absenceType,
            description = description,
            priority = priority
        )
    }
    
    private fun validateRequiredFields(): Boolean {
        var isValid = true
        
        // Validar nombre del empleado
        if (binding.editTextEmployeeName.text.isNullOrBlank()) {
            binding.editTextEmployeeName.error = "El nombre es obligatorio"
            isValid = false
        }
        
        // Validar email del empleado
        if (binding.editTextEmployeeEmail.text.isNullOrBlank()) {
            binding.editTextEmployeeEmail.error = "El email es obligatorio"
            isValid = false
        }
        
        // Validar rol del empleado
        if (binding.editTextEmployeeRole.text.isNullOrBlank()) {
            binding.editTextEmployeeRole.error = "El rol es obligatorio"
            isValid = false
        }
        
        // Validar fecha de inicio
        if (selectedStartDate == null) {
            binding.editTextStartDate.error = "La fecha de inicio es obligatoria"
            isValid = false
        }
        
        // Validar fecha de fin
        if (selectedEndDate == null) {
            binding.editTextEndDate.error = "La fecha de fin es obligatoria"
            isValid = false
        }
        
        // Validar descripción
        if (binding.editTextDescription.text.isNullOrBlank()) {
            binding.editTextDescription.error = "La descripción es obligatoria"
            isValid = false
        }
        
        return isValid
    }
    
    private fun clearForm() {
        binding.apply {
            editTextEmployeeName.text?.clear()
            editTextEmployeeEmail.text?.clear()
            editTextEmployeeRole.text?.clear()
            editTextStartDate.text?.clear()
            editTextEndDate.text?.clear()
            editTextDescription.text?.clear()
            textViewTotalDays.visibility = View.GONE
            textViewDateError.visibility = View.GONE
            spinnerAbsenceType.setSelection(0)
            spinnerPriority.setSelection(1) // Normal
        }
        
        selectedStartDate = null
        selectedEndDate = null
        
        // Recargar datos del usuario
        loadCurrentUserData()
    }
    
    private fun navigateBack() {
        // Navegar de vuelta
        requireActivity().onBackPressed()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
