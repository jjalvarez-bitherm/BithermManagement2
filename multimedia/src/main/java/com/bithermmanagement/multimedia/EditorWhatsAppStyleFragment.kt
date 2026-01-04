package com.bithermmanagement.multimedia

import android.app.AlertDialog
import android.app.Dialog
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import kotlin.math.*
import com.bithermmanagement.core.data.db.AppDatabase
import com.bithermmanagement.core.database.entities.EquipoView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EditorWhatsAppStyleFragment : DialogFragment() {
    
    @Inject
    lateinit var database: AppDatabase
    
    // Views - Barra principal
    private lateinit var photoView: PhotoView
    private lateinit var barraHerramientasPrincipal: LinearLayout
    private lateinit var btnRotar: ImageButton
    private lateinit var btnDibujar: ImageButton
    private lateinit var btnPoligonos: ImageButton
    private lateinit var btnFlecha: ImageButton
    private lateinit var btnTexto: ImageButton
    private lateinit var btnPolilinea: ImageButton
    private lateinit var btnDeshacer: ImageButton
    
    // Views - Barra contextual
    private lateinit var barraContextual: LinearLayout
    private lateinit var btnColorPicker: ImageButton
    private lateinit var seekBarGrosor: SeekBar
    private lateinit var btnFinHerramienta: Button
    
    // Views - Parte inferior
    private lateinit var etTagEquipo: EditText
    private lateinit var btnBuscarEquipo: ImageButton
    private lateinit var etComentario: EditText
    private lateinit var spinnerTipoFoto: Spinner
    private lateinit var btnCancelar: ImageButton
    private lateinit var btnAceptar: ImageButton
    private lateinit var overlayEditables: ElementosEditablesOverlay
    
    // Variables de estado
    private var rutaFotoOriginal: String? = null
    private var equipoId: String? = null
    private var coordenadas: String? = null
    private var precision: String? = null
    private var altitud: String? = null
    private var nombreProyecto: String? = null
    private var tipoDenuncia: String? = null
    private var tipoFoto: TipoFoto = TipoFoto.FOTO_EQUIPO
    private var bitmapOriginal: Bitmap? = null
    
    // Variables para gestos multi-touch
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var lastDistance = 0f
    private var lastRotation = 0f
    private var bitmapActual: Bitmap? = null
    private var rotacionActual: Float = 0f
    
    // Variables para elementos editables
    private var modoDibujo: Boolean = false
    private var modoPolilinea: Boolean = false
    private var modoTexto: Boolean = false
    private var elementosEditables = mutableListOf<ElementoEditable>()
    private var elementoActual: ElementoEditable? = null
    private var elementoSeleccionado: ElementoEditable? = null
    
    // Variables para dibujo y polilínea
    private var pathDibujo: Path? = null
    private var pathPolilinea: Path? = null
    private var puntosPolilinea = mutableListOf<PointF>()
    private var paintDibujo: Paint? = null
    private var colorActual: Int = Color.RED
    private var grosorActual: Float = 8f
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    
    // Variables para manipular imagen de fondo
    private var imagenFondoEscala: Float = 1f
    private var imagenFondoRotacion: Float = 0f
    private var imagenFondoTranslacionX: Float = 0f
    private var imagenFondoTranslacionY: Float = 0f
    
    // Listener para notificar cuando se complete la edición
    private var onEdicionCompletadaListener: ((String) -> Unit)? = null
    private var guardadoEnProgreso = false
    
    // Sistema de capas
    private lateinit var layerBase: LayerBase
    private var estadoEditor: EstadoEditor = EstadoEditor.AJUSTANDO_FOTO
    private var fotoConfirmada: Boolean = false
    
    // Referencia al elemento STAMP para actualizarlo dinámicamente
    private var elementoStamp: ElementoEditable? = null
    private var tagActual: String = "SIN EQUIPO ASIG."
    private var comentarioActual: String = ""
    
    fun setOnEdicionCompletadaListener(listener: (String) -> Unit) {
        onEdicionCompletadaListener = listener
    }
    
    enum class TipoFoto(val descripcion: String, val numero: Int) {
        FOTO_EQUIPO("EQUIPO", 1),
        FOTO_UBICACION("UBICACIÓN", 2),
        FOTO_MANIFOLD("MANIFOLD", 3),
        CAPTURA_GPS("GPS", 4),
        FOTO_NOTA("NOTA", 5),
        FOTO_DENUNCIA("DENUNCIA", 6),
        FOTO_PROYECTO("PROYECTO", 7),
        EXTRAS("EXTRA", 8)
    }
    
    data class ElementoEditable(
        var tipo: TipoElemento,
        var x: Float,
        var y: Float,
        var escala: Float = 1f,
        var rotacion: Float = 0f,
        var texto: String = "",
        var color: Int = Color.RED,
        var path: Path? = null,
        var width: Float = 100f,
        var height: Float = 100f,
        var strokeWidth: Float = 8f,
        var bitmap: Bitmap? = null,
        var seleccionado: Boolean = false,
        // Datos dinámicos para STAMP
        var stampTag: String = "SIN EQUIPO ASIG.",
        var stampComentario: String = "",
        var stampCoordenadas: String = "0.0, 0.0 (0m)",
        var stampFecha: String = ""
    )
    
    enum class TipoElemento {
        FLECHA, CIRCULO, CUADRADO, TRIANGULO, TEXTO, DIBUJO, LINEA, STAMP, LOGO
    }
    
    enum class EstadoEditor {
        AJUSTANDO_FOTO,      // Usuario ajusta foto inicial (zoom/pan/rotate) - solo botones GIRAR/OK
        EDITANDO_ELEMENTOS   // Usuario añade/edita elementos - barra completa de herramientas
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.fragment_editor_whatsapp_style)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return dialog
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editor_whatsapp_style, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EditorWhatsApp", "=== ONVIEWCREATED INICIADO ===")
        
        // Obtener argumentos
        arguments?.let { args ->
            rutaFotoOriginal = args.getString("ruta_foto")
            equipoId = args.getString("equipo_id")
            coordenadas = args.getString("coordenadas")
            precision = args.getString("precision")
            altitud = args.getString("altitud")
            nombreProyecto = args.getString("nombre_proyecto")
            tipoDenuncia = args.getString("tipo_denuncia")
            tipoFoto = TipoFoto.values()[args.getInt("tipo_foto", 0)]
        }
        
        inicializarViews(view)
        configurarUI()
        configurarListeners()
        cargarFotoOriginal()
        
        Log.d("EditorWhatsApp", "=== ONVIEWCREATED COMPLETADO ===")
    }
    
    private fun inicializarViews(view: View) {
        Log.d("EditorWhatsApp", "=== INICIALIZANDO VIEWS ===")
        
        // Vista de foto
        photoView = view.findViewById(R.id.photoView)
        overlayEditables = view.findViewById(R.id.overlayEditables)
        
        // Barra principal
        barraHerramientasPrincipal = view.findViewById(R.id.barraHerramientasPrincipal)
        btnRotar = view.findViewById(R.id.btnRotar)
        btnDibujar = view.findViewById(R.id.btnDibujar)
        btnPoligonos = view.findViewById(R.id.btnPoligonos)
        btnFlecha = view.findViewById(R.id.btnFlecha)
        btnTexto = view.findViewById(R.id.btnTexto)
        btnPolilinea = view.findViewById(R.id.btnPolilinea)
        btnDeshacer = view.findViewById(R.id.btnDeshacer)
        
        // Barra contextual
        barraContextual = view.findViewById(R.id.barraContextual)
        btnColorPicker = view.findViewById(R.id.btnColorPicker)
        seekBarGrosor = view.findViewById(R.id.seekBarGrosor)
        btnFinHerramienta = view.findViewById(R.id.btnFinHerramienta)
        
        // Parte inferior
        etTagEquipo = view.findViewById(R.id.etTagEquipo)
        btnBuscarEquipo = view.findViewById(R.id.btnBuscarEquipo)
        etComentario = view.findViewById(R.id.etComentario)
        spinnerTipoFoto = view.findViewById(R.id.spinnerTipoFoto)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnAceptar = view.findViewById(R.id.btnAceptar)
        
        Log.d("EditorWhatsApp", "Views inicializadas correctamente")
        
        Log.d("EditorWhatsApp", "=== TODOS LOS VIEWS INICIALIZADOS ===")
    }
    
    private fun configurarUI() {
        // Campo TAG siempre visible y editable
        etTagEquipo.visibility = View.VISIBLE
        if (tipoFoto == TipoFoto.FOTO_EQUIPO) {
            etTagEquipo.setText(equipoId ?: "")
        }
        
        // Configurar PhotoView
        photoView.maximumScale = 5f
        photoView.minimumScale = 0.5f
        photoView.mediumScale = 2f
        
        // Configurar spinner de tipo de foto dinámicamente basado en el contexto
        Log.d("EditorWhatsApp", "=== CONFIGURANDO SPINNER ===")
        Log.d("EditorWhatsApp", "tipoFoto: $tipoFoto")
        Log.d("EditorWhatsApp", "nombreProyecto: '$nombreProyecto'")
        Log.d("EditorWhatsApp", "nombreProyecto.isNullOrEmpty(): ${nombreProyecto.isNullOrEmpty()}")
        
        val tipos = when {
            tipoFoto == TipoFoto.CAPTURA_GPS -> arrayOf("GPS", "UBICACIÓN", "EXTRA")
            tipoFoto == TipoFoto.FOTO_NOTA -> arrayOf("NOTA", "EXTRA")
            tipoFoto == TipoFoto.FOTO_DENUNCIA -> arrayOf("DENUNCIA", "EXTRA")
            tipoFoto == TipoFoto.FOTO_PROYECTO || !nombreProyecto.isNullOrEmpty() -> {
                // Para fotos de proyecto, obtener todos los proyectos disponibles
                val proyectosDir = File(requireContext().filesDir, "proyectos")
                if (proyectosDir.exists() && proyectosDir.isDirectory) {
                    val proyectos = proyectosDir.listFiles { file -> 
                        file.isDirectory && !file.name.startsWith(".")
                    }?.map { it.name }?.sorted()?.toTypedArray() ?: arrayOf("PROYECTO")
                    
                    if (proyectos.isEmpty()) arrayOf("PROYECTO") else proyectos
                } else {
                    arrayOf("PROYECTO")
                }
            }
            else -> arrayOf("EQUIPO", "UBICACIÓN", "MANIFOLD", "EXTRA")
        }
        
        Log.d("EditorWhatsApp", "Tipos array: ${tipos.contentToString()}")
        Log.d("EditorWhatsApp", "Tipos array size: ${tipos.size}")
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoFoto.adapter = adapter
        
        // Seleccionar el tipo actual como primera opción
        val posicionActual = when {
            tipoFoto == TipoFoto.FOTO_EQUIPO -> 0
            tipoFoto == TipoFoto.FOTO_UBICACION -> 1
            tipoFoto == TipoFoto.FOTO_MANIFOLD -> 2
            tipoFoto == TipoFoto.CAPTURA_GPS -> 0
            tipoFoto == TipoFoto.FOTO_NOTA -> 0
            tipoFoto == TipoFoto.FOTO_DENUNCIA -> 0
            tipoFoto == TipoFoto.FOTO_PROYECTO || !nombreProyecto.isNullOrEmpty() -> {
                if (!nombreProyecto.isNullOrEmpty()) {
                    tipos.indexOf(nombreProyecto).takeIf { it >= 0 } ?: 0
                } else {
                    0
                }
            }
            else -> tipos.size - 1 // EXTRA como última opción
        }
        spinnerTipoFoto.setSelection(posicionActual)
        
        // ✅ FIX: Actualizar nombreProyecto cuando cambia spinner
        spinnerTipoFoto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seleccionado = parent?.getItemAtPosition(position).toString()
                val proyectosDisponibles = obtenerProyectosDisponibles()
                
                Log.d("EditorWhatsApp", "📝 Spinner cambiado a: '$seleccionado'")
                
                if (proyectosDisponibles.contains(seleccionado)) {
                    // Es un proyecto específico
                    nombreProyecto = seleccionado
                    tipoFoto = TipoFoto.FOTO_PROYECTO
                    Log.d("EditorWhatsApp", "✓ Proyecto actualizado: '$nombreProyecto'")
                } else {
                    // Mapear a otros tipos
                    when (seleccionado) {
                        "EQUIPO" -> tipoFoto = TipoFoto.FOTO_EQUIPO
                        "UBICACIÓN" -> tipoFoto = TipoFoto.FOTO_UBICACION
                        "MANIFOLD" -> tipoFoto = TipoFoto.FOTO_MANIFOLD
                        "GPS" -> tipoFoto = TipoFoto.CAPTURA_GPS
                        "NOTA" -> tipoFoto = TipoFoto.FOTO_NOTA
                        "DENUNCIA" -> tipoFoto = TipoFoto.FOTO_DENUNCIA
                        "EXTRA" -> tipoFoto = TipoFoto.EXTRAS
                        else -> {
                            Log.w("EditorWhatsApp", "⚠️ Tipo desconocido: '$seleccionado'")
                            tipoFoto = TipoFoto.EXTRAS
                        }
                    }
                    Log.d("EditorWhatsApp", "✓ Tipo foto actualizado: $tipoFoto")
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                Log.d("EditorWhatsApp", "Spinner: Nada seleccionado")
            }
        }
        
        // Configurar paint para dibujo con valores iniciales
        actualizarPaintDibujo()
        
        // Configurar el overlay de elementos editables
        overlayEditables.setPhotoView(photoView)
        overlayEditables.setElementosEditables(elementosEditables)
        
        // Configurar SeekBar de grosor
        seekBarGrosor.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                grosorActual = maxOf(1f, progress.toFloat())
                actualizarPaintDibujo()
                
                // Actualizar preview si estamos en modo dibujo
                if (modoDibujo || modoPolilinea) {
                    overlayEditables.setPaintPreview(paintDibujo!!)
                    overlayEditables.invalidate()
                }
                
                // Actualizar elemento seleccionado
                elementoSeleccionado?.let { elemento ->
                    elemento.strokeWidth = grosorActual
                    redibujarElementos()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        Log.d("EditorWhatsApp", "UI configurada")
    }
    
    private fun actualizarPaintDibujo() {
        paintDibujo = Paint().apply {
            color = colorActual
            strokeWidth = grosorActual
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        
        // Actualizar preview en overlay si estamos en modo dibujo
        if (modoDibujo || modoPolilinea) {
            overlayEditables.setPaintPreview(paintDibujo!!)
            overlayEditables.invalidate()
        }
    }
    
    /**
     * Muestra barra de ajuste de foto (solo GIRAR y OK visible)
     * Se usa cuando estadoEditor == AJUSTANDO_FOTO
     */
    private fun mostrarBarraAjusteFoto() {
        Log.d("EditorWhatsApp", "=== MOSTRANDO BARRA AJUSTE FOTO ===")
        
        // Ocultar todos los botones de herramientas excepto GIRAR
        btnDibujar.visibility = View.GONE
        btnPoligonos.visibility = View.GONE
        btnFlecha.visibility = View.GONE
        btnTexto.visibility = View.GONE
        btnPolilinea.visibility = View.GONE
        btnDeshacer.visibility = View.GONE
        
        // Solo GIRAR visible
        btnRotar.visibility = View.VISIBLE
        
        // Ocultar campos de texto y búsqueda (TAG, Comentario, lupa)
        etTagEquipo.visibility = View.GONE
        etComentario.visibility = View.GONE
        btnBuscarEquipo.visibility = View.GONE
        
        // Ocultar barra contextual (no se usa en modo ajuste)
        barraContextual.visibility = View.GONE
        
        // Mostrar preview del marco cuadrado 1:1
        overlayEditables.setMostrarMarcoPreview(true)
        overlayEditables.visibility = View.VISIBLE
        
        estadoEditor = EstadoEditor.AJUSTANDO_FOTO
        Log.d("EditorWhatsApp", "✓ Estado: AJUSTANDO_FOTO - solo GIRAR/OK visibles + marco preview")
    }
    
    /**
     * Muestra barra completa de herramientas (todos los botones)
     * Se usa cuando estadoEditor == EDITANDO_ELEMENTOS
     */
    private fun mostrarBarraHerramientas() {
        Log.d("EditorWhatsApp", "=== MOSTRANDO BARRA HERRAMIENTAS COMPLETA ===")
        
        // Mostrar todos los botones de herramientas
        btnRotar.visibility = View.VISIBLE
        btnDibujar.visibility = View.VISIBLE
        btnPoligonos.visibility = View.VISIBLE
        btnFlecha.visibility = View.VISIBLE
        btnTexto.visibility = View.VISIBLE
        btnPolilinea.visibility = View.VISIBLE
        btnDeshacer.visibility = View.VISIBLE
        
        // Mostrar campos de texto y búsqueda (TAG, Comentario, lupa)
        etTagEquipo.visibility = View.VISIBLE
        etComentario.visibility = View.VISIBLE
        btnBuscarEquipo.visibility = View.VISIBLE
        
        estadoEditor = EstadoEditor.EDITANDO_ELEMENTOS
        Log.d("EditorWhatsApp", "✓ Estado: EDITANDO_ELEMENTOS - todas herramientas visibles")
    }
    
    /**
     * Confirma el ajuste de la foto y la rasteriza al layer base.
     * Después de esto, la foto ya no es editable y se activan las herramientas.
     */
    private fun confirmarFoto() {
        Log.d("EditorWhatsApp", "=== CONFIRMANDO FOTO ===")
        
        try {
            // Desactivar preview del marco
            overlayEditables.setMostrarMarcoPreview(false)
            
            bitmapActual?.let { bitmap ->
                // Obtener el RectF de la imagen mostrada en el PhotoView (cambia con zoom/pan)
                val displayRect = photoView.displayRect
                
                Log.d("EditorWhatsApp", "Display Rect: $displayRect")
                Log.d("EditorWhatsApp", "Bitmap original: ${bitmap.width}x${bitmap.height}")
                
                // MARCO FIJO en coordenadas de pantalla (NO cambia con zoom)
                val marcoSize = minOf(overlayEditables.width, overlayEditables.height).toFloat()
                val marcoLeft = (overlayEditables.width - marcoSize) / 2f
                val marcoTop = (overlayEditables.height - marcoSize) / 2f
                val marcoRight = marcoLeft + marcoSize
                val marcoBottom = marcoTop + marcoSize
                
                Log.d("EditorWhatsApp", "Marco FIJO pantalla: $marcoSize x $marcoSize en ($marcoLeft, $marcoTop)")
                Log.d("EditorWhatsApp", "DisplayRect (imagen): ${displayRect.width()}x${displayRect.height()} en (${displayRect.left}, ${displayRect.top})")
                
                // Calcular INTERSECCIÓN entre marco fijo y displayRect
                val intersectLeft = maxOf(marcoLeft, displayRect.left)
                val intersectTop = maxOf(marcoTop, displayRect.top)
                val intersectRight = minOf(marcoRight, displayRect.right)
                val intersectBottom = minOf(marcoBottom, displayRect.bottom)
                
                // Convertir intersección a coordenadas relativas al displayRect
                val cropLeftDisplay = intersectLeft - displayRect.left
                val cropTopDisplay = intersectTop - displayRect.top
                val cropWidthDisplay = intersectRight - intersectLeft
                val cropHeightDisplay = intersectBottom - intersectTop
                
                Log.d("EditorWhatsApp", "Intersección display: ($cropLeftDisplay, $cropTopDisplay) ${cropWidthDisplay}x$cropHeightDisplay")
                
                // Escala entre imagen original y imagen mostrada
                val escalaDisplay = displayRect.width() / bitmap.width.toFloat()
                
                Log.d("EditorWhatsApp", "Escala display: $escalaDisplay")
                
                // Convertir a coordenadas de la imagen original
                val srcLeft = (cropLeftDisplay / escalaDisplay).toInt().coerceIn(0, bitmap.width)
                val srcTop = (cropTopDisplay / escalaDisplay).toInt().coerceIn(0, bitmap.height)
                val srcWidth = (cropWidthDisplay / escalaDisplay).toInt()
                val srcHeight = (cropHeightDisplay / escalaDisplay).toInt()
                
                // Asegurar que no excedemos los límites del bitmap
                val srcRight = (srcLeft + srcWidth).coerceAtMost(bitmap.width)
                val srcBottom = (srcTop + srcHeight).coerceAtMost(bitmap.height)
                
                val finalWidth = srcRight - srcLeft
                val finalHeight = srcBottom - srcTop
                
                // Forzar CUADRADO usando el menor lado
                val finalSize = minOf(finalWidth, finalHeight)
                
                Log.d("EditorWhatsApp", "Crop original: ($srcLeft, $srcTop) tamaño ${finalSize}x${finalSize}")
                
                // Crear bitmap recortado CUADRADO de la imagen original
                val bitmapRecortado = Bitmap.createBitmap(bitmap, srcLeft, srcTop, finalSize, finalSize)
                
                Log.d("EditorWhatsApp", "✓ Bitmap recortado cuadrado: ${bitmapRecortado.width}x${bitmapRecortado.height}")
                
                // Rasterizar foto recortada al layer base
                layerBase.rasterizarFoto(bitmapRecortado, Matrix())
                Log.d("EditorWhatsApp", "✓ Foto rasterizada al layer base 1920x1920")
                
                // Ocultar PhotoView
                photoView.visibility = View.GONE
                
                // Mostrar preview del layer base en el overlay
                overlayEditables.setLayerBase(layerBase.getBitmap())
                overlayEditables.visibility = View.VISIBLE
                
                // Añadir STAMP y LOGO automáticamente CON COORDENADAS DEL LAYER BASE
                añadirStampAutomaticoLayerBase()
                añadirLogoAutomaticoLayerBase()
                
                // Cambiar a modo edición de elementos
                mostrarBarraHerramientas()
                fotoConfirmada = true
                
                Log.d("EditorWhatsApp", "✓ Foto confirmada - modo edición de elementos activado")
            }
        } catch (e: Exception) {
            Log.e("EditorWhatsApp", "ERROR confirmando foto", e)
        }
    }
    
    /**
     * Añade STAMP con coordenadas del layer base (1920x1920px).
     * Se añade como elemento editable para permitir actualizaciones dinámicas.
     */
    private fun añadirStampAutomaticoLayerBase() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO STAMP AL LAYER BASE ===")
        
        val coordsTexto = if (!coordenadas.isNullOrEmpty()) {
            val coords = coordenadas!!.split(",")
            val lat = coords.getOrNull(0)?.trim() ?: "0.0"
            val lon = coords.getOrNull(1)?.trim() ?: "0.0"
            val prec = precision ?: "0"
            "$lat, $lon (${prec}m)"
        } else {
            "0.0, 0.0 (0m)"
        }
        
        val tagInicial = if (etTagEquipo.text.isNotBlank()) {
            etTagEquipo.text.toString()
        } else {
            "SIN EQUIPO ASIG."
        }
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.STAMP,
            x = 40f, // Coordenadas del LAYER BASE 1920x1920 (mantener margen de 40px)
            y = 40f,
            width = 550f * 1.2f,  // 120% del tamaño (660f)
            height = 250f * 1.2f,  // 120% del tamaño (300f)
            color = Color.BLACK,
            seleccionado = false,
            stampTag = tagInicial,
            stampComentario = etComentario.text.toString(),
            stampCoordenadas = coordsTexto,
            stampFecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
        
        // Guardar referencia al STAMP para actualizaciones dinámicas
        elementoStamp = elemento
        elementosEditables.add(elemento)
        
        // Configurar listeners para actualizar dinámicamente
        configurarListenersStamp()
        
        // Redibujar elementos
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "✓ STAMP rasterizado al layer base en (40, 40)")
    }
    
    /**
     * Añade LOGO con coordenadas del layer base (1920x1920px).
     * Se añade como elemento editable.
     */
    private fun añadirLogoAutomaticoLayerBase() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO LOGO AL LAYER BASE ===")
        
        try {
            // Cargar logo desde assets
            val inputStream = requireContext().assets.open("LogoBithermSWW.png")
            val logoBitmap = BitmapFactory.decodeStream(inputStream)
            
            val logoWidth = 550f * 1.2f  // 120% del tamaño (660f)
            val logoHeight = 250f * 1.2f  // 120% del tamaño (300f)
            val layerSize = LayerBase.LAYER_SIZE.toFloat()
            
            val elemento = ElementoEditable(
                tipo = TipoElemento.LOGO,
                x = layerSize - logoWidth - 40f, // Mantener margen de 40px desde borde derecho
                y = 40f, // Mantener margen de 40px desde borde superior
                width = logoWidth,
                height = logoHeight,
                bitmap = logoBitmap,
                seleccionado = false
            )
            
            // Añadir como elemento editable
            elementosEditables.add(elemento)
            redibujarElementos()
            
            Log.d("EditorWhatsApp", "✓ LOGO añadido en (${elemento.x}, 40)")
        } catch (e: Exception) {
            Log.e("EditorWhatsApp", "Error cargando logo", e)
        }
    }
    
    private fun configurarListeners() {
        Log.d("EditorWhatsApp", "=== CONFIGURANDO LISTENERS ===")
        
        // Botones de la barra principal
        btnRotar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Rotar presionado")
            rotarImagen()
        }
        
        btnDibujar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Dibujar presionado")
            activarModoDibujo()
        }
        
        btnPoligonos.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Polígonos presionado")
            mostrarSelectorPoligonos()
        }
        
        btnFlecha.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Flecha presionado")
            añadirFlecha()
        }
        
        btnTexto.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Texto presionado")
            activarModoTexto()
        }
        
        btnPolilinea.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Polilínea presionado")
            activarModoPolilinea()
        }
        
        btnDeshacer.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Deshacer presionado")
            deshacerUltimoElemento()
        }
        
        btnBuscarEquipo.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Buscar Equipo presionado")
            mostrarDialogoBuscarEquipo()
        }
        
        // Botones de la barra contextual
        btnColorPicker.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Color Picker presionado")
            mostrarSelectorColor()
        }
        
        btnFinHerramienta.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón FIN presionado")
            finalizarHerramientaActual()
        }
        
        btnCancelar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Cancelar presionado")
            Log.d("EditorWhatsApp", "🎭 EDITOR CANCELADO SIN GUARDAR")
            dismiss()
        }
        
        btnAceptar.setOnClickListener {
            when (estadoEditor) {
                EstadoEditor.AJUSTANDO_FOTO -> {
                    // Usuario confirma ajuste de foto → rasterizar al layer base
                    Log.d("EditorWhatsApp", "OK presionado - Confirmando foto")
                    confirmarFoto()
                }
                EstadoEditor.EDITANDO_ELEMENTOS -> {
                    // Usuario guarda foto final
                    if (guardadoEnProgreso) {
                        Log.w("EditorWhatsApp", "Guardado ya en progreso, ignorando click")
                        return@setOnClickListener
                    }
                    Log.d("EditorWhatsApp", "Botón GUARDAR presionado")
                    Log.d("EditorWhatsApp", "🎭 EDITOR ACEPTADO - GUARDANDO FOTO")
                    guardadoEnProgreso = true
                    btnAceptar.isEnabled = false
                    guardarFoto()
                }
            }
        }
        
        // Configurar touch listener para dibujo y selección de elementos
        overlayEditables.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    lastTouchY = event.y
                    activePointerId = event.getPointerId(0)
                    
                    if (modoDibujo) {
                        // Usar coordenadas del OVERLAY directamente - igual que polilínea
                        pathDibujo = Path()
                        pathDibujo?.moveTo(event.x, event.y)
                        overlayEditables.setPathPreview(pathDibujo)
                        overlayEditables.invalidate()
                        true
                    } else if (modoPolilinea) {
                        // Usar coordenadas del OVERLAY directamente
                        puntosPolilinea.add(PointF(event.x, event.y))
                        
                        if (puntosPolilinea.size == 1) {
                            pathPolilinea = Path()
                            pathPolilinea?.moveTo(event.x, event.y)
                        } else {
                            pathPolilinea?.lineTo(event.x, event.y)
                        }
                        
                        overlayEditables.setPathPreview(pathPolilinea)
                        overlayEditables.invalidate()
                        true
                    } else {
                        // Manejar selección de elementos
                        manejarSeleccionElemento(event.x, event.y)
                        // Devolver true si se seleccionó un elemento, false si no
                        elementoSeleccionado != null
                    }
                }
                
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Segundo dedo - iniciar gestos de zoom/rotación
                    if (event.pointerCount == 2) {
                        lastDistance = calcularDistancia(event)
                        lastRotation = calcularRotacion(event)
                    }
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    when {
                        modoDibujo && pathDibujo != null -> {
                            // Dibujar en coordenadas del OVERLAY - capturar muchos puntos para dibujo suave
                            // Usar quadTo para líneas más suaves en lugar de lineTo
                            val historySize = event.historySize
                            for (i in 0 until historySize) {
                                val historicalX = event.getHistoricalX(i)
                                val historicalY = event.getHistoricalY(i)
                                pathDibujo?.lineTo(historicalX, historicalY)
                            }
                            pathDibujo?.lineTo(event.x, event.y)
                            // Mostrar preview en tiempo real
                            overlayEditables.setPathPreview(pathDibujo)
                            overlayEditables.invalidate()
                            true
                        }
                        
                        modoPolilinea -> {
                            // Mostrar preview de la polilínea en tiempo real
                            overlayEditables.setPathPreview(pathPolilinea)
                            overlayEditables.invalidate()
                            true
                        }
                        
                        event.pointerCount == 2 && elementoSeleccionado != null -> {
                            // Gestos de dos dedos: zoom y rotación del elemento seleccionado
                            manejarGestosDosDedesElemento(event)
                            true
                        }
                        
                        event.pointerCount == 2 && elementoSeleccionado == null -> {
                            // Gestos de dos dedos: zoom y rotación de la imagen de fondo
                            manejarGestosDosDedesImagenFondo(event)
                            true
                        }
                        
                        event.pointerCount == 1 && elementoSeleccionado != null -> {
                            // Un dedo: mover elemento seleccionado
                            val pointerIndex = event.findPointerIndex(activePointerId)
                            if (pointerIndex >= 0) {
                                moverElemento(event.getX(pointerIndex), event.getY(pointerIndex))
                            }
                            true
                        }
                        
                        event.pointerCount == 1 && elementoSeleccionado == null -> {
                            // Un dedo: mover imagen de fondo
                            val pointerIndex = event.findPointerIndex(activePointerId)
                            if (pointerIndex >= 0) {
                                val dx = event.getX(pointerIndex) - lastTouchX
                                val dy = event.getY(pointerIndex) - lastTouchY
                                imagenFondoTranslacionX += dx
                                imagenFondoTranslacionY += dy
                                lastTouchX = event.getX(pointerIndex)
                                lastTouchY = event.getY(pointerIndex)
                                aplicarTransformacionImagenFondo()
                            }
                            true
                        }
                        
                        else -> {
                            false
                        }
                    }
                }
                
                MotionEvent.ACTION_POINTER_UP -> {
                    // Soltar un dedo en gesto multi-touch
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    
                    if (pointerId == activePointerId) {
                        // Cambiar al otro dedo activo
                        val newPointerIndex = if (pointerIndex == 0) 1 else 0
                        lastTouchX = event.getX(newPointerIndex)
                        lastTouchY = event.getY(newPointerIndex)
                        activePointerId = event.getPointerId(newPointerIndex)
                    }
                    true
                }
                
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // NO limpiar pathDibujo aquí - se hace en finalizarHerramientaActual()
                    // Igual que la polilínea, el dibujo se guarda cuando se finaliza la herramienta
                    if (elementoSeleccionado != null) {
                        // Deseleccionar elemento
                        elementoSeleccionado?.seleccionado = false
                        elementoSeleccionado = null
                        redibujarElementos()
                        true
                    } else {
                        false
                    }
                }
                
                else -> false
            }
        }
        
        Log.d("EditorWhatsApp", "=== TODOS LOS LISTENERS CONFIGURADOS ===")
    }
    
    private fun calcularDistancia(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    private fun calcularRotacion(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f
        val dx = (event.getX(1) - event.getX(0)).toDouble()
        val dy = (event.getY(1) - event.getY(0)).toDouble()
        return Math.toDegrees(kotlin.math.atan2(dy, dx)).toFloat()
    }
    
    private fun manejarGestosDosDedesElemento(event: MotionEvent) {
        elementoSeleccionado?.let { elemento ->
            // No permitir transformar STAMP ni LOGO
            if (elemento.tipo == TipoElemento.STAMP || elemento.tipo == TipoElemento.LOGO) {
                return
            }
            
            val newDistance = calcularDistancia(event)
            val newRotation = calcularRotacion(event)
            
            // Calcular cambio de escala
            if (lastDistance > 0) {
                val scaleFactor = newDistance / lastDistance
                elemento.escala *= scaleFactor
                // Limitar escala entre 0.1x y 10x
                elemento.escala = elemento.escala.coerceIn(0.1f, 10f)
            }
            
            // Calcular cambio de rotación
            if (lastRotation != 0f) {
                var deltaRotation = newRotation - lastRotation
                // Normalizar a -180...180
                while (deltaRotation > 180) deltaRotation -= 360
                while (deltaRotation < -180) deltaRotation += 360
                elemento.rotacion += deltaRotation
            }
            
            lastDistance = newDistance
            lastRotation = newRotation
            
            redibujarElementos()
        }
    }
    
    private fun manejarGestosDosDedesImagenFondo(event: MotionEvent) {
        val newDistance = calcularDistancia(event)
        val newRotation = calcularRotacion(event)
        
        // Calcular cambio de escala
        if (lastDistance > 0) {
            val scaleFactor = newDistance / lastDistance
            imagenFondoEscala *= scaleFactor
            // Limitar escala entre 0.1x y 5x
            imagenFondoEscala = imagenFondoEscala.coerceIn(0.1f, 5f)
        }
        
        // Calcular cambio de rotación
        if (lastRotation != 0f) {
            var deltaRotation = newRotation - lastRotation
            // Normalizar a -180...180
            while (deltaRotation > 180) deltaRotation -= 360
            while (deltaRotation < -180) deltaRotation += 360
            imagenFondoRotacion += deltaRotation
        }
        
        lastDistance = newDistance
        lastRotation = newRotation
        
        aplicarTransformacionImagenFondo()
    }
    
    private fun aplicarTransformacionImagenFondo() {
        // Aplicar escala usando API de PhotoView
        photoView.scale = imagenFondoEscala
        
        // Aplicar rotación usando API de View
        photoView.rotation = imagenFondoRotacion
        
        // Aplicar traslación usando API de View
        photoView.translationX = imagenFondoTranslacionX
        photoView.translationY = imagenFondoTranslacionY
        
        // Invalidar para redibujar
        photoView.invalidate()
        overlayEditables.invalidate()
    }
    
    private fun manejarSeleccionElemento(x: Float, y: Float) {
        // CANVAS FIJO: Los elementos están en coordenadas ABSOLUTAS de pantalla
        // NO necesitamos convertir usando imageMatrix
        
        // Deseleccionar todos los elementos primero
        elementosEditables.forEach { it.seleccionado = false }
        elementoSeleccionado = null
        
        // Buscar elemento en la posición tocada (en orden inverso para priorizar los más recientes)
        for (i in elementosEditables.indices.reversed()) {
            val elemento = elementosEditables[i]
            
            // STAMP y LOGO no son seleccionables (están fijos)
            if (elemento.tipo == TipoElemento.STAMP || elemento.tipo == TipoElemento.LOGO) {
                continue
            }
            
            // Calcular bounds considerando transformaciones y agregar margen de tolerancia
            val halfWidth = (elemento.width * elemento.escala) / 2 + 200f  // +200px de tolerancia para mejor selección
            val halfHeight = (elemento.height * elemento.escala) / 2 + 200f  // +200px de tolerancia para mejor selección
            
            val rect = RectF(
                elemento.x - halfWidth,
                elemento.y - halfHeight,
                elemento.x + halfWidth,
                elemento.y + halfHeight
            )
            
            // Comparar directamente con coordenadas de pantalla
            if (rect.contains(x, y)) {
                elementoSeleccionado = elemento
                elemento.seleccionado = true
                Log.d("EditorWhatsApp", "✅ Elemento seleccionado: ${elemento.tipo} en (${elemento.x}, ${elemento.y})")
                redibujarElementos()
                return
            }
        }
        
        Log.d("EditorWhatsApp", "❌ No se seleccionó ningún elemento en ($x, $y)")
        redibujarElementos()
    }
    
    private fun moverElemento(x: Float, y: Float) {
        elementoSeleccionado?.let { elemento ->
            // CANVAS FIJO: Los elementos están en coordenadas ABSOLUTAS de pantalla
            // NO necesitamos convertir usando imageMatrix
            elemento.x = x
            elemento.y = y
            redibujarElementos()
        }
    }
    

    
    private fun redibujarElementos() {
        // Actualizar el overlay con los elementos editables
        overlayEditables.setElementosEditables(elementosEditables)
        
        // Desactivar zoom/pan del PhotoView si hay un elemento seleccionado
        // Esto permite que los gestos solo afecten al elemento, no a la imagen de fondo
        photoView.isZoomable = elementoSeleccionado == null
    }
    

    
    

    
    private fun cargarFotoOriginal() {
        Log.d("EditorWhatsApp", "=== CARGANDO FOTO ORIGINAL ===")
        
        // Inicializar layer base
        layerBase = LayerBase()
        Log.d("EditorWhatsApp", "✓ Layer base inicializado (1920x1920px)")
        
        // Mostrar barra de ajuste de foto (solo GIRAR/OK)
        mostrarBarraAjusteFoto()
        
        rutaFotoOriginal?.let { ruta ->
            Log.d("EditorWhatsApp", "🎨 FOTO ENTRA AL EDITOR: $ruta")
            Log.d("EditorWhatsApp", "🎨 Archivo existe al entrar al editor: ${File(ruta).exists()}")
            Log.d("EditorWhatsApp", "🎨 Tamaño archivo al entrar al editor: ${File(ruta).length()} bytes")
            
            val file = File(ruta)
            if (file.exists()) {
                bitmapOriginal = BitmapFactory.decodeFile(ruta)
                Log.d("EditorWhatsApp", "Bitmap original cargado: ${bitmapOriginal != null}")
                
                if (bitmapOriginal != null) {
                    Log.d("EditorWhatsApp", "Dimensiones del bitmap: ${bitmapOriginal!!.width}x${bitmapOriginal!!.height}")
                    
                    // Aplicar rotación automática de 90° horario
                    aplicarRotacionAutomatica()
                    
                    // Configurar PhotoView con imagen
                    photoView.setImageBitmap(bitmapActual)
                    photoView.visibility = View.VISIBLE
                    
                    // Configurar zoom y rotación inicial usando propiedades de View/PhotoView
                    photoView.setZoomable(true)
                    photoView.minimumScale = 0.1f
                    photoView.maximumScale = 5f
                    photoView.scale = imagenFondoEscala
                    photoView.rotation = imagenFondoRotacion
                    
                    Log.d("EditorWhatsApp", "Bitmap asignado al PhotoView")
                    Log.d("EditorWhatsApp", "📸 ESTADO: Usuario puede ajustar foto (zoom/pan/rotate)")
                    Log.d("EditorWhatsApp", "📸 MIRILLA: Marco cuadrado muestra área final 1:1")
                } else {
                    Log.e("EditorWhatsApp", "ERROR: No se pudo cargar el bitmap desde: $ruta")
                }
            } else {
                Log.e("EditorWhatsApp", "ERROR: El archivo no existe en: $ruta")
            }
        } ?: run {
            Log.e("EditorWhatsApp", "ERROR: rutaFotoOriginal es null")
        }
        
        Log.d("EditorWhatsApp", "=== CARGA DE FOTO ORIGINAL COMPLETADA ===")
    }
    
    private fun aplicarRotacionAutomatica() {
        bitmapOriginal?.let { bitmap ->
            Log.d("EditorWhatsApp", "=== APLICANDO ROTACIÓN AUTOMÁTICA ===")
            
            // No aplicar rotación automática - el usuario ajustará manualmente
            rotacionActual = 0f
            bitmapActual = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            
            // No aplicar rotación en la matriz
            val matrix = Matrix()
            // matrix.postRotate(0f) // Sin rotación
            
            // Aplicar la rotación
            val bitmapRotado = Bitmap.createBitmap(bitmapActual!!, 0, 0, bitmapActual!!.width, bitmapActual!!.height, matrix, true)
            bitmapActual = bitmapRotado
            
            Log.d("EditorWhatsApp", "Rotación de 90° antihorario aplicada automáticamente")
            Log.d("EditorWhatsApp", "Dimensiones después de rotación: ${bitmapActual?.width}x${bitmapActual?.height}")
            
            // NO añadir STAMP/LOGO aún - se añaden después de confirmar foto
            Log.d("EditorWhatsApp", "STAMP y LOGO se añadirán al confirmar foto")
        } ?: run {
            Log.e("EditorWhatsApp", "ERROR: bitmapOriginal es null, no se puede aplicar rotación")
        }
    }
    
    private fun añadirStampAutomatico() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO STAMP AUTOMÁTICO (COORDENADAS ABSOLUTAS) ===")
        
        // Calcular posición relativa al marco cuadrado
        overlayEditables.post {
            val tamaño = minOf(overlayEditables.width, overlayEditables.height).toFloat()
            val marcoLeft = (overlayEditables.width - tamaño) / 2f
            val marcoTop = (overlayEditables.height - tamaño) / 2f
            
            // Obtener coordenadas actuales
            val coordsTexto = if (!coordenadas.isNullOrEmpty()) {
                val coords = coordenadas!!.split(",")
                val lat = coords.getOrNull(0)?.trim() ?: "0.0"
                val lon = coords.getOrNull(1)?.trim() ?: "0.0"
                val prec = precision ?: "0"
                "$lat, $lon (${prec}m)"
            } else {
                "0.0, 0.0 (0m)"
            }
            
            // Obtener TAG inicial desde etTagEquipo
            val tagInicial = if (etTagEquipo.text.isNotBlank()) {
                etTagEquipo.text.toString()
            } else {
                "SIN EQUIPO ASIG."
            }
            
            val elemento = ElementoEditable(
                tipo = TipoElemento.STAMP,
                x = marcoLeft + 15f, // 15px desde borde izquierdo (mantener margen)
                y = marcoTop + 15f,  // 15px desde borde superior (mantener margen)
                width = 550f * 1.2f,  // 120% del tamaño original (660f)
                height = 250f * 1.2f, // 120% del tamaño original (300f)
                color = Color.BLACK,
                seleccionado = false, // STAMP no es seleccionable
                stampTag = tagInicial,
                stampComentario = etComentario.text.toString(),
                stampCoordenadas = coordsTexto,
                stampFecha = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
            )
            
            elementosEditables.add(elemento)
            elementoStamp = elemento // Guardar referencia para actualizarlo
            
            // Configurar listeners para actualizar STAMP dinámicamente
            configurarListenersStamp()
            
            redibujarElementos()
            
            Log.d("EditorWhatsApp", "✓ Stamp añadido en (${elemento.x}, ${elemento.y}) - esquina superior izquierda del cuadrado")
        }
    }
    
    private fun añadirLogoAutomatico() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO LOGO AUTOMÁTICO (COORDENADAS ABSOLUTAS) ===")
        
        // Calcular posición relativa al marco cuadrado
        overlayEditables.post {
            val tamaño = minOf(overlayEditables.width, overlayEditables.height).toFloat()
            val marcoLeft = (overlayEditables.width - tamaño) / 2f
            val marcoRight = marcoLeft + tamaño
            
            val marcoTop = (overlayEditables.height - tamaño) / 2f
            
            val logoWidth = 550f * 1.2f  // 120% del tamaño original (660f)
            val logoHeight = 250f * 1.2f // 120% del tamaño original (300f)
            
            val elemento = ElementoEditable(
                tipo = TipoElemento.LOGO,
                x = marcoRight - logoWidth - 15f, // 15px desde borde derecho (mantener margen)
                y = marcoTop + 15f,  // 15px desde borde superior (mantener margen)
                width = logoWidth,
                height = logoHeight,
                color = Color.parseColor("#FF5722"),
                seleccionado = false // LOGO no es seleccionable
            )
            
            elementosEditables.add(elemento)
            redibujarElementos()
            
            Log.d("EditorWhatsApp", "✓ Logo añadido en (${elemento.x}, ${elemento.y}) - esquina superior derecha del cuadrado")
        }
    }
    
    /**
     * Configura listeners para actualizar el STAMP dinámicamente
     * cuando el usuario escribe en los campos TAG y Comentario
     */
    private fun configurarListenersStamp() {
        // Listener para TAG
        etTagEquipo.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                elementoStamp?.let { stamp ->
                    stamp.stampTag = if (s.isNullOrBlank()) "SIN EQUIPO ASIG." else s.toString()
                    redibujarElementos()
                    Log.d("EditorWhatsApp", "✓ STAMP actualizado - TAG: ${stamp.stampTag}")
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        
        // Listener para Comentario
        etComentario.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                elementoStamp?.let { stamp ->
                    stamp.stampComentario = s?.toString() ?: ""
                    redibujarElementos()
                    Log.d("EditorWhatsApp", "✓ STAMP actualizado - Comentario: ${stamp.stampComentario}")
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
    
    
    private fun rotarImagen() {
        bitmapActual?.let { bitmap ->
            rotacionActual += 90f
            if (rotacionActual >= 360) rotacionActual = 0f
            
            val bitmapRotado = aplicarRotacion(bitmap, rotacionActual)
            bitmapActual = bitmapRotado
            photoView.setImageBitmap(bitmapRotado)
            
            Log.d("EditorWhatsApp", "Imagen rotada: ${rotacionActual.toInt()}°")
        }
    }
    
    private fun aplicarRotacion(bitmap: Bitmap, angulo: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angulo)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun añadirCirculo() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO CÍRCULO ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.CIRCULO,
            x = overlayEditables.width / 2f,
            y = overlayEditables.height / 2f,
            width = 150f,
            height = 150f,
            color = Color.BLUE,
            seleccionado = true
        )
        
        // Deseleccionar otros elementos
        elementosEditables.forEach { it.seleccionado = false }
        elemento.seleccionado = true
        elementoSeleccionado = elemento
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Círculo añadido como elemento editable")
        Toast.makeText(requireContext(), "Círculo añadido - Seleccionado", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun añadirCuadrado() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO CUADRADO ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.CUADRADO,
            x = overlayEditables.width / 2f,
            y = overlayEditables.height / 2f,
            width = 150f,
            height = 150f,
            color = Color.GREEN,
            seleccionado = true
        )
        
        // Deseleccionar otros elementos
        elementosEditables.forEach { it.seleccionado = false }
        elemento.seleccionado = true
        elementoSeleccionado = elemento
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Cuadrado añadido como elemento editable")
        Toast.makeText(requireContext(), "Cuadrado añadido - Seleccionado", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun añadirLogoBitherm() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO LOGO BITHERM ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.LOGO,
            x = (bitmapActual?.width ?: 0) - 400f, // Con padding derecho
            y = 400f, // Con padding superior
            width = 800f, // 800% del tamaño actual (400% * 2)
            height = 800f,
            color = Color.parseColor("#FF5722")
        )
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Logo Bitherm añadido como elemento editable")
        Toast.makeText(requireContext(), "Logo añadido - Toca para seleccionar", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun añadirStamp() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO STAMP ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.STAMP,
            x = 250f, // Con padding izquierdo
            y = 150f, // Con padding superior
            width = 600f, // 600% del tamaño actual (200% * 3)
            height = 300f,
            color = Color.BLACK
        )
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Stamp añadido como elemento editable")
        Toast.makeText(requireContext(), "Stamp añadido - Toca para seleccionar", Toast.LENGTH_SHORT).show()
    }

    
    private fun activarModoTexto() {
        Log.d("EditorWhatsApp", "=== ACTIVANDO MODO TEXTO ===")
        
        val editText = EditText(requireContext()).apply {
            hint = "Escribe el texto..."
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Añadir texto")
            .setView(editText)
            .setPositiveButton("Añadir") { _, _ ->
                val texto = editText.text.toString()
                if (texto.isNotEmpty()) {
                    añadirTexto(texto)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun añadirTexto(texto: String) {
        Log.d("EditorWhatsApp", "=== AÑADIENDO TEXTO: $texto ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.TEXTO,
            x = overlayEditables.width / 2f,
            y = overlayEditables.height / 2f,
            width = 300f,
            height = 100f,
            texto = texto,
            color = Color.WHITE,
            seleccionado = true
        )
        
        // Deseleccionar otros elementos
        elementosEditables.forEach { it.seleccionado = false }
        elemento.seleccionado = true
        elementoSeleccionado = elemento
        
        elementosEditables.add(elemento)
        Log.d("EditorWhatsApp", "Elemento añadido. Total elementos: ${elementosEditables.size}")
        Log.d("EditorWhatsApp", "Posición del texto: (${elemento.x}, ${elemento.y})")
        Log.d("EditorWhatsApp", "Color del texto: ${elemento.color}")
        
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Texto añadido como elemento editable")
        Toast.makeText(requireContext(), "Texto añadido - Seleccionado", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun activarModoDibujo() {
        Log.d("EditorWhatsApp", "=== ACTIVANDO MODO DIBUJO ===")
        
        modoDibujo = true
        modoPolilinea = false
        
        // Deshabilitar zoom del PhotoView para evitar conflictos con el dibujo
        photoView.isZoomable = false
        
        // Mostrar barra contextual
        mostrarBarraContextual()
        
        Toast.makeText(requireContext(), "Modo dibujo activado - Dibuja sobre la imagen", Toast.LENGTH_SHORT).show()
        Log.d("EditorWhatsApp", "PhotoView zoom deshabilitado para modo dibujo")
    }
    
    private fun activarModoPolilinea() {
        Log.d("EditorWhatsApp", "=== ACTIVANDO MODO POLILÍNEA ===")
        
        modoPolilinea = true
        modoDibujo = false
        puntosPolilinea.clear()
        pathPolilinea = null
        
        // Deshabilitar zoom del PhotoView
        photoView.isZoomable = false
        
        // Mostrar barra contextual
        mostrarBarraContextual()
        
        Toast.makeText(requireContext(), "Modo polilínea activado - Toca para añadir puntos, pulsa FIN para terminar", Toast.LENGTH_LONG).show()
        Log.d("EditorWhatsApp", "Modo polilínea activado")
    }
    
    private fun mostrarBarraContextual() {
        barraHerramientasPrincipal.visibility = View.GONE
        barraContextual.visibility = View.VISIBLE
    }
    
    private fun ocultarBarraContextual() {
        barraContextual.visibility = View.GONE
        barraHerramientasPrincipal.visibility = View.VISIBLE
    }
    
    private fun mostrarSelectorPoligonos() {
        val opciones = arrayOf("Círculo", "Cuadrado", "Triángulo")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar forma")
            .setItems(opciones) { _, which ->
                mostrarBarraContextual()
                when (which) {
                    0 -> añadirCirculo()
                    1 -> añadirCuadrado()
                    2 -> añadirTriangulo()
                }
            }
            .show()
    }
    
    private fun añadirTriangulo() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO TRIÁNGULO ===")
        
        val tamañoBase = 300f
        val centroX = overlayEditables.width / 2f
        val centroY = overlayEditables.height / 2f
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.TRIANGULO,
            x = centroX - tamañoBase / 2,
            y = centroY - tamañoBase / 2,
            width = tamañoBase,
            height = tamañoBase,
            color = colorActual,
            strokeWidth = grosorActual,
            seleccionado = true
        )
        
        // Deseleccionar otros elementos
        elementosEditables.forEach { it.seleccionado = false }
        elemento.seleccionado = true
        elementoSeleccionado = elemento
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Toast.makeText(requireContext(), "Triángulo añadido - Seleccionado", Toast.LENGTH_SHORT).show()
    }
    
    private fun mostrarSelectorColor() {
        val colores = arrayOf(
            "Rojo" to Color.RED,
            "Azul" to Color.BLUE,
            "Verde" to Color.GREEN,
            "Amarillo" to Color.YELLOW,
            "Negro" to Color.BLACK,
            "Blanco" to Color.WHITE,
            "Naranja" to Color.parseColor("#FF9800"),
            "Morado" to Color.parseColor("#9C27B0")
        )
        
        val nombresColores = colores.map { it.first }.toTypedArray()
        
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar color")
            .setItems(nombresColores) { _, which ->
                colorActual = colores[which].second
                actualizarPaintDibujo()
                
                // Actualizar preview si estamos en modo dibujo
                if (modoDibujo || modoPolilinea) {
                    overlayEditables.setPaintPreview(paintDibujo!!)
                    overlayEditables.invalidate()
                }
                
                // Actualizar elemento seleccionado
                elementoSeleccionado?.let { elemento ->
                    elemento.color = colorActual
                    redibujarElementos()
                }
                
                Toast.makeText(requireContext(), "Color: ${colores[which].first}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    
    private fun finalizarHerramientaActual() {
        Log.d("EditorWhatsApp", "=== FINALIZANDO HERRAMIENTA ACTUAL ===")
        
        when {
            modoDibujo -> {
                // Crear ElementoEditable con el path dibujado - exactamente igual que polilínea
                if (pathDibujo != null && !pathDibujo!!.isEmpty) {
                    val pathCopia = Path(pathDibujo) // Crear copia del path
                    Log.d("EditorWhatsApp", "=== GUARDANDO DIBUJO ===")
                    Log.d("EditorWhatsApp", "Path isEmpty: ${pathCopia.isEmpty}")
                    val bounds = RectF()
                    pathCopia.computeBounds(bounds, true)
                    Log.d("EditorWhatsApp", "Path bounds: $bounds")
                    
                    val elemento = ElementoEditable(
                        tipo = TipoElemento.DIBUJO,
                        x = 0f,
                        y = 0f,
                        path = pathCopia,
                        color = colorActual,
                        strokeWidth = grosorActual,
                        seleccionado = false
                    )
                    
                    elementosEditables.add(elemento)
                    Log.d("EditorWhatsApp", "Elemento DIBUJO añadido. Total elementos: ${elementosEditables.size}")
                    redibujarElementos()
                    
                    Toast.makeText(requireContext(), "Dibujo guardado", Toast.LENGTH_SHORT).show()
                } else {
                    Log.w("EditorWhatsApp", "⚠ pathDibujo es null o vacío, no se puede guardar")
                }
                
                modoDibujo = false
                pathDibujo = null
                overlayEditables.setPathPreview(null)
                overlayEditables.invalidate()
            }
            modoPolilinea -> {
                // Crear ElementoEditable con la polilínea si tiene al menos 2 puntos
                if (puntosPolilinea.size >= 2 && pathPolilinea != null) {
                    val elemento = ElementoEditable(
                        tipo = TipoElemento.DIBUJO,
                        x = 0f,
                        y = 0f,
                        path = pathPolilinea,
                        color = colorActual,
                        strokeWidth = grosorActual,
                        seleccionado = false
                    )
                    
                    elementosEditables.add(elemento)
                    redibujarElementos()
                    
                    Toast.makeText(requireContext(), "Polilínea guardada con ${puntosPolilinea.size} puntos", Toast.LENGTH_SHORT).show()
                }
                
                modoPolilinea = false
                puntosPolilinea.clear()
                pathPolilinea = null
                overlayEditables.setPathPreview(null)
                overlayEditables.invalidate()
            }
        }
        
        // Re-habilitar zoom y ocultar barra contextual
        photoView.isZoomable = true
        ocultarBarraContextual()
    }
    
    private fun añadirFlecha() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO FLECHA ===")
        
        mostrarBarraContextual()
        
        val tamañoBase = 50f // Reducido al 25% (era 200f)
        val centroX = overlayEditables.width / 2f
        val centroY = overlayEditables.height / 2f
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.FLECHA,
            x = centroX - tamañoBase,
            y = centroY - tamañoBase / 4,
            width = tamañoBase * 2,
            height = tamañoBase / 2,
            color = colorActual,
            strokeWidth = grosorActual,
            seleccionado = true // Marcar como seleccionado por defecto
        )
        
        // Deseleccionar otros elementos
        elementosEditables.forEach { it.seleccionado = false }
        elemento.seleccionado = true
        elementoSeleccionado = elemento
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Toast.makeText(requireContext(), "Flecha añadida - Seleccionada", Toast.LENGTH_SHORT).show()
    }
    
    private fun guardarFoto() {
        Log.d("EditorWhatsApp", "💾 === INICIANDO GUARDADO DE FOTO ===")
        Log.d("EditorWhatsApp", "💾 Ruta foto original: $rutaFotoOriginal")
        Log.d("EditorWhatsApp", "💾 bitmapActual: $bitmapActual")
        Log.d("EditorWhatsApp", "💾 Elementos editables: ${elementosEditables.size}")
        elementosEditables.forEachIndexed { index, elemento ->
            Log.d("EditorWhatsApp", "  Elemento $index: ${elemento.texto} - ${elemento.tipo}")
        }
        
        bitmapActual?.let { bitmap ->
            Log.d("EditorWhatsApp", "Bitmap original: ${bitmap.width}x${bitmap.height}")
            
            try {
                // Crear bitmap final usando LayerBase (crop cuadrado + borde) + elementos editables
                Log.d("EditorWhatsApp", "Creando bitmap final con LayerBase (1920x1920px)")
                val bitmapFinal = crearBitmapFinalConLayerBase(bitmap)
                
                Log.d("EditorWhatsApp", "Bitmap final: ${bitmapFinal.width}x${bitmapFinal.height}")
                // Obtener el tipo seleccionado del spinner
                val tipoSeleccionado = spinnerTipoFoto.selectedItem.toString()
                val tipoFotoEnum = when (tipoSeleccionado) {
                    "EQUIPO" -> TipoFoto.FOTO_EQUIPO
                    "UBICACIÓN" -> TipoFoto.FOTO_UBICACION
                    "MANIFOLD" -> TipoFoto.FOTO_MANIFOLD
                    "GPS" -> TipoFoto.CAPTURA_GPS
                    "NOTA" -> TipoFoto.FOTO_NOTA
                    "DENUNCIA" -> TipoFoto.FOTO_DENUNCIA
                    // Detectar dinámicamente si es un proyecto (cualquier nombre de proyecto)
                    else -> {
                        val proyectosDisponibles = obtenerProyectosDisponibles()
                        if (proyectosDisponibles.contains(tipoSeleccionado)) {
                            TipoFoto.FOTO_PROYECTO
                        } else {
                            TipoFoto.EXTRAS
                        }
                    }
                }
                
                // Crear nombre de archivo con formato: TAG_TIPOFOTO_FECHA
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val tipoFotoString = when (tipoFotoEnum) {
                    TipoFoto.FOTO_EQUIPO -> "EQUIPO"
                    TipoFoto.FOTO_UBICACION -> "UBICACION" 
                    TipoFoto.FOTO_MANIFOLD -> "MANIFOLD"
                    TipoFoto.CAPTURA_GPS -> "GPS"
                    TipoFoto.FOTO_NOTA -> "NOTA"
                    TipoFoto.FOTO_DENUNCIA -> "DENUNCIA"
                    TipoFoto.FOTO_PROYECTO -> tipoSeleccionado // Usar el nombre del proyecto seleccionado
                    else -> "EXTRA"
                }
                
                // Determinar directorio basado en tipo de foto
                val directorio = when (tipoFotoEnum) {
                    TipoFoto.FOTO_PROYECTO -> {
                        // Para proyectos, usar la carpeta específica del proyecto
                        val nombreProyectoFinal = nombreProyecto?.takeIf { it.isNotBlank() } ?: tipoSeleccionado
                        
                        // ✅ VALIDACIÓN ESTRICTA: nombreProyecto NO puede ser genérico o vacío
                        if (nombreProyectoFinal.isBlank() || nombreProyectoFinal == "PROYECTO") {
                            Log.e("EditorWhatsApp", "❌ ERROR: Nombre de proyecto inválido")
                            Log.e("EditorWhatsApp", "  nombreProyecto: '$nombreProyecto'")
                            Log.e("EditorWhatsApp", "  tipoSeleccionado: '$tipoSeleccionado'")
                            Log.e("EditorWhatsApp", "  nombreProyectoFinal: '$nombreProyectoFinal'")
                            
                            Toast.makeText(
                                requireContext(),
                                "ERROR: Selecciona un proyecto específico en el menú",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            guardadoEnProgreso = false
                            btnAceptar.isEnabled = true
                            return
                        }
                        
                        val dirProyecto = File(requireContext().filesDir, "proyectos/$nombreProyectoFinal")
                        Log.d("EditorWhatsApp", "✓ Directorio proyecto validado: ${dirProyecto.absolutePath}")
                        dirProyecto
                    }
                    TipoFoto.CAPTURA_GPS -> {
                        // Para GPS, usar la carpeta gps_fotos
                        File(requireContext().filesDir, "gps_fotos")
                    }
                    TipoFoto.FOTO_DENUNCIA -> {
                        // Para DENUNCIA, usar la carpeta denuncias
                        File(requireContext().filesDir, "denuncias")
                    }
                    TipoFoto.FOTO_NOTA -> {
                        // Para NOTA, usar la carpeta foto_notas
                        File(requireContext().filesDir, "foto_notas")
                    }
                    else -> {
                        // Para otros tipos, usar la carpeta fotos general
                        File(requireContext().filesDir, "fotos")
                    }
                }
                
                if (!directorio.exists()) {
                    directorio.mkdirs()
                    Log.d("EditorWhatsApp", "Directorio creado: ${directorio.absolutePath}")
                }
                
                // Extraer comentarios de los elementos de texto
                val comentarios = extraerComentariosTexto()
                val sufijoComentario = if (comentarios.isNotEmpty()) "_${comentarios}" else ""
                
                // Agregar GPS al nombre si hay coordenadas
                val sufijoGPS = if (coordenadas != null && precision != null) {
                    // Limpiar coordenadas para el nombre del archivo (reemplazar espacios y comas problemáticas)
                    val coordenadasLimpias = coordenadas!!.replace(" ", "").replace(",", "-")
                    val precisionLimpia = precision!!.replace(" ", "")
                    "_GPS:${coordenadasLimpias}_${precisionLimpia}"
                } else if (coordenadas != null) {
                    val coordenadasLimpias = coordenadas!!.replace(" ", "").replace(",", "-")
                    "_GPS:${coordenadasLimpias}"
                } else {
                    ""
                }
                
                // Agregar TIPO de denuncia al nombre si existe (después del GPS)
                val tipoDenunciaActual = tipoDenuncia
                val sufijoTipo = if (tipoDenunciaActual != null && tipoDenunciaActual.isNotBlank()) {
                    val tipoLimpio = tipoDenunciaActual.replace(" ", "_").uppercase()
                    "_TIPO:${tipoLimpio}"
                } else {
                    ""
                }
                
                val sufijo = sufijoComentario + sufijoGPS + sufijoTipo
                
                // Generar nombre único si ya existe
                var contador = 1
                var nombreArchivo: String
                var archivoFinal: File
                
                do {
                    nombreArchivo = if (contador == 1) {
                        "${tipoFotoString}_${timeStamp}${sufijo}.jpg"
                    } else {
                        "${tipoFotoString}_${timeStamp}${sufijo}_${contador}.jpg"
                    }
                    
                    archivoFinal = File(directorio, nombreArchivo)
                    contador++
                } while (archivoFinal.exists())
                
                Log.d("EditorWhatsApp", "💾 GUARDANDO BITMAP FINAL:")
                Log.d("EditorWhatsApp", "💾   - Ruta: ${archivoFinal.absolutePath}")
                Log.d("EditorWhatsApp", "💾   - Directorio: ${directorio.absolutePath}")
                Log.d("EditorWhatsApp", "💾   - Nombre archivo: $nombreArchivo")
                Log.d("EditorWhatsApp", "💾   - Archivo existe antes de guardar: ${archivoFinal.exists()}")
                
                // Guardar bitmap final editado como JPEG
                val outputStream = FileOutputStream(archivoFinal)
                bitmapFinal.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                
                Log.d("EditorWhatsApp", "💾 ARCHIVO GUARDADO EXITOSAMENTE:")
                Log.d("EditorWhatsApp", "💾   - Archivo existe después de guardar: ${archivoFinal.exists()}")
                Log.d("EditorWhatsApp", "💾   - Tamaño final: ${archivoFinal.length()} bytes")
                Log.d("EditorWhatsApp", "💾 ✅ GUARDADO ÚNICO COMPLETADO - NO SE GUARDARÁ NUEVAMENTE")
                
                // Mostrar mensaje de éxito
                Toast.makeText(requireContext(), "Foto guardada: $nombreArchivo", Toast.LENGTH_LONG).show()
                
                Log.d("EditorWhatsApp", "💾 FOTO SALE DEL EDITOR - PROCESO COMPLETADO")
                Log.d("EditorWhatsApp", "💾 Archivo final guardado: ${archivoFinal.absolutePath}")
                
                // Verificar duplicados en directorio raíz
                val directorioProyectos = File(requireContext().filesDir, "proyectos")
                Log.d("EditorWhatsApp", "🔍 VERIFICANDO DUPLICADOS EN RAÍZ:")
                directorioProyectos.listFiles()?.filter { it.isFile }?.forEach { archivo ->
                    Log.w("EditorWhatsApp", "⚠️ ARCHIVO EN RAÍZ (NO DEBERÍA ESTAR AQUÍ): ${archivo.name} (${archivo.length()} bytes)")
                }
                
                // Notificar al listener que la edición se completó
                onEdicionCompletadaListener?.invoke(archivoFinal.absolutePath)
                
                // Cerrar el editor
                dismiss()
                
                Log.d("EditorWhatsApp", "=== GUARDADO FINALIZADO EXITOSAMENTE ===")
                
            } catch (e: Exception) {
                Log.e("EditorWhatsApp", "ERROR al guardar la foto final: ${e.message}")
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al guardar la foto", Toast.LENGTH_SHORT).show()
                guardadoEnProgreso = false
                btnAceptar.isEnabled = true
            }
        } ?: run {
            Log.e("EditorWhatsApp", "ERROR: bitmapActual es null")
            Toast.makeText(requireContext(), "Error: No hay imagen para guardar", Toast.LENGTH_SHORT).show()
            guardadoEnProgreso = false
            btnAceptar.isEnabled = true
        }
    }
    
    private fun obtenerProyectosDisponibles(): Array<String> {
        return try {
            Log.d("EditorWhatsApp", "=== OBTENIENDO PROYECTOS DISPONIBLES ===")
            val activity = requireActivity()
            // Usar el mismo directorio que PhotoScanner: filesDir/proyectos
            val proyectoDir = File(activity.filesDir, "proyectos")
            
            Log.d("EditorWhatsApp", "Proyecto dir: ${proyectoDir.absolutePath}")
            Log.d("EditorWhatsApp", "Proyecto dir existe: ${proyectoDir.exists()}")
            
            if (proyectoDir.exists() && proyectoDir.isDirectory) {
                val subdirs = proyectoDir.listFiles { file -> 
                    file.isDirectory && !file.name.startsWith(".")
                }?.map { it.name }?.sorted()?.toTypedArray() ?: arrayOf()
                
                Log.d("EditorWhatsApp", "Proyectos encontrados: ${subdirs.contentToString()}")
                subdirs
            } else {
                Log.w("EditorWhatsApp", "Directorio de proyectos no existe")
                arrayOf()
            }
        } catch (e: Exception) {
            Log.e("EditorWhatsApp", "Error obteniendo proyectos disponibles", e)
            arrayOf()
        }
    }
    
    /**
     * Crea el bitmap final usando LayerBase (1920x1920px con borde y crop cuadrado)
     * y aplicando todos los elementos editables encima
     */
    private fun crearBitmapFinalConLayerBase(fotoOriginal: Bitmap): Bitmap {
        Log.d("EditorWhatsApp", "=== CREANDO BITMAP FINAL CON LAYERBASE ===")
        Log.d("EditorWhatsApp", "Aplicando ${elementosEditables.size} elementos editables al layer base")
        
        // Validar que layerBase esté inicializado
        if (layerBase == null) {
            Log.e("EditorWhatsApp", "ERROR: layerBase es null, inicializando...")
            layerBase = LayerBase()
        }
        
        // Validar que overlayEditables esté inicializado
        if (overlayEditables == null) {
            Log.e("EditorWhatsApp", "ERROR: overlayEditables es null")
            throw IllegalStateException("overlayEditables no está inicializado")
        }
        
        // Obtener factor de escala y offset del overlay
        val escalaDisplay = overlayEditables.layerBaseEscala
        val offsetX = overlayEditables.layerBaseOffsetX
        val offsetY = overlayEditables.layerBaseOffsetY
        
        Log.d("EditorWhatsApp", "Factor escala display: $escalaDisplay")
        Log.d("EditorWhatsApp", "Offset: ($offsetX, $offsetY)")
        
        // NO rasterizar la foto de nuevo - ya está rasterizada desde confirmarFoto()
        // El layerBase ya contiene el bitmap recortado y escalado a 1920x1920
        
        // Rasterizar TODOS los elementos editables al layer base antes de guardar
        elementosEditables.forEach { elemento ->
            try {
            Log.d("EditorWhatsApp", "  Rasterizando: ${elemento.tipo} en pantalla (${elemento.x}, ${elemento.y})")
            
            // STAMP y LOGO ya tienen coordenadas del LayerBase, el resto están en espacio de pantalla
            val elementoEscalado = if (elemento.tipo == TipoElemento.STAMP || elemento.tipo == TipoElemento.LOGO) {
                elemento  // Ya tienen coordenadas correctas del LayerBase
            } else {
                // Convertir de coordenadas de pantalla a coordenadas del LayerBase
                // 1. Restar offset (para obtener coords relativas al LayerBase en pantalla)
                // 2. Dividir por escala display (para obtener coords del LayerBase real 1920x1920)
                val xLayerBase = (elemento.x - offsetX) / escalaDisplay
                val yLayerBase = (elemento.y - offsetY) / escalaDisplay
                val widthLayerBase = elemento.width / escalaDisplay
                val heightLayerBase = elemento.height / escalaDisplay
                
                Log.d("EditorWhatsApp", "    → LayerBase: ($xLayerBase, $yLayerBase)")
                
                // Transformar el path si existe (para DIBUJO y LINEA)
                val pathTransformado = elemento.path?.let { pathOriginal ->
                    Log.d("EditorWhatsApp", "  Transformando path para ${elemento.tipo}")
                    Log.d("EditorWhatsApp", "    Path original isEmpty: ${pathOriginal.isEmpty}")
                    val boundsOriginal = RectF()
                    pathOriginal.computeBounds(boundsOriginal, true)
                    Log.d("EditorWhatsApp", "    Path original bounds: $boundsOriginal")
                    Log.d("EditorWhatsApp", "    Offset: ($offsetX, $offsetY), Escala: $escalaDisplay")
                    Log.d("EditorWhatsApp", "    Elemento.x=${elemento.x}, Elemento.y=${elemento.y}")
                    
                    if (pathOriginal.isEmpty) {
                        Log.w("EditorWhatsApp", "⚠ Path original está vacío para ${elemento.tipo}")
                        null
                    } else {
                        val pathNuevo = Path(pathOriginal)
                        val matrix = Matrix()
                        // Restar offset y luego dividir por escala
                        // El path resultante estará en coordenadas del LayerBase completo (0-1920)
                        // El canvas en LayerBase tiene translate(BORDER_SIZE, BORDER_SIZE),
                        // así que el path debe estar en coordenadas del LayerBase completo (0-1920)
                        // NO restamos BORDER_SIZE porque el canvas ya tiene el translate aplicado
                        matrix.postTranslate(-offsetX, -offsetY)
                        matrix.postScale(1f / escalaDisplay, 1f / escalaDisplay)
                        pathNuevo.transform(matrix)
                        val boundsNuevo = RectF()
                        pathNuevo.computeBounds(boundsNuevo, true)
                        Log.d("EditorWhatsApp", "    ✓ Path transformado, isEmpty=${pathNuevo.isEmpty}, bounds=$boundsNuevo")
                        // Verificar que el path no esté vacío después de la transformación
                        if (pathNuevo.isEmpty) {
                            Log.e("EditorWhatsApp", "❌ ERROR: Path quedó vacío después de transformación!")
                            null
                        } else {
                            // Verificar que el path esté dentro del área visible del LayerBase
                            val layerSize = LayerBase.LAYER_SIZE.toFloat()
                            if (boundsNuevo.left < -1000f || boundsNuevo.top < -1000f || 
                                boundsNuevo.right > layerSize + 1000f || boundsNuevo.bottom > layerSize + 1000f) {
                                Log.w("EditorWhatsApp", "⚠ Path transformado está fuera del área visible del LayerBase")
                                Log.w("EditorWhatsApp", "    Bounds: $boundsNuevo, LayerSize: $layerSize")
                            }
                            pathNuevo
                        }
                    }
                } ?: run {
                    Log.w("EditorWhatsApp", "  ⚠ Path es null para ${elemento.tipo}")
                    null
                }
                
                elemento.copy(
                    x = xLayerBase,
                    y = yLayerBase,
                    width = widthLayerBase,
                    height = heightLayerBase,
                    strokeWidth = elemento.strokeWidth / escalaDisplay,
                    path = pathTransformado
                )
            }
            
                layerBase.rasterizarElemento(elementoEscalado, escalaDisplay)
            } catch (e: Exception) {
                Log.e("EditorWhatsApp", "ERROR al rasterizar elemento ${elemento.tipo}: ${e.message}", e)
                // Continuar con el siguiente elemento en lugar de fallar completamente
            }
        }
        
        val bitmapFinal = layerBase.getBitmap()
        
        Log.d("EditorWhatsApp", "✓ Bitmap final del layer base: ${bitmapFinal.width}x${bitmapFinal.height}")
        
        return bitmapFinal
    }
    
    // Función para aplicar todos los elementos editables (textos, dibujos, stamps, logos) al bitmap final
    private fun aplicarElementosEditablesAlBitmap(bitmapOriginal: Bitmap): Bitmap {
        Log.d("EditorWhatsApp", "=== APLICANDO ELEMENTOS EDITABLES AL BITMAP ===")
        
        // Crear un bitmap mutable copia del original
        val bitmapFinal = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapFinal)
        
        Log.d("EditorWhatsApp", "Bitmap final creado: ${bitmapFinal.width}x${bitmapFinal.height}")
        
        // CRÍTICO: Elementos dibujados en coordenadas de OVERLAY (fijas), NO en displayRect (cambia con zoom)
        // Calcular escala directamente: bitmap final / overlay (donde se dibujaron los elementos)
        val overlayAncho = overlayEditables.width.toFloat()
        val overlayAlto = overlayEditables.height.toFloat()
        
        // Factor de escala: cuánto hay que escalar las coordenadas del overlay para que coincidan con el bitmap
        val escalaX = bitmapFinal.width.toFloat() / overlayAncho
        val escalaY = bitmapFinal.height.toFloat() / overlayAlto
        
        Log.d("EditorWhatsApp", "📐 Overlay dimensiones: ${overlayAncho}x${overlayAlto}")
        Log.d("EditorWhatsApp", "📐 Bitmap dimensiones: ${bitmapFinal.width}x${bitmapFinal.height}")
        Log.d("EditorWhatsApp", "📐 Factor de escala (overlay→bitmap): X=$escalaX, Y=$escalaY")
        
        // NO hay offset - elementos en overlay ya están en coordenadas correctas (0,0 = esquina overlay)
        val offsetX = 0f
        val offsetY = 0f
        
        // Aplicar todos los elementos editables
        elementosEditables.forEach { elemento ->
            Log.d("EditorWhatsApp", "Aplicando elemento: ${elemento.tipo}")
            
            when (elemento.tipo) {
                TipoElemento.TEXTO -> {
                    val paintTexto = Paint().apply {
                        color = elemento.color
                        textSize = 50f * escalaY // Escalar tamaño de texto
                        typeface = Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                        style = Paint.Style.FILL
                    }
                    // Ajustar coordenadas: restar offset y luego escalar
                    val xEscalado = (elemento.x - offsetX) * escalaX
                    val yEscalado = (elemento.y - offsetY) * escalaY
                    canvas.drawText(elemento.texto, xEscalado, yEscalado, paintTexto)
                    Log.d("EditorWhatsApp", "Texto aplicado: '${elemento.texto}' en (${xEscalado}, ${yEscalado}) [original: (${elemento.x}, ${elemento.y})]")
                }
                TipoElemento.DIBUJO -> {
                    elemento.path?.let { trazo ->
                        // Ajustar path: restar offset y luego escalar
                        val pathEscalado = Path(trazo)
                        val matrixTransform = Matrix()
                        matrixTransform.postTranslate(-offsetX, -offsetY) // Restar offset primero
                        matrixTransform.postScale(escalaX, escalaY) // Luego escalar
                        pathEscalado.transform(matrixTransform)
                        
                        val paintDibujo = Paint().apply {
                            color = elemento.color
                            strokeWidth = 8f * ((escalaX + escalaY) / 2f) // Escalar grosor
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }
                        canvas.drawPath(pathEscalado, paintDibujo)
                        Log.d("EditorWhatsApp", "Path de dibujo aplicado con escala X=$escalaX, Y=$escalaY")
                    }
                }
                TipoElemento.LINEA -> {
                    elemento.path?.let { trazo ->
                        val pathEscalado = Path(trazo)
                        val matrixTransform = Matrix()
                        matrixTransform.postTranslate(-offsetX, -offsetY)
                        matrixTransform.postScale(escalaX, escalaY)
                        pathEscalado.transform(matrixTransform)
                        
                        val paintLinea = Paint().apply {
                            color = elemento.color
                            strokeWidth = elemento.strokeWidth * ((escalaX + escalaY) / 2f)
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            isAntiAlias = true
                        }
                        canvas.drawPath(pathEscalado, paintLinea)
                        Log.d("EditorWhatsApp", "Línea aplicada con escala")
                    }
                }
                TipoElemento.STAMP -> {
                    // Dibujar stamp usando la misma lógica que ElementosEditablesOverlay
                    // pero con coordenadas escaladas
                    dibujarStampEnCanvas(canvas, elemento, escalaX, escalaY)
                    Log.d("EditorWhatsApp", "STAMP aplicado con escala")
                }
                TipoElemento.LOGO -> {
                    // Dibujar logo usando la misma lógica que ElementosEditablesOverlay
                    // pero con coordenadas escaladas
                    dibujarLogoEnCanvas(canvas, elemento, escalaX, escalaY)
                    Log.d("EditorWhatsApp", "LOGO aplicado con escala")
                }
                TipoElemento.FLECHA -> {
                    canvas.save()
                    
                    // Ajustar coordenadas: restar offset de imagen y luego escalar
                    val xEscalado = (elemento.x - offsetX) * escalaX
                    val yEscalado = (elemento.y - offsetY) * escalaY
                    
                    // Aplicar transformaciones - NO usar elemento.escala para evitar descuadre
                    canvas.translate(xEscalado, yEscalado)
                    canvas.rotate(elemento.rotacion)
                    // canvas.scale(elemento.escala, elemento.escala) // REMOVIDO: causaba descuadre
                    canvas.translate(-xEscalado, -yEscalado)
                    
                    // Dibujar flecha
                    val paint = Paint().apply {
                        color = elemento.color
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    
                    val path = Path()
                    val escalaPromedio = (escalaX + escalaY) / 2f
                    val ancho = elemento.width * 4f * escalaPromedio
                    val alto = elemento.height * 4f * escalaPromedio
                    val centerX = xEscalado
                    val centerY = yEscalado
                    
                    // Triángulo
                    val puntaLeft = centerX + ancho/2 - ancho * 0.25f
                    val puntaRight = centerX + ancho/2
                    val puntaTop = centerY - alto/2
                    val puntaBottom = centerY + alto/2
                    
                    path.moveTo(puntaRight, centerY)
                    path.lineTo(puntaLeft, puntaTop)
                    path.lineTo(puntaLeft, puntaBottom)
                    path.close()
                    
                    // Línea
                    val cuerpoAlto = alto * 0.15f
                    val cuerpoLeft = centerX - ancho/2
                    val cuerpoRight = puntaLeft
                    val cuerpoTop = centerY - cuerpoAlto/2
                    val cuerpoBottom = centerY + cuerpoAlto/2
                    
                    path.addRect(cuerpoLeft, cuerpoTop, cuerpoRight, cuerpoBottom, Path.Direction.CW)
                    canvas.drawPath(path, paint)
                    canvas.restore()
                    Log.d("EditorWhatsApp", "FLECHA aplicada con escala")
                }
                TipoElemento.CIRCULO -> {
                    canvas.save()
                    
                    // Ajustar coordenadas: restar offset de imagen y luego escalar
                    val xEscalado = (elemento.x - offsetX) * escalaX
                    val yEscalado = (elemento.y - offsetY) * escalaY
                    
                    canvas.translate(xEscalado, yEscalado)
                    canvas.rotate(elemento.rotacion)
                    // canvas.scale(elemento.escala, elemento.escala) // REMOVIDO: causaba descuadre
                    canvas.translate(-xEscalado, -yEscalado)
                    
                    val paint = Paint().apply {
                        color = elemento.color
                        style = Paint.Style.STROKE
                        strokeWidth = 8f * ((escalaX + escalaY) / 2f) // Escalar grosor
                        isAntiAlias = true
                    }
                    val escalaPromedio = (escalaX + escalaY) / 2f
                    val radioEscalado = (elemento.width / 2) * escalaPromedio
                    canvas.drawCircle(xEscalado, yEscalado, radioEscalado, paint)
                    canvas.restore()
                    Log.d("EditorWhatsApp", "CIRCULO aplicado con escala")
                }
                TipoElemento.CUADRADO -> {
                    canvas.save()
                    
                    // Ajustar coordenadas: restar offset de imagen y luego escalar
                    val xEscalado = (elemento.x - offsetX) * escalaX
                    val yEscalado = (elemento.y - offsetY) * escalaY
                    
                    canvas.translate(xEscalado, yEscalado)
                    canvas.rotate(elemento.rotacion)
                    // canvas.scale(elemento.escala, elemento.escala) // REMOVIDO: causaba descuadre
                    canvas.translate(-xEscalado, -yEscalado)
                    
                    val paint = Paint().apply {
                        color = elemento.color
                        style = Paint.Style.STROKE
                        strokeWidth = 8f * ((escalaX + escalaY) / 2f) // Escalar grosor
                        isAntiAlias = true
                    }
                    val escalaPromedio = (escalaX + escalaY) / 2f
                    val widthEscalado = elemento.width * escalaPromedio
                    val heightEscalado = elemento.height * escalaPromedio
                    val rect = RectF(
                        xEscalado - widthEscalado/2,
                        yEscalado - heightEscalado/2,
                        xEscalado + widthEscalado/2,
                        yEscalado + heightEscalado/2
                    )
                    canvas.drawRect(rect, paint)
                    canvas.restore()
                    Log.d("EditorWhatsApp", "CUADRADO aplicado con escala")
                }
                TipoElemento.TRIANGULO -> {
                    canvas.save()
                    
                    // Ajustar coordenadas: restar offset de imagen y luego escalar
                    val xEscalado = (elemento.x - offsetX) * escalaX
                    val yEscalado = (elemento.y - offsetY) * escalaY
                    
                    canvas.translate(xEscalado, yEscalado)
                    canvas.rotate(elemento.rotacion)
                    // canvas.scale(elemento.escala, elemento.escala) // REMOVIDO: causaba descuadre
                    canvas.translate(-xEscalado, -yEscalado)
                    
                    val paint = Paint().apply {
                        color = elemento.color
                        style = Paint.Style.STROKE
                        strokeWidth = elemento.strokeWidth * ((escalaX + escalaY) / 2f) // Escalar grosor
                        isAntiAlias = true
                    }
                    
                    val path = Path()
                    val centerX = xEscalado
                    val centerY = yEscalado
                    val escalaPromedio = (escalaX + escalaY) / 2f
                    val width = elemento.width * escalaPromedio
                    val height = elemento.height * escalaPromedio
                    
                    // Triángulo equilátero con punta hacia arriba
                    path.moveTo(centerX, centerY - height/2)
                    path.lineTo(centerX - width/2, centerY + height/2)
                    path.lineTo(centerX + width/2, centerY + height/2)
                    path.close()
                    
                    canvas.drawPath(path, paint)
                    canvas.restore()
                    Log.d("EditorWhatsApp", "TRIANGULO aplicado con escala")
                }
            }
        }
        
        Log.d("EditorWhatsApp", "Todos los elementos aplicados exitosamente")
        return bitmapFinal
    }
    
    private fun dibujarStampEnCanvas(canvas: Canvas, elemento: ElementoEditable, escalaX: Float, escalaY: Float) {
        val paintFondo = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Escalar todas las dimensiones y posiciones
        val stampWidth = elemento.width * escalaX
        val stampHeight = elemento.height * escalaY
        val stampX = elemento.x * escalaX
        val stampY = elemento.y * escalaY
        
        canvas.drawRect(stampX, stampY, stampX + stampWidth, stampY + stampHeight, paintFondo)
        
        val margenSuperior = stampHeight * 0.05f
        val margenInferior = stampHeight * 0.05f
        val alturaDisponible = stampHeight - margenSuperior - margenInferior
        
        val alturaLinea1 = alturaDisponible * 0.25f
        val alturaLinea2 = alturaDisponible * 0.18f
        val alturaLinea3 = alturaDisponible * 0.18f
        val alturaLinea4 = alturaDisponible * 0.18f
        
        val separacionTotal = alturaDisponible - (alturaLinea1 + alturaLinea2 + alturaLinea3 + alturaLinea4)
        val separacion = separacionTotal / 3f
        
        val paintLinea1 = Paint().apply {
            color = Color.WHITE
            textSize = alturaLinea1 * 0.8f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        val paintLineas234 = Paint().apply {
            color = Color.WHITE
            textSize = alturaLinea2 * 0.8f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        
        val margenIzquierdo = stampWidth * 0.05f
        val comentario = etComentario.text.toString()
        val tieneComentario = comentario.isNotEmpty()
        
        // Recalcular alturas si no hay comentario (redistribuir entre líneas 2 y 3)
        val alturaLinea2Final: Float
        val alturaLinea3Final: Float
        val separacionFinal: Float
        
        if (!tieneComentario) {
            // Sin comentario: redistribuir espacio entre coordenadas y fecha
            val espacioExtra = alturaLinea4 + separacion
            alturaLinea2Final = alturaLinea2 + espacioExtra / 2f
            alturaLinea3Final = alturaLinea3 + espacioExtra / 2f
            separacionFinal = separacion
        } else {
            alturaLinea2Final = alturaLinea2
            alturaLinea3Final = alturaLinea3
            separacionFinal = separacion
        }
        
        // Posiciones Y
        val y1 = stampY + margenSuperior + alturaLinea1 * 0.75f
        val y2 = y1 + alturaLinea1 * 0.25f + separacionFinal + alturaLinea2Final * 0.75f
        val y3 = y2 + alturaLinea2Final * 0.25f + separacionFinal + alturaLinea3Final * 0.75f
        val y4 = y3 + alturaLinea3Final * 0.25f + separacionFinal + alturaLinea4 * 0.75f
        
        // Línea 1: TAG_EQUIPO (siempre fija)
        val equipoTexto = if (equipoId.isNullOrBlank()) "SIN EQUIPO ASIG" else equipoId!!
        canvas.drawText(equipoTexto, stampX + margenIzquierdo, y1, paintLinea1)
        
        // Línea 2: Coordenadas con precisión
        val coordenadasTexto = if (!coordenadas.isNullOrBlank() && !precision.isNullOrBlank()) {
            "$coordenadas (±${precision}m)"
        } else if (!coordenadas.isNullOrBlank()) {
            coordenadas!!
        } else {
            "0.0, 0.0 (±0.0m)"
        }
        canvas.drawText(coordenadasTexto, stampX + margenIzquierdo, y2, paintLineas234)
        
        // Línea 3: Fecha
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText(fecha, stampX + margenIzquierdo, y3, paintLineas234)
        
        // Línea 4: Comentario (solo si existe)
        if (tieneComentario) {
            canvas.drawText(comentario, stampX + margenIzquierdo, y4, paintLineas234)
        }
    }
    
    private fun dibujarLogoEnCanvas(canvas: Canvas, elemento: ElementoEditable, escalaX: Float, escalaY: Float) {
        try {
            val inputStream = requireContext().assets.open("LogoBithermSWW.png")
            val logoBitmap = BitmapFactory.decodeStream(inputStream)
            
            if (logoBitmap != null) {
                // Escalar dimensiones y posición
                val widthEscalado = (elemento.width * escalaX).toInt()
                val heightEscalado = (elemento.height * escalaY).toInt()
                val xEscalado = elemento.x * escalaX
                val yEscalado = elemento.y * escalaY
                
                val logoEscalado = Bitmap.createScaledBitmap(
                    logoBitmap,
                    widthEscalado,
                    heightEscalado,
                    true
                )
                canvas.drawBitmap(logoEscalado, xEscalado, yEscalado, null)
                logoEscalado.recycle()
                logoBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e("EditorWhatsApp", "Error al cargar logo: ${e.message}")
        }
    }
    
    private fun extraerComentariosTexto(): String {
        val comentarios = elementosEditables
            .filter { it.tipo == TipoElemento.TEXTO && it.texto.isNotEmpty() }
            .map { it.texto.replace("[^A-Za-z0-9_-]".toRegex(), "_") } // Limpiar caracteres especiales
            .take(3) // Máximo 3 comentarios para evitar nombres muy largos
            .joinToString("_")
        
        Log.d("EditorWhatsApp", "Comentarios extraídos: $comentarios")
        return comentarios
    }
    
    /**
     * Muestra un diálogo para seleccionar un equipo de la base de datos completa
     */
    private fun mostrarSelectorEquipos() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Obtener todos los equipos de la base de datos
                val equipos = database.inspeccionDao().getAllEquiposView()
                
                withContext(Dispatchers.Main) {
                    if (equipos.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "No hay equipos en la base de datos. Sincroniza primero.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@withContext
                    }
                    
                    // Crear array con IDs de equipos para mostrar en el diálogo
                    val equiposIds = equipos.map { it.id }.toTypedArray()
                    
                    // Crear array con información completa para mostrar en el diálogo
                    val equiposDisplay = equipos.map { equipo ->
                        buildString {
                            append(equipo.id)
                            if (!equipo.estado.isNullOrEmpty()) append(" - ${equipo.estado}")
                            if (!equipo.area.isNullOrEmpty()) append(" | ${equipo.area}")
                            if (!equipo.marca.isNullOrEmpty()) append(" | ${equipo.marca}")
                        }
                    }.toTypedArray()
                    
                    // Buscar posición del equipo actual si existe
                    val posicionActual = equipoId?.let { id ->
                        equiposIds.indexOf(id).takeIf { it >= 0 } ?: -1
                    } ?: -1
                    
                    // Mostrar diálogo de selección
                    AlertDialog.Builder(requireContext())
                        .setTitle("Seleccionar Equipo")
                        .setSingleChoiceItems(equiposDisplay, posicionActual) { dialog, which ->
                            // Actualizar el campo TAG con el ID del equipo seleccionado
                            equipoId = equiposIds[which]
                            etTagEquipo.setText(equipoId)
                            
                            // Redibujar STAMP si ya está añadido (el STAMP usa directamente equipoId)
                            elementosEditables.find { it.tipo == TipoElemento.STAMP }?.let {
                                redibujarElementos()
                            }
                            
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancelar", null)
                        .setNeutralButton("Limpiar") { dialog, _ ->
                            equipoId = null
                            etTagEquipo.setText("")
                            
                            // Redibujar STAMP si ya está añadido
                            elementosEditables.find { it.tipo == TipoElemento.STAMP }?.let {
                                redibujarElementos()
                            }
                            
                            dialog.dismiss()
                        }
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("EditorWhatsApp", "Error al cargar equipos: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar equipos: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    /**
     * Deshacer último elemento añadido (eliminar de la lista).
     * Solo elimina elementos añadidos por el usuario (no STAMP ni LOGO).
     */
    private fun deshacerUltimoElemento() {
        // Buscar el último elemento que no sea STAMP ni LOGO
        val ultimoElemento = elementosEditables.findLast { 
            it.tipo != TipoElemento.STAMP && it.tipo != TipoElemento.LOGO 
        }
        
        if (ultimoElemento != null) {
            elementosEditables.remove(ultimoElemento)
            redibujarElementos()
            Toast.makeText(requireContext(), "Elemento eliminado", Toast.LENGTH_SHORT).show()
            Log.d("EditorWhatsApp", "✓ Elemento deshecho: ${ultimoElemento.tipo}")
        } else {
            Toast.makeText(requireContext(), "No hay elementos para deshacer", Toast.LENGTH_SHORT).show()
            Log.d("EditorWhatsApp", "⚠ No hay elementos para deshacer")
        }
    }
    
    /**
     * Mostrar diálogo de búsqueda de equipo por Unidad/Área/Equipo.
     * Filtra en la base de datos completa y permite seleccionar un equipo.
     */
    private fun mostrarDialogoBuscarEquipo() {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // Obtener todos los equipos de la base de datos
                val todosEquipos = withContext(Dispatchers.IO) {
                    database.inspeccionDao().getAllEquiposView()
                }
                
                if (todosEquipos.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "No hay equipos en la base de datos. Sincroniza primero.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                
                // Extraer listas únicas de unidades y áreas
                val unidades = todosEquipos.mapNotNull { it.unidad }.distinct().sorted()
                val areas = todosEquipos.mapNotNull { it.area }.distinct().sorted()
                
                // Crear layout del diálogo
                val layout = android.widget.LinearLayout(requireContext()).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(64, 32, 64, 32)
                    }
                    
                    val tvUnidad = android.widget.TextView(requireContext()).apply {
                        text = "Unidad:"
                        textSize = 14f
                        setPadding(0, 8, 0, 4)
                    }
                    val spinnerUnidad = android.widget.Spinner(requireContext())
                    
                    val tvArea = android.widget.TextView(requireContext()).apply {
                        text = "Área:"
                        textSize = 14f
                        setPadding(0, 16, 0, 4)
                    }
                    val spinnerArea = android.widget.Spinner(requireContext())
                    
                    val tvEquipo = android.widget.TextView(requireContext()).apply {
                        text = "Equipo:"
                        textSize = 14f
                        setPadding(0, 16, 0, 4)
                    }
                    val spinnerEquipo = android.widget.Spinner(requireContext())
                    
                    layout.addView(tvUnidad)
                    layout.addView(spinnerUnidad)
                    layout.addView(tvArea)
                    layout.addView(spinnerArea)
                    layout.addView(tvEquipo)
                    layout.addView(spinnerEquipo)
                    
                    // Configurar adaptadores
                    val adapterUnidad = android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        listOf("Todas") + unidades
                    )
                    adapterUnidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerUnidad.adapter = adapterUnidad
                    
                    val adapterArea = android.widget.ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        listOf("Todas") + areas
                    )
                    adapterArea.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerArea.adapter = adapterArea
                    
                    // Función para actualizar spinner de equipos según filtros
                    fun actualizarEquipos() {
                        val unidadSeleccionada = spinnerUnidad.selectedItem?.toString()
                        val areaSeleccionada = spinnerArea.selectedItem?.toString()
                        
                        var equiposFiltrados = todosEquipos
                        
                        if (unidadSeleccionada != "Todas") {
                            equiposFiltrados = equiposFiltrados.filter { it.unidad == unidadSeleccionada }
                        }
                        
                        if (areaSeleccionada != "Todas") {
                            equiposFiltrados = equiposFiltrados.filter { it.area == areaSeleccionada }
                        }
                        
                        val equiposDisplay = equiposFiltrados.map { equipo ->
                            buildString {
                                append(equipo.id)
                                if (!equipo.estado.isNullOrEmpty()) append(" - ${equipo.estado}")
                                if (!equipo.marca.isNullOrEmpty()) append(" | ${equipo.marca}")
                            }
                        }
                        
                        val adapterEquipo = android.widget.ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            equiposDisplay
                        )
                        adapterEquipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerEquipo.adapter = adapterEquipo
                        spinnerEquipo.tag = equiposFiltrados // Guardar lista filtrada
                    }
                    
                    // Listeners para actualizar al cambiar filtros
                    spinnerUnidad.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            actualizarEquipos()
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    
                    spinnerArea.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                            actualizarEquipos()
                        }
                        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                    }
                    
                    // Inicializar lista de equipos
                    actualizarEquipos()
                    
                // Mostrar diálogo
                AlertDialog.Builder(requireContext())
                    .setTitle("Buscar Equipo")
                    .setView(layout)
                    .setPositiveButton("Seleccionar") { dialog, _ ->
                        @Suppress("UNCHECKED_CAST")
                        val equiposFiltrados = spinnerEquipo.tag as? List<*>
                        val posicion = spinnerEquipo.selectedItemPosition
                        
                        if (equiposFiltrados != null && posicion >= 0 && posicion < equiposFiltrados.size) {
                            val equipoSeleccionado = equiposFiltrados[posicion] as? com.bithermmanagement.core.database.entities.EquipoView
                            equipoSeleccionado?.let { equipo ->
                                equipoId = equipo.id
                                etTagEquipo.setText(equipoId)
                                
                                // Actualizar STAMP si ya está añadido
                                elementoStamp?.stampTag = equipoId ?: "SIN EQUIPO ASIG."
                                redibujarElementos()
                            }
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } catch (e: Exception) {
                Log.e("EditorWhatsApp", "Error al cargar equipos: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error al cargar equipos para búsqueda: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        fun newInstance(
            rutaFoto: String,
            equipoId: String,
            tipoFoto: TipoFoto,
            coordenadas: String?,
            precision: String?,
            altitud: String?,
            nombreProyecto: String? = null,
            tipoDenuncia: String? = null
        ): EditorWhatsAppStyleFragment {
            return EditorWhatsAppStyleFragment().apply {
                arguments = Bundle().apply {
                    putString("ruta_foto", rutaFoto)
                    putString("equipo_id", equipoId)
                    putInt("tipo_foto", tipoFoto.ordinal)
                    putString("coordenadas", coordenadas)
                    putString("precision", precision)
                    putString("altitud", altitud)
                    putString("nombre_proyecto", nombreProyecto)
                    putString("tipo_denuncia", tipoDenuncia)
                }
            }
        }
    }
} 