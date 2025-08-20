package com.bithermmanagement.ausencias.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.ausencias.adapters.AbsenceCalendarAdapter
import com.bithermmanagement.ausencias.adapters.AbsenceListAdapter
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceStatus
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.viewmodels.AbsenceViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.appcompat.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import java.util.Calendar
import com.bithermmanagement.database.entities.UserEntity
import com.bithermmanagement.data.UserManager
import android.util.Log
import android.widget.Button
import javax.inject.Inject
import com.bithermmanagement.ui.dialogs.MultiSelectFilterDialog

@AndroidEntryPoint
class FragmentAusenciasCuadrante : Fragment() {

    private val viewModel: AbsenceViewModel by viewModels()
    
    @Inject
    lateinit var userManager: UserManager
    
    private lateinit var recyclerViewCalendar: RecyclerView
    private lateinit var adapter: AbsenceCalendarAdapter
    private lateinit var recyclerViewAbsences: RecyclerView
    private lateinit var absenceListAdapter: AbsenceListAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewCurrentMonth: TextView
    private lateinit var buttonPreviousMonth: ImageButton
    private lateinit var buttonNextMonth: ImageButton
    private lateinit var buttonUserFilter: Button
    private lateinit var fabNewAbsence: FloatingActionButton
    
    private var selectedUserIds: MutableSet<String> = mutableSetOf()
    private var allUsers: List<UserEntity> = emptyList()

    private val dateFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
    private var currentMonth = Calendar.getInstance().apply {
        set(2025, Calendar.APRIL, 1) // Forzar abril 2025
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ausencias_cuadrante, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // Cargar datos iniciales
        loadCalendarData()
    }

    private fun initViews(view: View) {
        recyclerViewCalendar = view.findViewById(R.id.recyclerViewCalendar)
        recyclerViewAbsences = view.findViewById(R.id.recyclerViewAbsences)
        progressBar = view.findViewById(R.id.progressBar)
        textViewCurrentMonth = view.findViewById(R.id.textViewCurrentMonth)
        buttonPreviousMonth = view.findViewById(R.id.buttonPreviousMonth)
        buttonNextMonth = view.findViewById(R.id.buttonNextMonth)
        buttonUserFilter = view.findViewById(R.id.buttonUserFilter)
        fabNewAbsence = view.findViewById(R.id.fabNewAbsence)
    }

    private fun setupRecyclerView() {
        // Configurar calendario
        adapter = AbsenceCalendarAdapter(
            onDayClick = { day ->
                // Mostrar popup con información de ausencias
                showAbsencesPopup(day)
            }
        )
        
        recyclerViewCalendar.layoutManager = GridLayoutManager(context, 7)
        recyclerViewCalendar.adapter = adapter
        
        // Configurar lista de ausencias
        absenceListAdapter = AbsenceListAdapter()
        recyclerViewAbsences.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        recyclerViewAbsences.adapter = absenceListAdapter
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.absences.collect { absences ->
                updateCalendar(absences)
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                error?.let {
                    // Mostrar error al usuario
                    AlertDialog.Builder(requireContext())
                        .setTitle("Error")
                        .setMessage(it)
                        .setPositiveButton("OK") { _, _ ->
                            viewModel.clearError()
                        }
                        .show()
                }
            }
        }
        
