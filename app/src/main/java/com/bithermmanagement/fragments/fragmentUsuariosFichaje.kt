package com.bithermmanagement.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import android.content.Context
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bithermmanagement.data.GoogleSheetsManager
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.location.Location
import android.os.CountDownTimer
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.LinearLayout
import android.view.Gravity
import android.graphics.Typeface
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.EditText
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.suspendCancellableCoroutine

class fragmentUsuariosFichaje : Fragment() {
    private lateinit var tvNombreTrabajador: TextView
    private lateinit var tvEstadoFichaje: TextView
    private lateinit var btnEntrada: Button
    private lateinit var btnSalida: Button
    private val FICHAJE_LOGS_SHEET = "FICHAJE-LOGS"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var locationPermissionAsked = false
    private lateinit var layoutHistorial: LinearLayout
    private lateinit var tvContador: TextView
    private var entradaTimestamp: Long? = null
    private var contador: CountDownTimer? = null
    private var nombreUsuario: String = ""
    private var apellidosUsuario: String = ""
    private var handlerLogAuto: Handler? = null
    private var runnableLogAuto: Runnable? = null
    private var intervaloLogAuto: Long = 15 * 60 * 1000L // 15 minutos por defecto
    private var modoLogAuto: String = "NO"
    private var logAutoActivo: Boolean = false
    private var ultimaLatLogAuto: Double? = null
    private var ultimaLonLogAuto: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_usuarios_fichaje, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireContext())
        tvNombreTrabajador = view.findViewById(R.id.tv_nombre_trabajador)
        tvEstadoFichaje = view.findViewById(R.id.tv_estado_fichaje)
        btnEntrada = view.findViewById(R.id.btn_registrar_entrada)
        btnSalida = view.findViewById(R.id.btn_registrar_salida)
        layoutHistorial = view.findViewById(R.id.layout_historial_fichaje)
        tvContador = TextView(requireContext())
        tvContador.textSize = 32f
        tvContador.setTypeface(null, Typeface.BOLD)
        tvContador.gravity = Gravity.CENTER
        tvContador.setTextColor(resources.getColor(android.R.color.white, null))
        val cardRegistro = (btnEntrada.parent as? LinearLayout)
        cardRegistro?.addView(tvContador, cardRegistro.indexOfChild(btnSalida) + 1)
        tvContador.visibility = View.GONE

        // Desactivar botones hasta tener permisos
        btnEntrada.isEnabled = false
        btnSalida.isEnabled = false
        checkLocationPermission()

        val prefs = requireContext().getSharedPreferences("bitherm_prefs", Context.MODE_PRIVATE)
        val usuarioLogin = prefs.getString("username", "") ?: ""

        CoroutineScope(Dispatchers.Main).launch {
            val credentialsStream = requireContext().assets.open("credentials.json")
            val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
            Log.d("FICHAJE", "Buscando usuario con username: '" + usuarioLogin + "'")
            val (header, filaUsuario) = sheetsManager.getUserRowByApp(usuarioLogin)
            var filaEncontrada = filaUsuario
            if (filaUsuario == null && usuarioLogin.isNotEmpty()) {
                // Búsqueda robusta: ignorar mayúsculas, espacios y tildes
                val idxApp = header.indexOf("APP")
                val dataResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .get(sheetsManager.spreadsheetIdPublic, "TRABAJADORES!A3:Z")
                    .execute()
                val dataRows = dataResponse.getValues() ?: emptyList<List<Any>>()
                filaEncontrada = dataRows.firstOrNull {
                    it.size > idxApp &&
                    it[idxApp].toString().trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u") ==
                    usuarioLogin.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                }
                Log.d("FICHAJE", "Resultado búsqueda robusta: $filaEncontrada")
            }
            if (filaEncontrada != null) {
                val idxNombre = header.indexOf("NOMBRE")
                val idxApellidos = header.indexOf("APELLIDOS")
                nombreUsuario = if (idxNombre != -1 && filaEncontrada.size > idxNombre) filaEncontrada[idxNombre].toString() else ""
                apellidosUsuario = if (idxApellidos != -1 && filaEncontrada.size > idxApellidos) filaEncontrada[idxApellidos].toString() else ""
                tvNombreTrabajador.text = "$nombreUsuario $apellidosUsuario"
                Log.d("FICHAJE", "Nombre encontrado: $nombreUsuario $apellidosUsuario")
            } else {
                tvNombreTrabajador.text = "Usuario no encontrado: $usuarioLogin"
                Log.e("FICHAJE", "No se encontró el usuario con APP: $usuarioLogin en la hoja TRABAJADORES")
            }
            actualizarEstadoFichaje(sheetsManager, usuarioLogin)
            btnEntrada.setOnClickListener {
                fichar(sheetsManager, usuarioLogin, "ENTRADA")
            }
            btnSalida.setOnClickListener {
                fichar(sheetsManager, usuarioLogin, "SALIDA")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        contador?.cancel()
    }

    private suspend fun actualizarEstadoFichaje(sheetsManager: GoogleSheetsManager, usuario: String) {
        withContext(Dispatchers.IO) {
            try {
                val hoy = dateFormat.format(Date())
                val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .get(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:H")
                    .execute()
                val values = response.getValues() ?: emptyList<List<Any>>()
                val fichajesHoy = values.filter { it.size > 3 && it[0] == usuario && it[1] == hoy }
                val ultimaEntrada = fichajesHoy.lastOrNull { it[3] == "ENTRADA" }
                val ultimaSalida = fichajesHoy.lastOrNull { it[3] == "SALIDA" }
                withContext(Dispatchers.Main) {
                    btnEntrada.visibility = View.GONE
                    btnSalida.visibility = View.GONE
                    tvContador.visibility = View.GONE
                    contador?.cancel()
                    Log.d("FICHAJE", "Estado actual: entrada=$ultimaEntrada, salida=$ultimaSalida")
                    if (ultimaEntrada != null && (ultimaSalida == null || fichajesHoy.indexOf(ultimaEntrada) > fichajesHoy.indexOf(ultimaSalida))) {
                        tvEstadoFichaje.text = "Estado: Fichado (entrada)"
                        btnSalida.visibility = View.VISIBLE
                        btnSalida.isEnabled = true
                        btnEntrada.isEnabled = false
                        // Calcular tiempo desde la entrada
                        val fechaEntrada = "${hoy} ${ultimaEntrada[2]}"
                        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        entradaTimestamp = try { sdf.parse(fechaEntrada)?.time } catch (e: Exception) { null }
                        entradaTimestamp?.let { startContador(it) }
                    } else {
                        tvEstadoFichaje.text = "Estado: No fichado"
                        btnEntrada.visibility = View.VISIBLE
                        btnEntrada.isEnabled = true
                        btnSalida.isEnabled = false
                    }
                    mostrarHistorial(values, usuario)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvEstadoFichaje.text = "Estado: Error"
                    btnEntrada.visibility = View.GONE
                    btnSalida.visibility = View.GONE
                    tvContador.visibility = View.GONE
                    contador?.cancel()
                    Toast.makeText(requireContext(), "Error consultando fichaje: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startContador(entradaMillis: Long) {
        tvContador.visibility = View.VISIBLE
        contador?.cancel()
        contador = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val diff = System.currentTimeMillis() - entradaMillis
                val horas = diff / (1000 * 60 * 60)
                val minutos = (diff / (1000 * 60)) % 60
                val segundos = (diff / 1000) % 60
                tvContador.text = String.format("%02d:%02d:%02d", horas, minutos, segundos)
            }
            override fun onFinish() {}
        }.start()
    }

    private fun mostrarHistorial(values: List<List<Any>>, usuario: String) {
        layoutHistorial.removeAllViews()
        val hoy = Date()
        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfMes = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val sdfNombreMes = SimpleDateFormat("MMMM yyyy", Locale("es"))
        val hoyStr = sdfFecha.format(hoy)
        val mesActual = sdfMes.format(hoy)
        val cal = Calendar.getInstance()
        cal.time = hoy
        cal.add(Calendar.MONTH, -1)
        val mesAnterior = sdfMes.format(cal.time)
        val nombreMesAnterior = sdfNombreMes.format(cal.time).replaceFirstChar { it.uppercase() }
        cal.add(Calendar.MONTH, -1)
        val mesAnterior2 = sdfMes.format(cal.time)
        val nombreMesAnterior2 = sdfNombreMes.format(cal.time).replaceFirstChar { it.uppercase() }
        cal.time = hoy
        val nombreMesActual = sdfNombreMes.format(cal.time).replaceFirstChar { it.uppercase() }

        // --- Historial de Hoy ---
        val tablaHoy = crearTablaHistorial(values, usuario) { fecha -> fecha == hoyStr }
        layoutHistorial.addView(tablaHoy)

        // --- Tabla Resumen Mensual ---
        val resumenLabel = TextView(requireContext())
        resumenLabel.text = "Resumen"
        resumenLabel.setTypeface(null, Typeface.BOLD)
        resumenLabel.textSize = 16f
        resumenLabel.gravity = Gravity.START
        resumenLabel.setTextColor(resources.getColor(R.color.bitherm_blue, null))
        val resumenPaddingTop = (16 * resources.displayMetrics.density).toInt()
        val resumenPaddingBottom = (4 * resources.displayMetrics.density).toInt()
        resumenLabel.setPadding(0, resumenPaddingTop, 0, resumenPaddingBottom)
        layoutHistorial.addView(resumenLabel)
        val tablaResumen = crearTablaResumenMensual(values, usuario)
        layoutHistorial.addView(tablaResumen)

        // --- Mes actual ---
        val tituloMesActual = TextView(requireContext())
        tituloMesActual.text = "Fichajes mes $nombreMesActual"
        tituloMesActual.setTypeface(null, Typeface.BOLD)
        tituloMesActual.textSize = 16f
        tituloMesActual.gravity = Gravity.START
        tituloMesActual.setTextColor(resources.getColor(R.color.bitherm_blue, null))
        tituloMesActual.setPadding(0, resumenPaddingTop, 0, resumenPaddingBottom)
        layoutHistorial.addView(tituloMesActual)
        val tablaMesActual = crearTablaHistorial(values, usuario) { fecha -> fecha.endsWith("/${mesActual}") }
        layoutHistorial.addView(tablaMesActual)

        // --- Mes anterior ---
        val tituloMesAnterior = TextView(requireContext())
        tituloMesAnterior.text = "Fichajes mes $nombreMesAnterior"
        tituloMesAnterior.setTypeface(null, Typeface.BOLD)
        tituloMesAnterior.textSize = 16f
        tituloMesAnterior.gravity = Gravity.START
        tituloMesAnterior.setTextColor(resources.getColor(R.color.bitherm_blue, null))
        tituloMesAnterior.setPadding(0, resumenPaddingTop, 0, resumenPaddingBottom)
        layoutHistorial.addView(tituloMesAnterior)
        val tablaMesAnterior = crearTablaHistorial(values, usuario) { fecha -> fecha.endsWith("/${mesAnterior}") }
        layoutHistorial.addView(tablaMesAnterior)

        // --- Mes anterior al anterior ---
        val tituloMesAnterior2 = TextView(requireContext())
        tituloMesAnterior2.text = "Fichajes mes $nombreMesAnterior2"
        tituloMesAnterior2.setTypeface(null, Typeface.BOLD)
        tituloMesAnterior2.textSize = 16f
        tituloMesAnterior2.gravity = Gravity.START
        tituloMesAnterior2.setTextColor(resources.getColor(R.color.bitherm_blue, null))
        tituloMesAnterior2.setPadding(0, resumenPaddingTop, 0, resumenPaddingBottom)
        layoutHistorial.addView(tituloMesAnterior2)
        val tablaMesAnterior2 = crearTablaHistorial(values, usuario) { fecha -> fecha.endsWith("/${mesAnterior2}") }
        layoutHistorial.addView(tablaMesAnterior2)
    }

    private fun crearTablaHistorial(values: List<List<Any>>, usuario: String, filtroFecha: (String) -> Boolean): TableLayout {
        val tabla = TableLayout(requireContext())
        val params = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 16, 0, 0)
        tabla.layoutParams = params
        tabla.setPadding(16, 8, 16, 8)
        tabla.isStretchAllColumns = true
        tabla.gravity = Gravity.CENTER
        val headerRow = TableRow(requireContext())
        val headers = listOf("Fecha", "Entrada", "Salida", "Tiempo")
        headers.forEach {
            val tv = TextView(requireContext())
            tv.text = it
            tv.setTypeface(null, Typeface.BOLD)
            tv.gravity = Gravity.CENTER
            tv.setBackgroundResource(android.R.color.darker_gray)
            tv.setPadding(8, 4, 8, 4)
            headerRow.addView(tv)
        }
        tabla.addView(headerRow)
        // Agrupar por fecha y buscar entrada/salida
        val registrosPorDia = values.filter { it.size > 3 && it[0] == usuario && filtroFecha(it[1].toString()) }
            .groupBy { it[1].toString() }
            .toList()
            .sortedByDescending { it.first }
        registrosPorDia.forEach { (fecha, registros) ->
            val entrada = registros.find { it[3] == "ENTRADA" }
            val salida = registros.find { it[3] == "SALIDA" }
            val row = TableRow(requireContext())
            val fechaCorta = fecha.take(5)
            val entradaHora = entrada?.getOrNull(2)?.toString() ?: "--"
            val salidaHora = salida?.getOrNull(2)?.toString() ?: "--"
            val tiempo = if (entrada != null && salida != null) {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val t1 = sdf.parse("$fecha ${entrada[2]}")?.time ?: 0L
                    val t2 = sdf.parse("$fecha ${salida[2]}")?.time ?: 0L
                    val diff = t2 - t1
                    val h = diff / (1000 * 60 * 60)
                    val m = (diff / (1000 * 60)) % 60
                    String.format("%02d:%02d", h, m)
                } catch (e: Exception) { "--" }
            } else "--"
            listOf(fechaCorta, entradaHora, salidaHora, tiempo).forEach {
                val tv = TextView(requireContext())
                tv.text = it
                tv.gravity = Gravity.CENTER
                tv.setPadding(8, 4, 8, 4)
                tv.setBackgroundResource(android.R.color.white)
                tv.setTextColor(resources.getColor(android.R.color.black, null))
                row.addView(tv)
            }
            row.setBackgroundResource(android.R.color.darker_gray)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            lp.setMargins(1, 1, 1, 1)
            row.layoutParams = lp
            tabla.addView(row)
        }
        return tabla
    }

    private fun crearTablaResumenMensual(values: List<List<Any>>, usuario: String): TableLayout {
        val tabla = TableLayout(requireContext())
        val params = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 16, 0, 0)
        tabla.layoutParams = params
        tabla.setPadding(16, 8, 16, 8)
        tabla.isStretchAllColumns = true
        tabla.gravity = Gravity.CENTER
        val headerRow = TableRow(requireContext())
        val headers = listOf("Mes", "DÍAS", "Σ horas", "Σ h_ext")
        headers.forEach {
            val tv = TextView(requireContext())
            tv.text = it
            tv.setTypeface(null, Typeface.BOLD)
            tv.gravity = Gravity.CENTER
            tv.setBackgroundResource(android.R.color.darker_gray)
            tv.setPadding(8, 4, 8, 4)
            headerRow.addView(tv)
        }
        tabla.addView(headerRow)
        // Agrupar por mes
        val sdfMes = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val sdfNombreMes = SimpleDateFormat("MMMM yyyy", Locale("es"))
        val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val sdfHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        val registrosPorMes = values.filter { it.size > 3 && it[0] == usuario }
            .groupBy { it[1].toString().takeLast(7) } // MM/yyyy
            .toSortedMap(compareByDescending { it })
        registrosPorMes.forEach { (mes, registros) ->
            // Agrupar por día
            val registrosPorDia = registros.groupBy { it[1].toString() }
            var dias = 0
            var horasTotales = 0f
            var horasExtraTotales = 0f
            registrosPorDia.forEach { (fecha, fichajes) ->
                val entrada = fichajes.find { it[3] == "ENTRADA" }
                val salida = fichajes.find { it[3] == "SALIDA" }
                if (entrada != null && salida != null) {
                    try {
                        val t1 = sdfFecha.parse(fecha)?.let { f ->
                            sdfHora.parse(entrada[2].toString())?.let { h ->
                                Calendar.getInstance().apply {
                                    time = f
                                    set(Calendar.HOUR_OF_DAY, h.hours)
                                    set(Calendar.MINUTE, h.minutes)
                                }.timeInMillis
                            }
                        } ?: 0L
                        val t2 = sdfFecha.parse(fecha)?.let { f ->
                            sdfHora.parse(salida[2].toString())?.let { h ->
                                Calendar.getInstance().apply {
                                    time = f
                                    set(Calendar.HOUR_OF_DAY, h.hours)
                                    set(Calendar.MINUTE, h.minutes)
                                }.timeInMillis
                            }
                        } ?: 0L
                        val diff = t2 - t1
                        val horas = diff / (1000f * 60f * 60f)
                        horasTotales += horas
                        if (horas > 0f) dias++
                        if (horas > 8f) horasExtraTotales += (horas - 8f)
                    } catch (_: Exception) {}
                }
            }
            val nombreMes = try { sdfNombreMes.format(SimpleDateFormat("MM/yyyy").parse(mes)!!) } catch (_: Exception) { mes }
            val row = TableRow(requireContext())
            listOf(nombreMes, dias.toString(), "%.2f".format(horasTotales), "%.2f".format(horasExtraTotales)).forEach {
                val tv = TextView(requireContext())
                tv.text = it
                tv.gravity = Gravity.CENTER
                tv.setPadding(8, 4, 8, 4)
                tv.setBackgroundResource(android.R.color.white)
                tv.setTextColor(resources.getColor(android.R.color.black, null))
                row.addView(tv)
            }
            row.setBackgroundResource(android.R.color.darker_gray)
            val lp = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT)
            lp.setMargins(1, 1, 1, 1)
            row.layoutParams = lp
            tabla.addView(row)
        }
        return tabla
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            btnEntrada.isEnabled = false
            btnSalida.isEnabled = false
            Toast.makeText(requireContext(), "Debes conceder el permiso de ubicación para fichar", Toast.LENGTH_LONG).show()
            if (!locationPermissionAsked) {
                locationPermissionAsked = true
                Handler(Looper.getMainLooper()).post {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
                }
            }
        } else {
            btnEntrada.isEnabled = true
            btnSalida.isEnabled = true
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                lastKnownLocation = location
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                btnEntrada.isEnabled = true
                btnSalida.isEnabled = true
                getLastLocation()
            } else {
                btnEntrada.isEnabled = false
                btnSalida.isEnabled = false
                Toast.makeText(requireContext(), "Permiso de localización denegado. No puedes fichar.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun mostrarDialogoSeleccionOTs(ots: List<String>): List<String> = suspendCancellableCoroutine { continuation ->
        val checkedItems = BooleanArray(ots.size) { false }
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Selecciona las OTs trabajadas hoy")
        builder.setMultiChoiceItems(ots.toTypedArray(), checkedItems) { _, which, isChecked ->
            checkedItems[which] = isChecked
        }
        builder.setPositiveButton("OK") { dialog, _ ->
            val seleccionadas = ots.filterIndexed { idx, _ -> checkedItems[idx] }
            dialog.dismiss()
            continuation.resume(seleccionadas, null)
        }
        builder.setCancelable(false)
        builder.show()
    }

    private suspend fun mostrarDialogoSliders(ots: List<String>, horasMax: Float, esExtra: Boolean = false): Map<String, Float> = suspendCancellableCoroutine { continuation ->
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(32, 32, 32, 32)
        val labelHoras = TextView(requireContext())
        labelHoras.text = "Horas a repartir: %.2f h".format(horasMax)
        labelHoras.setPadding(0, 0, 0, 16)
        layout.addView(labelHoras)
        val sliders = mutableListOf<Pair<String, SeekBar>>()
        val valores = mutableMapOf<String, Float>()
        val labelsValor = mutableMapOf<String, TextView>()
        val sliderLayoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        ots.forEach { ot ->
            val row = LinearLayout(requireContext())
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, 8, 0, 8)
            val label = TextView(requireContext())
            label.text = ot
            label.setPadding(0, 0, 16, 0)
            label.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            val seek = SeekBar(requireContext())
            seek.max = (horasMax * 4).toInt() // paso de 0.25
            seek.progress = (horasMax / ots.size * 4).toInt()
            seek.layoutParams = sliderLayoutParams
            val labelValor = TextView(requireContext())
            labelValor.text = "%.2f h".format(seek.progress / 4f)
            labelValor.setPadding(16, 0, 0, 0)
            labelValor.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(label)
            row.addView(seek)
            row.addView(labelValor)
            layout.addView(row)
            sliders.add(ot to seek)
            valores[ot] = horasMax / ots.size
            labelsValor[ot] = labelValor
        }
        val totalLabel = TextView(requireContext())
        totalLabel.text = "Total asignado: %.2f h".format(horasMax)
        totalLabel.setPadding(0, 16, 0, 16)
        layout.addView(totalLabel)
        // Lógica de sliders
        sliders.forEach { (ot, seek) ->
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val valor = progress / 4f
                    valores[ot] = valor
                    labelsValor[ot]?.text = "%.2f h".format(valor)
                    if (sliders.size == 2 && fromUser) {
                        // Autoajuste para dos sliders
                        val otro = sliders.first { it.first != ot }
                        val otroValor = horasMax - valor
                        otro.second.progress = (otroValor * 4).toInt().coerceIn(0, otro.second.max)
                        valores[otro.first] = otroValor
                        labelsValor[otro.first]?.text = "%.2f h".format(otroValor)
                    }
                    val suma = valores.values.sum()
                    totalLabel.text = "Total asignado: %.2f h".format(suma)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(if (esExtra) "Reparte las horas extra trabajadas" else "Reparte las horas normales trabajadas")
        builder.setView(layout)
        builder.setPositiveButton("GUARDAR") { dialog, _ ->
            val suma = valores.values.sum()
            if (Math.abs(suma - horasMax) > 0.1) {
                Toast.makeText(requireContext(), "La suma debe ser %.2f h".format(horasMax), Toast.LENGTH_LONG).show()
            } else {
                Log.d("FICHAJE-OT-MES", "Pulsado GUARDAR en sliders (${if (esExtra) "extra" else "normales"}), valores: $valores")
                Toast.makeText(requireContext(), "GUARDAR sliders (${if (esExtra) "extra" else "normales"}): $valores", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
                continuation.resume(valores, null)
            }
        }
        builder.setNegativeButton("ATRÁS") { dialog, _ ->
            dialog.dismiss()
            continuation.resume(emptyMap(), null)
        }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)?.setTextColor(resources.getColor(R.color.bitherm_blue, null))
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)?.setTextColor(resources.getColor(R.color.bitherm_blue, null))
        }
        builder.setCancelable(false)
        dialog.show()
    }

    private suspend fun obtenerOTsUsuarioHoy(usuario: String): List<String> {
        Log.d("FICHAJE-OT-MES", "[obtenerOTsUsuarioHoy] INICIO para usuario: $usuario")
        return withContext(Dispatchers.IO) {
            try {
                val sheetsManager = GoogleSheetsManager(requireContext().assets.open("credentials.json"), requireContext())
                val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .get(sheetsManager.spreadsheetIdPublic, "FICHAJE-OT-MES!A:G")
                    .execute()
                val values = response.getValues() ?: emptyList<List<Any>>()
                val hoy = SimpleDateFormat("d/M", Locale.getDefault()).format(Date())
                val ots = values.filter { it.size > 2 && it[2].toString() == usuario }
                    .mapNotNull { it.getOrNull(4)?.toString() }
                    .distinct()
                Log.d("FICHAJE-OT-MES", "[obtenerOTsUsuarioHoy] OTs encontradas: $ots")
                ots
            } catch (e: Exception) {
                Log.e("FICHAJE-OT-MES", "[obtenerOTsUsuarioHoy] Error: ${e.message}", e)
                emptyList()
            }
        }
    }

    private fun fichar(sheetsManager: GoogleSheetsManager, usuario: String, tipo: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FICHAJE", "Intentando fichar tipo: $tipo para usuario: $usuario")
                val ahora = Date()
                val hoy = dateFormat.format(ahora)
                val hora = timeFormat.format(ahora)
                val location = getLocationForFichaje()
                val (nombreLugar, enlaceMaps) = getLugarDesdeUbicacion(location)
                val prefs = requireContext().getSharedPreferences("bitherm_prefs", Context.MODE_PRIVATE)
                val usuarioLogin = prefs.getString("username", "") ?: ""
                val values = listOf(listOf(
                    usuarioLogin, // USUARIO (APP)
                    hoy,     // FECHA
                    hora,    // HORA
                    tipo,    // FICHAJE
                    "0",    // T (min)
                    enlaceMaps, // GPS
                    nombreLugar // ¿DONDE?
                ))
                sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .append(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:G", com.google.api.services.sheets.v4.model.ValueRange().setValues(values))
                    .setValueInputOption("RAW")
                    .execute()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Fichaje de $tipo registrado", Toast.LENGTH_SHORT).show()
                    Log.d("FICHAJE", "Fichaje de $tipo registrado correctamente")
                    if (tipo == "ENTRADA") {
                        Handler(Looper.getMainLooper()).postDelayed({
                            viewLifecycleOwner.lifecycleScope.launch {
                                actualizarEstadoFichaje(sheetsManager, usuarioLogin)
                            }
                        }, 2000)
                    } else {
                        viewLifecycleOwner.lifecycleScope.launch {
                            actualizarEstadoFichaje(sheetsManager, usuarioLogin)
                        }
                    }
                }
                if (tipo == "SALIDA") {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val ots = obtenerOTsUsuarioHoy(usuarioLogin)
                        Log.d("FICHAJE-OT-MES", "[obtenerOTsUsuarioHoy] OTs recibidas: $ots")
                        if (ots.isEmpty()) {
                            Toast.makeText(requireContext(), "No tienes OTs asignadas hoy", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        Log.d("FICHAJE-OT-MES", "Mostrando diálogo de selección de OTs")
                        val seleccionadas = mostrarDialogoSeleccionOTs(ots)
                        Log.d("FICHAJE-OT-MES", "[callback mostrarDialogoSeleccionOTs] Seleccionadas: $seleccionadas")
                        Toast.makeText(requireContext(), "Seleccionadas: $seleccionadas", Toast.LENGTH_SHORT).show()
                        if (seleccionadas.isEmpty()) {
                            Toast.makeText(requireContext(), "Debes seleccionar al menos una OT", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        Log.d("FICHAJE-OT-MES", "Antes de calcularHorasTrabajadasHoy para $usuarioLogin")
                        try {
                            val horasTrabajadas = calcularHorasTrabajadasHoy(usuarioLogin)
                            Log.d("FICHAJE-OT-MES", "Después de calcularHorasTrabajadasHoy: $horasTrabajadas")
                            if (seleccionadas.size == 1 && (seleccionadas[0] == "VACACIONES" || seleccionadas[0] == "BAJAMED")) {
                                withContext(Dispatchers.IO) {
                                    guardarDistribucionOTs(usuarioLogin, seleccionadas, 8f, 0f)
                                }
                            } else {
                                val horasNormales = minOf(horasTrabajadas, 8f)
                                val horasExtra = maxOf(horasTrabajadas - 8f, 0f)
                                Log.d("FICHAJE-OT-MES", "Mostrando diálogo de sliders de horas normales")
                                val distribucionNormales = mostrarDialogoSliders(seleccionadas, horasNormales, esExtra = false)
                                if (horasExtra > 0.1) {
                                    Log.d("FICHAJE-OT-MES", "Mostrando diálogo de sliders de horas extra")
                                    val distribucionExtra = mostrarDialogoSliders(seleccionadas, horasExtra, esExtra = true)
                                    Log.d("FICHAJE-OT-MES", "Antes de llamar a guardarDistribucionOTs (extra), normales: $distribucionNormales, extra: $distribucionExtra")
                                    Toast.makeText(requireContext(), "Llamando a guardarDistribucionOTs (extra)", Toast.LENGTH_SHORT).show()
                                    withContext(Dispatchers.IO) {
                                        guardarDistribucionOTs(usuarioLogin, seleccionadas, horasNormales, horasExtra, distribucionNormales, distribucionExtra)
                                    }
                                } else {
                                    withContext(Dispatchers.IO) {
                                        guardarDistribucionOTs(usuarioLogin, seleccionadas, horasNormales, 0f, distribucionNormales, emptyMap())
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FICHAJE-OT-MES", "Error en calcularHorasTrabajadasHoy: ", e)
                            Toast.makeText(requireContext(), "Error al calcular horas: "+e.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                logAutomaticoLocaleLogs(location, nombreLugar)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al fichar: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("FICHAJE", "Error al fichar: ${e.message}")
                }
            }
        }
    }

    private suspend fun getLocationForFichaje(): Location? = withContext(Dispatchers.Main) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val task = fusedLocationClient.lastLocation
            return@withContext kotlinx.coroutines.suspendCancellableCoroutine<Location?> { cont ->
                task.addOnSuccessListener { location ->
                    cont.resume(location, null)
                }
                task.addOnFailureListener {
                    cont.resume(null, null)
                }
            }
        } else null
    }

    private suspend fun getLugarDesdeUbicacion(location: Location?): Pair<String, String> = withContext(Dispatchers.IO) {
        if (location == null) return@withContext Pair("LUGAR DESCONOCIDO", "")
        try {
            val credentialsStream = requireContext().assets.open("credentials.json")
            val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
            val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                .get(sheetsManager.spreadsheetIdPublic, "EQUIPOS!E2:G")
                .execute()
            val values = response.getValues() ?: emptyList<List<Any>>()
            val lat = location.latitude
            val lon = location.longitude
            var mejorLugar = "LUGAR DESCONOCIDO"
            var mejorDistancia = Double.MAX_VALUE
            var tolerancia = 0.0
            for (row in values) {
                if (row.size >= 3) {
                    val nombreLugar = row[0].toString()
                    val coords = row[1].toString().split(",")
                    if (coords.size == 2) {
                        val latLugar = coords[0].trim().toDoubleOrNull() ?: continue
                        val lonLugar = coords[1].trim().toDoubleOrNull() ?: continue
                        val toleranciaLugar = row[2].toString().toDoubleOrNull() ?: continue
                        val distancia = calcularDistancia(lat, lon, latLugar, lonLugar)
                        Log.d("GPS-DEPURACION", "Lugar: $nombreLugar, Distancia: $distancia, Tolerancia: $toleranciaLugar")
                        if (distancia <= toleranciaLugar && distancia < mejorDistancia) {
                            mejorLugar = nombreLugar
                            mejorDistancia = distancia
                            tolerancia = toleranciaLugar
                        }
                    }
                }
            }
            val enlaceMaps = "https://www.google.com/maps?q=$lat,$lon"
            Log.d("GPS-DEPURACION", "Mejor lugar encontrado: $mejorLugar, distancia: $mejorDistancia, tolerancia: $tolerancia")
            return@withContext Pair(mejorLugar, enlaceMaps)
        } catch (e: Exception) {
            return@withContext Pair("LUGAR DESCONOCIDO", "")
        }
    }

    private fun calcularDistancia(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    private suspend fun calcularHorasTrabajadasHoy(usuario: String): Float {
        // Cálculo real de horas trabajadas hoy
        val sheetsManager = GoogleSheetsManager(requireContext().assets.open("credentials.json"), requireContext())
        val hoy = dateFormat.format(Date())
        var horas = 0f
        withContext(Dispatchers.IO) {
            val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                .get(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:H")
                .execute()
            val values = response.getValues() ?: emptyList<List<Any>>()
            val fichajesHoy = values.filter { it.size > 3 && it[0] == usuario && it[1] == hoy }
            val entrada = fichajesHoy.lastOrNull { it[3] == "ENTRADA" }
            val salida = fichajesHoy.lastOrNull { it[3] == "SALIDA" }
            if (entrada != null && salida != null) {
                try {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val t1 = sdf.parse("$hoy ${entrada[2]}")?.time ?: 0L
                    val t2 = sdf.parse("$hoy ${salida[2]}")?.time ?: 0L
                    val diff = t2 - t1
                    horas = diff / (1000f * 60f * 60f)
                } catch (e: Exception) { horas = 0f }
            }
        }
        return horas
    }

    private fun guardarDistribucionOTs(usuario: String, ots: List<String>, horasNormales: Float, horasExtra: Float, distribucionNormales: Map<String, Float> = emptyMap(), distribucionExtra: Map<String, Float> = emptyMap()) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FICHAJE-OT-MES", "INICIO guardarDistribucionOTs para $usuario, OTs: $ots, normales: $distribucionNormales, extra: $distribucionExtra")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Guardando distribución de OTs...", Toast.LENGTH_SHORT).show()
                }
                val sheetsManager = GoogleSheetsManager(requireContext().assets.open("credentials.json"), requireContext())
                val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .get(sheetsManager.spreadsheetIdPublic, "FICHAJE-OT-MES!A1:ZZ")
                    .execute()
                val values = response.getValues() ?: emptyList<List<Any>>()
                val header = values.firstOrNull() ?: emptyList<Any>()
                val hoy = SimpleDateFormat("d/M", Locale.getDefault()).format(Date())
                Log.d("FICHAJE-OT-MES", "[LOG FECHA] Hoy es: $hoy")
                Log.d("FICHAJE-OT-MES", "[LOG FECHA] Header completo: $header")
                val colFecha = header.indexOfFirst {
                    val headerFecha = it.toString().trim().lowercase().replace("^0", "")
                    val hoyNormalizado = hoy.trim().lowercase().replace("^0", "")
                    headerFecha == hoyNormalizado
                }
                Log.d("FICHAJE-OT-MES", "[LOG FECHA] Índice de columna encontrado para '$hoy': $colFecha")
                if (colFecha == -1) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No se encontró la columna del día en FICHAJE-OT-MES", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                // Buscar filas del usuario y OT
                for ((ot, horas) in distribucionNormales) {
                    val fila = values.indexOfFirst { it.size > 2 && it[2].toString() == usuario && it[4].toString() == ot }
                    Log.d("FICHAJE-OT-MES", "Buscando fila para usuario $usuario y OT $ot, índice: $fila")
                    if (fila != -1) {
                        val colExcel = indiceAColumnaExcel(colFecha)
                        val range = "FICHAJE-OT-MES!${colExcel}${fila + 1}"
                        Log.d("FICHAJE-OT-MES", "Guardando $horas horas normales en $range")
                        sheetsManager.sheetsServicePublic.spreadsheets().values()
                            .update(sheetsManager.spreadsheetIdPublic, range, com.google.api.services.sheets.v4.model.ValueRange().setValues(listOf(listOf(horas))))
                            .setValueInputOption("RAW")
                            .execute()
                    }
                }
                for ((ot, horas) in distribucionExtra) {
                    val fila = values.indexOfFirst { it.size > 2 && it[2].toString() == usuario && it[4].toString() == ot }
                    Log.d("FICHAJE-OT-MES", "Buscando fila para usuario $usuario y OT $ot (extra), índice: $fila")
                    if (fila != -1) {
                        val colExcel = indiceAColumnaExcel(colFecha + 1)
                        val range = "FICHAJE-OT-MES!${colExcel}${fila + 1}"
                        Log.d("FICHAJE-OT-MES", "Guardando $horas horas extra en $range")
                        sheetsManager.sheetsServicePublic.spreadsheets().values()
                            .update(sheetsManager.spreadsheetIdPublic, range, com.google.api.services.sheets.v4.model.ValueRange().setValues(listOf(listOf(horas))))
                            .setValueInputOption("RAW")
                            .execute()
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Distribución guardada en FICHAJE-OT-MES", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error guardando distribución: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun indiceAColumnaExcel(index: Int): String {
        var i = index
        var col = ""
        while (i >= 0) {
            col = ('A' + (i % 26)).toString() + col
            i = i / 26 - 1
        }
        return col
    }

    private fun logAutomaticoLocaleLogs(location: Location?, nombreLugar: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val credentialsStream = requireContext().assets.open("credentials.json")
                val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                val prefs = requireContext().getSharedPreferences("bitherm_prefs", Context.MODE_PRIVATE)
                val usuarioLogin = prefs.getString("username", "") ?: ""
                // Leer configuración de LOCALE-LOGS (A2:C)
                val configResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .get(sheetsManager.spreadsheetIdPublic, "LOCALE-LOGS!A2:C")
                    .execute()
                val configValues = configResponse.getValues() ?: emptyList<List<Any>>()
                val configRow = configValues.firstOrNull { it.isNotEmpty() && it[0].toString() == usuarioLogin }
                val modo = configRow?.getOrNull(1)?.toString()?.uppercase() ?: "NO"
                Log.d("LOCALE-LOGS", "Modo de logs para $usuarioLogin: $modo")
                if (modo == "NO") return@launch
                if (modo == "WK") {
                    // Solo si fichaje activo
                    val hoy = dateFormat.format(Date())
                    val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                        .get(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:H")
                        .execute()
                    val values = response.getValues() ?: emptyList<List<Any>>()
                    val fichajesHoy = values.filter { it.size > 3 && it[0] == usuarioLogin && it[1] == hoy }
                    val ultimaEntrada = fichajesHoy.lastOrNull { it[3] == "ENTRADA" }
                    val ultimaSalida = fichajesHoy.lastOrNull { it[3] == "SALIDA" }
                    if (ultimaEntrada == null || (ultimaSalida != null && fichajesHoy.indexOf(ultimaSalida) > fichajesHoy.indexOf(ultimaEntrada))) {
                        Log.d("LOCALE-LOGS", "No hay fichaje activo, no se envía log automático.")
                        return@launch
                    }
                }
                // Enviar log automático
                val now = Date()
                val enlaceMaps = if (location != null) "https://www.google.com/maps?q=${location.latitude},${location.longitude}" else ""
                val values = listOf(listOf(
                    usuarioLogin,
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(now),
                    nombreLugar,
                    enlaceMaps
                ))
                sheetsManager.sheetsServicePublic.spreadsheets().values()
                    .append(sheetsManager.spreadsheetIdPublic, "LOCALE-LOGS!A:D", com.google.api.services.sheets.v4.model.ValueRange().setValues(values))
                    .setValueInputOption("RAW")
                    .execute()
                Log.d("LOCALE-LOGS", "Log automático enviado para $usuarioLogin: $values")
            } catch (e: Exception) {
                Log.e("LOCALE-LOGS", "Error enviando log automático: ${e.message}", e)
            }
        }
    }

    private fun iniciarLogAutomatico() {
        detenerLogAutomatico()
        val prefs = requireContext().getSharedPreferences("bitherm_prefs", Context.MODE_PRIVATE)
        ultimaLatLogAuto = prefs.getString("ultimaLatLogAuto", null)?.toDoubleOrNull()
        ultimaLonLogAuto = prefs.getString("ultimaLonLogAuto", null)?.toDoubleOrNull()
        handlerLogAuto = Handler(Looper.getMainLooper())
        runnableLogAuto = object : Runnable {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val credentialsStream = requireContext().assets.open("credentials.json")
                        val sheetsManager = GoogleSheetsManager(credentialsStream, requireContext())
                        val prefs = requireContext().getSharedPreferences("bitherm_prefs", Context.MODE_PRIVATE)
                        val usuarioLogin = prefs.getString("username", "") ?: ""
                        // Leer configuración de LOCALE-LOGS (A2:C)
                        val configResponse = sheetsManager.sheetsServicePublic.spreadsheets().values()
                            .get(sheetsManager.spreadsheetIdPublic, "LOCALE-LOGS!A2:C")
                            .execute()
                        val configValues = configResponse.getValues() ?: emptyList<List<Any>>()
                        val configRow = configValues.firstOrNull { it.isNotEmpty() && it[0].toString() == usuarioLogin }
                        modoLogAuto = configRow?.getOrNull(1)?.toString()?.uppercase() ?: "NO"
                        intervaloLogAuto = (configRow?.getOrNull(2)?.toString()?.toLongOrNull() ?: 15L) * 60 * 1000L
                        Log.d("LOCALE-LOGS", "[AUTO] Modo: $modoLogAuto, Intervalo: $intervaloLogAuto ms")
                        if (modoLogAuto == "NO") {
                            logAutoActivo = false
                            return@launch
                        }
                        if (modoLogAuto == "WK") {
                            // Solo si fichaje activo
                            val hoy = dateFormat.format(Date())
                            val response = sheetsManager.sheetsServicePublic.spreadsheets().values()
                                .get(sheetsManager.spreadsheetIdPublic, "$FICHAJE_LOGS_SHEET!A:H")
                                .execute()
                            val values = response.getValues() ?: emptyList<List<Any>>()
                            val fichajesHoy = values.filter { it.size > 3 && it[0] == usuarioLogin && it[1] == hoy }
                            val ultimaEntrada = fichajesHoy.lastOrNull { it[3] == "ENTRADA" }
                            val ultimaSalida = fichajesHoy.lastOrNull { it[3] == "SALIDA" }
                            if (ultimaEntrada == null || (ultimaSalida != null && fichajesHoy.indexOf(ultimaSalida) > fichajesHoy.indexOf(ultimaEntrada))) {
                                logAutoActivo = false
                                Log.d("LOCALE-LOGS", "[AUTO] No hay fichaje activo, no se envía log automático.")
                                return@launch
                            }
                        }
                        // Obtener última localización
                        withContext(Dispatchers.Main) {
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                                    if (location != null) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            val latAnterior = ultimaLatLogAuto
                                            val lonAnterior = ultimaLonLogAuto
                                            val latActual = location.latitude
                                            val lonActual = location.longitude
                                            val distanciaMin = if (latAnterior != null && lonAnterior != null) calcularDistancia(latAnterior, lonAnterior, latActual, lonActual) else 1000.0
                                            if (distanciaMin > 15.0) {
                                                val (nombreLugar, _) = getLugarDesdeUbicacion(location)
                                                logAutomaticoLocaleLogs(location, nombreLugar)
                                                ultimaLatLogAuto = latActual
                                                ultimaLonLogAuto = lonActual
                                                // Guardar en preferencias
                                                prefs.edit().putString("ultimaLatLogAuto", latActual.toString()).putString("ultimaLonLogAuto", lonActual.toString()).apply()
                                            } else {
                                                Log.d("LOCALE-LOGS", "[AUTO] No se envía log, distancia a última ubicación: $distanciaMin m")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        logAutoActivo = true
                    } catch (e: Exception) {
                        Log.e("LOCALE-LOGS", "[AUTO] Error en log automático: ${e.message}", e)
                        logAutoActivo = false
                    }
                }
                handlerLogAuto?.postDelayed(this, intervaloLogAuto)
            }
        }
        handlerLogAuto?.post(runnableLogAuto!!)
    }

    private fun detenerLogAutomatico() {
        handlerLogAuto?.removeCallbacksAndMessages(null)
        handlerLogAuto = null
        runnableLogAuto = null
        logAutoActivo = false
    }

    override fun onResume() {
        super.onResume()
        iniciarLogAutomatico()
    }

    override fun onPause() {
        super.onPause()
        detenerLogAutomatico()
    }
} 