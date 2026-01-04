package com.bithermmanagement.multimedia

import android.graphics.*
import android.util.Log
import com.bithermmanagement.multimedia.EditorWhatsAppStyleFragment.ElementoEditable
import com.bithermmanagement.multimedia.EditorWhatsAppStyleFragment.TipoElemento

/**
 * Layer Base: Lienzo fijo de 1920x1920px con borde semitransparente de 15px.
 * Todos los elementos (foto, formas, textos, STAMP, LOGO) se rasterizan aquí.
 * Este bitmap es el resultado final que se guarda.
 */
class LayerBase {
    companion object {
        const val LAYER_SIZE = 1920 // Dimensión total del layer (cuadrado)
        const val BORDER_SIZE = 30  // Grosor del borde (30px semitransparente)
        const val CONTENT_SIZE = LAYER_SIZE - (BORDER_SIZE * 2) // 1860x1860px área útil
    }
    
    private var layerBitmap: Bitmap = Bitmap.createBitmap(LAYER_SIZE, LAYER_SIZE, Bitmap.Config.ARGB_8888)
    private var layerCanvas: Canvas = Canvas(layerBitmap)
    
    private val borderPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0) // Negro semitransparente IGUAL al STAMP
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val contentPaint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
    }
    
    init {
        Log.d("LayerBase", "=== INICIALIZANDO LAYER BASE ===")
        Log.d("LayerBase", "Dimensiones: ${LAYER_SIZE}x${LAYER_SIZE}px (1:1 cuadrado)")
        Log.d("LayerBase", "Borde: ${BORDER_SIZE}px (semitransparente)")
        Log.d("LayerBase", "Área útil: ${CONTENT_SIZE}x${CONTENT_SIZE}px")
        
        // Fondo blanco completo
        layerCanvas.drawColor(Color.WHITE)
    }
    
    /**
     * Dibuja el borde semitransparente SOBRE el contenido ya rasterizado.
     * Se llama al final para que aparezca encima de la foto.
     */
    private fun dibujarBordeSemitransparente() {
        // Borde superior
        layerCanvas.drawRect(0f, 0f, LAYER_SIZE.toFloat(), BORDER_SIZE.toFloat(), borderPaint)
        
        // Borde inferior  
        layerCanvas.drawRect(0f, (LAYER_SIZE - BORDER_SIZE).toFloat(), LAYER_SIZE.toFloat(), LAYER_SIZE.toFloat(), borderPaint)
        
        // Borde izquierdo
        layerCanvas.drawRect(0f, BORDER_SIZE.toFloat(), BORDER_SIZE.toFloat(), (LAYER_SIZE - BORDER_SIZE).toFloat(), borderPaint)
        
        // Borde derecho
        layerCanvas.drawRect((LAYER_SIZE - BORDER_SIZE).toFloat(), BORDER_SIZE.toFloat(), LAYER_SIZE.toFloat(), (LAYER_SIZE - BORDER_SIZE).toFloat(), borderPaint)
        
        Log.d("LayerBase", "✓ Borde semitransparente dibujado sobre contenido")
    }
    
    /**
     * Rasteriza la foto transformada al layer base CUADRADO (1920x1920).
     * La foto debe venir ya recortada en formato cuadrado 1:1.
     * Se escala para llenar completamente el LayerBase manteniendo aspect ratio.
     * 
     * @param bitmap Bitmap de la foto YA RECORTADA cuadrada
     * @param matrix Matriz de transformación aplicada (zoom/pan/rotate) - NO USADA, el usuario ya ajustó en pantalla
     */
    fun rasterizarFoto(bitmap: Bitmap, matrix: Matrix) {
        Log.d("LayerBase", "=== RASTERIZANDO FOTO AL LAYER BASE ===")
        Log.d("LayerBase", "Foto recortada: ${bitmap.width}x${bitmap.height}")
        
        // Fondo blanco
        layerCanvas.drawColor(Color.WHITE)
        
        layerCanvas.save()
        
        // La foto ya viene cuadrada del crop, solo escalar a 1920x1920
        val escala = LAYER_SIZE.toFloat() / bitmap.width.toFloat()
        
        // Dibujar foto escalada a LAYER_SIZE
        layerCanvas.scale(escala, escala)
        layerCanvas.drawBitmap(bitmap, 0f, 0f, contentPaint)
        
        layerCanvas.restore()
        
        // Dibujar borde semitransparente SOBRE la foto
        dibujarBordeSemitransparente()
        
        Log.d("LayerBase", "✓ Foto rasterizada: escala=$escala")
        Log.d("LayerBase", "✓ Layer final: ${LAYER_SIZE}x${LAYER_SIZE} (1:1 cuadrado)")
    }
    
    /**
     * Rasteriza un elemento editable al layer base.
     * Una vez rasterizado, el elemento ya no es editable.
     * 
     * @param elemento Elemento a rasterizar
     * @param escalaDisplay Factor de escala usado para convertir de pantalla a LayerBase (opcional, solo necesario para TEXTO)
     */
    fun rasterizarElemento(elemento: ElementoEditable, escalaDisplay: Float = 1f) {
        Log.d("LayerBase", "=== RASTERIZANDO ELEMENTO AL LAYER BASE ===")
        Log.d("LayerBase", "Tipo: ${elemento.tipo}, Color: ${elemento.color}, Grosor: ${elemento.strokeWidth}")
        
        val paint = Paint().apply {
            color = elemento.color
            strokeWidth = elemento.strokeWidth
            // FLECHA debe estar rellena, TEXTO también, el resto con STROKE
            style = when (elemento.tipo) {
                TipoElemento.TEXTO, TipoElemento.FLECHA -> Paint.Style.FILL
                else -> Paint.Style.STROKE
            }
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        
        // Trasladar coordenadas al área de contenido del layer base
        layerCanvas.save()
        layerCanvas.translate(BORDER_SIZE.toFloat(), BORDER_SIZE.toFloat())
        
        // Aplicar transformaciones del elemento (rotación y escala del usuario)
        // DIBUJO y LINEA ya tienen el path transformado con las coordenadas correctas, no aplicar transformaciones adicionales
        if (elemento.tipo != TipoElemento.STAMP && elemento.tipo != TipoElemento.LOGO && 
            elemento.tipo != TipoElemento.DIBUJO && elemento.tipo != TipoElemento.LINEA) {
            layerCanvas.translate(elemento.x, elemento.y)
            layerCanvas.rotate(elemento.rotacion)
            layerCanvas.scale(elemento.escala, elemento.escala)
            layerCanvas.translate(-elemento.x, -elemento.y)
        }
        
        when (elemento.tipo) {
            TipoElemento.CIRCULO -> {
                // Usar width/height reales del elemento (canvas.scale ya está aplicado)
                val radio = elemento.width / 2f
                layerCanvas.drawCircle(elemento.x, elemento.y, radio, paint)
                Log.d("LayerBase", "✓ Círculo rasterizado en (${elemento.x}, ${elemento.y}) radio=$radio (width=${elemento.width})")
            }
            
            TipoElemento.CUADRADO -> {
                // Usar width/height reales del elemento (canvas.scale ya está aplicado)
                val rect = RectF(
                    elemento.x - elemento.width / 2f,
                    elemento.y - elemento.height / 2f,
                    elemento.x + elemento.width / 2f,
                    elemento.y + elemento.height / 2f
                )
                layerCanvas.drawRect(rect, paint)
                Log.d("LayerBase", "✓ Cuadrado rasterizado en (${elemento.x}, ${elemento.y}) width=${elemento.width} height=${elemento.height}")
            }
            
            TipoElemento.TRIANGULO -> {
                val lado = elemento.escala * 100f
                val altura = (lado * 0.866f)
                val path = Path().apply {
                    moveTo(elemento.x, elemento.y - altura / 2f)
                    lineTo(elemento.x - lado / 2f, elemento.y + altura / 2f)
                    lineTo(elemento.x + lado / 2f, elemento.y + altura / 2f)
                    close()
                }
                layerCanvas.drawPath(path, paint)
                Log.d("LayerBase", "✓ Triángulo rasterizado en (${elemento.x}, ${elemento.y}) lado=$lado")
            }
            
            TipoElemento.FLECHA -> {
                // Usar la misma estructura que en ElementosEditablesOverlay: triángulo + rectángulo, 400% más grande (4x)
                // Pero canvas.scale ya está aplicado, así que solo multiplicamos por 4x
                val centerX = elemento.x
                val centerY = elemento.y
                val ancho = elemento.width * 4f  // 400% más grande como en overlay
                val alto = elemento.height * 4f   // 400% más grande como en overlay
                
                val path = Path()
                
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
                val cuerpoAlto = alto * 0.15f  // Línea más fina
                val cuerpoLeft = centerX - ancho/2
                val cuerpoRight = puntaLeft
                val cuerpoTop = centerY - cuerpoAlto/2
                val cuerpoBottom = centerY + cuerpoAlto/2
                
                // Dibujar línea (cola)
                path.addRect(cuerpoLeft, cuerpoTop, cuerpoRight, cuerpoBottom, Path.Direction.CW)
                
                layerCanvas.drawPath(path, paint)
                Log.d("LayerBase", "✓ Flecha rasterizada en (${elemento.x}, ${elemento.y}) ancho=$ancho alto=$alto (width=${elemento.width}, height=${elemento.height})")
            }
            
            TipoElemento.DIBUJO -> {
                // Renderizar dibujo libre (lápiz)
                elemento.path?.let { path ->
                    if (!path.isEmpty) {
                        val bounds = RectF()
                        path.computeBounds(bounds, true)
                        Log.d("LayerBase", "DIBUJO path bounds antes de dibujar: $bounds")
                        Log.d("LayerBase", "DIBUJO elemento.x=${elemento.x}, elemento.y=${elemento.y}")
                        Log.d("LayerBase", "DIBUJO strokeWidth=${elemento.strokeWidth}")
                        
                        val paintDibujo = Paint(paint).apply {
                            strokeWidth = elemento.strokeWidth
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }
                        layerCanvas.drawPath(path, paintDibujo)
                        Log.d("LayerBase", "✓ Dibujo rasterizado, path no vacío, bounds=$bounds")
                    } else {
                        Log.w("LayerBase", "⚠ Path del dibujo está vacío, no se rasteriza")
                    }
                } ?: run {
                    Log.w("LayerBase", "⚠ Path del dibujo es null, no se rasteriza")
                }
            }
            
            TipoElemento.LINEA -> {
                // Renderizar línea o polilínea
                elemento.path?.let { path ->
                    val paintLinea = Paint(paint).apply {
                        strokeWidth = elemento.strokeWidth
                        style = Paint.Style.STROKE
                    }
                    layerCanvas.drawPath(path, paintLinea)
                    Log.d("LayerBase", "✓ Línea rasterizada")
                }
            }
            
            TipoElemento.TEXTO -> {
                // El canvas.scale(elemento.escala) ya está aplicado arriba
                // El overlay usa 48f fijo y canvas.scale(elemento.escala), así que el tamaño visual es 48f * elemento.escala
                // Cuando se convierte de pantalla a LayerBase, el textSize también necesita escalarse
                // El textSize en píxeles de pantalla (48f) necesita escalarse al tamaño del LayerBase
                // escalaDisplay es la relación entre el tamaño del overlay en pantalla y el LayerBase (1920x1920)
                // Si el overlay se muestra a escala 0.5 en pantalla y el LayerBase es 1920, entonces escalaDisplay = 0.5
                // Para mantener el mismo tamaño visual, el textSize debe ser 48f / escalaDisplay
                // Pero como canvas.scale(elemento.escala) ya está aplicado, el textSize base debe ser 48f / escalaDisplay
                val textPaint = Paint(paint).apply {
                    textSize = 48f / escalaDisplay  // Dividir por escalaDisplay para mantener el tamaño visual relativo
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    setShadowLayer(3f, 1f, 1f, Color.BLACK)
                    style = Paint.Style.FILL
                }
                layerCanvas.drawText(elemento.texto ?: "", elemento.x, elemento.y, textPaint)
                Log.d("LayerBase", "✓ Texto rasterizado: '${elemento.texto}' en (${elemento.x}, ${elemento.y}) textSize=${48f / escalaDisplay} (escalaDisplay=$escalaDisplay, elemento.escala=${elemento.escala})")
            }
            
            TipoElemento.STAMP -> {
                // Renderizar STAMP con fondo semitransparente y texto dinámico
                val paintFondo = Paint().apply {
                    color = Color.parseColor("#80000000")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                
                val stampX = elemento.x
                val stampY = elemento.y
                val stampWidth = elemento.width
                val stampHeight = elemento.height
                
                // Fondo
                layerCanvas.drawRect(stampX, stampY, stampX + stampWidth, stampY + stampHeight, paintFondo)
                
                // Texto dinámico
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
                val y1 = stampY + margenSuperior + alturaLinea1 * 0.75f
                val y2 = y1 + alturaLinea1 * 0.25f + separacion + alturaLinea2 * 0.75f
                val y3 = y2 + alturaLinea2 * 0.25f + separacion + alturaLinea3 * 0.75f
                val y4 = y3 + alturaLinea3 * 0.25f + separacion + alturaLinea4 * 0.75f
                
                layerCanvas.drawText(elemento.stampTag, stampX + margenIzquierdo, y1, paintLinea1)
                layerCanvas.drawText(elemento.stampCoordenadas, stampX + margenIzquierdo, y2, paintLineas234)
                layerCanvas.drawText(elemento.stampFecha, stampX + margenIzquierdo, y3, paintLineas234)
                
                if (elemento.stampComentario.isNotBlank()) {
                    layerCanvas.drawText(elemento.stampComentario, stampX + margenIzquierdo, y4, paintLineas234)
                }
                
                Log.d("LayerBase", "✓ STAMP rasterizado en ($stampX, $stampY)")
            }
            
            TipoElemento.LOGO -> {
                // Renderizar LOGO desde assets
                elemento.bitmap?.let { logoBitmap ->
                    val logoX = elemento.x
                    val logoY = elemento.y
                    val logoWidth = elemento.width
                    val logoHeight = elemento.height
                    
                    val destRect = RectF(logoX, logoY, logoX + logoWidth, logoY + logoHeight)
                    layerCanvas.drawBitmap(logoBitmap, null, destRect, contentPaint)
                    
                    Log.d("LayerBase", "✓ LOGO rasterizado en ($logoX, $logoY)")
                }
            }
            
            else -> {
                Log.w("LayerBase", "⚠ Tipo de elemento no soportado: ${elemento.tipo}")
            }
        }
        
        layerCanvas.restore()
    }
    
    /**
     * Obtiene el bitmap final del layer base.
     * Este es el bitmap que se guarda como foto final.
     */
    fun getBitmap(): Bitmap {
        return layerBitmap
    }
    
    /**
     * Limpia el layer base y reinicia el borde negro.
     */
    fun limpiar() {
        Log.d("LayerBase", "Limpiando layer base...")
        layerBitmap = Bitmap.createBitmap(LAYER_SIZE, LAYER_SIZE, Bitmap.Config.ARGB_8888)
        layerCanvas = Canvas(layerBitmap)
        layerCanvas.drawColor(Color.WHITE)
    }
}
