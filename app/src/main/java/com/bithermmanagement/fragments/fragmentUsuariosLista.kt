package com.bithermmanagement.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import android.content.Context
import android.widget.*
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bithermmanagement.data.GoogleSheetsManager
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Typeface
import android.util.Log
import android.view.Gravity
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.DatePicker
import androidx.core.content.ContextCompat
import android.app.DatePickerDialog
import android.graphics.Color

class fragmentUsuariosLista : Fragment() {
    private val TAG = "USUARIOS_LISTA"
    private lateinit var layoutResultados: LinearLayout
    private lateinit var btnGenerarReporte: Button
    private lateinit var btnExportar: Button
    private lateinit var spinnerPeriodo: Spinner
    private lateinit var spinnerUsuario: Spinner
    private lateinit var spinnerVista: Spinner
    private lateinit var btnFechaInicio: Button
    private lateinit var btnFechaFin: Button
    private val FICHAJE_LOGS_SHEET = "FICHAJE-LOGS"
    private val TRABAJADORES_SHEET = "TRABAJADORES"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var fechaInicio: Date? = null
    private var fechaFin: Date? = null
    private var usuariosDisponibles = mutableListOf<String>()
    private var usuariosMap = mutableMapOf<String, String>() // nombre visible -> username
    private var vistaActual = "POR_DIA" // POR_DIA o POR_USUARIO

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView: Iniciando fragment de análisis de tiempos")
        return inflater.inflate(R.layout.fragment_usuarios_lista, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Configurando vistas")
        
        layoutResultados = view.findViewById(R.id.layout_resultados_analisis)
        btnGenerarReporte = view.findViewById(R.id.btn_generar_reporte)
        btnExportar = view.findViewById(R.id.btn_exportar_reporte)
        spinnerPeriodo = view.findViewById(R.id.spinner_periodo)
        spinnerUsuario = view.findViewById(R.id.spinner_usuario)
        spinnerVista = view.findViewById(R.id.spinner_vista)
        btnFechaInicio = view.findViewById(R.id.btn_fecha_inicio)
        btnFechaFin = view.findViewById(R.id.btn_fecha_fin)

        Log.d(TAG, "onViewCreated: Vistas encontradas - layoutResultados: ${layoutResultados != null}, btnGenerarReporte: ${btnGenerarReporte != null}")

        configurarSpinners()
        configurarBotones()
        cargarUsuariosDisponibles()
    }

    private fun configurarSpinners() {
        Log.d(TAG, "configurarSpinners: Configurando spinners")
        
        // Spinner de períodos
        val periodos = arrayOf("Hoy", "Esta semana", "Este mes", "Este trimestre", "Este año", "Personalizado")
        val adapterPeriodos = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, periodos)
        adapterPeriodos.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerPeriodo.adapter = adapterPeriodos
        Log.d(TAG, "configurarSpinners: Spinner períodos configurado con ${periodos.size} opciones")

