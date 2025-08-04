package com.bithermmanagement.multimedia

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.chrisbanes.photoview.PhotoView

class CustomPhotoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PhotoView(context, attrs, defStyleAttr) {
    
    private var modoDibujo = false
    private var flechas = mutableListOf<Flecha>()
    private var flechaActual: Flecha? = null
    private var onFlechaDibujadaListener: ((Flecha) -> Unit)? = null
    
    fun setModoDibujo(modo: Boolean) {
        modoDibujo = modo
        invalidate()
    }
    
    fun setOnFlechaDibujadaListener(listener: (Flecha) -> Unit) {
        onFlechaDibujadaListener = listener
    }
    
    fun setOnModoDibujoChangedListener(listener: (Boolean) -> Unit) {
        // Este método se puede implementar si es necesario
        // Por ahora lo dejamos vacío para evitar errores de compilación
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (modoDibujo) {
            dibujarFlechas(canvas)
        }
    }
    
    private fun dibujarFlechas(canvas: Canvas) {
        val paintFlecha = Paint().apply {
            isAntiAlias = true
            color = Color.RED
            strokeWidth = 8f
            style = Paint.Style.STROKE
        }
        
        val paintPunta = Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.FILL
        }
        
        // Dibujar flechas guardadas
        flechas.forEach { flecha ->
            dibujarFlecha(canvas, flecha, paintFlecha, paintPunta)
        }
        
        // Dibujar flecha actual si existe
        flechaActual?.let { flecha ->
            dibujarFlecha(canvas, flecha, paintFlecha, paintPunta)
        }
    }
    
    private fun dibujarFlecha(
        canvas: Canvas, 
        flecha: Flecha, 
        paintFlecha: Paint, 
        paintPunta: Paint
    ) {
        // Dibujar línea principal
        canvas.drawLine(flecha.startX, flecha.startY, flecha.endX, flecha.endY, paintFlecha)
        
        // Calcular ángulo de la flecha
        val angle = Math.atan2(
            (flecha.endY - flecha.startY).toDouble(),
            (flecha.endX - flecha.startX).toDouble()
        )
        
        // Dibujar punta de flecha
        val arrowLength = 20f
        val arrowAngle = Math.PI / 6 // 30 grados
        
        val x1 = flecha.endX - (arrowLength * Math.cos(angle - arrowAngle)).toFloat()
        val y1 = flecha.endY - (arrowLength * Math.sin(angle - arrowAngle)).toFloat()
        val x2 = flecha.endX - (arrowLength * Math.cos(angle + arrowAngle)).toFloat()
        val y2 = flecha.endY - (arrowLength * Math.sin(angle + arrowAngle)).toFloat()
        
        val path = Path()
        path.moveTo(flecha.endX, flecha.endY)
        path.lineTo(x1, y1)
        path.lineTo(x2, y2)
        path.close()
        
        canvas.drawPath(path, paintPunta)
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (modoDibujo) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    flechaActual = Flecha(
                        event.x, event.y, event.x, event.y
                    )
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    flechaActual?.let { flecha ->
                        flechaActual = flecha.copy(endX = event.x, endY = event.y)
                        invalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    flechaActual?.let { flecha ->
                        if (Math.abs(flecha.endX - flecha.startX) > 10 || 
                            Math.abs(flecha.endY - flecha.startY) > 10) {
                            flechas.add(flecha)
                            onFlechaDibujadaListener?.invoke(flecha)
                        }
                        flechaActual = null
                        invalidate()
                    }
                    return true
                }
            }
        }
        
        return super.onTouchEvent(event)
    }
    
    fun limpiarFlechas() {
        flechas.clear()
        flechaActual = null
        invalidate()
    }
    
    fun obtenerFlechas(): List<Flecha> {
        return flechas.toList()
    }
} 