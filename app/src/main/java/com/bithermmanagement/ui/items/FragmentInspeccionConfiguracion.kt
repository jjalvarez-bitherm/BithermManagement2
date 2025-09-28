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
    private lateinit var spinnerLibro: Spinner
    private lateinit var spinnerHoja: Spinner
    private lateinit var spinnerDb: Spinner
    private lateinit var spinnerInspecciones: Spinner
    private lateinit var spinnerNumeroInspeccion: Spinner
    private lateinit var spinnerReparaciones: Spinner
    private lateinit var btnDescargar: MaterialButton
    private lateinit var btnActualizar: MaterialButton
    private lateinit var btnDescargarFotos: MaterialButton
    private lateinit var btnActualizarFotos: MaterialButton
    private lateinit var btnCopiarDB: MaterialButton
    private lateinit var btnCalibrarPantalla: MaterialButton
    private lateinit var btnBuscarActualizaciones: MaterialButton
    private lateinit var txtTotalEquipos: TextView
    private lateinit var txtInspeccionados: TextView
    private lateinit var txtModificados: TextView
    private lateinit var txtFotosDrive: TextView
    private lateinit var txtFotosNuevas: TextView
    private lateinit var txtFotosPendientes: TextView
    private lateinit var imgBorrar: ImageView
    private lateinit var progressBar: ProgressBar

    private lateinit var prefs: SharedPreferences
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

    private val CAMPOS_OBLIGATORIOS = listOf(
        "id", "area", "unidad", "manifold", "lineaEquipo", "ubicacion", "gpsCoord", "gpsAcc", 
        "marca", "modelo", "tipo", "diametro", "conexion", "presEntrada", "presSalida", "byPass", 
        "aislamiento", "descarga", "aplicacion", "foto", "fotoUbic", "fotoMf", "fotoExtra", 
        "orden", "ordenJuan", "ordenPaco", "ordenTome", "badActors", "modificaciones"
    )
    
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
        bindViews(view)
        setupTitulo()
        val fechaGuardada = prefs.getString(DATE_KEY, null)
        val libroGuardado = prefs.getString(LIBRO_KEY, null)
        val hojaGuardada = prefs.getString(HOJA_KEY, null)
        // Los campos siempre estarán habilitados salvo que explícitamente se bloqueen tras descargar
        setupFecha(fechaGuardada, false)
        setupDropdownsGoogle(libroGuardado, hojaGuardada, false)
        configurarSpinnerDb()
        configurarSpinnerNumeroInspeccion()
        configurarSpinnerReparaciones()
        setupBotones()
        spinnerLibro.isEnabled = true
        spinnerHoja.isEnabled = true
        txtFecha.isEnabled = true
        cargarResumen()
    }

    private fun bindViews(view: View) {
        txtTitulo = view.findViewById(R.id.txtTitulo)
        txtFecha = view.findViewById(R.id.txtFecha)
        spinnerLibro = view.findViewById(R.id.spinnerLibro)
        spinnerHoja = view.findViewById(R.id.spinnerHoja)
        spinnerDb = view.findViewById(R.id.spinnerDb)
        spinnerInspecciones = view.findViewById(R.id.spinnerInspecciones)
        spinnerNumeroInspeccion = view.findViewById(R.id.spinnerNumeroInspeccion)
        spinnerReparaciones = view.findViewById(R.id.spinnerReparaciones)
        btnDescargar = view.findViewById(R.id.btnDescargar)
        btnActualizar = view.findViewById(R.id.btnActualizar)
        btnDescargarFotos = view.findViewById(R.id.btnDescargarFotos)
        btnActualizarFotos = view.findViewById(R.id.btnActualizarFotos)
        btnCopiarDB = view.findViewById(R.id.btnCopiarDB)
        btnCalibrarPantalla = view.findViewById(R.id.btnCalibrarPantalla)
        btnBuscarActualizaciones = view.findViewById(R.id.btnBuscarActualizaciones)
        txtTotalEquipos = view.findViewById(R.id.txtTotalEquipos)
        txtInspeccionados = view.findViewById(R.id.txtInspeccionados)
        txtModificados = view.findViewById(R.id.txtModificados)
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

    private fun configurarSpinnerDb() {
        Log.d("FragmentInspeccionConfiguracion", "=== INICIANDO CONFIGURACIÓN SPINNER DB ===")
        Log.d("FragmentInspeccionConfiguracion", "Fragment context: ${requireContext()}")
        Log.d("FragmentInspeccionConfiguracion", "Spinner DB: $spinnerDb")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                Log.d("FragmentInspeccionConfiguracion", "Context obtenido")
                
                val settingsManager = SettingsManager(context)
                val settings = settingsManager.getSettings()
                Log.d("FragmentInspeccionConfiguracion", "Settings obtenidos: useOAuth=${settings.useOAuth}, oAuthEmail=${settings.oAuthEmail}")
                
                val credentialsStream = if (settings.useOAuth) {
                    null // Para OAuth no necesitamos credenciales
                } else {
                    settingsManager.getCredentialsInputStream() // Para Service Account sí necesitamos credenciales
                }
                
                Log.d("FragmentInspeccionConfiguracion", "Creando GoogleDriveManager...")
                val driveManager = GoogleDriveManager(
                    credentialsStream = credentialsStream,
                    context = context,
                    useOAuth = settings.useOAuth,
                    oAuthEmail = settings.oAuthEmail
                )
                Log.d("FragmentInspeccionConfiguracion", "GoogleDriveManager creado exitosamente")
                
                val oAuthEmail = settings.oAuthEmail
                if (oAuthEmail.isNullOrEmpty()) {
                    Log.e("FragmentInspeccionConfiguracion", "No hay email OAuth configurado")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No hay email OAuth configurado", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                Log.d("FragmentInspeccionConfiguracion", "Listando spreadsheets...")
                
                // Buscar TODOS los spreadsheets - usar una búsqueda más amplia
                val spreadsheets = try {
                    // Usar el método existente pero filtrar por "LISTADOS" en lugar de "(APP)"
                    val spreadsheetsApp = driveManager.listarSpreadsheetsApp()
                    Log.d("FragmentInspeccionConfiguracion", "Spreadsheets con (APP): ${spreadsheetsApp.size}")
                    Log.d("FragmentInspeccionConfiguracion", "Nombres encontrados: ${spreadsheetsApp.map { it.name }}")
                    
                    // Filtrar por "LISTADOS" en lugar de "(APP)"
                    val spreadsheetsListados = spreadsheetsApp.filter { it.name.contains("LISTADOS", ignoreCase = true) }
                    Log.d("FragmentInspeccionConfiguracion", "Spreadsheets con LISTADOS: ${spreadsheetsListados.size}")
                    
                    // Si no encontramos con "LISTADOS", usar todos los que contienen "(APP)"
                    if (spreadsheetsListados.isEmpty()) {
                        Log.d("FragmentInspeccionConfiguracion", "No se encontraron archivos con LISTADOS, usando todos los (APP)")
                        spreadsheetsApp
                    } else {
                        spreadsheetsListados
                    }
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionConfiguracion", "Error listando spreadsheets: ${e.message}", e)
                    emptyList()
                }
                
                Log.d("FragmentInspeccionConfiguracion", "Total spreadsheets encontrados: ${spreadsheets.size}")
                Log.d("FragmentInspeccionConfiguracion", "Nombres de spreadsheets: ${spreadsheets.map { it.name }}")
                
                val librosDbInspecciones = spreadsheets.filter { it.name.contains("LISTADOS", ignoreCase = true) }
                Log.d("FragmentInspeccionConfiguracion", "Libros DB inspecciones encontrados: ${librosDbInspecciones.size}")
                Log.d("FragmentInspeccionConfiguracion", "Nombres de libros DB: ${librosDbInspecciones.map { it.name }}")
                
                // Logging adicional para diagnosticar el filtrado
                Log.d("FragmentInspeccionConfiguracion", "=== DIAGNÓSTICO FILTRADO ===")
                spreadsheets.forEach { spreadsheet ->
                    val contieneListados = spreadsheet.name.contains("LISTADOS", ignoreCase = true)
                    Log.d("FragmentInspeccionConfiguracion", "Spreadsheet: '${spreadsheet.name}' -> contiene LISTADOS: $contieneListados")
                }
                
                // Si no se encuentran libros con "LISTADOS", usar todos los libros
                val librosParaMostrar = if (librosDbInspecciones.isEmpty()) {
                    Log.w("FragmentInspeccionConfiguracion", "No se encontraron libros con 'LISTADOS', mostrando todos los libros")
                    spreadsheets
                } else {
                    librosDbInspecciones
                }
                
                Log.d("FragmentInspeccionConfiguracion", "Libros para mostrar: ${librosParaMostrar.size}")
                
                if (librosParaMostrar.isEmpty()) {
                    Log.e("FragmentInspeccionConfiguracion", "No se encontraron libros para mostrar")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No se encontraron libros disponibles", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    Log.d("FragmentInspeccionConfiguracion", "Configurando adapter con ${librosParaMostrar.size} libros")
                    val libroAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, librosParaMostrar.map { it.name })
                    libroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerDb.adapter = libroAdapter
                    
                    // Cargar valor guardado
                    val libroGuardado = prefs.getString(DB_LIBRO_KEY, null)
                    val libroIndex = if (libroGuardado != null) librosParaMostrar.indexOfFirst { it.name == libroGuardado } else -1
                    if (libroIndex >= 0) spinnerDb.setSelection(libroIndex)
                    
                    spinnerDb.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val libro = librosParaMostrar[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerDbInspecciones: libro seleccionado=${libro.name}")
                            prefs.edit().putString(DB_LIBRO_KEY, libro.name).putString(DB_LIBRO_ID_KEY, libro.id).apply()
                            cargarHojasDb(libro.id)
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                    
                    // Cargar hojas del primer libro si no hay selección previa
                    if (libroIndex >= 0) {
                        cargarHojasDb(librosParaMostrar[libroIndex].id)
                    } else if (librosParaMostrar.isNotEmpty()) {
                        // Auto-seleccionar el primer libro si no hay selección previa
                        val primerLibro = librosParaMostrar[0]
                        Log.d("FragmentInspeccionConfiguracion", "Auto-seleccionando primer libro: ${primerLibro.name}")
                        prefs.edit().putString(DB_LIBRO_KEY, primerLibro.name).putString(DB_LIBRO_ID_KEY, primerLibro.id).apply()
                        spinnerDb.setSelection(0)
                        cargarHojasDb(primerLibro.id)
                    }
                }
                
                Log.d("FragmentInspeccionConfiguracion", "=== CONFIGURACIÓN SPINNER DB COMPLETADA ===")
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error configurando spinner DB inspecciones: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error configurando DB inspecciones: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun configurarSpinnerNumeroInspeccion() {
        Log.d("FragmentInspeccionConfiguracion", "configurarSpinnerNumeroInspeccion")
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
                
                // Obtener libro y hoja seleccionados
                val libroId = prefs.getString(DB_LIBRO_ID_KEY, null)
                val hoja = prefs.getString(DB_INSPECCIONES_HOJA_KEY, "FLOTA")
                
                if (libroId == null) {
                    Log.e("FragmentInspeccionConfiguracion", "No hay libro DB inspecciones seleccionado")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Selecciona primero un libro de DB inspecciones", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                // Leer la fila 2 para obtener los números de inspección disponibles
                val numerosInspeccion = detectarNumerosInspeccion(sheetsManager, libroId, hoja ?: "FLOTA")
                Log.d("FragmentInspeccionConfiguracion", "Números de inspección detectados: $numerosInspeccion")
                
                withContext(Dispatchers.Main) {
                    val numeroAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, numerosInspeccion)
                    numeroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerNumeroInspeccion.adapter = numeroAdapter
                    
                    // Cargar valor guardado
                    val numeroGuardado = prefs.getString(NUMERO_INSPECCION_KEY, null)
                    val numeroIndex = if (numeroGuardado != null) numerosInspeccion.indexOf(numeroGuardado) else -1
                    if (numeroIndex >= 0) spinnerNumeroInspeccion.setSelection(numeroIndex)
                    
                    spinnerNumeroInspeccion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val numero = numerosInspeccion[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerNumeroInspeccion: número seleccionado=$numero")
                            prefs.edit().putString(NUMERO_INSPECCION_KEY, numero).apply()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error configurando spinner número inspección: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error configurando número de inspección: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private suspend fun detectarNumerosInspeccion(sheetsManager: GoogleSheetsManager, libroId: String, hoja: String): List<String> {
        return try {
            // Leer la fila 2 (índice 1) para obtener los números de inspección
            val range = "$hoja!2:2"
            val values = sheetsManager.leerRango(libroId, range)
            
            if (values.isEmpty() || values[0].isEmpty()) {
                Log.w("FragmentInspeccionConfiguracion", "No se encontraron datos en la fila 2")
                return emptyList()
            }
            
            val fila2 = values[0]
            val numerosInspeccion = mutableListOf<String>()
            
            // Buscar patrones como "ESTADO 25", "ESTADO 26", etc. en la fila 2
            for (i in fila2.indices) {
                val valor = fila2[i]?.toString()?.trim()
                if (valor != null && valor.startsWith("ESTADO ")) {
                    val numero = valor.substringAfter("ESTADO ").trim()
                    if (numero.matches(Regex("\\d+"))) {
                        val numeroInt = numero.toIntOrNull()
                        if (numeroInt != null && numeroInt >= 25 && numeroInt <= 50) { // Rango típico de inspecciones
                            numerosInspeccion.add(numero)
                        }
                    }
                }
            }
            
            // Ordenar numéricamente
            numerosInspeccion.sortBy { it.toIntOrNull() ?: 0 }
            numerosInspeccion
        } catch (e: Exception) {
            Log.e("FragmentInspeccionConfiguracion", "Error detectando números de inspección: ${e.message}", e)
            emptyList()
        }
    }

    private fun cargarHojasDb(libroId: String) {
        Log.d("FragmentInspeccionConfiguracion", "cargarHojasDb: libroId=$libroId")
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
                val hojasInspecciones = hojasList.filter { it.contains("FLOTA") }
                val hojasReparaciones = hojasList.filter { it.contains("REPARACION") }
                
                Log.d("FragmentInspeccionConfiguracion", "Hojas inspecciones encontradas: ${hojasInspecciones.size}")
                Log.d("FragmentInspeccionConfiguracion", "Hojas reparaciones encontradas: ${hojasReparaciones.size}")
                
                withContext(Dispatchers.Main) {
                    // Configurar spinner de inspecciones
                    val inspeccionesAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, hojasInspecciones)
                    inspeccionesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerInspecciones.adapter = inspeccionesAdapter
                    
                    // Cargar valor guardado para inspecciones
                    val hojaInspeccionesGuardada = prefs.getString(DB_INSPECCIONES_HOJA_KEY, null)
                    val inspeccionesIndex = if (hojaInspeccionesGuardada != null && hojasInspecciones.contains(hojaInspeccionesGuardada)) 
                        hojasInspecciones.indexOf(hojaInspeccionesGuardada) 
                    else 
                        hojasInspecciones.indexOf("FLOTA")
                    
                    if (inspeccionesIndex >= 0) {
                        spinnerInspecciones.setSelection(inspeccionesIndex)
                        prefs.edit().putString(DB_INSPECCIONES_HOJA_KEY, hojasInspecciones[inspeccionesIndex]).apply()
                    }
                    
                    // Configurar listener para inspecciones
                    spinnerInspecciones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val hoja = hojasInspecciones[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerInspecciones: hoja seleccionada=$hoja")
                            prefs.edit().putString(DB_INSPECCIONES_HOJA_KEY, hoja).apply()
                            configurarSpinnerNumeroInspeccion()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                    
                    // Configurar spinner de reparaciones
                    val reparacionesAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, hojasReparaciones)
                    reparacionesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerReparaciones.adapter = reparacionesAdapter
                    
                    // Cargar valor guardado para reparaciones
                    val hojaReparacionesGuardada = prefs.getString(DB_REPARACIONES_HOJA_KEY, null)
                    val reparacionesIndex = if (hojaReparacionesGuardada != null && hojasReparaciones.contains(hojaReparacionesGuardada)) 
                        hojasReparaciones.indexOf(hojaReparacionesGuardada) 
                    else 
                        hojasReparaciones.indexOfFirst { it.contains("REPARACION") }
                    
                    if (reparacionesIndex >= 0) {
                        spinnerReparaciones.setSelection(reparacionesIndex)
                        prefs.edit().putString(DB_REPARACIONES_HOJA_KEY, hojasReparaciones[reparacionesIndex]).apply()
                    }
                    
                    // Configurar listener para reparaciones
                    spinnerReparaciones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val hoja = hojasReparaciones[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerReparaciones: hoja seleccionada=$hoja")
                            prefs.edit().putString(DB_REPARACIONES_HOJA_KEY, hoja).apply()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
                    
                    // Configurar el spinner de número de inspección
                    configurarSpinnerNumeroInspeccion()
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error cargando hojas DB: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error cargando hojas DB: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cargarHojasGoogle(libroId: String, hojaGuardada: String?, locked: Boolean) {
        Log.d("FragmentInspeccionConfiguracion", "cargarHojasGoogle: libroId=$libroId, hojaGuardada=$hojaGuardada, locked=$locked")
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
                hojas = hojasList
                Log.d("FragmentInspeccionConfiguracion", "Hojas encontradas: ${hojas.size}")
                withContext(Dispatchers.Main) {
                    val hojaAdapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, hojas)
                    hojaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerHoja.adapter = hojaAdapter
                    val hojaIndex = if (hojaGuardada != null && hojas.contains(hojaGuardada)) hojas.indexOf(hojaGuardada) else hojas.indexOf("INSPECCIÓN")
                    if (hojaIndex >= 0) spinnerHoja.setSelection(hojaIndex)
                    spinnerHoja.isEnabled = !locked
                    spinnerHoja.onItemSelectedListener = if (!locked) object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                            val hoja = hojas[position]
                            Log.d("FragmentInspeccionConfiguracion", "SELECT en spinnerHoja: hoja seleccionada=$hoja")
                            prefs.edit().putString(HOJA_KEY, hoja).apply()
                        }
                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    } else null
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
            
            // Obtener el libro DB y hoja INSPECCIONES seleccionados
            val dbLibroId = prefs.getString(DB_LIBRO_ID_KEY, null)
            val dbHoja = prefs.getString(DB_INSPECCIONES_HOJA_KEY, null)
            
            if (dbLibroId.isNullOrEmpty() || dbHoja.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Configura primero el DB y la hoja de Inspecciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            Toast.makeText(requireContext(), "ACTUALIZANDO DESDE $dbHoja", Toast.LENGTH_SHORT).show()
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
                    procesarImportacionDatos(headerRowString, dataRows, null)
                    
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
        imgBorrar.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en imgBorrar")
            Toast.makeText(requireContext(), "Base de datos restablecida", Toast.LENGTH_SHORT).show()
            prefs.edit().clear().apply()
            spinnerLibro.isEnabled = true
            spinnerHoja.isEnabled = true
            txtFecha.isEnabled = true
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(requireContext())
                db.equipoDao().borrarTodo()
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    setupFecha(null, false)
                    setupDropdownsGoogle(null, null, false)
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
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnActualizarFotos")
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
            
            // Obtener el libro DB y hoja INSPECCIONES seleccionados
            val dbLibroId = prefs.getString(DB_LIBRO_ID_KEY, null)
            val dbHoja = prefs.getString(DB_INSPECCIONES_HOJA_KEY, null)
            
            if (dbLibroId.isNullOrEmpty() || dbHoja.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Configura primero el DB y la hoja de Inspecciones", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
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
                    
                    // Obtener el ID del libro DB
                    val libroId = dbLibroId
                    if (libroId == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No se pudo obtener el ID del libro", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        return@launch
                    }
                    
                    // Obtener configuración de DB inspecciones para usar hoja FLOTA
                    val dbLibroId = prefs.getString(DB_LIBRO_ID_KEY, null)
                    val dbHoja = prefs.getString(DB_INSPECCIONES_HOJA_KEY, "FLOTA")
                    
                    if (dbLibroId == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No hay libro DB inspecciones configurado", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        return@launch
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Descargando desde libro: $dbLibroId, hoja: $dbHoja")
                    
                    Log.d("FragmentInspeccionConfiguracion", "Leyendo cabeceras desde hoja FLOTA: $dbLibroId, hoja: $dbHoja")
                    
                    // Leer cabeceras de la hoja FLOTA (fila 2)
                    val headerResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(dbLibroId, "$dbHoja!A2:ZZ2")
                        .execute()
                    val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Cabeceras encontradas en FLOTA: ${headerRow.joinToString(", ")}")
                    
                    // Detectar automáticamente la última columna basada en el mapeo JSON y las columnas disponibles
                    val mapeoJson = requireContext().assets.open("mapeo_columnas.json").bufferedReader().use(BufferedReader::readText)
                    val mapeoColumnas = JSONObject(mapeoJson)
                    
                    // Encontrar la última columna que contiene datos del mapeo
                    var ultimaColumnaIdx = 0
                    for (i in headerRow.indices) {
                        val columna = headerRow[i].toString()
                        // Verificar si esta columna está en el mapeo JSON
                        val estaEnMapeo = mapeoColumnas.keys().asSequence().any { campo ->
                            mapeoColumnas.getString(campo) == columna
                        }
                        if (columna.isNotBlank() || estaEnMapeo) {
                            ultimaColumnaIdx = i
                        }
                    }
                    
                    // Convertir índice a letra de columna
                    val ultimaColumna = if (ultimaColumnaIdx < 26) {
                        ('A'.code + ultimaColumnaIdx).toChar().toString()
                    } else {
                        val primeraLetra = ('A'.code + (ultimaColumnaIdx / 26 - 1)).toChar()
                        val segundaLetra = ('A'.code + (ultimaColumnaIdx % 26)).toChar()
                        "$primeraLetra$segundaLetra"
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Última columna detectada: $ultimaColumna (índice: $ultimaColumnaIdx)")
                    
                    // Leer datos desde la hoja FLOTA (desde fila 3)
                    val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(dbLibroId, "$dbHoja!A3:$ultimaColumna")
                        .execute()
                    val dataRows = dataResponse.getValues() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Datos obtenidos desde FLOTA: ${dataRows.size} filas con ${headerRow.size} columnas")
                    
                    // Procesar los datos usando la función existente
                    importarDatosDesdeSheet(headerRow.map { it.toString() }, dataRows)
                    
                    // ACTUALIZAR COLORES DE ESTADOS DESDE SPREADSHEET
                    Log.d("FragmentInspeccionConfiguracion", "Actualizando colores de estados...")
                    val coloresActualizados = sheetsManager.actualizarColoresEstados(
                        spreadsheetId = dbLibroId,
                        sheetName = dbHoja ?: "FLOTA" // Usar la hoja FLOTA donde están los datos
                    )
                    
                    if (coloresActualizados) {
                        Log.d("FragmentInspeccionConfiguracion", "Colores de estados actualizados exitosamente")
                    } else {
                        Log.w("FragmentInspeccionConfiguracion", "No se pudieron actualizar los colores de estados")
                    }
                    
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        val mensaje = if (coloresActualizados) {
                            "Descarga completada desde $dbHoja (colores actualizados)"
                        } else {
                            "Descarga completada desde $dbHoja"
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
                
                Log.d("FragmentInspeccionConfiguracion", "Resumen calculado:")
                Log.d("FragmentInspeccionConfiguracion", "  - Total equipos: $totalEquipos")
                Log.d("FragmentInspeccionConfiguracion", "  - Inspeccionados: $inspeccionados")
                Log.d("FragmentInspeccionConfiguracion", "  - Modificados: ${modificados.size}")
                
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
                txtFotosDrive.text = "Fotos en Drive: $fotosDrive"
                txtFotosNuevas.text = "Fotos nuevas: $fotosNuevas"
                txtFotosPendientes.text = "Fotos pendientes de subir: $fotosPendientes"
                }
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error al cargar resumen: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al cargar resumen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun normalizaNombre(nombre: String): String {
        return nombre.lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace(".", "")
            .replace("(", "")
            .replace(")", "")
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
    }

    private fun importarDatosDesdeSheet(header: List<String>, rows: List<List<Any>>) {
        Log.d("FragmentInspeccionConfiguracion", "importarDatosDesdeSheet llamado")
        val context = requireContext()
        
        // Primero, detectar campos de orden disponibles
        val camposOrdenDisponibles = header.filter { it.startsWith("orden_") }
        
        if (camposOrdenDisponibles.isNotEmpty()) {
            // Mostrar diálogo de selección de orden
            lifecycleScope.launch(Dispatchers.Main) {
                mostrarDialogoSeleccionOrden(camposOrdenDisponibles) { campoOrdenSeleccionado ->
                    if (campoOrdenSeleccionado != null) {
                        // Guardar la selección del usuario
                        prefs.edit().putString("campo_orden_seleccionado", campoOrdenSeleccionado).apply()
                        // Continuar con la importación usando el campo de orden seleccionado
                        procesarImportacionDatos(header, rows, campoOrdenSeleccionado)
                    } else {
                        // Usar orden_default como fallback
                        procesarImportacionDatos(header, rows, "orden_default")
                    }
                }
            }
        } else {
            // No hay campos de orden, continuar sin orden
            procesarImportacionDatos(header, rows, null)
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

    private fun procesarImportacionDatos(header: List<String>, rows: List<List<Any>>, campoOrdenSeleccionado: String?) {
        Log.d("FragmentInspeccionConfiguracion", "procesarImportacionDatos llamado")
        val context = requireContext()
        
        // Cargar cache de índices
        val cacheIndices = cargarCacheIndices()
        
        // Leer mapeo desde assets
        val mapeoJson = context.assets.open("mapeo_columnas.json").bufferedReader().use(BufferedReader::readText)
        val mapeoColumnas = JSONObject(mapeoJson)
        val mapeoPrefs = prefs.getStringSet("mapeo_inspeccion", null)?.associate {
            val (campo, columna) = it.split(":")
            campo to columna
        }?.toMutableMap() ?: mutableMapOf()

        // Mapeo inteligente: usar primero el JSON, luego preferencias, luego coincidencias normalizadas
        val headerNormalizado = header.map { normalizaNombre(it) }
        val mapeoAuto = mutableMapOf<String, String>()
        
        Log.d("FragmentInspeccionConfiguracion", "=== MAPEO DE CAMPOS ===")
        Log.d("FragmentInspeccionConfiguracion", "Headers disponibles: ${header.joinToString(", ")}")
        Log.d("FragmentInspeccionConfiguracion", "Headers normalizados: ${headerNormalizado.joinToString(", ")}")
        
        for (campo in CAMPOS_OBLIGATORIOS) {
            if (campo == "gps") {
                mapeoAuto[campo] = "GPS_COORD"
                Log.d("FragmentInspeccionConfiguracion", "Campo $campo -> GPS_COORD (hardcoded)")
                continue
            }
            val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
            val columnaManual = mapeoPrefs[campo]
            
            Log.d("FragmentInspeccionConfiguracion", "Campo $campo: JSON=$columnaJson, Manual=$columnaManual")
            
            val columnaFinal = columnaManual ?: columnaJson
            if (columnaFinal != null) {
                // Usar cache para buscar la columna
                val indice = buscarIndiceColumna(header, columnaFinal, cacheIndices)
                if (indice != -1) {
                    mapeoAuto[campo] = columnaFinal
                    Log.d("FragmentInspeccionConfiguracion", "Campo $campo mapeado a columna '$columnaFinal' (índice $indice)")
            } else {
                    Log.w("FragmentInspeccionConfiguracion", "Campo $campo NO MAPEADO - columna '$columnaFinal' no encontrada")
                }
            } else {
                // Buscar por coincidencia normalizada
                val indice = headerNormalizado.indexOfFirst { it.equals(normalizaNombre(campo), ignoreCase = true) }
                if (indice != -1) {
                    val columnaEncontrada = header[indice]
                    mapeoAuto[campo] = columnaEncontrada
                    // Guardar en cache
                    cacheIndices["columna_$columnaEncontrada"] = indice
                    Log.d("FragmentInspeccionConfiguracion", "Campo $campo mapeado automáticamente a columna '$columnaEncontrada' (índice $indice)")
                } else {
                    Log.w("FragmentInspeccionConfiguracion", "Campo $campo NO MAPEADO")
                }
            }
        }
        
        // Guardar cache actualizado
        guardarCacheIndices(cacheIndices)
        
        Log.d("FragmentInspeccionConfiguracion", "Mapeo final: $mapeoAuto")
        // Solo pedir mapeo para los campos que no se pudieron mapear automáticamente y que no sean 'gps'
        val camposFaltantes = CAMPOS_OBLIGATORIOS.filter { campo ->
            campo != "gps" && mapeoAuto[campo] == null
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
                    procesarImportacionDatos(header, rows, campoOrdenSeleccionado)
                }
            }
            return
        }
        // Procesar filas y guardar en la base de datos - filtrar solo filas con datos de equipos
        val equipos = rows.mapNotNull { row ->
            try {
                // Verificar si esta fila contiene datos de equipo (debe tener TAG y al menos algunos campos básicos)
                val tagValue = if (row.size > 1) row[1].toString().trim() else ""
                val areaValue = if (row.size > 4) row[4].toString().trim() else ""
                val unidadValue = if (row.size > 5) row[5].toString().trim() else ""
                
                // Solo procesar filas que tengan TAG y al menos AREA o UNIDAD
                if (tagValue.isEmpty() || (areaValue.isEmpty() && unidadValue.isEmpty())) {
                    Log.d("FragmentInspeccionConfiguracion", "Saltando fila sin datos de equipo: TAG='$tagValue', AREA='$areaValue', UNIDAD='$unidadValue'")
                    return@mapNotNull null
                }
                
                Log.d("FragmentInspeccionConfiguracion", "Procesando fila de equipo: TAG='$tagValue', AREA='$areaValue', UNIDAD='$unidadValue'")
                val get = { campo: String ->
                    val columnaManual = mapeoPrefs[campo]
                    val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
                    val columna = columnaJson ?: columnaManual ?: mapeoAuto[campo] ?: campo
                    val idx = header.indexOf(columna)
                    if (idx != -1 && row.size > idx) row[idx].toString() else null
                }
                // Obtener el valor del campo de orden seleccionado
                val valorOrden = if (campoOrdenSeleccionado != null) {
                    val idx = header.indexOf(campoOrdenSeleccionado)
                    if (idx != -1 && row.size > idx) row[idx].toString().toDoubleOrNull() else 0.0
                } else {
                    0.0
                }
                val fechaInspeccion = get("fechaInspeccion")
                val identidadInspector = get("identidadInspector")
                val detectorUtilizado = get("detectorUtilizado")
                val marca = get("marca")
                val modelo = get("modelo")
                val tipo = get("tipo")
                val area = get("area")
                val unidad = get("unidad")
                
                // Logging detallado para diagnosticar el mapeo
                Log.d("FragmentInspeccionConfiguracion", "=== DIAGNÓSTICO MAPEO ===")
                Log.d("FragmentInspeccionConfiguracion", "Campo 'marca': JSON=${mapeoColumnas.optString("marca", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["marca"]}, Auto=${mapeoAuto["marca"]}")
                Log.d("FragmentInspeccionConfiguracion", "Campo 'modelo': JSON=${mapeoColumnas.optString("modelo", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["modelo"]}, Auto=${mapeoAuto["modelo"]}")
                Log.d("FragmentInspeccionConfiguracion", "Campo 'tipo': JSON=${mapeoColumnas.optString("tipo", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["tipo"]}, Auto=${mapeoAuto["tipo"]}")
                Log.d("FragmentInspeccionConfiguracion", "Campo 'area': JSON=${mapeoColumnas.optString("area", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["area"]}, Auto=${mapeoAuto["area"]}")
                Log.d("FragmentInspeccionConfiguracion", "Campo 'unidad': JSON=${mapeoColumnas.optString("unidad", "NO_ENCONTRADO")}, Manual=${mapeoPrefs["unidad"]}, Auto=${mapeoAuto["unidad"]}")
                
                // Logging de la fila completa para ver qué datos se están leyendo
                Log.d("FragmentInspeccionConfiguracion", "=== DATOS DE LA FILA ===")
                Log.d("FragmentInspeccionConfiguracion", "Fila completa: ${row.joinToString(" | ")}")
                Log.d("FragmentInspeccionConfiguracion", "Tamaño de la fila: ${row.size}")
                
                // Logging de índices específicos
                val marcaIndex = header.indexOf("MARCA")
                val modeloIndex = header.indexOf("MODELO")
                val tipoIndex = header.indexOf("TIPO")
                val areaIndex = header.indexOf("AREA")
                val unidadIndex = header.indexOf("UNIDAD")
                
                Log.d("FragmentInspeccionConfiguracion", "Índices: MARCA=$marcaIndex, MODELO=$modeloIndex, TIPO=$tipoIndex, AREA=$areaIndex, UNIDAD=$unidadIndex")
                
                if (marcaIndex != -1 && row.size > marcaIndex) {
                    Log.d("FragmentInspeccionConfiguracion", "Valor MARCA en índice $marcaIndex: '${row[marcaIndex]}'")
                }
                if (modeloIndex != -1 && row.size > modeloIndex) {
                    Log.d("FragmentInspeccionConfiguracion", "Valor MODELO en índice $modeloIndex: '${row[modeloIndex]}'")
                }
                if (tipoIndex != -1 && row.size > tipoIndex) {
                    Log.d("FragmentInspeccionConfiguracion", "Valor TIPO en índice $tipoIndex: '${row[tipoIndex]}'")
                }
                if (areaIndex != -1 && row.size > areaIndex) {
                    Log.d("FragmentInspeccionConfiguracion", "Valor AREA en índice $areaIndex: '${row[areaIndex]}'")
                }
                if (unidadIndex != -1 && row.size > unidadIndex) {
                    Log.d("FragmentInspeccionConfiguracion", "Valor UNIDAD en índice $unidadIndex: '${row[unidadIndex]}'")
                }
                
                Log.d("FragmentInspeccionConfiguracion", "IMPORT_EQUIPO: id=${get("id")}, fechaInspeccion=$fechaInspeccion, identidadInspector=$identidadInspector, detectorUtilizado=$detectorUtilizado")
                Log.d("FragmentInspeccionConfiguracion", "IMPORT_EQUIPO: marca=$marca, modelo=$modelo, tipo=$tipo, area=$area, unidad=$unidad")
                
                val equipo = com.bithermmanagement.database.entities.Equipo(
                    id = get("id") ?: "",
                    instalacion = get("instalacion"),
                    unidad = get("unidad"),
                    area = get("area"),
                    linea = get("linea"),
                    marca = get("marca"),
                    modelo = get("modelo"),
                    tipo = get("tipo"),
                    periodicidad = get("p"),
                    diametro = get("diametro"),
                    conexion = get("conexion"),
                    aislamiento = get("aislamiento"),
                    presEntrada = get("presEntrada"),
                    presSalida = get("presSalida"),
                    byPass = get("byPass")?.equals("true", ignoreCase = true),
                    descarga = get("descarga"),
                    aplicacion = get("aplicacion"),
                    servicio = get("servicio"),
                    ubicacion = get("ubicacion"),
                    estado = get("estado"),
                    fechaInspeccion = fechaInspeccion,
                    nota = get("nota"),
                    identidadInspector = identidadInspector,
                    detectorUtilizado = detectorUtilizado,
                    incidencias = get("incidencias"),
                    gpsCoord = get("gps"),
                    urlFotoEquipo = get("foto"),
                    urlFotoUbicacion = get("fotoUbic"),
                    orden = valorOrden,
                    gpsAcc = get("gpsAcc"),
                    extra = null,
                    modificadoLocal = false,
                    instalacionMf = get("instalacionMf"),
                    urlFotoManifold = null,
                    urlFotosExtra = null
                )
                
                equipo
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error importando fila: ${e.message}", e)
                null
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                db.equipoDao().upsertAll(equipos)
                withContext(Dispatchers.Main) {
                    val mensaje = if (campoOrdenSeleccionado != null) {
                        "Importación completada: ${equipos.size} registros (Orden: $campoOrdenSeleccionado)"
                    } else {
                        "Importación completada: ${equipos.size} registros"
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
    
    private fun configurarSpinnerReparaciones() {
        Log.d("FragmentInspeccionConfiguracion", "Configurando spinner de reparaciones")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val settingsManager = SettingsManager(context)
                val settings = settingsManager.getSettings()
                
                val driveManager = GoogleDriveManager(
                    credentialsStream = settingsManager.getCredentialsInputStream(),
                    context = context,
                    useOAuth = settings.useOAuth,
                    oAuthEmail = settings.oAuthEmail
                )
                
                // Obtener libros que contengan "LISTADOS"
                val libros = driveManager.listarSpreadsheetsApp()
                val librosListados = libros.filter { libro: SpreadsheetInfo -> libro.name.contains("LISTADOS", ignoreCase = true) }
                
                Log.d("FragmentInspeccionConfiguracion", "Libros LISTADOS encontrados: ${librosListados.size}")
                
                withContext(Dispatchers.Main) {
                    val adapter = ArrayAdapter(
                        context,
                        android.R.layout.simple_spinner_item,
                        librosListados.map { libro: SpreadsheetInfo -> libro.name }.toTypedArray()
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerReparaciones.adapter = adapter
                    
                    // Auto-seleccionar si no hay configuración guardada
                    val libroGuardado = prefs.getString("reparaciones_libro", null)
                    if (libroGuardado != null) {
                        val index = librosListados.indexOfFirst { libro: SpreadsheetInfo -> libro.name == libroGuardado }
                        if (index >= 0) {
                            spinnerReparaciones.setSelection(index)
                        }
                    } else if (librosListados.isNotEmpty()) {
                        // Auto-seleccionar el primero
                        spinnerReparaciones.setSelection(0)
                        val libroSeleccionado = librosListados[0]
                        prefs.edit()
                            .putString("reparaciones_libro", libroSeleccionado.name)
                            .putString("reparaciones_libro_id", libroSeleccionado.id)
                            .apply()
                        Log.d("FragmentInspeccionConfiguracion", "Auto-seleccionado libro: ${libroSeleccionado.name}")
                    }
                    
                    // Configurar listener
                    spinnerReparaciones.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            val libroSeleccionado = librosListados[position]
                            prefs.edit()
                                .putString("reparaciones_libro", libroSeleccionado.name)
                                .putString("reparaciones_libro_id", libroSeleccionado.id)
                                .apply()
                            Log.d("FragmentInspeccionConfiguracion", "Libro reparaciones seleccionado: ${libroSeleccionado.name}")
                            cargarHojasReparaciones(libroSeleccionado.id)
                        }
                        
                        override fun onNothingSelected(parent: AdapterView<*>?) {}
                    }
                }
                
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error configurando spinner reparaciones: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error configurando reparaciones: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun cargarHojasReparaciones(libroId: String) {
        Log.d("FragmentInspeccionConfiguracion", "Cargando hojas de reparaciones para libro: $libroId")
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val settingsManager = SettingsManager(context)
                val settings = settingsManager.getSettings()
                
                val authAdapter = GoogleAuthAdapter(
                    context = context,
                    useOAuth = settings.useOAuth,
                    oAuthEmail = settings.oAuthEmail,
                    credentialsStream = settingsManager.getCredentialsInputStream()
                )
                val sheetsManager = GoogleSheetsManager(
                    authAdapter = authAdapter,
                    context = context
                )
                
                val hojas = sheetsManager.listarHojas(libroId)
                val hojasReparaciones = hojas.filter { it.contains("REPARACION", ignoreCase = true) }
                
                Log.d("FragmentInspeccionConfiguracion", "Hojas de reparaciones encontradas: ${hojasReparaciones.size}")
                
                withContext(Dispatchers.Main) {
                    // Si hay hojas de reparaciones, seleccionar la primera
                    if (hojasReparaciones.isNotEmpty()) {
                        val hojaSeleccionada = hojasReparaciones[0]
                        prefs.edit()
                            .putString("reparaciones_hoja", hojaSeleccionada)
                            .apply()
                        Log.d("FragmentInspeccionConfiguracion", "Hoja reparaciones seleccionada: $hojaSeleccionada")
                    } else {
                        Log.w("FragmentInspeccionConfiguracion", "No se encontraron hojas de reparaciones")
                    }
                }
                
            } catch (e: Exception) {
                Log.e("FragmentInspeccionConfiguracion", "Error cargando hojas reparaciones: ${e.message}", e)
            }
        }
    }
    
    /**
     * Carga el cache de índices de columnas desde SharedPreferences
     */
    private fun cargarCacheIndices(): MutableMap<String, Int> {
        val cacheJson = prefs.getString("cache_indices_columnas", "{}")
        return try {
            val jsonObject = JSONObject(cacheJson ?: "{}")
            val cache = mutableMapOf<String, Int>()
            jsonObject.keys().forEach { key ->
                cache[key] = jsonObject.getInt(key)
            }
            Log.d("FragmentInspeccionConfiguracion", "Cache de índices cargado: $cache")
            cache
        } catch (e: Exception) {
            Log.w("FragmentInspeccionConfiguracion", "Error cargando cache de índices: ${e.message}")
            mutableMapOf()
        }
    }
    
    /**
     * Guarda el cache de índices de columnas en SharedPreferences
     */
    private fun guardarCacheIndices(cache: Map<String, Int>) {
        try {
            val jsonObject = JSONObject()
            cache.forEach { (key, value) ->
                jsonObject.put(key, value)
            }
            prefs.edit()
                .putString("cache_indices_columnas", jsonObject.toString())
                .apply()
            Log.d("FragmentInspeccionConfiguracion", "Cache de índices guardado: $cache")
        } catch (e: Exception) {
            Log.e("FragmentInspeccionConfiguracion", "Error guardando cache de índices: ${e.message}")
        }
    }
    
    /**
     * Busca el índice de una columna usando cache primero, luego búsqueda directa
     */
    private fun buscarIndiceColumna(header: List<String>, nombreColumna: String, cache: MutableMap<String, Int>): Int {
        // Verificar cache primero
        val cacheKey = "columna_$nombreColumna"
        if (cache.containsKey(cacheKey)) {
            val indiceCache = cache[cacheKey]!!
            if (indiceCache < header.size && header[indiceCache].equals(nombreColumna, ignoreCase = true)) {
                Log.d("FragmentInspeccionConfiguracion", "Índice encontrado en cache para '$nombreColumna': $indiceCache")
                return indiceCache
            } else {
                // Cache inválido, remover
                cache.remove(cacheKey)
                Log.d("FragmentInspeccionConfiguracion", "Cache inválido para '$nombreColumna', removido")
            }
        }
        
        // Búsqueda directa
        val indice = header.indexOfFirst { it.equals(nombreColumna, ignoreCase = true) }
        if (indice != -1) {
            // Guardar en cache
            cache[cacheKey] = indice
            Log.d("FragmentInspeccionConfiguracion", "Índice encontrado para '$nombreColumna': $indice (guardado en cache)")
        } else {
            Log.w("FragmentInspeccionConfiguracion", "Columna '$nombreColumna' no encontrada")
        }
        
        return indice
    }
} 