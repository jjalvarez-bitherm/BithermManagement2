package com.bithermmanagement.multimedia

import android.app.Dialog
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bithermmanagement.multimedia.R
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Typeface
import android.graphics.RectF
import android.graphics.Path
import android.graphics.Rect
import androidx.appcompat.app.AlertDialog
import android.view.MotionEvent
import android.graphics.Matrix
import kotlin.math.*
import java.io.IOException
import android.util.Log

class EditorFotosPasoAPasoFragment : androidx.fragment.app.DialogFragment() {
    
    // Views
    private lateinit var photoView: PhotoView
    private lateinit var tvPasoActual: TextView
    private lateinit var tvTipoFoto: TextView
    private lateinit var btnDeshacer: ImageButton
    private lateinit var btnCancelar: ImageButton
    private lateinit var overlayRecorte: View
    
    // Controles por paso
    private lateinit var controlesPaso1: LinearLayout
    private lateinit var controlesPaso2: LinearLayout
    private lateinit var controlesPaso3: LinearLayout
    private lateinit var controlesPaso4: LinearLayout
    private lateinit var btnSiguiente: Button
    private lateinit var btnAtras: Button
    private lateinit var btnGirar: ImageButton
    private lateinit var btnRecortar: ImageButton
    private lateinit var btnNota: ImageButton
    private lateinit var btnFlecha: ImageButton
    private lateinit var btnFin: Button
    
    // Barra de progreso
    private lateinit var paso1: View
    private lateinit var paso2: View
    private lateinit var paso3: View
    private lateinit var paso4: View
    
    // Variables de estado
    private var rutaFotoOriginal: String? = null
    private var equipoId: String? = null
    private var coordenadas: String? = null
    private var precision: String? = null
    private var altitud: String? = null
    private var tipoFoto: TipoFoto = TipoFoto.FOTO_EQUIPO
    private var pasoActual: Int = 1
    private var bitmapOriginal: Bitmap? = null
    private var bitmapActual: Bitmap? = null
    private var rotacionActual: Float = 0f
    private var necesitaRotacion: Boolean = false
    
    // Variables para flechas y texto
    private var modoDibujoFlechas: Boolean = false
    private var flechas = mutableListOf<FlechaEditable>()
    private var flechaActual: FlechaEditable? = null
    private var textoNota: String = ""
    private var modoRecorte: Boolean = false
    private var rectanguloRecorte: RectF? = null
    
    // Historial para deshacer
    private val historial = mutableListOf<EstadoFoto>()
    
    enum class TipoFoto(val descripcion: String, val numero: Int) {
        FOTO_EQUIPO("FOTO EQUIPO", 1),
        FOTO_UBICACION("FOTO UBICACIÓN", 2),
        FOTO_MANIFOLD("FOTO MANIFOLD", 3),
        CAPTURA_GPS("CAPTURA GPS", 4),
        EXTRAS("EXTRAS", 5)
    }
    
    data class EstadoFoto(
        val bitmap: Bitmap,
        val rotacion: Float,
        val paso: Int,
        val descripcion: String
    )
    
