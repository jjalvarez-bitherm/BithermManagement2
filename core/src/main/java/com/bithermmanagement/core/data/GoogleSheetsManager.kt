package com.bithermmanagement.core.data

import android.content.Context
import android.util.Log
import com.bithermmanagement.core.data.db.MenuEntity
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleSheetsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "GoogleSheetsManager"
    private val spreadsheetId = "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4"
    private var sheetsService: Sheets? = null

    init {
        try {
            val credentialsStream = context.assets.open("credentials2.json")
            val credentials = GoogleCredential.fromStream(credentialsStream)
                .createScoped(listOf(SheetsScopes.SPREADSHEETS))

            sheetsService = Sheets.Builder(
                NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                credentials as HttpRequestInitializer
            )
                .setApplicationName("BithermManagement")
                .build()
            Log.d(TAG, "Google Sheets service inicializado correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error inicializando Google Sheets API: ${e.message}")
            sheetsService = null
        }
    }

    private suspend fun getRoleMappings(): Map<String, Int> = withContext(Dispatchers.IO) {
        val mappings = mutableMapOf<String, Int>()
        try {
            val range = "G.MENU!A21:C" // Asume que los roles están en la columna A y los rolPound en la C
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            val values = response?.getValues()

            if (values.isNullOrEmpty()) {
                Log.e(TAG, "No se encontraron mapeos de roles en G.MENU.")
                return@withContext emptyMap()
            }

            for (row in values) {
                val roleName = row.getOrNull(0)?.toString()
                val rolePound = row.getOrNull(2)?.toString()?.toIntOrNull()
                if (!roleName.isNullOrBlank() && rolePound != null) {
                    mappings[roleName.uppercase()] = rolePound
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener mapeos de roles", e)
        }
        mappings
    }

    suspend fun getUserData(app: String, pass: String): UserData? = withContext(Dispatchers.IO) {
        try {
            val localSheetsService = sheetsService ?: return@withContext null

            val roleMappings = getRoleMappings()
            if (roleMappings.isEmpty()) {
                Log.e(TAG, "No se pudieron cargar los mapeos de roles. No se puede autenticar.")
                return@withContext null
            }

            val range = "TRABAJADORES!A:Z"
            val response = localSheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
            val values = response.getValues()

            if (values.isNullOrEmpty() || values.size < 2) {
                Log.e(TAG, "No se encontraron datos o cabecera en la hoja de TRABAJADORES.")
                return@withContext null
            }

            val headerRow = values[1].map { it.toString() } // La cabecera está en la fila 2
            val appColIndex = headerRow.indexOf(ColumnMapping.APP)
            val passColIndex = headerRow.indexOf("PASS")

            if (appColIndex == -1 || passColIndex == -1) {
                Log.e(TAG, "No se encontraron las columnas APP o PASS en la fila de cabecera.")
                Log.d(TAG, "Cabecera encontrada: $headerRow")
                return@withContext null
            }

            val dataRows = values.drop(2) // Los datos empiezan en la fila 3

            val userRow = dataRows.find { row ->
                row.getOrNull(appColIndex)?.toString().equals(app, ignoreCase = true) &&
                        row.getOrNull(passColIndex)?.toString() == pass
            }

            userRow?.let { row ->
                val headerMap = headerRow.withIndex().associate { (index, header) -> header to index }
                fun getStr(colName: String): String {
                    val index = headerMap[colName]
                    return if (index != null && index < row.size) row[index]?.toString() ?: "" else ""
                }

                val userRoleString = getStr(ColumnMapping.ROL)
                val userRolPound = roleMappings[userRoleString.uppercase()] ?: 0

                return@withContext UserData(
                    codigo = getStr(ColumnMapping.COD),
                    nombre = getStr(ColumnMapping.NOMBRE),
                    apellidos = getStr(ColumnMapping.APELLIDOS),
                    dni = getStr(ColumnMapping.DNI),
                    fechaNacimiento = getStr(ColumnMapping.FECHA_NAC),
                    app = app,
                    pass = pass,
                    rol = userRoleString,
                    rolPound = userRolPound,
                    swWeb = getStr(ColumnMapping.SW_WEB),
                    equipoAsignado = getStr(ColumnMapping.EQUIPO_ASIGN),
                    fechaCalibracion = getStr(ColumnMapping.FECHA_CAL),
                    telefonoEmpresa = getStr("TELEFONO"),
                    emailEmpresa = getStr(ColumnMapping.EMAIL_EMPR),
                    fechaAltaEmpresa = getStr(ColumnMapping.ALTA_EMPR),
                    telefonoPersonal = getStr(ColumnMapping.TELEF_PERSONAL),
                    emailPersonal = getStr(ColumnMapping.EMAIL_PERSONAL),
                    categoria = getStr(ColumnMapping.CATEGORIA),
                    revisionMedica = getStr(ColumnMapping.R_MEDICO),
                    accesoRLR = getStr(ColumnMapping.ACCESO_RLR).equals("1", ignoreCase = true),
                    supervisorEjecutivo = getStr(ColumnMapping.SUP_EJEC).equals("1", ignoreCase = true),
                    apodo = getStr("APODO")
                )
            }
            Log.w(TAG, "Usuario no encontrado o contraseña incorrecta.")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando usuario por app/pass", e)
            return@withContext null
        }
    }

    suspend fun findUser(username: String): UserData? = withContext(Dispatchers.IO) {
        try {
            if (sheetsService == null) {
                Log.e(TAG, "Google Sheets service no está disponible")
                return@withContext null
            }

            val roleMappings = getRoleMappings()
            if (roleMappings.isEmpty()) {
                Log.e(TAG, "No se pudieron cargar los mapeos de roles. No se puede continuar.")
                return@withContext null
            }

            val range = "TRABAJADORES!A:Z"
            val response = sheetsService?.spreadsheets()?.values()?.get(spreadsheetId, range)?.execute()
            val values = response?.getValues()

            if (values.isNullOrEmpty() || values.size < 2) {
                Log.e(TAG, "No se encontraron datos o cabecera en la hoja de cálculo.")
                return@withContext null
            }

            val headerRow = values[1].map { it.toString() } // La cabecera está en la fila 2
            val dataRows = values.drop(2) // Los datos empiezan en la fila 3
            val appColIndex = headerRow.indexOf(ColumnMapping.APP)

            if (appColIndex == -1) {
                Log.e(TAG, "No se encontró la columna 'APP' en la cabecera.")
                return@withContext null
            }

            for (row in dataRows) {
                val user = row.getOrNull(appColIndex)?.toString()?.trim()
                if (user.equals(username, ignoreCase = true)) {
                    val headerMap = headerRow.withIndex().associate { (index, header) -> header to index }
                    fun getStr(colName: String): String {
                        val index = headerMap[colName]
                        return if (index != null && index < row.size) row[index]?.toString() ?: "" else ""
                    }

                    val userRoleString = getStr(ColumnMapping.ROL)
                    val userRolPound = roleMappings[userRoleString.uppercase()] ?: 0

                    return@withContext UserData(
                        codigo = getStr(ColumnMapping.COD),
                        nombre = getStr(ColumnMapping.NOMBRE),
                        apellidos = getStr(ColumnMapping.APELLIDOS),
                        dni = getStr(ColumnMapping.DNI),
                        fechaNacimiento = getStr(ColumnMapping.FECHA_NAC),
                        app = username,
                        pass = getStr("PASS"),
                        rol = userRoleString,
                        rolPound = userRolPound,
                        swWeb = getStr("SW_WEB"),
                        equipoAsignado = getStr(ColumnMapping.EQUIPO_ASIGN),
                        fechaCalibracion = getStr(ColumnMapping.FECHA_CAL),
                        telefonoEmpresa = getStr("TELEFONO"),
                        emailEmpresa = getStr(ColumnMapping.EMAIL_EMPR),
                        fechaAltaEmpresa = getStr(ColumnMapping.ALTA_EMPR),
                        telefonoPersonal = getStr(ColumnMapping.TELEF_PERSONAL),
                        emailPersonal = getStr(ColumnMapping.EMAIL_PERSONAL),
                        categoria = getStr(ColumnMapping.CATEGORIA),
                        revisionMedica = getStr(ColumnMapping.R_MEDICO),
                        accesoRLR = getStr(ColumnMapping.ACCESO_RLR).equals("1", ignoreCase = true),
                        supervisorEjecutivo = getStr(ColumnMapping.SUP_EJEC).equals("1", ignoreCase = true),
                        apodo = getStr("APODO")
                    )
                }
            }
            return@withContext null // User not found
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando usuario en Google Sheets", e)
            return@withContext null
        }
    }

    suspend fun getMainMenuItems(userRol: String, userRolPound: Int): List<MenuEntity> = withContext(Dispatchers.IO) {
        try {
            val localSheetsService = sheetsService ?: run {
                Log.e(TAG, "Google Sheets service no está disponible")
                return@withContext emptyList()
            }

            val isSuperAdmin = userRol.equals("SUPERADMIN", ignoreCase = true)

            val range = "G.MENU!A1:Z"
            val response = localSheetsService.spreadsheets().values().get(spreadsheetId, range)?.execute()
            val values = response?.getValues()

            if (values.isNullOrEmpty()) {
                Log.e(TAG, "No data found in G.MENU.")
                return@withContext emptyList()
            }

            val headers = values[0].map { it.toString() }
            val dataRows = values.drop(1)

            val tituloFragmentCol = headers.indexOf("tituloFragment")
            val mainMenuVisibleNameCol = headers.indexOf("nombreVisible") // First occurrence
            val pesoCol = headers.indexOf("peso")
            val colorFragmentCol = headers.indexOf("colorFragment")
            val mainMenuCol = headers.indexOf("mainMenu")
            val rolCol = headers.indexOf("rol")

            if (tituloFragmentCol == -1) {
                Log.e(TAG, "La columna 'tituloFragment' es esencial y no se encontró.")
                return@withContext emptyList()
            }

            val allMainMenuDefs = mutableMapOf<String, MenuEntity>()
            val subItemRows = mutableListOf<List<Any>>()

            for ((index, row) in dataRows.withIndex()) {
                val tituloFragment = if(tituloFragmentCol != -1 && tituloFragmentCol < row.size) row[tituloFragmentCol]?.toString()?.trim() ?: "" else ""
                val nombreVisible = if (mainMenuVisibleNameCol != -1 && mainMenuVisibleNameCol < row.size) row[mainMenuVisibleNameCol]?.toString()?.trim() ?: "" else ""
                val peso = if (pesoCol != -1 && pesoCol < row.size) row[pesoCol]?.toString()?.trim() ?: "" else ""
                val colorFragment = if (colorFragmentCol != -1 && colorFragmentCol < row.size) row[colorFragmentCol]?.toString()?.trim() ?: "" else ""
                
                Log.d(TAG, "Procesando Fila ${index + 2}: titulo:'$tituloFragment', visible:'$nombreVisible', peso:'$peso', color:'$colorFragment'")

                // Condición para un Menú Principal: las 4 columnas clave de la izquierda deben tener datos.
                if (tituloFragment.isNotEmpty() && nombreVisible.isNotEmpty() && peso.isNotEmpty() && colorFragment.isNotEmpty()) {
                    Log.d(TAG, "==> Fila ${index + 2} IDENTIFICADA COMO MENÚ PRINCIPAL")

                    allMainMenuDefs[tituloFragment] = MenuEntity(
                        id = tituloFragment,
                        parentId = "0",
                        titleFragment = tituloFragment,
                        visibleName = nombreVisible,
                        weight = peso.toIntOrNull() ?: 0,
                        colorFragment = colorFragment,
                        mainMenu = tituloFragment,
                        item = tituloFragment,
                        role = 0,
                        colorCard = colorFragment,
                        iconName = ""
                    )
                }
                
                val mainMenuFk = if (mainMenuCol != -1 && mainMenuCol < row.size) row[mainMenuCol]?.toString()?.trim() ?: "" else ""
                if (mainMenuFk.isNotEmpty() && rolCol != -1 && rolCol < row.size && row.getOrNull(rolCol) != null) {
                    subItemRows.add(row)
                }
            }

            Log.d(TAG, "Total de menús principales definidos: ${allMainMenuDefs.size}")
            
            val accessibleMainMenuList = allMainMenuDefs.values.toMutableList()

            return@withContext accessibleMainMenuList.sortedBy { it.weight }
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getSubMenuItems(mainMenuKey: String, userRol: String, userRolPound: Int): List<MenuEntity> = withContext(Dispatchers.IO) {
        if (mainMenuKey.isEmpty()) return@withContext emptyList()

        try {
            val localSheetsService = sheetsService ?: run {
                Log.e(TAG, "Google Sheets service no está disponible")
                return@withContext emptyList()
            }

            val isSuperAdmin = userRol.equals("SUPERADMIN", ignoreCase = true)

            val range = "G.MENU!A1:Z"
            val response = localSheetsService.spreadsheets().values().get(spreadsheetId, range)?.execute()
            val values = response?.getValues()

            if (values.isNullOrEmpty()) {
                Log.e(TAG, "No data found in G.MENU.")
                return@withContext emptyList()
            }

            val headers = values[0].map { it.toString() }
            val dataRows = values.drop(1)

            val mainMenuCol = headers.indexOf("mainMenu")
            val rolCol = headers.indexOf("rol")
            val itemCol = headers.indexOf("item")
            val subMenuVisibleNameCol = headers.lastIndexOf("nombreVisible") // Last occurrence
            val pesoCol = headers.indexOf("peso")
            val iconNameCol = headers.indexOf("iconName")
            val colorCardCol = headers.indexOf("colorCard")
            val titleFragmentCol = headers.indexOf("titleFragment")

            val subMenuList = mutableListOf<MenuEntity>()

            for (row in dataRows) {
                if(mainMenuCol == -1 || mainMenuCol >= row.size || rolCol == -1 || rolCol >= row.size || itemCol == -1 || itemCol >= row.size) continue

                val rowMainMenu = row.getOrNull(mainMenuCol)?.toString()?.trim()
                
                if (rowMainMenu == mainMenuKey) {
                    val rolPoundFragment = row.getOrNull(rolCol)?.toString()?.toIntOrNull() ?: 0
                    val hasAccess = isSuperAdmin || (userRolPound != 0 && rolPoundFragment != 0 && rolPoundFragment % userRolPound == 0)

                    if (hasAccess) {
                        val itemKey = row.getOrNull(itemCol)?.toString()?.trim()
                        if (!itemKey.isNullOrEmpty()) {
                            Log.d(TAG, "Submenú con Acceso: '$itemKey' para el menú principal '$mainMenuKey'")
                            val weightValue = if(pesoCol != -1 && pesoCol < row.size) row.getOrNull(pesoCol)?.toString()?.toIntOrNull() ?: 0 else 0
                            val titleFragmentValue = if(titleFragmentCol != -1 && titleFragmentCol < row.size) row.getOrNull(titleFragmentCol)?.toString()?.takeIf { it.isNotBlank() } ?: itemKey else itemKey
                            val visibleNameValue = if(subMenuVisibleNameCol != -1 && subMenuVisibleNameCol < row.size) row.getOrNull(subMenuVisibleNameCol)?.toString()?.takeIf { it.isNotBlank() } ?: itemKey else itemKey
                            val colorCardValue = if(colorCardCol != -1 && colorCardCol < row.size) row.getOrNull(colorCardCol)?.toString() ?: "#FFFFFF" else "#FFFFFF"
                            val iconNameValue = if(iconNameCol != -1 && iconNameCol < row.size) row.getOrNull(iconNameCol)?.toString() ?: "" else ""
                            
                            subMenuList.add(
                                MenuEntity(
                                    id = itemKey,
                                    parentId = mainMenuKey,
                                    titleFragment = titleFragmentValue,
                                    visibleName = visibleNameValue,
                                    weight = weightValue,
                                    colorFragment = "",
                                    mainMenu = mainMenuKey,
                                    item = itemKey,
                                    role = rolPoundFragment,
                                    colorCard = colorCardValue,
                                    iconName = iconNameValue
                                )
                            )
                        }
                    }
                }
            }
            
            return@withContext subMenuList.sortedBy { it.weight }
            
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyList()
        }
    }

    suspend fun getFavoriteMenuItems(userName: String): List<MenuEntity> = withContext(Dispatchers.IO) {
        try {
            val localSheetsService = sheetsService ?: return@withContext emptyList()
            val range = "G.MENU!A:X" // Leer hasta la columna de usuarios
            val response = localSheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
            val values = response?.getValues()

            if (values.isNullOrEmpty() || values.size < 2) {
                Log.e(TAG, "No se encontraron datos o cabecera en la hoja G.MENU.")
                return@withContext emptyList()
            }

            val headers = values[0].map { it.toString().trim() }
            val dataRows = values.drop(1)

            val userColIndex = headers.indexOf(userName)
            if (userColIndex == -1) {
                Log.e(TAG, "FAVORITOS: No se encontró la columna para el usuario '$userName'.")
                return@withContext emptyList()
            }

            val itemCol = headers.indexOf("item")
            val mainMenuCol = headers.indexOf("mainMenu")
            val subMenuVisibleNameCol = headers.lastIndexOf("nombreVisible")
            val pesoCol = headers.indexOf("peso")
            val iconNameCol = headers.indexOf("iconName")
            val colorCardCol = headers.indexOf("colorCard")
            val titleFragmentCol = headers.indexOf("titleFragment")
            val rolCol = headers.indexOf("rol")

            val favoriteItems = mutableListOf<MenuEntity>()

            for (row in dataRows) {
                if (userColIndex < row.size && row[userColIndex]?.toString() == "1") {
                    val itemKey = if (itemCol != -1 && itemCol < row.size) row.getOrNull(itemCol)?.toString()?.trim() else null
                    if (itemKey.isNullOrEmpty()) continue

                    val mainMenuKey = if (mainMenuCol != -1 && mainMenuCol < row.size) row.getOrNull(mainMenuCol)?.toString()?.trim() ?: "" else ""
                    val weightValue = if(pesoCol != -1 && pesoCol < row.size) row.getOrNull(pesoCol)?.toString()?.toIntOrNull() ?: 0 else 0
                    val titleFragmentValue = if(titleFragmentCol != -1 && titleFragmentCol < row.size) row.getOrNull(titleFragmentCol)?.toString()?.takeIf { it.isNotBlank() } ?: itemKey else itemKey
                    val visibleNameValue = if(subMenuVisibleNameCol != -1 && subMenuVisibleNameCol < row.size) row.getOrNull(subMenuVisibleNameCol)?.toString()?.takeIf { it.isNotBlank() } ?: itemKey else itemKey
                    val colorCardValue = if(colorCardCol != -1 && colorCardCol < row.size) row.getOrNull(colorCardCol)?.toString() ?: "#FFFFFF" else "#FFFFFF"
                    val iconNameValue = if(iconNameCol != -1 && iconNameCol < row.size) row.getOrNull(iconNameCol)?.toString() ?: "" else ""
                    val rolPoundFragment = if (rolCol != -1 && rolCol < row.size) row.getOrNull(rolCol)?.toString()?.toIntOrNull() ?: 0 else 0

                    favoriteItems.add(
                        MenuEntity(
                            id = itemKey,
                            parentId = mainMenuKey,
                            titleFragment = titleFragmentValue,
                            visibleName = visibleNameValue,
                            weight = weightValue,
                            colorFragment = "",
                            mainMenu = mainMenuKey,
                            item = itemKey,
                            role = rolPoundFragment,
                            colorCard = colorCardValue,
                            iconName = iconNameValue
                        )
                    )
                }
            }
            val favoriteNames = favoriteItems.joinToString { it.visibleName }
            Log.d(TAG, "FAVORITOS: Usuario: '$userName' | Encontrados: ${favoriteItems.size} | Nombres: [$favoriteNames]")
            return@withContext favoriteItems.sortedBy { it.weight }

        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener los elementos favoritos del menú", e)
            return@withContext emptyList()
        }
    }

    suspend fun getUserByDNI(dni: String): UserData? = withContext(Dispatchers.IO) {
        try {
            val localSheetsService = sheetsService ?: return@withContext null

            val roleMappings = getRoleMappings()
            if (roleMappings.isEmpty()) {
                Log.e(TAG, "No se pudieron cargar los mapeos de roles. No se puede autenticar.")
                return@withContext null
            }

            val range = "TRABAJADORES!A:Z"
            val response = localSheetsService.spreadsheets().values().get(spreadsheetId, range).execute()
            val values = response.getValues()

            if (values.isNullOrEmpty() || values.size < 2) {
                Log.e(TAG, "No se encontraron datos en la hoja de TRABAJADORES.")
                return@withContext null
            }

            val headers = values[1].map { it.toString() } // La cabecera está en la fila 2
            val dniCol = headers.indexOf(ColumnMapping.DNI)
            if (dniCol == -1) {
                Log.e(TAG, "No se encontró la columna DNI en la hoja de TRABAJADORES.")
                return@withContext null
            }

            val dataRows = values.drop(2) // Los datos empiezan en la fila 3
            val userRow = dataRows.find { row ->
                row.getOrNull(dniCol)?.toString().equals(dni, ignoreCase = true)
            }

            userRow?.let { row ->
                val headerMap = headers.withIndex().associate { (index, header) -> header to index }
                fun getStr(colName: String): String {
                    val index = headerMap[colName]
                    return if (index != null && index < row.size) row[index]?.toString() ?: "" else ""
                }

                val userRoleString = getStr(ColumnMapping.ROL)
                val userRolPound = roleMappings[userRoleString.uppercase()] ?: 0

                return@withContext UserData(
                    codigo = getStr(ColumnMapping.COD),
                    nombre = getStr(ColumnMapping.NOMBRE),
                    apellidos = getStr(ColumnMapping.APELLIDOS),
                    dni = getStr(ColumnMapping.DNI),
                    fechaNacimiento = getStr(ColumnMapping.FECHA_NAC),
                    app = getStr(ColumnMapping.APP),
                    pass = getStr("PASS"),
                    rol = userRoleString,
                    rolPound = userRolPound,
                    swWeb = getStr("SW_WEB"),
                    equipoAsignado = getStr(ColumnMapping.EQUIPO_ASIGN),
                    fechaCalibracion = getStr(ColumnMapping.FECHA_CAL),
                    telefonoEmpresa = getStr("TELEFONO"),
                    emailEmpresa = getStr(ColumnMapping.EMAIL_EMPR),
                    fechaAltaEmpresa = getStr(ColumnMapping.ALTA_EMPR),
                    telefonoPersonal = getStr(ColumnMapping.TELEF_PERSONAL),
                    emailPersonal = getStr(ColumnMapping.EMAIL_PERSONAL),
                    categoria = getStr(ColumnMapping.CATEGORIA),
                    revisionMedica = getStr(ColumnMapping.R_MEDICO),
                    accesoRLR = getStr(ColumnMapping.ACCESO_RLR).equals("1", ignoreCase = true),
                    supervisorEjecutivo = getStr(ColumnMapping.SUP_EJEC).equals("1", ignoreCase = true),
                    apodo = getStr("APODO")
                )
            }
            return@withContext null // User not found
        } catch (e: Exception) {
            Log.e(TAG, "Error buscando usuario en Google Sheets", e)
            return@withContext null
        }
    }
} 