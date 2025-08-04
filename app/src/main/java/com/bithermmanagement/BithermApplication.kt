package com.bithermmanagement

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import com.bithermmanagement.database.AppDatabase

@HiltAndroidApp
class BithermApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Inicialización de la aplicación
        // La base de datos se inicializará automáticamente cuando sea necesaria
        
        // TEMPORAL: Borrar base de datos para evitar problemas de migración
        // Comentar esta línea después de la primera ejecución exitosa
        // try {
        //     Log.d("BithermApplication", "Borrando base de datos para recreación limpia...")
        //     AppDatabase.resetDatabase(this)
        //     Log.d("BithermApplication", "Base de datos borrada exitosamente")
        // } catch (e: Exception) {
        //     Log.e("BithermApplication", "Error borrando base de datos", e)
        // }
    }
} 