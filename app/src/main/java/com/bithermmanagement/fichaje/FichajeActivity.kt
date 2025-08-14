package com.bithermmanagement.fichaje

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bithermmanagement.core.base.DebugBaseActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.R
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.fichaje.models.FichajeEntity
import com.bithermmanagement.fichaje.adapters.OTDistributionAdapter
import com.bithermmanagement.fichaje.adapters.OTSelectionAdapter
import com.bithermmanagement.fichaje.models.OTDistributionItem
import com.bithermmanagement.fichaje.models.OTSelectionItem
import com.bithermmanagement.fichaje.renderers.CustomPieChartRenderer
import com.bithermmanagement.fichaje.services.GoogleSheetsService
import com.bithermmanagement.fichaje.utils.Constants
import com.bithermmanagement.fichaje.utils.LocationUtils
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FichajeActivity : DebugBaseActivity() {
    
    // UI Components
    private lateinit var workerNameTextView: TextView
    private lateinit var currentDateTextView: TextView
    private lateinit var assignedOTsTextView: TextView
    private lateinit var specificOTsTextView: TextView
    private lateinit var btnCheckIn: MaterialButton
    private lateinit var btnCheckOut: MaterialButton
    private lateinit var timerTextView: TextView
    private lateinit var hoursDistributionCard: View
    private lateinit var totalHoursTextView: TextView
    private lateinit var otDistributionRecyclerView: RecyclerView
    private lateinit var btnSaveDistribution: MaterialButton
    private lateinit var timeChart: PieChart

    // Data
    private var selectedSpreadsheetId: String = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
    private val FICHAJE_LOGS_SHEET = "FICHAJE-LOGS"
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("d/M", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private lateinit var usuario_login: String
    
    // Timer
    private var timer: CountDownTimer? = null
    private var startTime: Long = 0
    private var elapsedTime: Long = 0
    private var isTimerRunning = false
    
    // Services
    private lateinit var sheetsService: GoogleSheetsService
    private lateinit var database: AppDatabase
    private lateinit var otDistributionAdapter: OTDistributionAdapter
    private var selectedOTs = mutableListOf<String>()
    private var assignedOTs = mutableListOf<OTSelectionItem>()
    private val otList = mutableListOf<OTDistributionItem>()

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "FichajeActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fichaje)
        
        initializeComponents()
        setupViews()
        setupRecyclerView()
        setupButtons()
        loadWorkerData()
        updateCurrentDate()
        checkLastCheckInStatus()
        requestLocationPermission()
    }

    private fun initializeComponents() {
        val sharedPreferences = getSharedPreferences(Constants.SHARED_PREFS_NAME, MODE_PRIVATE)
        usuario_login = sharedPreferences.getString(Constants.PREF_USERNAME, "") ?: ""
        
        sheetsService = GoogleSheetsService(this)
        database = AppDatabase.getDatabase(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupViews() {
        workerNameTextView = findViewById(R.id.workerNameTextView)
        currentDateTextView = findViewById(R.id.currentDateTextView)
        assignedOTsTextView = findViewById(R.id.assignedOTsTextView)
        specificOTsTextView = findViewById(R.id.specificOTsTextView)
        btnCheckIn = findViewById(R.id.btnCheckIn)
        btnCheckOut = findViewById(R.id.btnCheckOut)
        timerTextView = findViewById(R.id.timerTextView)
        hoursDistributionCard = findViewById(R.id.hoursDistributionCard)
        totalHoursTextView = findViewById(R.id.totalHoursTextView)
        otDistributionRecyclerView = findViewById(R.id.otDistributionRecyclerView)
        btnSaveDistribution = findViewById(R.id.btnSaveDistribution)
        timeChart = findViewById(R.id.timeChart)
        
        setupTimeChart()
    }

    private fun setupTimeChart() {
        timeChart.apply {
            description.isEnabled = false
            isRotationEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(true)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(16f)
            setHoleColor(Color.WHITE)
            setHoleRadius(75f)
            setDrawHoleEnabled(true)
            setDrawCenterText(false)
            setDrawRoundedSlices(false)
            setDrawSlicesUnderHole(false)
            setUsePercentValues(false)
            setMaxAngle(360f)
            setRotationAngle(120f)
            setHighlightPerTapEnabled(false)
            minOffset = 0f
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // Primera sección (8 horas) - Verde (Horario normal)
        repeat(8) {
            entries.add(PieEntry(1f, "${it}h"))
            colors.add(Color.parseColor("#FFFFFF")) // Fondo blanco
        }

        // Segunda sección (1 hora) - Amarillo (Horario comida)
        entries.add(PieEntry(1f, "8h"))
        colors.add(Color.parseColor("#FFFFFF"))

        // Tercera sección (3 horas) - Naranjo (Horario extra)
        repeat(3) {
            entries.add(PieEntry(1f, "${it + 9}h"))
            colors.add(Color.parseColor("#FFFFFF"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(true)
            sliceSpace = 0f
            valueTextColor = Color.BLACK
            valueTextSize = 16f
            setDrawIcons(false)
            setSelectionShift(0f)
            setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
            setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return ""
                }
            }
        }

        timeChart.data = PieData(dataSet)
        updateChartHand(0f)
    }

    private fun updateChartHand(elapsedHours: Float) {
        timeChart.apply {
            val handColor = Color.BLACK
            renderer = CustomPieChartRenderer(
                this,
                animator,
                viewPortHandler,
                handColor,
                elapsedHours
            )
            invalidate()
        }
    }

    private fun updateTimerDisplay() {
        val hours = elapsedTime / (1000 * 60 * 60)
        val minutes = (elapsedTime % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (elapsedTime % (1000 * 60)) / 1000
        
        timerTextView.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        
        // Actualizar la posición de la aguja
        updateChartHand(hours + minutes / 60f)
    }

    private fun startTimer() {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime = System.currentTimeMillis() - startTime
                updateTimerDisplay()
            }
            override fun onFinish() {}
        }.start()
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLastKnownLocation()
        }
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    lastKnownLocation = location
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error getting location", e)
        }
    }

    private fun setupButtons() {
        btnCheckIn.setOnClickListener {
            startCheckIn()
        }
        
        btnCheckOut.setOnClickListener {
            startCheckOut()
        }
        
        btnSaveDistribution.setOnClickListener {
            saveDistribution(otList)
        }
    }

    private fun setupRecyclerView() {
        otDistributionAdapter = OTDistributionAdapter(otList) { item, hours ->
            item.hours = hours
            updateTotalHours()
        }
        
        otDistributionRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@FichajeActivity)
            adapter = otDistributionAdapter
        }
    }

    private fun updateTotalHours() {
        val totalHours = otList.sumOf { it.hours.toDouble() }
        totalHoursTextView.text = String.format("Horas totales: %.1f", totalHours)
    }

    private fun checkLastCheckInStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Primero verificar en la base de datos local
                val lastFichaje = database.fichajeDao().getLastFichajeByUser(usuario_login)
                
                withContext(Dispatchers.Main) {
                    when {
                        lastFichaje == null -> {
                            btnCheckIn.visibility = View.VISIBLE
                            btnCheckOut.visibility = View.GONE
                            isTimerRunning = false
                        }
                        lastFichaje.tipo == "ENTRADA" -> {
                            btnCheckIn.visibility = View.GONE
                            btnCheckOut.visibility = View.VISIBLE
                            // Recuperar la hora de entrada
                            val entryDate = dateTimeFormat.parse(lastFichaje.fecha + " " + lastFichaje.hora)
                            entryDate?.let {
                                startTime = it.time
                                elapsedTime = System.currentTimeMillis() - startTime
                                isTimerRunning = true
                                startTimer()
                            }
                        }
                        else -> {
                            btnCheckIn.visibility = View.VISIBLE
                            btnCheckOut.visibility = View.GONE
                            isTimerRunning = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking last check-in status", e)
                withContext(Dispatchers.Main) {
                    btnCheckIn.visibility = View.VISIBLE
                    btnCheckOut.visibility = View.GONE
                }
            }
        }
    }

    private fun startCheckIn() {
        if (!isTimerRunning) {
            startTime = System.currentTimeMillis()
            startTimer()
            isTimerRunning = true
            btnCheckIn.visibility = View.GONE
            btnCheckOut.visibility = View.VISIBLE
            hoursDistributionCard.visibility = View.GONE
            
            saveCheckInTime()
        }
    }

    private fun startCheckOut() {
        if (isTimerRunning) {
            timer?.cancel()
            isTimerRunning = false
            btnCheckIn.visibility = View.VISIBLE
            btnCheckOut.visibility = View.GONE
            
            showOTSelectionDialog()
        }
    }

    private fun showOTSelectionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_ot_selection)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.otSelectionRecyclerView)
        val btnConfirm = dialog.findViewById<MaterialButton>(R.id.btnConfirmOTs)
        val totalHoursText = dialog.findViewById<TextView>(R.id.totalHoursText)
        
        val adapter = OTSelectionAdapter(assignedOTs) { item ->
            if (item.isSelected) {
                if (item.isSpecial) { // Solo VACACIONES y BAJAMED son especiales
                    // Si se selecciona una OT especial, deseleccionar todas las demás
                    selectedOTs.clear()
                    selectedOTs.add(item.id)
                    // Establecer 8 horas automáticamente
                    showHoursDistributionDialog(listOf(item.id))
                    dialog.dismiss()
                } else {
                    selectedOTs.add(item.id)
                }
            } else {
                selectedOTs.remove(item.id)
            }
            // Habilitar el botón si hay OTs seleccionadas
            btnConfirm.isEnabled = selectedOTs.isNotEmpty()
            recyclerView.post {
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnConfirm.setOnClickListener {
            if (selectedOTs.isNotEmpty()) {
                showHoursDistributionDialog(selectedOTs)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Debes seleccionar al menos una OT", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showHoursDistributionDialog(selectedOTs: List<String>) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Distribuye las horas trabajadas")
            .create()

        val view = layoutInflater.inflate(R.layout.dialog_hours_distribution, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.hoursDistributionRecyclerView)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmHours)
        val totalHoursText = view.findViewById<TextView>(R.id.totalHoursText)

        val totalHoursWorked = (elapsedTime / 3600000f).coerceAtMost(8f)
        val distributionItems = selectedOTs.map { OTDistributionItem(title = it) }
        
        // Si solo hay una OT seleccionada
        if (distributionItems.size == 1) {
            val item = distributionItems[0]
            // Si es VACACIONES o PERM.RET, asignar 8 horas
            if (item.title == "VACACIONES" || item.title == "PERM.RET") {
                item.hours = 8f
            } else {
                item.hours = totalHoursWorked
            }
        } else {
            // Distribuir las horas equitativamente
            val hoursPerOT = totalHoursWorked / distributionItems.size
            distributionItems.forEach { it.hours = hoursPerOT }
        }

        val adapter = OTDistributionAdapter(distributionItems) { item, hours ->
            item.hours = hours
            val total = distributionItems.sumOf { it.hours.toDouble() }
            totalHoursText.text = String.format("Total: %.1f horas", total)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnConfirm.setOnClickListener {
            saveCheckOutTime(distributionItems)
            dialog.dismiss()
        }

        dialog.setView(view)
        dialog.show()
    }

    private fun saveCheckInTime() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val now = Date()
                val locationName = LocationUtils.findNearestLocation(
                    lastKnownLocation?.latitude ?: 0.0,
                    lastKnownLocation?.longitude ?: 0.0
                )
                val gpsLink = LocationUtils.createGoogleMapsLink(
                    lastKnownLocation?.latitude ?: 0.0,
                    lastKnownLocation?.longitude ?: 0.0
                )
                
                // Guardar en base de datos local
                val fichaje = FichajeEntity(
                    usuario = usuario_login,
                    fecha = shortDateFormat.format(now),
                    hora = timeFormat.format(now),
                    tipo = "ENTRADA",
                    tiempoMinutos = 0,
                    gpsLink = gpsLink,
                    ubicacion = locationName,
                    timestamp = now.time
                )
                
                database.fichajeDao().insertFichaje(fichaje)
                
                // Sincronizar con Google Sheets
                val values = listOf(listOf(
                    usuario_login,                    // USUARIO
                    shortDateFormat.format(now),      // FECHA CORTA
                    timeFormat.format(now),           // HORA HH:MM
                    "ENTRADA",                        // FICHAJE
                    "0",                             // T (min)
                    gpsLink,                         // GPS (enlace maps)
                    locationName                      // NOMBRE DEL LUGAR
                ))
                
                sheetsService.appendValues(selectedSpreadsheetId, FICHAJE_LOGS_SHEET + "!A:G", values)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Entrada registrada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Error al registrar entrada: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveCheckOutTime(distributionItems: List<OTDistributionItem>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val now = Date()
                val elapsedMinutes = (elapsedTime / 60000).toInt() // Convertir a minutos
                val locationName = LocationUtils.findNearestLocation(
                    lastKnownLocation?.latitude ?: 0.0,
                    lastKnownLocation?.longitude ?: 0.0
                )
                val gpsLink = LocationUtils.createGoogleMapsLink(
                    lastKnownLocation?.latitude ?: 0.0,
                    lastKnownLocation?.longitude ?: 0.0
                )
                
                // Guardar en base de datos local
                val fichaje = FichajeEntity(
                    usuario = usuario_login,
                    fecha = shortDateFormat.format(now),
                    hora = timeFormat.format(now),
                    tipo = "SALIDA",
                    tiempoMinutos = elapsedMinutes,
                    gpsLink = gpsLink,
                    ubicacion = locationName,
                    timestamp = now.time
                )
                
                database.fichajeDao().insertFichaje(fichaje)
                
                // Sincronizar con Google Sheets
                val values = listOf(listOf(
                    usuario_login,                    // USUARIO
                    shortDateFormat.format(now),      // FECHA CORTA
                    timeFormat.format(now),           // HORA HH:MM
                    "SALIDA",                        // FICHAJE
                    elapsedMinutes.toString(),       // T (min)
                    gpsLink,                         // GPS (enlace maps)
                    locationName                      // NOMBRE DEL LUGAR
                ))
                
                sheetsService.appendValues(selectedSpreadsheetId, FICHAJE_LOGS_SHEET + "!A:G", values)
                
                // Guardar distribución de horas
                saveDistribution(distributionItems)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Salida registrada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Error al registrar salida: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun saveDistribution(distributionItems: List<OTDistributionItem>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val now = Date()
                val currentMonth = monthYearFormat.format(now)
                
                // Guardar distribución en Google Sheets
                distributionItems.forEach { item ->
                    val values = listOf(listOf(
                        usuario_login,                    // USUARIO
                        shortDateFormat.format(now),      // FECHA
                        currentMonth,                     // MES/AÑO
                        item.title,                       // OT
                        item.hours.toString()             // HORAS
                    ))
                    
                    sheetsService.appendValues(selectedSpreadsheetId, "FICHAJE-OT-MES!A:E", values)
                }
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Distribución guardada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Error al guardar distribución: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadWorkerData() {
        workerNameTextView.text = "Trabajador: $usuario_login"
        loadAssignedOTs()
    }

    private fun loadAssignedOTs() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val range = "FICHAJE-OT-MES!A2:E"
                val response = sheetsService.sheetsService.spreadsheets().values()
                    .get(selectedSpreadsheetId, range)
                    .execute()
                val values = response.getValues() ?: emptyList<List<Any>>()
                
                val specialOTs = mutableListOf<OTSelectionItem>() // VACACIONES, BAJAMED
                val administrativeOTs = mutableListOf<OTSelectionItem>() // PERM.RET, DELEGACION
                val workOTs = mutableListOf<OTSelectionItem>() // OTs de trabajo (comunes y específicas)
                val exclusiveOTs = listOf("VACACIONES", "BAJAMED")
                
                values.forEach { row ->
                    if (row.size > 4 && row[2].toString().trim().equals(usuario_login, ignoreCase = true)) {
                        val ot = row[4].toString().trim()
                        val isCommon = row[3].toString().trim().equals("COMUN", ignoreCase = true)
                        
                        when (ot.uppercase()) {
                            in exclusiveOTs -> {
                                specialOTs.add(OTSelectionItem(id = ot, name = ot, isSpecial = true))
                            }
                            "PERM.RET", "DELEGACION" -> {
                                administrativeOTs.add(OTSelectionItem(id = ot, name = ot, isAdministrative = true))
                            }
                            else -> {
                                workOTs.add(OTSelectionItem(id = ot, name = ot, isCommon = isCommon))
                            }
                        }
                    }
                }
                
                withContext(Dispatchers.Main) {
                    // Actualizar la vista con las OTs específicas
                    val specificOTsText = workOTs.filter { !it.isCommon }
                        .let { specificOTs ->
                            if (specificOTs.isEmpty()) "No hay OTs específicas asignadas"
                            else specificOTs.joinToString("\n• ", "• ") { it.name }
                        }
                    specificOTsTextView.text = specificOTsText

                    // Actualizar la vista con las OTs comunes
                    val commonOTsText = workOTs.filter { it.isCommon }
                        .let { commonOTs ->
                            if (commonOTs.isEmpty()) "No hay OTs comunes asignadas"
                            else commonOTs.joinToString("\n• ", "• ") { it.name }
                        }
                    assignedOTsTextView.text = commonOTsText
                    assignedOTsTextView.visibility = View.VISIBLE

                    // Guardar todas las OTs para el diálogo de selección
                    assignedOTs.clear()
                    assignedOTs.addAll(specialOTs) // Primero las especiales
                    assignedOTs.addAll(administrativeOTs) // Luego las administrativas
                    assignedOTs.addAll(workOTs.filter { !it.isCommon }) // Luego las específicas
                    assignedOTs.addAll(workOTs.filter { it.isCommon }) // Finalmente las comunes
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FichajeActivity,
                        "Error al cargar OTs: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateCurrentDate() {
        val currentDate = Calendar.getInstance().time
        currentDateTextView.text = "Fecha: ${dateOnlyFormat.format(currentDate)}"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastKnownLocation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
} 