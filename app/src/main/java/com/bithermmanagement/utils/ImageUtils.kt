package com.bithermmanagement.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    fun getNextPhotoNumber(context: Context, idEquipo: String, tipo: String): Int {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BithermManagement")
        if (!dir.exists()) dir.mkdirs()
        val files = dir.listFiles { file -> file.name.startsWith("${idEquipo}_${tipo}_") && file.name.endsWith(".jpg") }
        return (files?.mapNotNull { Regex("_(\\d{2})").find(it.name)?.groupValues?.get(1)?.toIntOrNull() }?.maxOrNull() ?: -1) + 1
    }

    fun createImageFile(context: Context, idEquipo: String, tipo: String, isFav: Boolean = false): File {
        val num = getNextPhotoNumber(context, idEquipo, tipo)
        val favSuffix = if (isFav) "_fav" else ""
        val fileName = "%s_%s_%02d%s.jpg".format(idEquipo, tipo, num, favSuffix)
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BithermManagement")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    fun renamePhotoAsFavorite(photoFile: File): File {
        val newName = if (!photoFile.name.contains("_fav")) photoFile.name.replace(".jpg", "_fav.jpg") else photoFile.name
        val newFile = File(photoFile.parent, newName)
        photoFile.renameTo(newFile)
        return newFile
    }

    fun removeFavoriteFromPhoto(photoFile: File): File {
        val newName = photoFile.name.replace("_fav.jpg", ".jpg")
        val newFile = File(photoFile.parent, newName)
        photoFile.renameTo(newFile)
        return newFile
    }

    fun saveBitmapToFile(context: Context, bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    fun loadBitmapFromFile(file: File): Bitmap? {
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    fun getAllImagesForEquipo(context: Context, idEquipo: String, tipo: String): List<String> {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "BithermManagement")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { file ->
            file.name.startsWith("${idEquipo}_${tipo}_") && file.name.endsWith(".jpg")
        }?.map { it.absolutePath }?.sorted() ?: emptyList()
    }
} 