    data class FlechaEditable(
        var x: Float,
        var y: Float,
        var escala: Float = 1f,
        var rotacion: Float = 0f,
        var ancho: Float = 100f,
        var alto: Float = 50f
    )
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.fragment_editor_fotos_paso_a_paso)
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
        return inflater.inflate(R.layout.fragment_editor_fotos_paso_a_paso, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("EditorFotos", "=== ONVIEWCREATED INICIADO ===")
        
        // Obtener argumentos
        arguments?.let { args ->
            rutaFotoOriginal = args.getString("ruta_foto")
            equipoId = args.getString("equipo_id")
            coordenadas = args.getString("coordenadas")
            precision = args.getString("precision")
            altitud = args.getString("altitud")
            tipoFoto = TipoFoto.values()[args.getInt("tipo_foto", 0)]
            
            Log.d("EditorFotos", "Argumentos recibidos:")
            Log.d("EditorFotos", "- rutaFotoOriginal: $rutaFotoOriginal")
            Log.d("EditorFotos", "- equipoId: $equipoId")
            Log.d("EditorFotos", "- coordenadas: $coordenadas")
            Log.d("EditorFotos", "- precision: $precision")
            Log.d("EditorFotos", "- altitud: $altitud")
            Log.d("EditorFotos", "- tipoFoto: $tipoFoto")
        }
        
        Log.d("EditorFotos", "Inicializando views...")
        inicializarViews(view)
        
        Log.d("EditorFotos", "Configurando UI...")
        configurarUI()
        
        Log.d("EditorFotos", "Cargando foto original...")
        cargarFotoOriginal()
        
        Log.d("EditorFotos", "Configurando listeners...")
        configurarListeners()
        
        Log.d("EditorFotos", "Actualizando paso inicial...")
        actualizarPaso(1)
        
        Log.d("EditorFotos", "=== ONVIEWCREATED COMPLETADO ===")
    }
    
    private fun inicializarViews(view: View) {
        Log.d("EditorFotos", "=== INICIALIZANDO VIEWS ===")
        
        photoView = view.findViewById(R.id.photoView)
        Log.d("EditorFotos", "photoView encontrado: ${photoView != null}")
        
        tvPasoActual = view.findViewById(R.id.tvPasoActual)
        Log.d("EditorFotos", "tvPasoActual encontrado: ${tvPasoActual != null}")
        
        tvTipoFoto = view.findViewById(R.id.tvTipoFoto)
        Log.d("EditorFotos", "tvTipoFoto encontrado: ${tvTipoFoto != null}")
        
        btnDeshacer = view.findViewById(R.id.btnDeshacer)
        Log.d("EditorFotos", "btnDeshacer encontrado: ${btnDeshacer != null}")
        
        btnCancelar = view.findViewById(R.id.btnCancelar)
        Log.d("EditorFotos", "btnCancelar encontrado: ${btnCancelar != null}")
        
        overlayRecorte = view.findViewById(R.id.overlayRecorte)
        Log.d("EditorFotos", "overlayRecorte encontrado: ${overlayRecorte != null}")
        Log.d("EditorFotos", "overlayRecorte tipo: ${overlayRecorte?.javaClass?.simpleName}")
        
        // Controles por paso
        controlesPaso1 = view.findViewById(R.id.controlesPaso1)
        Log.d("EditorFotos", "controlesPaso1 encontrado: ${controlesPaso1 != null}")
        
        controlesPaso2 = view.findViewById(R.id.controlesPaso2)
        Log.d("EditorFotos", "controlesPaso2 encontrado: ${controlesPaso2 != null}")
        
        controlesPaso3 = view.findViewById(R.id.controlesPaso3)
        Log.d("EditorFotos", "controlesPaso3 encontrado: ${controlesPaso3 != null}")
        
        controlesPaso4 = view.findViewById(R.id.controlesPaso4)
        Log.d("EditorFotos", "controlesPaso4 encontrado: ${controlesPaso4 != null}")
        
        btnSiguiente = view.findViewById(R.id.btnSiguiente)
        Log.d("EditorFotos", "btnSiguiente encontrado: ${btnSiguiente != null}")
        
        btnAtras = view.findViewById(R.id.btnAtras)
        Log.d("EditorFotos", "btnAtras encontrado: ${btnAtras != null}")
        
        btnGirar = view.findViewById(R.id.btnGirar)
        Log.d("EditorFotos", "btnGirar encontrado: ${btnGirar != null}")
        
        btnRecortar = view.findViewById(R.id.btnRecortar)
        Log.d("EditorFotos", "btnRecortar encontrado: ${btnRecortar != null}")
        
        btnNota = view.findViewById(R.id.btnNota)
        Log.d("EditorFotos", "btnNota encontrado: ${btnNota != null}")
        
        btnFlecha = view.findViewById(R.id.btnFlecha)
        Log.d("EditorFotos", "btnFlecha encontrado: ${btnFlecha != null}")
        
        btnFin = view.findViewById(R.id.btnFin)
        Log.d("EditorFotos", "btnFin encontrado: ${btnFin != null}")
        
        // Barra de progreso
        paso1 = view.findViewById(R.id.paso1)
        Log.d("EditorFotos", "paso1 encontrado: ${paso1 != null}")
        
        paso2 = view.findViewById(R.id.paso2)
        Log.d("EditorFotos", "paso2 encontrado: ${paso2 != null}")
        
        paso3 = view.findViewById(R.id.paso3)
        Log.d("EditorFotos", "paso3 encontrado: ${paso3 != null}")
        
        paso4 = view.findViewById(R.id.paso4)
        Log.d("EditorFotos", "paso4 encontrado: ${paso4 != null}")
        
        Log.d("EditorFotos", "=== TODOS LOS VIEWS INICIALIZADOS ===")
    }
    
    private fun configurarUI() {
        // Configurar PhotoView
        photoView.maximumScale = 5f
        photoView.minimumScale = 0.5f
        photoView.mediumScale = 2f
        
        // Configurar tipo de foto
        tvTipoFoto.text = tipoFoto.descripcion
        
        // Configurar overlay de recorte
        overlayRecorte.visibility = View.GONE
    }
    
    private fun cargarFotoOriginal() {
        rutaFotoOriginal?.let { ruta ->
            val file = File(ruta)
            if (file.exists()) {
                bitmapOriginal = BitmapFactory.decodeFile(ruta)
                bitmapActual = bitmapOriginal?.copy(Bitmap.Config.ARGB_8888, true)
                
                // Detectar si necesita rotación automática
                detectarRotacionNecesaria()
                
                photoView.setImageBitmap(bitmapActual)
                
                // Guardar estado inicial
                guardarEstado("Estado inicial")
            }
        }
    }
    
    private fun detectarRotacionNecesaria() {
        bitmapOriginal?.let { bitmap ->
            // Si la imagen es más alta que ancha, probablemente necesita rotación
            if (bitmap.height > bitmap.width) {
                necesitaRotacion = true
                // Aplicar rotación automática de 90 grados
                rotacionActual = 90f
                val bitmapRotado = aplicarRotacion(bitmap, rotacionActual)
                bitmapActual = bitmapRotado
                photoView.setImageBitmap(bitmapRotado)
                Toast.makeText(requireContext(), "Rotación automática aplicada", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun configurarListeners() {
        Log.d("EditorFotos", "=== CONFIGURANDO LISTENERS ===")
        
        btnCancelar.setOnClickListener {
            Log.d("EditorFotos", "Botón Cancelar presionado")
            parentFragmentManager.popBackStack()
        }
        Log.d("EditorFotos", "Listener de btnCancelar configurado")
        
        btnDeshacer.setOnClickListener {
            Log.d("EditorFotos", "Botón Deshacer presionado")
            deshacer()
        }
        Log.d("EditorFotos", "Listener de btnDeshacer configurado")
        
        btnSiguiente.setOnClickListener {
            Log.d("EditorFotos", "Botón Siguiente presionado")
            siguientePaso()
        }
        Log.d("EditorFotos", "Listener de btnSiguiente configurado")
        
        btnAtras.setOnClickListener {
            Log.d("EditorFotos", "Botón Atrás presionado")
            pasoAnterior()
        }
        Log.d("EditorFotos", "Listener de btnAtras configurado")
        
        // Controles del Paso 1: Rotación
        btnGirar.setOnClickListener {
            Log.d("EditorFotos", "Botón Girar presionado")
            girarImagen()
        }
        Log.d("EditorFotos", "Listener de btnGirar configurado")
        
        // Controles del Paso 2: Recorte
        btnRecortar.setOnClickListener {
            Log.d("EditorFotos", "Botón Recortar presionado")
            activarModoRecorte()
        }
        Log.d("EditorFotos", "Listener de btnRecortar configurado")
        
        // Controles del Paso 3: Nota
        btnNota.setOnClickListener {
            Log.d("EditorFotos", "Botón Nota presionado")
            mostrarDialogoNota()
        }
        Log.d("EditorFotos", "Listener de btnNota configurado")
        
        // Controles del Paso 4: Flecha
        btnFlecha.setOnClickListener {
            Log.d("EditorFotos", "Botón Flecha presionado")
            añadirFlecha()
        }
        Log.d("EditorFotos", "Listener de btnFlecha configurado")
        
        btnFin.setOnClickListener {
            Log.d("EditorFotos", "Botón Fin presionado")
            guardarFinal()
        }
        Log.d("EditorFotos", "Listener de btnFin configurado")
        
        Log.d("EditorFotos", "=== TODOS LOS LISTENERS CONFIGURADOS ===")
    }
    
    private fun actualizarPaso(nuevoPaso: Int) {
        Log.d("EditorFotos", "=== ACTUALIZANDO PASO ===")
        Log.d("EditorFotos", "Paso anterior: $pasoActual, Nuevo paso: $nuevoPaso")
        
        pasoActual = nuevoPaso
        
        // Actualizar texto del paso
        val textoPaso = when (pasoActual) {
            1 -> "Paso 1: Rotación"
            2 -> "Paso 2: Recorte"
            3 -> "Paso 3: Nota"
            4 -> "Paso 4: Flecha"
            else -> "Paso $pasoActual"
        }
        tvPasoActual.text = textoPaso
        Log.d("EditorFotos", "Texto del paso actualizado: $textoPaso")
        
        // Actualizar barra de progreso
        actualizarBarraProgreso()
        
        // Mostrar/ocultar controles según el paso
        Log.d("EditorFotos", "=== CONFIGURANDO VISIBILIDAD DE CONTROLES ===")
        
        val visibilidadPaso1 = if (pasoActual == 1) View.VISIBLE else View.GONE
        controlesPaso1.visibility = visibilidadPaso1
        Log.d("EditorFotos", "controlesPaso1.visibility = $visibilidadPaso1")
        
        val visibilidadPaso2 = if (pasoActual == 2) View.VISIBLE else View.GONE
        controlesPaso2.visibility = visibilidadPaso2
        Log.d("EditorFotos", "controlesPaso2.visibility = $visibilidadPaso2")
        
        val visibilidadPaso3 = if (pasoActual == 3) View.VISIBLE else View.GONE
        controlesPaso3.visibility = visibilidadPaso3
        Log.d("EditorFotos", "controlesPaso3.visibility = $visibilidadPaso3")
        
        val visibilidadPaso4 = if (pasoActual == 4) View.VISIBLE else View.GONE
        controlesPaso4.visibility = visibilidadPaso4
        Log.d("EditorFotos", "controlesPaso4.visibility = $visibilidadPaso4")
        
        // Mostrar botón deshacer si hay historial
        val visibilidadDeshacer = if (historial.size > 1) View.VISIBLE else View.GONE
        btnDeshacer.visibility = visibilidadDeshacer
        Log.d("EditorFotos", "btnDeshacer.visibility = $visibilidadDeshacer (historial.size = ${historial.size})")
        
        // Configurar botones según el paso
        configurarBotonesPaso()
        
        Log.d("EditorFotos", "=== PASO ACTUALIZADO COMPLETAMENTE ===")
    }
    
    private fun configurarBotonesPaso() {
        when (pasoActual) {
            1 -> {
                btnSiguiente.text = "Siguiente"
                btnAtras.visibility = View.GONE
            }
            2 -> {
                btnSiguiente.text = "Siguiente"
                btnAtras.visibility = View.VISIBLE
            }
            3 -> {
                btnSiguiente.text = "Siguiente"
                btnAtras.visibility = View.VISIBLE
            }
            4 -> {
                btnSiguiente.visibility = View.GONE
                btnAtras.visibility = View.VISIBLE
            }
        }
    }
    
    private fun actualizarBarraProgreso() {
        paso1.setBackgroundResource(if (pasoActual >= 1) R.color.azul_bitherm else android.R.color.darker_gray)
        paso2.setBackgroundResource(if (pasoActual >= 2) R.color.azul_bitherm else android.R.color.darker_gray)
        paso3.setBackgroundResource(if (pasoActual >= 3) R.color.azul_bitherm else android.R.color.darker_gray)
        paso4.setBackgroundResource(if (pasoActual >= 4) R.color.azul_bitherm else android.R.color.darker_gray)
    }
    
    private fun girarImagen() {
        bitmapActual?.let { bitmap ->
            rotacionActual += 90f
            if (rotacionActual >= 360) rotacionActual = 0f
            
            val bitmapRotado = aplicarRotacion(bitmap, rotacionActual)
            bitmapActual = bitmapRotado
            photoView.setImageBitmap(bitmapRotado)
            
            guardarEstado("Rotación ${rotacionActual.toInt()}°")
        }
    }
    
    private fun activarModoRecorte() {
        Log.d("EditorFotos", "=== ACTIVAR MODO RECORTE ===")
        Log.d("EditorFotos", "modoRecorte actual: $modoRecorte")
        Log.d("EditorFotos", "overlayRecorte encontrado: ${::overlayRecorte.isInitialized}")
        
        modoRecorte = !modoRecorte
        Log.d("EditorFotos", "modoRecorte cambiado a: $modoRecorte")
        
        if (modoRecorte) {
            Log.d("EditorFotos", "Activando overlay de recorte...")
            overlayRecorte.visibility = View.VISIBLE
            
            // Crear rectángulo de recorte centrado
            val ancho = photoView.width
            val alto = photoView.height
            Log.d("EditorFotos", "Dimensiones photoView: ${ancho}x${alto}")
            
            val size = minOf(ancho, alto) * 0.8f // 80% del tamaño menor
            val left = (ancho - size) / 2f
            val top = (alto - size) / 2f
            val right = left + size
            val bottom = top + size
            
            rectanguloRecorte = RectF(left, top, right, bottom)
            Log.d("EditorFotos", "Rectángulo de recorte creado: $rectanguloRecorte")
            
            // Configurar el overlay con el rectángulo
            if (overlayRecorte is OverlayRecorteView) {
                (overlayRecorte as OverlayRecorteView).setRectanguloRecorte(rectanguloRecorte)
                (overlayRecorte as OverlayRecorteView).setOnRecorteAplicadoListener {
                    Log.d("EditorFotos", "Toque en overlay detectado - aplicando recorte")
                    activarModoRecorte() // Esto desactivará el modo y aplicará el recorte
                }
                Log.d("EditorFotos", "Overlay configurado con rectángulo y listener")
            } else {
                Log.e("EditorFotos", "ERROR: overlayRecorte no es OverlayRecorteView")
            }
            
            Toast.makeText(requireContext(), "Modo recorte activado - Toca para aplicar", Toast.LENGTH_SHORT).show()
        } else {
            Log.d("EditorFotos", "Desactivando overlay de recorte...")
            overlayRecorte.visibility = View.GONE
            // Aplicar recorte
            aplicarRecorte()
        }
    }
    
    private fun aplicarRecorte() {
        bitmapActual?.let { bitmap ->
            // Crear bitmap 1:1 del área visible
            val size = minOf(bitmap.width, bitmap.height)
            val bitmapRecortado = Bitmap.createBitmap(bitmap, 0, 0, size, size)
            
            bitmapActual = bitmapRecortado
            photoView.setImageBitmap(bitmapRecortado)
            
            guardarEstado("Recorte 1:1 aplicado")
        }
    }
    
    private fun mostrarDialogoNota() {
        val editText = EditText(requireContext()).apply {
            hint = "Introduce una nota (opcional)..."
            setText(textoNota)
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Añadir Nota")
            .setMessage("Introduce una nota para incluir en la foto")
            .setView(editText)
            .setPositiveButton("Añadir") { dialog, which ->
                textoNota = editText.text.toString()
                
                bitmapActual?.let { bitmap ->
                    val bitmapConNota = crearMarcaDeAgua(bitmap)
                    bitmapActual = bitmapConNota
                    photoView.setImageBitmap(bitmapConNota)
                    guardarEstado("Nota añadida: ${if (textoNota.isNotEmpty()) textoNota else "sin nota"}")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun añadirFlecha() {
        // Crear una flecha en el centro de la pantalla
        val centerX = photoView.width / 2f
        val centerY = photoView.height / 2f
        val anchoFlecha = photoView.width * 0.3f // 30% del ancho
        
        val flecha = FlechaEditable(
            x = centerX,
            y = centerY,
            ancho = anchoFlecha,
            alto = anchoFlecha * 0.5f
        )
        
        flechas.add(flecha)
        flechaActual = flecha
        modoDibujoFlechas = true
        
        photoView.invalidate()
        Toast.makeText(requireContext(), "Flecha añadida", Toast.LENGTH_SHORT).show()
    }
    
    private fun siguientePaso() {
        when (pasoActual) {
            1 -> actualizarPaso(2)
            2 -> actualizarPaso(3)
            3 -> actualizarPaso(4)
        }
    }
    
    private fun pasoAnterior() {
        when (pasoActual) {
            2 -> actualizarPaso(1)
            3 -> actualizarPaso(2)
            4 -> actualizarPaso(3)
        }
    }
    
    private fun guardarEstado(descripcion: String) {
        bitmapActual?.let { bitmap ->
            val estado = EstadoFoto(
                bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true),
                rotacion = rotacionActual,
                paso = pasoActual,
                descripcion = descripcion
            )
            historial.add(estado)
            
            // Limitar el historial a 10 estados
            if (historial.size > 10) {
                historial.removeAt(0)
            }
        }
    }
    
    private fun deshacer() {
        if (historial.size > 1) {
            historial.removeAt(historial.size - 1)
            val estadoAnterior = historial.last()
            
            bitmapActual = estadoAnterior.bitmap
            rotacionActual = estadoAnterior.rotacion
            pasoActual = estadoAnterior.paso
            
            photoView.setImageBitmap(bitmapActual)
            actualizarPaso(pasoActual)
            
            Toast.makeText(requireContext(), "Deshecho: ${estadoAnterior.descripcion}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun aplicarRotacion(bitmap: Bitmap, rotacion: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(rotacion)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    private fun crearMarcaDeAgua(imagenOriginal: Bitmap): Bitmap {
        var bitmapResultado = imagenOriginal.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapResultado)
        
        // Calcular tamaños proporcionales
        val imageWidth = imagenOriginal.width
        val imageHeight = imagenOriginal.height
        val padding = imageWidth * 0.02f
        val fontSize = imageWidth * 0.025f
        val lineHeight = fontSize * 1.3f
        
        // Crear fondo semiopaco
        val paintFondo = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }
        
        // Calcular contenido
        val contenido = mutableListOf<String>()
        
        // TAG del equipo
        equipoId?.let { contenido.add(it) }
        
        // Coordenadas
        val coordenadasCompletas = StringBuilder().apply {
            coordenadas?.let { append(it) }
            precision?.let { append(" ±${it}m") }
            altitud?.let { append(" (${it}m)") }
        }.toString()
        if (coordenadasCompletas.isNotEmpty()) {
            contenido.add(coordenadasCompletas)
        }
        
        // Nota personalizada
        if (textoNota.isNotEmpty()) {
            contenido.add(textoNota)
        }
        
        // Dibujar fondo si hay contenido
        if (contenido.isNotEmpty()) {
            val logoSize = 200f // Tamaño fijo para el logo
            val infoWidth = imageWidth * 0.5f
            val alturaReal = logoSize + (lineHeight * contenido.size) + (padding * 2)
            
            val rectFondo = RectF(padding, padding, padding + infoWidth, padding + alturaReal)
            canvas.drawRoundRect(rectFondo, padding, padding, paintFondo)
            
            // Dibujar logo
            val logoBitmap = crearLogoBasico()
            val logoWidth = infoWidth * 0.7f
            val logoHeight = logoWidth * (logoBitmap.height.toFloat() / logoBitmap.width.toFloat())
            val logoX = imageWidth - logoWidth - padding
            val logoY = padding
            val logoRect = RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight)
            canvas.drawBitmap(logoBitmap, null, logoRect, null)
            
            // Dibujar texto
            val paintTexto = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = fontSize
                typeface = Typeface.DEFAULT_BOLD
            }
            
            val paintTag = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = fontSize * 3.0f
                typeface = Typeface.DEFAULT_BOLD
            }
            
            val contenidoHeight = lineHeight * contenido.size
            val startY = padding + (alturaReal - contenidoHeight) / 2
            
            contenido.forEachIndexed { index, texto ->
                val currentY = startY + (index * lineHeight)
                val paint = if (index == 0) paintTag else paintTexto
                canvas.drawText(texto, padding + 20f, currentY, paint)
            }
        }
        
        return bitmapResultado
    }
    
    private fun crearLogoBasico(): Bitmap {
        // Intentar cargar el logo desde assets
        try {
            val assetManager = requireContext().assets
            assetManager.open("logo_empresa.png").use { inputStream ->
                return BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            // Si no existe, generar el logo por código
            val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            // Fondo azul
            val paintFondo = Paint().apply {
                color = Color.BLUE
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, 200f, 200f, paintFondo)
            // Texto "BITHERM"
            val paintTexto = Paint().apply {
                isAntiAlias = true
                color = Color.WHITE
                textSize = 40f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("BITHERM", 100f, 120f, paintTexto)
            return bitmap
        }
    }
    
    private fun guardarFinal() {
        guardarFotoEditada()
    }
    
    private fun guardarFotoEditada() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bitmapActual?.let { bitmap ->
                    val fotoEditada = guardarBitmap(bitmap)
                    
                    withContext(Dispatchers.Main) {
                        // Notificar al fragmento padre
                        val bundle = Bundle().apply {
                            putString("ruta_foto_editada", fotoEditada.absolutePath)
                            putString("ruta_foto_original", rutaFotoOriginal)
                            putInt("tipo_foto", tipoFoto.numero)
                            putBoolean("foto_actualizada", true)
                        }
                        parentFragmentManager.setFragmentResult("foto_editada", bundle)
                        
                        Toast.makeText(requireContext(), "Foto guardada exitosamente", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun guardarBitmap(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "edited_${equipoId}_${tipoFoto.numero}_$timeStamp.jpg"
        
        val dir = File(requireContext().filesDir, "fotos_editadas")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        val file = File(dir, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.close()
        
        return file
    }
    
    companion object {
        fun newInstance(
            rutaFoto: String,
            equipoId: String,
            tipoFoto: TipoFoto,
            coordenadas: String?,
            precision: String?,
            altitud: String?
        ): EditorFotosPasoAPasoFragment {
            return EditorFotosPasoAPasoFragment().apply {
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