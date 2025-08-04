package com.bithermmanagement.multimedia

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.util.Log
import android.view.MotionEvent

class OverlayRecorteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var rectanguloRecorte: RectF? = null
    private var onRecorteAplicadoListener: (() -> Unit)? = null
    
    private val paintFondo = Paint().apply {
        color = Color.parseColor("#80000000") // Negro semitransparente (50% opacidad)
        style = Paint.Style.FILL
    }
    
    private val paintBorde = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    
    private val paintGuia = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f, 0f), 0f) // Línea punteada
    }
    
    fun setRectanguloRecorte(rect: RectF?) {
        rectanguloRecorte = rect
        invalidate()
    }
    
    fun setOnRecorteAplicadoListener(listener: () -> Unit) {
        onRecorteAplicadoListener = listener
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("OverlayRecorte", "Toque detectado en overlay")
                onRecorteAplicadoListener?.invoke()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        Log.d("OverlayRecorte", "onDraw llamado - width: $width, height: $height")
        
        rectanguloRecorte?.let { rect ->
            Log.d("OverlayRecorte", "Rectángulo de recorte: $rect")
            
            // Guardar el estado del canvas
            canvas.save()
            
            // Dibujar fondo semitransparente en toda la pantalla
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintFondo)
            
            // Limpiar el área del recorte (hacerla transparente)
            canvas.clipOutRect(rect)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            
            // Restaurar el canvas
            canvas.restore()
            
            // Dibujar borde blanco del cuadro de recorte
            canvas.drawRect(rect, paintBorde)
            
            // Dibujar líneas guía en el centro del cuadro
            val centerX = rect.centerX()
            val centerY = rect.centerY()
            
            // Línea vertical punteada
            canvas.drawLine(centerX, rect.top, centerX, rect.bottom, paintGuia)
            
            // Línea horizontal punteada
            canvas.drawLine(rect.left, centerY, rect.right, centerY, paintGuia)
            
            // Dibujar texto indicativo
            val paintTexto = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            
            val texto = "Área válida 1:1"
            val textoY = rect.bottom + 40f
            canvas.drawText(texto, centerX, textoY, paintTexto)
            
            Log.d("OverlayRecorte", "Dibujo completado")
        } ?: run {
            Log.d("OverlayRecorte", "No hay rectángulo de recorte definido")
        }
    }
} 
 
 