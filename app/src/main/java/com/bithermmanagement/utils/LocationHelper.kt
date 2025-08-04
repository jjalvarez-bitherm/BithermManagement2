package com.bithermmanagement.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "LocationHelper"
    }

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    suspend fun getCurrentLocation(
        onSuccess: (Location) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                onFailure(Exception("Permiso de ubicación no concedido"))
                return
            }

            val location = getCurrentLocationSuspend()
            onSuccess(location)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ubicación: ${e.message}")
            onFailure(e)
        }
    }

    private suspend fun getCurrentLocationSuspend(): Location = suspendCancellableCoroutine { continuation ->
        try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resumeWithException(SecurityException("Permiso de ubicación no concedido"))
                return@suspendCancellableCoroutine
            }

            val cancellationToken = object : CancellationToken() {
                override fun onCanceledRequested(listener: OnTokenCanceledListener): CancellationToken {
                    return CancellationTokenSource().token
                }

                override fun isCancellationRequested(): Boolean {
                    return false
                }
            }

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationToken)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        Log.d(TAG, "Ubicación obtenida: ${location.latitude}, ${location.longitude}")
                        continuation.resume(location)
                    } else {
                        continuation.resumeWithException(Exception("No se pudo obtener la ubicación"))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al obtener ubicación: ${exception.message}")
                    continuation.resumeWithException(exception)
                }

        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun formatLocation(location: Location): String {
        return "${location.latitude},${location.longitude}"
    }

    fun calculateDistance(location1: Location, location2: Location): Float {
        return location1.distanceTo(location2)
    }
} 