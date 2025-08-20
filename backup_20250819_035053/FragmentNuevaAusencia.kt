package com.bithermmanagement.ausencias.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bithermmanagement.R
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.viewmodels.AbsenceViewModel
import com.bithermmanagement.core.utils.VariablesManager
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FragmentNuevaAusencia : Fragment() {

    private val viewModel: AbsenceViewModel by viewModels()
    
    @Inject
    lateinit var variablesManager: VariablesManager
    
    @Inject
    lateinit var loginRepository: com.bithermmanagement.core.data.LoginRepository
    
    private lateinit var editTextEmployeeName: TextInputEditText
    private lateinit var editTextRequestDate: TextInputEditText
    private lateinit var spinnerAbsenceType: AutoCompleteTextView
    private lateinit var editTextStartDate: TextInputEditText
    private lateinit var editTextEndDate: TextInputEditText
    private lateinit var editTextWorkingDays: TextInputEditText
    private lateinit var editTextDescription: TextInputEditText
    private lateinit var buttonCancel: Button
    private lateinit var buttonSubmit: Button
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val requestDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private var startDate: Date? = null
    private var endDate: Date? = null
    private var absenceType: AbsenceType = AbsenceType.VACACIONES

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nueva_ausencia, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        android.util.Log.d("FragmentNuevaAusencia", "=== onViewCreated INICIADO ===")
        
        initViews(view)
        setupInitialData()
        setupAbsenceTypeSpinner()
        setupDatePickers()
        setupClickListeners()
        
        android.util.Log.d("FragmentNuevaAusencia", "=== onViewCreated COMPLETADO ===")
    }

    override fun onResume() {
        super.onResume()
        try {
            val user = variablesManager.currentUser
            if (this::editTextEmployeeName.isInitialized && (editTextEmployeeName.text.isNullOrBlank() || editTextEmployeeName.text.toString() == "Usuario no identificado")) {
                if (user != null) {
                    editTextEmployeeName.setText("${user.nombre} ${user.apellidos}")
                    android.util.Log.d("FragmentNuevaAusencia", "onResume: usuario rellenado ${user.nombre} ${user.apellidos}")
                } else {
                    android.util.Log.w("FragmentNuevaAusencia", "onResume: currentUser sigue null")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FragmentNuevaAusencia", "onResume error: ${e.message}", e)
        }
    }

    private fun initViews(view: View) {
        editTextEmployeeName = view.findViewById(R.id.editTextEmployeeName)
        editTextRequestDate = view.findViewById(R.id.editTextRequestDate)
        spinnerAbsenceType = view.findViewById(R.id.spinnerAbsenceType)
        editTextStartDate = view.findViewById(R.id.editTextStartDate)
        editTextEndDate = view.findViewById(R.id.editTextEndDate)
        editTextWorkingDays = view.findViewById(R.id.editTextWorkingDays)
        editTextDescription = view.findViewById(R.id.editTextDescription)
        buttonCancel = view.findViewById(R.id.buttonCancel)
        buttonSubmit = view.findViewById(R.id.buttonSubmit)
        
        // Forzar que los hints sean visibles
        forceHintsVisible()
    }
    
    private fun forceHintsVisible() {
        // Forzar que los hints se muestren siempre
        editTextStartDate.post {
            editTextStartDate.requestFocus()
            editTextStartDate.clearFocus()
        }
        
        editTextEndDate.post {
            editTextEndDate.requestFocus()
            editTextEndDate.clearFocus()
        }
        
        editTextDescription.post {
            editTextDescription.requestFocus()
            editTextDescription.clearFocus()
        }
    }

    private fun setupInitialData() {
        android.util.Log.d("FragmentNuevaAusencia", "=== setupInitialData INICIADO ===")
        
        // Configurar fecha de solicitud
        editTextRequestDate.setText(requestDateFormat.format(Date()))
        
        // Configurar motivo por defecto (vacío para que el usuario lo complete)
        editTextDescription.setText("")
        
        // Intentar obtener el usuario
        setupUserData()
        
        android.util.Log.d("FragmentNuevaAusencia", "=== setupInitialData COMPLETADO ===")
    }
    
    private fun setupUserData() {
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        val username = prefs.getString("username", null)

        // Primero intentar desde VariablesManager
        var currentUser = variablesManager.currentUser
        android.util.Log.d("FragmentNuevaAusencia", "VariablesManager.currentUser: $currentUser, usernameSP=$username")

        if (currentUser == null && !username.isNullOrBlank()) {
            // Intentar obtener desde Room por APP
            try {
                val db = com.bithermmanagement.database.AppDatabase.getDatabase(requireContext())
                val userEntity = kotlinx.coroutines.runBlocking { db.userDao().getUserByApp(username) }
                if (userEntity != null) {
                    currentUser = com.bithermmanagement.core.data.UserData(
                        codigo = userEntity.cod,
                        nombre = userEntity.nombre,
                        apellidos = userEntity.apellidos,
                        dni = userEntity.dni,
                        fechaNacimiento = userEntity.fechaNacimiento?.let { dateFormat.format(it) } ?: "",
                        app = userEntity.app,
                        pass = userEntity.password,
                        rol = userEntity.rol,
                        rolPound = 0,
                        swWeb = userEntity.swWeb ?: "",
                        equipoAsignado = userEntity.equipoAsignado ?: "",
                        fechaCalibracion = userEntity.fechaCalibracion?.let { dateFormat.format(it) } ?: "",
                        telefonoEmpresa = userEntity.telefonoEmpresa ?: "",
                        emailEmpresa = userEntity.emailEmpresa ?: "",
                        fechaAltaEmpresa = userEntity.altaEmpresa?.let { dateFormat.format(it) } ?: "",
                        telefonoPersonal = userEntity.telefonoPersonal ?: "",
                        emailPersonal = userEntity.emailPersonal ?: "",
                        categoria = userEntity.categoria ?: "",
                        revisionMedica = userEntity.rMedico.toString(),
                        accesoRLR = userEntity.accesoRLR,
                        supervisorEjecutivo = userEntity.supEjec,
                        apodo = ""
                    )
                    variablesManager.currentUser = currentUser
                    android.util.Log.d("FragmentNuevaAusencia", "Usuario cargado desde Room por APP: ${currentUser?.nombre} ${currentUser?.apellidos}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FragmentNuevaAusencia", "Error cargando usuario desde Room: ${e.message}", e)
            }
        }

        if (currentUser != null) {
            editTextEmployeeName.setText("${currentUser.nombre} ${currentUser.apellidos}")
        } else {
            editTextEmployeeName.setText("Usuario no identificado")
        }
    }
    
    private fun setupAbsenceTypeSpinner() {
        // Lista completa de tipos de ausencia
        val absenceTypes = listOf(
            "Vacaciones",
            "Permiso Retribuido", 
            "Baja por Enfermedad",
            "Permiso No Retribuido",
            "Formación",
            "Otros"
        )
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, absenceTypes)
        spinnerAbsenceType.setAdapter(adapter)
        
        // Seleccionar el tipo por defecto (Vacaciones)
        spinnerAbsenceType.setText("Vacaciones", false)
        
        // Configurar para mostrar dropdown al hacer click
        spinnerAbsenceType.setOnClickListener {
            spinnerAbsenceType.showDropDown()
        }
        
        // Configurar listener para cambiar el motivo por defecto según el tipo
        spinnerAbsenceType.setOnItemClickListener { _, _, position, _ ->
            val selectedType = absenceTypes[position]
            val defaultDescription = when (selectedType) {
                "Vacaciones" -> "Vacaciones"
                "Permiso Retribuido" -> "Permiso retribuido"
                "Baja por Enfermedad" -> "Baja médica"
                "Permiso No Retribuido" -> "Permiso no retribuido"
                "Formación" -> "Formación"
                else -> ""
            }
            editTextDescription.setText(defaultDescription)
        }
    }

    private fun setupDatePickers() {
        val tilStart = view?.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilStartDate)
        val tilEnd = view?.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilEndDate)

        editTextStartDate.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                editTextStartDate.setText(dateFormat.format(date))
                tilStart?.isHintEnabled = true
                calculateWorkingDays()
            }
        }

        editTextEndDate.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                editTextEndDate.setText(dateFormat.format(date))
                tilEnd?.isHintEnabled = true
                calculateWorkingDays()
            }
        }
    }
    
    private fun calculateWorkingDays() {
        if (startDate != null && endDate != null) {
            viewModel.calculateWorkingDays(startDate!!, endDate!!) { workingDays ->
                editTextWorkingDays.setText(workingDays.toString())
            }
        }
    }

    private fun setupClickListeners() {
        buttonCancel.setOnClickListener {
            // Volver al fragmento anterior
            requireActivity().onBackPressed()
        }
        
        buttonSubmit.setOnClickListener {
            submitAbsenceRequest()
        }
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }.time
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }

    private fun submitAbsenceRequest() {
        // Validar campos
        if (!validateFields()) {
            return
        }
        
        // Obtener valores
        val selectedType = spinnerAbsenceType.text.toString()
        val description = editTextDescription.text.toString()
        val workingDays = editTextWorkingDays.text.toString().toIntOrNull() ?: 0
        
        // Obtener información del usuario actual
        val currentUser = variablesManager.currentUser
        
        // Mapear el tipo seleccionado al enum AbsenceType
        val absenceType = when (selectedType) {
            "Vacaciones" -> AbsenceType.VACACIONES
            "Permiso Retribuido" -> AbsenceType.PERMISO
            "Baja por Enfermedad" -> AbsenceType.ENFERMEDAD
            "Permiso No Retribuido" -> AbsenceType.PERMISO
            "Formación" -> AbsenceType.OTROS
            else -> AbsenceType.OTROS
        }
        
        // Crear solicitud
        viewModel.createAbsenceRequest(
            employeeId = currentUser?.app ?: "unknown",
            employeeName = editTextEmployeeName.text.toString(),
            employeeEmail = currentUser?.emailEmpresa ?: "unknown@empresa.com",
            employeeRole = currentUser?.rol ?: "Empleado",
            startDate = startDate!!,
            endDate = endDate!!,
            absenceType = absenceType,
            description = description
        )

        // Evitar cerrar inmediatamente: esperar confirmación desde el ViewModel para no cancelar la corrutina
        buttonSubmit.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            // Esperar a que el ViewModel reporte un ID creado
            viewModel.uiState.first { it.lastCreatedRequestId > 0L }
            Toast.makeText(requireContext(), "Solicitud enviada correctamente", Toast.LENGTH_SHORT).show()
            buttonSubmit.isEnabled = true
            requireActivity().onBackPressed()
        }
    }

    private fun validateFields(): Boolean {
        if (startDate == null) {
            editTextStartDate.error = "Seleccione fecha de inicio"
            return false
        }
        
        if (endDate == null) {
            editTextEndDate.error = "Seleccione fecha de fin"
            return false
        }
        
        if (startDate!!.after(endDate!!)) {
            editTextEndDate.error = "La fecha de fin debe ser posterior a la de inicio"
            return false
        }
        
        val workingDays = editTextWorkingDays.text.toString().toIntOrNull() ?: 0
        if (workingDays <= 0) {
            editTextWorkingDays.error = "Debe haber al menos un día laborable"
            return false
        }
        
        if (editTextDescription.text.isNullOrBlank()) {
            editTextDescription.error = "Ingrese un motivo de solicitud"
            return false
        }
        
        return true
    }
}
