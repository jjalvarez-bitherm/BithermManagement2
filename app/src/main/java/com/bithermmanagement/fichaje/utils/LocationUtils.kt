package com.bithermmanagement.fichaje.utils

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object LocationUtils {
    
    data class LocationInfo(
        val name: String,
        val latitude: Double,
        val longitude: Double,
        val toleranceRadius: Int
    )
    
    suspend fun findNearestLocation(latitude: Double, longitude: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val locations = loadLocations()
                if (locations.isEmpty()) return@withContext "LUGAR DESCONOCIDO"

                var nearestLocation = locations[0]
                var minDistance = calculateDistance(
                    latitude, longitude,
                    nearestLocation.latitude, nearestLocation.longitude
                )

                locations.forEach { location ->
                    val distance = calculateDistance(
                        latitude, longitude,
                        location.latitude, location.longitude
                    )
                    if (distance < minDistance) {
                        minDistance = distance
                        nearestLocation = location
                    }
                }

                if (minDistance <= nearestLocation.toleranceRadius) nearestLocation.name else "LUGAR DESCONOCIDO"
            } catch (e: Exception) {
                "LUGAR DESCONOCIDO"
            }
        }
    }
    
    fun createGoogleMapsLink(latitude: Double, longitude: Double): String {
        return "https://maps.google.com/?q=$latitude,$longitude"
    }
    
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }
    
    private suspend fun loadLocations(): List<LocationInfo> {
        return withContext(Dispatchers.IO) {
            // Aquí cargarías las ubicaciones desde Google Sheets o base de datos local
            // Por ahora retornamos ubicaciones de ejemplo
            listOf(
                LocationInfo("OFICINA PRINCIPAL", 40.4168, -3.7038, 100),
                LocationInfo("ALMACÉN CENTRAL", 40.4000, -3.7000, 150),
                LocationInfo("TALLER MECÁNICO", 40.4100, -3.7100, 200)
            )
        }
    }
} 