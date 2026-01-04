package com.bithermmanagement.data

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ValueRange
import com.bithermmanagement.database.AppDatabase
import com.bithermmanagement.database.entities.UserEntity
import com.bithermmanagement.database.entities.Equipo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

class GoogleSheetsManager(
    private val authAdapter: GoogleAuthAdapter,
    private val context: Context
) {
    private val TAG = "GoogleSheetsManager"
    private val spreadsheetId = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
    private val range = "TRABAJADORES!A2:T"  // Cambiado a T (20 columnas)
    private val sheetsService: Sheets
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    init {
        Log.d(TAG, "GoogleSheetsManager.init: Inicializando con GoogleAuthAdapter...")
        
        // Usar el adapter para crear el servicio de Sheets
        sheetsService = authAdapter.createSheetsService()
        Log.d(TAG, "GoogleSheetsManager.init: Inicialización completada exitosamente")
    }

    val sheetsServicePublic: Sheets get() = sheetsService
    val spreadsheetIdPublic: String get() = spreadsheetId

    suspend fun getUserData(app: String, pass: String): UserData? = withContext(Dispatchers.IO) {
        var attempts = 0
        val maxAttempts = 3
        val delayMs = 2000L // 2 segundos entre intentos
        
        while (attempts < maxAttempts) {
            try {
                attempts++
                Log.d(TAG, "=== INICIO PROCESO DE LOGIN (Intento $attempts/$maxAttempts) ===")
                Log.d(TAG, "Usuario: $app, Contraseña: $pass")
                
                Log.d(TAG, "1. Obteniendo cabecera de la hoja...")
                Log.d(TAG, "   Spreadsheet ID: $spreadsheetId")
                Log.d(TAG, "   Sheets Service: ${sheetsService != null}")
                
                val headerResponse = try {
                    sheetsService.spreadsheets().values()
                        .get(spreadsheetId, "TRABAJADORES!A2:2")
                        .execute()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al obtener cabecera: ${e.message}")
                    Log.e(TAG, "Stack trace: ${e.stackTraceToString()}")
                    throw e
                }
                Log.d(TAG, "2. Respuesta de cabecera obtenida exitosamente")
                
                val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()
                Log.d(TAG, "3. Cabecera obtenida: ${headerRow.joinToString(", ")}")
                
                // Encontrar la última columna no vacía de forma segura
                var lastColIdx = 0
                for (i in headerRow.indices) {
                    if (headerRow[i].toString().isNotBlank()) {
                        lastColIdx = i
                        Log.d(TAG, "4. Columna no vacía encontrada en índice: $i, valor: ${headerRow[i]}")
                    }
                }
                
                // Convertir el índice a letra de columna
                val lastColLetter = if (lastColIdx < 26) {
                    ('A'.code + lastColIdx).toChar().toString()
                } else {
                    val firstChar = ('A'.code + (lastColIdx / 26 - 1)).toChar()
                    val secondChar = ('A'.code + (lastColIdx % 26)).toChar()
                    "$firstChar$secondChar"
                }
                
                Log.d(TAG, "5. Última columna encontrada: $lastColLetter (índice: $lastColIdx)")
                
                // Construir rangos
                val headerRange = "TRABAJADORES!A2:${lastColLetter}2"
                val dataRange = "TRABAJADORES!A3:$lastColLetter"
                Log.d(TAG, "6. Rangos construidos - Header: $headerRange, Data: $dataRange")

                Log.d(TAG, "7. Obteniendo datos de la hoja...")
                val response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, dataRange)
                    .execute()
                val values = response.getValues()
                
                Log.d(TAG, "8. Datos obtenidos: ${values?.size ?: 0} filas")
                
                if (values == null || values.isEmpty()) {
                    Log.e(TAG, "9. No se encontraron datos en la hoja")
                    return@withContext null
                }

                Log.d(TAG, "10. Buscando usuario en los datos...")
                for (row in values) {
                    val usernameIdx = headerRow.indexOf("APP")
                    val passwordIdx = headerRow.indexOf("PASS")
                    
                    Log.d(TAG, "11. Índices encontrados - Usuario: $usernameIdx, Contraseña: $passwordIdx")
                    
                    if (usernameIdx != -1 && passwordIdx != -1 && row.size > maxOf(usernameIdx, passwordIdx)) {
                        val username = row[usernameIdx].toString().trim()
                        val password = row[passwordIdx].toString().trim()
                        
                        Log.d(TAG, "12. Comparando - Usuario encontrado: $username, Contraseña encontrada: $password")
                        
                        if (username == app && password == pass) {
                            Log.d(TAG, "13. ¡Usuario encontrado! Creando UserData...")
                            
                            // Utilizar nombres de columna para obtener los datos
                            fun getStr(col: String) = headerRow.indexOf(col).let { idx -> 
                                if (idx != -1 && row.size > idx) row[idx].toString() else ""
                            }
                            
                            val userEntity = UserEntity(
                                cod = getStr(ColumnMapping.COD),
                                nombre = getStr(ColumnMapping.NOMBRE),
                                apellidos = getStr(ColumnMapping.APELLIDOS),
                                dni = getStr(ColumnMapping.DNI),
                                fechaNacimiento = try { dateFormat.parse(getStr(ColumnMapping.FECHA_NAC)) } catch (e: Exception) { null },
                                app = username,
                                password = password,
                                rol = getStr(ColumnMapping.ROL),
                                swWeb = getStr(ColumnMapping.SW_WEB).takeIf { it.isNotEmpty() } ?: "",
                                equipoAsignado = getStr(ColumnMapping.EQUIPO_ASIGN).takeIf { it.isNotEmpty() },
                                fechaCalibracion = try { dateFormat.parse(getStr(ColumnMapping.FECHA_CAL)) } catch (e: Exception) { null },
                                telefonoEmpresa = getStr(ColumnMapping.TELEF_EMPR).takeIf { it.isNotEmpty() },
                                emailEmpresa = getStr(ColumnMapping.EMAIL_EMPR).takeIf { it.isNotEmpty() },
                                altaEmpresa = try { dateFormat.parse(getStr(ColumnMapping.ALTA_EMPR)) } catch (e: Exception) { null },
                                telefonoPersonal = getStr(ColumnMapping.TELEF_PERSONAL).takeIf { it.isNotEmpty() },
                                emailPersonal = getStr(ColumnMapping.EMAIL_PERSONAL).takeIf { it.isNotEmpty() },
                                categoria = getStr(ColumnMapping.CATEGORIA).takeIf { it.isNotEmpty() },
                                rMedico = getStr(ColumnMapping.R_MEDICO).equals("true", ignoreCase = true),
                                accesoRLR = getStr(ColumnMapping.ACCESO_RLR).equals("true", ignoreCase = true),
                                supEjec = getStr(ColumnMapping.SUP_EJEC).equals("true", ignoreCase = true)
                            )
                            
                            Log.d(TAG, "14. UserEntity creado: ${userEntity.toString()}")

                            // Guardar en la base de datos local
                            Log.d(TAG, "15. Guardando en base de datos local...")
                            val database = AppDatabase.getDatabase(context)
                            database.userDao().insertUser(userEntity)
                            Log.d(TAG, "16. Usuario guardado en base de datos local")

                            // Crear y retornar UserData
                            val userData = UserData(
                                codigo = userEntity.cod,
                                nombre = userEntity.nombre,
                                apellidos = userEntity.apellidos,
                                dni = userEntity.dni,
                                fechaNacimiento = userEntity.fechaNacimiento?.let { dateFormat.format(it) } ?: "",
                                app = userEntity.app,
                                pass = userEntity.password,
                                rol = userEntity.rol,
                                swWeb = userEntity.swWeb,
                                equipoAsignado = userEntity.equipoAsignado ?: "",
                                fechaCalibracion = userEntity.fechaCalibracion?.let { dateFormat.format(it) } ?: "",
                                telefonoEmpresa = userEntity.telefonoEmpresa ?: "",
                                emailEmpresa = userEntity.emailEmpresa ?: "",
                                fechaAltaEmpresa = userEntity.altaEmpresa?.let { dateFormat.format(it) } ?: "",
                                telefonoPersonal = userEntity.telefonoPersonal ?: "",
                                emailPersonal = userEntity.emailPersonal ?: "",
                                categoria = userEntity.categoria ?: "",
                                revisionMedica = userEntity.rMedico.toString(),
                                accesoRLR = userEntity.accesoRLR,
                                supervisorEjecutivo = userEntity.supEjec,
                                apodo = getStr("APODO")
                            )
                            
                            Log.d(TAG, "17. UserData creado: ${userData.toString()}")
                            Log.d(TAG, "=== FIN PROCESO DE LOGIN EXITOSO ===")
                            return@withContext userData
                        }
                    }
                }
                
                Log.e(TAG, "18. Usuario no encontrado en los datos")
                Log.d(TAG, "=== FIN PROCESO DE LOGIN FALLIDO ===")
                return@withContext null
                
            } catch (e: Exception) {
                Log.e(TAG, "ERROR durante el inicio de sesión (Intento $attempts/$maxAttempts)", e)
                
                // Verificar si es un error temporal que se puede reintentar
                val isRetryableError = when {
                    e.message?.contains("503") == true -> true
                    e.message?.contains("Service Unavailable") == true -> true
                    e.message?.contains("backendError") == true -> true
                    e.message?.contains("quota") == true -> true
                    else -> false
                }
                
                if (isRetryableError && attempts < maxAttempts) {
                    Log.d(TAG, "Error temporal detectado. Reintentando en ${delayMs}ms...")
                    kotlinx.coroutines.delay(delayMs)
                    continue
                } else {
                    Log.d(TAG, "=== FIN PROCESO DE LOGIN CON ERROR ===")
                    return@withContext null
                }
            }
        }
        
        Log.e(TAG, "Se agotaron todos los intentos de conexión")
        return@withContext null
    }

    /**
     * Lee la pestaña G.MENU y devuelve la estructura de menús, submenús, colores, roles y favoritos filtrados por el rol del usuario.
     * @param rolPoundUsuario El valor de rolPound del usuario logueado.
     * @param usuarioLogin El nombre de usuario para obtener sus favoritos.
     */
    suspend fun getMenuStructureForUser(rolPoundUsuario: Int, usuarioLogin: String): MenuStructureForUser? = withContext(Dispatchers.IO) {
        try {
            val menuRange = "G.MENU!A1:AB100" // Ajusta el rango según el tamaño de tu hoja
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, menuRange)
                .execute()
            val values = response.getValues() ?: return@withContext null

            // Mapear cabecera
            val header = values.first().map { it.toString() }
            val rows = values.drop(1)

            // Estructuras para el menú principal, submenús y favoritos
            val mainMenus = mutableListOf<MainMenuItem>()
            val subMenus = mutableListOf<SubMenuItem>()
            val favoritos = mutableListOf<SubMenuItem>()

            // Índice de la columna de favoritos para el usuario de login (APP)
            Log.d(TAG, "Buscando columna de favoritos para usuarioLogin: $usuarioLogin")
            val favColIdx = header.indexOfFirst { it.equals(usuarioLogin, ignoreCase = true) }
            Log.d(TAG, "Índice de columna de favoritos encontrado: $favColIdx, header: ${header.joinToString()}")

            // Solo procesar filas 2 a 19 (índices 1 a 18)
            val mainMenuRows = rows.take(18)
            for (row in mainMenuRows) {
                // Menú principal
                val fragment = row.getOrNull(0)?.toString() ?: continue
                val nombreVisible = row.getOrNull(1)?.toString() ?: fragment
                val index = row.getOrNull(2)?.toString()?.toIntOrNull() ?: continue
                val color = row.getOrNull(3)?.toString() ?: ""
                if (fragment.isNotBlank()) {
                    mainMenus.add(MainMenuItem(fragment, index, color, nombreVisible))
                }
            }
            // Procesar submenús y favoritos como antes
            for (row in rows) {
                val menuPrincipal = row.getOrNull(5)?.toString() ?: ""
                val subItem = row.getOrNull(6)?.toString() ?: ""
                val nombreVisible = row.getOrNull(7)?.toString() ?: subItem
                val rol = row.getOrNull(8)?.toString()?.toIntOrNull() ?: 0
                val colorCard = row.getOrNull(9)?.toString() ?: ""
                if (menuPrincipal.isNotBlank() && subItem.isNotBlank()) {
                    if (rol != 0 && rolPoundUsuario != 0 && rol % rolPoundUsuario == 0) {
                        val subMenuItem = SubMenuItem(menuPrincipal, subItem, rol, colorCard, nombreVisible)
                        subMenus.add(subMenuItem)
                        if (favColIdx != -1) {
                            val fav = row.getOrNull(favColIdx)?.toString()
                            Log.d(TAG, "Fila: ${row.joinToString()} | Favorito: $fav")
                            if (fav == "1") {
                                favoritos.add(subMenuItem)
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "Favoritos finales: ${favoritos.map { it.nombreVisible }}")
            return@withContext MenuStructureForUser(mainMenus, subMenus, favoritos)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo G.MENU", e)
            return@withContext null
        }
    }

    suspend fun getUserRowByApp(app: String): Pair<List<Any>, List<Any>?> = withContext(Dispatchers.IO) {
        try {
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "TRABAJADORES!A2:2")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()
            var lastColIdx = 0
            for (i in headerRow.indices) {
                if (headerRow[i].toString().isNotBlank()) {
                    lastColIdx = i
                }
            }
            val lastColLetter = if (lastColIdx < 26) {
                ('A'.code + lastColIdx).toChar().toString()
            } else {
                val firstChar = ('A'.code + (lastColIdx / 26 - 1)).toChar()
                val secondChar = ('A'.code + (lastColIdx % 26)).toChar()
                "$firstChar$secondChar"
            }
            val dataRange = "TRABAJADORES!A3:$lastColLetter"
            val dataResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, dataRange)
                .execute()
            val dataRows = dataResponse.getValues()
            val idxApp = headerRow.indexOf("APP")
            val filaUsuario = dataRows?.firstOrNull { it.size > idxApp && it[idxApp].toString() == app }
            return@withContext Pair(headerRow, filaUsuario)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener la fila del usuario por APP", e)
            return@withContext Pair(emptyList(), null)
        }
    }

    suspend fun getUserProfileDataByApp(app: String): Map<String, List<Triple<String, String, Int>>> = withContext(Dispatchers.IO) {
        val datosPorGrupo = mutableMapOf<String, MutableList<Triple<String, String, Int>>>()
        try {
            // Leer dos filas de cabecera: fila 1 (grupo/card), fila 2 (campo)
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "TRABAJADORES!A1:Z2")
                .execute()
            val headerRows = headerResponse.getValues()
            if (headerRows == null || headerRows.size < 2) {
                return@withContext emptyMap()
            }
            val grupos = headerRows[0].map { it.toString() }
            val campos = headerRows[1].map { it.toString() }

            // Buscar la fila del usuario
            val dataResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "TRABAJADORES!A3:Z")
                .execute()
            val dataRows = dataResponse.getValues()
            val idxApp = campos.indexOf("APP")
            val filaUsuario = dataRows?.firstOrNull { it.size > idxApp && it[idxApp].toString() == app }
            if (filaUsuario == null) {
                return@withContext emptyMap()
            }
            // Agrupar los datos por grupo (según fila 1)
            for (i in campos.indices) {
                val grupo = grupos.getOrNull(i) ?: "Datos Extra"
                val campo = campos.getOrNull(i) ?: ""
                val valor = filaUsuario.getOrNull(i)?.toString() ?: ""
                if (campo.isNotBlank()) {
                    datosPorGrupo.getOrPut(grupo) { mutableListOf() }.add(Triple(campo, valor, i))
                }
            }
            return@withContext datosPorGrupo
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener los datos de perfil por APP", e)
            return@withContext emptyMap()
        }
    }

    suspend fun updateUserCellByApp(app: String, campo: String, nuevoValor: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Intentando actualizar campo: $campo para usuario: $app con valor: $nuevoValor")
            // Leer cabeceras para encontrar columna y fila
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "TRABAJADORES!A2:2")
                .execute()
            val campos = headerResponse.getValues()?.firstOrNull()?.map { it.toString() } ?: return@withContext false
            Log.d(TAG, "Cabeceras encontradas: ${campos.joinToString()}")
            // Búsqueda flexible de columna para el campo a editar
            val campoNorm = campo.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n")
            val colIdx = campos.indexOfFirst {
                it.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n") == campoNorm
            }
            Log.d(TAG, "Índice de columna encontrado: $colIdx para campo normalizado: $campoNorm")
            if (colIdx == -1) return@withContext false

            // Buscar la columna APP dinámicamente
            val appColIdx = campos.indexOfFirst {
                it.trim().lowercase().replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ñ", "n") == "app"
            }
            Log.d(TAG, "Índice de columna APP encontrado: $appColIdx")
            if (appColIdx == -1) return@withContext false

            // Obtener todas las filas de datos
            val dataResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "TRABAJADORES!A3:Z")
                .execute()
            val dataRows = dataResponse.getValues() ?: return@withContext false
            // Buscar la fila donde la columna APP coincide con el usuario
            val rowIdx = dataRows.indexOfFirst { it.size > appColIdx && it[appColIdx].toString() == app }
            Log.d(TAG, "Índice de fila encontrado: $rowIdx para usuario: $app")
            if (rowIdx == -1) return@withContext false

            // Calcular la letra de la columna
            val colLetter = if (colIdx < 26) {
                ('A'.code + colIdx).toChar().toString()
            } else {
                val firstChar = ('A'.code + (colIdx / 26 - 1)).toChar()
                val secondChar = ('A'.code + (colIdx % 26)).toChar()
                "$firstChar$secondChar"
            }
            val cell = "TRABAJADORES!$colLetter${3 + rowIdx}"
            Log.d(TAG, "Celda a actualizar: $cell")
            val valueRange = com.google.api.services.sheets.v4.model.ValueRange().setValues(listOf(listOf(nuevoValor)))
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, cell, valueRange)
                .setValueInputOption("RAW")
                .execute()
            Log.d(TAG, "Actualización exitosa en Google Sheets")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando celda en Google Sheets", e)
            return@withContext false
        }
    }

    suspend fun listarHojas(spreadsheetId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute()
            return@withContext spreadsheet.sheets.mapNotNull { it.properties?.title }
        } catch (e: Exception) {
            Log.e(TAG, "Error al listar hojas", e)
            return@withContext emptyList()
        }
    }

    suspend fun leerCabecera(spreadsheetId: String, sheetName: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val range = "$sheetName!1:1"
            val response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
            return@withContext response.getValues()?.firstOrNull()?.map { it.toString() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error al leer cabecera", e)
            return@withContext emptyList()
        }
    }
    
    suspend fun leerRango(spreadsheetId: String, range: String): List<List<Any?>> = withContext(Dispatchers.IO) {
        var attempts = 0
        val maxAttempts = 3
        val delayMs = 2000L // 2 segundos entre intentos
        
        while (attempts < maxAttempts) {
            try {
                Log.d(TAG, "Leyendo rango $range (intento ${attempts + 1}/$maxAttempts)")
                val response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
                Log.d(TAG, "Rango $range leído exitosamente")
                return@withContext response.getValues() ?: emptyList()
            } catch (e: Exception) {
                attempts++
                Log.e(TAG, "Error al leer rango $range (intento $attempts/$maxAttempts): ${e.message}")
                
                // Verificar si es un error temporal que se puede reintentar
                val isRetryableError = when {
                    e.message?.contains("timeout") == true -> true
                    e.message?.contains("SocketTimeoutException") == true -> true
                    e.message?.contains("Socket closed") == true -> true
                    e.message?.contains("503") == true -> true
                    e.message?.contains("Service Unavailable") == true -> true
                    e.message?.contains("backendError") == true -> true
                    else -> false
                }
                
                if (isRetryableError && attempts < maxAttempts) {
                    Log.d(TAG, "Error temporal detectado. Reintentando en ${delayMs}ms...")
                    kotlinx.coroutines.delay(delayMs)
                    continue
                } else {
                    Log.e(TAG, "Error final al leer rango $range", e)
                    return@withContext emptyList()
                }
            }
        }
        
        Log.e(TAG, "Se agotaron todos los intentos para leer rango $range")
        return@withContext emptyList()
    }
    
    suspend fun escribirRango(spreadsheetId: String, range: String, values: List<List<Any?>>): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Escribiendo en rango: $range de spreadsheet: $spreadsheetId")
            Log.d(TAG, "Valores a escribir: $values")
            
            val valueRange = ValueRange().setValues(values)
            
            sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, valueRange)
                .setValueInputOption("RAW")
                .execute()
            
            // Debug de escritura: registrar la escritura con color aleatorio
            if (com.bithermmanagement.utils.DebugEscrituraManager.isDebugEscrituraEnabled(context)) {
                val valorString = values.joinToString(" | ") { fila -> fila.joinToString(", ") }
                com.bithermmanagement.utils.DebugEscrituraManager.registrarEscritura(
                    context, spreadsheetId, range, valorString
                )
            }
            
            Log.d(TAG, "Datos escritos exitosamente en $range")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error escribiendo en rango $range: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Limpia todos los colores de debug de un spreadsheet
     */
    suspend fun limpiarColoresDebug(spreadsheetId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!com.bithermmanagement.utils.DebugEscrituraManager.isDebugEscrituraEnabled(context)) {
                Log.d(TAG, "Debug de escritura deshabilitado, no se limpian colores")
                return@withContext true
            }
            
            Log.d(TAG, "Limpiando colores de debug del spreadsheet: $spreadsheetId")
            
            // TODO: Implementar la limpieza real de colores usando Google Sheets API
            // Por ahora solo loggeamos la acción
            
            Log.d(TAG, "Colores de debug limpiados exitosamente")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error limpiando colores de debug: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getAllUsers(): Pair<List<Any>, List<List<Any>>> = withContext(Dispatchers.IO) {
        try {
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "TRABAJADORES!A2:2")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()
            
            var lastColIdx = 0
            for (i in headerRow.indices) {
                if (headerRow[i].toString().isNotBlank()) {
                    lastColIdx = i
                }
            }
            
            val lastColLetter = if (lastColIdx < 26) {
                ('A'.code + lastColIdx).toChar().toString()
            } else {
                val firstChar = ('A'.code + (lastColIdx / 26 - 1)).toChar()
                val secondChar = ('A'.code + (lastColIdx % 26)).toChar()
                "$firstChar$secondChar"
            }
            
            val dataRange = "TRABAJADORES!A3:$lastColLetter"
            val dataResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, dataRange)
                .execute()
            val dataRows = dataResponse.getValues() ?: emptyList()
            
            return@withContext Pair(headerRow, dataRows)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener todos los usuarios", e)
            return@withContext Pair(emptyList(), emptyList())
        }
    }

    /**
     * Sincroniza equipos modificados desde la base de datos local a Google Sheets
     */
    suspend fun sincronizarEquiposModificados(equipos: List<Equipo>): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== INICIO SINCRONIZACIÓN DE EQUIPOS MODIFICADOS A GOOGLE SHEETS ===")
            Log.d(TAG, "Equipos a sincronizar: ${equipos.size}")
            
            if (equipos.isEmpty()) {
                Log.d(TAG, "No hay equipos para sincronizar")
                return@withContext true
            }
            
            // Leer cabeceras de la hoja de equipos
            Log.d(TAG, "Leyendo cabeceras de EQUIPOS!A1:Z1...")
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "EQUIPOS!A1:Z1")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()
            Log.d(TAG, "Cabeceras encontradas: ${headerRow.joinToString(", ")}")
            
            if (headerRow.isEmpty()) {
                Log.e(TAG, "No se encontraron cabeceras en la hoja EQUIPOS")
                return@withContext false
            }
            
            // Crear mapeo de columnas
            val columnMapping = createColumnMapping(headerRow)
            Log.d(TAG, "Mapeo de columnas creado: $columnMapping")
            
            // Procesar cada equipo modificado
            var equiposActualizados = 0
            var equiposConError = 0
            
            for (equipo in equipos) {
                try {
                    Log.d(TAG, "Procesando equipo: ${equipo.id}")
                    
                    // Buscar la fila del equipo en Google Sheets
                    val filaEquipo = buscarFilaEquipo(equipo.id)
                    
                    if (filaEquipo != null) {
                        // Actualizar fila existente
                        val actualizado = actualizarFilaEquipo(equipo, filaEquipo, columnMapping)
                        if (actualizado) {
                            equiposActualizados++
                            Log.d(TAG, "Equipo ${equipo.id} actualizado exitosamente")
                        } else {
                            equiposConError++
                            Log.e(TAG, "Error al actualizar equipo ${equipo.id}")
                        }
                    } else {
                        // Crear nueva fila (si es necesario)
                        Log.w(TAG, "Equipo ${equipo.id} no encontrado en Google Sheets, saltando...")
                        equiposConError++
                    }
                    
                } catch (e: Exception) {
                    equiposConError++
                    Log.e(TAG, "Error procesando equipo ${equipo.id}: ${e.message}", e)
                }
            }
            
            Log.d(TAG, "=== SINCRONIZACIÓN COMPLETADA ===")
            Log.d(TAG, "Equipos actualizados: $equiposActualizados")
            Log.d(TAG, "Equipos con error: $equiposConError")
            
            equiposConError == 0
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en sincronización de equipos modificados: ${e.message}", e)
            false
        }
    }
    
    /**
     * Crea el mapeo de columnas basado en las cabeceras de Google Sheets
     */
    private fun createColumnMapping(headerRow: List<Any>): Map<String, Int> {
        val mapping = mutableMapOf<String, Int>()
        
        headerRow.forEachIndexed { index, header ->
            val headerStr = header.toString().trim()
            mapping[headerStr] = index
        }
        
        return mapping
    }
    
    /**
     * Busca la fila de un equipo en Google Sheets
     */
    private suspend fun buscarFilaEquipo(equipoId: String): Int? = withContext(Dispatchers.IO) {
        try {
            // Leer todas las filas de datos
            val dataResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "EQUIPOS!A2:Z")
                .execute()
            val dataRows = dataResponse.getValues() ?: return@withContext null
            
            // Buscar la fila con el ID del equipo
            dataRows.forEachIndexed { index, row ->
                if (row.isNotEmpty() && row[0].toString().trim() == equipoId) {
                    return@withContext index + 2 // +2 porque empezamos desde la fila 2 (después de cabeceras)
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando fila del equipo $equipoId: ${e.message}", e)
            null
        }
    }
    
    /**
     * Actualiza una fila específica de un equipo en Google Sheets
     */
    private suspend fun actualizarFilaEquipo(
        equipo: Equipo, 
        fila: Int, 
        columnMapping: Map<String, Int>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando fila $fila para equipo ${equipo.id}")
            
            // Crear lista de valores a actualizar
            val values = mutableListOf<Any>()
            
            // Mapear campos del equipo a columnas de Google Sheets
            val camposActualizar = mapOf(
                EquipoColumnMapping.ESTADO to equipo.estado,
                EquipoColumnMapping.FECHA_ESTADO to equipo.fechaInspeccion,
                EquipoColumnMapping.IDENTIDAD_INSPECTOR to equipo.identidadInspector,
                EquipoColumnMapping.DETECTOR_UTILIZADO to equipo.detectorUtilizado,
                EquipoColumnMapping.UBICACION to equipo.ubicacion,
                EquipoColumnMapping.NOTA to equipo.nota,
                EquipoColumnMapping.INCIDENCIAS to equipo.incidencias,
                EquipoColumnMapping.GPS_COORD to equipo.gpsCoord,
                EquipoColumnMapping.GPS_ACC to equipo.gpsAcc,
                EquipoColumnMapping.FOTO to equipo.urlFotoEquipo,
                EquipoColumnMapping.FOTO_UBIC to equipo.urlFotoUbicacion,
                EquipoColumnMapping.FOTO_MF to equipo.urlFotoManifold,
                EquipoColumnMapping.FOTO_EXTRA to equipo.urlFotosExtra,
                EquipoColumnMapping.INSTALACION_TYPE to equipo.instalacion,
                EquipoColumnMapping.INSTALACION_LINE to equipo.linea,
                EquipoColumnMapping.INSTALACION_MF to equipo.instalacionMf,
                EquipoColumnMapping.AISLAMIENTO to equipo.aislamiento,
                EquipoColumnMapping.PERIODICIDAD to equipo.periodicidad,
                EquipoColumnMapping.BY_PASS to if (equipo.byPass == true) "true" else "false"
            )
            
            // Crear rango de actualización
            val range = "EQUIPOS!A$fila:Z$fila"
            val updateValues = mutableListOf<List<Any>>()
            val rowValues = mutableListOf<Any>()
            
            // Llenar la fila con valores vacíos primero
            repeat(26) { rowValues.add("") }
            
            // Actualizar solo los campos que tienen valores
            camposActualizar.forEach { (campo, valor) ->
                val colIndex = columnMapping[campo]
                if (colIndex != null && valor != null && valor.isNotEmpty()) {
                    if (colIndex < rowValues.size) {
                        rowValues[colIndex] = valor
                        Log.d(TAG, "Campo $campo (col $colIndex) = $valor")
                    }
                }
            }
            
            updateValues.add(rowValues)
            
            // Ejecutar actualización
            val body = ValueRange().setValues(updateValues)
            val result = sheetsService.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute()
            
            Log.d(TAG, "Fila $fila actualizada exitosamente")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando fila $fila: ${e.message}", e)
            false
        }
    }

    /**
     * Sincroniza equipos desde Google Sheets a la base de datos local
     */
    suspend fun sincronizarEquipos(campoOrdenSeleccionado: String? = null): Boolean = withContext(Dispatchers.IO) {
        var resultado = false
        try {
            Log.d(TAG, "=== INICIO SINCRONIZACIÓN DE EQUIPOS ===")
            Log.d(TAG, "Spreadsheet ID: $spreadsheetId")
            Log.d(TAG, "Campo de orden recibido: $campoOrdenSeleccionado")
            
            // Leer cabeceras de la hoja de equipos
            Log.d(TAG, "Leyendo cabeceras de EQUIPOS!A1:Z1...")
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "EQUIPOS!A1:Z1")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()
            Log.d(TAG, "Cabeceras encontradas: ${headerRow.joinToString(", ")}")
            
            if (headerRow.isEmpty()) {
                Log.e(TAG, "No se encontraron cabeceras en la hoja EQUIPOS")
                return@withContext false
            }
            
            // Buscar campos de orden disponibles
            val camposOrden = headerRow.filter { it.toString().startsWith("orden_") }
            Log.d(TAG, "Campos de orden encontrados: ${camposOrden.joinToString(", ")}")
            
            // Si no se especificó campo de orden, usar el primero disponible o null
            val campoOrdenFinal = campoOrdenSeleccionado ?: camposOrden.firstOrNull()?.toString()
            Log.d(TAG, "Campo de orden seleccionado: $campoOrdenFinal")
            
            // Encontrar la última columna no vacía
            var lastColIdx = 0
            for (i in headerRow.indices) {
                if (headerRow[i].toString().isNotBlank()) {
                    lastColIdx = i
                }
            }
            // Construir rangos
            val lastColLetter = if (lastColIdx < 26) {
                ('A'.code + lastColIdx).toChar().toString()
            } else {
                val firstChar = ('A'.code + (lastColIdx / 26 - 1)).toChar()
                val secondChar = ('A'.code + (lastColIdx % 26)).toChar()
                "$firstChar$secondChar"
            }
            val dataRange = "EQUIPOS!A2:$lastColLetter"
            Log.d(TAG, "Rango de datos: $dataRange")
            
            // Obtener datos de equipos
            Log.d(TAG, "Obteniendo datos de equipos...")
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, dataRange)
                .execute()
            val values = response.getValues()
            Log.d(TAG, "Respuesta de Google Sheets recibida")
            
            if (values == null || values.isEmpty()) {
                Log.e(TAG, "No se encontraron datos de equipos")
                resultado = false
            } else {
                Log.d(TAG, "Datos obtenidos: ${values.size} filas")
                // Función para obtener valor real de una fila
                fun getValue(row: List<Any>, col: String): String {
                    val idx = headerRow.indexOf(col)
                    return if (idx != -1 && row.size > idx) row[idx].toString() else ""
                }
                
                val database = AppDatabase.getDatabase(context)
                val equipoDao = database.equipoDao()
                var equiposInsertados = 0
                var equiposActualizados = 0
                var filasProcesadas = 0
                var filasConIdVacio = 0
                
                Log.d(TAG, "Iniciando procesamiento de ${values.size} filas...")
                
                for (row in values) {
                    filasProcesadas++
                    if (row.isEmpty()) {
                        Log.d(TAG, "Fila $filasProcesadas: Vacía, saltando...")
                        continue
                    }
                    
                    val id = getValue(row, EquipoColumnMapping.ID)
                    Log.d(TAG, "Fila $filasProcesadas: ID = '$id'")
                    
                    if (id.isBlank()) {
                        filasConIdVacio++
                        Log.d(TAG, "Fila $filasProcesadas: ID vacío, saltando...")
                        continue
                    }
                    
                    // Obtener valor de orden del campo seleccionado
                    val valorOrden = if (campoOrdenFinal != null) {
                        getValue(row, campoOrdenFinal).toDoubleOrNull()
                    } else null
                    
                    Log.d(TAG, "Fila $filasProcesadas: Creando equipo con ID '$id', orden = $valorOrden")
                    
                    try {
                        val equipo = com.bithermmanagement.database.entities.Equipo(
                            id = id,
                            instalacion = getValue(row, EquipoColumnMapping.INSTALACION_TYPE).takeIf { it.isNotEmpty() },
                            unidad = getValue(row, EquipoColumnMapping.UNIDAD).takeIf { it.isNotEmpty() },
                            area = getValue(row, EquipoColumnMapping.AREA).takeIf { it.isNotEmpty() },
                            linea = getValue(row, EquipoColumnMapping.INSTALACION_LINE).takeIf { it.isNotEmpty() },
                            marca = getValue(row, EquipoColumnMapping.MARCA).takeIf { it.isNotEmpty() },
                            modelo = getValue(row, EquipoColumnMapping.MODELO).takeIf { it.isNotEmpty() },
                            tipo = getValue(row, EquipoColumnMapping.TIPO).takeIf { it.isNotEmpty() },
                            periodicidad = getValue(row, EquipoColumnMapping.PERIODICIDAD).takeIf { it.isNotEmpty() },
                            diametro = getValue(row, EquipoColumnMapping.DIAMETRO).takeIf { it.isNotEmpty() },
                            conexion = getValue(row, EquipoColumnMapping.CONEXION).takeIf { it.isNotEmpty() },
                            aislamiento = getValue(row, EquipoColumnMapping.AISLAMIENTO).takeIf { it.isNotEmpty() },
                            presEntrada = getValue(row, EquipoColumnMapping.PRES_ENTRADA).takeIf { it.isNotEmpty() },
                            presSalida = getValue(row, EquipoColumnMapping.PRES_SALIDA).takeIf { it.isNotEmpty() },
                            byPass = getValue(row, EquipoColumnMapping.BY_PASS).equals("true", ignoreCase = true),
                            descarga = getValue(row, EquipoColumnMapping.DESCARGA).takeIf { it.isNotEmpty() },
                            aplicacion = getValue(row, EquipoColumnMapping.APLICACION).takeIf { it.isNotEmpty() },
                            servicio = getValue(row, EquipoColumnMapping.SERVICIO).takeIf { it.isNotEmpty() },
                            ubicacion = getValue(row, EquipoColumnMapping.UBICACION).takeIf { it.isNotEmpty() },
                            estado = getValue(row, EquipoColumnMapping.ESTADO).takeIf { it.isNotEmpty() },
                            flota = null, // FLOTA se obtiene de la columna FLOTA en la importación
                            fechaInspeccion = getValue(row, EquipoColumnMapping.FECHA_ESTADO).takeIf { it.isNotEmpty() },
                            nota = getValue(row, EquipoColumnMapping.NOTA).takeIf { it.isNotEmpty() },
                            identidadInspector = getValue(row, EquipoColumnMapping.IDENTIDAD_INSPECTOR).takeIf { it.isNotEmpty() },
                            detectorUtilizado = getValue(row, EquipoColumnMapping.DETECTOR_UTILIZADO).takeIf { it.isNotEmpty() },
                            incidencias = getValue(row, EquipoColumnMapping.INCIDENCIAS).takeIf { it.isNotEmpty() },
                            gpsCoord = getValue(row, EquipoColumnMapping.GPS_COORD).takeIf { it.isNotEmpty() },
                            urlFotoEquipo = getValue(row, EquipoColumnMapping.FOTO).takeIf { it.isNotEmpty() },
                            urlFotoUbicacion = getValue(row, EquipoColumnMapping.FOTO_UBIC).takeIf { it.isNotEmpty() },
                            orden = valorOrden,
                            gpsAcc = getValue(row, EquipoColumnMapping.GPS_ACC).takeIf { it.isNotEmpty() },
                            extra = null,
                            modificadoLocal = false,
                            instalacionMf = getValue(row, EquipoColumnMapping.INSTALACION_MF).takeIf { it.isNotEmpty() },
                            urlFotoManifold = getValue(row, EquipoColumnMapping.FOTO_MF).takeIf { it.isNotEmpty() },
                            urlFotosExtra = getValue(row, EquipoColumnMapping.FOTO_EXTRA).takeIf { it.isNotEmpty() }
                        )
                        
                        Log.d(TAG, "Fila $filasProcesadas: Equipo creado exitosamente: ${equipo.id}")
                        
                        val equipoExistente = equipoDao.getEquipoById(id)
                        Log.d(TAG, "Fila $filasProcesadas: Verificando equipo existente para ID '$id': ${if (equipoExistente != null) "EXISTE" else "NO EXISTE"}")
                        
                        if (equipoExistente == null) {
                            Log.d(TAG, "Fila $filasProcesadas: Intentando INSERTAR equipo: $id")
                            try {
                                equipoDao.insertEquipo(equipo)
                                equiposInsertados++
                                Log.d(TAG, "Fila $filasProcesadas: ✅ Equipo INSERTADO exitosamente: $id")
                            } catch (e: Exception) {
                                Log.e(TAG, "Fila $filasProcesadas: ❌ Error INSERTANDO equipo '$id': ${e.message}", e)
                            }
                        } else {
                            Log.d(TAG, "Fila $filasProcesadas: Intentando ACTUALIZAR equipo: $id")
                            try {
                                equipoDao.updateEquipo(equipo)
                                equiposActualizados++
                                Log.d(TAG, "Fila $filasProcesadas: ✅ Equipo ACTUALIZADO exitosamente: $id")
                            } catch (e: Exception) {
                                Log.e(TAG, "Fila $filasProcesadas: ❌ Error ACTUALIZANDO equipo '$id': ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fila $filasProcesadas: Error procesando equipo con ID '$id': ${e.message}", e)
                    }
                }
                Log.d(TAG, "=== FIN SINCRONIZACIÓN DE EQUIPOS ===")
                Log.d(TAG, "Resumen:")
                Log.d(TAG, "- Filas procesadas: $filasProcesadas")
                Log.d(TAG, "- Filas con ID vacío: $filasConIdVacio")
                Log.d(TAG, "- Equipos insertados: $equiposInsertados")
                Log.d(TAG, "- Equipos actualizados: $equiposActualizados")
                Log.d(TAG, "- Campo de orden usado: $campoOrdenFinal")
                
                // Verificar el estado final de la base de datos
                try {
                    val totalEquipos = equipoDao.getAllEquipos().size
                    Log.d(TAG, "- Total equipos en base de datos: $totalEquipos")
                } catch (e: Exception) {
                    Log.e(TAG, "Error verificando total de equipos: ${e.message}", e)
                }
                
                resultado = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sincronizando equipos", e)
            resultado = false
        }
        resultado
    }

    /**
     * Obtiene los campos de orden disponibles en el spreadsheet
     */
    suspend fun getCamposOrdenDisponibles(): List<String> = withContext(Dispatchers.IO) {
        var resultado = emptyList<String>()
        try {
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "EQUIPOS!A1:Z1")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()
            
            resultado = headerRow.filter { it.toString().startsWith("orden_") }
                .map { it.toString() }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo campos de orden", e)
            resultado = emptyList()
        }
        resultado
    }
    
    /**
     * Actualiza el campo de orden seleccionado para un equipo
     */
    suspend fun actualizarOrdenEquipo(idEquipo: String, campoOrden: String): Boolean = withContext(Dispatchers.IO) {
        var resultado = false
        try {
            val database = AppDatabase.getDatabase(context)
            val equipoDao = database.equipoDao()
            val equipo = equipoDao.getEquipoById(idEquipo)

            if (equipo != null) {
                val headerResponse = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, "EQUIPOS!A1:Z1")
                    .execute()
                val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList<Any>()

                val dataResponse = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, "EQUIPOS!A2:Z")
                    .execute()
                val dataRows = dataResponse.getValues()

                val idxId = headerRow.indexOf(EquipoColumnMapping.ID)
                val filaEquipo = dataRows?.firstOrNull {
                    it.size > idxId && it[idxId].toString() == idEquipo
                }

                if (filaEquipo != null) {
                    val idxOrden = headerRow.indexOf(campoOrden)
                    val valorOrden = if (idxOrden != -1 && filaEquipo.size > idxOrden) {
                        filaEquipo[idxOrden].toString().toDoubleOrNull()
                    } else null

                    val equipoActualizado = equipo.copy(
                        orden = valorOrden
                    )
                    equipoDao.updateEquipo(equipoActualizado)
                    Log.d(TAG, "Orden actualizado para equipo $idEquipo: $campoOrden = $valorOrden")
                    resultado = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando orden del equipo", e)
            resultado = false
        }
        resultado
    }

    /**
     * Actualiza los colores de estados desde Google Sheets y escribe el JSON
     */
    suspend fun actualizarColoresEstados(
        spreadsheetId: String = this.spreadsheetId,
        sheetName: String = "EQUIPOS"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== ACTUALIZANDO COLORES DE ESTADOS ===")
            Log.d(TAG, "Spreadsheet: $spreadsheetId")
            Log.d(TAG, "Hoja: $sheetName")
            
            // Primero leer las cabeceras para encontrar las columnas (fila 2, donde están las cabeceras reales)
            val headerResponse = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A2:ZZ2")
                .execute()
            val headerRow = headerResponse.getValues()?.firstOrNull() ?: emptyList()
            
            if (headerRow.isEmpty()) {
                Log.e(TAG, "No se encontraron cabeceras en $sheetName")
                return@withContext false
            }
            
            Log.d(TAG, "Cabeceras encontradas: ${headerRow.joinToString(", ")}")
            
            // Buscar las columnas por nombre
            val estadoColorIdx = headerRow.indexOfFirst { it.toString().trim().equals("ESTADO_COLOR", ignoreCase = true) }
            val bgColorIdx = headerRow.indexOfFirst { it.toString().trim().equals("BG_COLOR", ignoreCase = true) }
            val textColorIdx = headerRow.indexOfFirst { it.toString().trim().equals("TEXT_COLOR", ignoreCase = true) }
            
            Log.d(TAG, "Índices encontrados - ESTADO_COLOR: $estadoColorIdx, BG_COLOR: $bgColorIdx, TEXT_COLOR: $textColorIdx")
            
            if (estadoColorIdx == -1 || bgColorIdx == -1) {
                Log.w(TAG, "No se encontraron las columnas ESTADO_COLOR o BG_COLOR, usando valores por defecto")
                return@withContext escribirColoresPorDefecto(context)
            }
            
            // Encontrar la última columna para el rango
            var ultimaColumnaIdx = maxOf(estadoColorIdx, bgColorIdx, textColorIdx)
            for (i in headerRow.indices) {
                if (headerRow[i].toString().isNotBlank()) {
                    ultimaColumnaIdx = maxOf(ultimaColumnaIdx, i)
                }
            }
            
            // Convertir a letra de columna
            val ultimaColumna = if (ultimaColumnaIdx < 26) {
                ('A'.code + ultimaColumnaIdx).toChar().toString()
            } else {
                val primeraLetra = ('A'.code + (ultimaColumnaIdx / 26 - 1)).toChar()
                val segundaLetra = ('A'.code + (ultimaColumnaIdx % 26)).toChar()
                "$primeraLetra$segundaLetra"
            }
            
            Log.d(TAG, "Rango de datos: A3:$ultimaColumna")
            
            // Leer datos de colores desde Google Sheets (desde fila 3, después de las cabeceras en fila 2)
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "$sheetName!A3:$ultimaColumna")
                .execute()
            val values = response.getValues()
            
            val jsonColores = JSONObject()
            
            if (values.isNullOrEmpty()) {
                Log.w(TAG, "No se encontraron datos de colores, usando valores por defecto")
                return@withContext escribirColoresPorDefecto(context)
            }
            
            Log.d(TAG, "Datos obtenidos: ${values.size} filas")
            
            for (row in values) {
                if (row.size > maxOf(estadoColorIdx, bgColorIdx)) {
                    val estado = row[estadoColorIdx].toString().trim().uppercase()
                    val colorFondo = row[bgColorIdx].toString().trim()
                    
                    if (estado.isNotBlank() && colorFondo.isNotBlank()) {
                        // Validar que el color sea un hex válido
                        if (colorFondo.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                            jsonColores.put(estado, colorFondo)
                            Log.d(TAG, "Color añadido: $estado -> $colorFondo")
                        } else {
                            Log.w(TAG, "Color inválido para $estado: $colorFondo (debe ser formato #RRGGBB)")
                        }
                    }
                }
            }
            
            // Si no se encontraron colores válidos, usar valores por defecto
            if (jsonColores.length() == 0) {
                Log.w(TAG, "No se encontraron colores válidos, usando valores por defecto")
                return@withContext escribirColoresPorDefecto(context)
            }
            
            // Escribir el JSON al archivo
            val jsonString = jsonColores.toString()
            val success = escribirJsonColores(context, jsonString)
            
            if (success) {
                Log.d(TAG, "Colores actualizados exitosamente: ${jsonColores.length()} estados")
                Log.d(TAG, "JSON generado: $jsonString")
            } else {
                Log.e(TAG, "Error escribiendo el archivo JSON")
            }
            
            return@withContext success
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando colores de estados: ${e.message}", e)
            Log.w(TAG, "Usando valores por defecto debido al error")
            return@withContext escribirColoresPorDefecto(context)
        }
    }
    
    /**
     * Escribe el JSON de colores al archivo assets
     */
    private fun escribirJsonColores(context: Context, jsonString: String): Boolean {
        return try {
            val file = java.io.File(context.filesDir, "estados_colores.json")
            file.writeText(jsonString)
            Log.d(TAG, "JSON escrito en: ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error escribiendo JSON: ${e.message}", e)
            false
        }
    }

    /**
     * Escribe los colores por defecto al archivo JSON
     */
    private fun escribirColoresPorDefecto(context: Context): Boolean {
        val jsonColores = JSONObject()
        val coloresPorDefecto = mapOf(
            "BIEN" to "#99CC99",
            "BAJA TEMPERATURA" to "#99CCFF", 
            "FUERA DE SERVICIO" to "#FFFF99",
            "NO VAPOR" to "#FFFF99",
            "FUGA CONTINUA" to "#e1664c",
            "FUGA PARCIAL" to "#e1664c",
            "CICLO CORTO" to "#e1664c",
            "INACCESIBLE" to "#a7a7a7",
            "ANULADO" to "#a7a7a7",
            "NO ENCONTRADO" to "#a7a7a7",
            "ACTIVO" to "#99CC99",
            "MONITORIZADO" to "#99CCFF",
            "AFS" to "#FFFF99",
            "ELIMINADO" to "#e1664c"
        )
        
        coloresPorDefecto.forEach { (estado, color) ->
            jsonColores.put(estado, color)
            Log.d(TAG, "Color por defecto añadido: $estado -> $color")
        }
        
        val jsonString = jsonColores.toString()
        val success = escribirJsonColores(context, jsonString)
        
        if (success) {
            Log.d(TAG, "Colores por defecto escritos exitosamente")
        }
        
        return success
    }

    data class MainMenuItem(val fragment: String, val index: Int, val color: String, val nombreVisible: String)
    data class SubMenuItem(val menuPrincipal: String, val item: String, val rol: Int, val colorCard: String, val nombreVisible: String)
    data class MenuStructureForUser(
        val mainMenus: List<MainMenuItem>,
        val subMenus: List<SubMenuItem>,
        val favoritos: List<SubMenuItem>
    )
    
    /**
     * Lista todas las hojas de un spreadsheet con sus IDs
     */
    suspend fun listarHojasConInfo(spreadsheetId: String): List<HojaInfo> = withContext(Dispatchers.IO) {
        try {
            val response = sheetsService.spreadsheets().get(spreadsheetId).execute()
            val sheets = response.sheets ?: emptyList()
            
            sheets.map { sheet ->
                HojaInfo(
                    id = sheet.properties?.sheetId?.toString() ?: "",
                    name = sheet.properties?.title ?: "",
                    index = sheet.properties?.index ?: 0
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listando hojas del spreadsheet $spreadsheetId", e)
            emptyList()
        }
    }
    
    data class HojaInfo(val id: String, val name: String, val index: Int)
    
    // Data classes para configuración de cámara
    data class ProyectoCamara(val nombre: String, val color: String)
    data class TipoDenuncia(val tipo: String, val color: String)
    data class CameraConfig(
        val proyectos: List<ProyectoCamara>,
        val tiposDenuncia: List<TipoDenuncia>
    )
    
    /**
     * Lee la configuración de cámara desde G.MENU a partir de la línea 45
     * Columnas A-B: PROYECTOS CÁMARA (columna A = nombre, columna B = color)
     * Columnas D-E: TIPOS DE DENUNCIAS (columna D = tipo, columna E = color)
     */
    suspend fun getCameraConfigFromSheet(): CameraConfig? = withContext(Dispatchers.IO) {
        try {
            // Leer desde línea 45 hasta línea 100 (ajustable)
            val menuRange = "G.MENU!A45:E100"
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, menuRange)
                .execute()
            val values = response.getValues() ?: return@withContext null
            
            val proyectos = mutableListOf<ProyectoCamara>()
            val tiposDenuncia = mutableListOf<TipoDenuncia>()
            
            for ((index, row) in values.withIndex()) {
                // Proyectos: Columna A (índice 0) = nombre, Columna B (índice 1) = color
                val proyectoNombre = row.getOrNull(0)?.toString()?.trim()
                val proyectoColor = row.getOrNull(1)?.toString()?.trim() ?: ""
                
                // Tipos de denuncia: Columna D (índice 3) = tipo, Columna E (índice 4) = color
                val tipoDenuncia = row.getOrNull(3)?.toString()?.trim()
                val tipoDenunciaColor = row.getOrNull(4)?.toString()?.trim() ?: ""
                
                Log.d(TAG, "Fila ${index + 45}: A='$proyectoNombre', B='$proyectoColor', D='$tipoDenuncia', E='$tipoDenunciaColor'")
                
                // Añadir proyecto si existe y no es un encabezado
                if (proyectoNombre != null && proyectoNombre.isNotBlank() && 
                    proyectoNombre != "PROYECTOS CÁMARA" && proyectoNombre != "GESTIÓN CÁMARA" &&
                    !proyectoNombre.startsWith("#")) { // Ignorar si es un color hex
                    proyectos.add(ProyectoCamara(proyectoNombre, proyectoColor))
                    Log.d(TAG, "  → Proyecto añadido: $proyectoNombre (color: $proyectoColor)")
                }
                
                // Añadir tipo de denuncia si existe y no es un encabezado
                // Verificar que no sea un color hex (empieza con #)
                if (tipoDenuncia != null && tipoDenuncia.isNotBlank() && 
                    tipoDenuncia != "TIPOS DE DENUNCIAS" &&
                    !tipoDenuncia.startsWith("#")) { // Ignorar si es un color hex
                    tiposDenuncia.add(TipoDenuncia(tipoDenuncia, tipoDenunciaColor))
                    Log.d(TAG, "  → Tipo denuncia añadido: $tipoDenuncia (color: $tipoDenunciaColor)")
                }
            }
            
            Log.d(TAG, "Configuración de cámara leída: ${proyectos.size} proyectos, ${tiposDenuncia.size} tipos de denuncia")
            return@withContext CameraConfig(proyectos, tiposDenuncia)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo configuración de cámara desde G.MENU", e)
            return@withContext null
        }
    }
} 