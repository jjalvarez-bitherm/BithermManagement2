package com.bithermmanagement.ui.items

import android.app.DatePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.R
import com.bithermmanagement.data.GoogleDriveManager
import com.bithermmanagement.data.GoogleDriveManager.SpreadsheetInfo
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.data.GoogleAuthAdapter
import com.bithermmanagement.data.SettingsManager
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.Equipo
import com.bithermmanagement.ui.dialogs.SeleccionOrdenDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONObject
import java.io.BufferedReader
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class FragmentInspeccionConfiguracion : Fragment() {
    private lateinit var txtTitulo: TextView
    private lateinit var txtFecha: TextView
    private lateinit var txtNumeroInspeccion: TextView
    private lateinit var spinnerLibro: Spinner
    private lateinit var spinnerHojaFlota: Spinner
    private lateinit var spinnerHojaInspeccion: Spinner
    private lateinit var spinnerHojaReparaciones: Spinner
    
    private lateinit var btnDescargar: MaterialButton
    private lateinit var btnActualizar: MaterialButton
    private lateinit var btnDescargarFotos: MaterialButton
    private lateinit var imgBorrarFotos: ImageView
    private lateinit var btnActualizarFotos: MaterialButton
    private lateinit var btnCopiarDB: MaterialButton
    private lateinit var btnCalibrarPantalla: MaterialButton
    private lateinit var btnBuscarActualizaciones: MaterialButton
    private lateinit var txtTotalEquipos: TextView
    private lateinit var txtInspeccionados: TextView
    private lateinit var txtModificados: TextView
    private lateinit var txtActivos: TextView
    private lateinit var txtMonitorizados: TextView
    private lateinit var txtAfsEliminados: TextView
    private lateinit var txtFotosDrive: TextView
    private lateinit var txtFotosNuevas: TextView
    private lateinit var txtFotosPendientes: TextView
    private lateinit var imgBorrar: ImageView
    private lateinit var progressBar: ProgressBar

    private lateinit var prefs: SharedPreferences
	private lateinit var dataProcessor: InspeccionDataProcessor
    private val PREFS_NAME = "configuracion_inspeccion"
    private val DATE_KEY = "fecha_descarga"
    private val LIBRO_KEY = "libro"
    private val HOJA_KEY = "hoja"
    private val LIBRO_ID_KEY = "libro_id"
    private val DB_LIBRO_KEY = "db_libro"
    private val DB_LIBRO_ID_KEY = "db_libro_id"
    private val DB_INSPECCIONES_HOJA_KEY = "db_inspecciones_hoja"
    private val DB_REPARACIONES_HOJA_KEY = "db_reparaciones_hoja"
    private val NUMERO_INSPECCION_KEY = "numero_inspeccion"
    private val HOJA_FLOTA_KEY = "hoja_flota"
    private val HOJA_INSPECCION_KEY = "hoja_inspeccion"
    private val HOJA_REPARACIONES_KEY = "hoja_reparaciones"

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateFormatISO = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var fechaSeleccionada: Calendar = Calendar.getInstance()
    
    /**
     * Parsea una fecha en cualquiera de los formatos soportados
     */
    private fun parsearFecha(fechaString: String): Date? {
        return try {
            // Intentar primero con formato dd/MM/yyyy
            dateFormat.parse(fechaString)
        } catch (e: Exception) {
            try {
                // Si falla, intentar con formato yyyy-MM-dd
                dateFormatISO.parse(fechaString)
            } catch (e2: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "No se pudo parsear fecha '$fechaString' en ningún formato")
                null
            }
        }
    }
    
    /**
     * Normaliza una fecha a la medianoche (00:00:00) de ese día
     */
    private fun getMidnightDate(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private var libros: List<GoogleDriveManager.SpreadsheetInfo> = emptyList()
    private var hojas: List<String> = emptyList()
    private var hojasFlota: List<String> = emptyList()
    private var hojasInspeccion: List<String> = emptyList()
    private var hojasReparaciones: List<String> = emptyList()

    
    /**
     * Obtiene un GoogleSheetsManager configurado según la configuración actual
     */
    private fun obtenerGoogleSheetsManager(): GoogleSheetsManager? {
        return try {
            val settingsManager = SettingsManager(requireContext())
            settingsManager.getGoogleSheetsManager()
        } catch (e: Exception) {
            Log.e("FragmentInspeccionConfiguracion", "Error al obtener GoogleSheetsManager: ${e.message}")
            null
        }
    }
    
    /**
     * Obtiene la configuración actual (libro, hoja FLOTA) de forma síncrona
     * @return Pair<libroId, hojaFlotaNombre> o null si no está configurado
     */
    private fun obtenerConfiguracionActualSync(): Pair<String, String>? {
        val libroId = prefs.getString(LIBRO_ID_KEY, null)
        val hojaFlota = prefs.getString(HOJA_FLOTA_KEY, null)
        
        if (!libroId.isNullOrEmpty() && !hojaFlota.isNullOrEmpty()) {
            return Pair(libroId, hojaFlota)
        }
        
        // Fallback: usar configuración antigua si existe
        val dbLibroId = prefs.getString(DB_LIBRO_ID_KEY, null)
        val dbHoja = prefs.getString(DB_INSPECCIONES_HOJA_KEY, null)
        if (!dbLibroId.isNullOrEmpty() && !dbHoja.isNullOrEmpty()) {
            return Pair(dbLibroId, dbHoja)
        }
        
        return null
    }
    
    /**
     * Obtiene la configuración de la hoja de inspección actual
     * @return Pair<libroId, hojaInspeccionNombre> o null si no está configurado
     */
    private fun obtenerConfiguracionInspeccionActualSync(): Pair<String, String>? {
        val libroId = prefs.getString(LIBRO_ID_KEY, null)
        val hojaInspeccion = prefs.getString(HOJA_INSPECCION_KEY, null)
        
        if (!libroId.isNullOrEmpty() && !hojaInspeccion.isNullOrEmpty()) {
            return Pair(libroId, hojaInspeccion)
        }
        
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("FragmentInspeccionConfiguracion", "onCreateView llamado")
        return inflater.inflate(R.layout.fragment_inspeccion_configuracion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("FragmentInspeccionConfiguracion", "onViewCreated llamado")
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
		dataProcessor = InspeccionDataProcessor(requireContext(), prefs)
        bindViews(view)
        setupTitulo()
        val fechaGuardada = prefs.getString(DATE_KEY, null)
        val libroGuardado = prefs.getString(LIBRO_KEY, null)
        val hojaGuardada = prefs.getString(HOJA_KEY, null)
        // Los campos siempre estarán habilitados salvo que explícitamente se bloqueen tras descargar
        setupFecha(fechaGuardada, false)
        setupDropdownsGoogle(libroGuardado, null, false)
        setupBotones()
        spinnerLibro.isEnabled = true
        spinnerHojaFlota.isEnabled = true
        spinnerHojaInspeccion.isEnabled = true
        spinnerHojaReparaciones.isEnabled = true
        txtFecha.isEnabled = true
        cargarResumen()
    }

    private fun bindViews(view: View) {
        txtTitulo = view.findViewById(R.id.txtTitulo)
        txtFecha = view.findViewById(R.id.txtFecha)
        txtNumeroInspeccion = view.findViewById(R.id.txtNumeroInspeccion)
        spinnerLibro = view.findViewById(R.id.spinnerLibro)
        spinnerHojaFlota = view.findViewById(R.id.spinnerHojaFlota)
        spinnerHojaInspeccion = view.findViewById(R.id.spinnerHojaInspeccion)
        spinnerHojaReparaciones = view.findViewById(R.id.spinnerHojaReparaciones)
        btnDescargar = view.findViewById(R.id.btnDescargar)
        btnActualizar = view.findViewById(R.id.btnActualizar)
        btnDescargarFotos = view.findViewById(R.id.btnDescargarFotos)
        imgBorrarFotos = view.findViewById(R.id.imgBorrarFotos)
        btnActualizarFotos = view.findViewById(R.id.btnActualizarFotos)
        btnCopiarDB = view.findViewById(R.id.btnCopiarDB)
        btnCalibrarPantalla = view.findViewById(R.id.btnCalibrarPantalla)
        btnBuscarActualizaciones = view.findViewById(R.id.btnBuscarActualizaciones)
        txtTotalEquipos = view.findViewById(R.id.txtTotalEquipos)
        txtInspeccionados = view.findViewById(R.id.txtInspeccionados)
        txtModificados = view.findViewById(R.id.txtModificados)
        txtActivos = view.findViewById(R.id.txtActivos)
        txtMonitorizados = view.findViewById(R.id.txtMonitorizados)
        txtAfsEliminados = view.findViewById(R.id.txtAfsEliminados)
        txtFotosDrive = view.findViewById(R.id.txtFotosDrive)
        txtFotosNuevas = view.findViewById(R.id.txtFotosNuevas)
        txtFotosPendientes = view.findViewById(R.id.txtFotosPendientes)
        imgBorrar = view.findViewById(R.id.imgBorrar)
        progressBar = view.findViewById(R.id.progressBar)
    }

    private fun setupTitulo() {
        txtTitulo.isSelected = true // Para que el autosize funcione bien
    }

    private fun setupFecha(fechaGuardada: String?, locked: Boolean) {
        if (fechaGuardada != null) {
            fechaSeleccionada.time = dateFormat.parse(fechaGuardada) ?: Date()
        }
        txtFecha.text = dateFormat.format(fechaSeleccionada.time)
        txtFecha.isEnabled = !locked
        
        // Inicializar número de inspección
        val numeroInspeccion = prefs.getString("numero_inspeccion", "25")
        txtNumeroInspeccion.text = numeroInspeccion
        if (!locked) {
            txtFecha.setOnClickListener {
                Log.d("FragmentInspeccionConfiguracion", "CLICK en txtFecha - abriendo DatePickerDialog")
                val c = fechaSeleccionada
                DatePickerDialog(requireContext(), { _, year, month, day ->
                    c.set(Calendar.YEAR, year)
                    c.set(Calendar.MONTH, month)
                    c.set(Calendar.DAY_OF_MONTH, day)
                    val nuevaFecha = dateFormat.format(c.time)
                    Log.d("FragmentInspeccionConfiguracion", "Fecha seleccionada: $nuevaFecha")
                    txtFecha.text = nuevaFecha
                    prefs.edit().putString(DATE_KEY, nuevaFecha).apply()
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }
        } else {
            txtFecha.setOnClickListener(null)
        }
    }

    private fun setupDropdownsGoogle(libroGuardado: String?, hojaGuardada: String?, locked: Boolean) {
        Log.d("FragmentInspeccionConfiguracion", "setupDropdownsGoogle: libroGuardado=$libroGuardado, hojaGuardada=$hojaGuardada, locked=$locked")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val settingsManager = SettingsManager(context)
                val sheetsManager = settingsManager.getGoogleSheetsManager()
                
                if (sheetsManager == null) {
                    Log.e("FragmentInspeccionConfiguracion", "No se pudo crear GoogleSheetsManager")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error de configuración. Verifica las credenciales.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                // Crear GoogleDriveManager con la configuración correcta
                val settings = settingsManager.getSettings()
                val credentialsStream = if (settings.useOAuth) {
                    null // Para OAuth no necesitamos credenciales
                } else {
                    settingsManager.getCredentialsInputStream() // Para Service Account sí necesitamos credenciales
                }
                
                val driveManager = GoogleDriveManager(
                    credentialsStream = credentialsStream,
                    context = context,
                    useOAuth = settings.useOAuth,
                    oAuthEmail = settings.oAuthEmail
                )
                
                val spreadsheets = driveManager.listarSpreadsheetsApp()
                libros = spreadsheets.filter { it.name.endsWith("(APP)") }
                Log.d("FragmentInspeccionConfiguracion", "Libros encontrados: ${libros.size}")
                withContext(Dispatchers.Main) {
                    val libroAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, libros.map { it.name })
                    libroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerLibro.adapter = libroAdapter
                    val libroIndex = if (libroGuardado != null) libros.indexOfFirst { it.name == libroGuardado } else -1
                    if (libroIndex >= 0) spinnerLibro.setSelection(libroIndex)
                    spinnerLibro.isEnabled = !locked
                    spinnerLibro.onItemSelectedListener = if (!locked) object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val libro = libros[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerLibro: libro seleccionado=${libro.name}")
                            prefs.edit().putString(LIBRO_KEY, libro.name).putString(LIBRO_ID_KEY, libro.id).apply()
                            cargarHojasGoogle(libro.id, hojaGuardada, locked)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    } else null
                    if (libroIndex >= 0) cargarHojasGoogle(libros[libroIndex].id, hojaGuardada, locked)
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error cargando libros: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error cargando libros: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

 
    
    // FUNCIONES ACTIVAS - Sistema simplificado
    
    private fun cargarHojasGoogle(libroId: String, hojaGuardada: String?, locked: Boolean) {
        Log.d("FragmentInspeccionConfiguracion", "cargarHojasGoogle: libroId=$libroId")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val sheetsManager = obtenerGoogleSheetsManager()
                
                if (sheetsManager == null) {
                    Log.e("FragmentInspeccionConfiguracion", "No se pudo crear GoogleSheetsManager")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error de configuración. Verifica las credenciales.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                val hojasList = sheetsManager.listarHojas(libroId)
                Log.d("FragmentInspeccionConfiguracion", "Hojas encontradas: ${hojasList.size}")
                
                // Separar hojas: FLOTA, REPARACIONES, y hojas numéricas (números enteros)
                val hojasFlotaList = mutableListOf<String>()
                val hojasInspeccionList = mutableListOf<String>()
                val hojasReparacionesList = mutableListOf<String>()
                
                for (hoja in hojasList) {
                    val hojaUpper = hoja.uppercase().trim()
                    // Si es un número entero, es una hoja de inspección
                    if (hoja.toIntOrNull() != null) {
                        hojasInspeccionList.add(hoja)
                    } else if (hojaUpper.contains("FLOTA", ignoreCase = true)) {
                        hojasFlotaList.add(hoja)
                    } else if (hojaUpper.contains("REPARACIONES", ignoreCase = true) || hojaUpper.contains("REPARACION", ignoreCase = true)) {
                        hojasReparacionesList.add(hoja)
                    }
                }
                
                // Ordenar hojas de inspección numéricamente
                hojasInspeccionList.sortWith(compareBy { it.toIntOrNull() ?: Int.MAX_VALUE })
                
                hojasFlota = hojasFlotaList
                hojasInspeccion = hojasInspeccionList
                hojasReparaciones = hojasReparacionesList
                
                Log.d("FragmentInspeccionConfiguracion", "Hojas FLOTA: ${hojasFlota.size} - ${hojasFlota.joinToString(", ")}")
                Log.d("FragmentInspeccionConfiguracion", "Hojas Inspección: ${hojasInspeccion.size} - ${hojasInspeccion.joinToString(", ")}")
                Log.d("FragmentInspeccionConfiguracion", "Hojas Reparaciones: ${hojasReparaciones.size} - ${hojasReparaciones.joinToString(", ")}")
                
                withContext(Dispatchers.Main) {
                    // Configurar spinner de hoja FLOTA
                    val hojaFlotaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, hojasFlota)
                    hojaFlotaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerHojaFlota.adapter = hojaFlotaAdapter
                    
                    val hojaFlotaGuardada = prefs.getString(HOJA_FLOTA_KEY, null)
                    // Buscar hoja que contenga "FLOTA" como predeterminada
                    val hojaFlotaPredeterminada = hojasFlota.firstOrNull { it.uppercase().contains("FLOTA", ignoreCase = true) }
                    val hojaFlotaIndex = if (hojaFlotaGuardada != null && hojasFlota.contains(hojaFlotaGuardada)) {
                        hojasFlota.indexOf(hojaFlotaGuardada)
                    } else if (hojaFlotaPredeterminada != null) {
                        hojasFlota.indexOf(hojaFlotaPredeterminada)
                    } else if (hojasFlota.isNotEmpty()) {
                        0
                    } else {
                        -1
                    }
                    if (hojaFlotaIndex >= 0) {
                        spinnerHojaFlota.setSelection(hojaFlotaIndex)
                        prefs.edit().putString(HOJA_FLOTA_KEY, hojasFlota[hojaFlotaIndex]).apply()
                    }
                    spinnerHojaFlota.isEnabled = !locked
                    spinnerHojaFlota.onItemSelectedListener = if (!locked) object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val hoja = hojasFlota[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerHojaFlota: hoja seleccionada=$hoja")
                            prefs.edit().putString(HOJA_FLOTA_KEY, hoja).apply()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    } else null
                    
                    // Configurar spinner de hoja de inspección actual
                    val hojaInspeccionAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, hojasInspeccion)
                    hojaInspeccionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerHojaInspeccion.adapter = hojaInspeccionAdapter
                    
                    val hojaInspeccionGuardada = prefs.getString(HOJA_INSPECCION_KEY, null)
                    val hojaInspeccionIndex = if (hojaInspeccionGuardada != null && hojasInspeccion.contains(hojaInspeccionGuardada)) {
                        hojasInspeccion.indexOf(hojaInspeccionGuardada)
                    } else if (hojasInspeccion.isNotEmpty()) {
                        // Seleccionar la hoja con el número más alto (última en la lista ordenada numéricamente)
                        hojasInspeccion.size - 1
                    } else {
                        -1
                    }
                    if (hojaInspeccionIndex >= 0) {
                        spinnerHojaInspeccion.setSelection(hojaInspeccionIndex)
                        prefs.edit().putString(HOJA_INSPECCION_KEY, hojasInspeccion[hojaInspeccionIndex]).apply()
                    }
                    spinnerHojaInspeccion.isEnabled = !locked
                    spinnerHojaInspeccion.onItemSelectedListener = if (!locked) object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val hoja = hojasInspeccion[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerHojaInspeccion: hoja seleccionada=$hoja")
                            prefs.edit().putString(HOJA_INSPECCION_KEY, hoja).apply()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    } else null
                    
                    // Configurar spinner de hoja de reparaciones
                    // Asegurar que siempre haya al menos una opción vacía si no hay hojas
                    val hojasReparacionesConVacio = if (hojasReparaciones.isEmpty()) {
                        listOf("(Sin hojas de reparaciones)")
                    } else {
                        hojasReparaciones
                    }
                    
                    val hojaReparacionesAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, hojasReparacionesConVacio)
                    hojaReparacionesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerHojaReparaciones.adapter = hojaReparacionesAdapter
                    
                    val hojaReparacionesGuardada = prefs.getString(HOJA_REPARACIONES_KEY, null)
                    // Buscar hoja que contenga "REPARACIONES" como predeterminada
                    val hojaReparacionesPredeterminada = hojasReparaciones.firstOrNull { 
                        it.uppercase().contains("REPARACIONES", ignoreCase = true) || 
                        it.uppercase().contains("REPARACION", ignoreCase = true) 
                    }
                    val hojaReparacionesIndex = if (hojasReparaciones.isEmpty()) {
                        0 // Seleccionar el placeholder "(Sin hojas de reparaciones)"
                    } else if (hojaReparacionesGuardada != null && hojasReparaciones.contains(hojaReparacionesGuardada)) {
                        hojasReparaciones.indexOf(hojaReparacionesGuardada)
                    } else if (hojaReparacionesPredeterminada != null) {
                        hojasReparaciones.indexOf(hojaReparacionesPredeterminada)
                    } else if (hojasReparaciones.isNotEmpty()) {
                        0
                    } else {
                        0
                    }
                    
                    spinnerHojaReparaciones.setSelection(hojaReparacionesIndex)
                    if (hojasReparaciones.isNotEmpty() && hojaReparacionesIndex >= 0 && hojaReparacionesIndex < hojasReparaciones.size) {
                        prefs.edit().putString(HOJA_REPARACIONES_KEY, hojasReparaciones[hojaReparacionesIndex]).apply()
                    }
                    
                    spinnerHojaReparaciones.isEnabled = !locked && hojasReparaciones.isNotEmpty()
                    spinnerHojaReparaciones.onItemSelectedListener = if (!locked && hojasReparaciones.isNotEmpty()) object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            if (position < hojasReparaciones.size) {
                                val hoja = hojasReparaciones[position]
                                Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerHojaReparaciones: hoja seleccionada=$hoja")
                                prefs.edit().putString(HOJA_REPARACIONES_KEY, hoja).apply()
                            }
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    } else null
                    
                    Log.d("FragmentInspeccionConfiguracion", "Spinner reparaciones configurado: ${hojasReparaciones.size} hojas, enabled=${spinnerHojaReparaciones.isEnabled}, adapter=${spinnerHojaReparaciones.adapter != null}")
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error cargando hojas: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error cargando hojas: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupBotones() {
        btnActualizar.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "=== CLICK EN BOTÓN ACTUALIZAR ===")
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnActualizar")
            
            // Obtener configuración de la hoja de inspección actual (donde guardar)
            val config = obtenerConfiguracionInspeccionActualSync()
            if (config == null) {
                Toast.makeText(requireContext(), "Configura primero el libro y la hoja de inspección actual", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val (dbLibroId, dbHoja) = config
            
            Toast.makeText(requireContext(), "ACTUALIZANDO A $dbHoja", Toast.LENGTH_SHORT).show()
            setLoading(true)
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val settingsManager = SettingsManager(context)
                    val sheetsManager = settingsManager.getGoogleSheetsManager()
                    
                    if (sheetsManager == null) {
                        Log.e("FragmentInspeccionConfiguracion", "No se pudo crear GoogleSheetsManager")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error de configuración. Verifica las credenciales.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Actualizando desde libro: $dbLibroId, hoja: $dbHoja")
                    
                    // Leer cabeceras de la hoja INSPECCIONES (fila 2)
                    val headerResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(dbLibroId, "$dbHoja!A2:ZZ2")
                        .execute()
                    val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Cabeceras encontradas en INSPECCIONES: ${headerRow.joinToString(", ")}")
                    
                    // Detectar la última columna con datos
                    val ultimaColumnaIdx = headerRow.size - 1
                    val ultimaColumna = if (ultimaColumnaIdx >= 0) {
                        val columnaLetra = (ultimaColumnaIdx / 26).let { if (it > 0) ('A' + it - 1).toString() else "" } + ('A' + ultimaColumnaIdx % 26)
                        columnaLetra
                        } else {
                        "ZZ"
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Última columna detectada: $ultimaColumna (índice: $ultimaColumnaIdx)")
                    
                    // Leer datos desde la hoja INSPECCIONES (desde fila 3)
                    val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(dbLibroId, "$dbHoja!A3:$ultimaColumna")
                        .execute()
                    val dataRows = dataResponse.getValues() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Datos obtenidos desde INSPECCIONES: ${dataRows.size} filas con ${headerRow.size} columnas")
                    
                    if (dataRows.isEmpty()) {
                        Log.w("FragmentInspeccionConfiguracion", "No se encontraron datos en la hoja INSPECCIONES")
                        withContext(Dispatchers.Main) {
                        setLoading(false)
                            Toast.makeText(requireContext(), "No se encontraron datos en la hoja INSPECCIONES", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    
                    // Procesar la importación de datos
                    val headerRowString = headerRow.map { it?.toString() ?: "" }
                    procesarImportacionDatos(headerRowString, dataRows, null, dbLibroId, sheetsManager)
                    
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Actualización completada desde $dbHoja", Toast.LENGTH_SHORT).show()
                        cargarResumen()
                    }
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionConfiguracion", "Error sincronizando equipos: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_LONG).show()
                        setLoading(false)
                    }
                }
            }
        }
        imgBorrarFotos.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en imgBorrarFotos")
            // TODO: Implementar borrado de fotos
            Toast.makeText(requireContext(), "Función de borrado de fotos pendiente", Toast.LENGTH_SHORT).show()
        }
        
        imgBorrar.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en imgBorrar")
            Toast.makeText(requireContext(), "Base de datos restablecida", Toast.LENGTH_SHORT).show()
            prefs.edit().clear().apply()
            spinnerLibro.isEnabled = true
            spinnerHojaFlota.isEnabled = true
            spinnerHojaInspeccion.isEnabled = true
            spinnerHojaReparaciones.isEnabled = true
            txtFecha.isEnabled = true
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                db.equipoDao().borrarTodo()
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    setupFecha(null, false)
                    // Recargar spinners con valores predeterminados después de borrar
                    val libroId = prefs.getString(LIBRO_ID_KEY, null)
                    setupDropdownsGoogle(libroId, null, false)
                    cargarResumen()
                }
            }
        }
        btnDescargarFotos.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnDescargarFotos")
            Toast.makeText(requireContext(), "Descargando fotos desde Google Drive...", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // TODO: Implementar descarga de fotos desde Google Drive
                withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Descarga de fotos completada", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                    cargarResumen()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error descargando fotos: ${e.message}", Toast.LENGTH_SHORT).show()
                        setLoading(false)
                    }
                }
            }
        }
        btnActualizarFotos.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en imgActualizarFotos")
            Toast.makeText(requireContext(), "Migrando y subiendo fotos...", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val db = AppDatabase.getDatabase(context)
                    val fotoDao = db.fotoEquipoDao()
                    val inspeccionDao = db.inspeccionDao()
                    
                    // PASO 1: Migrar fotos existentes de equipos a FotoEquipoEntity
                    Log.d("FragmentInspeccionConfiguracion", "Iniciando migración de fotos existentes...")
                    val equipos = inspeccionDao.getAllEquipos()
                    var fotosMigradas = 0
                    
                    // DEBUG: Verificar fotos en sistema de archivos
                    Log.d("FragmentInspeccionConfiguracion", "=== DEBUG: Verificando fotos en sistema de archivos ===")
                    val fotosDir = File(context.filesDir, "fotos")
                    if (fotosDir.exists()) {
                        val archivosFotos = fotosDir.listFiles()
                        Log.d("FragmentInspeccionConfiguracion", "Directorio fotos existe. Archivos encontrados: ${archivosFotos?.size ?: 0}")
                        archivosFotos?.forEach { archivo ->
                            Log.d("FragmentInspeccionConfiguracion", "Archivo foto: ${archivo.name}")
                        }
                    } else {
                        Log.d("FragmentInspeccionConfiguracion", "Directorio fotos NO existe")
                    }
                    
                    // DEBUG: Verificar fotos en directorio externo
                    val externalFotosDir = File(context.getExternalFilesDir(null), "BithermFotos")
                    if (externalFotosDir.exists()) {
                        Log.d("FragmentInspeccionConfiguracion", "Directorio externo BithermFotos existe")
                        externalFotosDir.walkTopDown().forEach { archivo ->
                            if (archivo.isFile) {
                                Log.d("FragmentInspeccionConfiguracion", "Archivo externo: ${archivo.absolutePath}")
                            }
                        }
                    } else {
                        Log.d("FragmentInspeccionConfiguracion", "Directorio externo BithermFotos NO existe")
                    }
                    
                    for (equipo in equipos) {
                        // Migrar foto del equipo
                        val urlFotoEquipo = equipo.urlFotoEquipo
                        if (!urlFotoEquipo.isNullOrEmpty()) {
                            val fotoEntity = com.bithermmanagement.database.entities.FotoEquipoEntity(
                                idEquipo = equipo.id,
                                tipo = "EQUIPO",
                                rutaLocal = urlFotoEquipo,
                                urlDrive = null,
                                esFavorita = false,
                                estadoSubida = "LOCAL",
                                fecha = System.currentTimeMillis()
                            )
                            fotoDao.insert(fotoEntity)
                            fotosMigradas++
                            Log.d("FragmentInspeccionConfiguracion", "Migrada foto EQUIPO: ${equipo.id}")
                        }
                        
                        // Migrar foto de ubicación
                        val urlFotoUbicacion = equipo.urlFotoUbicacion
                        if (!urlFotoUbicacion.isNullOrEmpty()) {
                            val fotoEntity = com.bithermmanagement.database.entities.FotoEquipoEntity(
                                idEquipo = equipo.id,
                                tipo = "UBICACION",
                                rutaLocal = urlFotoUbicacion,
                                urlDrive = null,
                                esFavorita = false,
                                estadoSubida = "LOCAL",
                                fecha = System.currentTimeMillis()
                            )
                            fotoDao.insert(fotoEntity)
                            fotosMigradas++
                            Log.d("FragmentInspeccionConfiguracion", "Migrada foto UBICACION: ${equipo.id}")
                        }
                        
                        // Migrar foto de manifold
                        val urlFotoManifold = equipo.urlFotoManifold
                        if (!urlFotoManifold.isNullOrEmpty()) {
                            val fotoEntity = com.bithermmanagement.database.entities.FotoEquipoEntity(
                                idEquipo = equipo.id,
                                tipo = "MANIFOLD",
                                rutaLocal = urlFotoManifold,
                                urlDrive = null,
                                esFavorita = false,
                                estadoSubida = "LOCAL",
                                fecha = System.currentTimeMillis()
                            )
                            fotoDao.insert(fotoEntity)
                            fotosMigradas++
                            Log.d("FragmentInspeccionConfiguracion", "Migrada foto MANIFOLD: ${equipo.id}")
                        }
                        
                        // Migrar fotos extra
                        val urlFotosExtra = equipo.urlFotosExtra
                        if (!urlFotosExtra.isNullOrEmpty()) {
                            val fotosExtra = urlFotosExtra.split(";")
                            for (i in fotosExtra.indices) {
                                if (fotosExtra[i].isNotEmpty()) {
                                    val fotoEntity = com.bithermmanagement.database.entities.FotoEquipoEntity(
                                        idEquipo = equipo.id,
                                        tipo = "EXTRA_${i + 1}",
                                        rutaLocal = fotosExtra[i],
                                        urlDrive = null,
                                        esFavorita = false,
                                        estadoSubida = "LOCAL",
                                        fecha = System.currentTimeMillis()
                                    )
                                    fotoDao.insert(fotoEntity)
                                    fotosMigradas++
                                    Log.d("FragmentInspeccionConfiguracion", "Migrada foto EXTRA_${i + 1}: ${equipo.id}")
                                }
                            }
                        }
                    }
                    
                    // PASO 1.5: Migrar fotos desde sistema de archivos
                    Log.d("FragmentInspeccionConfiguracion", "=== MIGRANDO FOTOS DESDE SISTEMA DE ARCHIVOS ===")
                    if (fotosDir.exists()) {
                        val archivosFotos = fotosDir.listFiles()
                        archivosFotos?.forEach { archivo ->
                            val nombreArchivo = archivo.name
                            Log.d("FragmentInspeccionConfiguracion", "Procesando archivo: $nombreArchivo")
                            
                            // Extraer ID del equipo del nombre del archivo
                            // Formato esperado: NA-00001_FOTO_EQUIPO_20250909_141312.jpg
                            val partes = nombreArchivo.split("_")
                            if (partes.size >= 2) {
                                val idEquipo = partes[0] // NA-00001
                                val tipoFoto = when {
                                    nombreArchivo.contains("FOTO_EQUIPO") -> "EQUIPO"
                                    nombreArchivo.contains("FOTO_UBICACION") -> "UBICACION"
                                    nombreArchivo.contains("FOTO_MANIFOLD") -> "MANIFOLD"
                                    nombreArchivo.contains("FOTO_EXTRA") -> "EXTRA_1"
                                    else -> "EQUIPO" // Por defecto
                                }
                                
                                Log.d("FragmentInspeccionConfiguracion", "ID Equipo: $idEquipo, Tipo: $tipoFoto")
                                
                                // Verificar si ya existe esta foto en la base de datos
                                val fotosExistentes = fotoDao.getFotosPorEquipoYTipo(idEquipo, tipoFoto)
                                if (fotosExistentes.isEmpty()) {
                                    val fotoEntity = com.bithermmanagement.database.entities.FotoEquipoEntity(
                                        idEquipo = idEquipo,
                                        tipo = tipoFoto,
                                        rutaLocal = archivo.absolutePath,
                                        urlDrive = null,
                                        esFavorita = false,
                                        estadoSubida = "LOCAL",
                                        fecha = archivo.lastModified()
                                    )
                                    fotoDao.insert(fotoEntity)
                                    fotosMigradas++
                                    Log.d("FragmentInspeccionConfiguracion", "Foto migrada desde archivo: $nombreArchivo")
                                } else {
                                    Log.d("FragmentInspeccionConfiguracion", "Foto ya existe en BD: $nombreArchivo")
                                }
                            }
                        }
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Migración completada: $fotosMigradas fotos migradas")
                    
                    // PASO 2: Subir fotos pendientes a Google Drive
                    val fotosPendientes = fotoDao.getFotosPendientes()
                    Log.d("FragmentInspeccionConfiguracion", "Fotos pendientes encontradas: ${fotosPendientes.size}")
                    
                    if (fotosPendientes.isEmpty()) {
                withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Migración completada: $fotosMigradas fotos migradas. No hay fotos pendientes de subir.", Toast.LENGTH_LONG).show()
                    setLoading(false)
                    cargarResumen()
                }
                        return@launch
                    }
                    
                    // Obtener GoogleDriveHelper
                    val driveHelper = com.bithermmanagement.utils.GoogleDriveHelper(context)
                    
                    var fotosSubidas = 0
                    var fotosConError = 0
                    
                    for (foto in fotosPendientes) {
                        try {
                            // Marcar como pendiente
                            fotoDao.update(foto.copy(estadoSubida = "PENDIENTE"))
                            
                            // Subir a Google Drive
                            val fileId = driveHelper.uploadPhoto(foto.rutaLocal, foto.idEquipo, foto.tipo)
                            
                            if (fileId != null) {
                                // Marcar como subida
                                fotoDao.update(foto.copy(
                                    estadoSubida = "SUBIDA",
                                    urlDrive = fileId
                                ))
                                fotosSubidas++
                                Log.d("FragmentInspeccionConfiguracion", "Foto subida: ${foto.rutaLocal}")
                            } else {
                                // Marcar como error
                                fotoDao.update(foto.copy(estadoSubida = "LOCAL"))
                                fotosConError++
                                Log.e("FragmentInspeccionConfiguracion", "Error subiendo foto: ${foto.rutaLocal}")
                            }
                        } catch (e: Exception) {
                            // Marcar como error
                            fotoDao.update(foto.copy(estadoSubida = "LOCAL"))
                            fotosConError++
                            Log.e("FragmentInspeccionConfiguracion", "Error subiendo foto ${foto.rutaLocal}: ${e.message}")
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        val mensaje = "Migración: $fotosMigradas fotos. Subida: $fotosSubidas subidas, $fotosConError errores"
                        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                        Log.d("FragmentInspeccionConfiguracion", mensaje)
                        setLoading(false)
                        cargarResumen()
                    }
                    
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error procesando fotos: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("FragmentInspeccionConfiguracion", "Error en procesamiento de fotos: ${e.message}", e)
                        setLoading(false)
                    }
                }
            }
        }
        btnCopiarDB.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnCopiarDB")
            Toast.makeText(requireContext(), "Copiar DB (en construcción)", Toast.LENGTH_SHORT).show()
            // Lógica de copia de base de datos aquí
        }
        btnCalibrarPantalla.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnCalibrarPantalla")
            Toast.makeText(requireContext(), "Calibrar pantalla (en construcción)", Toast.LENGTH_SHORT).show()
        }
        btnBuscarActualizaciones.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnBuscarActualizaciones")
            Toast.makeText(requireContext(), "Listando hojas disponibles...", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val sheetsManager = obtenerGoogleSheetsManager()
                    
                    if (sheetsManager == null) {
                        Log.e("FragmentInspeccionConfiguracion", "No se pudo crear GoogleSheetsManager")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Error de configuración. Verifica las credenciales.", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Listando hojas del spreadsheet...")
                    val hojas = sheetsManager.listarHojas(sheetsManager.spreadsheetIdPublic)
                    
                    Log.d("FragmentInspeccionConfiguracion", "Hojas encontradas: ${hojas.joinToString(", ")}")
                    
                    // Verificar cada hoja que podría contener equipos
                    for (hoja in hojas) {
                        if (hoja.contains("EQUIPO", ignoreCase = true) || hoja.contains("INSPECCION", ignoreCase = true)) {
                            Log.d("FragmentInspeccionConfiguracion", "Verificando hoja: $hoja")
                            try {
                                val cabeceras = sheetsManager.leerCabecera(sheetsManager.spreadsheetIdPublic, hoja)
                                Log.d("FragmentInspeccionConfiguracion", "Cabeceras de $hoja: ${cabeceras.joinToString(", ")}")
                            } catch (e: Exception) {
                                Log.e("FragmentInspeccionConfiguracion", "Error leyendo cabeceras de $hoja: ${e.message}")
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Hojas listadas en logs", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionConfiguracion", "Error listando hojas: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        btnDescargar.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnDescargar")
            
            // Obtener configuración (libro y hoja FLOTA)
            val config = obtenerConfiguracionActualSync()
            if (config == null) {
                Toast.makeText(requireContext(), "Configura primero el libro y la hoja FLOTA", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            val (dbLibroId, dbHoja) = config
            
            Toast.makeText(requireContext(), "DESCARGANDO DESDE $dbHoja", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val settingsManager = SettingsManager(context)
                val sheetsManager = settingsManager.getGoogleSheetsManager()
                
                if (sheetsManager == null) {
                    Log.e("FragmentInspeccionConfiguracion", "No se pudo crear GoogleSheetsManager")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error de configuración. Verifica las credenciales.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                    
                    // Obtener el campo de orden seleccionado previamente
                    val campoOrdenSeleccionado = prefs.getString("campo_orden_seleccionado", null)
                    
                    // Usar el libro y hoja FLOTA obtenidos de la configuración
                    val libroId = dbLibroId
                    val hojaFlota = dbHoja
                    
                    if (libroId == null || hojaFlota == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No se pudo obtener la configuración", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        return@launch
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Descargando desde libro: $libroId, hoja FLOTA: $hojaFlota")
                    
                    // Escapar el nombre de la hoja para evitar errores de parsing
					val hojaFlotaEscapada = dataProcessor.escapeSheetName(hojaFlota)
                    Log.d("FragmentInspeccionConfiguracion", "Nombre de hoja original: '$hojaFlota', escapado: '$hojaFlotaEscapada'")
                    
                    // Leer cabeceras de la hoja FLOTA (fila 2, que es donde están las cabeceras)
                    // Primero leer un rango limitado para evitar timeout (hasta columna AT, que debería cubrir todos los datos generales)
                    // Según la cabecera proporcionada, MODIFICACIONES está antes de ESTADO 25, así que AT debería ser suficiente
                    val rangoHeader = "$hojaFlotaEscapada!A2:AT2"
                    Log.d("FragmentInspeccionConfiguracion", "Rango de cabeceras: $rangoHeader")
                    val headerResponse = try {
                        sheetsManager.sheetsServicePublic.spreadsheets().values()
                            .get(libroId, rangoHeader)
                            .execute()
                    } catch (e: Exception) {
                        Log.w("FragmentInspeccionConfiguracion", "Error leyendo hasta AT, intentando rango más pequeño: ${e.message}")
                        // Si falla, intentar con un rango aún más pequeño
                        val rangoHeaderPequeño = "$hojaFlotaEscapada!A2:Z2"
                        Log.d("FragmentInspeccionConfiguracion", "Intentando rango más pequeño: $rangoHeaderPequeño")
                        sheetsManager.sheetsServicePublic.spreadsheets().values()
                            .get(libroId, rangoHeaderPequeño)
                            .execute()
                    }
                    
                    var headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList()
                    
                    // Si no encontramos "ESTADO 25" o "MODIFICACIONES" en el rango inicial, intentar leer más columnas
                    val tieneModificaciones = headerRow.any { it.toString().trim().equals("MODIFICACIONES", ignoreCase = true) }
                    val tieneEstado = headerRow.any { it.toString().trim().matches(Regex("ESTADO\\s+\\d+", RegexOption.IGNORE_CASE)) }
                    
                    if (!tieneModificaciones && !tieneEstado && headerRow.isNotEmpty()) {
                        Log.d("FragmentInspeccionConfiguracion", "No se encontró MODIFICACIONES ni ESTADO en rango inicial, leyendo más columnas...")
                        try {
                            val extendedResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                                .get(libroId, "$hojaFlotaEscapada!AU2:BF2")
                                .execute()
                            val extendedRow = extendedResponse.getValues()?.firstOrNull() ?: emptyList()
                            headerRow = headerRow + extendedRow
                            Log.d("FragmentInspeccionConfiguracion", "Rango extendido leído. Total columnas: ${headerRow.size}")
                        } catch (e: Exception) {
                            Log.w("FragmentInspeccionConfiguracion", "No se pudo leer rango extendido: ${e.message}")
                        }
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Cabeceras encontradas en FLOTA: ${headerRow.size} columnas")
                    Log.d("FragmentInspeccionConfiguracion", "Primeras 20 cabeceras: ${headerRow.take(20).map { it.toString() }.joinToString(", ")}")
                    
                    // NO filtrar columnas vacías - mantener todas para que los índices coincidan con los datos
                    // Solo detectar dónde empiezan las inspecciones históricas para limitar la lectura
                    var indiceInicioInspecciones = -1
                    
                    for (i in headerRow.indices) {
                        val columna = headerRow[i].toString().trim()
                        // Detectar el inicio de las columnas de inspecciones históricas
                        if (columna.matches(Regex("ESTADO\\s+\\d+"))) {
                            indiceInicioInspecciones = i
                            Log.d("FragmentInspeccionConfiguracion", "Inicio de columnas de inspecciones históricas detectado en índice $i: $columna")
                            break
                        }
                    }
                    
                    // Si no encontramos "ESTADO 25", usar todas las columnas hasta encontrar una que empiece con "ESTADO"
                    if (indiceInicioInspecciones == -1) {
                        for (i in headerRow.indices) {
                            val columna = headerRow[i].toString().trim()
                            if (columna.startsWith("ESTADO", ignoreCase = true)) {
                                indiceInicioInspecciones = i
                                Log.d("FragmentInspeccionConfiguracion", "Inicio de columnas de inspecciones detectado en índice $i: $columna")
                                break
                            }
                        }
                    }
                    
                    // Si aún no encontramos, buscar MODIFICACIONES
                    if (indiceInicioInspecciones == -1) {
                        for (i in headerRow.indices) {
                            val columna = headerRow[i].toString().trim()
                            if (columna.equals("MODIFICACIONES", ignoreCase = true)) {
                                indiceInicioInspecciones = i + 1
                                break
                            }
                        }
                    }
                    
                    // Crear headerRowFiltrado manteniendo TODAS las columnas (incluyendo vacías) hasta indiceInicioInspecciones
                    val headerRowFiltrado = if (indiceInicioInspecciones > 0) {
                        headerRow.take(indiceInicioInspecciones).map { it.toString() }
                    } else {
                        headerRow.map { it.toString() }
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Cabeceras (manteniendo vacías): ${headerRowFiltrado.size} columnas")
                    Log.d("FragmentInspeccionConfiguracion", "Primeras 30 cabeceras: ${headerRowFiltrado.take(30).joinToString(", ")}")
                    
                    // Convertir el índice de inicio de inspecciones a letra de columna para limitar la lectura
                    val ultimaColumnaGeneral = if (indiceInicioInspecciones > 0) {
                        // Leer hasta la última columna de datos generales
                        if (indiceInicioInspecciones < 26) {
                            ('A'.code + indiceInicioInspecciones - 1).toChar().toString()
                        } else {
                            val primeraLetra = ('A'.code + ((indiceInicioInspecciones - 1) / 26 - 1)).toChar()
                            val segundaLetra = ('A'.code + ((indiceInicioInspecciones - 1) % 26)).toChar()
                            "$primeraLetra$segundaLetra"
                        }
                    } else {
                        "ZZ" // Si no encontramos, leer hasta ZZ
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Última columna de datos generales: $ultimaColumnaGeneral")
                    
                    // Leer datos desde la hoja FLOTA (desde fila 3, que es donde empiezan los datos después de la cabecera en fila 2)
                    val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(libroId, "$hojaFlotaEscapada!A3:$ultimaColumnaGeneral")
                        .execute()
                    val dataRows = dataResponse.getValues() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Datos obtenidos desde FLOTA: ${dataRows.size} filas con ${headerRowFiltrado.size} columnas")
                    
                    // Procesar los datos usando la función existente (solo con las cabeceras de datos generales)
                    importarDatosDesdeSheet(headerRowFiltrado, dataRows)
                    
                    // ACTUALIZAR COLORES DE ESTADOS DESDE SPREADSHEET
                    Log.d("FragmentInspeccionConfiguracion", "Actualizando colores de estados...")
                    val coloresActualizados = sheetsManager.actualizarColoresEstados(
                        spreadsheetId = libroId,
                        sheetName = hojaFlota // Usar la hoja FLOTA donde están los datos
                    )
                    
                    if (coloresActualizados) {
                        Log.d("FragmentInspeccionConfiguracion", "Colores de estados actualizados exitosamente")
                    } else {
                        Log.w("FragmentInspeccionConfiguracion", "No se pudieron actualizar los colores de estados")
                    }
                    
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        val mensaje = if (coloresActualizados) {
                            "Descarga completada desde $hojaFlota (colores actualizados)"
                        } else {
                            "Descarga completada desde $hojaFlota"
                        }
                        Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                        cargarResumen()
                    }
                    
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionConfiguracion", "Error en descarga: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Error en descarga: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun cargarResumen() {
        Log.d("FragmentInspeccionConfiguracion", "cargarResumen llamado")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
            val db = AppDatabase.getDatabase(requireContext())
                val inspeccionDao = db.inspeccionDao()
                val all = inspeccionDao.getAllEquipos()
            val totalEquipos = all.size
                
                // Obtener la fecha del DatePicker de configuración
                val fechaConfiguracion = prefs.getString(DATE_KEY, null)
                val fechaLimite = if (fechaConfiguracion != null) {
                    parsearFecha(fechaConfiguracion)?.let { getMidnightDate(it) } ?: getMidnightDate(Date())
                } else {
                    // Si no hay fecha guardada, usar la fecha actual (comportamiento por defecto)
                    getMidnightDate(Date())
                }
                
                Log.d("FragmentInspeccionConfiguracion", "Fecha de configuración: $fechaConfiguracion")
                Log.d("FragmentInspeccionConfiguracion", "Fecha límite parseada: $fechaLimite")
                
                // Contar equipos inspeccionados (fecha igual o posterior a la fecha de configuración)
                val inspeccionados = all.count { equipo ->
                    val fechaInspeccion = equipo.fechaInspeccion
                    if (!fechaInspeccion.isNullOrEmpty()) {
                        val fechaInspeccionParsed = parsearFecha(fechaInspeccion)?.let { getMidnightDate(it) }
                        val esInspeccionado = fechaInspeccionParsed != null && (fechaInspeccionParsed.after(fechaLimite) || fechaInspeccionParsed.equals(fechaLimite))
                        
                        // Log detallado para los primeros 3 equipos
                        if (all.indexOf(equipo) < 3) {
                            Log.d("FragmentInspeccionConfiguracion", "Equipo ${equipo.id}: fechaInspeccion='$fechaInspeccion' -> fechaInspeccionParsed=$fechaInspeccionParsed")
                            Log.d("FragmentInspeccionConfiguracion", "Equipo ${equipo.id}: fechaLimite=$fechaLimite")
                            Log.d("FragmentInspeccionConfiguracion", "Equipo ${equipo.id}: esInspeccionado=$esInspeccionado")
                        }
                        
                        esInspeccionado
                    } else {
                        false
                    }
                }
                
                val modificados = inspeccionDao.getModificadasLocal()
                
                // Obtener estadísticas de la última importación
                val equiposIgnorados = prefs.getInt("equipos_ignorados_ultima_importacion", 0)
                val equiposActivos = prefs.getInt("equipos_activos_ultima_importacion", 0)
                val equiposMonitorizados = prefs.getInt("equipos_monitorizados_ultima_importacion", 0)
                val equiposAfsEliminados = prefs.getInt("equipos_afs_eliminados_ultima_importacion", 0)
                
                Log.d("FragmentInspeccionConfiguracion", "Resumen calculado:")
                Log.d("FragmentInspeccionConfiguracion", "  - Total equipos: $totalEquipos")
                Log.d("FragmentInspeccionConfiguracion", "  - Inspeccionados: $inspeccionados")
                Log.d("FragmentInspeccionConfiguracion", "  - Modificados: ${modificados.size}")
                Log.d("FragmentInspeccionConfiguracion", "  - Ignorados: $equiposIgnorados")
                Log.d("FragmentInspeccionConfiguracion", "  - Activos: $equiposActivos")
                Log.d("FragmentInspeccionConfiguracion", "  - Monitorizados: $equiposMonitorizados")
                Log.d("FragmentInspeccionConfiguracion", "  - AFS/Eliminados: $equiposAfsEliminados")
                
                // Contar fotos
                val fotosDao = db.fotoEquipoDao()
                val todasLasFotos = fotosDao.getAll()
                val fotosDrive = todasLasFotos.count { it.estadoSubida == "SUBIDA" }
                val fotosNuevas = todasLasFotos.count { it.estadoSubida == "LOCAL" }
                val fotosPendientes = todasLasFotos.count { it.estadoSubida == "PENDIENTE" }
                
                Log.d("FragmentInspeccionConfiguracion", "Fotos contadas:")
                Log.d("FragmentInspeccionConfiguracion", "  - En Drive: $fotosDrive")
                Log.d("FragmentInspeccionConfiguracion", "  - Nuevas (LOCAL): $fotosNuevas")
                Log.d("FragmentInspeccionConfiguracion", "  - Pendientes: $fotosPendientes")
                
            withContext(Dispatchers.Main) {
                txtTotalEquipos.text = "Total equipos: $totalEquipos"
                txtInspeccionados.text = "Equipos inspeccionados: $inspeccionados"
                    txtModificados.text = "Equipos modificados: ${modificados.size}"
                txtActivos.text = "Equipos activos: $equiposActivos"
                txtMonitorizados.text = "Equipos monitorizados: $equiposMonitorizados"
                txtAfsEliminados.text = "Equipos AFS/Eliminados: $equiposAfsEliminados"
                txtFotosDrive.text = "Fotos en Google Drive: $fotosDrive"
                txtFotosNuevas.text = "Fotos en móvil: $fotosNuevas"
                txtFotosPendientes.text = "Fotos pte. de subir: $fotosPendientes"
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error al cargar resumen: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al cargar resumen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    /**
     * Busca los datos de inspección (estado, fecha, inspector, detector, nota) para un TAG
     * en la hoja de inspección anterior a la seleccionada (si seleccionan "38", busca en "37")
     */
    private fun importarDatosDesdeSheet(header: List<String>, rows: List<List<Any>>) {
        Log.d("FragmentInspeccionConfiguracion", "importarDatosDesdeSheet llamado")
        val context = requireContext()
        
        // Obtener libroId y sheetsManager para buscar datos de inspección
        val libroId = prefs.getString(LIBRO_ID_KEY, null)
        
        // Primero, detectar campos de orden disponibles
        val camposOrdenDisponibles = header.filter { it.startsWith("orden_") }
        
        if (camposOrdenDisponibles.isNotEmpty()) {
            // Mostrar diálogo de selección de orden
            lifecycleScope.launch(Dispatchers.Main) {
                mostrarDialogoSeleccionOrden(camposOrdenDisponibles) { campoOrdenSeleccionado ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val sheetsManager = obtenerGoogleSheetsManager()
                        if (campoOrdenSeleccionado != null) {
                            // Guardar la selección del usuario
                            prefs.edit().putString("campo_orden_seleccionado", campoOrdenSeleccionado).apply()
                            // Continuar con la importación usando el campo de orden seleccionado
                            procesarImportacionDatos(header, rows, campoOrdenSeleccionado, libroId, sheetsManager)
                        } else {
                            // Usar orden_default como fallback
                            procesarImportacionDatos(header, rows, "orden_default", libroId, sheetsManager)
                        }
                    }
                }
            }
        } else {
            // No hay campos de orden, continuar sin orden
            lifecycleScope.launch(Dispatchers.IO) {
                val sheetsManager = obtenerGoogleSheetsManager()
                procesarImportacionDatos(header, rows, null, libroId, sheetsManager)
            }
        }
    }

    private fun mostrarDialogoSeleccionOrden(camposOrden: List<String>, onCampoSeleccionado: (String?) -> Unit) {
        Log.d("FragmentInspeccionConfiguracion", "mostrarDialogoSeleccionOrden llamado")
        val dialog = SeleccionOrdenDialog(
            context = requireContext(),
            camposOrden = camposOrden,
            campoActual = prefs.getString("campo_orden_seleccionado", "orden_default"),
            onCampoSeleccionado = { campoSeleccionado ->
                Log.d("FragmentInspeccionConfiguracion", "SELECT en SeleccionOrdenDialog: campo seleccionado=$campoSeleccionado")
                onCampoSeleccionado(campoSeleccionado)
            }
        )
        dialog.show()
    }
    
   

    private suspend fun procesarImportacionDatos(header: List<String>, rows: List<List<Any>>, campoOrdenSeleccionado: String?, libroId: String? = null, sheetsManager: GoogleSheetsManager? = null) {
        Log.e("FragmentInspeccionConfiguracion", "procesarImportacionDatos llamado - libroId=$libroId, sheetsManager=${sheetsManager != null}")
        val context = requireContext()
        
        // Cargar cache de índices
		val cacheIndices = dataProcessor.cargarCacheIndices()
        
        // Leer mapeo desde assets y preferencias usando el helper
        val (mapeoColumnas, mapeoPrefs) = dataProcessor.leerMapeoColumnas()
        
        // Crear mapeo automático usando el helper
        val mapeoAuto = dataProcessor.crearMapeoAutomatico(header, mapeoColumnas, mapeoPrefs, cacheIndices)
        
        // Guardar cache actualizado
        dataProcessor.guardarCacheIndices(cacheIndices)
        
        Log.d("FragmentInspeccionConfiguracion", "Mapeo final: $mapeoAuto")
        
        // Log detallado del mapeo para campos críticos (después de completar el mapeo automático)
        val camposCriticos = listOf("area", "unidad", "instalacion", "ubicacion", "marca", "modelo", "manifold", "estado", "flota")
        Log.d("FragmentInspeccionConfiguracion", "=== MAPEO DETALLADO CAMPOS CRÍTICOS ===")
        for (campo in camposCriticos) {
            val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
            val columnaManual = mapeoPrefs[campo]
            val columnaAuto = mapeoAuto[campo]
            val columnaFinal = columnaJson ?: columnaManual ?: columnaAuto ?: campo
            val idx = header.indexOf(columnaFinal)
            Log.d("FragmentInspeccionConfiguracion", "Campo '$campo': JSON=$columnaJson, Manual=$columnaManual, Auto=$columnaAuto, Final='$columnaFinal', idx=$idx")
        }
        Log.d("FragmentInspeccionConfiguracion", "=== FIN MAPEO DETALLADO ===")
        
        // Mostrar diagnóstico de mapeo UNA SOLA VEZ antes de comenzar la importación
        Log.d("FragmentInspeccionConfiguracion", "=== DIAGNÓSTICO MAPEO (una vez) ===")
        Log.d("FragmentInspeccionConfiguracion", "Campo 'marca': JSON=${mapeoColumnas.optString("marca", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["marca"]}, Auto=${mapeoAuto["marca"]}")
        Log.d("FragmentInspeccionConfiguracion", "Campo 'modelo': JSON=${mapeoColumnas.optString("modelo", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["modelo"]}, Auto=${mapeoAuto["modelo"]}")
        Log.d("FragmentInspeccionConfiguracion", "Campo 'tipo': JSON=${mapeoColumnas.optString("tipo", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["tipo"]}, Auto=${mapeoAuto["tipo"]}")
        Log.d("FragmentInspeccionConfiguracion", "Campo 'area': JSON=${mapeoColumnas.optString("area", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["area"]}, Auto=${mapeoAuto["area"]}")
        Log.d("FragmentInspeccionConfiguracion", "Campo 'unidad': JSON=${mapeoColumnas.optString("unidad", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["unidad"]}, Auto=${mapeoAuto["unidad"]}")
        
        // Solo pedir mapeo para los campos que no se pudieron mapear automáticamente
        // Excluir 'gps' (se maneja de forma especial) y 'estado' (viene de la hoja anterior, no de FLOTA)
        val camposFaltantes = InspeccionDataProcessor.CAMPOS_OBLIGATORIOS.filter { campo ->
            campo != "gps" && campo != "estado" && mapeoAuto[campo] == null
        }
        if (camposFaltantes.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.Main) {
                mostrarDialogoMapeo(camposFaltantes, header) { nuevoMapeo ->
                    if (nuevoMapeo == null) {
                        Toast.makeText(context, "Importación cancelada. Faltan campos obligatorios.", Toast.LENGTH_LONG).show()
                        setLoading(false)
                        return@mostrarDialogoMapeo
                    }
                    val mapeoFinal = mapeoPrefs.toMutableMap().apply { putAll(nuevoMapeo) }
                    prefs.edit().putStringSet("mapeo_inspeccion", mapeoFinal.map { "${it.key}:${it.value}" }.toSet()).apply()
                    lifecycleScope.launch(Dispatchers.IO) {
                        val sheetsManager = obtenerGoogleSheetsManager()
                        procesarImportacionDatos(header, rows, campoOrdenSeleccionado, libroId, sheetsManager)
                    }
                }
            }
            return
        }
        
        // Función helper para obtener valores usando el mapeo
        val get = { campo: String, row: List<Any> ->
            val columnaManual = mapeoPrefs[campo]
            val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
            val columnaAuto = mapeoAuto[campo]
            val columna = columnaJson ?: columnaManual ?: columnaAuto ?: campo
            val idx = header.indexOf(columna)
            
            // Si no se encuentra, intentar búsqueda case-insensitive
            val idxFinal = if (idx == -1) {
                header.indexOfFirst { it.toString().trim().equals(columna, ignoreCase = true) }
            } else {
                idx
            }
            
            val valor = if (idxFinal != -1 && row.size > idxFinal) row[idxFinal].toString().trim() else ""
            
            // Log de advertencia si no se encuentra la columna (solo para campos críticos)
            if (idxFinal == -1 && campo in listOf("area", "unidad", "instalacion", "ubicacion", "marca", "modelo", "tipo", "estado")) {
                Log.w("FragmentInspeccionConfiguracion", "get($campo): Columna '$columna' NO ENCONTRADA. JSON=$columnaJson, Manual=$columnaManual, Auto=$columnaAuto")
            }
            
            valor
        }
        
        // Obtener índices de columnas clave usando el mapeo (una sola vez)
        val tagColumna = mapeoPrefs["id"] ?: mapeoColumnas.optString("id", "TAG") ?: mapeoAuto["id"] ?: "TAG"
        val areaColumna = mapeoPrefs["area"] ?: mapeoColumnas.optString("area", "AREA") ?: mapeoAuto["area"] ?: "AREA"
        val unidadColumna = mapeoPrefs["unidad"] ?: mapeoColumnas.optString("unidad", "UNIDAD") ?: mapeoAuto["unidad"] ?: "UNIDAD"
        
        val tagIndex = header.indexOf(tagColumna)
        val areaIndex = header.indexOf(areaColumna)
        val unidadIndex = header.indexOf(unidadColumna)
        
        Log.d("FragmentInspeccionConfiguracion", "Índices de columnas clave: TAG($tagColumna)=$tagIndex, AREA($areaColumna)=$areaIndex, UNIDAD($unidadColumna)=$unidadIndex")
        Log.d("FragmentInspeccionConfiguracion", "Total de filas a procesar: ${rows.size}")
        
        // Obtener hoja de inspección seleccionada para buscar datos en hoja anterior
        val hojaInspeccionSeleccionada = prefs.getString(HOJA_INSPECCION_KEY, null)
		
		// Cargar todos los datos de la hoja anterior de una vez (optimización)
		Log.e("FragmentInspeccionConfiguracion", "=== INICIO CARGA HOJA ANTERIOR ===")
		Log.e("FragmentInspeccionConfiguracion", "libroId: $libroId")
		Log.e("FragmentInspeccionConfiguracion", "sheetsManager: ${if (sheetsManager != null) "NO NULL" else "NULL"}")
		Log.e("FragmentInspeccionConfiguracion", "hojaInspeccionSeleccionada: $hojaInspeccionSeleccionada")
		val datosInspeccionAnteriorMap = dataProcessor.cargarDatosInspeccionAnterior(libroId, sheetsManager, hojaInspeccionSeleccionada)
		Log.e("FragmentInspeccionConfiguracion", "=== FIN CARGA HOJA ANTERIOR ===")
		Log.e("FragmentInspeccionConfiguracion", "Registros cargados: ${datosInspeccionAnteriorMap.size}")
		if (datosInspeccionAnteriorMap.isNotEmpty()) {
			val primeros5 = datosInspeccionAnteriorMap.entries.take(5)
			Log.e("FragmentInspeccionConfiguracion", "Primeros 5 registros: $primeros5")
		} else {
			Log.e("FragmentInspeccionConfiguracion", "ERROR: No se cargaron datos de la hoja anterior. El mapa está vacío.")
		}
        
 
        
        // Procesar filas y guardar en la base de datos - filtrar solo filas con datos de equipos
        var filasProcesadas = 0
        var filasSaltadas = 0
        val equipos = mutableListOf<com.bithermmanagement.database.entities.Equipo>()
        
        for (row in rows) {
            try {
                // Verificar si esta fila contiene datos de equipo usando el mapeo de columnas
                val tagValue = if (tagIndex != -1 && row.size > tagIndex) row[tagIndex].toString().trim() else ""
                
                // Solo procesar filas que tengan TAG (AREA y UNIDAD son opcionales)
                if (tagValue.isEmpty()) {
                    filasSaltadas++
                    // Log detallado de por qué se ignora (solo las primeras 5)
                    if (filasSaltadas <= 5) {
                        val rowPreview = row.take(5).joinToString("|") { it.toString().trim() }
                        Log.d("FragmentInspeccionConfiguracion", "Saltando fila $filasSaltadas sin TAG. Preview: $rowPreview")
                    }
                    continue
                }
                
                filasProcesadas++
                
                // Log detallado: solo los primeros 10 items con todas las columnas separadas por |
                if (filasProcesadas <= 10) {
                    val valoresColumnas = row.mapIndexed { idx, valor ->
                        val headerName = if (idx < header.size) header[idx] else "COL_$idx"
                        "$headerName=${valor.toString().trim()}"
                    }.joinToString("|")
                    Log.d("FragmentInspeccionConfiguracion", "Item $filasProcesadas: $valoresColumnas")
                }
                
                val getRow = { campo: String ->
                    val valor = get(campo, row)
                    // Log detallado para campos importantes (solo en las primeras 5 filas)
                    if (filasProcesadas <= 5 && campo in listOf("area", "unidad", "instalacion", "ubicacion", "marca", "modelo", "estado")) {
                        val columnaManual = mapeoPrefs[campo]
                        val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
                        val columnaAuto = mapeoAuto[campo]
                        val columnaFinal = columnaJson ?: columnaManual ?: columnaAuto ?: campo
                        val idx = header.indexOf(columnaFinal)
                        Log.d("FragmentInspeccionConfiguracion", "getRow($campo): columnaJson=$columnaJson, columnaManual=$columnaManual, columnaAuto=$columnaAuto, columnaFinal=$columnaFinal, idx=$idx, valor='$valor'")
                    }
                    valor
                }
                // Obtener el valor del campo de orden seleccionado
                val valorOrden = if (campoOrdenSeleccionado != null) {
                    val idx = header.indexOf(campoOrdenSeleccionado)
                    if (idx != -1 && row.size > idx) row[idx].toString().toDoubleOrNull() else 0.0
                } else {
                    0.0
                }
                
                // Obtener todos los valores con mapeo detallado
                val idVal = getRow("id").ifEmpty { tagValue }
                val instalacionVal = getRow("instalacion")
                val unidadVal = getRow("unidad")
                val areaVal = getRow("area")
                val lineaVal = getRow("linea")
                val marcaVal = getRow("marca")
                val modeloVal = getRow("modelo")
                val tipoVal = getRow("tipo")
                val ubicacionVal = getRow("ubicacion")
                
                // Obtener estado de la hoja anterior desde el mapa (optimizado)
                val datosInspeccionAnterior = datosInspeccionAnteriorMap[idVal.uppercase()]
                val estadoVal = datosInspeccionAnterior?.estado ?: ""
                
                // Log para los primeros 10 equipos para verificar la carga de estado
                if (filasProcesadas <= 10) {
                    Log.e("FragmentInspeccionConfiguracion", "Equipo $idVal: datosInspeccionAnterior=${datosInspeccionAnterior != null}, estado='$estadoVal', mapaSize=${datosInspeccionAnteriorMap.size}")
                }
                val fechaInspeccion = datosInspeccionAnterior?.fecha ?: getRow("fechaInspeccion")
                val identidadInspector = datosInspeccionAnterior?.inspector ?: getRow("identidadInspector")
                val detectorUtilizado = datosInspeccionAnterior?.detector ?: getRow("detectorUtilizado")
                val notaInspeccion = datosInspeccionAnterior?.nota ?: getRow("nota")
                
                // Obtener FLOTA y normalizarlo (no se usa para colorear, solo para mostrar)
                val flotaRaw = getRow("flota")
                val flotaValue = flotaRaw.uppercase().trim()
                
                // Log detallado para los primeros 20 equipos
                if (filasProcesadas <= 20) {
                    val columnaJson = if (mapeoColumnas.has("flota")) mapeoColumnas.getString("flota") else null
                    val columnaManual = mapeoPrefs["flota"]
                    val columnaAuto = mapeoAuto["flota"]
                    val columnaFinal = columnaJson ?: columnaManual ?: columnaAuto ?: "flota"
                    val idx = header.indexOf(columnaFinal)
                    val idxFinal = if (idx == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinal, ignoreCase = true) }
                    } else {
                        idx
                    }
                    val valorReal = if (idxFinal != -1 && row.size > idxFinal) row[idxFinal].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(flota): columnaJson=$columnaJson, columnaManual=$columnaManual, columnaAuto=$columnaAuto, columnaFinal=$columnaFinal, idx=$idxFinal, valorRaw='$flotaRaw', valorReal='$valorReal', flotaValue='$flotaValue'")
                }
                
                val flotaNormalizado = when {
                    flotaValue.isEmpty() || flotaValue == "ACT" || flotaValue == "ACTIVO" -> "ACTIVO"
                    flotaValue == "MON" || flotaValue == "MONITORIZADO" -> "MONITORIZADO"
                    flotaValue == "AFS" -> "AFS"
                    flotaValue == "ELIM" || flotaValue.contains("ELIMINADO", ignoreCase = true) || flotaValue.contains("ELIMIN", ignoreCase = true) -> "ELIMINADO"
                    else -> if (flotaValue.isNotEmpty()) flotaValue else "ACTIVO" // Por defecto ACTIVO si está vacío
                }
                
                // Log para verificar la flota normalizada
                if (filasProcesadas <= 20) {
                    Log.d("FragmentInspeccionConfiguracion", "Flota normalizada: '$flotaRaw' -> '$flotaValue' -> '$flotaNormalizado'")
                }
                
                // Log detallado para estado (similar a flota)
                if (filasProcesadas <= 20) {
                    val columnaJson = if (mapeoColumnas.has("estado")) mapeoColumnas.getString("estado") else null
                    val columnaManual = mapeoPrefs["estado"]
                    val columnaAuto = mapeoAuto["estado"]
                    val columnaFinal = columnaJson ?: columnaManual ?: columnaAuto ?: "estado"
                    val idx = header.indexOf(columnaFinal)
                    val idxFinal = if (idx == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinal, ignoreCase = true) }
                    } else {
                        idx
                    }
                    val valorReal = if (idxFinal != -1 && row.size > idxFinal) row[idxFinal].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(estado): columnaJson=$columnaJson, columnaManual=$columnaManual, columnaAuto=$columnaAuto, columnaFinal=$columnaFinal, idx=$idxFinal, valorRaw='$estadoVal', valorReal='$valorReal'")
                }
                
                // Log detallado ANTES de guardar en BD para los primeros 20 equipos
                if (filasProcesadas <= 20) {
                    Log.d("FragmentInspeccionConfiguracion", "=== ANTES DE GUARDAR EN BD - Equipo #$filasProcesadas: $idVal ===")
                    Log.d("FragmentInspeccionConfiguracion", "MARCA: valor='$marcaVal'")
                    Log.d("FragmentInspeccionConfiguracion", "MODELO: valor='$modeloVal'")
                    Log.d("FragmentInspeccionConfiguracion", "UBICACION: valor='$ubicacionVal'")
                    Log.d("FragmentInspeccionConfiguracion", "ESTADO: valor='$estadoVal' (¿está vacío? ${estadoVal.isEmpty()})")
                    Log.d("FragmentInspeccionConfiguracion", "FLOTA: valor='$flotaNormalizado'")
                }
                
                // Log detallado de marca, modelo, ubicacion y estado para los primeros 20 equipos (comparación)
                if (filasProcesadas <= 20) {
                    // Log para MARCA
                    val columnaJsonMarca = if (mapeoColumnas.has("marca")) mapeoColumnas.getString("marca") else null
                    val columnaManualMarca = mapeoPrefs["marca"]
                    val columnaAutoMarca = mapeoAuto["marca"]
                    val columnaFinalMarca = columnaJsonMarca ?: columnaManualMarca ?: columnaAutoMarca ?: "marca"
                    val idxMarca = header.indexOf(columnaFinalMarca)
                    val idxFinalMarca = if (idxMarca == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinalMarca, ignoreCase = true) }
                    } else {
                        idxMarca
                    }
                    val valorRealMarca = if (idxFinalMarca != -1 && row.size > idxFinalMarca) row[idxFinalMarca].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(MARCA): columnaJson=$columnaJsonMarca, columnaManual=$columnaManualMarca, columnaAuto=$columnaAutoMarca, columnaFinal=$columnaFinalMarca, idx=$idxFinalMarca, valor='$marcaVal', valorReal='$valorRealMarca'")
                    
                    // Log para MODELO
                    val columnaJsonModelo = if (mapeoColumnas.has("modelo")) mapeoColumnas.getString("modelo") else null
                    val columnaManualModelo = mapeoPrefs["modelo"]
                    val columnaAutoModelo = mapeoAuto["modelo"]
                    val columnaFinalModelo = columnaJsonModelo ?: columnaManualModelo ?: columnaAutoModelo ?: "modelo"
                    val idxModelo = header.indexOf(columnaFinalModelo)
                    val idxFinalModelo = if (idxModelo == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinalModelo, ignoreCase = true) }
                    } else {
                        idxModelo
                    }
                    val valorRealModelo = if (idxFinalModelo != -1 && row.size > idxFinalModelo) row[idxFinalModelo].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(MODELO): columnaJson=$columnaJsonModelo, columnaManual=$columnaManualModelo, columnaAuto=$columnaAutoModelo, columnaFinal=$columnaFinalModelo, idx=$idxFinalModelo, valor='$modeloVal', valorReal='$valorRealModelo'")
                    
                    // Log para UBICACION
                    val columnaJsonUbicacion = if (mapeoColumnas.has("ubicacion")) mapeoColumnas.getString("ubicacion") else null
                    val columnaManualUbicacion = mapeoPrefs["ubicacion"]
                    val columnaAutoUbicacion = mapeoAuto["ubicacion"]
                    val columnaFinalUbicacion = columnaJsonUbicacion ?: columnaManualUbicacion ?: columnaAutoUbicacion ?: "ubicacion"
                    val idxUbicacion = header.indexOf(columnaFinalUbicacion)
                    val idxFinalUbicacion = if (idxUbicacion == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinalUbicacion, ignoreCase = true) }
                    } else {
                        idxUbicacion
                    }
                    val valorRealUbicacion = if (idxFinalUbicacion != -1 && row.size > idxFinalUbicacion) row[idxFinalUbicacion].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(UBICACION): columnaJson=$columnaJsonUbicacion, columnaManual=$columnaManualUbicacion, columnaAuto=$columnaAutoUbicacion, columnaFinal=$columnaFinalUbicacion, idx=$idxFinalUbicacion, valor='$ubicacionVal', valorReal='$valorRealUbicacion'")
                    
                    // Log para ESTADO (comparación con los anteriores)
                    val columnaJsonEstado = if (mapeoColumnas.has("estado")) mapeoColumnas.getString("estado") else null
                    val columnaManualEstado = mapeoPrefs["estado"]
                    val columnaAutoEstado = mapeoAuto["estado"]
                    val columnaFinalEstado = columnaJsonEstado ?: columnaManualEstado ?: columnaAutoEstado ?: "estado"
                    val idxEstado = header.indexOf(columnaFinalEstado)
                    val idxFinalEstado = if (idxEstado == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinalEstado, ignoreCase = true) }
                    } else {
                        idxEstado
                    }
                    val valorRealEstado = if (idxFinalEstado != -1 && row.size > idxFinalEstado) row[idxFinalEstado].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(ESTADO): columnaJson=$columnaJsonEstado, columnaManual=$columnaManualEstado, columnaAuto=$columnaAutoEstado, columnaFinal=$columnaFinalEstado, idx=$idxFinalEstado, valor='$estadoVal', valorReal='$valorRealEstado'")
                    
                    // Log para INSTALACION
                    val columnaJsonInstalacion = if (mapeoColumnas.has("instalacion")) mapeoColumnas.getString("instalacion") else null
                    val columnaManualInstalacion = mapeoPrefs["instalacion"]
                    val columnaAutoInstalacion = mapeoAuto["instalacion"]
                    val columnaFinalInstalacion = columnaJsonInstalacion ?: columnaManualInstalacion ?: columnaAutoInstalacion ?: "instalacion"
                    val idxInstalacion = header.indexOf(columnaFinalInstalacion)
                    val idxFinalInstalacion = if (idxInstalacion == -1) {
                        header.indexOfFirst { it.toString().trim().equals(columnaFinalInstalacion, ignoreCase = true) }
                    } else {
                        idxInstalacion
                    }
                    val valorRealInstalacion = if (idxFinalInstalacion != -1 && row.size > idxFinalInstalacion) row[idxFinalInstalacion].toString().trim() else ""
                    Log.d("FragmentInspeccionConfiguracion", "getRow(INSTALACION): columnaJson=$columnaJsonInstalacion, columnaManual=$columnaManualInstalacion, columnaAuto=$columnaAutoInstalacion, columnaFinal=$columnaFinalInstalacion, idx=$idxFinalInstalacion, valor='$instalacionVal', valorReal='$valorRealInstalacion'")
                }
                
                // Log detallado del mapeo para los primeros 20 equipos
                if (filasProcesadas <= 20) {
                    Log.d("FragmentInspeccionConfiguracion", "=== MAPEO EQUIPO #$filasProcesadas: $idVal ===")
                    val camposMapeo = mapOf(
                        "id" to idVal,
                        "area" to areaVal,
                        "unidad" to unidadVal,
                        "instalacion" to instalacionVal,
                        "linea" to lineaVal,
                        "ubicacion" to ubicacionVal,
                        "marca" to marcaVal,
                        "modelo" to modeloVal,
                        "tipo" to tipoVal,
                        "estado" to estadoVal,
                        "flota" to flotaNormalizado
                    )
                    camposMapeo.forEach { (campo, valor) ->
                        val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
                        val columnaManual = mapeoPrefs[campo]
                        val columnaAuto = mapeoAuto[campo]
                        val columnaFinal = columnaJson ?: columnaManual ?: columnaAuto ?: campo
                        val idx = header.indexOf(columnaFinal)
                        Log.d("FragmentInspeccionConfiguracion", "  Campo BD '$campo' <- Columna GS '$columnaFinal' (idx=$idx) = '$valor'")
                    }
                    Log.d("FragmentInspeccionConfiguracion", "=== FIN MAPEO EQUIPO #$filasProcesadas ===")
                }
                
                val equipo = com.bithermmanagement.database.entities.Equipo(
                    id = idVal,
                    instalacion = instalacionVal,
                    unidad = unidadVal,
                    area = areaVal,
                    linea = lineaVal,
                    marca = marcaVal,
                    modelo = modeloVal,
                    tipo = tipoVal,
                    periodicidad = getRow("p"),
                    diametro = getRow("diametro"),
                    conexion = getRow("conexion"),
                    aislamiento = getRow("aislamiento"),
                    presEntrada = getRow("presEntrada"),
                    presSalida = getRow("presSalida"),
                    byPass = getRow("byPass").equals("true", ignoreCase = true),
                    descarga = getRow("descarga"),
                    aplicacion = getRow("aplicacion"),
                    servicio = getRow("servicio"),
                    ubicacion = ubicacionVal,
                    estado = estadoVal,
                    flota = flotaNormalizado,
                    fechaInspeccion = fechaInspeccion,
                    nota = notaInspeccion,
                    identidadInspector = identidadInspector,
                    detectorUtilizado = detectorUtilizado,
                    incidencias = getRow("incidencias"),
                    gpsCoord = getRow("gps"),
                    urlFotoEquipo = getRow("foto"),
                    urlFotoUbicacion = getRow("fotoUbic"),
                    orden = valorOrden,
                    gpsAcc = getRow("gpsAcc"),
                    extra = null,
                    modificadoLocal = false,
                    instalacionMf = getRow("instalacionMf"),
                    urlFotoManifold = null,
                    urlFotosExtra = null
                )
                
                equipos.add(equipo)
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error importando fila: ${e.message}", e)
            }
        }
        
        Log.d("FragmentInspeccionConfiguracion", "Importación completada: $filasProcesadas equipos procesados, $filasSaltadas filas saltadas de ${rows.size} totales")
        
        // Contar equipos por FLOTA antes de guardar
        val activos = equipos.count { it.flota.isNullOrEmpty() || it.flota == "ACTIVO" || it.flota == "ACT" }
        val monitorizados = equipos.count { it.flota == "MONITORIZADO" || it.flota == "MON" }
        
        // Contar AFS y ELIMINADOS por separado y luego sumarlos
        val afs = equipos.count { equipo ->
            val flota = equipo.flota?.uppercase()?.trim() ?: ""
            flota == "AFS"
        }
        val eliminados = equipos.count { equipo ->
            val flota = equipo.flota?.uppercase()?.trim() ?: ""
            flota == "ELIMINADO" || 
            flota == "ELIM" ||
            flota.contains("ELIMINADO", ignoreCase = true) ||
            flota.contains("ELIMIN", ignoreCase = true)
        }
        val afsEliminados = afs + eliminados
        
        Log.d("FragmentInspeccionConfiguracion", "Conteo STATUS: AFS=$afs, ELIMINADOS=$eliminados, TOTAL=$afsEliminados")
        
        // Guardar estadísticas en SharedPreferences para mostrarlas en el resumen
        prefs.edit().apply {
            putInt("equipos_ignorados_ultima_importacion", filasSaltadas)
            putInt("equipos_activos_ultima_importacion", activos)
            putInt("equipos_monitorizados_ultima_importacion", monitorizados)
            putInt("equipos_afs_eliminados_ultima_importacion", afsEliminados)
        }.apply()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                
                // Log detallado de los primeros 10 equipos antes de guardar
                Log.d("FragmentInspeccionConfiguracion", "=== ANTES DE GUARDAR EN BD ===")
                equipos.take(10).forEachIndexed { idx, equipo ->
                    Log.d("FragmentInspeccionConfiguracion", "Equipo #${idx + 1} antes de upsertAll: id=${equipo.id}, area='${equipo.area}', unidad='${equipo.unidad}', instalacion='${equipo.instalacion}', ubicacion='${equipo.ubicacion}', marca='${equipo.marca}', modelo='${equipo.modelo}', estado='${equipo.estado}', flota='${equipo.flota}'")
                }
                Log.d("FragmentInspeccionConfiguracion", "Total equipos a guardar: ${equipos.size}")
                
                db.equipoDao().upsertAll(equipos)
                
                Log.d("FragmentInspeccionConfiguracion", "=== DESPUÉS DE GUARDAR EN BD ===")
                // Verificar los primeros 10 equipos después de guardar
                val equiposVerificados = db.inspeccionDao().getAllEquiposFull().take(10)
                equiposVerificados.forEachIndexed { idx, equipo ->
                    Log.d("FragmentInspeccionConfiguracion", "Equipo #${idx + 1} después de upsertAll: id=${equipo.id}, area='${equipo.area}', unidad='${equipo.unidad}', instalacion='${equipo.instalacion}', ubicacion='${equipo.ubicacion}', marca='${equipo.marca}', modelo='${equipo.modelo}', estado='${equipo.estado}', flota='${equipo.flota}'")
                }
                
                // Obtener el total real después del upsert para corregir la discrepancia
                val totalReal = db.inspeccionDao().getAllEquipos().size
                
                withContext(Dispatchers.Main) {
                    val mensaje = if (campoOrdenSeleccionado != null) {
                        "Importación completada: $totalReal registros (Orden: $campoOrdenSeleccionado)"
                    } else {
                        "Importación completada: $totalReal registros"
                    }
                    Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
                    setLoading(false)
                    cargarResumen()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error en importación: ${e.message}", Toast.LENGTH_LONG).show()
                    setLoading(false)
                }
            }
        }
    }

    private fun mostrarDialogoMapeo(camposFaltantes: List<String>, header: List<String>, onMapeoFinalizado: (Map<String, String>?) -> Unit) {
        Log.d("FragmentInspeccionConfiguracion", "mostrarDialogoMapeo llamado")
        val context = requireContext()
        val mapeoSeleccionado = mutableMapOf<String, String>()
        val views = camposFaltantes.map { campo ->
            val layout = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            val label = TextView(context).apply { text = "$campo:"; setPadding(8, 8, 8, 8) }
            val spinner = Spinner(context)
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, header)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
            layout.addView(label)
            layout.addView(spinner)
            campo to spinner
        }
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            views.forEach { addView(it.second.parent as View) }
        }
        AlertDialog.Builder(context)
            .setTitle("Mapeo de columnas")
            .setMessage("Selecciona la columna real para cada campo obligatorio:")
            .setView(container)
            .setPositiveButton("Aceptar") { dialog, _ ->
                views.forEach { (campo, spinner) ->
                    val seleccion = spinner.selectedItem as? String
                    if (seleccion != null) mapeoSeleccionado[campo] = seleccion
                }
                if (mapeoSeleccionado.size == camposFaltantes.size) {
                    onMapeoFinalizado(mapeoSeleccionado)
                } else {
                    onMapeoFinalizado(null)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                onMapeoFinalizado(null)
                dialog.dismiss()
            }
            .show()
    }

    private fun obtenerLibroId(libroNombre: String): String? {
        return libros.find { it.name == libroNombre }?.id
    }

    private suspend fun obtenerHojaId(libroId: String, hojaNombre: String): String? {
        return try {
            val sheetsManager = obtenerGoogleSheetsManager()
            if (sheetsManager == null) {
                Log.e("FragmentInspeccionConfiguracion", "No se pudo crear GoogleSheetsManager")
                return null
            }
            val hojasList = sheetsManager.listarHojasConInfo(libroId)
            val hoja = hojasList.find { it.name.equals(hojaNombre, ignoreCase = true) }
            hoja?.id
        } catch (e: Exception) {
            Log.e("FragmentInspeccionConfiguracion", "Error obteniendo ID de hoja: ${e.message}", e)
            null
        }
    }
}
