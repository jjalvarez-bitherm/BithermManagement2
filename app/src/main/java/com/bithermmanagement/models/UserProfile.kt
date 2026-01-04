package com.bithermmanagement.models

import java.util.Date

data class UserProfile(
    val id: String = "",
    val nombre: String = "",
    val edad: Int = 0,
    val sexo: String = "", // "Masculino", "Femenino", "Otro"
    val fotos: List<String> = emptyList(),
    val bio: String = "",
    val ocupacion: String = "",
    val estudios: String = "",
    val hobbies: List<String> = emptyList(),
    val idiomas: List<String> = emptyList(),
    val altura: Int = 0, // en cm
    val signoZodiacal: String = "",
    val intereses: List<String> = emptyList(),
    val queBusca: String = "", // "Amistad", "Relación seria", "Cita casual", "Networking"
    val fechaCreacion: Date = Date(),
    val fechaUltimaActualizacion: Date = Date()
)

data class VisibilitySettings(
    val nombreVisible: Boolean = false,
    val edadVisible: Boolean = true, // Obligatorio
    val sexoVisible: Boolean = true, // Obligatorio
    val fotosVisible: Boolean = false,
    val bioVisible: Boolean = false,
    val ocupacionVisible: Boolean = false,
    val estudiosVisible: Boolean = false,
    val hobbiesVisible: Boolean = false,
    val idiomasVisible: Boolean = false,
    val alturaVisible: Boolean = false,
    val signoZodiacalVisible: Boolean = false,
    val interesesVisible: Boolean = true, // Obligatorio
    val queBuscaVisible: Boolean = true // Obligatorio
)

data class UserPreferences(
    val rangoEdadMin: Int = 18,
    val rangoEdadMax: Int = 100,
    val distanciaMaxima: Int = 50, // en km
    val soloVerVerificados: Boolean = false,
    val notificacionesPush: Boolean = true,
    val notificacionesEmail: Boolean = true,
    val modoOscuro: Boolean = false
)
