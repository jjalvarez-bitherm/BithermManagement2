package com.bithermmanagement.multimedia

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object MiniGaleriaUtils {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generarNombreFoto(tag: String, tipo: String): String {
        val fecha = dateFormat.format(Date())
        return "${tag}_${tipo}_$fecha.jpg"
    }

    fun getFotoFile(context: Context, tag: String, tipo: String): File {
        val dir = File(context.filesDir, "fotos")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, generarNombreFoto(tag, tipo))
    }

    fun borrarFoto(path: String): Boolean {
        val file = File(path)
        return file.exists() && file.delete()
    }
} 