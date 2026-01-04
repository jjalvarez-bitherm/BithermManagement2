package com.bithermmanagement.multimedia

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.github.chrisbanes.photoview.PhotoView

class ElementosEditablesOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var elementosEditables = mutableListOf<EditorWhatsAppStyleFragment.ElementoEditable>()
    private var photoView: PhotoView? = null
    private var layerBaseBitmap: Bitmap? = null  // Layer base 1920x1920px como fondo
    private var mostrarMarcoPreview: Boolean = false  // Mostrar preview del marco 1:1
    private var pathPreview: Path? = null
    private var paintPreview: Paint = Paint().apply {
        color = Color.RED
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val layerBasePaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    // Propiedades públicas para conversión de coordenadas
    var layerBaseEscala: Float = 1f
        private set
    var layerBaseOffsetX: Float = 0f
        private set
    var layerBaseOffsetY: Float = 0f
        private set
    
    fun setPhotoView(photoView: PhotoView) {
        this.photoView = photoView
    }
    
    /**
     * Establece el bitmap del layer base (1920x1920px) como fondo.
     * Este bitmap se escala para encajar en la pantalla manteniendo aspect ratio.
     */
    fun setLayerBase(bitmap: Bitmap) {
        this.layerBaseBitmap = bitmap
        invalidate()
    }
    
    /**
     * Activa/desactiva el preview del marco cuadrado 1:1.
     * Usado en fase AJUSTANDO_FOTO para mostrar el área que se capturará.
     */
    fun setMostrarMarcoPreview(mostrar: Boolean) {
        this.mostrarMarcoPreview = mostrar
        invalidate()
    }
    
    fun setElementosEditables(elementos: MutableList<EditorWhatsAppStyleFragment.ElementoEditable>) {
        this.elementosEditables = elementos
        invalidate()
    }
    
    fun setPathPreview(path: Path?) {
        this.pathPreview = path
        invalidate()
    }
    
    fun setPaintPreview(paint: Paint) {
        this.paintPreview = paint
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Si estamos en modo preview del marco, dibujar fondo opaco + marco cuadrado FIJO
        if (mostrarMarcoPreview) {
            // MARCO FIJO: tamaño basado en las dimensiones de la VISTA, no del displayRect
            // El marco NO debe escalarse con el zoom - permanece fijo en pantalla
            val marcoSize = minOf(width, height).toFloat()
            val marcoLeft = (width - marcoSize) / 2f
            val marcoTop = (height - marcoSize) / 2f
            
            // Dibujar fondo opaco fuera del marco
            val paintOscuro = Paint().apply {
                color = Color.argb(180, 0, 0, 0)  // Negro semitransparente
                style = Paint.Style.FILL
            }
            
            // Rectángulos opacos alrededor del marco
            // Arriba
            canvas.drawRect(0f, 0f, width.toFloat(), marcoTop, paintOscuro)
            // Abajo
            canvas.drawRect(0f, marcoTop + marcoSize, width.toFloat(), height.toFloat(), paintOscuro)
            // Izquierda
            canvas.drawRect(0f, marcoTop, marcoLeft, marcoTop + marcoSize, paintOscuro)
            // Derecha
            canvas.drawRect(marcoLeft + marcoSize, marcoTop, width.toFloat(), marcoTop + marcoSize, paintOscuro)
            
            // Dibujar borde del marco cuadrado
            val paintMarco = Paint().apply {
                color = Color.WHITE
                strokeWidth = 4f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawRect(marcoLeft, marcoTop, marcoLeft + marcoSize, marcoTop + marcoSize, paintMarco)
            
            return  // No dibujar nada más en modo preview
        }
        
        // Dibujar layer base como fondo (si está disponible)
        layerBaseBitmap?.let { bitmap ->
            // Escalar layer base (1920x1920px) para encajar en pantalla manteniendo aspect ratio
            layerBaseEscala = minOf(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
            val scaledWidth = bitmap.width * layerBaseEscala
            val scaledHeight = bitmap.height * layerBaseEscala
            
            // Centrar en pantalla
            layerBaseOffsetX = (width - scaledWidth) / 2f
            layerBaseOffsetY = (height - scaledHeight) / 2f
            
            val destRect = RectF(layerBaseOffsetX, layerBaseOffsetY, layerBaseOffsetX + scaledWidth, layerBaseOffsetY + scaledHeight)
            canvas.drawBitmap(bitmap, null, destRect, layerBasePaint)
        }
        
        // Dibujar preview del path (dibujo en tiempo real)
        pathPreview?.let { path ->
            canvas.drawPath(path, paintPreview)
        }
        
        // Dibujar cada elemento editable
        elementosEditables.forEach { elemento ->
            dibujarElemento(canvas, elemento)
        }
    }
    
    private fun dibujarElemento(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        // Guardar el estado actual del canvas
        canvas.save()
        
        // Convertir coordenadas si es STAMP o LOGO (están en espacio LayerBase)
        val (elementoX, elementoY, elementoWidth, elementoHeight) = if (elemento.tipo == EditorWhatsAppStyleFragment.TipoElemento.STAMP || 
                                                                         elemento.tipo == EditorWhatsAppStyleFragment.TipoElemento.LOGO) {
            // Convertir de coordenadas LayerBase a coordenadas de pantalla
            val xPantalla = elemento.x * layerBaseEscala + layerBaseOffsetX
            val yPantalla = elemento.y * layerBaseEscala + layerBaseOffsetY
            val widthPantalla = elemento.width * layerBaseEscala
            val heightPantalla = elemento.height * layerBaseEscala
            arrayOf(xPantalla, yPantalla, widthPantalla, heightPantalla)
        } else {
            // Ya están en coordenadas de pantalla
            arrayOf(elemento.x, elemento.y, elemento.width, elemento.height)
        }
        
        // Aplicar transformaciones al elemento
        canvas.translate(elementoX, elementoY)
        canvas.rotate(elemento.rotacion)
        canvas.scale(elemento.escala, elemento.escala)
        canvas.translate(-elementoX, -elementoY)
        
        // Crear una copia temporal del elemento con las coordenadas convertidas
        val elementoAjustado = if (elemento.tipo == EditorWhatsAppStyleFragment.TipoElemento.STAMP || 
                                    elemento.tipo == EditorWhatsAppStyleFragment.TipoElemento.LOGO) {
            elemento.copy(x = elementoX, y = elementoY, width = elementoWidth, height = elementoHeight)
        } else {
            elemento
        }
        
        when (elementoAjustado.tipo) {
            EditorWhatsAppStyleFragment.TipoElemento.FLECHA -> dibujarFlecha(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.CIRCULO -> dibujarCirculo(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.CUADRADO -> dibujarCuadrado(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.TRIANGULO -> dibujarTriangulo(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.TEXTO -> dibujarTexto(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.DIBUJO -> dibujarDibujo(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.LINEA -> dibujarLinea(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.STAMP -> dibujarStamp(canvas, elementoAjustado)
            EditorWhatsAppStyleFragment.TipoElemento.LOGO -> dibujarLogo(canvas, elementoAjustado)
            else -> {}
        }
        
        // Restaurar el estado del canvas
        canvas.restore()
    }
    
    private fun dibujarFlecha(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        val paint = Paint().apply {
            color = elemento.color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Flecha SVG-style: triángulo a la derecha, línea a la izquierda
        // Tamaño 400% más grande (4x)
        val path = Path()
        
        val centerX = elemento.x
        val centerY = elemento.y
        val ancho = elemento.width * 4f  // 400% más grande
        val alto = elemento.height * 4f   // 400% más grande
        
        // Punta de la flecha (triángulo) - en el lado derecho
        val puntaLeft = centerX + ancho/2 - ancho * 0.25f
        val puntaRight = centerX + ancho/2
        val puntaTop = centerY - alto/2
        val puntaBottom = centerY + alto/2
        
        // Dibujar triángulo apuntando a la derecha
        path.moveTo(puntaRight, centerY)  // Punta
        path.lineTo(puntaLeft, puntaTop)   // Esquina superior
        path.lineTo(puntaLeft, puntaBottom) // Esquina inferior
        path.close()
        
        // Cuerpo de la flecha (línea rectangular) - en el lado izquierdo
        val cuerpoAncho = ancho * 0.75f
        val cuerpoAlto = alto * 0.15f  // Línea más fina
        val cuerpoLeft = centerX - ancho/2
        val cuerpoRight = puntaLeft
        val cuerpoTop = centerY - cuerpoAlto/2
        val cuerpoBottom = centerY + cuerpoAlto/2
        
        // Dibujar línea (cola)
        path.addRect(cuerpoLeft, cuerpoTop, cuerpoRight, cuerpoBottom, Path.Direction.CW)
        
        canvas.drawPath(path, paint)
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawPath(path, paintBorde)
        }
    }
    
    private fun dibujarCirculo(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        val paint = Paint().apply {
            color = elemento.color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        
        canvas.drawCircle(elemento.x, elemento.y, elemento.width/2, paint)
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawCircle(elemento.x, elemento.y, elemento.width/2, paintBorde)
        }
    }
    
    private fun dibujarCuadrado(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        val paint = Paint().apply {
            color = elemento.color
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        
        val rect = RectF(
            elemento.x - elemento.width/2,
            elemento.y - elemento.height/2,
            elemento.x + elemento.width/2,
            elemento.y + elemento.height/2
        )
        
        canvas.drawRect(rect, paint)
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawRect(rect, paintBorde)
        }
    }
    
    private fun dibujarTriangulo(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        val paint = Paint().apply {
            color = elemento.color
            style = Paint.Style.STROKE
            strokeWidth = elemento.strokeWidth
            isAntiAlias = true
        }
        
        val path = Path()
        
        // Triángulo equilátero con punta hacia arriba
        val centerX = elemento.x
        val centerY = elemento.y
        val width = elemento.width
        val height = elemento.height
        
        // Punto superior (punta)
        path.moveTo(centerX, centerY - height/2)
        
        // Punto inferior izquierdo
        path.lineTo(centerX - width/2, centerY + height/2)
        
        // Punto inferior derecho
        path.lineTo(centerX + width/2, centerY + height/2)
        
        // Cerrar el triángulo
        path.close()
        
        canvas.drawPath(path, paint)
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawPath(path, paintBorde)
        }
    }
    
    private fun dibujarDibujo(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        elemento.path?.let { path ->
            val paint = Paint().apply {
                color = elemento.color
                strokeWidth = elemento.strokeWidth
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                isAntiAlias = true
            }
            canvas.drawPath(path, paint)
        }
    }
    
    private fun dibujarLinea(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        elemento.path?.let { path ->
            val paint = Paint().apply {
                color = elemento.color
                strokeWidth = elemento.strokeWidth
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                isAntiAlias = true
            }
            canvas.drawPath(path, paint)
        }
    }
    
    private fun dibujarTexto(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        val paintTexto = Paint().apply {
            color = elemento.color
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }
        
        canvas.drawText(elemento.texto, elemento.x, elemento.y, paintTexto)
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val bounds = Rect()
            paintTexto.getTextBounds(elemento.texto, 0, elemento.texto.length, bounds)
            val rect = RectF(
                elemento.x - bounds.width()/2 - 10,
                elemento.y - bounds.height() - 10,
                elemento.x + bounds.width()/2 + 10,
                elemento.y + 10
            )
            canvas.drawRect(rect, paintBorde)
        }
    }
    
    private fun dibujarStamp(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        // Configurar paints para el stamp
        val paintFondo = Paint().apply {
            color = Color.parseColor("#80000000") // Negro semi-transparente
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Posición del stamp - alineado a esquina superior izquierda
        val stampWidth = elemento.width
        val stampHeight = elemento.height
        val stampX = elemento.x  // Sin centrar, directo desde la posición X
        val stampY = elemento.y  // Sin centrar, directo desde la posición Y
        
        // Dibujar fondo del stamp
        canvas.drawRect(stampX, stampY, stampX + stampWidth, stampY + stampHeight, paintFondo)
        
        // Calcular espacios para las 4 líneas de texto
        val margenSuperior = stampHeight * 0.05f  // 5% margen superior
        val margenInferior = stampHeight * 0.05f  // 5% margen inferior
        val alturaDisponible = stampHeight - margenSuperior - margenInferior
        
        // Línea 1: 25% del alto del stamp
        val alturaLinea1 = alturaDisponible * 0.25f
        // Líneas 2-4: 18% del alto del stamp cada una
        val alturaLinea2 = alturaDisponible * 0.18f
        val alturaLinea3 = alturaDisponible * 0.18f
        val alturaLinea4 = alturaDisponible * 0.18f
        
        // Separaciones pequeñas entre líneas (resto del espacio distribuido)
        val separacionTotal = alturaDisponible - (alturaLinea1 + alturaLinea2 + alturaLinea3 + alturaLinea4)
        val separacion = separacionTotal / 3f  // 3 separaciones entre 4 líneas
        
        // Configurar paints para cada línea
        val paintLinea1 = Paint().apply {
            color = Color.WHITE
            textSize = alturaLinea1 * 0.8f  // 80% del espacio para el texto
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        val paintLineas234 = Paint().apply {
            color = Color.WHITE
            textSize = alturaLinea2 * 0.8f  // 80% del espacio para el texto
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }
        
        // Calcular posiciones Y para cada línea (baseline del texto)
        val margenIzquierdo = stampWidth * 0.05f  // 5% margen izquierdo
        val y1 = stampY + margenSuperior + alturaLinea1 * 0.75f  // 75% de la altura para centrar el texto
        val y2 = y1 + alturaLinea1 * 0.25f + separacion + alturaLinea2 * 0.75f
        val y3 = y2 + alturaLinea2 * 0.25f + separacion + alturaLinea3 * 0.75f
        val y4 = y3 + alturaLinea3 * 0.25f + separacion + alturaLinea4 * 0.75f
        
        // Dibujar las líneas de texto dinámicamente
        // Línea 1: TAG del equipo (25% del alto)
        canvas.drawText(elemento.stampTag, stampX + margenIzquierdo, y1, paintLinea1)
        
        // Línea 2: Coordenadas y precisión (18% del alto)
        canvas.drawText(elemento.stampCoordenadas, stampX + margenIzquierdo, y2, paintLineas234)
        
        // Línea 3: Fecha (18% del alto)
        canvas.drawText(elemento.stampFecha, stampX + margenIzquierdo, y3, paintLineas234)
        
        // Línea 4: Comentario (18% del alto) - SOLO SI HAY COMENTARIO
        if (elemento.stampComentario.isNotBlank()) {
            canvas.drawText(elemento.stampComentario, stampX + margenIzquierdo, y4, paintLineas234)
        }
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            canvas.drawRect(stampX, stampY, stampX + stampWidth, stampY + stampHeight, paintBorde)
        }
    }
    
    private fun dibujarLogo(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        try {
            // Intentar cargar el logo real desde assets
            val inputStream = context.assets.open("LogoBithermSWW.png")
            val logoBitmap = BitmapFactory.decodeStream(inputStream)
            
            if (logoBitmap != null) {
                // Escalar el logo al tamaño deseado
                val logoEscalado = Bitmap.createScaledBitmap(
                    logoBitmap, 
                    elemento.width.toInt(), 
                    elemento.height.toInt(), 
                    true
                )
                
                // Dibujar desde la esquina superior derecha (alineado simétrico al STAMP)
                canvas.drawBitmap(
                    logoEscalado,
                    elemento.x,  // Posición directa desde la esquina
                    elemento.y,  // Posición directa desde la esquina
                    null
                )
                
                logoEscalado.recycle()
                logoBitmap.recycle()
            } else {
                // Fallback: crear logo simple
                val paint = Paint().apply {
                    color = elemento.color
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                
                val paintTexto = Paint().apply {
                    color = Color.WHITE
                    textSize = 32f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                    isFakeBoldText = true
                }
                
                val centerX = elemento.x + elemento.width/2
                val centerY = elemento.y + elemento.height/2
                canvas.drawCircle(centerX, centerY, elemento.width/2, paint)
                canvas.drawText("BITHERM", centerX, centerY + 10, paintTexto)
            }
        } catch (e: Exception) {
            // Fallback: crear logo simple
            val paint = Paint().apply {
                color = elemento.color
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            
            val paintTexto = Paint().apply {
                color = Color.WHITE
                textSize = 32f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                isFakeBoldText = true
            }
            
            val centerX = elemento.x + elemento.width/2
            val centerY = elemento.y + elemento.height/2
            canvas.drawCircle(centerX, centerY, elemento.width/2, paint)
            canvas.drawText("BITHERM", centerX, centerY + 10, paintTexto)
        }
        
        // Dibujar borde si está seleccionado
        if (elemento.seleccionado) {
            val paintBorde = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 4f
                isAntiAlias = true
            }
            val centerX = elemento.x + elemento.width/2
            val centerY = elemento.y + elemento.height/2
            canvas.drawCircle(centerX, centerY, elemento.width/2, paintBorde)
        }
    }
} 