        // Observer para sincronización
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    // Mostrar indicador de progreso
                    progressBar.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun setupClickListeners() {
        buttonPreviousMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, -1)
            loadCalendarData()
        }
        
        buttonNextMonth.setOnClickListener {
            currentMonth.add(Calendar.MONTH, 1)
            loadCalendarData()
        }
        
        buttonUserFilter.setOnClickListener {
            if (userManager.hasAdminPrivileges()) {
                showUserFilterDialog()
            }
            // Para usuarios normales, no hacer nada (botón deshabilitado)
        }
        
        // Configurar FAB según el rol del usuario
        if (userManager.hasAdminPrivileges()) {
            // Para admin/superadmin: mostrar popup con opciones
            fabNewAbsence.setOnClickListener {
                showAdminFabMenu()
            }
        } else {
            // Para usuarios normales (VIEWER): abrir directamente el formulario
            fabNewAbsence.setOnClickListener {
                navigateToNewAbsence(AbsenceType.VACACIONES)
            }
        }
    }
    
    private fun showAdminFabMenu() {
        val popupMenu = PopupMenu(requireContext(), fabNewAbsence)
        popupMenu.menuInflater.inflate(R.menu.fab_menu_admin, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_leer_datos -> {
                    val hasAdminPrivileges = userManager.hasAdminPrivileges()
                    if (hasAdminPrivileges) {
                        leerDatosDeGoogleSheets()
                    }
                    true
                }
                R.id.action_nueva_solicitud_ausencia -> {
                    // Navegar al fragment de nueva solicitud unificado
                    navigateToNewAbsence(AbsenceType.VACACIONES)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    private fun leerDatosDeGoogleSheets() {
        // Mostrar diálogo de confirmación
        AlertDialog.Builder(requireContext())
            .setTitle("Leer datos de Google Sheets")
            .setMessage("¿Desea leer los datos de AUSENCIAS-CUADRO y transferirlos a AUSENCIAS-LOGS?")
            .setPositiveButton("Sí") { _, _ ->
                performDataTransfer()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun performDataTransfer() {
        // Mostrar diálogo de progreso
        val progressDialog = AlertDialog.Builder(requireContext())
            .setTitle("Transferencia en progreso")
            .setMessage("Leyendo datos de Google Sheets...")
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Llamar al método de sincronización del ViewModel
                viewModel.syncWithGoogleSheets()
                
                                // Esperar a que termine la sincronización (solo una vez)
                val isLoading = viewModel.isLoading.first { !it }
                Log.d("FragmentAusenciasCuadrante", "Sincronización completada, isLoading = $isLoading")
                
                progressDialog.dismiss()
                
                // Verificar si hubo error
                val error = viewModel.errorMessage.value
                if (error != null) {
                    Log.d("FragmentAusenciasCuadrante", "Mostrando diálogo de error")
                    val errorDialog = AlertDialog.Builder(requireContext())
                        .setTitle("Error en la transferencia")
                        .setMessage(error)
                        .setPositiveButton("OK") { dialog, _ ->
                            Log.d("FragmentAusenciasCuadrante", "Botón OK pulsado en error - cerrando diálogo")
                            viewModel.clearError()
                            dialog.dismiss()
                            Log.d("FragmentAusenciasCuadrante", "Diálogo de error cerrado exitosamente")
                        }
                        .create()
                    
                    errorDialog.setOnDismissListener {
                        Log.d("FragmentAusenciasCuadrante", "Diálogo de error descartado/destruido")
                    }
                    
                    errorDialog.show()
                    Log.d("FragmentAusenciasCuadrante", "Diálogo de error mostrado")
                } else {
                    // Éxito
                    Log.d("FragmentAusenciasCuadrante", "Mostrando diálogo de éxito")
                    val successDialog = AlertDialog.Builder(requireContext())
                        .setTitle("Transferencia completada")
                        .setMessage("Los datos han sido transferidos correctamente desde AUSENCIAS-CUADRO a AUSENCIAS-LOGS.")
                        .setPositiveButton("OK") { dialog, _ ->
                            Log.d("FragmentAusenciasCuadrante", "Botón OK pulsado - cerrando diálogo")
                            dialog.dismiss()
                            Log.d("FragmentAusenciasCuadrante", "Diálogo cerrado exitosamente")
                        }
                        .create()
                    
                    successDialog.setOnDismissListener {
                        Log.d("FragmentAusenciasCuadrante", "Diálogo descartado/destruido")
                    }
                    
                    successDialog.show()
                    Log.d("FragmentAusenciasCuadrante", "Diálogo de éxito mostrado")
                    
                    // Recargar datos del calendario
                    loadCalendarData()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.d("FragmentAusenciasCuadrante", "Mostrando diálogo de error general")
                val generalErrorDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Error al transferir datos: ${e.message}")
                    .setPositiveButton("OK") { dialog, _ ->
                        Log.d("FragmentAusenciasCuadrante", "Botón OK pulsado en error general - cerrando diálogo")
                        dialog.dismiss()
                        Log.d("FragmentAusenciasCuadrante", "Diálogo de error general cerrado exitosamente")
                    }
                    .create()
                
                generalErrorDialog.setOnDismissListener {
                    Log.d("FragmentAusenciasCuadrante", "Diálogo de error general descartado/destruido")
                }
                
                generalErrorDialog.show()
                Log.d("FragmentAusenciasCuadrante", "Diálogo de error general mostrado")
            }
        }
    }
    
    private fun navigateToNewAbsence(absenceType: AbsenceType) {
        // Crear y mostrar el fragmento de nueva ausencia (formulario unificado)
        val fragment = FragmentNuevaAusencia()
        
        // Reemplazar el fragmento actual
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    private fun getCurrentUser(): com.bithermmanagement.core.data.UserData? {
        // Obtener usuario desde VariablesManager
        val variablesManager = com.bithermmanagement.core.utils.VariablesManager(requireContext())
        return variablesManager.currentUser
    }

    private fun loadCalendarData() {
        Log.d("FragmentAusenciasCuadrante", "=== INICIANDO CARGA DE DATOS DEL CALENDARIO ===")
        
        // Mostrar loading
        progressBar.visibility = View.VISIBLE
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("FragmentAusenciasCuadrante", "Cargando ausencias para el mes actual")
                
                // Cargar ausencias del mes actual
                val year = currentMonth.get(Calendar.YEAR)
                val month = currentMonth.get(Calendar.MONTH)
                viewModel.loadAbsencesByMonth(year, month)
                
                // Observar cambios en las ausencias
                viewModel.absences.collect { absences ->
                    Log.d("FragmentAusenciasCuadrante", "Ausencias recibidas: ${absences.size}")
                    
                    // Ocultar loading
                    progressBar.visibility = View.GONE
                    
                    // Actualizar la UI con los datos
                    updateCalendar(absences)
                }
                
            } catch (e: Exception) {
                Log.e("FragmentAusenciasCuadrante", "Error cargando datos: ${e.message}", e)
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun updateCalendar(absences: List<AbsenceRecord>) {
        Log.d("FragmentAusenciasCuadrante", "=== INICIANDO UPDATE CALENDAR SIMPLIFICADO ===")
        Log.d("FragmentAusenciasCuadrante", "Ausencias recibidas: ${absences.size}")
        
        val year = currentMonth.get(Calendar.YEAR)
        val month = currentMonth.get(Calendar.MONTH)
        Log.d("FragmentAusenciasCuadrante", "Año: $year, Mes: $month")
        
        // Actualizar el mes mostrado
        textViewCurrentMonth.text = dateFormat.format(currentMonth.time)
        
        val calendarDays = adapter.generateCalendarDays(year, month)
        Log.d("FragmentAusenciasCuadrante", "Días del calendario generados: ${calendarDays.size}")
        
        // Cargar festivos para este mes
        loadHolidaysForMonth(year, month)
        
        // Configurar botón de filtro según el rol
        if (userManager.hasAdminPrivileges()) {
            buttonUserFilter.visibility = View.VISIBLE
            buttonUserFilter.isEnabled = true
            // Cargar usuarios si es admin y no se han cargado aún
            if (allUsers.isEmpty()) {
                loadUsersFromAllAbsences()
            }
        } else {
            // Para usuarios normales: deshabilitar filtro y mostrar solo sus ausencias
            buttonUserFilter.visibility = View.VISIBLE
            buttonUserFilter.isEnabled = false
            
            // Obtener usuario actual y mostrar su nombre en el botón
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                buttonUserFilter.text = currentUser.app
                selectedUserIds = mutableSetOf(currentUser.app)
            } else {
                buttonUserFilter.text = "Mi usuario"
            }
        }
        
        // Filtrar ausencias según el rol del usuario (usando ID canónico)
        val filteredAbsences = if (userManager.hasAdminPrivileges()) {
            // Para admins: mostrar ausencias de usuarios seleccionados o todas si no hay filtro
            if (selectedUserIds.isNotEmpty()) {
                absences.filter { absence -> 
                    selectedUserIds.contains(canonicalEmployeeId(absence.employeeId, absence.employeeName)) 
                }
            } else {
                absences
            }
        } else {
            // Para usuarios normales: mostrar solo sus propias ausencias
            val currentUser = getCurrentUser()
            if (currentUser != null) {
                absences.filter { absence -> 
                    canonicalEmployeeId(absence.employeeId, absence.employeeName) == currentUser.app 
                }
            } else {
                emptyList()
            }
        }
        
        Log.d("FragmentAusenciasCuadrante", "Ausencias filtradas: ${filteredAbsences.size}")
        
        // Eliminar duplicados usando ID canónico para los cards (igual que en puntos y popup)
        val dedupedAbsences = filteredAbsences.distinctBy { 
            "${canonicalEmployeeId(it.employeeId, it.employeeName)}-${it.absenceType}-${it.startDate.time}-${it.endDate.time}"
        }
        
        Log.d("FragmentAusenciasCuadrante", "Ausencias desduplicadas para cards: ${dedupedAbsences.size}")
        
        // Log detallado de las ausencias que se pasan al adaptador
        Log.d("FragmentAusenciasCuadrante", "=== PASANDO AUSENCIAS AL ADAPTADOR ===")
        dedupedAbsences.forEachIndexed { index, absence ->
            val hash = System.identityHashCode(absence)
            Log.d("FragmentAusenciasCuadrante", "ADAPTADOR - Ausencia $index [Hash: $hash]: employeeId='${absence.employeeId}', employeeName='${absence.employeeName}', tipo='${absence.absenceType}', fechas='${absence.startDate}' a '${absence.endDate}'")
        }
        Log.d("FragmentAusenciasCuadrante", "=== FIN AUSENCIAS AL ADAPTADOR ===")
        
        // Actualizar el calendario con las ausencias deduplicadas (para mostrar puntos únicos)
        adapter.updateData(calendarDays, dedupedAbsences)
        
        // Actualizar la lista de ausencias con las filtradas y desduplicadas
        absenceListAdapter.updateAbsences(dedupedAbsences)
        
        Log.d("FragmentAusenciasCuadrante", "=== FIN UPDATE CALENDAR SIMPLIFICADO ===")
    }
    
    private fun loadUsersFromAbsences(absences: List<AbsenceRecord>) {
        Log.d("FragmentAusenciasCuadrante", "Procesando ${absences.size} ausencias para extraer usuarios")
        
        val uniqueUsers = absences
            .groupBy { canonicalEmployeeId(it.employeeId, it.employeeName) }
            .map { (canonicalId, absences) ->
                val firstAbsence = absences.first()
                Log.d("FragmentAusenciasCuadrante", "Creando usuario: $canonicalId - ${firstAbsence.employeeName}")
                UserEntity(
                    cod = canonicalId,
                    nombre = firstAbsence.employeeName.split(" ").firstOrNull() ?: "",
                    apellidos = firstAbsence.employeeName.split(" ").drop(1).joinToString(" "),
                    dni = "",
                    fechaNacimiento = null,
                    app = canonicalId, // Usar ID canónico como app
                    password = "",
                    rol = "USER",
                    swWeb = "S",
                    equipoAsignado = null,
                    fechaCalibracion = null,
                    telefonoEmpresa = null,
                    emailEmpresa = firstAbsence.employeeEmail,
                    altaEmpresa = null,
                    telefonoPersonal = null,
                    emailPersonal = null,
                    categoria = null,
                    rMedico = false,
                    accesoRLR = false,
                    supEjec = false
                )
            }
        
        allUsers = uniqueUsers
        Log.d("FragmentAusenciasCuadrante", "Usuarios únicos encontrados: ${allUsers.size}")
        
        // Por defecto, seleccionar todos los usuarios
        selectedUserIds.clear()
        selectedUserIds.addAll(allUsers.map { it.cod })
        Log.d("FragmentAusenciasCuadrante", "Usuarios seleccionados: ${selectedUserIds.size}")
    }
    
    private fun loadUsersFromAllAbsences() {
        Log.d("FragmentAusenciasCuadrante", "=== INICIANDO CARGA DE USUARIOS ===")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("FragmentAusenciasCuadrante", "Llamando a loadAllAbsencesFromLogs()")
                // Cargar todas las ausencias de AUSENCIAS-LOGS (sin filtro de mes)
                val allAbsences = viewModel.loadAllAbsencesFromLogs()
                Log.d("FragmentAusenciasCuadrante", "Ausencias cargadas: ${allAbsences.size}")
                
                if (allAbsences.isNotEmpty()) {
                    Log.d("FragmentAusenciasCuadrante", "Primera ausencia: ${allAbsences.first().employeeId} - ${allAbsences.first().employeeName}")
                    loadUsersFromAbsences(allAbsences)
                    Log.d("FragmentAusenciasCuadrante", "Usuarios cargados: ${allUsers.size}")
                    Log.d("FragmentAusenciasCuadrante", "Usuarios: ${allUsers.map { it.app }}")
                } else {
                    Log.w("FragmentAusenciasCuadrante", "No se encontraron ausencias para cargar usuarios")
                }
            } catch (e: Exception) {
                Log.e("FragmentAusenciasCuadrante", "Error cargando usuarios: ${e.message}", e)
            }
        }
    }
    
    private fun showUserFilterDialog() {
        Log.d("FragmentAusenciasCuadrante", "=== MOSTRANDO DIÁLOGO DE FILTRO ===")
        Log.d("FragmentAusenciasCuadrante", "allUsers.size: ${allUsers.size}")
        Log.d("FragmentAusenciasCuadrante", "allUsers: ${allUsers.map { it.app }}")
        
        // Verificar que hay usuarios disponibles
        if (allUsers.isEmpty()) {
            Log.w("FragmentAusenciasCuadrante", "No hay usuarios disponibles, intentando cargar...")
            
            // Intentar cargar usuarios antes de mostrar el error
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    Log.d("FragmentAusenciasCuadrante", "Intentando cargar usuarios desde showUserFilterDialog")
                    val allAbsences = viewModel.loadAllAbsencesFromLogs()
                    Log.d("FragmentAusenciasCuadrante", "Ausencias cargadas en diálogo: ${allAbsences.size}")
                    
                    if (allAbsences.isNotEmpty()) {
                        loadUsersFromAbsences(allAbsences)
                        Log.d("FragmentAusenciasCuadrante", "Usuarios cargados exitosamente: ${allUsers.size}")
                        
                        // Mostrar el diálogo de filtro ahora que tenemos usuarios
                        showUserFilterDialogInternal()
                    } else {
                        Log.w("FragmentAusenciasCuadrante", "No se encontraron ausencias, mostrando error")
                        showNoUsersErrorDialog()
                    }
                } catch (e: Exception) {
                    Log.e("FragmentAusenciasCuadrante", "Error cargando usuarios en diálogo: ${e.message}", e)
                    showNoUsersErrorDialog()
                }
            }
            return
        }
        
        // Si ya tenemos usuarios, mostrar el diálogo directamente
        showUserFilterDialogInternal()
    }
    
    private fun showNoUsersErrorDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Sin datos")
            .setMessage("No hay usuarios disponibles para filtrar. Asegúrate de que hay datos de ausencias cargados.")
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showUserFilterDialogInternal() {
        Log.d("FragmentAusenciasCuadrante", "Mostrando diálogo interno con ${allUsers.size} usuarios")
        
        // Usar solo los nombres de usuario (app) para el filtro
        val opciones = allUsers.map { it.app }
        val seleccionados = mutableSetOf<String>()
        
        // Mapear los IDs seleccionados a las opciones del diálogo
        allUsers.forEachIndexed { index, user ->
            if (selectedUserIds.contains(user.cod)) {
                seleccionados.add(opciones[index])
            }
        }
        
        Log.d("FragmentAusenciasCuadrante", "Opciones: $opciones")
        Log.d("FragmentAusenciasCuadrante", "Seleccionados: $seleccionados")
        
        val dialog = MultiSelectFilterDialog(
            context = requireContext(),
            titulo = "Seleccionar Usuarios para Filtrar",
            opciones = opciones,
            seleccionados = seleccionados
        ) {
            // Callback cuando se actualiza el filtro
            // Mapear las opciones seleccionadas de vuelta a los IDs
            selectedUserIds.clear()
            allUsers.forEachIndexed { index, user ->
                if (seleccionados.contains(opciones[index])) {
                    selectedUserIds.add(user.cod)
                }
            }
            
            // Actualizar inmediatamente sin recargar datos
            updateCalendarFromCurrentData()
        }
        
        dialog.show()
    }
    
    private fun updateCalendarFromCurrentData() {
        // Obtener las ausencias actuales del ViewModel
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.absences.first().let { absences ->
                updateCalendar(absences)
            }
        }
    }

    private fun showAbsencesPopup(day: AbsenceCalendarAdapter.CalendarDay) {
        Log.d("FragmentAusenciasCuadrante", "=== MOSTRANDO POPUP SIMPLIFICADO PARA DÍA ${day.dayNumber} ===")
        Log.d("FragmentAusenciasCuadrante", "Ausencias en el día: ${day.absences.size}")
        
        // Log detallado de todas las ausencias del día con hash para rastrear origen
        day.absences.forEachIndexed { index, absence ->
            val hash = System.identityHashCode(absence)
            Log.d("FragmentAusenciasCuadrante", "POPUP DÍA ${day.dayNumber} - Ausencia $index [Hash: $hash]: employeeId='${absence.employeeId}', employeeName='${absence.employeeName}', tipo='${absence.absenceType}', fechas='${absence.startDate}' a '${absence.endDate}'")
        }
        
        if (day.absences.isEmpty()) {
            // Si no hay ausencias, mostrar mensaje simple
            AlertDialog.Builder(requireContext())
                .setTitle("Ausencias del día: ${day.dayNumber} de ${getMonthName(day.date)}")
                .setMessage("No hay ausencias registradas para este día.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        // Desduplicar ausencias de forma simple para el popup
        // Desduplicar usando un ID canónico de usuario para fusionar '0' y 'f.tome'
        val uniqueAbsences = day.absences.distinctBy { 
            "${canonicalEmployeeId(it.employeeId, it.employeeName)}-${it.absenceType}-${it.startDate.time}-${it.endDate.time}"
        }
        
        Log.d("FragmentAusenciasCuadrante", "POPUP DÍA ${day.dayNumber}: ${day.absences.size} ausencias originales, ${uniqueAbsences.size} únicas después de distinctBy")
        
        // Log detallado de las ausencias únicas
        uniqueAbsences.forEachIndexed { index, absence ->
            val hash = System.identityHashCode(absence)
            Log.d("FragmentAusenciasCuadrante", "POPUP DÍA ${day.dayNumber} - Única $index [Hash: $hash]: employeeId='${absence.employeeId}', employeeName='${absence.employeeName}', tipo='${absence.absenceType}'")
        }
        
        // Construir mensaje con información de ausencias
        val message = buildString {
            uniqueAbsences.forEachIndexed { index, absence ->
                val dateFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))
                val startDate = dateFormat.format(absence.startDate)
                val endDate = dateFormat.format(absence.endDate)
                
                val dateRange = if (absence.startDate == absence.endDate) {
                    startDate
                } else {
                    "$startDate - $endDate"
                }
                
                val absenceType = when (absence.absenceType) {
                    AbsenceType.VACACIONES -> "Vacaciones"
                    AbsenceType.ENFERMEDAD -> "Enfermedad"
                    AbsenceType.PERMISO -> "Permiso"
                    AbsenceType.ASUNTOS_PERSONALES -> "Asuntos Personales"
                    AbsenceType.FORMACION -> "Formación"
                    else -> "Otros"
                }
                
                // Mostrar employeeId canónico como nombre
                val displayName = canonicalEmployeeId(absence.employeeId, absence.employeeName)
                
                val hash = System.identityHashCode(absence)
                Log.d("FragmentAusenciasCuadrante", "POPUP DÍA ${day.dayNumber} - Mostrando item $index [Hash: $hash]: '$displayName - $absenceType' ($dateRange)")
                
                appendLine("• $displayName - $absenceType")
                appendLine("  $dateRange")
                appendLine()
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Ausencias del día: ${day.dayNumber} de ${getMonthName(day.date)}")
            .setMessage(message.trim())
            .setPositiveButton("OK", null)
            .show()
            
        Log.d("FragmentAusenciasCuadrante", "=== FIN POPUP SIMPLIFICADO ===")
    }

    private fun canonicalEmployeeId(employeeId: String, employeeName: String): String {
        val isNumericId = employeeId.matches(Regex("\\d+"))
        val looksLikeAppId = employeeName.matches(Regex("[a-zA-Z]+\\.[a-zA-Z]+"))
        val looksLikeValidAppId = employeeName.matches(Regex("[a-zA-Z]+[a-zA-Z0-9]*"))
        
        return when {
            // Caso 1: employeeId es numérico y employeeName tiene formato "a.b" (como "f.tome")
            isNumericId && looksLikeAppId -> employeeName
            // Caso 2: employeeId es numérico y employeeName es un app ID válido (como "jjalvarez")
            isNumericId && looksLikeValidAppId && employeeName.length > 2 -> employeeName
            // Caso 3: employeeId ya es un app ID válido
            else -> employeeId
        }
    }
    
    private fun getMonthName(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val monthFormat = SimpleDateFormat("MMMM", Locale("es", "ES"))
        return monthFormat.format(date).replaceFirstChar { it.uppercase() }
    }
    
    private fun loadHolidaysForMonth(year: Int, month: Int) {
        Log.d("FragmentAusenciasCuadrante", "Cargando festivos para $month/$year")
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Usar el ViewModel para cargar festivos desde Google Sheets
                val holidays = viewModel.loadHolidaysForMonth(year, month)
                
                adapter.updateHolidays(holidays)
                Log.d("FragmentAusenciasCuadrante", "Festivos cargados para $month/$year: ${holidays.size}")
                
                if (holidays.isNotEmpty()) {
                    Log.d("FragmentAusenciasCuadrante", "Festivos: $holidays")
                }
            } catch (e: Exception) {
                Log.e("FragmentAusenciasCuadrante", "Error cargando festivos: ${e.message}", e)
                adapter.updateHolidays(emptyMap())
            }
        }
    }
}
