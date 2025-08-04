package com.bithermmanagement.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.bithermmanagement.R
import com.bithermmanagement.data.UserRole

@Parcelize
data class UserData(
    val codigo: String,
    val nombre: String,
    val apellidos: String,
    val dni: String,
    val fechaNacimiento: String,
    val app: String, // Usuario
    val pass: String, // Contraseña
    val rol: String,
    val swWeb: String,
    val equipoAsignado: String,
    val fechaCalibracion: String,
    val telefonoEmpresa: String,
    val emailEmpresa: String,
    val fechaAltaEmpresa: String,
    val telefonoPersonal: String,
    val emailPersonal: String,
    val categoria: String,
    val revisionMedica: String,
    val accesoRLR: Boolean,
    val supervisorEjecutivo: Boolean,
    val apodo: String,
    var role: UserRole = UserRole.VIEWER
) : Parcelable {
    fun getRoleFromString(): UserRole {
        return when (rol.uppercase()) {
            "SUPERADMIN" -> UserRole.SUPERADMIN
            "ADMIN" -> UserRole.ADMIN
            "TEAMLIDER" -> UserRole.TEAMLIDER
            "INSPECTOR" -> UserRole.INSPECTOR
            else -> UserRole.VIEWER
        }
    }

    fun hasPermissionByWeight(requiredWeight: Int): Boolean {
        return role.weight >= requiredWeight
    }

    fun canAccessMenuItem(itemId: Int): Boolean {
        return when (itemId) {
            R.id.nav_user_config -> hasPermissionByWeight(1)
            R.id.nav_app_config -> hasPermissionByWeight(4)
            R.id.nav_device_config -> hasPermissionByWeight(3)
            
            R.id.nav_my_profile -> true
            R.id.nav_my_checkin -> true
            R.id.nav_worker_list -> hasPermissionByWeight(3)
            R.id.nav_today_checkins -> hasPermissionByWeight(3)
            
            R.id.nav_new_facility -> hasPermissionByWeight(4)
            R.id.nav_facilities_list -> hasPermissionByWeight(1)
            
            R.id.nav_inspection_config -> hasPermissionByWeight(4)
            R.id.nav_current_inspection -> hasPermissionByWeight(2)
            R.id.nav_current_download -> hasPermissionByWeight(2)
            R.id.nav_previous_inspections -> hasPermissionByWeight(1)
            R.id.nav_search_purger -> true
            
            R.id.nav_new_equipment -> hasPermissionByWeight(4)
            R.id.nav_equipment_list -> hasPermissionByWeight(1)
            
            R.id.nav_logout -> hasPermissionByWeight(1)
            
            else -> false
        }
    }

    fun getMenuVisibility(): Map<Int, Boolean> {
        return mapOf(
            R.id.nav_user_config to hasPermissionByWeight(1),
            R.id.nav_app_config to hasPermissionByWeight(4),
            R.id.nav_device_config to hasPermissionByWeight(3),
            
            R.id.nav_my_profile to true,
            R.id.nav_my_checkin to true,
            R.id.nav_worker_list to hasPermissionByWeight(3),
            R.id.nav_today_checkins to hasPermissionByWeight(3),
            
            R.id.nav_new_facility to hasPermissionByWeight(4),
            R.id.nav_facilities_list to hasPermissionByWeight(1),
            
            R.id.nav_inspection_config to hasPermissionByWeight(4),
            R.id.nav_current_inspection to hasPermissionByWeight(2),
            R.id.nav_current_download to hasPermissionByWeight(2),
            R.id.nav_previous_inspections to hasPermissionByWeight(1),
            R.id.nav_search_purger to true,
            
            R.id.nav_new_equipment to hasPermissionByWeight(4),
            R.id.nav_equipment_list to hasPermissionByWeight(1),
            
            R.id.nav_logout to hasPermissionByWeight(1)
        )
    }

    fun getNombreCompletoCorto(): String {
        // Separar nombres y apellidos
        val nombres = nombre.trim().split(" ")
        val apellidosList = apellidos.trim().split(" ")
        val primerNombre = nombres.getOrNull(0) ?: ""
        val segundoNombre = nombres.getOrNull(1)?.let { it.first().uppercaseChar().toString() + "." } ?: ""
        val primerApellido = apellidosList.getOrNull(0) ?: ""
        return if (segundoNombre.isNotEmpty())
            "$primerNombre $segundoNombre $primerApellido"
        else
            "$primerNombre $primerApellido"
    }

    fun guardarNombreCompletoCortoEnPrefs(context: android.content.Context) {
        val prefs = context.getSharedPreferences("bitherm_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("nombreCompletoCorto", getNombreCompletoCorto()).apply()
    }

    fun getRolPound(): Int {
        return when (rol.uppercase()) {
            "SUPERADMIN" -> 1
            "ADMIN" -> 2
            "INSPECTOR" -> 3
            "JEFEQUIPO" -> 5
            "VIEWER" -> 7
            "CLIENTE" -> 9
            "RESERVA" -> 11
            else -> 99 // Valor por defecto para roles desconocidos
        }
    }
} 