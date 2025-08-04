package com.bithermmanagement.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bithermmanagement.core.database.entities.InspeccionEntity

@Database(entities = [MenuEntity::class, InspeccionEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun menuDao(): MenuDao
    abstract fun inspeccionDao(): InspeccionDao
} 