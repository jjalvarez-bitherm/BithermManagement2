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

class EditorWhatsAppStyleFragment : DialogFragment() {
    
    // Views
    private lateinit var photoView: PhotoView
    private lateinit var btnGuardar: ImageButton
    private lateinit var btnRecortar: ImageButton
    private lateinit var btnRotar: ImageButton
    private lateinit var btnEmoticonos: ImageButton
    private lateinit var btnTexto: ImageButton
    private lateinit var btnDibujar: ImageButton
    private lateinit var etComentario: EditText
    private lateinit var spinnerTipoFoto: Spinner
    private lateinit var btnCancelar: Button
    private lateinit var btnAceptar: Button
    private lateinit var overlayEditables: ElementosEditablesOverlay
    
    // Variables de estado
    private var rutaFotoOriginal: String? = null
    private var equipoId: String? = null
    private var coordenadas: String? = null
    private var precision: String? = null
    private var altitud: String? = null
    private var tipoFoto: TipoFoto = TipoFoto.FOTO_EQUIPO
    private var bitmapOriginal: Bitmap? = null
    private var bitmapActual: Bitmap? = null
    private var rotacionActual: Float = 0f
    
    // Variables para elementos editables
    private var modoDibujo: Boolean = false
    private var modoTexto: Boolean = false
    private var modoEmoticonos: Boolean = false
    private var modoRecorte: Boolean = false
    private var elementosEditables = mutableListOf<ElementoEditable>()
    private var elementoActual: ElementoEditable? = null
    private var elementoSeleccionado: ElementoEditable? = null
    
    // Variables para dibujo
    private var pathDibujo: Path? = null
    private var paintDibujo: Paint? = null
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    
    // Variables para recorte
    private var overlayRecorte: OverlayRecorteView? = null
    private var modoRecorteActivo: Boolean = false
    
    // Listener para notificar cuando se complete la edición
    private var onEdicionCompletadaListener: ((String) -> Unit)? = null
    
    fun setOnEdicionCompletadaListener(listener: (String) -> Unit) {
        onEdicionCompletadaListener = listener
    }
    
    enum class TipoFoto(val descripcion: String, val numero: Int) {
        FOTO_EQUIPO("EQUIPO", 1),
        FOTO_UBICACION("UBICACIÓN", 2),
        FOTO_MANIFOLD("MANIFOLD", 3),
        CAPTURA_GPS("CAPTURA GPS", 4),
        EXTRAS("EXTRA", 5)
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
        var bitmap: Bitmap? = null,
        var seleccionado: Boolean = false
    )
    
    enum class TipoElemento {
        FLECHA, CIRCULO, CUADRADO, TEXTO, DIBUJO, STAMP, LOGO
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
        
        photoView = view.findViewById(R.id.photoView)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnRecortar = view.findViewById(R.id.btnRecortar)
        btnRotar = view.findViewById(R.id.btnRotar)
        btnEmoticonos = view.findViewById(R.id.btnEmoticonos)
        btnTexto = view.findViewById(R.id.btnTexto)
        btnDibujar = view.findViewById(R.id.btnDibujar)
        etComentario = view.findViewById(R.id.etComentario)
        spinnerTipoFoto = view.findViewById(R.id.spinnerTipoFoto)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnAceptar = view.findViewById(R.id.btnAceptar)
        overlayEditables = view.findViewById(R.id.overlayEditables)
        
        Log.d("EditorWhatsApp", "=== TODOS LOS VIEWS INICIALIZADOS ===")
    }
    
    private fun configurarUI() {
        // Configurar PhotoView
        photoView.maximumScale = 5f
        photoView.minimumScale = 0.5f
        photoView.mediumScale = 2f
        
        // Configurar spinner de tipo de foto con los tipos correctos y texto negro
        val tipos = arrayOf("EQUIPO", "UBICACIÓN", "MANIFOLD", "EXTRA")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTipoFoto.adapter = adapter
        
        // Seleccionar el tipo actual
        val posicionActual = when (tipoFoto) {
            TipoFoto.FOTO_EQUIPO -> 0
            TipoFoto.FOTO_UBICACION -> 1
            TipoFoto.FOTO_MANIFOLD -> 2
            else -> 3
        }
        spinnerTipoFoto.setSelection(posicionActual)
        
        // Configurar paint para dibujo
        paintDibujo = Paint().apply {
            color = Color.RED
            strokeWidth = 8f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        
        // Configurar el overlay de elementos editables
        overlayEditables.setPhotoView(photoView)
        overlayEditables.setElementosEditables(elementosEditables)
        
        Log.d("EditorWhatsApp", "UI configurada")
    }
    
    private fun configurarListeners() {
        Log.d("EditorWhatsApp", "=== CONFIGURANDO LISTENERS ===")
        
        btnGuardar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Guardar presionado")
            guardarFoto()
        }
        
        btnRecortar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Recortar presionado")
            activarModoRecorte()
        }
        
