package com.bithermmanagement.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bithermmanagement.core.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationTrackerService : Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var lastKnownLocation: Location? = null
    // GoogleSheetsManager se implementará cuando sea necesario
    private lateinit var sharedPreferences: SharedPreferences
    private var usuario_login: String = ""
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    private val selectedSpreadsheetId = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
    private val LOCALE_LOGS_SHEET = "LOCALE-LOGS"
    private var locationUpdateJob: Job? = null
    private var userTrackingInterval: Long = 900000 // Default 15 minutos
    private var isTrackingEnabled = false
    private var trackingMode = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedPreferences = getSharedPreferences(Constants.SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        usuario_login = sharedPreferences.getString(Constants.PREF_USERNAME, "") ?: ""
        
        setupLocationCallback()
        // Por ahora, iniciar tracking básico sin configuración de Google Sheets
        startBasicLocationUpdates()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sistema de Ubicación",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Servicio de tracking de ubicación en segundo plano"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, Class.forName("com.bithermmanagement.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, 0)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sistema de Ubicación")
            .setContentText("Tracking activo para $usuario_login")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setShowWhen(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (lastKnownLocation == null || 
                        calculateDistance(lastKnownLocation!!, location) > 100) {
                        lastKnownLocation = location
                        saveLocationToSheets(location)
                    }
                }
            }
        }
    }

    private fun startBasicLocationUpdates() {
        // Iniciar tracking básico con intervalo por defecto
        userTrackingInterval = 15 * 60 * 1000L // 15 minutos
        startLocationUpdates()
        Log.d("LocationService", "Iniciado tracking básico para $usuario_login")
    }



    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, userTrackingInterval)
            .setMinUpdateIntervalMillis(userTrackingInterval)
            .setMaxUpdateDelayMillis(userTrackingInterval * 2)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            Log.d("LocationService", "Iniciado tracking de ubicación para $usuario_login cada ${userTrackingInterval/60000} minutos")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Error al solicitar actualizaciones de ubicación: ${e.message}")
        }
    }

    private fun calculateDistance(loc1: Location, loc2: Location): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            loc1.latitude,
            loc1.longitude,
            loc2.latitude,
            loc2.longitude,
            results
        )
        return results[0]
    }

    private fun saveLocationToSheets(location: Location) {
        locationUpdateJob?.cancel()
        locationUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Date()
                val gpsLink = "https://www.google.com/maps?q=${location.latitude},${location.longitude}"
                
                // Por ahora solo hacer log, la integración con Google Sheets se implementará después
                Log.d("LocationService", "Ubicación capturada para $usuario_login: ${location.latitude}, ${location.longitude}")
                Log.d("LocationService", "Enlace GPS: $gpsLink")
                Log.d("LocationService", "Fecha: ${dateFormat.format(now)}")
            } catch (e: Exception) {
                Log.e("LocationTracker", "Error al procesar ubicación", e)
            }
        }
    }



    private fun stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("LocationService", "Detenido tracking de ubicación para $usuario_login")
        } catch (e: Exception) {
            Log.e("LocationTracker", "Error al detener actualizaciones de ubicación", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        locationUpdateJob?.cancel()
        Log.d("LocationService", "Servicio de ubicación destruido")
    }

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
        
        fun startService(context: Context) {
            val intent = Intent(context, LocationTrackerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, LocationTrackerService::class.java)
            context.stopService(intent)
        }
    }
} 