package com.bithermmanagement.multimedia

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

class EditorFotosFragment : androidx.fragment.app.DialogFragment() {
    
    private lateinit var photoView: PhotoView
    private lateinit var btnGuardar: ImageButton
    private lateinit var btnCancelar: ImageButton
    private lateinit var btnFlecha: ImageButton
    private lateinit var btnTexto: ImageButton
    private lateinit var btnRecortar: ImageButton
    private lateinit var btnGirar: ImageButton
    
    private var rutaFotoOriginal: String? = null
    private var equipoId: String? = null
    private var coordenadas: String? = null
    private var precision: String? = null
    private var altitud: String? = null
    private var logoEmpresa: Bitmap? = null
    private var bitmapOriginal: Bitmap? = null
    private var bitmapActual: Bitmap? = null
    private var rotacionActual: Float = 0f
    private var textoNota: String = ""
    
    // Variables para mantener el estado de transformaciones
    private var rotacionGuardada: Float = 0f
    private var escalaGuardada: Float = 1f
    private var posicionXGuardada: Float = 0f
    private var posicionYGuardada: Float = 0f
    private var recorteAplicado: Boolean = false
    private var bitmapRecortado: Bitmap? = null
    
    private val flechas = mutableListOf<Flecha>()
    private var flechaActual: Flecha? = null
    private var modoDibujo = false
    
