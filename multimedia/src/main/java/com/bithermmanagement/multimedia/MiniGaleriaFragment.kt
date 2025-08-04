package com.bithermmanagement.multimedia

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bithermmanagement.multimedia.R
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.bithermmanagement.multimedia.FotoManager
import com.google.android.gms.maps.model.LatLng
import android.util.Log

class MiniGaleriaFragment : Fragment() {
    companion object {
        private const val ARG_EQUIPO_ID = "equipo_id"
        fun newInstance(equipoId: String): MiniGaleriaFragment {
            val frag = MiniGaleriaFragment()
            val args = Bundle()
            args.putString(ARG_EQUIPO_ID, equipoId)
            frag.arguments = args
            return frag
        }
    }
    
    // Interfaz de callback para comunicar con el fragmento padre
    interface OnFotoActualizadaListener {
        fun onFotoActualizada(tipoFoto: String, rutaFoto: String)
    }
    
    private var onFotoActualizadaListener: OnFotoActualizadaListener? = null
    
    fun setOnFotoActualizadaListener(listener: OnFotoActualizadaListener) {
        this.onFotoActualizadaListener = listener
    }

    private var equipoId: String? = null
    private var tipoFotoActual: String? = null
    private var fileFotoActual: File? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GaleriaCarruselAdapter
    private lateinit var fotoManager: FotoManager
    
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && fileFotoActual != null) {
            Toast.makeText(requireContext(), "Foto guardada", Toast.LENGTH_SHORT).show()
            recargarFotos()
        } else {
            fileFotoActual?.delete()
        }
    }
    
    private var tipoFotoActualEditor: EditorWhatsAppStyleFragment.TipoFoto? = null
    
    private val cameraLauncherWithEditor = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && fileFotoActual != null && fileFotoActual!!.exists()) {
            Toast.makeText(requireContext(), "Foto tomada, abriendo editor...", Toast.LENGTH_SHORT).show()
            
            // Obtener información GPS del equipo actual
            val coordenadas = obtenerCoordenadasGPS()
            val precision = obtenerPrecisionGPS()
            val altitud = obtenerAltitudGPS()
            
            // Lanzar editor de fotos WhatsApp style directamente
            tipoFotoActualEditor?.let { tipoFoto ->
                val editorFragment = EditorWhatsAppStyleFragment.newInstance(
                    rutaFoto = fileFotoActual!!.absolutePath,
                    equipoId = equipoId ?: "",
                    tipoFoto = tipoFoto,
                    coordenadas = coordenadas,
                    precision = precision,
                    altitud = altitud
                )
                
                // Configurar listener para cuando se complete la edición
                editorFragment.setOnEdicionCompletadaListener { rutaFotoEditada ->
                    // Reemplazar la foto original con la editada
                    val archivoOriginal = fileFotoActual!!
                    val archivoEditado = File(rutaFotoEditada)
                    
                    if (archivoEditado.exists()) {
                        archivoOriginal.delete()
                        archivoEditado.copyTo(archivoOriginal, overwrite = true)
                        archivoEditado.delete()
                        
                        // Actualizar base de datos con la ruta de la foto
                        val tipoFoto = tipoFotoActualEditor?.name ?: "EXTRA"
                        actualizarBaseDeDatos(tipoFoto, archivoOriginal.absolutePath)
                        
                        Toast.makeText(requireContext(), "Foto editada y guardada (Tipo: $tipoFoto)", Toast.LENGTH_SHORT).show()
                        recargarFotos()
                    }
                }
                
                // Mostrar el editor
                editorFragment.show(parentFragmentManager, "EditorWhatsAppStyle")
            }
        } else {
            fileFotoActual?.delete()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        equipoId = arguments?.getString(ARG_EQUIPO_ID)
        fotoManager = FotoManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.view_mini_galeria, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        recargarFotos()
    }

    private fun setupRecyclerView() {
        recyclerView = view?.findViewById(R.id.recyclerViewGaleria)!!
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        
        adapter = GaleriaCarruselAdapter(
            context = requireContext(),
            equipoId = equipoId ?: "",
            onFotoClick = { foto -> onFotoClick(foto) },
            onBorrarClick = { foto -> onBorrarClick(foto) },
            onEditarClick = { foto -> onEditarClick(foto) },
            onAnadirClick = { onAnadirClick() }
        )
        
        recyclerView.adapter = adapter
    }

    fun setEquipoId(nuevoId: String?) {
        equipoId = nuevoId
        recargarFotos()
    }

    private fun recargarFotos() {
        val context = requireContext()
        val tag = equipoId ?: return
        
        // Obtener rutas de fotos usando FotoManager
        val rutasFotos = fotoManager.obtenerRutasFotos(tag)
        
        // POSICIONES FIJAS según especificación del usuario
        val fotosObligatorias = listOf(
            // Posición 1: FOTO EQUIPO
            FotoItem("EQUIPO", obtenerArchivoDesdeRuta(rutasFotos[FotoManager.TIPO_EQUIPO]), true),
            // Posición 2: FOTO UBICACIÓN  
            FotoItem("UBICACION", obtenerArchivoDesdeRuta(rutasFotos[FotoManager.TIPO_UBICACION]), true),
            // Posición 3: FOTO MANIFOLD (si existe)
            FotoItem("MANIFOLD", obtenerArchivoDesdeRuta(rutasFotos[FotoManager.TIPO_MANIFOLD]), true),
            // Posición 4: MAPA GPS (generado automáticamente)
            FotoItem("MAPA", obtenerArchivoDesdeRuta(rutasFotos[FotoManager.TIPO_MAPA]), true)
        )
        
        // Posición 5+: FOTOS EXTRA
        val fotosExtra = obtenerFotosExtra(rutasFotos)
        
        adapter.actualizarFotos(fotosObligatorias, fotosExtra)
    }

    private fun obtenerArchivoDesdeRuta(ruta: String?): File? {
        return if (ruta != null && File(ruta).exists()) File(ruta) else null
    }

    private fun obtenerFotosExtra(rutasFotos: Map<String, String>): List<FotoItem> {
        val fotosExtra = mutableListOf<FotoItem>()
        
        rutasFotos.entries
            .filter { it.key.startsWith(FotoManager.TIPO_EXTRA) }
            .sortedBy { it.key }
            .forEach { (tipo, ruta) ->
                val archivo = File(ruta)
                if (archivo.exists()) {
                    fotosExtra.add(FotoItem(tipo, archivo, false))
                }
            }
        
        return fotosExtra
    }

    private fun onFotoClick(foto: FotoItem) {
        if (foto.file != null && foto.file.exists()) {
            // Mostrar foto en visor
            mostrarVisorFoto(foto.file)
        } else {
            // Tomar foto o generar mapa
            when (foto.tipo) {
                "MAPA" -> generarMapa()
                else -> {
                    // Siempre preguntar el tipo de foto antes de tomar
                    val seleccionFragment = SeleccionTipoFotoFragment.newInstance()
                    seleccionFragment.setOnTipoSeleccionadoListener { tipoFoto ->
                        tipoFotoActualEditor = tipoFoto
                        lanzarCamara(tipoFoto.name)
                    }
                    seleccionFragment.show(parentFragmentManager, "SeleccionTipoFoto")
                }
            }
        }
    }

    private fun generarMapa() {
        // Obtener coordenadas del equipo desde la BD
        // Por ahora, usar coordenadas de ejemplo
        val coordenadas = LatLng(40.4168, -3.7038) // Madrid como ejemplo
        
        try {
            val bitmap = fotoManager.generarImagenMapa(equipoId ?: "", coordenadas)
            
            // Guardar el mapa generado
            val ruta = fotoManager.guardarFoto(
                equipoId = equipoId ?: "",
                tipo = FotoManager.TIPO_MAPA,
                bitmap = bitmap,
                libro = "Libro Ejemplo", // Obtener desde BD
                unidad = "Unidad Ejemplo", // Obtener desde BD
                area = "Area Ejemplo" // Obtener desde BD
            )
            
            Toast.makeText(requireContext(), "Mapa generado", Toast.LENGTH_SHORT).show()
            recargarFotos()
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error generando mapa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onBorrarClick(foto: FotoItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar borrado")
            .setMessage("¿Borrar foto ${foto.tipo}?")
            .setPositiveButton("Borrar") { _, _ ->
                if (foto.file != null && foto.file.exists()) {
                    foto.file.delete()
                    Toast.makeText(requireContext(), "Foto borrada", Toast.LENGTH_SHORT).show()
                    recargarFotos()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun onEditarClick(foto: FotoItem) {
        foto.file?.let { file ->
            // Mostrar diálogo de selección de tipo de foto
            val seleccionFragment = SeleccionTipoFotoFragment.newInstance()
            seleccionFragment.setOnTipoSeleccionadoListener { tipoFoto ->
                // Obtener información GPS del equipo actual
                val coordenadas = obtenerCoordenadasGPS()
                val precision = obtenerPrecisionGPS()
                val altitud = obtenerAltitudGPS()
                
                // Lanzar editor de fotos WhatsApp style
                val editorFragment = EditorWhatsAppStyleFragment.newInstance(
                    rutaFoto = file.absolutePath,
                    equipoId = equipoId ?: "",
                    tipoFoto = tipoFoto,
                    coordenadas = coordenadas,
                    precision = precision,
                    altitud = altitud
                )
                
                // Configurar listener para cuando se complete la edición
                editorFragment.setOnEdicionCompletadaListener { rutaFotoEditada ->
                    // Reemplazar la foto original con la editada
                    val archivoOriginal = file
                    val archivoEditado = File(rutaFotoEditada)
                    
                    if (archivoEditado.exists()) {
                        archivoOriginal.delete()
                        archivoEditado.copyTo(archivoOriginal, overwrite = true)
                        archivoEditado.delete()
                        
                        // Actualizar base de datos con la ruta de la foto
                        val tipoFoto = tipoFoto.name
                        actualizarBaseDeDatos(tipoFoto, archivoOriginal.absolutePath)
                        
                        Toast.makeText(requireContext(), "Foto editada y guardada (Tipo: $tipoFoto)", Toast.LENGTH_SHORT).show()
                        recargarFotos()
                    }
                }
                
                // Mostrar el editor
                editorFragment.show(parentFragmentManager, "EditorWhatsAppStyle")
            }
            
            // Mostrar el diálogo de selección
            seleccionFragment.show(parentFragmentManager, "SeleccionTipoFoto")
        }
    }
    
    private fun obtenerCoordenadasGPS(): String? {
        // Obtener coordenadas del GPS del equipo actual
        return try {
            // Intentar obtener desde la base de datos del equipo
            equipoId?.let { id ->
                // Aquí deberías hacer una consulta a la base de datos
                // Por ahora usamos un valor de ejemplo
                // En producción, esto debería ser algo como:
                // val equipo = database.inspeccionDao().getEquipoPorId(id)
                // equipo?.gpsCoord
                "40.4168, -3.7038"
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun obtenerPrecisionGPS(): String? {
        // Obtener precisión del GPS
        return try {
            // Aquí deberías obtener la precisión desde el GPS
            "5.2"
        } catch (e: Exception) {
            null
        }
    }
    
    private fun obtenerAltitudGPS(): String? {
        // Obtener altitud del GPS
        return try {
            // Aquí deberías obtener la altitud desde el GPS
            "655"
        } catch (e: Exception) {
            null
        }
    }

    private fun onAnadirClick() {
        // Siempre mostrar diálogo de selección de tipo primero
        val seleccionFragment = SeleccionTipoFotoFragment.newInstance()
        seleccionFragment.setOnTipoSeleccionadoListener { tipoFoto ->
            tipoFotoActualEditor = tipoFoto
            lanzarCamaraConEditor(tipoFoto.name, tipoFoto)
        }
        
        // Mostrar el diálogo de selección
        seleccionFragment.show(parentFragmentManager, "SeleccionTipoFoto")
    }
    
    private fun lanzarCamaraConEditor(tipoExtra: String, tipoFoto: EditorWhatsAppStyleFragment.TipoFoto) {
        val tag = equipoId ?: return
        val file = MiniGaleriaUtils.getFotoFile(requireContext(), tag, tipoExtra)
        fileFotoActual = file
        tipoFotoActual = tipoExtra
        tipoFotoActualEditor = tipoFoto
        
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "com.bithermmanagement.multimedia.provider",
            file
        )
        
        cameraLauncherWithEditor.launch(uri)
    }

    private fun obtenerSiguienteNumeroExtra(): Int {
        val rutasFotos = fotoManager.obtenerRutasFotos(equipoId ?: "")
        
        val numeros = rutasFotos.entries
            .filter { it.key.startsWith(FotoManager.TIPO_EXTRA) }
            .mapNotNull { (tipo, _) ->
                try {
                    tipo.substringAfter("${FotoManager.TIPO_EXTRA}_").toInt()
                } catch (e: NumberFormatException) {
                    null
                }
            }
        
        return if (numeros.isEmpty()) 1 else numeros.maxOrNull()!! + 1
    }

    private fun mostrarVisorFoto(file: File) {
        val visorFragment = VisorFotosFragment.newInstance(file.absolutePath, "Visor de Foto")
        visorFragment.show(childFragmentManager, "VisorFotos")
    }

    private fun lanzarCamara(tipo: String) {
        val tag = equipoId ?: return
        val file = MiniGaleriaUtils.getFotoFile(requireContext(), tag, tipo)
        fileFotoActual = file
        tipoFotoActual = tipo
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "com.bithermmanagement.multimedia.provider",
            file
        )
        cameraLauncher.launch(uri)
    }

    // Método para obtener la ruta de una foto específica
    fun obtenerRutaFoto(tipo: String): String {
        val rutasFotos = fotoManager.obtenerRutasFotos(equipoId ?: "")
        return rutasFotos[tipo] ?: ""
    }

    // Método para obtener las rutas de fotos extra para guardar en la variable 'extra'
    fun obtenerRutasFotosExtra(): String {
        return fotoManager.obtenerFotosExtraString(equipoId ?: "")
    }

    // Método para cargar fotos extra desde la variable 'extra'
    fun cargarFotosExtraDesdeString(rutasString: String) {
        if (rutasString.isEmpty()) return
        
        val rutas = rutasString.split(";")
        val context = requireContext()
        val tag = equipoId ?: return
        val dir = File(context.filesDir, "fotos")
        
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        rutas.forEach { ruta ->
            val archivoOrigen = File(ruta)
            if (archivoOrigen.exists()) {
                // Copiar al directorio de fotos con el formato correcto
                val nombreArchivo = archivoOrigen.name
                if (nombreArchivo.startsWith("${tag}_EXTRA_")) {
                    val archivoDestino = File(dir, nombreArchivo)
                    archivoOrigen.copyTo(archivoDestino, overwrite = true)
                }
            }
        }
        
        recargarFotos()
    }
    
    /**
     * Actualiza la base de datos con la ruta de la foto según el tipo
     */
    private fun actualizarBaseDeDatos(tipoFoto: String, rutaFoto: String) {
        // Notificar al fragmento padre sobre la actualización de la foto
        onFotoActualizadaListener?.onFotoActualizada(tipoFoto, rutaFoto)
        
        // Para fotos extra, también actualizar en el FotoManager
        if (tipoFoto.startsWith("EXTRA")) {
            val tag = equipoId ?: return
            val fotosExtraActuales = fotoManager.obtenerFotosExtraString(tag)
            val fotosExtraList = if (fotosExtraActuales.isEmpty()) {
                listOf(rutaFoto)
            } else {
                fotosExtraActuales.split(";").toMutableList().apply {
                    add(rutaFoto)
                }
            }
            fotoManager.guardarFotosExtra(tag, fotosExtraList.joinToString(";"))
            Log.d("MiniGaleriaFragment", "Foto extra actualizada en FotoManager: $tipoFoto")
        }
    }
} 