        // Spinner de usuarios
        val adapterUsuarios = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, mutableListOf("Todos los usuarios"))
        adapterUsuarios.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerUsuario.adapter = adapterUsuarios
        Log.d(TAG, "configurarSpinners: Spinner usuarios configurado")

        // Spinner de vista
        val vistas = arrayOf("Por Día", "Por Usuario")
        val adapterVista = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, vistas)
        adapterVista.setDropDownViewResource(R.layout.spinner_item_dropdown)
        spinnerVista.adapter = adapterVista
        Log.d(TAG, "configurarSpinners: Spinner vista configurado con ${vistas.size} opciones")

        val actualizarTabla = {
            Log.d(TAG, "configurarSpinners: Actualizando tabla por cambio en spinner")
            generarReporte(auto = true)
        }

        spinnerPeriodo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "configurarSpinners: Período seleccionado: ${periodos[position]}")
                actualizarFechasSegunPeriodo(position)
                actualizarTabla()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerUsuario.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Log.d(TAG, "configurarSpinners: Usuario seleccionado: ${spinnerUsuario.selectedItem}")
                actualizarTabla()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerVista.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                vistaActual = if (position == 0) "POR_DIA" else "POR_USUARIO"
                Log.d(TAG, "configurarSpinners: Vista seleccionada: $vistaActual")
                actualizarTabla()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarBotones() {
        Log.d(TAG, "configurarBotones: Configurando botones")
        
        btnFechaInicio.setOnClickListener {
            Log.d(TAG, "configurarBotones: Click en fecha inicio")
            mostrarSelectorFecha(true)
        }

        btnFechaFin.setOnClickListener {
            Log.d(TAG, "configurarBotones: Click en fecha fin")
            mostrarSelectorFecha(false)
        }

        btnGenerarReporte.setOnClickListener {
            Log.d(TAG, "configurarBotones: Click en generar reporte")
            generarReporte()
        }

        btnExportar.setOnClickListener {
            Log.d(TAG, "configurarBotones: Click en exportar")
            exportarReporte()
        }
    }

    private fun actualizarFechasSegunPeriodo(periodo: Int) {
        Log.d(TAG, "actualizarFechasSegunPeriodo: Actualizando fechas para período $periodo")
        val cal = Calendar.getInstance()
        val hoy = cal.time

        when (periodo) {
            0 -> { // Hoy
                fechaInicio = hoy
                fechaFin = hoy
                btnFechaInicio.text = "Inicio: ${dateFormat.format(hoy)}"
                btnFechaFin.text = "Fin: ${dateFormat.format(hoy)}"
                Log.d(TAG, "actualizarFechasSegunPeriodo: Período HOY - ${dateFormat.format(hoy)}")
            }
            1 -> { // Esta semana
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                fechaInicio = cal.time
                cal.add(Calendar.DAY_OF_WEEK, 6)
                fechaFin = cal.time
                btnFechaInicio.text = "Inicio: ${dateFormat.format(fechaInicio!!)}"
                btnFechaFin.text = "Fin: ${dateFormat.format(fechaFin!!)}"
                Log.d(TAG, "actualizarFechasSegunPeriodo: Período SEMANA - ${dateFormat.format(fechaInicio!!)} a ${dateFormat.format(fechaFin!!)}")
            }
            2 -> { // Este mes
                cal.set(Calendar.DAY_OF_MONTH, 1)
                fechaInicio = cal.time
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fechaFin = cal.time
                btnFechaInicio.text = "Inicio: ${dateFormat.format(fechaInicio!!)}"
                btnFechaFin.text = "Fin: ${dateFormat.format(fechaFin!!)}"
                Log.d(TAG, "actualizarFechasSegunPeriodo: Período MES - ${dateFormat.format(fechaInicio!!)} a ${dateFormat.format(fechaFin!!)}")
            }
            3 -> { // Este trimestre
                val mes = cal.get(Calendar.MONTH)
                val trimestre = (mes / 3) * 3
                cal.set(Calendar.MONTH, trimestre)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                fechaInicio = cal.time
                cal.add(Calendar.MONTH, 2)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                fechaFin = cal.time
                btnFechaInicio.text = "Inicio: ${dateFormat.format(fechaInicio!!)}"
                btnFechaFin.text = "Fin: ${dateFormat.format(fechaFin!!)}"
                Log.d(TAG, "actualizarFechasSegunPeriodo: Período TRIMESTRE - ${dateFormat.format(fechaInicio!!)} a ${dateFormat.format(fechaFin!!)}")
            }
            4 -> { // Este año
                cal.set(Calendar.DAY_OF_YEAR, 1)
                fechaInicio = cal.time
                cal.set(Calendar.DAY_OF_YEAR, cal.getActualMaximum(Calendar.DAY_OF_YEAR))
                fechaFin = cal.time
                btnFechaInicio.text = "Inicio: ${dateFormat.format(fechaInicio!!)}"
                btnFechaFin.text = "Fin: ${dateFormat.format(fechaFin!!)}"
                Log.d(TAG, "actualizarFechasSegunPeriodo: Período AÑO - ${dateFormat.format(fechaInicio!!)} a ${dateFormat.format(fechaFin!!)}")
            }
            5 -> { // Personalizado
                btnFechaInicio.text = "Seleccionar fecha inicio"
                btnFechaFin.text = "Seleccionar fecha fin"
                Log.d(TAG, "actualizarFechasSegunPeriodo: Período PERSONALIZADO")
            }
        }
    }

    private fun mostrarSelectorFecha(esInicio: Boolean) {
        Log.d(TAG, "mostrarSelectorFecha: Mostrando selector para ${if (esInicio) "inicio" else "fin"}")
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val fechaSeleccionada = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDay)
            }.time

            if (esInicio) {
                fechaInicio = fechaSeleccionada
                btnFechaInicio.text = "Inicio: ${dateFormat.format(fechaSeleccionada)}"
                Log.d(TAG, "mostrarSelectorFecha: Fecha inicio seleccionada: ${dateFormat.format(fechaSeleccionada)}")
            } else {
                fechaFin = fechaSeleccionada
                btnFechaFin.text = "Fin: ${dateFormat.format(fechaSeleccionada)}"
                Log.d(TAG, "mostrarSelectorFecha: Fecha fin seleccionada: ${dateFormat.format(fechaSeleccionada)}")
            }
        }, year, month, day).show()
    }

    private fun cargarUsuariosDisponibles() {
        Log.d(TAG, "cargarUsuariosDisponibles: Iniciando carga de usuarios")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "cargarUsuariosDisponibles: Abriendo credentials.json")
                val credentialsStream = requireContext().assets.open("credentials.json")
                Log.d(TAG, "cargarUsuariosDisponibles: Credentials abiertos correctamente")
                
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                Log.d(TAG, "cargarUsuariosDisponibles: GoogleSheetsManager creado")
                
                Log.d(TAG, "cargarUsuariosDisponibles: Obteniendo usuarios...")
                val (header, trabajadores) = sheetsManager.getAllUsers()
                Log.d(TAG, "cargarUsuariosDisponibles: Usuarios obtenidos - Header: ${header.size} campos, Trabajadores: ${trabajadores.size} filas")
                Log.d(TAG, "cargarUsuariosDisponibles: Header: $header")
                
                val idxApp = header.indexOf("APP")
                Log.d(TAG, "cargarUsuariosDisponibles: Índice de columna APP: $idxApp")
                usuariosDisponibles.clear()
                usuariosMap.clear()
                usuariosDisponibles.add("Todos los usuarios")
                usuariosMap["Todos los usuarios"] = ""
                
                trabajadores.forEach { trabajador ->
                    Log.d(TAG, "cargarUsuariosDisponibles: Trabajador raw: $trabajador")
                    if (trabajador.size >= 3 && idxApp != -1 && trabajador.size > idxApp) {
                        val username = trabajador[idxApp].toString()
                        val nombre = "${trabajador[1]} ${trabajador[2]}".trim()
                        usuariosDisponibles.add(nombre)
                        usuariosMap[nombre] = username
                        Log.d(TAG, "cargarUsuariosDisponibles: Usuario añadido: $nombre ($username)")
                    } else {
                        Log.w(TAG, "cargarUsuariosDisponibles: Trabajador con datos insuficientes: ${trabajador.size} campos o sin APP")
                    }
                }
                Log.d(TAG, "cargarUsuariosDisponibles: usuariosDisponibles = $usuariosDisponibles")
                Log.d(TAG, "cargarUsuariosDisponibles: usuariosMap = $usuariosMap")
                val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item_selected, usuariosDisponibles)
                adapter.setDropDownViewResource(R.layout.spinner_item_dropdown)
                spinnerUsuario.adapter = adapter
                Log.d(TAG, "cargarUsuariosDisponibles: Spinner actualizado con ${usuariosDisponibles.size} usuarios")
                
            } catch (e: Exception) {
                Log.e(TAG, "cargarUsuariosDisponibles: Error cargando usuarios", e)
                Toast.makeText(requireContext(), "Error cargando usuarios: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun generarReporte(auto: Boolean = false) {
        Log.d(TAG, "generarReporte: Iniciando generación de reporte")
        if (fechaInicio == null || fechaFin == null) {
            Log.w(TAG, "generarReporte: Fechas no seleccionadas")
            if (!auto) Toast.makeText(requireContext(), "Debe seleccionar las fechas", Toast.LENGTH_SHORT).show()
            mostrarMensajeSinDatos("Debe seleccionar las fechas")
            return
        }

        Log.d(TAG, "generarReporte: Fechas válidas - Inicio: ${dateFormat.format(fechaInicio!!)}, Fin: ${dateFormat.format(fechaFin!!)}")
        Log.d(TAG, "generarReporte: Vista seleccionada: $vistaActual")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "generarReporte: Abriendo credentials.json")
                val credentialsStream = requireContext().assets.open("credentials.json")
                Log.d(TAG, "generarReporte: Credentials abiertos correctamente")
                
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                Log.d(TAG, "generarReporte: GoogleSheetsManager creado")
                
                // Obtener fichajes del período
                Log.d(TAG, "generarReporte: Obteniendo fichajes...")
                val response = withContext(Dispatchers.IO) {
                    sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:H")
                        .execute()
                }
                Log.d(TAG, "generarReporte: Respuesta de fichajes obtenida")
                
                val todosFichajes = response.getValues() ?: emptyList()
                Log.d(TAG, "generarReporte: Total de fichajes obtenidos: ${todosFichajes.size}")
                
                val fichajesFiltrados = filtrarFichajes(todosFichajes)
                Log.d(TAG, "generarReporte: Fichajes filtrados: ${fichajesFiltrados.size}")
                
                Log.d(TAG, "generarReporte: Mostrando resultados en UI")
                if (fichajesFiltrados.isEmpty()) {
                    mostrarMensajeSinDatos("No hay fichajes para los filtros seleccionados")
                } else if (vistaActual == "POR_DIA") {
                    mostrarResultadosPorDia(fichajesFiltrados)
                } else {
                    mostrarResultadosPorUsuario(fichajesFiltrados)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "generarReporte: Error generando reporte", e)
                if (!auto) Toast.makeText(requireContext(), "Error generando reporte: ${e.message}", Toast.LENGTH_LONG).show()
                mostrarMensajeSinDatos("Error generando reporte: ${e.message}")
            }
        }
    }

    private fun filtrarFichajes(todosFichajes: List<List<Any>>): List<List<Any>> {
        Log.d(TAG, "filtrarFichajes: Iniciando filtrado de ${todosFichajes.size} fichajes")
        val nombreSeleccionado = spinnerUsuario.selectedItem.toString()
        val usuarioSeleccionado = usuariosMap[nombreSeleccionado] ?: ""
        Log.d(TAG, "filtrarFichajes: Usuario seleccionado: $nombreSeleccionado ($usuarioSeleccionado)")
        Log.d(TAG, "filtrarFichajes: usuariosMap = $usuariosMap")
        Log.d(TAG, "filtrarFichajes: Primeros 5 fichajes: ${todosFichajes.take(5)}")
        
        return todosFichajes.filter { fichaje ->
            if (fichaje.size < 4) {
                Log.d(TAG, "filtrarFichajes: Fichaje con datos insuficientes, descartado: $fichaje")
                return@filter false
            }
            val fechaFichaje = fichaje[1].toString()
            if (fechaFichaje.equals("FECHA", ignoreCase = true)) {
                Log.d(TAG, "filtrarFichajes: Fila de cabecera detectada, descartada: $fichaje")
                return@filter false
            }
            val usuarioFichaje = fichaje[0].toString()
            Log.d(TAG, "filtrarFichajes: Comparando usuarioFichaje='$usuarioFichaje' con usuarioSeleccionado='$usuarioSeleccionado'")
            // Filtrar por fecha
            val fecha = try {
                dateFormat.parse(fechaFichaje)
            } catch (e: Exception) {
                Log.w(TAG, "filtrarFichajes: Error parseando fecha $fechaFichaje", e)
                null
            }
            if (fecha == null || fecha.before(fechaInicio) || fecha.after(fechaFin)) {
                Log.d(TAG, "filtrarFichajes: Fichaje fuera de rango de fechas: $fechaFichaje ($fichaje)")
                return@filter false
            }
            // Filtrar por usuario
            if (nombreSeleccionado != "Todos los usuarios") {
                val coincide = usuarioFichaje == usuarioSeleccionado
                if (!coincide) {
                    Log.d(TAG, "filtrarFichajes: Usuario no coincide: $usuarioFichaje vs $usuarioSeleccionado")
                } else {
                    Log.d(TAG, "filtrarFichajes: Usuario coincide: $usuarioFichaje == $usuarioSeleccionado")
                }
                return@filter coincide
            }
            true
        }.also {
            Log.d(TAG, "filtrarFichajes: Filtrado completado, ${it.size} fichajes válidos")
        }
    }

    private fun mostrarResultadosPorDia(fichajes: List<List<Any>>) {
        Log.d(TAG, "mostrarResultadosPorDia: Mostrando ${fichajes.size} fichajes por día")
        layoutResultados.removeAllViews()

        // Agrupar por día
        val fichajesPorDia = fichajes.groupBy { it[1].toString() }.toSortedMap()
        Log.d(TAG, "mostrarResultadosPorDia: ${fichajesPorDia.size} días con fichajes")

        fichajesPorDia.forEach { (fecha, fichajesDia) ->
            Log.d(TAG, "mostrarResultadosPorDia: Procesando día $fecha con ${fichajesDia.size} fichajes")
            
            // Título del día
            val tituloDia = TextView(requireContext())
            tituloDia.text = "Fichajes del $fecha"
            tituloDia.setTypeface(null, Typeface.BOLD)
            tituloDia.textSize = 16f
            tituloDia.gravity = Gravity.START
            tituloDia.setTextColor(resources.getColor(R.color.bitherm_blue, null))
            val paddingTop = (16 * resources.displayMetrics.density).toInt()
            val paddingBottom = (4 * resources.displayMetrics.density).toInt()
            tituloDia.setPadding(0, paddingTop, 0, paddingBottom)
            layoutResultados.addView(tituloDia)

            // Tabla del día
            val tabla = TableLayout(requireContext())
            tabla.setPadding(16, 8, 16, 8)
            tabla.isStretchAllColumns = true

            // Header
            val headerRow = TableRow(requireContext())
            val headers = listOf("Usuario", "Entrada", "Salida", "H.N", "H.E")
            headers.forEach { header ->
                val tv = TextView(requireContext())
                tv.text = header
                tv.setTypeface(null, Typeface.BOLD)
                tv.gravity = Gravity.CENTER
                tv.setBackgroundResource(android.R.color.darker_gray)
                tv.setTextColor(resources.getColor(android.R.color.white, null))
                tv.setPadding(8, 4, 8, 4)
                headerRow.addView(tv)
            }
            tabla.addView(headerRow)

            // Agrupar fichajes por usuario para este día
            val fichajesPorUsuario = fichajesDia.groupBy { it[0].toString() }
            
            fichajesPorUsuario.forEach { (usuario, fichajesUsuario) ->
                val entrada = fichajesUsuario.find { it[3] == "ENTRADA" }
                val salida = fichajesUsuario.find { it[3] == "SALIDA" }
                
                if (entrada != null && salida != null) {
                    val estadisticas = calcularEstadisticasDia(entrada, salida, fecha)
                    
                    val row = TableRow(requireContext())
                    
                    // Usuario
                    val tvUsuario = TextView(requireContext())
                    tvUsuario.text = usuario
                    tvUsuario.gravity = Gravity.CENTER
                    tvUsuario.setPadding(8, 4, 8, 4)
                    tvUsuario.setTextColor(Color.WHITE)
                    row.addView(tvUsuario)

                    // Entrada
                    val tvEntrada = TextView(requireContext())
                    tvEntrada.text = entrada[2].toString()
                    tvEntrada.gravity = Gravity.CENTER
                    tvEntrada.setPadding(8, 4, 8, 4)
                    tvEntrada.setTextColor(Color.WHITE)
                    row.addView(tvEntrada)

                    // Salida
                    val tvSalida = TextView(requireContext())
                    tvSalida.text = salida[2].toString()
                    tvSalida.gravity = Gravity.CENTER
                    tvSalida.setPadding(8, 4, 8, 4)
                    tvSalida.setTextColor(Color.WHITE)
                    row.addView(tvSalida)

                    // Horas totales
                    val tvHoras = TextView(requireContext())
                    tvHoras.text = String.format("%.2f", estadisticas.horasTotales)
                    tvHoras.gravity = Gravity.CENTER
                    tvHoras.setPadding(8, 4, 8, 4)
                    tvHoras.setTextColor(Color.WHITE)
                    row.addView(tvHoras)

                    // Horas extra
                    val tvHorasExtra = TextView(requireContext())
                    tvHorasExtra.text = String.format("%.2f", estadisticas.horasExtra)
                    tvHorasExtra.gravity = Gravity.CENTER
                    tvHorasExtra.setPadding(8, 4, 8, 4)
                    tvHorasExtra.setTextColor(Color.WHITE)
                    row.addView(tvHorasExtra)

                    tabla.addView(row)
                }
            }

            layoutResultados.addView(tabla)
        }
        
        Log.d(TAG, "mostrarResultadosPorDia: Visualización completada")
    }

    private fun mostrarResultadosPorUsuario(fichajes: List<List<Any>>) {
        Log.d(TAG, "mostrarResultadosPorUsuario: Mostrando ${fichajes.size} fichajes por usuario")
        layoutResultados.removeAllViews()

        // Agrupar por usuario
        val fichajesPorUsuario = fichajes.groupBy { it[0].toString() }
        Log.d(TAG, "mostrarResultadosPorUsuario: ${fichajesPorUsuario.size} usuarios con fichajes")

        fichajesPorUsuario.forEach { (usuario, fichajesUsuario) ->
            Log.d(TAG, "mostrarResultadosPorUsuario: Procesando usuario $usuario con ${fichajesUsuario.size} fichajes")
            
            // Título del usuario
            val tituloUsuario = TextView(requireContext())
            tituloUsuario.text = "Fichajes de $usuario"
            tituloUsuario.setTypeface(null, Typeface.BOLD)
            tituloUsuario.textSize = 16f
            tituloUsuario.gravity = Gravity.START
            tituloUsuario.setTextColor(resources.getColor(R.color.bitherm_blue, null))
            val paddingTop = (16 * resources.displayMetrics.density).toInt()
            val paddingBottom = (4 * resources.displayMetrics.density).toInt()
            tituloUsuario.setPadding(0, paddingTop, 0, paddingBottom)
            layoutResultados.addView(tituloUsuario)

            // Tabla del usuario
            val tabla = TableLayout(requireContext())
            tabla.setPadding(16, 8, 16, 8)
            tabla.isStretchAllColumns = true

            // Header
            val headerRow = TableRow(requireContext())
            val headers = listOf("Fecha", "Entrada", "Salida", "H.N", "H.E")
            headers.forEach { header ->
                val tv = TextView(requireContext())
                tv.text = header
                tv.setTypeface(null, Typeface.BOLD)
                tv.gravity = Gravity.CENTER
                tv.setBackgroundResource(android.R.color.darker_gray)
                tv.setTextColor(resources.getColor(android.R.color.white, null))
                tv.setPadding(8, 4, 8, 4)
                headerRow.addView(tv)
            }
            tabla.addView(headerRow)

            // Agrupar fichajes por día para este usuario
            val fichajesPorDia = fichajesUsuario.groupBy { it[1].toString() }.toSortedMap()
            
            fichajesPorDia.forEach { (fecha, fichajesDia) ->
                val entrada = fichajesDia.find { it[3] == "ENTRADA" }
                val salida = fichajesDia.find { it[3] == "SALIDA" }
                
                if (entrada != null && salida != null) {
                    val estadisticas = calcularEstadisticasDia(entrada, salida, fecha)
                    
                    val row = TableRow(requireContext())
                    
                    // Fecha
                    val tvFecha = TextView(requireContext())
                    tvFecha.text = fecha
                    tvFecha.gravity = Gravity.CENTER
                    tvFecha.setPadding(8, 4, 8, 4)
                    tvFecha.setTextColor(Color.WHITE)
                    row.addView(tvFecha)

                    // Entrada
                    val tvEntrada = TextView(requireContext())
                    tvEntrada.text = entrada[2].toString()
                    tvEntrada.gravity = Gravity.CENTER
                    tvEntrada.setPadding(8, 4, 8, 4)
                    tvEntrada.setTextColor(Color.WHITE)
                    row.addView(tvEntrada)

                    // Salida
                    val tvSalida = TextView(requireContext())
                    tvSalida.text = salida[2].toString()
                    tvSalida.gravity = Gravity.CENTER
                    tvSalida.setPadding(8, 4, 8, 4)
                    tvSalida.setTextColor(Color.WHITE)
                    row.addView(tvSalida)

                    // Horas totales
                    val tvHoras = TextView(requireContext())
                    tvHoras.text = String.format("%.2f", estadisticas.horasTotales)
                    tvHoras.gravity = Gravity.CENTER
                    tvHoras.setPadding(8, 4, 8, 4)
                    tvHoras.setTextColor(Color.WHITE)
                    row.addView(tvHoras)

                    // Horas extra
                    val tvHorasExtra = TextView(requireContext())
                    tvHorasExtra.text = String.format("%.2f", estadisticas.horasExtra)
                    tvHorasExtra.gravity = Gravity.CENTER
                    tvHorasExtra.setPadding(8, 4, 8, 4)
                    tvHorasExtra.setTextColor(Color.WHITE)
                    row.addView(tvHorasExtra)

                    tabla.addView(row)
                }
            }

            layoutResultados.addView(tabla)
        }
        
        Log.d(TAG, "mostrarResultadosPorUsuario: Visualización completada")
    }

    data class EstadisticasDia(
        val horasTotales: Float,
        val horasExtra: Float
    )

    private fun calcularEstadisticasDia(entrada: List<Any>, salida: List<Any>, fecha: String): EstadisticasDia {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val t1 = sdf.parse("$fecha ${entrada[2]}")?.time ?: 0L
            val t2 = sdf.parse("$fecha ${salida[2]}")?.time ?: 0L
            val diff = t2 - t1
            val horas = diff / (1000f * 60f * 60f)
            val horasExtra = if (horas > 8f) horas - 8f else 0f
            
            Log.d(TAG, "calcularEstadisticasDia: $fecha - Horas: $horas, Extra: $horasExtra")
            EstadisticasDia(horas, horasExtra)
        } catch (e: Exception) {
            Log.e(TAG, "calcularEstadisticasDia: Error calculando estadísticas para $fecha", e)
            EstadisticasDia(0f, 0f)
        }
    }

    private fun exportarReporte() {
        Log.d(TAG, "exportarReporte: Función de exportación en desarrollo")
        // Aquí implementarías la exportación a PDF/Excel
        Toast.makeText(requireContext(), "Función de exportación en desarrollo", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarMensajeSinDatos(mensaje: String) {
        layoutResultados.removeAllViews()
        val tv = TextView(requireContext())
        tv.text = mensaje
        tv.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        tv.textSize = 16f
        tv.gravity = Gravity.CENTER
        tv.setPadding(32, 32, 32, 32)
        layoutResultados.addView(tv)
    }
} 