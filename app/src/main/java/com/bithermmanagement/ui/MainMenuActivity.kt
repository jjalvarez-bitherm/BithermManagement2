package com.bithermmanagement.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.bithermmanagement.core.base.DebugBaseActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.commit
import com.bithermmanagement.R
import com.bithermmanagement.data.UserData
import com.bithermmanagement.ui.favoritos.FragmentFavoritos
import com.bithermmanagement.ui.work.WorkFragment
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
import android.view.LayoutInflater
import android.widget.Switch
import android.widget.Toast

@AndroidEntryPoint
class MainMenuActivity : DebugBaseActivity() {
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var bestLocation: Location? = null
    private var bestPrecision: Float = Float.MAX_VALUE
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 30000 // 30 segundos

    object GpsProvider {
        var lastLocation: Location? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainMenuActivity", "onCreate llamado")
        setContentView(R.layout.activity_main_menu)

        // Obtener datos del usuario
        val userData = intent.getParcelableExtra<UserData>("USER_DATA")
        Log.d("MainMenuActivity", "userData recibido: $userData")

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
                    supportFragmentManager.commit {
                        Log.d("MainMenuActivity", "Cargando WorkFragment desde bottomNav")
                        replace(R.id.fragment_container, WorkFragment())
                    }
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

    private fun showGpsDebugDialog() {
        // Crear el layout personalizado
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_gps_debug, null)
        
        // Configurar información GPS
        val tvGpsInfo = dialogView.findViewById<TextView>(R.id.tv_gps_info)
        val loc = bestLocation
        val gpsText = if (loc != null) {
            "📍 GPS Actual\n\nCoordenadas:\n${loc.latitude}, ${loc.longitude}\n\nPrecisión: ${"%.1f".format(loc.accuracy)} m"
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
        
        // Crear y mostrar el diálogo
        AlertDialog.Builder(this)
            .setTitle("GPS & Debug")
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Revertir el estado del switch si se cancela
                switchDebug.isChecked = DebugConfigManager.isDebugModeEnabled(this)
                dialog.dismiss()
            }
            .show()
    }
} 