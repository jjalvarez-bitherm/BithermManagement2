package com.bithermmanagement.ui.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bithermmanagement.R
import com.bithermmanagement.camera.CameraManager
import com.bithermmanagement.camera.CategorizedGalleryFragment
import com.bithermmanagement.data.SettingsManager
import com.bithermmanagement.data.CameraConfigManager
import com.bithermmanagement.data.GoogleSheetsManager
import com.bithermmanagement.multimedia.EditorWhatsAppStyleFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.bithermmanagement.ui.MainMenuActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class WorkCameraFragment : Fragment() {
    var settingsManager: SettingsManager? = null
    private var cameraConfigManager: CameraConfigManager? = null
    private var fileFotoActual: File? = null
    private var tipoFotoActual: String? = null
    private var tipoFotoActualEditor: EditorWhatsAppStyleFragment.TipoFoto? = null
    private var nombreProyectoActual: String = ""
    private var tipoDenunciaActual: String? = null
    
    // Instancia estática para usar desde companion object
    companion object {
				@JvmStatic
		var instance: WorkCameraFragment? = null
			private set
        private var cameraLauncher: ActivityResultLauncher<Uri>? = null
        
        // Datos estáticos que persisten entre instancias
        private var staticFileFotoActual: File? = null
        private var staticTipoFotoActual: String? = null
        private var staticTipoFotoActualEditor: EditorWhatsAppStyleFragment.TipoFoto? = null
        private var staticNombreProyectoActual: String = ""
        private var staticTipoDenunciaActual: String? = null
        
        fun setCameraLauncher(launcher: ActivityResultLauncher<Uri>) {
            cameraLauncher = launcher
        }
        
        fun setupButtonsForView(view: View, activity: MainMenuActivity) {
            Log.d("WorkCameraFragment", "=== INICIO setupButtonsForView ===")
            Log.d("WorkCameraFragment", "View recibido: ${view.javaClass.simpleName}")
            
            // Crear o reutilizar instancia
            if (instance == null) {
                instance = WorkCameraFragment()
                instance?.settingsManager = SettingsManager(activity)
                instance?.cameraConfigManager = CameraConfigManager(activity)
                Log.d("WorkCameraFragment", "Nueva instancia creada")
            } else {
                Log.d("WorkCameraFragment", "Reutilizando instancia existente")
            }
            
            val fragment = instance!!
            
            // Si el view es un ViewGroup, buscar el LinearLayout hijo que contiene los botones
            val rootView = if (view is ViewGroup && view.childCount > 0) {
                val child = view.getChildAt(0)
                Log.d("WorkCameraFragment", "Hijo encontrado: ${child.javaClass.simpleName}")
                child
            } else {
                view
            }
            
            val btnGPS = rootView.findViewById<Button>(R.id.btnGPS)
            val btnDenuncia = rootView.findViewById<Button>(R.id.btnDenuncia)
            val btnNota = rootView.findViewById<Button>(R.id.btnNota)
            val btnProyecto = rootView.findViewById<Button>(R.id.btnProyecto)
            val btnGaleria = rootView.findViewById<Button>(R.id.btnGaleria)
            
            Log.d("WorkCameraFragment", "Botones encontrados:")
            Log.d("WorkCameraFragment", "  - GPS: ${btnGPS != null}")
            Log.d("WorkCameraFragment", "  - DENUNCIA: ${btnDenuncia != null}")
            Log.d("WorkCameraFragment", "  - NOTA: ${btnNota != null}")
            Log.d("WorkCameraFragment", "  - PROYECTO: ${btnProyecto != null}")
            Log.d("WorkCameraFragment", "  - GALERÍA: ${btnGaleria != null}")
            
            btnGPS?.setOnClickListener {
                Log.d("WorkCameraFragment", "*** CLICK EN GPS DETECTADO ***")
                fragment.lanzarCamaraConEditor(activity, CameraManager.TIPO_GPS, EditorWhatsAppStyleFragment.TipoFoto.CAPTURA_GPS)
            }
            
            btnDenuncia?.setOnClickListener {
                Log.d("WorkCameraFragment", "*** CLICK EN DENUNCIA DETECTADO ***")
                fragment.mostrarSelectorDenuncia(activity, fragment)
            }
            
            btnNota?.setOnClickListener {
                Log.d("WorkCameraFragment", "*** CLICK EN NOTA DETECTADO ***")
                fragment.lanzarCamaraConEditor(activity, CameraManager.TIPO_NOTA, EditorWhatsAppStyleFragment.TipoFoto.FOTO_NOTA)
            }
            
            btnProyecto?.setOnClickListener {
                Log.d("WorkCameraFragment", "*** CLICK EN PROYECTO DETECTADO ***")
                fragment.mostrarSelectorProyecto(activity, fragment)
            }
            
            btnGaleria?.setOnClickListener {
                Log.d("WorkCameraFragment", "*** CLICK EN GALERÍA DETECTADO ***")
                android.widget.Toast.makeText(activity, "GALERÍA CLICK", android.widget.Toast.LENGTH_SHORT).show()
                fragment.openGallery(activity)
            }
            
            Log.d("WorkCameraFragment", "=== FIN setupButtonsForView ===")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("WorkCameraFragment", "onCreateView llamado")
        return inflater.inflate(R.layout.fragment_work_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("WorkCameraFragment", "onViewCreated llamado")
        
        if (settingsManager == null) {
            settingsManager = SettingsManager(requireContext())
        }
        if (cameraConfigManager == null) {
            cameraConfigManager = CameraConfigManager(requireContext())
        }
        setupButtons(view)
        
        Log.d("WorkCameraFragment", "Setup completado")
    }
    
    private fun setupButtons(view: View) {
        Log.d("WorkCameraFragment", "Configurando botones...")
        
        val btnGPS = view.findViewById<Button>(R.id.btnGPS)
        val btnDenuncia = view.findViewById<Button>(R.id.btnDenuncia)
        val btnNota = view.findViewById<Button>(R.id.btnNota)
        val btnProyecto = view.findViewById<Button>(R.id.btnProyecto)
        val btnGaleria = view.findViewById<Button>(R.id.btnGaleria)
        
        btnGPS?.setOnClickListener {
            Log.d("WorkCameraFragment", "Click en GPS")
            lanzarCamaraConEditor(requireContext() as MainMenuActivity, CameraManager.TIPO_GPS, EditorWhatsAppStyleFragment.TipoFoto.CAPTURA_GPS)
        }
        
        btnDenuncia?.setOnClickListener {
            Log.d("WorkCameraFragment", "Click en DENUNCIA")
            mostrarSelectorDenuncia(requireContext() as MainMenuActivity, this)
        }
        
        btnNota?.setOnClickListener {
            Log.d("WorkCameraFragment", "Click en NOTA")
            lanzarCamaraConEditor(requireContext() as MainMenuActivity, CameraManager.TIPO_NOTA, EditorWhatsAppStyleFragment.TipoFoto.FOTO_NOTA)
        }
        
        btnProyecto?.setOnClickListener {
            Log.d("WorkCameraFragment", "Click en PROYECTO")
            mostrarSelectorProyecto(requireContext() as MainMenuActivity, this)
        }
        
        btnGaleria?.setOnClickListener {
            Log.d("WorkCameraFragment", "Click en GALERÍA")
            openGallery(requireActivity() as MainMenuActivity)
        }
    }
    
    fun lanzarCamaraConEditor(activity: MainMenuActivity, tipo: String, tipoFoto: EditorWhatsAppStyleFragment.TipoFoto, nombreProyecto: String = "", tipoDenuncia: String? = null) {
        Log.d("WorkCameraFragment", "=== INICIANDO lanzarCamaraConEditor ===")
        Log.d("WorkCameraFragment", "Tipo: $tipo")
        Log.d("WorkCameraFragment", "TipoFoto: $tipoFoto")
        Log.d("WorkCameraFragment", "Nombre proyecto: $nombreProyecto")
        Log.d("WorkCameraFragment", "Tipo denuncia: $tipoDenuncia")
        
        try {
            // Primero guardar el nombre del proyecto para que esté disponible al crear el archivo
            nombreProyectoActual = nombreProyecto
            staticNombreProyectoActual = nombreProyecto
            tipoDenunciaActual = tipoDenuncia
            staticTipoDenunciaActual = tipoDenuncia
            
            val file = createImageFile(activity, tipo)
            Log.d("WorkCamera", "📸 ARCHIVO PARA CÁMARA CREADO: ${file.absolutePath}")
            Log.d("WorkCamera", "📸 Directorio padre: ${file.parentFile?.absolutePath}")
            Log.d("WorkCamera", "📸 Nombre archivo: ${file.name}")
            Log.d("WorkCamera", "📸 Archivo existe antes de cámara: ${file.exists()}")
            Log.d("WorkCamera", "📸 Directorio padre existe: ${file.parentFile?.exists()}")
            
            // Guardar en variables de instancia
            fileFotoActual = file
            tipoFotoActual = tipo
            tipoFotoActualEditor = tipoFoto
            
            // También guardar en variables estáticas para persistencia
            staticFileFotoActual = file
            staticTipoFotoActual = tipo
            staticTipoFotoActualEditor = tipoFoto
            staticNombreProyectoActual = nombreProyecto
            staticTipoDenunciaActual = tipoDenuncia
            
            // NUEVO: Guardar en SharedPreferences para sobrevivir al reinicio del proceso
            val prefs = activity.getSharedPreferences("WorkCameraSession", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("file_path", file.absolutePath)
                .putString("tipo_foto", tipo)
                .putString("tipo_editor", tipoFoto.name)
                .putString("nombre_proyecto", nombreProyecto)
                .putString("tipo_denuncia", tipoDenuncia)
                .putLong("session_timestamp", System.currentTimeMillis())
                .apply()
            Log.d("WorkCameraFragment", "Datos guardados en SharedPreferences")
            
            Log.d("WorkCameraFragment", "Creando URI con FileProvider...")
            val uri = FileProvider.getUriForFile(
                activity,
                "com.bithermmanagement.multimedia.provider",
                file
            )
            Log.d("WorkCameraFragment", "URI creado: $uri")
            
            // Usar el launcher del companion object
            cameraLauncher?.let { launcher ->
                Log.d("WorkCameraFragment", "Lanzando cámara...")
                launcher.launch(uri)
                Log.d("WorkCameraFragment", "Cámara lanzada exitosamente")
            } ?: run {
                Log.e("WorkCameraFragment", "Camera launcher no está inicializado")
                Toast.makeText(activity, "Error: Launcher de cámara no disponible", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WorkCameraFragment", "Error en lanzarCamaraConEditor", e)
            Toast.makeText(activity, "Error al abrir cámara: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        Log.d("WorkCameraFragment", "=== FIN lanzarCamaraConEditor ===")
    }
    
    fun handleCameraResult(activity: MainMenuActivity, success: Boolean) {
        Log.d("WorkCameraFragment", "=== INICIANDO handleCameraResult ===")
        Log.d("WorkCameraFragment", "Success: $success")
        
        // Verificar que la actividad está en un estado válido
        if (activity.isFinishing || activity.isDestroyed) {
            Log.e("WorkCameraFragment", "Activity está finishing o destroyed, abortando procesamiento")
            return
        }
        
        // Usar datos estáticos si los de instancia no están disponibles
        var workingFile = fileFotoActual ?: staticFileFotoActual
        var workingTipo = tipoFotoActual ?: staticTipoFotoActual
        var workingTipoEditor = tipoFotoActualEditor ?: staticTipoFotoActualEditor
        var workingNombreProyecto = nombreProyectoActual.takeIf { it.isNotEmpty() } ?: staticNombreProyectoActual
        var workingTipoDenuncia = tipoDenunciaActual ?: staticTipoDenunciaActual
        
        // Si aún no tenemos datos, recuperar de SharedPreferences (caso de reinicio de proceso)
        if (workingFile == null || workingTipoEditor == null) {
            Log.d("WorkCameraFragment", "Recuperando datos de SharedPreferences...")
            val prefs = activity.getSharedPreferences("WorkCameraSession", Context.MODE_PRIVATE)
            val sessionTimestamp = prefs.getLong("session_timestamp", 0)
            
            // Solo usar datos de hasta 5 minutos de antigüedad
            if (System.currentTimeMillis() - sessionTimestamp < 300000) {
                val filePath = prefs.getString("file_path", null)
                val tipoFoto = prefs.getString("tipo_foto", null)
                val tipoEditorStr = prefs.getString("tipo_editor", null)
                val nombreProyectoPrefs = prefs.getString("nombre_proyecto", "")
                val tipoDenunciaPrefs = prefs.getString("tipo_denuncia", null)
                
                if (filePath != null && tipoEditorStr != null) {
                    workingFile = File(filePath)
                    workingTipo = tipoFoto
                    workingTipoEditor = try {
                        EditorWhatsAppStyleFragment.TipoFoto.valueOf(tipoEditorStr)
                    } catch (e: Exception) {
                        Log.e("WorkCameraFragment", "Error convirtiendo TipoFoto: $tipoEditorStr", e)
                        null
                    }
                    workingNombreProyecto = nombreProyectoPrefs ?: ""
                    workingTipoDenuncia = tipoDenunciaPrefs
                    Log.d("WorkCameraFragment", "Datos recuperados de SharedPreferences")
                }
            } else {
                Log.w("WorkCameraFragment", "Datos de sesión muy antiguos, ignorando")
            }
        }
        
        Log.d("WorkCamera", "📷 FOTO SALIÓ DE CÁMARA: $workingFile")
        Log.d("WorkCamera", "📷 Archivo existe después de cámara: ${workingFile?.exists()}")
        Log.d("WorkCamera", "📷 Tamaño archivo después de cámara: ${workingFile?.length()} bytes")
        Log.d("WorkCamera", "📷 Tipo editor: $workingTipoEditor")
        Log.d("WorkCamera", "📷 Nombre proyecto: $workingNombreProyecto")
        
        try {
            if (success && workingFile != null && workingFile.exists()) {
                Log.d("WorkCameraFragment", "Foto capturada correctamente: ${workingFile.absolutePath}")
                Toast.makeText(activity, "Foto tomada, abriendo editor...", Toast.LENGTH_SHORT).show()
                
                // Dar un pequeño retraso para permitir que la actividad se estabilice
                activity.runOnUiThread {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!activity.isFinishing && !activity.isDestroyed) {
                            mostrarEditor(activity, workingFile, workingTipoEditor, workingNombreProyecto, workingTipoDenuncia)
                        } else {
                            Log.e("WorkCameraFragment", "Activity no válida en el callback diferido")
                        }
                    }, 200) // 200ms de retraso
                }
            } else {
                Log.w("WorkCameraFragment", "Foto no capturada o archivo no existe")
                if (!success) {
                    Log.w("WorkCameraFragment", "Usuario canceló la captura")
                    Toast.makeText(activity, "Captura cancelada", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("WorkCameraFragment", "Archivo de foto no existe después de captura")
                    Toast.makeText(activity, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
                }
                workingFile?.delete()
            }
        } catch (e: Exception) {
            Log.e("WorkCameraFragment", "Error crítico en handleCameraResult", e)
            Toast.makeText(activity, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
            workingFile?.delete()
        } finally {
            // Limpiar datos estáticos después del procesamiento
            staticFileFotoActual = null
            staticTipoFotoActual = null
            staticTipoFotoActualEditor = null
            staticNombreProyectoActual = ""
            
            // Limpiar SharedPreferences
            val prefs = activity.getSharedPreferences("WorkCameraSession", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            Log.d("WorkCameraFragment", "Datos de sesión limpiados")
        }
        
        Log.d("WorkCameraFragment", "=== FIN handleCameraResult ===")
    }
    
    private fun createImageFile(activity: MainMenuActivity, tipo: String): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${tipo}_${timeStamp}_temp.jpg"
        
        // Crear archivo temporal en cache directory, no en la carpeta final
        val storageDir = activity.cacheDir
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File(storageDir, fileName)
    }
    
    private fun getStorageDirectoryForType(activity: MainMenuActivity, type: String): File {
        return when (type) {
            CameraManager.TIPO_PROYECTO -> {
                // Para proyectos, usar filesDir/proyectos/[nombre_proyecto]
                // El nombre del proyecto debe haberse guardado en staticNombreProyectoActual
                val nombreProyecto = staticNombreProyectoActual ?: nombreProyectoActual ?: "Proyecto_Default"
                File(activity.filesDir, "proyectos/$nombreProyecto")
            }
            else -> {
                // Para otros tipos, usar el directorio interno
                File(activity.filesDir, when(type) {
                    CameraManager.TIPO_GPS -> "gps_fotos"
                    CameraManager.TIPO_DENUNCIA -> "denuncias"
                    CameraManager.TIPO_NOTA -> "foto_notas"
                    else -> "fotos"
                })
            }
        }
    }
    
    private fun obtenerCoordenadasGPS(): String? {
        return try {
            val lastLocation = MainMenuActivity.GpsProvider.lastLocation
            lastLocation?.let { "${it.latitude}, ${it.longitude}" }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun obtenerPrecisionGPS(): String? {
        return try {
            val lastLocation = MainMenuActivity.GpsProvider.lastLocation
            lastLocation?.let { "%.1f".format(it.accuracy) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun obtenerAltitudGPS(): String? {
        return try {
            val lastLocation = MainMenuActivity.GpsProvider.lastLocation
            lastLocation?.let { "%.1f".format(it.altitude) }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun mostrarSelectorProyecto(activity: MainMenuActivity, fragment: WorkCameraFragment) {
        Log.d("WorkCameraFragment", "=== INICIANDO mostrarSelectorProyecto ===")
        
        // Obtener configuración híbrida (cache + sheet)
        val sheetsManager = settingsManager?.getGoogleSheetsManager()
        CoroutineScope(Dispatchers.Main).launch {
            val config = cameraConfigManager?.getCameraConfig(sheetsManager)
            
            // Combinar proyectos del sheet con proyectos locales (carpetas)
            val proyectosSheet = config?.proyectos?.map { it.nombre } ?: emptyList()
            val proyectosLocales = obtenerProyectosDisponibles(activity)
            
            // Unir ambos (sin duplicados)
            val todosProyectos = (proyectosSheet + proyectosLocales).distinct().sorted()
            
            Log.d("WorkCameraFragment", "Proyectos disponibles: ${todosProyectos.size} (${proyectosSheet.size} del sheet, ${proyectosLocales.size} locales)")
            
            if (todosProyectos.isEmpty()) {
                Log.w("WorkCameraFragment", "No hay proyectos disponibles")
                AlertDialog.Builder(activity)
                    .setTitle("Sin Proyectos")
                    .setMessage("No se encontraron proyectos. Creando carpetas de ejemplo...")
                    .setPositiveButton("OK") { dialog, _ ->
                        crearCarpetasProyectoEjemplo(activity)
                        dialog.dismiss()
                    }
                    .show()
                return@launch
            }
            
            // Crear diálogo con proyectos
            val dialogBuilder = AlertDialog.Builder(activity)
            dialogBuilder.setTitle("Seleccionar Proyecto")
            
            dialogBuilder.setItems(todosProyectos.toTypedArray()) { dialog, which ->
                val proyectoSeleccionado = todosProyectos[which]
                guardarProyectoSeleccionado(activity, proyectoSeleccionado)
                dialog.dismiss()
                
                fragment.lanzarCamaraConEditor(
                    activity, 
                    CameraManager.TIPO_PROYECTO, 
                    EditorWhatsAppStyleFragment.TipoFoto.FOTO_PROYECTO,
                    proyectoSeleccionado
                )
            }
            
            dialogBuilder.setNeutralButton("+ Nuevo") { dialog, _ ->
                dialog.dismiss()
                mostrarDialogoCrearProyecto(activity, fragment)
            }
            
            dialogBuilder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            
            dialogBuilder.show()
        }
    }
    
    private fun mostrarSelectorDenuncia(activity: MainMenuActivity, fragment: WorkCameraFragment) {
        Log.d("WorkCameraFragment", "=== INICIANDO mostrarSelectorDenuncia ===")
        Log.d("WorkCameraFragment", "settingsManager: ${settingsManager != null}")
        Log.d("WorkCameraFragment", "cameraConfigManager: ${cameraConfigManager != null}")
        
        // Asegurar que los managers estén inicializados
        if (settingsManager == null) {
            settingsManager = SettingsManager(activity)
        }
        if (cameraConfigManager == null) {
            cameraConfigManager = CameraConfigManager(activity)
        }
        
        // Obtener configuración híbrida (cache + sheet)
        val sheetsManager = settingsManager?.getGoogleSheetsManager()
        Log.d("WorkCameraFragment", "sheetsManager: ${sheetsManager != null}")
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val config = cameraConfigManager?.getCameraConfig(sheetsManager)
                Log.d("WorkCameraFragment", "Config obtenida: ${config != null}")
                val tiposDenuncia = config?.tiposDenuncia ?: emptyList()
                
                Log.d("WorkCameraFragment", "Tipos de denuncia disponibles: ${tiposDenuncia.size}")
                tiposDenuncia.forEach { tipo ->
                    Log.d("WorkCameraFragment", "  - ${tipo.tipo} (color: ${tipo.color})")
                }
                
                if (tiposDenuncia.isEmpty()) {
                    Log.w("WorkCameraFragment", "No hay tipos de denuncia disponibles")
                    Toast.makeText(activity, "No hay tipos de denuncia configurados", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                // Crear diálogo personalizado con botones coloreados
                val dialog = AlertDialog.Builder(activity)
                    .setTitle("Seleccionar Tipo de Denuncia")
                    .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
                    .create()
                
                // Crear layout para los botones
                val layout = android.widget.LinearLayout(activity).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(32, 16, 32, 16)
                }
                
                // Crear botones para cada tipo de denuncia con sus colores
                tiposDenuncia.forEach { tipoDenuncia ->
                    val button = com.google.android.material.button.MaterialButton(activity).apply {
                        text = tipoDenuncia.tipo
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 8, 0, 8)
                        }
                        
                        // Aplicar color del sheet si existe, sino usar color por defecto
                        val colorHex = tipoDenuncia.color.takeIf { it.isNotBlank() && it.startsWith("#") } ?: "#F44336"
                        Log.d("WorkCameraFragment", "Botón ${tipoDenuncia.tipo}: colorHex='$colorHex'")
                        try {
                            val color = android.graphics.Color.parseColor(colorHex)
                            setBackgroundColor(color)
                            setTextColor(android.graphics.Color.WHITE)
                            Log.d("WorkCameraFragment", "  ✓ Color aplicado correctamente: $colorHex")
                        } catch (e: Exception) {
                            Log.e("WorkCameraFragment", "Error parseando color: $colorHex", e)
                            setBackgroundColor(android.graphics.Color.parseColor("#F44336"))
                            setTextColor(android.graphics.Color.WHITE)
                        }
                        
                        setOnClickListener {
                            dialog.dismiss()
                            Log.d("WorkCameraFragment", "Tipo de denuncia seleccionado: ${tipoDenuncia.tipo}")
                            
                            // Lanzar cámara con el tipo de denuncia seleccionado
                            fragment.lanzarCamaraConEditor(
                                activity, 
                                CameraManager.TIPO_DENUNCIA, 
                                EditorWhatsAppStyleFragment.TipoFoto.FOTO_DENUNCIA,
                                nombreProyecto = "", // No es un proyecto
                                tipoDenuncia = tipoDenuncia.tipo
                            )
                        }
                    }
                    layout.addView(button)
                }
                
                dialog.setView(layout)
                dialog.show()
                Log.d("WorkCameraFragment", "Diálogo mostrado con ${tiposDenuncia.size} botones")
            } catch (e: Exception) {
                Log.e("WorkCameraFragment", "Error en mostrarSelectorDenuncia", e)
                Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun obtenerProyectoGuardado(context: Context): String {
        val prefs = context.getSharedPreferences("WorkCameraPrefs", Context.MODE_PRIVATE)
        return prefs.getString("proyecto_seleccionado", "") ?: ""
    }
    
    private fun guardarProyectoSeleccionado(context: Context, proyecto: String) {
        val prefs = context.getSharedPreferences("WorkCameraPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("proyecto_seleccionado", proyecto).apply()
    }
    
    private fun obtenerProyectosDisponibles(activity: MainMenuActivity): Array<String> {
        Log.d("WorkCameraFragment", "=== INICIANDO obtenerProyectosDisponibles ===")
        
        // Usar el mismo directorio que PhotoScanner: filesDir/proyectos
        val proyectoDir = File(activity.filesDir, "proyectos")
        
        Log.d("WorkCameraFragment", "Proyecto dir: ${proyectoDir.absolutePath}")
        Log.d("WorkCameraFragment", "Proyecto dir existe: ${proyectoDir.exists()}")
        
        return if (proyectoDir.exists() && proyectoDir.isDirectory) {
            val subdirs = proyectoDir.listFiles { file -> 
                file.isDirectory && !file.name.startsWith(".")
            }?.map { it.name }?.sorted()?.toTypedArray() ?: arrayOf()
            
            Log.d("WorkCameraFragment", "Subdirectorios encontrados: ${subdirs.contentToString()}")
            subdirs
        } else {
            Log.w("WorkCameraFragment", "Directorio de proyectos no existe o no es directorio")
            arrayOf()
        }
    }
    
    private fun crearCarpetasProyectoEjemplo(activity: MainMenuActivity) {
        // Usar el mismo directorio que PhotoScanner: filesDir/proyectos
        val proyectoDir = File(activity.filesDir, "proyectos")
        
        val carpetasEjemplo = arrayOf("Proyecto Alpha", "Proyecto Beta", "Proyecto Gamma")
        
        carpetasEjemplo.forEach { nombreCarpeta ->
            val carpeta = File(proyectoDir, nombreCarpeta)
            if (!carpeta.exists()) {
                carpeta.mkdirs()
                Log.d("WorkCameraFragment", "Carpeta creada: ${carpeta.absolutePath}")
            }
        }
    }

    private fun mostrarEditor(activity: MainMenuActivity, file: File, tipoEditor: EditorWhatsAppStyleFragment.TipoFoto?, nombreProyecto: String?, tipoDenuncia: String? = null) {
        try {
            Log.d("WorkCameraFragment", "Iniciando mostrarEditor...")
            
            // Obtener información GPS
            val coordenadas = obtenerCoordenadasGPS()
            val precision = obtenerPrecisionGPS()
            val altitud = obtenerAltitudGPS()
            
            tipoEditor?.let { tipoFoto ->
                Log.d("WorkCameraFragment", "Creando EditorWhatsAppStyleFragment...")
                
                val editorFragment = EditorWhatsAppStyleFragment.newInstance(
                    rutaFoto = file.absolutePath,
                    equipoId = "",
                    tipoFoto = tipoFoto,
                    coordenadas = coordenadas,
                    precision = precision,
                    altitud = altitud,
                    nombreProyecto = nombreProyecto ?: "",
                    tipoDenuncia = tipoDenuncia
                )
                
                // Configurar listener para cuando se complete la edición
                editorFragment.setOnEdicionCompletadaListener { rutaFotoEditada ->
                    Log.d("WorkCameraFragment", "Edición completada: $rutaFotoEditada")
                    try {
                        val archivoOriginal = file
                        val archivoEditado = File(rutaFotoEditada)
                        
                        if (archivoEditado.exists()) {
                            // Solo eliminar el archivo temporal original de la cámara
                            // La foto editada ya está guardada en la ubicación correcta por el editor
                            archivoOriginal.delete()
                            
                            Toast.makeText(activity, "Foto editada y guardada en $nombreProyecto", Toast.LENGTH_SHORT).show()
                            Log.d("WorkCameraFragment", "Foto procesada y guardada exitosamente en: $rutaFotoEditada")
                            Log.d("WorkCameraFragment", "Archivo temporal eliminado: ${archivoOriginal.absolutePath}")
                        } else {
                            Log.e("WorkCameraFragment", "Archivo editado no existe: $rutaFotoEditada")
                        }
                    } catch (e: Exception) {
                        Log.e("WorkCameraFragment", "Error en callback de edición", e)
                    }
                }
                
                // Mostrar el editor
                val fragmentManager = activity.supportFragmentManager
                if (!fragmentManager.isStateSaved && !activity.isFinishing && !activity.isDestroyed) {
                    editorFragment.show(fragmentManager, "EditorWhatsAppStyle")
                    Log.d("WorkCamera", "Editor mostrado exitosamente")
                } else {
                    Log.e("WorkCameraFragment", "No se puede mostrar editor: state saved o activity finishing")
                }
            } ?: run {
                Log.e("WorkCameraFragment", "tipoEditor es null")
                Toast.makeText(activity, "Error: Tipo de foto no definido", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("WorkCameraFragment", "Error en mostrarEditor", e)
            Toast.makeText(activity, "Error al mostrar editor: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openGallery(activity: MainMenuActivity) {
        Log.d("WorkCameraFragment", "Abriendo galería...")
        
        // Crear y mostrar la galería personalizada
        activity.showCustomGallery()
    }
    
    // Función para mostrar diálogo de crear proyecto
    private fun mostrarDialogoCrearProyecto(activity: MainMenuActivity, fragment: WorkCameraFragment) {
        val editText = EditText(activity)
        editText.hint = "Nombre del proyecto"
        
        AlertDialog.Builder(activity)
            .setTitle("Crear nuevo proyecto")
            .setView(editText)
            .setPositiveButton("Crear") { dialog, _ ->
                val nombreProyecto = editText.text.toString().trim()
                if (nombreProyecto.isNotEmpty()) {
                    crearCarpetaProyecto(activity, nombreProyecto, fragment)
                } else {
                    Toast.makeText(activity, "Por favor, ingresa un nombre válido", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
                // Volver a mostrar el selector de proyectos
                mostrarSelectorProyecto(activity, fragment)
            }
            .show()
    }
    
    // Función para crear la carpeta del proyecto
    private fun crearCarpetaProyecto(activity: MainMenuActivity, nombreProyecto: String, fragment: WorkCameraFragment) {
        try {
            val proyectosDir = File(activity.filesDir, "proyectos")
            if (!proyectosDir.exists()) {
                proyectosDir.mkdirs()
            }
            
            val nuevaCarpetaProyecto = File(proyectosDir, nombreProyecto)
            
            if (nuevaCarpetaProyecto.exists()) {
                Toast.makeText(activity, "Ya existe un proyecto con ese nombre", Toast.LENGTH_SHORT).show()
                // Volver a mostrar el diálogo de crear proyecto
                mostrarDialogoCrearProyecto(activity, fragment)
                return
            }
            
            val creado = nuevaCarpetaProyecto.mkdirs()
            if (creado) {
                Toast.makeText(activity, "Proyecto '$nombreProyecto' creado exitosamente", Toast.LENGTH_SHORT).show()
                
                // Seleccionar automáticamente el proyecto recién creado
                guardarProyectoSeleccionado(activity, nombreProyecto)
                
                // Lanzar la cámara con el nuevo proyecto
                fragment.lanzarCamaraConEditor(
                    activity, 
                    CameraManager.TIPO_PROYECTO, 
                    EditorWhatsAppStyleFragment.TipoFoto.FOTO_PROYECTO,
                    nombreProyecto
                )
            } else {
                Toast.makeText(activity, "Error al crear el proyecto", Toast.LENGTH_SHORT).show()
                // Volver a mostrar el selector de proyectos
                mostrarSelectorProyecto(activity, fragment)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            // Volver a mostrar el selector de proyectos
            mostrarSelectorProyecto(activity, fragment)
        }
    }
}