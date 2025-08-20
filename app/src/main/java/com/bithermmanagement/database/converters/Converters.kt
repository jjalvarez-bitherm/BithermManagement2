package com.bithermmanagement.database.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.models.AbsenceStatus

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromList(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    // Conversores para AbsenceType
    @TypeConverter
    fun fromAbsenceType(value: AbsenceType?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAbsenceType(value: String?): AbsenceType? {
        return value?.let { AbsenceType.valueOf(it) }
    }

    // Conversores para AbsenceStatus
    @TypeConverter
    fun fromAbsenceStatus(value: AbsenceStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toAbsenceStatus(value: String?): AbsenceStatus? {
        return value?.let { AbsenceStatus.valueOf(it) }
    }
} 