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
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.Equipo
import com.bithermmanagement.ui.dialogs.SeleccionOrdenDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var fechaSeleccionada: Calendar = Calendar.getInstance()

    private var libros: List<GoogleDriveManager.SpreadsheetInfo> = emptyList()
    private var hojas: List<String> = emptyList()

    private val CAMPOS_OBLIGATORIOS = listOf(
        "id", "instalacion", "unidad", "area", "linea", "marca", "modelo", "tipo", "p", "diametro", 
        "conexion", "aislamiento", "presEntrada", "presSalida", "byPass", "descarga", "aplicacion", 
        "servicio", "ubicacion", "estado", "fechaInspeccion", "fugaKgH", "nota", "identidadInspector", 
        "detectorUtilizado", "incidencias", "gps", "foto", "fotoUbic", "gpsAcc", "instalacionMf"
    )

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
                val credentialsStream = context.assets.open("credentials.json")
                val driveManager = GoogleDriveManager(credentialsStream, context)
                val sheetsManager = GoogleSheetsManager(context.assets.open("credentials.json"), context)
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

    private fun cargarHojasGoogle(libroId: String, hojaGuardada: String?, locked: Boolean) {
        Log.d("FragmentInspeccionConfiguracion", "cargarHojasGoogle: libroId=$libroId, hojaGuardada=$hojaGuardada, locked=$locked")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val context = requireContext()
                val sheetsManager = GoogleSheetsManager(context.assets.open("credentials.json"), context)
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
            Toast.makeText(requireContext(), "Función de actualizar pendiente de implementar", Toast.LENGTH_SHORT).show()
        }

        btnDescargar.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnDescargar")
            
            // Obtener el libro y hoja seleccionados del spinner
            val libroSeleccionado = spinnerLibro.selectedItem?.toString()
            val hojaSeleccionada = spinnerHoja.selectedItem?.toString()
            
            if (libroSeleccionado.isNullOrEmpty() || hojaSeleccionada.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Selecciona un libro y una hoja primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            Toast.makeText(requireContext(), "Descargando desde: $libroSeleccionado - $hojaSeleccionada", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val sheetsManager = GoogleSheetsManager(context.assets.open("credentials.json"), context)
                    
                    // Obtener el campo de orden seleccionado previamente
                    val campoOrdenSeleccionado = prefs.getString("campo_orden_seleccionado", null)
                    
                    // Obtener el ID del libro seleccionado
                    val libroId = obtenerLibroId(libroSeleccionado)
                    if (libroId == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No se pudo obtener el ID del libro", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        return@launch
                    }
                    
                    // Obtener el ID de la hoja seleccionada
                    val hojaId = obtenerHojaId(libroId, hojaSeleccionada)
                    if (hojaId == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No se pudo obtener el ID de la hoja", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        return@launch
                    }
                    
                    Log.d("FragmentInspeccionConfiguracion", "Descargando desde libro: $libroId, hoja: $hojaSeleccionada")
                    
                    // Leer cabeceras de la hoja usando el nombre
                    val headerResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(libroId, "$hojaSeleccionada!A1:ZZ1")
                        .execute()
                    val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Cabeceras encontradas: ${headerRow.joinToString(", ")}")
                    
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
                    
                    // Leer datos de la hoja usando el rango detectado automáticamente
                    val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(libroId, "$hojaSeleccionada!A2:$ultimaColumna")
                        .execute()
                    val dataRows = dataResponse.getValues() ?: emptyList()
                    
                    Log.d("FragmentInspeccionConfiguracion", "Datos obtenidos: ${dataRows.size} filas con ${headerRow.size} columnas")
                    
                    // Procesar los datos usando la función existente
                    importarDatosDesdeSheet(headerRow.map { it.toString() }, dataRows)
                    
                    // ACTUALIZAR COLORES DE ESTADOS DESDE SPREADSHEET
                    Log.d("FragmentInspeccionConfiguracion", "Actualizando colores de estados...")
                    val coloresActualizados = sheetsManager.actualizarColoresEstados(
                        spreadsheetId = libroId,
                        sheetName = "EQUIPOS" // Hoja EQUIPOS - busca columnas dinámicamente
                    )
                    
                    if (coloresActualizados) {
                        Log.d("FragmentInspeccionConfiguracion", "Colores de estados actualizados exitosamente")
                    } else {
                        Log.w("FragmentInspeccionConfiguracion", "No se pudieron actualizar los colores de estados")
                    }
                    
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        val mensaje = if (coloresActualizados) {
                            "Descarga completada desde $hojaSeleccionada (colores actualizados)"
                        } else {
                            "Descarga completada desde $hojaSeleccionada"
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
        
        // Botón para actualizar solo los colores de estados
        btnActualizar.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnActualizar - Actualizando colores")
            
            val libroSeleccionado = spinnerLibro.selectedItem?.toString()
            if (libroSeleccionado.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Selecciona un libro primero", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            Toast.makeText(requireContext(), "Actualizando colores de estados...", Toast.LENGTH_SHORT).show()
            setLoading(true)
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val context = requireContext()
                    val sheetsManager = GoogleSheetsManager(context.assets.open("credentials.json"), context)
                    
                    // Obtener el ID del libro seleccionado
                    val libroId = obtenerLibroId(libroSeleccionado)
                    if (libroId == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: No se pudo obtener el ID del libro", Toast.LENGTH_LONG).show()
                            setLoading(false)
                        }
                        return@launch
                    }
                    
                    // Actualizar colores de estados
                    val coloresActualizados = sheetsManager.actualizarColoresEstados(
                        spreadsheetId = libroId,
                        sheetName = "EQUIPOS" // Hoja EQUIPOS - busca columnas dinámicamente
                    )
                    
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        if (coloresActualizados) {
                            Toast.makeText(context, "Colores de estados actualizados exitosamente", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Error: No se pudieron actualizar los colores", Toast.LENGTH_LONG).show()
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e("FragmentInspeccionConfiguracion", "Error actualizando colores: ${e.message}", e)
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        Toast.makeText(requireContext(), "Error actualizando colores: ${e.message}", Toast.LENGTH_LONG).show()
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
            Toast.makeText(requireContext(), "Gestión de fotos en construcción", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                // Simulación de carga de fotos
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    cargarResumen()
                }
            }
        }
        btnActualizarFotos.setOnClickListener {
            Log.d("FragmentInspeccionConfiguracion", "CLICK en btnActualizarFotos")
            Toast.makeText(requireContext(), "Gestión de fotos en construcción", Toast.LENGTH_SHORT).show()
            setLoading(true)
            lifecycleScope.launch(Dispatchers.IO) {
                // Simulación de actualización de fotos
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    cargarResumen()
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
                    val sheetsManager = GoogleSheetsManager(context.assets.open("credentials.json"), context)
                    
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
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun cargarResumen() {
        Log.d("FragmentInspeccionConfiguracion", "cargarResumen llamado")
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(requireContext())
            val all = db.equipoDao().getAll()
            val totalEquipos = all.size
            val inspeccionados = all.count { !it.fechaInspeccion.isNullOrEmpty() }
            val modificados = all.count { it.modificadoLocal == true }
            // TODO: Añadir lógica real para fotosDrive, fotosNuevas, fotosPendientes
            val fotosDrive = 0
            val fotosNuevas = 0
            val fotosPendientes = 0
            withContext(Dispatchers.Main) {
                txtTotalEquipos.text = "Total equipos: $totalEquipos"
                txtInspeccionados.text = "Equipos inspeccionados: $inspeccionados"
                txtModificados.text = "Equipos modificados: $modificados"
                txtFotosDrive.text = "Fotos en Drive: $fotosDrive"
                txtFotosNuevas.text = "Fotos nuevas: $fotosNuevas"
                txtFotosPendientes.text = "Fotos pendientes de subir: $fotosPendientes"
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
        for (campo in CAMPOS_OBLIGATORIOS) {
            if (campo == "gps") {
                mapeoAuto[campo] = "GPS_COORD"
                continue
            }
            val columnaJson = if (mapeoColumnas.has(campo)) mapeoColumnas.getString(campo) else null
            val columnaManual = mapeoPrefs[campo]
            if (columnaJson != null && header.contains(columnaJson)) {
                mapeoAuto[campo] = columnaJson
            } else if (columnaManual != null && header.contains(columnaManual)) {
                mapeoAuto[campo] = columnaManual
            } else {
                val idx = headerNormalizado.indexOf(normalizaNombre(campo))
                if (idx != -1) {
                    mapeoAuto[campo] = header[idx]
                }
            }
        }
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
        // Procesar filas y guardar en la base de datos
        val equipos = rows.mapNotNull { row ->
            try {
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
                Log.d("FragmentInspeccionConfiguracion", "IMPORT_EQUIPO: id=${get("id")}, fechaInspeccion=$fechaInspeccion, identidadInspector=$identidadInspector, detectorUtilizado=$detectorUtilizado")
                
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
            val sheetsManager = GoogleSheetsManager(requireContext().assets.open("credentials.json"), requireContext())
            val hojasList = sheetsManager.listarHojasConInfo(libroId)
            val hoja = hojasList.find { it.name.equals(hojaNombre, ignoreCase = true) }
            hoja?.id
        } catch (e: Exception) {
            Log.e("FragmentInspeccionConfiguracion", "Error obteniendo ID de hoja: ${e.message}", e)
            null
        }
    }
} 