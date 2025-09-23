package com.bithermmanagement.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bithermmanagement.database.dao.UserDao
import com.bithermmanagement.database.entities.UserEntity
import com.bithermmanagement.database.DateConverter
import com.bithermmanagement.database.entities.Equipo
import com.bithermmanagement.database.dao.EquipoDao
import com.bithermmanagement.database.dao.InspeccionDao
import com.bithermmanagement.database.converters.Converters
import com.bithermmanagement.database.entities.MenuEntity
import com.bithermmanagement.database.entities.SubMenuEntity
import com.bithermmanagement.database.entities.FavoritoEntity
import com.bithermmanagement.database.entities.PermisoEntity
import com.bithermmanagement.database.dao.MenuDao
import com.bithermmanagement.database.dao.SubMenuDao
import com.bithermmanagement.database.dao.FavoritoDao
import com.bithermmanagement.database.dao.PermisoDao
import com.bithermmanagement.fichaje.dao.FichajeDao
import com.bithermmanagement.fichaje.models.FichajeEntity
import com.bithermmanagement.ausencias.dao.AbsenceRecordDao
import com.bithermmanagement.ausencias.dao.AbsenceRequestDao
import com.bithermmanagement.ausencias.dao.AbsenceLogDao
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceRequest
import com.bithermmanagement.ausencias.models.AbsenceLog
import com.bithermmanagement.database.entities.FotoEquipoEntity
import com.bithermmanagement.database.dao.FotoEquipoDao