        btnRotar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Rotar presionado")
            rotarImagen()
        }
        
        btnEmoticonos.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Emoticonos presionado")
            mostrarSelectorEmoticonos()
        }
        
        btnTexto.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Texto presionado")
            activarModoTexto()
        }
        
        btnDibujar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Dibujar presionado")
            activarModoDibujo()
        }
        
        btnCancelar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Cancelar presionado")
            dismiss()
        }
        
        btnAceptar.setOnClickListener {
            Log.d("EditorWhatsApp", "Botón Aceptar presionado")
            guardarFoto()
        }
        
        // Configurar touch listener para dibujo y selección de elementos
        overlayEditables.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.x
                    lastY = event.y
                    
                    if (modoDibujo) {
                        // Convertir coordenadas de pantalla a coordenadas de imagen
                        val imageView = photoView
                        val imageMatrix = imageView.imageMatrix
                        val inverseMatrix = Matrix()
                        imageMatrix.invert(inverseMatrix)
                        
                        val points = floatArrayOf(event.x, event.y)
                        inverseMatrix.mapPoints(points)
                        
                        pathDibujo = Path()
                        pathDibujo?.moveTo(points[0], points[1])
                        true
                    } else {
                        // Manejar selección de elementos usando coordenadas de pantalla
                        manejarSeleccionElemento(event.x, event.y)
                        elementoSeleccionado != null
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (modoDibujo && pathDibujo != null) {
                        // Convertir coordenadas de pantalla a coordenadas de imagen
                        val imageView = photoView
                        val imageMatrix = imageView.imageMatrix
                        val inverseMatrix = Matrix()
                        imageMatrix.invert(inverseMatrix)
                        
                        val points = floatArrayOf(event.x, event.y)
                        inverseMatrix.mapPoints(points)
                        
                        pathDibujo?.lineTo(points[0], points[1])
                        // Invalidar el overlay para redibujar
                        overlayEditables.invalidate()
                        true
                    } else if (elementoSeleccionado != null) {
                        // Mover elemento seleccionado usando coordenadas de pantalla
                        moverElemento(event.x, event.y)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    if (modoDibujo && pathDibujo != null) {
                        // Añadir el dibujo al bitmap
                        bitmapActual?.let { bitmap ->
                            val bitmapConDibujo = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                            val canvas = Canvas(bitmapConDibujo)
                            paintDibujo?.let { paint ->
                                canvas.drawPath(pathDibujo!!, paint)
                            }
                            bitmapActual = bitmapConDibujo
                            photoView.setImageBitmap(bitmapConDibujo)
                        }
                        pathDibujo = null
                        true
                    } else if (elementoSeleccionado != null) {
                        elementoSeleccionado = null
                        true
                    } else false
                }
                else -> false
            }
        }
        
        Log.d("EditorWhatsApp", "=== TODOS LOS LISTENERS CONFIGURADOS ===")
    }
    
    private fun manejarSeleccionElemento(x: Float, y: Float) {
        // Convertir coordenadas de pantalla a coordenadas de imagen
        val imageView = photoView
        val imageMatrix = imageView.imageMatrix
        val inverseMatrix = Matrix()
        imageMatrix.invert(inverseMatrix)
        
        val points = floatArrayOf(x, y)
        inverseMatrix.mapPoints(points)
        
        val imageX = points[0]
        val imageY = points[1]
        
        // Buscar elemento en la posición tocada
        elementosEditables.forEach { elemento ->
            val rect = RectF(
                elemento.x - elemento.width/2,
                elemento.y - elemento.height/2,
                elemento.x + elemento.width/2,
                elemento.y + elemento.height/2
            )
            if (rect.contains(imageX, imageY)) {
                elementoSeleccionado = elemento
                elemento.seleccionado = true
                return
            }
        }
    }
    
    private fun moverElemento(x: Float, y: Float) {
        elementoSeleccionado?.let { elemento ->
            // Convertir coordenadas de pantalla a coordenadas de imagen
            val imageView = photoView
            val imageMatrix = imageView.imageMatrix
            val inverseMatrix = Matrix()
            imageMatrix.invert(inverseMatrix)
            
            val points = floatArrayOf(x, y)
            inverseMatrix.mapPoints(points)
            
            elemento.x = points[0]
            elemento.y = points[1]
            redibujarElementos()
        }
    }
    

    
    private fun redibujarElementos() {
        // Actualizar el overlay con los elementos editables
        overlayEditables.setElementosEditables(elementosEditables)
    }
    

    
    

    
    private fun cargarFotoOriginal() {
        Log.d("EditorWhatsApp", "=== CARGANDO FOTO ORIGINAL ===")
        
        rutaFotoOriginal?.let { ruta ->
            Log.d("EditorWhatsApp", "Ruta de la foto: $ruta")
            
            val file = File(ruta)
            if (file.exists()) {
                bitmapOriginal = BitmapFactory.decodeFile(ruta)
                Log.d("EditorWhatsApp", "Bitmap original cargado: ${bitmapOriginal != null}")
                
                if (bitmapOriginal != null) {
                    Log.d("EditorWhatsApp", "Dimensiones del bitmap: ${bitmapOriginal!!.width}x${bitmapOriginal!!.height}")
                    
                    // Aplicar rotación automática de 90° horario
                    aplicarRotacionAutomatica()
                    
                    photoView.setImageBitmap(bitmapActual)
                    Log.d("EditorWhatsApp", "Bitmap asignado al PhotoView")
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
            
            // Aplicar rotación de 90° horario automáticamente
            rotacionActual = 90f
            bitmapActual = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            
            // Crear matriz de rotación
            val matrix = Matrix()
            matrix.postRotate(90f)
            
            // Aplicar la rotación
            val bitmapRotado = Bitmap.createBitmap(bitmapActual!!, 0, 0, bitmapActual!!.width, bitmapActual!!.height, matrix, true)
            bitmapActual = bitmapRotado
            
            Log.d("EditorWhatsApp", "Rotación de 90° horario aplicada automáticamente")
            Log.d("EditorWhatsApp", "Dimensiones después de rotación: ${bitmapActual!!.width}x${bitmapActual!!.height}")
        }
    }
    
    private fun activarModoRecorte() {
        Log.d("EditorWhatsApp", "=== ACTIVANDO MODO RECORTE ===")
        
        if (!modoRecorteActivo) {
            // Activar modo recorte
            modoRecorteActivo = true
            
            // Crear overlay de recorte usando la clase existente
            overlayRecorte = OverlayRecorteView(requireContext())
            
            // Configurar el rectángulo inicial de recorte centrado
            val ancho = photoView.width
            val alto = photoView.height
            val size = minOf(ancho, alto) * 0.8f // 80% del tamaño menor
            val left = (ancho - size) / 2f
            val top = (alto - size) / 2f
            val right = left + size
            val bottom = top + size
            
            val rectInicial = RectF(left, top, right, bottom)
            overlayRecorte?.setRectanguloRecorte(rectInicial)
            
            // Configurar listener para cuando se complete el recorte
            overlayRecorte?.setOnRecorteAplicadoListener {
                Log.d("EditorWhatsApp", "Recorte aplicado desde overlay")
                // Por ahora, aplicar un recorte simple centrado
                val rect = RectF(
                    photoView.width * 0.1f,
                    photoView.height * 0.1f,
                    photoView.width * 0.9f,
                    photoView.height * 0.9f
                )
                aplicarRecorte(rect)
                modoRecorteActivo = false
                overlayRecorte = null
            }
            
            // Añadir overlay al PhotoView
            val parent = photoView.parent as ViewGroup
            parent.addView(overlayRecorte)
            
            Toast.makeText(requireContext(), "Modo recorte activado - Toca para aplicar", Toast.LENGTH_SHORT).show()
        } else {
            // Desactivar modo recorte y aplicar recorte
            Log.d("EditorWhatsApp", "Desactivando modo recorte y aplicando recorte")
            
            overlayRecorte?.let { overlay ->
                // Por ahora, aplicar un recorte simple centrado
                val rect = RectF(
                    photoView.width * 0.1f,
                    photoView.height * 0.1f,
                    photoView.width * 0.9f,
                    photoView.height * 0.9f
                )
                Log.d("EditorWhatsApp", "Aplicando recorte con rectángulo: $rect")
                aplicarRecorte(rect)
                
                // Remover overlay
                val parent = overlay.parent as ViewGroup
                parent.removeView(overlay)
            }
            
            overlayRecorte = null
            modoRecorteActivo = false
            
            Toast.makeText(requireContext(), "Recorte aplicado", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun aplicarRecorte(rect: RectF) {
        bitmapActual?.let { bitmap ->
            Log.d("EditorWhatsApp", "=== APLICANDO RECORTE ===")
            Log.d("EditorWhatsApp", "Rectángulo del overlay: $rect")
            Log.d("EditorWhatsApp", "Dimensiones del bitmap: ${bitmap.width}x${bitmap.height}")
            Log.d("EditorWhatsApp", "Dimensiones del PhotoView: ${photoView.width}x${photoView.height}")
            
            // Convertir coordenadas del overlay a coordenadas del bitmap
            val scaleX = bitmap.width.toFloat() / photoView.width
            val scaleY = bitmap.height.toFloat() / photoView.height
            
            val left = (rect.left * scaleX).toInt()
            val top = (rect.top * scaleY).toInt()
            val right = (rect.right * scaleX).toInt()
            val bottom = (rect.bottom * scaleY).toInt()
            
            Log.d("EditorWhatsApp", "Coordenadas convertidas: left=$left, top=$top, right=$right, bottom=$bottom")
            
            // Verificar que las coordenadas están dentro de los límites
            val width = right - left
            val height = bottom - top
            
            if (width > 0 && height > 0 && right <= bitmap.width && bottom <= bitmap.height) {
                val bitmapRecortado = Bitmap.createBitmap(bitmap, left, top, width, height)
                bitmapActual = bitmapRecortado
                photoView.setImageBitmap(bitmapRecortado)
                
                Log.d("EditorWhatsApp", "Recorte aplicado exitosamente: ${bitmapRecortado.width}x${bitmapRecortado.height}")
                Toast.makeText(requireContext(), "Recorte aplicado: ${bitmapRecortado.width}x${bitmapRecortado.height}", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("EditorWhatsApp", "ERROR: Coordenadas de recorte fuera de límites")
                Log.e("EditorWhatsApp", "Width: $width, Height: $height, Right: $right, Bottom: $bottom")
                Toast.makeText(requireContext(), "Error: Coordenadas de recorte inválidas", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e("EditorWhatsApp", "ERROR: bitmapActual es null")
            Toast.makeText(requireContext(), "Error: No hay imagen para recortar", Toast.LENGTH_SHORT).show()
        }
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
    
    private fun mostrarSelectorEmoticonos() {
        Log.d("EditorWhatsApp", "=== MOSTRANDO SELECTOR EMOTICONOS ===")
        
        val opciones = arrayOf("Flecha", "Círculo", "Cuadrado", "Logo Bitherm", "Stamp")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar forma")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> añadirFlecha()
                    1 -> añadirCirculo()
                    2 -> añadirCuadrado()
                    3 -> añadirLogoBitherm()
                    4 -> añadirStamp()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun añadirFlecha() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO FLECHA ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.FLECHA,
            x = bitmapActual?.width?.div(2f) ?: 0f,
            y = bitmapActual?.height?.div(2f) ?: 0f,
            width = 200f,
            height = 100f,
            color = Color.RED
        )
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Flecha añadida como elemento editable")
        Toast.makeText(requireContext(), "Flecha añadida - Toca para seleccionar", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun añadirCirculo() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO CÍRCULO ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.CIRCULO,
            x = bitmapActual?.width?.div(2f) ?: 0f,
            y = bitmapActual?.height?.div(2f) ?: 0f,
            width = 150f,
            height = 150f,
            color = Color.BLUE
        )
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Círculo añadido como elemento editable")
        Toast.makeText(requireContext(), "Círculo añadido - Toca para seleccionar", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun añadirCuadrado() {
        Log.d("EditorWhatsApp", "=== AÑADIENDO CUADRADO ===")
        
        val elemento = ElementoEditable(
            tipo = TipoElemento.CUADRADO,
            x = bitmapActual?.width?.div(2f) ?: 0f,
            y = bitmapActual?.height?.div(2f) ?: 0f,
            width = 150f,
            height = 150f,
            color = Color.GREEN
        )
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Cuadrado añadido como elemento editable")
        Toast.makeText(requireContext(), "Cuadrado añadido - Toca para seleccionar", Toast.LENGTH_SHORT).show()
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
            x = bitmapActual?.width?.div(2f) ?: 0f,
            y = bitmapActual?.height?.div(2f) ?: 0f,
            width = 300f,
            height = 100f,
            texto = texto,
            color = Color.WHITE
        )
        
        elementosEditables.add(elemento)
        redibujarElementos()
        
        Log.d("EditorWhatsApp", "Texto añadido como elemento editable")
        Toast.makeText(requireContext(), "Texto añadido - Toca para seleccionar", Toast.LENGTH_SHORT).show()
    }
    

    
    private fun activarModoDibujo() {
        Log.d("EditorWhatsApp", "=== ACTIVANDO MODO DIBUJO ===")
        
        modoDibujo = !modoDibujo
        
        if (modoDibujo) {
            Toast.makeText(requireContext(), "Modo dibujo activado - Dibuja sobre la imagen", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Modo dibujo desactivado", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun guardarFoto() {
        Log.d("EditorWhatsApp", "=== GUARDANDO FOTO ===")
        
        bitmapActual?.let { bitmap ->
            try {
                // Obtener el tipo seleccionado del spinner
                val tipoSeleccionado = spinnerTipoFoto.selectedItem.toString()
                val tipoFotoEnum = when (tipoSeleccionado) {
                    "EQUIPO" -> TipoFoto.FOTO_EQUIPO
                    "UBICACIÓN" -> TipoFoto.FOTO_UBICACION
                    "MANIFOLD" -> TipoFoto.FOTO_MANIFOLD
                    else -> TipoFoto.EXTRAS
                }
                
                // Crear nombre de archivo con formato: TAG_TIPOFOTO_FECHA
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val tipoFotoString = when (tipoFotoEnum) {
                    TipoFoto.FOTO_EQUIPO -> "EQUIPO"
                    TipoFoto.FOTO_UBICACION -> "UBICACION"
                    TipoFoto.FOTO_MANIFOLD -> "MANIFOLD"
                    else -> "EXTRA"
                }
                val nombreArchivo = "${equipoId}_${tipoFotoString}_${timeStamp}.jpg"
                
                Log.d("EditorWhatsApp", "Nombre del archivo: $nombreArchivo")
                Log.d("EditorWhatsApp", "Tipo seleccionado: $tipoSeleccionado")
                
                // Crear directorio si no existe
                val directorio = File(requireContext().filesDir, "fotos")
                if (!directorio.exists()) {
                    directorio.mkdirs()
                }
                
                val archivoFinal = File(directorio, nombreArchivo)
                Log.d("EditorWhatsApp", "Ruta del archivo final: ${archivoFinal.absolutePath}")
                
                // Guardar bitmap como JPEG
                val outputStream = FileOutputStream(archivoFinal)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()
                
                Log.d("EditorWhatsApp", "Archivo guardado exitosamente")
                Log.d("EditorWhatsApp", "Tamaño del archivo final: ${archivoFinal.length()} bytes")
                
                // Mostrar mensaje de éxito
                Toast.makeText(requireContext(), "Foto guardada: $nombreArchivo", Toast.LENGTH_LONG).show()
                
                // Notificar al listener que la edición se completó
                onEdicionCompletadaListener?.invoke(archivoFinal.absolutePath)
                
                // Cerrar el editor
                dismiss()
                
                Log.d("EditorWhatsApp", "=== GUARDADO FINALIZADO EXITOSAMENTE ===")
                
            } catch (e: Exception) {
                Log.e("EditorWhatsApp", "ERROR al guardar la foto final: ${e.message}")
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al guardar la foto", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Log.e("EditorWhatsApp", "ERROR: bitmapActual es null")
            Toast.makeText(requireContext(), "Error: No hay imagen para guardar", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        fun newInstance(
            rutaFoto: String,
            equipoId: String,
            tipoFoto: TipoFoto,
            coordenadas: String?,
            precision: String?,
            altitud: String?
        ): EditorWhatsAppStyleFragment {
            return EditorWhatsAppStyleFragment().apply {
                arguments = Bundle().apply {
                    putString("ruta_foto", rutaFoto)
                    putString("equipo_id", equipoId)
                    putInt("tipo_foto", tipoFoto.ordinal)
                    putString("coordenadas", coordenadas)
                    putString("precision", precision)
                    putString("altitud", altitud)
                }
            }
        }
    }
} 