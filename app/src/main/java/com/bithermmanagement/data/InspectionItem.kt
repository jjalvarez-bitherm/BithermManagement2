package com.bithermmanagement.data

data class InspectionItem(
    val id: String,
    var tag: String,
    var description: String,
    var location: String,
    var status: String,
    var brand: String,
    var model: String,
    var notes: String,
    var gpsLocation: String? = null,
    var photoPath: String? = null,
    var additionalPhotos: List<String> = emptyList(),
    var isPrimaryPhoto: Boolean = false,
    var isModified: Boolean = false,
    var lastInspectionDate: String? = null,
    var pressure: String? = null,
    var temperature: String? = null,
    var area: String? = null,
    var unit: String? = null
) {
    fun addPhoto(photoPath: String, isPrimary: Boolean = false) {
        if (isPrimary) {
            // Si es foto principal, mover la anterior a adicionales
            this.photoPath?.let { 
                if (it.isNotEmpty()) {
                    additionalPhotos = additionalPhotos + it
                }
            }
            this.photoPath = photoPath
            this.isPrimaryPhoto = true
        } else {
            // Si no es principal, agregar a adicionales
            additionalPhotos = additionalPhotos + photoPath
        }
        isModified = true
    }

    fun removePhoto(photoPath: String) {
        if (this.photoPath == photoPath) {
            // Si es la foto principal, mover la primera adicional a principal
            if (additionalPhotos.isNotEmpty()) {
                this.photoPath = additionalPhotos.first()
                additionalPhotos = additionalPhotos.drop(1)
                this.isPrimaryPhoto = true
            } else {
                this.photoPath = null
                this.isPrimaryPhoto = false
            }
        } else {
            // Si es una foto adicional, removerla
            additionalPhotos = additionalPhotos.filter { it != photoPath }
        }
        isModified = true
    }

    fun getAllPhotos(): List<String> {
        val allPhotos = mutableListOf<String>()
        photoPath?.let { if (it.isNotEmpty()) allPhotos.add(it) }
        allPhotos.addAll(additionalPhotos)
        return allPhotos
    }

    fun getStatusColor(): Int {
        return when (status.lowercase()) {
            "bien" -> android.graphics.Color.GREEN
            "fuera de servicio" -> android.graphics.Color.RED
            "no vapor" -> android.graphics.Color.YELLOW
            "baja temperatura" -> android.graphics.Color.CYAN
            "fuga continua" -> android.graphics.Color.RED
            "anulado" -> android.graphics.Color.BLACK
            "antes fugaba" -> android.graphics.Color.MAGENTA
            "no encontrado" -> android.graphics.Color.GRAY
            "inaccesible" -> android.graphics.Color.DKGRAY
            "mal instalado" -> android.graphics.Color.RED
            "ciclo corto" -> android.graphics.Color.YELLOW
            "falta purgador" -> android.graphics.Color.RED
            "fuga parcial" -> android.graphics.Color.YELLOW
            else -> android.graphics.Color.GRAY
        }
    }
} 