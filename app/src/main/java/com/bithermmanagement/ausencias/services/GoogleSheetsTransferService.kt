package com.bithermmanagement.ausencias.services

import android.content.Context
import android.util.Log
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import com.bithermmanagement.ausencias.models.AbsenceRecord
import com.bithermmanagement.ausencias.models.AbsenceType
import com.bithermmanagement.ausencias.models.AbsenceStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import java.io.InputStreamReader

@Singleton
class GoogleSheetsTransferService @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val SPREADSHEET_ID = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
        private const val CUADRO_SHEET_NAME = "AUSENCIAS-CUADRO"
        private const val LOGS_SHEET_NAME = "AUSENCIAS-LOG"
        private const val DATE_FORMAT = "dd/MM/yyyy"
        private const val TAG = "GoogleSheetsTransferService"
    }
    
    private val sheetsService: Sheets by lazy {
        try {
            Log.d(TAG, "Iniciando configuración del servicio de Google Sheets")
            
            val credentials = try {
                context.assets.open("credentials_default.json").use { inputStream ->
                    GoogleCredentials.fromStream(InputStreamReader(inputStream).readText().byteInputStream())
                        .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al leer credentials_default.json: ${e.message}", e)
                throw RuntimeException("Error al leer el archivo de credenciales", e)
            }

            val transport = try {
                GoogleNetHttpTransport.newTrustedTransport()
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear el transporte HTTP: ${e.message}", e)
                throw RuntimeException("Error al configurar el transporte seguro", e)
            }

            Log.d(TAG, "Construyendo el servicio de Google Sheets")
            Sheets.Builder(
                transport,
                GsonFactory.getDefaultInstance(),
                HttpCredentialsAdapter(credentials)
            )
            .setApplicationName("BithermManagement2")
            .build()
            .also { Log.d(TAG, "Servicio de Google Sheets construido exitosamente") }
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar el servicio de Google Sheets: ${e.message}", e)
            throw e
        }
    }
    
    // FUNCIÓN BÁSICA: Transferir datos de CUADRO a LOGS
    suspend fun transferDataFromCuadroToLogs(): List<AbsenceRecord> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== INICIANDO TRANSFERENCIA DE DATOS ===")
        
        try {
            // 1. Leer datos de AUSENCIAS-CUADRO
            Log.d(TAG, "Paso 1: Leyendo datos de $CUADRO_SHEET_NAME")
            val cuadroData = readFromCuadroSheet()
            Log.d(TAG, "Datos leídos: ${cuadroData.size} filas")
            
            // 2. Extraer y guardar festivos en JSON
            Log.d(TAG, "Paso 2: Extrayendo festivos y guardando en JSON")
            extractAndSaveHolidays(cuadroData)
            
            // 3. Convertir a AbsenceRecord y marcar celdas procesadas
            Log.d(TAG, "Paso 3: Convirtiendo datos a registros de ausencia")
            val (absenceRecords, processedCells) = convertToAbsenceRecordsAndMarkProcessed(cuadroData)
            Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
            
            // 4. Escribir en AUSENCIAS-LOG
            Log.d(TAG, "Paso 4: Escribiendo en $LOGS_SHEET_NAME")
            writeToLogsSheet(absenceRecords, cuadroData)
            
            // 5. Marcar celdas procesadas en CUADRO (agregar punto)
            Log.d(TAG, "Paso 5: Marcando celdas procesadas en $CUADRO_SHEET_NAME")
            markProcessedCells(processedCells)
            
            Log.d(TAG, "=== TRANSFERENCIA COMPLETADA EXITOSAMENTE ===")
            Log.d(TAG, "Total de registros transferidos: ${absenceRecords.size}")
            
            absenceRecords
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR EN TRANSFERENCIA ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
            Log.e(TAG, "Tipo de excepción: ${e.javaClass.simpleName}")
            throw Exception("Error en transferencia: ${e.message}")
        }
    }
    
    // FUNCIÓN PARA FORZAR TRANSFERENCIA DE TODAS LAS AUSENCIAS (incluyendo las que ya tienen punto)
    suspend fun forceTransferAllAbsences(): List<AbsenceRecord> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== INICIANDO TRANSFERENCIA FORZADA DE TODAS LAS AUSENCIAS ===")
        
        try {
            // 1. Leer datos de AUSENCIAS-CUADRO
            Log.d(TAG, "Paso 1: Leyendo datos de $CUADRO_SHEET_NAME")
            val cuadroData = readFromCuadroSheet()
            Log.d(TAG, "Datos leídos: ${cuadroData.size} filas")
            
            // 2. Extraer y guardar festivos en JSON
            Log.d(TAG, "Paso 2: Extrayendo festivos y guardando en JSON")
            extractAndSaveHolidays(cuadroData)
            
            // 3. Convertir TODAS las ausencias (incluyendo las que ya tienen punto)
            Log.d(TAG, "Paso 3: Convirtiendo TODAS las ausencias")
            val (absenceRecords, processedCells) = convertToAbsenceRecordsAndMarkAllProcessed(cuadroData)
            Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
            
            // 4. Escribir en AUSENCIAS-LOG
            Log.d(TAG, "Paso 4: Escribiendo en $LOGS_SHEET_NAME")
            writeToLogsSheet(absenceRecords, cuadroData)
            
            // 5. Marcar TODAS las celdas procesadas en CUADRO
            Log.d(TAG, "Paso 5: Marcando TODAS las celdas procesadas en $CUADRO_SHEET_NAME")
            markProcessedCells(processedCells)
            
            Log.d(TAG, "=== TRANSFERENCIA FORZADA COMPLETADA EXITOSAMENTE ===")
            Log.d(TAG, "Total de registros transferidos: ${absenceRecords.size}")
            
            absenceRecords
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR EN TRANSFERENCIA FORZADA ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
            Log.e(TAG, "Tipo de excepción: ${e.javaClass.simpleName}")
            throw Exception("Error en transferencia forzada: ${e.message}")
        }
    }
    
    // FUNCIÓN BÁSICA: Leer ausencias desde LOGS
    suspend fun readAbsencesFromLogs(year: Int, month: Int): List<AbsenceRecord> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== LEYENDO AUSENCIAS DESDE AUSENCIAS-LOG ===")
        Log.d(TAG, "Año: $year, Mes: $month")
        
        try {
            // Leer datos de AUSENCIAS-LOG
            val logsData = readFromLogsSheet()
            Log.d(TAG, "Datos leídos de AUSENCIAS-LOG: ${logsData.size} filas")
            
            // Filtrar por año y mes
            val filteredData = filterDataByMonth(logsData, year, month)
            Log.d(TAG, "Datos filtrados por mes: ${filteredData.size} filas")
            
            // Convertir a AbsenceRecord (SIN normalización por ahora)
            val absenceRecords = convertLogsToAbsenceRecordsSimple(filteredData)
            Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
            
            Log.d(TAG, "=== LECTURA COMPLETADA ===")
            absenceRecords
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR LEYENDO AUSENCIAS-LOG ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
            throw Exception("Error leyendo ausencias: ${e.message}")
        }
    }
    
    // FUNCIÓN BÁSICA: Leer todas las ausencias
    suspend fun readAllAbsencesFromLogs(): List<AbsenceRecord> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== LEYENDO TODAS LAS AUSENCIAS DESDE AUSENCIAS-LOG ===")
        
        try {
            // Leer datos de AUSENCIAS-LOG
            val logsData = readFromLogsSheet()
            Log.d(TAG, "Datos leídos de AUSENCIAS-LOG: ${logsData.size} filas")
            
            // Convertir a AbsenceRecord (SIN normalización por ahora)
            val absenceRecords = convertLogsToAbsenceRecordsSimple(logsData)
            Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
            
            Log.d(TAG, "=== LECTURA COMPLETADA ===")
            absenceRecords
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR LEYENDO TODAS LAS AUSENCIAS-LOG ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
            throw Exception("Error leyendo todas las ausencias: ${e.message}")
        }
    }
    
    // FUNCIÓN BÁSICA: Leer festivos desde JSON
    suspend fun readHolidaysFromJson(year: Int, month: Int): Map<String, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== LEYENDO FESTIVOS DESDE JSON ===")
        Log.d(TAG, "Año: $year, Mes: $month")
        
        try {
            val holidaysFile = context.getFileStreamPath("holidays.json")
            if (!holidaysFile.exists()) {
                Log.w(TAG, "Archivo holidays.json no existe")
                return@withContext emptyMap()
            }
            
            val jsonString = holidaysFile.readText()
            val holidaysData = org.json.JSONObject(jsonString)
            
            val holidays = mutableMapOf<String, String>()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Buscar festivos para el mes específico
            val monthKey = "${year}_${month + 1}" // month + 1 porque Calendar.MONTH es 0-based
            if (holidaysData.has(monthKey)) {
                val monthHolidays = holidaysData.getJSONObject(monthKey)
                val keys = monthHolidays.keys()
                
                while (keys.hasNext()) {
                    val dateStr = keys.next()
                    val holidayType = monthHolidays.getString(dateStr)
                    holidays[dateStr] = holidayType
                    Log.d(TAG, "Festivo encontrado: $dateStr -> $holidayType")
                }
            }
            
            Log.d(TAG, "Festivos encontrados para $month/$year: ${holidays.size}")
            Log.d(TAG, "Festivos: $holidays")
            
            holidays
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR LEYENDO FESTIVOS DESDE JSON ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
            // En caso de error, retornar mapa vacío
            emptyMap()
        }
    }
    
    // FUNCIÓN BÁSICA: Escribir nueva solicitud de ausencia
    suspend fun writeNewAbsenceRequestToLogs(
        employeeId: String,
        employeeName: String,
        absenceType: String,
        startDate: String,
        endDate: String,
        workingDays: Int,
        description: String,
        requestDate: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Escribiendo nueva solicitud de ausencia en Google Sheets")
        
        try {
            // 1. Escribir en AUSENCIAS-LOG
            // Generar nuevo formato de ID: AAMMDD(inicial)-userAPP-AAMMDD(final)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val startDateObj = dateFormat.parse(startDate)
            val endDateObj = dateFormat.parse(endDate)
            
            val absenceId = if (startDateObj != null && endDateObj != null) {
                generateAbsenceId(employeeId, startDateObj, endDateObj)
            } else {
                // Fallback al formato numérico si hay error parseando fechas
                val nextId = getNextAvailableId()
                nextId.toString()
            }
            
            Log.d(TAG, "ID generado: $absenceId")
            
            val row = listOf<Any>(
                absenceId, // ID (nuevo formato)
                employeeId, // USUARIO
                employeeName, // NOMBRE COMPLETO
                absenceType, // TIPO
                startDate, // FECHA INICIO
                endDate, // FECHA FIN
                workingDays.toString(), // DIA NAT
                workingDays.toString(), // DIA LAB (por ahora igual)
                description, // MOTIVO
                requestDate, // FECHA SOLICITUD
                "Pendiente", // ESTADO
                "Solicitud creada desde la app", // COMENTARIOS
                "Nueva solicitud" // OBSERVACIONES
            )
            
            val values = listOf(row)
            val valueRange = ValueRange().setValues(values)
            
            val range = "$LOGS_SHEET_NAME!A:M"
            
            Log.d(TAG, "Iniciando append de nueva solicitud en AUSENCIAS-LOG...")
            Log.d(TAG, "Spreadsheet ID: $SPREADSHEET_ID")
            Log.d(TAG, "Rango: $range")
            Log.d(TAG, "Datos: $row")
            
            val response = sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, range, valueRange)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute()
            
            Log.d(TAG, "Nueva solicitud escrita exitosamente en AUSENCIAS-LOG")
            Log.d(TAG, "Respuesta de Google Sheets: ${response.updates?.updatedRows ?: "N/A"} filas actualizadas")
            
            // 2. Escribir en AUSENCIAS-CUADRO
            writeToCuadroSheet(employeeId, employeeName, absenceType, startDate, endDate)
            
            // 3. Actualizar JSON local
            updateLocalJson(employeeId, employeeName, absenceType, startDate, endDate, description)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir nueva solicitud en Google Sheets: ${e.message}", e)
            throw Exception("Error al escribir nueva solicitud en Google Sheets: ${e.message}")
        }
    }
    
    // Función para escribir en AUSENCIAS-CUADRO
    private suspend fun writeToCuadroSheet(
        employeeId: String,
        employeeName: String,
        absenceType: String,
        startDate: String,
        endDate: String
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Escribiendo en AUSENCIAS-CUADRO...")
            
            // Leer datos actuales del cuadro
            val cuadroData = readFromCuadroSheet()
            if (cuadroData.size < 3) {
                Log.w(TAG, "Datos insuficientes en AUSENCIAS-CUADRO")
                return@withContext
            }
            
            // Buscar la fila del usuario
            val userRowIndex = findUserRow(cuadroData, employeeId, employeeName)
            if (userRowIndex == -1) {
                Log.w(TAG, "Usuario no encontrado en AUSENCIAS-CUADRO: $employeeId")
                return@withContext
            }
            
            // Obtener las fechas del mes (columna G en adelante, fila 0)
            val dateRow = cuadroData[0]
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            
            // Convertir fechas de inicio y fin
            val startDateObj = dateFormat.parse(startDate)
            val endDateObj = dateFormat.parse(endDate)
            
            if (startDateObj == null || endDateObj == null) {
                Log.e(TAG, "Error parseando fechas: $startDate, $endDate")
                return@withContext
            }
            
            // Obtener siglas de ausencia
            val absenceCode = getAbsenceCode(absenceType)
            
            // Actualizar las celdas correspondientes
            val updates = mutableListOf<com.google.api.services.sheets.v4.model.ValueRange>()
            
            // Calcular días entre inicio y fin
            val calendar = Calendar.getInstance()
            calendar.time = startDateObj
            val endCalendar = Calendar.getInstance()
            endCalendar.time = endDateObj
            
            while (!calendar.after(endCalendar)) {
                val currentDate = calendar.time
                val dateStr = dateFormat.format(currentDate)
                
                // Buscar la columna correspondiente a esta fecha
                val colIndex = findDateColumn(dateRow, dateStr)
                if (colIndex != -1) {
                    // Crear actualización para esta celda
                    val cellValue = "$absenceCode·" // Siglas + punto medio
                    val range = "$CUADRO_SHEET_NAME!${getColumnLetter(colIndex + 7)}${userRowIndex + 1}"
                    val valueRange = ValueRange().setValues(listOf(listOf(cellValue)))
                    
                    Log.d(TAG, "Actualizando celda $range con valor '$cellValue'")
                    
                    sheetsService.spreadsheets().values()
                        .update(SPREADSHEET_ID, range, valueRange)
                        .setValueInputOption("RAW")
                        .execute()
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            Log.d(TAG, "AUSENCIAS-CUADRO actualizado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error escribiendo en AUSENCIAS-CUADRO: ${e.message}", e)
        }
    }
    
    // Función para actualizar JSON local
    private suspend fun updateLocalJson(
        employeeId: String,
        employeeName: String,
        absenceType: String,
        startDate: String,
        endDate: String,
        description: String
    ) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Actualizando JSON local...")
            
            // Leer JSON actual
            val holidaysFile = context.getFileStreamPath("holidays.json")
            val jsonObject = if (holidaysFile.exists()) {
                org.json.JSONObject(holidaysFile.readText())
            } else {
                org.json.JSONObject()
            }
            
            // Parsear fechas
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val startDateObj = dateFormat.parse(startDate)
            val endDateObj = dateFormat.parse(endDate)
            
            if (startDateObj != null && endDateObj != null) {
                val calendar = Calendar.getInstance()
                calendar.time = startDateObj
                val endCalendar = Calendar.getInstance()
                endCalendar.time = endDateObj
                
                while (!calendar.after(endCalendar)) {
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val monthKey = "${year}_$month"
                    
                    if (!jsonObject.has(monthKey)) {
                        jsonObject.put(monthKey, org.json.JSONObject())
                    }
                    
                    val monthObject = jsonObject.getJSONObject(monthKey)
                    val dateStr = dateFormat.format(calendar.time)
                    
                    // Agregar ausencia al JSON (opcional, para tracking)
                    val absenceKey = "absence_$dateStr"
                    val absenceData = org.json.JSONObject().apply {
                        put("employeeId", employeeId)
                        put("employeeName", employeeName)
                        put("absenceType", absenceType)
                        put("description", description)
                    }
                    monthObject.put(absenceKey, absenceData)
                    
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            // Guardar JSON actualizado
            holidaysFile.writeText(jsonObject.toString(2))
            Log.d(TAG, "JSON local actualizado exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando JSON local: ${e.message}", e)
        }
    }
    
    // Función auxiliar para encontrar la fila del usuario
    private fun findUserRow(cuadroData: List<List<Any>>, employeeId: String, employeeName: String): Int {
        for (i in 2 until cuadroData.size) {
            val row = cuadroData[i]
            if (row.size >= 7) {
                // Buscar por employeeId o employeeName
                for (colIndex in 0 until row.size) {
                    val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                    if (cellValue == employeeId || cellValue == employeeName) {
                        return i
                    }
                }
            }
        }
        return -1
    }
    
    // Función auxiliar para obtener siglas de ausencia
    private fun getAbsenceCode(absenceType: String): String {
        return when (absenceType.uppercase()) {
            "VACACIONES" -> "V"
            "PERMISO", "PERMISO RETRIBUIDO" -> "PR"
            "ENFERMEDAD", "BAJA" -> "BJ"
            "FORMACION" -> "F"
            "ASUNTOS_PERSONALES" -> "AP"
            else -> "O"
        }
    }
    
    // Función auxiliar para encontrar columna de fecha
    private fun findDateColumn(dateRow: List<Any>, targetDate: String): Int {
        for (i in 7 until dateRow.size) {
            val dateStr = dateRow.getOrNull(i)?.toString()
            if (dateStr == targetDate) {
                return i - 7 // Ajustar índice
            }
        }
        return -1
    }
    
    // Función auxiliar para convertir índice a letra de columna
    private fun getColumnLetter(columnIndex: Int): String {
        var result = ""
        var index = columnIndex
        while (index > 0) {
            index--
            result = ('A' + (index % 26)) + result
            index /= 26
        }
        return result
    }
    
    // FUNCIÓN BÁSICA: Calcular días laborables
    suspend fun calculateWorkingDays(startDate: Date, endDate: Date): Int = withContext(Dispatchers.IO) {
        try {
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            
            var workingDays = 0
            val endCalendar = Calendar.getInstance()
            endCalendar.time = endDate
            
            while (!calendar.after(endCalendar)) {
                if (!isWeekendOrHoliday(calendar.time)) {
                    workingDays++
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            Log.d(TAG, "Días laborables calculados: $workingDays (del ${SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(startDate)} al ${SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(endDate)})")
            workingDays
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando días laborables: ${e.message}", e)
            // En caso de error, calcular solo excluyendo fines de semana
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            
            var workingDays = 0
            val endCalendar = Calendar.getInstance()
            endCalendar.time = endDate
            
            while (!calendar.after(endCalendar)) {
                if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && 
                    calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    workingDays++
                }
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            workingDays
        }
    }
    
    // ========== FUNCIONES PRIVADAS BÁSICAS ==========
    
    private fun extractAndSaveHolidays(cuadroData: List<List<Any>>) {
        Log.d(TAG, "=== EXTRAYENDO FESTIVOS Y GUARDANDO EN JSON ===")
        
        try {
            if (cuadroData.size < 2) {
                Log.w(TAG, "Datos insuficientes para extraer festivos")
                return
            }
            
            val holidaysRow = cuadroData[1] // Fila 1 contiene festivos
            val datesRow = cuadroData[0]    // Fila 0 contiene fechas
            
            Log.d(TAG, "Fila de festivos: $holidaysRow")
            Log.d(TAG, "Fila de fechas: $datesRow")
            
            val allHolidays = mutableMapOf<String, MutableMap<String, String>>()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Procesar cada columna
            for (i in 1 until minOf(holidaysRow.size, datesRow.size)) {
                val holidayType = holidaysRow[i].toString().trim()
                val dateStr = datesRow[i].toString().trim()
                
                if (holidayType.isNotEmpty() && (holidayType == "F" || holidayType == "FS")) {
                    try {
                        val date = dateFormat.parse(dateStr)
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH) + 1 // Convertir a 1-based
                        val monthKey = "${year}_$month"
                        
                        if (!allHolidays.containsKey(monthKey)) {
                            allHolidays[monthKey] = mutableMapOf()
                        }
                        
                        allHolidays[monthKey]!![dateStr] = holidayType
                        Log.d(TAG, "Festivo extraído: $dateStr -> $holidayType (mes: $monthKey)")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parseando fecha: $dateStr - ${e.message}")
                    }
                }
            }
            
            // Guardar en JSON
            val jsonObject = org.json.JSONObject()
            allHolidays.forEach { (monthKey, holidays) ->
                val monthObject = org.json.JSONObject()
                holidays.forEach { (date, type) ->
                    monthObject.put(date, type)
                }
                jsonObject.put(monthKey, monthObject)
            }
            
            val jsonString = jsonObject.toString(2) // Pretty print
            val holidaysFile = context.getFileStreamPath("holidays.json")
            holidaysFile.writeText(jsonString)
            
            Log.d(TAG, "Festivos guardados en JSON: ${allHolidays.size} meses")
            Log.d(TAG, "Archivo guardado en: ${holidaysFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR EXTRAYENDO FESTIVOS ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
        }
    }
    
    private suspend fun readFromCuadroSheet(): List<List<Any>> {
        Log.d(TAG, "Leyendo hoja $CUADRO_SHEET_NAME del spreadsheet $SPREADSHEET_ID")
        val range = "$CUADRO_SHEET_NAME!A:NZ"
        Log.d(TAG, "Rango de lectura: $range")
        
        try {
            val response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute()
            
            val values = response.getValues() ?: emptyList()
            Log.d(TAG, "Lectura exitosa: ${values.size} filas obtenidas")
            
            if (values.isNotEmpty()) {
                Log.d(TAG, "Primera fila: ${values[0]}")
                if (values.size > 1) {
                    Log.d(TAG, "Segunda fila: ${values[1]}")
                }
            }
            
            return values
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo hoja $CUADRO_SHEET_NAME: ${e.message}", e)
            throw e
        }
    }
    
    private suspend fun convertToAbsenceRecordsAndMarkProcessed(cuadroData: List<List<Any>>): Pair<List<AbsenceRecord>, List<ProcessedCell>> {
        Log.d(TAG, "Iniciando conversión de datos del cuadro. Filas totales: ${cuadroData.size}")
        
        if (cuadroData.size < 3) {
            Log.w(TAG, "Datos insuficientes: solo ${cuadroData.size} filas, se necesitan al menos 3")
            return Pair(emptyList(), emptyList())
        }
        
        val records = mutableListOf<AbsenceRecord>()
        val processedCells = mutableListOf<ProcessedCell>()
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        
        // Obtener las fechas del mes (columna G en adelante, fila 0)
        val dateRow = cuadroData.getOrNull(0) ?: return Pair(emptyList(), emptyList())
        val dates = mutableListOf<Date>()
        
        // Empezar desde la columna G (índice 7) que es donde están las fechas
        for (col in 7 until dateRow.size) {
            val dateStr = dateRow.getOrNull(col)?.toString()
            
            if (dateStr != null && dateStr.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                try {
                    val date = parseDate(dateStr, dateFormat)
                    dates.add(date)
                    Log.d(TAG, "Fecha válida encontrada: ${dateFormat.format(date)}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando fecha '$dateStr': ${e.message}")
                    continue
                }
            }
        }
        
        if (dates.isEmpty()) {
            Log.w(TAG, "No se encontraron fechas válidas, abortando conversión")
            return Pair(emptyList(), emptyList())
        }
        
        // Procesar cada empleado (filas 2 en adelante)
        for (rowIndex in 2 until cuadroData.size) {
            val row = cuadroData[rowIndex]
            
            if (row.size < 7) {
                Log.w(TAG, "Fila $rowIndex muy corta (${row.size} columnas), saltando")
                continue
            }
            
            try {
                // Verificar que la fila tenga suficientes columnas
                if (row.size < 7) {
                    Log.w(TAG, "Fila $rowIndex muy corta (${row.size} columnas), saltando")
                    continue
                }
                
                // Buscar las columnas correctas: USUARIO y NOMBRE Y APELLIDOS
                // Las columnas pueden variar, así que buscamos por contenido
                var employeeId = ""
                var employeeName = ""
                
                // Buscar la columna USUARIO (debe contener un app ID como "jjalvarez", "f.tome", etc.)
                for (colIndex in 0 until row.size) {
                    val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                    if (cellValue.matches(Regex("[a-zA-Z]+\\.[a-zA-Z]+|[a-zA-Z]+[a-zA-Z0-9]*")) && cellValue.length > 2) {
                        employeeId = cellValue
                        Log.d(TAG, "Columna USUARIO encontrada en índice $colIndex: $employeeId")
                        break
                    }
                }
                
                // Buscar la columna NOMBRE Y APELLIDOS (debe contener un nombre completo)
                for (colIndex in 0 until row.size) {
                    val cellValue = row.getOrNull(colIndex)?.toString() ?: ""
                    if (cellValue.contains(" ") && cellValue.length > 10 && cellValue.matches(Regex(".*[A-ZÁÉÍÓÚÑ].*"))) {
                        employeeName = cellValue
                        Log.d(TAG, "Columna NOMBRE Y APELLIDOS encontrada en índice $colIndex: $employeeName")
                        break
                    }
                }
                
                // Si no se encontraron, usar las columnas por defecto (5 y 6)
                if (employeeId.isBlank()) {
                    employeeId = row.getOrNull(5)?.toString() ?: ""
                }
                if (employeeName.isBlank()) {
                    employeeName = row.getOrNull(6)?.toString() ?: ""
                }
                
                // Log para debug
                Log.d(TAG, "Procesando fila $rowIndex: $row")
                Log.d(TAG, "Datos extraídos - ID: $employeeId, Nombre: $employeeName")
                
                // Validar que tenemos datos válidos
                if (employeeId.isBlank()) {
                    Log.w(TAG, "Usuario vacío en fila $rowIndex")
                    continue
                }
                
                // Validar que employeeId no sea numérico (debe ser un app ID)
                if (employeeId.matches(Regex("\\d+"))) {
                    Log.w(TAG, "employeeId es numérico en fila $rowIndex: $employeeId, saltando")
                    continue
                }
                
                if (employeeName.isBlank()) {
                    Log.w(TAG, "Nombre vacío en fila $rowIndex")
                    continue
                }
                
                Log.d(TAG, "Empleado: $employeeId - $employeeName")
                
                // Recopilar todas las ausencias del empleado para agruparlas
                val employeeAbsences = mutableListOf<Pair<Date, String>>()
                
                // Procesar cada día del mes (columna G en adelante)
                for (colIndex in 7 until minOf(row.size, dates.size + 7)) {
                    val dayValue = row.getOrNull(colIndex)?.toString() ?: continue
                    val date = dates.getOrNull(colIndex - 7) ?: continue
                    
                    // Procesar si tiene un valor de ausencia válido y no tiene punto
                    if (dayValue.isNotBlank() && dayValue != "0" && !dayValue.endsWith(".")) {
                        // Verificar que sea un tipo de ausencia válido (V, B, F, PR, etc.)
                        val cleanValue = dayValue.trim().uppercase()
                        if (cleanValue.matches(Regex("^[VBFPRAPO]+$"))) {
                            Log.d(TAG, "Ausencia encontrada: $employeeId en ${dateFormat.format(date)} = '$dayValue'")
                            employeeAbsences.add(Pair(date, dayValue))
                            
                            // Marcar celda para procesamiento
                            processedCells.add(ProcessedCell(rowIndex, colIndex, dayValue))
                            Log.d(TAG, "Celda marcada para procesamiento: fila $rowIndex, columna $colIndex, valor '$dayValue'")
                        } else {
                            Log.d(TAG, "Valor ignorado (no es ausencia válida): '$dayValue' en ${dateFormat.format(date)}")
                        }
                    } else if (dayValue.isNotBlank() && dayValue != "0" && dayValue.endsWith(".")) {
                        Log.d(TAG, "Ausencia ya procesada (tiene punto): '$dayValue' en ${dateFormat.format(date)}")
                    }
                }
                
                // Agrupar ausencias consecutivas por tipo
                val groupedAbsences = groupConsecutiveAbsences(employeeAbsences, cuadroData, dateFormat, employeeId, employeeName)
                records.addAll(groupedAbsences)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando empleado en fila $rowIndex: ${e.message}", e)
                continue
            }
        }
        
        Log.d(TAG, "Conversión completada: ${records.size} ausencias agrupadas, ${processedCells.size} celdas marcadas")
        return Pair(records, processedCells)
    }
    
    // Clase para representar una celda procesada
    data class ProcessedCell(
        val rowIndex: Int,
        val colIndex: Int,
        val originalValue: String
    )
    
    // Función para convertir TODAS las ausencias (SIMPLIFICADA)
    private suspend fun convertToAbsenceRecordsAndMarkAllProcessed(cuadroData: List<List<Any>>): Pair<List<AbsenceRecord>, List<ProcessedCell>> {
        Log.d(TAG, "Iniciando conversión SIMPLIFICADA de datos del cuadro. Filas totales: ${cuadroData.size}")
        
        if (cuadroData.size < 3) {
            Log.w(TAG, "Datos insuficientes: solo ${cuadroData.size} filas, se necesitan al menos 3")
            return Pair(emptyList(), emptyList())
        }
        
        val records = mutableListOf<AbsenceRecord>()
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        
        // Obtener las fechas del mes (columna G en adelante, fila 0)
        val dateRow = cuadroData.getOrNull(0) ?: return Pair(emptyList(), emptyList())
        val dates = mutableListOf<Date>()
        
        // Empezar desde la columna G (índice 7) que es donde están las fechas
        for (col in 7 until dateRow.size) {
            val dateStr = dateRow.getOrNull(col)?.toString()
            
            if (dateStr != null && dateStr.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                try {
                    val date = parseDate(dateStr, dateFormat)
                    dates.add(date)
                    Log.d(TAG, "Fecha válida encontrada: ${dateFormat.format(date)}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando fecha '$dateStr': ${e.message}")
                    continue
                }
            }
        }
        
        if (dates.isEmpty()) {
            Log.w(TAG, "No se encontraron fechas válidas, abortando conversión")
            return Pair(emptyList(), emptyList())
        }
        
        // Buscar la columna USUARIO en la cabecera (fila 0)
        val headerRow = cuadroData[0]
        var usuarioColumnIndex = -1
        
        for (colIndex in 0 until headerRow.size) {
            val headerValue = headerRow.getOrNull(colIndex)?.toString() ?: ""
            if (headerValue.uppercase() == "USUARIO") {
                usuarioColumnIndex = colIndex
                Log.d(TAG, "Columna USUARIO encontrada en índice: $usuarioColumnIndex")
                break
            }
        }
        
        if (usuarioColumnIndex == -1) {
            Log.e(TAG, "No se encontró la columna USUARIO en la cabecera")
            return Pair(emptyList(), emptyList())
        }
        
        // Procesar cada empleado (filas 2 en adelante)
        for (rowIndex in 2 until cuadroData.size) {
            val row = cuadroData[rowIndex]
            
            if (row.size <= usuarioColumnIndex) {
                Log.w(TAG, "Fila $rowIndex muy corta (${row.size} columnas), saltando")
                continue
            }
            
            try {
                // Obtener el USUARIO directamente de la columna correspondiente
                val employeeId = row.getOrNull(usuarioColumnIndex)?.toString() ?: ""
                
                if (employeeId.isBlank()) {
                    Log.w(TAG, "Usuario vacío en fila $rowIndex")
                    continue
                }
                
                Log.d(TAG, "Procesando empleado: $employeeId (fila $rowIndex)")
                
                // Recopilar TODAS las ausencias del empleado
                val employeeAbsences = mutableListOf<Pair<Date, String>>()
                
                // Procesar cada día del mes (columna G en adelante)
                for (colIndex in 7 until minOf(row.size, dates.size + 7)) {
                    val dayValue = row.getOrNull(colIndex)?.toString() ?: continue
                    val date = dates.getOrNull(colIndex - 7) ?: continue
                    
                    // Procesar ausencias válidas (V, B, F, PR, etc.)
                    if (dayValue.isNotBlank() && dayValue != "0") {
                        val cleanValue = dayValue.trim().uppercase().removeSuffix(".")
                        if (cleanValue.matches(Regex("^[VBFPRAPO]+$"))) {
                            Log.d(TAG, "Ausencia encontrada: $employeeId en ${dateFormat.format(date)} = '$dayValue'")
                            employeeAbsences.add(Pair(date, cleanValue))
                        }
                    }
                }
                
                // Obtener el nombre completo desde la columna "NOMBRE Y APELLIDOS" (índice 6)
                val employeeName = row.getOrNull(6)?.toString() ?: employeeId
                
                // Agrupar ausencias consecutivas por tipo (excluyendo fines de semana y festivos)
                val groupedAbsences = groupConsecutiveAbsencesSimple(employeeAbsences, dateFormat, employeeId, employeeName)
                records.addAll(groupedAbsences)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando empleado en fila $rowIndex: ${e.message}", e)
                continue
            }
        }
        
        Log.d(TAG, "Conversión SIMPLIFICADA completada: ${records.size} ausencias agrupadas")
        return Pair(records, emptyList()) // No marcamos celdas con puntos
    }
    
    // Función COMPLETAMENTE REWRITE para agrupar ausencias consecutivas (excluyendo fines de semana y festivos)
    private fun groupConsecutiveAbsencesSimple(
        absences: List<Pair<Date, String>>,
        dateFormat: SimpleDateFormat,
        employeeId: String,
        employeeName: String
    ): List<AbsenceRecord> {
        if (absences.isEmpty()) return emptyList()
        
        val records = mutableListOf<AbsenceRecord>()
        val sortedAbsences = absences.sortedBy { it.first }
        
        var currentType = sortedAbsences[0].second
        var currentStart = sortedAbsences[0].first
        var currentEnd = sortedAbsences[0].first
        
        for (i in 1 until sortedAbsences.size) {
            val (date, type) = sortedAbsences[i]
            
            // NUEVA LÓGICA: Si es el mismo tipo, verificar si se puede agrupar
            if (type == currentType) {
                // Verificar si esta fecha es "consecutiva" a través de fines de semana/festivos
                if (canGroupWithPreviousDate(currentEnd, date)) {
                    currentEnd = date
                    Log.d(TAG, "Agrupando ausencia: ${dateFormat.format(currentStart)} - ${dateFormat.format(currentEnd)} (tipo: $currentType)")
                } else {
                    // No se puede agrupar, finalizar grupo anterior
                    if (isValidWorkingPeriod(currentStart, currentEnd)) {
                        val record = createAbsenceRecord(
                            employeeId = employeeId,
                            employeeName = employeeName,
                            absenceType = currentType,
                            startDate = currentStart,
                            endDate = currentEnd,
                            dateFormat = dateFormat
                        )
                        records.add(record)
                        Log.d(TAG, "Grupo finalizado: ${dateFormat.format(currentStart)} - ${dateFormat.format(currentEnd)} (tipo: $currentType)")
                    }
                    
                    // Iniciar nuevo grupo
                    currentType = type
                    currentStart = date
                    currentEnd = date
                }
            } else {
                // Tipo diferente, finalizar grupo anterior
                if (isValidWorkingPeriod(currentStart, currentEnd)) {
                    val record = createAbsenceRecord(
                        employeeId = employeeId,
                        employeeName = employeeName,
                        absenceType = currentType,
                        startDate = currentStart,
                        endDate = currentEnd,
                        dateFormat = dateFormat
                    )
                    records.add(record)
                    Log.d(TAG, "Grupo finalizado por cambio de tipo: ${dateFormat.format(currentStart)} - ${dateFormat.format(currentEnd)} (tipo: $currentType)")
                }
                
                // Iniciar nuevo grupo
                currentType = type
                currentStart = date
                currentEnd = date
            }
        }
        
        // Agregar el último grupo
        if (isValidWorkingPeriod(currentStart, currentEnd)) {
            val record = createAbsenceRecord(
                employeeId = employeeId,
                employeeName = employeeName,
                absenceType = currentType,
                startDate = currentStart,
                endDate = currentEnd,
                dateFormat = dateFormat
            )
            records.add(record)
            Log.d(TAG, "Último grupo finalizado: ${dateFormat.format(currentStart)} - ${dateFormat.format(currentEnd)} (tipo: $currentType)")
        }
        
        Log.d(TAG, "Total de grupos creados: ${records.size}")
        return records
    }
    
    // NUEVA FUNCIÓN: Verificar si dos fechas se pueden agrupar (a través de fines de semana/festivos)
    private fun canGroupWithPreviousDate(previousDate: Date, currentDate: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = previousDate
        
        // Buscar el siguiente día laborable después de previousDate
        do {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        } while (isWeekend(calendar.time) || isHoliday(calendar.time))
        
        // Verificar si el siguiente día laborable es currentDate
        val nextWorkingDay = calendar.time
        val calendar2 = Calendar.getInstance()
        calendar2.time = currentDate
        
        // Comparar solo año, mes y día (ignorar hora)
        val canGroup = (calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH))
        
        if (canGroup) {
            Log.d(TAG, "Fechas agrupables: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(previousDate)} -> ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)}")
        } else {
            Log.d(TAG, "Fechas NO agrupables: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(previousDate)} -> ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentDate)}")
        }
        
        return canGroup
    }
    
    // Verificar si dos fechas son días laborables consecutivos (NUEVA LÓGICA CORREGIDA)
    private fun isConsecutiveWorkingDay(date1: Date, date2: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date1
        
        // Buscar el siguiente día laborable después de date1
        do {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        } while (isWeekend(calendar.time) || isHoliday(calendar.time))
        
        // Verificar si el siguiente día laborable es date2
        val nextWorkingDay = calendar.time
        val calendar2 = Calendar.getInstance()
        calendar2.time = date2
        
        // Comparar solo año, mes y día (ignorar hora)
        return (calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar.get(Calendar.MONTH) == calendar2.get(Calendar.MONTH) &&
                calendar.get(Calendar.DAY_OF_MONTH) == calendar2.get(Calendar.DAY_OF_MONTH))
    }
    
    // Verificar si un período es válido (solo días laborables)
    private fun isValidWorkingPeriod(startDate: Date, endDate: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        
        while (!calendar.time.after(endDate)) {
            if (!isWeekend(calendar.time) && !isHoliday(calendar.time)) {
                return true // Al menos un día laborable
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        return false
    }
    
    // Verificar si una fecha es fin de semana
    private fun isWeekend(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
    }
    
    // Verificar si una fecha es festivo (leyendo desde holidays.json)
    private fun isHoliday(date: Date): Boolean {
        try {
            val holidaysFile = context.getFileStreamPath("holidays.json")
            if (!holidaysFile.exists()) {
                return false
            }
            
            val jsonString = holidaysFile.readText()
            val holidaysData = org.json.JSONObject(jsonString)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dateStr = dateFormat.format(date)
            
            // Buscar en todos los meses
            val keys = holidaysData.keys()
            while (keys.hasNext()) {
                val monthKey = keys.next()
                val monthHolidays = holidaysData.getJSONObject(monthKey)
                
                if (monthHolidays.has(dateStr)) {
                    val holidayType = monthHolidays.getString(dateStr)
                    Log.d(TAG, "Festivo encontrado: $dateStr -> $holidayType")
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.w(TAG, "Error verificando festivo para fecha $date: ${e.message}")
            return false
        }
    }
    
    // Crear registro de ausencia con ID personalizado
    private fun createAbsenceRecord(
        employeeId: String,
        employeeName: String,
        absenceType: String,
        startDate: Date,
        endDate: Date,
        dateFormat: SimpleDateFormat
    ): AbsenceRecord {
        // Crear ID con formato: fecha inicial - username - fecha final
        val startDateStr = dateFormat.format(startDate)
        val endDateStr = dateFormat.format(endDate)
        val customId = "$startDateStr-$employeeId-$endDateStr"
        
        // NUEVA LÓGICA: Calcular estado basado en fechas
        val currentDate = Date()
        val status = if (startDate.after(currentDate)) {
            // La ausencia empieza en el futuro
            AbsenceStatus.PENDIENTE
        } else if (endDate.before(currentDate)) {
            // La ausencia ya terminó
            AbsenceStatus.APROBADA
        } else {
            // La ausencia está en curso
            AbsenceStatus.APROBADA
        }
        
        return AbsenceRecord(
            id = 0, // Auto-generado por Room
            employeeId = employeeId,
            employeeName = employeeName,
            absenceType = when (absenceType) {
                "V" -> AbsenceType.VACACIONES
                "B" -> AbsenceType.ENFERMEDAD
                "F" -> AbsenceType.PERMISO
                "PR" -> AbsenceType.PERMISO
                "AP" -> AbsenceType.PERMISO
                "O" -> AbsenceType.OTROS
                else -> AbsenceType.OTROS
            },
            startDate = startDate,
            endDate = endDate,
            googleSheetsId = customId,
            employeeEmail = "",
            totalDays = 0,
            status = status
        )
    }
    
    // Función para marcar celdas procesadas en CUADRO (agregar punto)
    private suspend fun markProcessedCells(processedCells: List<ProcessedCell>) = withContext(Dispatchers.IO) {
        if (processedCells.isEmpty()) {
            Log.d(TAG, "No hay celdas para marcar como procesadas")
            return@withContext
        }
        
        try {
            Log.d(TAG, "Marcando ${processedCells.size} celdas como procesadas en CUADRO")
            
            // Procesar en lotes más pequeños para evitar rate limiting
            val batchSize = 50 // Límite más conservador
            val totalBatches = (processedCells.size + batchSize - 1) / batchSize
            
            for (batchIndex in 0 until totalBatches) {
                val startIndex = batchIndex * batchSize
                val endIndex = minOf(startIndex + batchSize, processedCells.size)
                val batchCells = processedCells.subList(startIndex, endIndex)
                
                Log.d(TAG, "Procesando lote ${batchIndex + 1}/$totalBatches (${batchCells.size} celdas)")
                
                // Agrupar actualizaciones por lotes para mayor eficiencia
                val batchUpdates = mutableListOf<com.google.api.services.sheets.v4.model.ValueRange>()
                
                for (cell in batchCells) {
                    val newValue = "${cell.originalValue}." // Agregar punto al final
                    val range = "$CUADRO_SHEET_NAME!${getColumnLetter(cell.colIndex + 1)}${cell.rowIndex + 1}"
                    val valueRange = ValueRange()
                        .setRange(range)
                        .setValues(listOf(listOf(newValue)))
                    
                    Log.d(TAG, "Preparando celda $range: '${cell.originalValue}' -> '$newValue'")
                    
                    // Agregar a la lista de actualizaciones por lotes
                    batchUpdates.add(valueRange)
                }
                
                // Ejecutar actualizaciones por lotes
                if (batchUpdates.isNotEmpty()) {
                    Log.d(TAG, "Ejecutando lote ${batchIndex + 1} con ${batchUpdates.size} actualizaciones")
                    
                    val batchRequest = com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest()
                        .setValueInputOption("RAW")
                        .setData(batchUpdates)
                    
                    val response = sheetsService.spreadsheets().values()
                        .batchUpdate(SPREADSHEET_ID, batchRequest)
                        .execute()
                    
                    Log.d(TAG, "Lote ${batchIndex + 1} completado: ${response.totalUpdatedCells} celdas actualizadas")
                    
                    // Pausa entre lotes para evitar rate limiting
                    if (batchIndex < totalBatches - 1) {
                        Log.d(TAG, "Pausa de 1 segundo entre lotes...")
                        delay(1000) // 1 segundo de pausa
                    }
                }
            }
            
            Log.d(TAG, "Todas las celdas marcadas como procesadas exitosamente")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error marcando celdas como procesadas: ${e.message}", e)
            
            // Fallback: intentar actualizaciones individuales con rate limiting
            Log.d(TAG, "Intentando actualizaciones individuales como fallback")
            for ((index, cell) in processedCells.withIndex()) {
                try {
                    val newValue = "${cell.originalValue}."
                    val range = "$CUADRO_SHEET_NAME!${getColumnLetter(cell.colIndex + 1)}${cell.rowIndex + 1}"
                    val valueRange = ValueRange()
                        .setRange(range)
                        .setValues(listOf(listOf(newValue)))
                    
                    sheetsService.spreadsheets().values()
                        .update(SPREADSHEET_ID, range, valueRange)
                        .setValueInputOption("RAW")
                        .execute()
                    
                    Log.d(TAG, "Celda marcada individualmente: $range")
                    
                    // Pausa cada 10 celdas para evitar rate limiting
                    if ((index + 1) % 10 == 0) {
                        Log.d(TAG, "Pausa de 500ms cada 10 celdas...")
                        delay(500)
                    }
                } catch (individualError: Exception) {
                    Log.e(TAG, "Error marcando celda individual ${cell.rowIndex},${cell.colIndex}: ${individualError.message}")
                    
                    // Si es error 429, hacer pausa más larga
                    if (individualError.message?.contains("429") == true) {
                        Log.d(TAG, "Error 429 detectado, pausa de 2 segundos...")
                        delay(2000)
                    }
                }
            }
        }
    }
    
    private fun groupConsecutiveAbsences(
        absences: List<Pair<Date, String>>,
        cuadroData: List<List<Any>>,
        dateFormat: SimpleDateFormat,
        employeeId: String,
        employeeName: String
    ): List<AbsenceRecord> {
        if (absences.isEmpty()) return emptyList()
        
        val records = mutableListOf<AbsenceRecord>()
        
        // Ordenar ausencias por fecha
        val sortedAbsences = absences.sortedBy { it.first }
        
        var currentGroup: MutableList<Pair<Date, String>> = mutableListOf()
        var currentType: String? = null
        
        for (i in sortedAbsences.indices) {
            val (date, type) = sortedAbsences[i]
            
            // Si es el mismo tipo y fecha consecutiva (o separada solo por fines de semana/festivos)
            if (currentType == type && (currentGroup.isEmpty() || isConsecutiveOrAdjacent(currentGroup.last().first, date, cuadroData))) {
                currentGroup.add(Pair(date, type))
            } else {
                // Finalizar grupo anterior si existe
                if (currentGroup.isNotEmpty()) {
                    records.add(createGroupedAbsenceRecord(currentGroup, cuadroData, dateFormat, employeeId, employeeName))
                }
                
                // Iniciar nuevo grupo
                currentGroup = mutableListOf(Pair(date, type))
                currentType = type
            }
        }
        
        // Finalizar último grupo
        if (currentGroup.isNotEmpty()) {
            records.add(createGroupedAbsenceRecord(currentGroup, cuadroData, dateFormat, employeeId, employeeName))
        }
        
        Log.d(TAG, "Ausencias agrupadas: ${records.size} grupos creados")
        return records
    }
    
    private fun isConsecutiveOrAdjacent(date1: Date, date2: Date, cuadroData: List<List<Any>>): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date1
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        
        // Si son consecutivas, es válido
        if (calendar.time == date2) return true
        
        // Si no son consecutivas, verificar si hay fines de semana/festivos entre ellas
        var currentDate = calendar.time
        while (currentDate.before(date2)) {
            if (!isWeekendOrHoliday(currentDate, cuadroData)) {
                return false // Hay un día laboral entre las fechas
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            currentDate = calendar.time
        }
        
        return true // Solo hay fines de semana/festivos entre las fechas
    }
    
    private fun isWeekendOrHoliday(date: Date, cuadroData: List<List<Any>>): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        // Verificar si es fin de semana (sábado = 6, domingo = 1)
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
            calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return true
        }
        
        // Verificar si es festivo según la fila 1 del cuadro
        if (cuadroData.size > 1) {
            val holidayRow = cuadroData[1]
            val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val dateStr = dateFormat.format(date)
            
            // Buscar en la fila de festivos si esta fecha está marcada como "F" o "FS"
            for (col in 7 until holidayRow.size) {
                val holidayValue = holidayRow.getOrNull(col)?.toString()
                if (holidayValue == "F" || holidayValue == "FS") {
                    // Verificar si corresponde a la fecha
                    if (col < cuadroData[0].size) {
                        val headerDate = cuadroData[0].getOrNull(col)?.toString()
                        if (headerDate == dateStr) {
                            return true
                        }
                    }
                }
            }
        }
        
        return false
    }
    
    private fun createGroupedAbsenceRecord(
        group: List<Pair<Date, String>>,
        cuadroData: List<List<Any>>,
        dateFormat: SimpleDateFormat,
        employeeId: String,
        employeeName: String
    ): AbsenceRecord {
        val startDate = group.first().first
        val endDate = group.last().first
        val type = group.first().second
        
        val totalDays = calculateDaysBetween(startDate, endDate)
        
        // Determinar el tipo de ausencia
        val absenceType = when (type.uppercase()) {
            "V", "VAC" -> AbsenceType.VACACIONES
            "B", "BAJA" -> AbsenceType.ENFERMEDAD
            "P", "PERM" -> AbsenceType.PERMISO
            else -> AbsenceType.OTROS
        }
        
        // Crear descripción con rango de fechas
        val description = if (group.size == 1) {
            "Transferido desde $CUADRO_SHEET_NAME - Día: ${dateFormat.format(startDate)}"
        } else {
            "Transferido desde $CUADRO_SHEET_NAME - Del ${dateFormat.format(startDate)} al ${dateFormat.format(endDate)} (${group.size} días)"
        }
        
        // NUEVA LÓGICA: Calcular estado basado en fechas
        val currentDate = Date()
        val status = if (startDate.after(currentDate)) {
            // La ausencia empieza en el futuro
            AbsenceStatus.PENDIENTE
        } else if (endDate.before(currentDate)) {
            // La ausencia ya terminó
            AbsenceStatus.APROBADA
        } else {
            // La ausencia está en curso
            AbsenceStatus.APROBADA
        }
        
        return AbsenceRecord(
            employeeId = employeeId,
            employeeName = employeeName,
            employeeEmail = "$employeeId@empresa.com",
            startDate = startDate,
            endDate = endDate,
            totalDays = totalDays,
            absenceType = absenceType,
            status = status,
            description = description
        )
    }
    
    private suspend fun writeToLogsSheet(records: List<AbsenceRecord>, cuadroData: List<List<Any>>) {
        Log.d(TAG, "Iniciando escritura en hoja de logs. Registros a escribir: ${records.size}")
        
        if (records.isEmpty()) {
            Log.w(TAG, "No hay registros para escribir")
            return
        }
        
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val values = mutableListOf<List<Any>>()
        
        // Obtener el siguiente ID disponible
        val nextId = getNextAvailableId()
        Log.d(TAG, "Siguiente ID disponible: $nextId")
        
        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        Log.d(TAG, "Fecha de transferencia: $currentDate")
        
        records.forEachIndexed { index, record ->
            // Calcular días laborales (excluyendo fines de semana y festivos)
            val workingDays = calculateWorkingDays(record.startDate, record.endDate, cuadroData)
            
            // Calcular días naturales (incluyendo fines de semana y festivos)
            val naturalDays = calculateDaysBetween(record.startDate, record.endDate)
            
            // Generar nuevo formato de ID: AAMMDD(inicial)-userAPP-AAMMDD(final)
            val absenceId = generateAbsenceId(record.employeeId ?: "", record.startDate, record.endDate)
            
            val row = listOf<Any>(
                absenceId, // ID - nuevo formato AAMMDD-userAPP-AAMMDD
                record.employeeId ?: "", // USUARIO - nombre de usuario
                record.employeeName ?: "", // NOMBRE COMPLETO - nombre completo del empleado
                record.absenceType.displayName, // TIPO - tal como está
                dateFormat.format(record.startDate), // FECHA INICIO - tal como está
                dateFormat.format(record.endDate), // FECHA FIN - tal como está
                naturalDays.toString(), // DIA NAT - días naturales (incluyendo FS y F)
                workingDays.toString(), // DIA LAB - días laborales (sin contar FS y F)
                "Transferencia automática", // MOTIVO - transferencia automática
                currentDate, // FECHA SOLICITUD - fecha de transferencia
                record.status.displayName, // ESTADO
                "Transferido automáticamente", // COMENTARIOS
                "Sistema de transferencia" // OBSERVACIONES
            )
            values.add(row)
            
            if (index < 3) { // Log solo los primeros 3 registros para no saturar
                Log.d(TAG, "Registro ${index + 1}: $row")
            }
        }
        
        Log.d(TAG, "Total de filas preparadas: ${values.size}")
        
        val valueRange = ValueRange().setValues(values)
        val range = "$LOGS_SHEET_NAME!A:M"
        
        try {
            Log.d(TAG, "Iniciando append a Google Sheets...")
            Log.d(TAG, "Spreadsheet ID: $SPREADSHEET_ID")
            Log.d(TAG, "Rango: $range")
            Log.d(TAG, "Filas a añadir: ${values.size}")
            
            val response = sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, range, valueRange)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute()
            
            Log.d(TAG, "Append completado exitosamente")
            Log.d(TAG, "Respuesta de Google Sheets: ${response.updates?.updatedRows ?: "N/A"} filas actualizadas")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al añadir valores en Google Sheets: ${e.message}", e)
            Log.e(TAG, "Tipo de error: ${e.javaClass.simpleName}")
            if (e is com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                Log.e(TAG, "Código de error HTTP: ${e.statusCode}")
                Log.e(TAG, "Detalles del error: ${e.details}")
            }
            throw Exception("Error al escribir en Google Sheets: ${e.message}")
        }
    }
    
    private fun calculateWorkingDays(startDate: Date, endDate: Date, cuadroData: List<List<Any>>): Int {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        
        var workingDays = 0
        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate
        
        while (!calendar.after(endCalendar)) {
            if (!isWeekendOrHoliday(calendar.time, cuadroData)) {
                workingDays++
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return workingDays
    }
    
    private suspend fun readFromLogsSheet(): List<List<Any>> {
        Log.d(TAG, "Leyendo datos de $LOGS_SHEET_NAME")
        
        val range = "$LOGS_SHEET_NAME!A:Z"
        val response = sheetsService.spreadsheets().values()
            .get(SPREADSHEET_ID, range)
            .execute()
        
        val values = response.getValues()
        if (values == null || values.isEmpty()) {
            Log.w(TAG, "No se encontraron datos en $LOGS_SHEET_NAME")
            return emptyList()
        }
        
        Log.d(TAG, "Datos leídos de $LOGS_SHEET_NAME: ${values.size} filas")
        
        if (values.isNotEmpty()) {
            Log.d(TAG, "Header de AUSENCIAS-LOG: ${values[0]}")
            if (values.size > 1) {
                Log.d(TAG, "Primera fila de datos: ${values[1]}")
            }
        }
        
        return values.map { row ->
            row.map { it.toString() }
        }
    }
    
    private fun filterDataByMonth(data: List<List<Any>>, year: Int, month: Int): List<List<Any>> {
        if (data.size <= 1) return emptyList() // Solo header o sin datos
        
        val filteredData = mutableListOf<List<Any>>()
        filteredData.add(data[0]) // Incluir header
        
        val targetYear = year
        val targetMonth = month
        
        for (i in 1 until data.size) {
            val row = data[i]
            if (row.size >= 5) { // Al menos debe tener fecha de inicio (columna E)
                try {
                    val startDateStr = row[4].toString() // Columna E (fecha inicio)
                    val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
                    val startDate = dateFormat.parse(startDateStr)
                    
                    if (startDate != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = startDate
                        
                        if (calendar.get(Calendar.YEAR) == targetYear && 
                            calendar.get(Calendar.MONTH) == targetMonth) {
                            filteredData.add(row)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando fecha en fila $i: ${e.message}")
                }
            }
        }
        
        Log.d(TAG, "Datos filtrados para $month/$year: ${filteredData.size - 1} filas (sin contar header)")
        return filteredData
    }
    
    // FUNCIÓN SIMPLIFICADA: Convertir logs a AbsenceRecord SIN normalización
    private fun convertLogsToAbsenceRecordsSimple(data: List<List<Any>>): List<AbsenceRecord> {
        Log.d(TAG, "=== INICIANDO convertLogsToAbsenceRecordsSimple ===")
        Log.d(TAG, "Datos recibidos: ${data.size} filas")
        
        if (data.size <= 1) {
            Log.d(TAG, "Datos insuficientes, retornando lista vacía")
            return emptyList()
        }
        
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        val records = mutableListOf<AbsenceRecord>()
        
        for (i in 1 until data.size) {
            val row = data[i]
            if (row.size >= 9) { // Mínimo de columnas necesarias (A-I)
                try {
                    // Estructura real de AUSENCIAS-LOG:
                    // A: ID, B: USUARIO, C: NOMBRE_COMPLETO, D: TIPO, E: FECHA_INICIO, F: FECHA_FIN, G: DIA_NAT, H: DIA_LAB, I: MOTIVO, J: FECHA_SOLICITUD, K: ESTADO, L: COMENTARIOS, M: OBSERVACIONES
                    val employeeId = row[1].toString() // Columna B: USUARIO
                    val employeeName = row[2].toString() // Columna C: NOMBRE COMPLETO
                    val typeStr = row[3].toString() // Columna D: TIPO
                    val startDateStr = row[4].toString() // Columna E: FECHA_INICIO
                    val endDateStr = row[5].toString() // Columna F: FECHA_FIN
                    val statusStr = if (row.size > 10) row[10].toString() else "APROBADA" // Columna K: ESTADO
                    val comment = if (row.size > 11) row[11].toString() else "" // Columna L: COMENTARIOS
                    
                    // Validar que no estamos intentando parsear el tipo como fecha
                    if (typeStr == startDateStr || typeStr == endDateStr) {
                        Log.w(TAG, "Fila $i: Error en mapeo de columnas - tipo: $typeStr, inicio: $startDateStr, fin: $endDateStr")
                        continue
                    }
                    
                    // Validar que las fechas no estén vacías
                    if (startDateStr.isBlank() || endDateStr.isBlank()) {
                        Log.w(TAG, "Fila $i: Fechas vacías, saltando")
                        continue
                    }
                    
                    val startDate = dateFormat.parse(startDateStr) ?: continue
                    val endDate = dateFormat.parse(endDateStr) ?: continue
                    
                    val absenceType = when (typeStr.uppercase()) {
                        "VACACIONES", "V" -> AbsenceType.VACACIONES
                        "ENFERMEDAD", "E", "BAJA" -> AbsenceType.ENFERMEDAD
                        "PERMISO", "P" -> AbsenceType.PERMISO
                        "ASUNTOS_PERSONALES", "AP" -> AbsenceType.ASUNTOS_PERSONALES
                        "FORMACION", "F" -> AbsenceType.FORMACION
                        else -> AbsenceType.OTROS
                    }
                    
                    val status = when (statusStr.uppercase()) {
                        "APROBADA", "A" -> AbsenceStatus.APROBADA
                        "PENDIENTE", "P" -> AbsenceStatus.PENDIENTE
                        "RECHAZADA", "R" -> AbsenceStatus.RECHAZADA
                        "CANCELADA", "C" -> AbsenceStatus.CANCELADA
                        else -> AbsenceStatus.PENDIENTE
                    }
                    
                    val absenceRecord = AbsenceRecord(
                        employeeId = employeeId,
                        employeeName = employeeName,
                        employeeEmail = "$employeeId@empresa.com",
                        startDate = startDate,
                        endDate = endDate,
                        totalDays = calculateDaysBetween(startDate, endDate),
                        absenceType = absenceType,
                        status = status,
                        description = comment,
                        googleSheetsId = row.getOrNull(0)?.toString()
                    )
                    
                    records.add(absenceRecord)
                    Log.d(TAG, "Registro agregado (GS ID=${absenceRecord.googleSheetsId}): ${absenceRecord.employeeId} - ${absenceRecord.employeeName} - ${absenceRecord.absenceType.displayName}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error procesando fila $i: ${e.message}")
                    Log.w(TAG, "Contenido de la fila: $row")
                }
            } else {
                Log.w(TAG, "Fila $i: Insuficientes columnas (${row.size}), se necesitan al menos 9")
            }
        }
        
        Log.d(TAG, "Registros finales: ${records.size}")
        Log.d(TAG, "=== FINALIZANDO convertLogsToAbsenceRecordsSimple ===")
        return records
    }
    
    private fun calculateDaysBetween(startDate: Date, endDate: Date): Int {
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        
        var days = 0
        val endCalendar = Calendar.getInstance()
        endCalendar.time = endDate
        
        while (!calendar.after(endCalendar)) {
            days++
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return days
    }
    
    // Función para generar nuevo formato de ID: AAMMDD(inicial)-userAPP-AAMMDD(final)
    private fun generateAbsenceId(employeeId: String, startDate: Date, endDate: Date): String {
        val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
        val startDateStr = dateFormat.format(startDate)
        val endDateStr = dateFormat.format(endDate)
        return "${startDateStr}-${employeeId}-${endDateStr}"
    }
    
    private suspend fun getNextAvailableId(): Int = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Obteniendo siguiente ID disponible en AUSENCIAS-LOGS")
            
            val logsData = readFromLogsSheet()
            if (logsData.size <= 1) {
                Log.d(TAG, "AUSENCIAS-LOGS está vacío o solo tiene header, siguiente ID: 1")
                return@withContext 1
            }
            
            var maxId = 0
            for (i in 1 until logsData.size) {
                val row = logsData[i]
                if (row.isNotEmpty()) {
                    try {
                        val idStr = row[0].toString()
                        // Verificar si es el nuevo formato (contiene guiones)
                        if (idStr.contains("-")) {
                            // Es el nuevo formato, no es numérico
                            continue
                        }
                        val id = idStr.toIntOrNull()
                        if (id != null && id > maxId) {
                            maxId = id
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parseando ID en fila $i: ${e.message}")
                    }
                }
            }
            
            val nextId = maxId + 1
            Log.d(TAG, "ID máximo encontrado: $maxId, siguiente ID: $nextId")
            return@withContext nextId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo siguiente ID: ${e.message}", e)
            return@withContext 1 // ID por defecto si hay error
        }
    }
    
    private fun parseDate(dateStr: String, dateFormat: SimpleDateFormat): Date {
        return try {
            dateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date() // Fecha por defecto si hay error
        }
    }
    
    private fun isWeekendOrHoliday(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        // Verificar si es fin de semana (sábado = 6, domingo = 1)
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || 
            calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return true
        }
        
        // TODO: Verificar festivos desde holidays.json
        // Por ahora solo excluimos fines de semana
        return false
    }
    
    /**
     * Lee usuarios desde una hoja específica de Google Sheets
     * @param spreadsheetId ID del spreadsheet
     * @param sheetName Nombre de la hoja
     * @return Lista de filas con datos de usuarios
     */
    suspend fun readUsersFromSheet(spreadsheetId: String, sheetName: String): List<List<Any>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Leyendo usuarios desde hoja: $sheetName en spreadsheet: $spreadsheetId")
            
            val range = "$sheetName" // Leer toda la hoja para buscar columnas por nombre
            Log.d(TAG, "Rango solicitado: $range")
            
            val response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute()
            
            Log.d(TAG, "Respuesta recibida de Google Sheets API")
            
            val values = response.getValues()
            if (values == null || values.isEmpty()) {
                Log.w(TAG, "No se encontraron datos en la hoja $sheetName")
                return@withContext emptyList()
            }
            
            Log.d(TAG, "Datos brutos de Google Sheets: $values")
            Log.d(TAG, "Datos leídos de $sheetName: ${values.size} filas")
            
            // Convertir a List<List<Any>> y filtrar filas vacías
            val result = values.map { row ->
                row.map { value -> value ?: "" }
            }.filter { row -> row.any { it.toString().isNotEmpty() } }
            
            Log.d(TAG, "Datos procesados: $result")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo usuarios desde Google Sheets: ${e.message}", e)
            Log.e(TAG, "Stack trace completo:", e)
            return@withContext emptyList()
        }
    }
}
