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
            
            // 3. Convertir a AbsenceRecord
            Log.d(TAG, "Paso 3: Convirtiendo datos a registros de ausencia")
            val absenceRecords = convertToAbsenceRecords(cuadroData)
            Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
            
            // 4. Escribir en AUSENCIAS-LOG
            Log.d(TAG, "Paso 4: Escribiendo en $LOGS_SHEET_NAME")
            writeToLogsSheet(absenceRecords, cuadroData)
            
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
             
             // Convertir a AbsenceRecord
             val absenceRecords = convertLogsToAbsenceRecords(filteredData)
             Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
             
             Log.d(TAG, "=== LECTURA COMPLETADA ===")
             absenceRecords
         } catch (e: Exception) {
             Log.e(TAG, "=== ERROR LEYENDO AUSENCIAS-LOG ===", e)
             Log.e(TAG, "Mensaje de error: ${e.message}")
             throw Exception("Error leyendo ausencias: ${e.message}")
         }
     }
     
         suspend fun readAllAbsencesFromLogs(): List<AbsenceRecord> = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== LEYENDO TODAS LAS AUSENCIAS DESDE AUSENCIAS-LOG ===")
        
        try {
            // Leer datos de AUSENCIAS-LOG
            val logsData = readFromLogsSheet()
            Log.d(TAG, "Datos leídos de AUSENCIAS-LOG: ${logsData.size} filas")
            
            // Convertir a AbsenceRecord (sin filtrar por mes)
            val absenceRecords = convertLogsToAbsenceRecords(logsData)
            Log.d(TAG, "Registros convertidos: ${absenceRecords.size}")
            
            Log.d(TAG, "=== LECTURA COMPLETADA ===")
            absenceRecords
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR LEYENDO TODAS LAS AUSENCIAS-LOG ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
            throw Exception("Error leyendo todas las ausencias: ${e.message}")
        }
    }
    
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
    
    private fun extractAndSaveAbsences(cuadroData: List<List<Any>>) {
        Log.d(TAG, "=== EXTRAYENDO AUSENCIAS Y GUARDANDO EN JSON ===")
        
        try {
            if (cuadroData.size < 3) {
                Log.w(TAG, "Datos insuficientes para extraer ausencias")
                return
            }
            
            val datesRow = cuadroData[0]    // Fila 0 contiene fechas
            val allAbsences = mutableMapOf<String, MutableList<AbsenceJsonData>>()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            // Procesar cada fila de empleado (desde la fila 2 en adelante)
            for (rowIndex in 2 until cuadroData.size) {
                val employeeRow = cuadroData[rowIndex]
                if (employeeRow.size < 2) continue
                
                val employeeId = employeeRow[0].toString().trim()
                val employeeName = employeeRow[1].toString().trim()
                
                if (employeeId.isEmpty() || employeeName.isEmpty()) continue
                
                Log.d(TAG, "Procesando empleado: $employeeId - $employeeName")
                
                // Procesar cada columna de ausencia (desde la columna 2 en adelante)
                for (colIndex in 2 until minOf(employeeRow.size, datesRow.size)) {
                    val absenceType = employeeRow[colIndex].toString().trim()
                    val dateStr = datesRow[colIndex].toString().trim()
                    
                    if (absenceType.isNotEmpty() && dateStr.isNotEmpty() && 
                        absenceType != "F" && absenceType != "FS" && absenceType != "L") {
                        try {
                            val date = dateFormat.parse(dateStr)
                            val calendar = Calendar.getInstance()
                            calendar.time = date
                            
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH) + 1 // Convertir a 1-based
                            val monthKey = "${year}_$month"
                            
                            if (!allAbsences.containsKey(monthKey)) {
                                allAbsences[monthKey] = mutableListOf()
                            }
                            
                            val absenceData = AbsenceJsonData(
                                employeeId = employeeId,
                                employeeName = employeeName,
                                absenceType = absenceType,
                                date = dateStr
                            )
                            
                            allAbsences[monthKey]!!.add(absenceData)
                            Log.d(TAG, "Ausencia extraída: $employeeId - $absenceType - $dateStr (mes: $monthKey)")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error parseando fecha: $dateStr - ${e.message}")
                        }
                    }
                }
            }
            
            // Guardar en JSON
            val jsonObject = org.json.JSONObject()
            allAbsences.forEach { (monthKey, absences) ->
                val monthArray = org.json.JSONArray()
                absences.forEach { absence ->
                    val absenceObject = org.json.JSONObject()
                    absenceObject.put("employeeId", absence.employeeId)
                    absenceObject.put("employeeName", absence.employeeName)
                    absenceObject.put("absenceType", absence.absenceType)
                    absenceObject.put("date", absence.date)
                    monthArray.put(absenceObject)
                }
                jsonObject.put(monthKey, monthArray)
            }
            
            val jsonString = jsonObject.toString(2) // Pretty print
            val absencesFile = context.getFileStreamPath("absences.json")
            absencesFile.writeText(jsonString)
            
            Log.d(TAG, "Ausencias guardadas en JSON: ${allAbsences.size} meses")
            Log.d(TAG, "Archivo guardado en: ${absencesFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR EXTRAYENDO AUSENCIAS ===", e)
            Log.e(TAG, "Mensaje de error: ${e.message}")
        }
    }
    
    // Clase de datos para JSON de ausencias
    private data class AbsenceJsonData(
        val employeeId: String,
        val employeeName: String,
        val absenceType: String,
        val date: String
    )
    
    private suspend fun readFromCuadroSheet(): List<List<Any>> {
        Log.d(TAG, "Leyendo hoja $CUADRO_SHEET_NAME del spreadsheet $SPREADSHEET_ID")
        // Leer hasta la columna NZ para cubrir todo el año (NZ = columna 650)
        val range = "$CUADRO_SHEET_NAME!A:NZ"
        Log.d(TAG, "Rango de lectura: $range")
        
        try {
            val response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute()
            
            val values = response.getValues() ?: emptyList()
            Log.d(TAG, "Lectura exitosa: ${values.size} filas obtenidas")
            
            // Log de las primeras filas para debug
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
    
    private suspend fun convertToAbsenceRecords(cuadroData: List<List<Any>>): List<AbsenceRecord> {
        Log.d(TAG, "Iniciando conversión de datos del cuadro. Filas totales: ${cuadroData.size}")
        
        if (cuadroData.size < 3) {
            Log.w(TAG, "Datos insuficientes: solo ${cuadroData.size} filas, se necesitan al menos 3")
            return emptyList()
        }
        
        val records = mutableListOf<AbsenceRecord>()
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        
        // Log de las primeras filas para debug
        Log.d(TAG, "Fila 0 (tipos): ${cuadroData[0]}")
        Log.d(TAG, "Fila 1 (fechas): ${cuadroData[1]}")
        if (cuadroData.size > 2) {
            Log.d(TAG, "Fila 2 (primer empleado): ${cuadroData[2]}")
        }
        
        // Obtener las fechas del mes (columna G en adelante, fila 0)
        val dateRow = cuadroData.getOrNull(0) ?: return emptyList()
        val dates = mutableListOf<Date>()
        
        Log.d(TAG, "Buscando fechas en la fila 0, columnas 7+ (total columnas: ${dateRow.size})")
        
        // Empezar desde la columna G (índice 7) que es donde están las fechas
        for (col in 7 until dateRow.size) {
            val dateStr = dateRow.getOrNull(col)?.toString()
            Log.d(TAG, "Columna $col: '$dateStr'")
            
            if (dateStr != null && dateStr.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) {
                try {
                    val date = parseDate(dateStr, dateFormat)
                    dates.add(date)
                    Log.d(TAG, "Fecha válida encontrada: ${dateFormat.format(date)}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error parseando fecha '$dateStr': ${e.message}")
                    continue
                }
            } else {
                Log.d(TAG, "Columna $col no es una fecha válida: '$dateStr'")
            }
        }
        
        Log.d(TAG, "Total de fechas encontradas: ${dates.size}")
        if (dates.isEmpty()) {
            Log.w(TAG, "No se encontraron fechas válidas, abortando conversión")
            return emptyList()
        }
        
        // Procesar cada empleado (filas 2 en adelante)
        var empleadosProcesados = 0
        var ausenciasEncontradas = 0
        
        for (rowIndex in 2 until cuadroData.size) {
            val row = cuadroData[rowIndex]
            Log.d(TAG, "Procesando fila $rowIndex (empleado): ${row.take(10)}...")
            
            if (row.size < 7) {
                Log.w(TAG, "Fila $rowIndex muy corta (${row.size} columnas), saltando")
                continue
            }
            
                         try {
                 // Obtener información del empleado
                 // Según la estructura de AUSENCIAS-CUADRO:
                 // Columna F (índice 5): USUARIO
                 // Columna G (índice 6): NOMBRE Y APELLIDOS
                 val employeeId = row.getOrNull(5)?.toString() ?: continue // Columna F: USUARIO
                 val employeeName = row.getOrNull(6)?.toString() ?: continue // Columna G: NOMBRE Y APELLIDOS
                 
                 Log.d(TAG, "Empleado: $employeeId - $employeeName")
                 
                 if (employeeId.isBlank()) {
                     Log.w(TAG, "Usuario vacío en fila $rowIndex")
                     continue
                 }
                
                empleadosProcesados++
                
                // Recopilar todas las ausencias del empleado para agruparlas
                val employeeAbsences = mutableListOf<Pair<Date, String>>()
                
                // Procesar cada día del mes (columna G en adelante)
                for (colIndex in 7 until minOf(row.size, dates.size + 7)) {
                    val dayValue = row.getOrNull(colIndex)?.toString() ?: continue
                    val date = dates.getOrNull(colIndex - 7) ?: continue
                    
                    if (dayValue.isNotBlank() && dayValue != "0") {
                        Log.d(TAG, "Ausencia encontrada: $employeeId en ${dateFormat.format(date)} = '$dayValue'")
                        employeeAbsences.add(Pair(date, dayValue))
                    }
                }
                
                // Agrupar ausencias consecutivas por tipo
                val groupedAbsences = groupConsecutiveAbsences(employeeAbsences, cuadroData, dateFormat, employeeId, employeeName)
                records.addAll(groupedAbsences)
                ausenciasEncontradas += groupedAbsences.size
                
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando empleado en fila $rowIndex: ${e.message}", e)
                continue
            }
        }
        
        Log.d(TAG, "Conversión completada: $empleadosProcesados empleados procesados, $ausenciasEncontradas ausencias agrupadas")
        return records
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
        
        // NO añadir header - solo los datos
        val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        Log.d(TAG, "Fecha de transferencia: $currentDate")
        
        records.forEachIndexed { index, record ->
            // Calcular días laborales (excluyendo fines de semana y festivos)
            val workingDays = calculateWorkingDays(record.startDate, record.endDate, cuadroData)
            
                         // Calcular días naturales (incluyendo fines de semana y festivos)
             val naturalDays = calculateDaysBetween(record.startDate, record.endDate)
             
             val row = listOf<Any>(
                 (nextId + index).toString(), // ID - numeral ascendente
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
        
        // Usar el mismo formato que funciona en BithermWORK, pero con todas las columnas
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
    
    private fun parseDate(dateStr: String, dateFormat: SimpleDateFormat): Date {
        return try {
            dateFormat.parse(dateStr) ?: Date()
        } catch (e: Exception) {
            Date() // Fecha por defecto si hay error
        }
    }
    
    private fun mapAbsenceType(typeStr: String): AbsenceType {
        return when (typeStr.uppercase()) {
            "VACACIONES", "VAC", "V" -> AbsenceType.VACACIONES
            "PERMISO", "PERM", "P" -> AbsenceType.PERMISO
            "ENFERMEDAD", "ENF", "E", "BAJA" -> AbsenceType.ENFERMEDAD
            "ASUNTOS PERSONALES", "ASUNTOS", "AP" -> AbsenceType.ASUNTOS_PERSONALES
            "FORMACIÓN", "FORMACION", "F" -> AbsenceType.FORMACION
            else -> AbsenceType.OTROS
        }
    }
    
    /**
     * Agrupa ausencias consecutivas del mismo tipo, considerando fines de semana y festivos
     */
    private fun groupConsecutiveAbsences(
        absences: List<Pair<Date, String>>,
        cuadroData: List<List<Any>>,
        dateFormat: SimpleDateFormat,
        employeeId: String,
        employeeName: String
    ): List<AbsenceRecord> {
        if (absences.isEmpty()) return emptyList()
        
        val records = mutableListOf<AbsenceRecord>()
        val calendar = Calendar.getInstance()
        
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
    
    /**
     * Verifica si dos fechas son consecutivas o están separadas solo por fines de semana/festivos
     */
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
    
    /**
     * Verifica si una fecha es fin de semana o festivo
     */
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
    
    /**
     * Crea un registro de ausencia agrupado
     */
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
        
                 // Calcular días naturales (incluyendo fines de semana y festivos)
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
        
        return AbsenceRecord(
            employeeId = employeeId,
            employeeName = employeeName,
            employeeEmail = "$employeeId@empresa.com",
            startDate = startDate,
            endDate = endDate,
            totalDays = totalDays,
            absenceType = absenceType,
            status = AbsenceStatus.APROBADA,
            description = description
        )
    }
    
    /**
     * Calcula los días laborales entre dos fechas (excluyendo fines de semana y festivos)
     */
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
    
    /**
     * Lee datos de AUSENCIAS-LOG
     */
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
        
        // Log de las primeras filas para debug
        if (values.isNotEmpty()) {
            Log.d(TAG, "Header de AUSENCIAS-LOG: ${values[0]}")
            if (values.size > 1) {
                Log.d(TAG, "Primera fila de datos: ${values[1]}")
            }
            if (values.size > 2) {
                Log.d(TAG, "Segunda fila de datos: ${values[2]}")
            }
        }
        
        return values.map { row ->
            row.map { it.toString() }
        }
    }
    
    /**
     * Filtra los datos por año y mes
     */
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
    
    /**
     * Convierte datos de AUSENCIAS-LOG a AbsenceRecord con deduplicación
     */
    private fun convertLogsToAbsenceRecords(data: List<List<Any>>): List<AbsenceRecord> {
        Log.d(TAG, "=== INICIANDO convertLogsToAbsenceRecords ===")
        Log.d(TAG, "Datos recibidos: ${data.size} filas")
        
        if (data.size <= 1) {
            Log.d(TAG, "Datos insuficientes, retornando lista vacía")
            return emptyList() // Solo header o sin datos
        }
        
        val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        
        // Log de la estructura para debug
        if (data.isNotEmpty()) {
            Log.d(TAG, "Header de AUSENCIAS-LOG: ${data[0]}")
            if (data.size > 1) {
                Log.d(TAG, "Primera fila de datos: ${data[1]}")
            }
        }
        
        // PASO 1: Procesar todas las filas y crear registros temporales
        val tempRecords = mutableListOf<AbsenceRecord>()
        
        for (i in 1 until data.size) {
            val row = data[i]
            if (row.size >= 9) { // Mínimo de columnas necesarias (A-I)
                try {
                    // Log de la fila actual para debug
                    Log.d(TAG, "Procesando fila $i: $row")
                    
                    // Estructura real de AUSENCIAS-LOG:
                    // A: ID, B: USUARIO, C: NOMBRE_COMPLETO, D: TIPO, E: FECHA_INICIO, F: FECHA_FIN, G: DIA_NAT, H: DIA_LAB, I: MOTIVO, J: FECHA_SOLICITUD, K: ESTADO, L: COMENTARIOS, M: OBSERVACIONES
                    val originalEmployeeId = row[1].toString() // Columna B: USUARIO
                    val originalEmployeeName = row[2].toString() // Columna C: NOMBRE COMPLETO
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
                    
                    val startDate = dateFormat.parse(startDateStr)
                    val endDate = dateFormat.parse(endDateStr)
                    
                    if (startDate != null && endDate != null) {
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
                            employeeId = originalEmployeeId,
                            employeeName = originalEmployeeName,
                            employeeEmail = "$originalEmployeeId@empresa.com",
                            startDate = startDate,
                            endDate = endDate,
                            totalDays = calculateDaysBetween(startDate, endDate),
                            absenceType = absenceType,
                            status = status,
                            description = comment
                        )
                        
                        tempRecords.add(absenceRecord)
                        Log.d(TAG, "Registro temporal agregado: ${absenceRecord.employeeId} - ${absenceRecord.employeeName} - ${absenceRecord.absenceType.displayName}")
                    } else {
                        Log.w(TAG, "Fila $i: Error parseando fechas - Inicio: $startDateStr, Fin: $endDateStr")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error procesando fila $i: ${e.message}")
                    Log.w(TAG, "Contenido de la fila: $row")
                }
            } else {
                Log.w(TAG, "Fila $i: Insuficientes columnas (${row.size}), se necesitan al menos 9")
            }
        }
        
        Log.d(TAG, "Registros temporales creados: ${tempRecords.size}")
        
        // PASO 2: Normalizar employeeId y deduplicar
        val normalizedRecords = mutableListOf<AbsenceRecord>()
        val processedKeys = mutableSetOf<String>()
        
        for (record in tempRecords) {
            // Normalizar employeeId
            var normalizedEmployeeId = record.employeeId
            var normalizedEmployeeName = record.employeeName
            
            // Si employeeId es numérico y employeeName parece un app ID, usar employeeName como employeeId
            if (record.employeeId.matches(Regex("\\d+")) && record.employeeName.matches(Regex("[a-zA-Z]+\\.[a-zA-Z]+"))) {
                Log.d(TAG, "Normalizando: employeeId='${record.employeeId}' (numérico) y employeeName='${record.employeeName}' (app ID)")
                normalizedEmployeeId = record.employeeName
                // employeeName se mantiene igual por ahora
            }
            
            // Crear clave única para deduplicación
            val uniqueKey = "${normalizedEmployeeId}-${record.absenceType.displayName}-${record.startDate.time}-${record.endDate.time}"
            
            if (!processedKeys.contains(uniqueKey)) {
                processedKeys.add(uniqueKey)
                
                // Crear registro normalizado
                val normalizedRecord = AbsenceRecord(
                    employeeId = normalizedEmployeeId,
                    employeeName = normalizedEmployeeName,
                    employeeEmail = "$normalizedEmployeeId@empresa.com",
                    startDate = record.startDate,
                    endDate = record.endDate,
                    totalDays = record.totalDays,
                    absenceType = record.absenceType,
                    status = record.status,
                    description = record.description
                )
                
                normalizedRecords.add(normalizedRecord)
                Log.d(TAG, "Registro normalizado agregado: ${normalizedRecord.employeeId} - ${normalizedRecord.employeeName} - ${normalizedRecord.absenceType.displayName}")
            } else {
                Log.d(TAG, "Registro duplicado ignorado: $uniqueKey")
            }
        }
        
        Log.d(TAG, "Registros finales después de normalización y deduplicación: ${normalizedRecords.size}")
        Log.d(TAG, "=== FINALIZANDO convertLogsToAbsenceRecords ===")
        return normalizedRecords
    }
    
    /**
     * Calcula días entre dos fechas
     */
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
    
    /**
     * Obtiene el siguiente ID disponible en AUSENCIAS-LOGS
     */
    private suspend fun getNextAvailableId(): Int {
        try {
            Log.d(TAG, "Obteniendo siguiente ID disponible en AUSENCIAS-LOGS")
            
            val logsData = readFromLogsSheet()
            if (logsData.size <= 1) {
                Log.d(TAG, "AUSENCIAS-LOGS está vacío o solo tiene header, siguiente ID: 1")
                return 1
            }
            
            var maxId = 0
            for (i in 1 until logsData.size) {
                val row = logsData[i]
                if (row.isNotEmpty()) {
                    try {
                        val idStr = row[0].toString()
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
            return nextId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo siguiente ID: ${e.message}", e)
            return 1 // ID por defecto si hay error
        }
    }
    
    /**
     * Calcula los días laborables entre dos fechas (excluyendo fines de semana y festivos)
     */
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
    
    /**
     * Verifica si una fecha es fin de semana o festivo
     */
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

    suspend fun writeNewAbsenceRequestToLogs(
        employeeId: String,
        employeeName: String,
        absenceType: String,
        startDate: String,
        endDate: String,
        workingDays: Int,
        description: String,
        requestDate: String
    ) {
        Log.d(TAG, "Escribiendo nueva solicitud de ausencia en Google Sheets")
        
        try {
            val nextId = getNextAvailableId()
            Log.d(TAG, "Siguiente ID disponible: $nextId")
            
            val row = listOf<Any>(
                nextId.toString(), // ID
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
            
            Log.d(TAG, "Iniciando append de nueva solicitud...")
            Log.d(TAG, "Spreadsheet ID: $SPREADSHEET_ID")
            Log.d(TAG, "Rango: $range")
            Log.d(TAG, "Datos: $row")
            
            val response = sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, range, valueRange)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute()
            
            Log.d(TAG, "Nueva solicitud escrita exitosamente")
            Log.d(TAG, "Respuesta de Google Sheets: ${response.updates?.updatedRows ?: "N/A"} filas actualizadas")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error al escribir nueva solicitud en Google Sheets: ${e.message}", e)
            throw Exception("Error al escribir nueva solicitud en Google Sheets: ${e.message}")
        }
    }
}
