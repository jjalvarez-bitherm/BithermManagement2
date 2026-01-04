package com.bithermmanagement.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.contract.ActivityResultContracts
import com.bithermmanagement.core.base.DebugBaseActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.commit
import com.bithermmanagement.R
import com.bithermmanagement.data.UserData
import com.bithermmanagement.ui.favoritos.FragmentFavoritos
import com.bithermmanagement.ui.work.WorkFragment
import com.bithermmanagement.data.SettingsManager
import com.bithermmanagement.ui.fragments.WorkCameraFragment
import com.bithermmanagement.ui.gallery.CustomGalleryView

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.content.Intent
import com.bithermmanagement.core.utils.DebugConfigManager
import com.bithermmanagement.database.AppDatabase
import android.widget.Switch
import android.widget.TextView
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import android.app.Dialog
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.LinearLayout
import android.widget.EditText
import com.bithermmanagement.chat.fragments.ChatGroupFragment
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import android.view.Gravity


@AndroidEntryPoint
class MainMenuActivity : DebugBaseActivity() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var settingsManager: SettingsManager
    private var bestLocation: Location? = null
    private var bestPrecision: Float = Float.MAX_VALUE
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 30000 // 30 segundos
    private var currentSubAppDialog: Dialog? = null // Para controlar window leaks

	// Launcher para cámara de WorkCameraFragment
	private val workCameraLauncher = registerForActivityResult(
		ActivityResultContracts.TakePicture()
	) { success ->
		Log.d("MainMenuActivity", "Camera result: $success")
		// Crear una instancia temporal si la original no está disponible
		WorkCameraFragment.instance?.let { fragment ->
			Log.d("MainMenuActivity", "Llamando handleCameraResult en fragment")
			fragment.handleCameraResult(this, success)
		} ?: run {
			Log.w("MainMenuActivity", "WorkCameraFragment.instance es null, creando instancia temporal")
			// Crear instancia temporal para manejar el resultado
			val tempFragment = WorkCameraFragment()
			tempFragment.settingsManager = SettingsManager(this)
			tempFragment.handleCameraResult(this, success)
		}
	}


    object GpsProvider {
        var lastLocation: Location? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainMenuActivity", "onCreate llamado")
        setContentView(R.layout.activity_main_menu)
        
        // Inicializar SettingsManager
        settingsManager = SettingsManager(this)

        // Obtener datos del usuario
        val userData = intent.getParcelableExtra<UserData>("USER_DATA")
        Log.d("MainMenuActivity", "userData recibido: $userData")
        
        // Crear canal de notificaciones para chat
        com.bithermmanagement.chat.services.ChatNotificationService.createNotificationChannel(this)

        // Configurar barra superior
        val tvWorkerName = findViewById<TextView>(R.id.tv_worker_name)
        if (userData != null) {
            tvWorkerName.text = "${userData.app} (${userData.rol})"
        } else {
            tvWorkerName.text = "Usuario"
        }

        val btnHome = findViewById<ImageButton>(R.id.btn_home)
        btnHome.setOnClickListener {
            Log.d("MainMenuActivity", "btnHome pulsado")
            supportFragmentManager.commit {
                Log.d("MainMenuActivity", "Cargando FragmentMainMenu desde btnHome")
                replace(R.id.fragment_container, FragmentMainMenu())
            }
        }

        val btnFavoritos = findViewById<ImageButton>(R.id.btn_favoritos)
        btnFavoritos.setOnClickListener {
            Log.d("MainMenuActivity", "btnFavoritos pulsado")
            supportFragmentManager.commit {
                Log.d("MainMenuActivity", "Cargando FragmentFavoritos desde btnFavoritos")
                replace(R.id.fragment_container, FragmentFavoritos())
            }
        }

        val btnLogout = findViewById<ImageButton>(R.id.btn_logout)
        btnLogout.setOnClickListener {
            Log.d("MainMenuActivity", "btnLogout pulsado")
            // Limpiar credenciales guardadas
            val prefs = getSharedPreferences("bitherm_prefs", MODE_PRIVATE)
            prefs.edit()
                .remove("saved_user")
                .remove("saved_pass")
                .putBoolean("remember", false)
                .apply()
            
            // Redirigir al login
            val intent = Intent(this, com.bithermmanagement.ui.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Configurar barra inferior
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            Log.d("MainMenuActivity", "BottomNav item seleccionado: ${item.itemId}")
            when (item.itemId) {
                R.id.nav_persona -> {
                    // Icono 1: Mi equipo - Mostrar popup modal
                    showSubAppPopup("Mi Equipo", R.layout.fragment_mi_equipo)
                    true
                }
                R.id.nav_mensajes -> {
                    // Icono 2: BithermChat - Mostrar popup modal
                    showSubAppPopup("BithermChat", R.layout.fragment_bitherm_chat)
                    true
                }
                R.id.nav_tareas -> {
                    // Icono 3: Tareas (centro) - Cambiar fragment principal
                    // Crear un fragment simple con el layout
                    val fragment = Fragment().apply {
                        setArguments(Bundle().apply {
                            putInt("layout_id", R.layout.fragment_tareas_principal)
                        })
                    }
                    
                    supportFragmentManager.commit {
                        Log.d("MainMenuActivity", "Cargando layout de tareas desde bottomNav")
                        replace(R.id.fragment_container, fragment)
                    }
                    true
                }
				
				
				R.id.nav_camara -> {
					// Icono 4: Usar nombre configurado de cámara - Mostrar popup modal
					val settings = settingsManager.getSettings()
					// Configurar el launcher antes de mostrar el diálogo
					WorkCameraFragment.setCameraLauncher(workCameraLauncher)
					showSubAppPopup(settings.cameraAppName, R.layout.fragment_work_camera)
					true
				}




                R.id.nav_notas -> {
                    // Icono 5: Mis tareas - Mostrar popup modal
                    showSubAppPopup("Mis Tareas", R.layout.fragment_mis_tareas)
                    true
                }
                else -> {
                    supportFragmentManager.commit {
                        Log.d("MainMenuActivity", "Cargando FragmentMainMenu desde bottomNav")
                        replace(R.id.fragment_container, FragmentMainMenu())
                    }
                    true
                }
            }
        }

        // Cargar el fragment principal al iniciar
        if (savedInstanceState == null) {
            Log.d("MainMenuActivity", "Cargando FragmentMainMenu inicial")
            supportFragmentManager.commit {
                replace(R.id.fragment_container, FragmentMainMenu())
            }
        }

        // Inicializar GPS manual (icono)
        initializeGps()
        
        // Inicializar GPS automático (segundo plano)
        initializeAutomaticGps()
        
        // Inicializar sincronización automática
        initializeAutomaticSync()
    }

    private fun initializeGps() {
        // Localización global
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    if (location.accuracy < bestPrecision) {
                        bestLocation = location
                        bestPrecision = location.accuracy
                    }
                    GpsProvider.lastLocation = location
                }
            }
        }
        startLocationUpdates()
        scheduleGpsUiUpdate()

        // Configurar icono GPS
        val iconGps = findViewById<ImageView>(R.id.icon_gps)
        iconGps.setOnClickListener {
            showGpsDebugDialog()
        }
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        val request = LocationRequest.create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun scheduleGpsUiUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                // Aquí podrías actualizar un log o hacer algo cada 30s
                handler.postDelayed(this, updateInterval)
            }
        }, updateInterval)
    }

    private fun initializeAutomaticGps() {
        // Verificar si el usuario tiene permisos de ubicación
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Habilitar el tracking automático
            Log.d("MainMenuActivity", "GPS automático activado")
        } else {
            Log.d("MainMenuActivity", "Sin permisos de ubicación para GPS automático")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
            // También activar GPS automático cuando se concedan permisos
            initializeAutomaticGps()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainMenuActivity", "onPause llamado")
        
        // Cerrar diálogo si está abierto para evitar window leak
        currentSubAppDialog?.let { dialog ->
            if (dialog.isShowing) {
                Log.d("MainMenuActivity", "Cerrando diálogo en onPause")
                dialog.dismiss()
            }
        }
        currentSubAppDialog = null
    }

    private fun showGpsDebugDialog() {
        // Crear el layout personalizado
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gps_debug, null)
        
        // Configurar información GPS
        val tvGpsInfo = dialogView.findViewById<TextView>(R.id.tv_gps_info)
        val loc = bestLocation
        val gpsText = if (loc != null) {
            "📍 GPS Actual\n\nCoordenadas:\n${loc.latitude}, ${loc.longitude} (±${"%.1f".format(loc.accuracy)}m)"
        } else {
            "📍 GPS Actual\n\nNo hay localización disponible aún."
        }
        tvGpsInfo.text = gpsText
        
        // Configurar switch de debug
        val switchDebug = dialogView.findViewById<Switch>(R.id.switch_debug)
        switchDebug.isChecked = DebugConfigManager.isDebugModeEnabled(this)
        
        switchDebug.setOnCheckedChangeListener { _, isChecked ->
            DebugConfigManager.setDebugMode(this, isChecked)
            val message = if (isChecked) "🐛 Debug Mode ENABLED" else "🐛 Debug Mode DISABLED"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
            // Actualizar el debug observer
            val debugObserver = com.bithermmanagement.core.utils.DebugLifecycleObserver.getInstance(this)
            debugObserver.updateDebugMode()
        }
        
        // Obtener estado actual de sincronización
        val prefs = getSharedPreferences("bitherm_prefs", MODE_PRIVATE)
        
        // Configurar switch de sincronización
        val switchSincronizacion = dialogView.findViewById<Switch>(R.id.switch_sincronizacion)
        val btnSincronizar = dialogView.findViewById<Button>(R.id.btn_sincronizar)
        val btnLimpiarColores = dialogView.findViewById<Button>(R.id.btn_limpiar_colores)
        val layoutUmbralSync = dialogView.findViewById<LinearLayout>(R.id.layout_umbral_sync)
        val etUmbralSync = dialogView.findViewById<EditText>(R.id.et_umbral_sync)
        
        // Configurar switch de debug de escritura
        val switchDebugEscritura = dialogView.findViewById<Switch>(R.id.switch_debug_escritura)
        val debugEscrituraEnabled = prefs.getBoolean("debug_escritura_enabled", false)
        switchDebugEscritura.isChecked = debugEscrituraEnabled
        
        switchDebugEscritura.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("debug_escritura_enabled", isChecked).apply()
            btnLimpiarColores.isEnabled = isChecked
            val message = if (isChecked) {
                "✏️ Debug de escritura ACTIVADO - Las celdas modificadas se colorearán"
            } else {
                "✏️ Debug de escritura DESACTIVADO"
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        val sincronizacionOnline = prefs.getBoolean("sincronizacion_online", false)
        val umbralSync = prefs.getInt("umbral_sincronizacion", 10)
        
        switchSincronizacion.isChecked = sincronizacionOnline
        etUmbralSync.setText(umbralSync.toString())
        
        // Mostrar/ocultar configuración de umbral según el modo
        layoutUmbralSync.visibility = if (sincronizacionOnline) View.VISIBLE else View.GONE
        
        // Configurar listener del switch de sincronización
        switchSincronizacion.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sincronizacion_online", isChecked).apply()
            btnSincronizar.visibility = if (isChecked) View.GONE else View.VISIBLE
            layoutUmbralSync.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            val message = if (isChecked) {
                "🔄 Sincronización ONLINE activada"
            } else {
                "🔄 Sincronización OFFLINE activada"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Configurar botón sincronizar
        btnSincronizar.visibility = if (sincronizacionOnline) View.GONE else View.VISIBLE
        btnSincronizar.setOnClickListener {
            // Implementar sincronización manual
            sincronizarEquiposModificados()
            Toast.makeText(this, "🔄 Sincronizando equipos modificados...", Toast.LENGTH_SHORT).show()
        }
        
        // Configurar botón limpiar colores debug
        btnLimpiarColores.isEnabled = debugEscrituraEnabled
        btnLimpiarColores.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val sheetsManager = obtenerGoogleSheetsManager()
                    if (sheetsManager != null) {
                        // Obtener el ID del spreadsheet actual (puedes cambiarlo según tu configuración)
                        val spreadsheetId = "1ATAixbvK1vVGWIxujQj0g7syscgYEnHF0FI3MXibCow" // ID de ejemplo
                        val resultado = sheetsManager.limpiarColoresDebug(spreadsheetId)
                        
                        if (resultado) {
                            Toast.makeText(this@MainMenuActivity, "🧹 Colores de debug limpiados exitosamente", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainMenuActivity, "❌ Error limpiando colores de debug", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainMenuActivity, "❌ Error: No se pudo conectar a Google Sheets", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainMenuActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        // Crear y mostrar el diálogo
        AlertDialog.Builder(this)
            .setTitle("Configuración")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                // Guardar el umbral de sincronización
                try {
                    val nuevoUmbral = etUmbralSync.text.toString().toInt()
                    if (nuevoUmbral > 0) {
                        prefs.edit().putInt("umbral_sincronizacion", nuevoUmbral).apply()
                        Log.d("MainMenuActivity", "Umbral de sincronización actualizado a: $nuevoUmbral")
                    } else {
                        Toast.makeText(this, "⚠️ El umbral debe ser mayor que 0", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "⚠️ Valor inválido para el umbral", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Revertir el estado de los switches si se cancela
                switchDebug.isChecked = DebugConfigManager.isDebugModeEnabled(this)
                switchSincronizacion.isChecked = prefs.getBoolean("sincronizacion_online", false)
                dialog.dismiss()
            }
            .show()
    }

    private fun sincronizarEquiposModificados() {
        lifecycleScope.launch {
            try {
                val db = AppDatabase.getDatabase(this@MainMenuActivity)
                val inspeccionDao = db.inspeccionDao()
                val equiposModificados = inspeccionDao.getModificadasLocal()
                
                if (equiposModificados.isEmpty()) {
                    Toast.makeText(this@MainMenuActivity, "No hay equipos modificados para sincronizar", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                Log.d("MainMenuActivity", "Iniciando sincronización de ${equiposModificados.size} equipos modificados")
                
                // Obtener GoogleSheetsManager
                val sheetsManager = obtenerGoogleSheetsManager()
                if (sheetsManager == null) {
                    Toast.makeText(this@MainMenuActivity, "❌ Error de configuración. Verifica las credenciales.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                // Sincronizar con Google Sheets
                val resultado = sheetsManager.sincronizarEquiposModificados(equiposModificados)
                
                if (resultado) {
                    // Marcar como sincronizados solo si la sincronización fue exitosa
                    equiposModificados.forEach { equipo ->
                        inspeccionDao.setModificadoLocal(equipo.id, false)
                    }
                    
                    Toast.makeText(this@MainMenuActivity, "✅ ${equiposModificados.size} equipos sincronizados exitosamente", Toast.LENGTH_SHORT).show()
                    Log.d("MainMenuActivity", "Sincronización completada exitosamente")
                } else {
                    Toast.makeText(this@MainMenuActivity, "❌ Error al sincronizar con Google Sheets", Toast.LENGTH_LONG).show()
                    Log.e("MainMenuActivity", "Error en sincronización con Google Sheets")
                }
                
            } catch (e: Exception) {
                Log.e("MainMenuActivity", "Error al sincronizar: ${e.message}", e)
                Toast.makeText(this@MainMenuActivity, "❌ Error al sincronizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun obtenerGoogleSheetsManager(): com.bithermmanagement.data.GoogleSheetsManager? {
        return try {
            val credentialsStream = applicationContext.assets.open("credentials_default.json")
            val authAdapter = com.bithermmanagement.data.GoogleAuthAdapter(
                context = applicationContext,
                useOAuth = false,
                oAuthEmail = "",
                credentialsStream = credentialsStream
            )
            com.bithermmanagement.data.GoogleSheetsManager(authAdapter, applicationContext)
        } catch (e: Exception) {
            Log.e("MainMenuActivity", "Error creando GoogleSheetsManager: ${e.message}", e)
            null
        }
    }
    
    private fun initializeAutomaticSync() {
        // Verificar sincronización automática cada 5 minutos
        val handler = Handler(Looper.getMainLooper())
        val syncRunnable = object : Runnable {
            override fun run() {
                verificarSincronizacionAutomatica()
                handler.postDelayed(this, 5 * 60 * 1000) // 5 minutos
            }
        }
        handler.post(syncRunnable)
    }
    
    private fun verificarSincronizacionAutomatica() {
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("bitherm_prefs", MODE_PRIVATE)
                val sincronizacionOnline = prefs.getBoolean("sincronizacion_online", false)
                val umbralSync = prefs.getInt("umbral_sincronizacion", 10)
                
                if (!sincronizacionOnline) {
                    Log.d("MainMenuActivity", "Sincronización automática deshabilitada (modo OFFLINE)")
                    return@launch
                }
                
                val db = AppDatabase.getDatabase(this@MainMenuActivity)
                val inspeccionDao = db.inspeccionDao()
                val equiposModificados = inspeccionDao.getModificadasLocal()
                
                Log.d("MainMenuActivity", "Verificando sincronización automática: ${equiposModificados.size} equipos modificados (umbral: $umbralSync)")
                
                // Sincronizar si hay el umbral configurado o más equipos modificados
                if (equiposModificados.size >= umbralSync) {
                    Log.d("MainMenuActivity", "Iniciando sincronización automática (${equiposModificados.size} equipos, umbral: $umbralSync)")
                    sincronizarEquiposModificados()
                }
                
            } catch (e: Exception) {
                Log.e("MainMenuActivity", "Error en verificación de sincronización automática: ${e.message}", e)
            }
        }
    }

    /**
     * Muestra un popup modal para las subAPPs
     * @param subAppName Nombre de la subAPP
     * @param subAppLayout ID del layout a mostrar en el popup
     */
    private fun showSubAppPopup(subAppName: String, subAppLayout: Int) {
        Log.d("MainMenuActivity", "=== INICIANDO showSubAppPopup ===")
        Log.d("MainMenuActivity", "subAppName: $subAppName, subAppLayout: $subAppLayout")
        
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        currentSubAppDialog = dialog // Guardar referencia
        Log.d("MainMenuActivity", "Dialog creado")
        
        dialog.setContentView(R.layout.dialog_subapp_modal)
        
        // Configurar listener para limpiar referencia cuando se cierre
        dialog.setOnDismissListener {
            Log.d("MainMenuActivity", "Dialog dismissed")
            currentSubAppDialog = null
        }
        Log.d("MainMenuActivity", "ContentView configurado")
        
        // Obtener dimensiones de la pantalla
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Calcular 97% del ancho y alto
        val dialogWidth = (screenWidth * 0.97).toInt()
        val dialogHeight = (screenHeight * 0.97).toInt()
        
        Log.d("MainMenuActivity", "Dimensiones calculadas: ${dialogWidth}x${dialogHeight}")
        
        dialog.window?.setLayout(dialogWidth, dialogHeight)
        
        // Centrar el diálogo y desplazarlo 3px hacia arriba
        val layoutParams = dialog.window?.attributes
        layoutParams?.y = -3 // Desplazar 3px hacia arriba
        dialog.window?.attributes = layoutParams
        dialog.window?.setGravity(Gravity.CENTER)
        
        val buttonClose = dialog.findViewById<ImageButton>(R.id.buttonClose)
        val textViewSubAppName = dialog.findViewById<TextView>(R.id.textViewSubAppName)
        val containerSubAppContent = dialog.findViewById<FrameLayout>(R.id.containerSubAppContent)
        
        Log.d("MainMenuActivity", "Elementos encontrados: buttonClose=${buttonClose != null}, textView=${textViewSubAppName != null}, container=${containerSubAppContent != null}")
        
        textViewSubAppName.text = subAppName
        buttonClose.setOnClickListener { 
            Log.d("MainMenuActivity", "Dialog cerrado por botón close")
            dialog.dismiss() 
        }
        
        // Inflar el contenido específico de la subApp
        val inflater = LayoutInflater.from(this)
        val subAppView = inflater.inflate(subAppLayout, containerSubAppContent, false)
        containerSubAppContent.removeAllViews()
        containerSubAppContent.addView(subAppView)
        
        Log.d("MainMenuActivity", "SubApp view inflado y agregado al container")
        
        // Mostrar el diálogo
        try {
            dialog.show()
            Log.d("MainMenuActivity", "=== DIALOG.SHOW() EJECUTADO ===")
            
            // Configurar botones específicamente para WorkCamera
            if (subAppLayout == R.layout.fragment_work_camera) {
                Log.d("MainMenuActivity", "Detectado WorkCamera por layout, configurando botones...")
                setupWorkCameraButtons(dialog)
            }
        } catch (e: Exception) {
            Log.e("MainMenuActivity", "Error al mostrar diálogo", e)
        }
        
        // Inicializar BithermChat si es necesario
                        if (subAppName == "BithermChat") {
                    Log.d("MainMenuActivity", "=== INICIANDO BITHERMCHAT CON LOADING ===")
                    
                    // Inflar el layout del chat primero (para que no se vea desordenado)
                    val chatView = LayoutInflater.from(this).inflate(R.layout.fragment_bitherm_chat, containerSubAppContent)
                    Log.d("MainMenuActivity", "Chat layout inflado")
                    
                    // INMEDIATAMENTE mostrar loading encima del chat (ocultando la desorden)
                    val loadingView = LayoutInflater.from(this).inflate(R.layout.loading_chat, containerSubAppContent)
                    Log.d("MainMenuActivity", "Loading mostrado, programando eliminación en 2 segundos")
                    
                    // Handler más robusto con fallback
                    val handler = Handler(Looper.getMainLooper())
                    val runnable = Runnable {
                        try {
                            Log.d("MainMenuActivity", "=== EJECUTANDO HANDLER DESPUÉS DE 2 SEGUNDOS ===")
                            Log.d("MainMenuActivity", "Vistas en contenedor antes de remover: ${containerSubAppContent.childCount}")
                            
                            // NUEVA ESTRATEGIA: Limpiar todo y volver a inflar solo el chat
                            containerSubAppContent.removeAllViews()
                            Log.d("MainMenuActivity", "Todas las vistas removidas, contador: ${containerSubAppContent.childCount}")
                            
                            // Inflar solo el chat
                            val finalChatView = LayoutInflater.from(this).inflate(R.layout.fragment_bitherm_chat, containerSubAppContent)
                            Log.d("MainMenuActivity", "Chat final inflado, contador: ${containerSubAppContent.childCount}")
                            
                            // Inicializar chat
                            initializeBithermChat(containerSubAppContent)
                            Log.d("MainMenuActivity", "Chat inicializado correctamente")
                            
                            // Verificar que el chat sea visible
                            if (finalChatView != null) {
                                Log.d("MainMenuActivity", "Chat view final: ${finalChatView.javaClass.simpleName}, visible: ${finalChatView.visibility == View.VISIBLE}")
                                finalChatView.visibility = View.VISIBLE
                                finalChatView.invalidate()
                            } else {
                                Log.e("MainMenuActivity", "No se pudo inflar el chat final")
                            }
                        } catch (e: Exception) {
                            Log.e("MainMenuActivity", "Error en Handler: ${e.message}", e)
                            // Fallback: remover todo y volver a inflar
                            containerSubAppContent.removeAllViews()
                            val fallbackChatView = LayoutInflater.from(this).inflate(R.layout.fragment_bitherm_chat, containerSubAppContent)
                            initializeBithermChat(containerSubAppContent)
                        }
                    }
                    
                    // Programar Handler (duplicado a 2 segundos)
                    handler.postDelayed(runnable, 2000)
                    
                    // Fallback: si después de 5 segundos no se ejecutó, forzar
                    handler.postDelayed({
                        Log.d("MainMenuActivity", "=== FALLBACK: FORZANDO INICIALIZACIÓN ===")
                        if (containerSubAppContent.childCount > 1) { // Si aún hay loading
                            runnable.run()
                        }
                    }, 5000)
                }
        
        dialog.show()
    }
    
    /**
     * Muestra la galería personalizada en un diálogo
     */
    fun showCustomGallery() {
        Log.d("MainMenuActivity", "Mostrando galería personalizada")
        
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_subapp_modal)
        
        // Obtener dimensiones de la pantalla
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Calcular 97% del ancho y alto
        val dialogWidth = (screenWidth * 0.97).toInt()
        val dialogHeight = (screenHeight * 0.97).toInt()
        
        dialog.window?.setLayout(dialogWidth, dialogHeight)
        
        // Centrar el diálogo y desplazarlo 3px hacia arriba
        val layoutParams = dialog.window?.attributes
        layoutParams?.y = -3 // Desplazar 3px hacia arriba
        dialog.window?.attributes = layoutParams
        dialog.window?.setGravity(Gravity.CENTER)
        
        val buttonClose = dialog.findViewById<ImageButton>(R.id.buttonClose)
        val textViewSubAppName = dialog.findViewById<TextView>(R.id.textViewSubAppName)
        val containerSubAppContent = dialog.findViewById<FrameLayout>(R.id.containerSubAppContent)
        
        textViewSubAppName.text = "Galería"
        buttonClose.setOnClickListener { dialog.dismiss() }
        
        // Limpiar contenedor
        containerSubAppContent.removeAllViews()
        
        // Crear y configurar la vista de galería personalizada
        val galleryView = com.bithermmanagement.ui.gallery.CustomGalleryView(this)
        galleryView.onBackPressed = { dialog.dismiss() }
        galleryView.onPhotoSelected = { photo ->
            Log.d("MainMenuActivity", "Foto seleccionada: ${photo.file.name}")
            android.widget.Toast.makeText(this, "Foto: ${photo.file.name}", android.widget.Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        
        // Agregar la vista al contenedor
        containerSubAppContent.addView(galleryView)
        
        // Mostrar el diálogo
        dialog.show()
        
        Log.d("MainMenuActivity", "Diálogo de galería mostrado")
    }

    /**
     * Muestra un popup modal con un fragmento
     * NOTA: Temporalmente deshabilitado por problemas de lifecycle
     */
    fun showSubAppPopupWithFragment(subAppName: String, fragment: androidx.fragment.app.Fragment) {
        Log.w("MainMenuActivity", "showSubAppPopupWithFragment deshabilitado temporalmente")
        android.widget.Toast.makeText(this, "Funcionalidad en desarrollo", android.widget.Toast.LENGTH_SHORT).show()
        
        /* CÓDIGO COMENTADO HASTA RESOLVER PROBLEMAS DE LIFECYCLE
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.dialog_subapp_modal)
        
        // Obtener dimensiones de la pantalla
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        // Calcular 97% del ancho y alto
        val dialogWidth = (screenWidth * 0.97).toInt()
        val dialogHeight = (screenHeight * 0.97).toInt()
        
        dialog.window?.setLayout(dialogWidth, dialogHeight)
        
        // Centrar el diálogo y desplazarlo 3px hacia arriba
        val layoutParams = dialog.window?.attributes
        layoutParams?.y = -3 // Desplazar 3px hacia arriba
        dialog.window?.attributes = layoutParams
        dialog.window?.setGravity(Gravity.CENTER)
        
        val buttonClose = dialog.findViewById<ImageButton>(R.id.buttonClose)
        val textViewSubAppName = dialog.findViewById<TextView>(R.id.textViewSubAppName)
        val containerSubAppContent = dialog.findViewById<FrameLayout>(R.id.containerSubAppContent)
        
        textViewSubAppName.text = subAppName
        buttonClose.setOnClickListener { dialog.dismiss() }
        
        // Limpiar contenedor
        containerSubAppContent.removeAllViews()
        
        // Mostrar el diálogo PRIMERO
        dialog.show()
        */
    }
    
	
		private fun setupWorkCameraButtons(dialog: Dialog) {
			Log.d("MainMenuActivity", "=== INICIANDO setupWorkCameraButtons CON DIÁLOGO ===")
			Handler(Looper.getMainLooper()).postDelayed({
				Log.d("MainMenuActivity", "=== EJECUTANDO setupWorkCameraButtons después del delay ===")
				try {
					// Buscar el contenedor EN LA VENTANA DEL DIÁLOGO
					val dialogDecorView = dialog.window?.decorView as? ViewGroup
					Log.d("MainMenuActivity", "Dialog DecorView obtenido: ${dialogDecorView?.javaClass?.simpleName}")
					
					if (dialogDecorView != null) {
						val containerView = findViewWithId(dialogDecorView, R.id.containerSubAppContent) as? ViewGroup
						Log.d("MainMenuActivity", "ContainerView encontrado en diálogo: ${containerView != null}")
						
						containerView?.let {
							Log.d("MainMenuActivity", "=== CONTAINER ENCONTRADO EN DIÁLOGO, LLAMANDO A WorkCameraFragment.setupButtonsForView ===")
							WorkCameraFragment.setupButtonsForView(it, this)
							Log.d("MainMenuActivity", "Botones de cámara configurados correctamente")
						} ?: run {
							Log.e("MainMenuActivity", "*** NO SE ENCONTRÓ EL CONTENEDOR EN EL DIÁLOGO ***")
							
							// Intentar buscar en toda la jerarquía del diálogo
							Log.d("MainMenuActivity", "=== BUSCANDO EN TODA LA JERARQUÍA DEL DIÁLOGO ===")
							logViewHierarchy(dialogDecorView, 0)
						}
					} else {
						Log.e("MainMenuActivity", "*** NO SE PUDO OBTENER EL DECORVIEW DEL DIÁLOGO ***")
					}
				} catch (e: Exception) {
					Log.e("MainMenuActivity", "Error configurando botones de cámara", e)
				}
			}, 500) // Delay para asegurar que el diálogo esté completamente renderizado
		}

		private fun findViewWithId(parent: ViewGroup, id: Int): View? {
			if (parent.id == id) return parent
			for (i in 0 until parent.childCount) {
				val child = parent.getChildAt(i)
				if (child.id == id) return child
				if (child is ViewGroup) {
					val found = findViewWithId(child, id)
					if (found != null) return found
				}
			}
			return null
		}
		
		private fun logViewHierarchy(view: View, depth: Int) {
			val indent = "  ".repeat(depth)
			val idName = try {
				if (view.id != View.NO_ID) resources.getResourceEntryName(view.id) else "NO_ID"
			} catch (e: Exception) {
				"UNKNOWN_ID"
			}
			Log.d("MainMenuActivity", "$indent${view.javaClass.simpleName} - ID: $idName")
			
			if (view is ViewGroup) {
				for (i in 0 until view.childCount) {
					logViewHierarchy(view.getChildAt(i), depth + 1)
				}
			}
		}
	
	
    /**
     * Inicializa manualmente la lógica del BithermChat
     */
    private fun initializeBithermChat(chatView: View) {
        try {
            Log.d("MainMenuActivity", "=== INICIALIZANDO BITHERMCHAT MANUALMENTE ===")
            
            // Inicializar vistas
            val tabLayout = chatView.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabLayout)
            val viewPager = chatView.findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPager)
            
            Log.d("MainMenuActivity", "Vistas encontradas: tabLayout=${tabLayout != null}, viewPager=${viewPager != null}")
            
            if (tabLayout != null && viewPager != null) {
                // Cargar datos directamente desde el servicio
                loadChatDataDirectly(chatView, tabLayout, viewPager)
            } else {
                Log.e("MainMenuActivity", "No se pudieron encontrar todas las vistas necesarias")
            }
        } catch (e: Exception) {
            Log.e("MainMenuActivity", "Error inicializando BithermChat", e)
        }
    }
    
    /**
     * Carga los datos del chat directamente desde el servicio
     */
    private fun loadChatDataDirectly(
        chatView: View,
        tabLayout: com.google.android.material.tabs.TabLayout,
        viewPager: androidx.viewpager2.widget.ViewPager2
    ) {
        // Ejecutar en background
        Thread {
            try {
                Log.d("MainMenuActivity", "Cargando datos del chat desde servicio...")
                
                // Crear servicio de datos
                val chatDataService = com.bithermmanagement.chat.services.ChatDataService(this)
                
                // Cargar usuarios
                val users = runBlocking { chatDataService.loadUsersFromGoogleSheets() }
                Log.d("MainMenuActivity", "Usuarios cargados: ${users.size}")
                
                // Actualizar estado online basado en fichajes
                val onlineManager = com.bithermmanagement.chat.services.FichajeBasedOnlineManager(this)
                for (user in users) {
                    val isOnline = runBlocking { onlineManager.isUserOnline(user.username) }
                    user.isOnline = isOnline
                    Log.d("MainMenuActivity", "Usuario ${user.username} (${user.visibleName}) - Online: $isOnline")
                }
                
                // Cargar grupos
                val groups = chatDataService.loadGroupsFromCache()
                Log.d("MainMenuActivity", "Grupos cargados: ${groups.size}")
                
                // Actualizar UI en el hilo principal
                runOnUiThread {
                    if (groups.isNotEmpty()) {
                        Log.d("MainMenuActivity", "Configurando tabs para grupos: ${groups.map { it.displayName }}")
                        setupChatTabs(tabLayout, viewPager, groups)
                    }
                    
                    Log.d("MainMenuActivity", "Mostrando todos los usuarios en el carrusel: ${users.size}")
                    updateOnlineUsersCarousel(chatView, users)
                }
            } catch (e: Exception) {
                Log.e("MainMenuActivity", "Error cargando datos del chat", e)
            }
        }.start()
    }
    
    /**
     * Configura los tabs del chat
     */
    private fun setupChatTabs(
        tabLayout: com.google.android.material.tabs.TabLayout,
        viewPager: androidx.viewpager2.widget.ViewPager2,
        groups: List<com.bithermmanagement.chat.models.ChatGroup>
    ) {
        // Limpiar tabs existentes
        tabLayout.removeAllTabs()
        
        // Agregar tabs para cada grupo
        groups.forEach { group ->
            val tab = tabLayout.newTab().setText(group.displayName)
            tabLayout.addTab(tab)
        }
        
        // Configurar listener para los tabs
        tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                tab?.let {
                    viewPager.currentItem = it.position
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })

        // CONFIGURAR EL ADAPTADOR INMEDIATAMENTE (no solo al seleccionar)
        val adapter = ChatPagerAdapter()
        adapter.updateGroups(groups)
        viewPager.adapter = adapter

        // Seleccionar el primer tab por defecto
        if (tabLayout.tabCount > 0) {
            tabLayout.selectTab(tabLayout.getTabAt(0))
        }
    }
    
    /**
     * Actualiza el carrusel de usuarios online
     */
    private fun updateOnlineUsersCarousel(
        chatView: View,
        onlineUsers: List<com.bithermmanagement.chat.models.ChatUser>
    ) {
        // Obtener la referencia al carrusel horizontal
        val usersCarousel = chatView.findViewById<LinearLayout>(R.id.usersCarousel)
        usersCarousel?.removeAllViews()
        
        // Obtener el usuario actual
        val currentUsername = getCurrentUsername()
        
        onlineUsers.forEach { user ->
            // Excluir al usuario actual del carrusel
            if (user.username != currentUsername) {
                val userAvatar = createUserAvatar(user)
                usersCarousel?.addView(userAvatar)
            }
        }
    }
    
    private fun getCurrentUsername(): String? {
        val prefs = getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        return prefs.getString("username", null)
    }
    
    /**
     * Crea un avatar de usuario
     */
    private fun createUserAvatar(user: com.bithermmanagement.chat.models.ChatUser): View {
        val avatarView = LayoutInflater.from(this).inflate(R.layout.item_user_avatar, null, false)
        
        val avatarImage = avatarView.findViewById<ImageView>(R.id.avatarImage)
        val onlineIndicator = avatarView.findViewById<View>(R.id.onlineIndicator)
        val userNameText = avatarView.findViewById<TextView>(R.id.userNameText)
        
        // Configurar avatar (por ahora usamos icono genérico)
        avatarImage.setImageResource(R.drawable.ic_person)
        
        // Configurar nombre del usuario
        userNameText.text = user.visibleName
        
        // Mostrar indicador online
        onlineIndicator.visibility = if (user.isOnline) View.VISIBLE else View.GONE
        
        // Configurar click listener
        avatarView.setOnClickListener {
            Log.d("MainMenuActivity", "Click en usuario: ${user.username}")
            openChatConversation(user.username, "individual", user.visibleName)
        }
        
        return avatarView
    }
    
    /**
     * Genera un chatId consistente para chats individuales
     */
    private fun generateChatId(user1: String, user2: String): String {
        // Ordenar alfabéticamente para que siempre sea el mismo ID
        val users = listOf(user1, user2).sorted()
        return "${users[0]}_${users[1]}"
    }
    
    /**
     * Abre una conversación de chat
     */
    private fun openChatConversation(otherUsername: String, chatType: String, chatName: String) {
        val currentUsername = getCurrentUsername()
        if (currentUsername == null) {
            Log.e("MainMenuActivity", "No se pudo obtener username actual")
            return
        }
        
        Log.d("MainMenuActivity", "Abriendo conversación: $otherUsername ($chatType) - $chatName")
        
        // Crear y mostrar el diálogo de conversación
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(R.layout.fragment_chat_conversation_whatsapp)
        
        // Configurar el diálogo
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Configurar elementos del diálogo
        val buttonBack = dialog.findViewById<ImageButton>(R.id.buttonBack)
        val textViewChatName = dialog.findViewById<TextView>(R.id.textViewChatName)
        val editTextMessage = dialog.findViewById<EditText>(R.id.editTextMessage)
        val buttonSend = dialog.findViewById<ImageButton>(R.id.buttonSend)
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recyclerViewMessages)
        
        // Configurar nombre del chat
        textViewChatName.text = chatName
        
        // Configurar RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = com.bithermmanagement.chat.adapters.ChatMessagesAdapter()
        recyclerView.adapter = adapter
        
        // Cargar mensajes existentes
        loadMessagesForChat(otherUsername, adapter, false) // false = chat individual
        
        // Guardar referencia al adaptador en el diálogo
        dialog.findViewById<View>(android.R.id.content).tag = adapter
        
        // REGISTRAR USUARIO EN FIREBASE Y ACTIVAR LISTENER
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val firebaseService = com.bithermmanagement.chat.services.FirebaseChatService(this@MainMenuActivity)
                
                // Registrar usuario actual en Firebase
                firebaseService.registerUser(currentUsername)
                Log.d("MainMenuActivity", "Usuario registrado en Firebase: $currentUsername")
                
                // Activar listener para mensajes dirigidos AL USUARIO ACTUAL
                firebaseService.listenToChat(currentUsername) { message ->
                    Log.d("MainMenuActivity", "Mensaje recibido en tiempo real: ${message.content}")
                    
                    // Actualizar UI en el hilo principal
                    runOnUiThread {
                        adapter.addMessage(message, currentUsername)
                        
                        // Scroll al último mensaje
                        recyclerView.post {
                            recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }
                Log.d("MainMenuActivity", "Listener de Firebase activado para chat: $currentUsername")
                
            } catch (e: Exception) {
                Log.e("MainMenuActivity", "Error configurando Firebase", e)
            }
        }
        
        // Configurar botón de retroceso
        buttonBack.setOnClickListener {
            dialog.dismiss()
        }
        
        // Configurar botón de menú
        val buttonMore = dialog.findViewById<ImageButton>(R.id.buttonMore)
        buttonMore.setOnClickListener {
            showChatMenu(dialog, otherUsername, chatType)
        }
        
        // Configurar envío de mensajes
        buttonSend.setOnClickListener {
            val messageText = editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessageDirectly(otherUsername, chatType, messageText, dialog)
                editTextMessage.text.clear()
            }
        }
        
        // Mostrar diálogo
        dialog.show()
    }
    
    /**
     * Envía mensaje directamente sin fragmentos
     */
    private fun sendMessageDirectly(otherUsername: String, chatType: String, messageText: String, dialog: Dialog) {
        val currentUsername = getCurrentUsername()
        if (currentUsername.isNullOrEmpty()) {
            Log.e("MainMenuActivity", "No se pudo obtener username actual")
            return
        }
        
        // Obtener el adaptador del tag del diálogo
        val adapter = dialog.findViewById<View>(android.R.id.content).tag as? com.bithermmanagement.chat.adapters.ChatMessagesAdapter
        if (adapter == null) {
            Log.e("MainMenuActivity", "No se pudo obtener el adaptador del diálogo")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainMenuActivity", "Enviando mensaje: $messageText a $otherUsername")
                
                val chatMessageService = com.bithermmanagement.chat.services.ChatMessageService(this@MainMenuActivity)
                val messageId = chatMessageService.sendMessage(
                    senderUsername = currentUsername,
                    chatId = otherUsername, // El destinatario es el chatId
                    chatType = chatType,
                    content = messageText
                )
                
                Log.d("MainMenuActivity", "Mensaje enviado con ID: $messageId")
                
                // Recargar mensajes para mostrar el nuevo
                loadMessagesForChat(otherUsername, adapter, false) // false = chat individual
                
            } catch (e: Exception) {
                Log.e("MainMenuActivity", "Error enviando mensaje", e)
            }
        }
    }
    
    /**
     * Muestra el menú del chat con opciones
     */
    private fun showChatMenu(dialog: Dialog, chatId: String, chatType: String) {
        val popupMenu = android.widget.PopupMenu(this, dialog.findViewById(R.id.buttonMore))
        popupMenu.menu.add("Vaciar chat")
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title) {
                "Vaciar chat" -> {
                    clearChat(dialog, chatId, chatType)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    /**
     * Vacía el chat eliminando todos los mensajes
     */
    private fun clearChat(dialog: Dialog, chatId: String, chatType: String) {
        val alertDialog = android.app.AlertDialog.Builder(this)
            .setTitle("Vaciar chat")
            .setMessage("¿Estás seguro de que quieres eliminar todos los mensajes de este chat?")
            .setPositiveButton("Sí, vaciar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Eliminar mensajes de Firebase
                        val firebaseService = com.bithermmanagement.chat.services.FirebaseChatService(this@MainMenuActivity)
                        firebaseService.clearChat(chatId)
                        
                        // Eliminar mensajes de la base de datos local
                        val chatMessageService = com.bithermmanagement.chat.services.ChatMessageService(this@MainMenuActivity)
                        chatMessageService.clearChat(chatId)
                        
                        // Actualizar UI
                        runOnUiThread {
                            val adapter = dialog.findViewById<View>(android.R.id.content).tag as? com.bithermmanagement.chat.adapters.ChatMessagesAdapter
                            adapter?.updateMessages(emptyList(), getCurrentUsername() ?: "", chatType == "group")
                        }
                    } catch (e: Exception) {
                        Log.e("MainMenuActivity", "Error vaciando chat", e)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        alertDialog.show()
    }
    
    /**
     * Carga mensajes para un chat específico
     */
    private fun loadMessagesForChat(chatId: String, adapter: com.bithermmanagement.chat.adapters.ChatMessagesAdapter, isGroup: Boolean) {
        val currentUsername = getCurrentUsername() ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val chatMessageService = com.bithermmanagement.chat.services.ChatMessageService(this@MainMenuActivity)
                val messages = chatMessageService.getMessagesForChat(chatId)
                
                withContext(Dispatchers.Main) {
                    adapter.updateMessages(messages, currentUsername, isGroup)
                    
                    // Scroll al último mensaje
                    if (messages.isNotEmpty()) {
                        val recyclerView = adapter.itemCount - 1
                        // TODO: Implementar scroll automático
                    }
                }
                
            } catch (e: Exception) {
                Log.e("MainMenuActivity", "Error cargando mensajes", e)
            }
        }
    }
    
    /**
     * Adaptador para el ViewPager del chat
     */
    private inner class ChatPagerAdapter : androidx.viewpager2.adapter.FragmentStateAdapter(this) {
        private var groups: List<com.bithermmanagement.chat.models.ChatGroup> = emptyList()
        
        fun updateGroups(newGroups: List<com.bithermmanagement.chat.models.ChatGroup>) {
            groups = newGroups
            notifyDataSetChanged()
        }
        
        override fun getItemCount(): Int = groups.size
        
        override fun createFragment(position: Int): Fragment {
            val group = groups[position]
            return ChatGroupFragment.newInstance(group.name, group.displayName)
        }
    }
} 