@Database(
    entities = [
        UserEntity::class,
        Equipo::class,
        MenuEntity::class,
        SubMenuEntity::class,
        FavoritoEntity::class,
        PermisoEntity::class,
        FichajeEntity::class,
        AbsenceRecord::class,
        AbsenceRequest::class,
        AbsenceLog::class,
        FotoEquipoEntity::class
    ],
    version = 23,
    exportSchema = true
)
@TypeConverters(DateConverter::class, Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun equipoDao(): EquipoDao
    abstract fun inspeccionDao(): InspeccionDao
    abstract fun menuDao(): MenuDao
    abstract fun subMenuDao(): SubMenuDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun permisoDao(): PermisoDao
    abstract fun fichajeDao(): FichajeDao
    abstract fun absenceRecordDao(): AbsenceRecordDao
    abstract fun absenceRequestDao(): AbsenceRequestDao
    abstract fun absenceLogDao(): AbsenceLogDao
    abstract fun fotoEquipoDao(): FotoEquipoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Asegurar que la tabla equipos tenga todos los campos necesarios
                // Si la tabla inspecciones existe, migrar datos si es necesario
                try {
                    // Verificar si existe la tabla inspecciones (antigua)
                    val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='inspecciones'")
                    val hasInspeccionesTable = cursor.count > 0
                    cursor.close()
                    
                    if (hasInspeccionesTable) {
                        // Migrar datos de inspecciones a equipos si es necesario
                        // Por ahora, solo eliminamos la tabla antigua
                        database.execSQL("DROP TABLE IF EXISTS inspecciones")
                    }
                    
                    // Asegurar que la tabla equipos tenga todos los campos necesarios
                    // Los campos se crearán automáticamente si no existen
                } catch (e: Exception) {
                    // Si hay algún error, continuar
                }
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Agregar el campo periodicidad a la tabla equipos
                try {
                    database.execSQL("ALTER TABLE equipos ADD COLUMN periodicidad TEXT")
                } catch (e: Exception) {
                    // Si el campo ya existe, ignorar el error
                }
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar si la columna gps existe
                    val cursor = database.query("PRAGMA table_info(equipos)")
                    val columnNames = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    cursor.close()
                    
                    // Si existe la columna gps y no existe gps_coord, renombrarla
                    if (columnNames.contains("gps") && !columnNames.contains("gps_coord")) {
                        database.execSQL("ALTER TABLE equipos RENAME COLUMN gps TO gps_coord")
                    }
                    
                    // Agregar la nueva columna gps_acc si no existe
                    if (!columnNames.contains("gps_acc")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_acc TEXT")
                    }
                    
                    // Si no existe gps_coord, crearla
                    if (!columnNames.contains("gps_coord")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                    }
                } catch (e: Exception) {
                    // Si hay algún error, intentar crear las columnas desde cero
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                    } catch (e2: Exception) {
                        // Ignorar si ya existe
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_acc TEXT")
                    } catch (e2: Exception) {
                        // Ignorar si ya existe
                    }
                }
            }
        }

        val MIGRATION_14_16 = object : Migration(14, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar qué columnas existen actualmente
                    val cursor = database.query("PRAGMA table_info(equipos)")
                    val columnNames = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    cursor.close()
                    
                    // Agregar periodicidad si no existe (de la migración 14_15)
                    if (!columnNames.contains("periodicidad")) {
                        try {
                            database.execSQL("ALTER TABLE equipos ADD COLUMN periodicidad TEXT")
                        } catch (e: Exception) {
                            // Ignorar si ya existe
                        }
                    }
                    
                    // Manejar las columnas GPS (de la migración 15_16)
                    if (columnNames.contains("gps") && !columnNames.contains("gps_coord")) {
                        // Renombrar gps a gps_coord
                        try {
                            database.execSQL("ALTER TABLE equipos RENAME COLUMN gps TO gps_coord")
                        } catch (e: Exception) {
                            // Si falla el rename, crear la columna nueva
                            try {
                                database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                            } catch (e2: Exception) {
                                // Ignorar si ya existe
                            }
                        }
                    } else if (!columnNames.contains("gps_coord")) {
                        // Crear gps_coord si no existe
                        try {
                            database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                        } catch (e: Exception) {
                            // Ignorar si ya existe
                        }
                    }
                    
                    // Agregar gps_acc si no existe
                    if (!columnNames.contains("gps_acc")) {
                        try {
                            database.execSQL("ALTER TABLE equipos ADD COLUMN gps_acc TEXT")
                        } catch (e: Exception) {
                            // Ignorar si ya existe
                        }
                    }
                    
                } catch (e: Exception) {
                    // Si hay algún error general, intentar crear las columnas básicas
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN periodicidad TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_acc TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                }
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar si la columna modificado_local ya existe
                    val cursor = database.query("PRAGMA table_info(equipos)")
                    val columnNames = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    cursor.close()
                    
                    // Agregar la columna modificado_local si no existe
                    if (!columnNames.contains("modificado_local")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN modificado_local INTEGER DEFAULT 0")
                    }
                } catch (e: Exception) {
                    // Si hay algún error, intentar crear la columna
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN modificado_local INTEGER DEFAULT 0")
                    } catch (e2: Exception) {
                        // Ignorar si ya existe
                    }
                }
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar columnas existentes
                    val cursor = database.query("PRAGMA table_info(equipos)")
                    val columnNames = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    cursor.close()
                    
                    // Renombrar foto a url_foto_equipo si existe
                    if (columnNames.contains("foto") && !columnNames.contains("url_foto_equipo")) {
                        database.execSQL("ALTER TABLE equipos RENAME COLUMN foto TO url_foto_equipo")
                    } else if (!columnNames.contains("url_foto_equipo")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_equipo TEXT")
                    }
                    
                    // Renombrar fotoUbicacion a url_foto_ubicacion si existe
                    if (columnNames.contains("fotoUbicacion") && !columnNames.contains("url_foto_ubicacion")) {
                        database.execSQL("ALTER TABLE equipos RENAME COLUMN fotoUbicacion TO url_foto_ubicacion")
                    } else if (!columnNames.contains("url_foto_ubicacion")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_ubicacion TEXT")
                    }
                    
                    // Agregar nuevas columnas de fotos
                    if (!columnNames.contains("url_foto_manifold")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_manifold TEXT")
                    }
                    
                    if (!columnNames.contains("url_fotos_extra")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_fotos_extra TEXT")
                    }
                    
                    // Cambiar by_pass de TEXT a INTEGER (Boolean)
                    if (columnNames.contains("by_pass")) {
                        // Crear una columna temporal
                        database.execSQL("ALTER TABLE equipos ADD COLUMN by_pass_new INTEGER")
                        // Copiar datos convirtiendo "true"/"false" a 1/0
                        database.execSQL("UPDATE equipos SET by_pass_new = CASE WHEN by_pass = 'true' THEN 1 ELSE 0 END")
                        // Eliminar columna antigua
                        database.execSQL("ALTER TABLE equipos DROP COLUMN by_pass")
                        // Renombrar nueva columna
                        database.execSQL("ALTER TABLE equipos RENAME COLUMN by_pass_new TO by_pass")
                    }
                    
                } catch (e: Exception) {
                    // Si hay algún error, intentar crear las columnas básicas
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_equipo TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_ubicacion TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_manifold TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_fotos_extra TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                }
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar columnas existentes
                    val cursor = database.query("PRAGMA table_info(equipos)")
                    val columnNames = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columnNames.add(cursor.getString(1))
                    }
                    cursor.close()
                    
                    // Agregar columnas que faltan según la entidad actual
                    if (!columnNames.contains("periodicidad")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN periodicidad TEXT")
                    }
                    
                    if (!columnNames.contains("gps_coord")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                    }
                    
                    if (!columnNames.contains("gps_acc")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_acc TEXT")
                    }
                    
                    if (!columnNames.contains("modificado_local")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN modificado_local INTEGER DEFAULT 0")
                    }
                    
                    if (!columnNames.contains("instalacion_mf")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN instalacion_mf TEXT")
                    }
                    
                    if (!columnNames.contains("url_foto_manifold")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_manifold TEXT")
                    }
                    
                    if (!columnNames.contains("url_fotos_extra")) {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_fotos_extra TEXT")
                    }
                    
                    // Asegurar que by_pass sea INTEGER (Boolean)
                    if (columnNames.contains("by_pass")) {
                        val typeCursor = database.query("PRAGMA table_info(equipos)")
                        var byPassType = ""
                        while (typeCursor.moveToNext()) {
                            if (typeCursor.getString(1) == "by_pass") {
                                byPassType = typeCursor.getString(2)
                                break
                            }
                        }
                        typeCursor.close()
                        
                        if (byPassType != "INTEGER") {
                            // Crear una columna temporal
                            database.execSQL("ALTER TABLE equipos ADD COLUMN by_pass_new INTEGER")
                            // Copiar datos convirtiendo "true"/"false" a 1/0
                            database.execSQL("UPDATE equipos SET by_pass_new = CASE WHEN by_pass = 'true' THEN 1 ELSE 0 END")
                            // Eliminar columna antigua
                            database.execSQL("ALTER TABLE equipos DROP COLUMN by_pass")
                            // Renombrar nueva columna
                            database.execSQL("ALTER TABLE equipos RENAME COLUMN by_pass_new TO by_pass")
                        }
                    }
                    
                    // Eliminar columnas que ya no están en la entidad (si existen)
                    val columnsToRemove = listOf("p", "fuga_kg_h", "fechasteado", "inspector", "detector", "fotos", "by_pass_new")
                    for (column in columnsToRemove) {
                        if (columnNames.contains(column)) {
                            try {
                                database.execSQL("ALTER TABLE equipos DROP COLUMN $column")
                            } catch (e: Exception) {
                                // Ignorar si no se puede eliminar
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    // Si hay algún error, intentar crear las columnas básicas
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN periodicidad TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_coord TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN gps_acc TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN modificado_local INTEGER DEFAULT 0")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN instalacion_mf TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_foto_manifold TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                    try {
                        database.execSQL("ALTER TABLE equipos ADD COLUMN url_fotos_extra TEXT")
                    } catch (e2: Exception) {
                        // Ignorar
                    }
                }
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar si existe la tabla equipos
                    val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='equipos'")
                    val tableExists = cursor.count > 0
                    cursor.close()
                    
                    if (tableExists) {
                        // Eliminar la tabla problemática y dejar que Room la recree
                        database.execSQL("DROP TABLE equipos")
                    }
                    
                    // También eliminar cualquier tabla temporal que pueda existir
                    try {
                        database.execSQL("DROP TABLE IF EXISTS equipos_temp")
                    } catch (e: Exception) {
                        // Ignorar
                    }
                    
                } catch (e: Exception) {
                    // Si hay algún error, intentar eliminar la tabla de todas formas
                    try {
                        database.execSQL("DROP TABLE IF EXISTS equipos")
                    } catch (e2: Exception) {
                        // Si todo falla, continuar
                    }
                }
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Verificar si existe la tabla users
                    val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
                    val tableExists = cursor.count > 0
                    cursor.close()
                    
                    if (tableExists) {
                        // Verificar el tipo de la columna swWeb
                        val typeCursor = database.query("PRAGMA table_info(users)")
                        var swWebType = ""
                        var swWebNotNull = false
                        while (typeCursor.moveToNext()) {
                            if (typeCursor.getString(1) == "swWeb") {
                                swWebType = typeCursor.getString(2)
                                swWebNotNull = typeCursor.getInt(3) == 1
                                break
                            }
                        }
                        typeCursor.close()
                        
                        // Si swWeb es INTEGER (Boolean) o no es NOT NULL, cambiarlo
                        if (swWebType == "INTEGER" || !swWebNotNull) {
                            // Crear una columna temporal
                            database.execSQL("ALTER TABLE users ADD COLUMN swWeb_new TEXT NOT NULL DEFAULT ''")
                            // Copiar datos convirtiendo 1/0 a "true"/"false" o usar valor por defecto
                            if (swWebType == "INTEGER") {
                                database.execSQL("UPDATE users SET swWeb_new = CASE WHEN swWeb = 1 THEN 'true' ELSE 'false' END")
                            } else {
                                database.execSQL("UPDATE users SET swWeb_new = COALESCE(swWeb, '')")
                            }
                            // Eliminar columna antigua
                            database.execSQL("ALTER TABLE users DROP COLUMN swWeb")
                            // Renombrar nueva columna
                            database.execSQL("ALTER TABLE users RENAME COLUMN swWeb_new TO swWeb")
                        }
                    }
                    
                } catch (e: Exception) {
                    // Si hay algún error, intentar recrear la tabla
                    try {
                        database.execSQL("DROP TABLE IF EXISTS users")
                    } catch (e2: Exception) {
                        // Si todo falla, continuar
                    }
                }
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Esta migración crea las tablas de ausencias si no existen y luego los índices
                try {
                    // Verificar si la tabla absence_records existe
                    val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='absence_records'")
                    val hasAbsenceRecords = cursor.count > 0
                    cursor.close()
                    
                    if (!hasAbsenceRecords) {
                        // Crear tabla absence_records
                        database.execSQL("""
                            CREATE TABLE absence_records (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                employeeId TEXT NOT NULL,
                                employeeName TEXT NOT NULL,
                                employeeEmail TEXT NOT NULL,
                                startDate INTEGER NOT NULL,
                                endDate INTEGER NOT NULL,
                                totalDays INTEGER NOT NULL,
                                absenceType TEXT NOT NULL,
                                status TEXT NOT NULL,
                                description TEXT,
                                approvedBy TEXT,
                                approvedDate INTEGER,
                                rejectionReason TEXT,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                googleSheetsId TEXT,
                                isSynced INTEGER NOT NULL,
                                lastSyncAttempt INTEGER
                            )
                        """)
                    }
                    
                    // Verificar si la tabla absence_requests existe
                    val cursor2 = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='absence_requests'")
                    val hasAbsenceRequests = cursor2.count > 0
                    cursor2.close()
                    
                    if (!hasAbsenceRequests) {
                        // Crear tabla absence_requests
                        database.execSQL("""
                            CREATE TABLE absence_requests (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                employeeId TEXT NOT NULL,
                                employeeName TEXT NOT NULL,
                                employeeEmail TEXT NOT NULL,
                                startDate INTEGER NOT NULL,
                                endDate INTEGER NOT NULL,
                                totalDays INTEGER NOT NULL,
                                absenceType TEXT NOT NULL,
                                priority TEXT NOT NULL,
                                description TEXT,
                                status TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL,
                                googleSheetsId TEXT,
                                isSynced INTEGER NOT NULL,
                                lastSyncAttempt INTEGER
                            )
                        """)
                    }
                    
                    // Verificar si la tabla absence_logs existe
                    val cursor3 = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='absence_logs'")
                    val hasAbsenceLogs = cursor3.count > 0
                    cursor3.close()
                    
                    if (!hasAbsenceLogs) {
                        // Crear tabla absence_logs
                        database.execSQL("""
                            CREATE TABLE absence_logs (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                absenceId TEXT NOT NULL,
                                action TEXT NOT NULL,
                                timestamp INTEGER NOT NULL,
                                userId TEXT NOT NULL,
                                userName TEXT NOT NULL,
                                details TEXT,
                                googleSheetsId TEXT
                            )
                        """)
                    }
                    
                    // Ahora crear los índices
                    try {
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_records_employee_id ON absence_records(employeeId)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_records_start_date ON absence_records(startDate)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_records_status ON absence_records(status)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_records_type ON absence_records(absenceType)")
                        
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_requests_employee_id ON absence_requests(employeeId)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_requests_status ON absence_requests(status)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_requests_priority ON absence_requests(priority)")
                        
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_logs_absence_id ON absence_logs(absenceId)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_logs_timestamp ON absence_logs(timestamp)")
                        database.execSQL("CREATE INDEX IF NOT EXISTS idx_absence_logs_action ON absence_logs(action)")
                    } catch (e: Exception) {
                        // Si hay algún error con los índices, continuar
                    }
                } catch (e: Exception) {
                    // Si hay algún error, continuar
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bitherm_database"
                )
                .fallbackToDestructiveMigration() // Esto eliminará la base de datos si hay problemas de migración
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetDatabase(context: Context) {
            INSTANCE?.close()
            context.deleteDatabase("bitherm_database")
            INSTANCE = null
        }

        fun clearDatabase(context: Context) {
            INSTANCE?.close()
            context.deleteDatabase("bitherm_database")
            INSTANCE = null
        }

        fun forceRecreateDatabase(context: Context) {
            INSTANCE?.close()
            context.deleteDatabase("bitherm_database")
            INSTANCE = null
            // Forzar la recreación inmediata
            getDatabase(context)
        }
    }
} 