    // Editor de capas editables
    private val editorCapas = EditorCapas()
    

    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cargarLogoEmpresa()
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.fragment_editor_fotos)
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
        return inflater.inflate(R.layout.fragment_editor_fotos, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Inicializar vistas
        photoView = view.findViewById(R.id.photoView)
        btnGuardar = view.findViewById(R.id.btnGuardar)
        btnCancelar = view.findViewById(R.id.btnCancelar)
        btnFlecha = view.findViewById(R.id.btnFlecha)
        btnTexto = view.findViewById(R.id.btnTexto)
        btnRecortar = view.findViewById(R.id.btnRecortar)
        btnGirar = view.findViewById(R.id.btnGirar)
        
        // Obtener argumentos
        arguments?.let { args ->
            rutaFotoOriginal = args.getString("ruta_foto")
            equipoId = args.getString("equipo_id")
            coordenadas = args.getString("coordenadas")
            precision = args.getString("precision")
            altitud = args.getString("altitud")
        }
        
        // Configurar UI
        configurarUI()
        
        // Cargar foto original
        cargarFotoOriginal()
        
        // Cargar logo de empresa
        verificarPermisosYCargarLogo()
        
        // Configurar listeners
        configurarListeners()
    }
    
    private fun configurarUI() {
        // Configurar PhotoView para permitir zoom y pan
        photoView.maximumScale = 5f
        photoView.minimumScale = 0.5f
        photoView.mediumScale = 2f
        
        // Configurar iconos de botones
        btnFlecha.setImageResource(R.drawable.ic_arrow) // Flecha para dibujar flechas en la foto
        btnTexto.setImageResource(R.drawable.ic_add_photo) // Añadir nota
        btnCancelar.setImageResource(R.drawable.ic_close) // Cancelar y volver
        btnGuardar.setImageResource(R.drawable.ic_save) // Guardar cambios
        btnRecortar.setImageResource(R.drawable.ic_crop) // Recortar imagen
        btnGirar.setImageResource(R.drawable.ic_rotate) // Girar imagen
    }
    
    private fun cargarFotoOriginal() {
        println("=== CARGANDO FOTO ORIGINAL ===")
        rutaFotoOriginal?.let { ruta ->
            val file = File(ruta)
            if (file.exists()) {
                bitmapOriginal = BitmapFactory.decodeFile(ruta)
                bitmapActual = bitmapOriginal?.copy(Bitmap.Config.ARGB_8888, true)
                
                // Inicializar capas editables
                inicializarCapas()
                
                photoView.setImageBitmap(bitmapActual)
                println("✅ Foto original cargada: ${bitmapOriginal?.width}x${bitmapOriginal?.height}")
                println("Estado inicial - Rotación: $rotacionGuardada, Recorte: $recorteAplicado")
            } else {
                println("❌ Archivo no encontrado: $ruta")
            }
        }
        println("=== FIN CARGA FOTO ORIGINAL ===")
    }
    
    private fun inicializarCapas() {
        println("=== INICIALIZANDO CAPAS ===")
        editorCapas.limpiarCapas()
        
        // Capa de fondo (foto original)
        bitmapOriginal?.let { bitmap ->
            val capaFondo = CapaEditable(
                tipo = TipoCapa.FONDO,
                bitmap = bitmap,
                posicionX = 0f,
                posicionY = 0f,
                escala = 1f,
                rotacion = 0f
            )
            editorCapas.añadirCapa(capaFondo)
            println("✅ Capa fondo añadida")
        }
        
        // Capa de logo (se añadirá cuando se cargue el logo)
        logoEmpresa?.let { logo ->
            val capaLogo = CapaEditable(
                tipo = TipoCapa.LOGO,
                bitmap = logo,
                posicionX = 0f,
                posicionY = 0f,
                escala = 1f,
                rotacion = 0f
            )
            editorCapas.añadirCapa(capaLogo)
            println("✅ Capa logo añadida")
        }
        
        // Capa de texto (se añadirá cuando se añada texto)
        if (textoNota.isNotEmpty()) {
            val capaTexto = CapaEditable(
                tipo = TipoCapa.TEXTO,
                texto = textoNota,
                posicionX = 50f,
                posicionY = 100f,
                escala = 1f,
                rotacion = 0f
            )
            editorCapas.añadirCapa(capaTexto)
            println("✅ Capa texto añadida")
        }
        
        println("=== FIN INICIALIZACIÓN CAPAS ===")
    }
    
    private fun verificarPermisosYCargarLogo() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cargarLogoEmpresa()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    private fun cargarLogoEmpresa() {
        println("=== INICIANDO CARGA DE LOGO ===")
        println("Contexto disponible: ${requireContext() != null}")
        println("Assets disponibles: ${requireContext().assets.list("")?.joinToString(", ")}")
        
        try {
            // Intentar cargar logo desde assets
            println("Intentando cargar LogoBithermSWW.png...")
            val inputStream = requireContext().assets.open("LogoBithermSWW.png")
            println("InputStream creado exitosamente")
            logoEmpresa = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (logoEmpresa != null) {
                println("✅ LogoBithermSWW.png cargado exitosamente: ${logoEmpresa?.width}x${logoEmpresa?.height}")
            } else {
                println("❌ LogoBithermSWW.png se abrió pero decodeStream devolvió null")
                throw Exception("decodeStream devolvió null")
            }
        } catch (e: Exception) {
            println("❌ Error cargando LogoBithermSWW.png: ${e.message}")
            e.printStackTrace()
            try {
                // Intentar con favicon-negro.png
                println("Intentando cargar favicon-negro.png...")
                val inputStream = requireContext().assets.open("favicon-negro.png")
                println("InputStream favicon-negro creado exitosamente")
                logoEmpresa = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
                if (logoEmpresa != null) {
                    println("✅ favicon-negro.png cargado exitosamente: ${logoEmpresa?.width}x${logoEmpresa?.height}")
                } else {
                    println("❌ favicon-negro.png se abrió pero decodeStream devolvió null")
                    throw Exception("decodeStream devolvió null")
                }
            } catch (e2: Exception) {
                println("❌ Error cargando favicon-negro.png: ${e2.message}")
                e2.printStackTrace()
                try {
                    // Intentar con logo_empresa.png
                    println("Intentando cargar logo_empresa.png...")
                    val inputStream = requireContext().assets.open("logo_empresa.png")
                    println("InputStream logo_empresa creado exitosamente")
                    logoEmpresa = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (logoEmpresa != null) {
                        println("✅ logo_empresa.png cargado exitosamente: ${logoEmpresa?.width}x${logoEmpresa?.height}")
                    } else {
                        println("❌ logo_empresa.png se abrió pero decodeStream devolvió null")
                        throw Exception("decodeStream devolvió null")
                    }
                } catch (e3: Exception) {
                    println("❌ Error cargando logo_empresa.png: ${e3.message}")
                    e3.printStackTrace()
                    // Si no existe ningún logo, crear uno básico
                    println("Creando logo básico...")
                    logoEmpresa = crearLogoBasico()
                    println("✅ Logo básico creado: ${logoEmpresa?.width}x${logoEmpresa?.height}")
                }
            }
        }
        println("=== FIN CARGA DE LOGO ===")
        
        // Actualizar capas si ya se ha cargado la foto
        if (bitmapOriginal != null) {
            println("Actualizando capas con logo cargado")
            actualizarCapaLogo()
        }
    }
    
    private fun actualizarCapaLogo() {
        logoEmpresa?.let { logo ->
            // Verificar si ya existe la capa de logo
            val capaExistente = editorCapas.obtenerCapa(TipoCapa.LOGO)
            if (capaExistente == null) {
                // Crear nueva capa de logo
                val capaLogo = CapaEditable(
                    tipo = TipoCapa.LOGO,
                    bitmap = logo,
                    posicionX = 0f,
                    posicionY = 0f,
                    escala = 1f,
                    rotacion = 0f
                )
                editorCapas.añadirCapa(capaLogo)
                println("✅ Nueva capa logo añadida")
            } else {
                // Actualizar capa existente
                editorCapas.actualizarCapa(TipoCapa.LOGO) { capa ->
                    capa.copy(bitmap = logo)
                }
                println("✅ Capa logo actualizada")
            }
        }
    }
    
    private fun actualizarCapaTexto() {
        // Verificar si ya existe la capa de texto
        val capaExistente = editorCapas.obtenerCapa(TipoCapa.TEXTO)
        if (capaExistente == null && textoNota.isNotEmpty()) {
            // Crear nueva capa de texto
            val capaTexto = CapaEditable(
                tipo = TipoCapa.TEXTO,
                texto = textoNota,
                posicionX = 50f,
                posicionY = 100f,
                escala = 1f,
                rotacion = 0f
            )
            editorCapas.añadirCapa(capaTexto)
            println("✅ Nueva capa texto añadida: $textoNota")
        } else if (capaExistente != null) {
            // Actualizar capa existente
            editorCapas.actualizarCapa(TipoCapa.TEXTO) { capa ->
                capa.copy(texto = textoNota)
            }
            println("✅ Capa texto actualizada: $textoNota")
        }
    }
    
    private fun crearLogoBasico(): Bitmap {
        println("=== CREANDO LOGO BÁSICO ===")
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fondo azul
        val paintFondo = Paint().apply {
            setColor(Color.BLUE)
            setStyle(Paint.Style.FILL)
        }
        canvas.drawRect(0f, 0f, 200f, 200f, paintFondo)
        println("Fondo azul dibujado")
        
        // Texto "BITHERM"
        val paintTexto = Paint().apply {
            setAntiAlias(true)
            setColor(Color.WHITE)
            setTextSize(40f)
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextAlign(Paint.Align.CENTER)
        }
        
        canvas.drawText("BITHERM", 100f, 120f, paintTexto)
        println("Texto BITHERM dibujado")
        println("✅ Logo básico creado: 200x200")
        println("=== FIN CREACIÓN LOGO BÁSICO ===")
        
        return bitmap
    }
    
    private fun configurarListeners() {
        btnGuardar.setOnClickListener {
            guardarFotoEditada()
        }
        
        btnCancelar.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        btnFlecha.setOnClickListener {
            println("=== BOTÓN FLECHA CLICKEADO ===")
            println("Modo dibujo anterior: $modoDibujo")
            modoDibujo = !modoDibujo
            println("Modo dibujo nuevo: $modoDibujo")
            
            btnFlecha.setBackgroundResource(
                if (modoDibujo) R.drawable.bg_button_active 
                else R.drawable.bg_button_rounded
            )
            // Cambiar icono para indicar estado
            btnFlecha.setImageResource(
                if (modoDibujo) R.drawable.ic_delete 
                else R.drawable.ic_edit
            )
            // Sincronizar con el CustomPhotoView
            (photoView as? CustomPhotoView)?.setModoDibujo(modoDibujo)
            println("Modo dibujo sincronizado con CustomPhotoView")
        }
        
        btnTexto.setOnClickListener {
            mostrarPopupNota()
        }
        
        btnRecortar.setOnClickListener {
            recortarImagen()
        }
        
        btnGirar.setOnClickListener {
            girarImagen()
        }
        
        // Configurar dibujo de flechas con PhotoView personalizado
        configurarDibujoFlechas()
    }
    
    private fun mostrarPopupNota() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_nota)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        val etNota = dialog.findViewById<EditText>(R.id.etNota)
        val btnAceptar = dialog.findViewById<Button>(R.id.btnAceptar)
        val btnCancelar = dialog.findViewById<Button>(R.id.btnCancelar)
        
        // Prellenar con texto existente
        etNota.setText(textoNota)
        
        btnAceptar.setOnClickListener {
            textoNota = etNota.text.toString()
            dialog.dismiss()
            
            // Actualizar capa de texto
            actualizarCapaTexto()
            
            aplicarMarcaDeAgua()
        }
        
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun configurarDibujoFlechas() {
        println("=== CONFIGURANDO DIBUJO DE FLECHAS ===")
        // Crear un PhotoView personalizado que permita dibujar flechas
        val customPhotoView = CustomPhotoView(requireContext())
        customPhotoView.apply {
            maximumScale = 5f
            minimumScale = 0.5f
            mediumScale = 2f
            setImageBitmap(bitmapActual)
        }
        println("CustomPhotoView creado con escala: min=0.5, max=5.0, medium=2.0")
        
        // Reemplazar el PhotoView original
        val parent = photoView.parent as ViewGroup
        val index = parent.indexOfChild(photoView)
        parent.removeView(photoView)
        parent.addView(customPhotoView, index)
        photoView = customPhotoView
        println("PhotoView reemplazado por CustomPhotoView")
        
        // Configurar callbacks para el dibujo
        customPhotoView.setOnFlechaDibujadaListener { flecha ->
            println("Flecha dibujada recibida: (${flecha.startX}, ${flecha.startY}) -> (${flecha.endX}, ${flecha.endY})")
            flechas.add(flecha)
        }
        
        customPhotoView.setOnModoDibujoChangedListener { modo ->
            println("Modo dibujo cambiado a: $modo")
            modoDibujo = modo
        }
        println("=== FIN CONFIGURACIÓN DIBUJO FLECHAS ===")
    }
    
    private fun recortarImagen() {
        println("=== RECORTANDO IMAGEN ===")
        
        // Determinar qué bitmap usar basado en el estado
        val bitmapParaRecortar = if (recorteAplicado) {
            bitmapRecortado ?: bitmapOriginal
        } else {
            bitmapOriginal
        }
        
        bitmapParaRecortar?.let { bitmap ->
            println("Usando bitmap para recorte: ${bitmap.width}x${bitmap.height}")
            println("Estado actual - Rotación: $rotacionGuardada, Recorte aplicado: $recorteAplicado")
            
            // Aplicar rotación guardada si existe
            val bitmapConRotacion = if (rotacionGuardada != 0f) {
                println("Aplicando rotación guardada: $rotacionGuardada")
                aplicarRotacion(bitmap, rotacionGuardada)
            } else {
                bitmap
            }
            
            // Redimensionar a 1:1
            val bitmap1x1 = redimensionarA1x1(bitmapConRotacion)
            bitmapActual = bitmap1x1
            bitmapRecortado = bitmap1x1
            recorteAplicado = true
            
            photoView.setImageBitmap(bitmap1x1)
            println("✅ Imagen recortada y redimensionada a 1:1 manteniendo transformaciones")
            Toast.makeText(requireContext(), "Imagen recortada y ajustada a formato 1:1", Toast.LENGTH_SHORT).show()
        } ?: run {
            println("❌ No hay bitmap para recortar")
            Toast.makeText(requireContext(), "Error: No hay imagen para recortar", Toast.LENGTH_SHORT).show()
        }
        println("=== FIN RECORTE ===")
    }
    
    private fun aplicarRotacion(bitmap: Bitmap, rotacion: Float): Bitmap {
        println("Aplicando rotación: $rotacion")
        
        // Calcular nuevas dimensiones
        val esVertical = rotacion == 90f || rotacion == 270f
        val nuevoAncho = if (esVertical) bitmap.height else bitmap.width
        val nuevoAlto = if (esVertical) bitmap.width else bitmap.height
        
        // Crear bitmap con las nuevas dimensiones
        val bitmapRotado = Bitmap.createBitmap(nuevoAncho, nuevoAlto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapRotado)
        
        // Configurar matriz de transformación
        val matrix = Matrix().apply {
            // Centrar la imagen
            postTranslate((nuevoAncho - bitmap.width) / 2f, (nuevoAlto - bitmap.height) / 2f)
            // Aplicar rotación desde el centro
            postRotate(rotacion, bitmap.width / 2f, bitmap.height / 2f)
        }
        
        canvas.drawBitmap(bitmap, matrix, null)
        println("✅ Rotación aplicada: ${bitmapRotado.width}x${bitmapRotado.height}")
        return bitmapRotado
    }
    
    private fun obtenerBitmapDeVista(view: View): Bitmap? {
        println("=== OBTENIENDO BITMAP DE VISTA ===")
        println("Dimensiones de la vista: ${view.width}x${view.height}")
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            println("✅ Bitmap obtenido de vista: ${bitmap.width}x${bitmap.height}")
            println("=== FIN OBTENCIÓN BITMAP ===")
            bitmap
        } catch (e: Exception) {
            println("❌ Error obteniendo bitmap de vista: ${e.message}")
            e.printStackTrace()
            println("=== FIN OBTENCIÓN BITMAP (ERROR) ===")
            null
        }
    }
    
    private fun girarImagen() {
        println("=== GIRANDO IMAGEN ===")
        bitmapActual?.let { bitmap ->
            println("Bitmap actual antes de girar: ${bitmap.width}x${bitmap.height}")
            rotacionActual += 90f
            if (rotacionActual >= 360f) rotacionActual = 0f
            println("Rotación aplicada: ${rotacionActual}°")
            
            // Guardar el estado de rotación
            rotacionGuardada = rotacionActual
            println("Rotación guardada: $rotacionGuardada")
            
            // Calcular nuevas dimensiones para mantener la imagen completa
            val esVertical = rotacionActual == 90f || rotacionActual == 270f
            val nuevoAncho = if (esVertical) bitmap.height else bitmap.width
            val nuevoAlto = if (esVertical) bitmap.width else bitmap.height
            
            println("Nuevas dimensiones calculadas: ${nuevoAncho}x${nuevoAlto}")
            
            // Crear bitmap con las nuevas dimensiones
            val bitmapRotado = Bitmap.createBitmap(nuevoAncho, nuevoAlto, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmapRotado)
            
            // Configurar matriz de transformación
            val matrix = Matrix().apply {
                // Centrar la imagen
                postTranslate((nuevoAncho - bitmap.width) / 2f, (nuevoAlto - bitmap.height) / 2f)
                // Aplicar rotación desde el centro
                postRotate(rotacionActual, bitmap.width / 2f, bitmap.height / 2f)
            }
            
            canvas.drawBitmap(bitmap, matrix, null)
            bitmapActual = bitmapRotado
            photoView.setImageBitmap(bitmapRotado)
            
            println("✅ Imagen rotada exitosamente: ${bitmapRotado.width}x${bitmapRotado.height}")
            Toast.makeText(requireContext(), "Imagen rotada ${rotacionActual.toInt()}°", Toast.LENGTH_SHORT).show()
        } ?: run {
            println("❌ No hay bitmap actual para girar")
            Toast.makeText(requireContext(), "Error: No hay imagen para girar", Toast.LENGTH_SHORT).show()
        }
        println("=== FIN GIRO IMAGEN ===")
    }
    
    private fun aplicarMarcaDeAgua() {
        println("=== APLICANDO MARCA DE AGUA ===")
        println("Estado actual - Rotación: $rotacionGuardada, Recorte: $recorteAplicado")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("Iniciando creación de marca de agua en background")
                val bitmapConMarca = crearMarcaDeAgua(bitmapActual ?: return@launch)
                println("Marca de agua creada exitosamente")
                
                withContext(Dispatchers.Main) {
                    println("Actualizando UI con marca de agua")
                    photoView.setImageBitmap(bitmapConMarca)
                    println("✅ Marca de agua aplicada exitosamente")
                }
            } catch (e: Exception) {
                println("❌ Error al aplicar marca de agua: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al aplicar marca de agua: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        println("=== FIN APLICACIÓN MARCA DE AGUA ===")
    }
    
    private fun crearMarcaDeAgua(bitmapOriginal: Bitmap): Bitmap {
        println("=== INICIANDO CREACIÓN MARCA DE AGUA ORIGINAL ===")
        println("Bitmap original: ${bitmapOriginal.width}x${bitmapOriginal.height}")
        
        // Mantener las dimensiones originales de la imagen
        val bitmapResultado = bitmapOriginal.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmapResultado)
        println("Canvas creado para bitmap resultado")
        
        // Calcular tamaños proporcionales a la imagen
        val imageWidth = bitmapOriginal.width
        val imageHeight = bitmapOriginal.height
        val padding = imageWidth * 0.02f // 2% del ancho
        val logoSize = imageWidth * 0.08f // 8% del ancho
        val textSize = imageWidth * 0.025f // 2.5% del ancho
        val lineHeight = textSize * 1.3f
        
        println("Dimensiones calculadas: padding=$padding, logoSize=$logoSize, textSize=$textSize")
        
        // Calcular dimensiones del área de información
        val infoWidth = imageWidth * 0.5f // 50% del ancho
        println("Info width: $infoWidth")
        
        // Calcular contenido para determinar altura real del cuadro
        val contenido = mutableListOf<String>()
        
        // TAG del equipo (sin etiqueta "TAG:")
        val tagEquipo = equipoId
        if (!tagEquipo.isNullOrEmpty()) {
            contenido.add(tagEquipo)
        }
        
        // Coordenadas + precisión + altitud en una línea
        val coordenadasCompletas = StringBuilder().apply {
            if (!coordenadas.isNullOrEmpty()) append(coordenadas)
            if (!precision.isNullOrEmpty()) append(" ±${precision}m")
            if (!altitud.isNullOrEmpty()) append(" (${altitud}m)")
        }.toString()
        if (coordenadasCompletas.isNotEmpty()) {
            contenido.add(coordenadasCompletas)
        }
        
        // Texto personalizado (solo si hay texto)
        if (textoNota.isNotEmpty()) {
            val paintTemp = Paint().apply {
                setTextSize(textSize)
            }
            val lines = dividirTextoEnLineas(textoNota, paintTemp, infoWidth - 40f) // 40f de padding
            contenido.addAll(lines.take(2)) // Máximo 2 líneas
        }
        
        // Calcular altura real del cuadro basada solo en el contenido real
        val alturaReal = if (contenido.isNotEmpty()) {
            logoSize + (lineHeight * contenido.size) + (padding * 2)
        } else {
            logoSize + (padding * 2)
        }
        
        // Crear fondo semiopaco para la información (solo una vez, sin sombra)
        val paintFondo = Paint().apply {
            setColor(Color.parseColor("#80000000")) // Negro semiopaco
            setStyle(Paint.Style.FILL)
        }
        
        // Dibujar fondo semiopaco (solo una vez, sin sombra)
        val rectFondo = RectF(padding, padding, padding + infoWidth, padding + alturaReal)
        canvas.drawRoundRect(rectFondo, padding, padding, paintFondo)
        
        // Configurar paint para texto
        val paintTexto = Paint().apply {
            setAntiAlias(true)
            setColor(Color.WHITE)
            setTextSize(textSize)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        
        // Configurar paint para TAG (300% más grande - 150% adicional al 150% original)
        val paintTag = Paint().apply {
            setAntiAlias(true)
            setColor(Color.WHITE)
            setTextSize(textSize * 3.0f) // 300% del tamaño normal (150% + 150%)
            setTypeface(Typeface.DEFAULT_BOLD)
        }
        
        // Dibujar logo en la esquina derecha (70% del ancho del cuadro de texto)
        println("=== INICIANDO DIBUJO DE LOGO ===")
        println("Logo disponible: ${logoEmpresa != null}")
        logoEmpresa?.let { logo ->
            println("Logo encontrado: ${logo.width}x${logo.height}")
            val logoWidth = infoWidth * 0.7f // 70% del ancho del cuadro
            val logoHeight = logoWidth * (logo.height.toFloat() / logo.width.toFloat()) // Proporcional
            val logoX = imageWidth - logoWidth - padding
            val logoY = padding
            val logoRect = RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight)
            println("Dibujando logo en rectángulo: $logoRect")
            canvas.drawBitmap(logo, null, logoRect, null)
            println("✅ Logo dibujado exitosamente en esquina superior derecha: x=$logoX, y=$logoY, w=$logoWidth, h=$logoHeight")
        } ?: run {
            println("❌ No hay logo disponible para dibujar")
            // Crear un logo de prueba rojo para verificar que se dibuja algo
            println("Creando logo de prueba rojo...")
            val logoPrueba = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val canvasLogo = Canvas(logoPrueba)
            val paintRojo = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
            }
            canvasLogo.drawRect(0f, 0f, 100f, 100f, paintRojo)
            
            val logoWidth = infoWidth * 0.7f
            val logoHeight = logoWidth
            val logoX = imageWidth - logoWidth - padding
            val logoY = padding
            val logoRect = RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight)
            println("Dibujando logo de prueba rojo en: $logoRect")
            canvas.drawBitmap(logoPrueba, null, logoRect, null)
            println("✅ Logo de prueba rojo dibujado")
        }
        println("=== FIN DIBUJO DE LOGO ===")
        
        // Dibujar información con alineación vertical correcta
        if (contenido.isNotEmpty()) {
            val contenidoHeight = lineHeight * contenido.size
            val startY = padding + (alturaReal - contenidoHeight) / 2 + lineHeight / 2
            
            contenido.forEachIndexed { index, texto ->
                val currentY = startY + (index * lineHeight)
                val paint = if (index == 0) paintTag else paintTexto
                canvas.drawText(texto, padding + 20f, currentY, paint) // 20f de padding interno
            }
        }
        
        // Dibujar flechas
        dibujarFlechas(canvas, bitmapOriginal.width, bitmapOriginal.height)
        
        println("✅ Marca de agua creada exitosamente")
        println("=== FIN CREACIÓN MARCA DE AGUA ORIGINAL ===")
        
        return bitmapResultado
    }
    
    private fun dividirTextoEnLineas(texto: String, paint: Paint, maxWidth: Float): List<String> {
        val palabras = texto.split(" ")
        val lineas = mutableListOf<String>()
        var lineaActual = ""
        
        palabras.forEach { palabra ->
            val textoPrueba = if (lineaActual.isEmpty()) palabra else "$lineaActual $palabra"
            val bounds = Rect()
            paint.getTextBounds(textoPrueba, 0, textoPrueba.length, bounds)
            
            if (bounds.width() <= maxWidth) {
                lineaActual = textoPrueba
            } else {
                if (lineaActual.isNotEmpty()) {
                    lineas.add(lineaActual)
                }
                lineaActual = palabra
            }
        }
        
        if (lineaActual.isNotEmpty()) {
            lineas.add(lineaActual)
        }
        
        return lineas
    }
    
    private fun dibujarFlechas(canvas: Canvas, imageWidth: Int, imageHeight: Int) {
        val paintFlecha = Paint().apply {
            setAntiAlias(true)
            setColor(Color.RED)
            setStrokeWidth(imageWidth * 0.01f) // 1% del ancho
            setStyle(Paint.Style.STROKE)
        }
        
        val paintPunta = Paint().apply {
            setAntiAlias(true)
            setColor(Color.RED)
            setStyle(Paint.Style.FILL)
        }
        
        // Dibujar flechas guardadas
        flechas.forEach { flecha ->
            dibujarFlecha(canvas, flecha, paintFlecha, paintPunta, imageWidth, imageHeight)
        }
        
        // Dibujar flecha actual si existe
        flechaActual?.let { flecha ->
            dibujarFlecha(canvas, flecha, paintFlecha, paintPunta, imageWidth, imageHeight)
        }
    }
    
    private fun dibujarFlecha(canvas: Canvas, flecha: Flecha, paintFlecha: Paint, paintPunta: Paint, imageWidth: Int, imageHeight: Int) {
        // Convertir coordenadas de pantalla a coordenadas de imagen
        val scaleX = imageWidth.toFloat() / photoView.width
        val scaleY = imageHeight.toFloat() / photoView.height
        
        val startX = flecha.startX * scaleX
        val startY = flecha.startY * scaleY
        val endX = flecha.endX * scaleX
        val endY = flecha.endY * scaleY
        
        // Línea principal
        canvas.drawLine(startX, startY, endX, endY, paintFlecha)
        
        // Calcular punta de flecha
        val angle = Math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())
        val arrowLength = imageWidth * 0.02f // 2% del ancho
        val arrowAngle = Math.PI / 6 // 30 grados
        
        val x1 = endX - (arrowLength * Math.cos(angle - arrowAngle)).toFloat()
        val y1 = endY - (arrowLength * Math.sin(angle - arrowAngle)).toFloat()
        val x2 = endX - (arrowLength * Math.cos(angle + arrowAngle)).toFloat()
        val y2 = endY - (arrowLength * Math.sin(angle + arrowAngle)).toFloat()
        
        // Dibujar punta de flecha
        val path = Path()
        path.moveTo(endX, endY)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.close()
        canvas.drawPath(path, paintPunta)
    }
    
    private fun guardarFotoEditada() {
        println("=== GUARDANDO FOTO EDITADA ===")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("Iniciando procesamiento de foto para guardar")
                val bitmapConMarca = crearMarcaDeAgua(bitmapActual ?: return@launch)
                println("Marca de agua aplicada, redimensionando a 1:1...")
                
                // Redimensionar a aspecto 1:1
                val bitmap1x1 = redimensionarA1x1(bitmapConMarca)
                println("Imagen redimensionada a 1:1: ${bitmap1x1.width}x${bitmap1x1.height}")
                
                // Guardar foto editada
                val fotoEditada = guardarBitmap(bitmap1x1)
                println("Foto guardada en: ${fotoEditada.absolutePath}")
                
                withContext(Dispatchers.Main) {
                    // Notificar al fragmento padre
                    val bundle = Bundle().apply {
                        putString("ruta_foto_editada", fotoEditada.absolutePath)
                        putString("ruta_foto_original", rutaFotoOriginal)
                        putBoolean("foto_actualizada", true)
                    }
                    parentFragmentManager.setFragmentResult("foto_editada", bundle)
                    
                    Toast.makeText(requireContext(), "Foto guardada exitosamente en formato 1:1", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                println("❌ Error al guardar: ${e.message}")
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        println("=== FIN GUARDADO FOTO EDITADA ===")
    }
    
    private fun guardarBitmap(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "edited_${equipoId}_$timeStamp.jpg"
        
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

    private fun redimensionarA1x1(bitmap: Bitmap): Bitmap {
        println("=== REDIMENSIONANDO A 1:1 ===")
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        println("Dimensiones originales: ${originalWidth}x${originalHeight}")
        
        // Calcular nuevas dimensiones para aspecto 1:1 (cuadrado)
        val targetWidth: Int
        val targetHeight: Int
        
        val aspectRatio = 1f // 1:1
        val currentAspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        if (currentAspectRatio > aspectRatio) {
            // La imagen es más ancha que 1:1, ajustar altura
            targetWidth = originalWidth
            targetHeight = originalWidth // Cuadrado
            println("Imagen más ancha que 1:1, ajustando altura: ${targetWidth}x${targetHeight}")
        } else {
            // La imagen es más alta que 1:1, ajustar ancho
            targetHeight = originalHeight
            targetWidth = originalHeight // Cuadrado
            println("Imagen más alta que 1:1, ajustando ancho: ${targetWidth}x${targetHeight}")
        }
        
        // Crear un nuevo bitmap con el tamaño objetivo
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        println("✅ Imagen redimensionada a 1:1: ${scaledBitmap.width}x${scaledBitmap.height}")
        println("=== FIN REDIMENSIONAMIENTO ===")
        
        return scaledBitmap
    }
    
    companion object {
        fun newInstance(
            rutaFoto: String,
            equipoId: String,
            coordenadas: String?,
            precision: String?,
            altitud: String?
        ): EditorFotosFragment {
            return EditorFotosFragment().apply {
                arguments = Bundle().apply {
                    putString("ruta_foto", rutaFoto)
                    putString("equipo_id", equipoId)
                    putString("coordenadas", coordenadas)
                    putString("precision", precision)
                    putString("altitud", altitud)
                }
            }
        }
    }
}

// Clase para manejar capas editables
data class CapaEditable(
    val tipo: TipoCapa,
    val bitmap: Bitmap? = null,
    val texto: String? = null,
    val posicionX: Float = 0f,
    val posicionY: Float = 0f,
    val escala: Float = 1f,
    val rotacion: Float = 0f,
    val visible: Boolean = true
)

enum class TipoCapa {
    FONDO,      // Foto original
    LOGO,       // Logo de empresa
    TEXTO,      // Marca de agua/texto
    FLECHA      // Flechas dibujadas
}

// Clase para manejar el editor de capas
class EditorCapas {
    private val capas = mutableListOf<CapaEditable>()
    
    fun añadirCapa(capa: CapaEditable) {
        capas.add(capa)
    }
    
    fun obtenerCapa(tipo: TipoCapa): CapaEditable? {
        return capas.find { it.tipo == tipo }
    }
    
    fun actualizarCapa(tipo: TipoCapa, actualizacion: (CapaEditable) -> CapaEditable) {
        val index = capas.indexOfFirst { it.tipo == tipo }
        if (index != -1) {
            capas[index] = actualizacion(capas[index])
        }
    }
    
    fun obtenerTodasLasCapas(): List<CapaEditable> = capas.toList()
    
    fun limpiarCapas() {
        capas.clear()
    }
    
    fun renderizarCapas(ancho: Int, alto: Int): Bitmap {
        val bitmapResultado = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapResultado)
        
        // Renderizar capas en orden (fondo primero, elementos encima)
        capas.filter { it.visible }.forEach { capa ->
            when (capa.tipo) {
                TipoCapa.FONDO -> {
                    capa.bitmap?.let { bitmap ->
                        val matrix = Matrix().apply {
                            postScale(capa.escala, capa.escala)
                            postRotate(capa.rotacion)
                            postTranslate(capa.posicionX, capa.posicionY)
                        }
                        canvas.drawBitmap(bitmap, matrix, null)
                    }
                }
                TipoCapa.LOGO -> {
                    capa.bitmap?.let { bitmap ->
                        val matrix = Matrix().apply {
                            postScale(capa.escala, capa.escala)
                            postRotate(capa.rotacion)
                            postTranslate(capa.posicionX, capa.posicionY)
                        }
                        canvas.drawBitmap(bitmap, matrix, null)
                    }
                }
                TipoCapa.TEXTO -> {
                    // Renderizar texto
                    capa.texto?.let { texto ->
                        val paint = Paint().apply {
                            color = Color.WHITE
                            textSize = 40f
                            isAntiAlias = true
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        canvas.drawText(texto, capa.posicionX, capa.posicionY, paint)
                    }
                }
                TipoCapa.FLECHA -> {
                    // Las flechas se renderizan por separado
                }
            }
        }
        
        return bitmapResultado
    }
} 
 
 