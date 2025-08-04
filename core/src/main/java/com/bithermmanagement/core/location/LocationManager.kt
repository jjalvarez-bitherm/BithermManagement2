package com.bithermmanagement.core.location

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bithermmanagement.core.utils.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationManager @Inject constructor(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        Constants.SHARED_PREFS_NAME, 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val TAG = "LocationManager"
    }
    
    /**
     * Inicia el servicio de tracking de ubicación
     */
    fun startLocationTracking() {
        try {
            Log.d(TAG, "Iniciando servicio de tracking de ubicación")
            LocationTrackerService.startService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar el servicio de ubicación: ${e.message}")
        }
    }
    
    /**
     * Detiene el servicio de tracking de ubicación
     */
    fun stopLocationTracking() {
        try {
            Log.d(TAG, "Deteniendo servicio de tracking de ubicación")
            LocationTrackerService.stopService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener el servicio de ubicación: ${e.message}")
        }
    }
    
    /**
     * Verifica si el servicio de ubicación está habilitado para el usuario actual
     */
    fun isLocationTrackingEnabled(): Boolean {
        return sharedPreferences.getBoolean(Constants.LOCATION_SERVICE_ENABLED, false)
    }
    
    /**
     * Habilita o deshabilita el servicio de ubicación
     */
    fun setLocationTrackingEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(Constants.LOCATION_SERVICE_ENABLED, enabled)
            .apply()
        
        if (enabled) {
            startLocationTracking()
        } else {
            stopLocationTracking()
        }
    }
    
    /**
     * Obtiene el intervalo de tracking configurado para el usuario
     */
    fun getTrackingInterval(): Long {
        return sharedPreferences.getLong(
            Constants.LOCATION_TRACKING_INTERVAL, 
            Constants.DEFAULT_TRACKING_INTERVAL_MINUTES
        )
    }
    
    /**
     * Configura el intervalo de tracking para el usuario
     */
    fun setTrackingInterval(minutes: Long) {
        sharedPreferences.edit()
            .putLong(Constants.LOCATION_TRACKING_INTERVAL, minutes)
            .apply()
    }
    
    /**
     * Obtiene el modo de tracking configurado para el usuario
     */
    fun getTrackingMode(): String {
        return sharedPreferences.getString(
            Constants.LOCATION_TRACKING_MODE, 
            Constants.TRACKING_MODE_DISABLED
        ) ?: Constants.TRACKING_MODE_DISABLED
    }
    
    /**
     * Configura el modo de tracking para el usuario
     */
    fun setTrackingMode(mode: String) {
        sharedPreferences.edit()
            .putString(Constants.LOCATION_TRACKING_MODE, mode)
            .apply()
    }
    
    /**
     * Verifica si el usuario tiene permisos de ubicación
     */
    fun hasLocationPermissions(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == 
               android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Reinicia el servicio de ubicación (útil para aplicar cambios de configuración)
     */
    fun restartLocationTracking() {
        stopLocationTracking()
        if (isLocationTrackingEnabled()) {
            startLocationTracking()
        }
    }
} 