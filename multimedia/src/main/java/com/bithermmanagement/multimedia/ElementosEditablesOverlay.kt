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
    
    fun setPhotoView(photoView: PhotoView) {
        this.photoView = photoView
    }
    
    fun setElementosEditables(elementos: MutableList<EditorWhatsAppStyleFragment.ElementoEditable>) {
        this.elementosEditables = elementos
        invalidate()
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        photoView?.let { pv ->
            // Obtener la matriz de transformación del PhotoView
            val imageMatrix = pv.imageMatrix
            
            // Aplicar la misma transformación que tiene la imagen
            canvas.concat(imageMatrix)
            
            // Dibujar cada elemento editable
            elementosEditables.forEach { elemento ->
                dibujarElemento(canvas, elemento)
            }
        }
    }
    
    private fun dibujarElemento(canvas: Canvas, elemento: EditorWhatsAppStyleFragment.ElementoEditable) {
        // Guardar el estado actual del canvas
        canvas.save()
        
        // Aplicar transformaciones al elemento
        canvas.translate(elemento.x, elemento.y)
        canvas.rotate(elemento.rotacion)
        canvas.scale(elemento.escala, elemento.escala)
        canvas.translate(-elemento.x, -elemento.y)
        
        when (elemento.tipo) {
            EditorWhatsAppStyleFragment.TipoElemento.FLECHA -> dibujarFlecha(canvas, elemento)
            EditorWhatsAppStyleFragment.TipoElemento.CIRCULO -> dibujarCirculo(canvas, elemento)
            EditorWhatsAppStyleFragment.TipoElemento.CUADRADO -> dibujarCuadrado(canvas, elemento)
            EditorWhatsAppStyleFragment.TipoElemento.TEXTO -> dibujarTexto(canvas, elemento)
            EditorWhatsAppStyleFragment.TipoElemento.STAMP -> dibujarStamp(canvas, elemento)
            EditorWhatsAppStyleFragment.TipoElemento.LOGO -> dibujarLogo(canvas, elemento)
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
        
        // Crear flecha con cola en la base del triángulo
        val path = Path()
        
        val centerX = elemento.x
        val centerY = elemento.y
        val ancho = elemento.width
        val alto = elemento.height
        
        // Punta de la flecha (triángulo) - en el lado derecho
        val puntaLeft = centerX + ancho/2 - ancho * 0.3f
        val puntaRight = centerX + ancho/2
        val puntaTop = centerY - alto/2
        val puntaBottom = centerY + alto/2
        
        // Dibujar punta (triángulo)
        path.moveTo(puntaLeft, centerY)
        path.lineTo(puntaRight, puntaTop)
        path.lineTo(puntaRight, puntaBottom)
        path.close()
        
        // Cuerpo de la flecha (rectángulo) - en el lado izquierdo (cola)
        val cuerpoAncho = ancho * 0.7f
        val cuerpoAlto = alto * 0.3f
        val cuerpoLeft = centerX - ancho/2
        val cuerpoRight = puntaLeft
        val cuerpoTop = centerY - cuerpoAlto/2
        val cuerpoBottom = centerY + cuerpoAlto/2
        
        // Dibujar cuerpo (cola)
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
        
        val paintTexto = Paint().apply {
            color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        val paintTextoPequeño = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        // Posición del stamp
        val stampWidth = elemento.width
        val stampHeight = elemento.height
        val stampX = elemento.x - stampWidth/2
        val stampY = elemento.y - stampHeight/2
        
        // Dibujar fondo del stamp
        canvas.drawRect(stampX, stampY, stampX + stampWidth, stampY + stampHeight, paintFondo)
        
        // Línea 1: TAG del equipo (simulado)
        canvas.drawText("TAG_EQUIPO", stampX + stampWidth/2, stampY + 60, paintTexto)
        
        // Línea 2: Coordenadas y precisión
        canvas.drawText("0,0 (0m)", stampX + stampWidth/2, stampY + 110, paintTextoPequeño)
        
        // Línea 3: Comentario
        canvas.drawText("Sin comentario", stampX + stampWidth/2, stampY + 150, paintTextoPequeño)
        
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
                
                canvas.drawBitmap(
                    logoEscalado,
                    elemento.x - elemento.width/2,
                    elemento.y - elemento.height/2,
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
                
                canvas.drawCircle(elemento.x, elemento.y, elemento.width/2, paint)
                canvas.drawText("BITHERM", elemento.x, elemento.y + 10, paintTexto)
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
            
            canvas.drawCircle(elemento.x, elemento.y, elemento.width/2, paint)
            canvas.drawText("BITHERM", elemento.x, elemento.y + 10, paintTexto)
        }
        
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
} 